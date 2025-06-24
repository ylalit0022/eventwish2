const express = require('express');
const router = express.Router();
const { validateTemplateId, validateTemplateExists, validateAIMetadata } = require('../../../middleware/templateValidators');
const generationController = require('../../../controllers/templates/ai/generationController');
const logger = require('../../../utils/logger');

/**
 * AI Generation Routes
 * CRUD operations for AI generation fields:
 * - isAIGenerated (Boolean)
 * - aiPrompt (String)
 * - aiModel (String)
 * - aiStyle (String)
 * - aiGenerationStage (String, enum: 'initial', 'on_edit', 'variation')
 * - generationMetadata (Object with resolution, seed, guidanceScale, timestamp)
 */

/**
 * @route GET /api/templates/ai/generation/:templateId
 * @desc Get AI generation data for a template
 * @access Public
 */
router.get('/:templateId', 
    validateTemplateId, 
    validateTemplateExists, 
    generationController.getGenerationData
);

/**
 * @route PUT /api/templates/ai/generation/:templateId
 * @desc Update AI generation data for a template
 * @access Public
 */
router.put('/:templateId', 
    validateTemplateId, 
    validateTemplateExists, 
    validateAIMetadata, 
    generationController.updateGenerationData
);

/**
 * @route DELETE /api/templates/ai/generation/:templateId
 * @desc Clear AI generation data for a template
 * @access Public
 */
router.delete('/:templateId', 
    validateTemplateId, 
    validateTemplateExists, 
    generationController.clearGenerationData
);

/**
 * @route POST /api/templates/ai/generation/:templateId/validate
 * @desc Validate AI generation data without saving
 * @access Public
 */
router.post('/:templateId/validate', 
    validateTemplateId, 
    validateAIMetadata, 
    generationController.validateGenerationData
);

/**
 * @route PUT /api/templates/ai/generation/:templateId/stage
 * @desc Update AI generation stage
 * @access Public
 */
router.put('/:templateId/stage', 
    validateTemplateId, 
    validateTemplateExists, 
    generationController.updateGenerationStage
);

/**
 * @route GET /api/templates/ai/generation/stage/:stage
 * @desc Get templates by AI generation stage
 * @access Public
 */
router.get('/stage/:stage', 
    generationController.getTemplatesByStage
);

/**
 * @route GET /api/templates/ai/generation/model/:model
 * @desc Get templates by AI model
 * @access Public
 */
router.get('/model/:model', 
    generationController.getTemplatesByModel
);

/**
 * @route GET /api/templates/ai/generation/style/:style
 * @desc Get templates by AI style
 * @access Public
 */
router.get('/style/:style', 
    generationController.getTemplatesByStyle
);

/**
 * @route POST /api/templates/ai/generation/batch/update
 * @desc Batch update AI generation data for multiple templates
 * @access Public
 */
router.post('/batch/update', 
    validateAIMetadata, 
    generationController.batchUpdateGeneration
);

/**
 * @route GET /api/templates/ai/generation/stats/overview
 * @desc Get AI generation statistics overview
 * @access Public
 */
router.get('/stats/overview', 
    generationController.getGenerationStats
);

/**
 * @route POST /api/templates/ai/generation/:templateId/metadata
 * @desc Update generation metadata for a template
 * @access Public
 */
router.post('/:templateId/metadata', 
    validateTemplateId, 
    validateTemplateExists, 
    generationController.updateGenerationMetadata
);

/**
 * @route GET /api/templates/ai/generation/:templateId/similar
 * @desc Get similar AI-generated templates based on prompt/style
 * @access Public
 */
router.get('/:templateId/similar', 
    validateTemplateId, 
    validateTemplateExists, 
    generationController.getSimilarTemplates
);

module.exports = router; 