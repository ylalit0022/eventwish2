const express = require('express');
const router = express.Router();
const User = require('../../models/firestore/User');
const logger = require('../../utils/logger');
const { validateFirebaseUid } = require('../../middleware/validators');
const { verifyFirebaseToken } = require('../../middleware/auth');

/**
 * @route   PUT /api/users/activity
 * @desc    Update user's last online timestamp and optionally record category visit
 * @access  Private
 */
router.put('/activity', validateFirebaseUid, verifyFirebaseToken, async (req, res) => {
    try {
        const { uid, category, source = 'direct' } = req.body;
        
        // Get user by uid
        let user = await User.getByUid(uid);
        
        if (!user) {
            logger.warn(`Activity update attempted for non-existent user: UID ${uid}`);
            return res.status(404).json({
                success: false,
                message: 'User not found'
            });
        }
        
        // Update lastOnline and record category visit if provided
        if (category) {
            await User.visitCategory(uid, category, source);
            logger.info(`User ${uid} visited category: ${category} (source: ${source})`);
        } else {
            await User.update(uid, { lastOnline: new Date() });
            logger.info(`User ${uid} activity updated (last online)`);
        }
        
        return res.status(200).json({
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
 * @route   POST /api/users/:uid/categories/visit
 * @desc    Record a category visit for the user
 * @access  Private
 */
router.post('/:uid/categories/visit', validateFirebaseUid, verifyFirebaseToken, async (req, res) => {
    try {
        const { uid } = req.params;
        const { category, source = 'direct', timestamp } = req.body;
        
        if (!category || category.trim() === '') {
            return res.status(400).json({
                success: false,
                message: 'Category is required'
            });
        }
        
        // Get user by uid
        let user = await User.getByUid(uid);
        
        if (!user) {
            logger.warn(`Category visit attempted for non-existent user: UID ${uid}`);
            return res.status(404).json({
                success: false,
                message: 'User not found'
            });
        }
        
        // Record category visit
        const visitCount = await User.visitCategory(uid, category, source);
        logger.info(`User ${uid} visited category: ${category} (source: ${source}, timestamp: ${timestamp})`);
        
        return res.status(200).json({
            success: true,
            message: 'Category visit recorded successfully',
            data: {
                category,
                source,
                visitCount
            }
        });
    } catch (error) {
        logger.error(`Category visit recording error: ${error.message}`);
        res.status(500).json({
            success: false,
            message: 'Server error recording category visit',
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
        
        // Get user by uid
        let user = await User.getByUid(uid);
        
        if (!user) {
            logger.warn(`Category history requested for non-existent user: UID ${uid}`);
            return res.status(404).json({
                success: false,
                message: 'User not found'
            });
        }
        
        // Get categories sorted by visit count
        const categories = await User.getCategoriesSorted(uid);
        
        return res.status(200).json({
            success: true,
            categories
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
