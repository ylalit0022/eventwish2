const Template = require('../../../models/Template');
const { handleAsyncOperation, formatSuccessResponse, formatErrorResponse } = require('../../../utils/templateHelpers');
const logger = require('../../../utils/logger');

/**
 * AI Metadata Controller
 * Handles CRUD operations for AI metadata fields
 */

/**
 * Get complete AI metadata for a template
 */
const getMetadata = async (req, res) => {
    await handleAsyncOperation(res, async () => {
        const { templateId } = req.params;
        
        const template = await Template.findById(templateId).select(
            'generationMetadata aiPrompt aiModel aiStyle'
        );
        
        if (!template) {
            return res.status(404).json(formatErrorResponse(
                'TEMPLATE_NOT_FOUND',
                'Template not found',
                'templateId',
                templateId
            ));
        }
        
        logger.info(`Retrieved AI metadata for template: ${templateId}`);
        
        return res.json(formatSuccessResponse({
            templateId: template._id,
            generationMetadata: template.generationMetadata,
            aiPrompt: template.aiPrompt,
            aiModel: template.aiModel,
            aiStyle: template.aiStyle
        }));
    });
};

/**
 * Update AI metadata for a template
 */
const updateMetadata = async (req, res) => {
    await handleAsyncOperation(res, async () => {
        const { templateId } = req.params;
        const { generationMetadata, aiPrompt, aiModel, aiStyle } = req.body;
        
        const updateData = {};
        const modifiedFields = [];
        
        if (generationMetadata !== undefined) {
            updateData.generationMetadata = generationMetadata;
            modifiedFields.push('generationMetadata');
        }
        if (aiPrompt !== undefined) {
            updateData.aiPrompt = aiPrompt;
            modifiedFields.push('aiPrompt');
        }
        if (aiModel !== undefined) {
            updateData.aiModel = aiModel;
            modifiedFields.push('aiModel');
        }
        if (aiStyle !== undefined) {
            updateData.aiStyle = aiStyle;
            modifiedFields.push('aiStyle');
        }
        
        if (Object.keys(updateData).length === 0) {
            return res.status(400).json(formatErrorResponse(
                'NO_FIELDS_TO_UPDATE',
                'No valid fields provided for update'
            ));
        }
        
        const template = await Template.findByIdAndUpdate(
            templateId,
            { $set: updateData },
            { new: true, runValidators: true }
        ).select('generationMetadata aiPrompt aiModel aiStyle');
        
        if (!template) {
            return res.status(404).json(formatErrorResponse(
                'TEMPLATE_NOT_FOUND',
                'Template not found',
                'templateId',
                templateId
            ));
        }
        
        logger.info(`Updated AI metadata for template: ${templateId}`, { modifiedFields });
        
        return res.json(formatSuccessResponse({
            templateId: template._id,
            generationMetadata: template.generationMetadata,
            aiPrompt: template.aiPrompt,
            aiModel: template.aiModel,
            aiStyle: template.aiStyle,
            modifiedFields
        }));
    });
};

/**
 * Clear AI metadata for a template
 */
const clearMetadata = async (req, res) => {
    await handleAsyncOperation(res, async () => {
        const { templateId } = req.params;
        
        const template = await Template.findByIdAndUpdate(
            templateId,
            { 
                $set: {
                    generationMetadata: {},
                    aiPrompt: '',
                    aiModel: '',
                    aiStyle: ''
                }
            },
            { new: true, runValidators: true }
        ).select('generationMetadata aiPrompt aiModel aiStyle');
        
        if (!template) {
            return res.status(404).json(formatErrorResponse(
                'TEMPLATE_NOT_FOUND',
                'Template not found',
                'templateId',
                templateId
            ));
        }
        
        logger.info(`Cleared AI metadata for template: ${templateId}`);
        
        return res.json(formatSuccessResponse({
            templateId: template._id,
            message: 'AI metadata cleared successfully',
            clearedFields: ['generationMetadata', 'aiPrompt', 'aiModel', 'aiStyle']
        }));
    });
};

/**
 * Update generation resolution
 */
const updateResolution = async (req, res) => {
    await handleAsyncOperation(res, async () => {
        const { templateId } = req.params;
        const { resolution } = req.body;
        
        if (!resolution || typeof resolution !== 'string') {
            return res.status(400).json(formatErrorResponse(
                'INVALID_RESOLUTION',
                'Resolution must be a string (e.g., "1024x1024")',
                'resolution',
                resolution
            ));
        }
        
        const template = await Template.findByIdAndUpdate(
            templateId,
            { $set: { 'generationMetadata.resolution': resolution } },
            { new: true, runValidators: true }
        ).select('generationMetadata');
        
        if (!template) {
            return res.status(404).json(formatErrorResponse(
                'TEMPLATE_NOT_FOUND',
                'Template not found',
                'templateId',
                templateId
            ));
        }
        
        logger.info(`Updated resolution for template: ${templateId} to ${resolution}`);
        
        return res.json(formatSuccessResponse({
            templateId: template._id,
            generationMetadata: template.generationMetadata,
            modifiedFields: ['generationMetadata.resolution']
        }));
    });
};

/**
 * Update generation seed
 */
const updateSeed = async (req, res) => {
    await handleAsyncOperation(res, async () => {
        const { templateId } = req.params;
        const { seed } = req.body;
        
        if (seed !== undefined && (typeof seed !== 'number' || seed < 0)) {
            return res.status(400).json(formatErrorResponse(
                'INVALID_SEED',
                'Seed must be a non-negative number',
                'seed',
                seed
            ));
        }
        
        const template = await Template.findByIdAndUpdate(
            templateId,
            { $set: { 'generationMetadata.seed': seed } },
            { new: true, runValidators: true }
        ).select('generationMetadata');
        
        if (!template) {
            return res.status(404).json(formatErrorResponse(
                'TEMPLATE_NOT_FOUND',
                'Template not found',
                'templateId',
                templateId
            ));
        }
        
        logger.info(`Updated seed for template: ${templateId} to ${seed}`);
        
        return res.json(formatSuccessResponse({
            templateId: template._id,
            generationMetadata: template.generationMetadata,
            modifiedFields: ['generationMetadata.seed']
        }));
    });
};

/**
 * Update guidance scale
 */
const updateGuidanceScale = async (req, res) => {
    await handleAsyncOperation(res, async () => {
        const { templateId } = req.params;
        const { guidanceScale } = req.body;
        
        if (guidanceScale !== undefined && (typeof guidanceScale !== 'number' || guidanceScale < 0)) {
            return res.status(400).json(formatErrorResponse(
                'INVALID_GUIDANCE_SCALE',
                'Guidance scale must be a non-negative number',
                'guidanceScale',
                guidanceScale
            ));
        }
        
        const template = await Template.findByIdAndUpdate(
            templateId,
            { $set: { 'generationMetadata.guidanceScale': guidanceScale } },
            { new: true, runValidators: true }
        ).select('generationMetadata');
        
        if (!template) {
            return res.status(404).json(formatErrorResponse(
                'TEMPLATE_NOT_FOUND',
                'Template not found',
                'templateId',
                templateId
            ));
        }
        
        logger.info(`Updated guidance scale for template: ${templateId} to ${guidanceScale}`);
        
        return res.json(formatSuccessResponse({
            templateId: template._id,
            generationMetadata: template.generationMetadata,
            modifiedFields: ['generationMetadata.guidanceScale']
        }));
    });
};

/**
 * Get templates by resolution
 */
const getTemplatesByResolution = async (req, res) => {
    await handleAsyncOperation(res, async () => {
        const { resolution } = req.params;
        const { page = 1, limit = 20 } = req.query;
        
        const skip = (page - 1) * limit;
        
        const templates = await Template.find({ 'generationMetadata.resolution': resolution })
            .select('title category generationMetadata aiModel aiStyle')
            .skip(skip)
            .limit(parseInt(limit))
            .sort({ createdAt: -1 });
        
        const total = await Template.countDocuments({ 'generationMetadata.resolution': resolution });
        
        logger.info(`Retrieved ${templates.length} templates with resolution: ${resolution}`);
        
        return res.json(formatSuccessResponse({
            templates,
            pagination: {
                page: parseInt(page),
                limit: parseInt(limit),
                total,
                pages: Math.ceil(total / limit)
            },
            filter: { resolution }
        }));
    });
};

/**
 * Get templates by seed value
 */
const getTemplatesBySeed = async (req, res) => {
    await handleAsyncOperation(res, async () => {
        const { seed } = req.params;
        const { page = 1, limit = 20 } = req.query;
        
        const seedValue = parseInt(seed);
        if (isNaN(seedValue)) {
            return res.status(400).json(formatErrorResponse(
                'INVALID_SEED',
                'Seed must be a valid number',
                'seed',
                seed
            ));
        }
        
        const skip = (page - 1) * limit;
        
        const templates = await Template.find({ 'generationMetadata.seed': seedValue })
            .select('title category generationMetadata aiModel aiStyle')
            .skip(skip)
            .limit(parseInt(limit))
            .sort({ createdAt: -1 });
        
        const total = await Template.countDocuments({ 'generationMetadata.seed': seedValue });
        
        logger.info(`Retrieved ${templates.length} templates with seed: ${seedValue}`);
        
        return res.json(formatSuccessResponse({
            templates,
            pagination: {
                page: parseInt(page),
                limit: parseInt(limit),
                total,
                pages: Math.ceil(total / limit)
            },
            filter: { seed: seedValue }
        }));
    });
};

/**
 * Get templates by guidance scale range
 */
const getTemplatesByGuidanceScale = async (req, res) => {
    await handleAsyncOperation(res, async () => {
        const { scale } = req.params;
        const { page = 1, limit = 20 } = req.query;
        
        const scaleValue = parseFloat(scale);
        if (isNaN(scaleValue)) {
            return res.status(400).json(formatErrorResponse(
                'INVALID_GUIDANCE_SCALE',
                'Guidance scale must be a valid number',
                'scale',
                scale
            ));
        }
        
        const skip = (page - 1) * limit;
        
        // Find templates within ±0.5 range of the guidance scale
        const templates = await Template.find({ 
            'generationMetadata.guidanceScale': { 
                $gte: scaleValue - 0.5, 
                $lte: scaleValue + 0.5 
            }
        })
            .select('title category generationMetadata aiModel aiStyle')
            .skip(skip)
            .limit(parseInt(limit))
            .sort({ createdAt: -1 });
        
        const total = await Template.countDocuments({ 
            'generationMetadata.guidanceScale': { 
                $gte: scaleValue - 0.5, 
                $lte: scaleValue + 0.5 
            }
        });
        
        logger.info(`Retrieved ${templates.length} templates with guidance scale around: ${scaleValue}`);
        
        return res.json(formatSuccessResponse({
            templates,
            pagination: {
                page: parseInt(page),
                limit: parseInt(limit),
                total,
                pages: Math.ceil(total / limit)
            },
            filter: { guidanceScaleRange: `${scaleValue - 0.5} - ${scaleValue + 0.5}` }
        }));
    });
};

/**
 * Search templates by metadata criteria
 */
const searchByMetadata = async (req, res) => {
    await handleAsyncOperation(res, async () => {
        const { resolution, seedRange, guidanceScaleRange, aiModel, aiStyle, page = 1, limit = 20 } = req.body;
        
        const query = {};
        const skip = (page - 1) * limit;
        
        if (resolution) {
            query['generationMetadata.resolution'] = resolution;
        }
        
        if (seedRange && seedRange.min !== undefined && seedRange.max !== undefined) {
            query['generationMetadata.seed'] = { $gte: seedRange.min, $lte: seedRange.max };
        }
        
        if (guidanceScaleRange && guidanceScaleRange.min !== undefined && guidanceScaleRange.max !== undefined) {
            query['generationMetadata.guidanceScale'] = { $gte: guidanceScaleRange.min, $lte: guidanceScaleRange.max };
        }
        
        if (aiModel) {
            query.aiModel = aiModel;
        }
        
        if (aiStyle) {
            query.aiStyle = aiStyle;
        }
        
        const templates = await Template.find(query)
            .select('title category generationMetadata aiModel aiStyle aiPrompt')
            .skip(skip)
            .limit(parseInt(limit))
            .sort({ createdAt: -1 });
        
        const total = await Template.countDocuments(query);
        
        logger.info(`Searched templates with metadata criteria, found: ${templates.length}`);
        
        return res.json(formatSuccessResponse({
            templates,
            pagination: {
                page: parseInt(page),
                limit: parseInt(limit),
                total,
                pages: Math.ceil(total / limit)
            },
            searchCriteria: { resolution, seedRange, guidanceScaleRange, aiModel, aiStyle }
        }));
    });
};

/**
 * Get resolution usage statistics
 */
const getResolutionStats = async (req, res) => {
    await handleAsyncOperation(res, async () => {
        const resolutionStats = await Template.aggregate([
            { $match: { 'generationMetadata.resolution': { $exists: true, $ne: '' } } },
            { $group: { _id: '$generationMetadata.resolution', count: { $sum: 1 } } },
            { $sort: { count: -1 } }
        ]);
        
        logger.info('Retrieved resolution usage statistics');
        
        return res.json(formatSuccessResponse({
            resolutionStats,
            totalTemplatesWithResolution: resolutionStats.reduce((sum, stat) => sum + stat.count, 0),
            generatedAt: new Date().toISOString()
        }));
    });
};

/**
 * Get guidance scale distribution statistics
 */
const getGuidanceScaleStats = async (req, res) => {
    await handleAsyncOperation(res, async () => {
        const guidanceScaleStats = await Template.aggregate([
            { $match: { 'generationMetadata.guidanceScale': { $exists: true, $ne: null } } },
            { 
                $group: { 
                    _id: { $round: ['$generationMetadata.guidanceScale', 1] }, 
                    count: { $sum: 1 },
                    avgSeed: { $avg: '$generationMetadata.seed' }
                } 
            },
            { $sort: { _id: 1 } }
        ]);
        
        logger.info('Retrieved guidance scale distribution statistics');
        
        return res.json(formatSuccessResponse({
            guidanceScaleStats,
            totalTemplatesWithGuidanceScale: guidanceScaleStats.reduce((sum, stat) => sum + stat.count, 0),
            generatedAt: new Date().toISOString()
        }));
    });
};

/**
 * Batch update metadata for multiple templates
 */
const batchUpdateMetadata = async (req, res) => {
    await handleAsyncOperation(res, async () => {
        const { templateIds, metadataUpdate } = req.body;
        
        if (!templateIds || !Array.isArray(templateIds) || templateIds.length === 0) {
            return res.status(400).json(formatErrorResponse(
                'INVALID_TEMPLATE_IDS',
                'Template IDs array is required'
            ));
        }
        
        if (!metadataUpdate || Object.keys(metadataUpdate).length === 0) {
            return res.status(400).json(formatErrorResponse(
                'NO_METADATA_UPDATE',
                'Metadata update is required'
            ));
        }
        
        const updateData = {};
        
        // Build update data for nested generationMetadata fields
        if (metadataUpdate.resolution !== undefined) {
            updateData['generationMetadata.resolution'] = metadataUpdate.resolution;
        }
        if (metadataUpdate.seed !== undefined) {
            updateData['generationMetadata.seed'] = metadataUpdate.seed;
        }
        if (metadataUpdate.guidanceScale !== undefined) {
            updateData['generationMetadata.guidanceScale'] = metadataUpdate.guidanceScale;
        }
        if (metadataUpdate.timestamp !== undefined) {
            updateData['generationMetadata.timestamp'] = new Date(metadataUpdate.timestamp);
        }
        
        const result = await Template.updateMany(
            { _id: { $in: templateIds } },
            { $set: updateData },
            { runValidators: true }
        );
        
        logger.info(`Batch updated metadata for ${result.modifiedCount} templates`);
        
        return res.json(formatSuccessResponse({
            message: 'Batch metadata update completed',
            matchedCount: result.matchedCount,
            modifiedCount: result.modifiedCount,
            templateIds,
            metadataUpdate
        }));
    });
};

module.exports = {
    getMetadata,
    updateMetadata,
    clearMetadata,
    updateResolution,
    updateSeed,
    updateGuidanceScale,
    getTemplatesByResolution,
    getTemplatesBySeed,
    getTemplatesByGuidanceScale,
    searchByMetadata,
    getResolutionStats,
    getGuidanceScaleStats,
    batchUpdateMetadata
};
