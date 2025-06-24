const express = require('express');
const router = express.Router();
const { validateTemplateId, validateTemplateExists, validateAIMetadata } = require('../../../middleware/templateValidators');
const metadataController = require('../../../controllers/templates/ai/metadataController');
const logger = require('../../../utils/logger');

/**
 * AI Metadata Routes
 * CRUD operations for AI metadata fields:
 * - generationMetadata (Object: resolution, seed, guidanceScale, timestamp)
 * - aiPrompt (String) - detailed prompt management
 * - aiModel (String) - model tracking and analytics
 * - aiStyle (String) - style categorization and filtering
 */

/**
 * @route GET /api/templates/ai/metadata/:templateId
 * @desc Get complete AI metadata for a template
 * @access Public
 */
router.get('/:templateId', 
    validateTemplateId, 
    validateTemplateExists, 
    metadataController.getMetadata
);

/**
 * @route PUT /api/templates/ai/metadata/:templateId
 * @desc Update AI metadata for a template
 * @access Public
 */
router.put('/:templateId', 
    validateTemplateId, 
    validateTemplateExists, 
    validateAIMetadata, 
    metadataController.updateMetadata
);

/**
 * @route DELETE /api/templates/ai/metadata/:templateId
 * @desc Clear AI metadata for a template
 * @access Public
 */
router.delete('/:templateId', 
    validateTemplateId, 
    validateTemplateExists, 
    metadataController.clearMetadata
);

/**
 * @route PUT /api/templates/ai/metadata/:templateId/resolution
 * @desc Update generation resolution
 * @access Public
 */
router.put('/:templateId/resolution', 
    validateTemplateId, 
    validateTemplateExists, 
    metadataController.updateResolution
);

/**
 * @route PUT /api/templates/ai/metadata/:templateId/seed
 * @desc Update generation seed
 * @access Public
 */
router.put('/:templateId/seed', 
    validateTemplateId, 
    validateTemplateExists, 
    metadataController.updateSeed
);

/**
 * @route PUT /api/templates/ai/metadata/:templateId/guidance-scale
 * @desc Update guidance scale
 * @access Public
 */
router.put('/:templateId/guidance-scale', 
    validateTemplateId, 
    validateTemplateExists, 
    metadataController.updateGuidanceScale
);

/**
 * @route GET /api/templates/ai/metadata/resolution/:resolution
 * @desc Get templates by resolution
 * @access Public
 */
router.get('/resolution/:resolution', 
    metadataController.getTemplatesByResolution
);

/**
 * @route GET /api/templates/ai/metadata/seed/:seed
 * @desc Get templates by seed value
 * @access Public
 */
router.get('/seed/:seed', 
    metadataController.getTemplatesBySeed
);

/**
 * @route GET /api/templates/ai/metadata/guidance-scale/:scale
 * @desc Get templates by guidance scale range
 * @access Public
 */
router.get('/guidance-scale/:scale', 
    metadataController.getTemplatesByGuidanceScale
);

/**
 * @route POST /api/templates/ai/metadata/search
 * @desc Search templates by metadata criteria
 * @access Public
 */
router.post('/search', 
    metadataController.searchByMetadata
);

/**
 * @route GET /api/templates/ai/metadata/stats/resolutions
 * @desc Get resolution usage statistics
 * @access Public
 */
router.get('/stats/resolutions', 
    metadataController.getResolutionStats
);

/**
 * @route GET /api/templates/ai/metadata/stats/guidance-scales
 * @desc Get guidance scale distribution statistics
 * @access Public
 */
router.get('/stats/guidance-scales', 
    metadataController.getGuidanceScaleStats
);

/**
 * @route POST /api/templates/ai/metadata/batch/update
 * @desc Batch update metadata for multiple templates
 * @access Public
 */
router.post('/batch/update', 
    metadataController.batchUpdateMetadata
);

module.exports = router; 