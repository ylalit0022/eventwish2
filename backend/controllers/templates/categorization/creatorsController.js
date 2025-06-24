const Template = require('../../../models/Template');
const User = require('../../../models/User');
const mongoose = require('mongoose');
const logger = require('../../../utils/logger');

/**
 * Creators Controller
 * Handles creator-related operations for templates:
 * - creatorId (ObjectId, ref: 'User')
 * - creatorUid (String, Firebase UID)
 * - category (String)
 * - festivalTag (String)
 */

// Helper function to handle async operations
const handleAsyncOperation = async (operation, res, successMessage) => {
    try {
        const result = await operation();
        logger.info(`Creator operation successful: ${successMessage}`);
        return res.status(200).json({
            success: true,
            message: successMessage,
            data: result,
            timestamp: new Date().toISOString()
        });
    } catch (error) {
        logger.error(`Creator operation failed: ${error.message}`, { error: error.stack });
        
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

// Helper function to validate ObjectId
const isValidObjectId = (id) => {
    return mongoose.Types.ObjectId.isValid(id);
};

/**
 * Get creator data for a template
 */
const getCreatorData = async (req, res) => {
    const { templateId } = req.params;
    
    await handleAsyncOperation(async () => {
        const template = await Template.findById(templateId)
            .select('creatorId creatorUid category festivalTag')
            .populate('creatorId', 'name email profileImage')
            .lean();
        
        if (!template) {
            throw new Error('Template not found');
        }
        
        return {
            templateId,
            creatorData: {
                creatorId: template.creatorId,
                creatorUid: template.creatorUid,
                category: template.category,
                festivalTag: template.festivalTag
            }
        };
    }, res, 'Creator data retrieved successfully');
};

/**
 * Update creator data for a template
 */
const updateCreatorData = async (req, res) => {
    const { templateId } = req.params;
    const { creatorId, creatorUid, category, festivalTag } = req.body;
    
    await handleAsyncOperation(async () => {
        const updateData = {};
        const modifiedFields = [];
        
        if (creatorId !== undefined) {
            if (creatorId && !isValidObjectId(creatorId)) {
                throw new Error('Invalid creatorId format');
            }
            updateData.creatorId = creatorId || null;
            modifiedFields.push('creatorId');
        }
        
        if (creatorUid !== undefined) {
            updateData.creatorUid = creatorUid || null;
            modifiedFields.push('creatorUid');
        }
        
        if (category !== undefined) {
            updateData.category = category || null;
            modifiedFields.push('category');
        }
        
        if (festivalTag !== undefined) {
            updateData.festivalTag = festivalTag || null;
            modifiedFields.push('festivalTag');
        }
        
        const template = await Template.findByIdAndUpdate(
            templateId,
            updateData,
            { new: true, runValidators: true }
        ).select('creatorId creatorUid category festivalTag')
        .populate('creatorId', 'name email profileImage');
        
        return {
            templateId,
            modifiedFields,
            creatorData: {
                creatorId: template.creatorId,
                creatorUid: template.creatorUid,
                category: template.category,
                festivalTag: template.festivalTag
            }
        };
    }, res, 'Creator data updated successfully');
};

/**
 * Clear creator data for a template
 */
const clearCreatorData = async (req, res) => {
    const { templateId } = req.params;
    
    await handleAsyncOperation(async () => {
        const template = await Template.findByIdAndUpdate(
            templateId,
            {
                $unset: {
                    creatorId: 1,
                    creatorUid: 1,
                    category: 1,
                    festivalTag: 1
                }
            },
            { new: true }
        ).select('creatorId creatorUid category festivalTag');
        
        return {
            templateId,
            clearedFields: ['creatorId', 'creatorUid', 'category', 'festivalTag']
        };
    }, res, 'Creator data cleared successfully');
};

/**
 * Update creatorId field
 */
const updateCreatorId = async (req, res) => {
    const { templateId } = req.params;
    const { creatorId } = req.body;
    
    await handleAsyncOperation(async () => {
        if (creatorId && !isValidObjectId(creatorId)) {
            throw new Error('Invalid creatorId format');
        }
        
        const template = await Template.findByIdAndUpdate(
            templateId,
            { creatorId: creatorId || null },
            { new: true, runValidators: true }
        ).select('creatorId')
        .populate('creatorId', 'name email profileImage');
        
        return {
            templateId,
            creatorId: template.creatorId
        };
    }, res, 'CreatorId updated successfully');
};

/**
 * Update creatorUid field
 */
const updateCreatorUid = async (req, res) => {
    const { templateId } = req.params;
    const { creatorUid } = req.body;
    
    await handleAsyncOperation(async () => {
        const template = await Template.findByIdAndUpdate(
            templateId,
            { creatorUid: creatorUid || null },
            { new: true, runValidators: true }
        ).select('creatorUid');
        
        return {
            templateId,
            creatorUid: template.creatorUid
        };
    }, res, 'CreatorUid updated successfully');
};

/**
 * Update category field
 */
const updateCategory = async (req, res) => {
    const { templateId } = req.params;
    const { category } = req.body;
    
    await handleAsyncOperation(async () => {
        const template = await Template.findByIdAndUpdate(
            templateId,
            { category: category || null },
            { new: true, runValidators: true }
        ).select('category');
        
        return {
            templateId,
            category: template.category
        };
    }, res, 'Category updated successfully');
};

/**
 * Update festivalTag field
 */
const updateFestivalTag = async (req, res) => {
    const { templateId } = req.params;
    const { festivalTag } = req.body;
    
    await handleAsyncOperation(async () => {
        const template = await Template.findByIdAndUpdate(
            templateId,
            { festivalTag: festivalTag || null },
            { new: true, runValidators: true }
        ).select('festivalTag');
        
        return {
            templateId,
            festivalTag: template.festivalTag
        };
    }, res, 'Festival tag updated successfully');
};

/**
 * Get templates by creator (ObjectId)
 */
const getTemplatesByCreator = async (req, res) => {
    const { creatorId } = req.params;
    const { page = 1, limit = 20, sort = '-createdAt' } = req.query;
    
    await handleAsyncOperation(async () => {
        if (!isValidObjectId(creatorId)) {
            throw new Error('Invalid creatorId format');
        }
        
        const skip = (parseInt(page) - 1) * parseInt(limit);
        
        const [templates, total] = await Promise.all([
            Template.find({ creatorId })
                .select('title description category festivalTag createdAt usageCount')
                .populate('creatorId', 'name email profileImage')
                .sort(sort)
                .skip(skip)
                .limit(parseInt(limit))
                .lean(),
            Template.countDocuments({ creatorId })
        ]);
        
        return {
            creatorId,
            templates,
            pagination: {
                page: parseInt(page),
                limit: parseInt(limit),
                total,
                pages: Math.ceil(total / parseInt(limit))
            }
        };
    }, res, 'Templates by creator retrieved successfully');
};

/**
 * Get templates by creator (Firebase UID)
 */
const getTemplatesByCreatorUid = async (req, res) => {
    const { uid } = req.params;
    const { page = 1, limit = 20, sort = '-createdAt' } = req.query;
    
    await handleAsyncOperation(async () => {
        const skip = (parseInt(page) - 1) * parseInt(limit);
        
        const [templates, total] = await Promise.all([
            Template.find({ creatorUid: uid })
                .select('title description category festivalTag createdAt usageCount')
                .sort(sort)
                .skip(skip)
                .limit(parseInt(limit))
                .lean(),
            Template.countDocuments({ creatorUid: uid })
        ]);
        
        return {
            creatorUid: uid,
            templates,
            pagination: {
                page: parseInt(page),
                limit: parseInt(limit),
                total,
                pages: Math.ceil(total / parseInt(limit))
            }
        };
    }, res, 'Templates by creator UID retrieved successfully');
};

/**
 * Get templates by category
 */
const getTemplatesByCategory = async (req, res) => {
    const { category } = req.params;
    const { page = 1, limit = 20, sort = '-usageCount' } = req.query;
    
    await handleAsyncOperation(async () => {
        const skip = (parseInt(page) - 1) * parseInt(limit);
        
        const [templates, total] = await Promise.all([
            Template.find({ category: new RegExp(category, 'i') })
                .select('title description category festivalTag createdAt usageCount')
                .populate('creatorId', 'name email profileImage')
                .sort(sort)
                .skip(skip)
                .limit(parseInt(limit))
                .lean(),
            Template.countDocuments({ category: new RegExp(category, 'i') })
        ]);
        
        return {
            category,
            templates,
            pagination: {
                page: parseInt(page),
                limit: parseInt(limit),
                total,
                pages: Math.ceil(total / parseInt(limit))
            }
        };
    }, res, 'Templates by category retrieved successfully');
};

/**
 * Get templates by festival tag
 */
const getTemplatesByFestival = async (req, res) => {
    const { festivalTag } = req.params;
    const { page = 1, limit = 20, sort = '-usageCount' } = req.query;
    
    await handleAsyncOperation(async () => {
        const skip = (parseInt(page) - 1) * parseInt(limit);
        
        const [templates, total] = await Promise.all([
            Template.find({ festivalTag: new RegExp(festivalTag, 'i') })
                .select('title description category festivalTag createdAt usageCount')
                .populate('creatorId', 'name email profileImage')
                .sort(sort)
                .skip(skip)
                .limit(parseInt(limit))
                .lean(),
            Template.countDocuments({ festivalTag: new RegExp(festivalTag, 'i') })
        ]);
        
        return {
            festivalTag,
            templates,
            pagination: {
                page: parseInt(page),
                limit: parseInt(limit),
                total,
                pages: Math.ceil(total / parseInt(limit))
            }
        };
    }, res, 'Templates by festival tag retrieved successfully');
};

/**
 * Get category statistics
 */
const getCategoryStats = async (req, res) => {
    await handleAsyncOperation(async () => {
        const stats = await Template.aggregate([
            {
                $match: {
                    category: { $exists: true, $ne: null, $ne: '' }
                }
            },
            {
                $group: {
                    _id: '$category',
                    count: { $sum: 1 },
                    totalUsage: { $sum: '$usageCount' },
                    avgUsage: { $avg: '$usageCount' },
                    avgRating: { $avg: '$rating' }
                }
            },
            {
                $sort: { count: -1 }
            },
            {
                $limit: 50
            }
        ]);
        
        const totalCategories = stats.length;
        const totalTemplates = stats.reduce((sum, stat) => sum + stat.count, 0);
        
        return {
            totalCategories,
            totalTemplates,
            categories: stats.map(stat => ({
                category: stat._id,
                templateCount: stat.count,
                totalUsage: stat.totalUsage,
                averageUsage: Math.round(stat.avgUsage * 100) / 100,
                averageRating: Math.round(stat.avgRating * 100) / 100
            }))
        };
    }, res, 'Category statistics retrieved successfully');
};

/**
 * Get festival tag statistics
 */
const getFestivalStats = async (req, res) => {
    await handleAsyncOperation(async () => {
        const stats = await Template.aggregate([
            {
                $match: {
                    festivalTag: { $exists: true, $ne: null, $ne: '' }
                }
            },
            {
                $group: {
                    _id: '$festivalTag',
                    count: { $sum: 1 },
                    totalUsage: { $sum: '$usageCount' },
                    avgUsage: { $avg: '$usageCount' },
                    avgRating: { $avg: '$rating' }
                }
            },
            {
                $sort: { count: -1 }
            },
            {
                $limit: 50
            }
        ]);
        
        const totalFestivals = stats.length;
        const totalTemplates = stats.reduce((sum, stat) => sum + stat.count, 0);
        
        return {
            totalFestivals,
            totalTemplates,
            festivals: stats.map(stat => ({
                festivalTag: stat._id,
                templateCount: stat.count,
                totalUsage: stat.totalUsage,
                averageUsage: Math.round(stat.avgUsage * 100) / 100,
                averageRating: Math.round(stat.avgRating * 100) / 100
            }))
        };
    }, res, 'Festival statistics retrieved successfully');
};

/**
 * Get creator statistics
 */
const getCreatorStats = async (req, res) => {
    await handleAsyncOperation(async () => {
        const stats = await Template.aggregate([
            {
                $match: {
                    $or: [
                        { creatorId: { $exists: true, $ne: null } },
                        { creatorUid: { $exists: true, $ne: null, $ne: '' } }
                    ]
                }
            },
            {
                $group: {
                    _id: {
                        creatorId: '$creatorId',
                        creatorUid: '$creatorUid'
                    },
                    count: { $sum: 1 },
                    totalUsage: { $sum: '$usageCount' },
                    avgUsage: { $avg: '$usageCount' },
                    avgRating: { $avg: '$rating' }
                }
            },
            {
                $sort: { count: -1 }
            },
            {
                $limit: 50
            }
        ]);
        
        // Populate creator information
        const populatedStats = await Promise.all(
            stats.map(async (stat) => {
                let creatorInfo = null;
                if (stat._id.creatorId) {
                    creatorInfo = await User.findById(stat._id.creatorId)
                        .select('name email profileImage')
                        .lean();
                }
                
                return {
                    creatorId: stat._id.creatorId,
                    creatorUid: stat._id.creatorUid,
                    creatorInfo,
                    templateCount: stat.count,
                    totalUsage: stat.totalUsage,
                    averageUsage: Math.round(stat.avgUsage * 100) / 100,
                    averageRating: Math.round(stat.avgRating * 100) / 100
                };
            })
        );
        
        const totalCreators = stats.length;
        const totalTemplates = stats.reduce((sum, stat) => sum + stat.count, 0);
        
        return {
            totalCreators,
            totalTemplates,
            creators: populatedStats
        };
    }, res, 'Creator statistics retrieved successfully');
};

/**
 * Batch assign creator for multiple templates
 */
const batchAssignCreator = async (req, res) => {
    const { templateIds, creatorId, creatorUid } = req.body;
    
    await handleAsyncOperation(async () => {
        if (!Array.isArray(templateIds) || templateIds.length === 0) {
            throw new Error('templateIds must be a non-empty array');
        }
        
        if (!creatorId && !creatorUid) {
            throw new Error('Either creatorId or creatorUid must be provided');
        }
        
        if (creatorId && !isValidObjectId(creatorId)) {
            throw new Error('Invalid creatorId format');
        }
        
        const updateData = {};
        if (creatorId) updateData.creatorId = creatorId;
        if (creatorUid) updateData.creatorUid = creatorUid;
        
        const result = await Template.updateMany(
            { _id: { $in: templateIds } },
            updateData
        );
        
        return {
            assignedCreator: updateData,
            matchedCount: result.matchedCount,
            modifiedCount: result.modifiedCount,
            templateIds
        };
    }, res, 'Creator assigned to templates successfully');
};

module.exports = {
    getCreatorData,
    updateCreatorData,
    clearCreatorData,
    updateCreatorId,
    updateCreatorUid,
    updateCategory,
    updateFestivalTag,
    getTemplatesByCreator,
    getTemplatesByCreatorUid,
    getTemplatesByCategory,
    getTemplatesByFestival,
    getCategoryStats,
    getFestivalStats,
    getCreatorStats,
    batchAssignCreator
}; 