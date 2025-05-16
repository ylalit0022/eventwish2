const express = require('express');
const router = express.Router();
const { 
    getTemplates, 
    getTemplatesByCategory, 
    getTemplateById 
} = require('../controllers/templateController');
const recommendationService = require('../services/recommendationService');
const logger = require('../utils/logger');
const mongoose = require('mongoose');

// Middleware to validate ObjectId
const validateObjectId = (req, res, next) => {
    const id = req.params.id;
    
    // Skip validation if a special ID format is used
    if (!id) {
        return res.status(400).json({
            success: false,
            message: 'Template ID is required'
        });
    }
    
    if (!mongoose.Types.ObjectId.isValid(id)) {
        logger.warn(`Invalid template ID format: ${id}`);
        return res.status(400).json({
            success: false,
            message: 'Invalid template ID format'
        });
    }
    
    next();
};

// Log middleware for debugging
const logApiRequest = (req, res, next) => {
    logger.debug(`[Template API] ${req.method} ${req.originalUrl}`);
    next();
};

// Get all templates with pagination
router.get('/', logApiRequest, getTemplates);

// Get templates by category
router.get('/category/:category', logApiRequest, getTemplatesByCategory);

// Get personalized recommendations based on user history
router.get('/recommendations/:deviceId', logApiRequest, async (req, res) => {
    try {
        const { deviceId } = req.params;
        const limit = parseInt(req.query.limit) || 10;
        
        if (!deviceId) {
            return res.status(400).json({
                success: false,
                message: 'Device ID is required'
            });
        }

        logger.info(`Getting personalized recommendations for device: ${deviceId}`);
        const recommendations = await recommendationService.getRecommendationsForUser(deviceId, limit);
        
        // Ensure ALL template IDs are strings in recommendations
        if (recommendations && Array.isArray(recommendations)) {
            recommendations.forEach(template => {
                if (template._id) {
                    template.id = template._id.toString();
                }
            });
        }
        
        res.json({
            success: true,
            recommendations,
            isDefault: recommendations.isDefault || false
        });
    } catch (error) {
        logger.error(`Error getting personalized recommendations: ${error.message}`);
        logger.error(error.stack);
        res.status(500).json({
            success: false,
            message: 'Error generating recommendations',
            error: error.message
        });
    }
});

// Get template by ID - validate ID first
router.get('/:id', [logApiRequest, validateObjectId], getTemplateById);

module.exports = router;
