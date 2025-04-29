/**
 * Sponsored Ad Controller
 * 
 * This module handles business logic for sponsored banner ads.
 */

const SponsoredAd = require('../models/SponsoredAd');
const logger = require('../config/logger');

/**
 * Get active sponsored ads
 * @param {Object} req - Express request object
 * @param {Object} res - Express response object
 */
exports.getActiveAds = async (req, res) => {
  try {
    const location = req.query.location || null;
    const limit = parseInt(req.query.limit) || 10;
    
    const ads = await SponsoredAd.getActiveAds(location);
    
    // Limit the number of ads returned based on priority
    const limitedAds = ads.slice(0, limit);
    
    res.json({
      success: true,
      ads: limitedAds
    });
  } catch (error) {
    logger.error(`Error in getActiveAds: ${error.message}`);
    res.status(500).json({
      success: false,
      message: 'Failed to get active ads',
      error: process.env.NODE_ENV === 'development' ? error.message : 'Server error'
    });
  }
};

/**
 * Record ad impression
 * @param {Object} req - Express request object
 * @param {Object} res - Express response object
 */
exports.recordImpression = async (req, res) => {
  try {
    const { id } = req.params;
    const deviceId = req.body.deviceId || req.headers['x-device-id'] || null;
    
    const ad = await SponsoredAd.findById(id);
    
    if (!ad) {
      return res.status(404).json({
        success: false,
        message: 'Ad not found'
      });
    }
    
    await ad.recordImpression(deviceId);
    
    res.json({
      success: true,
      message: 'Impression recorded successfully'
    });
  } catch (error) {
    logger.error(`Error in recordImpression: ${error.message}`);
    res.status(500).json({
      success: false,
      message: 'Failed to record impression',
      error: process.env.NODE_ENV === 'development' ? error.message : 'Server error'
    });
  }
};

/**
 * Record ad click
 * @param {Object} req - Express request object
 * @param {Object} res - Express response object
 */
exports.recordClick = async (req, res) => {
  try {
    const { id } = req.params;
    const deviceId = req.body.deviceId || req.headers['x-device-id'] || null;
    
    const ad = await SponsoredAd.findById(id);
    
    if (!ad) {
      return res.status(404).json({
        success: false,
        message: 'Ad not found'
      });
    }
    
    await ad.recordClick(deviceId);
    
    res.json({
      success: true,
      message: 'Click recorded successfully'
    });
  } catch (error) {
    logger.error(`Error in recordClick: ${error.message}`);
    res.status(500).json({
      success: false,
      message: 'Failed to record click',
      error: process.env.NODE_ENV === 'development' ? error.message : 'Server error'
    });
  }
};

/**
 * Get ad statistics
 * @param {Object} req - Express request object
 * @param {Object} res - Express response object
 */
exports.getAdStats = async (req, res) => {
  try {
    const { id } = req.params;
    
    const ad = await SponsoredAd.findById(id);
    
    if (!ad) {
      return res.status(404).json({
        success: false,
        message: 'Ad not found'
      });
    }
    
    // Calculate CTR (Click-Through Rate)
    const ctr = ad.impression_count > 0 
      ? (ad.click_count / ad.impression_count * 100).toFixed(2) 
      : 0;
    
    res.json({
      success: true,
      stats: {
        impressions: ad.impression_count,
        clicks: ad.click_count,
        ctr: parseFloat(ctr),
        start_date: ad.start_date,
        end_date: ad.end_date,
        status: ad.status
      }
    });
  } catch (error) {
    logger.error(`Error in getAdStats: ${error.message}`);
    res.status(500).json({
      success: false,
      message: 'Failed to get ad statistics',
      error: process.env.NODE_ENV === 'development' ? error.message : 'Server error'
    });
  }
}; 