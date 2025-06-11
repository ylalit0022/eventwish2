/**
 * Input validation middleware functions
 */
const logger = require('../utils/logger');

/**
 * Validates device ID in request body
 * @param {Object} req - Express request object
 * @param {Object} res - Express response object
 * @param {Function} next - Express next middleware function
 */
const validateDeviceId = (req, res, next) => {
    const { deviceId } = req.body;
    
    // Check if deviceId exists
    if (!deviceId) {
        logger.warn('Device ID validation failed: Missing deviceId');
        return res.status(400).json({
            success: false,
            message: 'Device ID is required'
        });
    }
    
    // Check if deviceId is a string
    if (typeof deviceId !== 'string') {
        logger.warn(`Device ID validation failed: Invalid type ${typeof deviceId}`);
        return res.status(400).json({
            success: false,
            message: 'Device ID must be a string'
        });
    }
    
    // Check minimum length
    if (deviceId.trim().length < 8) {
        logger.warn(`Device ID validation failed: Too short ${deviceId.length}`);
        return res.status(400).json({
            success: false,
            message: 'Device ID must be at least 8 characters long'
        });
    }
    
    // Check maximum length
    if (deviceId.trim().length > 64) {
        logger.warn(`Device ID validation failed: Too long ${deviceId.length}`);
        return res.status(400).json({
            success: false,
            message: 'Device ID cannot exceed 64 characters'
        });
    }
    
    // Validate format (alphanumeric plus some common separators)
    const validDeviceIdPattern = /^[a-zA-Z0-9._\-:]+$/;
    if (!validDeviceIdPattern.test(deviceId)) {
        logger.warn(`Device ID validation failed: Invalid format ${deviceId}`);
        return res.status(400).json({
            success: false,
            message: 'Device ID contains invalid characters'
        });
    }
    
    // All checks passed
    next();
};

/**
 * Validates category name
 * @param {Object} req - Express request object
 * @param {Object} res - Express response object
 * @param {Function} next - Express next middleware function
 */
const validateCategory = (req, res, next) => {
    const { category } = req.body;
    
    // Skip validation if category is not provided
    if (!category) {
        return next();
    }
    
    // Check if category is a string
    if (typeof category !== 'string') {
        logger.warn(`Category validation failed: Invalid type ${typeof category}`);
        return res.status(400).json({
            success: false,
            message: 'Category must be a string'
        });
    }
    
    // Check minimum length
    if (category.trim().length < 2) {
        logger.warn(`Category validation failed: Too short ${category.length}`);
        return res.status(400).json({
            success: false,
            message: 'Category must be at least 2 characters long'
        });
    }
    
    // Check maximum length
    if (category.trim().length > 50) {
        logger.warn(`Category validation failed: Too long ${category.length}`);
        return res.status(400).json({
            success: false,
            message: 'Category cannot exceed 50 characters'
        });
    }
    
    // All checks passed
    next();
};

/**
 * Middleware to validate Firebase UID
 * @param {Object} req - Express request object
 * @param {Object} res - Express response object
 * @param {Function} next - Express next function
 * @returns {void}
 */
const validateFirebaseUid = (req, res, next) => {
    const uid = req.body.uid || req.params.uid || req.query.uid;
    
    if (!uid) {
        return res.status(400).json({
            success: false,
            message: 'Firebase UID is required'
        });
    }
    
    // Checking for development mode
    if (process.env.NODE_ENV === 'development' && process.env.SKIP_AUTH === 'true') {
        // Allow test UIDs in development mode
        if (uid.startsWith('test-') && typeof uid === 'string' && uid.length >= 10) {
            return next();
        }
    }
    
    // Firebase UIDs are typically 28 characters
    if (typeof uid !== 'string' || uid.length < 20) {
        return res.status(400).json({
            success: false,
            message: 'Invalid Firebase UID format'
        });
    }
    
    next();
};

module.exports = {
    validateDeviceId,
    validateCategory,
    validateFirebaseUid
}; 