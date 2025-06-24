const Template = require('../../../models/Template');
const logger = require('../../../utils/logger');
const { handleAsyncOperation, createSuccessResponse, createErrorResponse } = require('../../../utils/templateHelpers');

/**
 * Update template visibility score
 */
const updateVisibilityScore = async (req, res) => {
    await handleAsyncOperation(res, async () => {
        const { id } = req.params;
        const { visibilityScore } = req.body;

        const template = await Template.findByIdAndUpdate(
            id,
            { 
                visibilityScore,
                updatedAt: new Date()
            },
            { new: true, runValidators: true }
        );

        if (!template) {
            return createErrorResponse('Template not found', 'TEMPLATE_NOT_FOUND', 'id', id);
        }

        logger.info(`Visibility score updated for template ${id}: ${visibilityScore}`);

        return createSuccessResponse({
            template: {
                _id: template._id,
                visibilityScore: template.visibilityScore,
                updatedAt: template.updatedAt
            },
            modified: ['visibilityScore']
        });
    });
};

/**
 * Update template boost priority
 */
const updateBoostPriority = async (req, res) => {
    await handleAsyncOperation(res, async () => {
        const { id } = req.params;
        const { boostPriority } = req.body;

        const template = await Template.findByIdAndUpdate(
            id,
            { 
                boostPriority,
                updatedAt: new Date()
            },
            { new: true, runValidators: true }
        );

        if (!template) {
            return createErrorResponse('Template not found', 'TEMPLATE_NOT_FOUND', 'id', id);
        }

        logger.info(`Boost priority updated for template ${id}: ${boostPriority}`);

        return createSuccessResponse({
            template: {
                _id: template._id,
                boostPriority: template.boostPriority,
                updatedAt: template.updatedAt
            },
            modified: ['boostPriority']
        });
    });
};

/**
 * Get visibility metrics for a template
 */
const getVisibilityMetrics = async (req, res) => {
    await handleAsyncOperation(res, async () => {
        const { id } = req.params;

        const template = await Template.findById(id).select(
            'visibilityScore boostPriority lastBoostedAt usageCount likes favorites viewCount trendingScore'
        );

        if (!template) {
            return createErrorResponse('Template not found', 'TEMPLATE_NOT_FOUND', 'id', id);
        }

        const metrics = {
            visibilityScore: template.visibilityScore || 0,
            boostPriority: template.boostPriority || 0,
            lastBoostedAt: template.lastBoostedAt,
            isBoosted: template.boostPriority > 0,
            engagementMetrics: {
                usageCount: template.usageCount || 0,
                likes: template.likes || 0,
                favorites: template.favorites || 0,
                viewCount: template.viewCount || 0
            },
            trendingScore: template.trendingScore || 0
        };

        return createSuccessResponse({ metrics });
    });
};

/**
 * Calculate and update visibility score based on engagement metrics
 */
const calculateVisibilityScore = async (req, res) => {
    await handleAsyncOperation(res, async () => {
        const { id } = req.params;

        const template = await Template.findById(id);
        if (!template) {
            return createErrorResponse('Template not found', 'TEMPLATE_NOT_FOUND', 'id', id);
        }

        // Calculate visibility score based on engagement metrics
        const engagementWeight = 0.4;
        const usageWeight = 0.3;
        const viewWeight = 0.2;
        const recentWeight = 0.1;

        const engagement = (template.likes || 0) + (template.favorites || 0);
        const usage = template.usageCount || 0;
        const views = template.viewCount || 0;
        
        // Recent activity bonus (templates updated in last 30 days)
        const daysSinceUpdate = (new Date() - template.updatedAt) / (1000 * 60 * 60 * 24);
        const recentBonus = daysSinceUpdate <= 30 ? 1 : Math.max(0, 1 - (daysSinceUpdate - 30) / 365);

        const calculatedScore = Math.round(
            (engagement * engagementWeight) +
            (usage * usageWeight) +
            (views * viewWeight) +
            (recentBonus * 100 * recentWeight)
        );

        // Update the template with calculated score
        const updatedTemplate = await Template.findByIdAndUpdate(
            id,
            { 
                visibilityScore: calculatedScore,
                updatedAt: new Date()
            },
            { new: true, runValidators: true }
        );

        logger.info(`Visibility score calculated and updated for template ${id}: ${calculatedScore}`);

        return createSuccessResponse({
            template: {
                _id: updatedTemplate._id,
                visibilityScore: updatedTemplate.visibilityScore,
                calculationDetails: {
                    engagement,
                    usage,
                    views,
                    recentBonus: Math.round(recentBonus * 100),
                    weights: { engagementWeight, usageWeight, viewWeight, recentWeight }
                }
            },
            modified: ['visibilityScore']
        });
    });
};

/**
 * Bulk update visibility scores for multiple templates
 */
const bulkUpdateVisibilityScores = async (req, res) => {
    await handleAsyncOperation(res, async () => {
        const { templates } = req.body;

        if (!Array.isArray(templates) || templates.length === 0) {
            return createErrorResponse('Templates array is required', 'VALIDATION_ERROR', 'templates', templates);
        }

        const updatePromises = templates.map(({ id, visibilityScore }) => {
            if (!id || typeof visibilityScore !== 'number') {
                throw new Error(`Invalid template data: id=${id}, visibilityScore=${visibilityScore}`);
            }

            return Template.findByIdAndUpdate(
                id,
                { 
                    visibilityScore,
                    updatedAt: new Date()
                },
                { new: true, runValidators: true }
            );
        });

        const results = await Promise.allSettled(updatePromises);
        
        const successful = results.filter(result => result.status === 'fulfilled').length;
        const failed = results.filter(result => result.status === 'rejected').length;

        logger.info(`Bulk visibility score update completed: ${successful} successful, ${failed} failed`);

        return createSuccessResponse({
            summary: {
                total: templates.length,
                successful,
                failed
            },
            results: results.map((result, index) => ({
                templateId: templates[index].id,
                status: result.status,
                error: result.status === 'rejected' ? result.reason.message : null
            }))
        });
    });
};

module.exports = {
    updateVisibilityScore,
    updateBoostPriority,
    getVisibilityMetrics,
    calculateVisibilityScore,
    bulkUpdateVisibilityScores
}; 