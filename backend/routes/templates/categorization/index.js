const express = require('express');
const router = express.Router();

// Import categorization route modules
const creatorsRoutes = require('./creators');
const tagsRoutes = require('./tags');
const relationsRoutes = require('./relations');
const searchRoutes = require('./search');

/**
 * Categorization Routes Index
 * Combines all categorization-related template routes
 */

// Creator management routes
// /api/templates/categorization/creators/*
router.use('/creators', creatorsRoutes);

// Tags management routes
// /api/templates/categorization/tags/*
router.use('/tags', tagsRoutes);

// Template relations routes
// /api/templates/categorization/relations/*
router.use('/relations', relationsRoutes);

// Search and filtering routes
// /api/templates/categorization/search/*
router.use('/search', searchRoutes);

module.exports = router; 