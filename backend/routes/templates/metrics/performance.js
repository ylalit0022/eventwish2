const express = require('express');
const router = express.Router();
const { validateTemplateId, validatePerformanceData } = require('../../../utils/templateValidators');
const performanceController = require('../../../controllers/templates/metrics/performanceController');

// Get performance metrics for a template
router.get('/:templateId', 
    validateTemplateId,
    performanceController.getPerformanceMetrics
);

// Update performance log for a template
router.put('/:templateId', 
    validateTemplateId,
    validatePerformanceData,
    performanceController.updatePerformanceLog
);

// Record template usage (updates lastUsedAt and usage counters)
router.post('/:templateId/usage', 
    validateTemplateId,
    performanceController.recordTemplateUsage
);

// Get performance summary
router.get('/:templateId/summary', 
    validateTemplateId,
    performanceController.getPerformanceSummary
);

// Batch update performance logs
router.post('/batch/update', 
    performanceController.batchUpdatePerformanceLogs
);

// Get performance leaderboard
router.get('/leaderboard/:metric/:period?', 
    performanceController.getPerformanceLeaderboard
);

// Reset performance counters
router.post('/:templateId/reset/:period', 
    validateTemplateId,
    performanceController.resetPerformanceCounters
);

// Get performance trends
router.get('/:templateId/trends/:period?', 
    validateTemplateId,
    performanceController.getPerformanceTrends
);

// Get low performing templates
router.get('/low-performing/:threshold?', 
    performanceController.getLowPerformingTemplates
);

// Update last used timestamp
router.put('/:templateId/last-used', 
    validateTemplateId,
    performanceController.updateLastUsedTimestamp
);

// Get performance statistics
router.get('/stats/overview', 
    performanceController.getPerformanceStats
);

module.exports = router; 