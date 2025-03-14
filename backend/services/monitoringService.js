const logger = require('../config/logger');
const NodeCache = require('node-cache');
const os = require('os');
const { AdMob } = require('../models/AdMob');

// Initialize cache with standard TTL of 5 minutes and check period of 60 seconds
const monitoringCache = new NodeCache({ stdTTL: 300, checkperiod: 60 });
const MONITORING_CACHE_KEY_PREFIX = 'monitoring_';

// Define alert thresholds
const ALERT_THRESHOLDS = {
  ERROR_RATE: 0.05, // 5% error rate
  RESPONSE_TIME: 500, // 500ms
  MEMORY_USAGE: 0.85, // 85% memory usage
  CPU_USAGE: 0.80, // 80% CPU usage
  DISK_USAGE: 0.90, // 90% disk usage
  REQUEST_RATE: 100, // 100 requests per second
  CACHE_HIT_RATE: 0.50 // 50% cache hit rate
};

// Define monitoring metrics
const metrics = {
  requests: {
    total: 0,
    success: 0,
    error: 0,
    byEndpoint: {}
  },
  responseTimes: {
    average: 0,
    byEndpoint: {}
  },
  cacheHits: {
    total: 0,
    miss: 0,
    rate: 0
  },
  system: {
    memory: {
      total: 0,
      used: 0,
      free: 0,
      usage: 0
    },
    cpu: {
      usage: 0
    },
    disk: {
      total: 0,
      used: 0,
      free: 0,
      usage: 0
    }
  },
  alerts: []
};

/**
 * Track request metrics
 * @param {string} endpoint - The endpoint being requested
 * @param {boolean} success - Whether the request was successful
 * @param {number} responseTime - The response time in milliseconds
 */
const trackRequest = (endpoint, success, responseTime) => {
  try {
    // Update total requests
    metrics.requests.total += 1;
    
    // Update success/error counts
    if (success) {
      metrics.requests.success += 1;
    } else {
      metrics.requests.error += 1;
    }
    
    // Update endpoint-specific metrics
    if (!metrics.requests.byEndpoint[endpoint]) {
      metrics.requests.byEndpoint[endpoint] = {
        total: 0,
        success: 0,
        error: 0,
        responseTime: {
          total: 0,
          count: 0,
          average: 0
        }
      };
    }
    
    metrics.requests.byEndpoint[endpoint].total += 1;
    
    if (success) {
      metrics.requests.byEndpoint[endpoint].success += 1;
    } else {
      metrics.requests.byEndpoint[endpoint].error += 1;
    }
    
    // Update response time metrics
    metrics.requests.byEndpoint[endpoint].responseTime.total += responseTime;
    metrics.requests.byEndpoint[endpoint].responseTime.count += 1;
    metrics.requests.byEndpoint[endpoint].responseTime.average = 
      metrics.requests.byEndpoint[endpoint].responseTime.total / 
      metrics.requests.byEndpoint[endpoint].responseTime.count;
    
    // Update overall response time metrics
    const totalResponseTime = Object.values(metrics.requests.byEndpoint).reduce(
      (total, endpoint) => total + endpoint.responseTime.total, 0
    );
    
    const totalResponseCount = Object.values(metrics.requests.byEndpoint).reduce(
      (total, endpoint) => total + endpoint.responseTime.count, 0
    );
    
    metrics.responseTimes.average = totalResponseTime / totalResponseCount;
    
    // Check for alerts
    checkAlerts();
    
    logger.debug(`Request tracked: ${endpoint}, success: ${success}, responseTime: ${responseTime}ms`);
  } catch (error) {
    logger.error(`Error tracking request metrics: ${error.message}`);
  }
};

/**
 * Track cache metrics
 * @param {boolean} hit - Whether the cache was hit
 */
const trackCache = (hit) => {
  try {
    // Update cache metrics
    if (hit) {
      metrics.cacheHits.total += 1;
    } else {
      metrics.cacheHits.miss += 1;
    }
    
    // Calculate cache hit rate
    const totalCacheRequests = metrics.cacheHits.total + metrics.cacheHits.miss;
    metrics.cacheHits.rate = totalCacheRequests > 0 ? 
      metrics.cacheHits.total / totalCacheRequests : 0;
    
    // Check for alerts
    checkAlerts();
    
    logger.debug(`Cache tracked: hit: ${hit}, rate: ${metrics.cacheHits.rate}`);
  } catch (error) {
    logger.error(`Error tracking cache metrics: ${error.message}`);
  }
};

/**
 * Update system metrics
 */
const updateSystemMetrics = () => {
  try {
    // Update memory metrics
    const totalMemory = os.totalmem();
    const freeMemory = os.freemem();
    const usedMemory = totalMemory - freeMemory;
    
    metrics.system.memory.total = totalMemory;
    metrics.system.memory.free = freeMemory;
    metrics.system.memory.used = usedMemory;
    metrics.system.memory.usage = usedMemory / totalMemory;
    
    // Update CPU metrics
    const cpus = os.cpus();
    let totalIdle = 0;
    let totalTick = 0;
    
    cpus.forEach(cpu => {
      for (const type in cpu.times) {
        totalTick += cpu.times[type];
      }
      totalIdle += cpu.times.idle;
    });
    
    metrics.system.cpu.usage = 1 - (totalIdle / totalTick);
    
    // Check for alerts
    checkAlerts();
    
    logger.debug(`System metrics updated: memory usage: ${metrics.system.memory.usage}, CPU usage: ${metrics.system.cpu.usage}`);
  } catch (error) {
    logger.error(`Error updating system metrics: ${error.message}`);
  }
};

/**
 * Check for alerts based on thresholds
 */
const checkAlerts = () => {
  try {
    const newAlerts = [];
    
    // Check error rate
    const errorRate = metrics.requests.total > 0 ? 
      metrics.requests.error / metrics.requests.total : 0;
    
    if (errorRate > ALERT_THRESHOLDS.ERROR_RATE) {
      newAlerts.push({
        type: 'ERROR_RATE',
        message: `Error rate (${(errorRate * 100).toFixed(2)}%) exceeds threshold (${(ALERT_THRESHOLDS.ERROR_RATE * 100).toFixed(2)}%)`,
        timestamp: new Date(),
        level: 'critical'
      });
    }
    
    // Check response time
    if (metrics.responseTimes.average > ALERT_THRESHOLDS.RESPONSE_TIME) {
      newAlerts.push({
        type: 'RESPONSE_TIME',
        message: `Average response time (${metrics.responseTimes.average.toFixed(2)}ms) exceeds threshold (${ALERT_THRESHOLDS.RESPONSE_TIME}ms)`,
        timestamp: new Date(),
        level: 'warning'
      });
    }
    
    // Check memory usage
    if (metrics.system.memory.usage > ALERT_THRESHOLDS.MEMORY_USAGE) {
      newAlerts.push({
        type: 'MEMORY_USAGE',
        message: `Memory usage (${(metrics.system.memory.usage * 100).toFixed(2)}%) exceeds threshold (${(ALERT_THRESHOLDS.MEMORY_USAGE * 100).toFixed(2)}%)`,
        timestamp: new Date(),
        level: 'warning'
      });
    }
    
    // Check CPU usage
    if (metrics.system.cpu.usage > ALERT_THRESHOLDS.CPU_USAGE) {
      newAlerts.push({
        type: 'CPU_USAGE',
        message: `CPU usage (${(metrics.system.cpu.usage * 100).toFixed(2)}%) exceeds threshold (${(ALERT_THRESHOLDS.CPU_USAGE * 100).toFixed(2)}%)`,
        timestamp: new Date(),
        level: 'warning'
      });
    }
    
    // Check cache hit rate
    if (metrics.cacheHits.rate < ALERT_THRESHOLDS.CACHE_HIT_RATE && 
        (metrics.cacheHits.total + metrics.cacheHits.miss) > 100) {
      newAlerts.push({
        type: 'CACHE_HIT_RATE',
        message: `Cache hit rate (${(metrics.cacheHits.rate * 100).toFixed(2)}%) below threshold (${(ALERT_THRESHOLDS.CACHE_HIT_RATE * 100).toFixed(2)}%)`,
        timestamp: new Date(),
        level: 'info'
      });
    }
    
    // Add new alerts to metrics
    if (newAlerts.length > 0) {
      metrics.alerts = [...newAlerts, ...metrics.alerts].slice(0, 100); // Keep last 100 alerts
      
      // Log alerts
      newAlerts.forEach(alert => {
        if (alert.level === 'critical') {
          logger.error(`ALERT: ${alert.message}`);
        } else if (alert.level === 'warning') {
          logger.warn(`ALERT: ${alert.message}`);
        } else {
          logger.info(`ALERT: ${alert.message}`);
        }
      });
    }
  } catch (error) {
    logger.error(`Error checking alerts: ${error.message}`);
  }
};

/**
 * Get current metrics
 * @returns {Object} - Current metrics
 */
const getMetrics = () => {
  try {
    // Update system metrics before returning
    updateSystemMetrics();
    
    return {
      ...metrics,
      timestamp: new Date()
    };
  } catch (error) {
    logger.error(`Error getting metrics: ${error.message}`);
    return {
      error: error.message,
      timestamp: new Date()
    };
  }
};

/**
 * Get ad-specific metrics
 * @returns {Promise<Object>} - Ad-specific metrics
 */
const getAdMetrics = async () => {
  try {
    // Check cache first
    const cacheKey = `${MONITORING_CACHE_KEY_PREFIX}ad_metrics`;
    const cachedMetrics = monitoringCache.get(cacheKey);
    
    if (cachedMetrics) {
      logger.debug('Cache hit for ad metrics');
      return cachedMetrics;
    }
    
    // Get all ads
    const ads = await AdMob.find();
    
    // Calculate metrics
    const adMetrics = {
      totalAds: ads.length,
      activeAds: ads.filter(ad => ad.status).length,
      inactiveAds: ads.filter(ad => !ad.status).length,
      byType: {},
      totalImpressions: 0,
      totalClicks: 0,
      averageCtr: 0,
      totalRevenue: 0
    };
    
    // Process each ad
    ads.forEach(ad => {
      // Add to totals
      adMetrics.totalImpressions += ad.impressions || 0;
      adMetrics.totalClicks += ad.clicks || 0;
      adMetrics.totalRevenue += ad.revenue || 0;
      
      // Add to type breakdown
      if (!adMetrics.byType[ad.adType]) {
        adMetrics.byType[ad.adType] = {
          total: 0,
          active: 0,
          inactive: 0,
          impressions: 0,
          clicks: 0,
          ctr: 0,
          revenue: 0
        };
      }
      
      adMetrics.byType[ad.adType].total += 1;
      
      if (ad.status) {
        adMetrics.byType[ad.adType].active += 1;
      } else {
        adMetrics.byType[ad.adType].inactive += 1;
      }
      
      adMetrics.byType[ad.adType].impressions += ad.impressions || 0;
      adMetrics.byType[ad.adType].clicks += ad.clicks || 0;
      adMetrics.byType[ad.adType].revenue += ad.revenue || 0;
      
      // Calculate CTR for this ad type
      if (adMetrics.byType[ad.adType].impressions > 0) {
        adMetrics.byType[ad.adType].ctr = 
          (adMetrics.byType[ad.adType].clicks / adMetrics.byType[ad.adType].impressions) * 100;
      }
    });
    
    // Calculate overall average CTR
    if (adMetrics.totalImpressions > 0) {
      adMetrics.averageCtr = (adMetrics.totalClicks / adMetrics.totalImpressions) * 100;
    }
    
    // Add timestamp
    adMetrics.timestamp = new Date();
    
    // Store in cache
    monitoringCache.set(cacheKey, adMetrics);
    
    return adMetrics;
  } catch (error) {
    logger.error(`Error getting ad metrics: ${error.message}`);
    return {
      error: error.message,
      timestamp: new Date()
    };
  }
};

/**
 * Reset metrics
 */
const resetMetrics = () => {
  try {
    // Reset request metrics
    metrics.requests = {
      total: 0,
      success: 0,
      error: 0,
      byEndpoint: {}
    };
    
    // Reset response time metrics
    metrics.responseTimes = {
      average: 0,
      byEndpoint: {}
    };
    
    // Reset cache metrics
    metrics.cacheHits = {
      total: 0,
      miss: 0,
      rate: 0
    };
    
    // Keep alerts
    
    logger.info('Metrics reset');
  } catch (error) {
    logger.error(`Error resetting metrics: ${error.message}`);
  }
};

// Initialize system metrics
updateSystemMetrics();

// Update system metrics every minute
setInterval(updateSystemMetrics, 60000);

module.exports = {
  trackRequest,
  trackCache,
  getMetrics,
  getAdMetrics,
  resetMetrics
}; 