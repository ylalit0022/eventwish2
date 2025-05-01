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
                return mongoose.Types.ObjectId.isValid(v);
            },
            message: props => `${props.value} is not a valid ObjectId!`
        }
    }
}, {
    timestamps: true,
    toJSON: {
        virtuals: true,
        transform: function(doc, ret) {
            if (ret.categoryIcon && typeof ret.categoryIcon === 'object' && ret.categoryIcon._id) {
                // If the categoryIcon is fully populated, leave it as is
                // This ensures the client receives the full object
                // Already transformed by CategoryIcon's own toJSON method
            } else if (ret.categoryIcon && typeof ret.categoryIcon === 'string') {
                // If it's just a string ID but not populated, leave as is
                // This would be an ObjectId string
            }
            return ret;
        }
    }
});

module.exports = mongoose.model('Template', templateSchema, 'templates');