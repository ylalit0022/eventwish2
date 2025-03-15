/**
 * Analytics Model
 * 
 * Stores analytics data for ad impressions, clicks, and conversions.
 * Used for tracking user interactions with ads and detecting fraud.
 */

const mongoose = require('mongoose');
const Schema = mongoose.Schema;

const AnalyticsSchema = new Schema({
  // Ad reference
  adId: {
    type: Schema.Types.ObjectId,
    ref: 'AdMob',
    required: true,
    index: true
  },
  
  // Event type (impression, click, conversion)
  eventType: {
    type: String,
    enum: ['impression', 'click', 'conversion'],
    required: true,
    index: true
  },
  
  // User identification
  userId: {
    type: String,
    index: true
  },
  
  // Device information
  deviceId: {
    type: String,
    index: true
  },
  
  // IP address
  ip: {
    type: String,
    index: true
  },
  
  // Timestamp
  timestamp: {
    type: Date,
    default: Date.now,
    index: true
  },
  
  // Revenue information (for impressions and conversions)
  revenue: {
    type: Number,
    default: 0
  },
  
  // Device fingerprint
  deviceFingerprint: {
    type: String,
    index: true
  },
  
  // IP fingerprint
  ipFingerprint: {
    type: String,
    index: true
  },
  
  // Fraud detection results
  fraudScore: {
    type: Number,
    default: 0,
    index: true
  },
  
  isFraudulent: {
    type: Boolean,
    default: false,
    index: true
  },
  
  // Additional context data
  context: {
    type: Map,
    of: Schema.Types.Mixed,
    default: () => ({})
  },
  
  // Metadata
  metadata: {
    type: Map,
    of: Schema.Types.Mixed,
    default: () => ({})
  }
}, {
  timestamps: true,
  collection: 'analytics'
});

// Create indexes for common queries
AnalyticsSchema.index({ adId: 1, eventType: 1, timestamp: -1 });
AnalyticsSchema.index({ userId: 1, eventType: 1, timestamp: -1 });
AnalyticsSchema.index({ deviceId: 1, eventType: 1, timestamp: -1 });
AnalyticsSchema.index({ ip: 1, eventType: 1, timestamp: -1 });
AnalyticsSchema.index({ adId: 1, userId: 1, eventType: 1, timestamp: -1 });

// Static methods
AnalyticsSchema.statics.trackImpression = async function(data) {
  return this.create({
    ...data,
    eventType: 'impression'
  });
};

AnalyticsSchema.statics.trackClick = async function(data) {
  return this.create({
    ...data,
    eventType: 'click'
  });
};

AnalyticsSchema.statics.trackConversion = async function(data) {
  return this.create({
    ...data,
    eventType: 'conversion'
  });
};

AnalyticsSchema.statics.getAdStats = async function(adId, startDate, endDate) {
  const match = {
    adId: mongoose.Types.ObjectId(adId)
  };
  
  if (startDate || endDate) {
    match.timestamp = {};
    if (startDate) match.timestamp.$gte = startDate;
    if (endDate) match.timestamp.$lte = endDate;
  }
  
  return this.aggregate([
    { $match: match },
    {
      $group: {
        _id: '$eventType',
        count: { $sum: 1 },
        revenue: { $sum: '$revenue' }
      }
    },
    {
      $project: {
        _id: 0,
        eventType: '$_id',
        count: 1,
        revenue: 1
      }
    }
  ]);
};

// Create and export the model
const Analytics = mongoose.model('Analytics', AnalyticsSchema);
module.exports = Analytics; 