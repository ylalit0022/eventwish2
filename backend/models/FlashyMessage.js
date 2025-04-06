const mongoose = require('mongoose');

const flashyMessageSchema = new mongoose.Schema({
  title: {
    type: String,
    required: true,
    trim: true
  },
  message: {
    type: String,
    required: true,
    trim: true
  },
  active: {
    type: Boolean,
    default: true
  },
  startDate: {
    type: Date,
    default: Date.now
  },
  endDate: {
    type: Date,
    default: () => new Date(Date.now() + 7 * 24 * 60 * 60 * 1000) // 7 days from now
  },
  targetAudience: {
    type: String,
    enum: ['all', 'new_users', 'inactive_users', 'active_users'],
    default: 'all'
  },
  priority: {
    type: Number,
    default: 1,
    min: 1,
    max: 10
  },
  createdAt: {
    type: Date,
    default: Date.now
  },
  createdBy: {
    type: String,
    default: 'system'
  }
});

// Create index for active and date fields for faster queries
flashyMessageSchema.index({ active: 1, startDate: 1, endDate: 1 });

module.exports = mongoose.model('FlashyMessage', flashyMessageSchema); 