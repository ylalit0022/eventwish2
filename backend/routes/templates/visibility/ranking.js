const express = require('express');
const router = express.Router();
const { 
    updateVisibilityScore,
    updateBoostPriority,
    getVisibilityMetrics,
    calculateVisibilityScore,
    bulkUpdateVisibilityScores
} = require('../../../controllers/templates/visibility/rankingController');
const { validateTemplateId, validateVisibilityScore, validateBoostPriority } = require('../../../utils/templateValidators');
const { verifyFirebaseToken } = require('../../../middleware/auth');

/**
 * @route PUT /api/templates/visibility/ranking/:id/score
 * @desc Update template visibility score
 * @access Private
 */
router.put('/:id/score', 
    verifyFirebaseToken,
    validateTemplateId,
    validateVisibilityScore,
    updateVisibilityScore
);

/**
 * @route PUT /api/templates/visibility/ranking/:id/priority
 * @desc Update template boost priority
 * @access Private
 */
router.put('/:id/priority',
    verifyFirebaseToken,
    validateTemplateId,
    validateBoostPriority,
    updateBoostPriority
);

/**
 * @route GET /api/templates/visibility/ranking/:id/metrics
 * @desc Get visibility metrics for a template
 * @access Private
 */
router.get('/:id/metrics',
    verifyFirebaseToken,
    validateTemplateId,
    getVisibilityMetrics
);

/**
 * @route POST /api/templates/visibility/ranking/:id/calculate
 * @desc Calculate and update visibility score based on engagement metrics
 * @access Private
 */
router.post('/:id/calculate',
    verifyFirebaseToken,
    validateTemplateId,
    calculateVisibilityScore
);

/**
 * @route PUT /api/templates/visibility/ranking/bulk/scores
 * @desc Bulk update visibility scores for multiple templates
 * @access Private
 */
router.put('/bulk/scores',
    verifyFirebaseToken,
    bulkUpdateVisibilityScores
);

module.exports = router; 