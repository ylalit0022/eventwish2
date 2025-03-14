const analyticsService = require('../services/analyticsService');
const logger = require('../config/logger');
const mongoose = require('mongoose');

/**
 * Get analytics data for a specific ad
 * @param {Object} req - Express request object
 * @param {Object} res - Express response object
 */
const getAdAnalytics = async (req, res) => {
  try {
    const { adId } = req.params;
    
    // Validate adId
    if (!adId || !mongoose.Types.ObjectId.isValid(adId)) {
      return res.status(400).json({
        success: false,
        message: 'Valid ad ID is required',
        error: 'INVALID_AD_ID'
      });
    }
    
    // Get analytics data
    const analytics = await analyticsService.getAdAnalytics(adId);
    
    // Return analytics data
    return res.json({
      success: true,
      data: analytics
    });
  } catch (error) {
    logger.error(`Error getting ad analytics: ${error.message}`, { error });
    return res.status(500).json({
      success: false,
      message: 'Failed to get ad analytics',
      error: error.message
    });
  }
};

/**
 * Get summary analytics for all ads
 * @param {Object} req - Express request object
 * @param {Object} res - Express response object
 */
const getSummaryAnalytics = async (req, res) => {
  try {
    // Extract filters from query parameters
    const { adType, status } = req.query;
    
    // Build filters object
    const filters = {};
    
    if (adType) {
      filters.adType = adType;
    }
    
    if (status !== undefined) {
      filters.status = status === 'true';
    }
    
    // Get summary analytics
    const summary = await analyticsService.getSummaryAnalytics(filters);
    
    // Return summary analytics
    return res.json({
      success: true,
      data: summary
    });
  } catch (error) {
    logger.error(`Error getting summary analytics: ${error.message}`, { error });
    return res.status(500).json({
      success: false,
      message: 'Failed to get summary analytics',
      error: error.message
    });
  }
};

/**
 * Track ad revenue
 * @param {Object} req - Express request object
 * @param {Object} res - Express response object
 */
const trackRevenue = async (req, res) => {
  try {
    const { adId } = req.params;
    const { amount, currency } = req.body;
    
    // Validate adId
    if (!adId || !mongoose.Types.ObjectId.isValid(adId)) {
      return res.status(400).json({
        success: false,
        message: 'Valid ad ID is required',
        error: 'INVALID_AD_ID'
      });
    }
    
    // Validate amount
    if (typeof amount !== 'number' || isNaN(amount) || amount < 0) {
      return res.status(400).json({
        success: false,
        message: 'Valid revenue amount is required',
        error: 'INVALID_AMOUNT'
      });
    }
    
    // Track revenue
    const result = await analyticsService.trackRevenue(adId, amount, currency);
    
    // Return result
    return res.json({
      success: true,
      message: 'Revenue tracked successfully',
      data: result
    });
  } catch (error) {
    logger.error(`Error tracking revenue: ${error.message}`, { error });
    return res.status(500).json({
      success: false,
      message: 'Failed to track revenue',
      error: error.message
    });
  }
};

/**
 * Invalidate analytics cache for a specific ad
 * @param {Object} req - Express request object
 * @param {Object} res - Express response object
 */
const invalidateCache = async (req, res) => {
  try {
    const { adId } = req.params;
    
    // Validate adId
    if (!adId || !mongoose.Types.ObjectId.isValid(adId)) {
      return res.status(400).json({
        success: false,
        message: 'Valid ad ID is required',
        error: 'INVALID_AD_ID'
      });
    }
    
    // Invalidate cache
    analyticsService.invalidateAnalyticsCache(adId);
    
    // Return success
    return res.json({
      success: true,
      message: 'Analytics cache invalidated successfully'
    });
  } catch (error) {
    logger.error(`Error invalidating analytics cache: ${error.message}`, { error });
    return res.status(500).json({
      success: false,
      message: 'Failed to invalidate analytics cache',
      error: error.message
    });
  }
};

/**
 * Invalidate all analytics caches
 * @param {Object} req - Express request object
 * @param {Object} res - Express response object
 */
const invalidateAllCaches = async (req, res) => {
  try {
    // Invalidate all caches
    analyticsService.invalidateAllAnalyticsCaches();
    
    // Return success
    return res.json({
      success: true,
      message: 'All analytics caches invalidated successfully'
    });
  } catch (error) {
    logger.error(`Error invalidating all analytics caches: ${error.message}`, { error });
    return res.status(500).json({
      success: false,
      message: 'Failed to invalidate all analytics caches',
      error: error.message
    });
  }
};

module.exports = {
  getAdAnalytics,
  getSummaryAnalytics,
  trackRevenue,
  invalidateCache,
  invalidateAllCaches
}; 