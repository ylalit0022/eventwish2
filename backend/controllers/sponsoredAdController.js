/**
 * Sponsored Ad Controller
 * 
 * This module handles business logic for sponsored banner ads.
 */

const SponsoredAd = require('../models/SponsoredAd');
const logger = require('../config/logger');
const mongoose = require('mongoose');

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

/**
 * Duplicate a sponsored ad
 * @param {Object} req - Express request object
 * @param {Object} res - Express response object
 */
exports.duplicateSponsoredAd = async (req, res) => {
  try {
    const { id } = req.params;
    
    logger.info(`Starting to duplicate sponsored ad with ID: ${id}`);
    
    // Validate ID format
    if (!mongoose.Types.ObjectId.isValid(id)) {
      logger.warn(`Invalid sponsored ad ID format: ${id}`);
      return res.status(400).json({
        success: false,
        message: 'Invalid sponsored ad ID format',
        error: 'INVALID_ID_FORMAT'
      });
    }
    
    // Find the original sponsored ad
    const originalAd = await SponsoredAd.findById(id);
    
    if (!originalAd) {
      logger.warn(`Sponsored ad not found with ID: ${id}`);
      return res.status(404).json({
        success: false,
        message: 'Sponsored ad not found',
        error: 'AD_NOT_FOUND'
      });
    }
    
    logger.info(`Found original ad: ${originalAd._id}, title: ${originalAd.title}`);
    
    // Convert to plain JavaScript object
    const adData = originalAd.toObject();
    
    // Remove fields that should not be duplicated
    delete adData._id;
    delete adData.id;
    delete adData.createdAt;
    delete adData.updatedAt;
    delete adData.__v;
    delete adData.impression_count;
    delete adData.click_count;
    delete adData.impressions;
    delete adData.clicks;
    delete adData.device_impressions;
    delete adData.device_clicks;
    
    // Log the uid value
    logger.info(`Original ad uid: ${adData.uid}`);
    
    // Make sure uid is preserved - it's required by the model and must be a valid ObjectId
    try {
      // Ensure uid is a valid MongoDB ObjectId
      if (adData.uid) {
        // If it's already an ObjectId, keep it
        if (typeof adData.uid === 'string') {
          adData.uid = mongoose.Types.ObjectId(adData.uid);
        }
        logger.info(`Using original ad uid: ${adData.uid}`);
      } else {
        logger.warn('No uid found in original ad, attempting to use alternative');
        
        // Try to find a user in the database to use as the owner
        const User = mongoose.model('User');
        const adminUser = await User.findOne({ role: 'admin' }).sort({ createdAt: 1 });
        
        if (adminUser) {
          adData.uid = adminUser._id;
          logger.info(`Using admin user ID: ${adData.uid}`);
        } else {
          // Use a default ObjectId as last resort
          adData.uid = mongoose.Types.ObjectId('000000000000000000000000');
          logger.warn('No admin user found, using default ObjectId');
        }
      }
    } catch (uidError) {
      logger.error(`Error processing uid: ${uidError.message}`);
      // Use a default ObjectId if there's an error
      adData.uid = mongoose.Types.ObjectId('000000000000000000000000');
      logger.warn('Error with uid, using default ObjectId');
    }
    
    // Modify fields as needed
    adData.title = `${adData.title} (Copy)`;
    adData.status = false; // Set status to inactive by default
    
    logger.info('Creating new sponsored ad with duplicated data');
    logger.debug('Ad data for duplication:', adData);
    
    // Create a new sponsored ad with duplicated data
    const newAd = new SponsoredAd(adData);
    
    // Log validation errors if any
    const validationError = newAd.validateSync();
    if (validationError) {
      logger.error('Validation error:', validationError);
      
      // Extract specific validation error messages
      const errorMessages = {};
      if (validationError.errors) {
        Object.keys(validationError.errors).forEach(field => {
          errorMessages[field] = validationError.errors[field].message;
        });
      }
      
      return res.status(400).json({
        success: false,
        message: 'Validation error when duplicating sponsored ad',
        error: 'VALIDATION_ERROR',
        validationErrors: errorMessages,
        details: validationError.message
      });
    }
    
    try {
      await newAd.save();
      
      logger.info(`Sponsored ad duplicated successfully: ${id} -> ${newAd._id}`);
      
      res.status(201).json({
        success: true,
        message: 'Sponsored ad duplicated successfully',
        sponsoredAd: newAd
      });
    } catch (saveError) {
      logger.error(`Error saving duplicated ad: ${saveError.message}`);
      
      // Handle duplicate key errors
      if (saveError.code === 11000) {
        const duplicateField = Object.keys(saveError.keyPattern)[0];
        return res.status(409).json({
          success: false,
          message: `Duplicate value for ${duplicateField}`,
          error: 'DUPLICATE_KEY_ERROR',
          field: duplicateField,
          details: saveError.message
        });
      }
      
      throw saveError; // Re-throw to be caught by the outer catch block
    }
  } catch (error) {
    logger.error(`Error duplicating sponsored ad: ${error.message}`);
    logger.error(`Error stack: ${error.stack}`);
    
    // Check if it's a validation error
    if (error.name === 'ValidationError') {
      logger.error('Validation error details:', error.errors);
      
      // Extract specific validation error messages
      const errorMessages = {};
      if (error.errors) {
        Object.keys(error.errors).forEach(field => {
          errorMessages[field] = error.errors[field].message;
        });
      }
      
      return res.status(400).json({
        success: false,
        message: 'Validation error when duplicating sponsored ad',
        error: 'VALIDATION_ERROR',
        validationErrors: errorMessages,
        details: error.message
      });
    }
    
    // Check for MongoDB-specific errors
    if (error.name === 'MongoError' || error.name === 'MongoServerError') {
      logger.error('MongoDB error details:', error);
      
      return res.status(500).json({
        success: false,
        message: 'Database error when duplicating sponsored ad',
        error: 'DATABASE_ERROR',
        details: process.env.NODE_ENV === 'development' ? error.message : 'Database error'
      });
    }
    
    // Generic error response
    res.status(500).json({
      success: false,
      message: 'Failed to duplicate sponsored ad',
      error: 'SERVER_ERROR',
      details: process.env.NODE_ENV === 'development' ? error.message : 'Server error'
    });
  }
}; 