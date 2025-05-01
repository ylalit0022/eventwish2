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
    const deviceId = req.headers['x-device-id'] || req.query.deviceId || null;
    
    // Log device ID if present
    if (deviceId) {
      logger.debug(`Device ID for ad request: ${deviceId}`);
    } else {
      logger.debug('No device ID provided for ad request');
    }
    
    // Get active ads with frequency capping
    const ads = await SponsoredAd.getActiveAds(location, deviceId);
    logger.debug(`Found ${ads.length} active ads for location: ${location || 'all'}`);
    
    // Limit the number of ads returned
    const limitedAds = ads.slice(0, limit);
    
    // Get count of ads that were filtered out due to frequency capping
    const filteredCount = deviceId ? ads.filter(ad => 
      ad.metrics && (ad.metrics.is_frequency_capped || ad.metrics.is_daily_frequency_capped)
    ).length : 0;
    
    // Remove internal metrics before sending response and ensure consistent field naming
    const cleanedAds = limitedAds.map(ad => {
      // Extract metrics for the response summary
      const metrics = ad.metrics;
      
      // Remove metrics from the individual ad objects
      const cleanAd = { ...ad };
      delete cleanAd.metrics;
      delete cleanAd.weightedScore;
      
      // Ensure all snake_case fields have equivalent camelCase versions
      // This is redundant with the toJSON transform but ensures consistency
      if (!cleanAd.imageUrl && cleanAd.image_url) cleanAd.imageUrl = cleanAd.image_url;
      if (!cleanAd.redirectUrl && cleanAd.redirect_url) cleanAd.redirectUrl = cleanAd.redirect_url;
      if (!cleanAd.startDate && cleanAd.start_date) cleanAd.startDate = cleanAd.start_date;
      if (!cleanAd.endDate && cleanAd.end_date) cleanAd.endDate = cleanAd.end_date;
      if (!cleanAd.frequencyCap && cleanAd.frequency_cap !== undefined) cleanAd.frequencyCap = cleanAd.frequency_cap;
      if (!cleanAd.dailyFrequencyCap && cleanAd.daily_frequency_cap !== undefined) cleanAd.dailyFrequencyCap = cleanAd.daily_frequency_cap;
      if (!cleanAd.clickCount && cleanAd.click_count !== undefined) cleanAd.clickCount = cleanAd.click_count;
      if (!cleanAd.impressionCount && cleanAd.impression_count !== undefined) cleanAd.impressionCount = cleanAd.impression_count;

      return cleanAd;
    });
    
    res.json({
      success: true,
      ads: cleanedAds,
      stats: {
        total_count: ads.length,
        returned_count: limitedAds.length,
        filtered_by_frequency_cap: filteredCount,
        device_id: deviceId ? deviceId.substring(0, 8) + '...' : null // For debugging, partial ID only
      },
      message: deviceId ? 
        `Ads filtered for device, showing ${limitedAds.length} of ${ads.length} active ads` : 
        `Showing ${limitedAds.length} of ${ads.length} active ads`
    });
  } catch (error) {
    logger.error(`Error in getActiveAds: ${error.message}`);
    logger.error(error.stack);
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
    
    if (!id) {
      return res.status(400).json({
        success: false,
        message: 'Ad ID is required'
      });
    }
    
    const ad = await SponsoredAd.findById(id);
    
    if (!ad) {
      logger.warn(`Impression attempt for non-existent ad: ${id}`);
      return res.status(404).json({
        success: false,
        message: 'Ad not found'
      });
    }
    
    try {
      await ad.recordImpression(deviceId);
      logger.debug(`Successfully recorded impression for ad ${id} from device ${deviceId || 'unknown'}`);
    } catch (trackError) {
      logger.error(`Error recording impression: ${trackError.message}`);
      logger.error(trackError.stack);
      
      return res.status(500).json({
        success: false,
        message: 'Failed to record impression',
        error: process.env.NODE_ENV === 'development' ? trackError.message : 'Error tracking impression'
      });
    }
    
    res.json({
      success: true,
      message: 'Impression recorded successfully',
      ad_id: id
    });
  } catch (error) {
    logger.error(`Error in recordImpression: ${error.message}`);
    logger.error(error.stack);
    
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
    
    if (!id) {
      return res.status(400).json({
        success: false,
        message: 'Ad ID is required'
      });
    }
    
    const ad = await SponsoredAd.findById(id);
    
    if (!ad) {
      logger.warn(`Click attempt for non-existent ad: ${id}`);
      return res.status(404).json({
        success: false,
        message: 'Ad not found'
      });
    }
    
    try {
      await ad.recordClick(deviceId);
      logger.debug(`Successfully recorded click for ad ${id} from device ${deviceId || 'unknown'}`);
    } catch (trackError) {
      logger.error(`Error recording click: ${trackError.message}`);
      logger.error(trackError.stack);
      
      return res.status(500).json({
        success: false,
        message: 'Failed to record click',
        error: process.env.NODE_ENV === 'development' ? trackError.message : 'Error tracking click'
      });
    }
    
    res.json({
      success: true,
      message: 'Click recorded successfully',
      ad_id: id
    });
  } catch (error) {
    logger.error(`Error in recordClick: ${error.message}`);
    logger.error(error.stack);
    
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
    
    if (!id) {
      return res.status(400).json({
        success: false,
        message: 'Ad ID is required'
      });
    }
    
    const ad = await SponsoredAd.findById(id);
    
    if (!ad) {
      logger.warn(`Stats request for non-existent ad: ${id}`);
      return res.status(404).json({
        success: false,
        message: 'Ad not found'
      });
    }
    
    // Calculate CTR (Click-Through Rate)
    const ctr = ad.impression_count > 0 
      ? (ad.click_count / ad.impression_count * 100).toFixed(2) 
      : 0;
    
    // Include both snake_case and camelCase versions of fields for compatibility
    res.json({
      success: true,
      stats: {
        // Snake case (original)
        impressions: ad.impression_count,
        clicks: ad.click_count,
        ctr: parseFloat(ctr),
        start_date: ad.start_date,
        end_date: ad.end_date,
        status: ad.status,
        
        // Camel case (for Android client)
        impressionCount: ad.impression_count,
        clickCount: ad.click_count,
        clickThroughRate: parseFloat(ctr),
        startDate: ad.start_date,
        endDate: ad.end_date
      },
      ad: {
        id: ad._id.toString(),
        title: ad.title,
        location: ad.location,
        priority: ad.priority,
        imageUrl: ad.image_url,
        redirectUrl: ad.redirect_url
      }
    });
  } catch (error) {
    logger.error(`Error in getAdStats: ${error.message}`);
    logger.error(error.stack);
    
    res.status(500).json({
      success: false,
      message: 'Failed to get ad statistics',
      error: process.env.NODE_ENV === 'development' ? error.message : 'Server error'
    });
  }
}; 