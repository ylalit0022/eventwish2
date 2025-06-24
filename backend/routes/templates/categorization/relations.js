const express = require('express');
const router = express.Router();
const { validateTemplateId, validateTemplateExists, validateCategorization } = require('../../../middleware/templateValidators');
const relationsController = require('../../../controllers/templates/categorization/relationsController');
const logger = require('../../../utils/logger');

/**
 * Relations Routes
 * CRUD operations for template relationship fields:
 * - relatedTemplates (Array of ObjectIds)
 * - similarTemplates (Array of ObjectIds)
 * - templateVariants (Array of ObjectIds)
 */

/**
 * @route GET /api/templates/categorization/relations/:templateId
 * @desc Get all relations for a template
 * @access Public
 */
router.get('/:templateId', 
    validateTemplateId, 
    validateTemplateExists, 
    relationsController.getAllRelations
);

/**
 * @route PUT /api/templates/categorization/relations/:templateId
 * @desc Update all relations for a template
 * @access Public
 */
router.put('/:templateId', 
    validateTemplateId, 
    validateTemplateExists, 
    validateCategorization, 
    relationsController.updateAllRelations
);

/**
 * @route DELETE /api/templates/categorization/relations/:templateId
 * @desc Clear all relations for a template
 * @access Public
 */
router.delete('/:templateId', 
    validateTemplateId, 
    validateTemplateExists, 
    relationsController.clearAllRelations
);

/**
 * @route GET /api/templates/categorization/relations/:templateId/related
 * @desc Get related templates
 * @access Public
 */
router.get('/:templateId/related', 
    validateTemplateId, 
    validateTemplateExists, 
    relationsController.getRelatedTemplates
);

/**
 * @route PUT /api/templates/categorization/relations/:templateId/related
 * @desc Update related templates
 * @access Public
 */
router.put('/:templateId/related', 
    validateTemplateId, 
    validateTemplateExists, 
    relationsController.updateRelatedTemplates
);

/**
 * @route POST /api/templates/categorization/relations/:templateId/related/add
 * @desc Add related templates
 * @access Public
 */
router.post('/:templateId/related/add', 
    validateTemplateId, 
    validateTemplateExists, 
    relationsController.addRelatedTemplates
);

/**
 * @route POST /api/templates/categorization/relations/:templateId/related/remove
 * @desc Remove related templates
 * @access Public
 */
router.post('/:templateId/related/remove', 
    validateTemplateId, 
    validateTemplateExists, 
    relationsController.removeRelatedTemplates
);

/**
 * @route GET /api/templates/categorization/relations/:templateId/similar
 * @desc Get similar templates
 * @access Public
 */
router.get('/:templateId/similar', 
    validateTemplateId, 
    validateTemplateExists, 
    relationsController.getSimilarTemplates
);

/**
 * @route PUT /api/templates/categorization/relations/:templateId/similar
 * @desc Update similar templates
 * @access Public
 */
router.put('/:templateId/similar', 
    validateTemplateId, 
    validateTemplateExists, 
    relationsController.updateSimilarTemplates
);

/**
 * @route POST /api/templates/categorization/relations/:templateId/similar/add
 * @desc Add similar templates
 * @access Public
 */
router.post('/:templateId/similar/add', 
    validateTemplateId, 
    validateTemplateExists, 
    relationsController.addSimilarTemplates
);

/**
 * @route POST /api/templates/categorization/relations/:templateId/similar/remove
 * @desc Remove similar templates
 * @access Public
 */
router.post('/:templateId/similar/remove', 
    validateTemplateId, 
    validateTemplateExists, 
    relationsController.removeSimilarTemplates
);

/**
 * @route GET /api/templates/categorization/relations/:templateId/variants
 * @desc Get template variants
 * @access Public
 */
router.get('/:templateId/variants', 
    validateTemplateId, 
    validateTemplateExists, 
    relationsController.getTemplateVariants
);

/**
 * @route PUT /api/templates/categorization/relations/:templateId/variants
 * @desc Update template variants
 * @access Public
 */
router.put('/:templateId/variants', 
    validateTemplateId, 
    validateTemplateExists, 
    relationsController.updateTemplateVariants
);

/**
 * @route POST /api/templates/categorization/relations/:templateId/variants/add
 * @desc Add template variants
 * @access Public
 */
router.post('/:templateId/variants/add', 
    validateTemplateId, 
    validateTemplateExists, 
    relationsController.addTemplateVariants
);

/**
 * @route POST /api/templates/categorization/relations/:templateId/variants/remove
 * @desc Remove template variants
 * @access Public
 */
router.post('/:templateId/variants/remove', 
    validateTemplateId, 
    validateTemplateExists, 
    relationsController.removeTemplateVariants
);

/**
 * @route POST /api/templates/categorization/relations/:templateId/auto-relate
 * @desc Auto-discover and set related templates based on similarity
 * @access Public
 */
router.post('/:templateId/auto-relate', 
    validateTemplateId, 
    validateTemplateExists, 
    relationsController.autoDiscoverRelated
);

/**
 * @route POST /api/templates/categorization/relations/:templateId/auto-similar
 * @desc Auto-discover and set similar templates based on content analysis
 * @access Public
 */
router.post('/:templateId/auto-similar', 
    validateTemplateId, 
    validateTemplateExists, 
    relationsController.autoDiscoverSimilar
);

/**
 * @route GET /api/templates/categorization/relations/:templateId/graph
 * @desc Get template relationship graph
 * @access Public
 */
router.get('/:templateId/graph', 
    validateTemplateId, 
    validateTemplateExists, 
    relationsController.getRelationshipGraph
);

/**
 * @route POST /api/templates/categorization/relations/batch/relate
 * @desc Batch create relationships between templates
 * @access Public
 */
router.post('/batch/relate', 
    relationsController.batchCreateRelations
);

/**
 * @route POST /api/templates/categorization/relations/batch/unrelate
 * @desc Batch remove relationships between templates
 * @access Public
 */
router.post('/batch/unrelate', 
    relationsController.batchRemoveRelations
);

/**
 * @route GET /api/templates/categorization/relations/stats/overview
 * @desc Get relations statistics overview
 * @access Public
 */
router.get('/stats/overview', 
    relationsController.getRelationsStats
);

/**
 * @route POST /api/templates/categorization/relations/validate/:templateId
 * @desc Validate template relations integrity
 * @access Public
 */
router.post('/validate/:templateId', 
    validateTemplateId, 
    validateTemplateExists, 
    relationsController.validateRelations
);

module.exports = router; 