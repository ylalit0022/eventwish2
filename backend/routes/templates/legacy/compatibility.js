const express = require('express');
const router = express.Router();
const { 
    updateCategoryIcon,
    getCategoryIcon,
    updateModerationStatus,
    getModerationStatus,
    updateLanguage,
    getLanguage,
    updateRegion,
    getRegion,
    getCompatibilityInfo,
    migrateToNewFormat,
    validateCompatibilityData
} = require('../../../controllers/templates/legacy/compatibilityController');
const { validateTemplateId, validateLegacyFields } = require('../../../utils/templateValidators');
const { verifyFirebaseToken } = require('../../../middleware/auth');

/**
 * @route PUT /api/templates/legacy/compatibility/:id/category-icon
 * @desc Update template category icon (legacy field)
 * @access Private
 */
router.put('/:id/category-icon',
    verifyFirebaseToken,
    validateTemplateId,
    updateCategoryIcon
);

/**
 * @route GET /api/templates/legacy/compatibility/:id/category-icon
 * @desc Get template category icon
 * @access Public
 */
router.get('/:id/category-icon',
    validateTemplateId,
    getCategoryIcon
);

/**
 * @route PUT /api/templates/legacy/compatibility/:id/moderation-status
 * @desc Update template moderation status
 * @access Private
 */
router.put('/:id/moderation-status',
    verifyFirebaseToken,
    validateTemplateId,
    updateModerationStatus
);

/**
 * @route GET /api/templates/legacy/compatibility/:id/moderation-status
 * @desc Get template moderation status
 * @access Private
 */
router.get('/:id/moderation-status',
    verifyFirebaseToken,
    validateTemplateId,
    getModerationStatus
);

/**
 * @route PUT /api/templates/legacy/compatibility/:id/language
 * @desc Update template language
 * @access Private
 */
router.put('/:id/language',
    verifyFirebaseToken,
    validateTemplateId,
    updateLanguage
);

/**
 * @route GET /api/templates/legacy/compatibility/:id/language
 * @desc Get template language
 * @access Public
 */
router.get('/:id/language',
    validateTemplateId,
    getLanguage
);

/**
 * @route PUT /api/templates/legacy/compatibility/:id/region
 * @desc Update template region
 * @access Private
 */
router.put('/:id/region',
    verifyFirebaseToken,
    validateTemplateId,
    updateRegion
);

/**
 * @route GET /api/templates/legacy/compatibility/:id/region
 * @desc Get template region
 * @access Public
 */
router.get('/:id/region',
    validateTemplateId,
    getRegion
);

/**
 * @route GET /api/templates/legacy/compatibility/:id/info
 * @desc Get all compatibility information for template
 * @access Public
 */
router.get('/:id/info',
    validateTemplateId,
    getCompatibilityInfo
);

/**
 * @route POST /api/templates/legacy/compatibility/:id/migrate
 * @desc Migrate template to new format
 * @access Private
 */
router.post('/:id/migrate',
    verifyFirebaseToken,
    validateTemplateId,
    migrateToNewFormat
);

/**
 * @route POST /api/templates/legacy/compatibility/validate
 * @desc Validate compatibility data
 * @access Public
 */
router.post('/validate',
    validateLegacyFields,
    validateCompatibilityData
);

module.exports = router; 