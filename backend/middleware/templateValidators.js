const { validateFields, formatErrorResponse, isValidObjectId } = require('../utils/templateHelpers');
const Template = require('../models/Template');
const logger = require('../utils/logger');

/**
 * Template Validation Middleware
 * Field-type specific validation middleware for template endpoints
 */

/**
 * Validates template ID parameter
 */
const validateTemplateId = (req, res, next) => {
    const { templateId } = req.params;
    
    if (!templateId) {
        return res.status(400).json(formatErrorResponse(
            'MISSING_PARAMETER',
            'Template ID is required',
            'templateId'
        ));
    }
    
    if (!isValidObjectId(templateId)) {
        return res.status(400).json(formatErrorResponse(
            'INVALID_ID',
            'Invalid template ID format',
            'templateId',
            templateId
        ));
    }
    
    next();
};

/**
 * Validates that template exists
 */
const validateTemplateExists = async (req, res, next) => {
    try {
        const { templateId } = req.params;
        const template = await Template.findById(templateId);
        
        if (!template) {
            return res.status(404).json(formatErrorResponse(
                'TEMPLATE_NOT_FOUND',
                'Template not found',
                'templateId',
                templateId
            ));
        }
        
        req.template = template;
        next();
    } catch (error) {
        logger.error('Error validating template existence:', error);
        return res.status(500).json(formatErrorResponse(
            'SERVER_ERROR',
            'Error validating template'
        ));
    }
};

/**
 * Validates core content fields
 */
const validateCoreContent = (req, res, next) => {
    const { title, category, htmlContent, cssContent, jsContent, previewUrl, videoUrl, imageUrl } = req.body;
    
    const fieldsToValidate = {};
    if (title !== undefined) fieldsToValidate.title = title;
    if (category !== undefined) fieldsToValidate.category = category;
    if (htmlContent !== undefined) fieldsToValidate.htmlContent = htmlContent;
    if (cssContent !== undefined) fieldsToValidate.cssContent = cssContent;
    if (jsContent !== undefined) fieldsToValidate.jsContent = jsContent;
    if (previewUrl !== undefined) fieldsToValidate.previewUrl = previewUrl;
    if (videoUrl !== undefined) fieldsToValidate.videoUrl = videoUrl;
    if (imageUrl !== undefined) fieldsToValidate.imageUrl = imageUrl;
    
    const validation = validateFields(fieldsToValidate);
    
    if (!validation.isValid) {
        return res.status(400).json(formatErrorResponse(
            'VALIDATION_ERROR',
            'Core content validation failed',
            validation.errors[0].field,
            validation.errors[0].value,
            validation.errors.map(e => e.error).join(', ')
        ));
    }
    
    next();
};

/**
 * Validates monetization fields
 */
const validateMonetization = (req, res, next) => {
    const { status, isPremium, isFeatured, isTrending, isFlagged, isLowPerforming, price, moderationStatus } = req.body;
    
    const fieldsToValidate = {};
    if (status !== undefined) fieldsToValidate.status = status;
    if (isPremium !== undefined) fieldsToValidate.isPremium = isPremium;
    if (isFeatured !== undefined) fieldsToValidate.isFeatured = isFeatured;
    if (isTrending !== undefined) fieldsToValidate.isTrending = isTrending;
    if (isFlagged !== undefined) fieldsToValidate.isFlagged = isFlagged;
    if (isLowPerforming !== undefined) fieldsToValidate.isLowPerforming = isLowPerforming;
    if (price !== undefined) fieldsToValidate.price = price;
    if (moderationStatus !== undefined) fieldsToValidate.moderationStatus = moderationStatus;
    
    const validation = validateFields(fieldsToValidate);
    
    if (!validation.isValid) {
        return res.status(400).json(formatErrorResponse(
            'VALIDATION_ERROR',
            'Monetization validation failed',
            validation.errors[0].field,
            validation.errors[0].value,
            validation.errors.map(e => e.error).join(', ')
        ));
    }
    
    next();
};

/**
 * Validates metrics fields
 */
const validateMetrics = (req, res, next) => {
    const { 
        usageCount, likes, favorites, viewCount, sharedCount, downloadCount, 
        reportCount, rating, ratingCount, visibilityScore, boostPriority 
    } = req.body;
    
    const fieldsToValidate = {};
    if (usageCount !== undefined) fieldsToValidate.usageCount = usageCount;
    if (likes !== undefined) fieldsToValidate.likes = likes;
    if (favorites !== undefined) fieldsToValidate.favorites = favorites;
    if (viewCount !== undefined) fieldsToValidate.viewCount = viewCount;
    if (sharedCount !== undefined) fieldsToValidate.sharedCount = sharedCount;
    if (downloadCount !== undefined) fieldsToValidate.downloadCount = downloadCount;
    if (reportCount !== undefined) fieldsToValidate.reportCount = reportCount;
    if (rating !== undefined) fieldsToValidate.rating = rating;
    if (ratingCount !== undefined) fieldsToValidate.ratingCount = ratingCount;
    if (visibilityScore !== undefined) fieldsToValidate.visibilityScore = visibilityScore;
    if (boostPriority !== undefined) fieldsToValidate.boostPriority = boostPriority;
    
    const validation = validateFields(fieldsToValidate);
    
    if (!validation.isValid) {
        return res.status(400).json(formatErrorResponse(
            'VALIDATION_ERROR',
            'Metrics validation failed',
            validation.errors[0].field,
            validation.errors[0].value,
            validation.errors.map(e => e.error).join(', ')
        ));
    }
    
    next();
};

/**
 * Validates weekly metrics fields
 */
const validateWeeklyMetrics = (req, res, next) => {
    const { 
        weeklyUsageCount, weeklyLikes, weeklyFavorites, weeklyViewCount, 
        weeklySharedCount, weeklyDownloadCount, weeklyReportCount, weeklyScoreLastReset 
    } = req.body;
    
    const fieldsToValidate = {};
    if (weeklyUsageCount !== undefined) fieldsToValidate.weeklyUsageCount = weeklyUsageCount;
    if (weeklyLikes !== undefined) fieldsToValidate.weeklyLikes = weeklyLikes;
    if (weeklyFavorites !== undefined) fieldsToValidate.weeklyFavorites = weeklyFavorites;
    if (weeklyViewCount !== undefined) fieldsToValidate.weeklyViewCount = weeklyViewCount;
    if (weeklySharedCount !== undefined) fieldsToValidate.weeklySharedCount = weeklySharedCount;
    if (weeklyDownloadCount !== undefined) fieldsToValidate.weeklyDownloadCount = weeklyDownloadCount;
    if (weeklyReportCount !== undefined) fieldsToValidate.weeklyReportCount = weeklyReportCount;
    if (weeklyScoreLastReset !== undefined) fieldsToValidate.weeklyScoreLastReset = weeklyScoreLastReset;
    
    const validation = validateFields(fieldsToValidate);
    
    if (!validation.isValid) {
        return res.status(400).json(formatErrorResponse(
            'VALIDATION_ERROR',
            'Weekly metrics validation failed',
            validation.errors[0].field,
            validation.errors[0].value,
            validation.errors.map(e => e.error).join(', ')
        ));
    }
    
    next();
};

/**
 * Validates AI metadata fields
 */
const validateAIMetadata = (req, res, next) => {
    const { 
        isAIGenerated, aiPrompt, aiModel, aiStyle, aiGenerationStage, 
        generatedByUser, generatedByUserUid, generationMetadata 
    } = req.body;
    
    const fieldsToValidate = {};
    if (isAIGenerated !== undefined) fieldsToValidate.isAIGenerated = isAIGenerated;
    if (aiPrompt !== undefined) fieldsToValidate.aiPrompt = aiPrompt;
    if (aiModel !== undefined) fieldsToValidate.aiModel = aiModel;
    if (aiStyle !== undefined) fieldsToValidate.aiStyle = aiStyle;
    if (aiGenerationStage !== undefined) fieldsToValidate.aiGenerationStage = aiGenerationStage;
    if (generatedByUser !== undefined) fieldsToValidate.generatedByUser = generatedByUser;
    if (generatedByUserUid !== undefined) fieldsToValidate.generatedByUserUid = generatedByUserUid;
    
    // Validate generation metadata structure if provided
    if (generationMetadata !== undefined) {
        if (typeof generationMetadata !== 'object' || generationMetadata === null) {
            return res.status(400).json(formatErrorResponse(
                'VALIDATION_ERROR',
                'Generation metadata must be an object',
                'generationMetadata',
                generationMetadata
            ));
        }
    }
    
    const validation = validateFields(fieldsToValidate);
    
    if (!validation.isValid) {
        return res.status(400).json(formatErrorResponse(
            'VALIDATION_ERROR',
            'AI metadata validation failed',
            validation.errors[0].field,
            validation.errors[0].value,
            validation.errors.map(e => e.error).join(', ')
        ));
    }
    
    next();
};

/**
 * Validates categorization fields
 */
const validateCategorization = (req, res, next) => {
    const { 
        creatorId, creatorUid, festivalTag, tags, styleTags, 
        searchKeywords, variationOf, relatedTemplates 
    } = req.body;
    
    const fieldsToValidate = {};
    if (creatorId !== undefined) fieldsToValidate.creatorId = creatorId;
    if (creatorUid !== undefined) fieldsToValidate.creatorUid = creatorUid;
    if (festivalTag !== undefined) fieldsToValidate.festivalTag = festivalTag;
    if (tags !== undefined) fieldsToValidate.tags = tags;
    if (styleTags !== undefined) fieldsToValidate.styleTags = styleTags;
    if (searchKeywords !== undefined) fieldsToValidate.searchKeywords = searchKeywords;
    if (variationOf !== undefined) fieldsToValidate.variationOf = variationOf;
    if (relatedTemplates !== undefined) fieldsToValidate.relatedTemplates = relatedTemplates;
    
    const validation = validateFields(fieldsToValidate);
    
    if (!validation.isValid) {
        return res.status(400).json(formatErrorResponse(
            'VALIDATION_ERROR',
            'Categorization validation failed',
            validation.errors[0].field,
            validation.errors[0].value,
            validation.errors.map(e => e.error).join(', ')
        ));
    }
    
    next();
};

/**
 * Validates visibility fields
 */
const validateVisibility = (req, res, next) => {
    const { templateType, visibilityScore, boostPriority, lastBoostedAt, ignoredByUsers } = req.body;
    
    const fieldsToValidate = {};
    if (templateType !== undefined) fieldsToValidate.templateType = templateType;
    if (visibilityScore !== undefined) fieldsToValidate.visibilityScore = visibilityScore;
    if (boostPriority !== undefined) fieldsToValidate.boostPriority = boostPriority;
    if (lastBoostedAt !== undefined) fieldsToValidate.lastBoostedAt = lastBoostedAt;
    if (ignoredByUsers !== undefined) fieldsToValidate.ignoredByUsers = ignoredByUsers;
    
    const validation = validateFields(fieldsToValidate);
    
    if (!validation.isValid) {
        return res.status(400).json(formatErrorResponse(
            'VALIDATION_ERROR',
            'Visibility validation failed',
            validation.errors[0].field,
            validation.errors[0].value,
            validation.errors.map(e => e.error).join(', ')
        ));
    }
    
    next();
};

/**
 * Validates customization options
 */
const validateCustomization = (req, res, next) => {
    const { customizationOptions } = req.body;
    
    if (customizationOptions !== undefined) {
        if (typeof customizationOptions !== 'object' || customizationOptions === null) {
            return res.status(400).json(formatErrorResponse(
                'VALIDATION_ERROR',
                'Customization options must be an object',
                'customizationOptions',
                customizationOptions
            ));
        }
        
        // Validate individual customization options
        const validOptions = ['allowNameEdit', 'allowPhotoEdit', 'allowBackgroundChange', 'allowMusic', 'allowThemeCustomization'];
        for (const [key, value] of Object.entries(customizationOptions)) {
            if (!validOptions.includes(key)) {
                return res.status(400).json(formatErrorResponse(
                    'VALIDATION_ERROR',
                    `Invalid customization option: ${key}`,
                    'customizationOptions',
                    key
                ));
            }
            
            if (typeof value !== 'boolean') {
                return res.status(400).json(formatErrorResponse(
                    'VALIDATION_ERROR',
                    `Customization option ${key} must be a boolean`,
                    'customizationOptions',
                    value
                ));
            }
        }
    }
    
    next();
};

/**
 * Validates legacy fields
 */
const validateLegacy = (req, res, next) => {
    const { categoryIcon, language, region, experimentTag, performanceLog } = req.body;
    
    const fieldsToValidate = {};
    if (categoryIcon !== undefined) fieldsToValidate.categoryIcon = categoryIcon;
    if (language !== undefined) fieldsToValidate.language = language;
    if (region !== undefined) fieldsToValidate.region = region;
    if (experimentTag !== undefined) fieldsToValidate.experimentTag = experimentTag;
    
    // Validate performance log structure if provided
    if (performanceLog !== undefined) {
        if (typeof performanceLog !== 'object' || performanceLog === null) {
            return res.status(400).json(formatErrorResponse(
                'VALIDATION_ERROR',
                'Performance log must be an object',
                'performanceLog',
                performanceLog
            ));
        }
        
        const { dailyUsage, weeklyUsage, lastUsedAt } = performanceLog;
        if (dailyUsage !== undefined && (typeof dailyUsage !== 'number' || dailyUsage < 0)) {
            return res.status(400).json(formatErrorResponse(
                'VALIDATION_ERROR',
                'Daily usage must be a non-negative number',
                'performanceLog.dailyUsage',
                dailyUsage
            ));
        }
        
        if (weeklyUsage !== undefined && (typeof weeklyUsage !== 'number' || weeklyUsage < 0)) {
            return res.status(400).json(formatErrorResponse(
                'VALIDATION_ERROR',
                'Weekly usage must be a non-negative number',
                'performanceLog.weeklyUsage',
                weeklyUsage
            ));
        }
        
        if (lastUsedAt !== undefined && isNaN(Date.parse(lastUsedAt))) {
            return res.status(400).json(formatErrorResponse(
                'VALIDATION_ERROR',
                'Last used at must be a valid date',
                'performanceLog.lastUsedAt',
                lastUsedAt
            ));
        }
    }
    
    const validation = validateFields(fieldsToValidate);
    
    if (!validation.isValid) {
        return res.status(400).json(formatErrorResponse(
            'VALIDATION_ERROR',
            'Legacy fields validation failed',
            validation.errors[0].field,
            validation.errors[0].value,
            validation.errors.map(e => e.error).join(', ')
        ));
    }
    
    next();
};

/**
 * Validates required fields for template creation
 */
const validateRequiredFields = (req, res, next) => {
    const { title, category, htmlContent } = req.body;
    
    const requiredFields = { title, category, htmlContent };
    const validation = validateFields(requiredFields);
    
    if (!validation.isValid) {
        return res.status(400).json(formatErrorResponse(
            'VALIDATION_ERROR',
            'Required fields validation failed',
            validation.errors[0].field,
            validation.errors[0].value,
            'Title, category, and HTML content are required for template creation'
        ));
    }
    
    next();
};

module.exports = {
    validateTemplateId,
    validateTemplateExists,
    validateCoreContent,
    validateMonetization,
    validateMetrics,
    validateWeeklyMetrics,
    validateAIMetadata,
    validateCategorization,
    validateVisibility,
    validateCustomization,
    validateLegacy,
    validateRequiredFields
}; 