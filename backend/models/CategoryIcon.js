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
            // Ensure id is always present (critical fix)
            ret.id = ret._id.toString();
            
            // Ensure _id is also string for consistency
            if (ret._id && typeof ret._id !== 'string') {
                ret._id = ret._id.toString();
            }
            
            delete ret.__v;
            return ret;
        }
    },
    toObject: {
        virtuals: true,
        transform: function(doc, ret) {
            // Also ensure id is set in toObject
            ret.id = ret._id.toString();
            
            if (ret._id && typeof ret._id !== 'string') {
                ret._id = ret._id.toString();
            }
            
            delete ret.__v;
            return ret;
        }
    }
});

// Add pre-save middleware to ensure id is set from _id if not provided
categoryIconSchema.pre('save', function(next) {
    if (!this.id && this._id) {
        this.id = this._id.toString();
    }
    next();
});

// Ensure indexes
categoryIconSchema.index({ category: 1 }, { unique: true });
categoryIconSchema.index({ id: 1 }, { unique: true });

module.exports = mongoose.model('CategoryIcon', categoryIconSchema, 'categoryicons');