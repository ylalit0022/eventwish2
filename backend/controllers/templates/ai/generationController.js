const Template = require('../../../models/Template');
const { handleAsyncOperation, formatSuccessResponse, formatErrorResponse } = require('../../../utils/templateHelpers');
const logger = require('../../../utils/logger');

/**
 * AI Generation Controller
 * Handles CRUD operations for AI generation fields
 */

/**
 * Get AI generation data for a template
 */
const getGenerationData = async (req, res) => {
    await handleAsyncOperation(res, async () => {
        const { templateId } = req.params;
        
        const template = await Template.findById(templateId).select(
            'isAIGenerated aiPrompt aiModel aiStyle aiGenerationStage generationMetadata'
        );
        
        if (!template) {
            return res.status(404).json(formatErrorResponse(
                'TEMPLATE_NOT_FOUND',
                'Template not found',
                'templateId',
                templateId
            ));
        }
        
        logger.info(`Retrieved AI generation data for template: ${templateId}`);
        
        return res.json(formatSuccessResponse({
            templateId: template._id,
            isAIGenerated: template.isAIGenerated,
            aiPrompt: template.aiPrompt,
            aiModel: template.aiModel,
            aiStyle: template.aiStyle,
            aiGenerationStage: template.aiGenerationStage,
            generationMetadata: template.generationMetadata
        }));
    });
};

/**
 * Update AI generation data for a template
 */
const updateGenerationData = async (req, res) => {
    await handleAsyncOperation(res, async () => {
        const { templateId } = req.params;
        const { 
            isAIGenerated, aiPrompt, aiModel, aiStyle, 
            aiGenerationStage, generationMetadata 
        } = req.body;
        
        const updateData = {};
        const modifiedFields = [];
        
        if (isAIGenerated !== undefined) {
            updateData.isAIGenerated = isAIGenerated;
            modifiedFields.push('isAIGenerated');
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
        if (aiGenerationStage !== undefined) {
            updateData.aiGenerationStage = aiGenerationStage;
            modifiedFields.push('aiGenerationStage');
        }
        if (generationMetadata !== undefined) {
            updateData.generationMetadata = generationMetadata;
            modifiedFields.push('generationMetadata');
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
        ).select('isAIGenerated aiPrompt aiModel aiStyle aiGenerationStage generationMetadata');
        
        if (!template) {
            return res.status(404).json(formatErrorResponse(
                'TEMPLATE_NOT_FOUND',
                'Template not found',
                'templateId',
                templateId
            ));
        }
        
        logger.info(`Updated AI generation data for template: ${templateId}`, { modifiedFields });
        
        return res.json(formatSuccessResponse({
            templateId: template._id,
            isAIGenerated: template.isAIGenerated,
            aiPrompt: template.aiPrompt,
            aiModel: template.aiModel,
            aiStyle: template.aiStyle,
            aiGenerationStage: template.aiGenerationStage,
            generationMetadata: template.generationMetadata,
            modifiedFields
        }));
    });
};

/**
 * Clear AI generation data for a template
 */
const clearGenerationData = async (req, res) => {
    await handleAsyncOperation(res, async () => {
        const { templateId } = req.params;
        
        const template = await Template.findByIdAndUpdate(
            templateId,
            { 
                $set: {
                    isAIGenerated: false,
                    aiPrompt: '',
                    aiModel: '',
                    aiStyle: '',
                    aiGenerationStage: 'initial',
                    generationMetadata: {}
                }
            },
            { new: true, runValidators: true }
        ).select('isAIGenerated aiPrompt aiModel aiStyle aiGenerationStage generationMetadata');
        
        if (!template) {
            return res.status(404).json(formatErrorResponse(
                'TEMPLATE_NOT_FOUND',
                'Template not found',
                'templateId',
                templateId
            ));
        }
        
        logger.info(`Cleared AI generation data for template: ${templateId}`);
        
        return res.json(formatSuccessResponse({
            templateId: template._id,
            message: 'AI generation data cleared successfully',
            clearedFields: ['isAIGenerated', 'aiPrompt', 'aiModel', 'aiStyle', 'aiGenerationStage', 'generationMetadata']
        }));
    });
};

/**
 * Validate AI generation data without saving
 */
const validateGenerationData = async (req, res) => {
    await handleAsyncOperation(res, async () => {
        const { templateId } = req.params;
        const { 
            isAIGenerated, aiPrompt, aiModel, aiStyle, 
            aiGenerationStage, generationMetadata 
        } = req.body;
        
        // Validation is already handled by middleware
        // This endpoint confirms the data would be valid
        
        logger.info(`Validated AI generation data for template: ${templateId}`);
        
        return res.json(formatSuccessResponse({
            templateId,
            message: 'AI generation data is valid',
            validatedFields: {
                isAIGenerated,
                aiPrompt,
                aiModel,
                aiStyle,
                aiGenerationStage,
                generationMetadata
            }
        }));
    });
};

/**
 * Update AI generation stage
 */
const updateGenerationStage = async (req, res) => {
    await handleAsyncOperation(res, async () => {
        const { templateId } = req.params;
        const { stage } = req.body;
        
        if (!stage || !['initial', 'on_edit', 'variation'].includes(stage)) {
            return res.status(400).json(formatErrorResponse(
                'INVALID_STAGE',
                'Stage must be one of: initial, on_edit, variation',
                'stage',
                stage
            ));
        }
        
        const template = await Template.findByIdAndUpdate(
            templateId,
            { $set: { aiGenerationStage: stage } },
            { new: true, runValidators: true }
        ).select('aiGenerationStage');
        
        if (!template) {
            return res.status(404).json(formatErrorResponse(
                'TEMPLATE_NOT_FOUND',
                'Template not found',
                'templateId',
                templateId
            ));
        }
        
        logger.info(`Updated AI generation stage for template: ${templateId} to ${stage}`);
        
        return res.json(formatSuccessResponse({
            templateId: template._id,
            aiGenerationStage: template.aiGenerationStage,
            modifiedFields: ['aiGenerationStage']
        }));
    });
};

/**
 * Get templates by AI generation stage
 */
const getTemplatesByStage = async (req, res) => {
    await handleAsyncOperation(res, async () => {
        const { stage } = req.params;
        const { page = 1, limit = 20 } = req.query;
        
        if (!['initial', 'on_edit', 'variation'].includes(stage)) {
            return res.status(400).json(formatErrorResponse(
                'INVALID_STAGE',
                'Stage must be one of: initial, on_edit, variation',
                'stage',
                stage
            ));
        }
        
        const skip = (page - 1) * limit;
        
        const templates = await Template.find({ aiGenerationStage: stage })
            .select('title category isAIGenerated aiPrompt aiModel aiStyle aiGenerationStage')
            .skip(skip)
            .limit(parseInt(limit))
            .sort({ createdAt: -1 });
        
        const total = await Template.countDocuments({ aiGenerationStage: stage });
        
        logger.info(`Retrieved ${templates.length} templates with stage: ${stage}`);
        
        return res.json(formatSuccessResponse({
            templates,
            pagination: {
                page: parseInt(page),
                limit: parseInt(limit),
                total,
                pages: Math.ceil(total / limit)
            },
            filter: { stage }
        }));
    });
};

/**
 * Get templates by AI model
 */
const getTemplatesByModel = async (req, res) => {
    await handleAsyncOperation(res, async () => {
        const { model } = req.params;
        const { page = 1, limit = 20 } = req.query;
        
        const skip = (page - 1) * limit;
        
        const templates = await Template.find({ aiModel: model })
            .select('title category isAIGenerated aiPrompt aiModel aiStyle aiGenerationStage')
            .skip(skip)
            .limit(parseInt(limit))
            .sort({ createdAt: -1 });
        
        const total = await Template.countDocuments({ aiModel: model });
        
        logger.info(`Retrieved ${templates.length} templates with model: ${model}`);
        
        return res.json(formatSuccessResponse({
            templates,
            pagination: {
                page: parseInt(page),
                limit: parseInt(limit),
                total,
                pages: Math.ceil(total / limit)
            },
            filter: { model }
        }));
    });
};

/**
 * Get templates by AI style
 */
const getTemplatesByStyle = async (req, res) => {
    await handleAsyncOperation(res, async () => {
        const { style } = req.params;
        const { page = 1, limit = 20 } = req.query;
        
        const skip = (page - 1) * limit;
        
        const templates = await Template.find({ aiStyle: style })
            .select('title category isAIGenerated aiPrompt aiModel aiStyle aiGenerationStage')
            .skip(skip)
            .limit(parseInt(limit))
            .sort({ createdAt: -1 });
        
        const total = await Template.countDocuments({ aiStyle: style });
        
        logger.info(`Retrieved ${templates.length} templates with style: ${style}`);
        
        return res.json(formatSuccessResponse({
            templates,
            pagination: {
                page: parseInt(page),
                limit: parseInt(limit),
                total,
                pages: Math.ceil(total / limit)
            },
            filter: { style }
        }));
    });
};

/**
 * Batch update AI generation data for multiple templates
 */
const batchUpdateGeneration = async (req, res) => {
    await handleAsyncOperation(res, async () => {
        const { templateIds, updateData } = req.body;
        
        if (!templateIds || !Array.isArray(templateIds) || templateIds.length === 0) {
            return res.status(400).json(formatErrorResponse(
                'INVALID_TEMPLATE_IDS',
                'Template IDs array is required'
            ));
        }
        
        if (!updateData || Object.keys(updateData).length === 0) {
            return res.status(400).json(formatErrorResponse(
                'NO_UPDATE_DATA',
                'Update data is required'
            ));
        }
        
        const result = await Template.updateMany(
            { _id: { $in: templateIds } },
            { $set: updateData },
            { runValidators: true }
        );
        
        logger.info(`Batch updated AI generation data for ${result.modifiedCount} templates`);
        
        return res.json(formatSuccessResponse({
            message: 'Batch update completed',
            matchedCount: result.matchedCount,
            modifiedCount: result.modifiedCount,
            templateIds,
            updateData
        }));
    });
};

/**
 * Get AI generation statistics overview
 */
const getGenerationStats = async (req, res) => {
    await handleAsyncOperation(res, async () => {
        const [
            totalAIGenerated,
            stageStats,
            modelStats,
            styleStats
        ] = await Promise.all([
            Template.countDocuments({ isAIGenerated: true }),
            Template.aggregate([
                { $match: { isAIGenerated: true } },
                { $group: { _id: '$aiGenerationStage', count: { $sum: 1 } } }
            ]),
            Template.aggregate([
                { $match: { isAIGenerated: true, aiModel: { $ne: '' } } },
                { $group: { _id: '$aiModel', count: { $sum: 1 } } },
                { $sort: { count: -1 } }
            ]),
            Template.aggregate([
                { $match: { isAIGenerated: true, aiStyle: { $ne: '' } } },
                { $group: { _id: '$aiStyle', count: { $sum: 1 } } },
                { $sort: { count: -1 } }
            ])
        ]);
        
        logger.info('Retrieved AI generation statistics');
        
        return res.json(formatSuccessResponse({
            totalAIGenerated,
            stageDistribution: stageStats.reduce((acc, stat) => {
                acc[stat._id] = stat.count;
                return acc;
            }, {}),
            topModels: modelStats.slice(0, 10),
            topStyles: styleStats.slice(0, 10),
            generatedAt: new Date().toISOString()
        }));
    });
};

/**
 * Update generation metadata for a template
 */
const updateGenerationMetadata = async (req, res) => {
    await handleAsyncOperation(res, async () => {
        const { templateId } = req.params;
        const { resolution, seed, guidanceScale, timestamp } = req.body;
        
        const metadataUpdate = {};
        const modifiedFields = [];
        
        if (resolution !== undefined) {
            metadataUpdate['generationMetadata.resolution'] = resolution;
            modifiedFields.push('resolution');
        }
        if (seed !== undefined) {
            metadataUpdate['generationMetadata.seed'] = seed;
            modifiedFields.push('seed');
        }
        if (guidanceScale !== undefined) {
            metadataUpdate['generationMetadata.guidanceScale'] = guidanceScale;
            modifiedFields.push('guidanceScale');
        }
        if (timestamp !== undefined) {
            metadataUpdate['generationMetadata.timestamp'] = new Date(timestamp);
            modifiedFields.push('timestamp');
        }
        
        if (Object.keys(metadataUpdate).length === 0) {
            return res.status(400).json(formatErrorResponse(
                'NO_METADATA_TO_UPDATE',
                'No valid metadata fields provided for update'
            ));
        }
        
        const template = await Template.findByIdAndUpdate(
            templateId,
            { $set: metadataUpdate },
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
        
        logger.info(`Updated generation metadata for template: ${templateId}`, { modifiedFields });
        
        return res.json(formatSuccessResponse({
            templateId: template._id,
            generationMetadata: template.generationMetadata,
            modifiedFields
        }));
    });
};

/**
 * Get similar AI-generated templates based on prompt/style
 */
const getSimilarTemplates = async (req, res) => {
    await handleAsyncOperation(res, async () => {
        const { templateId } = req.params;
        const { limit = 10 } = req.query;
        
        const template = await Template.findById(templateId).select('aiPrompt aiStyle aiModel');
        
        if (!template) {
            return res.status(404).json(formatErrorResponse(
                'TEMPLATE_NOT_FOUND',
                'Template not found',
                'templateId',
                templateId
            ));
        }
        
        const similarityQuery = {
            _id: { $ne: templateId },
            isAIGenerated: true
        };
        
        // Build similarity query based on available fields
        const orConditions = [];
        if (template.aiStyle) {
            orConditions.push({ aiStyle: template.aiStyle });
        }
        if (template.aiModel) {
            orConditions.push({ aiModel: template.aiModel });
        }
        if (template.aiPrompt) {
            // Simple text similarity - in production, use more sophisticated matching
            orConditions.push({ 
                aiPrompt: { $regex: template.aiPrompt.split(' ').slice(0, 3).join('|'), $options: 'i' } 
            });
        }
        
        if (orConditions.length > 0) {
            similarityQuery.$or = orConditions;
        }
        
        const similarTemplates = await Template.find(similarityQuery)
            .select('title category aiPrompt aiStyle aiModel previewUrl')
            .limit(parseInt(limit))
            .sort({ createdAt: -1 });
        
        logger.info(`Found ${similarTemplates.length} similar templates for: ${templateId}`);
        
        return res.json(formatSuccessResponse({
            templateId,
            similarTemplates,
            basedOn: {
                aiStyle: template.aiStyle,
                aiModel: template.aiModel,
                promptKeywords: template.aiPrompt ? template.aiPrompt.split(' ').slice(0, 3) : []
            }
        }));
    });
};

module.exports = {
    getGenerationData,
    updateGenerationData,
    clearGenerationData,
    validateGenerationData,
    updateGenerationStage,
    getTemplatesByStage,
    getTemplatesByModel,
    getTemplatesByStyle,
    batchUpdateGeneration,
    getGenerationStats,
    updateGenerationMetadata,
    getSimilarTemplates
};
