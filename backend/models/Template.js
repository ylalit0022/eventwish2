const mongoose = require('mongoose');
const logger = require('../utils/logger') || console;

const templateSchema = new mongoose.Schema({
    title: {
        type: String,
        required: true
    },
    category: {
        type: String,
        required: true
    },
    htmlContent: {
        type: String,
        required: true
    },
    cssContent: {
        type: String,
        default: ''
    },
    jsContent: {
        type: String,
        default: ''
    },
    previewUrl: {
        type: String,
        default: ''
    },
    status: {
        type: Boolean,
        default: true
    },
    categoryIcon: {
        type: mongoose.Schema.Types.ObjectId,
        ref: 'CategoryIcon',
        required: false,
        validate: {
            validator: function(v) {
                return v === null || v === undefined || mongoose.Types.ObjectId.isValid(v);
            },
            message: props => `${props.value} is not a valid ObjectId!`
        }
    }
}, {
    timestamps: true,
    toJSON: {
        virtuals: true,
        transform: function(doc, ret) {
            // CRITICAL: Ensure id is ALWAYS a string for Android navigation
            // This is required because Android's Navigation component expects string arguments
            ret.id = ret._id ? ret._id.toString() : '';
            
            // Ensure the populated categoryIcon is preserved in the JSON output
            if (ret.categoryIcon) {
                if (typeof ret.categoryIcon === 'object' && ret.categoryIcon._id) {
                    // If categoryIcon is a populated object, ensure it has both _id and id fields
                    if (!ret.categoryIcon.id && ret.categoryIcon._id) {
                        ret.categoryIcon.id = ret.categoryIcon._id.toString();
                    }
                } else if (typeof ret.categoryIcon === 'string') {
                    // If it's just a string ID but not populated, leave as is
                    // This would be an ObjectId string
                } else if (ret.categoryIcon instanceof mongoose.Types.ObjectId) {
                    // Handle ObjectId type (not as common)
                    ret.categoryIcon = ret.categoryIcon.toString();
                }
            }
            
            delete ret._id;
            delete ret.__v;
            return ret;
        }
    },
    toObject: {
        virtuals: true,
        transform: function(doc, ret) {
            // CRITICAL: Ensure id is ALWAYS a string for Android navigation
            // This is required because Android's Navigation component expects string arguments
            ret.id = ret._id ? ret._id.toString() : '';
            
            // Ensure the populated categoryIcon is preserved in the object output
            if (ret.categoryIcon) {
                if (typeof ret.categoryIcon === 'object' && ret.categoryIcon._id) {
                    // If categoryIcon is a populated object, ensure it has both _id and id fields
                    if (!ret.categoryIcon.id && ret.categoryIcon._id) {
                        ret.categoryIcon.id = ret.categoryIcon._id.toString();
                    }
                } else if (typeof ret.categoryIcon === 'string') {
                    // If it's just a string ID but not populated, leave as is
                    // This would be an ObjectId string
                } else if (ret.categoryIcon instanceof mongoose.Types.ObjectId) {
                    // Handle ObjectId type (not as common)
                    ret.categoryIcon = ret.categoryIcon.toString();
                }
            }
            
            // We keep _id in toObject output for Mongoose operations
            delete ret.__v;
            return ret;
        }
    }
});

// Create index for faster category lookups
templateSchema.index({ category: 1, status: 1 });
templateSchema.index({ createdAt: -1 });

// Add a pre-save hook to ensure id is set correctly
templateSchema.pre('save', function(next) {
    if (this._id && !this.id) {
        this.id = this._id.toString();
    }
    next();
});

// Add a virtual for id for consistent access
templateSchema.virtual('id').get(function() {
    return this._id ? this._id.toString() : '';
});

// This helps when templates are returned without using toJSON
templateSchema.set('toJSON', {
    virtuals: true,
    transform: function(doc, ret) {
        ret.id = ret._id ? ret._id.toString() : '';
        delete ret._id;
        delete ret.__v;
        return ret;
    }
});

module.exports = mongoose.model('Template', templateSchema, 'templates');