const express = require('express');
const router = express.Router();

// Import core route modules
const contentRoutes = require('./content');
const mediaRoutes = require('./media');

/**
 * Core Template Routes
 * Handles fundamental template operations: content and media management
 */

// Content management routes
// /api/templates/core/content/*
router.use('/content', contentRoutes);

// Media management routes  
// /api/templates/core/media/*
router.use('/media', mediaRoutes);

module.exports = router; 