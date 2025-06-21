const mongoose = require('mongoose');

const PushNotificationSchema = new mongoose.Schema({
  title: {
    type: String,
    required: true,
    trim: true
  },
  body: {
    type: String,
    required: true,
    trim: true
  },
  imageUrl: {
    type: String,
    trim: true,
    default: null
  },
  data: {
    type: Map,
    of: String,
    default: {}
  },
  // Deep link for in-app navigation
  deepLink: {
    type: String,
    trim: true,
    default: null
  },
  type: {
    type: String,
    enum: ['BULK', 'TOPIC', 'PERSONALIZED'],
    required: true
  },
  // Notification category for analytics and filtering
  notificationType: {
    type: String,
    enum: ['GENERAL', 'PROMOTIONAL', 'TRANSACTIONAL', 'INACTIVITY', 'FESTIVAL'],
    default: 'GENERAL'
  },
  // Personalization fields
  personalizationKeys: {
    type: [String],
    default: []
  },
  // Flag to indicate if this is a template notification
  isTemplate: {
    type: Boolean,
    default: false
  },
  topic: {
    type: String,
    trim: true,
    default: null
  },
  targetUserIds: [{
    type: mongoose.Schema.Types.ObjectId,
    ref: 'User'
  }],
  status: {
    type: String,
    enum: ['DRAFT', 'SCHEDULED', 'SENDING', 'SENT', 'FAILED'],
    default: 'DRAFT'
  },
  scheduledFor: {
    type: Date,
    default: null
  },
  sentAt: {
    type: Date,
    default: null
  },
  // Last time this notification was used for inactivity notifications
  lastUsedForInactivity: {
    type: Date,
    default: null
  },
  stats: {
    total: {
      type: Number,
      default: 0
    },
    success: {
      type: Number,
      default: 0
    },
    failure: {
      type: Number,
      default: 0
    },
    // Track opens if using a tracking pixel or deeplink analytics
    opens: {
      type: Number,
      default: 0
    },
    // Track clicks if using deeplinks
    clicks: {
      type: Number,
      default: 0
    }
  },
  // Error log for tracking failures
  errorLog: [{
    userId: {
      type: mongoose.Schema.Types.ObjectId,
      ref: 'User'
    },
    error: {
      type: String
    },
    timestamp: {
      type: Date,
      default: Date.now
    },
    fcmError: {
      type: Object
    }
  }],
  createdBy: {
    type: String,
    required: true
  },
  createdAt: {
    type: Date,
    default: Date.now
  },
  updatedAt: {
    type: Date,
    default: Date.now
  }
});

// Update the 'updatedAt' field on save
PushNotificationSchema.pre('save', function(next) {
  this.updatedAt = Date.now();
  next();
});

module.exports = mongoose.model('PushNotification', PushNotificationSchema); 