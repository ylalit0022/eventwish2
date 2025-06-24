const Template = require('../../../models/Template');
const logger = require('../../../utils/logger');
const { handleAsyncOperation, createSuccessResponse, createErrorResponse } = require('../../../utils/templateHelpers');

/**
 * Add legacy reference to template
 */
const addLegacyReference = async (req, res) => {
    await handleAsyncOperation(res, async () => {
        const { id } = req.params;
        const { referenceType, referenceId, referenceData } = req.body;

        if (!referenceType || !referenceId) {
            return createErrorResponse(
                'Reference type and ID are required',
                'VALIDATION_ERROR',
                'reference',
                { referenceType, referenceId }
            );
        }

        // Create reference object
        const reference = {
            type: referenceType,
            id: referenceId,
            data: referenceData || {},
            createdAt: new Date()
        };

        const template = await Template.findByIdAndUpdate(
            id,
            { 
                $push: { 
                    legacyReferences: reference
                },
                updatedAt: new Date()
            },
            { new: true, runValidators: true }
        );

        if (!template) {
            return createErrorResponse('Template not found', 'TEMPLATE_NOT_FOUND', 'id', id);
        }

        logger.info(`Added legacy reference to template ${id}`, {
            templateId: id,
            referenceType,
            referenceId
        });

        return createSuccessResponse({
            template: {
                id: template._id,
                legacyReferences: template.legacyReferences,
                updatedAt: template.updatedAt
            },
            addedReference: reference
        }, 'Legacy reference added successfully');
    });
};

/**
 * Remove legacy reference from template
 */
const removeLegacyReference = async (req, res) => {
    await handleAsyncOperation(res, async () => {
        const { id, referenceId } = req.params;

        const template = await Template.findByIdAndUpdate(
            id,
            { 
                $pull: { 
                    legacyReferences: { id: referenceId }
                },
                updatedAt: new Date()
            },
            { new: true, runValidators: true }
        );

        if (!template) {
            return createErrorResponse('Template not found', 'TEMPLATE_NOT_FOUND', 'id', id);
        }

        logger.info(`Removed legacy reference from template ${id}`, {
            templateId: id,
            referenceId
        });

        return createSuccessResponse({
            template: {
                id: template._id,
                legacyReferences: template.legacyReferences,
                updatedAt: template.updatedAt
            }
        }, 'Legacy reference removed successfully');
    });
};

/**
 * Get all legacy references for template
 */
const getLegacyReferences = async (req, res) => {
    await handleAsyncOperation(res, async () => {
        const { id } = req.params;

        const template = await Template.findById(id).select('legacyReferences');

        if (!template) {
            return createErrorResponse('Template not found', 'TEMPLATE_NOT_FOUND', 'id', id);
        }

        const references = template.legacyReferences || [];
        const referencesByType = {};

        // Group references by type
        references.forEach(ref => {
            if (!referencesByType[ref.type]) {
                referencesByType[ref.type] = [];
            }
            referencesByType[ref.type].push(ref);
        });

        return createSuccessResponse({
            templateId: id,
            references,
            referencesByType,
            totalReferences: references.length
        });
    });
};

/**
 * Update legacy reference
 */
const updateLegacyReference = async (req, res) => {
    await handleAsyncOperation(res, async () => {
        const { id, referenceId } = req.params;
        const { referenceData } = req.body;

        const template = await Template.findOneAndUpdate(
            { 
                _id: id, 
                'legacyReferences.id': referenceId 
            },
            { 
                $set: { 
                    'legacyReferences.$.data': referenceData,
                    'legacyReferences.$.updatedAt': new Date()
                },
                updatedAt: new Date()
            },
            { new: true, runValidators: true }
        );

        if (!template) {
            return createErrorResponse(
                'Template or reference not found',
                'TEMPLATE_OR_REFERENCE_NOT_FOUND',
                'id',
                { templateId: id, referenceId }
            );
        }

        const updatedReference = template.legacyReferences.find(ref => ref.id === referenceId);

        logger.info(`Updated legacy reference for template ${id}`, {
            templateId: id,
            referenceId,
            referenceData
        });

        return createSuccessResponse({
            template: {
                id: template._id,
                updatedAt: template.updatedAt
            },
            updatedReference
        }, 'Legacy reference updated successfully');
    });
};

/**
 * Validate legacy reference data
 */
const validateLegacyReference = async (req, res) => {
    await handleAsyncOperation(res, async () => {
        const { referenceType, referenceId, referenceData } = req.body;
        const errors = [];
        const warnings = [];

        // Validate required fields
        if (!referenceType || typeof referenceType !== 'string') {
            errors.push('Reference type is required and must be a string');
        }

        if (!referenceId || typeof referenceId !== 'string') {
            errors.push('Reference ID is required and must be a string');
        }

        // Validate reference data structure
        if (referenceData && typeof referenceData !== 'object') {
            errors.push('Reference data must be an object');
        }

        // Check for common reference types
        const commonTypes = ['external_id', 'old_system_id', 'migration_ref', 'backup_ref'];
        if (referenceType && !commonTypes.includes(referenceType)) {
            warnings.push(`Uncommon reference type: ${referenceType}`);
        }

        return createSuccessResponse({
            isValid: errors.length === 0,
            errors,
            warnings,
            validatedReference: errors.length === 0 ? { referenceType, referenceId, referenceData } : null
        });
    });
};

/**
 * Migrate legacy references to new format
 */
const migrateLegacyReferences = async (req, res) => {
    await handleAsyncOperation(res, async () => {
        const { id } = req.params;

        const template = await Template.findById(id);

        if (!template) {
            return createErrorResponse('Template not found', 'TEMPLATE_NOT_FOUND', 'id', id);
        }

        const migrations = [];
        const legacyReferences = template.legacyReferences || [];

        // Process each legacy reference
        legacyReferences.forEach((ref, index) => {
            // Add migration timestamp if missing
            if (!ref.createdAt) {
                template.legacyReferences[index].createdAt = new Date();
                migrations.push({
                    referenceId: ref.id,
                    action: 'added_timestamp',
                    field: 'createdAt'
                });
            }

            // Standardize reference type format
            if (ref.type && ref.type !== ref.type.toLowerCase()) {
                template.legacyReferences[index].type = ref.type.toLowerCase();
                migrations.push({
                    referenceId: ref.id,
                    action: 'standardized_type',
                    oldValue: ref.type,
                    newValue: ref.type.toLowerCase()
                });
            }
        });

        if (migrations.length > 0) {
            const updatedTemplate = await template.save();

            logger.info(`Migrated legacy references for template ${id}`, {
                templateId: id,
                migrations
            });

            return createSuccessResponse({
                template: {
                    id: updatedTemplate._id,
                    updatedAt: updatedTemplate.updatedAt
                },
                migrations,
                migrationCount: migrations.length
            }, `Legacy references migrated with ${migrations.length} changes`);
        } else {
            return createSuccessResponse({
                template: {
                    id: template._id
                },
                migrations: [],
                migrationCount: 0
            }, 'No migration needed - references already in correct format');
        }
    });
};

/**
 * Cleanup orphaned legacy references
 */
const cleanupOrphanedReferences = async (req, res) => {
    await handleAsyncOperation(res, async () => {
        // This would typically check external systems to verify reference validity
        // For now, we'll clean up references with invalid structure

        const templates = await Template.find({ 
            legacyReferences: { $exists: true, $ne: [] } 
        });

        let cleanedCount = 0;
        const cleanupResults = [];

        for (const template of templates) {
            const validReferences = [];
            const removedReferences = [];

            template.legacyReferences.forEach(ref => {
                // Check if reference has required fields
                if (ref.type && ref.id) {
                    validReferences.push(ref);
                } else {
                    removedReferences.push(ref);
                    cleanedCount++;
                }
            });

            if (removedReferences.length > 0) {
                template.legacyReferences = validReferences;
                await template.save();

                cleanupResults.push({
                    templateId: template._id,
                    removedCount: removedReferences.length,
                    removedReferences
                });
            }
        }

        logger.info(`Cleaned up ${cleanedCount} orphaned legacy references`, {
            cleanedCount,
            templatesAffected: cleanupResults.length
        });

        return createSuccessResponse({
            cleanedCount,
            templatesAffected: cleanupResults.length,
            cleanupResults
        }, `Cleaned up ${cleanedCount} orphaned legacy references`);
    });
};

/**
 * Get legacy reference statistics
 */
const getLegacyReferenceStats = async (req, res) => {
    await handleAsyncOperation(res, async () => {
        const pipeline = [
            { $match: { legacyReferences: { $exists: true, $ne: [] } } },
            { $unwind: '$legacyReferences' },
            {
                $group: {
                    _id: '$legacyReferences.type',
                    count: { $sum: 1 },
                    templates: { $addToSet: '$_id' }
                }
            },
            {
                $project: {
                    referenceType: '$_id',
                    count: 1,
                    templateCount: { $size: '$templates' },
                    _id: 0
                }
            },
            { $sort: { count: -1 } }
        ];

        const stats = await Template.aggregate(pipeline);

        const totalReferences = stats.reduce((sum, stat) => sum + stat.count, 0);
        const totalTemplatesWithReferences = await Template.countDocuments({
            legacyReferences: { $exists: true, $ne: [] }
        });

        return createSuccessResponse({
            totalReferences,
            totalTemplatesWithReferences,
            referenceTypeStats: stats,
            uniqueReferenceTypes: stats.length
        });
    });
};

module.exports = {
    addLegacyReference,
    removeLegacyReference,
    getLegacyReferences,
    updateLegacyReference,
    validateLegacyReference,
    migrateLegacyReferences,
    cleanupOrphanedReferences,
    getLegacyReferenceStats
}; 