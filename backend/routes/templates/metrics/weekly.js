const express = require('express');
const router = express.Router();
const { validateTemplateId, validateWeeklyMetricsData } = require('../../../utils/templateValidators');
const weeklyController = require('../../../controllers/templates/metrics/weeklyController');

// Get weekly metrics for a template
router.get('/:templateId', 
    validateTemplateId,
    weeklyController.getWeeklyMetrics
);

// Update weekly metrics for a template
router.put('/:templateId', 
    validateTemplateId,
    validateWeeklyMetricsData,
    weeklyController.updateWeeklyMetrics
);

// Increment weekly counters
router.post('/:templateId/increment', 
    validateTemplateId,
    weeklyController.incrementWeeklyCounters
);

// Reset weekly metrics for a template
router.post('/:templateId/reset', 
    validateTemplateId,
    weeklyController.resetWeeklyMetrics
);

// Batch reset weekly metrics (for weekly cleanup jobs)
router.post('/batch/reset', 
    weeklyController.batchResetWeeklyMetrics
);

// Get weekly leaderboard
router.get('/leaderboard/:type?', 
    weeklyController.getWeeklyLeaderboard
);

// Get weekly trending score
router.get('/:templateId/trending-score', 
    validateTemplateId,
    weeklyController.getWeeklyTrendingScore
);

// Get templates due for weekly reset
router.get('/due-for-reset/:days?', 
    weeklyController.getTemplatesDueForReset
);

// Update weekly score reset timestamp
router.put('/:templateId/reset-timestamp', 
    validateTemplateId,
    weeklyController.updateWeeklyResetTimestamp
);

// Get weekly statistics overview
router.get('/stats/overview', 
    weeklyController.getWeeklyStatsOverview
);

// Batch increment weekly metrics
router.post('/batch/increment', 
    weeklyController.batchIncrementWeeklyMetrics
);

module.exports = router; 