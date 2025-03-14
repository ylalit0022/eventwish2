const mongoose = require('mongoose');

/**
 * User Segment Schema
 * Represents a segment of users for targeting purposes
 */
const UserSegmentSchema = new mongoose.Schema({
  // Segment name
  name: {
    type: String,
    required: true,
    trim: true,
    unique: true
  },
  
  // Segment description
  description: {
    type: String,
    trim: true
  },
  
  // Segment type (demographic, behavioral, contextual, custom)
  type: {
    type: String,
    required: true,
    enum: ['demographic', 'behavioral', 'contextual', 'custom'],
    default: 'custom'
  },
  
  // Targeting criteria for this segment
  criteria: {
    type: Map,
    of: mongoose.Schema.Types.Mixed,
    default: {}
  },
  
  // Whether the segment is active
  isActive: {
    type: Boolean,
    default: true
  },
  
  // Estimated size of the segment (percentage of users)
  estimatedSize: {
    type: Number,
    min: 0,
    max: 100,
    default: 0
  },
  
  // Tags for categorizing segments
  tags: {
    type: [String],
    default: []
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

// Create model
const UserSegment = mongoose.model('UserSegment', UserSegmentSchema);

module.exports = { UserSegment }; 