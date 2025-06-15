const mongoose = require('mongoose');

const festivalSchema = new mongoose.Schema({
    name: {
        type: String,
        required: true
    },
    slug: {
        type: String,
        unique: true,
        trim: true
    },
    date: {
        type: Date,
        required: true
    },
    startDate: {
        type: Date // Useful for multi-day festivals
    },
    endDate: {
        type: Date
    },
    description: {
        type: String,
        default: ''
    },
    category: {
        type: String,
        required: true
    },
    categoryIcon: {
        type: mongoose.Schema.Types.ObjectId,
        ref: 'CategoryIcon'
    },
    imageUrl: {
        type: String,
        default: ''
    },
    bannerUrl: {
        type: String,
        default: ''
    },
    templates: [{
        type: mongoose.Schema.Types.ObjectId,
        ref: 'Template'
    }],
    isActive: {
        type: Boolean,
        default: true
    },
    status: {
        type: String,
        enum: ['UPCOMING', 'ONGOING', 'ENDED'],
        default: 'UPCOMING'
    },
    priority: {
        type: Number,
        default: 0 // Can be used to sort upcoming festivals
    },
    deepLink: {
        type: String,
        default: ''
    },
    pushEnabled: {
        type: Boolean,
        default: true
    },
    personalizedPushTemplate: {
        type: String,
        default: 'Hey {{name}}, check out Diwali wishes waiting for you!'
    },
    notifyCountdown: {
        type: Boolean,
        default: false
    },
    countdownDays: {
        type: Number,
        default: 3 // How many days before to show countdown like 3, 2, 1, Today
    },
    localizedNames: {
        type: Map,
        of: String,
        default: {} // e.g., { hi: 'दिवाली', en: 'Diwali' }
    },
    themeColors: {
        type: Map,
        of: String,
        default: {} // e.g., { primary: "#FFC107", background: "#FFF8E1" }
    }
}, {
    timestamps: true
});

module.exports = mongoose.model('Festival', festivalSchema, 'festivals');