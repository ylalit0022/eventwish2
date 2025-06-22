const express = require('express');
const router = express.Router();
const User = require('../../models/User');
const logger = require('../../utils/logger');
const { validateFirebaseUid } = require('../../middleware/validators');
const { verifyFirebaseToken } = require('../../middleware/auth');
const { cleanupSubscriptionData } = require('./utils/helpers');

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

module.exports = router; 