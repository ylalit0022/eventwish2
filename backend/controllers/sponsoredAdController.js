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
 * Get ads for rotation with exclusion support
 * @param {Object} req - Express request object
 * @param {Object} res - Express response object
 */
exports.getAdsForRotation = async (req, res) => {
  try {
    const location = req.query.location || null;
    const limit = parseInt(req.query.limit) || 10;
    
    // Parse exclude parameter
    let excludeIds = [];
    if (req.query.exclude) {
      excludeIds = Array.isArray(req.query.exclude) 
        ? req.query.exclude 
        : [req.query.exclude];
    }
    
    // Get ads with exclusion
    const ads = await SponsoredAd.getAdsForRotation(location, excludeIds);
    
    // Apply fair distribution
    const distributedAds = SponsoredAd.applyFairDistribution(ads, limit);
    
    res.json({
      success: true,
      ads: distributedAds
    });
  } catch (error) {
    logger.error(`Error in getAdsForRotation: ${error.message}`);
    res.status(500).json({
      success: false,
      message: 'Failed to get ads for rotation',
      error: process.env.NODE_ENV === 'development' ? error.message : 'Server error'
    });
  }
};

/**
 * Get ads with fair distribution (weighted by priority and impressions)
 * @param {Object} req - Express request object
 * @param {Object} res - Express response object
 */
exports.getFairDistributedAds = async (req, res) => {
  try {
    const location = req.query.location || null;
    const limit = parseInt(req.query.limit) || 10;
    
    // Get ads with fair distribution
    const ads = await SponsoredAd.getFairDistributedAds(location, limit);
    
    res.json({
      success: true,
      ads: ads
    });
  } catch (error) {
    logger.error(`Error in getFairDistributedAds: ${error.message}`);
    res.status(500).json({
      success: false,
      message: 'Failed to get fair distributed ads',
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
    
    logger.debug(`Processing impression request for ad ${id} from device ${deviceId || 'unknown'}`);
    
    const ad = await SponsoredAd.findById(id);
    
    if (!ad) {
      logger.warn(`Impression attempt for non-existent ad: ${id}`);
      return res.status(404).json({
        success: false,
        message: 'Ad not found'
      });
    }
    
    // Capture state before recording impression
    const prevImpressionCount = ad.impression_count;
    const prevDeviceImpressions = deviceId ? (ad.device_impressions.get(deviceId) || 0) : 0;
    
    // Get daily impression data if available
    const today = new Date().toISOString().split('T')[0]; // YYYY-MM-DD format
    let prevDailyImpressions = 0;
    let isCapped = false;
    let isDailyCapped = false;
    
    if (deviceId && ad.device_daily_impressions) {
      const deviceDailyData = ad.device_daily_impressions.get(deviceId);
      if (deviceDailyData) {
        if (deviceDailyData instanceof Map) {
          prevDailyImpressions = deviceDailyData.get(today) || 0;
        } else if (typeof deviceDailyData === 'object') {
          prevDailyImpressions = deviceDailyData[today] || 0;
        }
      }
      
      // Log before state for debugging
      logger.debug(`Pre-impression tracking data for ad ${id}, device ${deviceId}: ` +
        `total=${prevImpressionCount}, device=${prevDeviceImpressions}, today=${prevDailyImpressions}`);
    }
    
    // Check if already capped
    if (ad.frequency_cap > 0 && prevDeviceImpressions >= ad.frequency_cap) {
      isCapped = true;
      logger.debug(`⚠️ Ad ${id} is already frequency capped for device ${deviceId}: ${prevDeviceImpressions}/${ad.frequency_cap}`);
    }
    
    if (ad.daily_frequency_cap > 0 && prevDailyImpressions >= ad.daily_frequency_cap) {
      isDailyCapped = true;
      logger.debug(`⚠️ Ad ${id} is already daily frequency capped for device ${deviceId}: ${prevDailyImpressions}/${ad.daily_frequency_cap}`);
    }
    
    try {
      // Record impression in database
      const updatedAd = await ad.recordImpression(deviceId);
      logger.debug(`Successfully recorded impression for ad ${id} from device ${deviceId || 'unknown'}`);
      
      // Capture state after recording impression
      const newImpressionCount = updatedAd.impression_count;
      const newDeviceImpressions = deviceId ? (updatedAd.device_impressions.get(deviceId) || 0) : 0;
      
      // Get updated daily impression count
      let newDailyImpressions = 0;
      if (deviceId && updatedAd.device_daily_impressions) {
        const deviceDailyData = updatedAd.device_daily_impressions.get(deviceId);
        if (deviceDailyData) {
          if (deviceDailyData instanceof Map) {
            newDailyImpressions = deviceDailyData.get(today) || 0;
          } else if (typeof deviceDailyData === 'object') {
            newDailyImpressions = deviceDailyData[today] || 0;
          }
        }
      }
      
      // Calculate if impression was actually recorded
      const wasRecorded = newImpressionCount > prevImpressionCount;
      const deviceImpressionDelta = newDeviceImpressions - prevDeviceImpressions;
      const dailyImpressionDelta = newDailyImpressions - prevDailyImpressions;
      
      // Log after state for debugging
      logger.debug(`Post-impression tracking data for ad ${id}, device ${deviceId}: ` +
        `total=${newImpressionCount} (Δ${newImpressionCount-prevImpressionCount}), ` +
        `device=${newDeviceImpressions} (Δ${deviceImpressionDelta}), ` +
        `today=${newDailyImpressions} (Δ${dailyImpressionDelta}), ` +
        `counted=${wasRecorded ? 'YES' : 'NO'}`);
      
      // Calculate remaining impressions
      const remainingTotalImpressions = updatedAd.frequency_cap > 0 ? 
        Math.max(0, updatedAd.frequency_cap - newDeviceImpressions) : null;
        
      const remainingDailyImpressions = updatedAd.daily_frequency_cap > 0 ? 
        Math.max(0, updatedAd.daily_frequency_cap - newDailyImpressions) : null;
      
      // Check current capping status
      const isNowCapped = updatedAd.frequency_cap > 0 && newDeviceImpressions >= updatedAd.frequency_cap;
      const isNowDailyCapped = updatedAd.daily_frequency_cap > 0 && newDailyImpressions >= updatedAd.daily_frequency_cap;
      
      // Calculate engagement metrics
      const clickCount = updatedAd.click_count || 0;
      const deviceClicks = deviceId ? (updatedAd.device_clicks.get(deviceId) || 0) : 0;
      
      const overallCTR = newImpressionCount > 0 ? (clickCount / newImpressionCount) * 100 : 0;
      const deviceCTR = newDeviceImpressions > 0 ? (deviceClicks / newDeviceImpressions) * 100 : 0;
      
      // Return detailed response with impression data
      res.json({
        success: true,
        message: wasRecorded ? 
          'Impression recorded successfully' : 
          'Impression not recorded due to frequency capping',
        ad_id: id,
        // Include tracking data in the response
        impression_count: newImpressionCount,
        device_impressions: newDeviceImpressions,
        daily_count: newDailyImpressions,
        is_capped: isNowCapped,
        is_daily_capped: isNowDailyCapped,
        frequency_cap: updatedAd.frequency_cap,
        daily_frequency_cap: updatedAd.daily_frequency_cap,
        was_counted: wasRecorded,
        
        // Include delta information
        delta: {
          total: newImpressionCount - prevImpressionCount,
          device: deviceImpressionDelta,
          daily: dailyImpressionDelta
        },
        
        // Include remaining impressions info
        remaining: {
          total: remainingTotalImpressions,
          daily: remainingDailyImpressions
        },
        
        // Include capping transition info
        capping: {
          was_frequency_capped: isCapped,
          is_now_frequency_capped: isNowCapped,
          was_daily_capped: isDailyCapped,
          is_now_daily_capped: isNowDailyCapped
        },
        
        // Include engagement metrics
        engagement: {
          click_count: clickCount,
          device_clicks: deviceClicks,
          ctr: overallCTR.toFixed(2),
          device_ctr: deviceCTR.toFixed(2)
        },
        
        // Also include camelCase versions for client compatibility
        impressionCount: newImpressionCount,
        deviceImpressions: newDeviceImpressions,
        dailyCount: newDailyImpressions,
        isCapped: isNowCapped,
        isDailyCapped: isNowDailyCapped,
        frequencyCap: updatedAd.frequency_cap,
        dailyFrequencyCap: updatedAd.daily_frequency_cap,
        wasCounted: wasRecorded,
        
        deltaInfo: {
          total: newImpressionCount - prevImpressionCount,
          device: deviceImpressionDelta,
          daily: dailyImpressionDelta
        },
        
        remainingImpressions: {
          total: remainingTotalImpressions,
          daily: remainingDailyImpressions
        },
        
        cappingInfo: {
          wasFrequencyCapped: isCapped,
          isNowFrequencyCapped: isNowCapped,
          wasDailyCapped: isDailyCapped,
          isNowDailyCapped: isNowDailyCapped
        },
        
        engagementMetrics: {
          clickCount: clickCount,
          deviceClicks: deviceClicks,
          ctr: parseFloat(overallCTR.toFixed(2)),
          deviceCtr: parseFloat(deviceCTR.toFixed(2))
        }
      });
    } catch (trackError) {
      logger.error(`Error recording impression: ${trackError.message}`);
      logger.error(trackError.stack);
      
      return res.status(500).json({
        success: false,
        message: 'Failed to record impression',
        error: process.env.NODE_ENV === 'development' ? trackError.message : 'Error tracking impression'
      });
    }
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
    
    logger.debug(`Processing click request for ad ${id} from device ${deviceId || 'unknown'}`);
    
    const ad = await SponsoredAd.findById(id);
    
    if (!ad) {
      logger.warn(`Click attempt for non-existent ad: ${id}`);
      return res.status(404).json({
        success: false,
        message: 'Ad not found'
      });
    }
    
    // Capture state before recording click
    const prevClickCount = ad.click_count;
    const prevDeviceClicks = deviceId ? (ad.device_clicks.get(deviceId) || 0) : 0;
    const prevImpressionCount = ad.impression_count;
    const prevDeviceImpressions = deviceId ? (ad.device_impressions.get(deviceId) || 0) : 0;
    
    // Log before state for debugging
    logger.debug(`Pre-click tracking data for ad ${id}, device ${deviceId}: ` +
      `clicks=${prevClickCount}, device_clicks=${prevDeviceClicks}, ` +
      `impressions=${prevImpressionCount}, device_impressions=${prevDeviceImpressions}`);
    
    try {
      // Record click in database
      const updatedAd = await ad.recordClick(deviceId);
      logger.debug(`Successfully recorded click for ad ${id} from device ${deviceId || 'unknown'}`);
      
      // Capture state after recording click
      const newClickCount = updatedAd.click_count;
      const newDeviceClicks = deviceId ? (updatedAd.device_clicks.get(deviceId) || 0) : 0;
      const clickDelta = newClickCount - prevClickCount;
      const deviceClickDelta = newDeviceClicks - prevDeviceClicks;
      
      // Log after state for debugging
      logger.debug(`Post-click tracking data for ad ${id}, device ${deviceId}: ` +
        `clicks=${newClickCount} (Δ${clickDelta}), ` +
        `device_clicks=${newDeviceClicks} (Δ${deviceClickDelta})`);
      
      // Calculate CTR metrics
      const overallCTR = updatedAd.impression_count > 0 ? 
                        (newClickCount / updatedAd.impression_count) * 100 : 0;
                        
      const deviceCTR = prevDeviceImpressions > 0 ? 
                       (newDeviceClicks / prevDeviceImpressions) * 100 : 0;
      
      // Get current time for timestamp
      const clickTimestamp = new Date();
      
      // Return detailed response with click data
      res.json({
        success: true,
        message: 'Click recorded successfully',
        ad_id: id,
        timestamp: clickTimestamp,
        
        // Include basic tracking data
        click_count: newClickCount,
        device_clicks: newDeviceClicks,
        delta: clickDelta,
        device_delta: deviceClickDelta,
        
        // Include impression data for reference
        impression_data: {
          impression_count: updatedAd.impression_count,
          device_impressions: prevDeviceImpressions,
        },
        
        // Include engagement metrics
        engagement: {
          ctr: overallCTR.toFixed(2),
          device_ctr: deviceCTR.toFixed(2),
          device_click_ratio: newDeviceClicks > 0 ? 
            (newDeviceClicks / Math.max(1, newClickCount)) * 100 : 0
        },
        
        // Temporal information
        timing: {
          click_time: clickTimestamp,
          click_time_iso: clickTimestamp.toISOString(),
          start_date: updatedAd.start_date,
          end_date: updatedAd.end_date,
          is_active: updatedAd.status && 
            clickTimestamp >= updatedAd.start_date && 
            clickTimestamp <= updatedAd.end_date
        },
        
        // Also include camelCase versions for client compatibility
        clickCount: newClickCount,
        deviceClicks: newDeviceClicks,
        delta: clickDelta,
        deviceDelta: deviceClickDelta,
        
        impressionData: {
          impressionCount: updatedAd.impression_count,
          deviceImpressions: prevDeviceImpressions,
        },
        
        engagementMetrics: {
          ctr: parseFloat(overallCTR.toFixed(2)),
          deviceCtr: parseFloat(deviceCTR.toFixed(2)),
          deviceClickRatio: newDeviceClicks > 0 ? 
            parseFloat(((newDeviceClicks / Math.max(1, newClickCount)) * 100).toFixed(2)) : 0
        },
        
        timingInfo: {
          clickTime: clickTimestamp,
          clickTimeIso: clickTimestamp.toISOString(),
          startDate: updatedAd.start_date,
          endDate: updatedAd.end_date,
          isActive: updatedAd.status && 
            clickTimestamp >= updatedAd.start_date && 
            clickTimestamp <= updatedAd.end_date
        }
      });
    } catch (trackError) {
      logger.error(`Error recording click: ${trackError.message}`);
      logger.error(trackError.stack);
      
      return res.status(500).json({
        success: false,
        message: 'Failed to record click',
        error: process.env.NODE_ENV === 'development' ? trackError.message : 'Error tracking click'
      });
    }
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