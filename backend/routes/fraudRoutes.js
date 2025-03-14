/**
 * Fraud Routes
 * 
 * This module defines the routes for fraud detection and prevention.
 */

const express = require('express');
const router = express.Router();
const fraudController = require('../controllers/fraudController');
const { authenticateApiKey } = require('../middleware/authMiddleware');

/**
 * @route POST /api/fraud/check
 * @description Check if a click is potentially fraudulent
 * @access Public (with app signature verification)
 */
router.post('/check', fraudController.checkFraud);

/**
 * @route GET /api/fraud/statistics
 * @description Get fraud detection statistics
 * @access Admin only
 */
router.get('/statistics', authenticateApiKey, fraudController.getStatistics);

/**
 * @route GET /api/fraud/thresholds
 * @description Get fraud detection thresholds
 * @access Admin only
 */
router.get('/thresholds', authenticateApiKey, fraudController.getThresholds);

/**
 * @route PUT /api/fraud/thresholds
 * @description Update fraud detection thresholds
 * @access Admin only
 */
router.put('/thresholds', authenticateApiKey, fraudController.updateThresholds);

module.exports = router; 