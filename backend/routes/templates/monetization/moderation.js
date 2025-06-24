// Moderation Routes

const express = require('express');
const router = express.Router();
const { 
    updateModerationStatus,
    getModerationInfo,
    bulkModerationUpdate,
    getModerationQueue,
    approveModerationBatch,
    rejectModerationBatch
} = require('../../../controllers/templates/monetization/moderationController');

const { templateFieldValidators } = require('../../../utils/templateValidators');
const { handleAsyncOperation } = require('../../../utils/templateHelpers');
const logger = require('../../../utils/logger');

/**
 * @route GET /api/templates/monetization/moderation/:templateId
 * @desc Get template moderation information
 * @access Admin
 * @params templateId - MongoDB ObjectId
 */
router.get('/:templateId', async (req, res) => {
    await handleAsyncOperation(
        () => getModerationInfo(req, res),
        res,
        'get template moderation info'
    );
});

/**
 * @route PUT /api/templates/monetization/moderation/:templateId
 * @desc Update template moderation status and notes
 * @access Admin
 * @params templateId - MongoDB ObjectId
 * @body { moderationStatus?, moderationNotes? }
 */
router.put('/:templateId',
    templateFieldValidators.moderationStatus,
    templateFieldValidators.moderationNotes,
    async (req, res) => {
        await handleAsyncOperation(
            () => updateModerationStatus(req, res),
            res,
            'update template moderation status'
        );
    }
);

/**
 * @route POST /api/templates/monetization/moderation/:templateId/approve
 * @desc Approve template for publication
 * @access Admin
 * @params templateId - MongoDB ObjectId
 * @body { moderationNotes? }
 */
router.post('/:templateId/approve',
    templateFieldValidators.moderationNotes,
    async (req, res) => {
        try {
            const { templateId } = req.params;
            const { moderationNotes = 'Template approved for publication' } = req.body;
            
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
            const updatedTemplate = await Template.findByIdAndUpdate(
                templateId,
                { 
                    moderationStatus: 'approved',
                    moderationNotes: moderationNotes,
                    status: true, // Ensure template is active
                    updatedAt: new Date()
                },
                { 
                    new: true,
                    select: 'title moderationStatus moderationNotes status updatedAt'
                }
            );
            
            if (!updatedTemplate) {
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

            logger.info(`Template approved: ${templateId}`, { moderationNotes });

            res.json({
                success: true,
                data: {
                    templateId: templateId,
                    title: updatedTemplate.title,
                    moderationStatus: updatedTemplate.moderationStatus,
                    moderationNotes: updatedTemplate.moderationNotes,
                    status: updatedTemplate.status,
                    updatedAt: updatedTemplate.updatedAt
                },
                modified: ['moderationStatus', 'moderationNotes', 'status'],
                timestamp: new Date().toISOString()
            });

        } catch (error) {
            logger.error('Error approving template:', error);
            res.status(500).json({
                success: false,
                error: {
                    code: 'SERVER_ERROR',
                    message: 'Internal server error while approving template',
                    details: error.message
                },
                timestamp: new Date().toISOString()
            });
        }
    }
);

/**
 * @route POST /api/templates/monetization/moderation/:templateId/reject
 * @desc Reject template with reason
 * @access Admin
 * @params templateId - MongoDB ObjectId
 * @body { moderationNotes }
 */
router.post('/:templateId/reject',
    templateFieldValidators.moderationNotes,
    async (req, res) => {
        try {
            const { templateId } = req.params;
            const { moderationNotes } = req.body;
            
            if (!moderationNotes || moderationNotes.trim().length === 0) {
                return res.status(400).json({
                    success: false,
                    error: {
                        code: 'VALIDATION_ERROR',
                        message: 'Moderation notes are required for rejection',
                        field: 'moderationNotes',
                        value: moderationNotes
                    },
                    timestamp: new Date().toISOString()
                });
            }

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
            const updatedTemplate = await Template.findByIdAndUpdate(
                templateId,
                { 
                    moderationStatus: 'rejected',
                    moderationNotes: moderationNotes,
                    status: false, // Deactivate rejected template
                    updatedAt: new Date()
                },
                { 
                    new: true,
                    select: 'title moderationStatus moderationNotes status updatedAt'
                }
            );
            
            if (!updatedTemplate) {
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

            logger.info(`Template rejected: ${templateId}`, { moderationNotes });

            res.json({
                success: true,
                data: {
                    templateId: templateId,
                    title: updatedTemplate.title,
                    moderationStatus: updatedTemplate.moderationStatus,
                    moderationNotes: updatedTemplate.moderationNotes,
                    status: updatedTemplate.status,
                    updatedAt: updatedTemplate.updatedAt
                },
                modified: ['moderationStatus', 'moderationNotes', 'status'],
                timestamp: new Date().toISOString()
            });

        } catch (error) {
            logger.error('Error rejecting template:', error);
            res.status(500).json({
                success: false,
                error: {
                    code: 'SERVER_ERROR',
                    message: 'Internal server error while rejecting template',
                    details: error.message
                },
                timestamp: new Date().toISOString()
            });
        }
    }
);

/**
 * @route GET /api/templates/monetization/moderation/queue
 * @desc Get templates pending moderation
 * @access Admin
 * @query page - Page number (default: 1)
 * @query limit - Items per page (default: 20)
 * @query status - Filter by moderation status (pending|approved|rejected)
 */
router.get('/queue', async (req, res) => {
    await handleAsyncOperation(
        () => getModerationQueue(req, res),
        res,
        'get moderation queue'
    );
});

/**
 * @route POST /api/templates/monetization/moderation/bulk-update
 * @desc Bulk update moderation status for multiple templates
 * @access Admin
 * @body { templateIds: [], moderationStatus, moderationNotes? }
 */
router.post('/bulk-update',
    templateFieldValidators.moderationStatus,
    templateFieldValidators.moderationNotes,
    async (req, res) => {
        await handleAsyncOperation(
            () => bulkModerationUpdate(req, res),
            res,
            'bulk update moderation status'
        );
    }
);

/**
 * @route GET /api/templates/monetization/moderation/stats
 * @desc Get moderation statistics
 * @access Admin
 * @query period - Time period (day|week|month|all)
 */
router.get('/stats', async (req, res) => {
    try {
        const { period = 'all' } = req.query;

        // Calculate date range based on period
        let dateFilter = {};
        const now = new Date();
        
        switch (period) {
            case 'day':
                dateFilter = { createdAt: { $gte: new Date(now.setHours(0, 0, 0, 0)) } };
                break;
            case 'week':
                const weekAgo = new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000);
                dateFilter = { createdAt: { $gte: weekAgo } };
                break;
            case 'month':
                const monthAgo = new Date(now.getTime() - 30 * 24 * 60 * 60 * 1000);
                dateFilter = { createdAt: { $gte: monthAgo } };
                break;
            default:
                dateFilter = {};
        }

        const Template = require('../../../models/Template');
        
        // Get moderation status counts
        const moderationStats = await Template.aggregate([
            { $match: dateFilter },
            {
                $group: {
                    _id: '$moderationStatus',
                    count: { $sum: 1 }
                }
            }
        ]);

        // Get total templates count
        const totalTemplates = await Template.countDocuments(dateFilter);

        // Format statistics
        const stats = {
            total: totalTemplates,
            pending: 0,
            approved: 0,
            rejected: 0,
            unmoderated: 0
        };

        moderationStats.forEach(stat => {
            if (stat._id) {
                stats[stat._id] = stat.count;
            } else {
                stats.unmoderated = stat.count;
            }
        });

        // Calculate percentages
        const percentages = {};
        Object.keys(stats).forEach(key => {
            if (key !== 'total') {
                percentages[key] = totalTemplates > 0 ? ((stats[key] / totalTemplates) * 100).toFixed(2) : 0;
            }
        });

        logger.info(`Moderation statistics retrieved for period: ${period}`);

        res.json({
            success: true,
            data: {
                period,
                statistics: stats,
                percentages,
                generatedAt: new Date().toISOString()
            },
            timestamp: new Date().toISOString()
        });

    } catch (error) {
        logger.error('Error retrieving moderation statistics:', error);
        res.status(500).json({
            success: false,
            error: {
                code: 'SERVER_ERROR',
                message: 'Internal server error while retrieving moderation statistics',
                details: error.message
            },
            timestamp: new Date().toISOString()
        });
    }
});

module.exports = router;
