// Moderation Controller

const Template = require('../../../models/Template');
const logger = require('../../../utils/logger');
const { 
    formatErrorResponse, 
    formatSuccessResponse,
    isValidObjectId
} = require('../../../utils/templateHelpers');

/**
 * Get template moderation information
 * @route GET /api/templates/monetization/moderation/:templateId
 * @access Admin
 */
const getModerationInfo = async (req, res) => {
    try {
        const { templateId } = req.params;

        if (!isValidObjectId(templateId)) {
            return res.status(400).json(formatErrorResponse(
                'VALIDATION_ERROR',
                'Invalid template ID format',
                'templateId',
                templateId
            ));
        }

        logger.info(`Fetching template moderation info: ${templateId}`);

        const template = await Template.findById(templateId)
            .select('title moderationStatus moderationNotes status createdAt updatedAt');

        if (!template) {
            return res.status(404).json(formatErrorResponse(
                'NOT_FOUND',
                'Template not found',
                'templateId',
                templateId
            ));
        }

        logger.info(`Template moderation info retrieved: ${templateId}`);

        res.json(formatSuccessResponse({
            templateId: template._id,
            title: template.title,
            moderation: {
                status: template.moderationStatus || 'pending',
                notes: template.moderationNotes || '',
                isActive: template.status,
                createdAt: template.createdAt,
                lastModified: template.updatedAt
            }
        }));

    } catch (error) {
        logger.error('Error getting template moderation info:', error);
        res.status(500).json(formatErrorResponse(
            'SERVER_ERROR',
            'Internal server error while retrieving moderation info',
            null,
            null,
            error.message
        ));
    }
};

/**
 * Update template moderation status and notes
 * @route PUT /api/templates/monetization/moderation/:templateId
 * @access Admin
 */
const updateModerationStatus = async (req, res) => {
    try {
        const { templateId } = req.params;
        const updateData = req.body;

        if (!isValidObjectId(templateId)) {
            return res.status(400).json(formatErrorResponse(
                'VALIDATION_ERROR',
                'Invalid template ID format',
                'templateId',
                templateId
            ));
        }

        logger.info(`Updating template moderation status: ${templateId}`, updateData);

        // Validate update fields
        const allowedFields = ['moderationStatus', 'moderationNotes'];
        const updateFields = Object.keys(updateData).filter(key => allowedFields.includes(key));
        
        if (updateFields.length === 0) {
            return res.status(400).json(formatErrorResponse(
                'VALIDATION_ERROR',
                'No valid moderation fields provided for update',
                null,
                null,
                { allowedFields }
            ));
        }

        // Validate moderation status enum
        const validStatuses = ['pending', 'approved', 'rejected'];
        if (updateData.moderationStatus && !validStatuses.includes(updateData.moderationStatus)) {
            return res.status(400).json(formatErrorResponse(
                'VALIDATION_ERROR',
                'Invalid moderation status',
                'moderationStatus',
                updateData.moderationStatus,
                { validStatuses }
            ));
        }

        // Prepare update object
        const updateObject = { updatedAt: new Date() };
        updateFields.forEach(field => {
            updateObject[field] = updateData[field];
        });

        // Auto-activate approved templates, deactivate rejected ones
        if (updateData.moderationStatus === 'approved') {
            updateObject.status = true;
        } else if (updateData.moderationStatus === 'rejected') {
            updateObject.status = false;
        }

        const updatedTemplate = await Template.findByIdAndUpdate(
            templateId,
            updateObject,
            { 
                new: true, 
                runValidators: true,
                select: 'title moderationStatus moderationNotes status updatedAt'
            }
        );

        if (!updatedTemplate) {
            return res.status(404).json(formatErrorResponse(
                'NOT_FOUND',
                'Template not found',
                'templateId',
                templateId
            ));
        }

        logger.info(`Template moderation status updated: ${templateId}`, { 
            updatedFields: updateFields,
            newStatus: updatedTemplate.moderationStatus 
        });

        res.json(formatSuccessResponse({
            templateId: updatedTemplate._id,
            title: updatedTemplate.title,
            moderation: {
                status: updatedTemplate.moderationStatus,
                notes: updatedTemplate.moderationNotes || '',
                isActive: updatedTemplate.status,
                lastModified: updatedTemplate.updatedAt
            }
        }, updateFields));

    } catch (error) {
        logger.error('Error updating template moderation status:', error);

        if (error.name === 'ValidationError') {
            const validationErrors = Object.keys(error.errors).map(key => ({
                field: key,
                message: error.errors[key].message,
                value: error.errors[key].value
            }));

            return res.status(400).json(formatErrorResponse(
                'VALIDATION_ERROR',
                'Moderation status validation failed',
                null,
                null,
                { validationErrors }
            ));
        }

        res.status(500).json(formatErrorResponse(
            'SERVER_ERROR',
            'Internal server error while updating moderation status',
            null,
            null,
            error.message
        ));
    }
};

/**
 * Get templates pending moderation (moderation queue)
 * @route GET /api/templates/monetization/moderation/queue
 * @access Admin
 */
const getModerationQueue = async (req, res) => {
    try {
        const { page = 1, limit = 20, status } = req.query;
        const skip = (parseInt(page) - 1) * parseInt(limit);

        logger.info('Fetching moderation queue:', { page, limit, status });

        // Build query based on status filter
        let query = {};
        if (status) {
            const validStatuses = ['pending', 'approved', 'rejected'];
            if (!validStatuses.includes(status)) {
                return res.status(400).json(formatErrorResponse(
                    'VALIDATION_ERROR',
                    'Invalid moderation status filter',
                    'status',
                    status,
                    { validStatuses }
                ));
            }
            query.moderationStatus = status;
        } else {
            // Default to pending if no status specified
            query.moderationStatus = { $in: ['pending', null] };
        }

        const templates = await Template.find(query)
            .select('title category moderationStatus moderationNotes status createdAt updatedAt')
            .sort({ createdAt: -1 })
            .skip(skip)
            .limit(parseInt(limit));

        const totalCount = await Template.countDocuments(query);
        const totalPages = Math.ceil(totalCount / parseInt(limit));

        logger.info(`Moderation queue retrieved: ${templates.length} items`);

        res.json(formatSuccessResponse({
            templates: templates.map(template => ({
                templateId: template._id,
                title: template.title,
                category: template.category,
                moderation: {
                    status: template.moderationStatus || 'pending',
                    notes: template.moderationNotes || '',
                    isActive: template.status
                },
                createdAt: template.createdAt,
                updatedAt: template.updatedAt
            })),
            pagination: {
                currentPage: parseInt(page),
                totalPages,
                totalItems: totalCount,
                itemsPerPage: parseInt(limit),
                hasNextPage: parseInt(page) < totalPages,
                hasPrevPage: parseInt(page) > 1
            },
            filter: {
                status: status || 'pending'
            }
        }));

    } catch (error) {
        logger.error('Error retrieving moderation queue:', error);
        res.status(500).json(formatErrorResponse(
            'SERVER_ERROR',
            'Internal server error while retrieving moderation queue',
            null,
            null,
            error.message
        ));
    }
};

/**
 * Bulk update moderation status for multiple templates
 * @route POST /api/templates/monetization/moderation/bulk-update
 * @access Admin
 */
const bulkModerationUpdate = async (req, res) => {
    try {
        const { templateIds, moderationStatus, moderationNotes } = req.body;

        if (!Array.isArray(templateIds) || templateIds.length === 0) {
            return res.status(400).json(formatErrorResponse(
                'VALIDATION_ERROR',
                'templateIds must be a non-empty array',
                'templateIds',
                templateIds
            ));
        }

        // Validate all template IDs
        const invalidIds = templateIds.filter(id => !isValidObjectId(id));
        if (invalidIds.length > 0) {
            return res.status(400).json(formatErrorResponse(
                'VALIDATION_ERROR',
                'Invalid template ID format in array',
                'templateIds',
                invalidIds
            ));
        }

        // Validate moderation status
        const validStatuses = ['pending', 'approved', 'rejected'];
        if (!moderationStatus || !validStatuses.includes(moderationStatus)) {
            return res.status(400).json(formatErrorResponse(
                'VALIDATION_ERROR',
                'Valid moderation status is required',
                'moderationStatus',
                moderationStatus,
                { validStatuses }
            ));
        }

        logger.info('Bulk updating moderation status:', { 
            templateCount: templateIds.length, 
            moderationStatus,
            moderationNotes 
        });

        // Prepare update object
        const updateObject = {
            moderationStatus: moderationStatus,
            updatedAt: new Date()
        };

        if (moderationNotes) {
            updateObject.moderationNotes = moderationNotes;
        }

        // Auto-activate approved templates, deactivate rejected ones
        if (moderationStatus === 'approved') {
            updateObject.status = true;
        } else if (moderationStatus === 'rejected') {
            updateObject.status = false;
        }

        const updateResult = await Template.updateMany(
            { _id: { $in: templateIds } },
            updateObject
        );

        logger.info(`Bulk moderation update completed: ${updateResult.modifiedCount} templates updated`);

        res.json(formatSuccessResponse({
            templateIds: templateIds,
            updatedCount: updateResult.modifiedCount,
            matchedCount: updateResult.matchedCount,
            appliedUpdates: {
                moderationStatus: moderationStatus,
                moderationNotes: moderationNotes || 'Updated via bulk operation',
                status: updateObject.status
            },
            message: `${updateResult.modifiedCount} templates ${moderationStatus} successfully`
        }, ['moderationStatus', 'moderationNotes', 'status']));

    } catch (error) {
        logger.error('Error during bulk moderation update:', error);
        res.status(500).json(formatErrorResponse(
            'SERVER_ERROR',
            'Internal server error during bulk moderation update',
            null,
            null,
            error.message
        ));
    }
};

/**
 * Approve multiple templates in batch
 * @route POST /api/templates/monetization/moderation/batch/approve
 * @access Admin
 */
const approveModerationBatch = async (req, res) => {
    try {
        const { templateIds, moderationNotes = 'Batch approved for publication' } = req.body;

        if (!Array.isArray(templateIds) || templateIds.length === 0) {
            return res.status(400).json(formatErrorResponse(
                'VALIDATION_ERROR',
                'templateIds must be a non-empty array',
                'templateIds',
                templateIds
            ));
        }

        // Validate all template IDs
        const invalidIds = templateIds.filter(id => !isValidObjectId(id));
        if (invalidIds.length > 0) {
            return res.status(400).json(formatErrorResponse(
                'VALIDATION_ERROR',
                'Invalid template ID format in array',
                'templateIds',
                invalidIds
            ));
        }

        logger.info('Batch approving templates:', { 
            templateCount: templateIds.length, 
            moderationNotes 
        });

        const updateResult = await Template.updateMany(
            { _id: { $in: templateIds } },
            {
                moderationStatus: 'approved',
                moderationNotes: moderationNotes,
                status: true, // Activate approved templates
                updatedAt: new Date()
            }
        );

        logger.info(`Batch approval completed: ${updateResult.modifiedCount} templates approved`);

        res.json(formatSuccessResponse({
            templateIds: templateIds,
            approvedCount: updateResult.modifiedCount,
            matchedCount: updateResult.matchedCount,
            moderationNotes: moderationNotes,
            message: `${updateResult.modifiedCount} templates approved successfully`
        }, ['moderationStatus', 'moderationNotes', 'status']));

    } catch (error) {
        logger.error('Error during batch approval:', error);
        res.status(500).json(formatErrorResponse(
            'SERVER_ERROR',
            'Internal server error during batch approval',
            null,
            null,
            error.message
        ));
    }
};

/**
 * Reject multiple templates in batch
 * @route POST /api/templates/monetization/moderation/batch/reject
 * @access Admin
 */
const rejectModerationBatch = async (req, res) => {
    try {
        const { templateIds, moderationNotes } = req.body;

        if (!Array.isArray(templateIds) || templateIds.length === 0) {
            return res.status(400).json(formatErrorResponse(
                'VALIDATION_ERROR',
                'templateIds must be a non-empty array',
                'templateIds',
                templateIds
            ));
        }

        if (!moderationNotes || moderationNotes.trim().length === 0) {
            return res.status(400).json(formatErrorResponse(
                'VALIDATION_ERROR',
                'Moderation notes are required for batch rejection',
                'moderationNotes',
                moderationNotes
            ));
        }

        // Validate all template IDs
        const invalidIds = templateIds.filter(id => !isValidObjectId(id));
        if (invalidIds.length > 0) {
            return res.status(400).json(formatErrorResponse(
                'VALIDATION_ERROR',
                'Invalid template ID format in array',
                'templateIds',
                invalidIds
            ));
        }

        logger.info('Batch rejecting templates:', { 
            templateCount: templateIds.length, 
            moderationNotes 
        });

        const updateResult = await Template.updateMany(
            { _id: { $in: templateIds } },
            {
                moderationStatus: 'rejected',
                moderationNotes: moderationNotes,
                status: false, // Deactivate rejected templates
                updatedAt: new Date()
            }
        );

        logger.info(`Batch rejection completed: ${updateResult.modifiedCount} templates rejected`);

        res.json(formatSuccessResponse({
            templateIds: templateIds,
            rejectedCount: updateResult.modifiedCount,
            matchedCount: updateResult.matchedCount,
            moderationNotes: moderationNotes,
            message: `${updateResult.modifiedCount} templates rejected successfully`
        }, ['moderationStatus', 'moderationNotes', 'status']));

    } catch (error) {
        logger.error('Error during batch rejection:', error);
        res.status(500).json(formatErrorResponse(
            'SERVER_ERROR',
            'Internal server error during batch rejection',
            null,
            null,
            error.message
        ));
    }
};

module.exports = {
    getModerationInfo,
    updateModerationStatus,
    getModerationQueue,
    bulkModerationUpdate,
    approveModerationBatch,
    rejectModerationBatch
};
