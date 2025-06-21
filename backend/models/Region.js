const mongoose = require('mongoose');

const regionSchema = new mongoose.Schema({
    code: {
        type: String,
        required: true,
        unique: true,
        trim: true,
        uppercase: true
    },
    name: {
        type: String,
        required: true,
        trim: true
    },
    continent: {
        type: String,
        trim: true,
        enum: ['Africa', 'Asia', 'Europe', 'North America', 'South America', 'Oceania', 'Antarctica', 'Global']
    },
    flagIcon: {
        type: String,
        default: '',
        validate: {
            validator: function(v) {
                return v === '' || /^https?:\/\/.+/.test(v);
            },
            message: props => `${props.value} is not a valid URL!`
        }
    },
    isActive: {
        type: Boolean,
        default: true
    },
    displayOrder: {
        type: Number,
        default: 0
    },
    localization: {
        type: Map,
        of: String,
        default: {}
    }
}, {
    timestamps: true,
    toJSON: { virtuals: true },
    toObject: { virtuals: true }
});

// Index for faster lookups
regionSchema.index({ code: 1 });
regionSchema.index({ continent: 1 });
regionSchema.index({ isActive: 1 });

module.exports = mongoose.model('Region', regionSchema, 'regions'); 