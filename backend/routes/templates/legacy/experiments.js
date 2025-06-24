const express = require('express');
const router = express.Router();
const { 
    addExperimentTag,
    removeExperimentTag,
    getExperimentTags,
    updatePerformanceLog,
    getPerformanceLog,
    analyzeExperimentResults,
    getExperimentStats,
    createExperiment,
    endExperiment,
    getActiveExperiments
} = require('../../../controllers/templates/legacy/experimentsController');
const { validateTemplateId, validateExperimentData, validatePerformanceData } = require('../../../utils/templateValidators');
const { verifyFirebaseToken } = require('../../../middleware/auth');

/**
 * @route POST /api/templates/legacy/experiments/:id/tags
 * @desc Add experiment tag to template
 * @access Private
 */
router.post('/:id/tags',
    verifyFirebaseToken,
    validateTemplateId,
    validateExperimentData,
    addExperimentTag
);

/**
 * @route DELETE /api/templates/legacy/experiments/:id/tags/:tag
 * @desc Remove experiment tag from template
 * @access Private
 */
router.delete('/:id/tags/:tag',
    verifyFirebaseToken,
    validateTemplateId,
    removeExperimentTag
);

/**
 * @route GET /api/templates/legacy/experiments/:id/tags
 * @desc Get experiment tags for template
 * @access Public
 */
router.get('/:id/tags',
    validateTemplateId,
    getExperimentTags
);

/**
 * @route PUT /api/templates/legacy/experiments/:id/performance-log
 * @desc Update performance log for template
 * @access Private
 */
router.put('/:id/performance-log',
    verifyFirebaseToken,
    validateTemplateId,
    validatePerformanceData,
    updatePerformanceLog
);

/**
 * @route GET /api/templates/legacy/experiments/:id/performance-log
 * @desc Get performance log for template
 * @access Private
 */
router.get('/:id/performance-log',
    verifyFirebaseToken,
    validateTemplateId,
    getPerformanceLog
);

/**
 * @route POST /api/templates/legacy/experiments/analyze
 * @desc Analyze experiment results
 * @access Private
 */
router.post('/analyze',
    verifyFirebaseToken,
    validateExperimentData,
    analyzeExperimentResults
);

/**
 * @route GET /api/templates/legacy/experiments/stats
 * @desc Get experiment statistics
 * @access Private
 */
router.get('/stats',
    verifyFirebaseToken,
    getExperimentStats
);

/**
 * @route POST /api/templates/legacy/experiments/create
 * @desc Create new experiment
 * @access Private
 */
router.post('/create',
    verifyFirebaseToken,
    validateExperimentData,
    createExperiment
);

/**
 * @route POST /api/templates/legacy/experiments/:experimentId/end
 * @desc End experiment
 * @access Private
 */
router.post('/:experimentId/end',
    verifyFirebaseToken,
    endExperiment
);

/**
 * @route GET /api/templates/legacy/experiments/active
 * @desc Get active experiments
 * @access Private
 */
router.get('/active',
    verifyFirebaseToken,
    getActiveExperiments
);

module.exports = router; 