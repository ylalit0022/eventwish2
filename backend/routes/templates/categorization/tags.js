const express = require('express');
const router = express.Router();
const { validateTemplateId, validateTemplateExists, validateCategorization } = require('../../../middleware/templateValidators');
const tagsController = require('../../../controllers/templates/categorization/tagsController');
const logger = require('../../../utils/logger');

/**
 * Tags Routes
 * CRUD operations for tag-related fields:
 * - tags (Array of Strings)
 * - styleTags (Array of Strings)
 * - searchKeywords (Array of Strings)
 */

/**
 * @route GET /api/templates/categorization/tags/:templateId
 * @desc Get all tags for a template
 * @access Public
 */
router.get('/:templateId', 
    validateTemplateId, 
    validateTemplateExists, 
    tagsController.getAllTags
);

/**
 * @route PUT /api/templates/categorization/tags/:templateId
 * @desc Update all tags for a template
 * @access Public
 */
router.put('/:templateId', 
    validateTemplateId, 
    validateTemplateExists, 
    validateCategorization, 
    tagsController.updateAllTags
);

/**
 * @route DELETE /api/templates/categorization/tags/:templateId
 * @desc Clear all tags for a template
 * @access Public
 */
router.delete('/:templateId', 
    validateTemplateId, 
    validateTemplateExists, 
    tagsController.clearAllTags
);

/**
 * @route GET /api/templates/categorization/tags/:templateId/general
 * @desc Get general tags for a template
 * @access Public
 */
router.get('/:templateId/general', 
    validateTemplateId, 
    validateTemplateExists, 
    tagsController.getGeneralTags
);

/**
 * @route PUT /api/templates/categorization/tags/:templateId/general
 * @desc Update general tags for a template
 * @access Public
 */
router.put('/:templateId/general', 
    validateTemplateId, 
    validateTemplateExists, 
    tagsController.updateGeneralTags
);

/**
 * @route POST /api/templates/categorization/tags/:templateId/general/add
 * @desc Add general tags to a template
 * @access Public
 */
router.post('/:templateId/general/add', 
    validateTemplateId, 
    validateTemplateExists, 
    tagsController.addGeneralTags
);

/**
 * @route POST /api/templates/categorization/tags/:templateId/general/remove
 * @desc Remove general tags from a template
 * @access Public
 */
router.post('/:templateId/general/remove', 
    validateTemplateId, 
    validateTemplateExists, 
    tagsController.removeGeneralTags
);

/**
 * @route GET /api/templates/categorization/tags/:templateId/style
 * @desc Get style tags for a template
 * @access Public
 */
router.get('/:templateId/style', 
    validateTemplateId, 
    validateTemplateExists, 
    tagsController.getStyleTags
);

/**
 * @route PUT /api/templates/categorization/tags/:templateId/style
 * @desc Update style tags for a template
 * @access Public
 */
router.put('/:templateId/style', 
    validateTemplateId, 
    validateTemplateExists, 
    tagsController.updateStyleTags
);

/**
 * @route POST /api/templates/categorization/tags/:templateId/style/add
 * @desc Add style tags to a template
 * @access Public
 */
router.post('/:templateId/style/add', 
    validateTemplateId, 
    validateTemplateExists, 
    tagsController.addStyleTags
);

/**
 * @route POST /api/templates/categorization/tags/:templateId/style/remove
 * @desc Remove style tags from a template
 * @access Public
 */
router.post('/:templateId/style/remove', 
    validateTemplateId, 
    validateTemplateExists, 
    tagsController.removeStyleTags
);

/**
 * @route GET /api/templates/categorization/tags/:templateId/keywords
 * @desc Get search keywords for a template
 * @access Public
 */
router.get('/:templateId/keywords', 
    validateTemplateId, 
    validateTemplateExists, 
    tagsController.getSearchKeywords
);

/**
 * @route PUT /api/templates/categorization/tags/:templateId/keywords
 * @desc Update search keywords for a template
 * @access Public
 */
router.put('/:templateId/keywords', 
    validateTemplateId, 
    validateTemplateExists, 
    tagsController.updateSearchKeywords
);

/**
 * @route POST /api/templates/categorization/tags/:templateId/keywords/add
 * @desc Add search keywords to a template
 * @access Public
 */
router.post('/:templateId/keywords/add', 
    validateTemplateId, 
    validateTemplateExists, 
    tagsController.addSearchKeywords
);

/**
 * @route POST /api/templates/categorization/tags/:templateId/keywords/remove
 * @desc Remove search keywords from a template
 * @access Public
 */
router.post('/:templateId/keywords/remove', 
    validateTemplateId, 
    validateTemplateExists, 
    tagsController.removeSearchKeywords
);

/**
 * @route GET /api/templates/categorization/tags/by-tag/:tag
 * @desc Get templates by general tag
 * @access Public
 */
router.get('/by-tag/:tag', 
    tagsController.getTemplatesByTag
);

/**
 * @route GET /api/templates/categorization/tags/by-style-tag/:styleTag
 * @desc Get templates by style tag
 * @access Public
 */
router.get('/by-style-tag/:styleTag', 
    tagsController.getTemplatesByStyleTag
);

/**
 * @route GET /api/templates/categorization/tags/by-keyword/:keyword
 * @desc Get templates by search keyword
 * @access Public
 */
router.get('/by-keyword/:keyword', 
    tagsController.getTemplatesByKeyword
);

/**
 * @route GET /api/templates/categorization/tags/stats/popular
 * @desc Get popular tags statistics
 * @access Public
 */
router.get('/stats/popular', 
    tagsController.getPopularTagsStats
);

/**
 * @route GET /api/templates/categorization/tags/stats/style-tags
 * @desc Get style tags statistics
 * @access Public
 */
router.get('/stats/style-tags', 
    tagsController.getStyleTagsStats
);

/**
 * @route GET /api/templates/categorization/tags/stats/keywords
 * @desc Get search keywords statistics
 * @access Public
 */
router.get('/stats/keywords', 
    tagsController.getKeywordsStats
);

/**
 * @route POST /api/templates/categorization/tags/batch/add
 * @desc Batch add tags to multiple templates
 * @access Public
 */
router.post('/batch/add', 
    tagsController.batchAddTags
);

/**
 * @route POST /api/templates/categorization/tags/batch/remove
 * @desc Batch remove tags from multiple templates
 * @access Public
 */
router.post('/batch/remove', 
    tagsController.batchRemoveTags
);

module.exports = router; 