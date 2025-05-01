/**
 * SponsoredAd Model
 * 
 * This module defines the schema for sponsored banner ads
 * that can be displayed in the application.
 */

const mongoose = require('mongoose');

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
    of: Object,
    default: () => new Map()
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
      ret.id = ret._id;
      delete ret._id;
      delete ret.__v;
      delete ret.device_impressions;
      delete ret.device_clicks;
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
    console.log(`Found ${ads.length} active ads for location: ${location || 'any'}`);
    
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
        }
        
        if (!ad.device_daily_impressions) {
          ad.device_daily_impressions = new Map();
        }
        
        // Get impression metrics for this device
        const totalImpressions = ad.device_impressions.get(deviceId) || 0;
        
        // Get daily impressions for today
        let dailyImpressions = {};
        try {
          dailyImpressions = ad.device_daily_impressions.get(deviceId) || {};
          // If dailyImpressions is not an object (could happen with older data), initialize it
          if (typeof dailyImpressions !== 'object' || dailyImpressions === null) {
            console.error(`Invalid dailyImpressions for ad ${ad._id}, device ${deviceId}: type=${typeof dailyImpressions}, value=`, dailyImpressions);
            dailyImpressions = {};
          }
        } catch (dailyError) {
          console.error(`Error accessing daily impressions for ad ${ad._id}, device ${deviceId}:`, dailyError);
          console.error(dailyError.stack);
          dailyImpressions = {};
        }
        
        const todayImpressions = dailyImpressions[today] || 0;
        
        // Calculate remaining available impressions
        const remainingImpressions = ad.frequency_cap > 0 ? Math.max(0, ad.frequency_cap - totalImpressions) : null;
        const remainingDailyImpressions = ad.daily_frequency_cap > 0 ? Math.max(0, ad.daily_frequency_cap - todayImpressions) : null;
        
        // Calculate impression status
        const isFrequencyCapped = ad.frequency_cap > 0 && totalImpressions >= ad.frequency_cap;
        const isDailyFrequencyCapped = ad.daily_frequency_cap > 0 && todayImpressions >= ad.daily_frequency_cap;
        
        return {
          ...ad.toObject(),
          metrics: {
            device_impressions: totalImpressions,
            device_daily_impressions: todayImpressions,
            remaining_impressions: remainingImpressions,
            remaining_daily_impressions: remainingDailyImpressions,
            is_frequency_capped: isFrequencyCapped,
            is_daily_frequency_capped: isDailyFrequencyCapped
          }
        };
      } catch (error) {
        console.error(`Error processing ad metrics for ad ${ad._id}:`, error);
        console.error(error.stack);
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
          console.log(`Ad ${ad._id} filtered: metrics error - ${ad.metrics.message}`);
          return false;
        }
        
        // If ad is deleted or inactive, skip it
        if (!ad.status) {
          console.log(`Ad ${ad._id} filtered: inactive status`);
          return false;
        }
        
        // Check total frequency cap
        if (ad.frequency_cap > 0) {
          const deviceImpressions = ad.metrics.device_impressions || 0;
          if (deviceImpressions >= ad.frequency_cap) {
            console.log(`Ad ${ad._id} filtered: reached total impression cap (${deviceImpressions}/${ad.frequency_cap})`);
            return false; // Ad has reached total impression cap for this device
          }
        }
        
        // Check daily frequency cap
        if (ad.daily_frequency_cap > 0) {
          const todayImpressions = ad.metrics.device_daily_impressions || 0;
          if (todayImpressions >= ad.daily_frequency_cap) {
            console.log(`Ad ${ad._id} filtered: reached daily impression cap (${todayImpressions}/${ad.daily_frequency_cap})`);
            return false; // Ad has reached daily impression cap for this device
          }
        }
        
        return true; // Ad is under frequency caps
      } catch (error) {
        console.error(`Error filtering ad ${ad._id}:`, error);
        console.error(error.stack);
        return false; // Skip ad if there's an error
      }
    });
    
    console.log(`Filtered to ${filteredAds.length} ads after frequency capping`);
    
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
    
    console.log(`Returning ${sortedAds.length} sorted ads`);
    return sortedAds;
  } catch (error) {
    console.error(`Error in getActiveAds:`, error);
    console.error(error.stack);
    return []; // Return empty array in case of error
  }
};

// Methods
sponsoredAdSchema.methods.recordImpression = async function(deviceId = null) {
  try {
    this.impression_count += 1;
    
    if (deviceId) {
      // Ensure Maps are initialized
      if (!this.device_impressions) {
        this.device_impressions = new Map();
      }
      
      if (!this.device_daily_impressions) {
        this.device_daily_impressions = new Map();
      }
      
      // Record total impressions for this device
      const currentCount = this.device_impressions.get(deviceId) || 0;
      this.device_impressions.set(deviceId, currentCount + 1);
      
      // Record daily impressions
      const today = new Date().toISOString().split('T')[0]; // YYYY-MM-DD format
      
      // Get or create the daily impressions object for this device
      let dailyImpressions = this.device_daily_impressions.get(deviceId);
      
      // If dailyImpressions doesn't exist or is not an object, create it
      if (!dailyImpressions || typeof dailyImpressions !== 'object' || dailyImpressions === null) {
        dailyImpressions = {};
      }
      
      // Increment today's count
      const todayCount = dailyImpressions[today] || 0;
      dailyImpressions[today] = todayCount + 1;
      
      // Clean up old daily impressions (older than 30 days)
      const thirtyDaysAgo = new Date();
      thirtyDaysAgo.setDate(thirtyDaysAgo.getDate() - 30);
      const thirtyDaysAgoStr = thirtyDaysAgo.toISOString().split('T')[0];
      
      for (const dateKey in dailyImpressions) {
        if (dateKey < thirtyDaysAgoStr) {
          delete dailyImpressions[dateKey];
        }
      }
      
      // Update the map with the modified daily impressions
      this.device_daily_impressions.set(deviceId, dailyImpressions);
      
      // Log impression details for debugging
      console.log(`Recorded impression for ad ${this._id}, device ${deviceId}: total=${currentCount + 1}, today=${todayCount + 1}`);
      
      // Use markModified to tell Mongoose that these Maps have been updated
      this.markModified('device_impressions');
      this.markModified('device_daily_impressions');
    }
    
    return this.save();
  } catch (error) {
    console.error(`Error recording impression: ${error.message}`);
    console.error(error.stack);
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
      }
      
      const currentCount = this.device_clicks.get(deviceId) || 0;
      this.device_clicks.set(deviceId, currentCount + 1);
      
      // Mark as modified so Mongoose knows to save the changes
      this.markModified('device_clicks');
      
      console.log(`Recorded click for ad ${this._id}, device ${deviceId}: total=${currentCount + 1}`);
    }
    
    return this.save();
  } catch (error) {
    console.error(`Error recording click: ${error.message}`);
    console.error(error.stack);
    throw error;
  }
};

module.exports = mongoose.model('SponsoredAd', sponsoredAdSchema, 'sponsored_ads'); 