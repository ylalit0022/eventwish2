const Template = require('../../../models/Template');
const logger = require('../../../utils/logger');
const { handleAsyncOperation } = require('../../../utils/templateHelpers');

/**
 * Get comprehensive analytics data for a template
 */
const getTemplateAnalytics = async (req, res) => {
    await handleAsyncOperation(async () => {
        const { templateId } = req.params;
        
        const template = await Template.findById(templateId).select(
            'title category usageCount likes favorites viewCount sharedCount downloadCount reportCount rating ratingCount ' +
            'weeklyUsageCount weeklyLikes weeklyFavorites weeklyViewCount weeklySharedCount weeklyDownloadCount weeklyReportCount ' +
            'weeklyScoreLastReset performanceLog createdAt updatedAt'
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
        
        // Calculate engagement rates
        const totalEngagement = template.likes + template.favorites + template.sharedCount;
        const engagementRate = template.viewCount > 0 ? (totalEngagement / template.viewCount) * 100 : 0;
        
        const analyticsData = {
            templateId: template._id,
            title: template.title,
            category: template.category,
            
            // Overall metrics
            overallMetrics: {
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
            },
            
            // Performance analytics
            performanceAnalytics: {
                engagementRate: Math.round(engagementRate * 100) / 100,
                performanceLog: template.performanceLog
            },
            
            // Timestamps
            timestamps: {
                created: template.createdAt,
                lastUpdated: template.updatedAt
            }
        };
        
        logger.info(`Retrieved analytics data for template ${templateId}`);
        
        res.json({
            success: true,
            data: analyticsData,
            timestamp: new Date().toISOString()
        });
    }, res, 'getTemplateAnalytics');
};

/**
 * Get trending score for a template
 */
const getTrendingScore = async (req, res) => {
    await handleAsyncOperation(async () => {
        const { templateId } = req.params;
        
        const template = await Template.findById(templateId).select(
            'usageCount likes favorites viewCount sharedCount downloadCount reportCount'
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
        
        const score = template.trendingScore;
        
        logger.info(`Retrieved trending score for template ${templateId}: ${score}`);
        
        res.json({
            success: true,
            data: {
                templateId: template._id,
                trendingScore: score,
                components: {
                    usageCount: template.usageCount,
                    likes: template.likes,
                    favorites: template.favorites,
                    viewCount: template.viewCount,
                    sharedCount: template.sharedCount,
                    downloadCount: template.downloadCount,
                    reportCount: template.reportCount
                }
            },
            timestamp: new Date().toISOString()
        });
    }, res, 'getTrendingScore');
};

/**
 * Get top trending templates
 */
const getTopTrendingTemplates = async (req, res) => {
    await handleAsyncOperation(async () => {
        const limit = parseInt(req.params.limit) || 50;
        const category = req.query.category;
        
        let matchStage = { status: true };
        if (category) {
            matchStage.category = category;
        }
        
        const templates = await Template.aggregate([
            { $match: matchStage },
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
        
        logger.info(`Retrieved top ${limit} trending templates`, { category });
        
        res.json({
            success: true,
            data: {
                templates,
                filters: {
                    limit,
                    category
                },
                count: templates.length
            },
            timestamp: new Date().toISOString()
        });
    }, res, 'getTopTrendingTemplates');
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
                },
                calculation: {
                    formula: '(weeklyUsageCount * 3) + (weeklyLikes * 2) + (weeklyFavorites * 2) + weeklyViewCount + weeklySharedCount + weeklyDownloadCount - (weeklyReportCount * 5)',
                    breakdown: {
                        weeklyUsagePoints: template.weeklyUsageCount * 3,
                        weeklyLikesPoints: template.weeklyLikes * 2,
                        weeklyFavoritesPoints: template.weeklyFavorites * 2,
                        weeklyViewPoints: template.weeklyViewCount,
                        weeklySharePoints: template.weeklySharedCount,
                        weeklyDownloadPoints: template.weeklyDownloadCount,
                        weeklyReportPenalty: template.weeklyReportCount * 5
                    }
                }
            },
            timestamp: new Date().toISOString()
        });
    }, res, 'getWeeklyTrendingScore');
};

/**
 * Get top weekly trending templates
 */
const getTopWeeklyTrendingTemplates = async (req, res) => {
    await handleAsyncOperation(async () => {
        const limit = parseInt(req.params.limit) || 50;
        const category = req.query.category;
        const minScore = parseInt(req.query.minScore) || 0;
        
        let matchStage = { status: true };
        if (category) {
            matchStage.category = category;
        }
        
        const templates = await Template.aggregate([
            { $match: matchStage },
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
            { $match: { weeklyTrendingScore: { $gte: minScore } } },
            { $sort: { weeklyTrendingScore: -1 } },
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
        
        logger.info(`Retrieved top ${limit} weekly trending templates`, { category, minScore });
        
        res.json({
            success: true,
            data: {
                templates,
                filters: {
                    limit,
                    category,
                    minScore
                },
                count: templates.length
            },
            timestamp: new Date().toISOString()
        });
    }, res, 'getTopWeeklyTrendingTemplates');
};

/**
 * Compare analytics between multiple templates
 */
const compareTemplateAnalytics = async (req, res) => {
    await handleAsyncOperation(async () => {
        const { templateIds } = req.body;
        
        if (!Array.isArray(templateIds) || templateIds.length === 0) {
            return res.status(400).json({
                success: false,
                error: {
                    code: 'VALIDATION_ERROR',
                    message: 'templateIds must be a non-empty array',
                    field: 'templateIds'
                },
                timestamp: new Date().toISOString()
            });
        }
        
        if (templateIds.length > 10) {
            return res.status(400).json({
                success: false,
                error: {
                    code: 'VALIDATION_ERROR',
                    message: 'Maximum 10 templates can be compared at once',
                    field: 'templateIds'
                },
                timestamp: new Date().toISOString()
            });
        }
        
        const templates = await Template.find({ _id: { $in: templateIds } }).select(
            'title category usageCount likes favorites viewCount sharedCount downloadCount reportCount rating ratingCount ' +
            'weeklyUsageCount weeklyLikes weeklyFavorites weeklyViewCount weeklySharedCount weeklyDownloadCount weeklyReportCount ' +
            'createdAt'
        );
        
        const comparison = templates.map(template => {
            const totalEngagement = template.likes + template.favorites + template.sharedCount;
            const engagementRate = template.viewCount > 0 ? (totalEngagement / template.viewCount) * 100 : 0;
            
            return {
                templateId: template._id,
                title: template.title,
                category: template.category,
                metrics: {
                    overall: {
                        usageCount: template.usageCount,
                        likes: template.likes,
                        favorites: template.favorites,
                        viewCount: template.viewCount,
                        sharedCount: template.sharedCount,
                        downloadCount: template.downloadCount,
                        reportCount: template.reportCount,
                        rating: template.rating,
                        trendingScore: template.trendingScore,
                        engagementRate: Math.round(engagementRate * 100) / 100
                    },
                    weekly: {
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
                createdAt: template.createdAt
            };
        });
        
        // Calculate comparison insights
        const insights = {
            totalTemplates: comparison.length,
            highestTrendingScore: Math.max(...comparison.map(t => t.metrics.overall.trendingScore)),
            highestWeeklyTrendingScore: Math.max(...comparison.map(t => t.metrics.weekly.weeklyTrendingScore)),
            avgEngagementRate: comparison.reduce((sum, t) => sum + t.metrics.overall.engagementRate, 0) / comparison.length,
            mostPopularCategory: comparison.reduce((acc, t) => {
                acc[t.category] = (acc[t.category] || 0) + 1;
                return acc;
            }, {})
        };
        
        logger.info(`Compared analytics for ${templateIds.length} templates`);
        
        res.json({
            success: true,
            data: {
                comparison,
                insights,
                requestedIds: templateIds,
                foundTemplates: templates.length
            },
            timestamp: new Date().toISOString()
        });
    }, res, 'compareTemplateAnalytics');
};

/**
 * Get analytics dashboard overview
 */
const getAnalyticsDashboard = async (req, res) => {
    await handleAsyncOperation(async () => {
        const dashboardData = await Template.aggregate([
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
                    
                    totalWeeklyUsage: { $sum: '$weeklyUsageCount' },
                    totalWeeklyLikes: { $sum: '$weeklyLikes' },
                    totalWeeklyFavorites: { $sum: '$weeklyFavorites' },
                    totalWeeklyViews: { $sum: '$weeklyViewCount' },
                    totalWeeklyShares: { $sum: '$weeklySharedCount' },
                    totalWeeklyDownloads: { $sum: '$weeklyDownloadCount' },
                    totalWeeklyReports: { $sum: '$weeklyReportCount' }
                }
            }
        ]);
        
        const categoryStats = await Template.aggregate([
            {
                $group: {
                    _id: '$category',
                    count: { $sum: 1 },
                    totalUsage: { $sum: '$usageCount' },
                    totalLikes: { $sum: '$likes' },
                    avgRating: { $avg: '$rating' },
                    totalWeeklyUsage: { $sum: '$weeklyUsageCount' }
                }
            },
            { $sort: { totalUsage: -1 } },
            { $limit: 10 }
        ]);
        
        const topTemplates = await Template.aggregate([
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
            { $limit: 5 },
            {
                $project: {
                    title: 1,
                    category: 1,
                    usageCount: 1,
                    likes: 1,
                    trendingScore: 1
                }
            }
        ]);
        
        logger.info('Retrieved analytics dashboard overview');
        
        res.json({
            success: true,
            data: {
                overview: dashboardData[0] || {},
                categoryStats,
                topTemplates,
                generatedAt: new Date().toISOString()
            },
            timestamp: new Date().toISOString()
        });
    }, res, 'getAnalyticsDashboard');
};

module.exports = {
    getTemplateAnalytics,
    getTrendingScore,
    getWeeklyTrendingScore,
    getTopTrendingTemplates,
    getTopWeeklyTrendingTemplates,
    compareTemplateAnalytics,
    getAnalyticsDashboard
}; 