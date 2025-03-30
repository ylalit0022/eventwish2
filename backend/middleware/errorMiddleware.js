const logger = require('../config/logger');

const errorHandler = (err, req, res, next) => {
    logger.error({
        message: err.message,
        stack: err.stack,
        request: {
            method: req.method,
            path: req.path,
            headers: req.headers,
            body: req.body
        }
    }, 'Error occurred');

    // Handle JWT errors
    if (err.name === 'JsonWebTokenError') {
        return res.status(401).json({
            success: false,
            message: 'Invalid token',
            error: 'AUTH_TOKEN_INVALID'
        });
    }

    // Handle validation errors
    if (err.name === 'ValidationError') {
        return res.status(400).json({
            success: false,
            message: err.message,
            error: 'VALIDATION_ERROR'
        });
    }

    // Default error
    res.status(500).json({
        success: false,
        message: 'Internal server error',
        error: 'SERVER_ERROR',
        details: process.env.NODE_ENV === 'development' ? err.message : undefined
    });
};

module.exports = errorHandler;
