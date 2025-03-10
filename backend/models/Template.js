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
<<<<<<< HEAD
        required: true,
=======
        required: false,
>>>>>>> 1e1ae0b5d1a51738de9a76d1458f67af0e80a103
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
            if (ret.categoryIcon && typeof ret.categoryIcon === 'object') {
                ret.categoryIcon = ret.categoryIcon._id;
            }
            return ret;
        }
    }
});

module.exports = mongoose.model('Template', templateSchema, 'templates');
