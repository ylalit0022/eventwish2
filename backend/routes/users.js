const express = require('express');
const router = express.Router();
const User = require('../models/User');
const logger = require('../utils/logger');
const { validateDeviceId } = require('../middleware/validators');
const recommendationService = require('../services/recommendationService');

/**
 * @route   POST /api/users/register
 * @desc    Register a new user with device ID
 * @access  Public
 */
router.post('/register', validateDeviceId, async (req, res) => {
    try {
        const { deviceId } = req.body;
        
        // Check if user already exists
        let user = await User.findOne({ deviceId });
        
        if (user) {
            // User already exists, update lastOnline
            user.lastOnline = Date.now();
            await user.save();
            
            logger.info(`Existing user logged in: ${deviceId}`);
            return res.status(200).json({
                success: true,
                message: 'User already exists',
                user
            });
        }
        
        // Create new user
        user = new User({
            deviceId,
            lastOnline: Date.now(),
            created: Date.now(),
            categories: []
        });
        
        await user.save();
        logger.info(`New user registered: ${deviceId}`);
        
        res.status(201).json({
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
 * @route   PUT /api/users/activity
 * @desc    Update user's last online timestamp and optionally record category visit
 * @access  Public
 */
router.put('/activity', validateDeviceId, async (req, res) => {
    try {
        const { deviceId, category, source = 'direct' } = req.body;
        
        // Find user by deviceId
        let user = await User.findOne({ deviceId });
        
        if (!user) {
            logger.warn(`Activity update attempted for non-existent user: ${deviceId}`);
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
            logger.info(`User ${deviceId} visited category: ${category} (source: ${source})`);
            
            // Invalidate recommendations cache on category visit
            await recommendationService.invalidateUserRecommendations(deviceId);
        } else {
            await user.save();
            logger.info(`User ${deviceId} activity updated (last online)`);
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
 * @route   PUT /api/users/template-view
 * @desc    Record a template view with its category
 * @access  Public
 */
router.put('/template-view', validateDeviceId, async (req, res) => {
    try {
        const { deviceId, templateId, category } = req.body;
        
        if (!templateId || !category) {
            return res.status(400).json({
                success: false,
                message: 'Template ID and category are required'
            });
        }
        
        // Find user by deviceId
        let user = await User.findOne({ deviceId });
        
        if (!user) {
            logger.warn(`Template view attempted for non-existent user: ${deviceId}`);
            return res.status(404).json({
                success: false,
                message: 'User not found'
            });
        }
        
        // Record category visit from template
        await user.visitCategoryFromTemplate(category, templateId);
        logger.info(`User ${deviceId} viewed template ${templateId} in category: ${category}`);
        
        // Invalidate recommendations cache on template view
        await recommendationService.invalidateUserRecommendations(deviceId);
        
        res.status(200).json({
            success: true,
            message: 'Template view recorded'
        });
    } catch (error) {
        logger.error(`Template view error: ${error.message}`);
        res.status(500).json({
            success: false,
            message: 'Server error recording template view',
            error: error.message
        });
    }
});

/**
 * @route   GET /api/users/:deviceId
 * @desc    Get user data by device ID (for testing and debugging)
 * @access  Public (should be restricted in production)
 */
router.get('/:deviceId', async (req, res) => {
    try {
        const deviceId = req.params.deviceId;
        
        // Find user by deviceId
        const user = await User.findOne({ deviceId });
        
        if (!user) {
            return res.status(404).json({
                success: false,
                message: 'User not found'
            });
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
 * @route   GET /api/users/:deviceId/recommendations
 * @desc    Get personalized template recommendations for a user
 * @access  Public
 */
router.get('/:deviceId/recommendations', async (req, res) => {
    try {
        const { deviceId } = req.params;
        const limit = parseInt(req.query.limit) || 10;
        
        if (!deviceId) {
            return res.status(400).json({
                success: false,
                message: 'Device ID is required'
            });
        }
        
        // Get recommendations using the recommendation service
        const recommendations = await recommendationService.getRecommendationsForUser(deviceId, limit);
        
        res.status(200).json({
            success: true,
            recommendations
        });
    } catch (error) {
        logger.error(`Recommendations error: ${error.message}`);
        res.status(500).json({
            success: false,
            message: 'Server error generating recommendations',
            error: error.message
        });
    }
});

module.exports = router; 