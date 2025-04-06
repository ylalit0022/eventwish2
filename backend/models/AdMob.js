const mongoose = require('mongoose');

const adTypes = ['Banner', 'Interstitial', 'Rewarded', 'Native', 'App Open', 'Video'];

// Create the schema
const AdMobSchema = new mongoose.Schema({
    adName: {
        type: String,
        required: [true, 'Ad name is required'],
        trim: true,
        maxLength: [100, 'Ad name cannot exceed 100 characters']
    },
    adUnitCode: {
        type: String,
        required: [true, 'Ad unit code is required'],
        trim: true,
        validate: {
            validator: function(v) {
                return /^ca-app-pub-\d{16}\/\d{10}$/.test(v);
            },
            message: props => `${props.value} is not a valid ad unit code format!`
        }
    },
    adType: {
        type: String,
        required: [true, 'Ad type is required'],
        enum: {
            values: adTypes,
            message: 'Invalid ad type. Must be one of: ' + adTypes.join(', ')
        }
    },
    status: {
        type: Boolean,
        default: true
    },
    // Targeting criteria
    targetingCriteria: {
        type: Map,
        of: mongoose.Schema.Types.Mixed,
        default: {}
    },
    // Target segments
    targetSegments: [{
        type: mongoose.Schema.Types.ObjectId,
        ref: 'UserSegment'
    }],
    // Targeting priority (higher number = higher priority)
    targetingPriority: {
        type: Number,
        default: 1,
        min: 1,
        max: 10
    },
    // Parameters for customizing the ad
    parameters: {
        type: Map,
        of: String,
        default: {}
    },
    // Analytics fields
    impressions: {
        type: Number,
        default: 0
    },
    clicks: {
        type: Number,
        default: 0
    },
    ctr: {
        type: Number,
        default: 0
    },
    revenue: {
        type: Number,
        default: 0
    },
    // Store recent impression data
    impressionData: [{
        timestamp: {
            type: Date,
            default: Date.now
        },
        context: {
            type: Object,
            default: {}
        }
    }],
    // Store recent click data
    clickData: [{
        timestamp: {
            type: Date,
            default: Date.now
        },
        context: {
            type: Object,
            default: {}
        }
    }],
    // Store revenue data
    revenueData: [{
        timestamp: {
            type: Date,
            default: Date.now
        },
        amount: {
            type: Number,
            required: true
        },
        currency: {
            type: String,
            default: 'USD'
        }
    }],
    // Performance metrics by segment
    segmentPerformance: {
        type: Map,
        of: {
            impressions: Number,
            clicks: Number,
            ctr: Number,
            revenue: Number
        },
        default: {}
    },
    // Ad display settings
    displaySettings: {
        maxImpressionsPerDay: {
            type: Number,
            default: 10,
            min: 1
        },
        minIntervalBetweenAds: {
            type: Number,
            default: 60, // seconds
            min: 30
        },
        cooldownPeriod: {
            type: Number,
            default: 15, // days
            min: 1,
            max: 30
        }
    },
    // Device-specific settings
    deviceSettings: {
        type: Map,
        of: {
            lastShown: Date,
            impressionsToday: Number,
            cooldownUntil: Date
        },
        default: {}
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

// Create unique index for adUnitCode
AdMobSchema.index(
    { adUnitCode: 1 },
    { 
        unique: true,
        collation: { locale: 'en', strength: 2 },
        background: true
    }
);

// Pre-save middleware to check for duplicates
AdMobSchema.pre('save', async function(next) {
    try {
        if (this.isModified('adUnitCode')) {
            const existingAd = await mongoose.models.AdMob.findOne({
                _id: { $ne: this._id },
                adUnitCode: this.adUnitCode
            }).collation({ locale: 'en', strength: 2 });

            if (existingAd) {
                throw new Error('Ad unit code already exists');
            }
        }
        next();
    } catch (error) {
        next(error);
    }
});

// Handle duplicate key errors
AdMobSchema.post('save', function(error, doc, next) {
    if (error.name === 'MongoServerError' && error.code === 11000) {
        next(new Error('Ad unit code already exists'));
    } else {
        next(error);
    }
});

// Methods for ad management
AdMobSchema.methods.canShowAd = async function(deviceId) {
    const now = new Date();
    const deviceData = this.deviceSettings.get(deviceId) || {
        lastShown: null,
        impressionsToday: 0,
        cooldownUntil: null
    };

    // Check cooldown
    if (deviceData.cooldownUntil && now < deviceData.cooldownUntil) {
        return {
            canShow: false,
            reason: 'cooldown',
            nextAvailable: deviceData.cooldownUntil
        };
    }

    // Check daily impression limit
    if (deviceData.impressionsToday >= this.displaySettings.maxImpressionsPerDay) {
        return {
            canShow: false,
            reason: 'daily_limit',
            nextAvailable: new Date(now.setDate(now.getDate() + 1))
        };
    }

    // Check minimum interval
    if (deviceData.lastShown) {
        const timeSinceLastAd = (now - deviceData.lastShown) / 1000;
        if (timeSinceLastAd < this.displaySettings.minIntervalBetweenAds) {
            return {
                canShow: false,
                reason: 'interval',
                nextAvailable: new Date(deviceData.lastShown.getTime() + 
                    this.displaySettings.minIntervalBetweenAds * 1000)
            };
        }
    }

    return { canShow: true };
};

AdMobSchema.methods.recordImpression = async function(deviceId, context = {}) {
    const now = new Date();
    const deviceData = this.deviceSettings.get(deviceId) || {
        lastShown: null,
        impressionsToday: 0,
        cooldownUntil: null
    };

    // Update device data
    deviceData.lastShown = now;
    deviceData.impressionsToday += 1;

    // Reset daily counter if needed
    if (deviceData.lastShown && 
        deviceData.lastShown.getDate() !== now.getDate()) {
        deviceData.impressionsToday = 1;
    }

    // Update device settings
    this.deviceSettings.set(deviceId, deviceData);

    // Record impression data
    this.impressionData.push({
        timestamp: now,
        context
    });

    // Update analytics
    this.impressions += 1;

    return this.save();
};

AdMobSchema.methods.recordClick = async function(deviceId, context = {}) {
    const now = new Date();
    
    // Record click data
    this.clickData.push({
        timestamp: now,
        context
    });

    // Update analytics
    this.clicks += 1;
    this.ctr = this.impressions > 0 ? (this.clicks / this.impressions) * 100 : 0;

    return this.save();
};

AdMobSchema.methods.startCooldown = async function(deviceId) {
    const now = new Date();
    const cooldownEnd = new Date(now.getTime() + 
        this.displaySettings.cooldownPeriod * 24 * 60 * 60 * 1000);

    const deviceData = this.deviceSettings.get(deviceId) || {
        lastShown: null,
        impressionsToday: 0,
        cooldownUntil: null
    };

    deviceData.cooldownUntil = cooldownEnd;
    this.deviceSettings.set(deviceId, deviceData);

    return this.save();
};

// Create the model
const AdMob = mongoose.model('AdMob', AdMobSchema);

module.exports = {
    AdMob,
    adTypes
};
