const express = require('express');
const router = express.Router();
const mongoose = require('mongoose');
const User = require('../../models/User');
const Template = require('../../models/Template');
const logger = require('../../utils/logger');

/**
 * @route   GET /api/users/health/template-interactions
 * @desc    Health check for template interaction endpoints
 * @access  Public
 */
router.get('/health/template-interactions', async (req, res) => {
    try {
        // Test database connection
        const dbStatus = mongoose.connection.readyState === 1 ? 'connected' : 'disconnected';
        
        // Test basic Template model access
        const templateCount = await Template.countDocuments().maxTimeMS(5000);
        
        // Test basic User model access
        const userCount = await User.countDocuments().maxTimeMS(5000);
        
        res.status(200).json({
            success: true,
            message: 'Template interaction endpoints are healthy',
            data: {
                database: dbStatus,
                templateCount: templateCount,
                userCount: userCount,
                timestamp: new Date().toISOString(),
                server: 'eventwish-backend'
            }
        });
    } catch (error) {
        logger.error('Template interaction health check failed:', error);
        res.status(503).json({
            success: false,
            message: 'Template interaction endpoints are unhealthy',
            error: error.message,
            timestamp: new Date().toISOString()
        });
    }
});

module.exports = router; 