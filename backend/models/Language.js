const mongoose = require('mongoose');

const languageSchema = new mongoose.Schema({
    code: {
        type: String,
        required: true,
        unique: true,
        trim: true,
        lowercase: true
    },
    name: {
        type: String,
        required: true,
        trim: true
    },
    nativeName: {
        type: String,
        required: true,
        trim: true
    },
    isRTL: {
        type: Boolean,
        default: false
    },
    isActive: {
        type: Boolean,
        default: true
    },
    displayOrder: {
        type: Number,
        default: 0
    }
}, {
    timestamps: true,
    toJSON: { virtuals: true },
    toObject: { virtuals: true }
});

// Index for faster lookups
languageSchema.index({ code: 1 });
languageSchema.index({ isActive: 1 });

module.exports = mongoose.model('Language', languageSchema, 'languages'); 