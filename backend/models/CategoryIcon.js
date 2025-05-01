const mongoose = require('mongoose');

const categoryIconSchema = new mongoose.Schema({
    // Use MongoDB's automatic _id field for object references
    // Custom id field for backward compatibility
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
            // Ensure ID fields are properly handled for consistency
            if (!ret.id) {
                ret.id = ret._id.toString();
            }
            delete ret.__v;
            return ret;
        }
    }
});

// Ensure indexes
categoryIconSchema.index({ category: 1 }, { unique: true });
categoryIconSchema.index({ id: 1 }, { unique: true });

module.exports = mongoose.model('CategoryIcon', categoryIconSchema, 'categoryicons');