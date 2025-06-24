const Template = require('../../../models/Template');
const logger = require('../../../utils/logger');
const { handleAsyncOperation } = require('../../../utils/templateHelpers');

/**
 * Get performance metrics for a template
 */
const getPerformanceMetrics = async (req, res) => {
    await handleAsyncOperation(async () => {
        const { templateId } = req.params;
        
        const template = await Template.findById(templateId).select(
            'title category performanceLog createdAt updatedAt'
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
        
        const performanceData = {
            templateId: template._id,
            title: template.title,
            category: template.category,
            performanceLog: template.performanceLog || {
                dailyUsage: 0,
                weeklyUsage: 0,
                lastUsedAt: null
            },
            timestamps: {
                created: template.createdAt,
                lastUpdated: template.updatedAt
            }
        };
        
        logger.info(`Retrieved performance metrics for template ${templateId}`);
        
        res.json({
            success: true,
            data: performanceData,
            timestamp: new Date().toISOString()
        });
    }, res, 'getPerformanceMetrics');
};

/**
 * Update performance log for a template
 */
const updatePerformanceLog = async (req, res) => {
    await handleAsyncOperation(async () => {
        const { templateId } = req.params;
        const updates = req.validatedPerformanceLog;
        
        const updateQuery = {};
        for (const field in updates) {
            updateQuery[`performanceLog.${field}`] = updates[field];
        }
        
        const template = await Template.findByIdAndUpdate(
            templateId,
            { $set: updateQuery },
            { new: true, runValidators: true }
        ).select('performanceLog');
        
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
        
        logger.info(`Updated performance log for template ${templateId}`, { updates });
        
        res.json({
            success: true,
            data: {
                templateId: template._id,
                updatedFields: Object.keys(updates),
                newValues: updates,
                currentPerformanceLog: template.performanceLog
            },
            timestamp: new Date().toISOString()
        });
    }, res, 'updatePerformanceLog');
};

/**
 * Record template usage (updates lastUsedAt and usage counters)
 */
const recordTemplateUsage = async (req, res) => {
    await handleAsyncOperation(async () => {
        const { templateId } = req.params;
        const { incrementDaily = 1, incrementWeekly = 1 } = req.body;
        
        const updateQuery = {
            'performanceLog.lastUsedAt': new Date()
        };
        
        const incrementQuery = {};
        if (incrementDaily > 0) {
            incrementQuery['performanceLog.dailyUsage'] = incrementDaily;
        }
        if (incrementWeekly > 0) {
            incrementQuery['performanceLog.weeklyUsage'] = incrementWeekly;
        }
        
        const template = await Template.findByIdAndUpdate(
            templateId,
            { 
                $set: updateQuery,
                $inc: incrementQuery
            },
            { new: true, runValidators: true }
        ).select('performanceLog');
        
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
        
        logger.info(`Recorded usage for template ${templateId}`, { 
            incrementDaily, 
            incrementWeekly,
            lastUsedAt: updateQuery['performanceLog.lastUsedAt']
        });
        
        res.json({
            success: true,
            data: {
                templateId: template._id,
                recorded: {
                    dailyIncrement: incrementDaily,
                    weeklyIncrement: incrementWeekly,
                    lastUsedAt: updateQuery['performanceLog.lastUsedAt']
                },
                currentPerformanceLog: template.performanceLog
            },
            timestamp: new Date().toISOString()
        });
    }, res, 'recordTemplateUsage');
};

/**
 * Get performance summary
 */
const getPerformanceSummary = async (req, res) => {
    await handleAsyncOperation(async () => {
        const { templateId } = req.params;
        
        const template = await Template.findById(templateId).select(
            'title category performanceLog usageCount createdAt'
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
        
        const performanceLog = template.performanceLog || {};
        const daysSinceCreated = Math.floor((new Date() - template.createdAt) / (1000 * 60 * 60 * 24));
        const avgDailyUsage = daysSinceCreated > 0 ? template.usageCount / daysSinceCreated : 0;
        
        const summary = {
            templateId: template._id,
            title: template.title,
            category: template.category,
            performanceMetrics: {
                totalUsage: template.usageCount,
                dailyUsage: performanceLog.dailyUsage || 0,
                weeklyUsage: performanceLog.weeklyUsage || 0,
                lastUsedAt: performanceLog.lastUsedAt,
                avgDailyUsage: Math.round(avgDailyUsage * 100) / 100,
                daysSinceCreated
            },
            insights: {
                isActive: performanceLog.lastUsedAt && 
                         (new Date() - new Date(performanceLog.lastUsedAt)) < (7 * 24 * 60 * 60 * 1000),
                performanceLevel: avgDailyUsage > 10 ? 'high' : avgDailyUsage > 1 ? 'medium' : 'low'
            }
        };
        
        logger.info(`Retrieved performance summary for template ${templateId}`);
        
        res.json({
            success: true,
            data: summary,
            timestamp: new Date().toISOString()
        });
    }, res, 'getPerformanceSummary');
};

/**
 * Get performance leaderboard
 */
const getPerformanceLeaderboard = async (req, res) => {
    await handleAsyncOperation(async () => {
        const { metric, period = 'all' } = req.params;
        const limit = parseInt(req.query.limit) || 20;
        const skip = parseInt(req.query.skip) || 0;
        
        const validMetrics = ['dailyUsage', 'weeklyUsage', 'totalUsage'];
        const validPeriods = ['all', 'active']; // active = used in last 7 days
        
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
        
        if (!validPeriods.includes(period)) {
            return res.status(400).json({
                success: false,
                error: {
                    code: 'VALIDATION_ERROR',
                    message: 'Invalid period',
                    field: 'period',
                    value: period,
                    details: { validPeriods }
                },
                timestamp: new Date().toISOString()
            });
        }
        
        let matchStage = { status: true };
        if (period === 'active') {
            const sevenDaysAgo = new Date(Date.now() - 7 * 24 * 60 * 60 * 1000);
            matchStage['performanceLog.lastUsedAt'] = { $gte: sevenDaysAgo };
        }
        
        let sortField;
        if (metric === 'totalUsage') {
            sortField = 'usageCount';
        } else {
            sortField = `performanceLog.${metric}`;
        }
        
        const templates = await Template.find(matchStage)
            .select('title category usageCount performanceLog createdAt')
            .sort({ [sortField]: -1 })
            .skip(skip)
            .limit(limit);
        
        logger.info(`Retrieved performance leaderboard for ${metric} (${period})`, { count: templates.length });
        
        res.json({
            success: true,
            data: {
                metric,
                period,
                templates,
                pagination: {
                    limit,
                    skip,
                    count: templates.length
                }
            },
            timestamp: new Date().toISOString()
        });
    }, res, 'getPerformanceLeaderboard');
};

/**
 * Get performance statistics overview
 */
const getPerformanceStats = async (req, res) => {
    await handleAsyncOperation(async () => {
        const stats = await Template.aggregate([
            {
                $group: {
                    _id: null,
                    totalTemplates: { $sum: 1 },
                    avgDailyUsage: { $avg: '$performanceLog.dailyUsage' },
                    avgWeeklyUsage: { $avg: '$performanceLog.weeklyUsage' },
                    totalDailyUsage: { $sum: '$performanceLog.dailyUsage' },
                    totalWeeklyUsage: { $sum: '$performanceLog.weeklyUsage' }
                }
            }
        ]);
        
        const activeTemplates = await Template.countDocuments({
            'performanceLog.lastUsedAt': { 
                $gte: new Date(Date.now() - 7 * 24 * 60 * 60 * 1000) 
            }
        });
        
        const topPerformers = await Template.find()
            .select('title category performanceLog.dailyUsage performanceLog.weeklyUsage')
            .sort({ 'performanceLog.dailyUsage': -1 })
            .limit(5);
        
        logger.info('Retrieved performance statistics overview');
        
        res.json({
            success: true,
            data: {
                overview: stats[0] || {},
                activeTemplatesLast7Days: activeTemplates,
                topPerformers
            },
            timestamp: new Date().toISOString()
        });
    }, res, 'getPerformanceStats');
};

module.exports = {
    getPerformanceMetrics,
    updatePerformanceLog,
    recordTemplateUsage,
    getPerformanceSummary,
    getPerformanceLeaderboard,
    getPerformanceStats
}; 