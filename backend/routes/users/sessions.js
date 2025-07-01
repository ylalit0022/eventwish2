const express = require('express');
const router = express.Router();
const User = require('../../models/firestore/User');
const logger = require('../../utils/logger');
const { validateFirebaseUid } = require('../../middleware/validators');
const { verifyFirebaseToken } = require('../../middleware/auth');

/**
 * @route   GET /api/users/:uid/sessions
 * @desc    Get all active sessions for a user
 * @access  Private
 */
router.get('/:uid/sessions', validateFirebaseUid, verifyFirebaseToken, async (req, res) => {
    try {
        const { uid } = req.params;
        
        // Get user by uid
        const user = await User.getByUid(uid);
        
        if (!user) {
            return res.status(404).json({
                success: false,
                message: 'User not found'
            });
        }
        
        // Get active sessions
        const sessions = await User.getActiveSessions(uid);
        
        logger.info(`Retrieved ${sessions.length} active sessions for user ${uid}`);
        
        return res.status(200).json({
            success: true,
            message: 'User sessions retrieved successfully',
            sessions: sessions.map(session => ({
                ...session,
                isCurrentDevice: session.deviceId === user.deviceId
            })),
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
        
        // Get user by uid
        let user = await User.getByUid(uid);
        
        if (!user) {
            logger.warn(`Session invalidation attempted for non-existent user: UID ${uid}`);
            return res.status(404).json({
                success: false,
                message: 'User not found'
            });
        }
        
        // Invalidate other sessions
        await User.invalidateOtherSessions(uid, deviceId);
        
        logger.info(`User ${uid} invalidated all other sessions except ${deviceId}`);
        
        return res.status(200).json({
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
        
        // Get user by uid
        let user = await User.getByUid(uid);
        
        if (!user) {
            logger.warn(`Session removal attempted for non-existent user: UID ${uid}`);
            return res.status(404).json({
                success: false,
                message: 'User not found'
            });
        }
        
        // Remove the session
        await User.removeDeviceSession(uid, deviceId);
        
        logger.info(`User ${uid} removed session for device ${deviceId}`);
        
        return res.status(200).json({
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
        
        // Get user by uid
        let user = await User.getByUid(uid);
        
        if (!user) {
            logger.warn(`Session update attempted for non-existent user: UID ${uid}`);
            return res.status(404).json({
                success: false,
                message: 'User not found'
            });
        }
        
        // Update session activity and user's lastOnline
        await User.updateDeviceSessionActivity(uid, deviceId);
        
        return res.status(200).json({
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
 * @desc    Add a new device session
 * @access  Private
 */
router.post('/:uid/device-sessions', validateFirebaseUid, verifyFirebaseToken, async (req, res) => {
    try {
        const { uid } = req.params;
        const { deviceId, deviceModel, deviceName, appVersion, osVersion } = req.body;
        
        if (!deviceId) {
            return res.status(400).json({
                success: false,
                message: 'Device ID is required'
            });
        }
        
        // Get user by uid
        let user = await User.getByUid(uid);
        
        if (!user) {
            logger.warn(`Device session addition attempted for non-existent user: UID ${uid}`);
            return res.status(404).json({
                success: false,
                message: 'User not found'
            });
        }
        
        // Add device session
        await User.addDeviceSession(uid, {
            deviceId,
            deviceModel: deviceModel || 'Unknown',
            deviceName: deviceName || 'Unknown Device',
            appVersion: appVersion || '1.0.0',
            osVersion: osVersion || 'Unknown'
        });
        
        logger.info(`Added device session for user ${uid}: ${deviceId}`);
        
        return res.status(200).json({
            success: true,
            message: 'Device session added successfully'
        });
    } catch (error) {
        logger.error(`Add device session error: ${error.message}`);
        res.status(500).json({
            success: false,
            message: 'Server error adding device session',
            error: error.message
        });
    }
});

/**
 * @route   DELETE /api/users/:uid/device-sessions/:deviceId
 * @desc    Remove a device session
 * @access  Private
 */
router.delete('/:uid/device-sessions/:deviceId', validateFirebaseUid, verifyFirebaseToken, async (req, res) => {
    try {
        const { uid, deviceId } = req.params;
        
        // Get user by uid
        let user = await User.getByUid(uid);
        
        if (!user) {
            logger.warn(`Device session removal attempted for non-existent user: UID ${uid}`);
            return res.status(404).json({
                success: false,
                message: 'User not found'
            });
        }
        
        // Remove device session
        await User.removeDeviceSession(uid, deviceId);
        
        logger.info(`Removed device session for user ${uid}: ${deviceId}`);
        
        return res.status(200).json({
            success: true,
            message: 'Device session removed successfully'
        });
    } catch (error) {
        logger.error(`Remove device session error: ${error.message}`);
        res.status(500).json({
            success: false,
            message: 'Server error removing device session',
            error: error.message
        });
    }
});

/**
 * @route   PUT /api/users/:uid/device-sessions/:deviceId/activity
 * @desc    Update device session activity
 * @access  Private
 */
router.put('/:uid/device-sessions/:deviceId/activity', validateFirebaseUid, verifyFirebaseToken, async (req, res) => {
    try {
        const { uid, deviceId } = req.params;
        
        // Get user by uid
        let user = await User.getByUid(uid);
        
        if (!user) {
            logger.warn(`Device session activity update attempted for non-existent user: UID ${uid}`);
            return res.status(404).json({
                success: false,
                message: 'User not found'
            });
        }
        
        // Update device session activity
        await User.updateDeviceSessionActivity(uid, deviceId);
        
        logger.info(`Updated device session activity for user ${uid}: ${deviceId}`);
        
        return res.status(200).json({
            success: true,
            message: 'Device session activity updated successfully'
        });
    } catch (error) {
        logger.error(`Update device session activity error: ${error.message}`);
        res.status(500).json({
            success: false,
            message: 'Server error updating device session activity',
            error: error.message
        });
    }
});

module.exports = router; 