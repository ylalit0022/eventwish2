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
    isPremium: {
        type: Boolean,
        default: false
    },
    creatorId: {
        type: mongoose.Schema.Types.ObjectId,
        ref: 'User'
    },
    festivalTag: {
        type: String,
        default: ''
    },
    tags: [{
        type: String
    }],
    usageCount: {
        type: Number,
        default: 0
    },
    likes: {
        type: Number,
        default: 0
    },
    favorites: {
        type: Number,
        default: 0
    },
    categoryIcon: {
        type: String,
        validate: {
            validator: function(v) {
                return v === null || v === '' || /^https?:\/\/.+/.test(v);
            },
            message: props => `${props.value} is not a valid URL!`
        }
    }
}, {
    timestamps: true,
    toJSON: {
        virtuals: true
    }
});

module.exports = mongoose.model('Template', templateSchema, 'templates');