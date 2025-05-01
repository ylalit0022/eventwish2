/**
 * Sponsored Ad Routes
 * 
 * This module defines the routes for sponsored banner ads.
 */

const express = require('express');
const router = express.Router();
const sponsoredAdController = require('../controllers/sponsoredAdController');
const { verifyApiKey } = require('../middleware/authMiddleware');
const validateObjectId = require('../middleware/validateObjectId');
const logger = require('../config/logger');

/**
 * Log middleware for debugging API requests
 */
const logApiRequest = (req, res, next) => {
  const deviceId = req.headers['x-device-id'] || req.query.deviceId || null;
  const maskedDeviceId = deviceId ? `${deviceId.substring(0, 6)}...` : 'none';
  
  logger.debug(`[API] ${req.method} ${req.originalUrl} | DeviceID: ${maskedDeviceId}`);
  next();
};

/**
 * @route GET /api/sponsored-ads
 * @description Get active sponsored ads
 * @access Public
 */
router.get('/', logApiRequest, sponsoredAdController.getActiveAds);

/**
 * @route POST /api/sponsored-ads/viewed/:id
 * @description Record ad impression
 * @access Public
 */
router.post('/viewed/:id', [logApiRequest, validateObjectId('id')], sponsoredAdController.recordImpression);

/**
 * @route POST /api/sponsored-ads/clicked/:id
 * @description Record ad click
 * @access Public
 */
router.post('/clicked/:id', [logApiRequest, validateObjectId('id')], sponsoredAdController.recordClick);

/**
 * @route GET /api/sponsored-ads/stats/:id
 * @description Get ad statistics
 * @access Private (API key required)
 */
router.get('/stats/:id', [verifyApiKey, validateObjectId('id')], sponsoredAdController.getAdStats);

/**
 * @route GET /api/sponsored-ads/test
 * @description Test endpoint to verify API connectivity
 * @access Public
 */
router.get('/test', (req, res) => {
  try {
    const deviceId = req.headers['x-device-id'] || req.query.deviceId || 'unknown';
    
    res.json({
      success: true,
      message: 'Sponsored ad API is working correctly',
      timestamp: new Date().toISOString(),
      deviceId: deviceId.substring(0, 6) + '...' // Only return partial ID for privacy
    });
  } catch (error) {
    logger.error('Error in test endpoint:', error);
    res.status(500).json({
      success: false,
      message: 'Error in test endpoint',
      timestamp: new Date().toISOString()
    });
  }
});

module.exports = router; 