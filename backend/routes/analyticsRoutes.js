const express = require('express');
const router = express.Router();
const analyticsController = require('../controllers/analyticsController');
const { verifyToken, verifyApiKey } = require('../middleware/authMiddleware');

// Apply authentication middleware to all routes
router.use(verifyToken);

/**
 * @route   GET /api/analytics/summary
 * @desc    Get summary analytics for all ads
 * @access  Private (with token)
 */
router.get('/summary', analyticsController.getSummaryAnalytics);

/**
 * @route   GET /api/analytics/ad/:adId
 * @desc    Get analytics data for a specific ad
 * @access  Private (with token)
 */
router.get('/ad/:adId', analyticsController.getAdAnalytics);

/**
 * @route   POST /api/analytics/revenue/:adId
 * @desc    Track ad revenue
 * @access  Private (with token)
 */
router.post('/revenue/:adId', analyticsController.trackRevenue);

/**
 * @route   POST /api/analytics/cache/invalidate/:adId
 * @desc    Invalidate analytics cache for a specific ad
 * @access  Private (with token)
 */
router.post('/cache/invalidate/:adId', analyticsController.invalidateCache);

/**
 * @route   POST /api/analytics/cache/invalidate-all
 * @desc    Invalidate all analytics caches
 * @access  Private (with token)
 */
router.post('/cache/invalidate-all', analyticsController.invalidateAllCaches);

module.exports = router; 