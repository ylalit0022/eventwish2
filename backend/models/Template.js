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
            // Ensure _id exists and is properly converted
            if (!ret.id && ret._id) {
                ret.id = ret._id.toString();
            }
            
            // If somehow both id and _id are missing, this is an error
            if (!ret.id && !ret._id) {
                console.error('Template missing ID:', ret);
                throw new Error('Template document missing ID');
            }
            
            delete ret._id;
            delete ret.__v;
            
            // Transform categoryIcon if it exists
            if (ret.categoryIcon && typeof ret.categoryIcon === 'object') {
                ret.categoryIcon = {
                    id: ret.categoryIcon._id ? ret.categoryIcon._id.toString() : ret.categoryIcon.id,
                    category: ret.categoryIcon.category,
                    categoryIcon: ret.categoryIcon.categoryIcon,
                    iconType: ret.categoryIcon.iconType || 'URL',
                    resourceName: ret.categoryIcon.resourceName || ''
                };
            }
            
            // Ensure required fields are present
            if (!ret.title) {
                console.error('Template missing title:', ret);
                throw new Error('Template document missing title');
            }
            
            if (!ret.category) {
                console.error('Template missing category:', ret);
                throw new Error('Template document missing category');
            }
            
            return ret;
        }
    }
});

// Add a pre-save middleware to ensure ID is set
templateSchema.pre('save', function(next) {
    if (!this._id) {
        this._id = new mongoose.Types.ObjectId();
    }
    next();
});

templateSchema.plugin(require('mongoose-autopopulate'));

templateSchema.pre(/^find/, function(next) {
    this.populate('categoryIcon');
    next();
});

module.exports = mongoose.model('Template', templateSchema, 'templates');
