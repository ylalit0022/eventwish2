const logger = require('../config/logger');

/**
 * Enhanced Error Handling Middleware
 * Provides comprehensive error classification, logging, and response formatting
 */

/**
 * Error Types and Classification
 */
const ErrorTypes = {
    VALIDATION_ERROR: 'VALIDATION_ERROR',
    AUTHENTICATION_ERROR: 'AUTHENTICATION_ERROR',
    AUTHORIZATION_ERROR: 'AUTHORIZATION_ERROR',
    NOT_FOUND_ERROR: 'NOT_FOUND_ERROR',
    CONFLICT_ERROR: 'CONFLICT_ERROR',
    RATE_LIMIT_ERROR: 'RATE_LIMIT_ERROR',
    DATABASE_ERROR: 'DATABASE_ERROR',
    EXTERNAL_SERVICE_ERROR: 'EXTERNAL_SERVICE_ERROR',
    SERVER_ERROR: 'SERVER_ERROR',
    BUSINESS_LOGIC_ERROR: 'BUSINESS_LOGIC_ERROR'
};

/**
 * HTTP Status Codes mapping
 */
const StatusCodes = {
    [ErrorTypes.VALIDATION_ERROR]: 400,
    [ErrorTypes.AUTHENTICATION_ERROR]: 401,
    [ErrorTypes.AUTHORIZATION_ERROR]: 403,
    [ErrorTypes.NOT_FOUND_ERROR]: 404,
    [ErrorTypes.CONFLICT_ERROR]: 409,
    [ErrorTypes.RATE_LIMIT_ERROR]: 429,
    [ErrorTypes.DATABASE_ERROR]: 500,
    [ErrorTypes.EXTERNAL_SERVICE_ERROR]: 502,
    [ErrorTypes.SERVER_ERROR]: 500,
    [ErrorTypes.BUSINESS_LOGIC_ERROR]: 422
};

/**
 * Custom Error Class for structured error handling
 */
class APIError extends Error {
    constructor(type, message, details = null, field = null, value = null) {
        super(message);
        this.name = 'APIError';
        this.type = type;
        this.statusCode = StatusCodes[type] || 500;
        this.details = details;
        this.field = field;
        this.value = value;
        this.timestamp = new Date().toISOString();
        
        // Capture stack trace
        Error.captureStackTrace(this, APIError);
    }
}

/**
 * Error Classification Helper
 */
const classifyError = (err) => {
    // MongoDB/Mongoose errors
    if (err.name === 'ValidationError') {
        return {
            type: ErrorTypes.VALIDATION_ERROR,
            message: 'Data validation failed',
            details: Object.values(err.errors).map(e => ({
                field: e.path,
                message: e.message,
                value: e.value
            }))
        };
    }
    
    if (err.name === 'CastError') {
        return {
            type: ErrorTypes.VALIDATION_ERROR,
            message: `Invalid ${err.path}: ${err.value}`,
            field: err.path,
            value: err.value
        };
    }
    
    if (err.code === 11000) {
        const field = Object.keys(err.keyPattern)[0];
        return {
            type: ErrorTypes.CONFLICT_ERROR,
            message: `${field} already exists`,
            field: field,
            value: err.keyValue[field]
        };
    }
    
    // JWT/Authentication errors
    if (err.name === 'JsonWebTokenError') {
        return {
            type: ErrorTypes.AUTHENTICATION_ERROR,
            message: 'Invalid authentication token',
            details: err.message
        };
    }
    
    if (err.name === 'TokenExpiredError') {
        return {
            type: ErrorTypes.AUTHENTICATION_ERROR,
            message: 'Authentication token has expired',
            details: 'Please login again'
        };
    }
    
    // Rate limiting errors
    if (err.name === 'RateLimitError') {
        return {
            type: ErrorTypes.RATE_LIMIT_ERROR,
            message: 'Too many requests',
            details: err.message
        };
    }
    
    // Custom API errors
    if (err instanceof APIError) {
        return {
            type: err.type,
            message: err.message,
            details: err.details,
            field: err.field,
            value: err.value
        };
    }
    
    // Network/External service errors
    if (err.code === 'ECONNREFUSED' || err.code === 'ENOTFOUND') {
        return {
            type: ErrorTypes.EXTERNAL_SERVICE_ERROR,
            message: 'External service unavailable',
            details: err.message
        };
    }
    
    // Database connection errors
    if (err.name === 'MongoServerError' || err.name === 'MongoNetworkError') {
        return {
            type: ErrorTypes.DATABASE_ERROR,
            message: 'Database connection error',
            details: process.env.NODE_ENV === 'development' ? err.message : 'Database temporarily unavailable'
        };
    }
    
    // Default server error
    return {
        type: ErrorTypes.SERVER_ERROR,
        message: 'Internal server error',
        details: process.env.NODE_ENV === 'development' ? err.message : 'An unexpected error occurred'
    };
};

/**
 * Format error response according to API standards
 */
const formatErrorResponse = (classification, requestId = null) => {
    const response = {
        success: false,
        error: {
            code: classification.type,
            message: classification.message,
            timestamp: new Date().toISOString()
        }
    };
    
    // Add optional fields if present
    if (classification.details) response.error.details = classification.details;
    if (classification.field) response.error.field = classification.field;
    if (classification.value !== undefined) response.error.value = classification.value;
    if (requestId) response.requestId = requestId;
    
    return response;
};

/**
 * Log error with appropriate level and context
 */
const logError = (err, req, classification) => {
    const logContext = {
        errorType: classification.type,
        statusCode: StatusCodes[classification.type] || 500,
        message: err.message,
        stack: err.stack,
        request: {
            method: req.method,
            path: req.path,
            url: req.originalUrl,
            userAgent: req.get('User-Agent'),
            ip: req.ip,
            headers: {
                'content-type': req.get('Content-Type'),
                'authorization': req.get('Authorization') ? '[REDACTED]' : undefined
            }
        },
        timestamp: new Date().toISOString()
    };
    
    // Log body for POST/PUT requests (excluding sensitive data)
    if (['POST', 'PUT', 'PATCH'].includes(req.method) && req.body) {
        logContext.request.body = sanitizeRequestBody(req.body);
    }
    
    // Log with appropriate level based on error type
    if ([ErrorTypes.SERVER_ERROR, ErrorTypes.DATABASE_ERROR].includes(classification.type)) {
        logger.error('Server Error', logContext);
    } else if ([ErrorTypes.EXTERNAL_SERVICE_ERROR].includes(classification.type)) {
        logger.warn('External Service Error', logContext);
    } else {
        logger.info('Client Error', logContext);
    }
};

/**
 * Sanitize request body for logging (remove sensitive information)
 */
const sanitizeRequestBody = (body) => {
    const sensitiveFields = ['password', 'token', 'apiKey', 'secret', 'authorization'];
    const sanitized = { ...body };
    
    sensitiveFields.forEach(field => {
        if (sanitized[field]) {
            sanitized[field] = '[REDACTED]';
        }
    });
    
    return sanitized;
};

/**
 * Enhanced Error Handler Middleware
 */
const errorHandler = (err, req, res, next) => {
    // Generate request ID for tracking
    const requestId = req.id || `req_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
    
    // Classify the error
    const classification = classifyError(err);
    
    // Log the error
    logError(err, req, classification);
    
    // Format response
    const response = formatErrorResponse(classification, requestId);
    
    // Send response
    const statusCode = StatusCodes[classification.type] || 500;
    res.status(statusCode).json(response);
};

/**
 * Not Found Handler (404 errors)
 */
const notFoundHandler = (req, res) => {
    const response = formatErrorResponse({
        type: ErrorTypes.NOT_FOUND_ERROR,
        message: `Route ${req.method} ${req.originalUrl} not found`,
        details: 'The requested endpoint does not exist'
    });
    
    logger.info('Route Not Found', {
        method: req.method,
        path: req.originalUrl,
        ip: req.ip,
        userAgent: req.get('User-Agent')
    });
    
    res.status(404).json(response);
};

/**
 * Async Error Wrapper
 * Wraps async route handlers to catch errors automatically
 */
const asyncHandler = (fn) => {
    return (req, res, next) => {
        Promise.resolve(fn(req, res, next)).catch(next);
    };
};

/**
 * Validation Error Helper
 * Creates standardized validation errors
 */
const createValidationError = (message, field = null, value = null, details = null) => {
    return new APIError(ErrorTypes.VALIDATION_ERROR, message, details, field, value);
};

/**
 * Business Logic Error Helper
 * Creates standardized business logic errors
 */
const createBusinessLogicError = (message, details = null) => {
    return new APIError(ErrorTypes.BUSINESS_LOGIC_ERROR, message, details);
};

/**
 * Not Found Error Helper
 * Creates standardized not found errors
 */
const createNotFoundError = (resource, identifier = null) => {
    const message = identifier ? 
        `${resource} with identifier '${identifier}' not found` : 
        `${resource} not found`;
    return new APIError(ErrorTypes.NOT_FOUND_ERROR, message, null, 'id', identifier);
};

/**
 * Authorization Error Helper
 * Creates standardized authorization errors
 */
const createAuthorizationError = (message = 'Access denied', details = null) => {
    return new APIError(ErrorTypes.AUTHORIZATION_ERROR, message, details);
};

module.exports = {
    errorHandler,
    notFoundHandler,
    asyncHandler,
    APIError,
    ErrorTypes,
    createValidationError,
    createBusinessLogicError,
    createNotFoundError,
    createAuthorizationError
};
