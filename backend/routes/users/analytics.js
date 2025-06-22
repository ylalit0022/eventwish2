const express = require('express');
const router = express.Router();
const User = require('../../models/User');
const logger = require('../../utils/logger');
const { validateFirebaseUid } = require('../../middleware/validators');
const { verifyFirebaseToken } = require('../../middleware/auth');
const recommendationService = require('../../services/recommendationService');

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
        
        // Get personalized recommendations
        const recommendations = await recommendationService.getPersonalizedRecommendations(uid, limit);
        
        res.status(200).json({
            success: true,
            recommendations,
            totalCount: recommendations.length
        });
    } catch (error) {
        logger.error(`Get recommendations error: ${error.message}`);
        res.status(500).json({
            success: false,
            message: 'Server error retrieving recommendations',
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

module.exports = router; 