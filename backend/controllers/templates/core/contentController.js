const Template = require('../../../models/Template');
const logger = require('../../../utils/logger');
const { 
    formatErrorResponse, 
    formatSuccessResponse,
    sanitizeHtmlContent,
    validateFields,
    isValidObjectId
} = require('../../../utils/templateHelpers');

/**
 * Create new template with core content
 * @route POST /api/templates/core/content
 * @access Public
 */
const createTemplateContent = async (req, res) => {
    try {
        const { title, category, htmlContent, cssContent, jsContent } = req.body;

        logger.info('Creating new template with core content:', { title, category });

        // Sanitize HTML content to prevent XSS
        const sanitizedHtmlContent = sanitizeHtmlContent(htmlContent);

        // Create new template
        const templateData = {
            title: title.trim(),
            category: category.trim(),
            htmlContent: sanitizedHtmlContent,
            cssContent: cssContent || '',
            jsContent: jsContent || '',
            status: true, // Active by default
            createdAt: new Date(),
            updatedAt: new Date()
        };

        const newTemplate = new Template(templateData);
        await newTemplate.save();

        logger.info(`Template created successfully: ${newTemplate._id}`);

        res.status(201).json(formatSuccessResponse({
            templateId: newTemplate._id,
            title: newTemplate.title,
            category: newTemplate.category,
            htmlContent: newTemplate.htmlContent,
            cssContent: newTemplate.cssContent,
            jsContent: newTemplate.jsContent,
            status: newTemplate.status,
            createdAt: newTemplate.createdAt,
            updatedAt: newTemplate.updatedAt
        }, ['title', 'category', 'htmlContent', 'cssContent', 'jsContent'], 1));

    } catch (error) {
        logger.error('Error creating template content:', error);

        if (error.name === 'ValidationError') {
            const validationErrors = Object.keys(error.errors).map(key => ({
                field: key,
                message: error.errors[key].message,
                value: error.errors[key].value
            }));

            return res.status(400).json(formatErrorResponse(
                'VALIDATION_ERROR',
                'Template validation failed',
                null,
                null,
                { validationErrors }
            ));
        }

        if (error.code === 11000) {
            return res.status(409).json(formatErrorResponse(
                'DUPLICATE_ERROR',
                'Template with this title already exists',
                'title',
                req.body.title
            ));
        }

        res.status(500).json(formatErrorResponse(
            'SERVER_ERROR',
            'Internal server error while creating template',
            null,
            null,
            error.message
        ));
    }
};

/**
 * Get template core content by ID
 * @route GET /api/templates/core/content/:templateId
 * @access Public
 */
const getTemplateContent = async (req, res) => {
    try {
        const { templateId } = req.params;

        if (!isValidObjectId(templateId)) {
            return res.status(400).json(formatErrorResponse(
                'VALIDATION_ERROR',
                'Invalid template ID format',
                'templateId',
                templateId
            ));
        }

        logger.info(`Fetching template content: ${templateId}`);

        const template = await Template.findById(templateId)
            .select('title category htmlContent cssContent jsContent status createdAt updatedAt');

        if (!template) {
            return res.status(404).json(formatErrorResponse(
                'NOT_FOUND',
                'Template not found',
                'templateId',
                templateId
            ));
        }

        logger.info(`Template content retrieved: ${templateId}`);

        res.json(formatSuccessResponse({
            templateId: template._id,
            title: template.title,
            category: template.category,
            htmlContent: template.htmlContent,
            cssContent: template.cssContent || '',
            jsContent: template.jsContent || '',
            status: template.status,
            createdAt: template.createdAt,
            updatedAt: template.updatedAt
        }));

    } catch (error) {
        logger.error('Error getting template content:', error);
        res.status(500).json(formatErrorResponse(
            'SERVER_ERROR',
            'Internal server error while retrieving template',
            null,
            null,
            error.message
        ));
    }
};

/**
 * Update template core content
 * @route PUT /api/templates/core/content/:templateId
 * @access Public
 */
const updateTemplateContent = async (req, res) => {
    try {
        const { templateId } = req.params;
        const updateData = req.body;

        if (!isValidObjectId(templateId)) {
            return res.status(400).json(formatErrorResponse(
                'VALIDATION_ERROR',
                'Invalid template ID format',
                'templateId',
                templateId
            ));
        }

        logger.info(`Updating template content: ${templateId}`, updateData);

        // Validate update fields
        const allowedFields = ['title', 'category', 'htmlContent', 'cssContent', 'jsContent'];
        const updateFields = Object.keys(updateData).filter(key => allowedFields.includes(key));
        
        if (updateFields.length === 0) {
            return res.status(400).json(formatErrorResponse(
                'VALIDATION_ERROR',
                'No valid fields provided for update',
                null,
                null,
                { allowedFields }
            ));
        }

        // Prepare update object
        const updateObject = {};
        updateFields.forEach(field => {
            if (field === 'htmlContent' && updateData[field]) {
                updateObject[field] = sanitizeHtmlContent(updateData[field]);
            } else if (field === 'title' || field === 'category') {
                updateObject[field] = updateData[field].trim();
            } else {
                updateObject[field] = updateData[field];
            }
        });

        updateObject.updatedAt = new Date();

        const updatedTemplate = await Template.findByIdAndUpdate(
            templateId,
            updateObject,
            { 
                new: true, 
                runValidators: true,
                select: 'title category htmlContent cssContent jsContent status updatedAt'
            }
        );

        if (!updatedTemplate) {
            return res.status(404).json(formatErrorResponse(
                'NOT_FOUND',
                'Template not found',
                'templateId',
                templateId
            ));
        }

        logger.info(`Template content updated: ${templateId}`, { updatedFields: updateFields });

        res.json(formatSuccessResponse({
            templateId: updatedTemplate._id,
            title: updatedTemplate.title,
            category: updatedTemplate.category,
            htmlContent: updatedTemplate.htmlContent,
            cssContent: updatedTemplate.cssContent || '',
            jsContent: updatedTemplate.jsContent || '',
            status: updatedTemplate.status,
            updatedAt: updatedTemplate.updatedAt
        }, updateFields));

    } catch (error) {
        logger.error('Error updating template content:', error);

        if (error.name === 'ValidationError') {
            const validationErrors = Object.keys(error.errors).map(key => ({
                field: key,
                message: error.errors[key].message,
                value: error.errors[key].value
            }));

            return res.status(400).json(formatErrorResponse(
                'VALIDATION_ERROR',
                'Template validation failed',
                null,
                null,
                { validationErrors }
            ));
        }

        if (error.code === 11000) {
            return res.status(409).json(formatErrorResponse(
                'DUPLICATE_ERROR',
                'Template with this title already exists',
                'title',
                req.body.title
            ));
        }

        res.status(500).json(formatErrorResponse(
            'SERVER_ERROR',
            'Internal server error while updating template',
            null,
            null,
            error.message
        ));
    }
};

/**
 * Delete template (soft delete by setting status to false)
 * @route DELETE /api/templates/core/content/:templateId
 * @access Public
 */
const deleteTemplateContent = async (req, res) => {
    try {
        const { templateId } = req.params;

        if (!isValidObjectId(templateId)) {
            return res.status(400).json(formatErrorResponse(
                'VALIDATION_ERROR',
                'Invalid template ID format',
                'templateId',
                templateId
            ));
        }

        logger.info(`Soft deleting template: ${templateId}`);

        const deletedTemplate = await Template.findByIdAndUpdate(
            templateId,
            { 
                status: false,
                updatedAt: new Date()
            },
            { 
                new: true,
                select: 'title status updatedAt'
            }
        );

        if (!deletedTemplate) {
            return res.status(404).json(formatErrorResponse(
                'NOT_FOUND',
                'Template not found',
                'templateId',
                templateId
            ));
        }

        logger.info(`Template soft deleted: ${templateId}`);

        res.json(formatSuccessResponse({
            templateId: templateId,
            title: deletedTemplate.title,
            status: deletedTemplate.status,
            deletedAt: deletedTemplate.updatedAt,
            message: 'Template deactivated successfully'
        }, ['status']));

    } catch (error) {
        logger.error('Error deleting template content:', error);
        res.status(500).json(formatErrorResponse(
            'SERVER_ERROR',
            'Internal server error while deleting template',
            null,
            null,
            error.message
        ));
    }
};

/**
 * Validate template content without saving
 * @route POST /api/templates/core/content/:templateId/validate
 * @access Public
 */
const validateTemplateContent = async (req, res) => {
    try {
        const { templateId } = req.params;
        const contentData = req.body;

        if (templateId && !isValidObjectId(templateId)) {
            return res.status(400).json(formatErrorResponse(
                'VALIDATION_ERROR',
                'Invalid template ID format',
                'templateId',
                templateId
            ));
        }

        logger.info('Validating template content:', contentData);

        // Validate fields using template helpers
        const validation = validateFields(contentData);
        
        if (!validation.isValid) {
            return res.status(400).json(formatErrorResponse(
                'VALIDATION_ERROR',
                'Content validation failed',
                null,
                null,
                { validationErrors: validation.errors }
            ));
        }

        // Additional content-specific validations
        const contentValidations = [];

        if (contentData.htmlContent) {
            const sanitizedHtml = sanitizeHtmlContent(contentData.htmlContent);
            if (sanitizedHtml !== contentData.htmlContent) {
                contentValidations.push({
                    field: 'htmlContent',
                    type: 'warning',
                    message: 'HTML content has been sanitized to remove potentially harmful scripts'
                });
            }
        }

        logger.info('Template content validation completed');

        res.json(formatSuccessResponse({
            isValid: true,
            validatedFields: Object.keys(contentData),
            contentValidations,
            sanitizedHtmlContent: contentData.htmlContent ? sanitizeHtmlContent(contentData.htmlContent) : null,
            message: 'Content validation passed'
        }));

    } catch (error) {
        logger.error('Error validating template content:', error);
        res.status(500).json(formatErrorResponse(
            'SERVER_ERROR',
            'Internal server error during validation',
            null,
            null,
            error.message
        ));
    }
};

module.exports = {
    createTemplateContent,
    getTemplateContent,
    updateTemplateContent,
    deleteTemplateContent,
    validateTemplateContent
}; 