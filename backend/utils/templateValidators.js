const mongoose = require('mongoose');
const logger = require('./logger');

/**
 * Template Field Validators
 * Middleware functions for validating specific template fields
 */

/**
 * Validates if a string is a valid MongoDB ObjectId
 */
const isValidObjectId = (id) => {
    return mongoose.Types.ObjectId.isValid(id);
};

/**
 * Validates string fields with specific constraints
 */
const validateStringField = (fieldName, maxLength = 1000, required = false, urlPattern = false) => {
    return (req, res, next) => {
        const value = req.body[fieldName];
        
        if (required && (!value || value.trim().length === 0)) {
            return res.status(400).json({
                success: false,
                error: {
                    code: 'VALIDATION_ERROR',
                    message: `${fieldName} is required`,
                    field: fieldName,
                    value: value
                },
                timestamp: new Date().toISOString()
            });
        }
        
        if (value && (typeof value !== 'string' || value.length > maxLength)) {
            return res.status(400).json({
                success: false,
                error: {
                    code: 'VALIDATION_ERROR',
                    message: `${fieldName} must be a string with maximum ${maxLength} characters`,
                    field: fieldName,
                    value: value
                },
                timestamp: new Date().toISOString()
            });
        }
        
        if (value && urlPattern && !/^https?:\/\/.+/.test(value)) {
            return res.status(400).json({
                success: false,
                error: {
                    code: 'VALIDATION_ERROR',
                    message: `${fieldName} must be a valid URL`,
                    field: fieldName,
                    value: value
                },
                timestamp: new Date().toISOString()
            });
        }
        
        next();
    };
};

/**
 * Validates number fields with specific constraints
 */
const validateNumberField = (fieldName, min = 0, max = null, required = false) => {
    return (req, res, next) => {
        const value = req.body[fieldName];
        
        if (required && (value === undefined || value === null)) {
            return res.status(400).json({
                success: false,
                error: {
                    code: 'VALIDATION_ERROR',
                    message: `${fieldName} is required`,
                    field: fieldName,
                    value: value
                },
                timestamp: new Date().toISOString()
            });
        }
        
        if (value !== undefined && value !== null) {
            if (typeof value !== 'number' || value < min || (max !== null && value > max)) {
                return res.status(400).json({
                    success: false,
                    error: {
                        code: 'VALIDATION_ERROR',
                        message: `${fieldName} must be a number between ${min} and ${max || 'infinity'}`,
                        field: fieldName,
                        value: value
                    },
                    timestamp: new Date().toISOString()
                });
            }
        }
        
        next();
    };
};

/**
 * Validates boolean fields
 */
const validateBooleanField = (fieldName, required = false) => {
    return (req, res, next) => {
        const value = req.body[fieldName];
        
        if (required && value === undefined) {
            return res.status(400).json({
                success: false,
                error: {
                    code: 'VALIDATION_ERROR',
                    message: `${fieldName} is required`,
                    field: fieldName,
                    value: value
                },
                timestamp: new Date().toISOString()
            });
        }
        
        if (value !== undefined && typeof value !== 'boolean') {
            return res.status(400).json({
                success: false,
                error: {
                    code: 'VALIDATION_ERROR',
                    message: `${fieldName} must be a boolean`,
                    field: fieldName,
                    value: value
                },
                timestamp: new Date().toISOString()
            });
        }
        
        next();
    };
};

/**
 * Validates array fields
 */
const validateArrayField = (fieldName, itemType = 'string', maxItems = 100, required = false) => {
    return (req, res, next) => {
        const value = req.body[fieldName];
        
        if (required && (!value || !Array.isArray(value) || value.length === 0)) {
            return res.status(400).json({
                success: false,
                error: {
                    code: 'VALIDATION_ERROR',
                    message: `${fieldName} is required and must be a non-empty array`,
                    field: fieldName,
                    value: value
                },
                timestamp: new Date().toISOString()
            });
        }
        
        if (value !== undefined) {
            if (!Array.isArray(value)) {
                return res.status(400).json({
                    success: false,
                    error: {
                        code: 'VALIDATION_ERROR',
                        message: `${fieldName} must be an array`,
                        field: fieldName,
                        value: value
                    },
                    timestamp: new Date().toISOString()
                });
            }
            
            if (value.length > maxItems) {
                return res.status(400).json({
                    success: false,
                    error: {
                        code: 'VALIDATION_ERROR',
                        message: `${fieldName} cannot have more than ${maxItems} items`,
                        field: fieldName,
                        value: value
                    },
                    timestamp: new Date().toISOString()
                });
            }
            
            // Validate array items
            for (let i = 0; i < value.length; i++) {
                const item = value[i];
                if (itemType === 'string' && typeof item !== 'string') {
                    return res.status(400).json({
                        success: false,
                        error: {
                            code: 'VALIDATION_ERROR',
                            message: `${fieldName}[${i}] must be a string`,
                            field: fieldName,
                            value: item
                        },
                        timestamp: new Date().toISOString()
                    });
                }
                
                if (itemType === 'objectId' && !isValidObjectId(item)) {
                    return res.status(400).json({
                        success: false,
                        error: {
                            code: 'VALIDATION_ERROR',
                            message: `${fieldName}[${i}] must be a valid ObjectId`,
                            field: fieldName,
                            value: item
                        },
                        timestamp: new Date().toISOString()
                    });
                }
            }
        }
        
        next();
    };
};

/**
 * Validates ObjectId fields
 */
const validateObjectIdField = (fieldName, required = false) => {
    return (req, res, next) => {
        const value = req.body[fieldName];
        
        if (required && (!value || value === '')) {
            return res.status(400).json({
                success: false,
                error: {
                    code: 'VALIDATION_ERROR',
                    message: `${fieldName} is required`,
                    field: fieldName,
                    value: value
                },
                timestamp: new Date().toISOString()
            });
        }
        
        if (value && value !== '' && !isValidObjectId(value)) {
            return res.status(400).json({
                success: false,
                error: {
                    code: 'VALIDATION_ERROR',
                    message: `${fieldName} must be a valid ObjectId`,
                    field: fieldName,
                    value: value
                },
                timestamp: new Date().toISOString()
            });
        }
        
        next();
    };
};

/**
 * Validates enum fields
 */
const validateEnumField = (fieldName, allowedValues, required = false) => {
    return (req, res, next) => {
        const value = req.body[fieldName];
        
        if (required && (!value || value === '')) {
            return res.status(400).json({
                success: false,
                error: {
                    code: 'VALIDATION_ERROR',
                    message: `${fieldName} is required`,
                    field: fieldName,
                    value: value
                },
                timestamp: new Date().toISOString()
            });
        }
        
        if (value && !allowedValues.includes(value)) {
            return res.status(400).json({
                success: false,
                error: {
                    code: 'VALIDATION_ERROR',
                    message: `${fieldName} must be one of: ${allowedValues.join(', ')}`,
                    field: fieldName,
                    value: value,
                    details: { allowedValues }
                },
                timestamp: new Date().toISOString()
            });
        }
        
        next();
    };
};

/**
 * Validates Firebase UID fields
 */
const validateFirebaseUid = (fieldName, required = false) => {
    return (req, res, next) => {
        const value = req.body[fieldName];
        
        if (required && (!value || value === '')) {
            return res.status(400).json({
                success: false,
                error: {
                    code: 'VALIDATION_ERROR',
                    message: `${fieldName} is required`,
                    field: fieldName,
                    value: value
                },
                timestamp: new Date().toISOString()
            });
        }
        
        if (value && value !== '' && !/^[a-zA-Z0-9_-]{10,128}$/.test(value)) {
            return res.status(400).json({
                success: false,
                error: {
                    code: 'VALIDATION_ERROR',
                    message: `${fieldName} must be a valid Firebase UID (10-128 alphanumeric characters, dashes, underscores)`,
                    field: fieldName,
                    value: value
                },
                timestamp: new Date().toISOString()
            });
        }
        
        next();
    };
};

// Specific field validators for Template schema
const templateFieldValidators = {
    // Core Content Fields
    title: validateStringField('title', 200, true),
    category: validateStringField('category', 100, true),
    htmlContent: validateStringField('htmlContent', 50000, true),
    cssContent: validateStringField('cssContent', 10000),
    jsContent: validateStringField('jsContent', 10000),
    previewUrl: validateStringField('previewUrl', 500, false, true),
    videoUrl: validateStringField('videoUrl', 500, false, true),
    imageUrl: validateStringField('imageUrl', 500, false, true),
    
    // Monetization Fields
    status: validateBooleanField('status'),
    isPremium: validateBooleanField('isPremium'),
    isFeatured: validateBooleanField('isFeatured'),
    isTrending: validateBooleanField('isTrending'),
    isFlagged: validateBooleanField('isFlagged'),
    isLowPerforming: validateBooleanField('isLowPerforming'),
    price: validateNumberField('price', 0),
    moderationStatus: validateEnumField('moderationStatus', ['approved', 'pending', 'rejected']),
    
    // Engagement Metrics
    usageCount: validateNumberField('usageCount', 0),
    likes: validateNumberField('likes', 0),
    favorites: validateNumberField('favorites', 0),
    viewCount: validateNumberField('viewCount', 0),
    sharedCount: validateNumberField('sharedCount', 0),
    downloadCount: validateNumberField('downloadCount', 0),
    reportCount: validateNumberField('reportCount', 0),
    rating: validateNumberField('rating', 0, 5),
    ratingCount: validateNumberField('ratingCount', 0),
    
    // Weekly Metrics
    weeklyUsageCount: validateNumberField('weeklyUsageCount', 0),
    weeklyLikes: validateNumberField('weeklyLikes', 0),
    weeklyFavorites: validateNumberField('weeklyFavorites', 0),
    weeklyViewCount: validateNumberField('weeklyViewCount', 0),
    weeklySharedCount: validateNumberField('weeklySharedCount', 0),
    weeklyDownloadCount: validateNumberField('weeklyDownloadCount', 0),
    weeklyReportCount: validateNumberField('weeklyReportCount', 0),
    
    // AI Fields
    isAIGenerated: validateBooleanField('isAIGenerated'),
    aiPrompt: validateStringField('aiPrompt', 1000),
    aiModel: validateStringField('aiModel', 100),
    aiStyle: validateStringField('aiStyle', 100),
    aiGenerationStage: validateEnumField('aiGenerationStage', ['initial', 'on_edit', 'variation']),
    generatedByUser: validateObjectIdField('generatedByUser'),
    generatedByUserUid: validateFirebaseUid('generatedByUserUid'),
    
    // Categorization Fields
    creatorId: validateObjectIdField('creatorId'),
    creatorUid: validateFirebaseUid('creatorUid'),
    festivalTag: validateStringField('festivalTag', 100),
    tags: validateArrayField('tags', 'string', 20),
    styleTags: validateArrayField('styleTags', 'string', 10),
    searchKeywords: validateArrayField('searchKeywords', 'string', 30),
    variationOf: validateObjectIdField('variationOf'),
    relatedTemplates: validateArrayField('relatedTemplates', 'objectId', 10),
    
    // Visibility Fields
    visibilityScore: validateNumberField('visibilityScore'),
    boostPriority: validateNumberField('boostPriority', 0),
    ignoredByUsers: validateArrayField('ignoredByUsers', 'objectId', 1000),
    templateType: validateEnumField('templateType', ['html', 'image', 'video']),
    
    // Legacy Fields
    categoryIcon: validateStringField('categoryIcon', 500, false, true),
    language: validateObjectIdField('language'),
    region: validateObjectIdField('region'),
    experimentTag: validateStringField('experimentTag', 100)
};

/**
 * Validates template ID parameter
 */
const validateTemplateId = (req, res, next) => {
    const templateId = req.params.templateId;
    
    if (!templateId || !isValidObjectId(templateId)) {
        return res.status(400).json({
            success: false,
            error: {
                code: 'VALIDATION_ERROR',
                message: 'Invalid template ID',
                field: 'templateId',
                value: templateId
            },
            timestamp: new Date().toISOString()
        });
    }
    
    next();
};

/**
 * Validates engagement data for updates
 */
const validateEngagementData = (req, res, next) => {
    const allowedFields = [
        'usageCount', 'likes', 'favorites', 'viewCount', 
        'sharedCount', 'downloadCount', 'reportCount', 
        'rating', 'ratingCount'
    ];
    
    const updates = {};
    let hasValidUpdates = false;
    
    for (const field of allowedFields) {
        if (req.body[field] !== undefined) {
            const value = req.body[field];
            
            if (field === 'rating') {
                if (typeof value !== 'number' || value < 0 || value > 5) {
                    return res.status(400).json({
                        success: false,
                        error: {
                            code: 'VALIDATION_ERROR',
                            message: 'Rating must be a number between 0 and 5',
                            field: field,
                            value: value
                        },
                        timestamp: new Date().toISOString()
                    });
                }
            } else {
                if (typeof value !== 'number' || value < 0) {
                    return res.status(400).json({
                        success: false,
                        error: {
                            code: 'VALIDATION_ERROR',
                            message: `${field} must be a non-negative number`,
                            field: field,
                            value: value
                        },
                        timestamp: new Date().toISOString()
                    });
                }
            }
            
            updates[field] = value;
            hasValidUpdates = true;
        }
    }
    
    if (!hasValidUpdates) {
        return res.status(400).json({
            success: false,
            error: {
                code: 'VALIDATION_ERROR',
                message: 'No valid engagement fields provided for update',
                details: { allowedFields }
            },
            timestamp: new Date().toISOString()
        });
    }
    
    req.validatedUpdates = updates;
    next();
};

/**
 * Validates batch engagement data
 */
const validateBatchEngagementData = (req, res, next) => {
    const { templates } = req.body;
    
    if (!Array.isArray(templates) || templates.length === 0) {
        return res.status(400).json({
            success: false,
            error: {
                code: 'VALIDATION_ERROR',
                message: 'templates must be a non-empty array',
                field: 'templates'
            },
            timestamp: new Date().toISOString()
        });
    }
    
    const allowedFields = [
        'usageCount', 'likes', 'favorites', 'viewCount', 
        'sharedCount', 'downloadCount', 'reportCount'
    ];
    
    for (let i = 0; i < templates.length; i++) {
        const template = templates[i];
        
        if (!template.templateId || !isValidObjectId(template.templateId)) {
            return res.status(400).json({
                success: false,
                error: {
                    code: 'VALIDATION_ERROR',
                    message: `Invalid templateId at index ${i}`,
                    field: `templates[${i}].templateId`,
                    value: template.templateId
                },
                timestamp: new Date().toISOString()
            });
        }
        
        if (!template.increments || typeof template.increments !== 'object') {
            return res.status(400).json({
                success: false,
                error: {
                    code: 'VALIDATION_ERROR',
                    message: `increments must be an object at index ${i}`,
                    field: `templates[${i}].increments`
                },
                timestamp: new Date().toISOString()
            });
        }
        
        for (const field in template.increments) {
            if (!allowedFields.includes(field)) {
                return res.status(400).json({
                    success: false,
                    error: {
                        code: 'VALIDATION_ERROR',
                        message: `Invalid field: ${field} at index ${i}`,
                        field: `templates[${i}].increments.${field}`,
                        details: { allowedFields }
                    },
                    timestamp: new Date().toISOString()
                });
            }
            
            const value = template.increments[field];
            if (typeof value !== 'number' || value < 0) {
                return res.status(400).json({
                    success: false,
                    error: {
                        code: 'VALIDATION_ERROR',
                        message: `${field} must be a non-negative number at index ${i}`,
                        field: `templates[${i}].increments.${field}`,
                        value: value
                    },
                    timestamp: new Date().toISOString()
                });
            }
        }
    }
    
    next();
};

/**
 * Validates weekly metrics data
 */
const validateWeeklyMetricsData = (req, res, next) => {
    const allowedFields = [
        'weeklyUsageCount', 'weeklyLikes', 'weeklyFavorites', 
        'weeklyViewCount', 'weeklySharedCount', 'weeklyDownloadCount', 
        'weeklyReportCount', 'weeklyScoreLastReset'
    ];
    
    const updates = {};
    let hasValidUpdates = false;
    
    for (const field of allowedFields) {
        if (req.body[field] !== undefined) {
            const value = req.body[field];
            
            if (field === 'weeklyScoreLastReset') {
                if (!(value instanceof Date) && !Date.parse(value)) {
                    return res.status(400).json({
                        success: false,
                        error: {
                            code: 'VALIDATION_ERROR',
                            message: 'weeklyScoreLastReset must be a valid date',
                            field: field,
                            value: value
                        },
                        timestamp: new Date().toISOString()
                    });
                }
            } else {
                if (typeof value !== 'number' || value < 0) {
                    return res.status(400).json({
                        success: false,
                        error: {
                            code: 'VALIDATION_ERROR',
                            message: `${field} must be a non-negative number`,
                            field: field,
                            value: value
                        },
                        timestamp: new Date().toISOString()
                    });
                }
            }
            
            updates[field] = value;
            hasValidUpdates = true;
        }
    }
    
    if (!hasValidUpdates) {
        return res.status(400).json({
            success: false,
            error: {
                code: 'VALIDATION_ERROR',
                message: 'No valid weekly metrics fields provided for update',
                details: { allowedFields }
            },
            timestamp: new Date().toISOString()
        });
    }
    
    req.validatedUpdates = updates;
    next();
};

/**
 * Validates performance data
 */
const validatePerformanceData = (req, res, next) => {
    const { performanceLog } = req.body;
    
    if (!performanceLog || typeof performanceLog !== 'object') {
        return res.status(400).json({
            success: false,
            error: {
                code: 'VALIDATION_ERROR',
                message: 'performanceLog must be an object',
                field: 'performanceLog'
            },
            timestamp: new Date().toISOString()
        });
    }
    
    const allowedFields = ['dailyUsage', 'weeklyUsage', 'lastUsedAt'];
    const updates = {};
    let hasValidUpdates = false;
    
    for (const field of allowedFields) {
        if (performanceLog[field] !== undefined) {
            const value = performanceLog[field];
            
            if (field === 'lastUsedAt') {
                if (!(value instanceof Date) && !Date.parse(value)) {
                    return res.status(400).json({
                        success: false,
                        error: {
                            code: 'VALIDATION_ERROR',
                            message: 'lastUsedAt must be a valid date',
                            field: `performanceLog.${field}`,
                            value: value
                        },
                        timestamp: new Date().toISOString()
                    });
                }
            } else {
                if (typeof value !== 'number' || value < 0) {
                    return res.status(400).json({
                        success: false,
                        error: {
                            code: 'VALIDATION_ERROR',
                            message: `${field} must be a non-negative number`,
                            field: `performanceLog.${field}`,
                            value: value
                        },
                        timestamp: new Date().toISOString()
                    });
                }
            }
            
            updates[field] = value;
            hasValidUpdates = true;
        }
    }
    
    if (!hasValidUpdates) {
        return res.status(400).json({
            success: false,
            error: {
                code: 'VALIDATION_ERROR',
                message: 'No valid performance log fields provided for update',
                details: { allowedFields }
            },
            timestamp: new Date().toISOString()
        });
    }
    
    req.validatedPerformanceLog = updates;
    next();
};

/**
 * Validates template exists in database
 */
const validateTemplateExists = async (req, res, next) => {
    try {
        const Template = require('../models/Template');
        const template = await Template.findById(req.params.templateId).select('_id');
        
        if (!template) {
            return res.status(404).json({
                success: false,
                error: {
                    code: 'TEMPLATE_NOT_FOUND',
                    message: 'Template not found',
                    templateId: req.params.templateId
                },
                timestamp: new Date().toISOString()
            });
        }
        
        next();
    } catch (error) {
        return res.status(500).json({
            success: false,
            error: {
                code: 'SERVER_ERROR',
                message: 'Error checking template existence',
                details: error.message
            },
            timestamp: new Date().toISOString()
        });
    }
};

/**
 * Categorization validation middleware
 */
const validateCategorization = (req, res, next) => {
    const errors = [];
    
    // Validate creator fields
    if (req.body.creatorId !== undefined) {
        if (req.body.creatorId && !isValidObjectId(req.body.creatorId)) {
            errors.push('creatorId must be a valid ObjectId');
        }
    }
    
    if (req.body.creatorUid !== undefined) {
        if (req.body.creatorUid && typeof req.body.creatorUid !== 'string') {
            errors.push('creatorUid must be a string');
        }
    }
    
    if (req.body.category !== undefined) {
        if (req.body.category && typeof req.body.category !== 'string') {
            errors.push('category must be a string');
        }
    }
    
    if (req.body.festivalTag !== undefined) {
        if (req.body.festivalTag && typeof req.body.festivalTag !== 'string') {
            errors.push('festivalTag must be a string');
        }
    }
    
    // Validate tag arrays
    const tagFields = ['tags', 'styleTags', 'searchKeywords'];
    tagFields.forEach(field => {
        if (req.body[field] !== undefined) {
            if (!Array.isArray(req.body[field])) {
                errors.push(`${field} must be an array`);
            } else if (req.body[field].length > 50) {
                errors.push(`${field} cannot exceed 50 items`);
            }
        }
    });
    
    // Validate relation arrays
    const relationFields = ['relatedTemplates', 'similarTemplates', 'templateVariants'];
    relationFields.forEach(field => {
        if (req.body[field] !== undefined) {
            if (!Array.isArray(req.body[field])) {
                errors.push(`${field} must be an array`);
            } else if (req.body[field].length > 50) {
                errors.push(`${field} cannot exceed 50 items`);
            } else {
                // Validate ObjectIds in array
                const invalidIds = req.body[field].filter(id => !isValidObjectId(id));
                if (invalidIds.length > 0) {
                    errors.push(`${field} contains invalid ObjectIds: ${invalidIds.join(', ')}`);
                }
            }
        }
    });
    
    if (errors.length > 0) {
        return res.status(400).json({
            success: false,
            error: 'VALIDATION_ERROR',
            message: 'Categorization validation failed',
            details: errors,
            timestamp: new Date().toISOString()
        });
    }
    
    next();
};

/**
 * Validate visibility score
 */
const validateVisibilityScore = (req, res, next) => {
    const { visibilityScore } = req.body;
    
    if (visibilityScore !== undefined) {
        if (typeof visibilityScore !== 'number' || visibilityScore < 0) {
            return res.status(400).json({
                success: false,
                error: {
                    code: 'VALIDATION_ERROR',
                    message: 'Visibility score must be a non-negative number',
                    field: 'visibilityScore',
                    value: visibilityScore
                },
                timestamp: new Date().toISOString()
            });
        }
    }
    
    next();
};

/**
 * Validate boost priority
 */
const validateBoostPriority = (req, res, next) => {
    const { boostPriority } = req.body;
    
    if (boostPriority !== undefined) {
        if (typeof boostPriority !== 'number' || boostPriority < 0) {
            return res.status(400).json({
                success: false,
                error: {
                    code: 'VALIDATION_ERROR',
                    message: 'Boost priority must be a non-negative number',
                    field: 'boostPriority',
                    value: boostPriority
                },
                timestamp: new Date().toISOString()
            });
        }
    }
    
    next();
};

/**
 * Validate boost duration
 */
const validateBoostDuration = (req, res, next) => {
    const { duration } = req.body;
    
    if (duration !== undefined) {
        if (typeof duration !== 'number' || duration <= 0 || duration > 8760) { // Max 1 year
            return res.status(400).json({
                success: false,
                error: {
                    code: 'VALIDATION_ERROR',
                    message: 'Boost duration must be a positive number (hours) and not exceed 8760 hours (1 year)',
                    field: 'duration',
                    value: duration
                },
                timestamp: new Date().toISOString()
            });
        }
    }
    
    next();
};

/**
 * Validate boost schedule
 */
const validateBoostSchedule = (req, res, next) => {
    const { scheduledAt, boostPriority, duration } = req.body;
    
    if (!scheduledAt) {
        return res.status(400).json({
            success: false,
            error: {
                code: 'VALIDATION_ERROR',
                message: 'Scheduled time is required',
                field: 'scheduledAt',
                value: scheduledAt
            },
            timestamp: new Date().toISOString()
        });
    }
    
    const scheduleDate = new Date(scheduledAt);
    if (isNaN(scheduleDate.getTime()) || scheduleDate <= new Date()) {
        return res.status(400).json({
            success: false,
            error: {
                code: 'VALIDATION_ERROR',
                message: 'Scheduled time must be a valid future date',
                field: 'scheduledAt',
                value: scheduledAt
            },
            timestamp: new Date().toISOString()
        });
    }
    
    if (boostPriority !== undefined && (typeof boostPriority !== 'number' || boostPriority < 0)) {
        return res.status(400).json({
            success: false,
            error: {
                code: 'VALIDATION_ERROR',
                message: 'Boost priority must be a non-negative number',
                field: 'boostPriority',
                value: boostPriority
            },
            timestamp: new Date().toISOString()
        });
    }
    
    if (duration !== undefined && (typeof duration !== 'number' || duration <= 0)) {
        return res.status(400).json({
            success: false,
            error: {
                code: 'VALIDATION_ERROR',
                message: 'Duration must be a positive number',
                field: 'duration',
                value: duration
            },
            timestamp: new Date().toISOString()
        });
    }
    
    next();
};

/**
 * Validate user ID
 */
const validateUserId = (req, res, next) => {
    const { userId } = req.params;
    
    if (!userId || typeof userId !== 'string' || userId.trim().length === 0) {
        return res.status(400).json({
            success: false,
            error: {
                code: 'VALIDATION_ERROR',
                message: 'Valid user ID is required',
                field: 'userId',
                value: userId
            },
            timestamp: new Date().toISOString()
        });
    }
    
    next();
};

/**
 * Validate user IDs array
 */
const validateUserIds = (req, res, next) => {
    const { userIds } = req.body;
    
    if (!Array.isArray(userIds) || userIds.length === 0) {
        return res.status(400).json({
            success: false,
            error: {
                code: 'VALIDATION_ERROR',
                message: 'User IDs array is required and cannot be empty',
                field: 'userIds',
                value: userIds
            },
            timestamp: new Date().toISOString()
        });
    }
    
    for (const userId of userIds) {
        if (!userId || typeof userId !== 'string' || userId.trim().length === 0) {
            return res.status(400).json({
                success: false,
                error: {
                    code: 'VALIDATION_ERROR',
                    message: 'All user IDs must be valid non-empty strings',
                    field: 'userIds',
                    value: userId
                },
                timestamp: new Date().toISOString()
            });
        }
    }
    
    next();
};

/**
 * Validate template type enum
 */
const validateTemplateTypeEnum = (req, res, next) => {
    const templateType = req.body.templateType || req.params.type;
    const validTypes = ['html', 'image', 'video'];
    
    if (templateType && !validTypes.includes(templateType)) {
        return res.status(400).json({
            success: false,
            error: {
                code: 'VALIDATION_ERROR',
                message: `Invalid template type. Must be one of: ${validTypes.join(', ')}`,
                field: 'templateType',
                value: templateType
            },
            timestamp: new Date().toISOString()
        });
    }
    
    next();
};

/**
 * Validate customization options data
 */
const validateCustomizationOptionsData = (req, res, next) => {
    const { customizationOptions } = req.body;
    
    if (!customizationOptions || typeof customizationOptions !== 'object') {
        return res.status(400).json({
            success: false,
            error: {
                code: 'VALIDATION_ERROR',
                message: 'Customization options object is required',
                field: 'customizationOptions',
                value: customizationOptions
            },
            timestamp: new Date().toISOString()
        });
    }
    
    const validOptions = ['allowTextEdit', 'allowColorChange', 'allowImageUpload', 'allowFontChange', 'allowLayoutChange'];
    const errors = [];
    
    Object.keys(customizationOptions).forEach(key => {
        if (!validOptions.includes(key)) {
            errors.push(`Invalid customization option: ${key}`);
        } else if (typeof customizationOptions[key] !== 'boolean') {
            errors.push(`Option '${key}' must be a boolean value`);
        }
    });
    
    if (errors.length > 0) {
        return res.status(400).json({
            success: false,
            error: {
                code: 'VALIDATION_ERROR',
                message: 'Invalid customization options',
                details: errors
            },
            timestamp: new Date().toISOString()
        });
    }
    
    req.validatedCustomizationOptions = customizationOptions;
    next();
};

/**
 * Validate permission data
 */
const validatePermissionData = (req, res, next) => {
    const { permissions } = req.body;
    
    if (!permissions || typeof permissions !== 'object') {
        return res.status(400).json({
            success: false,
            error: {
                code: 'VALIDATION_ERROR',
                message: 'Permissions object is required',
                field: 'permissions',
                value: permissions
            },
            timestamp: new Date().toISOString()
        });
    }
    
    const validPermissions = ['allowTextEdit', 'allowColorChange', 'allowImageUpload', 'allowFontChange', 'allowLayoutChange'];
    const errors = [];
    
    Object.keys(permissions).forEach(key => {
        if (!validPermissions.includes(key)) {
            errors.push(`Invalid permission: ${key}`);
        } else if (typeof permissions[key] !== 'boolean') {
            errors.push(`Permission '${key}' must be a boolean value`);
        }
    });
    
    if (errors.length > 0) {
        return res.status(400).json({
            success: false,
            error: {
                code: 'VALIDATION_ERROR',
                message: 'Invalid permissions data',
                details: errors
            },
            timestamp: new Date().toISOString()
        });
    }
    
    next();
};

/**
 * Validate legacy fields data
 */
const validateLegacyFields = (req, res, next) => {
    const { categoryIcon, moderationStatus, language, region } = req.body;
    const errors = [];

    if (categoryIcon !== undefined && typeof categoryIcon !== 'string') {
        errors.push('categoryIcon must be a string');
    }

    if (moderationStatus !== undefined) {
        const validStatuses = ['pending', 'approved', 'rejected'];
        if (!validStatuses.includes(moderationStatus)) {
            errors.push(`moderationStatus must be one of: ${validStatuses.join(', ')}`);
        }
    }

    if (language !== undefined && typeof language !== 'string') {
        errors.push('language must be a string');
    }

    if (region !== undefined && typeof region !== 'string') {
        errors.push('region must be a string');
    }

    if (errors.length > 0) {
        return res.status(400).json({
            success: false,
            error: {
                code: 'VALIDATION_ERROR',
                message: 'Legacy fields validation failed',
                details: errors
            },
            timestamp: new Date().toISOString()
        });
    }

    next();
};

/**
 * Validate reference data
 */
const validateReferenceData = (req, res, next) => {
    const { referenceType, referenceId, referenceData } = req.body;
    const errors = [];

    if (!referenceType || typeof referenceType !== 'string') {
        errors.push('referenceType is required and must be a string');
    }

    if (!referenceId || typeof referenceId !== 'string') {
        errors.push('referenceId is required and must be a string');
    }

    if (referenceData !== undefined && typeof referenceData !== 'object') {
        errors.push('referenceData must be an object');
    }

    if (errors.length > 0) {
        return res.status(400).json({
            success: false,
            error: {
                code: 'VALIDATION_ERROR',
                message: 'Reference data validation failed',
                details: errors
            },
            timestamp: new Date().toISOString()
        });
    }

    next();
};

/**
 * Validate experiment data
 */
const validateExperimentData = (req, res, next) => {
    const { experimentTag, experimentData, templateIds, experimentConfig } = req.body;
    const errors = [];

    if (experimentTag !== undefined && typeof experimentTag !== 'string') {
        errors.push('experimentTag must be a string');
    }

    if (experimentData !== undefined && typeof experimentData !== 'object') {
        errors.push('experimentData must be an object');
    }

    if (templateIds !== undefined && !Array.isArray(templateIds)) {
        errors.push('templateIds must be an array');
    }

    if (experimentConfig !== undefined && typeof experimentConfig !== 'object') {
        errors.push('experimentConfig must be an object');
    }

    if (errors.length > 0) {
        return res.status(400).json({
            success: false,
            error: {
                code: 'VALIDATION_ERROR',
                message: 'Experiment data validation failed',
                details: errors
            },
            timestamp: new Date().toISOString()
        });
    }

    next();
};

module.exports = {
    isValidObjectId,
    validateStringField,
    validateNumberField,
    validateBooleanField,
    validateArrayField,
    validateObjectIdField,
    validateEnumField,
    validateFirebaseUid,
    templateFieldValidators,
    validateTemplateId,
    validateTemplateExists,
    validateEngagementData,
    validateBatchEngagementData,
    validateWeeklyMetricsData,
    validatePerformanceData,
    validateCategorization,
    validateVisibilityScore,
    validateBoostPriority,
    validateBoostDuration,
    validateBoostSchedule,
    validateUserId,
    validateUserIds,
    validateTemplateTypeEnum,
    validateCustomizationOptionsData,
    validatePermissionData,
    validateLegacyFields,
    validateReferenceData,
    validateExperimentData
};
