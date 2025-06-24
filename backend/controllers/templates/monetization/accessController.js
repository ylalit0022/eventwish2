const Template = require('../../../models/Template');
const logger = require('../../../utils/logger');
const { 
    formatErrorResponse, 
    formatSuccessResponse,
    isValidObjectId
} = require('../../../utils/templateHelpers');

/**
 * Get template access control settings
 * @route GET /api/templates/monetization/access/:templateId
 * @access Public
 */
const getTemplateAccess = async (req, res) => {
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

        logger.info(`Fetching template access settings: ${templateId}`);

        const template = await Template.findById(templateId)
            .select('title isPremium premiumAccess status moderationStatus updatedAt');

        if (!template) {
            return res.status(404).json(formatErrorResponse(
                'NOT_FOUND',
                'Template not found',
                'templateId',
                templateId
            ));
        }

        logger.info(`Template access settings retrieved: ${templateId}`);

        res.json(formatSuccessResponse({
            templateId: template._id,
            title: template.title,
            accessControl: {
                isPremium: template.isPremium || false,
                premiumAccess: template.premiumAccess || false,
                status: template.status,
                moderationStatus: template.moderationStatus || 'pending'
            },
            updatedAt: template.updatedAt
        }));

    } catch (error) {
        logger.error('Error getting template access settings:', error);
        res.status(500).json(formatErrorResponse(
            'SERVER_ERROR',
            'Internal server error while retrieving access settings',
            null,
            null,
            error.message
        ));
    }
};

/**
 * Update template access control settings
 * @route PUT /api/templates/monetization/access/:templateId
 * @access Admin
 */
const updateTemplateAccess = async (req, res) => {
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

        logger.info(`Updating template access settings: ${templateId}`, updateData);

        // Validate update fields
        const allowedFields = ['isPremium', 'premiumAccess'];
        const updateFields = Object.keys(updateData).filter(key => allowedFields.includes(key));
        
        if (updateFields.length === 0) {
            return res.status(400).json(formatErrorResponse(
                'VALIDATION_ERROR',
                'No valid access fields provided for update',
                null,
                null,
                { allowedFields }
            ));
        }

        // Prepare update object
        const updateObject = {};
        updateFields.forEach(field => {
            updateObject[field] = Boolean(updateData[field]);
        });

        updateObject.updatedAt = new Date();

        const updatedTemplate = await Template.findByIdAndUpdate(
            templateId,
            updateObject,
            { 
                new: true, 
                runValidators: true,
                select: 'title isPremium premiumAccess status updatedAt'
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

        logger.info(`Template access settings updated: ${templateId}`, { updatedFields: updateFields });

        res.json(formatSuccessResponse({
            templateId: updatedTemplate._id,
            title: updatedTemplate.title,
            accessControl: {
                isPremium: updatedTemplate.isPremium || false,
                premiumAccess: updatedTemplate.premiumAccess || false,
                status: updatedTemplate.status
            },
            updatedAt: updatedTemplate.updatedAt
        }, updateFields));

    } catch (error) {
        logger.error('Error updating template access settings:', error);

        if (error.name === 'ValidationError') {
            const validationErrors = Object.keys(error.errors).map(key => ({
                field: key,
                message: error.errors[key].message,
                value: error.errors[key].value
            }));

            return res.status(400).json(formatErrorResponse(
                'VALIDATION_ERROR',
                'Access settings validation failed',
                null,
                null,
                { validationErrors }
            ));
        }

        res.status(500).json(formatErrorResponse(
            'SERVER_ERROR',
            'Internal server error while updating access settings',
            null,
            null,
            error.message
        ));
    }
};

/**
 * Toggle template premium status
 * @route POST /api/templates/monetization/access/:templateId/toggle-premium
 * @access Admin
 */
const togglePremiumStatus = async (req, res) => {
    try {
        const { templateId } = req.params;
        const { isPremium } = req.body;

        if (!isValidObjectId(templateId)) {
            return res.status(400).json(formatErrorResponse(
                'VALIDATION_ERROR',
                'Invalid template ID format',
                'templateId',
                templateId
            ));
        }

        if (typeof isPremium !== 'boolean') {
            return res.status(400).json(formatErrorResponse(
                'VALIDATION_ERROR',
                'isPremium must be a boolean value',
                'isPremium',
                isPremium
            ));
        }

        logger.info(`Toggling premium status for template: ${templateId}`, { isPremium });

        const updatedTemplate = await Template.findByIdAndUpdate(
            templateId,
            { 
                isPremium: isPremium,
                updatedAt: new Date()
            },
            { 
                new: true,
                select: 'title isPremium premiumAccess status updatedAt'
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

        logger.info(`Template premium status toggled: ${templateId}`, { 
            newStatus: updatedTemplate.isPremium 
        });

        res.json(formatSuccessResponse({
            templateId: templateId,
            title: updatedTemplate.title,
            accessControl: {
                isPremium: updatedTemplate.isPremium,
                premiumAccess: updatedTemplate.premiumAccess || false,
                status: updatedTemplate.status
            },
            updatedAt: updatedTemplate.updatedAt,
            message: `Template ${isPremium ? 'set to premium' : 'made free'} successfully`
        }, ['isPremium']));

    } catch (error) {
        logger.error('Error toggling template premium status:', error);
        res.status(500).json(formatErrorResponse(
            'SERVER_ERROR',
            'Internal server error while toggling premium status',
            null,
            null,
            error.message
        ));
    }
};

/**
 * Validate user access permissions for template
 * @route POST /api/templates/monetization/access/:templateId/validate
 * @access Public
 */
const validateAccessPermissions = async (req, res) => {
    try {
        const { templateId } = req.params;
        const { userId, userSubscriptionType, userPremiumStatus } = req.body;

        if (!isValidObjectId(templateId)) {
            return res.status(400).json(formatErrorResponse(
                'VALIDATION_ERROR',
                'Invalid template ID format',
                'templateId',
                templateId
            ));
        }

        logger.info('Validating template access permissions:', { 
            templateId, 
            userId, 
            userSubscriptionType, 
            userPremiumStatus 
        });

        const template = await Template.findById(templateId)
            .select('title isPremium premiumAccess status moderationStatus');

        if (!template) {
            return res.status(404).json(formatErrorResponse(
                'NOT_FOUND',
                'Template not found',
                'templateId',
                templateId
            ));
        }

        // Check if template is active and approved
        if (!template.status) {
            return res.json(formatSuccessResponse({
                templateId: templateId,
                title: template.title,
                hasAccess: false,
                accessLevel: 'denied',
                reason: 'Template is not active',
                requiresSubscription: false
            }));
        }

        if (template.moderationStatus === 'rejected') {
            return res.json(formatSuccessResponse({
                templateId: templateId,
                title: template.title,
                hasAccess: false,
                accessLevel: 'denied',
                reason: 'Template has been rejected by moderation',
                requiresSubscription: false
            }));
        }

        // Check premium access logic
        let hasAccess = true;
        let accessLevel = 'granted';
        let reason = 'Access granted';
        let requiresSubscription = false;

        if (template.isPremium) {
            // Template is premium, check user's subscription status
            if (!userId) {
                hasAccess = false;
                accessLevel = 'restricted';
                reason = 'Premium template requires user authentication';
                requiresSubscription = true;
            } else if (!userPremiumStatus && userSubscriptionType !== 'premium') {
                hasAccess = false;
                accessLevel = 'restricted';
                reason = 'Premium template requires active subscription';
                requiresSubscription = true;
            } else if (template.premiumAccess && !userPremiumStatus) {
                hasAccess = false;
                accessLevel = 'restricted';
                reason = 'Template requires premium access privileges';
                requiresSubscription = true;
            }
        }

        logger.info(`Template access validation completed: ${templateId}`, { 
            hasAccess, 
            accessLevel, 
            reason 
        });

        res.json(formatSuccessResponse({
            templateId: templateId,
            title: template.title,
            isPremium: template.isPremium,
            premiumAccess: template.premiumAccess,
            hasAccess: hasAccess,
            accessLevel: accessLevel,
            reason: reason,
            requiresSubscription: requiresSubscription,
            userInfo: {
                userId: userId || null,
                subscriptionType: userSubscriptionType || 'free',
                premiumStatus: userPremiumStatus || false
            }
        }));

    } catch (error) {
        logger.error('Error validating template access permissions:', error);
        res.status(500).json(formatErrorResponse(
            'SERVER_ERROR',
            'Internal server error during access validation',
            null,
            null,
            error.message
        ));
    }
};

/**
 * Bulk update access settings for multiple templates
 * @route POST /api/templates/monetization/access/bulk-update
 * @access Admin
 */
const bulkUpdateAccess = async (req, res) => {
    try {
        const { templateIds, updates } = req.body;

        if (!Array.isArray(templateIds) || templateIds.length === 0) {
            return res.status(400).json(formatErrorResponse(
                'VALIDATION_ERROR',
                'templateIds must be a non-empty array',
                'templateIds',
                templateIds
            ));
        }

        // Validate all template IDs
        const invalidIds = templateIds.filter(id => !isValidObjectId(id));
        if (invalidIds.length > 0) {
            return res.status(400).json(formatErrorResponse(
                'VALIDATION_ERROR',
                'Invalid template ID format in array',
                'templateIds',
                invalidIds
            ));
        }

        if (!updates || typeof updates !== 'object') {
            return res.status(400).json(formatErrorResponse(
                'VALIDATION_ERROR',
                'Updates object is required',
                'updates',
                updates
            ));
        }

        logger.info('Bulk updating template access settings:', { 
            templateCount: templateIds.length, 
            updates 
        });

        // Validate update fields
        const allowedFields = ['isPremium', 'premiumAccess'];
        const updateFields = Object.keys(updates).filter(key => allowedFields.includes(key));
        
        if (updateFields.length === 0) {
            return res.status(400).json(formatErrorResponse(
                'VALIDATION_ERROR',
                'No valid access fields provided for update',
                'updates',
                updates,
                { allowedFields }
            ));
        }

        // Prepare update object
        const updateObject = { updatedAt: new Date() };
        updateFields.forEach(field => {
            updateObject[field] = Boolean(updates[field]);
        });

        const updateResult = await Template.updateMany(
            { _id: { $in: templateIds } },
            updateObject
        );

        logger.info(`Bulk access update completed: ${updateResult.modifiedCount} templates updated`);

        res.json(formatSuccessResponse({
            templateIds: templateIds,
            updatedCount: updateResult.modifiedCount,
            matchedCount: updateResult.matchedCount,
            appliedUpdates: updateObject,
            message: `${updateResult.modifiedCount} templates updated successfully`
        }, updateFields));

    } catch (error) {
        logger.error('Error during bulk access update:', error);
        res.status(500).json(formatErrorResponse(
            'SERVER_ERROR',
            'Internal server error during bulk access update',
            null,
            null,
            error.message
        ));
    }
};

module.exports = {
    getTemplateAccess,
    updateTemplateAccess,
    togglePremiumStatus,
    validateAccessPermissions,
    bulkUpdateAccess
};
