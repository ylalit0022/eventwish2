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
            // Ensure proper ID transformation
            if (ret._id) {
                ret.id = ret._id.toString();
            } else if (!ret.id) {
                ret.id = ''; // Fallback to empty string for client compatibility
            }
            
            delete ret._id;
            delete ret.__v;
            
            // Transform categoryIcon if it exists
            if (ret.categoryIcon && typeof ret.categoryIcon === 'object') {
                const iconId = ret.categoryIcon._id ? ret.categoryIcon._id.toString() : 
                             ret.categoryIcon.id ? ret.categoryIcon.id : '';
                             
                ret.categoryIcon = {
                    id: iconId,
                    category: ret.categoryIcon.category || '',
                    categoryIcon: ret.categoryIcon.categoryIcon || '',
                    iconType: ret.categoryIcon.iconType || 'URL',
                    resourceName: ret.categoryIcon.resourceName || ''
                };
            } else {
                // Ensure categoryIcon is never null for client compatibility
                ret.categoryIcon = {
                    id: '',
                    category: '',
                    categoryIcon: '',
                    iconType: 'URL',
                    resourceName: ''
                };
            }
            
            // Ensure required fields are never null
            ret.title = ret.title || '';
            ret.category = ret.category || '';
            ret.htmlContent = ret.htmlContent || '';
            ret.cssContent = ret.cssContent || '';
            ret.jsContent = ret.jsContent || '';
            ret.previewUrl = ret.previewUrl || '';
            
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

// Ensure categoryIcon is always populated
templateSchema.pre(/^find/, function(next) {
    this.populate('categoryIcon');
    next();
});

module.exports = mongoose.model('Template', templateSchema, 'templates');
