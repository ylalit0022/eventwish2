const express = require('express');
const router = express.Router();
const { validateTemplateId, validateTemplateExists, validateCategorization } = require('../../../middleware/templateValidators');
const searchController = require('../../../controllers/templates/categorization/searchController');
const logger = require('../../../utils/logger');

/**
 * Search Routes
 * Advanced search and filtering operations for templates
 * Combines all categorization fields for comprehensive search
 */

/**
 * @route POST /api/templates/categorization/search
 * @desc Advanced template search with multiple filters
 * @access Public
 */
router.post('/', 
    searchController.advancedSearch
);

/**
 * @route POST /api/templates/categorization/search/by-text
 * @desc Text-based search across content and tags
 * @access Public
 */
router.post('/by-text', 
    searchController.textSearch
);

/**
 * @route POST /api/templates/categorization/search/by-category
 * @desc Search templates by category with filters
 * @access Public
 */
router.post('/by-category', 
    searchController.categorySearch
);

/**
 * @route POST /api/templates/categorization/search/by-creator
 * @desc Search templates by creator with filters
 * @access Public
 */
router.post('/by-creator', 
    searchController.creatorSearch
);

/**
 * @route POST /api/templates/categorization/search/by-tags
 * @desc Search templates by tags combination
 * @access Public
 */
router.post('/by-tags', 
    searchController.tagsSearch
);

/**
 * @route POST /api/templates/categorization/search/similar
 * @desc Find similar templates to a given template
 * @access Public
 */
router.post('/similar', 
    searchController.similarTemplatesSearch
);

/**
 * @route POST /api/templates/categorization/search/faceted
 * @desc Faceted search with aggregated results
 * @access Public
 */
router.post('/faceted', 
    searchController.facetedSearch
);

/**
 * @route GET /api/templates/categorization/search/suggestions/:query
 * @desc Get search suggestions for auto-complete
 * @access Public
 */
router.get('/suggestions/:query', 
    searchController.getSearchSuggestions
);

/**
 * @route POST /api/templates/categorization/search/filters
 * @desc Get available filter options for search
 * @access Public
 */
router.post('/filters', 
    searchController.getFilterOptions
);

/**
 * @route POST /api/templates/categorization/search/trending
 * @desc Search trending templates with categorization filters
 * @access Public
 */
router.post('/trending', 
    searchController.trendingSearch
);

/**
 * @route POST /api/templates/categorization/search/popular
 * @desc Search popular templates with categorization filters
 * @access Public
 */
router.post('/popular', 
    searchController.popularSearch
);

/**
 * @route POST /api/templates/categorization/search/recent
 * @desc Search recent templates with categorization filters
 * @access Public
 */
router.post('/recent', 
    searchController.recentSearch
);

/**
 * @route POST /api/templates/categorization/search/recommended
 * @desc Get recommended templates based on user preferences
 * @access Public
 */
router.post('/recommended', 
    searchController.recommendedSearch
);

/**
 * @route POST /api/templates/categorization/search/export
 * @desc Export search results
 * @access Public
 */
router.post('/export', 
    searchController.exportSearchResults
);

/**
 * @route GET /api/templates/categorization/search/stats/queries
 * @desc Get search query statistics
 * @access Public
 */
router.get('/stats/queries', 
    searchController.getSearchStats
);

/**
 * @route POST /api/templates/categorization/search/save
 * @desc Save search query for later use
 * @access Public
 */
router.post('/save', 
    searchController.saveSearchQuery
);

/**
 * @route GET /api/templates/categorization/search/saved/:userId
 * @desc Get saved search queries for user
 * @access Public
 */
router.get('/saved/:userId', 
    searchController.getSavedSearches
);

/**
 * @route DELETE /api/templates/categorization/search/saved/:searchId
 * @desc Delete saved search query
 * @access Public
 */
router.delete('/saved/:searchId', 
    searchController.deleteSavedSearch
);

/**
 * @route POST /api/templates/categorization/search/bulk
 * @desc Bulk search operations
 * @access Public
 */
router.post('/bulk', 
    searchController.bulkSearch
);

/**
 * @route POST /api/templates/categorization/search/index/rebuild
 * @desc Rebuild search index
 * @access Admin
 */
router.post('/index/rebuild', 
    searchController.rebuildSearchIndex
);

module.exports = router; 