const express = require('express');
const router = express.Router();
const mongoose = require('mongoose');
const User = require('../../models/User');
const Template = require('../../models/Template');
const logger = require('../../utils/logger');
const { validateFirebaseUid } = require('../../middleware/validators');
const { verifyFirebaseToken } = require('../../middleware/auth');
const recommendationService = require('../../services/recommendationService');
const { 
    handleAsyncOperation, 
    validateTemplateInteraction, 
    safeUpdateTemplateCounts, 
    isValidObjectId 
} = require('./utils/helpers');

/**
 * @route   GET /api/users/:uid/templates/favorites
 * @desc    Get user's favorite templates
 * @access  Public
 */
router.get('/:uid/templates/favorites', async (req, res) => {
    return handleAsyncOperation(async () => {
        const { uid } = req.params;
        const { page = 1, limit = 10 } = req.query;
        
        const user = await User.findOne({ uid });
        if (!user) {
            return res.status(404).json({
                success: false,
                message: 'User not found'
            });
        }
        
        const favorites = user.favorites || [];
        
        if (favorites.length === 0) {
            return res.status(200).json({
                success: true,
                favorites: [],
                totalCount: 0,
                currentPage: parseInt(page),
                totalPages: 0
            });
        }
        
        // Paginate favorites
        const skip = (parseInt(page) - 1) * parseInt(limit);
        const paginatedFavorites = favorites.slice(skip, skip + parseInt(limit));
        
        // Populate template details
        const templates = await Template.find({
            _id: { $in: paginatedFavorites }
        }).select('name imageUrl category likes favorites shares description tags');
        
        const totalPages = Math.ceil(favorites.length / parseInt(limit));
        
        return res.status(200).json({
            success: true,
            favorites: templates,
            totalCount: favorites.length,
            currentPage: parseInt(page),
            totalPages
        });
    }, 'Get user favorites', res, req.params.uid);
});

/**
 * @route   GET /api/users/:uid/templates/likes
 * @desc    Get user's liked templates
 * @access  Public
 */
router.get('/:uid/templates/likes', async (req, res) => {
    return handleAsyncOperation(async () => {
        const { uid } = req.params;
        const { page = 1, limit = 10 } = req.query;
        
        const user = await User.findOne({ uid });
        if (!user) {
            return res.status(404).json({
                success: false,
                message: 'User not found'
            });
        }
        
        const likes = user.likes || [];
        
        if (likes.length === 0) {
            return res.status(200).json({
                success: true,
                likes: [],
                totalCount: 0,
                currentPage: parseInt(page),
                totalPages: 0
            });
        }
        
        // Paginate likes
        const skip = (parseInt(page) - 1) * parseInt(limit);
        const paginatedLikes = likes.slice(skip, skip + parseInt(limit));
        
        // Populate template details
        const templates = await Template.find({
            _id: { $in: paginatedLikes }
        }).select('name imageUrl category likes favorites shares description tags');
        
        const totalPages = Math.ceil(likes.length / parseInt(limit));
        
        return res.status(200).json({
            success: true,
            likes: templates,
            totalCount: likes.length,
            currentPage: parseInt(page),
            totalPages
        });
    }, 'Get user likes', res, req.params.uid);
});

/**
 * @route   GET /api/users/:uid/templates/recent
 * @desc    Get user's recently used templates
 * @access  Private
 */
router.get('/:uid/templates/recent', verifyFirebaseToken, async (req, res) => {
    return handleAsyncOperation(async () => {
        const { uid } = req.params;
        const { limit = 10 } = req.query;
        
        const user = await User.findOne({ uid });
        if (!user) {
            return res.status(404).json({
                success: false,
                message: 'User not found'
            });
        }
        
        const recentTemplates = user.recentTemplatesUsed || [];
        
        if (recentTemplates.length === 0) {
            return res.status(200).json({
                success: true,
                recentTemplates: []
            });
        }
        
        // Get the most recent templates (limit the number)
        const limitedRecent = recentTemplates.slice(0, parseInt(limit));
        
        // Populate template details
        const templates = await Template.find({
            _id: { $in: limitedRecent }
        }).select('name imageUrl category likes favorites shares description tags');
        
        return res.status(200).json({
            success: true,
            recentTemplates: templates
        });
    }, 'Get user recent templates', res, req.params.uid);
});

/**
 * @route   PUT /api/users/:uid/favorites/:templateId
 * @desc    Add a template to user's favorites
 * @access  Private
 */
router.put('/:uid/favorites/:templateId', validateFirebaseUid, verifyFirebaseToken, async (req, res) => {
    return handleAsyncOperation(async () => {
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

        // Start a transaction for data consistency
        const session = await mongoose.startSession();
        
        try {
            return await session.withTransaction(async () => {
                // Find user by uid
                const user = await User.findOne({ uid }).session(session);
                
                if (!user) {
                    throw new Error('User not found');
                }
                
                // Verify template exists
                const template = await Template.findById(templateId).session(session);
                if (!template) {
                    throw new Error('Template not found');
                }
                
                // Initialize favorites array if it doesn't exist
                if (!user.favorites) {
                    user.favorites = [];
                }
                
                // Check if template is already favorited
                const isAlreadyFavorited = user.favorites.some(id => id.toString() === templateId.toString());
                
                if (!isAlreadyFavorited) {
                    // Add to favorites
                    user.favorites.push(templateId);
                    
                    // Add to engagement log
                    if (!user.engagementLog) {
                        user.engagementLog = [];
                    }
                    
                    user.engagementLog.push({
                        action: 'FAVORITE',
                        templateId,
                        timestamp: Date.now()
                    });
                    
                    // Update last active template
                    user.lastActiveTemplate = templateId;
                    user.lastActionOnTemplate = 'FAVORITE';
                    
                    // Update last online
                    user.lastOnline = Date.now();
                    
                    // Save user first
                    await user.save({ session });
                    
                    // Increment template favorite count
                    await safeUpdateTemplateCounts(templateId, { favorites: 1 }, session);
                    
                    // Invalidate recommendations
                    await recommendationService.invalidateUserRecommendations(uid);
                    
                    logger.info(`User ${uid} favorited template ${templateId}`);
                    
                    return res.status(200).json({
                        success: true,
                        message: 'Template favorited successfully',
                        data: {
                            templateId,
                            isFavorited: true,
                            totalFavorites: user.favorites.length
                        }
                    });
                } else {
                    // Template already favorited
                    return res.status(200).json({
                        success: true,
                        message: 'Template already favorited',
                        data: {
                            templateId,
                            isFavorited: true,
                            totalFavorites: user.favorites.length
                        }
                    });
                }
            });
        } finally {
            await session.endSession();
        }
    }, 'Add favorite', res, uid);
});

/**
 * @route   DELETE /api/users/:uid/favorites/:templateId
 * @desc    Remove a template from user's favorites
 * @access  Private
 */
router.delete('/:uid/favorites/:templateId', validateFirebaseUid, verifyFirebaseToken, async (req, res) => {
    return handleAsyncOperation(async () => {
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

        // Start a transaction for data consistency
        const session = await mongoose.startSession();
        
        try {
            return await session.withTransaction(async () => {
                // Find user by uid
                const user = await User.findOne({ uid }).session(session);
                
                if (!user) {
                    throw new Error('User not found');
                }
                
                // Initialize favorites array if it doesn't exist
                if (!user.favorites) {
                    user.favorites = [];
                }
                
                // Check if template is favorited
                const favoriteIndex = user.favorites.findIndex(id => id.toString() === templateId.toString());
                
                if (favoriteIndex !== -1) {
                    // Remove from favorites
                    user.favorites.splice(favoriteIndex, 1);
                    
                    // Add to engagement log
                    if (!user.engagementLog) {
                        user.engagementLog = [];
                    }
                    
                    user.engagementLog.push({
                        action: 'UNFAVORITE',
                        templateId,
                        timestamp: Date.now()
                    });
                    
                    // Update last online
                    user.lastOnline = Date.now();
                    
                    // Save user first
                    await user.save({ session });
                    
                    // Decrement template favorite count
                    await safeUpdateTemplateCounts(templateId, { favorites: -1 }, session);
                    
                    // Invalidate recommendations
                    await recommendationService.invalidateUserRecommendations(uid);
                    
                    logger.info(`User ${uid} unfavorited template ${templateId}`);
                    
                    return res.status(200).json({
                        success: true,
                        message: 'Template removed from favorites successfully',
                        data: {
                            templateId,
                            isFavorited: false,
                            totalFavorites: user.favorites.length
                        }
                    });
                } else {
                    // Template not favorited
                    return res.status(200).json({
                        success: true,
                        message: 'Template was not favorited',
                        data: {
                            templateId,
                            isFavorited: false,
                            totalFavorites: user.favorites.length
                        }
                    });
                }
            });
        } finally {
            await session.endSession();
        }
    }, 'Remove favorite', res, uid);
});

/**
 * @route   PUT /api/users/:uid/likes/:templateId
 * @desc    Add a template to user's likes
 * @access  Private
 */
router.put('/:uid/likes/:templateId', validateFirebaseUid, verifyFirebaseToken, async (req, res) => {
    return handleAsyncOperation(async () => {
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

        // Simplified version without transactions for debugging
        try {
            // Find user by uid
            const user = await User.findOne({ uid });
            
            if (!user) {
                return res.status(404).json({
                    success: false,
                    message: 'User not found'
                });
            }
            
            // Verify template exists
            const template = await Template.findById(templateId);
            if (!template) {
                return res.status(404).json({
                    success: false,
                    message: 'Template not found'
                });
            }
            
            // Initialize likes array if it doesn't exist
            if (!user.likes) {
                user.likes = [];
            }
            
            // Check if template is already liked
            const isAlreadyLiked = user.likes.some(id => id.toString() === templateId.toString());
            
            if (!isAlreadyLiked) {
                // Add to likes
                user.likes.push(templateId);
                
                // Add to engagement log
                if (!user.engagementLog) {
                    user.engagementLog = [];
                }
                
                user.engagementLog.push({
                    action: 'LIKE',
                    templateId,
                    timestamp: Date.now()
                });
                
                // Update last active template
                user.lastActiveTemplate = templateId;
                user.lastActionOnTemplate = 'LIKE';
                
                // Update last online
                user.lastOnline = Date.now();
                
                // Save user first
                await user.save();
                
                // Increment template like count (simplified without transaction)
                try {
                    await Template.findByIdAndUpdate(
                        templateId,
                        { $inc: { likes: 1 } },
                        { new: true }
                    );
                } catch (templateError) {
                    logger.warn(`Failed to update template like count: ${templateError.message}`);
                    // Continue anyway - user like was saved
                }
                
                // Invalidate recommendations (but don't fail if it errors)
                try {
                    await recommendationService.invalidateUserRecommendations(uid);
                } catch (recError) {
                    logger.warn(`Failed to invalidate recommendations: ${recError.message}`);
                    // Continue anyway
                }
                
                logger.info(`User ${uid} liked template ${templateId}`);
                
                return res.status(200).json({
                    success: true,
                    message: 'Template liked successfully',
                    data: {
                        templateId,
                        isLiked: true,
                        totalLikes: user.likes.length
                    }
                });
            } else {
                // Template already liked
                return res.status(200).json({
                    success: true,
                    message: 'Template already liked',
                    data: {
                        templateId,
                        isLiked: true,
                        totalLikes: user.likes.length
                    }
                });
            }
        } catch (error) {
            logger.error(`Like operation error: ${error.message}`);
            throw error; // Let handleAsyncOperation handle it
        }
    }, 'Add like', res, uid);
});

/**
 * @route   DELETE /api/users/:uid/likes/:templateId
 * @desc    Remove a template from user's likes
 * @access  Private
 */
router.delete('/:uid/likes/:templateId', validateFirebaseUid, verifyFirebaseToken, async (req, res) => {
    return handleAsyncOperation(async () => {
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

        // Simplified version without transactions for debugging
        try {
            // Find user by uid
            const user = await User.findOne({ uid });
            
            if (!user) {
                return res.status(404).json({
                    success: false,
                    message: 'User not found'
                });
            }
            
            // Initialize likes array if it doesn't exist
            if (!user.likes) {
                user.likes = [];
            }
            
            // Check if template is liked
            const likeIndex = user.likes.findIndex(id => id.toString() === templateId.toString());
            
            if (likeIndex !== -1) {
                // Remove from likes
                user.likes.splice(likeIndex, 1);
                
                // Add to engagement log
                if (!user.engagementLog) {
                    user.engagementLog = [];
                }
                
                user.engagementLog.push({
                    action: 'UNLIKE',
                    templateId,
                    timestamp: Date.now()
                });
                
                // Update last online
                user.lastOnline = Date.now();
                
                // Save user first
                await user.save();
                
                // Decrement template like count (simplified without transaction)
                try {
                    await Template.findByIdAndUpdate(
                        templateId,
                        { $inc: { likes: -1 } },
                        { new: true }
                    );
                } catch (templateError) {
                    logger.warn(`Failed to update template like count: ${templateError.message}`);
                    // Continue anyway - user unlike was saved
                }
                
                // Invalidate recommendations (but don't fail if it errors)
                try {
                    await recommendationService.invalidateUserRecommendations(uid);
                } catch (recError) {
                    logger.warn(`Failed to invalidate recommendations: ${recError.message}`);
                    // Continue anyway
                }
                
                logger.info(`User ${uid} unliked template ${templateId}`);
                
                return res.status(200).json({
                    success: true,
                    message: 'Template unliked successfully',
                    data: {
                        templateId,
                        isLiked: false,
                        totalLikes: user.likes.length
                    }
                });
            } else {
                // Template not liked
                return res.status(200).json({
                    success: true,
                    message: 'Template was not liked',
                    data: {
                        templateId,
                        isLiked: false,
                        totalLikes: user.likes.length
                    }
                });
            }
        } catch (error) {
            logger.error(`Unlike operation error: ${error.message}`);
            throw error; // Let handleAsyncOperation handle it
        }
    }, 'Remove like', res, uid);
});

/**
 * @route   POST /api/users/:uid/templates/:templateId/share
 * @desc    Record a template share
 * @access  Private
 */
router.post('/:uid/templates/:templateId/share', validateFirebaseUid, verifyFirebaseToken, async (req, res) => {
    return handleAsyncOperation(async () => {
        const { uid, templateId } = req.params;
        const { platform, method } = req.body;
        
        // Validate request
        const validation = validateTemplateInteraction(uid, templateId);
        if (!validation.isValid) {
            return res.status(400).json({
                success: false,
                message: 'Validation failed',
                errors: validation.errors
            });
        }

        // Start a transaction for data consistency
        const session = await mongoose.startSession();
        
        try {
            return await session.withTransaction(async () => {
                // Find user by uid
                const user = await User.findOne({ uid }).session(session);
                
                if (!user) {
                    throw new Error('User not found');
                }
                
                // Verify template exists
                const template = await Template.findById(templateId).session(session);
                if (!template) {
                    throw new Error('Template not found');
                }
                
                // Add to engagement log
                if (!user.engagementLog) {
                    user.engagementLog = [];
                }
                
                user.engagementLog.push({
                    action: 'SHARE',
                    templateId,
                    platform: platform || 'unknown',
                    method: method || 'unknown',
                    timestamp: Date.now()
                });
                
                // Update last active template
                user.lastActiveTemplate = templateId;
                user.lastActionOnTemplate = 'SHARE';
                
                // Update last online
                user.lastOnline = Date.now();
                
                // Save user first
                await user.save({ session });
                
                // Increment template share count
                await safeUpdateTemplateCounts(templateId, { shares: 1 }, session);
                
                // Invalidate recommendations
                await recommendationService.invalidateUserRecommendations(uid);
                
                logger.info(`User ${uid} shared template ${templateId} via ${platform || 'unknown'}`);
                
                return res.status(200).json({
                    success: true,
                    message: 'Template share recorded successfully',
                    data: {
                        templateId,
                        platform: platform || 'unknown',
                        method: method || 'unknown',
                        timestamp: Date.now()
                    }
                });
            });
        } finally {
            await session.endSession();
        }
    }, 'Record share', res, uid);
});

/**
 * @route   GET /api/users/:uid/templates/:templateId/interaction-status
 * @desc    Get user's interaction status with a specific template
 * @access  Private
 */
router.get('/:uid/templates/:templateId/interaction-status', verifyFirebaseToken, async (req, res) => {
    return handleAsyncOperation(async () => {
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
        
        const user = await User.findOne({ uid });
        if (!user) {
            return res.status(404).json({
                success: false,
                message: 'User not found'
            });
        }
        
        const isLiked = user.likes && user.likes.some(id => id.toString() === templateId.toString());
        const isFavorited = user.favorites && user.favorites.some(id => id.toString() === templateId.toString());
        
        // Check if template was recently used
        const isRecentlyUsed = user.recentTemplatesUsed && 
            user.recentTemplatesUsed.some(id => id.toString() === templateId.toString());
        
        // Get last interaction from engagement log
        let lastInteraction = null;
        if (user.engagementLog) {
            const templateInteractions = user.engagementLog
                .filter(log => log.templateId && log.templateId.toString() === templateId.toString())
                .sort((a, b) => b.timestamp - a.timestamp);
            
            if (templateInteractions.length > 0) {
                lastInteraction = templateInteractions[0];
            }
        }
        
        return res.status(200).json({
            success: true,
            data: {
                templateId,
                isLiked,
                isFavorited,
                isRecentlyUsed,
                lastInteraction
            }
        });
    }, 'Get interaction status', res, uid);
});

module.exports = router; 