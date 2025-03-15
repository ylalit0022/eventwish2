/**
 * Test Routes
 * 
 * This module defines routes for testing and health checks.
 */

const express = require('express');
const router = express.Router();

/**
 * @route GET /api/test/time
 * @description Get current server time (health check endpoint)
 * @access Public
 */
router.get('/time', (req, res) => {
  res.json({
    success: true,
    time: new Date().toISOString(),
    environment: process.env.NODE_ENV || 'development'
  });
});

/**
 * @route GET /api/test/env
 * @description Get environment information
 * @access Public
 */
router.get('/env', (req, res) => {
  res.json({
    success: true,
    nodeVersion: process.version,
    environment: process.env.NODE_ENV || 'development',
    platform: process.platform,
    arch: process.arch,
    memoryUsage: process.memoryUsage()
  });
});

module.exports = router; 