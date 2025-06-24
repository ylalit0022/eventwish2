const express = require('express');
const router = express.Router();

// Import AI route modules
const generationRoutes = require('./generation');
const metadataRoutes = require('./metadata');
const usersRoutes = require('./users');

/**
 * AI Routes Index
 * Combines all AI-related template routes
 */

// AI generation routes
// /api/templates/ai/generation/*
router.use('/generation', generationRoutes);

// AI metadata routes
// /api/templates/ai/metadata/*
router.use('/metadata', metadataRoutes);

// AI user-related routes
// /api/templates/ai/users/*
router.use('/users', usersRoutes);

module.exports = router; 