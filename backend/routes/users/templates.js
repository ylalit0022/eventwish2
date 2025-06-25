const express = require('express');
const router = express.Router();
const mongoose = require('mongoose');
const User = require('../../models/User');
const Template = require('../../models/Template');
const logger = require('../../utils/logger');
const { validateFirebaseUid } = require('../../middleware/validators');
const { verifyFirebaseToken } = require('../../middleware/auth');

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
    console.log('🚀 ENHANCED FAVORITE ROUTE CALLED 🚀');
    console.log('UID:', req.params.uid);
    console.log('Template ID:', req.params.templateId);
    
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

        // Enhanced error handling and logging for debugging server crashes
        logger.info(`Starting favorite operation for user ${uid}, template ${templateId}`);
        
        try {
            // Find user by uid with error handling
            let user;
            try {
                user = await User.findOne({ uid });
                logger.info(`User lookup completed for ${uid}: ${user ? 'found' : 'not found'}`);
            } catch (userError) {
                logger.error(`User lookup failed for ${uid}: ${userError.message}`);
                return res.status(500).json({
                    success: false,
                    message: 'Database error during user lookup',
                    error: userError.message
                });
            }
            
            if (!user) {
                logger.warn(`User not found: ${uid}`);
                return res.status(404).json({
                    success: false,
                    message: 'User not found'
                });
            }
            
            // Verify template exists with error handling
            let template;
            try {
                template = await Template.findById(templateId);
                logger.info(`Template lookup completed for ${templateId}: ${template ? 'found' : 'not found'}`);
            } catch (templateError) {
                logger.error(`Template lookup failed for ${templateId}: ${templateError.message}`);
                return res.status(500).json({
                    success: false,
                    message: 'Database error during template lookup',
                    error: templateError.message
                });
            }
            
            if (!template) {
                logger.warn(`Template not found: ${templateId}`);
                return res.status(404).json({
                    success: false,
                    message: 'Template not found'
                });
            }
            
            // Initialize favorites array if it doesn't exist
            if (!user.favorites) {
                user.favorites = [];
                logger.info(`Initialized favorites array for user ${uid}`);
            }
            
            // Check if template is already favorited
            const isAlreadyFavorited = user.favorites.some(id => id.toString() === templateId.toString());
            logger.info(`Favorite check for user ${uid}, template ${templateId}: already favorited = ${isAlreadyFavorited}`);
            
            if (!isAlreadyFavorited) {
                // Add to favorites
                user.favorites.push(templateId);
                logger.info(`Added template ${templateId} to user ${uid} favorites`);
                
                // Add to engagement log safely
                try {
                    if (!user.engagementLog) {
                        user.engagementLog = [];
                    }
                    
                    user.engagementLog.push({
                        action: 'FAVORITE',
                        templateId,
                        timestamp: Date.now()
                    });
                    logger.info(`Added engagement log entry for user ${uid}`);
                } catch (engagementError) {
                    logger.warn(`Failed to add engagement log for user ${uid}: ${engagementError.message}`);
                    // Continue anyway
                }
                
                // Update user metadata safely
                try {
                    user.lastActiveTemplate = templateId;
                    user.lastActionOnTemplate = 'FAVORITE';
                    user.lastOnline = Date.now();
                    logger.info(`Updated user metadata for ${uid}`);
                } catch (metadataError) {
                    logger.warn(`Failed to update user metadata for ${uid}: ${metadataError.message}`);
                    // Continue anyway
                }
                
                // Save user changes with error handling
                try {
                    await user.save();
                    logger.info(`Saved user ${uid} changes successfully`);
                } catch (saveError) {
                    logger.error(`Failed to save user ${uid} changes: ${saveError.message}`);
                    return res.status(500).json({
                        success: false,
                        message: 'Failed to save user changes',
                        error: saveError.message
                    });
                }
                
                // 📊 NEW: Update template favorite count with enhanced error handling
                try {
                    const updatedTemplate = await Template.findByIdAndUpdate(
                        templateId,
                        { $inc: { favorites: 1 } },
                        { new: true }
                    );
                    logger.info(`📈 Template ${templateId} favorite count incremented successfully. New count: ${updatedTemplate.favorites}`);
                } catch (templateError) {
                    logger.error(`❌ Failed to update template favorite count for ${templateId}: ${templateError.message}`);
                    // Don't fail the request - user favorite was saved
                }
                
                logger.info(`User ${uid} favorited template ${templateId}`);
                
                // In the favorite route, add more detailed logging
                logger.info(`User lookup result: ${user ? 'User found' : 'User NOT found'}`, { 
                    uid, 
                    userExists: !!user 
                });

                logger.info(`Template lookup result: ${template ? 'Template found' : 'Template NOT found'}`, { 
                    templateId, 
                    templateExists: !!template 
                });

                // Log favorites array before modification
                logger.info(`User favorites BEFORE modification: ${JSON.stringify(user.favorites)}`, { 
                    uid, 
                    currentFavorites: user.favorites 
                });

                // After adding to favorites
                logger.info(`User favorites AFTER modification: ${JSON.stringify(user.favorites)}`, { 
                    uid, 
                    updatedFavorites: user.favorites 
                });
                
                // In the favorite route, after user and template lookup
                logger.info('🔬 FAVORITE OPERATION DETAILS', {
                    userFavorites: user.favorites ? user.favorites.map(f => f.toString()) : 'No favorites',
                    isAlreadyFavorited: isAlreadyFavorited,
                    templateId: templateId,
                    uid: uid
                });
                
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
                logger.info(`Template ${templateId} already favorited by user ${uid}`);
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
        } catch (error) {
            logger.error(`Critical error in favorite operation for user ${uid}, template ${templateId}: ${error.message}`);
            logger.error(`Error stack: ${error.stack}`);
            
            // Return a safe error response
            return res.status(500).json({
                success: false,
                message: 'Internal server error during favorite operation',
                error: error.message
            });
        }
    } catch (error) {
        logger.error(`Critical error in favorite route for user ${req.params.uid}: ${error.message}`);
        return res.status(500).json({
            success: false,
            message: 'Internal server error',
            error: error.message
        });
    }
});

/**
 * @route   DELETE /api/users/:uid/favorites/:templateId
 * @desc    Remove a template from user's favorites
 * @access  Private
 */
router.delete('/:uid/favorites/:templateId', validateFirebaseUid, verifyFirebaseToken, async (req, res) => {
    console.log('🚀 ENHANCED UNFAVORITE ROUTE CALLED 🚀');
    console.log('UID:', req.params.uid);
    console.log('Template ID:', req.params.templateId);
    
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

        // Enhanced error handling and logging for debugging server crashes
        logger.info(`Starting unfavorite operation for user ${uid}, template ${templateId}`);
        
        try {
            // Find user by uid with error handling
            let user;
            try {
                user = await User.findOne({ uid });
                logger.info(`User lookup completed for ${uid}: ${user ? 'found' : 'not found'}`);
            } catch (userError) {
                logger.error(`User lookup failed for ${uid}: ${userError.message}`);
                return res.status(500).json({
                    success: false,
                    message: 'Database error during user lookup',
                    error: userError.message
                });
            }
            
            if (!user) {
                logger.warn(`User not found: ${uid}`);
                return res.status(404).json({
                    success: false,
                    message: 'User not found'
                });
            }
            
            // Verify template exists with error handling
            let template;
            try {
                template = await Template.findById(templateId);
                logger.info(`Template lookup completed for ${templateId}: ${template ? 'found' : 'not found'}`);
            } catch (templateError) {
                logger.error(`Template lookup failed for ${templateId}: ${templateError.message}`);
                return res.status(500).json({
                    success: false,
                    message: 'Database error during template lookup',
                    error: templateError.message
                });
            }
            
            if (!template) {
                logger.warn(`Template not found: ${templateId}`);
                return res.status(404).json({
                    success: false,
                    message: 'Template not found'
                });
            }
            
            // Check if template is in favorites
            const favoriteIndex = user.favorites ? user.favorites.findIndex(id => id.toString() === templateId.toString()) : -1;
            logger.info(`Favorite check for user ${uid}, template ${templateId}: found at index ${favoriteIndex}`);
            
            if (favoriteIndex !== -1) {
                // Remove from favorites
                user.favorites.splice(favoriteIndex, 1);
                logger.info(`Removed template ${templateId} from user ${uid} favorites`);
                
                // Add to engagement log safely
                try {
                    if (!user.engagementLog) {
                        user.engagementLog = [];
                    }
                    
                    user.engagementLog.push({
                        action: 'UNFAVORITE',
                        templateId,
                        timestamp: Date.now()
                    });
                    logger.info(`Added engagement log entry for user ${uid}`);
                } catch (engagementError) {
                    logger.warn(`Failed to add engagement log for user ${uid}: ${engagementError.message}`);
                    // Continue anyway
                }
                
                // Update user metadata safely
                try {
                    user.lastActiveTemplate = templateId;
                    user.lastActionOnTemplate = 'UNFAVORITE';
                    user.lastOnline = Date.now();
                    logger.info(`Updated user metadata for ${uid}`);
                } catch (metadataError) {
                    logger.warn(`Failed to update user metadata for ${uid}: ${metadataError.message}`);
                    // Continue anyway
                }
                
                // Save user changes with error handling
                try {
                    await user.save();
                    logger.info(`Saved user ${uid} changes successfully`);
                } catch (saveError) {
                    logger.error(`Failed to save user ${uid} changes: ${saveError.message}`);
                    return res.status(500).json({
                        success: false,
                        message: 'Failed to save user changes',
                        error: saveError.message
                    });
                }
                
                // 📊 NEW: Update template favorite count with enhanced error handling
                try {
                    const updatedTemplate = await Template.findByIdAndUpdate(
                        templateId,
                        { $inc: { favorites: -1 } },
                        { new: true }
                    );
                    logger.info(`📉 Template ${templateId} favorite count decremented successfully. New count: ${updatedTemplate.favorites}`);
                } catch (templateError) {
                    logger.error(`❌ Failed to update template favorite count for ${templateId}: ${templateError.message}`);
                    // Don't fail the request - user unfavorite was saved
                }
                
                logger.info(`User ${uid} unfavorited template ${templateId}`);
                
                // In the unfavorite route, add comparable logging
                logger.info(`User favorites BEFORE removal: ${JSON.stringify(user.favorites)}`, { 
                    uid, 
                    currentFavorites: user.favorites 
                });

                // After removing from favorites
                logger.info(`User favorites AFTER removal: ${JSON.stringify(user.favorites)}`, { 
                    uid, 
                    updatedFavorites: user.favorites 
                });
                
                // In the favorite route, after user and template lookup
                logger.info('🔬 FAVORITE OPERATION DETAILS', {
                    userFavorites: user.favorites ? user.favorites.map(f => f.toString()) : 'No favorites',
                    isAlreadyFavorited: false,
                    templateId: templateId,
                    uid: uid
                });
                
                return res.status(200).json({
                    success: true,
                    message: 'Template unfavorited successfully',
                    data: {
                        templateId,
                        isFavorited: false,
                        totalFavorites: user.favorites.length
                    }
                });
            } else {
                // Template not in favorites
                logger.info(`Template ${templateId} not found in user ${uid} favorites`);
                return res.status(200).json({
                    success: true,
                    message: 'Template was not favorited',
                    data: {
                        templateId,
                        isFavorited: false,
                        totalFavorites: user.favorites ? user.favorites.length : 0
                    }
                });
            }
        } catch (error) {
            logger.error(`Critical error in unfavorite operation for user ${uid}, template ${templateId}: ${error.message}`);
            logger.error(`Error stack: ${error.stack}`);
            
            // Return a safe error response
            return res.status(500).json({
                success: false,
                message: 'Internal server error during unfavorite operation',
                error: error.message
            });
        }
    } catch (error) {
        logger.error(`Critical error in unfavorite route for user ${req.params.uid}: ${error.message}`);
        return res.status(500).json({
            success: false,
            message: 'Internal server error',
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
    console.log('🚀 ENHANCED LIKE ROUTE CALLED 🚀');
    console.log('UID:', req.params.uid);
    console.log('Template ID:', req.params.templateId);
    console.log('Method:', req.method);
    console.log('Path:', req.path);
    console.log('Headers:', JSON.stringify(req.headers, null, 2));
    
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

        // Enhanced error handling and logging for debugging server crashes
        logger.info(`Starting like operation for user ${uid}, template ${templateId}`);
        
        try {
            // Find user by uid with error handling
            let user;
            try {
                user = await User.findOne({ uid });
                logger.info(`User lookup completed for ${uid}: ${user ? 'found' : 'not found'}`);
            } catch (userError) {
                logger.error(`User lookup failed for ${uid}: ${userError.message}`);
                return res.status(500).json({
                    success: false,
                    message: 'Database error during user lookup',
                    error: userError.message
                });
            }
            
            if (!user) {
                logger.warn(`User not found: ${uid}`);
                return res.status(404).json({
                    success: false,
                    message: 'User not found'
                });
            }
            
            // Verify template exists with error handling
            let template;
            try {
                template = await Template.findById(templateId);
                logger.info(`Template lookup completed for ${templateId}: ${template ? 'found' : 'not found'}`);
            } catch (templateError) {
                logger.error(`Template lookup failed for ${templateId}: ${templateError.message}`);
                return res.status(500).json({
                    success: false,
                    message: 'Database error during template lookup',
                    error: templateError.message
                });
            }
            
            if (!template) {
                logger.warn(`Template not found: ${templateId}`);
                return res.status(404).json({
                    success: false,
                    message: 'Template not found'
                });
            }
            
            // Initialize likes array if it doesn't exist
            if (!user.likes) {
                user.likes = [];
                logger.info(`Initialized likes array for user ${uid}`);
            }
            
            // Check if template is already liked
            const isAlreadyLiked = user.likes.some(id => id.toString() === templateId.toString());
            logger.info(`Like check for user ${uid}, template ${templateId}: already liked = ${isAlreadyLiked}`);
            
            if (!isAlreadyLiked) {
                // Add to likes
                user.likes.push(templateId);
                logger.info(`Added template ${templateId} to user ${uid} likes`);
                
                // Add to engagement log safely
                try {
                    if (!user.engagementLog) {
                        user.engagementLog = [];
                    }
                    
                    user.engagementLog.push({
                        action: 'LIKE',
                        templateId,
                        timestamp: Date.now()
                    });
                    logger.info(`Added engagement log entry for user ${uid}`);
                } catch (engagementError) {
                    logger.warn(`Failed to add engagement log for user ${uid}: ${engagementError.message}`);
                    // Continue anyway
                }
                
                // Update user metadata safely
                try {
                    user.lastActiveTemplate = templateId;
                    user.lastActionOnTemplate = 'LIKE';
                    user.lastOnline = Date.now();
                    logger.info(`Updated user metadata for ${uid}`);
                } catch (metadataError) {
                    logger.warn(`Failed to update user metadata for ${uid}: ${metadataError.message}`);
                    // Continue anyway
                }
                
                // Save user with enhanced error handling
                try {
                    await user.save();
                    logger.info(`User ${uid} saved successfully after like operation`);
                } catch (saveError) {
                    logger.error(`Failed to save user ${uid}: ${saveError.message}`);
                    return res.status(500).json({
                        success: false,
                        message: 'Failed to save user like',
                        error: saveError.message
                    });
                }
                
                // 📊 NEW: Update template like count with enhanced error handling
                try {
                    const updatedTemplate = await Template.findByIdAndUpdate(
                        templateId,
                        { $inc: { likes: 1 } },
                        { new: true }
                    );
                    logger.info(`👍 Template ${templateId} like count incremented successfully. New count: ${updatedTemplate.likes}`);
                } catch (templateError) {
                    logger.error(`❌ Failed to update template like count for ${templateId}: ${templateError.message}`);
                    // Don't fail the request - user like was saved
                }
                
                logger.info(`Like operation completed successfully for user ${uid}, template ${templateId}`);
                
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
                logger.info(`Template ${templateId} already liked by user ${uid}`);
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
            logger.error(`Critical error in like operation for user ${uid}, template ${templateId}: ${error.message}`);
            logger.error(`Error stack: ${error.stack}`);
            
            // Return a safe error response
            return res.status(500).json({
                success: false,
                message: 'Internal server error during like operation',
                error: error.message
            });
        }
    } catch (error) {
        logger.error(`Critical error in like route for user ${req.params.uid}: ${error.message}`);
        return res.status(500).json({
            success: false,
            message: 'Internal server error',
            error: error.message
        });
    }
});

/**
 * @route   DELETE /api/users/:uid/likes/:templateId
 * @desc    Remove a template from user's likes
 * @access  Private
 */
router.delete('/:uid/likes/:templateId', validateFirebaseUid, verifyFirebaseToken, async (req, res) => {
    console.log('🚀 ENHANCED UNLIKE ROUTE CALLED 🚀');
    console.log('UID:', req.params.uid);
    console.log('Template ID:', req.params.templateId);
    
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

        // Enhanced error handling and logging for debugging server crashes
        logger.info(`Starting unlike operation for user ${uid}, template ${templateId}`);
        
        try {
            // Find user by uid with error handling
            let user;
            try {
                user = await User.findOne({ uid });
                logger.info(`User lookup completed for ${uid}: ${user ? 'found' : 'not found'}`);
            } catch (userError) {
                logger.error(`User lookup failed for ${uid}: ${userError.message}`);
                return res.status(500).json({
                    success: false,
                    message: 'Database error during user lookup',
                    error: userError.message
                });
            }
            
            if (!user) {
                logger.warn(`User not found: ${uid}`);
                return res.status(404).json({
                    success: false,
                    message: 'User not found'
                });
            }
            
            // Initialize likes array if it doesn't exist
            if (!user.likes) {
                user.likes = [];
                logger.info(`Initialized likes array for user ${uid}`);
            }
            
            // Check if template is liked
            const likeIndex = user.likes.findIndex(id => id.toString() === templateId.toString());
            logger.info(`Unlike check for user ${uid}, template ${templateId}: like index = ${likeIndex}`);
            
            if (likeIndex !== -1) {
                // Remove from likes
                user.likes.splice(likeIndex, 1);
                logger.info(`Removed template ${templateId} from user ${uid} likes`);
                
                // Add to engagement log safely
                try {
                    if (!user.engagementLog) {
                        user.engagementLog = [];
                    }
                    
                    user.engagementLog.push({
                        action: 'UNLIKE',
                        templateId,
                        timestamp: Date.now()
                    });
                    logger.info(`Added unlike engagement log entry for user ${uid}`);
                } catch (engagementError) {
                    logger.warn(`Failed to add engagement log for user ${uid}: ${engagementError.message}`);
                    // Continue anyway
                }
                
                // Update user metadata safely
                try {
                    user.lastOnline = Date.now();
                    logger.info(`Updated user metadata for ${uid}`);
                } catch (metadataError) {
                    logger.warn(`Failed to update user metadata for ${uid}: ${metadataError.message}`);
                    // Continue anyway
                }
                
                // Save user with enhanced error handling
                try {
                    await user.save();
                    logger.info(`User ${uid} saved successfully after unlike operation`);
                } catch (saveError) {
                    logger.error(`Failed to save user ${uid}: ${saveError.message}`);
                    return res.status(500).json({
                        success: false,
                        message: 'Failed to save user unlike',
                        error: saveError.message
                    });
                }
                
                // 📊 NEW: Update template like count with enhanced error handling
                try {
                    const updatedTemplate = await Template.findByIdAndUpdate(
                        templateId,
                        { $inc: { likes: -1 } },
                        { new: true }
                    );
                    logger.info(`👎 Template ${templateId} like count decremented successfully. New count: ${updatedTemplate.likes}`);
                } catch (templateError) {
                    logger.error(`❌ Failed to update template like count for ${templateId}: ${templateError.message}`);
                    // Don't fail the request - user unlike was saved
                }
                
                // Invalidate recommendations (but don't fail if it errors)
                try {
                    // Note: Recommendation system removed
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
            logger.error(`Critical error in unlike operation for user ${uid}, template ${templateId}: ${error.message}`);
            logger.error(`Error stack: ${error.stack}`);
            
            return res.status(500).json({
                success: false,
                message: 'Internal server error during unlike operation',
                error: error.message
            });
        }
    } catch (error) {
        logger.error(`Critical error in unlike route for user ${req.params.uid}: ${error.message}`);
        return res.status(500).json({
            success: false,
            message: 'Internal server error',
            error: error.message
        });
    }
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
                    // Note: Recommendation system removed
                
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
