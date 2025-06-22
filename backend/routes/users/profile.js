const express = require('express');
const router = express.Router();
const User = require('../../models/User');
const logger = require('../../utils/logger');
const { validateFirebaseUid } = require('../../middleware/validators');
const { verifyFirebaseToken } = require('../../middleware/auth');
const { handleAsyncOperation, cleanupSubscriptionData } = require('./utils/helpers');

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
        const Template = require('../../models/Template');
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

module.exports = router; 