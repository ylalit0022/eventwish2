/**
 * Suspicious Activity Routes
 * 
 * This module defines the routes for suspicious activity monitoring.
 */

const express = require('express');
const router = express.Router();
const suspiciousActivityController = require('../controllers/suspiciousActivityController');
const { authenticateApiKey } = require('../middleware/authMiddleware');

/**
 * @route GET /api/suspicious-activity/dashboard
 * @description Get suspicious activity dashboard data
 * @access Admin only
 */
router.get('/dashboard', authenticateApiKey, suspiciousActivityController.getDashboardData);

/**
 * @route GET /api/suspicious-activity/:entityType/:entityId
 * @description Get suspicious activities for a specific entity
 * @access Admin only
 */
router.get('/:entityType/:entityId', authenticateApiKey, suspiciousActivityController.getActivities);

/**
 * @route GET /api/suspicious-activity/:entityType/:entityId/reputation
 * @description Get reputation score for a specific entity
 * @access Admin only
 */
router.get('/:entityType/:entityId/reputation', authenticateApiKey, suspiciousActivityController.getReputationScore);

/**
 * @route GET /api/suspicious-activity/:entityType/:entityId/analyze
 * @description Analyze traffic patterns for a specific entity
 * @access Admin only
 */
router.get('/:entityType/:entityId/analyze', authenticateApiKey, suspiciousActivityController.analyzeTrafficPatterns);

/**
 * @route GET /api/suspicious-activity/:entityType/:entityId/check
 * @description Check if an entity is suspicious
 * @access Admin only
 */
router.get('/:entityType/:entityId/check', authenticateApiKey, suspiciousActivityController.checkSuspicious);

module.exports = router; 