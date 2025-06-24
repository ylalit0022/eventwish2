const Template = require('../../../models/Template');
const logger = require('../../../utils/logger');
const { handleAsyncOperation, createSuccessResponse, createErrorResponse } = require('../../../utils/templateHelpers');

/**
 * Add user to template's ignored users list
 */
const addUserToIgnoreList = async (req, res) => {
    await handleAsyncOperation(res, async () => {
        const { id, userId } = req.params;

        const template = await Template.findByIdAndUpdate(
            id,
            { 
                $addToSet: { ignoredByUsers: userId },
                updatedAt: new Date()
            },
            { new: true, runValidators: true }
        );

        if (!template) {
            return createErrorResponse('Template not found', 'TEMPLATE_NOT_FOUND', 'id', id);
        }

        logger.info(`User ${userId} added to ignore list for template ${id}`);

        return createSuccessResponse({
            template: {
                _id: template._id,
                ignoredByUsers: template.ignoredByUsers,
                ignoredCount: template.ignoredByUsers ? template.ignoredByUsers.length : 0
            },
            modified: ['ignoredByUsers']
        });
    });
};

/**
 * Remove user from template's ignored users list
 */
const removeUserFromIgnoreList = async (req, res) => {
    await handleAsyncOperation(res, async () => {
        const { id, userId } = req.params;

        const template = await Template.findByIdAndUpdate(
            id,
            { 
                $pull: { ignoredByUsers: userId },
                updatedAt: new Date()
            },
            { new: true, runValidators: true }
        );

        if (!template) {
            return createErrorResponse('Template not found', 'TEMPLATE_NOT_FOUND', 'id', id);
        }

        logger.info(`User ${userId} removed from ignore list for template ${id}`);

        return createSuccessResponse({
            template: {
                _id: template._id,
                ignoredByUsers: template.ignoredByUsers,
                ignoredCount: template.ignoredByUsers ? template.ignoredByUsers.length : 0
            },
            modified: ['ignoredByUsers']
        });
    });
};

/**
 * Get list of users who have ignored this template
 */
const getIgnoredUsers = async (req, res) => {
    await handleAsyncOperation(res, async () => {
        const { id } = req.params;
        const { page = 1, limit = 20 } = req.query;
        const skip = (page - 1) * limit;

        const template = await Template.findById(id).select('ignoredByUsers title');

        if (!template) {
            return createErrorResponse('Template not found', 'TEMPLATE_NOT_FOUND', 'id', id);
        }

        const ignoredUsers = template.ignoredByUsers || [];
        const paginatedUsers = ignoredUsers.slice(skip, skip + parseInt(limit));

        return createSuccessResponse({
            template: {
                _id: template._id,
                title: template.title
            },
            ignoredUsers: paginatedUsers,
            pagination: {
                page: parseInt(page),
                limit: parseInt(limit),
                total: ignoredUsers.length,
                pages: Math.ceil(ignoredUsers.length / limit)
            }
        });
    });
};

/**
 * Check if a specific user is ignoring this template
 */
const isUserIgnoring = async (req, res) => {
    await handleAsyncOperation(res, async () => {
        const { id, userId } = req.params;

        const template = await Template.findById(id).select('ignoredByUsers');

        if (!template) {
            return createErrorResponse('Template not found', 'TEMPLATE_NOT_FOUND', 'id', id);
        }

        const isIgnoring = template.ignoredByUsers && template.ignoredByUsers.includes(userId);

        return createSuccessResponse({
            templateId: id,
            userId,
            isIgnoring,
            ignoredCount: template.ignoredByUsers ? template.ignoredByUsers.length : 0
        });
    });
};

/**
 * Add multiple users to ignored list
 */
const bulkAddIgnoredUsers = async (req, res) => {
    await handleAsyncOperation(res, async () => {
        const { id } = req.params;
        const { userIds } = req.body;

        if (!Array.isArray(userIds) || userIds.length === 0) {
            return createErrorResponse('User IDs array is required', 'VALIDATION_ERROR', 'userIds', userIds);
        }

        const template = await Template.findByIdAndUpdate(
            id,
            { 
                $addToSet: { ignoredByUsers: { $each: userIds } },
                updatedAt: new Date()
            },
            { new: true, runValidators: true }
        );

        if (!template) {
            return createErrorResponse('Template not found', 'TEMPLATE_NOT_FOUND', 'id', id);
        }

        logger.info(`${userIds.length} users added to ignore list for template ${id}`);

        return createSuccessResponse({
            template: {
                _id: template._id,
                ignoredByUsers: template.ignoredByUsers,
                ignoredCount: template.ignoredByUsers ? template.ignoredByUsers.length : 0
            },
            addedUsers: userIds,
            modified: ['ignoredByUsers']
        });
    });
};

/**
 * Remove multiple users from ignored list
 */
const bulkRemoveIgnoredUsers = async (req, res) => {
    await handleAsyncOperation(res, async () => {
        const { id } = req.params;
        const { userIds } = req.body;

        if (!Array.isArray(userIds) || userIds.length === 0) {
            return createErrorResponse('User IDs array is required', 'VALIDATION_ERROR', 'userIds', userIds);
        }

        const template = await Template.findByIdAndUpdate(
            id,
            { 
                $pullAll: { ignoredByUsers: userIds },
                updatedAt: new Date()
            },
            { new: true, runValidators: true }
        );

        if (!template) {
            return createErrorResponse('Template not found', 'TEMPLATE_NOT_FOUND', 'id', id);
        }

        logger.info(`${userIds.length} users removed from ignore list for template ${id}`);

        return createSuccessResponse({
            template: {
                _id: template._id,
                ignoredByUsers: template.ignoredByUsers,
                ignoredCount: template.ignoredByUsers ? template.ignoredByUsers.length : 0
            },
            removedUsers: userIds,
            modified: ['ignoredByUsers']
        });
    });
};

/**
 * Clear all ignored users for a template
 */
const clearIgnoreList = async (req, res) => {
    await handleAsyncOperation(res, async () => {
        const { id } = req.params;

        const template = await Template.findById(id).select('ignoredByUsers');
        if (!template) {
            return createErrorResponse('Template not found', 'TEMPLATE_NOT_FOUND', 'id', id);
        }

        const previousCount = template.ignoredByUsers ? template.ignoredByUsers.length : 0;

        const updatedTemplate = await Template.findByIdAndUpdate(
            id,
            { 
                $unset: { ignoredByUsers: 1 },
                updatedAt: new Date()
            },
            { new: true, runValidators: true }
        );

        logger.info(`Cleared ${previousCount} users from ignore list for template ${id}`);

        return createSuccessResponse({
            template: {
                _id: updatedTemplate._id,
                ignoredByUsers: [],
                ignoredCount: 0
            },
            clearedCount: previousCount,
            modified: ['ignoredByUsers']
        });
    });
};

module.exports = {
    addUserToIgnoreList,
    removeUserFromIgnoreList,
    getIgnoredUsers,
    isUserIgnoring,
    bulkAddIgnoredUsers,
    bulkRemoveIgnoredUsers,
    clearIgnoreList
}; 