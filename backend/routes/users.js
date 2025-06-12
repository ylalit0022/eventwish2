const express = require('express');
const router = express.Router();
const User = require('../models/User');
const logger = require('../utils/logger');
const { validateDeviceId, validateFirebaseUid } = require('../middleware/validators');
const { verifyFirebaseToken, optionalFirebaseAuth } = require('../middleware/auth');
const recommendationService = require('../services/recommendationService');

/**
 * @route   POST /api/users/profile
 * @desc    Update user profile in MongoDB after Firebase authentication
 * @access  Private
 */
router.post('/profile', validateFirebaseUid, verifyFirebaseToken, async (req, res) => {
    try {
        const { uid, displayName, email, profilePhoto, lastOnline } = req.body;
        
        // Find user by uid only
        let user = await User.findOne({ uid });
        
        if (user) {
            // User exists, update data
            user.lastOnline = lastOnline || Date.now();
            
            // Update profile info if provided
            if (displayName) user.displayName = displayName;
            if (email) user.email = email;
            if (profilePhoto) user.profilePhoto = profilePhoto;
            
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
                lastOnline: lastOnline || Date.now(),
                created: Date.now(),
                categories: []
            });
            
            await user.save();
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
        const { uid, displayName, email, profilePhoto } = req.body;
        
        // Find user by uid
        let user = await User.findOne({ uid });
        
        if (user) {
            // User already exists, update lastOnline
            user.lastOnline = Date.now();
            
            // Update profile info if provided
            if (displayName) user.displayName = displayName;
            if (email) user.email = email;
            if (profilePhoto) user.profilePhoto = profilePhoto;
            
            await user.save();
            
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
            lastOnline: Date.now(),
            created: Date.now(),
            categories: []
        });
        
        await user.save();
        logger.info(`New user registered: UID: ${uid}`);
        
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
    try {
        const { uid, type, templateId, category, timestamp, durationMs, engagementScore, source } = req.body;
        
        // Validate required fields
        if (!uid || !type) {
            return res.status(400).json({
                success: false,
                message: 'Firebase UID and engagement type are required'
            });
        }
        
        // Find user by uid
        let user = await User.findOne({ uid });
        
        if (!user) {
            logger.warn(`Engagement tracking attempted for non-existent user: UID ${uid}`);
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
                    
                    logger.info(`User ${uid} engagement: favorited template ${templateId}`);
                }
                break;
                
            default:
                logger.warn(`Unknown engagement type ${type} from user ${uid}`);
        }
        
        // Update last online time
        user.lastOnline = Date.now();
        await user.save();
        
        // Invalidate recommendations cache
        await recommendationService.invalidateUserRecommendations(uid);
        
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
 * @access  Private
 */
router.post('/engagement/sync', validateFirebaseUid, verifyFirebaseToken, async (req, res) => {
    try {
        const { uid, engagements } = req.body;
        
        if (!uid || !engagements || !Array.isArray(engagements)) {
            return res.status(400).json({
                success: false,
                message: 'Firebase UID and engagement array are required'
            });
        }
        
        // Find user by uid
        let user = await User.findOne({ uid });
        
        if (!user) {
            logger.warn(`Batch engagement sync attempted for non-existent user: UID ${uid}`);
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
        await recommendationService.invalidateUserRecommendations(uid);
        
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
        
        // Find user by uid
        let user = await User.findOne({ uid }).populate('favorites');
        
        // For backward compatibility, try deviceId fallback
        if (!user) {
            user = await User.findOne({ deviceId: uid }).populate('favorites');
            
            if (!user) {
                logger.warn(`Favorites requested for non-existent user: UID ${uid}`);
                return res.status(404).json({
                    success: false,
                    message: 'User not found'
                });
            }
            
            logger.info(`Found user by deviceId fallback for favorites: ${uid}`);
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
 * @route   GET /api/users/:uid/templates/likes
 * @desc    Get user's liked templates
 * @access  Public
 */
router.get('/:uid/templates/likes', async (req, res) => {
    try {
        const uid = req.params.uid;
        
        // Find user by uid
        let user = await User.findOne({ uid }).populate('likes');
        
        // For backward compatibility, try deviceId fallback
        if (!user) {
            user = await User.findOne({ deviceId: uid }).populate('likes');
            
            if (!user) {
                logger.warn(`Likes requested for non-existent user: UID ${uid}`);
                return res.status(404).json({
                    success: false,
                    message: 'User not found'
                });
            }
            
            logger.info(`Found user by deviceId fallback for likes: ${uid}`);
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
        const { uid, deviceId } = req.body;
        
        if (!uid) {
            return res.status(400).json({
                success: false,
                message: 'Firebase UID is required'
            });
        }
        
        // Check if user exists by UID
        let user = await User.findOne({ uid });
        let isNewUser = false;
        
        if (user) {
            // User exists, update last online
            user.lastOnline = Date.now();
            await user.save();
            logger.info(`Existing user authenticated: UID ${uid}`);
        } else {
            // Check if there's a user with the provided deviceId
            if (deviceId) {
                const deviceUser = await User.findOne({ deviceId });
                
                if (deviceUser) {
                    // Link existing device user with Firebase UID
                    deviceUser.uid = uid;
                    deviceUser.lastOnline = Date.now();
                    await deviceUser.save();
                    
                    user = deviceUser;
                    logger.info(`Linked device ID ${deviceId} with Firebase UID ${uid}`);
                } else {
                    // Create new user with both UID and deviceId
                    isNewUser = true;
                    user = new User({
                        uid,
                        deviceId: deviceId || null,
                        lastOnline: Date.now(),
                        created: Date.now()
                    });
                    
                    await user.save();
                    logger.info(`New user created with UID ${uid} and deviceId ${deviceId || 'null'}`);
                }
            } else {
                // Create new user with just UID
                isNewUser = true;
                user = new User({
                    uid,
                    lastOnline: Date.now(),
                    created: Date.now()
                });
                
                await user.save();
                logger.info(`New user created with UID ${uid}`);
            }
        }
        
        // Return user data and new user flag
        res.status(200).json({
            success: true,
            isNewUser,
            user: {
                uid: user.uid,
                deviceId: user.deviceId,
                displayName: user.displayName,
                email: user.email,
                profilePhoto: user.profilePhoto,
                lastOnline: user.lastOnline,
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
    try {
        const { uid, templateId } = req.params;
        
        // Find user by uid
        let user = await User.findOne({ uid });
        
        if (!user) {
            logger.warn(`Favorite operation attempted for non-existent user: UID ${uid}`);
            return res.status(404).json({
                success: false,
                message: 'User not found'
            });
        }
        
        // Initialize favorites array if it doesn't exist
        if (!user.favorites) {
            user.favorites = [];
        }
        
        // Check if template is already in favorites
        if (!user.favorites.some(id => id.toString() === templateId.toString())) {
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
            
            await user.save();
            
            // Invalidate recommendations
            await recommendationService.invalidateUserRecommendations(uid);
            
            logger.info(`User ${uid} added template ${templateId} to favorites`);
            
            return res.status(200).json({
                success: true,
                message: 'Template added to favorites',
                favorites: user.favorites
            });
        } else {
            // Template already in favorites
            return res.status(200).json({
                success: true,
                message: 'Template already in favorites',
                favorites: user.favorites
            });
        }
    } catch (error) {
        logger.error(`Add favorite error: ${error.message}`);
        res.status(500).json({
            success: false,
            message: 'Server error adding template to favorites',
            error: error.message
        });
    }
});

/**
 * @route   DELETE /api/users/:uid/favorites/:templateId
 * @desc    Remove a template from user's favorites
 * @access  Private
 */
router.delete('/:uid/favorites/:templateId', validateFirebaseUid, verifyFirebaseToken, async (req, res) => {
    try {
        const { uid, templateId } = req.params;
        
        // Find user by uid
        let user = await User.findOne({ uid });
        
        if (!user) {
            logger.warn(`Favorite removal attempted for non-existent user: UID ${uid}`);
            return res.status(404).json({
                success: false,
                message: 'User not found'
            });
        }
        
        // Check if user has favorites
        if (!user.favorites || user.favorites.length === 0) {
            return res.status(200).json({
                success: true,
                message: 'No favorites to remove',
                favorites: []
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
            
            await user.save();
            
            // Invalidate recommendations
            await recommendationService.invalidateUserRecommendations(uid);
            
            logger.info(`User ${uid} removed template ${templateId} from favorites`);
            
            return res.status(200).json({
                success: true,
                message: 'Template removed from favorites',
                favorites: user.favorites
            });
        } else {
            // Template not in favorites
            return res.status(200).json({
                success: true,
                message: 'Template not in favorites',
                favorites: user.favorites
            });
        }
    } catch (error) {
        logger.error(`Remove favorite error: ${error.message}`);
        res.status(500).json({
            success: false,
            message: 'Server error removing template from favorites',
            error: error.message
        });
    }
});

/**
 * @route   PUT /api/users/:uid/likes/:templateId
 * @desc    Add a template to user's likes
 * @access  Private
 */
router.put('/:uid/likes/:templateId', validateFirebaseUid, verifyFirebaseToken, async (req, res) => {
    try {
        const { uid, templateId } = req.params;
        
        // Find user by uid
        let user = await User.findOne({ uid });
        
        if (!user) {
            logger.warn(`Like operation attempted for non-existent user: UID ${uid}`);
            return res.status(404).json({
                success: false,
                message: 'User not found'
            });
        }
        
        // Initialize likes array if it doesn't exist
        if (!user.likes) {
            user.likes = [];
        }
        
        // Check if template is already liked
        if (!user.likes.some(id => id.toString() === templateId.toString())) {
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
            
            await user.save();
            
            // Invalidate recommendations
            await recommendationService.invalidateUserRecommendations(uid);
            
            logger.info(`User ${uid} liked template ${templateId}`);
            
            return res.status(200).json({
                success: true,
                message: 'Template liked',
                likes: user.likes
            });
        } else {
            // Template already liked
            return res.status(200).json({
                success: true,
                message: 'Template already liked',
                likes: user.likes
            });
        }
    } catch (error) {
        logger.error(`Add like error: ${error.message}`);
        res.status(500).json({
            success: false,
            message: 'Server error liking template',
            error: error.message
        });
    }
});

/**
 * @route   DELETE /api/users/:uid/likes/:templateId
 * @desc    Remove a template from user's likes
 * @access  Private
 */
router.delete('/:uid/likes/:templateId', validateFirebaseUid, verifyFirebaseToken, async (req, res) => {
    try {
        const { uid, templateId } = req.params;
        
        // Find user by uid
        let user = await User.findOne({ uid });
        
        if (!user) {
            logger.warn(`Like removal attempted for non-existent user: UID ${uid}`);
            return res.status(404).json({
                success: false,
                message: 'User not found'
            });
        }
        
        // Check if user has likes
        if (!user.likes || user.likes.length === 0) {
            return res.status(200).json({
                success: true,
                message: 'No likes to remove',
                likes: []
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
            
            await user.save();
            
            // Invalidate recommendations
            await recommendationService.invalidateUserRecommendations(uid);
            
            logger.info(`User ${uid} unliked template ${templateId}`);
            
            return res.status(200).json({
                success: true,
                message: 'Template unliked',
                likes: user.likes
            });
        } else {
            // Template not liked
            return res.status(200).json({
                success: true,
                message: 'Template not liked',
                likes: user.likes
            });
        }
    } catch (error) {
        logger.error(`Remove like error: ${error.message}`);
        res.status(500).json({
            success: false,
            message: 'Server error unliking template',
            error: error.message
        });
    }
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

module.exports = router; 