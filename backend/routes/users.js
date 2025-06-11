const express = require('express');
const router = express.Router();
const User = require('../models/User');
const logger = require('../utils/logger');
const { validateDeviceId } = require('../middleware/validators');
const recommendationService = require('../services/recommendationService');

/**
 * @route   POST /api/users/register
 * @desc    Register a new user with device ID
 * @access  Public
 */
router.post('/register', validateDeviceId, async (req, res) => {
    try {
        const { deviceId, uid, displayName, email, profilePhoto } = req.body;
        
        // Check if user already exists
        let user = await User.findOne({ deviceId });
        
        if (user) {
            // User already exists, update lastOnline and possibly link with Firebase UID
            user.lastOnline = Date.now();
            
            // If uid is provided and user doesn't have one yet, link the accounts
            if (uid && !user.uid) {
                user.uid = uid;
            }
            
            // Update profile info if provided
            if (displayName) user.displayName = displayName;
            if (email) user.email = email;
            if (profilePhoto) user.profilePhoto = profilePhoto;
            
            await user.save();
            
            logger.info(`Existing user logged in: ${deviceId}, UID: ${uid || 'anonymous'}`);
            return res.status(200).json({
                success: true,
                message: 'User already exists',
                user
            });
        }
        
        // Create new user
        user = new User({
            deviceId,
            uid: uid || null,
            displayName: displayName || null,
            email: email || null,
            profilePhoto: profilePhoto || null,
            lastOnline: Date.now(),
            created: Date.now(),
            categories: []
        });
        
        await user.save();
        logger.info(`New user registered: ${deviceId}, UID: ${uid || 'anonymous'}`);
        
        res.status(201).json({
            success: true,
            message: 'User registered successfully',
            user
        });
    } catch (error) {
        logger.error(`User registration error: ${error.message}`);
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
 * @access  Public
 */
router.put('/activity', validateDeviceId, async (req, res) => {
    try {
        const { deviceId, category, source = 'direct' } = req.body;
        
        // Find user by deviceId
        let user = await User.findOne({ deviceId });
        
        if (!user) {
            logger.warn(`Activity update attempted for non-existent user: ${deviceId}`);
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
            logger.info(`User ${deviceId} visited category: ${category} (source: ${source})`);
            
            // Invalidate recommendations cache on category visit
            await recommendationService.invalidateUserRecommendations(deviceId);
        } else {
            await user.save();
            logger.info(`User ${deviceId} activity updated (last online)`);
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
 * @access  Public
 */
router.put('/template-view', validateDeviceId, async (req, res) => {
    try {
        const { deviceId, templateId, category } = req.body;
        
        if (!templateId || !category) {
            return res.status(400).json({
                success: false,
                message: 'Template ID and category are required'
            });
        }
        
        // Find user by deviceId
        let user = await User.findOne({ deviceId });
        
        if (!user) {
            logger.warn(`Template view attempted for non-existent user: ${deviceId}`);
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
        
        logger.info(`User ${deviceId} viewed template ${templateId} in category: ${category}`);
        
        // Invalidate recommendations cache on template view
        await recommendationService.invalidateUserRecommendations(deviceId);
        
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
 * @route   GET /api/users/:identifier
 * @desc    Get user data by device ID or Firebase UID
 * @access  Public (should be restricted in production)
 */
router.get('/:identifier', async (req, res) => {
    try {
        const identifier = req.params.identifier;
        
        // Try to find user by uid first, then by deviceId
        let user = await User.findOne({ uid: identifier });
        
        // If not found by uid, try deviceId
        if (!user) {
            user = await User.findOne({ deviceId: identifier });
        }
        
        if (!user) {
            return res.status(404).json({
                success: false,
                message: 'User not found'
            });
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
 * @route   GET /api/users/:identifier/recommendations
 * @desc    Get personalized template recommendations for a user
 * @access  Public
 */
router.get('/:identifier/recommendations', async (req, res) => {
    try {
        const identifier = req.params.identifier;
        const limit = parseInt(req.query.limit) || 10;
        
        if (!identifier) {
            return res.status(400).json({
                success: false,
                message: 'User identifier is required'
            });
        }
        
        // Try to find user by uid first, then by deviceId
        let user = await User.findOne({ uid: identifier });
        
        // If not found by uid, try deviceId
        if (!user) {
            user = await User.findOne({ deviceId: identifier });
        }
        
        if (!user) {
            return res.status(404).json({
                success: false,
                message: 'User not found'
            });
        }
        
        // Use deviceId for recommendation service (for backward compatibility)
        const deviceId = user.deviceId;
        
        // Get recommendations using the recommendation service
        const recommendations = await recommendationService.getRecommendationsForUser(deviceId, limit);
        
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
 * @access  Public
 */
router.post('/engagement', validateDeviceId, async (req, res) => {
    try {
        const { deviceId, type, templateId, category, timestamp, durationMs, engagementScore, source } = req.body;
        
        // Validate required fields
        if (!deviceId || !type) {
            return res.status(400).json({
                success: false,
                message: 'Device ID and engagement type are required'
            });
        }
        
        // Find user by deviceId
        let user = await User.findOne({ deviceId });
        
        if (!user) {
            logger.warn(`Engagement tracking attempted for non-existent user: ${deviceId}`);
            return res.status(404).json({
                success: false,
                message: 'User not found'
            });
        }
        
        // Record engagement based on type
        switch (type) {
            case 1: // Category visit
                if (category) {
                    await user.visitCategory(category, source || 'direct');
                    logger.info(`User ${deviceId} engagement: visited category ${category}`);
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
                    
                    logger.info(`User ${deviceId} engagement: viewed template ${templateId} in ${category}`);
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
                    
                    logger.info(`User ${deviceId} engagement: used template ${templateId} in ${category}`);
                }
                break;
                
            case 4: // Explicit like
                if (templateId && category) {
                    // Record as like
                    await user.visitCategoryFromTemplate(category, templateId);
                    await user.setLastActiveTemplate(templateId, 'LIKE');
                    
                    // Add to likes if not already there
                    if (!user.likes) user.likes = [];
                    if (!user.likes.includes(templateId)) {
                        user.likes.push(templateId);
                    }
                    
                    // Add to engagement log
                    if (!user.engagementLog) user.engagementLog = [];
                    user.engagementLog.push({
                        action: 'LIKE',
                        templateId,
                        timestamp: timestamp || Date.now()
                    });
                    
                    logger.info(`User ${deviceId} engagement: liked template ${templateId}`);
                }
                break;
                
            case 5: // Add to favorites
                if (templateId && category) {
                    // Record as favorite
                    await user.visitCategoryFromTemplate(category, templateId);
                    await user.setLastActiveTemplate(templateId, 'FAV');
                    
                    // Add to favorites if not already there
                    if (!user.favorites) user.favorites = [];
                    if (!user.favorites.includes(templateId)) {
                        user.favorites.push(templateId);
                    }
                    
                    // Add to engagement log
                    if (!user.engagementLog) user.engagementLog = [];
                    user.engagementLog.push({
                        action: 'FAV',
                        templateId,
                        timestamp: timestamp || Date.now()
                    });
                    
                    logger.info(`User ${deviceId} engagement: favorited template ${templateId}`);
                }
                break;
                
            default:
                logger.warn(`Unknown engagement type ${type} from user ${deviceId}`);
        }
        
        // Update last online time
        user.lastOnline = Date.now();
        await user.save();
        
        // Invalidate recommendations cache
        await recommendationService.invalidateUserRecommendations(deviceId);
        
        res.status(200).json({
            success: true,
            message: 'Engagement recorded successfully'
        });
        
    } catch (error) {
        logger.error(`Engagement tracking error: ${error.message}`, { error });
        res.status(500).json({
            success: false,
            message: 'Server error recording engagement',
            error: error.message
        });
    }
});

/**
 * @route   POST /api/users/engagement/sync
 * @desc    Sync multiple engagement records in batch
 * @access  Public
 */
router.post('/engagement/sync', validateDeviceId, async (req, res) => {
    try {
        const { deviceId, engagements } = req.body;
        
        if (!deviceId || !engagements || !Array.isArray(engagements)) {
            return res.status(400).json({
                success: false,
                message: 'Device ID and engagement array are required'
            });
        }
        
        // Find user by deviceId
        let user = await User.findOne({ deviceId });
        
        if (!user) {
            logger.warn(`Batch engagement sync attempted for non-existent user: ${deviceId}`);
            return res.status(404).json({
                success: false,
                message: 'User not found'
            });
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
                    
                    // For template use, add to recent templates
                    if (type === 3) {
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
                    if (!user.likes.some(id => id.toString() === templateId.toString())) {
                        user.likes.push(templateId);
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
                    if (!user.favorites.some(id => id.toString() === templateId.toString())) {
                        user.favorites.push(templateId);
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
        await user.save();
        
        // Invalidate recommendations cache
        await recommendationService.invalidateUserRecommendations(deviceId);
        
        res.status(200).json({
            success: true,
            message: `Processed ${processed} of ${engagements.length} engagement records`
        });
        
    } catch (error) {
        logger.error(`Batch engagement sync error: ${error.message}`, { error });
        res.status(500).json({
            success: false,
            message: 'Server error during batch engagement sync',
            error: error.message
        });
    }
});

/**
 * @route   PUT /api/users/preferences
 * @desc    Update user preferences
 * @access  Public
 */
router.put('/preferences', validateDeviceId, async (req, res) => {
    try {
        const { deviceId, preferences } = req.body;
        
        if (!deviceId || !preferences) {
            return res.status(400).json({
                success: false,
                message: 'Device ID and preferences are required'
            });
        }
        
        // Find user by deviceId
        let user = await User.findOne({ deviceId });
        
        if (!user) {
            logger.warn(`Preferences update attempted for non-existent user: ${deviceId}`);
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
        
        logger.info(`User ${deviceId} preferences updated`);
        
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
 * @route   GET /api/users/:identifier/templates/favorites
 * @desc    Get user's favorite templates
 * @access  Public
 */
router.get('/:identifier/templates/favorites', async (req, res) => {
    try {
        const identifier = req.params.identifier;
        
        // Try to find user by uid first, then by deviceId
        let user = await User.findOne({ uid: identifier }).populate('favorites');
        
        // If not found by uid, try deviceId
        if (!user) {
            user = await User.findOne({ deviceId: identifier }).populate('favorites');
        }
        
        if (!user) {
            return res.status(404).json({
                success: false,
                message: 'User not found'
            });
        }
        
        res.status(200).json({
            success: true,
            favorites: user.favorites || []
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
 * @route   GET /api/users/:identifier/templates/likes
 * @desc    Get user's liked templates
 * @access  Public
 */
router.get('/:identifier/templates/likes', async (req, res) => {
    try {
        const identifier = req.params.identifier;
        
        // Try to find user by uid first, then by deviceId
        let user = await User.findOne({ uid: identifier }).populate('likes');
        
        // If not found by uid, try deviceId
        if (!user) {
            user = await User.findOne({ deviceId: identifier }).populate('likes');
        }
        
        if (!user) {
            return res.status(404).json({
                success: false,
                message: 'User not found'
            });
        }
        
        res.status(200).json({
            success: true,
            likes: user.likes || []
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
 * @route   GET /api/users/:identifier/templates/recent
 * @desc    Get user's recently used templates
 * @access  Public
 */
router.get('/:identifier/templates/recent', async (req, res) => {
    try {
        const identifier = req.params.identifier;
        
        // Try to find user by uid first, then by deviceId
        let user = await User.findOne({ uid: identifier }).populate('recentTemplatesUsed');
        
        // If not found by uid, try deviceId
        if (!user) {
            user = await User.findOne({ deviceId: identifier }).populate('recentTemplatesUsed');
        }
        
        if (!user) {
            return res.status(404).json({
                success: false,
                message: 'User not found'
            });
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

module.exports = router; 