const Template = require('../../../models/Template');
const logger = require('../../../utils/logger');
const { handleAsyncOperation } = require('../../../utils/templateHelpers');

/**
 * Get engagement metrics for a template
 */
const getEngagementMetrics = async (req, res) => {
    await handleAsyncOperation(async () => {
        const { templateId } = req.params;
        
        const template = await Template.findById(templateId).select(
            'usageCount likes favorites viewCount sharedCount downloadCount reportCount rating ratingCount'
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
        
        const engagementData = {
            templateId: template._id,
            usageCount: template.usageCount,
            likes: template.likes,
            favorites: template.favorites,
            viewCount: template.viewCount,
            sharedCount: template.sharedCount,
            downloadCount: template.downloadCount,
            reportCount: template.reportCount,
            rating: template.rating,
            ratingCount: template.ratingCount,
            trendingScore: template.trendingScore // Virtual field
        };
        
        logger.info(`Retrieved engagement metrics for template ${templateId}`);
        
        res.json({
            success: true,
            data: engagementData,
            timestamp: new Date().toISOString()
        });
    }, res, 'getEngagementMetrics');
};

/**
 * Update engagement metrics for a template
 */
const updateEngagementMetrics = async (req, res) => {
    await handleAsyncOperation(async () => {
        const { templateId } = req.params;
        const updates = req.validatedUpdates;
        
        const template = await Template.findByIdAndUpdate(
            templateId,
            { $set: updates },
            { new: true, runValidators: true }
        ).select('usageCount likes favorites viewCount sharedCount downloadCount reportCount rating ratingCount');
        
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
        
        logger.info(`Updated engagement metrics for template ${templateId}`, { updates });
        
        res.json({
            success: true,
            data: {
                templateId: template._id,
                updatedFields: Object.keys(updates),
                newValues: updates,
                currentMetrics: {
                    usageCount: template.usageCount,
                    likes: template.likes,
                    favorites: template.favorites,
                    viewCount: template.viewCount,
                    sharedCount: template.sharedCount,
                    downloadCount: template.downloadCount,
                    reportCount: template.reportCount,
                    rating: template.rating,
                    ratingCount: template.ratingCount,
                    trendingScore: template.trendingScore
                }
            },
            timestamp: new Date().toISOString()
        });
    }, res, 'updateEngagementMetrics');
};

/**
 * Increment specific engagement counters
 */
const incrementEngagementCounters = async (req, res) => {
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
            'usageCount', 'likes', 'favorites', 'viewCount', 
            'sharedCount', 'downloadCount', 'reportCount', 'ratingCount'
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
        ).select('usageCount likes favorites viewCount sharedCount downloadCount reportCount rating ratingCount');
        
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
        
        logger.info(`Incremented engagement counters for template ${templateId}`, { increments: incrementUpdates });
        
        res.json({
            success: true,
            data: {
                templateId: template._id,
                incrementedFields: Object.keys(incrementUpdates),
                increments: incrementUpdates,
                currentMetrics: {
                    usageCount: template.usageCount,
                    likes: template.likes,
                    favorites: template.favorites,
                    viewCount: template.viewCount,
                    sharedCount: template.sharedCount,
                    downloadCount: template.downloadCount,
                    reportCount: template.reportCount,
                    rating: template.rating,
                    ratingCount: template.ratingCount,
                    trendingScore: template.trendingScore
                }
            },
            timestamp: new Date().toISOString()
        });
    }, res, 'incrementEngagementCounters');
};

/**
 * Get engagement leaderboard
 */
const getEngagementLeaderboard = async (req, res) => {
    await handleAsyncOperation(async () => {
        const { type = 'trendingScore' } = req.params;
        const limit = parseInt(req.query.limit) || 20;
        const skip = parseInt(req.query.skip) || 0;
        
        const validTypes = [
            'usageCount', 'likes', 'favorites', 'viewCount', 
            'sharedCount', 'downloadCount', 'rating', 'trendingScore'
        ];
        
        if (!validTypes.includes(type)) {
            return res.status(400).json({
                success: false,
                error: {
                    code: 'VALIDATION_ERROR',
                    message: 'Invalid leaderboard type',
                    field: 'type',
                    value: type,
                    details: { validTypes }
                },
                timestamp: new Date().toISOString()
            });
        }
        
        let templates;
        
        if (type === 'trendingScore') {
            // Use aggregation pipeline for virtual field
            templates = await Template.aggregate([
                {
                    $addFields: {
                        trendingScore: {
                            $subtract: [
                                {
                                    $add: [
                                        { $multiply: ['$usageCount', 3] },
                                        { $multiply: ['$likes', 2] },
                                        { $multiply: ['$favorites', 2] },
                                        '$viewCount',
                                        '$sharedCount',
                                        '$downloadCount'
                                    ]
                                },
                                { $multiply: ['$reportCount', 5] }
                            ]
                        }
                    }
                },
                { $sort: { trendingScore: -1 } },
                { $skip: skip },
                { $limit: limit },
                {
                    $project: {
                        title: 1,
                        category: 1,
                        usageCount: 1,
                        likes: 1,
                        favorites: 1,
                        viewCount: 1,
                        sharedCount: 1,
                        downloadCount: 1,
                        reportCount: 1,
                        rating: 1,
                        trendingScore: 1,
                        createdAt: 1
                    }
                }
            ]);
        } else {
            templates = await Template.find({ status: true })
                .select('title category usageCount likes favorites viewCount sharedCount downloadCount reportCount rating ratingCount createdAt')
                .sort({ [type]: -1 })
                .skip(skip)
                .limit(limit);
        }
        
        logger.info(`Retrieved engagement leaderboard for type: ${type}`);
        
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
    }, res, 'getEngagementLeaderboard');
};

/**
 * Get engagement statistics overview
 */
const getEngagementStats = async (req, res) => {
    await handleAsyncOperation(async () => {
        const stats = await Template.aggregate([
            {
                $group: {
                    _id: null,
                    totalTemplates: { $sum: 1 },
                    totalUsage: { $sum: '$usageCount' },
                    totalLikes: { $sum: '$likes' },
                    totalFavorites: { $sum: '$favorites' },
                    totalViews: { $sum: '$viewCount' },
                    totalShares: { $sum: '$sharedCount' },
                    totalDownloads: { $sum: '$downloadCount' },
                    totalReports: { $sum: '$reportCount' },
                    avgRating: { $avg: '$rating' },
                    totalRatings: { $sum: '$ratingCount' }
                }
            }
        ]);
        
        const topCategories = await Template.aggregate([
            {
                $group: {
                    _id: '$category',
                    count: { $sum: 1 },
                    totalUsage: { $sum: '$usageCount' },
                    totalLikes: { $sum: '$likes' },
                    avgRating: { $avg: '$rating' }
                }
            },
            { $sort: { totalUsage: -1 } },
            { $limit: 10 }
        ]);
        
        logger.info('Retrieved engagement statistics overview');
        
        res.json({
            success: true,
            data: {
                overview: stats[0] || {},
                topCategories
            },
            timestamp: new Date().toISOString()
        });
    }, res, 'getEngagementStats');
};

/**
 * Reset engagement metrics for a template
 */
const resetEngagementMetrics = async (req, res) => {
    await handleAsyncOperation(async () => {
        const { templateId } = req.params;
        const { fields } = req.body;
        
        const resetFields = {};
        const allowedFields = [
            'usageCount', 'likes', 'favorites', 'viewCount', 
            'sharedCount', 'downloadCount', 'reportCount', 'rating', 'ratingCount'
        ];
        
        if (fields && Array.isArray(fields)) {
            for (const field of fields) {
                if (allowedFields.includes(field)) {
                    resetFields[field] = 0;
                }
            }
        } else {
            // Reset all fields if none specified
            allowedFields.forEach(field => {
                resetFields[field] = 0;
            });
        }
        
        const template = await Template.findByIdAndUpdate(
            templateId,
            { $set: resetFields },
            { new: true, runValidators: true }
        ).select('usageCount likes favorites viewCount sharedCount downloadCount reportCount rating ratingCount');
        
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
        
        logger.info(`Reset engagement metrics for template ${templateId}`, { resetFields });
        
        res.json({
            success: true,
            data: {
                templateId: template._id,
                resetFields: Object.keys(resetFields),
                currentMetrics: {
                    usageCount: template.usageCount,
                    likes: template.likes,
                    favorites: template.favorites,
                    viewCount: template.viewCount,
                    sharedCount: template.sharedCount,
                    downloadCount: template.downloadCount,
                    reportCount: template.reportCount,
                    rating: template.rating,
                    ratingCount: template.ratingCount
                }
            },
            timestamp: new Date().toISOString()
        });
    }, res, 'resetEngagementMetrics');
};

/**
 * Get templates by engagement threshold
 */
const getTemplatesByEngagementThreshold = async (req, res) => {
    await handleAsyncOperation(async () => {
        const { metric, value } = req.params;
        const threshold = parseInt(value);
        const operator = req.query.operator || 'gte'; // gte, lte, eq
        
        const validMetrics = [
            'usageCount', 'likes', 'favorites', 'viewCount', 
            'sharedCount', 'downloadCount', 'reportCount', 'rating', 'ratingCount'
        ];
        
        if (!validMetrics.includes(metric)) {
            return res.status(400).json({
                success: false,
                error: {
                    code: 'VALIDATION_ERROR',
                    message: 'Invalid metric',
                    field: 'metric',
                    value: metric,
                    details: { validMetrics }
                },
                timestamp: new Date().toISOString()
            });
        }
        
        if (isNaN(threshold)) {
            return res.status(400).json({
                success: false,
                error: {
                    code: 'VALIDATION_ERROR',
                    message: 'Invalid threshold value',
                    field: 'value',
                    value: value
                },
                timestamp: new Date().toISOString()
            });
        }
        
        const operatorMap = {
            'gte': '$gte',
            'lte': '$lte',
            'eq': '$eq',
            'gt': '$gt',
            'lt': '$lt'
        };
        
        if (!operatorMap[operator]) {
            return res.status(400).json({
                success: false,
                error: {
                    code: 'VALIDATION_ERROR',
                    message: 'Invalid operator',
                    field: 'operator',
                    value: operator,
                    details: { validOperators: Object.keys(operatorMap) }
                },
                timestamp: new Date().toISOString()
            });
        }
        
        const query = {
            [metric]: { [operatorMap[operator]]: threshold }
        };
        
        const templates = await Template.find(query)
            .select('title category usageCount likes favorites viewCount sharedCount downloadCount reportCount rating ratingCount createdAt')
            .sort({ [metric]: -1 })
            .limit(100);
        
        logger.info(`Retrieved templates by ${metric} ${operator} ${threshold}`, { count: templates.length });
        
        res.json({
            success: true,
            data: {
                metric,
                operator,
                threshold,
                templates,
                count: templates.length
            },
            timestamp: new Date().toISOString()
        });
    }, res, 'getTemplatesByEngagementThreshold');
};

/**
 * Update rating for a template
 */
const updateTemplateRating = async (req, res) => {
    await handleAsyncOperation(async () => {
        const { templateId } = req.params;
        const { rating, increment = true } = req.body;
        
        if (typeof rating !== 'number' || rating < 0 || rating > 5) {
            return res.status(400).json({
                success: false,
                error: {
                    code: 'VALIDATION_ERROR',
                    message: 'Rating must be a number between 0 and 5',
                    field: 'rating',
                    value: rating
                },
                timestamp: new Date().toISOString()
            });
        }
        
        const template = await Template.findById(templateId);
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
        
        let newRating, newRatingCount;
        
        if (increment) {
            // Calculate new average rating
            const totalRating = (template.rating * template.ratingCount) + rating;
            newRatingCount = template.ratingCount + 1;
            newRating = totalRating / newRatingCount;
        } else {
            // Set absolute rating
            newRating = rating;
            newRatingCount = template.ratingCount;
        }
        
        const updatedTemplate = await Template.findByIdAndUpdate(
            templateId,
            { 
                $set: { 
                    rating: newRating,
                    ratingCount: newRatingCount
                }
            },
            { new: true, runValidators: true }
        ).select('rating ratingCount');
        
        logger.info(`Updated rating for template ${templateId}`, { 
            oldRating: template.rating,
            newRating: updatedTemplate.rating,
            ratingCount: updatedTemplate.ratingCount
        });
        
        res.json({
            success: true,
            data: {
                templateId: updatedTemplate._id,
                rating: updatedTemplate.rating,
                ratingCount: updatedTemplate.ratingCount,
                previousRating: template.rating,
                previousRatingCount: template.ratingCount
            },
            timestamp: new Date().toISOString()
        });
    }, res, 'updateTemplateRating');
};

/**
 * Get engagement history/trends (placeholder for future implementation)
 */
const getEngagementHistory = async (req, res) => {
    await handleAsyncOperation(async () => {
        const { templateId } = req.params;
        const { period = '7d' } = req.query;
        
        // For now, return current metrics
        // In the future, this could track historical data
        const template = await Template.findById(templateId).select(
            'usageCount likes favorites viewCount sharedCount downloadCount reportCount rating ratingCount createdAt updatedAt'
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
        
        logger.info(`Retrieved engagement history for template ${templateId}`);
        
        res.json({
            success: true,
            data: {
                templateId: template._id,
                period,
                currentMetrics: {
                    usageCount: template.usageCount,
                    likes: template.likes,
                    favorites: template.favorites,
                    viewCount: template.viewCount,
                    sharedCount: template.sharedCount,
                    downloadCount: template.downloadCount,
                    reportCount: template.reportCount,
                    rating: template.rating,
                    ratingCount: template.ratingCount
                },
                timestamps: {
                    created: template.createdAt,
                    lastUpdated: template.updatedAt
                },
                note: 'Historical tracking not yet implemented'
            },
            timestamp: new Date().toISOString()
        });
    }, res, 'getEngagementHistory');
};

/**
 * Bulk update engagement metrics
 */
const bulkUpdateEngagementMetrics = async (req, res) => {
    await handleAsyncOperation(async () => {
        const { updates } = req.body;
        
        if (!Array.isArray(updates) || updates.length === 0) {
            return res.status(400).json({
                success: false,
                error: {
                    code: 'VALIDATION_ERROR',
                    message: 'updates must be a non-empty array',
                    field: 'updates'
                },
                timestamp: new Date().toISOString()
            });
        }
        
        const results = [];
        const errors = [];
        
        for (const update of updates) {
            try {
                const { templateId, metrics } = update;
                
                if (!templateId || !metrics) {
                    errors.push({
                        templateId,
                        error: 'templateId and metrics are required'
                    });
                    continue;
                }
                
                const template = await Template.findByIdAndUpdate(
                    templateId,
                    { $set: metrics },
                    { new: true, runValidators: true }
                ).select('usageCount likes favorites viewCount sharedCount downloadCount reportCount rating ratingCount');
                
                if (template) {
                    results.push({
                        templateId: template._id,
                        success: true,
                        updatedMetrics: metrics
                    });
                } else {
                    errors.push({
                        templateId,
                        error: 'Template not found'
                    });
                }
            } catch (error) {
                errors.push({
                    templateId: update.templateId,
                    error: error.message
                });
            }
        }
        
        logger.info(`Bulk update engagement metrics completed`, { 
            successful: results.length, 
            failed: errors.length 
        });
        
        res.json({
            success: true,
            data: {
                successful: results.length,
                failed: errors.length,
                results,
                errors
            },
            timestamp: new Date().toISOString()
        });
    }, res, 'bulkUpdateEngagementMetrics');
};

module.exports = {
    getEngagementMetrics,
    updateEngagementMetrics,
    incrementEngagementCounters,
    getEngagementLeaderboard,
    getEngagementStats,
    resetEngagementMetrics,
    getTemplatesByEngagementThreshold,
    updateTemplateRating,
    getEngagementHistory,
    bulkUpdateEngagementMetrics
}; 