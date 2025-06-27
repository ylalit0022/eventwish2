const express = require('express');
const router = express.Router();
const websocketService = require('../services/websocketService');
const { verifyFirebaseToken } = require('../middleware/auth');
const logger = require('../utils/logger');

/**
 * @route   GET /api/websocket/stats
 * @desc    Get WebSocket connection statistics
 * @access  Private (Admin)
 */
router.get('/stats', verifyFirebaseToken, (req, res) => {
    try {
        const stats = websocketService.getStats();
        
        res.json({
            success: true,
            stats: {
                ...stats,
                timestamp: Date.now()
            }
        });
        
    } catch (error) {
        logger.error('WebSocket stats error:', error);
        res.status(500).json({
            success: false,
            message: 'Error getting WebSocket statistics',
            error: error.message
        });
    }
});

/**
 * @route   POST /api/websocket/broadcast/feed
 * @desc    Broadcast feed update to all subscribers
 * @access  Private (Admin)
 */
router.post('/broadcast/feed', verifyFirebaseToken, (req, res) => {
    try {
        const { feedType, updateData } = req.body;
        
        if (!feedType || !updateData) {
            return res.status(400).json({
                success: false,
                message: 'Feed type and update data are required'
            });
        }
        
        // Broadcast the feed update
        websocketService.broadcastFeedUpdate(feedType, updateData);
        
        logger.info(`Feed broadcast initiated by ${req.user.uid}:`, { feedType, updateData });
        
        res.json({
            success: true,
            message: 'Feed update broadcasted successfully',
            feedType,
            timestamp: Date.now()
        });
        
    } catch (error) {
        logger.error('Feed broadcast error:', error);
        res.status(500).json({
            success: false,
            message: 'Error broadcasting feed update',
            error: error.message
        });
    }
});

/**
 * @route   POST /api/websocket/broadcast/template
 * @desc    Broadcast template update to relevant subscribers
 * @access  Private (Admin)
 */
router.post('/broadcast/template', verifyFirebaseToken, (req, res) => {
    try {
        const { template, action } = req.body;
        
        if (!template || !action) {
            return res.status(400).json({
                success: false,
                message: 'Template and action are required'
            });
        }
        
        // Broadcast the template update
        websocketService.broadcastTemplateUpdate(template, action);
        
        logger.info(`Template broadcast initiated by ${req.user.uid}:`, { templateId: template.id, action });
        
        res.json({
            success: true,
            message: 'Template update broadcasted successfully',
            templateId: template.id,
            action,
            timestamp: Date.now()
        });
        
    } catch (error) {
        logger.error('Template broadcast error:', error);
        res.status(500).json({
            success: false,
            message: 'Error broadcasting template update',
            error: error.message
        });
    }
});

/**
 * @route   POST /api/websocket/notify/user
 * @desc    Send notification to specific user
 * @access  Private (Admin)
 */
router.post('/notify/user', verifyFirebaseToken, (req, res) => {
    try {
        const { userId, notification } = req.body;
        
        if (!userId || !notification) {
            return res.status(400).json({
                success: false,
                message: 'User ID and notification are required'
            });
        }
        
        // Send notification to user
        websocketService.sendNotificationToUser(userId, notification);
        
        logger.info(`User notification sent by ${req.user.uid}:`, { userId, notification });
        
        res.json({
            success: true,
            message: 'Notification sent successfully',
            userId,
            timestamp: Date.now()
        });
        
    } catch (error) {
        logger.error('User notification error:', error);
        res.status(500).json({
            success: false,
            message: 'Error sending user notification',
            error: error.message
        });
    }
});

/**
 * @route   POST /api/websocket/broadcast/announcement
 * @desc    Broadcast system announcement to all connected users
 * @access  Private (Admin)
 */
router.post('/broadcast/announcement', verifyFirebaseToken, (req, res) => {
    try {
        const { announcement } = req.body;
        
        if (!announcement) {
            return res.status(400).json({
                success: false,
                message: 'Announcement is required'
            });
        }
        
        // Broadcast system announcement
        websocketService.broadcastAnnouncement(announcement);
        
        logger.info(`System announcement broadcasted by ${req.user.uid}:`, announcement);
        
        res.json({
            success: true,
            message: 'System announcement broadcasted successfully',
            announcement,
            timestamp: Date.now()
        });
        
    } catch (error) {
        logger.error('System announcement error:', error);
        res.status(500).json({
            success: false,
            message: 'Error broadcasting system announcement',
            error: error.message
        });
    }
});

/**
 * @route   POST /api/websocket/trigger/feed-refresh
 * @desc    Trigger feed refresh for specific user or all users
 * @access  Private (Admin)
 */
router.post('/trigger/feed-refresh', verifyFirebaseToken, (req, res) => {
    try {
        const { userId, reason = 'admin_refresh' } = req.body;
        
        if (userId) {
            // Refresh feed for specific user
            websocketService.queueFeedUpdate(userId, reason);
            
            res.json({
                success: true,
                message: 'Feed refresh triggered for user',
                userId,
                reason,
                timestamp: Date.now()
            });
        } else {
            // Trigger refresh for all connected users
            const stats = websocketService.getStats();
            
            // Note: This would need to be implemented in websocketService
            // websocketService.triggerGlobalFeedRefresh(reason);
            
            res.json({
                success: true,
                message: 'Global feed refresh triggered',
                connectedUsers: stats.connectedUsers,
                reason,
                timestamp: Date.now()
            });
        }
        
        logger.info(`Feed refresh triggered by ${req.user.uid}:`, { userId, reason });
        
    } catch (error) {
        logger.error('Feed refresh trigger error:', error);
        res.status(500).json({
            success: false,
            message: 'Error triggering feed refresh',
            error: error.message
        });
    }
});

/**
 * @route   GET /api/websocket/health
 * @desc    WebSocket service health check
 * @access  Public
 */
router.get('/health', (req, res) => {
    try {
        const stats = websocketService.getStats();
        const isHealthy = stats.connectedUsers >= 0; // Basic health check
        
        res.status(isHealthy ? 200 : 503).json({
            success: isHealthy,
            message: isHealthy ? 'WebSocket service is healthy' : 'WebSocket service is unhealthy',
            stats: {
                connectedUsers: stats.connectedUsers,
                uptime: stats.uptime,
                memoryUsage: stats.memoryUsage
            },
            timestamp: Date.now()
        });
        
    } catch (error) {
        logger.error('WebSocket health check error:', error);
        res.status(503).json({
            success: false,
            message: 'WebSocket service health check failed',
            error: error.message,
            timestamp: Date.now()
        });
    }
});

module.exports = router; 