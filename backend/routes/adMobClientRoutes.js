const express = require('express');
const router = express.Router();
const adMobController = require('../controllers/adMobController');
const rateLimit = require('express-rate-limit');
const { verifyClientApp } = require('../middleware/authMiddleware');

// Rate limiting middleware
const apiLimiter = rateLimit({
  windowMs: 15 * 60 * 1000, // 15 minutes
  max: 100, // limit each IP to 100 requests per windowMs
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

module.exports = router; 