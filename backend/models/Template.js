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
            validator: async function(v) {
                if (!v) return true; // Allow null/undefined
                const CategoryIcon = mongoose.model('CategoryIcon');
                const icon = await CategoryIcon.findById(v);
                return icon !== null;
            },
            message: props => `CategoryIcon with ID ${props.value} does not exist!`
        }
    }
}, {
    timestamps: true,
    toJSON: {
        virtuals: true,
        transform: function(doc, ret) {
            ret.id = ret._id;
            delete ret._id;
            delete ret.__v;
            
            // Transform categoryIcon if it exists
            if (ret.categoryIcon && typeof ret.categoryIcon === 'object') {
                ret.categoryIcon = {
                    id: ret.categoryIcon._id || ret.categoryIcon.id,
                    category: ret.categoryIcon.category,
                    categoryIcon: ret.categoryIcon.categoryIcon,
                    iconType: ret.categoryIcon.iconType || 'URL',
                    resourceName: ret.categoryIcon.resourceName || ''
                };
            }
            return ret;
        }
    }
});

templateSchema.plugin(require('mongoose-autopopulate'));

templateSchema.pre(/^find/, function(next) {
    this.populate('categoryIcon');
    next();
});

module.exports = mongoose.model('Template', templateSchema, 'templates');
