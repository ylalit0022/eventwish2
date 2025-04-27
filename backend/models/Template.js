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
        autopopulate: true,
        validate: {
            validator: function(v) {
                return v === null || mongoose.Types.ObjectId.isValid(v);
            },
            message: props => `${props.value} is not a valid ObjectId!`
        },
        get: function(v) {
            if (v && typeof v === 'object' && v.categoryIcon) {
                return v;
            }
            return null;
        }
    }
}, {
    timestamps: true,
    toJSON: {
        virtuals: true,
        getters: true,
        transform: function(doc, ret) {
            if (ret.categoryIcon) {
                if (typeof ret.categoryIcon === 'object' && ret.categoryIcon.categoryIcon) {
                    return ret;
                }
                ret.categoryIcon = null;
            }
            return ret;
        }
    }
});

templateSchema.plugin(require('mongoose-autopopulate'));

templateSchema.pre('find', function() {
    this.populate('categoryIcon');
});

templateSchema.pre('findOne', function() {
    this.populate('categoryIcon');
});

module.exports = mongoose.model('Template', templateSchema, 'templates');
