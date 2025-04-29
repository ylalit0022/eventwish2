/**
 * Sponsored Ad Routes
 * 
 * This module defines the routes for sponsored banner ads.
 */

const express = require('express');
const router = express.Router();
const sponsoredAdController = require('../controllers/sponsoredAdController');
const { verifyApiKey } = require('../middleware/authMiddleware');

/**
 * @route GET /api/sponsored-ads
 * @description Get active sponsored ads
 * @access Public
 */
router.get('/', sponsoredAdController.getActiveAds);

/**
 * @route POST /api/sponsored-ads/viewed/:id
 * @description Record ad impression
 * @access Public
 */
router.post('/viewed/:id', sponsoredAdController.recordImpression);

/**
 * @route POST /api/sponsored-ads/clicked/:id
 * @description Record ad click
 * @access Public
 */
router.post('/clicked/:id', sponsoredAdController.recordClick);

/**
 * @route GET /api/sponsored-ads/stats/:id
 * @description Get ad statistics
 * @access Private (API key required)
 */
router.get('/stats/:id', verifyApiKey, sponsoredAdController.getAdStats);

module.exports = router; 