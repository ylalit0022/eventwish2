const express = require('express');
const router = express.Router();
const { 
    getTemplates, 
    getTemplatesByCategory, 
    getTemplateById,
    duplicateTemplate
} = require('../controllers/templateController');
const recommendationService = require('../services/recommendationService');
const logger = require('../utils/logger');
const Template = require('../models/Template');
const User = require('../models/User');
const { handleError, handleNotFound } = require('../utils/errorHandler');
const { validateObjectId } = require('../utils/validators');

// Get all templates with pagination
router.get('/', getTemplates);

// Get templates by category
router.get('/category/:category', getTemplatesByCategory);

// Get personalized recommendations based on user history
router.get('/recommendations/:deviceId', async (req, res) => {
    try {
        const { deviceId } = req.params;
        const limit = parseInt(req.query.limit) || 10;
        
        if (!deviceId) {
            return res.status(400).json({
                success: false,
                message: 'Device ID is required'
            });
        }

        logger.info(`Getting personalized recommendations for device: ${deviceId}`);
        const recommendations = await recommendationService.getRecommendationsForUser(deviceId, limit);
        
        res.json({
            success: true,
            recommendations,
            isDefault: recommendations.isDefault || false
        });
    } catch (error) {
        logger.error(`Error getting personalized recommendations: ${error.message}`);
        res.status(500).json({
            success: false,
            message: 'Error generating recommendations',
            error: error.message
        });
    }
});

// Duplicate a template
router.post('/:id/duplicate', duplicateTemplate);

// Get template by ID
router.get('/:id', getTemplateById);

/**
 * Get creator profile for a specific template
 * GET /api/templates/:templateId/creator
 * 
 * Returns creator profile information including name and profile photo
 * Uses uid fields for efficient lookups with ObjectId fallback
 */
router.get('/:templateId/creator', async (req, res) => {
    try {
        const { templateId } = req.params;

        // Validate templateId
        if (!validateObjectId(templateId)) {
            logger.warn(`Invalid template ID provided: ${templateId}`);
            return res.status(400).json({
                success: false,
                message: 'Invalid template ID format'
            });
        }

        logger.info(`Fetching creator profile for template: ${templateId}`);

        // Find the template
        const template = await Template.findById(templateId);
        if (!template) {
            logger.warn(`Template not found: ${templateId}`);
            return handleNotFound(res, 'Template', templateId);
        }

        let creatorProfile = null;
        let creatorSource = 'default';

        // Priority 1: Try to get creator using creatorUid (most efficient)
        if (template.creatorUid) {
            try {
                const creator = await User.findOne({ uid: template.creatorUid }).select('uid displayName email profilePhoto');
                if (creator) {
                    creatorProfile = {
                        creatorId: creator._id,
                        creatorUid: creator.uid,
                        creatorName: creator.displayName || creator.email?.split('@')[0] || 'User',
                        creatorProfilePhoto: creator.profilePhoto || null,
                        creatorEmail: creator.email || null
                    };
                    creatorSource = 'creatorUid';
                    logger.info(`Found creator via creatorUid: ${template.creatorUid}`);
                }
            } catch (error) {
                logger.warn(`Error fetching creator by creatorUid ${template.creatorUid}: ${error.message}`);
            }
        }

        // Priority 2: Try to get creator using creatorId (ObjectId fallback)
        if (!creatorProfile && template.creatorId && validateObjectId(template.creatorId)) {
            try {
                const creator = await User.findById(template.creatorId).select('uid displayName email profilePhoto');
                if (creator) {
                    creatorProfile = {
                        creatorId: template.creatorId,
                        creatorUid: creator.uid,
                        creatorName: creator.displayName || creator.email?.split('@')[0] || 'User',
                        creatorProfilePhoto: creator.profilePhoto || null,
                        creatorEmail: creator.email || null
                    };
                    creatorSource = 'creatorId';
                    logger.info(`Found creator via creatorId: ${template.creatorId}`);
                }
            } catch (error) {
                logger.warn(`Error fetching creator by creatorId ${template.creatorId}: ${error.message}`);
            }
        }

        // Priority 3: Try to get creator using generatedByUserUid
        if (!creatorProfile && template.generatedByUserUid) {
            try {
                const generator = await User.findOne({ uid: template.generatedByUserUid }).select('uid displayName email profilePhoto');
                if (generator) {
                    creatorProfile = {
                        creatorId: generator._id,
                        creatorUid: generator.uid,
                        creatorName: generator.displayName || generator.email?.split('@')[0] || 'User',
                        creatorProfilePhoto: generator.profilePhoto || null,
                        creatorEmail: generator.email || null
                    };
                    creatorSource = 'generatedByUserUid';
                    logger.info(`Found creator via generatedByUserUid: ${template.generatedByUserUid}`);
                }
            } catch (error) {
                logger.warn(`Error fetching creator by generatedByUserUid ${template.generatedByUserUid}: ${error.message}`);
            }
        }

        // Priority 4: Try to get creator using generatedByUser (ObjectId fallback)
        if (!creatorProfile && template.generatedByUser && validateObjectId(template.generatedByUser)) {
            try {
                const generator = await User.findById(template.generatedByUser).select('uid displayName email profilePhoto');
                if (generator) {
                    creatorProfile = {
                        creatorId: template.generatedByUser,
                        creatorUid: generator.uid,
                        creatorName: generator.displayName || generator.email?.split('@')[0] || 'User',
                        creatorProfilePhoto: generator.profilePhoto || null,
                        creatorEmail: generator.email || null
                    };
                    creatorSource = 'generatedByUser';
                    logger.info(`Found creator via generatedByUser: ${template.generatedByUser}`);
                }
            } catch (error) {
                logger.warn(`Error fetching creator by generatedByUser ${template.generatedByUser}: ${error.message}`);
            }
        }

        // Priority 5: Default fallback to app branding
        if (!creatorProfile) {
            creatorProfile = {
                creatorId: null,
                creatorUid: null,
                creatorName: 'eventwish',
                creatorProfilePhoto: null, // Client will use app logo
                creatorEmail: null
            };
            creatorSource = 'default';
            logger.info(`Using default creator profile for template: ${templateId}`);
        }

        // Return the creator profile information
        res.json({
            success: true,
            data: {
                templateId: templateId,
                creatorProfile: creatorProfile,
                creatorSource: creatorSource,
                metadata: {
                    hasCreatorUid: !!template.creatorUid,
                    hasCreatorId: !!template.creatorId,
                    hasGeneratedByUserUid: !!template.generatedByUserUid,
                    hasGeneratedByUser: !!template.generatedByUser,
                    templateTitle: template.title,
                    templateCategory: template.category
                }
            }
        });

        logger.info(`Creator profile retrieved successfully for template ${templateId} via ${creatorSource}`);

    } catch (error) {
        logger.error(`Error fetching creator profile for template ${req.params.templateId}: ${error.message}`, {
            stack: error.stack,
            templateId: req.params.templateId
        });
        handleError(res, error);
    }
});

/**
 * Get creator profiles for multiple templates (batch operation)
 * POST /api/templates/creators/batch
 * Body: { templateIds: ["id1", "id2", "id3"] }
 * 
 * Returns creator profile information for multiple templates at once
 * Uses uid fields for efficient batch lookups with ObjectId fallback
 */
router.post('/creators/batch', async (req, res) => {
    try {
        const { templateIds } = req.body;

        // Validate input
        if (!Array.isArray(templateIds) || templateIds.length === 0) {
            return res.status(400).json({
                success: false,
                message: 'templateIds must be a non-empty array'
            });
        }

        if (templateIds.length > 100) {
            return res.status(400).json({
                success: false,
                message: 'Maximum 100 templates can be processed at once'
            });
        }

        // Validate all template IDs
        const invalidIds = templateIds.filter(id => !validateObjectId(id));
        if (invalidIds.length > 0) {
            return res.status(400).json({
                success: false,
                message: 'Invalid template ID format',
                invalidIds: invalidIds
            });
        }

        logger.info(`Fetching creator profiles for ${templateIds.length} templates`);

        // Find all templates
        const templates = await Template.find({ 
            _id: { $in: templateIds } 
        }).select('_id title category creatorId creatorUid generatedByUser generatedByUserUid');

        if (templates.length === 0) {
            return res.json({
                success: true,
                data: [],
                message: 'No templates found'
            });
        }

        // Collect all unique user IDs and UIDs
        const userIds = new Set();
        const userUids = new Set();
        
        templates.forEach(template => {
            // Collect UIDs (preferred method)
            if (template.creatorUid) {
                userUids.add(template.creatorUid);
            }
            if (template.generatedByUserUid) {
                userUids.add(template.generatedByUserUid);
            }
            
            // Collect ObjectIds (fallback method)
            if (template.creatorId && validateObjectId(template.creatorId)) {
                userIds.add(template.creatorId.toString());
            }
            if (template.generatedByUser && validateObjectId(template.generatedByUser)) {
                userIds.add(template.generatedByUser.toString());
            }
        });

        // Fetch users by UID (most efficient) and ObjectId (fallback)
        const usersByUid = userUids.size > 0 ? await User.find({ 
            uid: { $in: Array.from(userUids) } 
        }).select('_id uid displayName email profilePhoto') : [];

        const usersByObjectId = userIds.size > 0 ? await User.find({ 
            _id: { $in: Array.from(userIds) } 
        }).select('_id uid displayName email profilePhoto') : [];

        // Create user lookup maps
        const userUidMap = {};
        const userObjectIdMap = {};
        
        usersByUid.forEach(user => {
            userUidMap[user.uid] = user;
        });
        
        usersByObjectId.forEach(user => {
            userObjectIdMap[user._id.toString()] = user;
        });

        // Process each template and build creator profiles
        const results = templates.map(template => {
            let creatorProfile = null;
            let creatorSource = 'default';

            // Priority 1: Try creatorUid (most efficient)
            if (template.creatorUid && userUidMap[template.creatorUid]) {
                const creator = userUidMap[template.creatorUid];
                creatorProfile = {
                    creatorId: creator._id,
                    creatorUid: creator.uid,
                    creatorName: creator.displayName || creator.email?.split('@')[0] || 'User',
                    creatorProfilePhoto: creator.profilePhoto || null,
                    creatorEmail: creator.email || null
                };
                creatorSource = 'creatorUid';
            }
            // Priority 2: Try creatorId (ObjectId fallback)
            else if (template.creatorId && userObjectIdMap[template.creatorId.toString()]) {
                const creator = userObjectIdMap[template.creatorId.toString()];
                creatorProfile = {
                    creatorId: template.creatorId,
                    creatorUid: creator.uid,
                    creatorName: creator.displayName || creator.email?.split('@')[0] || 'User',
                    creatorProfilePhoto: creator.profilePhoto || null,
                    creatorEmail: creator.email || null
                };
                creatorSource = 'creatorId';
            }
            // Priority 3: Try generatedByUserUid
            else if (template.generatedByUserUid && userUidMap[template.generatedByUserUid]) {
                const generator = userUidMap[template.generatedByUserUid];
                creatorProfile = {
                    creatorId: generator._id,
                    creatorUid: generator.uid,
                    creatorName: generator.displayName || generator.email?.split('@')[0] || 'User',
                    creatorProfilePhoto: generator.profilePhoto || null,
                    creatorEmail: generator.email || null
                };
                creatorSource = 'generatedByUserUid';
            }
            // Priority 4: Try generatedByUser (ObjectId fallback)
            else if (template.generatedByUser && userObjectIdMap[template.generatedByUser.toString()]) {
                const generator = userObjectIdMap[template.generatedByUser.toString()];
                creatorProfile = {
                    creatorId: template.generatedByUser,
                    creatorUid: generator.uid,
                    creatorName: generator.displayName || generator.email?.split('@')[0] || 'User',
                    creatorProfilePhoto: generator.profilePhoto || null,
                    creatorEmail: generator.email || null
                };
                creatorSource = 'generatedByUser';
            }
            // Priority 5: Default fallback
            else {
                creatorProfile = {
                    creatorId: null,
                    creatorUid: null,
                    creatorName: 'eventwish',
                    creatorProfilePhoto: null,
                    creatorEmail: null
                };
                creatorSource = 'default';
            }

            return {
                templateId: template._id,
                templateTitle: template.title,
                templateCategory: template.category,
                creatorProfile: creatorProfile,
                creatorSource: creatorSource,
                metadata: {
                    hasCreatorUid: !!template.creatorUid,
                    hasCreatorId: !!template.creatorId,
                    hasGeneratedByUserUid: !!template.generatedByUserUid,
                    hasGeneratedByUser: !!template.generatedByUser
                }
            };
        });

        // Return successful response
        res.json({
            success: true,
            data: results,
            metadata: {
                totalTemplates: templates.length,
                totalUniqueUsers: usersByUid.length + usersByObjectId.length,
                lookupMethods: {
                    byUid: usersByUid.length,
                    byObjectId: usersByObjectId.length
                }
            }
        });

        logger.info(`Batch creator profiles retrieved for ${templates.length} templates`);

    } catch (error) {
        logger.error(`Error fetching batch creator profiles: ${error.message}`, {
            stack: error.stack,
            templateIds: req.body.templateIds
        });
        handleError(res, error);
    }
});

/**
 * Get creator statistics
 * GET /api/templates/creators/:userId/stats
 * 
 * Returns statistics about templates created by a specific user
 */
router.get('/creators/:userId/stats', async (req, res) => {
    try {
        const { userId } = req.params;

        // Validate userId
        if (!validateObjectId(userId)) {
            return res.status(400).json({
                success: false,
                message: 'Invalid user ID format'
            });
        }

        logger.info(`Fetching creator statistics for user: ${userId}`);

        // Check if user exists
        const user = await User.findById(userId).select('displayName email profilePhoto');
        if (!user) {
            return handleNotFound(res, 'User', userId);
        }

        // Get template statistics
        const [
            createdTemplates,
            generatedTemplates,
            totalLikes,
            totalFavorites,
            totalViews,
            totalShares
        ] = await Promise.all([
            Template.countDocuments({ creatorId: userId }),
            Template.countDocuments({ generatedByUser: userId }),
            Template.aggregate([
                { $match: { $or: [{ creatorId: userId }, { generatedByUser: userId }] } },
                { $group: { _id: null, total: { $sum: '$likes' } } }
            ]),
            Template.aggregate([
                { $match: { $or: [{ creatorId: userId }, { generatedByUser: userId }] } },
                { $group: { _id: null, total: { $sum: '$favorites' } } }
            ]),
            Template.aggregate([
                { $match: { $or: [{ creatorId: userId }, { generatedByUser: userId }] } },
                { $group: { _id: null, total: { $sum: '$viewCount' } } }
            ]),
            Template.aggregate([
                { $match: { $or: [{ creatorId: userId }, { generatedByUser: userId }] } },
                { $group: { _id: null, total: { $sum: '$sharedCount' } } }
            ])
        ]);

        // Get category breakdown
        const categoryStats = await Template.aggregate([
            { $match: { $or: [{ creatorId: userId }, { generatedByUser: userId }] } },
            { $group: { _id: '$category', count: { $sum: 1 } } },
            { $sort: { count: -1 } }
        ]);

        const stats = {
            user: {
                userId: userId,
                displayName: user.displayName,
                email: user.email,
                profilePhoto: user.profilePhoto
            },
            templates: {
                totalCreated: createdTemplates,
                totalGenerated: generatedTemplates,
                totalAll: createdTemplates + generatedTemplates
            },
            engagement: {
                totalLikes: totalLikes[0]?.total || 0,
                totalFavorites: totalFavorites[0]?.total || 0,
                totalViews: totalViews[0]?.total || 0,
                totalShares: totalShares[0]?.total || 0
            },
            categories: categoryStats.map(cat => ({
                category: cat._id,
                templateCount: cat.count
            }))
        };

        res.json({
            success: true,
            data: stats
        });

        logger.info(`Creator statistics retrieved successfully for user: ${userId}`);

    } catch (error) {
        logger.error(`Error fetching creator statistics for user ${req.params.userId}: ${error.message}`, {
            stack: error.stack,
            userId: req.params.userId
        });
        handleError(res, error);
    }
});

/**
 * Health check endpoint for template interactions
 * GET /api/templates/health/template-interactions
 * 
 * Returns the health status of the template interactions system
 */
router.get('/health/template-interactions', async (req, res) => {
    try {
        // Check database connection
        const templateCount = await Template.countDocuments();
        const userCount = await User.countDocuments();
        
        // Get current timestamp
        const timestamp = new Date();
        
        // Create health status
        const healthStatus = {
            status: 'healthy',
            timestamp: timestamp,
            database: {
                connected: true,
                templateCount: templateCount,
                userCount: userCount
            },
            services: {
                creatorProfileService: 'operational',
                templateService: 'operational',
                userService: 'operational'
            },
            endpoints: {
                singleCreatorProfile: '/api/templates/{templateId}/creator',
                batchCreatorProfiles: '/api/templates/creators/batch',
                creatorStatistics: '/api/templates/creators/{userId}/stats',
                healthCheck: '/api/templates/health/template-interactions'
            }
        };
        
        logger.info('Health check performed successfully');
        
        res.json({
            success: true,
            data: healthStatus,
            message: 'Template interactions service is healthy'
        });
        
    } catch (error) {
        logger.error('Health check failed:', error);
        
        const healthStatus = {
            status: 'unhealthy',
            timestamp: new Date(),
            error: error.message,
            database: {
                connected: false
            }
        };
        
        res.status(503).json({
            success: false,
            data: healthStatus,
            message: 'Template interactions service is unhealthy'
        });
    }
});

/**
 * Utility function to populate UID fields from ObjectId references
 * @param {Object} template - Template document
 * @returns {Promise<Object>} - Template with populated UID fields
 */
async function populateTemplateUids(template) {
    try {
        let updated = false;
        
        // Populate creatorUid if missing but creatorId exists
        if (template.creatorId && !template.creatorUid && validateObjectId(template.creatorId)) {
            const creator = await User.findById(template.creatorId).select('uid');
            if (creator && creator.uid) {
                template.creatorUid = creator.uid;
                updated = true;
                logger.info(`Populated creatorUid for template ${template._id}: ${creator.uid}`);
            }
        }
        
        // Populate generatedByUserUid if missing but generatedByUser exists
        if (template.generatedByUser && !template.generatedByUserUid && validateObjectId(template.generatedByUser)) {
            const generator = await User.findById(template.generatedByUser).select('uid');
            if (generator && generator.uid) {
                template.generatedByUserUid = generator.uid;
                updated = true;
                logger.info(`Populated generatedByUserUid for template ${template._id}: ${generator.uid}`);
            }
        }
        
        // Save if updated
        if (updated) {
            await template.save();
            logger.info(`Saved UID updates for template ${template._id}`);
        }
        
        return template;
    } catch (error) {
        logger.error(`Error populating UIDs for template ${template._id}: ${error.message}`);
        return template; // Return original template if error occurs
    }
}

/**
 * Sync existing templates with user UIDs
 * POST /api/templates/sync/uids
 * Body: { limit?: number, offset?: number }
 * 
 * Populates missing UID fields in existing templates
 * Useful for migrating existing data to the new UID system
 */
router.post('/sync/uids', async (req, res) => {
    try {
        const { limit = 100, offset = 0 } = req.body;

        logger.info(`Starting UID sync for templates (limit: ${limit}, offset: ${offset})`);

        // Find templates missing UID fields but having ObjectId references
        const templates = await Template.find({
            $or: [
                { creatorId: { $exists: true, $ne: null }, creatorUid: { $exists: false } },
                { creatorId: { $exists: true, $ne: null }, creatorUid: null },
                { creatorId: { $exists: true, $ne: null }, creatorUid: '' },
                { generatedByUser: { $exists: true, $ne: null }, generatedByUserUid: { $exists: false } },
                { generatedByUser: { $exists: true, $ne: null }, generatedByUserUid: null },
                { generatedByUser: { $exists: true, $ne: null }, generatedByUserUid: '' }
            ]
        })
        .limit(limit)
        .skip(offset)
        .select('_id title creatorId creatorUid generatedByUser generatedByUserUid');

        if (templates.length === 0) {
            return res.json({
                success: true,
                message: 'No templates need UID synchronization',
                data: {
                    processed: 0,
                    updated: 0,
                    errors: 0
                }
            });
        }

        let processed = 0;
        let updated = 0;
        let errors = 0;

        // Process each template
        for (const template of templates) {
            try {
                const originalCreatorUid = template.creatorUid;
                const originalGeneratedByUserUid = template.generatedByUserUid;
                
                await populateTemplateUids(template);
                
                // Check if any UIDs were updated
                if (template.creatorUid !== originalCreatorUid || 
                    template.generatedByUserUid !== originalGeneratedByUserUid) {
                    updated++;
                }
                
                processed++;
            } catch (error) {
                logger.error(`Error processing template ${template._id}: ${error.message}`);
                errors++;
            }
        }

        res.json({
            success: true,
            message: `UID synchronization completed`,
            data: {
                processed: processed,
                updated: updated,
                errors: errors,
                limit: limit,
                offset: offset,
                hasMore: templates.length === limit
            }
        });

        logger.info(`UID sync completed: ${processed} processed, ${updated} updated, ${errors} errors`);

    } catch (error) {
        logger.error(`Error in UID sync: ${error.message}`, {
            stack: error.stack
        });
        handleError(res, error);
    }
});

module.exports = router;
