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

// Create the model
const AdMob = mongoose.model('AdMob', AdMobSchema);

module.exports = {
    AdMob,
    adTypes
};
