const express = require('express');
const router = express.Router();
const User = require('../../models/firestore/User');
const logger = require('../../utils/logger');
const { validateFirebaseUid } = require('../../middleware/validators');
const { verifyFirebaseToken } = require('../../middleware/auth');
const { handleAsyncOperation, cleanupSubscriptionData } = require('./utils/helpers');

/**
 * @route   POST /api/users/profile
 * @desc    Update user profile in Firestore after Firebase authentication
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
        
        // Enhanced logging for debugging
        logger.info(`Profile update request for UID: ${uid}`, {
            hasDisplayName: !!displayName,
            displayNameLength: displayName ? displayName.length : 0,
            hasEmail: !!email,
            hasDeviceId: !!deviceId,
            deviceModel,
            appVersion
        });
        
        // Validate displayName if provided
        if (displayName !== undefined && displayName !== null) {
            if (typeof displayName !== 'string') {
                logger.warn(`Invalid displayName type for ${uid}: ${typeof displayName}`);
                return res.status(400).json({
                    success: false,
                    message: 'Display name must be a string'
                });
            }
            
            const trimmedName = displayName.trim();
            if (trimmedName.length < 2) {
                logger.warn(`Display name too short for ${uid}: ${trimmedName.length} characters`);
                return res.status(400).json({
                    success: false,
                    message: 'Display name must be at least 2 characters long'
                });
            }
            
            if (trimmedName.length > 50) {
                logger.warn(`Display name too long for ${uid}: ${trimmedName.length} characters`);
                return res.status(400).json({
                    success: false,
                    message: 'Display name cannot exceed 50 characters'
                });
            }
        }
        
        // Find user by uid
        let user = await User.getByUid(uid);
        
        if (user) {
            // User exists, update data
            const updateData = {
                lastOnline: lastOnline || new Date(),
                lastActive: new Date()
            };
            
            // Update profile info if provided
            if (displayName) updateData.displayName = displayName.trim();
            if (email) updateData.email = email.trim();
            if (profilePhoto) updateData.profilePhoto = profilePhoto;
            
            // Update device info if provided
            if (deviceId) updateData.deviceId = deviceId;
            if (deviceModel) updateData.deviceModel = deviceModel;
            if (deviceName) updateData.deviceName = deviceName;
            if (appVersion) updateData.appVersion = appVersion;
            if (osVersion) updateData.osVersion = osVersion;
            
            // Update login timestamp
            updateData.loginTimestamp = new Date();
            
            // Update the user document
            user = await User.update(uid, updateData);
            
            // Track device session if device info is provided
            if (deviceId) {
                try {
                    await User.addDeviceSession(uid, {
                        deviceId,
                        deviceModel: deviceModel || 'Unknown',
                        deviceName: deviceName || 'Unknown Device',
                        appVersion: appVersion || '1.0.0',
                        osVersion: osVersion || 'Unknown'
                    });
                    logger.info(`Device session added successfully for user ${uid}: ${deviceId}`);
                } catch (sessionError) {
                    logger.warn(`Failed to add device session for user ${uid}: ${sessionError.message}`);
                    // Don't fail the entire profile update if device session fails
                }
            }
            
            logger.info(`User profile updated: UID: ${uid}`);
            return res.status(200).json({
                success: true,
                message: 'User profile updated',
                user
            });
        } else {
            // User doesn't exist, create new user
            const userData = {
                uid,
                displayName: displayName ? displayName.trim() : null,
                email: email ? email.trim() : null,
                profilePhoto: profilePhoto || null,
                deviceId: deviceId || null,
                deviceModel: deviceModel || null,
                deviceName: deviceName || null,
                appVersion: appVersion || null,
                osVersion: osVersion || null,
                lastOnline: lastOnline || new Date(),
                loginTimestamp: new Date(),
                categories: []
            };
            
            user = await User.create(userData);
            
            // Track device session if device info is provided
            if (deviceId) {
                try {
                    await User.addDeviceSession(uid, {
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
        
        // Check if user already exists
        let user = await User.getByUid(uid);
        
        if (user) {
            logger.warn(`Registration attempt for existing user: ${uid}`);
            return res.status(400).json({
                success: false,
                message: 'User already exists',
                user
            });
        }
        
        // Create new user
        const userData = {
            uid,
            displayName: displayName ? displayName.trim() : null,
            email: email ? email.trim() : null,
            profilePhoto: profilePhoto || null,
            deviceId: deviceId || null,
            deviceModel: deviceModel || null,
            deviceName: deviceName || null,
            appVersion: appVersion || null,
            osVersion: osVersion || null,
            lastOnline: new Date(),
            loginTimestamp: new Date(),
            categories: []
        };
        
        user = await User.create(userData);
        
        // Track device session if device info is provided
        if (deviceId) {
            try {
                await User.addDeviceSession(uid, {
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
        
        logger.info(`New user registered: UID: ${uid}`);
        
        return res.status(201).json({
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
 * @route   GET /api/users/profile/:uid
 * @desc    Get user profile by UID
 * @access  Private
 */
router.get('/profile/:uid', validateFirebaseUid, verifyFirebaseToken, async (req, res) => {
    try {
        const { uid } = req.params;
        
        const user = await User.getByUid(uid);
        
        if (!user) {
            return res.status(404).json({
                success: false,
                message: 'User not found'
            });
        }
        
        return res.status(200).json({
            success: true,
            user
        });
    } catch (error) {
        logger.error(`Error fetching user profile: ${error.message}`);
        res.status(500).json({
            success: false,
            message: 'Server error fetching user profile',
            error: error.message
        });
    }
});

/**
 * @route   DELETE /api/users/profile/:uid
 * @desc    Delete user profile
 * @access  Private
 */
router.delete('/profile/:uid', validateFirebaseUid, verifyFirebaseToken, async (req, res) => {
    try {
        const { uid } = req.params;
        
        // Check if user exists
        const user = await User.getByUid(uid);
        
        if (!user) {
            return res.status(404).json({
                success: false,
                message: 'User not found'
            });
        }
        
        // Delete user document
        await User.delete(uid);
        
        logger.info(`User profile deleted: ${uid}`);
        
        return res.status(200).json({
            success: true,
            message: 'User profile deleted successfully'
        });
    } catch (error) {
        logger.error(`Error deleting user profile: ${error.message}`);
        res.status(500).json({
            success: false,
            message: 'Server error deleting user profile',
            error: error.message
        });
    }
});

module.exports = router; 