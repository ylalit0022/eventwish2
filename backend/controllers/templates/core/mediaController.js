const Template = require('../../../models/Template');
const logger = require('../../../utils/logger');
const { 
    formatErrorResponse, 
    formatSuccessResponse,
    isValidObjectId
} = require('../../../utils/templateHelpers');

/**
 * Get template media URLs
 * @route GET /api/templates/core/media/:templateId
 * @access Public
 */
const getTemplateMedia = async (req, res) => {
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

        logger.info(`Fetching template media: ${templateId}`);

        const template = await Template.findById(templateId)
            .select('title previewUrl videoUrl imageUrl updatedAt');

        if (!template) {
            return res.status(404).json(formatErrorResponse(
                'NOT_FOUND',
                'Template not found',
                'templateId',
                templateId
            ));
        }

        logger.info(`Template media retrieved: ${templateId}`);

        res.json(formatSuccessResponse({
            templateId: template._id,
            title: template.title,
            media: {
                previewUrl: template.previewUrl || '',
                videoUrl: template.videoUrl || '',
                imageUrl: template.imageUrl || ''
            },
            updatedAt: template.updatedAt
        }));

    } catch (error) {
        logger.error('Error getting template media:', error);
        res.status(500).json(formatErrorResponse(
            'SERVER_ERROR',
            'Internal server error while retrieving template media',
            null,
            null,
            error.message
        ));
    }
};

/**
 * Update template media URLs
 * @route PUT /api/templates/core/media/:templateId
 * @access Public
 */
const updateTemplateMedia = async (req, res) => {
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

        logger.info(`Updating template media: ${templateId}`, updateData);

        // Validate update fields
        const allowedFields = ['previewUrl', 'videoUrl', 'imageUrl'];
        const updateFields = Object.keys(updateData).filter(key => allowedFields.includes(key));
        
        if (updateFields.length === 0) {
            return res.status(400).json(formatErrorResponse(
                'VALIDATION_ERROR',
                'No valid media fields provided for update',
                null,
                null,
                { allowedFields }
            ));
        }

        // Prepare update object
        const updateObject = {};
        updateFields.forEach(field => {
            updateObject[field] = updateData[field] || '';
        });

        updateObject.updatedAt = new Date();

        const updatedTemplate = await Template.findByIdAndUpdate(
            templateId,
            updateObject,
            { 
                new: true, 
                runValidators: true,
                select: 'title previewUrl videoUrl imageUrl updatedAt'
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

        logger.info(`Template media updated: ${templateId}`, { updatedFields: updateFields });

        res.json(formatSuccessResponse({
            templateId: updatedTemplate._id,
            title: updatedTemplate.title,
            media: {
                previewUrl: updatedTemplate.previewUrl || '',
                videoUrl: updatedTemplate.videoUrl || '',
                imageUrl: updatedTemplate.imageUrl || ''
            },
            updatedAt: updatedTemplate.updatedAt
        }, updateFields));

    } catch (error) {
        logger.error('Error updating template media:', error);

        if (error.name === 'ValidationError') {
            const validationErrors = Object.keys(error.errors).map(key => ({
                field: key,
                message: error.errors[key].message,
                value: error.errors[key].value
            }));

            return res.status(400).json(formatErrorResponse(
                'VALIDATION_ERROR',
                'Media validation failed',
                null,
                null,
                { validationErrors }
            ));
        }

        res.status(500).json(formatErrorResponse(
            'SERVER_ERROR',
            'Internal server error while updating template media',
            null,
            null,
            error.message
        ));
    }
};

/**
 * Clear template media URLs
 * @route DELETE /api/templates/core/media/:templateId
 * @access Public
 */
const deleteTemplateMedia = async (req, res) => {
    try {
        const { templateId } = req.params;
        const { mediaType = 'all' } = req.query;

        if (!isValidObjectId(templateId)) {
            return res.status(400).json(formatErrorResponse(
                'VALIDATION_ERROR',
                'Invalid template ID format',
                'templateId',
                templateId
            ));
        }

        logger.info(`Clearing template media: ${templateId}, type: ${mediaType}`);

        // Determine which fields to clear
        const updateObject = { updatedAt: new Date() };
        const clearedFields = [];

        switch (mediaType.toLowerCase()) {
            case 'preview':
                updateObject.previewUrl = '';
                clearedFields.push('previewUrl');
                break;
            case 'video':
                updateObject.videoUrl = '';
                clearedFields.push('videoUrl');
                break;
            case 'image':
                updateObject.imageUrl = '';
                clearedFields.push('imageUrl');
                break;
            case 'all':
            default:
                updateObject.previewUrl = '';
                updateObject.videoUrl = '';
                updateObject.imageUrl = '';
                clearedFields.push('previewUrl', 'videoUrl', 'imageUrl');
                break;
        }

        const updatedTemplate = await Template.findByIdAndUpdate(
            templateId,
            updateObject,
            { 
                new: true,
                select: 'title previewUrl videoUrl imageUrl updatedAt'
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

        logger.info(`Template media cleared: ${templateId}`, { clearedFields });

        res.json(formatSuccessResponse({
            templateId: templateId,
            title: updatedTemplate.title,
            media: {
                previewUrl: updatedTemplate.previewUrl || '',
                videoUrl: updatedTemplate.videoUrl || '',
                imageUrl: updatedTemplate.imageUrl || ''
            },
            clearedFields,
            updatedAt: updatedTemplate.updatedAt,
            message: `Media ${mediaType} cleared successfully`
        }, clearedFields));

    } catch (error) {
        logger.error('Error clearing template media:', error);
        res.status(500).json(formatErrorResponse(
            'SERVER_ERROR',
            'Internal server error while clearing template media',
            null,
            null,
            error.message
        ));
    }
};

/**
 * Validate media URLs without saving
 * @route POST /api/templates/core/media/:templateId/validate
 * @access Public
 */
const validateMediaUrls = async (req, res) => {
    try {
        const { templateId } = req.params;
        const mediaData = req.body;

        if (templateId && !isValidObjectId(templateId)) {
            return res.status(400).json(formatErrorResponse(
                'VALIDATION_ERROR',
                'Invalid template ID format',
                'templateId',
                templateId
            ));
        }

        logger.info('Validating template media URLs:', mediaData);

        // Validate media URLs
        const validationResults = [];
        const allowedFields = ['previewUrl', 'videoUrl', 'imageUrl'];

        for (const field of allowedFields) {
            if (mediaData[field]) {
                const url = mediaData[field];
                const validation = validateMediaUrl(url, field);
                validationResults.push(validation);
            }
        }

        // Check if any validation failed
        const hasErrors = validationResults.some(result => !result.isValid);

        if (hasErrors) {
            const errors = validationResults.filter(result => !result.isValid);
            return res.status(400).json(formatErrorResponse(
                'VALIDATION_ERROR',
                'Media URL validation failed',
                null,
                null,
                { validationErrors: errors }
            ));
        }

        logger.info('Template media URL validation completed');

        res.json(formatSuccessResponse({
            isValid: true,
            validatedFields: Object.keys(mediaData),
            validationResults,
            message: 'Media URL validation passed'
        }));

    } catch (error) {
        logger.error('Error validating template media URLs:', error);
        res.status(500).json(formatErrorResponse(
            'SERVER_ERROR',
            'Internal server error during media validation',
            null,
            null,
            error.message
        ));
    }
};

/**
 * Helper function to validate individual media URLs
 * @param {string} url - URL to validate
 * @param {string} type - Type of media (preview, video, image)
 * @returns {object} - Validation result
 */
const validateMediaUrl = (url, type) => {
    try {
        // Basic URL validation
        const urlObj = new URL(url);
        
        // Check protocol
        if (!['http:', 'https:'].includes(urlObj.protocol)) {
            return {
                field: type,
                isValid: false,
                message: 'URL must use HTTP or HTTPS protocol',
                value: url
            };
        }

        // Type-specific validations
        switch (type) {
            case 'videoUrl':
                // Check for video file extensions or video platforms
                const videoPatterns = [
                    /\.(mp4|avi|mov|wmv|flv|webm)$/i,
                    /youtube\.com\/watch/i,
                    /youtu\.be\//i,
                    /vimeo\.com\//i
                ];
                const isVideoUrl = videoPatterns.some(pattern => pattern.test(url));
                if (!isVideoUrl) {
                    return {
                        field: type,
                        isValid: false,
                        message: 'URL does not appear to be a valid video URL',
                        value: url
                    };
                }
                break;

            case 'imageUrl':
                // Check for image file extensions
                const imagePattern = /\.(jpg|jpeg|png|gif|bmp|webp|svg)$/i;
                if (!imagePattern.test(url)) {
                    return {
                        field: type,
                        isValid: false,
                        message: 'URL does not appear to be a valid image URL',
                        value: url
                    };
                }
                break;

            case 'previewUrl':
                // Preview URL can be image or video
                const previewPatterns = [
                    /\.(jpg|jpeg|png|gif|bmp|webp|svg|mp4|avi|mov|wmv|flv|webm)$/i,
                    /youtube\.com\/watch/i,
                    /youtu\.be\//i,
                    /vimeo\.com\//i
                ];
                const isPreviewUrl = previewPatterns.some(pattern => pattern.test(url));
                if (!isPreviewUrl) {
                    return {
                        field: type,
                        isValid: false,
                        message: 'URL does not appear to be a valid preview URL (image or video)',
                        value: url
                    };
                }
                break;
        }

        return {
            field: type,
            isValid: true,
            message: 'Valid URL',
            value: url
        };

    } catch (error) {
        return {
            field: type,
            isValid: false,
            message: 'Invalid URL format',
            value: url
        };
    }
};

module.exports = {
    getTemplateMedia,
    updateTemplateMedia,
    deleteTemplateMedia,
    validateMediaUrls
};
