const express = require('express');
const router = express.Router();
const { 
    checkCustomizationPermissions,
    updateCustomizationPermissions,
    getCustomizationPermissions,
    revokeCustomizationPermissions,
    grantCustomizationPermissions,
    getPermissionHistory,
    validatePermissionRequest
} = require('../../../controllers/templates/customization/permissionsController');
const { validateTemplateId, validatePermissionData } = require('../../../utils/templateValidators');
const { verifyFirebaseToken } = require('../../../middleware/auth');

/**
 * @route GET /api/templates/customization/permissions/:id/check
 * @desc Check if user has customization permissions for template
 * @access Private
 */
router.get('/:id/check',
    verifyFirebaseToken,
    validateTemplateId,
    checkCustomizationPermissions
);

/**
 * @route PUT /api/templates/customization/permissions/:id
 * @desc Update customization permissions for template
 * @access Private
 */
router.put('/:id',
    verifyFirebaseToken,
    validateTemplateId,
    validatePermissionData,
    updateCustomizationPermissions
);

/**
 * @route GET /api/templates/customization/permissions/:id
 * @desc Get customization permissions for template
 * @access Private
 */
router.get('/:id',
    verifyFirebaseToken,
    validateTemplateId,
    getCustomizationPermissions
);

/**
 * @route DELETE /api/templates/customization/permissions/:id
 * @desc Revoke customization permissions for template
 * @access Private
 */
router.delete('/:id',
    verifyFirebaseToken,
    validateTemplateId,
    revokeCustomizationPermissions
);

/**
 * @route POST /api/templates/customization/permissions/:id/grant
 * @desc Grant customization permissions for template
 * @access Private
 */
router.post('/:id/grant',
    verifyFirebaseToken,
    validateTemplateId,
    validatePermissionData,
    grantCustomizationPermissions
);

/**
 * @route GET /api/templates/customization/permissions/:id/history
 * @desc Get permission history for template
 * @access Private
 */
router.get('/:id/history',
    verifyFirebaseToken,
    validateTemplateId,
    getPermissionHistory
);

/**
 * @route POST /api/templates/customization/permissions/validate
 * @desc Validate permission request data
 * @access Private
 */
router.post('/validate',
    verifyFirebaseToken,
    validatePermissionData,
    validatePermissionRequest
);

module.exports = router; 