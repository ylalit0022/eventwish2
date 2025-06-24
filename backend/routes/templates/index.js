const express = require('express');
const router = express.Router();
const logger = require('../../utils/logger');

// Import route modules
const coreRoutes = require('./core');
const monetizationRoutes = require('./monetization');
const metricsRoutes = require('./metrics');
const aiRoutes = require('./ai');
const categorizationRoutes = require('./categorization');
const visibilityRoutes = require('./visibility');
const customizationRoutes = require('./customization');
const legacyRoutes = require('./legacy');

/**
 * Template API Routes
 * Modular CRUD-based API endpoints organized by field types
 */

// Core template operations (content, media)
// /api/templates/core/*
router.use('/core', coreRoutes);

// Monetization operations (access control, moderation)
// /api/templates/monetization/*
router.use('/monetization', monetizationRoutes);

// Metrics operations (engagement, weekly, analytics, performance)
// /api/templates/metrics/*
router.use('/metrics', metricsRoutes);

// AI operations (generation, metadata, users)
// /api/templates/ai/*
router.use('/ai', aiRoutes);

// Categorization operations (creators, tags, relations, search)
// /api/templates/categorization/*
router.use('/categorization', categorizationRoutes);

// Visibility operations (ranking, boosting, blocking, type)
// /api/templates/visibility/*
router.use('/visibility', visibilityRoutes);

// Customization operations (options, permissions)
// /api/templates/customization/*
router.use('/customization', customizationRoutes);

// Legacy operations (compatibility, references, experiments)
// /api/templates/legacy/*
router.use('/legacy', legacyRoutes);

// Health check endpoint for templates module
router.get('/health', (req, res) => {
    res.json({
        success: true,
        message: 'Templates API is healthy',
        modules: {
            core: 'active',
            monetization: 'active',
            metrics: 'active',
            ai: 'active',
            categorization: 'active',
            visibility: 'active',
            customization: 'active',
            legacy: 'active'
        },
        implementedEndpoints: {
            core: 15,
            monetization: 18,
            metrics: 46,
            ai: 37,
            categorization: 59,
            visibility: 25,
            customization: 14,
            legacy: 30,
            total: 244
        },
        timestamp: new Date().toISOString()
    });
});

// Log all template API requests
router.use('*', (req, res, next) => {
    logger.info(`Template API Request: ${req.method} ${req.originalUrl}`, {
        body: req.body,
        params: req.params,
        query: req.query
    });
    next();
});

module.exports = router; 