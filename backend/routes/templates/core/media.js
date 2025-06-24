const express = require('express');
const router = express.Router();
const { 
    updateTemplateMedia,
    getTemplateMedia,
    deleteTemplateMedia,
    validateMediaUrls
} = require('../../../controllers/templates/core/mediaController');

const { templateFieldValidators } = require('../../../utils/templateValidators');
const { handleAsyncOperation } = require('../../../utils/templateHelpers');
const logger = require('../../../utils/logger');

/**
 * @route GET /api/templates/core/media/:templateId
 * @desc Get template media URLs
 * @access Public
 * @params templateId - MongoDB ObjectId
 */
router.get('/:templateId', async (req, res) => {
    await handleAsyncOperation(
        () => getTemplateMedia(req, res),
        res,
        'get template media'
    );
});

/**
 * @route PUT /api/templates/core/media/:templateId
 * @desc Update template media URLs
 * @access Public
 * @params templateId - MongoDB ObjectId
 * @body { previewUrl?, videoUrl?, imageUrl? }
 */
router.put('/:templateId',
    templateFieldValidators.previewUrl,
    templateFieldValidators.videoUrl,
    templateFieldValidators.imageUrl,
    async (req, res) => {
        await handleAsyncOperation(
            () => updateTemplateMedia(req, res),
            res,
            'update template media'
        );
    }
);

/**
 * @route DELETE /api/templates/core/media/:templateId
 * @desc Clear template media URLs
 * @access Public
 * @params templateId - MongoDB ObjectId
 * @query mediaType - Specific media type to clear (preview|video|image|all)
 */
router.delete('/:templateId', async (req, res) => {
    await handleAsyncOperation(
        () => deleteTemplateMedia(req, res),
        res,
        'delete template media'
    );
});

/**
 * @route POST /api/templates/core/media/:templateId/validate
 * @desc Validate media URLs without saving
 * @access Public
 * @params templateId - MongoDB ObjectId
 * @body { previewUrl?, videoUrl?, imageUrl? }
 */
router.post('/:templateId/validate',
    templateFieldValidators.previewUrl,
    templateFieldValidators.videoUrl,
    templateFieldValidators.imageUrl,
    async (req, res) => {
        await handleAsyncOperation(
            () => validateMediaUrls(req, res),
            res,
            'validate media URLs'
        );
    }
);

/**
 * @route PUT /api/templates/core/media/:templateId/preview
 * @desc Update only preview URL
 * @access Public
 * @params templateId - MongoDB ObjectId
 * @body { previewUrl }
 */
router.put('/:templateId/preview',
    templateFieldValidators.previewUrl,
    async (req, res) => {
        try {
            const { templateId } = req.params;
            const { previewUrl } = req.body;
            
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
                    previewUrl: previewUrl || '',
                    updatedAt: new Date()
                },
                { 
                    new: true,
                    select: 'previewUrl title updatedAt'
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

            logger.info(`Template preview URL updated: ${templateId}`);

            res.json({
                success: true,
                data: {
                    templateId: templateId,
                    title: updatedTemplate.title,
                    previewUrl: updatedTemplate.previewUrl,
                    updatedAt: updatedTemplate.updatedAt
                },
                modified: ['previewUrl'],
                timestamp: new Date().toISOString()
            });

        } catch (error) {
            logger.error('Error updating template preview URL:', error);
            res.status(500).json({
                success: false,
                error: {
                    code: 'SERVER_ERROR',
                    message: 'Internal server error while updating preview URL',
                    details: error.message
                },
                timestamp: new Date().toISOString()
            });
        }
    }
);

/**
 * @route PUT /api/templates/core/media/:templateId/video
 * @desc Update only video URL
 * @access Public
 * @params templateId - MongoDB ObjectId
 * @body { videoUrl }
 */
router.put('/:templateId/video',
    templateFieldValidators.videoUrl,
    async (req, res) => {
        try {
            const { templateId } = req.params;
            const { videoUrl } = req.body;
            
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
                    videoUrl: videoUrl || '',
                    updatedAt: new Date()
                },
                { 
                    new: true,
                    select: 'videoUrl title updatedAt'
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

            logger.info(`Template video URL updated: ${templateId}`);

            res.json({
                success: true,
                data: {
                    templateId: templateId,
                    title: updatedTemplate.title,
                    videoUrl: updatedTemplate.videoUrl,
                    updatedAt: updatedTemplate.updatedAt
                },
                modified: ['videoUrl'],
                timestamp: new Date().toISOString()
            });

        } catch (error) {
            logger.error('Error updating template video URL:', error);
            res.status(500).json({
                success: false,
                error: {
                    code: 'SERVER_ERROR',
                    message: 'Internal server error while updating video URL',
                    details: error.message
                },
                timestamp: new Date().toISOString()
            });
        }
    }
);

/**
 * @route PUT /api/templates/core/media/:templateId/image
 * @desc Update only image URL
 * @access Public
 * @params templateId - MongoDB ObjectId
 * @body { imageUrl }
 */
router.put('/:templateId/image',
    templateFieldValidators.imageUrl,
    async (req, res) => {
        try {
            const { templateId } = req.params;
            const { imageUrl } = req.body;
            
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
                    imageUrl: imageUrl || '',
                    updatedAt: new Date()
                },
                { 
                    new: true,
                    select: 'imageUrl title updatedAt'
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

            logger.info(`Template image URL updated: ${templateId}`);

            res.json({
                success: true,
                data: {
                    templateId: templateId,
                    title: updatedTemplate.title,
                    imageUrl: updatedTemplate.imageUrl,
                    updatedAt: updatedTemplate.updatedAt
                },
                modified: ['imageUrl'],
                timestamp: new Date().toISOString()
            });

        } catch (error) {
            logger.error('Error updating template image URL:', error);
            res.status(500).json({
                success: false,
                error: {
                    code: 'SERVER_ERROR',
                    message: 'Internal server error while updating image URL',
                    details: error.message
                },
                timestamp: new Date().toISOString()
            });
        }
    }
);

module.exports = router; 