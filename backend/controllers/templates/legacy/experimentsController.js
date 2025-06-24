const Template = require('../../../models/Template');
const logger = require('../../../utils/logger');
const { handleAsyncOperation, createSuccessResponse, createErrorResponse } = require('../../../utils/templateHelpers');

/**
 * Add experiment tag to template
 */
const addExperimentTag = async (req, res) => {
    await handleAsyncOperation(res, async () => {
        const { id } = req.params;
        const { experimentTag, experimentData } = req.body;

        if (!experimentTag || typeof experimentTag !== 'string') {
            return createErrorResponse(
                'Experiment tag is required and must be a string',
                'VALIDATION_ERROR',
                'experimentTag',
                experimentTag
            );
        }

        const template = await Template.findByIdAndUpdate(
            id,
            { 
                experimentTag,
                experimentData: experimentData || {},
                updatedAt: new Date()
            },
            { new: true, runValidators: true }
        );

        if (!template) {
            return createErrorResponse('Template not found', 'TEMPLATE_NOT_FOUND', 'id', id);
        }

        logger.info(`Added experiment tag to template ${id}`, {
            templateId: id,
            experimentTag,
            experimentData
        });

        return createSuccessResponse({
            template: {
                id: template._id,
                experimentTag: template.experimentTag,
                experimentData: template.experimentData,
                updatedAt: template.updatedAt
            }
        }, 'Experiment tag added successfully');
    });
};

/**
 * Remove experiment tag from template
 */
const removeExperimentTag = async (req, res) => {
    await handleAsyncOperation(res, async () => {
        const { id, tag } = req.params;

        const template = await Template.findById(id);

        if (!template) {
            return createErrorResponse('Template not found', 'TEMPLATE_NOT_FOUND', 'id', id);
        }

        if (template.experimentTag !== tag) {
            return createErrorResponse(
                'Experiment tag does not match',
                'TAG_MISMATCH',
                'experimentTag',
                { current: template.experimentTag, requested: tag }
            );
        }

        template.experimentTag = undefined;
        template.experimentData = undefined;
        template.updatedAt = new Date();

        const updatedTemplate = await template.save();

        logger.info(`Removed experiment tag from template ${id}`, {
            templateId: id,
            removedTag: tag
        });

        return createSuccessResponse({
            template: {
                id: updatedTemplate._id,
                updatedAt: updatedTemplate.updatedAt
            }
        }, 'Experiment tag removed successfully');
    });
};

/**
 * Get experiment tags for template
 */
const getExperimentTags = async (req, res) => {
    await handleAsyncOperation(res, async () => {
        const { id } = req.params;

        const template = await Template.findById(id).select('experimentTag experimentData');

        if (!template) {
            return createErrorResponse('Template not found', 'TEMPLATE_NOT_FOUND', 'id', id);
        }

        return createSuccessResponse({
            templateId: id,
            experimentTag: template.experimentTag,
            experimentData: template.experimentData || {},
            hasActiveExperiment: !!template.experimentTag
        });
    });
};

/**
 * Update performance log for template
 */
const updatePerformanceLog = async (req, res) => {
    await handleAsyncOperation(res, async () => {
        const { id } = req.params;
        const performanceLog = req.validatedPerformanceLog || req.body.performanceLog;

        const template = await Template.findByIdAndUpdate(
            id,
            { 
                performanceLog: {
                    ...performanceLog,
                    lastUpdated: new Date()
                },
                updatedAt: new Date()
            },
            { new: true, runValidators: true }
        );

        if (!template) {
            return createErrorResponse('Template not found', 'TEMPLATE_NOT_FOUND', 'id', id);
        }

        logger.info(`Updated performance log for template ${id}`, {
            templateId: id,
            performanceLog
        });

        return createSuccessResponse({
            template: {
                id: template._id,
                performanceLog: template.performanceLog,
                updatedAt: template.updatedAt
            }
        }, 'Performance log updated successfully');
    });
};

/**
 * Get performance log for template
 */
const getPerformanceLog = async (req, res) => {
    await handleAsyncOperation(res, async () => {
        const { id } = req.params;

        const template = await Template.findById(id).select('performanceLog');

        if (!template) {
            return createErrorResponse('Template not found', 'TEMPLATE_NOT_FOUND', 'id', id);
        }

        const performanceLog = template.performanceLog || {};
        const performanceMetrics = {
            dailyUsage: performanceLog.dailyUsage || 0,
            weeklyUsage: performanceLog.weeklyUsage || 0,
            lastUsedAt: performanceLog.lastUsedAt,
            lastUpdated: performanceLog.lastUpdated,
            performanceScore: calculatePerformanceScore(performanceLog)
        };

        return createSuccessResponse({
            templateId: id,
            performanceLog: performanceMetrics
        });
    });
};

/**
 * Analyze experiment results
 */
const analyzeExperimentResults = async (req, res) => {
    await handleAsyncOperation(res, async () => {
        const { experimentTag, dateRange } = req.body;

        if (!experimentTag) {
            return createErrorResponse(
                'Experiment tag is required',
                'VALIDATION_ERROR',
                'experimentTag',
                experimentTag
            );
        }

        // Build query for experiment analysis
        const query = { experimentTag };
        if (dateRange && dateRange.start && dateRange.end) {
            query.updatedAt = {
                $gte: new Date(dateRange.start),
                $lte: new Date(dateRange.end)
            };
        }

        const templates = await Template.find(query).select(
            'experimentTag experimentData performanceLog usageCount likes favorites viewCount sharedCount'
        );

        if (templates.length === 0) {
            return createSuccessResponse({
                experimentTag,
                templatesFound: 0,
                analysis: null
            }, 'No templates found for experiment');
        }

        // Analyze experiment results
        const analysis = {
            experimentTag,
            templatesCount: templates.length,
            metrics: {
                totalUsage: templates.reduce((sum, t) => sum + (t.usageCount || 0), 0),
                totalLikes: templates.reduce((sum, t) => sum + (t.likes || 0), 0),
                totalFavorites: templates.reduce((sum, t) => sum + (t.favorites || 0), 0),
                totalViews: templates.reduce((sum, t) => sum + (t.viewCount || 0), 0),
                totalShares: templates.reduce((sum, t) => sum + (t.sharedCount || 0), 0)
            },
            averages: {},
            performanceDistribution: {}
        };

        // Calculate averages
        analysis.averages = {
            usagePerTemplate: analysis.metrics.totalUsage / templates.length,
            likesPerTemplate: analysis.metrics.totalLikes / templates.length,
            favoritesPerTemplate: analysis.metrics.totalFavorites / templates.length,
            viewsPerTemplate: analysis.metrics.totalViews / templates.length,
            sharesPerTemplate: analysis.metrics.totalShares / templates.length
        };

        // Performance distribution
        const performanceScores = templates.map(t => calculatePerformanceScore(t.performanceLog || {}));
        analysis.performanceDistribution = {
            min: Math.min(...performanceScores),
            max: Math.max(...performanceScores),
            average: performanceScores.reduce((sum, score) => sum + score, 0) / performanceScores.length
        };

        logger.info(`Analyzed experiment results for tag: ${experimentTag}`, {
            experimentTag,
            templatesAnalyzed: templates.length
        });

        return createSuccessResponse({
            analysis,
            dateRange: dateRange || null
        }, 'Experiment analysis completed');
    });
};

/**
 * Get experiment statistics
 */
const getExperimentStats = async (req, res) => {
    await handleAsyncOperation(res, async () => {
        const pipeline = [
            { $match: { experimentTag: { $exists: true, $ne: null } } },
            {
                $group: {
                    _id: '$experimentTag',
                    count: { $sum: 1 },
                    totalUsage: { $sum: '$usageCount' },
                    totalLikes: { $sum: '$likes' },
                    templates: { $push: '$_id' }
                }
            },
            {
                $project: {
                    experimentTag: '$_id',
                    templateCount: '$count',
                    totalUsage: 1,
                    totalLikes: 1,
                    averageUsage: { $divide: ['$totalUsage', '$count'] },
                    averageLikes: { $divide: ['$totalLikes', '$count'] },
                    _id: 0
                }
            },
            { $sort: { templateCount: -1 } }
        ];

        const experimentStats = await Template.aggregate(pipeline);

        const totalExperimentTemplates = await Template.countDocuments({
            experimentTag: { $exists: true, $ne: null }
        });

        return createSuccessResponse({
            totalExperimentTemplates,
            activeExperiments: experimentStats.length,
            experimentStats
        });
    });
};

/**
 * Create new experiment
 */
const createExperiment = async (req, res) => {
    await handleAsyncOperation(res, async () => {
        const { experimentTag, templateIds, experimentConfig } = req.body;

        if (!experimentTag || !Array.isArray(templateIds) || templateIds.length === 0) {
            return createErrorResponse(
                'Experiment tag and template IDs array are required',
                'VALIDATION_ERROR',
                'experiment',
                { experimentTag, templateIds }
            );
        }

        // Update templates with experiment tag
        const updateResult = await Template.updateMany(
            { _id: { $in: templateIds } },
            {
                experimentTag,
                experimentData: {
                    ...experimentConfig,
                    startedAt: new Date(),
                    status: 'active'
                },
                updatedAt: new Date()
            }
        );

        logger.info(`Created experiment: ${experimentTag}`, {
            experimentTag,
            templatesUpdated: updateResult.modifiedCount,
            experimentConfig
        });

        return createSuccessResponse({
            experimentTag,
            templatesUpdated: updateResult.modifiedCount,
            experimentConfig: {
                ...experimentConfig,
                startedAt: new Date(),
                status: 'active'
            }
        }, `Experiment created with ${updateResult.modifiedCount} templates`);
    });
};

/**
 * End experiment
 */
const endExperiment = async (req, res) => {
    await handleAsyncOperation(res, async () => {
        const { experimentId } = req.params;

        // Find templates with this experiment
        const templates = await Template.find({ experimentTag: experimentId });

        if (templates.length === 0) {
            return createErrorResponse(
                'No templates found for this experiment',
                'EXPERIMENT_NOT_FOUND',
                'experimentId',
                experimentId
            );
        }

        // End experiment for all templates
        const updateResult = await Template.updateMany(
            { experimentTag: experimentId },
            {
                $set: {
                    'experimentData.status': 'ended',
                    'experimentData.endedAt': new Date()
                },
                updatedAt: new Date()
            }
        );

        logger.info(`Ended experiment: ${experimentId}`, {
            experimentId,
            templatesUpdated: updateResult.modifiedCount
        });

        return createSuccessResponse({
            experimentId,
            templatesUpdated: updateResult.modifiedCount,
            endedAt: new Date()
        }, `Experiment ended for ${updateResult.modifiedCount} templates`);
    });
};

/**
 * Get active experiments
 */
const getActiveExperiments = async (req, res) => {
    await handleAsyncOperation(res, async () => {
        const activeExperiments = await Template.aggregate([
            {
                $match: {
                    experimentTag: { $exists: true, $ne: null },
                    'experimentData.status': 'active'
                }
            },
            {
                $group: {
                    _id: '$experimentTag',
                    templateCount: { $sum: 1 },
                    startedAt: { $min: '$experimentData.startedAt' },
                    experimentConfig: { $first: '$experimentData' }
                }
            },
            {
                $project: {
                    experimentTag: '$_id',
                    templateCount: 1,
                    startedAt: 1,
                    experimentConfig: 1,
                    _id: 0
                }
            },
            { $sort: { startedAt: -1 } }
        ]);

        return createSuccessResponse({
            activeExperiments,
            totalActiveExperiments: activeExperiments.length
        });
    });
};

/**
 * Calculate performance score based on performance log
 */
const calculatePerformanceScore = (performanceLog) => {
    if (!performanceLog || typeof performanceLog !== 'object') {
        return 0;
    }

    const dailyUsage = performanceLog.dailyUsage || 0;
    const weeklyUsage = performanceLog.weeklyUsage || 0;
    
    // Simple scoring algorithm - can be made more sophisticated
    const dailyScore = Math.min(dailyUsage * 10, 50); // Max 50 points for daily usage
    const weeklyScore = Math.min(weeklyUsage * 2, 50); // Max 50 points for weekly usage
    
    return Math.round(dailyScore + weeklyScore);
};

module.exports = {
    addExperimentTag,
    removeExperimentTag,
    getExperimentTags,
    updatePerformanceLog,
    getPerformanceLog,
    analyzeExperimentResults,
    getExperimentStats,
    createExperiment,
    endExperiment,
    getActiveExperiments
}; 