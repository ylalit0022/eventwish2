const mongoose = require('mongoose');

const deviceTokenSchema = new mongoose.Schema({
  token: {
    type: String,
    required: true,
    unique: true,
    trim: true
  },
  createdAt: {
    type: Date,
    default: Date.now
  },
  lastUsed: {
    type: Date,
    default: Date.now
  },
  platform: {
    type: String,
    enum: ['android', 'ios', 'web'],
    default: 'android'
  },
  active: {
    type: Boolean,
    default: true
  }
});

// Create index for token field
deviceTokenSchema.index({ token: 1 }, { unique: true });

// Create index for active field for faster queries
deviceTokenSchema.index({ active: 1 });

module.exports = mongoose.model('DeviceToken', deviceTokenSchema); 