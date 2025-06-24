const express = require('express');
const router = express.Router();
const { 
    updateCustomizationOptions,
    getCustomizationOptions,
    resetCustomizationOptions,
    updateSpecificOption,
    getAvailableOptions,
    validateCustomizationOptions,
    bulkUpdateCustomizationOptions
} = require('../../../controllers/templates/customization/optionsController');
const { validateTemplateId, validateCustomizationOptionsData } = require('../../../utils/templateValidators');
const { verifyFirebaseToken } = require('../../../middleware/auth');

/**
 * @route PUT /api/templates/customization/options/:id
 * @desc Update template customization options
 * @access Private
 */
router.put('/:id',
    verifyFirebaseToken,
    validateTemplateId,
    validateCustomizationOptionsData,
    updateCustomizationOptions
);

/**
 * @route GET /api/templates/customization/options/:id
 * @desc Get template customization options
 * @access Public
 */
router.get('/:id',
    validateTemplateId,
    getCustomizationOptions
);

/**
 * @route POST /api/templates/customization/options/:id/reset
 * @desc Reset customization options to default values
 * @access Private
 */
router.post('/:id/reset',
    verifyFirebaseToken,
    validateTemplateId,
    resetCustomizationOptions
);

/**
 * @route PUT /api/templates/customization/options/:id/:optionName
 * @desc Update a specific customization option
 * @access Private
 */
router.put('/:id/:optionName',
    verifyFirebaseToken,
    validateTemplateId,
    updateSpecificOption
);

/**
 * @route GET /api/templates/customization/options/available
 * @desc Get list of available customization options
 * @access Public
 */
router.get('/available',
    getAvailableOptions
);

/**
 * @route POST /api/templates/customization/options/validate
 * @desc Validate customization options data
 * @access Public
 */
router.post('/validate',
    validateCustomizationOptionsData,
    validateCustomizationOptions
);

/**
 * @route PUT /api/templates/customization/options/bulk
 * @desc Bulk update customization options for multiple templates
 * @access Private
 */
router.put('/bulk',
    verifyFirebaseToken,
    validateCustomizationOptionsData,
    bulkUpdateCustomizationOptions
);

module.exports = router; 