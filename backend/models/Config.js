/**
 * Config Model
 * 
 * Used for storing application-wide configuration settings
 * that can be modified through the admin panel.
 */

const mongoose = require('mongoose');
const Schema = mongoose.Schema;

const ConfigSchema = new Schema({
  key: { 
    type: String, 
    required: true, 
    unique: true,
    index: true
  },
  value: { 
    type: mongoose.Schema.Types.Mixed, 
    required: true 
  },
  description: { 
    type: String 
  },
  lastUpdated: { 
    type: Date, 
    default: Date.now 
  },
  updatedBy: { 
    type: String 
  }
}, {
  timestamps: true
});

module.exports = mongoose.model('Config', ConfigSchema); 