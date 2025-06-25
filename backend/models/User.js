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
        enum: ['MONTHLY', 'QUARTERLY', 'HALF_YEARLY', 'YEARLY', 'NONE'],
        default: 'NONE'
    },
    planLevel: {
        type: String,
        enum: ['BASIC', 'PREMIUM', 'PRO', 'NONE'],
        default: 'NONE'
    },

    startedAt: { 
        type: Date, 
        default: null 
    },
    expiresAt: { 
        type: Date, 
        default: null 
    },
    features: {
        allowNameEdit: { type: Boolean, default: false },
        allowPhotoEdit: { type: Boolean, default: false },
        allowAdFree: { type: Boolean, default: false },
        allowDraftSave: { type: Boolean, default: false },
        allowAnalytics: { type: Boolean, default: false },
        allowMusic: { type: Boolean, default: false },
        allowThemeCustomization: { type: Boolean, default: false },
        allowPrioritySupport: { type: Boolean, default: false }
    },
    appliedPricingRuleId: {
        type: mongoose.Schema.Types.ObjectId,
        ref: 'PricingRule',
        default: null
    },
    basePrice: {
        type: Number,
        default: 0
    },
    finalPrice: {
        type: Number,
        default: 0
    },
    currency: {
        type: String,
        default: 'INR'
    }
});

// Subscription offer schema
const SubscriptionOfferSchema = new Schema({
    planAssigned: { 
        type: String, 
        enum: ['BASIC', 'PREMIUM', 'PRO', 'NONE'], 
        default: 'NONE' 
    },
    originalPrice: { 
        type: Number, 
        default: 0 
    },
    discountedPrice: { 
        type: Number, 
        default: 0 
    },
    discountPercentage: { 
        type: Number, 
        default: 0 
    },
    offerEndsAt: { 
        type: Date 
    },
    assignedAt: { 
        type: Date, 
        default: Date.now 
    },
    experimentTag: { 
        type: String, 
        default: '' 
    }
});

// Subscription history schema
const SubscriptionHistorySchema = new Schema({
    plan: { 
        type: String, 
        enum: ['BASIC', 'PREMIUM', 'PRO'], 
        required: true 
    },
    startedAt: { 
        type: Date, 
        required: true 
    },
    endedAt: { 
        type: Date, 
        required: true 
    }
});

// Ignored template schema
const IgnoredTemplateSchema = new Schema({
    templateId: { 
        type: mongoose.Schema.Types.ObjectId, 
        ref: 'Template' 
    },
    views: { 
        type: Number, 
        default: 1 
    },
    lastViewed: { 
        type: Date 
    },
    lastIgnoredScore: { 
        type: Number, 
        default: 1 
    }
});

// Draft schema
const DraftSchema = new Schema({
    templateId: { 
        type: mongoose.Schema.Types.ObjectId, 
        ref: 'Template' 
    },
    html: { 
        type: String 
    },
    lastUpdated: { 
        type: Date, 
        default: Date.now 
    }
});

// AI Usage Tracking
const AIUsageSchema = new Schema({
    monthlyGenerationCount: { 
        type: Number, 
        default: 0 
    },
    lastGenerationAt: { 
        type: Date 
    },
    quota: { 
        type: Number, 
        default: 5 
    },
    recentPrompts: [{ 
        type: String 
    }],
    stylePreferences: [{ 
        type: String 
    }]
});

// Blocking information schema (subdocument)
const BlockInfoSchema = new Schema({
    blockedBy: {
        type: String,  // Admin UID who blocked the user
        required: true
    },
    reason: {
        type: String,
        default: ''
    },
    blockedAt: {
        type: Date,
        default: Date.now
    },
    blockExpiresAt: {
        type: Date,
        default: null  // null means indefinite block
    },
    notes: {
        type: String,
        default: ''
    }
});

// 🔔 FCM Token schema
const FcmTokenSchema = new Schema({
    token: { 
        type: String, 
        required: true 
    },
    platform: { 
        type: String, 
        enum: ['android', 'ios', 'web'], 
        default: 'android' 
    },
    subscribedTopics: [{ 
        type: String 
    }],
    updatedAt: { 
        type: Date, 
        default: Date.now 
    }
});

// User schema
const UserSchema = new Schema({
    uid: { 
        type: String, 
        required: true,
        unique: true,
        index: true // Add index for efficient queries
    }, // Firebase UID (primary identifier for authenticated users)
    deviceId: {
        type: String,
        required: false,
        unique: true,
        sparse: true, // Allow null/undefined values for deviceId
        trim: true,
        index: true // Add index for efficient queries
    },
    deviceModel: {
        type: String
    },
    deviceName: {
        type: String
    },
    appVersion: {
        type: String
    },
    osVersion: {
        type: String
    },
    loginTimestamp: {
        type: Date,
        default: Date.now
    },
    // Active sessions on different devices
    activeSessions: {
        type: Map,
        of: {
            deviceId: String,
            deviceModel: String,
            deviceName: String,
            appVersion: String,
            osVersion: String,
            loginTimestamp: Date,
            lastActiveTimestamp: Date
        },
        default: {}
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

    // Subscription & Access
    subscription: SubscriptionSchema,
    subscriptionOffer: SubscriptionOfferSchema,
    subscriptionHistory: [SubscriptionHistorySchema],
    lastSubscriptionEndedAt: { 
        type: Date 
    },
    monthsWithoutPayment: { 
        type: Number, 
        default: 0 
    },
    adsAllowed: { 
        type: Boolean, 
        default: true 
    }, // false if premium/no-ads user

    // AI
    aiUsage: AIUsageSchema,

    // Activity & Status
    lastOnline: {
        type: Date,
        default: Date.now
    },
    lastActive: {
        type: Date,
        default: Date.now
    },
    lastInactivityNotification: {
        type: Date,
        default: null
    },
    created: {
        type: Date,
        default: Date.now
    },
    isBlocked: {
        type: Boolean,
        default: false,
        index: true // Add index for filtering blocked users
    },
    blockInfo: {
        type: BlockInfoSchema,
        default: null
    },
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
    fcmTokens: [FcmTokenSchema], // Array of FCM tokens for push notifications
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
    // Engagement
    likes: [{ 
        type: mongoose.Schema.Types.ObjectId, 
        ref: 'Template' 
    }],
    favorites: [{ 
        type: mongoose.Schema.Types.ObjectId, 
        ref: 'Template' 
    }],
    recentTemplatesUsed: [{ 
        type: mongoose.Schema.Types.ObjectId, 
        ref: 'Template' 
    }],
    
    lastActiveTemplate: {
        type: mongoose.Schema.Types.ObjectId,
        ref: 'Template',
        default: null
    },
    lastActionOnTemplate: {
        type: String,
        enum: ['VIEW', 'LIKE', 'FAV', 'FAVORITE', 'SHARE', 'UNLIKE', 'UNFAV', 'UNFAVORITE', null],
        default: null
    },
    engagementLog: [{
        action: { 
            type: String, 
            enum: ['SHARE', 'VIEW', 'LIKE', 'FAV', 'FAVORITE', 'UNLIKE', 'UNFAV', 'UNFAVORITE'], 
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
    }],
    templateAffinity: [{ 
        tag: String, 
        score: { 
            type: Number, 
            default: 1 
        } 
    }],
    categories: [CategoryVisitSchema],
    ignoredTemplates: [IgnoredTemplateSchema],
    viewedTemplatesMap: { 
        type: Map, 
        of: Date, 
        default: {} 
    },
    drafts: [DraftSchema],

    // Feed
    cachedHomeFeed: [{
        type: { 
            type: String, 
            required: true 
        },
        templateIds: [{ 
            type: mongoose.Schema.Types.ObjectId, 
            ref: 'Template' 
        }]
    }],
    homeFeedLastGeneratedAt: { 
        type: Date 
    },

    // Notifications & Preferences
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
    fcmTokens: [FcmTokenSchema], // Array of FCM tokens for push notifications
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

    // Referrals
    referredBy: ReferralSchema,
    referralCode: { 
        type: String 
    }
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

// Add method to block a user
UserSchema.methods.blockUser = async function(adminUid, reason, expiresAt, notes) {
    this.isBlocked = true;
    this.blockInfo = {
        blockedBy: adminUid,
        reason: reason || 'Blocked by administrator',
        blockedAt: new Date(),
        blockExpiresAt: expiresAt || null,
        notes: notes || ''
    };
    
    await this.save();
    return this;
};

// Add method to unblock a user
UserSchema.methods.unblockUser = async function() {
    this.isBlocked = false;
    this.blockInfo = null;
    
    await this.save();
    return this;
};

// Add method to check if user is currently blocked
UserSchema.methods.isCurrentlyBlocked = function() {
    if (!this.isBlocked) return false;
    
    // If block has an expiration time and that time has passed, user is no longer blocked
    if (this.blockInfo && this.blockInfo.blockExpiresAt && 
        new Date() > this.blockInfo.blockExpiresAt) {
        // Auto-unblock
        this.isBlocked = false;
        return false;
    }
    
    return true;
};

// Add method to add or update FCM token
UserSchema.methods.addFcmToken = function(token, platform = 'android') {
    // Check if token already exists
    const tokenIndex = this.fcmTokens.findIndex(t => t.token === token);
    
    if (tokenIndex !== -1) {
        // Update existing token
        this.fcmTokens[tokenIndex].platform = platform;
        this.fcmTokens[tokenIndex].updatedAt = Date.now();
    } else {
        // Add new token
        this.fcmTokens.push({
            token,
            platform,
            subscribedTopics: [],
            updatedAt: Date.now()
        });
    }
    
    return this.save();
};

// Add method to remove FCM token
UserSchema.methods.removeFcmToken = function(token) {
    this.fcmTokens = this.fcmTokens.filter(t => t.token !== token);
    return this.save();
};

// Add method to subscribe token to topic
UserSchema.methods.subscribeTokenToTopic = function(token, topic) {
    const tokenObj = this.fcmTokens.find(t => t.token === token);
    
    if (tokenObj && !tokenObj.subscribedTopics.includes(topic)) {
        tokenObj.subscribedTopics.push(topic);
        tokenObj.updatedAt = Date.now();
    }
    
    return this.save();
};

// Add method to unsubscribe token from topic
UserSchema.methods.unsubscribeTokenFromTopic = function(token, topic) {
    const tokenObj = this.fcmTokens.find(t => t.token === token);
    
    if (tokenObj) {
        tokenObj.subscribedTopics = tokenObj.subscribedTopics.filter(t => t !== topic);
        tokenObj.updatedAt = Date.now();
    }
    
    return this.save();
};

// Add method to track device session
UserSchema.methods.addDeviceSession = function(deviceInfo) {
    if (!this.activeSessions) {
        this.activeSessions = new Map();
    }
    
    const { deviceId, deviceModel, deviceName, appVersion, osVersion } = deviceInfo;
    
    if (!deviceId) {
        throw new Error('deviceId is required for tracking sessions');
    }
    
    this.activeSessions.set(deviceId, {
        deviceId,
        deviceModel: deviceModel || 'Unknown',
        deviceName: deviceName || 'Unknown Device',
        appVersion: appVersion || 'Unknown',
        osVersion: osVersion || 'Unknown',
        loginTimestamp: new Date(),
        lastActiveTimestamp: new Date()
    });
    
    return this.save();
};

// Add method to update device session activity
UserSchema.methods.updateDeviceSessionActivity = function(deviceId) {
    if (!this.activeSessions || !this.activeSessions.has(deviceId)) {
        return this.save();
    }
    
    const session = this.activeSessions.get(deviceId);
    session.lastActiveTimestamp = new Date();
    this.activeSessions.set(deviceId, session);
    
    return this.save();
};

// Add method to remove device session
UserSchema.methods.removeDeviceSession = function(deviceId) {
    if (!this.activeSessions) {
        return this.save();
    }
    
    this.activeSessions.delete(deviceId);
    return this.save();
};

// Add method to invalidate all other device sessions
UserSchema.methods.invalidateOtherSessions = function(currentDeviceId) {
    if (!this.activeSessions) {
        return this.save();
    }
    
    // Keep only the current device session
    const currentSession = this.activeSessions.get(currentDeviceId);
    this.activeSessions.clear();
    
    if (currentSession) {
        this.activeSessions.set(currentDeviceId, currentSession);
    }
    
    return this.save();
};

// Add method to update template affinity
UserSchema.methods.updateTemplateAffinity = function(tag, score = 1) {
    const existingTag = this.templateAffinity.find(t => t.tag === tag);
    
    if (existingTag) {
        existingTag.score += score;
    } else {
        this.templateAffinity.push({ tag, score });
    }
    
    return this.save();
};

// Add method to manage drafts
UserSchema.methods.saveDraft = function(templateId, html) {
    const existingDraft = this.drafts.find(d => 
        d.templateId.toString() === templateId.toString()
    );
    
    if (existingDraft) {
        existingDraft.html = html;
        existingDraft.lastUpdated = Date.now();
    } else {
        this.drafts.push({
            templateId,
            html,
            lastUpdated: Date.now()
        });
    }
    
    return this.save();
};

// Add method to delete a draft
UserSchema.methods.deleteDraft = function(templateId) {
    this.drafts = this.drafts.filter(d => 
        d.templateId.toString() !== templateId.toString()
    );
    
    return this.save();
};

// Add method to track ignored templates
UserSchema.methods.ignoreTemplate = function(templateId, score = 1) {
    const existingIgnore = this.ignoredTemplates.find(t => 
        t.templateId.toString() === templateId.toString()
    );
    
    if (existingIgnore) {
        existingIgnore.views += 1;
        existingIgnore.lastViewed = Date.now();
        existingIgnore.lastIgnoredScore = score;
    } else {
        this.ignoredTemplates.push({
            templateId,
            views: 1,
            lastViewed: Date.now(),
            lastIgnoredScore: score
        });
    }
    
    return this.save();
};

// Add method to find applicable pricing rules
UserSchema.methods.findApplicablePricingRules = async function(planLevel, billingCycle) {
    const PricingRule = mongoose.model('PricingRule');
    
    // Calculate user account age in days
    const userAccountAge = Math.floor((Date.now() - this.created) / (1000 * 60 * 60 * 24));
    
    // Determine user segment based on activity and subscription history
    let userSegment = 'NEW_USER';
    
    if (this.subscriptionHistory && this.subscriptionHistory.length > 0) {
        userSegment = 'RETURNING_USER';
    } else if (this.lastActive && ((Date.now() - this.lastActive) > (30 * 24 * 60 * 60 * 1000))) {
        userSegment = 'INACTIVE_USER';
    }
    
    // Find applicable pricing rules
    const rules = await PricingRule.find({
        active: true,
        planLevel: planLevel,
        billingCycle: billingCycle,
        $or: [
            { ruleType: 'GLOBAL' },
            { ruleType: 'USER_SEGMENT', targetSegment: userSegment },
            { ruleType: 'EXPERIMENT', experimentTag: { $in: this.experimentTags || [] } }
        ],
        $or: [
            { expiresAt: null },
            { expiresAt: { $gt: new Date() } }
        ],
        minUserAge: { $lte: userAccountAge }
    }).sort({ priority: -1 });
    
    return rules;
};

// Add method to apply a pricing rule
UserSchema.methods.applyPricingRule = async function(planLevel, billingCycle) {
    // Find applicable rules
    const rules = await this.findApplicablePricingRules(planLevel, billingCycle);
    
    // If no rules found, return false
    if (!rules || rules.length === 0) {
        return false;
    }
    
    // Apply the highest priority rule
    const rule = rules[0];
    
    // Update subscription with pricing rule
    if (!this.subscription) {
        this.subscription = {};
    }
    
    this.subscription.planLevel = planLevel;
    this.subscription.plan = billingCycle;
    this.subscription.appliedPricingRuleId = rule._id;
    this.subscription.basePrice = rule.price;
    this.subscription.finalPrice = rule.getFinalPrice();
    this.subscription.currency = rule.currency;
    
    await this.save();
    return true;
};

module.exports = mongoose.model('User', UserSchema); 