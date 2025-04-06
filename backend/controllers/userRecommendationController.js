const User = require('../models/User');
const logger = require('../utils/logger');
const recommendationService = require('../services/recommendationService');

/**
 * Get personalized template recommendations for a user
 * @param {Object} req - Express request object
 * @param {Object} res - Express response object
 */
exports.getUserRecommendations = async (req, res) => {
    try {
        const { deviceId } = req.params;
        const limit = parseInt(req.query.limit) || 10;
        
        if (!deviceId) {
            return res.status(400).json({
                success: false,
                message: 'Device ID is required'
            });
        }
        
        // Find user by deviceId to ensure they exist
        const user = await User.findOne({ deviceId });
        
        if (!user) {
            logger.warn(`Recommendations requested for non-existent user: ${deviceId}`);
            return res.status(404).json({
                success: false,
                message: 'User not found'
            });
        }
        
        // Get recommendations using the recommendation service
        const recommendations = await recommendationService.getRecommendationsForUser(deviceId, limit);
        
        res.status(200).json({
            success: true,
            recommendations
        });
    } catch (error) {
        logger.error(`Recommendations error: ${error.message}`);
        res.status(500).json({
            success: false,
            message: 'Server error generating recommendations',
            error: error.message
        });
    }
}; 