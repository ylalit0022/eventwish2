/**
 * SponsoredAd Model
 * 
 * This module defines the schema for sponsored banner ads
 * that can be displayed in the application.
 */

const mongoose = require('mongoose');

const sponsoredAdSchema = new mongoose.Schema({
  uid: { type: mongoose.Schema.Types.ObjectId, ref: 'User', required: true }, 
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
sponsoredAdSchema.statics.getActiveAds = async function(location = null) {
  const now = new Date();
  const query = {
    status: true,
    start_date: { $lte: now },
    end_date: { $gte: now }
  };
  
  if (location) {
    query.location = location;
  }
  
  return this.find(query).sort({ priority: -1 });
};

/**
 * Get active ads for rotation, excluding specified IDs
 * @param {String} location - The location to filter by
 * @param {Array} excludeIds - Array of ad IDs to exclude
 * @returns {Promise<Array>} - List of matching ads
 */
sponsoredAdSchema.statics.getAdsForRotation = async function(location = null, excludeIds = []) {
  const now = new Date();
  const query = {
    status: true,
    start_date: { $lte: now },
    end_date: { $gte: now }
  };
  
  if (location) {
    query.location = location;
  }
  
  if (excludeIds && excludeIds.length > 0) {
    // Convert string IDs to ObjectIds if needed
    const objectIds = excludeIds.map(id => {
      try {
        return mongoose.Types.ObjectId(id);
      } catch (e) {
        return id; // Keep as is if not valid ObjectId
      }
    });
    
    query._id = { $nin: objectIds };
  }
  
  return this.find(query).sort({ priority: -1 });
};

/**
 * Get ads with fair distribution based on impressions
 * @param {String} location - The location to filter by
 * @param {Number} limit - Maximum number of ads to return
 * @returns {Promise<Array>} - List of fairly distributed ads
 */
sponsoredAdSchema.statics.getFairDistributedAds = async function(location = null, limit = 10) {
  // Get active ads
  const ads = await this.getActiveAds(location);
  
  // Apply fair distribution algorithm
  return this.applyFairDistribution(ads, limit);
};

/**
 * Apply fair distribution algorithm based on priority and impressions
 * @param {Array} ads - List of ads to distribute
 * @param {Number} limit - Maximum number of ads to return
 * @returns {Array} - Fairly distributed ads
 */
sponsoredAdSchema.statics.applyFairDistribution = function(ads, limit) {
  if (!ads || ads.length === 0) {
    return [];
  }
  
  // Calculate weights based on priority and impression count
  const adsWithWeights = ads.map(ad => {
    // Higher priority and fewer impressions = higher weight
    const impressionFactor = 1.0 / (1 + Math.log(1 + ad.impression_count));
    const weight = ad.priority * impressionFactor;
    
    return {
      ad,
      weight
    };
  });
  
  // Sort by weight (descending)
  adsWithWeights.sort((a, b) => b.weight - a.weight);
  
  // Return ads up to limit
  return adsWithWeights.slice(0, limit).map(item => item.ad);
};

// Methods
sponsoredAdSchema.methods.recordImpression = async function(deviceId = null) {
  this.impression_count += 1;
  
  if (deviceId) {
    const currentCount = this.device_impressions.get(deviceId) || 0;
    this.device_impressions.set(deviceId, currentCount + 1);
  }
  
  return this.save();
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