const express = require('express');
const router = express.Router();
const mongoose = require('mongoose');
const User = require('../../models/User');
const logger = require('../../utils/logger');
const { validateFirebaseUid } = require('../../middleware/validators');
const { verifyFirebaseToken } = require('../../middleware/auth');

const { 
    handleAsyncOperation, 
    safeUpdateTemplateCounts, 
    isValidObjectId 
} = require('./utils/helpers');

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
        // Note: Recommendation system removed
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

module.exports = router;
