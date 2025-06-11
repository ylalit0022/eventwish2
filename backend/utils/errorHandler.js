/**
 * Error Handler Utility
 * 
 * Handles errors in a consistent way across the API
 */

const logger = require('../utils/logger');

/**
 * Handle error for API responses
 * @param {Object} res - Express response object
 * @param {Error} error - Error object
 * @param {number} [statusCode=500] - HTTP status code to return
 */
exports.handleError = (res, error, statusCode = 500) => {
    // Log the error
    logger.error(`API Error: ${error.message}`, { 
        stack: error.stack,
        code: error.code
    });
    
    // Check for specific error types
    if (error.name === 'ValidationError') {
        return res.status(400).json({
            success: false,
            message: 'Validation error',
            error: error.message,
            details: error.errors
        });
    }
    
    if (error.name === 'MongoError' || error.name === 'MongoServerError') {
        // Handle MongoDB specific errors
        if (error.code === 11000) {
            // Duplicate key error
            return res.status(409).json({
                success: false,
                message: 'Duplicate entry',
                error: 'A record with this information already exists'
            });
        }
        
        return res.status(500).json({
            success: false,
            message: 'Database error',
            error: error.message
        });
    }
    
    // Default error response
    return res.status(statusCode).json({
        success: false,
        message: 'Server error',
        error: error.message
    });
};

/**
 * Format validation error messages
 * @param {Object} validationErrors - Validation errors object from express-validator
 * @returns {Object} - Formatted error object
 */
exports.formatValidationErrors = (validationErrors) => {
    const errors = {};
    
    validationErrors.forEach(error => {
        errors[error.param] = error.msg;
    });
    
    return errors;
};

/**
 * Handle not found errors
 * @param {Object} res - Express response object
 * @param {string} entity - Name of the entity that wasn't found
 * @param {string} [id] - ID that was searched for
 */
exports.handleNotFound = (res, entity, id) => {
    const message = id 
        ? `${entity} not found with ID: ${id}`
        : `${entity} not found`;
        
    logger.warn(message);
    
    return res.status(404).json({
        success: false,
        message
    });
};

/**
 * Handle unauthorized errors
 * @param {Object} res - Express response object
 * @param {string} [message] - Custom error message
 */
exports.handleUnauthorized = (res, message = 'Unauthorized access') => {
    logger.warn(`Unauthorized access: ${message}`);
    
    return res.status(401).json({
        success: false,
        message
    });
};

/**
 * Handle forbidden errors
 * @param {Object} res - Express response object
 * @param {string} [message] - Custom error message
 */
exports.handleForbidden = (res, message = 'Access forbidden') => {
    logger.warn(`Forbidden access: ${message}`);
    
    return res.status(403).json({
        success: false,
        message
    });
}; 