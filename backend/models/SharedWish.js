const mongoose = require('mongoose');

const sharedWishSchema = new mongoose.Schema({
    shortCode: {
        type: String,
        required: true,
        unique: true
    },
    templateId: {
        type: mongoose.Schema.Types.ObjectId,
        ref: 'Template',
        required: true
    },
    recipientName: {
        type: String,
        required: true
    },
    senderName: {
        type: String,
        required: true
    },
    customizedHtml: {
        type: String,
        required: true
    },
    cssContent: {
        type: String,
        default: ''
    },
    jsContent: {
        type: String,
        default: ''
    },
    sharedVia: {
        type: String,
        enum: ['LINK', 'WHATSAPP', 'OTHER'],
        default: 'LINK'
    },
    views: {
        type: Number,
        default: 0
    },
    lastSharedAt: {
        type: Date,
        default: Date.now
    }
}, {
    timestamps: true
});

module.exports = mongoose.model('SharedWish', sharedWishSchema, 'sharedwishes');
