const express = require('express');
const router = express.Router();
const { validateTemplateId, validateTemplateExists, validateCategorization } = require('../../../middleware/templateValidators');
const creatorsController = require('../../../controllers/templates/categorization/creatorsController');
const logger = require('../../../utils/logger');

/**
 * Creators Routes
 * CRUD operations for creator-related fields:
 * - creatorId (ObjectId, ref: 'User')
 * - creatorUid (String, Firebase UID)
 * - category (String)
 * - festivalTag (String)
 */

/**
 * @route GET /api/templates/categorization/creators/:templateId
 * @desc Get creator data for a template
 * @access Public
 */
router.get('/:templateId', 
    validateTemplateId, 
    validateTemplateExists, 
    creatorsController.getCreatorData
);

/**
 * @route PUT /api/templates/categorization/creators/:templateId
 * @desc Update creator data for a template
 * @access Public
 */
router.put('/:templateId', 
    validateTemplateId, 
    validateTemplateExists, 
    validateCategorization, 
    creatorsController.updateCreatorData
);

/**
 * @route DELETE /api/templates/categorization/creators/:templateId
 * @desc Clear creator data for a template
 * @access Public
 */
router.delete('/:templateId', 
    validateTemplateId, 
    validateTemplateExists, 
    creatorsController.clearCreatorData
);

/**
 * @route PUT /api/templates/categorization/creators/:templateId/creator-id
 * @desc Update creatorId field
 * @access Public
 */
router.put('/:templateId/creator-id', 
    validateTemplateId, 
    validateTemplateExists, 
    creatorsController.updateCreatorId
);

/**
 * @route PUT /api/templates/categorization/creators/:templateId/creator-uid
 * @desc Update creatorUid field
 * @access Public
 */
router.put('/:templateId/creator-uid', 
    validateTemplateId, 
    validateTemplateExists, 
    creatorsController.updateCreatorUid
);

/**
 * @route PUT /api/templates/categorization/creators/:templateId/category
 * @desc Update category field
 * @access Public
 */
router.put('/:templateId/category', 
    validateTemplateId, 
    validateTemplateExists, 
    creatorsController.updateCategory
);

/**
 * @route PUT /api/templates/categorization/creators/:templateId/festival-tag
 * @desc Update festivalTag field
 * @access Public
 */
router.put('/:templateId/festival-tag', 
    validateTemplateId, 
    validateTemplateExists, 
    creatorsController.updateFestivalTag
);

/**
 * @route GET /api/templates/categorization/creators/by-creator/:creatorId
 * @desc Get templates by creator (ObjectId)
 * @access Public
 */
router.get('/by-creator/:creatorId', 
    creatorsController.getTemplatesByCreator
);

/**
 * @route GET /api/templates/categorization/creators/by-creator-uid/:uid
 * @desc Get templates by creator (Firebase UID)
 * @access Public
 */
router.get('/by-creator-uid/:uid', 
    creatorsController.getTemplatesByCreatorUid
);

/**
 * @route GET /api/templates/categorization/creators/by-category/:category
 * @desc Get templates by category
 * @access Public
 */
router.get('/by-category/:category', 
    creatorsController.getTemplatesByCategory
);

/**
 * @route GET /api/templates/categorization/creators/by-festival/:festivalTag
 * @desc Get templates by festival tag
 * @access Public
 */
router.get('/by-festival/:festivalTag', 
    creatorsController.getTemplatesByFestival
);

/**
 * @route GET /api/templates/categorization/creators/stats/categories
 * @desc Get category statistics
 * @access Public
 */
router.get('/stats/categories', 
    creatorsController.getCategoryStats
);

/**
 * @route GET /api/templates/categorization/creators/stats/festivals
 * @desc Get festival tag statistics
 * @access Public
 */
router.get('/stats/festivals', 
    creatorsController.getFestivalStats
);

/**
 * @route GET /api/templates/categorization/creators/stats/creators
 * @desc Get creator statistics
 * @access Public
 */
router.get('/stats/creators', 
    creatorsController.getCreatorStats
);

/**
 * @route POST /api/templates/categorization/creators/batch/assign
 * @desc Batch assign creator for multiple templates
 * @access Public
 */
router.post('/batch/assign', 
    creatorsController.batchAssignCreator
);

module.exports = router; 