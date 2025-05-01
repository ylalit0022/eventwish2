const mongoose = require('mongoose');

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
            
            ret.id = ret._id;
            delete ret._id;
            delete ret.__v;
            return ret;
        }
    }
});

// Create index for faster category lookups
templateSchema.index({ category: 1, status: 1 });
templateSchema.index({ createdAt: -1 });

module.exports = mongoose.model('Template', templateSchema, 'templates');