/**
 * Health Check Service
 * 
 * This service provides health check functionality for ad-related services.
 * It monitors the health of various components and provides status information.
 */

const mongoose = require('mongoose');
const redis = require('../config/redis');
const axios = require('axios');
const os = require('os');
const fs = require('fs').promises;
const path = require('path');
const monitoringService = require('./monitoringService');

// Health check status constants
const STATUS = {
  UP: 'up',
  DOWN: 'down',
  DEGRADED: 'degraded'
};

/**
 * Perform a comprehensive health check of all services
 * @returns {Promise<Object>} Health check results
 */
async function checkHealth() {
  try {
    // Check all components
    const [
      dbStatus,
      redisStatus,
      apiStatus,
      diskStatus,
      memoryStatus,
      cpuStatus,
      adMobStatus
    ] = await Promise.all([
      checkDatabase(),
      checkRedis(),
      checkApiEndpoints(),
      checkDiskSpace(),
      checkMemory(),
      checkCpu(),
      checkAdMobServices()
    ]);
    
    // Determine overall status
    const overallStatus = determineOverallStatus([
      dbStatus,
      redisStatus,
      apiStatus,
      diskStatus,
      memoryStatus,
      cpuStatus,
      adMobStatus
    ]);
    
    // Record metrics
    recordHealthMetrics({
      database: dbStatus,
      redis: redisStatus,
      api: apiStatus,
      disk: diskStatus,
      memory: memoryStatus,
      cpu: cpuStatus,
      adMob: adMobStatus,
      overall: overallStatus
    });
    
    return {
      status: overallStatus,
      timestamp: new Date().toISOString(),
      services: {
        database: dbStatus,
        redis: redisStatus,
        api: apiStatus,
        disk: diskStatus,
        memory: memoryStatus,
        cpu: cpuStatus,
        adMob: adMobStatus
      }
    };
  } catch (error) {
    console.error('Error performing health check:', error);
    return {
      status: STATUS.DOWN,
      timestamp: new Date().toISOString(),
      error: error.message
    };
  }
}

/**
 * Check database connection health
 * @returns {Promise<Object>} Database health status
 */
async function checkDatabase() {
  try {
    // Check if connected
    if (mongoose.connection.readyState !== 1) {
      return {
        status: STATUS.DOWN,
        message: 'Database disconnected',
        details: { readyState: mongoose.connection.readyState }
      };
    }
    
    // Check response time
    const startTime = Date.now();
    await mongoose.connection.db.admin().ping();
    const responseTime = Date.now() - startTime;
    
    // Check if response time is acceptable
    const status = responseTime < 500 ? STATUS.UP : STATUS.DEGRADED;
    
    return {
      status,
      message: status === STATUS.UP ? 'Database connected' : 'Database slow response',
      details: {
        responseTime,
        host: mongoose.connection.host,
        name: mongoose.connection.name
      }
    };
  } catch (error) {
    return {
      status: STATUS.DOWN,
      message: 'Database error',
      details: { error: error.message }
    };
  }
}

/**
 * Check Redis connection health
 * @returns {Promise<Object>} Redis health status
 */
async function checkRedis() {
  try {
    const client = redis.getClient();
    
    // Check if connected
    if (!client.connected) {
      return {
        status: STATUS.DOWN,
        message: 'Redis disconnected',
        details: { connected: false }
      };
    }
    
    // Check response time
    const startTime = Date.now();
    await client.ping();
    const responseTime = Date.now() - startTime;
    
    // Check if response time is acceptable
    const status = responseTime < 100 ? STATUS.UP : STATUS.DEGRADED;
    
    return {
      status,
      message: status === STATUS.UP ? 'Redis connected' : 'Redis slow response',
      details: {
        responseTime,
        host: client.options.host,
        port: client.options.port
      }
    };
  } catch (error) {
    return {
      status: STATUS.DOWN,
      message: 'Redis error',
      details: { error: error.message }
    };
  }
}

/**
 * Check API endpoints health
 * @returns {Promise<Object>} API health status
 */
async function checkApiEndpoints() {
  try {
    const endpoints = [
      { url: '/api/admob/health', name: 'AdMob Admin API' },
      { url: '/api/client/health', name: 'Client API' },
      { url: '/api/analytics/health', name: 'Analytics API' },
      { url: '/api/fraud/health', name: 'Fraud Detection API' },
      { url: '/api/suspicious-activity/health', name: 'Suspicious Activity API' }
    ];
    
    const baseUrl = process.env.API_BASE_URL || 'http://localhost:3000';
    
    const results = await Promise.all(
      endpoints.map(async (endpoint) => {
        try {
          const startTime = Date.now();
          const response = await axios.get(`${baseUrl}${endpoint.url}`, {
            timeout: 5000,
            headers: {
              'x-api-key': process.env.INTERNAL_API_KEY
            }
          });
          const responseTime = Date.now() - startTime;
          
          // Check if response is valid and response time is acceptable
          const status = response.status === 200 && responseTime < 1000 ? STATUS.UP : STATUS.DEGRADED;
          
          return {
            name: endpoint.name,
            status,
            responseTime,
            statusCode: response.status
          };
        } catch (error) {
          return {
            name: endpoint.name,
            status: STATUS.DOWN,
            error: error.message
          };
        }
      })
    );
    
    // Determine overall API status
    const overallStatus = determineOverallStatus(results.map(r => ({ status: r.status })));
    
    return {
      status: overallStatus,
      message: `API endpoints: ${results.filter(r => r.status === STATUS.UP).length}/${results.length} up`,
      details: { endpoints: results }
    };
  } catch (error) {
    return {
      status: STATUS.DOWN,
      message: 'API check error',
      details: { error: error.message }
    };
  }
}

/**
 * Check disk space
 * @returns {Promise<Object>} Disk space health status
 */
async function checkDiskSpace() {
  try {
    // Get disk usage for the current directory
    const stats = await fs.statfs(path.resolve('.'));
    
    const totalSpace = stats.blocks * stats.bsize;
    const freeSpace = stats.bfree * stats.bsize;
    const usedSpace = totalSpace - freeSpace;
    const usedPercentage = (usedSpace / totalSpace) * 100;
    
    // Determine status based on disk usage
    let status = STATUS.UP;
    let message = 'Disk space sufficient';
    
    if (usedPercentage > 90) {
      status = STATUS.DOWN;
      message = 'Disk space critically low';
    } else if (usedPercentage > 80) {
      status = STATUS.DEGRADED;
      message = 'Disk space low';
    }
    
    return {
      status,
      message,
      details: {
        totalSpace: formatBytes(totalSpace),
        freeSpace: formatBytes(freeSpace),
        usedSpace: formatBytes(usedSpace),
        usedPercentage: usedPercentage.toFixed(2) + '%'
      }
    };
  } catch (error) {
    return {
      status: STATUS.DOWN,
      message: 'Disk check error',
      details: { error: error.message }
    };
  }
}

/**
 * Check memory usage
 * @returns {Promise<Object>} Memory health status
 */
async function checkMemory() {
  try {
    const totalMemory = os.totalmem();
    const freeMemory = os.freemem();
    const usedMemory = totalMemory - freeMemory;
    const usedPercentage = (usedMemory / totalMemory) * 100;
    
    // Determine status based on memory usage
    let status = STATUS.UP;
    let message = 'Memory usage normal';
    
    if (usedPercentage > 90) {
      status = STATUS.DOWN;
      message = 'Memory usage critically high';
    } else if (usedPercentage > 80) {
      status = STATUS.DEGRADED;
      message = 'Memory usage high';
    }
    
    return {
      status,
      message,
      details: {
        totalMemory: formatBytes(totalMemory),
        freeMemory: formatBytes(freeMemory),
        usedMemory: formatBytes(usedMemory),
        usedPercentage: usedPercentage.toFixed(2) + '%'
      }
    };
  } catch (error) {
    return {
      status: STATUS.DOWN,
      message: 'Memory check error',
      details: { error: error.message }
    };
  }
}

/**
 * Check CPU usage
 * @returns {Promise<Object>} CPU health status
 */
async function checkCpu() {
  try {
    const cpus = os.cpus();
    let totalIdle = 0;
    let totalTick = 0;
    
    for (const cpu of cpus) {
      for (const type in cpu.times) {
        totalTick += cpu.times[type];
      }
      totalIdle += cpu.times.idle;
    }
    
    const usedPercentage = 100 - (totalIdle / totalTick * 100);
    
    // Determine status based on CPU usage
    let status = STATUS.UP;
    let message = 'CPU usage normal';
    
    if (usedPercentage > 90) {
      status = STATUS.DOWN;
      message = 'CPU usage critically high';
    } else if (usedPercentage > 70) {
      status = STATUS.DEGRADED;
      message = 'CPU usage high';
    }
    
    return {
      status,
      message,
      details: {
        cpuCount: cpus.length,
        model: cpus[0].model,
        usedPercentage: usedPercentage.toFixed(2) + '%'
      }
    };
  } catch (error) {
    return {
      status: STATUS.DOWN,
      message: 'CPU check error',
      details: { error: error.message }
    };
  }
}

/**
 * Check AdMob services health
 * @returns {Promise<Object>} AdMob services health status
 */
async function checkAdMobServices() {
  try {
    // Check AdMob configuration service
    const adMobConfigCheck = await checkAdMobConfigService();
    
    // Check ad serving service
    const adServingCheck = await checkAdServingService();
    
    // Check fraud detection service
    const fraudDetectionCheck = await checkFraudDetectionService();
    
    // Check suspicious activity service
    const suspiciousActivityCheck = await checkSuspiciousActivityService();
    
    // Determine overall status
    const overallStatus = determineOverallStatus([
      adMobConfigCheck,
      adServingCheck,
      fraudDetectionCheck,
      suspiciousActivityCheck
    ]);
    
    return {
      status: overallStatus,
      message: `AdMob services: ${[adMobConfigCheck, adServingCheck, fraudDetectionCheck, suspiciousActivityCheck].filter(s => s.status === STATUS.UP).length}/4 up`,
      details: {
        configService: adMobConfigCheck,
        adServingService: adServingCheck,
        fraudDetectionService: fraudDetectionCheck,
        suspiciousActivityService: suspiciousActivityCheck
      }
    };
  } catch (error) {
    return {
      status: STATUS.DOWN,
      message: 'AdMob services check error',
      details: { error: error.message }
    };
  }
}

/**
 * Check AdMob configuration service health
 * @returns {Promise<Object>} AdMob configuration service health status
 */
async function checkAdMobConfigService() {
  try {
    // Check if AdMob configuration collection exists and has documents
    const configCount = await mongoose.connection.db.collection('admob_configs').countDocuments();
    
    if (configCount === 0) {
      return {
        status: STATUS.DEGRADED,
        message: 'No AdMob configurations found'
      };
    }
    
    return {
      status: STATUS.UP,
      message: 'AdMob configuration service healthy',
      details: { configCount }
    };
  } catch (error) {
    return {
      status: STATUS.DOWN,
      message: 'AdMob configuration service error',
      details: { error: error.message }
    };
  }
}

/**
 * Check ad serving service health
 * @returns {Promise<Object>} Ad serving service health status
 */
async function checkAdServingService() {
  try {
    // Check if ad serving is working by making a test request
    const response = await axios.get(`${process.env.API_BASE_URL || 'http://localhost:3000'}/api/client/test-ad`, {
      timeout: 3000,
      headers: {
        'x-api-key': process.env.INTERNAL_API_KEY
      }
    });
    
    if (response.status !== 200 || !response.data.success) {
      return {
        status: STATUS.DOWN,
        message: 'Ad serving service not responding correctly',
        details: { statusCode: response.status }
      };
    }
    
    return {
      status: STATUS.UP,
      message: 'Ad serving service healthy',
      details: { responseTime: response.headers['x-response-time'] }
    };
  } catch (error) {
    return {
      status: STATUS.DOWN,
      message: 'Ad serving service error',
      details: { error: error.message }
    };
  }
}

/**
 * Check fraud detection service health
 * @returns {Promise<Object>} Fraud detection service health status
 */
async function checkFraudDetectionService() {
  try {
    // Check if fraud detection service is working
    const response = await axios.get(`${process.env.API_BASE_URL || 'http://localhost:3000'}/api/fraud/health`, {
      timeout: 3000,
      headers: {
        'x-api-key': process.env.INTERNAL_API_KEY
      }
    });
    
    if (response.status !== 200 || !response.data.success) {
      return {
        status: STATUS.DOWN,
        message: 'Fraud detection service not responding correctly',
        details: { statusCode: response.status }
      };
    }
    
    return {
      status: STATUS.UP,
      message: 'Fraud detection service healthy',
      details: { responseTime: response.headers['x-response-time'] }
    };
  } catch (error) {
    return {
      status: STATUS.DOWN,
      message: 'Fraud detection service error',
      details: { error: error.message }
    };
  }
}

/**
 * Check suspicious activity service health
 * @returns {Promise<Object>} Suspicious activity service health status
 */
async function checkSuspiciousActivityService() {
  try {
    // Check if suspicious activity service is working
    const response = await axios.get(`${process.env.API_BASE_URL || 'http://localhost:3000'}/api/suspicious-activity/health`, {
      timeout: 3000,
      headers: {
        'x-api-key': process.env.INTERNAL_API_KEY
      }
    });
    
    if (response.status !== 200 || !response.data.success) {
      return {
        status: STATUS.DOWN,
        message: 'Suspicious activity service not responding correctly',
        details: { statusCode: response.status }
      };
    }
    
    return {
      status: STATUS.UP,
      message: 'Suspicious activity service healthy',
      details: { responseTime: response.headers['x-response-time'] }
    };
  } catch (error) {
    return {
      status: STATUS.DOWN,
      message: 'Suspicious activity service error',
      details: { error: error.message }
    };
  }
}

/**
 * Determine overall status based on component statuses
 * @param {Array<Object>} components - Component status objects
 * @returns {string} Overall status
 */
function determineOverallStatus(components) {
  if (components.some(c => c.status === STATUS.DOWN)) {
    return STATUS.DOWN;
  }
  
  if (components.some(c => c.status === STATUS.DEGRADED)) {
    return STATUS.DEGRADED;
  }
  
  return STATUS.UP;
}

/**
 * Record health metrics for monitoring
 * @param {Object} healthData - Health check data
 */
function recordHealthMetrics(healthData) {
  // Record overall status
  monitoringService.trackMetric('health_status', {
    status: healthData.overall,
    timestamp: Date.now()
  });
  
  // Record component statuses
  for (const [component, data] of Object.entries(healthData.services || {})) {
    monitoringService.trackMetric(`health_${component}`, {
      status: data.status,
      timestamp: Date.now()
    });
  }
}

/**
 * Format bytes to human-readable format
 * @param {number} bytes - Bytes to format
 * @returns {string} Formatted string
 */
function formatBytes(bytes) {
  if (bytes === 0) return '0 Bytes';
  
  const k = 1024;
  const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
}

module.exports = {
  STATUS,
  checkHealth,
  checkDatabase,
  checkRedis,
  checkApiEndpoints,
  checkDiskSpace,
  checkMemory,
  checkCpu,
  checkAdMobServices
}; 