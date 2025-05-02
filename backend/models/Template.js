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
        type: String,
        required: false,
        validate: {
            validator: function(v) {
                // Allow null/empty or valid URL
                return v === null || v === '' || /^https?:\/\/.+/.test(v);
            },
            message: props => `${props.value} is not a valid URL!`
        }
    },
    categoryIconObj: {
        type: mongoose.Schema.Types.ObjectId,
        ref: 'CategoryIcon',
        required: false
    }
}, {
    timestamps: true,
    toJSON: {
        virtuals: true,
        transform: function(doc, ret) {
            if (ret.categoryIconObj && typeof ret.categoryIconObj === 'object') {
                ret.categoryIcon = ret.categoryIconObj;
            }
            return ret;
        }
    }
});

module.exports = mongoose.model('Template', templateSchema, 'templates');