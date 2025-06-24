const Template = require('../../../models/Template');
const logger = require('../../../utils/logger');
const { handleAsyncOperation, createSuccessResponse, createErrorResponse } = require('../../../utils/templateHelpers');

// Default customization options
const DEFAULT_CUSTOMIZATION_OPTIONS = {
    allowTextEdit: true,
    allowColorChange: true,
    allowImageUpload: true,
    allowFontChange: false,
    allowLayoutChange: false
};

/**
 * Update template customization options
 */
const updateCustomizationOptions = async (req, res) => {
    await handleAsyncOperation(res, async () => {
        const { id } = req.params;
        const customizationOptions = req.validatedCustomizationOptions || req.body.customizationOptions;

        const template = await Template.findByIdAndUpdate(
            id,
            { 
                customizationOptions: {
                    ...DEFAULT_CUSTOMIZATION_OPTIONS,
                    ...customizationOptions
                },
                updatedAt: new Date()
            },
            { new: true, runValidators: true }
        );

        if (!template) {
            return createErrorResponse('Template not found', 'TEMPLATE_NOT_FOUND', 'id', id);
        }

        logger.info(`Updated customization options for template ${id}`, {
            templateId: id,
            options: customizationOptions
        });

        return createSuccessResponse({
            template: {
                id: template._id,
                customizationOptions: template.customizationOptions,
                updatedAt: template.updatedAt
            }
        }, 'Customization options updated successfully');
    });
};

/**
 * Get template customization options
 */
const getCustomizationOptions = async (req, res) => {
    await handleAsyncOperation(res, async () => {
        const { id } = req.params;

        const template = await Template.findById(id).select('customizationOptions');

        if (!template) {
            return createErrorResponse('Template not found', 'TEMPLATE_NOT_FOUND', 'id', id);
        }

        return createSuccessResponse({
            templateId: id,
            customizationOptions: template.customizationOptions || DEFAULT_CUSTOMIZATION_OPTIONS
        });
    });
};

/**
 * Reset customization options to default values
 */
const resetCustomizationOptions = async (req, res) => {
    await handleAsyncOperation(res, async () => {
        const { id } = req.params;

        const template = await Template.findByIdAndUpdate(
            id,
            { 
                customizationOptions: DEFAULT_CUSTOMIZATION_OPTIONS,
                updatedAt: new Date()
            },
            { new: true, runValidators: true }
        );

        if (!template) {
            return createErrorResponse('Template not found', 'TEMPLATE_NOT_FOUND', 'id', id);
        }

        logger.info(`Reset customization options for template ${id}`);

        return createSuccessResponse({
            template: {
                id: template._id,
                customizationOptions: template.customizationOptions,
                updatedAt: template.updatedAt
            }
        }, 'Customization options reset to default values');
    });
};

/**
 * Update a specific customization option
 */
const updateSpecificOption = async (req, res) => {
    await handleAsyncOperation(res, async () => {
        const { id, optionName } = req.params;
        const { value } = req.body;

        // Validate option name
        if (!DEFAULT_CUSTOMIZATION_OPTIONS.hasOwnProperty(optionName)) {
            return createErrorResponse(
                `Invalid customization option: ${optionName}`,
                'VALIDATION_ERROR',
                'optionName',
                optionName
            );
        }

        // Validate value type
        if (typeof value !== 'boolean') {
            return createErrorResponse(
                'Customization option value must be a boolean',
                'VALIDATION_ERROR',
                'value',
                value
            );
        }

        const updatePath = `customizationOptions.${optionName}`;
        const template = await Template.findByIdAndUpdate(
            id,
            { 
                [updatePath]: value,
                updatedAt: new Date()
            },
            { new: true, runValidators: true }
        );

        if (!template) {
            return createErrorResponse('Template not found', 'TEMPLATE_NOT_FOUND', 'id', id);
        }

        logger.info(`Updated customization option ${optionName} for template ${id}`, {
            templateId: id,
            optionName,
            value
        });

        return createSuccessResponse({
            template: {
                id: template._id,
                customizationOptions: template.customizationOptions,
                updatedAt: template.updatedAt
            }
        }, `Customization option '${optionName}' updated successfully`);
    });
};

/**
 * Get list of available customization options
 */
const getAvailableOptions = async (req, res) => {
    await handleAsyncOperation(res, async () => {
        const availableOptions = Object.keys(DEFAULT_CUSTOMIZATION_OPTIONS).map(key => ({
            name: key,
            type: 'boolean',
            default: DEFAULT_CUSTOMIZATION_OPTIONS[key],
            description: getOptionDescription(key)
        }));

        return createSuccessResponse({
            availableOptions,
            totalOptions: availableOptions.length
        });
    });
};

/**
 * Validate customization options data
 */
const validateCustomizationOptions = async (req, res) => {
    await handleAsyncOperation(res, async () => {
        const { customizationOptions } = req.body;
        const errors = [];
        const warnings = [];

        // Check for invalid option names
        Object.keys(customizationOptions).forEach(key => {
            if (!DEFAULT_CUSTOMIZATION_OPTIONS.hasOwnProperty(key)) {
                errors.push(`Invalid customization option: ${key}`);
            } else if (typeof customizationOptions[key] !== 'boolean') {
                errors.push(`Option '${key}' must be a boolean value`);
            }
        });

        // Check for missing recommended options
        const recommendedOptions = ['allowTextEdit', 'allowColorChange'];
        recommendedOptions.forEach(option => {
            if (customizationOptions[option] === undefined) {
                warnings.push(`Recommended option '${option}' is not specified`);
            }
        });

        return createSuccessResponse({
            isValid: errors.length === 0,
            errors,
            warnings,
            validatedOptions: errors.length === 0 ? customizationOptions : null
        });
    });
};

/**
 * Bulk update customization options for multiple templates
 */
const bulkUpdateCustomizationOptions = async (req, res) => {
    await handleAsyncOperation(res, async () => {
        const { templateIds, customizationOptions } = req.body;

        if (!Array.isArray(templateIds) || templateIds.length === 0) {
            return createErrorResponse(
                'Template IDs array is required',
                'VALIDATION_ERROR',
                'templateIds',
                templateIds
            );
        }

        const updateResult = await Template.updateMany(
            { _id: { $in: templateIds } },
            { 
                customizationOptions: {
                    ...DEFAULT_CUSTOMIZATION_OPTIONS,
                    ...customizationOptions
                },
                updatedAt: new Date()
            }
        );

        logger.info(`Bulk updated customization options for ${updateResult.modifiedCount} templates`, {
            templateIds,
            options: customizationOptions
        });

        return createSuccessResponse({
            modifiedCount: updateResult.modifiedCount,
            matchedCount: updateResult.matchedCount,
            templateIds,
            customizationOptions
        }, `Updated customization options for ${updateResult.modifiedCount} templates`);
    });
};

/**
 * Get description for customization option
 */
const getOptionDescription = (optionName) => {
    const descriptions = {
        allowTextEdit: 'Allow users to edit text content in the template',
        allowColorChange: 'Allow users to change colors in the template',
        allowImageUpload: 'Allow users to upload and replace images',
        allowFontChange: 'Allow users to change font styles and families',
        allowLayoutChange: 'Allow users to modify the template layout'
    };
    
    return descriptions[optionName] || 'No description available';
};

module.exports = {
    updateCustomizationOptions,
    getCustomizationOptions,
    resetCustomizationOptions,
    updateSpecificOption,
    getAvailableOptions,
    validateCustomizationOptions,
    bulkUpdateCustomizationOptions
}; 