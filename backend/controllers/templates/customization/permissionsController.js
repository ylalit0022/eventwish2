const Template = require('../../../models/Template');
const logger = require('../../../utils/logger');
const { handleAsyncOperation, createSuccessResponse, createErrorResponse } = require('../../../utils/templateHelpers');

/**
 * Check if user has customization permissions for template
 */
const checkCustomizationPermissions = async (req, res) => {
    await handleAsyncOperation(res, async () => {
        const { id } = req.params;
        const { uid } = req.user; // From Firebase token

        const template = await Template.findById(id).select('creatorUid isPremium customizationOptions');

        if (!template) {
            return createErrorResponse('Template not found', 'TEMPLATE_NOT_FOUND', 'id', id);
        }

        // Check if user is the creator
        const isCreator = template.creatorUid === uid;
        
        // Check if template allows customization
        const customizationAllowed = template.customizationOptions && 
            Object.values(template.customizationOptions).some(option => option === true);

        // Check if user has premium access for premium templates
        const hasPremiumAccess = !template.isPremium || req.user.isPremium;

        const permissions = {
            canCustomize: isCreator || (customizationAllowed && hasPremiumAccess),
            isCreator,
            customizationAllowed,
            hasPremiumAccess,
            requiresPremium: template.isPremium,
            availableOptions: template.customizationOptions || {}
        };

        return createSuccessResponse({
            templateId: id,
            userId: uid,
            permissions
        });
    });
};

/**
 * Update customization permissions for template
 */
const updateCustomizationPermissions = async (req, res) => {
    await handleAsyncOperation(res, async () => {
        const { id } = req.params;
        const { uid } = req.user;
        const { permissions } = req.body;

        const template = await Template.findById(id);

        if (!template) {
            return createErrorResponse('Template not found', 'TEMPLATE_NOT_FOUND', 'id', id);
        }

        // Only creator can update permissions
        if (template.creatorUid !== uid) {
            return createErrorResponse(
                'Only template creator can update permissions',
                'PERMISSION_DENIED',
                'creatorUid',
                uid
            );
        }

        // Update template with new permissions
        const updatedTemplate = await Template.findByIdAndUpdate(
            id,
            { 
                customizationOptions: {
                    ...template.customizationOptions,
                    ...permissions
                },
                updatedAt: new Date()
            },
            { new: true, runValidators: true }
        );

        logger.info(`Updated customization permissions for template ${id}`, {
            templateId: id,
            creatorUid: uid,
            permissions
        });

        return createSuccessResponse({
            template: {
                id: updatedTemplate._id,
                customizationOptions: updatedTemplate.customizationOptions,
                updatedAt: updatedTemplate.updatedAt
            }
        }, 'Customization permissions updated successfully');
    });
};

/**
 * Get customization permissions for template
 */
const getCustomizationPermissions = async (req, res) => {
    await handleAsyncOperation(res, async () => {
        const { id } = req.params;
        const { uid } = req.user;

        const template = await Template.findById(id).select('creatorUid customizationOptions isPremium');

        if (!template) {
            return createErrorResponse('Template not found', 'TEMPLATE_NOT_FOUND', 'id', id);
        }

        // Only creator or authorized users can view detailed permissions
        const isCreator = template.creatorUid === uid;
        if (!isCreator) {
            return createErrorResponse(
                'Insufficient permissions to view template permissions',
                'PERMISSION_DENIED',
                'creatorUid',
                uid
            );
        }

        return createSuccessResponse({
            templateId: id,
            permissions: {
                customizationOptions: template.customizationOptions || {},
                isPremium: template.isPremium,
                creatorUid: template.creatorUid
            }
        });
    });
};

/**
 * Revoke customization permissions for template
 */
const revokeCustomizationPermissions = async (req, res) => {
    await handleAsyncOperation(res, async () => {
        const { id } = req.params;
        const { uid } = req.user;

        const template = await Template.findById(id);

        if (!template) {
            return createErrorResponse('Template not found', 'TEMPLATE_NOT_FOUND', 'id', id);
        }

        // Only creator can revoke permissions
        if (template.creatorUid !== uid) {
            return createErrorResponse(
                'Only template creator can revoke permissions',
                'PERMISSION_DENIED',
                'creatorUid',
                uid
            );
        }

        // Revoke all customization permissions
        const revokedPermissions = {
            allowTextEdit: false,
            allowColorChange: false,
            allowImageUpload: false,
            allowFontChange: false,
            allowLayoutChange: false
        };

        const updatedTemplate = await Template.findByIdAndUpdate(
            id,
            { 
                customizationOptions: revokedPermissions,
                updatedAt: new Date()
            },
            { new: true, runValidators: true }
        );

        logger.info(`Revoked customization permissions for template ${id}`, {
            templateId: id,
            creatorUid: uid
        });

        return createSuccessResponse({
            template: {
                id: updatedTemplate._id,
                customizationOptions: updatedTemplate.customizationOptions,
                updatedAt: updatedTemplate.updatedAt
            }
        }, 'All customization permissions revoked');
    });
};

/**
 * Grant customization permissions for template
 */
const grantCustomizationPermissions = async (req, res) => {
    await handleAsyncOperation(res, async () => {
        const { id } = req.params;
        const { uid } = req.user;
        const { permissions } = req.body;

        const template = await Template.findById(id);

        if (!template) {
            return createErrorResponse('Template not found', 'TEMPLATE_NOT_FOUND', 'id', id);
        }

        // Only creator can grant permissions
        if (template.creatorUid !== uid) {
            return createErrorResponse(
                'Only template creator can grant permissions',
                'PERMISSION_DENIED',
                'creatorUid',
                uid
            );
        }

        // Grant specified permissions
        const updatedPermissions = {
            ...template.customizationOptions,
            ...permissions
        };

        const updatedTemplate = await Template.findByIdAndUpdate(
            id,
            { 
                customizationOptions: updatedPermissions,
                updatedAt: new Date()
            },
            { new: true, runValidators: true }
        );

        logger.info(`Granted customization permissions for template ${id}`, {
            templateId: id,
            creatorUid: uid,
            grantedPermissions: permissions
        });

        return createSuccessResponse({
            template: {
                id: updatedTemplate._id,
                customizationOptions: updatedTemplate.customizationOptions,
                updatedAt: updatedTemplate.updatedAt
            }
        }, 'Customization permissions granted successfully');
    });
};

/**
 * Get permission history for template
 */
const getPermissionHistory = async (req, res) => {
    await handleAsyncOperation(res, async () => {
        const { id } = req.params;
        const { uid } = req.user;

        const template = await Template.findById(id).select('creatorUid customizationOptions updatedAt createdAt');

        if (!template) {
            return createErrorResponse('Template not found', 'TEMPLATE_NOT_FOUND', 'id', id);
        }

        // Only creator can view permission history
        if (template.creatorUid !== uid) {
            return createErrorResponse(
                'Only template creator can view permission history',
                'PERMISSION_DENIED',
                'creatorUid',
                uid
            );
        }

        // For now, return current state and timestamps
        // In a full implementation, you'd track permission changes in a separate collection
        const history = [
            {
                timestamp: template.createdAt,
                action: 'TEMPLATE_CREATED',
                permissions: template.customizationOptions || {},
                userId: template.creatorUid
            },
            {
                timestamp: template.updatedAt,
                action: 'PERMISSIONS_UPDATED',
                permissions: template.customizationOptions || {},
                userId: template.creatorUid
            }
        ];

        return createSuccessResponse({
            templateId: id,
            history,
            totalEvents: history.length
        });
    });
};

/**
 * Validate permission request data
 */
const validatePermissionRequest = async (req, res) => {
    await handleAsyncOperation(res, async () => {
        const { permissions } = req.body;
        const errors = [];
        const warnings = [];

        if (!permissions || typeof permissions !== 'object') {
            errors.push('Permissions object is required');
        } else {
            // Validate permission structure
            const validPermissions = ['allowTextEdit', 'allowColorChange', 'allowImageUpload', 'allowFontChange', 'allowLayoutChange'];
            
            Object.keys(permissions).forEach(key => {
                if (!validPermissions.includes(key)) {
                    errors.push(`Invalid permission: ${key}`);
                } else if (typeof permissions[key] !== 'boolean') {
                    errors.push(`Permission '${key}' must be a boolean value`);
                }
            });

            // Check for security warnings
            if (permissions.allowLayoutChange === true) {
                warnings.push('Allowing layout changes may significantly alter template appearance');
            }
            
            if (permissions.allowImageUpload === true) {
                warnings.push('Allowing image uploads requires proper content moderation');
            }
        }

        return createSuccessResponse({
            isValid: errors.length === 0,
            errors,
            warnings,
            validatedPermissions: errors.length === 0 ? permissions : null
        });
    });
};

module.exports = {
    checkCustomizationPermissions,
    updateCustomizationPermissions,
    getCustomizationPermissions,
    revokeCustomizationPermissions,
    grantCustomizationPermissions,
    getPermissionHistory,
    validatePermissionRequest
}; 