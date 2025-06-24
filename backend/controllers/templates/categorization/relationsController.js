const Template = require('../../../models/Template');
const mongoose = require('mongoose');
const logger = require('../../../utils/logger');

/**
 * Relations Controller
 * Handles template relationship operations:
 * - relatedTemplates (Array of ObjectIds)
 * - similarTemplates (Array of ObjectIds)
 * - templateVariants (Array of ObjectIds)
 */

// Helper function to handle async operations
const handleAsyncOperation = async (operation, res, successMessage) => {
    try {
        const result = await operation();
        logger.info(`Relations operation successful: ${successMessage}`);
        return res.status(200).json({
            success: true,
            message: successMessage,
            data: result,
            timestamp: new Date().toISOString()
        });
    } catch (error) {
        logger.error(`Relations operation failed: ${error.message}`, { error: error.stack });
        
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

// Helper function to validate and clean ObjectId arrays
const validateObjectIdArray = (ids, fieldName = 'ids') => {
    if (!Array.isArray(ids)) {
        throw new Error(`${fieldName} must be an array`);
    }
    
    const validIds = ids.filter(id => {
        if (typeof id === 'string' && isValidObjectId(id)) {
            return true;
        }
        return false;
    });
    
    // Remove duplicates
    const uniqueIds = [...new Set(validIds)];
    
    if (uniqueIds.length > 50) {
        throw new Error(`${fieldName} cannot exceed 50 items`);
    }
    
    return uniqueIds;
};

/**
 * Get all relations for a template
 */
const getAllRelations = async (req, res) => {
    const { templateId } = req.params;
    
    await handleAsyncOperation(async () => {
        const template = await Template.findById(templateId)
            .select('relatedTemplates similarTemplates templateVariants')
            .populate('relatedTemplates', 'title description category')
            .populate('similarTemplates', 'title description category')
            .populate('templateVariants', 'title description category')
            .lean();
        
        if (!template) {
            throw new Error('Template not found');
        }
        
        return {
            templateId,
            relations: {
                related: template.relatedTemplates || [],
                similar: template.similarTemplates || [],
                variants: template.templateVariants || []
            }
        };
    }, res, 'All relations retrieved successfully');
};

/**
 * Update all relations for a template
 */
const updateAllRelations = async (req, res) => {
    const { templateId } = req.params;
    const { relatedTemplates, similarTemplates, templateVariants } = req.body;
    
    await handleAsyncOperation(async () => {
        const updateData = {};
        const modifiedFields = [];
        
        if (relatedTemplates !== undefined) {
            updateData.relatedTemplates = validateObjectIdArray(relatedTemplates, 'relatedTemplates');
            modifiedFields.push('relatedTemplates');
        }
        
        if (similarTemplates !== undefined) {
            updateData.similarTemplates = validateObjectIdArray(similarTemplates, 'similarTemplates');
            modifiedFields.push('similarTemplates');
        }
        
        if (templateVariants !== undefined) {
            updateData.templateVariants = validateObjectIdArray(templateVariants, 'templateVariants');
            modifiedFields.push('templateVariants');
        }
        
        const template = await Template.findByIdAndUpdate(
            templateId,
            updateData,
            { new: true, runValidators: true }
        ).select('relatedTemplates similarTemplates templateVariants')
        .populate('relatedTemplates', 'title description category')
        .populate('similarTemplates', 'title description category')
        .populate('templateVariants', 'title description category');
        
        return {
            templateId,
            modifiedFields,
            relations: {
                related: template.relatedTemplates || [],
                similar: template.similarTemplates || [],
                variants: template.templateVariants || []
            }
        };
    }, res, 'All relations updated successfully');
};

/**
 * Clear all relations for a template
 */
const clearAllRelations = async (req, res) => {
    const { templateId } = req.params;
    
    await handleAsyncOperation(async () => {
        const template = await Template.findByIdAndUpdate(
            templateId,
            {
                $unset: {
                    relatedTemplates: 1,
                    similarTemplates: 1,
                    templateVariants: 1
                }
            },
            { new: true }
        ).select('relatedTemplates similarTemplates templateVariants');
        
        return {
            templateId,
            clearedFields: ['relatedTemplates', 'similarTemplates', 'templateVariants']
        };
    }, res, 'All relations cleared successfully');
};

/**
 * Get related templates
 */
const getRelatedTemplates = async (req, res) => {
    const { templateId } = req.params;
    
    await handleAsyncOperation(async () => {
        const template = await Template.findById(templateId)
            .select('relatedTemplates')
            .populate('relatedTemplates', 'title description category usageCount rating')
            .lean();
        
        if (!template) {
            throw new Error('Template not found');
        }
        
        return {
            templateId,
            relatedTemplates: template.relatedTemplates || []
        };
    }, res, 'Related templates retrieved successfully');
};

/**
 * Update related templates
 */
const updateRelatedTemplates = async (req, res) => {
    const { templateId } = req.params;
    const { relatedTemplates } = req.body;
    
    await handleAsyncOperation(async () => {
        const validIds = validateObjectIdArray(relatedTemplates, 'relatedTemplates');
        
        const template = await Template.findByIdAndUpdate(
            templateId,
            { relatedTemplates: validIds },
            { new: true, runValidators: true }
        ).select('relatedTemplates')
        .populate('relatedTemplates', 'title description category usageCount rating');
        
        return {
            templateId,
            relatedTemplates: template.relatedTemplates || []
        };
    }, res, 'Related templates updated successfully');
};

/**
 * Add related templates
 */
const addRelatedTemplates = async (req, res) => {
    const { templateId } = req.params;
    const { relatedTemplates } = req.body;
    
    await handleAsyncOperation(async () => {
        const validIds = validateObjectIdArray(relatedTemplates, 'relatedTemplates');
        
        const template = await Template.findByIdAndUpdate(
            templateId,
            { $addToSet: { relatedTemplates: { $each: validIds } } },
            { new: true, runValidators: true }
        ).select('relatedTemplates')
        .populate('relatedTemplates', 'title description category usageCount rating');
        
        return {
            templateId,
            addedTemplates: validIds,
            allRelatedTemplates: template.relatedTemplates || []
        };
    }, res, 'Related templates added successfully');
};

/**
 * Remove related templates
 */
const removeRelatedTemplates = async (req, res) => {
    const { templateId } = req.params;
    const { relatedTemplates } = req.body;
    
    await handleAsyncOperation(async () => {
        const validIds = validateObjectIdArray(relatedTemplates, 'relatedTemplates');
        
        const template = await Template.findByIdAndUpdate(
            templateId,
            { $pull: { relatedTemplates: { $in: validIds } } },
            { new: true }
        ).select('relatedTemplates')
        .populate('relatedTemplates', 'title description category usageCount rating');
        
        return {
            templateId,
            removedTemplates: validIds,
            remainingRelatedTemplates: template.relatedTemplates || []
        };
    }, res, 'Related templates removed successfully');
};

/**
 * Get similar templates
 */
const getSimilarTemplates = async (req, res) => {
    const { templateId } = req.params;
    
    await handleAsyncOperation(async () => {
        const template = await Template.findById(templateId)
            .select('similarTemplates')
            .populate('similarTemplates', 'title description category usageCount rating')
            .lean();
        
        if (!template) {
            throw new Error('Template not found');
        }
        
        return {
            templateId,
            similarTemplates: template.similarTemplates || []
        };
    }, res, 'Similar templates retrieved successfully');
};

/**
 * Update similar templates
 */
const updateSimilarTemplates = async (req, res) => {
    const { templateId } = req.params;
    const { similarTemplates } = req.body;
    
    await handleAsyncOperation(async () => {
        const validIds = validateObjectIdArray(similarTemplates, 'similarTemplates');
        
        const template = await Template.findByIdAndUpdate(
            templateId,
            { similarTemplates: validIds },
            { new: true, runValidators: true }
        ).select('similarTemplates')
        .populate('similarTemplates', 'title description category usageCount rating');
        
        return {
            templateId,
            similarTemplates: template.similarTemplates || []
        };
    }, res, 'Similar templates updated successfully');
};

/**
 * Add similar templates
 */
const addSimilarTemplates = async (req, res) => {
    const { templateId } = req.params;
    const { similarTemplates } = req.body;
    
    await handleAsyncOperation(async () => {
        const validIds = validateObjectIdArray(similarTemplates, 'similarTemplates');
        
        const template = await Template.findByIdAndUpdate(
            templateId,
            { $addToSet: { similarTemplates: { $each: validIds } } },
            { new: true, runValidators: true }
        ).select('similarTemplates')
        .populate('similarTemplates', 'title description category usageCount rating');
        
        return {
            templateId,
            addedTemplates: validIds,
            allSimilarTemplates: template.similarTemplates || []
        };
    }, res, 'Similar templates added successfully');
};

/**
 * Remove similar templates
 */
const removeSimilarTemplates = async (req, res) => {
    const { templateId } = req.params;
    const { similarTemplates } = req.body;
    
    await handleAsyncOperation(async () => {
        const validIds = validateObjectIdArray(similarTemplates, 'similarTemplates');
        
        const template = await Template.findByIdAndUpdate(
            templateId,
            { $pull: { similarTemplates: { $in: validIds } } },
            { new: true }
        ).select('similarTemplates')
        .populate('similarTemplates', 'title description category usageCount rating');
        
        return {
            templateId,
            removedTemplates: validIds,
            remainingSimilarTemplates: template.similarTemplates || []
        };
    }, res, 'Similar templates removed successfully');
};

/**
 * Get template variants
 */
const getTemplateVariants = async (req, res) => {
    const { templateId } = req.params;
    
    await handleAsyncOperation(async () => {
        const template = await Template.findById(templateId)
            .select('templateVariants')
            .populate('templateVariants', 'title description category usageCount rating')
            .lean();
        
        if (!template) {
            throw new Error('Template not found');
        }
        
        return {
            templateId,
            templateVariants: template.templateVariants || []
        };
    }, res, 'Template variants retrieved successfully');
};

/**
 * Update template variants
 */
const updateTemplateVariants = async (req, res) => {
    const { templateId } = req.params;
    const { templateVariants } = req.body;
    
    await handleAsyncOperation(async () => {
        const validIds = validateObjectIdArray(templateVariants, 'templateVariants');
        
        const template = await Template.findByIdAndUpdate(
            templateId,
            { templateVariants: validIds },
            { new: true, runValidators: true }
        ).select('templateVariants')
        .populate('templateVariants', 'title description category usageCount rating');
        
        return {
            templateId,
            templateVariants: template.templateVariants || []
        };
    }, res, 'Template variants updated successfully');
};

/**
 * Add template variants
 */
const addTemplateVariants = async (req, res) => {
    const { templateId } = req.params;
    const { templateVariants } = req.body;
    
    await handleAsyncOperation(async () => {
        const validIds = validateObjectIdArray(templateVariants, 'templateVariants');
        
        const template = await Template.findByIdAndUpdate(
            templateId,
            { $addToSet: { templateVariants: { $each: validIds } } },
            { new: true, runValidators: true }
        ).select('templateVariants')
        .populate('templateVariants', 'title description category usageCount rating');
        
        return {
            templateId,
            addedVariants: validIds,
            allTemplateVariants: template.templateVariants || []
        };
    }, res, 'Template variants added successfully');
};

/**
 * Remove template variants
 */
const removeTemplateVariants = async (req, res) => {
    const { templateId } = req.params;
    const { templateVariants } = req.body;
    
    await handleAsyncOperation(async () => {
        const validIds = validateObjectIdArray(templateVariants, 'templateVariants');
        
        const template = await Template.findByIdAndUpdate(
            templateId,
            { $pull: { templateVariants: { $in: validIds } } },
            { new: true }
        ).select('templateVariants')
        .populate('templateVariants', 'title description category usageCount rating');
        
        return {
            templateId,
            removedVariants: validIds,
            remainingTemplateVariants: template.templateVariants || []
        };
    }, res, 'Template variants removed successfully');
};

/**
 * Auto-discover and set related templates based on similarity
 */
const autoDiscoverRelated = async (req, res) => {
    const { templateId } = req.params;
    const { limit = 10, threshold = 0.7 } = req.body;
    
    await handleAsyncOperation(async () => {
        const template = await Template.findById(templateId)
            .select('category tags styleTags searchKeywords festivalTag')
            .lean();
        
        if (!template) {
            throw new Error('Template not found');
        }
        
        // Build similarity query based on template attributes
        const similarityQuery = {
            _id: { $ne: templateId },
            $or: []
        };
        
        if (template.category) {
            similarityQuery.$or.push({ category: template.category });
        }
        
        if (template.festivalTag) {
            similarityQuery.$or.push({ festivalTag: template.festivalTag });
        }
        
        if (template.tags && template.tags.length > 0) {
            similarityQuery.$or.push({ tags: { $in: template.tags } });
        }
        
        if (template.styleTags && template.styleTags.length > 0) {
            similarityQuery.$or.push({ styleTags: { $in: template.styleTags } });
        }
        
        if (template.searchKeywords && template.searchKeywords.length > 0) {
            similarityQuery.$or.push({ searchKeywords: { $in: template.searchKeywords } });
        }
        
        if (similarityQuery.$or.length === 0) {
            throw new Error('Template has insufficient data for similarity matching');
        }
        
        const relatedTemplates = await Template.find(similarityQuery)
            .select('_id title description category usageCount rating')
            .sort({ usageCount: -1, rating: -1 })
            .limit(parseInt(limit))
            .lean();
        
        const relatedIds = relatedTemplates.map(t => t._id.toString());
        
        // Update the template with discovered relations
        const updatedTemplate = await Template.findByIdAndUpdate(
            templateId,
            { relatedTemplates: relatedIds },
            { new: true }
        ).select('relatedTemplates')
        .populate('relatedTemplates', 'title description category usageCount rating');
        
        return {
            templateId,
            discoveredRelated: relatedTemplates.length,
            relatedTemplates: updatedTemplate.relatedTemplates || []
        };
    }, res, 'Related templates auto-discovered successfully');
};

/**
 * Auto-discover and set similar templates based on content analysis
 */
const autoDiscoverSimilar = async (req, res) => {
    const { templateId } = req.params;
    const { limit = 10 } = req.body;
    
    await handleAsyncOperation(async () => {
        const template = await Template.findById(templateId)
            .select('title description category tags styleTags')
            .lean();
        
        if (!template) {
            throw new Error('Template not found');
        }
        
        // Use text search for content similarity
        const searchTerms = [];
        
        if (template.title) {
            searchTerms.push(...template.title.split(' ').filter(word => word.length > 3));
        }
        
        if (template.description) {
            searchTerms.push(...template.description.split(' ').filter(word => word.length > 3));
        }
        
        if (template.tags) {
            searchTerms.push(...template.tags);
        }
        
        if (searchTerms.length === 0) {
            throw new Error('Template has insufficient content for similarity analysis');
        }
        
        const searchRegex = new RegExp(searchTerms.join('|'), 'i');
        
        const similarTemplates = await Template.find({
            _id: { $ne: templateId },
            $or: [
                { title: searchRegex },
                { description: searchRegex },
                { tags: { $in: searchTerms } },
                { styleTags: { $in: searchTerms } }
            ]
        })
        .select('_id title description category usageCount rating')
        .sort({ usageCount: -1, rating: -1 })
        .limit(parseInt(limit))
        .lean();
        
        const similarIds = similarTemplates.map(t => t._id.toString());
        
        // Update the template with discovered similar templates
        const updatedTemplate = await Template.findByIdAndUpdate(
            templateId,
            { similarTemplates: similarIds },
            { new: true }
        ).select('similarTemplates')
        .populate('similarTemplates', 'title description category usageCount rating');
        
        return {
            templateId,
            discoveredSimilar: similarTemplates.length,
            similarTemplates: updatedTemplate.similarTemplates || []
        };
    }, res, 'Similar templates auto-discovered successfully');
};

/**
 * Get template relationship graph
 */
const getRelationshipGraph = async (req, res) => {
    const { templateId } = req.params;
    const { depth = 2 } = req.query;
    
    await handleAsyncOperation(async () => {
        const visited = new Set();
        const graph = { nodes: [], edges: [] };
        
        const buildGraph = async (currentId, currentDepth) => {
            if (currentDepth > parseInt(depth) || visited.has(currentId)) {
                return;
            }
            
            visited.add(currentId);
            
            const template = await Template.findById(currentId)
                .select('title description category relatedTemplates similarTemplates templateVariants')
                .populate('relatedTemplates', 'title description category')
                .populate('similarTemplates', 'title description category')
                .populate('templateVariants', 'title description category')
                .lean();
            
            if (!template) return;
            
            // Add current node
            graph.nodes.push({
                id: currentId,
                title: template.title,
                description: template.description,
                category: template.category,
                depth: currentDepth
            });
            
            // Add edges and recurse
            const relations = [
                { type: 'related', templates: template.relatedTemplates || [] },
                { type: 'similar', templates: template.similarTemplates || [] },
                { type: 'variant', templates: template.templateVariants || [] }
            ];
            
            for (const relation of relations) {
                for (const relatedTemplate of relation.templates) {
                    const relatedId = relatedTemplate._id.toString();
                    
                    graph.edges.push({
                        source: currentId,
                        target: relatedId,
                        type: relation.type
                    });
                    
                    if (currentDepth < parseInt(depth)) {
                        await buildGraph(relatedId, currentDepth + 1);
                    }
                }
            }
        };
        
        await buildGraph(templateId, 0);
        
        return {
            templateId,
            depth: parseInt(depth),
            graph: {
                nodes: graph.nodes,
                edges: graph.edges,
                stats: {
                    totalNodes: graph.nodes.length,
                    totalEdges: graph.edges.length,
                    edgeTypes: {
                        related: graph.edges.filter(e => e.type === 'related').length,
                        similar: graph.edges.filter(e => e.type === 'similar').length,
                        variant: graph.edges.filter(e => e.type === 'variant').length
                    }
                }
            }
        };
    }, res, 'Relationship graph retrieved successfully');
};

/**
 * Batch create relationships between templates
 */
const batchCreateRelations = async (req, res) => {
    const { relations } = req.body;
    
    await handleAsyncOperation(async () => {
        if (!Array.isArray(relations) || relations.length === 0) {
            throw new Error('relations must be a non-empty array');
        }
        
        const operations = [];
        
        for (const relation of relations) {
            const { sourceId, targetIds, relationType } = relation;
            
            if (!isValidObjectId(sourceId)) {
                throw new Error(`Invalid sourceId: ${sourceId}`);
            }
            
            const validTargetIds = validateObjectIdArray(targetIds, 'targetIds');
            
            if (!['related', 'similar', 'variant'].includes(relationType)) {
                throw new Error(`Invalid relationType: ${relationType}`);
            }
            
            const fieldName = relationType === 'related' ? 'relatedTemplates' :
                             relationType === 'similar' ? 'similarTemplates' :
                             'templateVariants';
            
            operations.push({
                updateOne: {
                    filter: { _id: sourceId },
                    update: { $addToSet: { [fieldName]: { $each: validTargetIds } } }
                }
            });
        }
        
        const result = await Template.bulkWrite(operations);
        
        return {
            processedRelations: relations.length,
            matchedCount: result.matchedCount,
            modifiedCount: result.modifiedCount
        };
    }, res, 'Batch relations created successfully');
};

/**
 * Batch remove relationships between templates
 */
const batchRemoveRelations = async (req, res) => {
    const { relations } = req.body;
    
    await handleAsyncOperation(async () => {
        if (!Array.isArray(relations) || relations.length === 0) {
            throw new Error('relations must be a non-empty array');
        }
        
        const operations = [];
        
        for (const relation of relations) {
            const { sourceId, targetIds, relationType } = relation;
            
            if (!isValidObjectId(sourceId)) {
                throw new Error(`Invalid sourceId: ${sourceId}`);
            }
            
            const validTargetIds = validateObjectIdArray(targetIds, 'targetIds');
            
            if (!['related', 'similar', 'variant'].includes(relationType)) {
                throw new Error(`Invalid relationType: ${relationType}`);
            }
            
            const fieldName = relationType === 'related' ? 'relatedTemplates' :
                             relationType === 'similar' ? 'similarTemplates' :
                             'templateVariants';
            
            operations.push({
                updateOne: {
                    filter: { _id: sourceId },
                    update: { $pull: { [fieldName]: { $in: validTargetIds } } }
                }
            });
        }
        
        const result = await Template.bulkWrite(operations);
        
        return {
            processedRelations: relations.length,
            matchedCount: result.matchedCount,
            modifiedCount: result.modifiedCount
        };
    }, res, 'Batch relations removed successfully');
};

/**
 * Get relations statistics overview
 */
const getRelationsStats = async (req, res) => {
    await handleAsyncOperation(async () => {
        const stats = await Template.aggregate([
            {
                $project: {
                    hasRelated: { $gt: [{ $size: { $ifNull: ['$relatedTemplates', []] } }, 0] },
                    hasSimilar: { $gt: [{ $size: { $ifNull: ['$similarTemplates', []] } }, 0] },
                    hasVariants: { $gt: [{ $size: { $ifNull: ['$templateVariants', []] } }, 0] },
                    relatedCount: { $size: { $ifNull: ['$relatedTemplates', []] } },
                    similarCount: { $size: { $ifNull: ['$similarTemplates', []] } },
                    variantsCount: { $size: { $ifNull: ['$templateVariants', []] } }
                }
            },
            {
                $group: {
                    _id: null,
                    totalTemplates: { $sum: 1 },
                    templatesWithRelated: { $sum: { $cond: ['$hasRelated', 1, 0] } },
                    templatesWithSimilar: { $sum: { $cond: ['$hasSimilar', 1, 0] } },
                    templatesWithVariants: { $sum: { $cond: ['$hasVariants', 1, 0] } },
                    totalRelatedConnections: { $sum: '$relatedCount' },
                    totalSimilarConnections: { $sum: '$similarCount' },
                    totalVariantConnections: { $sum: '$variantsCount' },
                    avgRelatedPerTemplate: { $avg: '$relatedCount' },
                    avgSimilarPerTemplate: { $avg: '$similarCount' },
                    avgVariantsPerTemplate: { $avg: '$variantsCount' }
                }
            }
        ]);
        
        const result = stats[0] || {};
        
        return {
            totalTemplates: result.totalTemplates || 0,
            coverage: {
                withRelated: result.templatesWithRelated || 0,
                withSimilar: result.templatesWithSimilar || 0,
                withVariants: result.templatesWithVariants || 0
            },
            connections: {
                totalRelated: result.totalRelatedConnections || 0,
                totalSimilar: result.totalSimilarConnections || 0,
                totalVariants: result.totalVariantConnections || 0
            },
            averages: {
                relatedPerTemplate: Math.round((result.avgRelatedPerTemplate || 0) * 100) / 100,
                similarPerTemplate: Math.round((result.avgSimilarPerTemplate || 0) * 100) / 100,
                variantsPerTemplate: Math.round((result.avgVariantsPerTemplate || 0) * 100) / 100
            }
        };
    }, res, 'Relations statistics retrieved successfully');
};

/**
 * Validate template relations integrity
 */
const validateRelations = async (req, res) => {
    const { templateId } = req.params;
    
    await handleAsyncOperation(async () => {
        const template = await Template.findById(templateId)
            .select('relatedTemplates similarTemplates templateVariants')
            .lean();
        
        if (!template) {
            throw new Error('Template not found');
        }
        
        const issues = [];
        const validations = [];
        
        // Check related templates
        if (template.relatedTemplates && template.relatedTemplates.length > 0) {
            const existingRelated = await Template.find({
                _id: { $in: template.relatedTemplates }
            }).select('_id').lean();
            
            const existingIds = existingRelated.map(t => t._id.toString());
            const missingRelated = template.relatedTemplates.filter(
                id => !existingIds.includes(id.toString())
            );
            
            if (missingRelated.length > 0) {
                issues.push({
                    type: 'missing_related',
                    count: missingRelated.length,
                    ids: missingRelated
                });
            }
            
            validations.push({
                type: 'related',
                total: template.relatedTemplates.length,
                existing: existingIds.length,
                missing: missingRelated.length
            });
        }
        
        // Check similar templates
        if (template.similarTemplates && template.similarTemplates.length > 0) {
            const existingSimilar = await Template.find({
                _id: { $in: template.similarTemplates }
            }).select('_id').lean();
            
            const existingIds = existingSimilar.map(t => t._id.toString());
            const missingSimilar = template.similarTemplates.filter(
                id => !existingIds.includes(id.toString())
            );
            
            if (missingSimilar.length > 0) {
                issues.push({
                    type: 'missing_similar',
                    count: missingSimilar.length,
                    ids: missingSimilar
                });
            }
            
            validations.push({
                type: 'similar',
                total: template.similarTemplates.length,
                existing: existingIds.length,
                missing: missingSimilar.length
            });
        }
        
        // Check template variants
        if (template.templateVariants && template.templateVariants.length > 0) {
            const existingVariants = await Template.find({
                _id: { $in: template.templateVariants }
            }).select('_id').lean();
            
            const existingIds = existingVariants.map(t => t._id.toString());
            const missingVariants = template.templateVariants.filter(
                id => !existingIds.includes(id.toString())
            );
            
            if (missingVariants.length > 0) {
                issues.push({
                    type: 'missing_variants',
                    count: missingVariants.length,
                    ids: missingVariants
                });
            }
            
            validations.push({
                type: 'variants',
                total: template.templateVariants.length,
                existing: existingIds.length,
                missing: missingVariants.length
            });
        }
        
        const isValid = issues.length === 0;
        
        return {
            templateId,
            isValid,
            issues,
            validations,
            summary: {
                totalIssues: issues.length,
                totalValidations: validations.length
            }
        };
    }, res, 'Relations validation completed successfully');
};

module.exports = {
    getAllRelations,
    updateAllRelations,
    clearAllRelations,
    getRelatedTemplates,
    updateRelatedTemplates,
    addRelatedTemplates,
    removeRelatedTemplates,
    getSimilarTemplates,
    updateSimilarTemplates,
    addSimilarTemplates,
    removeSimilarTemplates,
    getTemplateVariants,
    updateTemplateVariants,
    addTemplateVariants,
    removeTemplateVariants,
    autoDiscoverRelated,
    autoDiscoverSimilar,
    getRelationshipGraph,
    batchCreateRelations,
    batchRemoveRelations,
    getRelationsStats,
    validateRelations
}; 