const Template = require('../../../models/Template');
const logger = require('../../../utils/logger');
const { handleAsyncOperation } = require('../../../utils/templateHelpers');

/**
 * Get weekly metrics for a template
 */
const getWeeklyMetrics = async (req, res) => {
    await handleAsyncOperation(async () => {
        const { templateId } = req.params;
        
        const template = await Template.findById(templateId).select(
            'weeklyUsageCount weeklyLikes weeklyFavorites weeklyViewCount weeklySharedCount weeklyDownloadCount weeklyReportCount weeklyScoreLastReset'
        );
        
        if (!template) {
            return res.status(404).json({
                success: false,
                error: {
                    code: 'TEMPLATE_NOT_FOUND',
                    message: 'Template not found',
                    field: 'templateId',
                    value: templateId
                },
                timestamp: new Date().toISOString()
            });
        }
        
        const weeklyData = {
            templateId: template._id,
            weeklyUsageCount: template.weeklyUsageCount,
            weeklyLikes: template.weeklyLikes,
            weeklyFavorites: template.weeklyFavorites,
            weeklyViewCount: template.weeklyViewCount,
            weeklySharedCount: template.weeklySharedCount,
            weeklyDownloadCount: template.weeklyDownloadCount,
            weeklyReportCount: template.weeklyReportCount,
            weeklyScoreLastReset: template.weeklyScoreLastReset,
            weeklyTrendingScore: template.weeklyTrendingScore // Virtual field
        };
        
        logger.info(`Retrieved weekly metrics for template ${templateId}`);
        
        res.json({
            success: true,
            data: weeklyData,
            timestamp: new Date().toISOString()
        });
    }, res, 'getWeeklyMetrics');
};

/**
 * Update weekly metrics for a template
 */
const updateWeeklyMetrics = async (req, res) => {
    await handleAsyncOperation(async () => {
        const { templateId } = req.params;
        const updates = req.validatedUpdates;
        
        const template = await Template.findByIdAndUpdate(
            templateId,
            { $set: updates },
            { new: true, runValidators: true }
        ).select('weeklyUsageCount weeklyLikes weeklyFavorites weeklyViewCount weeklySharedCount weeklyDownloadCount weeklyReportCount weeklyScoreLastReset');
        
        if (!template) {
            return res.status(404).json({
                success: false,
                error: {
                    code: 'TEMPLATE_NOT_FOUND',
                    message: 'Template not found',
                    field: 'templateId',
                    value: templateId
                },
                timestamp: new Date().toISOString()
            });
        }
        
        logger.info(`Updated weekly metrics for template ${templateId}`, { updates });
        
        res.json({
            success: true,
            data: {
                templateId: template._id,
                updatedFields: Object.keys(updates),
                newValues: updates,
                currentMetrics: {
                    weeklyUsageCount: template.weeklyUsageCount,
                    weeklyLikes: template.weeklyLikes,
                    weeklyFavorites: template.weeklyFavorites,
                    weeklyViewCount: template.weeklyViewCount,
                    weeklySharedCount: template.weeklySharedCount,
                    weeklyDownloadCount: template.weeklyDownloadCount,
                    weeklyReportCount: template.weeklyReportCount,
                    weeklyScoreLastReset: template.weeklyScoreLastReset,
                    weeklyTrendingScore: template.weeklyTrendingScore
                }
            },
            timestamp: new Date().toISOString()
        });
    }, res, 'updateWeeklyMetrics');
};

/**
 * Increment weekly counters
 */
const incrementWeeklyCounters = async (req, res) => {
    await handleAsyncOperation(async () => {
        const { templateId } = req.params;
        const { increments } = req.body;
        
        if (!increments || typeof increments !== 'object') {
            return res.status(400).json({
                success: false,
                error: {
                    code: 'VALIDATION_ERROR',
                    message: 'increments object is required',
                    field: 'increments'
                },
                timestamp: new Date().toISOString()
            });
        }
        
        const allowedFields = [
            'weeklyUsageCount', 'weeklyLikes', 'weeklyFavorites', 
            'weeklyViewCount', 'weeklySharedCount', 'weeklyDownloadCount', 'weeklyReportCount'
        ];
        
        const incrementUpdates = {};
        for (const field in increments) {
            if (allowedFields.includes(field)) {
                const value = increments[field];
                if (typeof value === 'number' && value > 0) {
                    incrementUpdates[field] = value;
                }
            }
        }
        
        if (Object.keys(incrementUpdates).length === 0) {
            return res.status(400).json({
                success: false,
                error: {
                    code: 'VALIDATION_ERROR',
                    message: 'No valid increment fields provided',
                    details: { allowedFields }
                },
                timestamp: new Date().toISOString()
            });
        }
        
        const template = await Template.findByIdAndUpdate(
            templateId,
            { $inc: incrementUpdates },
            { new: true, runValidators: true }
        ).select('weeklyUsageCount weeklyLikes weeklyFavorites weeklyViewCount weeklySharedCount weeklyDownloadCount weeklyReportCount');
        
        if (!template) {
            return res.status(404).json({
                success: false,
                error: {
                    code: 'TEMPLATE_NOT_FOUND',
                    message: 'Template not found',
                    field: 'templateId',
                    value: templateId
                },
                timestamp: new Date().toISOString()
            });
        }
        
        logger.info(`Incremented weekly counters for template ${templateId}`, { increments: incrementUpdates });
        
        res.json({
            success: true,
            data: {
                templateId: template._id,
                incrementedFields: Object.keys(incrementUpdates),
                increments: incrementUpdates,
                currentMetrics: {
                    weeklyUsageCount: template.weeklyUsageCount,
                    weeklyLikes: template.weeklyLikes,
                    weeklyFavorites: template.weeklyFavorites,
                    weeklyViewCount: template.weeklyViewCount,
                    weeklySharedCount: template.weeklySharedCount,
                    weeklyDownloadCount: template.weeklyDownloadCount,
                    weeklyReportCount: template.weeklyReportCount,
                    weeklyTrendingScore: template.weeklyTrendingScore
                }
            },
            timestamp: new Date().toISOString()
        });
    }, res, 'incrementWeeklyCounters');
};

/**
 * Reset weekly metrics for a template
 */
const resetWeeklyMetrics = async (req, res) => {
    await handleAsyncOperation(async () => {
        const { templateId } = req.params;
        const { fields } = req.body;
        
        const resetFields = {
            weeklyScoreLastReset: new Date()
        };
        
        const weeklyFields = [
            'weeklyUsageCount', 'weeklyLikes', 'weeklyFavorites', 
            'weeklyViewCount', 'weeklySharedCount', 'weeklyDownloadCount', 'weeklyReportCount'
        ];
        
        if (fields && Array.isArray(fields)) {
            for (const field of fields) {
                if (weeklyFields.includes(field)) {
                    resetFields[field] = 0;
                }
            }
        } else {
            // Reset all weekly fields if none specified
            weeklyFields.forEach(field => {
                resetFields[field] = 0;
            });
        }
        
        const template = await Template.findByIdAndUpdate(
            templateId,
            { $set: resetFields },
            { new: true, runValidators: true }
        ).select('weeklyUsageCount weeklyLikes weeklyFavorites weeklyViewCount weeklySharedCount weeklyDownloadCount weeklyReportCount weeklyScoreLastReset');
        
        if (!template) {
            return res.status(404).json({
                success: false,
                error: {
                    code: 'TEMPLATE_NOT_FOUND',
                    message: 'Template not found',
                    field: 'templateId',
                    value: templateId
                },
                timestamp: new Date().toISOString()
            });
        }
        
        logger.info(`Reset weekly metrics for template ${templateId}`, { resetFields });
        
        res.json({
            success: true,
            data: {
                templateId: template._id,
                resetFields: Object.keys(resetFields),
                resetTimestamp: resetFields.weeklyScoreLastReset,
                currentMetrics: {
                    weeklyUsageCount: template.weeklyUsageCount,
                    weeklyLikes: template.weeklyLikes,
                    weeklyFavorites: template.weeklyFavorites,
                    weeklyViewCount: template.weeklyViewCount,
                    weeklySharedCount: template.weeklySharedCount,
                    weeklyDownloadCount: template.weeklyDownloadCount,
                    weeklyReportCount: template.weeklyReportCount,
                    weeklyScoreLastReset: template.weeklyScoreLastReset
                }
            },
            timestamp: new Date().toISOString()
        });
    }, res, 'resetWeeklyMetrics');
};

/**
 * Get weekly leaderboard
 */
const getWeeklyLeaderboard = async (req, res) => {
    await handleAsyncOperation(async () => {
        const { type = 'weeklyTrendingScore' } = req.params;
        const limit = parseInt(req.query.limit) || 20;
        const skip = parseInt(req.query.skip) || 0;
        
        const validTypes = [
            'weeklyUsageCount', 'weeklyLikes', 'weeklyFavorites', 
            'weeklyViewCount', 'weeklySharedCount', 'weeklyDownloadCount', 'weeklyTrendingScore'
        ];
        
        if (!validTypes.includes(type)) {
            return res.status(400).json({
                success: false,
                error: {
                    code: 'VALIDATION_ERROR',
                    message: 'Invalid weekly leaderboard type',
                    field: 'type',
                    value: type,
                    details: { validTypes }
                },
                timestamp: new Date().toISOString()
            });
        }
        
        let templates;
        
        if (type === 'weeklyTrendingScore') {
            // Use aggregation pipeline for virtual field
            templates = await Template.aggregate([
                {
                    $addFields: {
                        weeklyTrendingScore: {
                            $subtract: [
                                {
                                    $add: [
                                        { $multiply: ['$weeklyUsageCount', 3] },
                                        { $multiply: ['$weeklyLikes', 2] },
                                        { $multiply: ['$weeklyFavorites', 2] },
                                        '$weeklyViewCount',
                                        '$weeklySharedCount',
                                        '$weeklyDownloadCount'
                                    ]
                                },
                                { $multiply: ['$weeklyReportCount', 5] }
                            ]
                        }
                    }
                },
                { $sort: { weeklyTrendingScore: -1 } },
                { $skip: skip },
                { $limit: limit },
                {
                    $project: {
                        title: 1,
                        category: 1,
                        weeklyUsageCount: 1,
                        weeklyLikes: 1,
                        weeklyFavorites: 1,
                        weeklyViewCount: 1,
                        weeklySharedCount: 1,
                        weeklyDownloadCount: 1,
                        weeklyReportCount: 1,
                        weeklyScoreLastReset: 1,
                        weeklyTrendingScore: 1,
                        createdAt: 1
                    }
                }
            ]);
        } else {
            templates = await Template.find({ status: true })
                .select('title category weeklyUsageCount weeklyLikes weeklyFavorites weeklyViewCount weeklySharedCount weeklyDownloadCount weeklyReportCount weeklyScoreLastReset createdAt')
                .sort({ [type]: -1 })
                .skip(skip)
                .limit(limit);
        }
        
        logger.info(`Retrieved weekly leaderboard for type: ${type}`);
        
        res.json({
            success: true,
            data: {
                type,
                templates,
                pagination: {
                    limit,
                    skip,
                    count: templates.length
                }
            },
            timestamp: new Date().toISOString()
        });
    }, res, 'getWeeklyLeaderboard');
};

/**
 * Get weekly trending score for a template
 */
const getWeeklyTrendingScore = async (req, res) => {
    await handleAsyncOperation(async () => {
        const { templateId } = req.params;
        
        const template = await Template.findById(templateId).select(
            'weeklyUsageCount weeklyLikes weeklyFavorites weeklyViewCount weeklySharedCount weeklyDownloadCount weeklyReportCount'
        );
        
        if (!template) {
            return res.status(404).json({
                success: false,
                error: {
                    code: 'TEMPLATE_NOT_FOUND',
                    message: 'Template not found',
                    field: 'templateId',
                    value: templateId
                },
                timestamp: new Date().toISOString()
            });
        }
        
        const score = template.weeklyTrendingScore;
        
        logger.info(`Retrieved weekly trending score for template ${templateId}: ${score}`);
        
        res.json({
            success: true,
            data: {
                templateId: template._id,
                weeklyTrendingScore: score,
                components: {
                    weeklyUsageCount: template.weeklyUsageCount,
                    weeklyLikes: template.weeklyLikes,
                    weeklyFavorites: template.weeklyFavorites,
                    weeklyViewCount: template.weeklyViewCount,
                    weeklySharedCount: template.weeklySharedCount,
                    weeklyDownloadCount: template.weeklyDownloadCount,
                    weeklyReportCount: template.weeklyReportCount
                }
            },
            timestamp: new Date().toISOString()
        });
    }, res, 'getWeeklyTrendingScore');
};

module.exports = {
    getWeeklyMetrics,
    updateWeeklyMetrics,
    incrementWeeklyCounters,
    resetWeeklyMetrics,
    getWeeklyLeaderboard,
    getWeeklyTrendingScore
}; 