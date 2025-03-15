/**
 * Suspicious Activity Routes
 * 
 * This module defines the routes for suspicious activity monitoring.
 */

const express = require('express');
const router = express.Router();
const suspiciousActivityController = require('../controllers/suspiciousActivityController');
const { verifyApiKey } = require('../middleware/authMiddleware');

/**
 * @route GET /api/suspicious-activity/dashboard
 * @description Get suspicious activity dashboard data
 * @access Admin only
 */
router.get('/dashboard', verifyApiKey, suspiciousActivityController.getDashboardData);

/**
 * @route GET /api/suspicious-activity/:entityType/:entityId
 * @description Get suspicious activities for a specific entity
 * @access Admin only
 */
router.get('/:entityType/:entityId', verifyApiKey, suspiciousActivityController.getActivities);

/**
 * @route GET /api/suspicious-activity/:entityType/:entityId/reputation
 * @description Get reputation score for a specific entity
 * @access Admin only
 */
router.get('/:entityType/:entityId/reputation', verifyApiKey, suspiciousActivityController.getReputationScore);

/**
 * @route GET /api/suspicious-activity/:entityType/:entityId/analyze
 * @description Analyze traffic patterns for a specific entity
 * @access Admin only
 */
router.get('/:entityType/:entityId/analyze', verifyApiKey, suspiciousActivityController.analyzeTrafficPatterns);

/**
 * @route GET /api/suspicious-activity/:entityType/:entityId/check
 * @description Check if an entity is suspicious
 * @access Admin only
 */
router.get('/:entityType/:entityId/check', verifyApiKey, suspiciousActivityController.checkSuspicious);

module.exports = router; 