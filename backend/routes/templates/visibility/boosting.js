const express = require('express');
const router = express.Router();
const { 
    boostTemplate,
    unboostTemplate,
    getBoostStatus,
    getBoostedTemplates,
    scheduleBoost,
    cancelScheduledBoost,
    getBoostHistory
} = require('../../../controllers/templates/visibility/boostingController');
const { validateTemplateId, validateBoostDuration, validateBoostSchedule } = require('../../../utils/templateValidators');
const { verifyFirebaseToken } = require('../../../middleware/auth');

/**
 * @route POST /api/templates/visibility/boosting/:id/boost
 * @desc Boost a template (set boost priority and lastBoostedAt)
 * @access Private
 */
router.post('/:id/boost',
    verifyFirebaseToken,
    validateTemplateId,
    validateBoostDuration,
    boostTemplate
);

/**
 * @route DELETE /api/templates/visibility/boosting/:id/boost
 * @desc Remove boost from a template
 * @access Private
 */
router.delete('/:id/boost',
    verifyFirebaseToken,
    validateTemplateId,
    unboostTemplate
);

/**
 * @route GET /api/templates/visibility/boosting/:id/status
 * @desc Get boost status for a template
 * @access Private
 */
router.get('/:id/status',
    verifyFirebaseToken,
    validateTemplateId,
    getBoostStatus
);

/**
 * @route GET /api/templates/visibility/boosting/active
 * @desc Get all currently boosted templates
 * @access Private
 */
router.get('/active',
    verifyFirebaseToken,
    getBoostedTemplates
);

/**
 * @route POST /api/templates/visibility/boosting/:id/schedule
 * @desc Schedule a boost for future activation
 * @access Private
 */
router.post('/:id/schedule',
    verifyFirebaseToken,
    validateTemplateId,
    validateBoostSchedule,
    scheduleBoost
);

/**
 * @route DELETE /api/templates/visibility/boosting/:id/schedule
 * @desc Cancel a scheduled boost
 * @access Private
 */
router.delete('/:id/schedule',
    verifyFirebaseToken,
    validateTemplateId,
    cancelScheduledBoost
);

/**
 * @route GET /api/templates/visibility/boosting/:id/history
 * @desc Get boost history for a template
 * @access Private
 */
router.get('/:id/history',
    verifyFirebaseToken,
    validateTemplateId,
    getBoostHistory
);

module.exports = router; 