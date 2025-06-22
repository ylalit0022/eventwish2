const express = require('express');
const router = express.Router();
const User = require('../../models/User');
const logger = require('../../utils/logger');
const { validateFirebaseUid } = require('../../middleware/validators');
const { verifyFirebaseToken } = require('../../middleware/auth');

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
 * @route   POST /api/users/:uid/fcm-tokens
 * @desc    Add FCM token for push notifications
 * @access  Private
 */
router.post('/:uid/fcm-tokens', verifyFirebaseToken, async (req, res) => {
    try {
        const { uid } = req.params;
        const { token, deviceInfo } = req.body;
        
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
        
        // Initialize fcmTokens if needed
        if (!user.fcmTokens) {
            user.fcmTokens = [];
        }
        
        // Check if token already exists
        const existingTokenIndex = user.fcmTokens.findIndex(t => t.token === token);
        
        if (existingTokenIndex !== -1) {
            // Update existing token
            user.fcmTokens[existingTokenIndex] = {
                token,
                deviceInfo: deviceInfo || user.fcmTokens[existingTokenIndex].deviceInfo,
                addedAt: user.fcmTokens[existingTokenIndex].addedAt,
                lastUsed: new Date()
            };
        } else {
            // Add new token
            user.fcmTokens.push({
                token,
                deviceInfo: deviceInfo || {},
                addedAt: new Date(),
                lastUsed: new Date()
            });
        }
        
        await user.save();
        
        res.status(200).json({
            success: true,
            message: 'FCM token added successfully'
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
        
        if (user.fcmTokens) {
            user.fcmTokens = user.fcmTokens.filter(t => t.token !== token);
            await user.save();
        }
        
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

module.exports = router; 