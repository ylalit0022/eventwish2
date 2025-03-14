const express = require('express');
const router = express.Router();
const monitoringController = require('../controllers/monitoringController');
const { verifyToken, verifyApiKey } = require('../middleware/authMiddleware');

// Apply authentication middleware to all routes
router.use(verifyToken);

/**
 * @route   GET /api/monitoring/metrics
 * @desc    Get current metrics
 * @access  Private (with token)
 */
router.get('/metrics', monitoringController.getMetrics);

/**
 * @route   GET /api/monitoring/ad-metrics
 * @desc    Get ad-specific metrics
 * @access  Private (with token)
 */
router.get('/ad-metrics', monitoringController.getAdMetrics);

/**
 * @route   POST /api/monitoring/reset
 * @desc    Reset metrics
 * @access  Private (with token)
 */
router.post('/reset', monitoringController.resetMetrics);

module.exports = router; 