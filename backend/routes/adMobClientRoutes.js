const express = require('express');
const router = express.Router();
const adMobController = require('../controllers/adMobController');
const rateLimit = require('express-rate-limit');
// Rate limiting middleware
const { verifyClientApp } = require('../middleware/authMiddleware');
const apiLimiter = rateLimit({
  windowMs: 15 * 60 * 1000, // 15 minutes
  max: Infinity, // Allow unlimited requests per IP
  standardHeaders: true, // Return rate limit info in the `RateLimit-*` headers
  legacyHeaders: false, // Disable the `X-RateLimit-*` headers
  message: {
    success: false,
    message: 'Too many requests, please try again later.',
    error: 'RATE_LIMIT_EXCEEDED'
  }
});

// Apply rate limiting to all routes
router.use(apiLimiter);

// Apply client app verification to all routes
router.use(verifyClientApp);

/**
 * @route   GET /api/admob/units
 * @desc    Get available ad units for the client
 * @access  Public (with app verification)
 */
router.get('/units', adMobController.getAdUnits);

/**
 * @route   GET /api/admob/status
 * @desc    Get ad status information
 * @access  Public (with app verification)
 */
router.get('/status', adMobController.getAdStatus);

/**
 * @route   GET /api/admob/config
 * @desc    Get ad configuration based on client context
 * @access  Public (with app verification)
 */
router.get('/config', adMobController.getAdConfig);

/**
 * @route   POST /api/admob/impression/:adId
 * @desc    Track ad impression
 * @access  Public (with app verification)
 */
router.post('/impression/:adId', adMobController.trackImpression);

/**
 * @route   POST /api/admob/click/:adId
 * @desc    Track ad click
 * @access  Public (with app verification)
 */
router.post('/click/:adId', adMobController.trackClick);

/**
 * @route   GET /api/admob/types
 * @desc    Get ad types
 * @access  Public (with app verification)
 */
router.get('/types', adMobController.getAdTypes);

/**
 * @route   POST /api/admob/engagement
 * @desc    Record user engagement with ads
 * @access  Public (with app verification)
 */
router.post('/engagement', adMobController.recordEngagement);

module.exports = router; 