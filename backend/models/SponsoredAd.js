/**
 * SponsoredAd Model
 * 
 * This module defines the schema for sponsored banner ads
 * that can be displayed in the application.
 */

const mongoose = require('mongoose');
const logger = require('../utils/logger') || console;

const sponsoredAdSchema = new mongoose.Schema({
  image_url: {
    type: String,
    required: [true, 'Image URL is required'],
    validate: {
      validator: function(v) {
        return /^https?:\/\/.+/.test(v);
      },
      message: props => `${props.value} is not a valid URL!`
    }
  },
  redirect_url: {
    type: String,
    required: [true, 'Redirect URL is required'],
    validate: {
      validator: function(v) {
        return /^https?:\/\/.+/.test(v);
      },
      message: props => `${props.value} is not a valid URL!`
    }
  },
  status: {
    type: Boolean,
    default: true
  },
  start_date: {
    type: Date,
    required: [true, 'Start date is required'],
    default: Date.now
  },
  end_date: {
    type: Date,
    required: [true, 'End date is required'],
    validate: {
      validator: function(v) {
        return v > this.start_date;
      },
      message: 'End date must be after start date'
    }
  },
  location: {
    type: String,
    required: [true, 'Ad location is required'],
    enum: ['home_top', 'home_bottom', 'category_below', 'details_top', 'details_bottom'],
    default: 'category_below'
  },
  priority: {
    type: Number,
    default: 1,
    min: 1,
    max: 10
  },
  frequency_cap: {
    type: Number,
    default: 0,
    min: 0,
    description: 'Maximum number of impressions per device (0 = unlimited)'
  },
  daily_frequency_cap: {
    type: Number,
    default: 0,
    min: 0,
    description: 'Maximum number of impressions per device per day (0 = unlimited)'
  },
  click_count: {
    type: Number,
    default: 0
  },
  impression_count: {
    type: Number,
    default: 0
  },
  device_impressions: {
    type: Map,
    of: Number,
    default: () => new Map()
  },
  device_clicks: {
    type: Map,
    of: Number,
    default: () => new Map()
  },
  device_daily_impressions: {
    type: Map,
    of: Map,
    default: () => new Map(),
    description: 'Daily impressions per device, stored as Map: deviceId -> (date string -> count)'
  },
  title: {
    type: String,
    default: 'Sponsored Ad'
  },
  description: {
    type: String,
    default: ''
  }
}, {
  timestamps: true,
  toJSON: {
    virtuals: true,
    transform: function(doc, ret) {
      // Use camelCase for client compatibility
      ret.id = ret._id.toString();
      ret.imageUrl = ret.image_url;
      ret.redirectUrl = ret.redirect_url;
      ret.startDate = ret.start_date;
      ret.endDate = ret.end_date;
      ret.frequencyCap = ret.frequency_cap;
      ret.dailyFrequencyCap = ret.daily_frequency_cap;
      ret.clickCount = ret.click_count;
      ret.impressionCount = ret.impression_count;
      
      // Don't expose internal tracking data to clients
      delete ret._id;
      delete ret.__v;
      delete ret.device_impressions;
      delete ret.device_clicks;
      delete ret.device_daily_impressions;
      
      // Keep both snake_case and camelCase for backward compatibility
      return ret;
    }
  }
});

// Create indexes
sponsoredAdSchema.index({ status: 1, start_date: 1, end_date: 1 });
sponsoredAdSchema.index({ location: 1, priority: -1 });

// Statics
sponsoredAdSchema.statics.getActiveAds = async function(location = null, deviceId = null) {
  try {
    const now = new Date();
    const query = {
      status: true,
      start_date: { $lte: now },
      end_date: { $gte: now }
    };
    
    if (location) {
      query.location = location;
    }
    
    // Get all potential ads first
    const ads = await this.find(query);
    logger.debug(`Found ${ads.length} active ads for location: ${location || 'any'}`);
    
    // If no device ID provided, just return all ads sorted by priority
    if (!deviceId) {
      return ads.sort((a, b) => b.priority - a.priority);
    }
    
    // Filter ads based on frequency capping
    const today = new Date().toISOString().split('T')[0]; // YYYY-MM-DD format
    
    // Get detailed impression data for analytics
    const adsWithMetrics = ads.map(ad => {
      try {
        // Initialize maps if they don't exist
        if (!ad.device_impressions) {
          ad.device_impressions = new Map();
          ad.markModified('device_impressions');
        }
        
        if (!ad.device_daily_impressions) {
          ad.device_daily_impressions = new Map();
          ad.markModified('device_daily_impressions');
        }
        
        // Get impression metrics for this device
        const totalImpressions = ad.device_impressions.get(deviceId) || 0;
        
        // Get daily impressions for today
        let dailyImpressions = 0;
        try {
          const deviceDailyData = ad.device_daily_impressions.get(deviceId);
          if (deviceDailyData) {
            dailyImpressions = deviceDailyData.get(today) || 0;
          }
        } catch (dailyError) {
          logger.error(`Error accessing daily impressions for ad ${ad._id}, device ${deviceId}:`, dailyError);
          logger.error(dailyError.stack);
          dailyImpressions = 0;
        }
        
        // Calculate remaining available impressions
        const remainingImpressions = ad.frequency_cap > 0 ? Math.max(0, ad.frequency_cap - totalImpressions) : null;
        const remainingDailyImpressions = ad.daily_frequency_cap > 0 ? Math.max(0, ad.daily_frequency_cap - dailyImpressions) : null;
        
        // Calculate impression status
        const isFrequencyCapped = ad.frequency_cap > 0 && totalImpressions >= ad.frequency_cap;
        const isDailyFrequencyCapped = ad.daily_frequency_cap > 0 && dailyImpressions >= ad.daily_frequency_cap;
        
        return {
          ...ad.toObject(),
          metrics: {
            device_impressions: totalImpressions,
            device_daily_impressions: dailyImpressions,
            remaining_impressions: remainingImpressions,
            remaining_daily_impressions: remainingDailyImpressions,
            is_frequency_capped: isFrequencyCapped,
            is_daily_frequency_capped: isDailyFrequencyCapped
          }
        };
      } catch (error) {
        logger.error(`Error processing ad metrics for ad ${ad._id}:`, error);
        logger.error(error.stack);
        // Return ad without metrics in case of error
        return {
          ...ad.toObject(),
          metrics: {
            error: true,
            message: error.message
          }
        };
      }
    });
    
    // Filter ads based on frequency capping
    const filteredAds = adsWithMetrics.filter(ad => {
      try {
        // Skip ads with errors in metrics
        if (ad.metrics && ad.metrics.error) {
          logger.debug(`Ad ${ad._id} filtered: metrics error - ${ad.metrics.message}`);
          return false;
        }
        
        // If ad is deleted or inactive, skip it
        if (!ad.status) {
          logger.debug(`Ad ${ad._id} filtered: inactive status`);
          return false;
        }
        
        // Check total frequency cap
        if (ad.frequency_cap > 0) {
          const deviceImpressions = ad.metrics.device_impressions || 0;
          if (deviceImpressions >= ad.frequency_cap) {
            logger.debug(`Ad ${ad._id} filtered: reached total impression cap (${deviceImpressions}/${ad.frequency_cap})`);
            return false; // Ad has reached total impression cap for this device
          }
        }
        
        // Check daily frequency cap
        if (ad.daily_frequency_cap > 0) {
          const todayImpressions = ad.metrics.device_daily_impressions || 0;
          if (todayImpressions >= ad.daily_frequency_cap) {
            logger.debug(`Ad ${ad._id} filtered: reached daily impression cap (${todayImpressions}/${ad.daily_frequency_cap})`);
            return false; // Ad has reached daily impression cap for this device
          }
        }
        
        return true; // Ad is under frequency caps
      } catch (error) {
        logger.error(`Error filtering ad ${ad._id}:`, error);
        logger.error(error.stack);
        return false; // Skip ad if there's an error
      }
    });
    
    logger.debug(`Filtered to ${filteredAds.length} ads after frequency capping`);
    
    // Use better weighted algorithm for more balanced distribution
    const totalPriority = filteredAds.reduce((sum, ad) => sum + ad.priority, 0);
    
    // Add weighted random score to each ad
    const adsWithWeightedScores = filteredAds.map(ad => {
      // Calculate weighted score: 
      // - Base priority percentage (how much of total priority this ad represents)
      // - Plus randomness factor scaled by priority
      const priorityWeight = totalPriority > 0 ? ad.priority / totalPriority : 1;
      const randomFactor = Math.random() * 0.4; // Random factor between 0-0.4
      const weightedScore = priorityWeight + (randomFactor * (ad.priority / 10)); // Scale randomness based on priority
      
      return { ...ad, weightedScore };
    });
    
    // Sort by weighted score, higher scores first
    const sortedAds = adsWithWeightedScores.sort((a, b) => b.weightedScore - a.weightedScore);
    
    logger.debug(`Returning ${sortedAds.length} sorted ads`);
    return sortedAds;
  } catch (error) {
    logger.error(`Error in getActiveAds:`, error);
    logger.error(error.stack);
    return []; // Return empty array in case of error
  }
};

// Methods
sponsoredAdSchema.methods.recordImpression = async function(deviceId = null) {
  try {
    // Skip recording if deviceId is not provided
    if (!deviceId) {
      this.impression_count += 1;
      return this.save();
    }
    
    // Ensure Maps are initialized
    if (!this.device_impressions) {
      this.device_impressions = new Map();
      this.markModified('device_impressions');
    }
    
    if (!this.device_daily_impressions) {
      this.device_daily_impressions = new Map();
      this.markModified('device_daily_impressions');
    }
    
    // Get current impression counts
    const currentTotalCount = this.device_impressions.get(deviceId) || 0;
    
    // Get daily impression count
    const today = new Date().toISOString().split('T')[0]; // YYYY-MM-DD format
    
    // Initialize device daily data if not exists
    if (!this.device_daily_impressions.has(deviceId)) {
      this.device_daily_impressions.set(deviceId, new Map());
      this.markModified('device_daily_impressions');
    }
    
    // Get today's count
    const deviceDailyData = this.device_daily_impressions.get(deviceId);
    const todayCount = deviceDailyData.get(today) || 0;
    
    // Check total frequency cap
    if (this.frequency_cap > 0 && currentTotalCount >= this.frequency_cap) {
      logger.debug(`Impression skipped: Ad ${this._id} reached total frequency cap (${currentTotalCount}/${this.frequency_cap}) for device ${deviceId}`);
      return this; // Return without saving - cap reached
    }
    
    // Check daily frequency cap
    if (this.daily_frequency_cap > 0 && todayCount >= this.daily_frequency_cap) {
      logger.debug(`Impression skipped: Ad ${this._id} reached daily frequency cap (${todayCount}/${this.daily_frequency_cap}) for device ${deviceId}`);
      return this; // Return without saving - daily cap reached
    }
    
    // If we get here, we can record the impression
    this.impression_count += 1;
    
    // Record total impressions for this device
    this.device_impressions.set(deviceId, currentTotalCount + 1);
    this.markModified('device_impressions');
    
    // Increment today's count
    deviceDailyData.set(today, todayCount + 1);
    
    // Clean up old daily impressions (older than 30 days)
    const thirtyDaysAgo = new Date();
    thirtyDaysAgo.setDate(thirtyDaysAgo.getDate() - 30);
    const thirtyDaysAgoStr = thirtyDaysAgo.toISOString().split('T')[0];
    
    // Using forEach with Map's API
    const datesToRemove = [];
    deviceDailyData.forEach((count, dateKey) => {
      if (dateKey < thirtyDaysAgoStr) {
        datesToRemove.push(dateKey);
      }
    });
    
    // Remove old dates
    datesToRemove.forEach(dateKey => {
      deviceDailyData.delete(dateKey);
    });
    
    // Mark the object as modified
    this.markModified('device_daily_impressions');
    
    // Log impression details for debugging
    logger.debug(`Recorded impression for ad ${this._id}, device ${deviceId}: total=${currentTotalCount + 1}, today=${todayCount + 1}`);
    
    return this.save();
  } catch (error) {
    logger.error(`Error recording impression for ad ${this._id}:`, error);
    logger.error(error.stack);
    // Re-throw for caller to handle
    throw error;
  }
};

sponsoredAdSchema.methods.recordClick = async function(deviceId = null) {
  try {
    this.click_count += 1;
    
    if (deviceId) {
      // Ensure Map is initialized
      if (!this.device_clicks) {
        this.device_clicks = new Map();
        this.markModified('device_clicks');
      }
      
      const currentCount = this.device_clicks.get(deviceId) || 0;
      this.device_clicks.set(deviceId, currentCount + 1);
      
      // Mark as modified so Mongoose knows to save the changes
      this.markModified('device_clicks');
      
      logger.debug(`Recorded click for ad ${this._id}, device ${deviceId}: total=${currentCount + 1}`);
    }
    
    return this.save();
  } catch (error) {
    logger.error(`Error recording click for ad ${this._id}:`, error);
    logger.error(error.stack);
    throw error;
  }
};

module.exports = mongoose.model('SponsoredAd', sponsoredAdSchema, 'sponsored_ads'); 