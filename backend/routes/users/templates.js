const express = require('express');
const router = express.Router();
const User = require('../../models/firestore/User');
const Template = require('../../models/Template'); // Note: Template model will be migrated in a separate PR
const websocketService = require('../../services/websocketService');
const logger = require('../../utils/logger');
const { validateFirebaseUid } = require('../../middleware/validators');
const { verifyFirebaseToken } = require('../../middleware/auth');

const { validateTemplateInteraction } = require('./utils/helpers');

/**
 * @route   GET /api/users/:uid/templates/favorites
 * @desc    Get user's favorite templates
 * @access  Public
 */
router.get('/:uid/templates/favorites', async (req, res) => {
    try {
        const { uid } = req.params;
        const { page = 1, limit = 10 } = req.query;
        
        // Get user by uid
        const user = await User.getByUid(uid);
        if (!user) {
            return res.status(404).json({
                success: false,
                message: 'User not found'
            });
        }
        
        // Get paginated favorites
        const { templates, totalCount, totalPages } = await User.getPaginatedFavorites(uid, {
            page: parseInt(page),
            limit: parseInt(limit)
        });
        
        return res.status(200).json({
            success: true,
            favorites: templates,
            totalCount,
            currentPage: parseInt(page),
            totalPages
        });
    } catch (error) {
        logger.error(`Get user favorites error: ${error.message}`);
        res.status(500).json({
            success: false,
            message: 'Server error getting favorites',
            error: error.message
        });
    }
});

/**
 * @route   GET /api/users/:uid/templates/likes
 * @desc    Get user's liked templates
 * @access  Public
 */
router.get('/:uid/templates/likes', async (req, res) => {
    try {
        const { uid } = req.params;
        const { page = 1, limit = 10 } = req.query;
        
        // Get user by uid
        const user = await User.getByUid(uid);
        if (!user) {
            return res.status(404).json({
                success: false,
                message: 'User not found'
            });
        }
        
        // Get paginated likes
        const { templates, totalCount, totalPages } = await User.getPaginatedLikes(uid, {
            page: parseInt(page),
            limit: parseInt(limit)
        });
        
        return res.status(200).json({
            success: true,
            likes: templates,
            totalCount,
            currentPage: parseInt(page),
            totalPages
        });
    } catch (error) {
        logger.error(`Get user likes error: ${error.message}`);
        res.status(500).json({
            success: false,
            message: 'Server error getting likes',
            error: error.message
        });
    }
});

/**
 * @route   GET /api/users/:uid/templates/recent
 * @desc    Get user's recently used templates
 * @access  Private
 */
router.get('/:uid/templates/recent', verifyFirebaseToken, async (req, res) => {
    try {
        const { uid } = req.params;
        const { limit = 10 } = req.query;
        
        // Get user by uid
        const user = await User.getByUid(uid);
        if (!user) {
            return res.status(404).json({
                success: false,
                message: 'User not found'
            });
        }
        
        // Get recent templates
        const templates = await User.getRecentTemplates(uid, parseInt(limit));
        
        return res.status(200).json({
            success: true,
            recentTemplates: templates
        });
    } catch (error) {
        logger.error(`Get user recent templates error: ${error.message}`);
        res.status(500).json({
            success: false,
            message: 'Server error getting recent templates',
            error: error.message
        });
    }
});

/**
 * @route   PUT /api/users/:uid/favorites/:templateId
 * @desc    Add a template to user's favorites
 * @access  Private
 */
router.put('/:uid/favorites/:templateId', validateFirebaseUid, verifyFirebaseToken, async (req, res) => {
    try {
        const { uid, templateId } = req.params;
        
        // Validate request
        const validation = validateTemplateInteraction(uid, templateId);
        if (!validation.isValid) {
            return res.status(400).json({
                success: false,
                message: 'Validation failed',
                errors: validation.errors
            });
        }
        
        logger.info(`Starting favorite operation for user ${uid}, template ${templateId}`);
        
        // Get user by uid
        const user = await User.getByUid(uid);
        if (!user) {
            logger.warn(`Favorite operation attempted for non-existent user: UID ${uid}`);
            return res.status(404).json({
                success: false,
                message: 'User not found'
            });
        }
        
        // Get template
        const template = await Template.findById(templateId);
        if (!template) {
            logger.warn(`Favorite operation attempted for non-existent template: ${templateId}`);
            return res.status(404).json({
                success: false,
                message: 'Template not found'
            });
        }
        
        // Add to favorites
        const isFavorited = await User.addToFavorites(uid, templateId);
        
        // Update template favorites count
        if (isFavorited) {
            template.favorites = (template.favorites || 0) + 1;
            await template.save();
            
            // Notify connected clients about the update
            websocketService.notifyTemplateUpdate(templateId, {
                favorites: template.favorites
            });
        }
        
        logger.info(`User ${uid} ${isFavorited ? 'favorited' : 'unfavorited'} template ${templateId}`);
        
        return res.status(200).json({
            success: true,
            message: isFavorited ? 'Template added to favorites' : 'Template removed from favorites',
            isFavorited
        });
    } catch (error) {
        logger.error(`Template favorite error: ${error.message}`);
        res.status(500).json({
            success: false,
            message: 'Server error updating favorites',
            error: error.message
        });
    }
});

/**
 * @route   PUT /api/users/:uid/likes/:templateId
 * @desc    Add a template to user's likes
 * @access  Private
 */
router.put('/:uid/likes/:templateId', validateFirebaseUid, verifyFirebaseToken, async (req, res) => {
    try {
        const { uid, templateId } = req.params;
        
        // Validate request
        const validation = validateTemplateInteraction(uid, templateId);
        if (!validation.isValid) {
            return res.status(400).json({
                success: false,
                message: 'Validation failed',
                errors: validation.errors
            });
        }
        
        logger.info(`Starting like operation for user ${uid}, template ${templateId}`);
        
        // Get user by uid
        const user = await User.getByUid(uid);
        if (!user) {
            logger.warn(`Like operation attempted for non-existent user: UID ${uid}`);
            return res.status(404).json({
                success: false,
                message: 'User not found'
            });
        }
        
        // Get template
        const template = await Template.findById(templateId);
        if (!template) {
            logger.warn(`Like operation attempted for non-existent template: ${templateId}`);
            return res.status(404).json({
                success: false,
                message: 'Template not found'
            });
        }
        
        // Add to likes
        const isLiked = await User.addToLikes(uid, templateId);
        
        // Update template likes count
        if (isLiked) {
            template.likes = (template.likes || 0) + 1;
            await template.save();
            
            // Notify connected clients about the update
            websocketService.notifyTemplateUpdate(templateId, {
                likes: template.likes
            });
        }
        
        logger.info(`User ${uid} ${isLiked ? 'liked' : 'unliked'} template ${templateId}`);
        
        return res.status(200).json({
            success: true,
            message: isLiked ? 'Template added to likes' : 'Template removed from likes',
            isLiked
        });
    } catch (error) {
        logger.error(`Template like error: ${error.message}`);
        res.status(500).json({
            success: false,
            message: 'Server error updating likes',
            error: error.message
        });
    }
});

/**
 * @route   POST /api/users/:uid/templates/:templateId/use
 * @desc    Record template usage by user
 * @access  Private
 */
router.post('/:uid/templates/:templateId/use', validateFirebaseUid, verifyFirebaseToken, async (req, res) => {
    try {
        const { uid, templateId } = req.params;
        
        // Validate request
        const validation = validateTemplateInteraction(uid, templateId);
        if (!validation.isValid) {
            return res.status(400).json({
                success: false,
                message: 'Validation failed',
                errors: validation.errors
            });
        }
        
        logger.info(`Starting template use operation for user ${uid}, template ${templateId}`);
        
        // Get user by uid
        const user = await User.getByUid(uid);
        if (!user) {
            logger.warn(`Template use attempted for non-existent user: UID ${uid}`);
            return res.status(404).json({
                success: false,
                message: 'User not found'
            });
        }
        
        // Get template
        const template = await Template.findById(templateId);
        if (!template) {
            logger.warn(`Template use attempted for non-existent template: ${templateId}`);
            return res.status(404).json({
                success: false,
                message: 'Template not found'
            });
        }
        
        // Record template use
        await User.recordTemplateUse(uid, templateId);
        
        // Update template usage count
        template.usageCount = (template.usageCount || 0) + 1;
        await template.save();
        
        // Notify connected clients about the update
        websocketService.notifyTemplateUpdate(templateId, {
            usageCount: template.usageCount
        });
        
        logger.info(`User ${uid} used template ${templateId}`);
        
        return res.status(200).json({
            success: true,
            message: 'Template usage recorded successfully'
        });
    } catch (error) {
        logger.error(`Record template use error: ${error.message}`);
        res.status(500).json({
            success: false,
            message: 'Server error recording template use',
            error: error.message
        });
    }
});

/**
 * @route   POST /api/users/:uid/templates/:templateId/share
 * @desc    Record template share by user
 * @access  Private
 */
router.post('/:uid/templates/:templateId/share', validateFirebaseUid, verifyFirebaseToken, async (req, res) => {
    try {
        const { uid, templateId } = req.params;
        const { platform } = req.body;
        
        // Validate request
        const validation = validateTemplateInteraction(uid, templateId);
        if (!validation.isValid) {
            return res.status(400).json({
                success: false,
                message: 'Validation failed',
                errors: validation.errors
            });
        }
        
        logger.info(`Starting template share operation for user ${uid}, template ${templateId}`);
        
        // Get user by uid
        const user = await User.getByUid(uid);
        if (!user) {
            logger.warn(`Template share attempted for non-existent user: UID ${uid}`);
            return res.status(404).json({
                success: false,
                message: 'User not found'
            });
        }
        
        // Get template
        const template = await Template.findById(templateId);
        if (!template) {
            logger.warn(`Template share attempted for non-existent template: ${templateId}`);
            return res.status(404).json({
                success: false,
                message: 'Template not found'
            });
        }
        
        // Record template share
        await User.recordTemplateShare(uid, templateId, platform);
        
        // Update template share count
        template.shares = (template.shares || 0) + 1;
        await template.save();
        
        // Notify connected clients about the update
        websocketService.notifyTemplateUpdate(templateId, {
            shares: template.shares
        });
        
        logger.info(`User ${uid} shared template ${templateId} on ${platform}`);
        
        return res.status(200).json({
            success: true,
            message: 'Template share recorded successfully'
        });
    } catch (error) {
        logger.error(`Record template share error: ${error.message}`);
        res.status(500).json({
            success: false,
            message: 'Server error recording template share',
            error: error.message
        });
    }
});

module.exports = router;

/**
 * @route   PUT /api/users/:uid/likes/:templateId
 * @desc    Add a template to user's likes
 * @access  Private
 */
router.put('/:uid/likes/:templateId', validateFirebaseUid, verifyFirebaseToken, async (req, res) => {
    try {
        const { uid, templateId } = req.params;
        
        // Validate request
        const validation = validateTemplateInteraction(uid, templateId);
        if (!validation.isValid) {
            return res.status(400).json({
                success: false,
                message: 'Validation failed',
                errors: validation.errors
            });
        }
        
        logger.info(`Starting like operation for user ${uid}, template ${templateId}`);
        
        // Get user by uid
        const user = await User.getByUid(uid);
        if (!user) {
            logger.warn(`Like operation attempted for non-existent user: UID ${uid}`);
            return res.status(404).json({
                success: false,
                message: 'User not found'
            });
        }
        
        // Get template
        const template = await Template.findById(templateId);
        if (!template) {
            logger.warn(`Like operation attempted for non-existent template: ${templateId}`);
            return res.status(404).json({
                success: false,
                message: 'Template not found'
            });
        }
        
        // Add to likes
        const isLiked = await User.addToLikes(uid, templateId);
        
        // Update template likes count
        if (isLiked) {
            template.likes = (template.likes || 0) + 1;
            await template.save();
            
            // Notify connected clients about the update
            websocketService.notifyTemplateUpdate(templateId, {
                likes: template.likes
            });
        }
        
        logger.info(`User ${uid} ${isLiked ? 'liked' : 'unliked'} template ${templateId}`);
        
        return res.status(200).json({
            success: true,
            message: isLiked ? 'Template added to likes' : 'Template removed from likes',
            isLiked
        });
    } catch (error) {
        logger.error(`Template like error: ${error.message}`);
        res.status(500).json({
            success: false,
            message: 'Server error updating likes',
            error: error.message
        });
    }
});

module.exports = router;
