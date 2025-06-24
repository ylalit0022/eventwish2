const express = require('express');
const router = express.Router();
const { 
    addLegacyReference,
    removeLegacyReference,
    getLegacyReferences,
    updateLegacyReference,
    validateLegacyReference,
    migrateLegacyReferences,
    cleanupOrphanedReferences,
    getLegacyReferenceStats
} = require('../../../controllers/templates/legacy/referencesController');
const { validateTemplateId, validateReferenceData } = require('../../../utils/templateValidators');
const { verifyFirebaseToken } = require('../../../middleware/auth');

/**
 * @route POST /api/templates/legacy/references/:id
 * @desc Add legacy reference to template
 * @access Private
 */
router.post('/:id',
    verifyFirebaseToken,
    validateTemplateId,
    validateReferenceData,
    addLegacyReference
);

/**
 * @route DELETE /api/templates/legacy/references/:id/:referenceId
 * @desc Remove legacy reference from template
 * @access Private
 */
router.delete('/:id/:referenceId',
    verifyFirebaseToken,
    validateTemplateId,
    removeLegacyReference
);

/**
 * @route GET /api/templates/legacy/references/:id
 * @desc Get all legacy references for template
 * @access Public
 */
router.get('/:id',
    validateTemplateId,
    getLegacyReferences
);

/**
 * @route PUT /api/templates/legacy/references/:id/:referenceId
 * @desc Update legacy reference
 * @access Private
 */
router.put('/:id/:referenceId',
    verifyFirebaseToken,
    validateTemplateId,
    validateReferenceData,
    updateLegacyReference
);

/**
 * @route POST /api/templates/legacy/references/validate
 * @desc Validate legacy reference data
 * @access Public
 */
router.post('/validate',
    validateReferenceData,
    validateLegacyReference
);

/**
 * @route POST /api/templates/legacy/references/:id/migrate
 * @desc Migrate legacy references to new format
 * @access Private
 */
router.post('/:id/migrate',
    verifyFirebaseToken,
    validateTemplateId,
    migrateLegacyReferences
);

/**
 * @route DELETE /api/templates/legacy/references/cleanup/orphaned
 * @desc Cleanup orphaned legacy references
 * @access Private
 */
router.delete('/cleanup/orphaned',
    verifyFirebaseToken,
    cleanupOrphanedReferences
);

/**
 * @route GET /api/templates/legacy/references/stats
 * @desc Get legacy reference statistics
 * @access Private
 */
router.get('/stats',
    verifyFirebaseToken,
    getLegacyReferenceStats
);

module.exports = router; 