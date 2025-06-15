const mongoose = require('mongoose');

const sharedWishSchema = new mongoose.Schema({
    shortCode: { type: String, required: true, unique: true },
    template: { type: mongoose.Schema.Types.ObjectId, ref: 'Template', required: true },
    
    title: { type: String, default: 'EventWish Greeting' },
    description: { type: String, default: 'A special wish for you' },
    recipientName: { type: String, required: true },
    senderName: { type: String, required: true },
    
    customizedHtml: { type: String, default: '' },
    cssContent: { type: String, default: '' },
    jsContent: { type: String, default: '' },
    
    previewUrl: { type: String },
    deeplink: { type: String, default: '' }, // deep link for re-open / share

    sharedVia: {
        type: String,
        enum: ['LINK', 'WHATSAPP', 'FACEBOOK', 'TWITTER', 'INSTAGRAM', 'EMAIL', 'SMS', 'OTHER'],
        default: 'LINK'
    },

    views: { type: Number, default: 0 },
    uniqueViews: { type: Number, default: 0 },
    viewerIps: { type: [String], default: [] },

    shareCount: { type: Number, default: 0 },
    shareHistory: [{
        platform: {
            type: String,
            enum: ['LINK', 'WHATSAPP', 'FACEBOOK', 'TWITTER', 'INSTAGRAM', 'EMAIL', 'SMS', 'OTHER']
        },
        timestamp: { type: Date, default: Date.now }
    }],

    lastSharedAt: { type: Date, default: Date.now },

    conversionSource: { type: String, default: null },
    referrer: { type: String, default: null },
    deviceInfo: { type: String, default: null },
    
    viewerEngagement: [{
        userId: { type: mongoose.Schema.Types.ObjectId, ref: 'User' },
        action: { type: String, enum: ['VIEWED', 'LIKED', 'FAVORITED', 'SHARED'] },
        timestamp: { type: Date, default: Date.now }
    }],

    isPremiumShared: { type: Boolean, default: false }, // For analytics: premium template shared?

}, {
    timestamps: true,
    toJSON: { getters: true }
});

module.exports = mongoose.model('SharedWish', sharedWishSchema, 'sharedwishes');
