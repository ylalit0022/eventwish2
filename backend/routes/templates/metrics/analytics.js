const express = require('express');
const router = express.Router();
const { validateTemplateId } = require('../../../utils/templateValidators');
const analyticsController = require('../../../controllers/templates/metrics/analyticsController');

// Get analytics data for a template
router.get('/:templateId', 
    validateTemplateId,
    analyticsController.getTemplateAnalytics
);

// Get trending score for a template
router.get('/:templateId/trending-score', 
    validateTemplateId,
    analyticsController.getTrendingScore
);

// Get weekly trending score for a template
router.get('/:templateId/weekly-trending-score', 
    validateTemplateId,
    analyticsController.getWeeklyTrendingScore
);

// Get top trending templates
router.get('/top-trending/:limit?', 
    analyticsController.getTopTrendingTemplates
);

// Get top weekly trending templates
router.get('/top-weekly-trending/:limit?', 
    analyticsController.getTopWeeklyTrendingTemplates
);

// Get analytics comparison between templates
router.post('/compare', 
    analyticsController.compareTemplateAnalytics
);

// Get analytics insights for a template
router.get('/:templateId/insights', 
    validateTemplateId,
    analyticsController.getTemplateInsights
);

// Get category analytics
router.get('/category/:category/overview', 
    analyticsController.getCategoryAnalytics
);

// Get analytics dashboard data
router.get('/dashboard/overview', 
    analyticsController.getAnalyticsDashboard
);

// Get performance metrics over time
router.get('/:templateId/performance-timeline', 
    validateTemplateId,
    analyticsController.getPerformanceTimeline
);

// Get engagement rate analysis
router.get('/:templateId/engagement-rate', 
    validateTemplateId,
    analyticsController.getEngagementRate
);

// Get analytics export data
router.get('/export/:format', 
    analyticsController.exportAnalyticsData
);

module.exports = router; 