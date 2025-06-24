const express = require('express');
const router = express.Router();
const { 
    addUserToIgnoreList,
    removeUserFromIgnoreList,
    getIgnoredUsers,
    isUserIgnoring,
    bulkAddIgnoredUsers,
    bulkRemoveIgnoredUsers,
    clearIgnoreList
} = require('../../../controllers/templates/visibility/blockingController');
const { validateTemplateId, validateUserId, validateUserIds } = require('../../../utils/templateValidators');
const { verifyFirebaseToken } = require('../../../middleware/auth');

/**
 * @route POST /api/templates/visibility/blocking/:id/users/:userId
 * @desc Add user to template's ignored users list
 * @access Private
 */
router.post('/:id/users/:userId',
    verifyFirebaseToken,
    validateTemplateId,
    validateUserId,
    addUserToIgnoreList
);

/**
 * @route DELETE /api/templates/visibility/blocking/:id/users/:userId
 * @desc Remove user from template's ignored users list
 * @access Private
 */
router.delete('/:id/users/:userId',
    verifyFirebaseToken,
    validateTemplateId,
    validateUserId,
    removeUserFromIgnoreList
);

/**
 * @route GET /api/templates/visibility/blocking/:id/users
 * @desc Get list of users who have ignored this template
 * @access Private
 */
router.get('/:id/users',
    verifyFirebaseToken,
    validateTemplateId,
    getIgnoredUsers
);

/**
 * @route GET /api/templates/visibility/blocking/:id/users/:userId/status
 * @desc Check if a specific user is ignoring this template
 * @access Private
 */
router.get('/:id/users/:userId/status',
    verifyFirebaseToken,
    validateTemplateId,
    validateUserId,
    isUserIgnoring
);

/**
 * @route POST /api/templates/visibility/blocking/:id/users/bulk
 * @desc Add multiple users to ignored list
 * @access Private
 */
router.post('/:id/users/bulk',
    verifyFirebaseToken,
    validateTemplateId,
    validateUserIds,
    bulkAddIgnoredUsers
);

/**
 * @route DELETE /api/templates/visibility/blocking/:id/users/bulk
 * @desc Remove multiple users from ignored list
 * @access Private
 */
router.delete('/:id/users/bulk',
    verifyFirebaseToken,
    validateTemplateId,
    validateUserIds,
    bulkRemoveIgnoredUsers
);

/**
 * @route DELETE /api/templates/visibility/blocking/:id/users/all
 * @desc Clear all ignored users for a template
 * @access Private
 */
router.delete('/:id/users/all',
    verifyFirebaseToken,
    validateTemplateId,
    clearIgnoreList
);

module.exports = router; 