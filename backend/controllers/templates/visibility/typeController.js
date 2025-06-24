const Template = require('../../../models/Template');
const logger = require('../../../utils/logger');
const { handleAsyncOperation, createSuccessResponse, createErrorResponse } = require('../../../utils/templateHelpers');

// Available template types (should match the enum in Template schema)
const TEMPLATE_TYPES = ['html', 'image', 'video'];

/**
 * Update template type
 */
const updateTemplateType = async (req, res) => {
    await handleAsyncOperation(res, async () => {
        const { id } = req.params;
        const { templateType } = req.body;

        if (!TEMPLATE_TYPES.includes(templateType)) {
            return createErrorResponse(
                `Invalid template type. Must be one of: ${TEMPLATE_TYPES.join(', ')}`,
                'VALIDATION_ERROR',
                'templateType',
                templateType
            );
        }

        const template = await Template.findByIdAndUpdate(
            id,
            { 
                templateType,
                updatedAt: new Date()
            },
            { new: true, runValidators: true }
        );

        if (!template) {
            return createErrorResponse('Template not found', 'TEMPLATE_NOT_FOUND', 'id', id);
        }

        logger.info(`Template type updated for template ${id}: ${templateType}`);

        return createSuccessResponse({
            template: {
                _id: template._id,
                templateType: template.templateType,
                updatedAt: template.updatedAt
            },
            modified: ['templateType']
        });
    });
};

/**
 * Get template type
 */
const getTemplateType = async (req, res) => {
    await handleAsyncOperation(res, async () => {
        const { id } = req.params;

        const template = await Template.findById(id).select('templateType title');

        if (!template) {
            return createErrorResponse('Template not found', 'TEMPLATE_NOT_FOUND', 'id', id);
        }

        return createSuccessResponse({
            template: {
                _id: template._id,
                title: template.title,
                templateType: template.templateType || 'html'
            }
        });
    });
};

/**
 * Get all templates of a specific type
 */
const getTemplatesByType = async (req, res) => {
    await handleAsyncOperation(res, async () => {
        const { type } = req.params;
        const { page = 1, limit = 20, sortBy = 'createdAt', sortOrder = 'desc' } = req.query;
        const skip = (page - 1) * limit;

        if (!TEMPLATE_TYPES.includes(type)) {
            return createErrorResponse(
                `Invalid template type. Must be one of: ${TEMPLATE_TYPES.join(', ')}`,
                'VALIDATION_ERROR',
                'type',
                type
            );
        }

        const sortDirection = sortOrder === 'asc' ? 1 : -1;
        const sortOptions = { [sortBy]: sortDirection };

        const templates = await Template.find({ templateType: type })
            .select('title category templateType status isPremium usageCount likes favorites createdAt')
            .sort(sortOptions)
            .skip(skip)
            .limit(parseInt(limit));

        const total = await Template.countDocuments({ templateType: type });

        return createSuccessResponse({
            templates: templates.map(template => ({
                _id: template._id,
                title: template.title,
                category: template.category,
                templateType: template.templateType,
                status: template.status,
                isPremium: template.isPremium,
                metrics: {
                    usageCount: template.usageCount || 0,
                    likes: template.likes || 0,
                    favorites: template.favorites || 0
                },
                createdAt: template.createdAt
            })),
            pagination: {
                page: parseInt(page),
                limit: parseInt(limit),
                total,
                pages: Math.ceil(total / limit)
            },
            filterType: type
        });
    });
};

/**
 * Get list of available template types
 */
const getAvailableTypes = async (req, res) => {
    await handleAsyncOperation(res, async () => {
        return createSuccessResponse({
            availableTypes: TEMPLATE_TYPES.map(type => ({
                value: type,
                label: type.charAt(0).toUpperCase() + type.slice(1),
                description: getTypeDescription(type)
            }))
        });
    });
};

/**
 * Validate template type against content
 */
const validateTemplateType = async (req, res) => {
    await handleAsyncOperation(res, async () => {
        const { id } = req.params;

        const template = await Template.findById(id).select(
            'templateType htmlContent cssContent jsContent imageUrl videoUrl'
        );

        if (!template) {
            return createErrorResponse('Template not found', 'TEMPLATE_NOT_FOUND', 'id', id);
        }

        const currentType = template.templateType || 'html';
        const validation = validateTypeAgainstContent(template);

        return createSuccessResponse({
            template: {
                _id: template._id,
                currentType
            },
            validation: {
                isValid: validation.isValid,
                suggestedType: validation.suggestedType,
                reasons: validation.reasons,
                warnings: validation.warnings
            }
        });
    });
};

/**
 * Bulk update template types
 */
const bulkUpdateTemplateTypes = async (req, res) => {
    await handleAsyncOperation(res, async () => {
        const { templates } = req.body;

        if (!Array.isArray(templates) || templates.length === 0) {
            return createErrorResponse('Templates array is required', 'VALIDATION_ERROR', 'templates', templates);
        }

        const updatePromises = templates.map(({ id, templateType }) => {
            if (!id || !TEMPLATE_TYPES.includes(templateType)) {
                throw new Error(`Invalid template data: id=${id}, templateType=${templateType}`);
            }

            return Template.findByIdAndUpdate(
                id,
                { 
                    templateType,
                    updatedAt: new Date()
                },
                { new: true, runValidators: true }
            );
        });

        const results = await Promise.allSettled(updatePromises);
        
        const successful = results.filter(result => result.status === 'fulfilled').length;
        const failed = results.filter(result => result.status === 'rejected').length;

        logger.info(`Bulk template type update completed: ${successful} successful, ${failed} failed`);

        return createSuccessResponse({
            summary: {
                total: templates.length,
                successful,
                failed
            },
            results: results.map((result, index) => ({
                templateId: templates[index].id,
                status: result.status,
                templateType: result.status === 'fulfilled' ? result.value.templateType : null,
                error: result.status === 'rejected' ? result.reason.message : null
            }))
        });
    });
};

/**
 * Get template type statistics
 */
const getTypeStatistics = async (req, res) => {
    await handleAsyncOperation(res, async () => {
        const pipeline = [
            {
                $group: {
                    _id: '$templateType',
                    count: { $sum: 1 },
                    totalUsage: { $sum: '$usageCount' },
                    totalLikes: { $sum: '$likes' },
                    totalFavorites: { $sum: '$favorites' },
                    premiumCount: {
                        $sum: {
                            $cond: ['$isPremium', 1, 0]
                        }
                    },
                    activeCount: {
                        $sum: {
                            $cond: [{ $eq: ['$status', true] }, 1, 0]
                        }
                    }
                }
            },
            {
                $sort: { count: -1 }
            }
        ];

        const statistics = await Template.aggregate(pipeline);
        const total = await Template.countDocuments();

        const formattedStats = statistics.map(stat => ({
            type: stat._id || 'html',
            count: stat.count,
            percentage: total > 0 ? Math.round((stat.count / total) * 100) : 0,
            metrics: {
                totalUsage: stat.totalUsage || 0,
                totalLikes: stat.totalLikes || 0,
                totalFavorites: stat.totalFavorites || 0,
                premiumCount: stat.premiumCount || 0,
                activeCount: stat.activeCount || 0
            }
        }));

        return createSuccessResponse({
            statistics: formattedStats,
            total,
            availableTypes: TEMPLATE_TYPES
        });
    });
};

/**
 * Helper function to get type description
 */
function getTypeDescription(type) {
    const descriptions = {
        html: 'Interactive templates with HTML, CSS, and JavaScript',
        image: 'Static image-based templates',
        video: 'Video-based templates and animations'
    };
    return descriptions[type] || 'Unknown template type';
}

/**
 * Helper function to validate template type against content
 */
function validateTypeAgainstContent(template) {
    const hasHtml = template.htmlContent && template.htmlContent.trim().length > 0;
    const hasCss = template.cssContent && template.cssContent.trim().length > 0;
    const hasJs = template.jsContent && template.jsContent.trim().length > 0;
    const hasImage = template.imageUrl && template.imageUrl.trim().length > 0;
    const hasVideo = template.videoUrl && template.videoUrl.trim().length > 0;

    const currentType = template.templateType || 'html';
    let suggestedType = currentType;
    let isValid = true;
    const reasons = [];
    const warnings = [];

    // Determine suggested type based on content
    if (hasVideo) {
        suggestedType = 'video';
        if (currentType !== 'video') {
            isValid = false;
            reasons.push('Template has video content but type is not set to video');
        }
    } else if (hasImage && !hasHtml && !hasCss && !hasJs) {
        suggestedType = 'image';
        if (currentType !== 'image') {
            warnings.push('Template appears to be image-only but type is not set to image');
        }
    } else if (hasHtml || hasCss || hasJs) {
        suggestedType = 'html';
        if (currentType !== 'html') {
            warnings.push('Template has HTML/CSS/JS content, consider setting type to html');
        }
    }

    // Additional validations
    if (currentType === 'video' && !hasVideo) {
        isValid = false;
        reasons.push('Template type is video but no video URL is provided');
    }

    if (currentType === 'image' && !hasImage && !hasHtml) {
        warnings.push('Template type is image but no image URL or HTML content is provided');
    }

    return {
        isValid,
        suggestedType,
        reasons,
        warnings
    };
}

module.exports = {
    updateTemplateType,
    getTemplateType,
    getTemplatesByType,
    getAvailableTypes,
    validateTemplateType,
    bulkUpdateTemplateTypes,
    getTypeStatistics
}; 