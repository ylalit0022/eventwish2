const Template = require('../../../models/Template');
const logger = require('../../../utils/logger');
const { handleAsyncOperation, createSuccessResponse, createErrorResponse } = require('../../../utils/templateHelpers');

// Legacy field mappings and validation
const LEGACY_FIELDS = {
    categoryIcon: { type: 'string', deprecated: true, replacement: 'category' },
    moderationStatus: { type: 'string', enum: ['pending', 'approved', 'rejected'], deprecated: false },
    language: { type: 'string', deprecated: false },
    region: { type: 'string', deprecated: false }
};

/**
 * Update template category icon (legacy field)
 */
const updateCategoryIcon = async (req, res) => {
    await handleAsyncOperation(res, async () => {
        const { id } = req.params;
        const { categoryIcon } = req.body;

        // Warn about deprecated field
        logger.warn(`Using deprecated field 'categoryIcon' for template ${id}`, {
            templateId: id,
            replacement: 'Use category field instead'
        });

        const template = await Template.findByIdAndUpdate(
            id,
            { 
                categoryIcon,
                updatedAt: new Date()
            },
            { new: true, runValidators: true }
        );

        if (!template) {
            return createErrorResponse('Template not found', 'TEMPLATE_NOT_FOUND', 'id', id);
        }

        return createSuccessResponse({
            template: {
                id: template._id,
                categoryIcon: template.categoryIcon,
                updatedAt: template.updatedAt
            },
            warning: 'categoryIcon is deprecated. Use category field instead.'
        }, 'Category icon updated successfully (deprecated field)');
    });
};

/**
 * Get template category icon
 */
const getCategoryIcon = async (req, res) => {
    await handleAsyncOperation(res, async () => {
        const { id } = req.params;

        const template = await Template.findById(id).select('categoryIcon category');

        if (!template) {
            return createErrorResponse('Template not found', 'TEMPLATE_NOT_FOUND', 'id', id);
        }

        return createSuccessResponse({
            templateId: id,
            categoryIcon: template.categoryIcon,
            category: template.category, // Show modern equivalent
            migration: {
                deprecated: true,
                modernField: 'category',
                value: template.category
            }
        });
    });
};

/**
 * Update template moderation status
 */
const updateModerationStatus = async (req, res) => {
    await handleAsyncOperation(res, async () => {
        const { id } = req.params;
        const { moderationStatus } = req.body;

        const validStatuses = ['pending', 'approved', 'rejected'];
        if (!validStatuses.includes(moderationStatus)) {
            return createErrorResponse(
                `Invalid moderation status. Must be one of: ${validStatuses.join(', ')}`,
                'VALIDATION_ERROR',
                'moderationStatus',
                moderationStatus
            );
        }

        const template = await Template.findByIdAndUpdate(
            id,
            { 
                moderationStatus,
                updatedAt: new Date()
            },
            { new: true, runValidators: true }
        );

        if (!template) {
            return createErrorResponse('Template not found', 'TEMPLATE_NOT_FOUND', 'id', id);
        }

        logger.info(`Updated moderation status for template ${id}`, {
            templateId: id,
            moderationStatus
        });

        return createSuccessResponse({
            template: {
                id: template._id,
                moderationStatus: template.moderationStatus,
                updatedAt: template.updatedAt
            }
        }, 'Moderation status updated successfully');
    });
};

/**
 * Get template moderation status
 */
const getModerationStatus = async (req, res) => {
    await handleAsyncOperation(res, async () => {
        const { id } = req.params;

        const template = await Template.findById(id).select('moderationStatus');

        if (!template) {
            return createErrorResponse('Template not found', 'TEMPLATE_NOT_FOUND', 'id', id);
        }

        return createSuccessResponse({
            templateId: id,
            moderationStatus: template.moderationStatus || 'pending'
        });
    });
};

/**
 * Update template language
 */
const updateLanguage = async (req, res) => {
    await handleAsyncOperation(res, async () => {
        const { id } = req.params;
        const { language } = req.body;

        if (!language || typeof language !== 'string') {
            return createErrorResponse(
                'Language must be a valid string',
                'VALIDATION_ERROR',
                'language',
                language
            );
        }

        const template = await Template.findByIdAndUpdate(
            id,
            { 
                language: language.toLowerCase(),
                updatedAt: new Date()
            },
            { new: true, runValidators: true }
        );

        if (!template) {
            return createErrorResponse('Template not found', 'TEMPLATE_NOT_FOUND', 'id', id);
        }

        return createSuccessResponse({
            template: {
                id: template._id,
                language: template.language,
                updatedAt: template.updatedAt
            }
        }, 'Language updated successfully');
    });
};

/**
 * Get template language
 */
const getLanguage = async (req, res) => {
    await handleAsyncOperation(res, async () => {
        const { id } = req.params;

        const template = await Template.findById(id).select('language');

        if (!template) {
            return createErrorResponse('Template not found', 'TEMPLATE_NOT_FOUND', 'id', id);
        }

        return createSuccessResponse({
            templateId: id,
            language: template.language || 'en'
        });
    });
};

/**
 * Update template region
 */
const updateRegion = async (req, res) => {
    await handleAsyncOperation(res, async () => {
        const { id } = req.params;
        const { region } = req.body;

        if (!region || typeof region !== 'string') {
            return createErrorResponse(
                'Region must be a valid string',
                'VALIDATION_ERROR',
                'region',
                region
            );
        }

        const template = await Template.findByIdAndUpdate(
            id,
            { 
                region: region.toLowerCase(),
                updatedAt: new Date()
            },
            { new: true, runValidators: true }
        );

        if (!template) {
            return createErrorResponse('Template not found', 'TEMPLATE_NOT_FOUND', 'id', id);
        }

        return createSuccessResponse({
            template: {
                id: template._id,
                region: template.region,
                updatedAt: template.updatedAt
            }
        }, 'Region updated successfully');
    });
};

/**
 * Get template region
 */
const getRegion = async (req, res) => {
    await handleAsyncOperation(res, async () => {
        const { id } = req.params;

        const template = await Template.findById(id).select('region');

        if (!template) {
            return createErrorResponse('Template not found', 'TEMPLATE_NOT_FOUND', 'id', id);
        }

        return createSuccessResponse({
            templateId: id,
            region: template.region || 'global'
        });
    });
};

/**
 * Get all compatibility information for template
 */
const getCompatibilityInfo = async (req, res) => {
    await handleAsyncOperation(res, async () => {
        const { id } = req.params;

        const template = await Template.findById(id).select('categoryIcon moderationStatus language region category');

        if (!template) {
            return createErrorResponse('Template not found', 'TEMPLATE_NOT_FOUND', 'id', id);
        }

        const compatibilityInfo = {
            templateId: id,
            legacyFields: {
                categoryIcon: template.categoryIcon,
                moderationStatus: template.moderationStatus || 'pending',
                language: template.language || 'en',
                region: template.region || 'global'
            },
            modernEquivalents: {
                category: template.category
            },
            deprecationWarnings: [],
            migrationSuggestions: []
        };

        // Check for deprecated fields
        if (template.categoryIcon) {
            compatibilityInfo.deprecationWarnings.push({
                field: 'categoryIcon',
                message: 'categoryIcon is deprecated',
                replacement: 'category'
            });
            compatibilityInfo.migrationSuggestions.push({
                action: 'migrate_category_icon',
                description: 'Migrate categoryIcon to category field'
            });
        }

        return createSuccessResponse(compatibilityInfo);
    });
};

/**
 * Migrate template to new format
 */
const migrateToNewFormat = async (req, res) => {
    await handleAsyncOperation(res, async () => {
        const { id } = req.params;

        const template = await Template.findById(id);

        if (!template) {
            return createErrorResponse('Template not found', 'TEMPLATE_NOT_FOUND', 'id', id);
        }

        const migrations = [];

        // Migrate categoryIcon to category if needed
        if (template.categoryIcon && !template.category) {
            template.category = template.categoryIcon;
            migrations.push({
                field: 'categoryIcon',
                action: 'migrated_to_category',
                oldValue: template.categoryIcon,
                newValue: template.category
            });
        }

        // Set default values for missing fields
        if (!template.moderationStatus) {
            template.moderationStatus = 'pending';
            migrations.push({
                field: 'moderationStatus',
                action: 'set_default',
                newValue: 'pending'
            });
        }

        if (!template.language) {
            template.language = 'en';
            migrations.push({
                field: 'language',
                action: 'set_default',
                newValue: 'en'
            });
        }

        if (!template.region) {
            template.region = 'global';
            migrations.push({
                field: 'region',
                action: 'set_default',
                newValue: 'global'
            });
        }

        const updatedTemplate = await template.save();

        logger.info(`Migrated template ${id} to new format`, {
            templateId: id,
            migrations
        });

        return createSuccessResponse({
            template: {
                id: updatedTemplate._id,
                updatedAt: updatedTemplate.updatedAt
            },
            migrations,
            migrationCount: migrations.length
        }, `Template migrated successfully with ${migrations.length} changes`);
    });
};

/**
 * Validate compatibility data
 */
const validateCompatibilityData = async (req, res) => {
    await handleAsyncOperation(res, async () => {
        const data = req.body;
        const errors = [];
        const warnings = [];

        // Validate each legacy field
        Object.keys(LEGACY_FIELDS).forEach(fieldName => {
            if (data[fieldName] !== undefined) {
                const fieldConfig = LEGACY_FIELDS[fieldName];
                const value = data[fieldName];

                // Type validation
                if (fieldConfig.type === 'string' && typeof value !== 'string') {
                    errors.push(`${fieldName} must be a string`);
                }

                // Enum validation
                if (fieldConfig.enum && !fieldConfig.enum.includes(value)) {
                    errors.push(`${fieldName} must be one of: ${fieldConfig.enum.join(', ')}`);
                }

                // Deprecation warnings
                if (fieldConfig.deprecated) {
                    warnings.push({
                        field: fieldName,
                        message: `${fieldName} is deprecated`,
                        replacement: fieldConfig.replacement
                    });
                }
            }
        });

        return createSuccessResponse({
            isValid: errors.length === 0,
            errors,
            warnings,
            validatedData: errors.length === 0 ? data : null
        });
    });
};

module.exports = {
    updateCategoryIcon,
    getCategoryIcon,
    updateModerationStatus,
    getModerationStatus,
    updateLanguage,
    getLanguage,
    updateRegion,
    getRegion,
    getCompatibilityInfo,
    migrateToNewFormat,
    validateCompatibilityData
}; 