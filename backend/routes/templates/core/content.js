const express = require('express');
const router = express.Router();
const { 
    createTemplateContent,
    getTemplateContent,
    updateTemplateContent,
    deleteTemplateContent,
    validateTemplateContent
} = require('../../../controllers/templates/core/contentController');

const { templateFieldValidators } = require('../../../utils/templateValidators');
const { handleAsyncOperation } = require('../../../utils/templateHelpers');
const logger = require('../../../utils/logger');

/**
 * @route POST /api/templates/core/content
 * @desc Create new template with core content
 * @access Public
 * @body { title, category, htmlContent, cssContent?, jsContent? }
 */
router.post('/', 
    templateFieldValidators.title,
    templateFieldValidators.category,
    templateFieldValidators.htmlContent,
    templateFieldValidators.cssContent,
    templateFieldValidators.jsContent,
    async (req, res) => {
        await handleAsyncOperation(
            () => createTemplateContent(req, res),
            res,
            'create template content'
        );
    }
);

/**
 * @route GET /api/templates/core/content/:templateId
 * @desc Get template core content by ID
 * @access Public
 * @params templateId - MongoDB ObjectId
 */
router.get('/:templateId', async (req, res) => {
    await handleAsyncOperation(
        () => getTemplateContent(req, res),
        res,
        'get template content'
    );
});

/**
 * @route PUT /api/templates/core/content/:templateId
 * @desc Update template core content
 * @access Public
 * @params templateId - MongoDB ObjectId
 * @body { title?, category?, htmlContent?, cssContent?, jsContent? }
 */
router.put('/:templateId',
    templateFieldValidators.title,
    templateFieldValidators.category,
    templateFieldValidators.htmlContent,
    templateFieldValidators.cssContent,
    templateFieldValidators.jsContent,
    async (req, res) => {
        await handleAsyncOperation(
            () => updateTemplateContent(req, res),
            res,
            'update template content'
        );
    }
);

/**
 * @route DELETE /api/templates/core/content/:templateId
 * @desc Delete template (soft delete by setting status to false)
 * @access Public
 * @params templateId - MongoDB ObjectId
 */
router.delete('/:templateId', async (req, res) => {
    await handleAsyncOperation(
        () => deleteTemplateContent(req, res),
        res,
        'delete template content'
    );
});

/**
 * @route POST /api/templates/core/content/:templateId/validate
 * @desc Validate template content without saving
 * @access Public
 * @params templateId - MongoDB ObjectId
 * @body { title?, category?, htmlContent?, cssContent?, jsContent? }
 */
router.post('/:templateId/validate',
    templateFieldValidators.title,
    templateFieldValidators.category,
    templateFieldValidators.htmlContent,
    templateFieldValidators.cssContent,
    templateFieldValidators.jsContent,
    async (req, res) => {
        await handleAsyncOperation(
            () => validateTemplateContent(req, res),
            res,
            'validate template content'
        );
    }
);

/**
 * @route GET /api/templates/core/content/:templateId/html
 * @desc Get only HTML content for rendering
 * @access Public
 * @params templateId - MongoDB ObjectId
 */
router.get('/:templateId/html', async (req, res) => {
    try {
        const { templateId } = req.params;
        
        if (!require('mongoose').Types.ObjectId.isValid(templateId)) {
            return res.status(400).json({
                success: false,
                error: {
                    code: 'VALIDATION_ERROR',
                    message: 'Invalid template ID format',
                    field: 'templateId',
                    value: templateId
                },
                timestamp: new Date().toISOString()
            });
        }

        const Template = require('../../../models/Template');
        const template = await Template.findById(templateId).select('htmlContent cssContent jsContent title');
        
        if (!template) {
            return res.status(404).json({
                success: false,
                error: {
                    code: 'NOT_FOUND',
                    message: 'Template not found',
                    field: 'templateId',
                    value: templateId
                },
                timestamp: new Date().toISOString()
            });
        }

        res.json({
            success: true,
            data: {
                templateId: templateId,
                title: template.title,
                htmlContent: template.htmlContent,
                cssContent: template.cssContent || '',
                jsContent: template.jsContent || ''
            },
            timestamp: new Date().toISOString()
        });

    } catch (error) {
        logger.error('Error getting template HTML content:', error);
        res.status(500).json({
            success: false,
            error: {
                code: 'SERVER_ERROR',
                message: 'Internal server error while retrieving HTML content',
                details: error.message
            },
            timestamp: new Date().toISOString()
        });
    }
});

/**
 * @route PUT /api/templates/core/content/:templateId/html
 * @desc Update only HTML content (for quick edits)
 * @access Public
 * @params templateId - MongoDB ObjectId
 * @body { htmlContent }
 */
router.put('/:templateId/html',
    templateFieldValidators.htmlContent,
    async (req, res) => {
        try {
            const { templateId } = req.params;
            const { htmlContent } = req.body;
            
            if (!require('mongoose').Types.ObjectId.isValid(templateId)) {
                return res.status(400).json({
                    success: false,
                    error: {
                        code: 'VALIDATION_ERROR',
                        message: 'Invalid template ID format',
                        field: 'templateId',
                        value: templateId
                    },
                    timestamp: new Date().toISOString()
                });
            }

            const Template = require('../../../models/Template');
            const updatedTemplate = await Template.findByIdAndUpdate(
                templateId,
                { 
                    htmlContent,
                    updatedAt: new Date()
                },
                { 
                    new: true,
                    select: 'htmlContent title updatedAt'
                }
            );
            
            if (!updatedTemplate) {
                return res.status(404).json({
                    success: false,
                    error: {
                        code: 'NOT_FOUND',
                        message: 'Template not found',
                        field: 'templateId',
                        value: templateId
                    },
                    timestamp: new Date().toISOString()
                });
            }

            logger.info(`Template HTML content updated: ${templateId}`);

            res.json({
                success: true,
                data: {
                    templateId: templateId,
                    title: updatedTemplate.title,
                    htmlContent: updatedTemplate.htmlContent,
                    updatedAt: updatedTemplate.updatedAt
                },
                modified: ['htmlContent'],
                timestamp: new Date().toISOString()
            });

        } catch (error) {
            logger.error('Error updating template HTML content:', error);
            res.status(500).json({
                success: false,
                error: {
                    code: 'SERVER_ERROR',
                    message: 'Internal server error while updating HTML content',
                    details: error.message
                },
                timestamp: new Date().toISOString()
            });
        }
    }
);

module.exports = router; 