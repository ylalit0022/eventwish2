const mongoose = require('mongoose');
const Template = require('../models/Template');
const logger = require('./logger');

/**
 * Template Helper Utilities
 * Common functions used across all template API endpoints
 */

/**
 * Validates if a string is a valid MongoDB ObjectId
 * @param {string} id - The ID to validate
 * @returns {boolean} - True if valid ObjectId, false otherwise
 */
const isValidObjectId = (id) => {
    return mongoose.Types.ObjectId.isValid(id);
};

/**
 * Validates if a template ID exists in the database
 * @param {string} templateId - The template ID to validate
 * @returns {Promise<boolean>} - True if template exists, false otherwise
 */
const templateExists = async (templateId) => {
    try {
        if (!isValidObjectId(templateId)) {
            return false;
        }
        const template = await Template.findById(templateId).select('_id');
        return !!template;
    } catch (error) {
        logger.error('Error checking template existence:', error);
        return false;
    }
};

/**
 * Validates field types based on Template schema
 * @param {string} fieldName - Name of the field
 * @param {any} value - Value to validate
 * @returns {Object} - {isValid: boolean, error: string}
 */
const validateFieldType = (fieldName, value) => {
    const fieldValidations = {
        // String fields
        title: (val) => typeof val === 'string' && val.trim().length > 0 && val.length <= 200,
        category: (val) => typeof val === 'string' && val.trim().length > 0 && val.length <= 100,
        htmlContent: (val) => typeof val === 'string' && val.trim().length > 0,
        cssContent: (val) => typeof val === 'string',
        jsContent: (val) => typeof val === 'string',
        previewUrl: (val) => typeof val === 'string' && (val === '' || /^https?:\/\/.+/.test(val)),
        videoUrl: (val) => typeof val === 'string' && (val === '' || /^https?:\/\/.+/.test(val)),
        imageUrl: (val) => typeof val === 'string' && (val === '' || /^https?:\/\/.+/.test(val)),
        festivalTag: (val) => typeof val === 'string' && val.length <= 100,
        aiPrompt: (val) => typeof val === 'string' && val.length <= 1000,
        aiModel: (val) => typeof val === 'string' && val.length <= 100,
        aiStyle: (val) => typeof val === 'string' && val.length <= 100,
        experimentTag: (val) => typeof val === 'string' && val.length <= 100,
        categoryIcon: (val) => typeof val === 'string' && (val === '' || /^https?:\/\/.+/.test(val)),
        generatedByUserUid: (val) => val === null || val === '' || /^[a-zA-Z0-9_-]{10,128}$/.test(val),
        creatorUid: (val) => val === null || val === '' || /^[a-zA-Z0-9_-]{10,128}$/.test(val),
        
        // Number fields
        price: (val) => typeof val === 'number' && val >= 0,
        usageCount: (val) => typeof val === 'number' && val >= 0,
        likes: (val) => typeof val === 'number' && val >= 0,
        favorites: (val) => typeof val === 'number' && val >= 0,
        viewCount: (val) => typeof val === 'number' && val >= 0,
        sharedCount: (val) => typeof val === 'number' && val >= 0,
        downloadCount: (val) => typeof val === 'number' && val >= 0,
        reportCount: (val) => typeof val === 'number' && val >= 0,
        rating: (val) => typeof val === 'number' && val >= 0 && val <= 5,
        ratingCount: (val) => typeof val === 'number' && val >= 0,
        visibilityScore: (val) => typeof val === 'number',
        boostPriority: (val) => typeof val === 'number' && val >= 0,
        weeklyUsageCount: (val) => typeof val === 'number' && val >= 0,
        weeklyLikes: (val) => typeof val === 'number' && val >= 0,
        weeklyFavorites: (val) => typeof val === 'number' && val >= 0,
        weeklyViewCount: (val) => typeof val === 'number' && val >= 0,
        weeklySharedCount: (val) => typeof val === 'number' && val >= 0,
        weeklyDownloadCount: (val) => typeof val === 'number' && val >= 0,
        weeklyReportCount: (val) => typeof val === 'number' && val >= 0,
        
        // Boolean fields
        status: (val) => typeof val === 'boolean',
        isPremium: (val) => typeof val === 'boolean',
        isFeatured: (val) => typeof val === 'boolean',
        isTrending: (val) => typeof val === 'boolean',
        isFlagged: (val) => typeof val === 'boolean',
        isLowPerforming: (val) => typeof val === 'boolean',
        isAIGenerated: (val) => typeof val === 'boolean',
        
        // Array fields
        tags: (val) => Array.isArray(val) && val.every(tag => typeof tag === 'string' && tag.length <= 50),
        styleTags: (val) => Array.isArray(val) && val.every(tag => typeof tag === 'string' && tag.length <= 50),
        searchKeywords: (val) => Array.isArray(val) && val.every(keyword => typeof keyword === 'string' && keyword.length <= 50),
        relatedTemplates: (val) => Array.isArray(val) && val.every(id => isValidObjectId(id)),
        ignoredByUsers: (val) => Array.isArray(val) && val.every(id => isValidObjectId(id)),
        
        // Enum fields
        aiGenerationStage: (val) => ['initial', 'on_edit', 'variation'].includes(val),
        templateType: (val) => ['html', 'image', 'video'].includes(val),
        moderationStatus: (val) => ['approved', 'pending', 'rejected'].includes(val),
        
        // ObjectId fields
        creatorId: (val) => val === null || isValidObjectId(val),
        generatedByUser: (val) => val === null || isValidObjectId(val),
        variationOf: (val) => val === null || isValidObjectId(val),
        language: (val) => val === null || val === '' || isValidObjectId(val),
        region: (val) => val === null || val === '' || isValidObjectId(val),
        
        // Date fields
        lastBoostedAt: (val) => val === null || val instanceof Date || !isNaN(Date.parse(val)),
        weeklyScoreLastReset: (val) => val instanceof Date || !isNaN(Date.parse(val))
    };

    const validator = fieldValidations[fieldName];
    if (!validator) {
        return { isValid: false, error: `Unknown field: ${fieldName}` };
    }

    const isValid = validator(value);
    return {
        isValid,
        error: isValid ? null : `Invalid value for field '${fieldName}': ${value}`
    };
};

/**
 * Validates multiple fields at once
 * @param {Object} fields - Object with field names as keys and values
 * @returns {Object} - {isValid: boolean, errors: Array}
 */
const validateFields = (fields) => {
    const errors = [];
    
    for (const [fieldName, value] of Object.entries(fields)) {
        const validation = validateFieldType(fieldName, value);
        if (!validation.isValid) {
            errors.push({
                field: fieldName,
                value: value,
                error: validation.error
            });
        }
    }
    
    return {
        isValid: errors.length === 0,
        errors
    };
};

/**
 * Sanitizes HTML content to prevent XSS attacks
 * @param {string} htmlContent - HTML content to sanitize
 * @returns {string} - Sanitized HTML content
 */
const sanitizeHtmlContent = (htmlContent) => {
    if (typeof htmlContent !== 'string') {
        return '';
    }
    
    // Basic XSS prevention - remove script tags and event handlers
    return htmlContent
        .replace(/<script\b[^<]*(?:(?!<\/script>)<[^<]*)*<\/script>/gi, '')
        .replace(/on\w+\s*=\s*"[^"]*"/gi, '')
        .replace(/on\w+\s*=\s*'[^']*'/gi, '')
        .replace(/javascript:/gi, '');
};

/**
 * Formats error response according to API standards
 * @param {string} code - Error code
 * @param {string} message - Error message
 * @param {string} field - Field name (optional)
 * @param {any} value - Field value (optional)
 * @param {string} details - Additional details (optional)
 * @returns {Object} - Formatted error response
 */
const formatErrorResponse = (code, message, field = null, value = null, details = null) => {
    return {
        success: false,
        error: {
            code,
            message,
            ...(field && { field }),
            ...(value !== null && { value }),
            ...(details && { details })
        },
        timestamp: new Date().toISOString()
    };
};

/**
 * Formats success response according to API standards
 * @param {any} data - Response data
 * @param {Array} modified - Modified fields (optional)
 * @param {number} version - Data version (optional)
 * @returns {Object} - Formatted success response
 */
const formatSuccessResponse = (data, modified = null, version = null) => {
    return {
        success: true,
        data: {
            ...data,
            ...(modified && { modified }),
            ...(version && { version })
        },
        timestamp: new Date().toISOString()
    };
};

/**
 * Enhanced async operation wrapper with comprehensive error handling
 * @param {Function} operation - Async operation to execute
 * @param {Object} res - Express response object
 * @param {string} operationName - Name of the operation for logging
 * @returns {Promise<any>} - Operation result or throws error for middleware
 */
const handleAsyncOperation = async (operation, res, operationName = 'operation') => {
    try {
        const result = await operation();
        return result;
    } catch (error) {
        // If error is already an APIError, let the error middleware handle it
        if (error.name === 'APIError') {
            throw error;
        }
        
        logger.error(`Error in ${operationName}:`, {
            message: error.message,
            stack: error.stack,
            operationName,
            timestamp: new Date().toISOString()
        });

        // Transform common errors to APIErrors for consistent handling
        const { APIError, ErrorTypes } = require('../middleware/errorMiddleware');
        
        if (error.name === 'ValidationError') {
            throw new APIError(
                ErrorTypes.VALIDATION_ERROR,
                'Data validation failed',
                Object.values(error.errors).map(e => ({
                    field: e.path,
                    message: e.message,
                    value: e.value
                }))
            );
        }

        if (error.name === 'CastError') {
            throw new APIError(
                ErrorTypes.VALIDATION_ERROR,
                `Invalid ${error.path}: ${error.value}`,
                `Expected ${error.kind}, received ${typeof error.value}`,
                error.path,
                error.value
            );
        }

        if (error.code === 11000) {
            const field = Object.keys(error.keyPattern)[0];
            throw new APIError(
                ErrorTypes.CONFLICT_ERROR,
                `${field} already exists`,
                'Duplicate key error',
                field,
                error.keyValue[field]
            );
        }

        // For database connection errors
        if (error.name === 'MongoServerError' || error.name === 'MongoNetworkError') {
            throw new APIError(
                ErrorTypes.DATABASE_ERROR,
                'Database operation failed',
                process.env.NODE_ENV === 'development' ? error.message : 'Database temporarily unavailable'
            );
        }

        // Default server error
        throw new APIError(
            ErrorTypes.SERVER_ERROR,
            `Error in ${operationName}`,
            process.env.NODE_ENV === 'development' ? error.message : 'An unexpected error occurred'
        );
    }
};

/**
 * Calculates trending score for a template
 * @param {Object} template - Template object
 * @returns {number} - Calculated trending score
 */
const calculateTrendingScore = (template) => {
    return (
        (template.usageCount || 0) * 3 +
        (template.likes || 0) * 2 +
        (template.favorites || 0) * 2 +
        (template.viewCount || 0) +
        (template.sharedCount || 0) +
        (template.downloadCount || 0) -
        (template.reportCount || 0) * 5
    );
};

/**
 * Calculates weekly trending score for a template
 * @param {Object} template - Template object
 * @returns {number} - Calculated weekly trending score
 */
const calculateWeeklyTrendingScore = (template) => {
    return (
        (template.weeklyUsageCount || 0) * 3 +
        (template.weeklyLikes || 0) * 2 +
        (template.weeklyFavorites || 0) * 2 +
        (template.weeklyViewCount || 0) +
        (template.weeklySharedCount || 0) +
        (template.weeklyDownloadCount || 0) -
        (template.weeklyReportCount || 0) * 5
    );
};

/**
 * Checks if weekly metrics need to be reset (weekly basis)
 * @param {Date} lastReset - Last reset date
 * @returns {boolean} - True if reset is needed
 */
const shouldResetWeeklyMetrics = (lastReset) => {
    if (!lastReset) return true;
    
    const now = new Date();
    const resetDate = new Date(lastReset);
    const daysDiff = Math.floor((now - resetDate) / (1000 * 60 * 60 * 24));
    
    return daysDiff >= 7;
};

/**
 * Gets template field groups for organized API responses
 * @returns {Object} - Field groups object
 */
const getTemplateFieldGroups = () => {
    return {
        core: ['title', 'category', 'htmlContent', 'cssContent', 'jsContent', 'previewUrl', 'videoUrl', 'imageUrl'],
        monetization: ['status', 'isPremium', 'isFeatured', 'isTrending', 'isFlagged', 'isLowPerforming', 'price', 'moderationStatus'],
        metrics: ['usageCount', 'likes', 'favorites', 'viewCount', 'sharedCount', 'downloadCount', 'reportCount', 'rating', 'ratingCount'],
        weeklyMetrics: ['weeklyUsageCount', 'weeklyLikes', 'weeklyFavorites', 'weeklyViewCount', 'weeklySharedCount', 'weeklyDownloadCount', 'weeklyReportCount', 'weeklyScoreLastReset'],
        ai: ['isAIGenerated', 'aiPrompt', 'aiModel', 'aiStyle', 'aiGenerationStage', 'generatedByUser', 'generatedByUserUid', 'generationMetadata'],
        categorization: ['creatorId', 'creatorUid', 'festivalTag', 'tags', 'styleTags', 'searchKeywords', 'variationOf', 'relatedTemplates'],
        visibility: ['templateType', 'visibilityScore', 'boostPriority', 'lastBoostedAt', 'ignoredByUsers'],
        customization: ['customizationOptions'],
        legacy: ['categoryIcon', 'language', 'region', 'experimentTag', 'performanceLog']
    };
};

module.exports = {
    isValidObjectId,
    templateExists,
    validateFieldType,
    validateFields,
    sanitizeHtmlContent,
    formatErrorResponse,
    formatSuccessResponse,
    handleAsyncOperation,
    calculateTrendingScore,
    calculateWeeklyTrendingScore,
    shouldResetWeeklyMetrics,
    getTemplateFieldGroups
}; 