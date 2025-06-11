const mongoose = require('mongoose');
const Schema = mongoose.Schema;

// Category visit schema (subdocument)
const CategoryVisitSchema = new Schema({
    category: {
        type: String,
        required: true,
        trim: true
    },
    visitDate: {
        type: Date,
        default: Date.now
    },
    visitCount: {
        type: Number,
        default: 1
    },
    source: {
        type: String,
        enum: ['direct', 'template'],
        default: 'direct'
    }
});

// Subdocument for referral/invite system
const ReferralSchema = new Schema({
    referredBy: { 
        type: String, 
        default: null 
    },
    referralCode: { 
        type: String, 
        default: null 
    }
});

// Subdocument for subscription
const SubscriptionSchema = new Schema({
    isActive: { 
        type: Boolean, 
        default: false 
    },
    plan: {
        type: String,
        enum: ['MONTHLY', 'QUARTERLY', 'HALF_YEARLY', 'YEARLY'],
        default: null
    },
    startedAt: { 
        type: Date, 
        default: null 
    },
    expiresAt: { 
        type: Date, 
        default: null 
    }
});

// User schema
const UserSchema = new Schema({
    uid: { 
        type: String, 
        unique: true, 
        sparse: true,
        index: true // Add index for efficient queries
    }, // Firebase UID (primary identifier for authenticated users)
    deviceId: {
        type: String,
        required: true,
        unique: true,
        trim: true,
        index: true // Add index for efficient queries
    },
    displayName: { 
        type: String 
    },
    email: { 
        type: String 
    },
    profilePhoto: { 
        type: String 
    },
    lastOnline: {
        type: Date,
        default: Date.now
    },
    created: {
        type: Date,
        default: Date.now
    },
    subscription: SubscriptionSchema,
    adsAllowed: { 
        type: Boolean, 
        default: true 
    }, // false if premium/no-ads user
    pushPreferences: {
        allowFestivalPush: { 
            type: Boolean, 
            default: true 
        },
        allowPersonalPush: { 
            type: Boolean, 
            default: true 
        }
    },
    topicSubscriptions: [{ 
        type: String 
    }], // e.g., ['diwali', 'holi']
    preferredTheme: { 
        type: String, 
        default: 'light' 
    },
    preferredLanguage: { 
        type: String, 
        default: 'en' 
    },
    timezone: { 
        type: String, 
        default: 'Asia/Kolkata' 
    },
    muteNotificationsUntil: { 
        type: Date, 
        default: null 
    },
    referredBy: ReferralSchema,
    referralCode: { 
        type: String 
    },
    recentTemplatesUsed: [{ 
        type: mongoose.Schema.Types.ObjectId, 
        ref: 'Template' 
    }],
    favorites: [{ 
        type: mongoose.Schema.Types.ObjectId, 
        ref: 'Template' 
    }],
    likes: [{ 
        type: mongoose.Schema.Types.ObjectId, 
        ref: 'Template' 
    }],
    categories: [CategoryVisitSchema],
    lastActiveTemplate: {
        type: mongoose.Schema.Types.ObjectId,
        ref: 'Template',
        default: null
    },
    lastActionOnTemplate: {
        type: String,
        enum: ['VIEW', 'LIKE', 'FAV', 'SHARE', null],
        default: null
    },
    engagementLog: [{
        action: { 
            type: String, 
            enum: ['SHARE', 'VIEW', 'LIKE', 'FAV'], 
            required: true 
        },
        templateId: { 
            type: mongoose.Schema.Types.ObjectId, 
            ref: 'Template' 
        },
        timestamp: { 
            type: Date, 
            default: Date.now 
        }
    }]
}, {
    timestamps: true // Automatically add createdAt and updatedAt fields
});

// Add a method to update lastOnline
UserSchema.methods.updateLastOnline = function() {
    this.lastOnline = Date.now();
    return this.save();
};

// Add a method to record a category visit
UserSchema.methods.visitCategory = function(categoryName, source = 'direct') {
    // Check if we already have this category in the list
    const existingCategory = this.categories.find(c => 
        c.category.toLowerCase() === categoryName.toLowerCase()
    );
    
    if (existingCategory) {
        // Update the visit date and increment counter for existing category
        existingCategory.visitDate = Date.now();
        existingCategory.visitCount += 1;
        existingCategory.source = source; // Update the latest source
    } else {
        // Add new category visit
        this.categories.push({
            category: categoryName,
            visitDate: Date.now(),
            visitCount: 1,
            source: source
        });
    }
    
    // Also update lastOnline time
    this.lastOnline = Date.now();
    
    return this.save();
};

// Add a method to record a category visit from template interaction
UserSchema.methods.visitCategoryFromTemplate = function(categoryName, templateId) {
    return this.visitCategory(categoryName, 'template');
};

// Add a method to set the last active template
UserSchema.methods.setLastActiveTemplate = function(templateId, action = null) {
    this.lastActiveTemplate = templateId;
    this.lastActionOnTemplate = action;
    this.lastOnline = Date.now();
    return this.save();
};

module.exports = mongoose.model('User', UserSchema); 