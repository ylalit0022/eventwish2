const mongoose = require('mongoose');

const cropAppUserSchema = new mongoose.Schema({
    deviceId: {
        type: String,
        required: true,
        unique: true,
        index: true
    },
    coinBalance: {
        type: Number,
        default: 0,
        min: 0
    },
    isPremium: {
        type: Boolean,
        default: false
    },
    premiumExpiryDate: {
        type: Date,
        default: null
    },
    dailyImageCount: {
        type: Number,
        default: 0
    },
    lastResetDate: {
        type: Date,
        default: Date.now
    },
    lastAdWatched: {
        type: Date,
        default: null
    },
    transactions: [{
        type: {
            type: String,
            enum: ['EARNED', 'SPENT'],
            required: true
        },
        amount: {
            type: Number,
            required: true
        },
        reason: {
            type: String,
            enum: ['AD_REWARD', 'PREMIUM_PURCHASE', 'BONUS', 'OTHER'],
            default: 'OTHER'
        },
        timestamp: {
            type: Date,
            default: Date.now
        },
        premiumDuration: {
            type: Number, // Duration in days
            default: null
        },
        metadata: {
            type: mongoose.Schema.Types.Mixed,
            default: {}
        }
    }],
    appVersion: {
        type: String,
        default: '1.0.0'
    },
    lastSyncedAt: {
        type: Date,
        default: Date.now
    }
}, {
    timestamps: true,
    collection: 'cropapp' // Use the specified table name
});

// Method to add coins
cropAppUserSchema.methods.addCoins = function(amount, reason = 'AD_REWARD', metadata = {}) {
    if (amount <= 0) {
        throw new Error('Coin amount must be positive');
    }
    
    this.coinBalance += amount;
    this.transactions.push({
        type: 'EARNED',
        amount,
        reason,
        timestamp: new Date(),
        metadata
    });
    
    return this.coinBalance;
};

// Method to spend coins
cropAppUserSchema.methods.spendCoins = function(amount, reason = 'PREMIUM_PURCHASE', premiumDuration = null, metadata = {}) {
    if (amount <= 0) {
        throw new Error('Coin amount must be positive');
    }
    
    if (this.coinBalance < amount) {
        throw new Error('Insufficient coins');
    }
    
    this.coinBalance -= amount;
    this.transactions.push({
        type: 'SPENT',
        amount,
        reason,
        timestamp: new Date(),
        premiumDuration,
        metadata
    });
    
    return this.coinBalance;
};

// Method to activate premium
cropAppUserSchema.methods.activatePremium = function(durationInDays) {
    if (durationInDays <= 0) {
        throw new Error('Premium duration must be positive');
    }
    
    const now = new Date();
    // If premium is already active, extend it
    if (this.isPremium && this.premiumExpiryDate > now) {
        this.premiumExpiryDate = new Date(this.premiumExpiryDate.getTime() + (durationInDays * 24 * 60 * 60 * 1000));
    } else {
        // Otherwise, set new expiry
        this.isPremium = true;
        this.premiumExpiryDate = new Date(now.getTime() + (durationInDays * 24 * 60 * 60 * 1000));
    }
    
    return this.premiumExpiryDate;
};

// Method to check premium status
cropAppUserSchema.methods.checkPremiumStatus = function() {
    const now = new Date();
    
    if (!this.isPremium) {
        return false;
    }
    
    if (this.premiumExpiryDate <= now) {
        this.isPremium = false;
        return false;
    }
    
    return true;
};

// Method to reset daily image count if needed
cropAppUserSchema.methods.checkAndResetDailyCount = function() {
    const now = new Date();
    const lastReset = new Date(this.lastResetDate);
    
    // Reset if it's a new day
    if (lastReset.getDate() !== now.getDate() || 
        lastReset.getMonth() !== now.getMonth() || 
        lastReset.getFullYear() !== now.getFullYear()) {
        
        this.dailyImageCount = 0;
        this.lastResetDate = now;
        return true;
    }
    
    return false;
};

// Method to increment image count
cropAppUserSchema.methods.incrementImageCount = function() {
    this.checkAndResetDailyCount();
    this.dailyImageCount += 1;
    return this.dailyImageCount;
};

// Method to update ad watched timestamp
cropAppUserSchema.methods.recordAdWatched = function() {
    this.lastAdWatched = new Date();
    return this.lastAdWatched;
};

module.exports = mongoose.model('CropAppUser', cropAppUserSchema); 