const express = require('express');
const router = express.Router();

// Import monetization route modules
const accessRoutes = require('./access');
const moderationRoutes = require('./moderation');

/**
 * Monetization Routes
 * Handles template premium access control and moderation workflows
 */

// Mount access control routes
// /api/templates/monetization/access/*
router.use('/access', accessRoutes);

// Mount moderation routes  
// /api/templates/monetization/moderation/*
router.use('/moderation', moderationRoutes);

/**
 * @route GET /api/templates/monetization/health
 * @desc Health check for monetization endpoints
 * @access Public
 */
router.get('/health', (req, res) => {
    res.json({
        success: true,
        message: 'Monetization endpoints are operational',
        endpoints: {
            access: {
                base: '/api/templates/monetization/access',
                routes: [
                    'GET /:templateId - Get access settings',
                    'PUT /:templateId - Update access settings', 
                    'POST /:templateId/toggle-premium - Toggle premium status',
                    'POST /:templateId/validate - Validate user access',
                    'GET /:templateId/status - Get access status',
                    'POST /bulk-update - Bulk update access',
                    'GET /premium/list - List premium templates',
                    'GET /free/list - List free templates'
                ]
            },
            moderation: {
                base: '/api/templates/monetization/moderation',
                routes: [
                    'GET /:templateId - Get moderation info',
                    'PUT /:templateId - Update moderation status',
                    'POST /:templateId/approve - Approve template',
                    'POST /:templateId/reject - Reject template',
                    'GET /queue - Get moderation queue',
                    'POST /bulk-update - Bulk update moderation',
                    'POST /batch/approve - Batch approve',
                    'POST /batch/reject - Batch reject',
                    'GET /stats - Moderation statistics'
                ]
            }
        },
        timestamp: new Date().toISOString()
    });
});

module.exports = router; 