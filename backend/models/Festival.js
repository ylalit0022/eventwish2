const mongoose = require('mongoose');

const festivalSchema = new mongoose.Schema({
    name: {
        type: String,
        required: true
    },
    date: {
        type: Date,
        required: true
    },
    description: {
        type: String,
        default: ''
    },
    category: {
        type: String,
        required: true
    },
    categoryIcon: {
        type: mongoose.Schema.Types.ObjectId,
        ref: 'CategoryIcon'
    },
    imageUrl: {
        type: String,
        default: ''
    },
    templates: [{
        type: mongoose.Schema.Types.ObjectId,
        ref: 'Template'
    }],
    isActive: {
        type: Boolean,
        default: true
    }
}, {
    timestamps: true
});

module.exports = mongoose.model('Festival', festivalSchema, 'festivals');