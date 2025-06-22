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
    videoUrl: {
        type: String,
        default: ''
    },
    imageUrl: {
        type: String,
        default: ''
    },

    // Access / Monetization
    status: {
        type: Boolean,
        default: true
    },
    isPremium: {
        type: Boolean,
        default: false
    },
    isFeatured: {
        type: Boolean,
        default: false
    },
    isTrending: {
        type: Boolean,
        default: false
    },
    isFlagged: {
        type: Boolean,
        default: false
    },
    isLowPerforming: {
        type: Boolean,
        default: false
    },
    price: {
        type: Number,
        default: 0
    },
    
    // Weekly Engagement Metrics
    weeklyUsageCount: {
        type: Number,
        default: 0
    },
    weeklyLikes: {
        type: Number,
        default: 0
    },
    weeklyFavorites: {
        type: Number,
        default: 0
    },
    weeklyViewCount: {
        type: Number,
        default: 0
    },
    weeklySharedCount: {
        type: Number,
        default: 0
    },
    weeklyDownloadCount: {
        type: Number,
        default: 0
    },
    weeklyReportCount: {
        type: Number,
        default: 0
    },
    weeklyScoreLastReset: {
        type: Date,
        default: Date.now
    },

    // Customization Settings
    customizationOptions: {
        allowNameEdit: {
            type: Boolean,
            default: true
        },
        allowPhotoEdit: {
            type: Boolean,
            default: false
        },
        allowBackgroundChange: {
            type: Boolean,
            default: false
        },
        allowMusic: {
            type: Boolean,
            default: false
        },
        allowThemeCustomization: {
            type: Boolean,
            default: false
        }
    },

    // AI Metadata
    isAIGenerated: {
        type: Boolean,
        default: false
    },
    aiPrompt: {
        type: String,
        default: ''
    },
    aiModel: {
        type: String,
        default: ''
    },
    aiStyle: {
        type: String,
        default: ''
    },
    aiGenerationStage: {
        type: String,
        enum: ['initial', 'on_edit', 'variation'],
        default: 'initial'
    },
    generatedByUser: {
        type: mongoose.Schema.Types.ObjectId,
        ref: 'User',
        default: null
    },
    generationMetadata: {
        resolution: {
            type: String,
            default: '1024x1024'
        },
        seed: {
            type: Number
        },
        guidanceScale: {
            type: Number
        },
        timestamp: {
            type: Date
        }
    },

    // Categorization
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
    styleTags: [{
        type: String
    }],
    searchKeywords: [{
        type: String
    }],
    variationOf: {
        type: mongoose.Schema.Types.ObjectId,
        ref: 'Template',
        default: null
    },
    relatedTemplates: [{
        type: mongoose.Schema.Types.ObjectId,
        ref: 'Template'
    }],

    // Engagement Metrics
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
    viewCount: {
        type: Number,
        default: 0
    },
    sharedCount: {
        type: Number,
        default: 0
    },
    downloadCount: {
        type: Number,
        default: 0
    },
    reportCount: {
        type: Number,
        default: 0
    },
    rating: {
        type: Number,
        default: 0
    },
    ratingCount: {
        type: Number,
        default: 0
    },
    templateType: {
        type: String,
        enum: ['html', 'image', 'video'],
        default: 'html'
    },
    // Visibility Ranking
    visibilityScore: {
        type: Number,
        default: 0
    },
    boostPriority: {
        type: Number,
        default: 0
    },
    lastBoostedAt: {
        type: Date,
        default: null
    },
    ignoredByUsers: [{
        type: mongoose.Schema.Types.ObjectId,
        ref: 'User'
    }],
    
    // Legacy fields maintained for compatibility
    categoryIcon: {
        type: String,
        validate: {
            validator: function(v) {
                return v === null || v === '' || /^https?:\/\/.+/.test(v);
            },
            message: props => `${props.value} is not a valid URL!`
        }
    },
    isFeatured: {
        type: Boolean,
        default: false
    },
    isTrending: {
        type: Boolean,
        default: false
    },
    isFlagged: {
        type: Boolean,
        default: false
    },
    moderationStatus: {
        type: String,
        enum: ['approved', 'pending', 'rejected'],
        default: 'approved'
    },
    language: {
        type: mongoose.Schema.Types.ObjectId,
        ref: 'Language'
    },
    region: {
        type: mongoose.Schema.Types.ObjectId,
        ref: 'Region'
    },
    experimentTag: {
        type: String,
        default: ''
    },
    variationOf: {
        type: mongoose.Schema.Types.ObjectId,
        ref: 'Template',
        default: null
    },
    relatedTemplates: [{
        type: mongoose.Schema.Types.ObjectId,
        ref: 'Template'
    }],
    performanceLog: {
        dailyUsage: { type: Number, default: 0 },
        weeklyUsage: { type: Number, default: 0 },
        lastUsedAt: { type: Date }
    }
}, {
    timestamps: true,
    toJSON: { virtuals: true },
    toObject: { virtuals: true }
});

// 🔥 Virtual field: trendingScore
templateSchema.virtual('trendingScore').get(function() {
    return (
        this.usageCount * 3 +
        this.likes * 2 +
        this.favorites * 2 +
        this.viewCount +
        this.sharedCount +
        this.downloadCount -
        this.reportCount * 5
    );
});

// Weekly trending score virtual
templateSchema.virtual('weeklyTrendingScore').get(function() {
    return (
        this.weeklyUsageCount * 3 +
        this.weeklyLikes * 2 +
        this.weeklyFavorites * 2 +
        this.weeklyViewCount +
        this.weeklySharedCount +
        this.weeklyDownloadCount -
        this.weeklyReportCount * 5
    );
});

// Pre-save middleware to ensure ObjectId references are strings
templateSchema.pre('save', function(next) {
    // Log count changes for debugging
    if (this.isModified('likes') || this.isModified('favorites') || this.isModified('sharedCount')) {
        console.log(`Template ${this._id} count update:`, {
            likes: this.likes,
            favorites: this.favorites,
            sharedCount: this.sharedCount,
            modifiedFields: this.modifiedPaths()
        });
    }
    
    // Ensure language is a string if it exists
    if (this.language !== null && this.language !== undefined) {
        if (typeof this.language !== 'string') {
            console.log('Converting language field from', typeof this.language, 'to string:', this.language);
            this.language = String(this.language);
        }
    } else if (this.language === null) {
        // Convert null to empty string
        this.language = '';
        console.log('Converting null language to empty string');
    }
    
    // Ensure region is a string if it exists
    if (this.region !== null && this.region !== undefined) {
        if (typeof this.region !== 'string') {
            console.log('Converting region field from', typeof this.region, 'to string:', this.region);
            this.region = String(this.region);
        }
    } else if (this.region === null) {
        // Convert null to empty string
        this.region = '';
        console.log('Converting null region to empty string');
    }
    
    // Ensure variationOf is a string if it exists
    if (this.variationOf !== null && this.variationOf !== undefined) {
        if (typeof this.variationOf !== 'string') {
            console.log('Converting variationOf field from', typeof this.variationOf, 'to string:', this.variationOf);
            this.variationOf = String(this.variationOf);
        }
    } else if (this.variationOf === null) {
        // Convert null to empty string
        this.variationOf = '';
        console.log('Converting null variationOf to empty string');
    }
    
    // Ensure relatedTemplates are strings
    if (this.relatedTemplates && Array.isArray(this.relatedTemplates)) {
        this.relatedTemplates = this.relatedTemplates.map(id => {
            if (id === null) return '';
            if (typeof id !== 'string') {
                console.log('Converting relatedTemplate id from', typeof id, 'to string:', id);
                return String(id);
            }
            return id;
        });
    }
    
    next();
});

// Post-save middleware to log successful count updates
templateSchema.post('save', function(doc) {
    if (doc.isModified && (doc.isModified('likes') || doc.isModified('favorites') || doc.isModified('sharedCount'))) {
        console.log(`Template ${doc._id} count update completed:`, {
            likes: doc.likes,
            favorites: doc.favorites,
            sharedCount: doc.sharedCount
        });
    }
});

// Pre-findOneAndUpdate hook to handle null/empty values for ObjectId fields
templateSchema.pre('findOneAndUpdate', function(next) {
    const update = this.getUpdate();
    
    // Log count increment operations
    if (update && update.$inc) {
        console.log(`Template findOneAndUpdate with $inc operation:`, {
            filter: this.getFilter(),
            increment: update.$inc,
            operation: 'findOneAndUpdate'
        });
    }
    
    // Handle ObjectId references - remove null/empty values
    if (update) {
        // Handle direct updates
        if (update.language === null || update.language === '') {
            console.log('Removing null/empty language field in pre-update hook');
            delete update.language;
        }
        
        if (update.region === null || update.region === '') {
            console.log('Removing null/empty region field in pre-update hook');
            delete update.region;
        }
        
        if (update.variationOf === null || update.variationOf === '') {
            console.log('Removing null/empty variationOf field in pre-update hook');
            delete update.variationOf;
        }
        
        if (update.relatedTemplates && Array.isArray(update.relatedTemplates)) {
            update.relatedTemplates = update.relatedTemplates
                .filter(id => id !== null && id !== undefined && id !== '');
            console.log('Filtered relatedTemplates in direct update');
        }
        
        // Handle $set updates
        if (update.$set) {
            if (update.$set.language === null || update.$set.language === '') {
                console.log('Removing null/empty language field in $set');
                delete update.$set.language;
            }
            
            if (update.$set.region === null || update.$set.region === '') {
                console.log('Removing null/empty region field in $set');
                delete update.$set.region;
            }
            
            if (update.$set.variationOf === null || update.$set.variationOf === '') {
                console.log('Removing null/empty variationOf field in $set');
                delete update.$set.variationOf;
            }
            
            if (update.$set.relatedTemplates && Array.isArray(update.$set.relatedTemplates)) {
                update.$set.relatedTemplates = update.$set.relatedTemplates
                    .filter(id => id !== null && id !== undefined && id !== '');
                console.log('Filtered relatedTemplates in $set');
            }
            
            // Remove empty $set if all properties were removed
            if (Object.keys(update.$set).length === 0) {
                delete update.$set;
            }
        }
    }
    
    next();
});

module.exports = mongoose.model('Template', templateSchema, 'templates');