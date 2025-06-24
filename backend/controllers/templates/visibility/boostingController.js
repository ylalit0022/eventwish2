const Template = require('../../../models/Template');
const logger = require('../../../utils/logger');
const { handleAsyncOperation, createSuccessResponse, createErrorResponse } = require('../../../utils/templateHelpers');

/**
 * Boost a template (set boost priority and lastBoostedAt)
 */
const boostTemplate = async (req, res) => {
    await handleAsyncOperation(res, async () => {
        const { id } = req.params;
        const { boostPriority = 100, duration } = req.body;

        const updateData = {
            boostPriority,
            lastBoostedAt: new Date(),
            updatedAt: new Date()
        };

        // If duration is specified, calculate boost expiration
        if (duration && typeof duration === 'number' && duration > 0) {
            const expirationDate = new Date();
            expirationDate.setHours(expirationDate.getHours() + duration);
            updateData.boostExpiresAt = expirationDate;
        }

        const template = await Template.findByIdAndUpdate(
            id,
            updateData,
            { new: true, runValidators: true }
        );

        if (!template) {
            return createErrorResponse('Template not found', 'TEMPLATE_NOT_FOUND', 'id', id);
        }

        logger.info(`Template ${id} boosted with priority ${boostPriority}${duration ? ` for ${duration} hours` : ''}`);

        return createSuccessResponse({
            template: {
                _id: template._id,
                boostPriority: template.boostPriority,
                lastBoostedAt: template.lastBoostedAt,
                boostExpiresAt: template.boostExpiresAt,
                isBoosted: true
            },
            modified: ['boostPriority', 'lastBoostedAt']
        });
    });
};

/**
 * Remove boost from a template
 */
const unboostTemplate = async (req, res) => {
    await handleAsyncOperation(res, async () => {
        const { id } = req.params;

        const template = await Template.findByIdAndUpdate(
            id,
            { 
                boostPriority: 0,
                $unset: { boostExpiresAt: 1 },
                updatedAt: new Date()
            },
            { new: true, runValidators: true }
        );

        if (!template) {
            return createErrorResponse('Template not found', 'TEMPLATE_NOT_FOUND', 'id', id);
        }

        logger.info(`Template ${id} boost removed`);

        return createSuccessResponse({
            template: {
                _id: template._id,
                boostPriority: template.boostPriority,
                lastBoostedAt: template.lastBoostedAt,
                isBoosted: false
            },
            modified: ['boostPriority']
        });
    });
};

/**
 * Get boost status for a template
 */
const getBoostStatus = async (req, res) => {
    await handleAsyncOperation(res, async () => {
        const { id } = req.params;

        const template = await Template.findById(id).select(
            'boostPriority lastBoostedAt boostExpiresAt'
        );

        if (!template) {
            return createErrorResponse('Template not found', 'TEMPLATE_NOT_FOUND', 'id', id);
        }

        const now = new Date();
        const isExpired = template.boostExpiresAt && template.boostExpiresAt < now;
        const isBoosted = template.boostPriority > 0 && !isExpired;

        const status = {
            isBoosted,
            boostPriority: template.boostPriority || 0,
            lastBoostedAt: template.lastBoostedAt,
            boostExpiresAt: template.boostExpiresAt,
            isExpired,
            timeRemaining: template.boostExpiresAt && !isExpired 
                ? Math.max(0, template.boostExpiresAt - now) 
                : null
        };

        return createSuccessResponse({ boostStatus: status });
    });
};

/**
 * Get all currently boosted templates
 */
const getBoostedTemplates = async (req, res) => {
    await handleAsyncOperation(res, async () => {
        const { page = 1, limit = 20, sortBy = 'boostPriority' } = req.query;
        const skip = (page - 1) * limit;

        // Find templates with boost priority > 0 and not expired
        const query = {
            boostPriority: { $gt: 0 },
            $or: [
                { boostExpiresAt: { $exists: false } },
                { boostExpiresAt: { $gt: new Date() } }
            ]
        };

        const templates = await Template.find(query)
            .select('title category boostPriority lastBoostedAt boostExpiresAt visibilityScore')
            .sort({ [sortBy]: -1 })
            .skip(skip)
            .limit(parseInt(limit));

        const total = await Template.countDocuments(query);

        return createSuccessResponse({
            templates: templates.map(template => ({
                _id: template._id,
                title: template.title,
                category: template.category,
                boostPriority: template.boostPriority,
                lastBoostedAt: template.lastBoostedAt,
                boostExpiresAt: template.boostExpiresAt,
                visibilityScore: template.visibilityScore
            })),
            pagination: {
                page: parseInt(page),
                limit: parseInt(limit),
                total,
                pages: Math.ceil(total / limit)
            }
        });
    });
};

/**
 * Schedule a boost for future activation
 */
const scheduleBoost = async (req, res) => {
    await handleAsyncOperation(res, async () => {
        const { id } = req.params;
        const { scheduledAt, boostPriority = 100, duration } = req.body;

        if (!scheduledAt) {
            return createErrorResponse('Scheduled time is required', 'VALIDATION_ERROR', 'scheduledAt', scheduledAt);
        }

        const scheduleDate = new Date(scheduledAt);
        if (scheduleDate <= new Date()) {
            return createErrorResponse('Scheduled time must be in the future', 'VALIDATION_ERROR', 'scheduledAt', scheduledAt);
        }

        const updateData = {
            boostScheduledAt: scheduleDate,
            scheduledBoostPriority: boostPriority,
            updatedAt: new Date()
        };

        if (duration && typeof duration === 'number' && duration > 0) {
            updateData.scheduledBoostDuration = duration;
        }

        const template = await Template.findByIdAndUpdate(
            id,
            updateData,
            { new: true, runValidators: true }
        );

        if (!template) {
            return createErrorResponse('Template not found', 'TEMPLATE_NOT_FOUND', 'id', id);
        }

        logger.info(`Boost scheduled for template ${id} at ${scheduleDate} with priority ${boostPriority}`);

        return createSuccessResponse({
            template: {
                _id: template._id,
                boostScheduledAt: template.boostScheduledAt,
                scheduledBoostPriority: template.scheduledBoostPriority,
                scheduledBoostDuration: template.scheduledBoostDuration
            },
            modified: ['boostScheduledAt', 'scheduledBoostPriority']
        });
    });
};

/**
 * Cancel a scheduled boost
 */
const cancelScheduledBoost = async (req, res) => {
    await handleAsyncOperation(res, async () => {
        const { id } = req.params;

        const template = await Template.findByIdAndUpdate(
            id,
            { 
                $unset: { 
                    boostScheduledAt: 1,
                    scheduledBoostPriority: 1,
                    scheduledBoostDuration: 1
                },
                updatedAt: new Date()
            },
            { new: true, runValidators: true }
        );

        if (!template) {
            return createErrorResponse('Template not found', 'TEMPLATE_NOT_FOUND', 'id', id);
        }

        logger.info(`Scheduled boost cancelled for template ${id}`);

        return createSuccessResponse({
            template: {
                _id: template._id,
                boostScheduledAt: null,
                scheduledBoostPriority: null
            },
            modified: ['boostScheduledAt', 'scheduledBoostPriority']
        });
    });
};

/**
 * Get boost history for a template
 */
const getBoostHistory = async (req, res) => {
    await handleAsyncOperation(res, async () => {
        const { id } = req.params;

        const template = await Template.findById(id).select(
            'title lastBoostedAt boostPriority boostExpiresAt boostScheduledAt'
        );

        if (!template) {
            return createErrorResponse('Template not found', 'TEMPLATE_NOT_FOUND', 'id', id);
        }

        // In a real implementation, you might want to store boost history in a separate collection
        // For now, we'll return current boost information
        const history = {
            currentBoost: {
                isActive: template.boostPriority > 0,
                priority: template.boostPriority,
                lastBoostedAt: template.lastBoostedAt,
                expiresAt: template.boostExpiresAt
            },
            scheduledBoost: {
                isScheduled: !!template.boostScheduledAt,
                scheduledAt: template.boostScheduledAt
            }
        };

        return createSuccessResponse({
            template: {
                _id: template._id,
                title: template.title
            },
            boostHistory: history
        });
    });
};

module.exports = {
    boostTemplate,
    unboostTemplate,
    getBoostStatus,
    getBoostedTemplates,
    scheduleBoost,
    cancelScheduledBoost,
    getBoostHistory
}; 