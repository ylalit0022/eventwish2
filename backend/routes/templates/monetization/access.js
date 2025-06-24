const express = require('express');
const router = express.Router();
const { 
    updateTemplateAccess,
    getTemplateAccess,
    togglePremiumStatus,
    validateAccessPermissions,
    bulkUpdateAccess
} = require('../../../controllers/templates/monetization/accessController');

const { templateFieldValidators } = require('../../../utils/templateValidators');
const { handleAsyncOperation } = require('../../../utils/templateHelpers');
const logger = require('../../../utils/logger');

/**
 * @route GET /api/templates/monetization/access/:templateId
 * @desc Get template access control settings
 * @access Public
 * @params templateId - MongoDB ObjectId
 */
router.get('/:templateId', async (req, res) => {
    await handleAsyncOperation(
        () => getTemplateAccess(req, res),
        res,
        'get template access settings'
    );
});

/**
 * @route PUT /api/templates/monetization/access/:templateId
 * @desc Update template access control settings
 * @access Admin
 * @params templateId - MongoDB ObjectId
 * @body { isPremium?, premiumAccess? }
 */
router.put('/:templateId',
    templateFieldValidators.isPremium,
    templateFieldValidators.premiumAccess,
    async (req, res) => {
        await handleAsyncOperation(
            () => updateTemplateAccess(req, res),
            res,
            'update template access settings'
        );
    }
);

/**
 * @route POST /api/templates/monetization/access/:templateId/toggle-premium
 * @desc Toggle template premium status
 * @access Admin
 * @params templateId - MongoDB ObjectId
 * @body { isPremium }
 */
router.post('/:templateId/toggle-premium',
    templateFieldValidators.isPremium,
    async (req, res) => {
        await handleAsyncOperation(
            () => togglePremiumStatus(req, res),
            res,
            'toggle template premium status'
        );
    }
);

/**
 * @route POST /api/templates/monetization/access/:templateId/validate
 * @desc Validate user access permissions for template
 * @access Public
 * @params templateId - MongoDB ObjectId
 * @body { userId?, userSubscriptionType?, userPremiumStatus? }
 */
router.post('/:templateId/validate',
    async (req, res) => {
        await handleAsyncOperation(
            () => validateAccessPermissions(req, res),
            res,
            'validate template access permissions'
        );
    }
);

/**
 * @route GET /api/templates/monetization/access/:templateId/status
 * @desc Get template access status for specific user
 * @access Public
 * @params templateId - MongoDB ObjectId
 * @query userId - User ID to check access for
 */
router.get('/:templateId/status', async (req, res) => {
    try {
        const { templateId } = req.params;
        const { userId } = req.query;
        
        if (!require('mongoose').Types.ObjectId.isValid(templateId)) {
            return res.status(400).json({
                success: false,
                error: {
                    code: 'VALIDATION_ERROR',
                    message: 'Invalid template ID format',
                    field: 'templateId',
                    value: templateId
                },
                timestamp: new Date().toISOString()
            });
        }

        const Template = require('../../../models/Template');
        const template = await Template.findById(templateId)
            .select('title isPremium premiumAccess status');
        
        if (!template) {
            return res.status(404).json({
                success: false,
                error: {
                    code: 'NOT_FOUND',
                    message: 'Template not found',
                    field: 'templateId',
                    value: templateId
                },
                timestamp: new Date().toISOString()
            });
        }

        // Basic access check (can be enhanced with user subscription logic)
        const hasAccess = !template.isPremium || (userId && template.premiumAccess);
        
        logger.info(`Template access status checked: ${templateId}`, { 
            userId, 
            isPremium: template.isPremium,
            hasAccess 
        });

        res.json({
            success: true,
            data: {
                templateId: templateId,
                title: template.title,
                isPremium: template.isPremium,
                premiumAccess: template.premiumAccess,
                hasAccess: hasAccess,
                accessLevel: hasAccess ? 'granted' : 'restricted',
                requiresSubscription: template.isPremium && !hasAccess
            },
            timestamp: new Date().toISOString()
        });

    } catch (error) {
        logger.error('Error checking template access status:', error);
        res.status(500).json({
            success: false,
            error: {
                code: 'SERVER_ERROR',
                message: 'Internal server error while checking access status',
                details: error.message
            },
            timestamp: new Date().toISOString()
        });
    }
});

/**
 * @route POST /api/templates/monetization/access/bulk-update
 * @desc Bulk update access settings for multiple templates
 * @access Admin
 * @body { templateIds: [], updates: { isPremium?, premiumAccess? } }
 */
router.post('/bulk-update',
    templateFieldValidators.isPremium,
    templateFieldValidators.premiumAccess,
    async (req, res) => {
        await handleAsyncOperation(
            () => bulkUpdateAccess(req, res),
            res,
            'bulk update template access settings'
        );
    }
);

/**
 * @route GET /api/templates/monetization/access/premium/list
 * @desc Get list of all premium templates
 * @access Public
 * @query page - Page number (default: 1)
 * @query limit - Items per page (default: 20)
 * @query category - Filter by category
 */
router.get('/premium/list', async (req, res) => {
    try {
        const { page = 1, limit = 20, category } = req.query;
        const skip = (parseInt(page) - 1) * parseInt(limit);

        const query = { 
            isPremium: true, 
            status: true 
        };
        
        if (category) {
            query.category = category;
        }

        const Template = require('../../../models/Template');
        const templates = await Template.find(query)
            .select('title category isPremium premiumAccess previewUrl createdAt')
            .sort({ createdAt: -1 })
            .skip(skip)
            .limit(parseInt(limit));

        const totalCount = await Template.countDocuments(query);
        const totalPages = Math.ceil(totalCount / parseInt(limit));

        logger.info(`Premium templates list retrieved: ${templates.length} items`);

        res.json({
            success: true,
            data: {
                templates,
                pagination: {
                    currentPage: parseInt(page),
                    totalPages,
                    totalItems: totalCount,
                    itemsPerPage: parseInt(limit),
                    hasNextPage: parseInt(page) < totalPages,
                    hasPrevPage: parseInt(page) > 1
                }
            },
            timestamp: new Date().toISOString()
        });

    } catch (error) {
        logger.error('Error retrieving premium templates list:', error);
        res.status(500).json({
            success: false,
            error: {
                code: 'SERVER_ERROR',
                message: 'Internal server error while retrieving premium templates',
                details: error.message
            },
            timestamp: new Date().toISOString()
        });
    }
});

/**
 * @route GET /api/templates/monetization/access/free/list
 * @desc Get list of all free templates
 * @access Public
 * @query page - Page number (default: 1)
 * @query limit - Items per page (default: 20)
 * @query category - Filter by category
 */
router.get('/free/list', async (req, res) => {
    try {
        const { page = 1, limit = 20, category } = req.query;
        const skip = (parseInt(page) - 1) * parseInt(limit);

        const query = { 
            isPremium: false, 
            status: true 
        };
        
        if (category) {
            query.category = category;
        }

        const Template = require('../../../models/Template');
        const templates = await Template.find(query)
            .select('title category isPremium previewUrl createdAt')
            .sort({ createdAt: -1 })
            .skip(skip)
            .limit(parseInt(limit));

        const totalCount = await Template.countDocuments(query);
        const totalPages = Math.ceil(totalCount / parseInt(limit));

        logger.info(`Free templates list retrieved: ${templates.length} items`);

        res.json({
            success: true,
            data: {
                templates,
                pagination: {
                    currentPage: parseInt(page),
                    totalPages,
                    totalItems: totalCount,
                    itemsPerPage: parseInt(limit),
                    hasNextPage: parseInt(page) < totalPages,
                    hasPrevPage: parseInt(page) > 1
                }
            },
            timestamp: new Date().toISOString()
        });

    } catch (error) {
        logger.error('Error retrieving free templates list:', error);
        res.status(500).json({
            success: false,
            error: {
                code: 'SERVER_ERROR',
                message: 'Internal server error while retrieving free templates',
                details: error.message
            },
            timestamp: new Date().toISOString()
        });
    }
});

module.exports = router; 