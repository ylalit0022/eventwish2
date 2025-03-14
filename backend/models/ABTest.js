const mongoose = require('mongoose');

/**
 * A/B Test Variant Schema
 * Represents a single variant in an A/B test
 */
const VariantSchema = new mongoose.Schema({
  // Variant name (e.g., "Control", "Variant A", "Variant B")
  name: {
    type: String,
    required: true,
    trim: true
  },
  
  // Variant description
  description: {
    type: String,
    trim: true
  },
  
  // Variant weight (for traffic allocation)
  weight: {
    type: Number,
    required: true,
    min: 0,
    max: 100,
    default: 50
  },
  
  // Ad configuration for this variant
  adConfig: {
    // Reference to the ad to show for this variant
    adId: {
      type: mongoose.Schema.Types.ObjectId,
      ref: 'AdMob',
      required: true
    },
    
    // Custom parameters for this variant
    parameters: {
      type: Map,
      of: mongoose.Schema.Types.Mixed,
      default: {}
    }
  }
});

/**
 * Targeting Rule Schema
 * Defines rules for targeting specific users
 */
const TargetingRuleSchema = new mongoose.Schema({
  // Rule type (e.g., "device", "platform", "country", "custom")
  type: {
    type: String,
    required: true,
    enum: ['device', 'platform', 'country', 'language', 'appVersion', 'custom'],
    default: 'device'
  },
  
  // Operator for comparison (e.g., "equals", "contains", "startsWith", "regex")
  operator: {
    type: String,
    required: true,
    enum: ['equals', 'notEquals', 'contains', 'notContains', 'startsWith', 'endsWith', 'regex', 'greaterThan', 'lessThan'],
    default: 'equals'
  },
  
  // Value to compare against
  value: {
    type: mongoose.Schema.Types.Mixed,
    required: true
  },
  
  // Field to apply the rule to (for custom rules)
  field: {
    type: String,
    required: function() {
      return this.type === 'custom';
    }
  }
});

/**
 * A/B Test Schema
 * Represents an A/B test for ad configurations
 */
const ABTestSchema = new mongoose.Schema({
  // Test name
  name: {
    type: String,
    required: true,
    trim: true,
    unique: true
  },
  
  // Test description
  description: {
    type: String,
    trim: true
  },
  
  // Ad type this test applies to
  adType: {
    type: String,
    required: true,
    trim: true
  },
  
  // Test status (draft, active, paused, completed)
  status: {
    type: String,
    required: true,
    enum: ['draft', 'active', 'paused', 'completed'],
    default: 'draft'
  },
  
  // Start date
  startDate: {
    type: Date,
    default: Date.now
  },
  
  // End date (optional)
  endDate: {
    type: Date
  },
  
  // Targeting rules (all rules must match for a user to be included in the test)
  targetingRules: {
    type: [TargetingRuleSchema],
    default: []
  },
  
  // Test variants
  variants: {
    type: [VariantSchema],
    validate: {
      validator: function(variants) {
        // Must have at least 2 variants
        return variants.length >= 2;
      },
      message: 'A/B test must have at least 2 variants'
    }
  },
  
  // Traffic allocation percentage (0-100)
  trafficAllocation: {
    type: Number,
    required: true,
    min: 0,
    max: 100,
    default: 100
  },
  
  // Metrics to track for this test
  metrics: {
    type: [String],
    default: ['impressions', 'clicks', 'ctr', 'revenue']
  },
  
  // Results data
  results: {
    type: Map,
    of: mongoose.Schema.Types.Mixed,
    default: {}
  },
  
  // Created by
  createdBy: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'User'
  },
  
  // Updated by
  updatedBy: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'User'
  }
}, {
  timestamps: true
});

// Validate that variant weights sum to 100
ABTestSchema.pre('save', function(next) {
  const totalWeight = this.variants.reduce((sum, variant) => sum + variant.weight, 0);
  if (totalWeight !== 100) {
    return next(new Error('Variant weights must sum to 100'));
  }
  next();
});

// Create model
const ABTest = mongoose.model('ABTest', ABTestSchema);

module.exports = { ABTest }; 