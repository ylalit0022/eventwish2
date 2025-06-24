const Template = require('../../../models/Template');
const mongoose = require('mongoose');
const logger = require('../../../utils/logger');

/**
 * Tags Controller
 * Handles tag-related operations for templates:
 * - tags (Array of Strings)
 * - styleTags (Array of Strings)
 * - searchKeywords (Array of Strings)
 */

// Helper function to handle async operations
const handleAsyncOperation = async (operation, res, successMessage) => {
    try {
        const result = await operation();
        logger.info(`Tags operation successful: ${successMessage}`);
        return res.status(200).json({
            success: true,
            message: successMessage,
            data: result,
            timestamp: new Date().toISOString()
        });
    } catch (error) {
        logger.error(`Tags operation failed: ${error.message}`, { error: error.stack });
        
        if (error.name === 'ValidationError') {
            return res.status(400).json({
                success: false,
                error: 'VALIDATION_ERROR',
                message: 'Validation failed',
                details: Object.values(error.errors).map(err => ({
                    field: err.path,
                    message: err.message,
                    value: err.value
                })),
                timestamp: new Date().toISOString()
            });
        }
        
        if (error.name === 'CastError') {
            return res.status(400).json({
                success: false,
                error: 'INVALID_ID',
                message: 'Invalid ID format',
                field: error.path,
                value: error.value,
                timestamp: new Date().toISOString()
            });
        }
        
        return res.status(500).json({
            success: false,
            error: 'SERVER_ERROR',
            message: 'Internal server error occurred',
            timestamp: new Date().toISOString()
        });
    }
};

// Helper function to validate and clean tags array
const validateTagsArray = (tags, fieldName = 'tags') => {
    if (!Array.isArray(tags)) {
        throw new Error(`${fieldName} must be an array`);
    }
    
    // Clean tags: remove empty strings, trim whitespace, remove duplicates
    const cleanedTags = [...new Set(
        tags
            .filter(tag => tag && typeof tag === 'string')
            .map(tag => tag.trim().toLowerCase())
            .filter(tag => tag.length > 0)
    )];
    
    if (cleanedTags.length > 50) {
        throw new Error(`${fieldName} cannot exceed 50 items`);
    }
    
    return cleanedTags;
};

/**
 * Get all tags for a template
 */
const getAllTags = async (req, res) => {
    const { templateId } = req.params;
    
    await handleAsyncOperation(async () => {
        const template = await Template.findById(templateId)
            .select('tags styleTags searchKeywords')
            .lean();
        
        if (!template) {
            throw new Error('Template not found');
        }
        
        return {
            templateId,
            tags: {
                general: template.tags || [],
                style: template.styleTags || [],
                keywords: template.searchKeywords || []
            }
        };
    }, res, 'All tags retrieved successfully');
};

/**
 * Update all tags for a template
 */
const updateAllTags = async (req, res) => {
    const { templateId } = req.params;
    const { tags, styleTags, searchKeywords } = req.body;
    
    await handleAsyncOperation(async () => {
        const updateData = {};
        const modifiedFields = [];
        
        if (tags !== undefined) {
            updateData.tags = validateTagsArray(tags, 'tags');
            modifiedFields.push('tags');
        }
        
        if (styleTags !== undefined) {
            updateData.styleTags = validateTagsArray(styleTags, 'styleTags');
            modifiedFields.push('styleTags');
        }
        
        if (searchKeywords !== undefined) {
            updateData.searchKeywords = validateTagsArray(searchKeywords, 'searchKeywords');
            modifiedFields.push('searchKeywords');
        }
        
        const template = await Template.findByIdAndUpdate(
            templateId,
            updateData,
            { new: true, runValidators: true }
        ).select('tags styleTags searchKeywords');
        
        return {
            templateId,
            modifiedFields,
            tags: {
                general: template.tags || [],
                style: template.styleTags || [],
                keywords: template.searchKeywords || []
            }
        };
    }, res, 'All tags updated successfully');
};

/**
 * Clear all tags for a template
 */
const clearAllTags = async (req, res) => {
    const { templateId } = req.params;
    
    await handleAsyncOperation(async () => {
        const template = await Template.findByIdAndUpdate(
            templateId,
            {
                $unset: {
                    tags: 1,
                    styleTags: 1,
                    searchKeywords: 1
                }
            },
            { new: true }
        ).select('tags styleTags searchKeywords');
        
        return {
            templateId,
            clearedFields: ['tags', 'styleTags', 'searchKeywords']
        };
    }, res, 'All tags cleared successfully');
};

/**
 * Get general tags for a template
 */
const getGeneralTags = async (req, res) => {
    const { templateId } = req.params;
    
    await handleAsyncOperation(async () => {
        const template = await Template.findById(templateId)
            .select('tags')
            .lean();
        
        if (!template) {
            throw new Error('Template not found');
        }
        
        return {
            templateId,
            tags: template.tags || []
        };
    }, res, 'General tags retrieved successfully');
};

/**
 * Update general tags for a template
 */
const updateGeneralTags = async (req, res) => {
    const { templateId } = req.params;
    const { tags } = req.body;
    
    await handleAsyncOperation(async () => {
        const cleanedTags = validateTagsArray(tags, 'tags');
        
        const template = await Template.findByIdAndUpdate(
            templateId,
            { tags: cleanedTags },
            { new: true, runValidators: true }
        ).select('tags');
        
        return {
            templateId,
            tags: template.tags || []
        };
    }, res, 'General tags updated successfully');
};

/**
 * Add general tags to a template
 */
const addGeneralTags = async (req, res) => {
    const { templateId } = req.params;
    const { tags } = req.body;
    
    await handleAsyncOperation(async () => {
        const newTags = validateTagsArray(tags, 'tags');
        
        const template = await Template.findByIdAndUpdate(
            templateId,
            { $addToSet: { tags: { $each: newTags } } },
            { new: true, runValidators: true }
        ).select('tags');
        
        return {
            templateId,
            addedTags: newTags,
            allTags: template.tags || []
        };
    }, res, 'General tags added successfully');
};

/**
 * Remove general tags from a template
 */
const removeGeneralTags = async (req, res) => {
    const { templateId } = req.params;
    const { tags } = req.body;
    
    await handleAsyncOperation(async () => {
        const tagsToRemove = validateTagsArray(tags, 'tags');
        
        const template = await Template.findByIdAndUpdate(
            templateId,
            { $pull: { tags: { $in: tagsToRemove } } },
            { new: true }
        ).select('tags');
        
        return {
            templateId,
            removedTags: tagsToRemove,
            remainingTags: template.tags || []
        };
    }, res, 'General tags removed successfully');
};

/**
 * Get style tags for a template
 */
const getStyleTags = async (req, res) => {
    const { templateId } = req.params;
    
    await handleAsyncOperation(async () => {
        const template = await Template.findById(templateId)
            .select('styleTags')
            .lean();
        
        if (!template) {
            throw new Error('Template not found');
        }
        
        return {
            templateId,
            styleTags: template.styleTags || []
        };
    }, res, 'Style tags retrieved successfully');
};

/**
 * Update style tags for a template
 */
const updateStyleTags = async (req, res) => {
    const { templateId } = req.params;
    const { styleTags } = req.body;
    
    await handleAsyncOperation(async () => {
        const cleanedTags = validateTagsArray(styleTags, 'styleTags');
        
        const template = await Template.findByIdAndUpdate(
            templateId,
            { styleTags: cleanedTags },
            { new: true, runValidators: true }
        ).select('styleTags');
        
        return {
            templateId,
            styleTags: template.styleTags || []
        };
    }, res, 'Style tags updated successfully');
};

/**
 * Add style tags to a template
 */
const addStyleTags = async (req, res) => {
    const { templateId } = req.params;
    const { styleTags } = req.body;
    
    await handleAsyncOperation(async () => {
        const newTags = validateTagsArray(styleTags, 'styleTags');
        
        const template = await Template.findByIdAndUpdate(
            templateId,
            { $addToSet: { styleTags: { $each: newTags } } },
            { new: true, runValidators: true }
        ).select('styleTags');
        
        return {
            templateId,
            addedTags: newTags,
            allStyleTags: template.styleTags || []
        };
    }, res, 'Style tags added successfully');
};

/**
 * Remove style tags from a template
 */
const removeStyleTags = async (req, res) => {
    const { templateId } = req.params;
    const { styleTags } = req.body;
    
    await handleAsyncOperation(async () => {
        const tagsToRemove = validateTagsArray(styleTags, 'styleTags');
        
        const template = await Template.findByIdAndUpdate(
            templateId,
            { $pull: { styleTags: { $in: tagsToRemove } } },
            { new: true }
        ).select('styleTags');
        
        return {
            templateId,
            removedTags: tagsToRemove,
            remainingStyleTags: template.styleTags || []
        };
    }, res, 'Style tags removed successfully');
};

/**
 * Get search keywords for a template
 */
const getSearchKeywords = async (req, res) => {
    const { templateId } = req.params;
    
    await handleAsyncOperation(async () => {
        const template = await Template.findById(templateId)
            .select('searchKeywords')
            .lean();
        
        if (!template) {
            throw new Error('Template not found');
        }
        
        return {
            templateId,
            searchKeywords: template.searchKeywords || []
        };
    }, res, 'Search keywords retrieved successfully');
};

/**
 * Update search keywords for a template
 */
const updateSearchKeywords = async (req, res) => {
    const { templateId } = req.params;
    const { searchKeywords } = req.body;
    
    await handleAsyncOperation(async () => {
        const cleanedKeywords = validateTagsArray(searchKeywords, 'searchKeywords');
        
        const template = await Template.findByIdAndUpdate(
            templateId,
            { searchKeywords: cleanedKeywords },
            { new: true, runValidators: true }
        ).select('searchKeywords');
        
        return {
            templateId,
            searchKeywords: template.searchKeywords || []
        };
    }, res, 'Search keywords updated successfully');
};

/**
 * Add search keywords to a template
 */
const addSearchKeywords = async (req, res) => {
    const { templateId } = req.params;
    const { searchKeywords } = req.body;
    
    await handleAsyncOperation(async () => {
        const newKeywords = validateTagsArray(searchKeywords, 'searchKeywords');
        
        const template = await Template.findByIdAndUpdate(
            templateId,
            { $addToSet: { searchKeywords: { $each: newKeywords } } },
            { new: true, runValidators: true }
        ).select('searchKeywords');
        
        return {
            templateId,
            addedKeywords: newKeywords,
            allKeywords: template.searchKeywords || []
        };
    }, res, 'Search keywords added successfully');
};

/**
 * Remove search keywords from a template
 */
const removeSearchKeywords = async (req, res) => {
    const { templateId } = req.params;
    const { searchKeywords } = req.body;
    
    await handleAsyncOperation(async () => {
        const keywordsToRemove = validateTagsArray(searchKeywords, 'searchKeywords');
        
        const template = await Template.findByIdAndUpdate(
            templateId,
            { $pull: { searchKeywords: { $in: keywordsToRemove } } },
            { new: true }
        ).select('searchKeywords');
        
        return {
            templateId,
            removedKeywords: keywordsToRemove,
            remainingKeywords: template.searchKeywords || []
        };
    }, res, 'Search keywords removed successfully');
};

/**
 * Get templates by general tag
 */
const getTemplatesByTag = async (req, res) => {
    const { tag } = req.params;
    const { page = 1, limit = 20, sort = '-usageCount' } = req.query;
    
    await handleAsyncOperation(async () => {
        const skip = (parseInt(page) - 1) * parseInt(limit);
        
        const [templates, total] = await Promise.all([
            Template.find({ tags: { $regex: new RegExp(tag, 'i') } })
                .select('title description tags category createdAt usageCount')
                .populate('creatorId', 'name email profileImage')
                .sort(sort)
                .skip(skip)
                .limit(parseInt(limit))
                .lean(),
            Template.countDocuments({ tags: { $regex: new RegExp(tag, 'i') } })
        ]);
        
        return {
            tag,
            templates,
            pagination: {
                page: parseInt(page),
                limit: parseInt(limit),
                total,
                pages: Math.ceil(total / parseInt(limit))
            }
        };
    }, res, 'Templates by tag retrieved successfully');
};

/**
 * Get templates by style tag
 */
const getTemplatesByStyleTag = async (req, res) => {
    const { styleTag } = req.params;
    const { page = 1, limit = 20, sort = '-usageCount' } = req.query;
    
    await handleAsyncOperation(async () => {
        const skip = (parseInt(page) - 1) * parseInt(limit);
        
        const [templates, total] = await Promise.all([
            Template.find({ styleTags: { $regex: new RegExp(styleTag, 'i') } })
                .select('title description styleTags category createdAt usageCount')
                .populate('creatorId', 'name email profileImage')
                .sort(sort)
                .skip(skip)
                .limit(parseInt(limit))
                .lean(),
            Template.countDocuments({ styleTags: { $regex: new RegExp(styleTag, 'i') } })
        ]);
        
        return {
            styleTag,
            templates,
            pagination: {
                page: parseInt(page),
                limit: parseInt(limit),
                total,
                pages: Math.ceil(total / parseInt(limit))
            }
        };
    }, res, 'Templates by style tag retrieved successfully');
};

/**
 * Get templates by search keyword
 */
const getTemplatesByKeyword = async (req, res) => {
    const { keyword } = req.params;
    const { page = 1, limit = 20, sort = '-usageCount' } = req.query;
    
    await handleAsyncOperation(async () => {
        const skip = (parseInt(page) - 1) * parseInt(limit);
        
        const [templates, total] = await Promise.all([
            Template.find({ searchKeywords: { $regex: new RegExp(keyword, 'i') } })
                .select('title description searchKeywords category createdAt usageCount')
                .populate('creatorId', 'name email profileImage')
                .sort(sort)
                .skip(skip)
                .limit(parseInt(limit))
                .lean(),
            Template.countDocuments({ searchKeywords: { $regex: new RegExp(keyword, 'i') } })
        ]);
        
        return {
            keyword,
            templates,
            pagination: {
                page: parseInt(page),
                limit: parseInt(limit),
                total,
                pages: Math.ceil(total / parseInt(limit))
            }
        };
    }, res, 'Templates by keyword retrieved successfully');
};

/**
 * Get popular tags statistics
 */
const getPopularTagsStats = async (req, res) => {
    await handleAsyncOperation(async () => {
        const stats = await Template.aggregate([
            {
                $match: {
                    tags: { $exists: true, $ne: [] }
                }
            },
            {
                $unwind: '$tags'
            },
            {
                $group: {
                    _id: '$tags',
                    count: { $sum: 1 },
                    templates: { $addToSet: '$_id' }
                }
            },
            {
                $addFields: {
                    templateCount: { $size: '$templates' }
                }
            },
            {
                $sort: { count: -1 }
            },
            {
                $limit: 100
            },
            {
                $project: {
                    tag: '$_id',
                    count: 1,
                    templateCount: 1,
                    _id: 0
                }
            }
        ]);
        
        return {
            totalTags: stats.length,
            popularTags: stats
        };
    }, res, 'Popular tags statistics retrieved successfully');
};

/**
 * Get style tags statistics
 */
const getStyleTagsStats = async (req, res) => {
    await handleAsyncOperation(async () => {
        const stats = await Template.aggregate([
            {
                $match: {
                    styleTags: { $exists: true, $ne: [] }
                }
            },
            {
                $unwind: '$styleTags'
            },
            {
                $group: {
                    _id: '$styleTags',
                    count: { $sum: 1 },
                    templates: { $addToSet: '$_id' }
                }
            },
            {
                $addFields: {
                    templateCount: { $size: '$templates' }
                }
            },
            {
                $sort: { count: -1 }
            },
            {
                $limit: 100
            },
            {
                $project: {
                    styleTag: '$_id',
                    count: 1,
                    templateCount: 1,
                    _id: 0
                }
            }
        ]);
        
        return {
            totalStyleTags: stats.length,
            popularStyleTags: stats
        };
    }, res, 'Style tags statistics retrieved successfully');
};

/**
 * Get search keywords statistics
 */
const getKeywordsStats = async (req, res) => {
    await handleAsyncOperation(async () => {
        const stats = await Template.aggregate([
            {
                $match: {
                    searchKeywords: { $exists: true, $ne: [] }
                }
            },
            {
                $unwind: '$searchKeywords'
            },
            {
                $group: {
                    _id: '$searchKeywords',
                    count: { $sum: 1 },
                    templates: { $addToSet: '$_id' }
                }
            },
            {
                $addFields: {
                    templateCount: { $size: '$templates' }
                }
            },
            {
                $sort: { count: -1 }
            },
            {
                $limit: 100
            },
            {
                $project: {
                    keyword: '$_id',
                    count: 1,
                    templateCount: 1,
                    _id: 0
                }
            }
        ]);
        
        return {
            totalKeywords: stats.length,
            popularKeywords: stats
        };
    }, res, 'Keywords statistics retrieved successfully');
};

/**
 * Batch add tags to multiple templates
 */
const batchAddTags = async (req, res) => {
    const { templateIds, tags, styleTags, searchKeywords } = req.body;
    
    await handleAsyncOperation(async () => {
        if (!Array.isArray(templateIds) || templateIds.length === 0) {
            throw new Error('templateIds must be a non-empty array');
        }
        
        const operations = [];
        
        if (tags && Array.isArray(tags)) {
            const cleanedTags = validateTagsArray(tags, 'tags');
            operations.push({
                updateMany: {
                    filter: { _id: { $in: templateIds } },
                    update: { $addToSet: { tags: { $each: cleanedTags } } }
                }
            });
        }
        
        if (styleTags && Array.isArray(styleTags)) {
            const cleanedStyleTags = validateTagsArray(styleTags, 'styleTags');
            operations.push({
                updateMany: {
                    filter: { _id: { $in: templateIds } },
                    update: { $addToSet: { styleTags: { $each: cleanedStyleTags } } }
                }
            });
        }
        
        if (searchKeywords && Array.isArray(searchKeywords)) {
            const cleanedKeywords = validateTagsArray(searchKeywords, 'searchKeywords');
            operations.push({
                updateMany: {
                    filter: { _id: { $in: templateIds } },
                    update: { $addToSet: { searchKeywords: { $each: cleanedKeywords } } }
                }
            });
        }
        
        if (operations.length === 0) {
            throw new Error('At least one tag type must be provided');
        }
        
        const result = await Template.bulkWrite(operations);
        
        return {
            templateIds,
            addedTags: {
                general: tags || [],
                style: styleTags || [],
                keywords: searchKeywords || []
            },
            modifiedCount: result.modifiedCount,
            matchedCount: result.matchedCount
        };
    }, res, 'Tags added to templates successfully');
};

/**
 * Batch remove tags from multiple templates
 */
const batchRemoveTags = async (req, res) => {
    const { templateIds, tags, styleTags, searchKeywords } = req.body;
    
    await handleAsyncOperation(async () => {
        if (!Array.isArray(templateIds) || templateIds.length === 0) {
            throw new Error('templateIds must be a non-empty array');
        }
        
        const operations = [];
        
        if (tags && Array.isArray(tags)) {
            const cleanedTags = validateTagsArray(tags, 'tags');
            operations.push({
                updateMany: {
                    filter: { _id: { $in: templateIds } },
                    update: { $pull: { tags: { $in: cleanedTags } } }
                }
            });
        }
        
        if (styleTags && Array.isArray(styleTags)) {
            const cleanedStyleTags = validateTagsArray(styleTags, 'styleTags');
            operations.push({
                updateMany: {
                    filter: { _id: { $in: templateIds } },
                    update: { $pull: { styleTags: { $in: cleanedStyleTags } } }
                }
            });
        }
        
        if (searchKeywords && Array.isArray(searchKeywords)) {
            const cleanedKeywords = validateTagsArray(searchKeywords, 'searchKeywords');
            operations.push({
                updateMany: {
                    filter: { _id: { $in: templateIds } },
                    update: { $pull: { searchKeywords: { $in: cleanedKeywords } } }
                }
            });
        }
        
        if (operations.length === 0) {
            throw new Error('At least one tag type must be provided');
        }
        
        const result = await Template.bulkWrite(operations);
        
        return {
            templateIds,
            removedTags: {
                general: tags || [],
                style: styleTags || [],
                keywords: searchKeywords || []
            },
            modifiedCount: result.modifiedCount,
            matchedCount: result.matchedCount
        };
    }, res, 'Tags removed from templates successfully');
};

module.exports = {
    getAllTags,
    updateAllTags,
    clearAllTags,
    getGeneralTags,
    updateGeneralTags,
    addGeneralTags,
    removeGeneralTags,
    getStyleTags,
    updateStyleTags,
    addStyleTags,
    removeStyleTags,
    getSearchKeywords,
    updateSearchKeywords,
    addSearchKeywords,
    removeSearchKeywords,
    getTemplatesByTag,
    getTemplatesByStyleTag,
    getTemplatesByKeyword,
    getPopularTagsStats,
    getStyleTagsStats,
    getKeywordsStats,
    batchAddTags,
    batchRemoveTags
}; 