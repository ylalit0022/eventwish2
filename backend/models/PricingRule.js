const mongoose = require('mongoose');

/**
 * PricingRule schema - Defines pricing rules for subscription plans
 * Supports global pricing, user segment pricing, experiment-based pricing, and time-limited offers
 */
const pricingRuleSchema = new mongoose.Schema({
  planLevel: {
    type: String,
    enum: ['BASIC', 'PREMIUM', 'PRO'],
    required: true
  },
  price: { 
    type: Number, 
    required: true 
  }, // Base price (before discount)
  discountPercentage: { 
    type: Number, 
    default: 0 
  }, // e.g. 30 for 30% off
  discountedPrice: { 
    type: Number 
  }, // Optional: pre-calculated
  ruleType: {
    type: String,
    enum: ['GLOBAL', 'USER_SEGMENT', 'EXPERIMENT', 'TIME_BASED'],
    default: 'GLOBAL'
  },
  targetSegment: { 
    type: String, 
    default: '' 
  }, // e.g. "INACTIVE_USERS"
  experimentTag: { 
    type: String, 
    default: '' 
  }, // e.g. "A/B_TEST_01"
  active: { 
    type: Boolean, 
    default: true 
  },
  description: { 
    type: String, 
    default: '' 
  },
  expiresAt: { 
    type: Date, 
    default: null 
  },
  createdBy: { 
    type: String, 
    default: 'system' 
  },
  priority: {
    type: Number,
    default: 0 // Higher number = higher priority when multiple rules apply
  },
  currency: {
    type: String,
    default: 'USD'
  },
  billingCycle: {
    type: String,
    enum: ['MONTHLY', 'QUARTERLY', 'HALF_YEARLY', 'YEARLY'],
    default: 'MONTHLY'
  },
  regions: [{
    type: String // Region codes where this pricing rule applies
  }],
  minUserAge: {
    type: Number, // Minimum user account age in days
    default: 0
  }
}, {
  timestamps: true
});

// Pre-save hook to calculate discountedPrice if not provided
pricingRuleSchema.pre('save', function(next) {
  if (this.price && this.discountPercentage && !this.discountedPrice) {
    this.discountedPrice = this.price * (1 - this.discountPercentage / 100);
  }
  next();
});

// Method to check if rule is currently valid
pricingRuleSchema.methods.isValid = function() {
  if (!this.active) return false;
  
  if (this.expiresAt && new Date() > this.expiresAt) {
    return false;
  }
  
  return true;
};

// Method to get final price after applying discount
pricingRuleSchema.methods.getFinalPrice = function() {
  return this.discountedPrice || this.price;
};

// Static method to find applicable rules for a user segment
pricingRuleSchema.statics.findForSegment = function(segment) {
  return this.find({
    active: true,
    $or: [
      { ruleType: 'GLOBAL' },
      { ruleType: 'USER_SEGMENT', targetSegment: segment }
    ],
    $or: [
      { expiresAt: null },
      { expiresAt: { $gt: new Date() } }
    ]
  }).sort({ priority: -1 });
};

// Static method to find rules applicable to a specific user
pricingRuleSchema.statics.findForUser = function(user, planLevel, billingCycle) {
  // Calculate user account age in days
  const userAccountAge = Math.floor((Date.now() - user.created) / (1000 * 60 * 60 * 24));
  
  // Determine user segment based on activity and subscription history
  let userSegment = 'NEW_USER';
  
  if (user.subscriptionHistory && user.subscriptionHistory.length > 0) {
    userSegment = 'RETURNING_USER';
  } else if (user.lastActive && ((Date.now() - user.lastActive) > (30 * 24 * 60 * 60 * 1000))) {
    userSegment = 'INACTIVE_USER';
  }
  
  // Find applicable pricing rules
  return this.find({
    active: true,
    planLevel: planLevel,
    billingCycle: billingCycle,
    $or: [
      { ruleType: 'GLOBAL' },
      { ruleType: 'USER_SEGMENT', targetSegment: userSegment },
      { ruleType: 'EXPERIMENT', experimentTag: { $in: user.experimentTags || [] } }
    ],
    $or: [
      { expiresAt: null },
      { expiresAt: { $gt: new Date() } }
    ],
    minUserAge: { $lte: userAccountAge }
  }).sort({ priority: -1 });
};

module.exports = mongoose.model('PricingRule', pricingRuleSchema); 