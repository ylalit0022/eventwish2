const mongoose = require('mongoose');
const Template = require('../../../models/Template');
const logger = require('../../../utils/logger');

/**
 * Helper function to validate ObjectId
 * @param {string} id - ID to validate
 * @returns {boolean} - Whether the ID is valid
 */
function isValidObjectId(id) {
    return mongoose.Types.ObjectId.isValid(id);
}

/**
 * Helper function to handle async operations with comprehensive error handling
 * @param {Function} operation - Async operation to execute
 * @param {string} operationName - Name of the operation for logging
 * @param {Object} res - Express response object
 * @param {string} uid - User ID for logging
 * @returns {Promise} - Result of the operation or error response
 */
async function handleAsyncOperation(operation, operationName, res, uid = 'unknown') {
    try {
        return await operation();
    } catch (error) {
        logger.error(`${operationName} error for user ${uid}: ${error.message}`, {
            stack: error.stack,
            operation: operationName,
            uid: uid
        });
        
        // Handle specific error types
        if (error.name === 'ValidationError') {
            const validationErrors = Object.values(error.errors).map(err => err.message);
            return res.status(400).json({
                success: false,
                message: `Validation error during ${operationName}`,
                errors: validationErrors
            });
        }
        
        if (error.name === 'CastError') {
            return res.status(400).json({
                success: false,
                message: 'Invalid ID format',
                error: 'INVALID_ID'
            });
        }
        
        if (error.code === 11000) {
            return res.status(400).json({
                success: false,
                message: 'Duplicate entry error',
                error: 'DUPLICATE_ENTRY'
            });
        }
        
        if (error.name === 'MongoNetworkError' || error.name === 'MongoTimeoutError') {
            return res.status(503).json({
                success: false,
                message: 'Database connection error. Please try again.',
                error: 'DATABASE_CONNECTION_ERROR'
            });
        }
        
        return res.status(500).json({
            success: false,
            message: `Server error during ${operationName}`,
            error: error.message
        });
    }
}

/**
 * Helper function to clean up invalid subscription data
 * @param {Object} user - User document
 */
function cleanupSubscriptionData(user) {
    if (user.subscription) {
        // Fix invalid plan values
        const validPlans = ['MONTHLY', 'QUARTERLY', 'HALF_YEARLY', 'YEARLY', 'NONE'];
        if (!validPlans.includes(user.subscription.plan)) {
            logger.warn(`Fixing invalid subscription plan: ${user.subscription.plan} -> 'NONE'`);
            user.subscription.plan = 'NONE';
        }
        
        // Fix invalid planLevel values
        const validPlanLevels = ['BASIC', 'PREMIUM', 'PRO', 'NONE'];
        if (user.subscription.planLevel !== undefined && !validPlanLevels.includes(user.subscription.planLevel)) {
            logger.warn(`Fixing invalid subscription planLevel: ${user.subscription.planLevel} -> 'NONE'`);
            user.subscription.planLevel = 'NONE';
        }
        
        // Ensure planLevel is set to NONE if undefined
        if (user.subscription.planLevel === undefined || user.subscription.planLevel === null) {
            user.subscription.planLevel = 'NONE';
        }
    }
}

/**
 * Helper function to safely update template counts with transaction support
 * @param {string} templateId - Template ID
 * @param {Object} updates - Object with count updates (e.g., { likes: 1, favorites: -1 })
 * @param {Object} session - MongoDB session for transaction
 * @returns {Promise<Object>} - Updated template or null if not found
 */
async function safeUpdateTemplateCounts(templateId, updates, session = null) {
    try {
        if (!isValidObjectId(templateId)) {
            throw new Error('Invalid template ID format');
        }
        
        // Build the update object
        const updateObj = {};
        for (const [field, increment] of Object.entries(updates)) {
            if (typeof increment === 'number') {
                updateObj[`$inc`] = updateObj[`$inc`] || {};
                updateObj[`$inc`][field] = increment;
            }
        }
        
        if (Object.keys(updateObj).length === 0) {
            throw new Error('No valid updates provided');
        }
        
        const options = {
            new: true,
            upsert: false,
            runValidators: true
        };
        
        if (session) {
            options.session = session;
        }
        
        const updatedTemplate = await Template.findByIdAndUpdate(
            templateId,
            updateObj,
            options
        );
        
        if (!updatedTemplate) {
            logger.warn(`Template not found for count update: ${templateId}`);
            return null;
        }
        
        logger.info(`Updated template ${templateId} counts:`, updates);
        return updatedTemplate;
        
    } catch (error) {
        logger.error(`Failed to update template counts for ${templateId}: ${error.message}`);
        throw error;
    }
}

/**
 * Helper function to validate template interaction request
 * @param {string} uid - User ID
 * @param {string} templateId - Template ID
 * @returns {Object} - Validation result
 */
function validateTemplateInteraction(uid, templateId) {
    const errors = [];
    
    if (!uid || typeof uid !== 'string' || uid.trim().length === 0) {
        errors.push('Valid user ID is required');
    }
    
    if (!templateId || typeof templateId !== 'string' || templateId.trim().length === 0) {
        errors.push('Valid template ID is required');
    }
    
    if (!isValidObjectId(templateId)) {
        errors.push('Invalid template ID format');
    }
    
    return {
        isValid: errors.length === 0,
        errors
    };
}

module.exports = {
    isValidObjectId,
    handleAsyncOperation,
    cleanupSubscriptionData,
    safeUpdateTemplateCounts,
    validateTemplateInteraction
}; 