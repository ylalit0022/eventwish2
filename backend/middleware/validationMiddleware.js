const { createValidationError, asyncHandler } = require('./errorMiddleware');
const { validateFields, isValidObjectId } = require('../utils/templateHelpers');
const logger = require('../utils/logger');

/**
 * Comprehensive Validation Middleware System
 * Provides reusable validation functions for all API endpoints
 */

/**
 * Request ID Middleware
 * Adds unique request ID for tracking
 */
const addRequestId = (req, res, next) => {
    req.id = `req_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
    res.setHeader('X-Request-ID', req.id);
    next();
};

/**
 * Generic ObjectId Validation
 */
const validateObjectId = (paramName) => {
    return (req, res, next) => {
        const id = req.params[paramName];
        
        if (!id) {
            throw createValidationError(
                `${paramName} is required`,
                paramName,
                undefined,
                'Missing required parameter'
            );
        }
        
        if (!isValidObjectId(id)) {
            throw createValidationError(
                `Invalid ${paramName} format`,
                paramName,
                id,
                'Must be a valid MongoDB ObjectId'
            );
        }
        
        next();
    };
};

/**
 * Template ID Validation Middleware
 */
const validateTemplateId = validateObjectId('templateId');

/**
 * User ID Validation Middleware
 */
const validateUserId = validateObjectId('userId');

/**
 * Generic Required Fields Validation
 */
const validateRequiredFields = (requiredFields) => {
    return (req, res, next) => {
        const missingFields = [];
        
        requiredFields.forEach(field => {
            if (req.body[field] === undefined || req.body[field] === null || req.body[field] === '') {
                missingFields.push(field);
            }
        });
        
        if (missingFields.length > 0) {
            throw createValidationError(
                `Missing required fields: ${missingFields.join(', ')}`,
                missingFields[0],
                undefined,
                `Required fields: ${requiredFields.join(', ')}`
            );
        }
        
        next();
    };
};

/**
 * Field Type Validation Middleware
 */
const validateFieldTypes = (fieldDefinitions) => {
    return (req, res, next) => {
        const fieldsToValidate = {};
        
        // Extract fields that are present in the request
        Object.keys(fieldDefinitions).forEach(field => {
            if (req.body[field] !== undefined) {
                fieldsToValidate[field] = req.body[field];
            }
        });
        
        // Validate using existing template helpers
        const validation = validateFields(fieldsToValidate);
        
        if (!validation.isValid) {
            const firstError = validation.errors[0];
            throw createValidationError(
                firstError.error,
                firstError.field,
                firstError.value,
                validation.errors.map(e => `${e.field}: ${e.error}`).join('; ')
            );
        }
        
        next();
    };
};

/**
 * String Length Validation
 */
const validateStringLength = (field, minLength = 0, maxLength = Infinity) => {
    return (req, res, next) => {
        const value = req.body[field];
        
        if (value !== undefined) {
            if (typeof value !== 'string') {
                throw createValidationError(
                    `${field} must be a string`,
                    field,
                    value
                );
            }
            
            if (value.length < minLength) {
                throw createValidationError(
                    `${field} must be at least ${minLength} characters long`,
                    field,
                    value,
                    `Current length: ${value.length}, minimum: ${minLength}`
                );
            }
            
            if (value.length > maxLength) {
                throw createValidationError(
                    `${field} must be no more than ${maxLength} characters long`,
                    field,
                    value,
                    `Current length: ${value.length}, maximum: ${maxLength}`
                );
            }
        }
        
        next();
    };
};

/**
 * Number Range Validation
 */
const validateNumberRange = (field, min = -Infinity, max = Infinity) => {
    return (req, res, next) => {
        const value = req.body[field];
        
        if (value !== undefined) {
            if (typeof value !== 'number' || isNaN(value)) {
                throw createValidationError(
                    `${field} must be a valid number`,
                    field,
                    value
                );
            }
            
            if (value < min) {
                throw createValidationError(
                    `${field} must be at least ${min}`,
                    field,
                    value,
                    `Current value: ${value}, minimum: ${min}`
                );
            }
            
            if (value > max) {
                throw createValidationError(
                    `${field} must be no more than ${max}`,
                    field,
                    value,
                    `Current value: ${value}, maximum: ${max}`
                );
            }
        }
        
        next();
    };
};

/**
 * Array Validation
 */
const validateArray = (field, itemValidator = null, minLength = 0, maxLength = Infinity) => {
    return (req, res, next) => {
        const value = req.body[field];
        
        if (value !== undefined) {
            if (!Array.isArray(value)) {
                throw createValidationError(
                    `${field} must be an array`,
                    field,
                    value
                );
            }
            
            if (value.length < minLength) {
                throw createValidationError(
                    `${field} must contain at least ${minLength} items`,
                    field,
                    value,
                    `Current length: ${value.length}, minimum: ${minLength}`
                );
            }
            
            if (value.length > maxLength) {
                throw createValidationError(
                    `${field} must contain no more than ${maxLength} items`,
                    field,
                    value,
                    `Current length: ${value.length}, maximum: ${maxLength}`
                );
            }
            
            // Validate array items if validator provided
            if (itemValidator) {
                value.forEach((item, index) => {
                    if (!itemValidator(item)) {
                        throw createValidationError(
                            `Invalid item at index ${index} in ${field}`,
                            `${field}[${index}]`,
                            item
                        );
                    }
                });
            }
        }
        
        next();
    };
};

/**
 * Enum Validation
 */
const validateEnum = (field, allowedValues) => {
    return (req, res, next) => {
        const value = req.body[field];
        
        if (value !== undefined && !allowedValues.includes(value)) {
            throw createValidationError(
                `${field} must be one of: ${allowedValues.join(', ')}`,
                field,
                value,
                `Allowed values: [${allowedValues.join(', ')}]`
            );
        }
        
        next();
    };
};

/**
 * URL Validation
 */
const validateURL = (field, required = false) => {
    return (req, res, next) => {
        const value = req.body[field];
        
        if (required && (value === undefined || value === null || value === '')) {
            throw createValidationError(
                `${field} is required`,
                field,
                value
            );
        }
        
        if (value && value !== '') {
            try {
                new URL(value);
                if (!value.startsWith('http://') && !value.startsWith('https://')) {
                    throw new Error('Invalid protocol');
                }
            } catch (error) {
                throw createValidationError(
                    `${field} must be a valid URL`,
                    field,
                    value,
                    'URL must start with http:// or https://'
                );
            }
        }
        
        next();
    };
};

/**
 * Email Validation
 */
const validateEmail = (field) => {
    return (req, res, next) => {
        const value = req.body[field];
        
        if (value !== undefined) {
            const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
            if (!emailRegex.test(value)) {
                throw createValidationError(
                    `${field} must be a valid email address`,
                    field,
                    value
                );
            }
        }
        
        next();
    };
};

/**
 * Content Sanitization Middleware
 */
const sanitizeContent = (fields) => {
    return (req, res, next) => {
        fields.forEach(field => {
            if (req.body[field] && typeof req.body[field] === 'string') {
                // Basic XSS prevention
                req.body[field] = req.body[field]
                    .replace(/<script\b[^<]*(?:(?!<\/script>)<[^<]*)*<\/script>/gi, '')
                    .replace(/on\w+\s*=\s*"[^"]*"/gi, '')
                    .replace(/on\w+\s*=\s*'[^']*'/gi, '')
                    .replace(/javascript:/gi, '');
            }
        });
        
        next();
    };
};

/**
 * Rate Limiting Validation
 */
const validateRateLimit = (maxRequests = 100, windowMs = 15 * 60 * 1000) => {
    const requests = new Map();
    
    return (req, res, next) => {
        const clientId = req.ip || req.connection.remoteAddress;
        const now = Date.now();
        
        // Clean old entries
        for (const [id, data] of requests.entries()) {
            if (now - data.firstRequest > windowMs) {
                requests.delete(id);
            }
        }
        
        // Check current client
        const clientData = requests.get(clientId);
        
        if (!clientData) {
            requests.set(clientId, { firstRequest: now, count: 1 });
        } else {
            clientData.count++;
            
            if (clientData.count > maxRequests) {
                const resetTime = new Date(clientData.firstRequest + windowMs);
                res.setHeader('X-RateLimit-Limit', maxRequests);
                res.setHeader('X-RateLimit-Remaining', 0);
                res.setHeader('X-RateLimit-Reset', resetTime.toISOString());
                
                throw createValidationError(
                    'Too many requests',
                    'rate_limit',
                    clientData.count,
                    `Limit: ${maxRequests} requests per ${windowMs / 1000} seconds`
                );
            }
        }
        
        // Set rate limit headers
        const remaining = Math.max(0, maxRequests - (clientData?.count || 0));
        res.setHeader('X-RateLimit-Limit', maxRequests);
        res.setHeader('X-RateLimit-Remaining', remaining);
        
        next();
    };
};

/**
 * Template-specific validation combinations
 */
const templateValidators = {
    // Core content validation
    coreContent: [
        validateRequiredFields(['title', 'category', 'htmlContent']),
        validateStringLength('title', 1, 200),
        validateStringLength('category', 1, 100),
        validateStringLength('htmlContent', 1),
        validateStringLength('cssContent', 0, 50000),
        validateStringLength('jsContent', 0, 50000),
        validateURL('previewUrl'),
        validateURL('videoUrl'),
        validateURL('imageUrl')
    ],
    
    // Monetization validation
    monetization: [
        validateEnum('moderationStatus', ['approved', 'pending', 'rejected']),
        validateNumberRange('price', 0, 999.99)
    ],
    
    // Metrics validation
    metrics: [
        validateNumberRange('usageCount', 0),
        validateNumberRange('likes', 0),
        validateNumberRange('favorites', 0),
        validateNumberRange('viewCount', 0),
        validateNumberRange('sharedCount', 0),
        validateNumberRange('downloadCount', 0),
        validateNumberRange('reportCount', 0),
        validateNumberRange('rating', 0, 5),
        validateNumberRange('ratingCount', 0),
        validateNumberRange('boostPriority', 0)
    ]
};

module.exports = {
    addRequestId,
    validateObjectId,
    validateTemplateId,
    validateUserId,
    validateRequiredFields,
    validateFieldTypes,
    validateStringLength,
    validateNumberRange,
    validateArray,
    validateEnum,
    validateURL,
    validateEmail,
    sanitizeContent,
    validateRateLimit,
    templateValidators,
    asyncHandler
}; 