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
            // Ensure both _id and id are present in the output
            if (!ret.id && ret._id) {
                ret.id = ret._id.toString();
            } else if (!ret._id && ret.id) {
                ret._id = ret.id;
            }
            
            // Ensure _id is also passed as string for consistency
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