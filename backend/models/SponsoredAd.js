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