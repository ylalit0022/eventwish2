const adMobService = require('../services/adMobService');
const logger = require('../config/logger');
const { AdMob } = require('../models/AdMob');
const mongoose = require('mongoose');
const fraudDetectionService = require('../services/fraudDetectionService');

/**
 * Get ad configuration based on client context
 * @param {Object} req - Express request object
 * @param {Object} res - Express response object
 */
const getAdConfig = async (req, res) => {
  try {
    const { 
      adType, 
      deviceType, 
      platform, 
      appVersion, 
      country, 
      language, 
      userId, 
      deviceId,
      screenSize,
      connectionType,
      timeZone,
      recentActivity
    } = req.query;
    
    // Validate required parameters
    if (!adType) {
      return res.status(400).json({
        success: false,
        message: 'Ad type is required',
        error: 'MISSING_AD_TYPE'
      });
    }
    
    // Build context object from request
    const context = {
      // User identification
      userId: userId || req.headers['x-user-id'] || 'anonymous',
      deviceId: deviceId || req.headers['x-device-id'] || 'unknown',
      
      // Device information
      deviceType: deviceType || req.headers['x-device-type'] || 'unknown',
      platform: platform || req.headers['x-platform'] || 'unknown',
      appVersion: appVersion || req.headers['x-app-version'] || 'unknown',
      screenSize: screenSize || req.headers['x-screen-size'] || 'unknown',
      
      // Location and language
      country: country || req.headers['x-country'] || 'unknown',
      language: language || req.headers['accept-language'] || 'en',
      timeZone: timeZone || req.headers['x-timezone'] || 'UTC',
      
      // Network information
      connectionType: connectionType || req.headers['x-connection-type'] || 'unknown',
      ip: req.ip,
      
      // User agent and referrer
      userAgent: req.headers['user-agent'] || 'unknown',
      referrer: req.headers['referer'] || 'unknown',
      
      // User behavior
      recentActivity: recentActivity ? JSON.parse(recentActivity) : [],
      
      // Request timestamp
      timestamp: new Date().toISOString()
    };
    
    // Get optimal ad configuration
    const adConfig = await adMobService.getOptimalAdConfig(context, adType);
    
    if (!adConfig) {
      return res.status(404).json({
        success: false,
        message: `No active ads found for type: ${adType}`,
        error: 'AD_NOT_FOUND'
      });
    }
    
    // Log the request for analytics
    logger.info('Ad config requested', {
      adType,
      adId: adConfig.id,
      context: {
        deviceType: context.deviceType,
        platform: context.platform,
        country: context.country,
        language: context.language
      } // Log only non-sensitive context data
    });
    
    // Return the ad configuration
    return res.status(200).json({
      success: true,
      data: {
        id: adConfig.id,
        adName: adConfig.adName,
        adType: adConfig.adType,
        adUnitCode: adConfig.adUnitCode,
        parameters: adConfig.parameters || {}
      }
    });
  } catch (error) {
    logger.error(`Error getting ad config: ${error.message}`, { error });
    return res.status(500).json({
      success: false,
      message: 'Failed to get ad configuration',
      error: 'SERVER_ERROR'
    });
  }
};

/**
 * Track ad impression
 * @param {Object} req - Express request object
 * @param {Object} res - Express response object
 */
const trackImpression = async (req, res) => {
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
    
    // Build context object from request
    const context = {
      deviceType: req.body.deviceType || 'unknown',
      platform: req.body.platform || 'unknown',
      appVersion: req.body.appVersion || 'unknown',
      ip: req.ip,
      userAgent: req.headers['user-agent'],
      timestamp: new Date().toISOString(),
      ...req.body.context
    };
    
    // Track impression
    await adMobService.trackImpression(adId, context);
    
    // Return success
    return res.json({
      success: true,
      message: 'Impression tracked successfully'
    });
  } catch (error) {
    logger.error(`Error tracking impression: ${error.message}`, { error });
    return res.status(500).json({
      success: false,
      message: 'Failed to track impression',
      error: error.message
    });
  }
};

/**
 * Track ad click
 * @param {Object} req - Express request object
 * @param {Object} res - Express response object
 */
const trackClick = async (req, res) => {
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
    
    // Build context object from request
    const context = {
      deviceType: req.body.deviceType || 'unknown',
      platform: req.body.platform || 'unknown',
      appVersion: req.body.appVersion || 'unknown',
      ip: req.ip,
      userAgent: req.headers['user-agent'],
      timestamp: new Date().toISOString(),
      ...req.body.context
    };
    
    // Check for click fraud
    const clickData = {
      userId: req.body.userId || req.headers['x-user-id'] || context.userId || 'anonymous',
      deviceId: req.body.deviceId || req.headers['x-device-id'] || context.deviceId || 'unknown',
      ip: req.ip || req.headers['x-forwarded-for'] || '0.0.0.0',
      adId,
      timestamp: Date.now(),
      context
    };
    
    const fraudResult = await fraudDetectionService.processClick(clickData);
    
    // If click is fraudulent, return error
    if (!fraudResult.allowed) {
      logger.warn(`Fraudulent click detected: ${JSON.stringify(fraudResult.fraudResult)}`);
      return res.status(403).json({
        success: false,
        message: 'Click rejected due to fraud detection',
        error: 'FRAUD_DETECTED',
        reasons: fraudResult.fraudResult.reasons
      });
    }
    
    // If fraud score is high but not blocked, add to context
    if (fraudResult.fraudResult && fraudResult.fraudResult.score > 30) {
      context.fraudScore = fraudResult.fraudResult.score;
      context.fraudReasons = fraudResult.fraudResult.reasons;
    }
    
    // Track click
    await adMobService.trackClick(adId, context);
    
    // Return success
    return res.json({
      success: true,
      message: 'Click tracked successfully'
    });
  } catch (error) {
    logger.error(`Error tracking click: ${error.message}`, { error });
    return res.status(500).json({
      success: false,
      message: 'Failed to track click',
      error: error.message
    });
  }
};

/**
 * Get all active ads
 * @param {Object} req - Express request object
 * @param {Object} res - Express response object
 */
const getActiveAds = async (req, res) => {
  try {
    const ads = await adMobService.getActiveAds();
    
    return res.json({
      success: true,
      data: {
        ads: ads.map(ad => ({
          id: ad.id,
          adName: ad.adName,
          adType: ad.adType,
          adUnitCode: ad.adUnitCode,
          status: ad.status
        }))
      }
    });
  } catch (error) {
    logger.error(`Error getting active ads: ${error.message}`, { error });
    return res.status(500).json({
      success: false,
      message: 'Failed to get active ads',
      error: error.message
    });
  }
};

/**
 * Get ad types
 * @param {Object} req - Express request object
 * @param {Object} res - Express response object
 */
const getAdTypes = async (req, res) => {
  try {
    const adTypes = await adMobService.getAdTypes();
    
    return res.json({
      success: true,
      data: {
        adTypes
      }
    });
  } catch (error) {
    logger.error(`Error getting ad types: ${error.message}`, { error });
    return res.status(500).json({
      success: false,
      message: 'Failed to get ad types',
      error: error.message
    });
  }
};

/**
 * Get ad units for client
 * @param {Object} req - Express request object
 * @param {Object} res - Express response object
 */
const getAdUnits = async (req, res) => {
  try {
    const adType = req.query.type; // Optional filter by ad type
    const deviceId = req.headers['x-device-id'];
    
    // Validate device ID
    if (!deviceId) {
      return res.status(400).json({
        success: false,
        message: 'Device ID is required',
        error: 'MISSING_DEVICE_ID'
      });
    }
    
    // Get all active ad units
    const query = { status: true };
    if (adType) {
      query.adType = adType;
    }
    
    const ads = await AdMob.find(query)
      .sort({ targetingPriority: -1 })
      .select('_id adName adType adUnitCode parameters status')
      .lean();
    
    // Return the ad units
    return res.status(200).json({
      success: true,
      data: {
        adUnits: ads.map(ad => ({
          id: ad._id,
          adName: ad.adName,
          adType: ad.adType,
          adUnitCode: ad.adUnitCode,
          parameters: ad.parameters || {},
          status: ad.status
        }))
      }
    });
  } catch (error) {
    logger.error(`Error getting ad units: ${error.message}`, { error });
    return res.status(500).json({
      success: false,
      message: 'Failed to get ad units',
      error: 'SERVER_ERROR'
    });
  }
};

/**
 * Get ad status
 * @param {Object} req - Express request object
 * @param {Object} res - Express response object
 */
const getAdStatus = async (req, res) => {
  try {
    const adType = req.query.type; // Optional filter by ad type
    const deviceId = req.headers['x-device-id'];
    
    // Validate device ID
    if (!deviceId) {
      return res.status(400).json({
        success: false,
        message: 'Device ID is required',
        error: 'MISSING_DEVICE_ID'
      });
    }
    
    // Get all active ad units
    const query = { status: true };
    if (adType) {
      query.adType = adType;
    }
    
    const ads = await AdMob.find(query)
      .sort({ targetingPriority: -1 })
      .select('_id adName adType adUnitCode parameters status')
      .lean();
    
    // Create status map
    const adStatus = {};
    for (const ad of ads) {
      adStatus[ad._id] = {
        canShow: true,
        cooldownUntil: null,
        reason: null
      };
    }
    
    // Return the ad status
    return res.status(200).json({
      success: true,
      data: {
        adStatus
      }
    });
  } catch (error) {
    logger.error(`Error getting ad status: ${error.message}`, { error });
    return res.status(500).json({
      success: false,
      message: 'Failed to get ad status',
      error: 'SERVER_ERROR'
    });
  }
};

/**
 * Record user engagement with ads
 * @param {Object} req - Express request object
 * @param {Object} res - Express response object
 */
const recordEngagement = async (req, res) => {
  try {
    // Check if this is a batch request
    const isBatch = Array.isArray(req.body.records);
    const records = isBatch ? req.body.records : [req.body];
    
    // Process all records
    const results = [];
    for (const record of records) {
      const { adUnitId, actionType, duration, timestamp } = record;
      
      // Basic validation
      if (!adUnitId) {
        results.push({
          success: false,
          message: 'Ad unit ID is required',
          error: 'MISSING_AD_UNIT_ID'
        });
        continue;
      }
      
      // Create engagement record
      const engagement = {
        adUnitId,
        actionType: actionType || 'view',
        duration: duration || 0,
        timestamp: timestamp || Date.now(),
        deviceId: req.headers['x-device-id'] || 'unknown',
        ip: req.ip,
        userAgent: req.headers['user-agent']
      };
      
      // Store engagement data
      try {
        // We would normally save this to database, but for now just log it
        logger.info('Ad engagement recorded', { engagement });
        
        results.push({
          success: true,
          message: 'Engagement recorded successfully',
          adUnitId
        });
      } catch (error) {
        results.push({
          success: false,
          message: 'Failed to record engagement',
          error: error.message,
          adUnitId
        });
      }
    }
    
    // Return batch results or single result
    if (isBatch) {
      return res.json({
        success: true,
        data: {
          results
        }
      });
    } else {
      const result = results[0];
      if (result.success) {
        return res.json({
          success: true,
          message: result.message
        });
      } else {
        return res.status(400).json({
          success: false,
          message: result.message,
          error: result.error
        });
      }
    }
  } catch (error) {
    logger.error(`Error recording engagement: ${error.message}`, { error });
    return res.status(500).json({
      success: false,
      message: 'Failed to record engagement',
      error: 'SERVER_ERROR'
    });
  }
};

module.exports = {
  getAdConfig,
  trackImpression,
  trackClick,
  getActiveAds,
  getAdTypes,
  getAdUnits,
  getAdStatus,
  recordEngagement
}; 