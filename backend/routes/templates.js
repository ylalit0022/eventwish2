const express = require('express');
const router = express.Router();
const { 
    getTemplates, 
    getTemplatesByCategory, 
    getTemplateById 
} = require('../controllers/templateController');
const recommendationService = require('../services/recommendationService');
const logger = require('../utils/logger');

// Get all templates with pagination
router.get('/', getTemplates);

// Get templates by category
router.get('/category/:category', getTemplatesByCategory);

// Get personalized recommendations based on user history
router.get('/recommendations/:deviceId', async (req, res) => {
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
        
        res.json({
            success: true,
            recommendations,
            isDefault: recommendations.isDefault || false
        });
    } catch (error) {
        logger.error(`Error getting personalized recommendations: ${error.message}`);
        res.status(500).json({
            success: false,
            message: 'Error generating recommendations',
            error: error.message
        });
    }
});

// Get template by ID
router.get('/:id', getTemplateById);

module.exports = router;
