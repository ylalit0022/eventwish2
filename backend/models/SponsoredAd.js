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
    default: {}
  },
  device_clicks: {
    type: Map,
    of: Number,
    default: {}
  },
  device_daily_impressions: {
    type: Map,
    of: Map,
    default: {}
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
  
  // If no device ID provided, just return all ads sorted by priority
  if (!deviceId) {
    return ads.sort((a, b) => b.priority - a.priority);
  }
  
  // Filter ads based on frequency capping
  const today = new Date().toISOString().split('T')[0]; // YYYY-MM-DD format
  
  // Get detailed impression data for analytics
  const adsWithMetrics = ads.map(ad => {
    // Get impression metrics for this device
    const totalImpressions = ad.device_impressions.get(deviceId) || 0;
    
    // Get daily impressions for today
    const dailyImpressions = ad.device_daily_impressions.get(deviceId);
    const todayImpressions = dailyImpressions ? (dailyImpressions.get(today) || 0) : 0;
    
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
  });
  
  // Filter ads based on frequency capping
  const filteredAds = adsWithMetrics.filter(ad => {
    // Check total frequency cap
    if (ad.frequency_cap > 0) {
      const deviceImpressions = ad.device_impressions.get(deviceId) || 0;
      if (deviceImpressions >= ad.frequency_cap) {
        return false; // Ad has reached total impression cap for this device
      }
    }
    
    // Check daily frequency cap
    if (ad.daily_frequency_cap > 0) {
      const dailyImpressions = ad.device_daily_impressions.get(deviceId);
      if (dailyImpressions) {
        const todayImpressions = dailyImpressions.get(today) || 0;
        if (todayImpressions >= ad.daily_frequency_cap) {
          return false; // Ad has reached daily impression cap for this device
        }
      }
    }
    
    return true; // Ad is under frequency caps
  });
  
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
  return adsWithWeightedScores.sort((a, b) => b.weightedScore - a.weightedScore);
};

// Methods
sponsoredAdSchema.methods.recordImpression = async function(deviceId = null) {
  try {
    this.impression_count += 1;
    
    if (deviceId) {
      // Record total impressions for this device
      const currentCount = this.device_impressions.get(deviceId) || 0;
      this.device_impressions.set(deviceId, currentCount + 1);
      
      // Record daily impressions with better date handling
      const today = new Date().toISOString().split('T')[0]; // YYYY-MM-DD format
      
      // Check if the device daily impressions map exists
      if (!this.device_daily_impressions) {
        this.device_daily_impressions = new Map();
      }
      
      // Initialize daily impressions map if it doesn't exist for this device
      if (!this.device_daily_impressions.has(deviceId)) {
        this.device_daily_impressions.set(deviceId, new Map());
      }
      
      // Get the daily impressions map for this device
      const dailyImpressions = this.device_daily_impressions.get(deviceId);
      
      // Increment today's count
      const todayCount = dailyImpressions.get(today) || 0;
      dailyImpressions.set(today, todayCount + 1);
      
      // Clean up old daily impressions (older than 30 days)
      const thirtyDaysAgo = new Date();
      thirtyDaysAgo.setDate(thirtyDaysAgo.getDate() - 30);
      const thirtyDaysAgoStr = thirtyDaysAgo.toISOString().split('T')[0];
      
      for (const [dateKey] of dailyImpressions) {
        if (dateKey < thirtyDaysAgoStr) {
          dailyImpressions.delete(dateKey);
        }
      }
      
      // Log impression details for debugging
      console.log(`Recorded impression for ad ${this._id}, device ${deviceId}: total=${currentCount + 1}, today=${todayCount + 1}`);
    }
    
    return this.save();
  } catch (error) {
    console.error(`Error recording impression: ${error.message}`);
    // Re-throw for caller to handle
    throw error;
  }
};

sponsoredAdSchema.methods.recordClick = async function(deviceId = null) {
  this.click_count += 1;
  
  if (deviceId) {
    const currentCount = this.device_clicks.get(deviceId) || 0;
    this.device_clicks.set(deviceId, currentCount + 1);
  }
  
  return this.save();
};

module.exports = mongoose.model('SponsoredAd', sponsoredAdSchema, 'sponsored_ads'); 