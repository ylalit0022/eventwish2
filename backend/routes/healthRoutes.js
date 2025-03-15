/**
 * Health Check Routes
 * 
 * This module defines the routes for health checks of the application and its services.
 */

const express = require('express');
const router = express.Router();
const healthCheckService = require('../services/healthCheckService');
const { verifyApiKey } = require('../middleware/authMiddleware');

/**
 * @route GET /api/health
 * @description Get overall health status of the application
 * @access Public (basic), Detailed (authenticated)
 */
router.get('/', async (req, res) => {
  try {
    // Start timer for response time measurement
    const startTime = Date.now();
    
    // Check if request has API key for detailed health info
    const isAuthenticated = req.headers['x-api-key'] === process.env.INTERNAL_API_KEY;
    
    // Get health status
    const healthStatus = await healthCheckService.checkHealth();
    
    // Calculate response time
    const responseTime = Date.now() - startTime;
    
    // Set response headers
    res.set('X-Response-Time', `${responseTime}ms`);
    res.set('Cache-Control', 'no-cache, no-store, must-revalidate');
    
    // Return appropriate response based on authentication
    if (isAuthenticated) {
      // Return detailed health status for authenticated requests
      return res.json({
        success: true,
        status: healthStatus.status,
        timestamp: healthStatus.timestamp,
        responseTime: `${responseTime}ms`,
        services: healthStatus.services
      });
    } else {
      // Return basic health status for public requests
      return res.json({
        success: true,
        status: healthStatus.status,
        timestamp: healthStatus.timestamp,
        responseTime: `${responseTime}ms`
      });
    }
  } catch (error) {
    console.error('Health check error:', error);
    return res.status(500).json({
      success: false,
      status: 'down',
      error: 'Health check failed'
    });
  }
});

/**
 * @route GET /api/health/database
 * @description Get database health status
 * @access Authenticated
 */
router.get('/database', verifyApiKey, async (req, res) => {
  try {
    const dbStatus = await healthCheckService.checkDatabase();
    return res.json({
      success: true,
      status: dbStatus.status,
      message: dbStatus.message,
      details: dbStatus.details
    });
  } catch (error) {
    console.error('Database health check error:', error);
    return res.status(500).json({
      success: false,
      status: 'down',
      error: 'Database health check failed'
    });
  }
});

/**
 * @route GET /api/health/redis
 * @description Get Redis health status
 * @access Authenticated
 */
router.get('/redis', verifyApiKey, async (req, res) => {
  try {
    const redisStatus = await healthCheckService.checkRedis();
    return res.json({
      success: true,
      status: redisStatus.status,
      message: redisStatus.message,
      details: redisStatus.details
    });
  } catch (error) {
    console.error('Redis health check error:', error);
    return res.status(500).json({
      success: false,
      status: 'down',
      error: 'Redis health check failed'
    });
  }
});

/**
 * @route GET /api/health/api
 * @description Get API endpoints health status
 * @access Authenticated
 */
router.get('/api', verifyApiKey, async (req, res) => {
  try {
    const apiStatus = await healthCheckService.checkApiEndpoints();
    return res.json({
      success: true,
      status: apiStatus.status,
      message: apiStatus.message,
      details: apiStatus.details
    });
  } catch (error) {
    console.error('API health check error:', error);
    return res.status(500).json({
      success: false,
      status: 'down',
      error: 'API health check failed'
    });
  }
});

/**
 * @route GET /api/health/system
 * @description Get system health status (disk, memory, CPU)
 * @access Authenticated
 */
router.get('/system', verifyApiKey, async (req, res) => {
  try {
    const [diskStatus, memoryStatus, cpuStatus] = await Promise.all([
      healthCheckService.checkDiskSpace(),
      healthCheckService.checkMemory(),
      healthCheckService.checkCpu()
    ]);
    
    // Determine overall system status
    const overallStatus = healthCheckService.determineOverallStatus([
      diskStatus,
      memoryStatus,
      cpuStatus
    ]);
    
    return res.json({
      success: true,
      status: overallStatus,
      disk: diskStatus,
      memory: memoryStatus,
      cpu: cpuStatus
    });
  } catch (error) {
    console.error('System health check error:', error);
    return res.status(500).json({
      success: false,
      status: 'down',
      error: 'System health check failed'
    });
  }
});

/**
 * @route GET /api/health/admob
 * @description Get AdMob services health status
 * @access Authenticated
 */
router.get('/admob', verifyApiKey, async (req, res) => {
  try {
    const adMobStatus = await healthCheckService.checkAdMobServices();
    return res.json({
      success: true,
      status: adMobStatus.status,
      message: adMobStatus.message,
      details: adMobStatus.details
    });
  } catch (error) {
    console.error('AdMob health check error:', error);
    return res.status(500).json({
      success: false,
      status: 'down',
      error: 'AdMob health check failed'
    });
  }
});

/**
 * @route GET /api/health/liveness
 * @description Simple liveness probe for Kubernetes
 * @access Public
 */
router.get('/liveness', (req, res) => {
  return res.status(200).json({
    success: true,
    status: 'up',
    timestamp: new Date().toISOString()
  });
});

/**
 * @route GET /api/health/readiness
 * @description Readiness probe for Kubernetes
 * @access Public
 */
router.get('/readiness', async (req, res) => {
  try {
    // Check only critical services for readiness
    const [dbStatus, redisStatus] = await Promise.all([
      healthCheckService.checkDatabase(),
      healthCheckService.checkRedis()
    ]);
    
    // If any critical service is down, return 503
    if (dbStatus.status === 'down' || redisStatus.status === 'down') {
      return res.status(503).json({
        success: false,
        status: 'down',
        message: 'Service not ready',
        timestamp: new Date().toISOString()
      });
    }
    
    return res.status(200).json({
      success: true,
      status: 'up',
      message: 'Service ready',
      timestamp: new Date().toISOString()
    });
  } catch (error) {
    console.error('Readiness check error:', error);
    return res.status(503).json({
      success: false,
      status: 'down',
      message: 'Service not ready',
      timestamp: new Date().toISOString()
    });
  }
});

module.exports = router; 