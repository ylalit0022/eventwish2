const express = require('express');
const router = express.Router();
const User = require('../models/User');
const Template = require('../models/Template');
const logger = require('../utils/logger');
const { validateDeviceId, validateFirebaseUid } = require('../middleware/validators');
const { verifyFirebaseToken, optionalFirebaseAuth } = require('../middleware/auth');
const recommendationService = require('../services/recommendationService');
const mongoose = require('mongoose');

/**
 * Helper function to validate ObjectId
 * @param {string} id - ID to validate
 * @returns {boolean} - Whether the ID is valid
 */
function isValidObjectId(id) {
    return mongoose.Types.ObjectId.isValid(id);
}

/**
 * Helper function to handle async operations with comprehensive error handling
 * @param {Function} operation - Async operation to execute
 * @param {string} operationName - Name of the operation for logging
 * @param {Object} res - Express response object
 * @param {string} uid - User ID for logging
 * @returns {Promise} - Result of the operation or error response
 */
async function handleAsyncOperation(operation, operationName, res, uid = 'unknown') {
    try {
        return await operation();
    } catch (error) {
        logger.error(`${operationName} error for user ${uid}: ${error.message}`, {
            stack: error.stack,
            operation: operationName,
            uid: uid
        });
        
        // Handle specific error types
        if (error.name === 'ValidationError') {
            const validationErrors = Object.values(error.errors).map(err => err.message);
            return res.status(400).json({
                success: false,
                message: `Validation error during ${operationName}`,
                errors: validationErrors
            });
        }
        
        if (error.name === 'CastError') {
            return res.status(400).json({
                success: false,
                message: 'Invalid ID format',
                error: 'INVALID_ID'
            });
        }
        
        if (error.code === 11000) {
            return res.status(400).json({
                success: false,
                message: 'Duplicate entry error',
                error: 'DUPLICATE_ENTRY'
            });
        }
        
        if (error.name === 'MongoNetworkError' || error.name === 'MongoTimeoutError') {
            return res.status(503).json({
                success: false,
                message: 'Database connection error. Please try again.',
                error: 'DATABASE_CONNECTION_ERROR'
            });
        }
        
        return res.status(500).json({
            success: false,
            message: `Server error during ${operationName}`,
            error: error.message
        });
    }
}

/**
 * Helper function to clean up invalid subscription data
 * @param {Object} user - User document
 */
function cleanupSubscriptionData(user) {
    if (user.subscription) {
        // Fix invalid plan values
        const validPlans = ['MONTHLY', 'QUARTERLY', 'HALF_YEARLY', 'YEARLY', ''];
        if (!validPlans.includes(user.subscription.plan)) {
            logger.warn(`Fixing invalid subscription plan: ${user.subscription.plan} -> ''`);
            user.subscription.plan = '';
        }
        
        // Fix invalid planLevel values
        const validPlanLevels = ['BASIC', 'PREMIUM', 'PRO', ''];
        if (user.subscription.planLevel !== undefined && !validPlanLevels.includes(user.subscription.planLevel)) {
            logger.warn(`Fixing invalid subscription planLevel: ${user.subscription.planLevel} -> ''`);
            user.subscription.planLevel = '';
        }
        
        // Ensure planLevel is set to empty string if undefined
        if (user.subscription.planLevel === undefined || user.subscription.planLevel === null) {
            user.subscription.planLevel = '';
        }
    }
}

/**
 * Helper function to safely update template counts with transaction support
 * @param {string} templateId - Template ID
 * @param {Object} updates - Object with count updates (e.g., { likes: 1, favorites: -1 })
 * @param {Object} session - MongoDB session for transaction
 * @returns {Promise<Object>} - Updated template or null if not found
 */
async function safeUpdateTemplateCounts(templateId, updates, session = null) {
    try {
        if (!isValidObjectId(templateId)) {
            throw new Error('Invalid template ID format');
        }
        
        // Build the update object
        const updateObj = {};
        for (const [field, increment] of Object.entries(updates)) {
            if (typeof increment === 'number') {
                updateObj[`$inc`] = updateObj[`$inc`] || {};
                updateObj[`$inc`][field] = increment;
            }
        }
        
        if (Object.keys(updateObj).length === 0) {
            throw new Error('No valid updates provided');
        }
        
        const options = {
            new: true,
            upsert: false,
            runValidators: true
        };
        
        if (session) {
            options.session = session;
        }
        
        const updatedTemplate = await Template.findByIdAndUpdate(
            templateId,
            updateObj,
            options
        );
        
        if (!updatedTemplate) {
            logger.warn(`Template not found for count update: ${templateId}`);
            return null;
        }
        
        logger.info(`Updated template ${templateId} counts:`, updates);
        return updatedTemplate;
        
    } catch (error) {
        logger.error(`Failed to update template counts for ${templateId}: ${error.message}`);
        throw error;
    }
}

/**
 * Helper function to validate template interaction request
 * @param {string} uid - User ID
 * @param {string} templateId - Template ID
 * @returns {Object} - Validation result
 */
function validateTemplateInteraction(uid, templateId) {
    const errors = [];
    
    if (!uid || typeof uid !== 'string' || uid.trim().length === 0) {
        errors.push('Valid user ID is required');
    }
    
    if (!templateId || typeof templateId !== 'string' || templateId.trim().length === 0) {
        errors.push('Valid template ID is required');
    }
    
    if (!isValidObjectId(templateId)) {
        errors.push('Invalid template ID format');
    }
    
    return {
        isValid: errors.length === 0,
        errors
    };
}

/**
 * @route   POST /api/users/profile
 * @desc    Update user profile in MongoDB after Firebase authentication
 * @access  Private
 */
router.post('/profile', validateFirebaseUid, verifyFirebaseToken, async (req, res) => {
    try {
        const { 
            uid, 
            displayName, 
            email, 
            profilePhoto, 
            lastOnline,
            deviceId,
            deviceModel,
            deviceName,
            appVersion,
            osVersion
        } = req.body;
        
        // Find user by uid only
        let user = await User.findOne({ uid });
        
        if (user) {
            // User exists, update data
            user.lastOnline = lastOnline || Date.now();
            
            // Update profile info if provided
            if (displayName) user.displayName = displayName;
            if (email) user.email = email;
            if (profilePhoto) user.profilePhoto = profilePhoto;
            
            // Update device info if provided
            if (deviceId) user.deviceId = deviceId;
            if (deviceModel) user.deviceModel = deviceModel;
            if (deviceName) user.deviceName = deviceName;
            if (appVersion) user.appVersion = appVersion;
            if (osVersion) user.osVersion = osVersion;
            
            // Update login timestamp
            user.loginTimestamp = Date.now();
            
            // Track device session if device info is provided
            if (deviceId) {
                try {
                    await user.addDeviceSession({
                        deviceId,
                        deviceModel,
                        deviceName,
                        appVersion,
                        osVersion
                    });
                } catch (sessionError) {
                    logger.warn(`Failed to add device session for user ${uid}: ${sessionError.message}`);
                }
            }
            
            // Clean up invalid subscription data before saving
            cleanupSubscriptionData(user);
            
            await user.save();
            
            logger.info(`User profile updated: UID: ${uid}`);
            return res.status(200).json({
                success: true,
                message: 'User profile updated',
                user
            });
        } else {
            // User doesn't exist, create new user with uid only
            user = new User({
                uid,
                displayName: displayName || null,
                email: email || null,
                profilePhoto: profilePhoto || null,
                deviceId: deviceId || null,
                deviceModel: deviceModel || null,
                deviceName: deviceName || null,
                appVersion: appVersion || null,
                osVersion: osVersion || null,
                lastOnline: lastOnline || Date.now(),
                loginTimestamp: Date.now(),
                created: Date.now(),
                categories: []
            });
            
            await user.save();
            
            // Track device session if device info is provided
            if (deviceId) {
                try {
                    await user.addDeviceSession({
                        deviceId,
                        deviceModel,
                        deviceName,
                        appVersion,
                        osVersion
                    });
                } catch (sessionError) {
                    logger.warn(`Failed to add device session for new user ${uid}: ${sessionError.message}`);
                }
            }
            
            logger.info(`New user profile created: UID: ${uid}`);
            
            return res.status(201).json({
                success: true,
                message: 'User profile created',
                user
            });
        }
    } catch (error) {
        logger.error(`User profile update error: ${error.message}`);
        res.status(500).json({
            success: false,
            message: 'Server error updating user profile',
            error: error.message
        });
    }
});

/**
 * @route   POST /api/users/register
 * @desc    Register a new user with Firebase UID
 * @access  Private
 */
router.post('/register', validateFirebaseUid, verifyFirebaseToken, async (req, res) => {
    try {
        const { 
            uid, 
            displayName, 
            email, 
            profilePhoto,
            deviceId,
            deviceModel,
            deviceName,
            appVersion,
            osVersion
        } = req.body;
        
        // Enhanced logging for debugging
        logger.info(`Registration attempt for UID: ${uid}`, {
            hasDisplayName: !!displayName,
            hasEmail: !!email,
            hasDeviceId: !!deviceId,
            deviceModel,
            appVersion
        });
        
        // Find user by uid
        let user = await User.findOne({ uid });
        
        if (user) {
            // User already exists, update lastOnline
            user.lastOnline = Date.now();
            user.loginTimestamp = Date.now();
            
            // Update profile info if provided
            if (displayName) user.displayName = displayName;
            if (email) user.email = email;
            if (profilePhoto) user.profilePhoto = profilePhoto;
            
            // Update device info if provided
            if (deviceId) user.deviceId = deviceId;
            if (deviceModel) user.deviceModel = deviceModel;
            if (deviceName) user.deviceName = deviceName;
            if (appVersion) user.appVersion = appVersion;
            if (osVersion) user.osVersion = osVersion;
            
            // Clean up invalid subscription data before saving
            cleanupSubscriptionData(user);
            
            await user.save();
            
            // Track device session if device info is provided
            if (deviceId) {
                try {
                    await user.addDeviceSession({
                        deviceId,
                        deviceModel,
                        deviceName,
                        appVersion,
                        osVersion
                    });
                    logger.info(`Device session added for existing user ${uid}: ${deviceId}`);
                } catch (sessionError) {
                    logger.warn(`Failed to add device session for existing user ${uid}: ${sessionError.message}`);
                }
            }
            
            logger.info(`Existing user logged in: UID: ${uid}`);
            return res.status(200).json({
                success: true,
                message: 'User already exists',
                user
            });
        }
        
        // Create new user with uid
        user = new User({
            uid,
            displayName: displayName || null,
            email: email || null,
            profilePhoto: profilePhoto || null,
            deviceId: deviceId || null,
            deviceModel: deviceModel || null,
            deviceName: deviceName || null,
            appVersion: appVersion || null,
            osVersion: osVersion || null,
            lastOnline: lastOnline || Date.now(),
            loginTimestamp: Date.now(),
            created: Date.now(),
            categories: []
        });
        
        await user.save();
        logger.info(`New user document saved: UID: ${uid}`);
        
        // Track device session if device info is provided
        if (deviceId) {
            try {
                await user.addDeviceSession({
                    deviceId,
                    deviceModel,
                    deviceName,
                    appVersion,
                    osVersion
                });
                logger.info(`Device session added for new user ${uid}: ${deviceId}`);
            } catch (sessionError) {
                logger.warn(`Failed to add device session for new registered user ${uid}: ${sessionError.message}`);
            }
        }
        
        logger.info(`New user registered successfully: UID: ${uid}`);
        
        res.status(201).json({
            success: true,
            message: 'User registered successfully',
            user
        });
    } catch (error) {
        logger.error(`User registration error: ${error.message}`, {
            stack: error.stack,
            uid: req.body?.uid,
            deviceId: req.body?.deviceId
        });
        
        // Check for specific MongoDB validation errors
        if (error.name === 'ValidationError') {
            const validationErrors = Object.values(error.errors).map(err => err.message);
            logger.error(`MongoDB validation errors: ${validationErrors.join(', ')}`);
            
            return res.status(400).json({
                success: false,
                message: 'Validation error during registration',
                errors: validationErrors
            });
        }
        
        // Check for duplicate key errors
        if (error.code === 11000) {
            logger.error(`Duplicate key error during registration: ${JSON.stringify(error.keyPattern)}`);
            
            return res.status(400).json({
                success: false,
                message: 'User with this identifier already exists',
                error: 'DUPLICATE_USER'
            });
        }
        
        res.status(500).json({
            success: false,
            message: 'Server error during registration',
            error: error.message
        });
    }
});

/**
 * @route   PUT /api/users/activity
 * @desc    Update user's last online timestamp and optionally record category visit
 * @access  Private
 */
router.put('/activity', validateFirebaseUid, verifyFirebaseToken, async (req, res) => {
    try {
        const { uid, category, source = 'direct' } = req.body;
        
        // Find user by uid
        let user = await User.findOne({ uid });
        
        if (!user) {
            logger.warn(`Activity update attempted for non-existent user: UID ${uid}`);
            return res.status(404).json({
                success: false,
                message: 'User not found'
            });
        }
        
        // Update lastOnline
        user.lastOnline = Date.now();
        
        // If category provided, record visit
        if (category) {
            await user.visitCategory(category, source);
            logger.info(`User ${uid} visited category: ${category} (source: ${source})`);
            
            // Invalidate recommendations cache on category visit
            await recommendationService.invalidateUserRecommendations(uid);
        } else {
            await user.save();
            logger.info(`User ${uid} activity updated (last online)`);
        }
        
        res.status(200).json({
            success: true,
            message: 'User activity updated'
        });
    } catch (error) {
        logger.error(`User activity update error: ${error.message}`);
        res.status(500).json({
            success: false,
            message: 'Server error during activity update',
            error: error.message
        });
    }
});

/**
 * @route   PUT /api/users/template-view
 * @desc    Record a template view with its category
 * @access  Private
 */
router.put('/template-view', validateFirebaseUid, verifyFirebaseToken, async (req, res) => {
    try {
        const { uid, templateId, category } = req.body;
        
        if (!templateId || !category) {
            return res.status(400).json({
                success: false,
                message: 'Template ID and category are required'
            });
        }
        
        // Find user by uid
        let user = await User.findOne({ uid });
        
        if (!user) {
            logger.warn(`Template view attempted for non-existent user: UID ${uid}`);
            return res.status(404).json({
                success: false,
                message: 'User not found'
            });
        }
        
        // Record category visit from template
        await user.visitCategoryFromTemplate(category, templateId);
        
        // Set last active template
        await user.setLastActiveTemplate(templateId, 'VIEW');
        
        // Add to engagement log
        if (!user.engagementLog) {
            user.engagementLog = [];
        }
        user.engagementLog.push({
            action: 'VIEW',
            templateId: templateId,
            timestamp: Date.now()
        });
        
        // Add to recent templates if not already there
        if (!user.recentTemplatesUsed) {
            user.recentTemplatesUsed = [];
        }
        
        // Remove the template if it's already in the list
        user.recentTemplatesUsed = user.recentTemplatesUsed.filter(
            id => id.toString() !== templateId.toString()
        );
        
        // Add to the beginning of the list (most recent)
        user.recentTemplatesUsed.unshift(templateId);
        
        // Keep only the 10 most recent templates
        if (user.recentTemplatesUsed.length > 10) {
            user.recentTemplatesUsed = user.recentTemplatesUsed.slice(0, 10);
        }
        
        await user.save();
        
        logger.info(`User ${uid} viewed template ${templateId} in category: ${category}`);
        
        // Invalidate recommendations cache on template view
        await recommendationService.invalidateUserRecommendations(uid);
        
        res.status(200).json({
            success: true,
            message: 'Template view recorded'
        });
    } catch (error) {
        logger.error(`Template view error: ${error.message}`);
        res.status(500).json({
            success: false,
            message: 'Server error recording template view',
            error: error.message
        });
    }
});

/**
 * @route   GET /api/users/:uid
 * @desc    Get user data by Firebase UID (preferred) or device ID (fallback)
 * @access  Private
 */
router.get('/:uid', verifyFirebaseToken, async (req, res) => {
    try {
        const uid = req.params.uid;
        
        // Find user by uid
        let user = await User.findOne({ uid });
        
        // For backward compatibility, try deviceId fallback
        if (!user) {
            user = await User.findOne({ deviceId: uid });
            
            if (!user) {
                logger.warn(`User requested for non-existent user: UID ${uid}`);
                return res.status(404).json({
                    success: false,
                    message: 'User not found'
                });
            }
            
            logger.info(`Found user by deviceId fallback: ${uid}`);
        }
        
        res.status(200).json({
            success: true,
            user
        });
    } catch (error) {
        logger.error(`Get user error: ${error.message}`);
        res.status(500).json({
            success: false,
            message: 'Server error retrieving user',
            error: error.message
        });
    }
});

/**
 * @route   GET /api/users/:uid/recommendations
 * @desc    Get personalized template recommendations for a user
 * @access  Public
 */
router.get('/:uid/recommendations', async (req, res) => {
    try {
        const uid = req.params.uid;
        const limit = parseInt(req.query.limit) || 10;
        
        if (!uid) {
            return res.status(400).json({
                success: false,
                message: 'Firebase UID is required'
            });
        }
        
        // Find user by uid
        let user = await User.findOne({ uid });
        
        if (!user) {
            // For backward compatibility, try to find by deviceId as a fallback
            user = await User.findOne({ deviceId: uid });
            
            if (!user) {
                logger.warn(`Recommendations requested for non-existent user: UID ${uid}`);
                return res.status(404).json({
                    success: false,
                    message: 'User not found'
                });
            }
            
            logger.info(`Found user by deviceId fallback for recommendations: ${uid}`);
        }
        
        // Use uid for recommendation service, ensuring we have a consistent identifier
        const userId = user.uid || uid;
        
        // Get recommendations using the recommendation service
        const recommendations = await recommendationService.getRecommendationsForUser(userId, limit);
        
        res.status(200).json({
            success: true,
            recommendations
        });
    } catch (error) {
        logger.error(`Recommendations error: ${error.message}`);
        res.status(500).json({
            success: false,
            message: 'Server error generating recommendations',
            error: error.message
        });
    }
});

/**
 * @route   POST /api/users/engagement
 * @desc    Record detailed user engagement metrics
 * @access  Private
 */
router.post('/engagement', validateFirebaseUid, verifyFirebaseToken, async (req, res) => {
    return handleAsyncOperation(async () => {
        const { uid, type, templateId, category, timestamp, durationMs, engagementScore, source } = req.body;
        
        // Validate required fields
        if (!uid || !type) {
            return res.status(400).json({
                success: false,
                message: 'Firebase UID and engagement type are required'
            });
        }
        
        // Validate templateId if provided
        if (templateId && !isValidObjectId(templateId)) {
            return res.status(400).json({
                success: false,
                message: 'Invalid template ID format'
            });
        }
        
        // Start a transaction for data consistency
        const session = await mongoose.startSession();
        
        try {
            await session.withTransaction(async () => {
        // Find user by uid
                const user = await User.findOne({ uid }).session(session);
        
        if (!user) {
                    throw new Error('User not found');
        }
        
        // Record engagement based on type
        switch (type) {
            case 1: // Category visit
                if (category) {
                    await user.visitCategory(category, source || 'direct');
                    logger.info(`User ${uid} engagement: visited category ${category}`);
                }
                break;
                
            case 2: // Template view
                if (templateId && category) {
                    await user.visitCategoryFromTemplate(category, templateId);
                    await user.setLastActiveTemplate(templateId, 'VIEW');
                    
                    // Add to engagement log
                    if (!user.engagementLog) user.engagementLog = [];
                    user.engagementLog.push({
                        action: 'VIEW',
                        templateId,
                        timestamp: timestamp || Date.now()
                    });
                    
                    logger.info(`User ${uid} engagement: viewed template ${templateId} in ${category}`);
                }
                break;
                
            case 3: // Template use
                if (templateId && category) {
                    // Record as a stronger engagement
                    await user.visitCategoryFromTemplate(category, templateId);
                    await user.setLastActiveTemplate(templateId, 'SHARE');
                    
                    // Add to engagement log
                    if (!user.engagementLog) user.engagementLog = [];
                    user.engagementLog.push({
                        action: 'SHARE',
                        templateId,
                        timestamp: timestamp || Date.now()
                    });
                            
                            // Increment template share count
                            await safeUpdateTemplateCounts(templateId, { sharedCount: 1 }, session);
                    
                    // Add to recent templates
                    if (!user.recentTemplatesUsed) user.recentTemplatesUsed = [];
                    
                    // Remove template if already in list
                    user.recentTemplatesUsed = user.recentTemplatesUsed.filter(
                        id => id.toString() !== templateId.toString()
                    );
                    
                    // Add to beginning of list
                    user.recentTemplatesUsed.unshift(templateId);
                    
                    // Keep only 10 most recent
                    if (user.recentTemplatesUsed.length > 10) {
                        user.recentTemplatesUsed = user.recentTemplatesUsed.slice(0, 10);
                    }
                    
                    logger.info(`User ${uid} engagement: used template ${templateId} in ${category}`);
                }
                break;
                
            case 4: // Explicit like
                if (templateId && category) {
                    // Record as like
                    await user.visitCategoryFromTemplate(category, templateId);
                    await user.setLastActiveTemplate(templateId, 'LIKE');
                    
                    // Add to likes if not already there
                    if (!user.likes) user.likes = [];
                            const wasNotLiked = !user.likes.some(id => id.toString() === templateId.toString());
                            if (wasNotLiked) {
                        user.likes.push(templateId);
                                // Increment template like count
                                await safeUpdateTemplateCounts(templateId, { likes: 1 }, session);
                    }
                    
                    // Add to engagement log
                    if (!user.engagementLog) user.engagementLog = [];
                    user.engagementLog.push({
                        action: 'LIKE',
                        templateId,
                        timestamp: timestamp || Date.now()
                    });
                    
                    logger.info(`User ${uid} engagement: liked template ${templateId}`);
                }
                break;
                
            case 5: // Add to favorites
                if (templateId && category) {
                    // Record as favorite
                    await user.visitCategoryFromTemplate(category, templateId);
                    await user.setLastActiveTemplate(templateId, 'FAV');
                    
                    // Add to favorites if not already there
                    if (!user.favorites) user.favorites = [];
                            const wasNotFavorited = !user.favorites.some(id => id.toString() === templateId.toString());
                            if (wasNotFavorited) {
                        user.favorites.push(templateId);
                                // Increment template favorite count
                                await safeUpdateTemplateCounts(templateId, { favorites: 1 }, session);
                    }
                    
                    // Add to engagement log
                    if (!user.engagementLog) user.engagementLog = [];
                    user.engagementLog.push({
                        action: 'FAV',
                        templateId,
                        timestamp: timestamp || Date.now()
                    });
                    
                    logger.info(`User ${uid} engagement: favorited template ${templateId}`);
                }
                break;
                
            default:
                logger.warn(`Unknown engagement type ${type} from user ${uid}`);
        }
        
        // Update last online time
        user.lastOnline = Date.now();
                await user.save({ session });
        
        // Invalidate recommendations cache
        await recommendationService.invalidateUserRecommendations(uid);
        
                return res.status(200).json({
            success: true,
            message: 'Engagement recorded successfully'
        });
            });
        } finally {
            await session.endSession();
        }
    }, 'Record engagement', res, uid);
});

/**
 * @route   POST /api/users/engagement/sync
 * @desc    Sync multiple engagement records in batch
 * @access  Private
 */
router.post('/engagement/sync', validateFirebaseUid, verifyFirebaseToken, async (req, res) => {
    return handleAsyncOperation(async () => {
        const { uid, engagements } = req.body;
        
        if (!uid || !engagements || !Array.isArray(engagements)) {
            return res.status(400).json({
                success: false,
                message: 'Firebase UID and engagement array are required'
            });
        }
        
        // Validate all templateIds in batch
        for (const engagement of engagements) {
            if (engagement.templateId && !isValidObjectId(engagement.templateId)) {
                return res.status(400).json({
                    success: false,
                    message: 'Invalid template ID format in engagement data'
                });
            }
        }
        
        // Start a transaction for data consistency
        const session = await mongoose.startSession();
        
        try {
            await session.withTransaction(async () => {
        // Find user by uid
                const user = await User.findOne({ uid }).session(session);
        
        if (!user) {
                    throw new Error('User not found');
        }
        
        // Process each engagement record
        let processed = 0;
        for (const engagement of engagements) {
            try {
                const { type, templateId, category, source, timestamp } = engagement;
                
                // Process based on type (simplified implementation)
                if (type === 1 && category) {
                    // Category visit
                    await user.visitCategory(category, source || 'direct');
                    processed++;
                } 
                else if ((type === 2 || type === 3) && templateId && category) {
                    // Template view or use
                    await user.visitCategoryFromTemplate(category, templateId);
                    
                    // Set appropriate action
                    const action = type === 2 ? 'VIEW' : 'SHARE';
                    await user.setLastActiveTemplate(templateId, action);
                    
                    // Add to engagement log
                    if (!user.engagementLog) user.engagementLog = [];
                    user.engagementLog.push({
                        action,
                        templateId,
                        timestamp: timestamp || Date.now()
                    });
                    
                            // For template use, add to recent templates and increment share count
                    if (type === 3) {
                                // Increment template share count
                                await safeUpdateTemplateCounts(templateId, { sharedCount: 1 }, session);
                                
                        if (!user.recentTemplatesUsed) user.recentTemplatesUsed = [];
                        
                        // Remove template if already in list
                        user.recentTemplatesUsed = user.recentTemplatesUsed.filter(
                            id => id.toString() !== templateId.toString()
                        );
                        
                        // Add to beginning of list
                        user.recentTemplatesUsed.unshift(templateId);
                        
                        // Keep only 10 most recent
                        if (user.recentTemplatesUsed.length > 10) {
                            user.recentTemplatesUsed = user.recentTemplatesUsed.slice(0, 10);
                        }
                    }
                    
                    processed++;
                }
                else if (type === 4 && templateId) {
                    // Like
                    if (category) {
                        await user.visitCategoryFromTemplate(category, templateId);
                    }
                    
                    await user.setLastActiveTemplate(templateId, 'LIKE');
                    
                    // Add to likes if not already there
                    if (!user.likes) user.likes = [];
                            const wasNotLiked = !user.likes.some(id => id.toString() === templateId.toString());
                            if (wasNotLiked) {
                        user.likes.push(templateId);
                                // Increment template like count only if it wasn't already liked
                                await safeUpdateTemplateCounts(templateId, { likes: 1 }, session);
                    }
                    
                    // Add to engagement log
                    if (!user.engagementLog) user.engagementLog = [];
                    user.engagementLog.push({
                        action: 'LIKE',
                        templateId,
                        timestamp: timestamp || Date.now()
                    });
                    
                    processed++;
                }
                else if (type === 5 && templateId) {
                    // Favorite
                    if (category) {
                        await user.visitCategoryFromTemplate(category, templateId);
                    }
                    
                    await user.setLastActiveTemplate(templateId, 'FAV');
                    
                    // Add to favorites if not already there
                    if (!user.favorites) user.favorites = [];
                            const wasNotFavorited = !user.favorites.some(id => id.toString() === templateId.toString());
                            if (wasNotFavorited) {
                        user.favorites.push(templateId);
                                // Increment template favorite count only if it wasn't already favorited
                                await safeUpdateTemplateCounts(templateId, { favorites: 1 }, session);
                    }
                    
                    // Add to engagement log
                    if (!user.engagementLog) user.engagementLog = [];
                    user.engagementLog.push({
                        action: 'FAV',
                        templateId,
                        timestamp: timestamp || Date.now()
                    });
                    
                    processed++;
                }
            } catch (err) {
                logger.warn(`Error processing engagement record: ${err.message}`);
                // Continue with next record even if one fails
            }
        }
        
        // Update last online time
        user.lastOnline = Date.now();
                await user.save({ session });
        
        // Invalidate recommendations cache
        await recommendationService.invalidateUserRecommendations(uid);
        
                return res.status(200).json({
            success: true,
            message: `Processed ${processed} of ${engagements.length} engagement records`
        });
            });
        } finally {
            await session.endSession();
        }
    }, 'Batch engagement sync', res, uid);
});

/**
 * @route   PUT /api/users/preferences
 * @desc    Update user preferences
 * @access  Private
 */
router.put('/preferences', validateFirebaseUid, verifyFirebaseToken, async (req, res) => {
    try {
        const { uid, preferences } = req.body;
        
        if (!uid || !preferences) {
            return res.status(400).json({
                success: false,
                message: 'Firebase UID and preferences are required'
            });
        }
        
        // Find user by uid
        let user = await User.findOne({ uid });
        
        if (!user) {
            logger.warn(`Preferences update attempted for non-existent user: UID ${uid}`);
            return res.status(404).json({
                success: false,
                message: 'User not found'
            });
        }
        
        // Update user preferences
        if (preferences.preferredTheme !== undefined) {
            user.preferredTheme = preferences.preferredTheme;
        }
        
        if (preferences.preferredLanguage !== undefined) {
            user.preferredLanguage = preferences.preferredLanguage;
        }
        
        if (preferences.timezone !== undefined) {
            user.timezone = preferences.timezone;
        }
        
        if (preferences.pushPreferences !== undefined) {
            user.pushPreferences = {
                ...user.pushPreferences || {},
                ...preferences.pushPreferences
            };
        }
        
        if (preferences.topicSubscriptions !== undefined && Array.isArray(preferences.topicSubscriptions)) {
            user.topicSubscriptions = preferences.topicSubscriptions;
        }
        
        if (preferences.muteNotificationsUntil !== undefined) {
            user.muteNotificationsUntil = preferences.muteNotificationsUntil;
        }
        
        // Update last online time
        user.lastOnline = Date.now();
        await user.save();
        
        logger.info(`User ${uid} preferences updated`);
        
        res.status(200).json({
            success: true,
            message: 'User preferences updated successfully',
            user: {
                preferredTheme: user.preferredTheme,
                preferredLanguage: user.preferredLanguage,
                timezone: user.timezone,
                pushPreferences: user.pushPreferences,
                topicSubscriptions: user.topicSubscriptions,
                muteNotificationsUntil: user.muteNotificationsUntil
            }
        });
        
    } catch (error) {
        logger.error(`User preferences update error: ${error.message}`, { error });
        res.status(500).json({
            success: false,
            message: 'Server error updating user preferences',
            error: error.message
        });
    }
});

/**
 * @route   GET /api/users/:uid/templates/favorites
 * @desc    Get user's favorite templates
 * @access  Public
 */
router.get('/:uid/templates/favorites', async (req, res) => {
    try {
        const uid = req.params.uid;
        
        // Find user by uid (don't populate, just get the IDs)
        let user = await User.findOne({ uid });
        
        // For backward compatibility, try deviceId fallback
        if (!user) {
            user = await User.findOne({ deviceId: uid });
            
            if (!user) {
                logger.warn(`Favorites requested for non-existent user: UID ${uid}`);
                return res.status(404).json({
                    success: false,
                    message: 'User not found'
                });
            }
            
            logger.info(`Found user by deviceId fallback for favorites: ${uid}`);
        }
        
        // Convert ObjectIds to strings for the response
        const favoriteIds = (user.favorites || []).map(id => id.toString());
        
        logger.info(`Returning ${favoriteIds.length} favorite template IDs for user ${uid}`);
        
        res.status(200).json({
            success: true,
            favorites: favoriteIds
        });
    } catch (error) {
        logger.error(`Get favorites error: ${error.message}`);
        res.status(500).json({
            success: false,
            message: 'Server error retrieving favorites',
            error: error.message
        });
    }
});

/**
 * @route   GET /api/users/:uid/templates/likes
 * @desc    Get user's liked templates
 * @access  Public
 */
router.get('/:uid/templates/likes', async (req, res) => {
    try {
        const uid = req.params.uid;
        
        // Find user by uid (don't populate, just get the IDs)
        let user = await User.findOne({ uid });
        
        // For backward compatibility, try deviceId fallback
        if (!user) {
            user = await User.findOne({ deviceId: uid });
            
            if (!user) {
                logger.warn(`Likes requested for non-existent user: UID ${uid}`);
                return res.status(404).json({
                    success: false,
                    message: 'User not found'
                });
            }
            
            logger.info(`Found user by deviceId fallback for likes: ${uid}`);
        }
        
        // Convert ObjectIds to strings for the response
        const likeIds = (user.likes || []).map(id => id.toString());
        
        logger.info(`Returning ${likeIds.length} liked template IDs for user ${uid}`);
        
        res.status(200).json({
            success: true,
            likes: likeIds
        });
    } catch (error) {
        logger.error(`Get likes error: ${error.message}`);
        res.status(500).json({
            success: false,
            message: 'Server error retrieving likes',
            error: error.message
        });
    }
});

/**
 * @route   GET /api/users/:uid/templates/recent
 * @desc    Get user's recently used templates
 * @access  Public
 */
router.get('/:uid/templates/recent', async (req, res) => {
    try {
        const uid = req.params.uid;
        
        // Find user by uid
        let user = await User.findOne({ uid }).populate('recentTemplatesUsed');
        
        // For backward compatibility, try deviceId fallback
        if (!user) {
            user = await User.findOne({ deviceId: uid }).populate('recentTemplatesUsed');
            
            if (!user) {
                logger.warn(`Recent templates requested for non-existent user: UID ${uid}`);
                return res.status(404).json({
                    success: false,
                    message: 'User not found'
                });
            }
            
            logger.info(`Found user by deviceId fallback for recent templates: ${uid}`);
        }
        
        res.status(200).json({
            success: true,
            recentTemplates: user.recentTemplatesUsed || []
        });
    } catch (error) {
        logger.error(`Get recent templates error: ${error.message}`);
        res.status(500).json({
            success: false,
            message: 'Server error retrieving recent templates',
            error: error.message
        });
    }
});

/**
 * @route   PUT /api/users/:uid/subscription
 * @desc    Update user subscription status
 * @access  Public
 */
router.put('/:uid/subscription', validateFirebaseUid, async (req, res) => {
    try {
        const { uid } = req.params;
        const subscriptionData = req.body;
        
        // Find user by uid
        let user = await User.findOne({ uid });
        
        if (!user) {
            logger.warn(`Subscription update attempted for non-existent user: UID ${uid}`);
            return res.status(404).json({
                success: false,
                message: 'User not found'
            });
        }
        
        // Update subscription
        user.subscription = {
            ...user.subscription || {},
            ...subscriptionData
        };
        
        // If subscription is active, disable ads
        if (user.subscription.isActive) {
            user.adsAllowed = false;
        }
        
        await user.save();
        logger.info(`User ${uid} subscription updated`);
        
        res.status(200).json({
            success: true,
            message: 'Subscription updated successfully',
            subscription: user.subscription
        });
        
    } catch (error) {
        logger.error(`Subscription update error: ${error.message}`);
        res.status(500).json({
            success: false,
            message: 'Server error updating subscription',
            error: error.message
        });
    }
});

/**
 * @route   PUT /api/users/:uid/push-preferences
 * @desc    Update user push notification preferences
 * @access  Public
 */
router.put('/:uid/push-preferences', validateFirebaseUid, async (req, res) => {
    try {
        const { uid } = req.params;
        const { pushPreferences } = req.body;
        
        // Find user by uid
        let user = await User.findOne({ uid });
        
        if (!user) {
            logger.warn(`Push preferences update attempted for non-existent user: UID ${uid}`);
            return res.status(404).json({
                success: false,
                message: 'User not found'
            });
        }
        
        // Update push preferences
        user.pushPreferences = {
            ...user.pushPreferences || {},
            ...pushPreferences
        };
        
        await user.save();
        logger.info(`User ${uid} push preferences updated`);
        
        res.status(200).json({
            success: true,
            message: 'Push preferences updated successfully',
            pushPreferences: user.pushPreferences
        });
        
    } catch (error) {
        logger.error(`Push preferences update error: ${error.message}`);
        res.status(500).json({
            success: false,
            message: 'Server error updating push preferences',
            error: error.message
        });
    }
});

/**
 * @route   POST /api/users/:uid/topics/subscribe
 * @desc    Subscribe to notification topics
 * @access  Public
 */
router.post('/:uid/topics/subscribe', validateFirebaseUid, async (req, res) => {
    try {
        const { uid } = req.params;
        const { topics } = req.body;
        
        if (!Array.isArray(topics)) {
            return res.status(400).json({
                success: false,
                message: 'Topics must be an array'
            });
        }
        
        // Find user by uid
        let user = await User.findOne({ uid });
        
        if (!user) {
            logger.warn(`Topic subscription attempted for non-existent user: UID ${uid}`);
            return res.status(404).json({
                success: false,
                message: 'User not found'
            });
        }
        
        // Initialize topicSubscriptions if needed
        if (!user.topicSubscriptions) {
            user.topicSubscriptions = [];
        }
        
        // Add new topics
        topics.forEach(topic => {
            if (!user.topicSubscriptions.includes(topic)) {
                user.topicSubscriptions.push(topic);
            }
        });
        
        await user.save();
        logger.info(`User ${uid} subscribed to topics: ${topics.join(', ')}`);
        
        res.status(200).json({
            success: true,
            message: 'Successfully subscribed to topics',
            topics: user.topicSubscriptions
        });
        
    } catch (error) {
        logger.error(`Topic subscription error: ${error.message}`);
        res.status(500).json({
            success: false,
            message: 'Server error subscribing to topics',
            error: error.message
        });
    }
});

/**
 * @route   POST /api/users/:uid/topics/unsubscribe
 * @desc    Unsubscribe from notification topics
 * @access  Public
 */
router.post('/:uid/topics/unsubscribe', validateFirebaseUid, async (req, res) => {
    try {
        const { uid } = req.params;
        const { topics } = req.body;
        
        if (!Array.isArray(topics)) {
            return res.status(400).json({
                success: false,
                message: 'Topics must be an array'
            });
        }
        
        // Find user by uid
        let user = await User.findOne({ uid });
        
        if (!user) {
            logger.warn(`Topic unsubscription attempted for non-existent user: UID ${uid}`);
            return res.status(404).json({
                success: false,
                message: 'User not found'
            });
        }
        
        // Remove topics
        if (user.topicSubscriptions) {
            user.topicSubscriptions = user.topicSubscriptions.filter(
                topic => !topics.includes(topic)
            );
        }
        
        await user.save();
        logger.info(`User ${uid} unsubscribed from topics: ${topics.join(', ')}`);
        
        res.status(200).json({
            success: true,
            message: 'Successfully unsubscribed from topics',
            topics: user.topicSubscriptions
        });
        
    } catch (error) {
        logger.error(`Topic unsubscription error: ${error.message}`);
        res.status(500).json({
            success: false,
            message: 'Server error unsubscribing from topics',
            error: error.message
        });
    }
});

/**
 * @route   PUT /api/users/:deviceId/link-firebase
 * @desc    Link existing user with Firebase UID
 * @access  Public
 */
router.put('/:deviceId/link-firebase', async (req, res) => {
    try {
        const { deviceId } = req.params;
        const { uid, displayName, email, photoUrl } = req.body;
        
        if (!uid) {
            return res.status(400).json({
                success: false,
                message: 'Firebase UID is required'
            });
        }
        
        // Find user by deviceId
        let user = await User.findOne({ deviceId });
        
        if (!user) {
            return res.status(404).json({
                success: false,
                message: 'User not found'
            });
        }
        
        // Check if UID is already linked to another user
        const existingUser = await User.findOne({ uid });
        if (existingUser && existingUser.deviceId !== deviceId) {
            return res.status(400).json({
                success: false,
                message: 'Firebase UID already linked to another user'
            });
        }
        
        // Update user data
        user.uid = uid;
        if (displayName) user.displayName = displayName;
        if (email) user.email = email;
        if (photoUrl) user.profilePhoto = photoUrl;
        
        await user.save();
        logger.info(`User ${deviceId} linked with Firebase UID: ${uid}`);
        
        res.status(200).json({
            success: true,
            message: 'Successfully linked with Firebase',
            user
        });
        
    } catch (error) {
        logger.error(`Firebase linking error: ${error.message}`);
        res.status(500).json({
            success: false,
            message: 'Server error linking Firebase account',
            error: error.message
        });
    }
});

/**
 * @route   POST /api/users/auth
 * @desc    Handle first-time Firebase authentication and determine if user exists
 * @access  Public
 */
router.post('/auth', validateFirebaseUid, async (req, res) => {
    try {
        const { 
            uid, 
            deviceId,
            deviceModel,
            deviceName,
            appVersion,
            osVersion
        } = req.body;
        
        if (!uid) {
            return res.status(400).json({
                success: false,
                message: 'Firebase UID is required'
            });
        }
        
        // Check if user exists by UID
        let user = await User.findOne({ uid });
        let isNewUser = false;
        let hasActiveSessions = false;
        let otherActiveSessions = [];
        
        if (user) {
            // User exists, update last online and login timestamp
            user.lastOnline = Date.now();
            user.loginTimestamp = Date.now();
            
            // Update device info if provided
            if (deviceId) user.deviceId = deviceId;
            if (deviceModel) user.deviceModel = deviceModel;
            if (deviceName) user.deviceName = deviceName;
            if (appVersion) user.appVersion = appVersion;
            if (osVersion) user.osVersion = osVersion;
            
            await user.save();
            
            // Check if there are other active sessions
            if (user.activeSessions && user.activeSessions.size > 0) {
                // Check if this device is not in the active sessions or if there are other sessions
                if (!deviceId || !user.activeSessions.has(deviceId) || user.activeSessions.size > 1) {
                    hasActiveSessions = true;
                    
                    // Collect other active sessions for the response
                    for (const [sessionDeviceId, session] of user.activeSessions.entries()) {
                        if (sessionDeviceId !== deviceId) {
                            otherActiveSessions.push({
                                deviceId: sessionDeviceId,
                                deviceModel: session.deviceModel,
                                deviceName: session.deviceName,
                                appVersion: session.appVersion,
                                osVersion: session.osVersion,
                                loginTimestamp: session.loginTimestamp,
                                lastActiveTimestamp: session.lastActiveTimestamp
                            });
                        }
                    }
                }
            }
            
            // Track device session if device info is provided
            if (deviceId) {
                try {
                    await user.addDeviceSession({
                        deviceId,
                        deviceModel,
                        deviceName,
                        appVersion,
                        osVersion
                    });
                } catch (sessionError) {
                    logger.warn(`Failed to add device session for existing user ${uid}: ${sessionError.message}`);
                }
            }
            
            logger.info(`Existing user authenticated: UID ${uid}`);
        } else {
            // Check if there's a user with the provided deviceId
            if (deviceId) {
                const deviceUser = await User.findOne({ deviceId });
                
                if (deviceUser) {
                    // Link existing device user with Firebase UID
                    deviceUser.uid = uid;
                    deviceUser.lastOnline = Date.now();
                    deviceUser.loginTimestamp = Date.now();
                    
                    // Update device info if provided
                    if (deviceModel) deviceUser.deviceModel = deviceModel;
                    if (deviceName) deviceUser.deviceName = deviceName;
                    if (appVersion) deviceUser.appVersion = appVersion;
                    if (osVersion) deviceUser.osVersion = osVersion;
                    
                    await deviceUser.save();
                    
                    // Track device session
                    try {
                        await deviceUser.addDeviceSession({
                            deviceId,
                            deviceModel,
                            deviceName,
                            appVersion,
                            osVersion
                        });
                    } catch (sessionError) {
                        logger.warn(`Failed to add device session for linked user ${uid}: ${sessionError.message}`);
                    }
                    
                    user = deviceUser;
                    logger.info(`Linked device ID ${deviceId} with Firebase UID ${uid}`);
                } else {
                    // Create new user with both UID and deviceId
                    isNewUser = true;
                    user = new User({
                        uid,
                        deviceId: deviceId || null,
                        deviceModel: deviceModel || null,
                        deviceName: deviceName || null,
                        appVersion: appVersion || null,
                        osVersion: osVersion || null,
                        lastOnline: Date.now(),
                        loginTimestamp: Date.now(),
                        created: Date.now()
                    });
                    
                    await user.save();
                    
                    // Track device session
                    try {
                        await user.addDeviceSession({
                            deviceId,
                            deviceModel,
                            deviceName,
                            appVersion,
                            osVersion
                        });
                    } catch (sessionError) {
                        logger.warn(`Failed to add device session for new user ${uid}: ${sessionError.message}`);
                    }
                    
                    logger.info(`New user created with UID ${uid} and deviceId ${deviceId || 'null'}`);
                }
            } else {
                // Create new user with just UID
                isNewUser = true;
                user = new User({
                    uid,
                    lastOnline: Date.now(),
                    loginTimestamp: Date.now(),
                    created: Date.now()
                });
                
                await user.save();
                logger.info(`New user created with UID ${uid}`);
            }
        }
        
        // Return user data, new user flag, and active sessions info
        res.status(200).json({
            success: true,
            isNewUser,
            hasActiveSessions,
            otherActiveSessions,
            user: {
                uid: user.uid,
                deviceId: user.deviceId,
                deviceModel: user.deviceModel,
                deviceName: user.deviceName,
                appVersion: user.appVersion,
                osVersion: user.osVersion,
                displayName: user.displayName,
                email: user.email,
                profilePhoto: user.profilePhoto,
                lastOnline: user.lastOnline,
                loginTimestamp: user.loginTimestamp,
                created: user.created,
                subscription: user.subscription,
                pushPreferences: user.pushPreferences,
                preferredTheme: user.preferredTheme,
                preferredLanguage: user.preferredLanguage,
                timezone: user.timezone
            }
        });
    } catch (error) {
        logger.error(`User authentication error: ${error.message}`);
        res.status(500).json({
            success: false,
            message: 'Server error during authentication',
            error: error.message
        });
    }
});

/**
 * @route   PUT /api/users/:uid/favorites/:templateId
 * @desc    Add a template to user's favorites
 * @access  Private
 */
router.put('/:uid/favorites/:templateId', validateFirebaseUid, verifyFirebaseToken, async (req, res) => {
    return handleAsyncOperation(async () => {
        const { uid, templateId } = req.params;
        
        // Validate request
        const validation = validateTemplateInteraction(uid, templateId);
        if (!validation.isValid) {
            return res.status(400).json({
                success: false,
                message: 'Validation failed',
                errors: validation.errors
            });
        }
        
        // Start a transaction for data consistency
        const session = await mongoose.startSession();
        
        try {
            await session.withTransaction(async () => {
        // Find user by uid
                const user = await User.findOne({ uid }).session(session);
        
        if (!user) {
                    throw new Error('User not found');
                }
                
                // Verify template exists
                const template = await Template.findById(templateId).session(session);
                if (!template) {
                    throw new Error('Template not found');
        }
        
        // Initialize favorites array if it doesn't exist
        if (!user.favorites) {
            user.favorites = [];
        }
        
        // Check if template is already in favorites
                const isAlreadyFavorited = user.favorites.some(id => id.toString() === templateId.toString());
                
                if (!isAlreadyFavorited) {
            // Add to favorites
            user.favorites.push(templateId);
            
            // Add to engagement log
            if (!user.engagementLog) {
                user.engagementLog = [];
            }
            
            user.engagementLog.push({
                action: 'FAV',
                templateId,
                timestamp: Date.now()
            });
            
            // Update last active template
            user.lastActiveTemplate = templateId;
            user.lastActionOnTemplate = 'FAV';
            
            // Update last online
            user.lastOnline = Date.now();
            
                    // Save user first
                    await user.save({ session });
                    
                    // Increment template favorite count
                    await safeUpdateTemplateCounts(templateId, { favorites: 1 }, session);
            
            // Invalidate recommendations
            await recommendationService.invalidateUserRecommendations(uid);
            
            logger.info(`User ${uid} added template ${templateId} to favorites`);
            
            return res.status(200).json({
                success: true,
                        message: 'Template added to favorites successfully',
                        data: {
                            templateId,
                            isFavorited: true,
                            totalFavorites: user.favorites.length
                        }
            });
        } else {
            // Template already in favorites
            return res.status(200).json({
                success: true,
                message: 'Template already in favorites',
                        data: {
                            templateId,
                            isFavorited: true,
                            totalFavorites: user.favorites.length
                        }
                    });
                }
            });
        } finally {
            await session.endSession();
        }
    }, 'Add favorite', res, uid);
});

/**
 * @route   DELETE /api/users/:uid/favorites/:templateId
 * @desc    Remove a template from user's favorites
 * @access  Private
 */
router.delete('/:uid/favorites/:templateId', validateFirebaseUid, verifyFirebaseToken, async (req, res) => {
    return handleAsyncOperation(async () => {
        const { uid, templateId } = req.params;
        
        // Validate request
        const validation = validateTemplateInteraction(uid, templateId);
        if (!validation.isValid) {
            return res.status(400).json({
                success: false,
                message: 'Validation failed',
                errors: validation.errors
            });
        }
        
        // Start a transaction for data consistency
        const session = await mongoose.startSession();
        
        try {
            await session.withTransaction(async () => {
        // Find user by uid
                const user = await User.findOne({ uid }).session(session);
        
        if (!user) {
                    throw new Error('User not found');
        }
        
        // Check if user has favorites
        if (!user.favorites || user.favorites.length === 0) {
            return res.status(200).json({
                success: true,
                message: 'No favorites to remove',
                        data: {
                            templateId,
                            isFavorited: false,
                            totalFavorites: 0
                        }
            });
        }
        
        // Remove template from favorites
        const initialCount = user.favorites.length;
        user.favorites = user.favorites.filter(id => id.toString() !== templateId.toString());
        
        // Check if anything was removed
        if (user.favorites.length < initialCount) {
            // Add to engagement log
            if (!user.engagementLog) {
                user.engagementLog = [];
            }
            
            user.engagementLog.push({
                action: 'UNFAV',
                templateId,
                timestamp: Date.now()
            });
            
            // Update last active template
            user.lastActiveTemplate = templateId;
            user.lastActionOnTemplate = 'UNFAV';
            
            // Update last online
            user.lastOnline = Date.now();
            
                    // Save user first
                    await user.save({ session });
                    
                    // Decrement template favorite count
                    await safeUpdateTemplateCounts(templateId, { favorites: -1 }, session);
            
            // Invalidate recommendations
            await recommendationService.invalidateUserRecommendations(uid);
            
            logger.info(`User ${uid} removed template ${templateId} from favorites`);
            
            return res.status(200).json({
                success: true,
                        message: 'Template removed from favorites successfully',
                        data: {
                            templateId,
                            isFavorited: false,
                            totalFavorites: user.favorites.length
                        }
            });
        } else {
            // Template not in favorites
            return res.status(200).json({
                success: true,
                        message: 'Template was not in favorites',
                        data: {
                            templateId,
                            isFavorited: false,
                            totalFavorites: user.favorites.length
                        }
                    });
                }
            });
        } finally {
            await session.endSession();
        }
    }, 'Remove favorite', res, uid);
});

/**
 * @route   PUT /api/users/:uid/likes/:templateId
 * @desc    Add a template to user's likes
 * @access  Private
 */
router.put('/:uid/likes/:templateId', validateFirebaseUid, verifyFirebaseToken, async (req, res) => {
    return handleAsyncOperation(async () => {
        const { uid, templateId } = req.params;
        
        // Validate request
        const validation = validateTemplateInteraction(uid, templateId);
        if (!validation.isValid) {
            return res.status(400).json({
                success: false,
                message: 'Validation failed',
                errors: validation.errors
            });
        }
        
        // Start a transaction for data consistency
        const session = await mongoose.startSession();
        
        try {
            await session.withTransaction(async () => {
        // Find user by uid
                const user = await User.findOne({ uid }).session(session);
        
        if (!user) {
                    throw new Error('User not found');
                }
                
                // Verify template exists
                const template = await Template.findById(templateId).session(session);
                if (!template) {
                    throw new Error('Template not found');
        }
        
        // Initialize likes array if it doesn't exist
        if (!user.likes) {
            user.likes = [];
        }
        
        // Check if template is already liked
                const isAlreadyLiked = user.likes.some(id => id.toString() === templateId.toString());
                
                if (!isAlreadyLiked) {
            // Add to likes
            user.likes.push(templateId);
            
            // Add to engagement log
            if (!user.engagementLog) {
                user.engagementLog = [];
            }
            
            user.engagementLog.push({
                action: 'LIKE',
                templateId,
                timestamp: Date.now()
            });
            
            // Update last active template
            user.lastActiveTemplate = templateId;
            user.lastActionOnTemplate = 'LIKE';
            
            // Update last online
            user.lastOnline = Date.now();
            
                    // Save user first
                    await user.save({ session });
                    
                    // Increment template like count
                    await safeUpdateTemplateCounts(templateId, { likes: 1 }, session);
            
            // Invalidate recommendations
            await recommendationService.invalidateUserRecommendations(uid);
            
            logger.info(`User ${uid} liked template ${templateId}`);
            
            return res.status(200).json({
                success: true,
                        message: 'Template liked successfully',
                        data: {
                            templateId,
                            isLiked: true,
                            totalLikes: user.likes.length
                        }
            });
        } else {
            // Template already liked
            return res.status(200).json({
                success: true,
                message: 'Template already liked',
                        data: {
                            templateId,
                            isLiked: true,
                            totalLikes: user.likes.length
                        }
                    });
                }
            });
        } finally {
            await session.endSession();
        }
    }, 'Add like', res, uid);
});

/**
 * @route   DELETE /api/users/:uid/likes/:templateId
 * @desc    Remove a template from user's likes
 * @access  Private
 */
router.delete('/:uid/likes/:templateId', validateFirebaseUid, verifyFirebaseToken, async (req, res) => {
    return handleAsyncOperation(async () => {
        const { uid, templateId } = req.params;
        
        // Validate request
        const validation = validateTemplateInteraction(uid, templateId);
        if (!validation.isValid) {
            return res.status(400).json({
                success: false,
                message: 'Validation failed',
                errors: validation.errors
            });
        }
        
        // Start a transaction for data consistency
        const session = await mongoose.startSession();
        
        try {
            await session.withTransaction(async () => {
        // Find user by uid
                const user = await User.findOne({ uid }).session(session);
        
        if (!user) {
                    throw new Error('User not found');
        }
        
        // Check if user has likes
        if (!user.likes || user.likes.length === 0) {
            return res.status(200).json({
                success: true,
                message: 'No likes to remove',
                        data: {
                            templateId,
                            isLiked: false,
                            totalLikes: 0
                        }
            });
        }
        
        // Remove template from likes
        const initialCount = user.likes.length;
        user.likes = user.likes.filter(id => id.toString() !== templateId.toString());
        
        // Check if anything was removed
        if (user.likes.length < initialCount) {
            // Add to engagement log
            if (!user.engagementLog) {
                user.engagementLog = [];
            }
            
            user.engagementLog.push({
                action: 'UNLIKE',
                templateId,
                timestamp: Date.now()
            });
            
            // Update last active template
            user.lastActiveTemplate = templateId;
            user.lastActionOnTemplate = 'UNLIKE';
            
            // Update last online
            user.lastOnline = Date.now();
            
                    // Save user first
                    await user.save({ session });
                    
                    // Decrement template like count
                    await safeUpdateTemplateCounts(templateId, { likes: -1 }, session);
            
            // Invalidate recommendations
            await recommendationService.invalidateUserRecommendations(uid);
            
            logger.info(`User ${uid} unliked template ${templateId}`);
            
            return res.status(200).json({
                success: true,
                        message: 'Template unliked successfully',
                        data: {
                            templateId,
                            isLiked: false,
                            totalLikes: user.likes.length
                        }
            });
        } else {
            // Template not liked
            return res.status(200).json({
                success: true,
                        message: 'Template was not liked',
                        data: {
                            templateId,
                            isLiked: false,
                            totalLikes: user.likes.length
                        }
                    });
                }
            });
        } finally {
            await session.endSession();
        }
    }, 'Remove like', res, uid);
});

/**
 * @route   POST /api/users/:uid/templates/:templateId/share
 * @desc    Record template share and increment share count
 * @access  Private
 */
router.post('/:uid/templates/:templateId/share', validateFirebaseUid, verifyFirebaseToken, async (req, res) => {
    return handleAsyncOperation(async () => {
        const { uid, templateId } = req.params;
        const { shareMethod = 'unknown', category } = req.body;
        
        // Validate request
        const validation = validateTemplateInteraction(uid, templateId);
        if (!validation.isValid) {
            return res.status(400).json({
            success: false,
                message: 'Validation failed',
                errors: validation.errors
            });
        }
        
        // Start a transaction for data consistency
        const session = await mongoose.startSession();
        
        try {
            await session.withTransaction(async () => {
                // Find user by uid
                const user = await User.findOne({ uid }).session(session);
                
                if (!user) {
                    throw new Error('User not found');
                }
                
                // Verify template exists
                const template = await Template.findById(templateId).session(session);
                if (!template) {
                    throw new Error('Template not found');
                }
                
                // Record category visit if provided
                if (category) {
                    await user.visitCategoryFromTemplate(category, templateId);
                }
                
                // Set last active template
                await user.setLastActiveTemplate(templateId, 'SHARE');
                
                // Add to engagement log
                if (!user.engagementLog) {
                    user.engagementLog = [];
                }
                
                user.engagementLog.push({
                    action: 'SHARE',
                    templateId,
                    timestamp: Date.now(),
                    metadata: { shareMethod }
                });
                
                // Add to recent templates used
                if (!user.recentTemplatesUsed) {
                    user.recentTemplatesUsed = [];
                }
                
                // Remove template if already in list
                user.recentTemplatesUsed = user.recentTemplatesUsed.filter(
                    id => id.toString() !== templateId.toString()
                );
                
                // Add to beginning of list
                user.recentTemplatesUsed.unshift(templateId);
                
                // Keep only 10 most recent
                if (user.recentTemplatesUsed.length > 10) {
                    user.recentTemplatesUsed = user.recentTemplatesUsed.slice(0, 10);
                }
                
                // Update last online
                user.lastOnline = Date.now();
                
                // Save user first
                await user.save({ session });
                
                // Increment template share count
                await safeUpdateTemplateCounts(templateId, { sharedCount: 1 }, session);
                
                // Invalidate recommendations
                await recommendationService.invalidateUserRecommendations(uid);
                
                logger.info(`User ${uid} shared template ${templateId} via ${shareMethod}`);
                
                return res.status(200).json({
                    success: true,
                    message: 'Template share recorded successfully',
                    data: {
                        templateId,
                        shareMethod,
                        totalShares: template.sharedCount + 1
                    }
                });
            });
        } finally {
            await session.endSession();
        }
    }, 'Record share', res, uid);
});

/**
 * @route   GET /api/users/:uid/templates/:templateId/interaction-status
 * @desc    Get user's interaction status with a specific template
 * @access  Private
 */
router.get('/:uid/templates/:templateId/interaction-status', validateFirebaseUid, verifyFirebaseToken, async (req, res) => {
    return handleAsyncOperation(async () => {
        const { uid, templateId } = req.params;
        
        // Validate request
        const validation = validateTemplateInteraction(uid, templateId);
        if (!validation.isValid) {
            return res.status(400).json({
                success: false,
                message: 'Validation failed',
                errors: validation.errors
            });
        }
        
        // Find user by uid
        const user = await User.findOne({ uid });
        
        if (!user) {
            return res.status(404).json({
                success: false,
                message: 'User not found'
            });
        }
        
        // Get template to include current counts
        const template = await Template.findById(templateId, 'likes favorites sharedCount viewCount');
        
        if (!template) {
            return res.status(404).json({
                success: false,
                message: 'Template not found'
            });
        }
        
        // Check interaction status
        const isLiked = user.likes && user.likes.some(id => id.toString() === templateId.toString());
        const isFavorited = user.favorites && user.favorites.some(id => id.toString() === templateId.toString());
        
        return res.status(200).json({
            success: true,
            data: {
                templateId,
                interactions: {
                    isLiked,
                    isFavorited
                },
                counts: {
                    likes: template.likes || 0,
                    favorites: template.favorites || 0,
                    shares: template.sharedCount || 0,
                    views: template.viewCount || 0
                }
            }
        });
    }, 'Get interaction status', res, uid);
});

/**
 * @route   POST /api/users/:uid/referral
 * @desc    Generate or update referral code for a user
 * @access  Private
 */
router.post('/:uid/referral', validateFirebaseUid, verifyFirebaseToken, async (req, res) => {
    try {
        const { uid } = req.params;
        
        // Find user by uid
        let user = await User.findOne({ uid });
        
        if (!user) {
            logger.warn(`Referral code generation attempted for non-existent user: UID ${uid}`);
            return res.status(404).json({
                success: false,
                message: 'User not found'
            });
        }
        
        // Generate a unique referral code if one doesn't exist
        if (!user.referralCode) {
            // Create a referral code based on UID and timestamp
            const timestamp = new Date().getTime().toString().slice(-6);
            const uidSuffix = uid.slice(-4);
            const referralCode = `EW-${timestamp}${uidSuffix}`.toUpperCase();
            
            user.referralCode = referralCode;
            await user.save();
            
            logger.info(`Generated referral code ${referralCode} for user ${uid}`);
        }
        
        res.status(200).json({
            success: true,
            referralCode: user.referralCode
        });
    } catch (error) {
        logger.error(`Referral code generation error: ${error.message}`);
        res.status(500).json({
            success: false,
            message: 'Server error generating referral code',
            error: error.message
        });
    }
});

/**
 * @route   POST /api/users/:uid/apply-referral
 * @desc    Apply a referral code to a user
 * @access  Private
 */
router.post('/:uid/apply-referral', validateFirebaseUid, verifyFirebaseToken, async (req, res) => {
    try {
        const { uid } = req.params;
        const { referralCode } = req.body;
        
        if (!referralCode) {
            return res.status(400).json({
                success: false,
                message: 'Referral code is required'
            });
        }
        
        // Find user by uid
        let user = await User.findOne({ uid });
        
        if (!user) {
            logger.warn(`Referral application attempted for non-existent user: UID ${uid}`);
            return res.status(404).json({
                success: false,
                message: 'User not found'
            });
        }
        
        // Check if user already has a referral
        if (user.referredBy && user.referredBy.referredBy) {
            return res.status(400).json({
                success: false,
                message: 'User already has a referral applied'
            });
        }
        
        // Find the referring user by referral code
        const referrer = await User.findOne({ referralCode });
        
        if (!referrer) {
            return res.status(404).json({
                success: false,
                message: 'Invalid referral code'
            });
        }
        
        // Prevent self-referral
        if (referrer.uid === uid) {
            return res.status(400).json({
                success: false,
                message: 'Cannot use your own referral code'
            });
        }
        
        // Apply the referral
        user.referredBy = {
            referredBy: referrer.uid,
            referralCode: referralCode
        };
        
        await user.save();
        logger.info(`User ${uid} applied referral code from user ${referrer.uid}`);
        
        res.status(200).json({
            success: true,
            message: 'Referral applied successfully'
        });
    } catch (error) {
        logger.error(`Referral application error: ${error.message}`);
        res.status(500).json({
            success: false,
            message: 'Server error applying referral',
            error: error.message
        });
    }
});

/**
 * @route   GET /api/users/:uid/categories
 * @desc    Get user's category visit history
 * @access  Private
 */
router.get('/:uid/categories', validateFirebaseUid, verifyFirebaseToken, async (req, res) => {
    try {
        const { uid } = req.params;
        
        // Find user by uid
        let user = await User.findOne({ uid });
        
        if (!user) {
            logger.warn(`Category history requested for non-existent user: UID ${uid}`);
            return res.status(404).json({
                success: false,
                message: 'User not found'
            });
        }
        
        // Sort categories by visit count (descending)
        const sortedCategories = [...(user.categories || [])].sort((a, b) => 
            b.visitCount - a.visitCount
        );
        
        res.status(200).json({
            success: true,
            categories: sortedCategories
        });
    } catch (error) {
        logger.error(`Get categories error: ${error.message}`);
        res.status(500).json({
            success: false,
            message: 'Server error retrieving categories',
            error: error.message
        });
    }
});

/**
 * @route   GET /api/users/:uid/analytics/engagement
 * @desc    Get user engagement analytics
 * @access  Private
 */
router.get('/:uid/analytics/engagement', validateFirebaseUid, verifyFirebaseToken, async (req, res) => {
    try {
        const { uid } = req.params;
        
        // Find user by uid
        let user = await User.findOne({ uid });
        
        if (!user) {
            logger.warn(`Analytics requested for non-existent user: UID ${uid}`);
            return res.status(404).json({
                success: false,
                message: 'User not found'
            });
        }
        
        // Group engagement by action type
        const engagementCounts = {
            VIEW: 0,
            LIKE: 0,
            FAV: 0,
            SHARE: 0
        };
        
        if (user.engagementLog && user.engagementLog.length > 0) {
            user.engagementLog.forEach(entry => {
                if (engagementCounts[entry.action] !== undefined) {
                    engagementCounts[entry.action]++;
                }
            });
        }
        
        // Get top categories
        const topCategories = [...(user.categories || [])]
            .sort((a, b) => b.visitCount - a.visitCount)
            .slice(0, 5);
        
        // Calculate total engagement
        const totalEngagement = Object.values(engagementCounts).reduce((sum, count) => sum + count, 0);
        
        res.status(200).json({
            success: true,
            analytics: {
                engagementCounts,
                totalEngagement,
                topCategories,
                lastActive: user.lastOnline,
                created: user.created
            }
        });
    } catch (error) {
        logger.error(`Get analytics error: ${error.message}`);
        res.status(500).json({
            success: false,
            message: 'Server error retrieving analytics',
            error: error.message
        });
    }
});

/**
 * @route   PUT /api/users/:uid/notifications/mute
 * @desc    Mute notifications for a specified duration
 * @access  Private
 */
router.put('/:uid/notifications/mute', validateFirebaseUid, verifyFirebaseToken, async (req, res) => {
    try {
        const { uid } = req.params;
        const { duration } = req.body; // Duration in hours
        
        if (!duration || isNaN(duration) || duration <= 0) {
            return res.status(400).json({
                success: false,
                message: 'Valid duration in hours is required'
            });
        }
        
        // Find user by uid
        let user = await User.findOne({ uid });
        
        if (!user) {
            logger.warn(`Notification mute attempted for non-existent user: UID ${uid}`);
            return res.status(404).json({
                success: false,
                message: 'User not found'
            });
        }
        
        // Calculate mute until time
        const muteUntil = new Date();
        muteUntil.setHours(muteUntil.getHours() + parseInt(duration));
        
        // Update user
        user.muteNotificationsUntil = muteUntil;
        await user.save();
        
        logger.info(`User ${uid} muted notifications until ${muteUntil.toISOString()}`);
        
        res.status(200).json({
            success: true,
            message: 'Notifications muted successfully',
            muteUntil: muteUntil
        });
    } catch (error) {
        logger.error(`Notification mute error: ${error.message}`);
        res.status(500).json({
            success: false,
            message: 'Server error muting notifications',
            error: error.message
        });
    }
});

/**
 * @route   PUT /api/users/:uid/notifications/unmute
 * @desc    Unmute notifications
 * @access  Private
 */
router.put('/:uid/notifications/unmute', validateFirebaseUid, verifyFirebaseToken, async (req, res) => {
    try {
        const { uid } = req.params;
        
        // Find user by uid
        let user = await User.findOne({ uid });
        
        if (!user) {
            logger.warn(`Notification unmute attempted for non-existent user: UID ${uid}`);
            return res.status(404).json({
                success: false,
                message: 'User not found'
            });
        }
        
        // Update user
        user.muteNotificationsUntil = null;
        await user.save();
        
        logger.info(`User ${uid} unmuted notifications`);
        
        res.status(200).json({
            success: true,
            message: 'Notifications unmuted successfully'
        });
    } catch (error) {
        logger.error(`Notification unmute error: ${error.message}`);
        res.status(500).json({
            success: false,
            message: 'Server error unmuting notifications',
            error: error.message
        });
    }
});

/**
 * @route   GET /api/users/:uid/notifications/status
 * @desc    Get notification status for a user
 * @access  Private
 */
router.get('/:uid/notifications/status', validateFirebaseUid, verifyFirebaseToken, async (req, res) => {
    try {
        const { uid } = req.params;
        
        // Find user by uid
        let user = await User.findOne({ uid });
        
        if (!user) {
            logger.warn(`Notification status requested for non-existent user: UID ${uid}`);
            return res.status(404).json({
                success: false,
                message: 'User not found'
            });
        }
        
        // Check if notifications are currently muted
        const now = new Date();
        const isMuted = user.muteNotificationsUntil && user.muteNotificationsUntil > now;
        
        res.status(200).json({
            success: true,
            notificationStatus: {
                isMuted,
                muteUntil: user.muteNotificationsUntil,
                pushPreferences: user.pushPreferences || {
                    allowFestivalPush: true,
                    allowPersonalPush: true
                },
                topicSubscriptions: user.topicSubscriptions || []
            }
        });
    } catch (error) {
        logger.error(`Get notification status error: ${error.message}`);
                res.status(500).json({
            success: false,
            message: 'Server error retrieving notification status',
            error: error.message
        });
    }
});

/**
 * @route   GET /api/users/:uid/sessions
 * @desc    Get all active sessions for a user
 * @access  Private
 */
router.get('/:uid/sessions', validateFirebaseUid, verifyFirebaseToken, async (req, res) => {
    try {
        const { uid } = req.params;
        
        // Find user by uid
        const user = await User.findOne({ uid });
        
        if (!user) {
            return res.status(404).json({
                success: false,
                message: 'User not found'
            });
        }
        
        // Convert Map to array for JSON response
        const sessions = [];
        if (user.activeSessions) {
            for (const [deviceId, sessionData] of user.activeSessions) {
                sessions.push({
                    deviceId,
                    ...sessionData,
                    isCurrentDevice: deviceId === user.deviceId
                });
            }
        }
        
        logger.info(`Retrieved ${sessions.length} active sessions for user ${uid}`);
        
        res.status(200).json({
            success: true,
            message: 'User sessions retrieved successfully',
            sessions: sessions,
            totalSessions: sessions.length,
            currentDeviceId: user.deviceId,
            lastOnline: user.lastOnline
        });
    } catch (error) {
        logger.error(`Get user sessions error: ${error.message}`);
        res.status(500).json({
            success: false,
            message: 'Server error retrieving user sessions',
            error: error.message
        });
    }
});

/**
 * @route   POST /api/users/:uid/sessions/invalidate
 * @desc    Invalidate all other sessions except current one
 * @access  Private
 */
router.post('/:uid/sessions/invalidate', validateFirebaseUid, verifyFirebaseToken, async (req, res) => {
    try {
        const { uid } = req.params;
        const { deviceId } = req.body;
        
        if (!deviceId) {
            return res.status(400).json({
                success: false,
                message: 'Device ID is required'
            });
        }
        
        // Find user by uid
        let user = await User.findOne({ uid });
        
        if (!user) {
            logger.warn(`Session invalidation attempted for non-existent user: UID ${uid}`);
            return res.status(404).json({
                success: false,
                message: 'User not found'
            });
        }
        
        // Invalidate other sessions
        await user.invalidateOtherSessions(deviceId);
        
        logger.info(`User ${uid} invalidated all other sessions except ${deviceId}`);
        
        res.status(200).json({
            success: true,
            message: 'All other sessions invalidated successfully'
        });
    } catch (error) {
        logger.error(`Session invalidation error: ${error.message}`);
        res.status(500).json({
            success: false,
            message: 'Server error invalidating sessions',
            error: error.message
        });
    }
});

/**
 * @route   DELETE /api/users/:uid/sessions/:deviceId
 * @desc    Remove a specific device session
 * @access  Private
 */
router.delete('/:uid/sessions/:deviceId', validateFirebaseUid, verifyFirebaseToken, async (req, res) => {
    try {
        const { uid, deviceId } = req.params;
        
        // Find user by uid
        let user = await User.findOne({ uid });
        
        if (!user) {
            logger.warn(`Session removal attempted for non-existent user: UID ${uid}`);
            return res.status(404).json({
                success: false,
                message: 'User not found'
            });
        }
        
        // Remove the session
        await user.removeDeviceSession(deviceId);
        
        logger.info(`User ${uid} removed session for device ${deviceId}`);
        
        res.status(200).json({
            success: true,
            message: 'Session removed successfully'
        });
    } catch (error) {
        logger.error(`Session removal error: ${error.message}`);
        res.status(500).json({
            success: false,
            message: 'Server error removing session',
            error: error.message
        });
    }
});

/**
 * @route   POST /api/users/:uid/sessions/update
 * @desc    Update activity timestamp for a device session
 * @access  Private
 */
router.post('/:uid/sessions/update', validateFirebaseUid, verifyFirebaseToken, async (req, res) => {
    try {
        const { uid } = req.params;
        const { deviceId } = req.body;
        
        if (!deviceId) {
            return res.status(400).json({
                success: false,
                message: 'Device ID is required'
            });
        }
        
        // Find user by uid
        let user = await User.findOne({ uid });
        
        if (!user) {
            logger.warn(`Session update attempted for non-existent user: UID ${uid}`);
            return res.status(404).json({
                success: false,
                message: 'User not found'
            });
        }
        
        // Update session activity
        await user.updateDeviceSessionActivity(deviceId);
        
        // Also update user's lastOnline
        user.lastOnline = Date.now();
        
        // Clean up invalid subscription data before saving
        cleanupSubscriptionData(user);
        
        await user.save();
        
        res.status(200).json({
            success: true,
            message: 'Session activity updated successfully'
        });
    } catch (error) {
        logger.error(`Session activity update error: ${error.message}`);
        res.status(500).json({
            success: false,
            message: 'Server error updating session activity',
            error: error.message
        });
    }
});

// =============================================================================
// COMPREHENSIVE USER CRUD OPERATIONS BASED ON SCHEMA FIELDS
// =============================================================================

/**
 * @route   GET /api/users/:uid/complete
 * @desc    Get complete user profile with all fields
 * @access  Private
 */
router.get('/:uid/complete', verifyFirebaseToken, async (req, res) => {
    try {
        const { uid } = req.params;
        
        const user = await User.findOne({ uid })
            .populate('likes', 'name imageUrl category')
            .populate('favorites', 'name imageUrl category')
            .populate('recentTemplatesUsed', 'name imageUrl category')
            .populate('lastActiveTemplate', 'name imageUrl category');
        
        if (!user) {
            return res.status(404).json({
                success: false,
                message: 'User not found'
            });
        }
        
        res.status(200).json({
            success: true,
            data: user
        });
    } catch (error) {
        logger.error(`Get complete user error: ${error.message}`);
        res.status(500).json({
            success: false,
            message: 'Server error',
            error: error.message
        });
    }
});

// =============================================================================
// DEVICE & SESSION MANAGEMENT
// =============================================================================

/**
 * @route   POST /api/users/:uid/device-sessions
 * @desc    Add or update device session
 * @access  Private
 */
router.post('/:uid/device-sessions', verifyFirebaseToken, async (req, res) => {
    try {
        const { uid } = req.params;
        const { deviceId, deviceModel, deviceName, appVersion, osVersion } = req.body;
        
        if (!deviceId) {
            return res.status(400).json({
                success: false,
                message: 'Device ID is required'
            });
        }
        
        const user = await User.findOne({ uid });
        if (!user) {
            return res.status(404).json({
                success: false,
                message: 'User not found'
            });
        }
        
        await user.addDeviceSession({
            deviceId,
            deviceModel: deviceModel || 'Unknown',
            deviceName: deviceName || 'Unknown Device',
            appVersion: appVersion || 'Unknown',
            osVersion: osVersion || 'Unknown'
        });
        
        res.status(200).json({
            success: true,
            message: 'Device session added successfully',
            activeSessions: user.activeSessions
        });
    } catch (error) {
        logger.error(`Add device session error: ${error.message}`);
        res.status(500).json({
            success: false,
            message: 'Server error',
            error: error.message
        });
    }
});

/**
 * @route   DELETE /api/users/:uid/device-sessions/:deviceId
 * @desc    Remove device session
 * @access  Private
 */
router.delete('/:uid/device-sessions/:deviceId', verifyFirebaseToken, async (req, res) => {
    try {
        const { uid, deviceId } = req.params;
        
        const user = await User.findOne({ uid });
        if (!user) {
            return res.status(404).json({
                success: false,
                message: 'User not found'
            });
        }
        
        await user.removeDeviceSession(deviceId);
        
        res.status(200).json({
            success: true,
            message: 'Device session removed successfully'
        });
    } catch (error) {
        logger.error(`Remove device session error: ${error.message}`);
        res.status(500).json({
            success: false,
            message: 'Server error',
            error: error.message
        });
    }
});

/**
 * @route   PUT /api/users/:uid/device-sessions/:deviceId/activity
 * @desc    Update device session activity
 * @access  Private
 */
router.put('/:uid/device-sessions/:deviceId/activity', verifyFirebaseToken, async (req, res) => {
    try {
        const { uid, deviceId } = req.params;
        
        const user = await User.findOne({ uid });
        if (!user) {
            return res.status(404).json({
                success: false,
                message: 'User not found'
            });
        }
        
        await user.updateDeviceSessionActivity(deviceId);
        
        res.status(200).json({
            success: true,
            message: 'Device session activity updated'
        });
    } catch (error) {
        logger.error(`Update device session activity error: ${error.message}`);
        res.status(500).json({
            success: false,
            message: 'Server error',
            error: error.message
        });
    }
});

// =============================================================================
// SUBSCRIPTION MANAGEMENT
// =============================================================================

/**
 * @route   PUT /api/users/:uid/subscription
 * @desc    Update user subscription details
 * @access  Private
 */
router.put('/:uid/subscription', verifyFirebaseToken, async (req, res) => {
    try {
        const { uid } = req.params;
        const subscriptionData = req.body;
        
        const user = await User.findOne({ uid });
        if (!user) {
            return res.status(404).json({
                success: false,
                message: 'User not found'
            });
        }
        
        // Update subscription fields
        if (!user.subscription) {
            user.subscription = {};
        }
        
        Object.assign(user.subscription, subscriptionData);
        await user.save();
        
        res.status(200).json({
            success: true,
            message: 'Subscription updated successfully',
            subscription: user.subscription
        });
    } catch (error) {
        logger.error(`Update subscription error: ${error.message}`);
        res.status(500).json({
            success: false,
            message: 'Server error',
            error: error.message
        });
    }
});

/**
 * @route   POST /api/users/:uid/subscription-history
 * @desc    Add subscription history entry
 * @access  Private
 */
router.post('/:uid/subscription-history', verifyFirebaseToken, async (req, res) => {
    try {
        const { uid } = req.params;
        const { plan, startedAt, endedAt } = req.body;
        
        if (!plan || !startedAt || !endedAt) {
            return res.status(400).json({
                success: false,
                message: 'Plan, startedAt, and endedAt are required'
            });
        }
        
        const user = await User.findOne({ uid });
        if (!user) {
            return res.status(404).json({
                success: false,
                message: 'User not found'
            });
        }
        
        if (!user.subscriptionHistory) {
            user.subscriptionHistory = [];
        }
        
        user.subscriptionHistory.push({
            plan,
            startedAt: new Date(startedAt),
            endedAt: new Date(endedAt)
        });
        
        await user.save();
        
        res.status(200).json({
            success: true,
            message: 'Subscription history added successfully',
            subscriptionHistory: user.subscriptionHistory
        });
    } catch (error) {
        logger.error(`Add subscription history error: ${error.message}`);
        res.status(500).json({
            success: false,
            message: 'Server error',
            error: error.message
        });
    }
});

/**
 * @route   PUT /api/users/:uid/subscription-offer
 * @desc    Update user subscription offer
 * @access  Private
 */
router.put('/:uid/subscription-offer', verifyFirebaseToken, async (req, res) => {
    try {
        const { uid } = req.params;
        const offerData = req.body;
        
        const user = await User.findOne({ uid });
        if (!user) {
            return res.status(404).json({
                success: false,
                message: 'User not found'
            });
        }
        
        user.subscriptionOffer = offerData;
        await user.save();
        
        res.status(200).json({
            success: true,
            message: 'Subscription offer updated successfully',
            subscriptionOffer: user.subscriptionOffer
        });
    } catch (error) {
        logger.error(`Update subscription offer error: ${error.message}`);
        res.status(500).json({
            success: false,
            message: 'Server error',
            error: error.message
        });
    }
});

// =============================================================================
// AI USAGE MANAGEMENT
// =============================================================================

/**
 * @route   PUT /api/users/:uid/ai-usage
 * @desc    Update AI usage data
 * @access  Private
 */
router.put('/:uid/ai-usage', verifyFirebaseToken, async (req, res) => {
    try {
        const { uid } = req.params;
        const { monthlyGenerationCount, quota, recentPrompts, stylePreferences } = req.body;
        
        const user = await User.findOne({ uid });
        if (!user) {
            return res.status(404).json({
                success: false,
                message: 'User not found'
            });
        }
        
        if (!user.aiUsage) {
            user.aiUsage = {};
        }
        
        if (monthlyGenerationCount !== undefined) {
            user.aiUsage.monthlyGenerationCount = monthlyGenerationCount;
            user.aiUsage.lastGenerationAt = new Date();
        }
        
        if (quota !== undefined) user.aiUsage.quota = quota;
        if (recentPrompts) user.aiUsage.recentPrompts = recentPrompts;
        if (stylePreferences) user.aiUsage.stylePreferences = stylePreferences;
        
        await user.save();
        
        res.status(200).json({
            success: true,
            message: 'AI usage updated successfully',
            aiUsage: user.aiUsage
        });
    } catch (error) {
        logger.error(`Update AI usage error: ${error.message}`);
        res.status(500).json({
            success: false,
            message: 'Server error',
            error: error.message
        });
    }
});

/**
 * @route   POST /api/users/:uid/ai-usage/increment
 * @desc    Increment AI generation count
 * @access  Private
 */
router.post('/:uid/ai-usage/increment', verifyFirebaseToken, async (req, res) => {
    try {
        const { uid } = req.params;
        const { prompt, style } = req.body;
        
        const user = await User.findOne({ uid });
        if (!user) {
            return res.status(404).json({
                success: false,
                message: 'User not found'
            });
        }
        
        if (!user.aiUsage) {
            user.aiUsage = {
                monthlyGenerationCount: 0,
                quota: 5,
                recentPrompts: [],
                stylePreferences: []
            };
        }
        
        // Check quota
        if (user.aiUsage.monthlyGenerationCount >= user.aiUsage.quota) {
            return res.status(403).json({
                success: false,
                message: 'AI generation quota exceeded',
                quota: user.aiUsage.quota,
                used: user.aiUsage.monthlyGenerationCount
            });
        }
        
        // Increment count
        user.aiUsage.monthlyGenerationCount += 1;
        user.aiUsage.lastGenerationAt = new Date();
        
        // Add prompt to recent prompts (keep last 10)
        if (prompt) {
            if (!user.aiUsage.recentPrompts) user.aiUsage.recentPrompts = [];
            user.aiUsage.recentPrompts.unshift(prompt);
            if (user.aiUsage.recentPrompts.length > 10) {
                user.aiUsage.recentPrompts = user.aiUsage.recentPrompts.slice(0, 10);
            }
        }
        
        // Track style preference
        if (style) {
            if (!user.aiUsage.stylePreferences) user.aiUsage.stylePreferences = [];
            if (!user.aiUsage.stylePreferences.includes(style)) {
                user.aiUsage.stylePreferences.push(style);
            }
        }
        
        await user.save();
        
        res.status(200).json({
            success: true,
            message: 'AI generation count incremented',
            aiUsage: user.aiUsage,
            remaining: user.aiUsage.quota - user.aiUsage.monthlyGenerationCount
        });
    } catch (error) {
        logger.error(`Increment AI usage error: ${error.message}`);
        res.status(500).json({
            success: false,
            message: 'Server error',
            error: error.message
        });
    }
});

// =============================================================================
// CATEGORY VISITS MANAGEMENT
// =============================================================================

/**
 * @route   POST /api/users/:uid/categories/visit
 * @desc    Record category visit
 * @access  Private
 */
router.post('/:uid/categories/visit', verifyFirebaseToken, async (req, res) => {
    try {
        const { uid } = req.params;
        const { category, source = 'direct' } = req.body;
        
        if (!category) {
            return res.status(400).json({
                success: false,
                message: 'Category is required'
            });
        }
        
        const user = await User.findOne({ uid });
        if (!user) {
            return res.status(404).json({
                success: false,
                message: 'User not found'
            });
        }
        
        await user.visitCategory(category, source);
        
        res.status(200).json({
            success: true,
            message: 'Category visit recorded',
            categories: user.categories
        });
    } catch (error) {
        logger.error(`Record category visit error: ${error.message}`);
        res.status(500).json({
            success: false,
            message: 'Server error',
            error: error.message
        });
    }
});

/**
 * @route   GET /api/users/:uid/categories/stats
 * @desc    Get category visit statistics
 * @access  Private
 */
router.get('/:uid/categories/stats', verifyFirebaseToken, async (req, res) => {
    try {
        const { uid } = req.params;
        
        const user = await User.findOne({ uid });
        if (!user) {
            return res.status(404).json({
                success: false,
                message: 'User not found'
            });
        }
        
        // Calculate category statistics
        const categoryStats = user.categories.map(cat => ({
            category: cat.category,
            visitCount: cat.visitCount,
            lastVisit: cat.visitDate,
            source: cat.source
        })).sort((a, b) => b.visitCount - a.visitCount);
        
        const totalVisits = categoryStats.reduce((sum, cat) => sum + cat.visitCount, 0);
        const mostVisited = categoryStats[0] || null;
        const recentVisits = categoryStats
            .sort((a, b) => new Date(b.lastVisit) - new Date(a.lastVisit))
            .slice(0, 5);
        
        res.status(200).json({
            success: true,
            data: {
                categories: categoryStats,
                summary: {
                    totalCategories: categoryStats.length,
                    totalVisits,
                    mostVisited,
                    recentVisits
                }
            }
        });
    } catch (error) {
        logger.error(`Get category stats error: ${error.message}`);
        res.status(500).json({
            success: false,
            message: 'Server error',
            error: error.message
        });
    }
});

// =============================================================================
// TEMPLATE AFFINITY MANAGEMENT
// =============================================================================

/**
 * @route   PUT /api/users/:uid/template-affinity
 * @desc    Update template affinity scores
 * @access  Private
 */
router.put('/:uid/template-affinity', verifyFirebaseToken, async (req, res) => {
    try {
        const { uid } = req.params;
        const { tag, score = 1 } = req.body;
        
        if (!tag) {
            return res.status(400).json({
                success: false,
                message: 'Tag is required'
            });
        }
        
        const user = await User.findOne({ uid });
        if (!user) {
            return res.status(404).json({
                success: false,
                message: 'User not found'
            });
        }
        
        await user.updateTemplateAffinity(tag, score);
        
        res.status(200).json({
            success: true,
            message: 'Template affinity updated',
            templateAffinity: user.templateAffinity
        });
    } catch (error) {
        logger.error(`Update template affinity error: ${error.message}`);
        res.status(500).json({
            success: false,
            message: 'Server error',
            error: error.message
        });
    }
});

/**
 * @route   GET /api/users/:uid/template-affinity/top
 * @desc    Get top template affinities
 * @access  Private
 */
router.get('/:uid/template-affinity/top', verifyFirebaseToken, async (req, res) => {
    try {
        const { uid } = req.params;
        const { limit = 10 } = req.query;
        
        const user = await User.findOne({ uid });
        if (!user) {
            return res.status(404).json({
                success: false,
                message: 'User not found'
            });
        }
        
        const topAffinities = user.templateAffinity
            .sort((a, b) => b.score - a.score)
            .slice(0, parseInt(limit));
        
        res.status(200).json({
            success: true,
            data: topAffinities
        });
    } catch (error) {
        logger.error(`Get top template affinity error: ${error.message}`);
        res.status(500).json({
            success: false,
            message: 'Server error',
            error: error.message
        });
    }
});

// =============================================================================
// IGNORED TEMPLATES MANAGEMENT
// =============================================================================

/**
 * @route   POST /api/users/:uid/ignored-templates
 * @desc    Add template to ignored list
 * @access  Private
 */
router.post('/:uid/ignored-templates', verifyFirebaseToken, async (req, res) => {
    try {
        const { uid } = req.params;
        const { templateId, score = 1 } = req.body;
        
        if (!templateId) {
            return res.status(400).json({
                success: false,
                message: 'Template ID is required'
            });
        }
        
        const user = await User.findOne({ uid });
        if (!user) {
            return res.status(404).json({
                success: false,
                message: 'User not found'
            });
        }
        
        await user.ignoreTemplate(templateId, score);
        
        res.status(200).json({
            success: true,
            message: 'Template added to ignored list',
            ignoredTemplates: user.ignoredTemplates
        });
    } catch (error) {
        logger.error(`Add ignored template error: ${error.message}`);
        res.status(500).json({
            success: false,
            message: 'Server error',
            error: error.message
        });
    }
});

/**
 * @route   DELETE /api/users/:uid/ignored-templates/:templateId
 * @desc    Remove template from ignored list
 * @access  Private
 */
router.delete('/:uid/ignored-templates/:templateId', verifyFirebaseToken, async (req, res) => {
    try {
        const { uid, templateId } = req.params;
        
        const user = await User.findOne({ uid });
        if (!user) {
            return res.status(404).json({
                success: false,
                message: 'User not found'
            });
        }
        
        user.ignoredTemplates = user.ignoredTemplates.filter(
            ignored => ignored.templateId.toString() !== templateId
        );
        
        await user.save();
        
        res.status(200).json({
            success: true,
            message: 'Template removed from ignored list'
        });
    } catch (error) {
        logger.error(`Remove ignored template error: ${error.message}`);
        res.status(500).json({
            success: false,
            message: 'Server error',
            error: error.message
        });
    }
});

// =============================================================================
// DRAFTS MANAGEMENT
// =============================================================================

/**
 * @route   POST /api/users/:uid/drafts
 * @desc    Save or update draft
 * @access  Private
 */
router.post('/:uid/drafts', verifyFirebaseToken, async (req, res) => {
    try {
        const { uid } = req.params;
        const { templateId, html } = req.body;
        
        if (!templateId || !html) {
            return res.status(400).json({
                success: false,
                message: 'Template ID and HTML content are required'
            });
        }
        
        const user = await User.findOne({ uid });
        if (!user) {
            return res.status(404).json({
                success: false,
                message: 'User not found'
            });
        }
        
        await user.saveDraft(templateId, html);
        
        res.status(200).json({
            success: true,
            message: 'Draft saved successfully',
            drafts: user.drafts
        });
    } catch (error) {
        logger.error(`Save draft error: ${error.message}`);
        res.status(500).json({
            success: false,
            message: 'Server error',
            error: error.message
        });
    }
});

/**
 * @route   GET /api/users/:uid/drafts
 * @desc    Get user's drafts
 * @access  Private
 */
router.get('/:uid/drafts', verifyFirebaseToken, async (req, res) => {
    try {
        const { uid } = req.params;
        
        const user = await User.findOne({ uid }).populate('drafts.templateId', 'name imageUrl category');
        if (!user) {
            return res.status(404).json({
                success: false,
                message: 'User not found'
            });
        }
        
        res.status(200).json({
            success: true,
            data: user.drafts
        });
    } catch (error) {
        logger.error(`Get drafts error: ${error.message}`);
        res.status(500).json({
            success: false,
            message: 'Server error',
            error: error.message
        });
    }
});

/**
 * @route   DELETE /api/users/:uid/drafts/:templateId
 * @desc    Delete specific draft
 * @access  Private
 */
router.delete('/:uid/drafts/:templateId', verifyFirebaseToken, async (req, res) => {
    try {
        const { uid, templateId } = req.params;
        
        const user = await User.findOne({ uid });
        if (!user) {
            return res.status(404).json({
                success: false,
                message: 'User not found'
            });
        }
        
        await user.deleteDraft(templateId);
        
        res.status(200).json({
            success: true,
            message: 'Draft deleted successfully'
        });
    } catch (error) {
        logger.error(`Delete draft error: ${error.message}`);
        res.status(500).json({
            success: false,
            message: 'Server error',
            error: error.message
        });
    }
});

// =============================================================================
// FCM TOKENS MANAGEMENT
// =============================================================================

/**
 * @route   POST /api/users/:uid/fcm-tokens
 * @desc    Add or update FCM token
 * @access  Private
 */
router.post('/:uid/fcm-tokens', verifyFirebaseToken, async (req, res) => {
    try {
        const { uid } = req.params;
        const { token, platform = 'android' } = req.body;
        
        if (!token) {
            return res.status(400).json({
                success: false,
                message: 'FCM token is required'
            });
        }
        
        const user = await User.findOne({ uid });
        if (!user) {
            return res.status(404).json({
                success: false,
                message: 'User not found'
            });
        }
        
        await user.addFcmToken(token, platform);
        
        res.status(200).json({
            success: true,
            message: 'FCM token added successfully',
            fcmTokens: user.fcmTokens
        });
    } catch (error) {
        logger.error(`Add FCM token error: ${error.message}`);
        res.status(500).json({
            success: false,
            message: 'Server error',
            error: error.message
        });
    }
});

/**
 * @route   DELETE /api/users/:uid/fcm-tokens/:token
 * @desc    Remove FCM token
 * @access  Private
 */
router.delete('/:uid/fcm-tokens/:token', verifyFirebaseToken, async (req, res) => {
    try {
        const { uid, token } = req.params;
        
        const user = await User.findOne({ uid });
        if (!user) {
            return res.status(404).json({
                success: false,
                message: 'User not found'
            });
        }
        
        await user.removeFcmToken(token);
        
        res.status(200).json({
            success: true,
            message: 'FCM token removed successfully'
        });
    } catch (error) {
        logger.error(`Remove FCM token error: ${error.message}`);
        res.status(500).json({
            success: false,
            message: 'Server error',
            error: error.message
        });
    }
});

/**
 * @route   POST /api/users/:uid/fcm-tokens/:token/topics/subscribe
 * @desc    Subscribe FCM token to topics
 * @access  Private
 */
router.post('/:uid/fcm-tokens/:token/topics/subscribe', verifyFirebaseToken, async (req, res) => {
    try {
        const { uid, token } = req.params;
        const { topics } = req.body;
        
        if (!topics || !Array.isArray(topics)) {
            return res.status(400).json({
                success: false,
                message: 'Topics array is required'
            });
        }
        
        const user = await User.findOne({ uid });
        if (!user) {
            return res.status(404).json({
                success: false,
                message: 'User not found'
            });
        }
        
        // Subscribe to each topic
        for (const topic of topics) {
            await user.subscribeTokenToTopic(token, topic);
        }
        
        res.status(200).json({
            success: true,
            message: 'Subscribed to topics successfully',
            fcmTokens: user.fcmTokens
        });
    } catch (error) {
        logger.error(`Subscribe to topics error: ${error.message}`);
        res.status(500).json({
            success: false,
            message: 'Server error',
            error: error.message
        });
    }
});

// =============================================================================
// CACHED HOME FEED MANAGEMENT
// =============================================================================

/**
 * @route   PUT /api/users/:uid/cached-feed
 * @desc    Update cached home feed
 * @access  Private
 */
router.put('/:uid/cached-feed', verifyFirebaseToken, async (req, res) => {
    try {
        const { uid } = req.params;
        const { feedData } = req.body;
        
        if (!feedData || !Array.isArray(feedData)) {
            return res.status(400).json({
                success: false,
                message: 'Feed data array is required'
            });
        }
        
        const user = await User.findOne({ uid });
        if (!user) {
            return res.status(404).json({
                success: false,
                message: 'User not found'
            });
        }
        
        user.cachedHomeFeed = feedData;
        user.homeFeedLastGeneratedAt = new Date();
        await user.save();
        
        res.status(200).json({
            success: true,
            message: 'Cached feed updated successfully',
            cachedHomeFeed: user.cachedHomeFeed,
            lastGenerated: user.homeFeedLastGeneratedAt
        });
    } catch (error) {
        logger.error(`Update cached feed error: ${error.message}`);
        res.status(500).json({
            success: false,
            message: 'Server error',
            error: error.message
        });
    }
});

/**
 * @route   GET /api/users/:uid/cached-feed
 * @desc    Get cached home feed
 * @access  Private
 */
router.get('/:uid/cached-feed', verifyFirebaseToken, async (req, res) => {
    try {
        const { uid } = req.params;
        
        const user = await User.findOne({ uid });
        if (!user) {
            return res.status(404).json({
                success: false,
                message: 'User not found'
            });
        }
        
        res.status(200).json({
            success: true,
            data: {
                cachedHomeFeed: user.cachedHomeFeed || [],
                lastGenerated: user.homeFeedLastGeneratedAt,
                isStale: user.homeFeedLastGeneratedAt && 
                        (Date.now() - user.homeFeedLastGeneratedAt.getTime()) > (24 * 60 * 60 * 1000) // 24 hours
            }
        });
    } catch (error) {
        logger.error(`Get cached feed error: ${error.message}`);
        res.status(500).json({
            success: false,
            message: 'Server error',
            error: error.message
        });
    }
});

// =============================================================================
// BLOCK MANAGEMENT (Admin Operations)
// =============================================================================

/**
 * @route   PUT /api/users/:uid/block
 * @desc    Block a user (Admin only)
 * @access  Private (Admin)
 */
router.put('/:uid/block', verifyFirebaseToken, async (req, res) => {
    try {
        const { uid } = req.params;
        const { reason, expiresAt, notes } = req.body;
        const adminUid = req.user.uid; // From Firebase token
        
        // TODO: Add admin verification middleware
        
        const user = await User.findOne({ uid });
        if (!user) {
            return res.status(404).json({
                success: false,
                message: 'User not found'
            });
        }
        
        await user.blockUser(
            adminUid,
            reason || 'Blocked by administrator',
            expiresAt ? new Date(expiresAt) : null,
            notes || ''
        );
        
        res.status(200).json({
            success: true,
            message: 'User blocked successfully',
            blockInfo: user.blockInfo
        });
    } catch (error) {
        logger.error(`Block user error: ${error.message}`);
        res.status(500).json({
            success: false,
            message: 'Server error',
            error: error.message
        });
    }
});

/**
 * @route   PUT /api/users/:uid/unblock
 * @desc    Unblock a user (Admin only)
 * @access  Private (Admin)
 */
router.put('/:uid/unblock', verifyFirebaseToken, async (req, res) => {
    try {
        const { uid } = req.params;
        
        // TODO: Add admin verification middleware
        
        const user = await User.findOne({ uid });
        if (!user) {
            return res.status(404).json({
                success: false,
                message: 'User not found'
            });
        }
        
        await user.unblockUser();
        
        res.status(200).json({
            success: true,
            message: 'User unblocked successfully'
        });
    } catch (error) {
        logger.error(`Unblock user error: ${error.message}`);
        res.status(500).json({
            success: false,
            message: 'Server error',
            error: error.message
        });
    }
});

// =============================================================================
// BULK OPERATIONS
// =============================================================================

/**
 * @route   POST /api/users/:uid/bulk-update
 * @desc    Bulk update multiple user fields
 * @access  Private
 */
router.post('/:uid/bulk-update', verifyFirebaseToken, async (req, res) => {
    try {
        const { uid } = req.params;
        const updateData = req.body;
        
        const user = await User.findOne({ uid });
        if (!user) {
            return res.status(404).json({
                success: false,
                message: 'User not found'
            });
        }
        
        // Update allowed fields
        const allowedFields = [
            'displayName', 'email', 'profilePhoto', 'preferredTheme', 
            'preferredLanguage', 'timezone', 'pushPreferences', 
            'topicSubscriptions', 'muteNotificationsUntil'
        ];
        
        for (const field of allowedFields) {
            if (updateData[field] !== undefined) {
                user[field] = updateData[field];
            }
        }
        
        await user.save();
        
        res.status(200).json({
            success: true,
            message: 'User updated successfully',
            user: {
                uid: user.uid,
                displayName: user.displayName,
                email: user.email,
                profilePhoto: user.profilePhoto,
                preferredTheme: user.preferredTheme,
                preferredLanguage: user.preferredLanguage,
                timezone: user.timezone,
                pushPreferences: user.pushPreferences,
                topicSubscriptions: user.topicSubscriptions,
                muteNotificationsUntil: user.muteNotificationsUntil
            }
        });
    } catch (error) {
        logger.error(`Bulk update user error: ${error.message}`);
        res.status(500).json({
            success: false,
            message: 'Server error',
            error: error.message
        });
    }
});

/**
 * @route   DELETE /api/users/:uid
 * @desc    Delete user account (GDPR compliance)
 * @access  Private
 */
router.delete('/:uid', verifyFirebaseToken, async (req, res) => {
    try {
        const { uid } = req.params;
        
        const user = await User.findOne({ uid });
        if (!user) {
            return res.status(404).json({
                success: false,
                message: 'User not found'
            });
        }
        
        // Remove user from all templates' likes and favorites
        await Template.updateMany(
            { $or: [{ likes: uid }, { favorites: uid }] },
            { $pull: { likes: uid, favorites: uid } }
        );
        
        // Delete user document
        await User.findOneAndDelete({ uid });
        
        logger.info(`User account deleted: ${uid}`);
        
        res.status(200).json({
            success: true,
            message: 'User account deleted successfully'
        });
    } catch (error) {
        logger.error(`Delete user error: ${error.message}`);
        res.status(500).json({
            success: false,
            message: 'Server error',
            error: error.message
        });
    }
});

module.exports = router; 