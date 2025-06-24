const express = require('express');
const router = express.Router();
const { 
    updateTemplateType,
    getTemplateType,
    getTemplatesByType,
    getAvailableTypes,
    validateTemplateType,
    bulkUpdateTemplateTypes,
    getTypeStatistics
} = require('../../../controllers/templates/visibility/typeController');
const { validateTemplateId, validateTemplateTypeEnum } = require('../../../utils/templateValidators');
const { verifyFirebaseToken } = require('../../../middleware/auth');

/**
 * @route PUT /api/templates/visibility/type/:id
 * @desc Update template type
 * @access Private
 */
router.put('/:id',
    verifyFirebaseToken,
    validateTemplateId,
    validateTemplateTypeEnum,
    updateTemplateType
);

/**
 * @route GET /api/templates/visibility/type/:id
 * @desc Get template type
 * @access Private
 */
router.get('/:id',
    verifyFirebaseToken,
    validateTemplateId,
    getTemplateType
);

/**
 * @route GET /api/templates/visibility/type/filter/:type
 * @desc Get all templates of a specific type
 * @access Private
 */
router.get('/filter/:type',
    verifyFirebaseToken,
    validateTemplateTypeEnum,
    getTemplatesByType
);

/**
 * @route GET /api/templates/visibility/type/available/list
 * @desc Get list of available template types
 * @access Public
 */
router.get('/available/list',
    getAvailableTypes
);

/**
 * @route POST /api/templates/visibility/type/:id/validate
 * @desc Validate template type against content
 * @access Private
 */
router.post('/:id/validate',
    verifyFirebaseToken,
    validateTemplateId,
    validateTemplateType
);

/**
 * @route PUT /api/templates/visibility/type/bulk/update
 * @desc Bulk update template types
 * @access Private
 */
router.put('/bulk/update',
    verifyFirebaseToken,
    bulkUpdateTemplateTypes
);

/**
 * @route GET /api/templates/visibility/type/statistics/summary
 * @desc Get template type statistics
 * @access Private
 */
router.get('/statistics/summary',
    verifyFirebaseToken,
    getTypeStatistics
);

module.exports = router; 