const express = require('express');
const router = express.Router();
const { validateTemplateId, validateEngagementData, validateBatchEngagementData } = require('../../../utils/templateValidators');
const engagementController = require('../../../controllers/templates/metrics/engagementController');

// Get engagement metrics for a template
router.get('/:templateId', 
    validateTemplateId,
    engagementController.getEngagementMetrics
);

// Update engagement metrics for a template
router.put('/:templateId', 
    validateTemplateId,
    validateEngagementData,
    engagementController.updateEngagementMetrics
);

// Increment specific engagement counters
router.post('/:templateId/increment', 
    validateTemplateId,
    engagementController.incrementEngagementCounters
);

// Batch increment engagement for multiple templates
router.post('/batch/increment', 
    validateBatchEngagementData,
    engagementController.batchIncrementEngagement
);

// Get engagement leaderboard
router.get('/leaderboard/:type?', 
    engagementController.getEngagementLeaderboard
);

// Get engagement statistics
router.get('/stats/overview', 
    engagementController.getEngagementStats
);

// Reset engagement metrics for a template
router.post('/:templateId/reset', 
    validateTemplateId,
    engagementController.resetEngagementMetrics
);

// Get templates by engagement threshold
router.get('/threshold/:metric/:value', 
    engagementController.getTemplatesByEngagementThreshold
);

// Update rating for a template
router.post('/:templateId/rating', 
    validateTemplateId,
    engagementController.updateTemplateRating
);

// Get engagement history/trends
router.get('/:templateId/history', 
    validateTemplateId,
    engagementController.getEngagementHistory
);

// Bulk update engagement metrics
router.put('/bulk/update', 
    engagementController.bulkUpdateEngagementMetrics
);

module.exports = router; 