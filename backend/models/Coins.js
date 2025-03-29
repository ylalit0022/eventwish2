const mongoose = require('mongoose');

const CoinsSchema = new mongoose.Schema({
    // Device identifier for the user
    deviceId: {
        type: String,
        required: [true, 'Device ID is required'],
        trim: true,
        index: true
    },
    
    // Number of coins the user has
    coins: {
        type: Number,
        default: 0,
        min: 0
    },
    
    // Whether the HTML edit feature is unlocked
    isUnlocked: {
        type: Boolean,
        default: false
    },
    
    // When the feature was unlocked
    unlockTimestamp: {
        type: Date,
        default: null
    },
    
    // Duration in days of the unlock
    unlockDuration: {
        type: Number,
        default: 30
    },
    
    // Signature to verify unlock validity
    unlockSignature: {
        type: String,
        default: null
    },
    
    // Last time user claimed a reward
    lastRewardTimestamp: {
        type: Date,
        default: null
    },
    
    // Record of reward actions
    rewardHistory: [{
        timestamp: {
            type: Date,
            default: Date.now
        },
        adUnitId: {
            type: String,
            required: true
        },
        adName: {
            type: String,
            required: true
        },
        coinsEarned: {
            type: Number,
            required: true
        },
        deviceInfo: {
            type: Object,
            default: {}
        }
    }],
    
    // Track security violations
    securityViolations: [{
        timestamp: {
            type: Date,
            default: Date.now
        },
        type: {
            type: String,
            enum: ['root', 'emulator', 'id_tampering', 'time_manipulation', 'other'],
            default: 'other'
        },
        details: {
            type: String
        },
        deviceInfo: {
            type: Object,
            default: {}
        },
        isRooted: {
            type: Boolean,
            default: false
        },
        isEmulator: {
            type: Boolean,
            default: false
        },
        action: {
            type: String,
            enum: ['warning', 'revoke', 'blacklist', 'none'],
            default: 'none'
        }
    }],
    
    // Last sync with server
    lastSyncTimestamp: {
        type: Date,
        default: Date.now
    },
    
    // Time offset between server and client
    timeOffset: {
        type: Number,
        default: 0
    },
    
    // Device integrity information
    deviceIntegrity: {
        fingerprint: {
            type: String,
            default: null
        },
        lastVerifiedAt: {
            type: Date,
            default: null
        },
        verified: {
            type: Boolean,
            default: false
        },
        failedVerifications: {
            type: Number,
            default: 0
        }
    },
    
    // Plan configuration
    plan: {
        requiredCoins: {
            type: Number,
            default: 100
        },
        coinsPerReward: {
            type: Number,
            default: 10
        },
        defaultUnlockDuration: {
            type: Number,
            default: 30
        }
    },

    // Add auth-related fields
    auth: {
        token: {
            type: String,
            default: null
        },
        refreshToken: {
            type: String,
            default: null
        },
        tokenExpiry: {
            type: Date,
            default: null
        },
        refreshTokenExpiry: {
            type: Date,
            default: null
        },
        lastLogin: {
            type: Date,
            default: null
        },
        isAuthenticated: {
            type: Boolean,
            default: false
        },
        deviceInfo: {
            type: Map,
            of: String,
            default: {}
        }
    }
}, {
    timestamps: true,
    toJSON: {
        virtuals: true,
        transform: function(doc, ret) {
            ret.id = ret._id;
            delete ret._id;
            delete ret.__v;
            return ret;
        }
    }
});

// Virtual for unlockExpiry
CoinsSchema.virtual('unlockExpiry').get(function() {
    if (!this.unlockTimestamp || !this.isUnlocked) {
        return null;
    }
    
    const unlockDate = new Date(this.unlockTimestamp);
    const expiryDate = new Date(unlockDate);
    expiryDate.setDate(expiryDate.getDate() + this.unlockDuration);
    
    return expiryDate;
});

// Virtual for remaining time in milliseconds
CoinsSchema.virtual('remainingTime').get(function() {
    if (!this.unlockTimestamp || !this.isUnlocked) {
        return 0;
    }
    
    const unlockDate = new Date(this.unlockTimestamp);
    const expiryDate = new Date(unlockDate);
    expiryDate.setDate(expiryDate.getDate() + this.unlockDuration);
    
    const currentTime = new Date();
    const remainingMs = expiryDate.getTime() - currentTime.getTime();
    
    return Math.max(0, remainingMs);
});

// Add API key validation
CoinsSchema.statics.validateApiKey = function(apiKey) {
    const validApiKey = process.env.API_KEY || 'your-default-api-key';
    return apiKey === validApiKey;
};

// Enhance addCoins method with validation
CoinsSchema.methods.addCoins = async function(amount, adUnitId, adName, deviceInfo = {}, apiKey) {
    // Validate API key
    if (!this.constructor.validateApiKey(apiKey)) {
        throw new Error('Invalid API key');
    }
    
    this.coins += amount;
    
    // Add to reward history
    this.rewardHistory.push({
        timestamp: new Date(),
        adUnitId,
        adName,
        coinsEarned: amount,
        deviceInfo
    });
    
    // Update last reward timestamp
    this.lastRewardTimestamp = new Date();
    
    return this.save();
};

// Method to unlock feature
CoinsSchema.methods.unlockFeature = async function(duration = null, serverTimestamp = null) {
    // Use provided duration or default from plan
    const unlockDuration = duration || this.plan.defaultUnlockDuration;
    
    // Use server timestamp if provided or current time
    const timestamp = serverTimestamp ? new Date(serverTimestamp) : new Date();
    
    this.isUnlocked = true;
    this.unlockTimestamp = timestamp;
    this.unlockDuration = unlockDuration;
    
    // Generate signature (in a real implementation, this would be more secure)
    this.unlockSignature = this._generateSignature();
    
    return this.save();
};

// Method to check if unlock is expired
CoinsSchema.methods.checkUnlockStatus = function() {
    if (!this.isUnlocked || !this.unlockTimestamp) {
        return false;
    }
    
    const unlockDate = new Date(this.unlockTimestamp);
    const expiryDate = new Date(unlockDate);
    expiryDate.setDate(expiryDate.getDate() + this.unlockDuration);
    
    return new Date() < expiryDate;
};

// Internal method to generate signature
CoinsSchema.methods._generateSignature = function() {
    const crypto = require('crypto');
    const data = `${this.deviceId}:${this.unlockTimestamp.getTime()}:${this.unlockDuration}`;
    const secret = process.env.JWT_SECRET || 'eventwish-coins-secret-key';
    
    return crypto.createHmac('sha256', secret).update(data).digest('hex');
};

// Static method to validate signature
CoinsSchema.statics.validateSignature = function(deviceId, timestamp, duration, signature) {
    const crypto = require('crypto');
    const data = `${deviceId}:${timestamp}:${duration}`;
    const secret = process.env.JWT_SECRET || 'eventwish-coins-secret-key';
    
    const expectedSignature = crypto.createHmac('sha256', secret).update(data).digest('hex');
    
    return signature === expectedSignature;
};

// Add method to update auth tokens
CoinsSchema.methods.updateAuthTokens = async function(token, refreshToken) {
    this.auth.token = token;
    this.auth.refreshToken = refreshToken;
    this.auth.tokenExpiry = new Date(Date.now() + (60 * 60 * 1000)); // 1 hour
    this.auth.refreshTokenExpiry = new Date(Date.now() + (7 * 24 * 60 * 60 * 1000)); // 7 days
    this.auth.lastLogin = new Date();
    this.auth.isAuthenticated = true;
    return this.save();
};

// Add method to clear auth tokens
CoinsSchema.methods.clearAuthTokens = async function() {
    this.auth.token = null;
    this.auth.refreshToken = null;
    this.auth.tokenExpiry = null;
    this.auth.refreshTokenExpiry = null;
    this.auth.isAuthenticated = false;
    return this.save();
};

// Add method to validate auth tokens
CoinsSchema.methods.validateAuth = function() {
    if (!this.auth.isAuthenticated || !this.auth.token || !this.auth.refreshToken) {
        return false;
    }
    
    const now = new Date();
    if (now > this.auth.refreshTokenExpiry) {
        return false;
    }
    
    return true;
};

// Create the model
const Coins = mongoose.model('Coins', CoinsSchema);

module.exports = Coins;