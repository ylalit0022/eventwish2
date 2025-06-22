const express = require('express');
const router = express.Router();
const User = require('../../models/User');
const logger = require('../../utils/logger');
const { verifyFirebaseToken } = require('../../middleware/auth');

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

module.exports = router; 