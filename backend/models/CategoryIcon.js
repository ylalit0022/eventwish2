const mongoose = require('mongoose');

const categoryIconSchema = new mongoose.Schema({
    id: {
        type: String,
        required: true,
        unique: true
    },
    category: {
        type: String,
        required: true,
        unique: true
    },
    categoryIcon: {
        type: String,
        required: true,
        validate: {
            validator: function(v) {
                return /^https?:\/\/.+/.test(v);
            },
            message: props => `${props.value} is not a valid URL!`
        }
    },
    iconType: {
        type: String,
        enum: ['URL', 'RESOURCE'],
        default: 'URL'
    },
    resourceName: {
        type: String,
        default: ''
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

// Ensure indexes
categoryIconSchema.index({ category: 1 }, { unique: true });
categoryIconSchema.index({ id: 1 }, { unique: true });

module.exports = mongoose.model('CategoryIcon', categoryIconSchema, 'categoryicons');