/**
 * Fraud Detection Service
 * 
 * This service provides methods for detecting and preventing ad fraud,
 * including click fraud, impression fraud, and other suspicious activities.
 */

const mongoose = require('mongoose');
const Analytics = require('../models/Analytics');
const monitoringService = require('./monitoringService');
const cacheService = require('./cacheService');
const fingerprintService = require('./fingerprintService');
const suspiciousActivityService = require('./suspiciousActivityService');

// Cache key prefixes
const CLICK_CACHE_PREFIX = 'fraud:click:';
const IP_CACHE_PREFIX = 'fraud:ip:';
const DEVICE_CACHE_PREFIX = 'fraud:device:';

// Fraud detection thresholds
const THRESHOLDS = {
  // Maximum clicks per user per hour
  MAX_CLICKS_PER_USER_HOUR: 10,
  // Maximum clicks per IP per hour
  MAX_CLICKS_PER_IP_HOUR: 20,
  // Maximum clicks per device per hour
  MAX_CLICKS_PER_DEVICE_HOUR: 15,
  // Minimum time between clicks in milliseconds (500ms)
  MIN_CLICK_INTERVAL: 500,
  // Maximum clicks per ad per user per day
  MAX_CLICKS_PER_AD_USER_DAY: 5,
  // Suspicious click-through rate threshold (%)
  SUSPICIOUS_CTR_THRESHOLD: 20,
  // Suspicious conversion rate threshold (%)
  SUSPICIOUS_CONVERSION_THRESHOLD: 30
};

/**
 * Check if a click is potentially fraudulent
 * @param {Object} clickData - Click data
 * @param {string} clickData.userId - User ID
 * @param {string} clickData.deviceId - Device ID
 * @param {string} clickData.ip - IP address
 * @param {string} clickData.adId - Ad ID
 * @param {string} clickData.timestamp - Click timestamp
 * @param {string} clickData.deviceFingerprint - Device fingerprint
 * @param {string} clickData.ipFingerprint - IP fingerprint
 * @param {Object} clickData.context - Click context
 * @returns {Promise<Object>} Fraud detection result
 */
async function detectClickFraud(clickData) {
  try {
    const { userId, deviceId, ip, adId, timestamp, deviceFingerprint, ipFingerprint, context } = clickData;
    
    // Initialize result
    const result = {
      isFraudulent: false,
      reasons: [],
      score: 0,
      details: {}
    };
    
    // Check for missing required data
    if (!userId || !deviceId || !ip || !adId) {
      result.isFraudulent = true;
      result.reasons.push('Missing required data');
      result.score = 100;
      return result;
    }
    
    // Get IP info from context
    const ipInfo = context && context.ipInfo ? context.ipInfo : null;
    
    // Run all fraud detection checks in parallel
    const [
      frequencyResult,
      intervalResult,
      patternResult,
      deviceResult,
      ipResult,
      ctrResult
    ] = await Promise.all([
      checkClickFrequency(userId, adId),
      checkClickInterval(userId),
      checkClickPattern(userId),
      checkDeviceReputation(deviceId, deviceFingerprint),
      checkIpReputation(ip, ipFingerprint, ipInfo),
      checkClickThroughRate(userId, adId)
    ]);
    
    // Combine results
    const checks = [
      frequencyResult,
      intervalResult,
      patternResult,
      deviceResult,
      ipResult,
      ctrResult
    ];
    
    // Calculate overall fraud score (0-100)
    let totalScore = 0;
    let totalWeight = 0;
    
    for (const check of checks) {
      if (check.score > 0) {
        result.reasons.push(check.reason);
        result.details[check.type] = check;
        totalScore += check.score * check.weight;
        totalWeight += check.weight;
      }
    }
    
    if (totalWeight > 0) {
      result.score = Math.round(totalScore / totalWeight);
    }
    
    // Determine if fraudulent based on score
    result.isFraudulent = result.score >= 70;
    
    // Log fraud detection result
    if (result.isFraudulent) {
      monitoringService.trackMetric('fraud_detection', {
        userId,
        deviceId,
        ip,
        adId,
        score: result.score,
        reasons: result.reasons,
        deviceFingerprint,
        ipFingerprint,
        ipInfo
      });
    }
    
    return result;
  } catch (error) {
    console.error('Error detecting click fraud:', error);
    // In case of error, allow the click but log the error
    return {
      isFraudulent: false,
      reasons: ['Error in fraud detection'],
      score: 0,
      details: { error: error.message }
    };
  }
}

/**
 * Check click frequency for a user
 * @param {string} userId - User ID
 * @param {string} adId - Ad ID
 * @returns {Promise<Object>} Check result
 */
async function checkClickFrequency(userId, adId) {
  try {
    const result = {
      type: 'frequency',
      reason: 'Excessive clicks',
      score: 0,
      weight: 3,
      details: {}
    };
    
    // Get current hour timestamp (floor to hour)
    const hourTimestamp = Math.floor(Date.now() / (60 * 60 * 1000)) * (60 * 60 * 1000);
    
    // Check clicks per user per hour
    const userHourKey = `${CLICK_CACHE_PREFIX}user:${userId}:hour:${hourTimestamp}`;
    const userHourClicks = await cacheService.increment(userHourKey, 1, 60 * 60); // 1 hour TTL
    
    if (userHourClicks > THRESHOLDS.MAX_CLICKS_PER_USER_HOUR) {
      result.score = Math.min(100, Math.round((userHourClicks / THRESHOLDS.MAX_CLICKS_PER_USER_HOUR) * 100));
      result.details.userHourClicks = userHourClicks;
      result.details.userHourThreshold = THRESHOLDS.MAX_CLICKS_PER_USER_HOUR;
    }
    
    // Check clicks per ad per user per day
    const dayTimestamp = Math.floor(Date.now() / (24 * 60 * 60 * 1000)) * (24 * 60 * 60 * 1000);
    const userAdDayKey = `${CLICK_CACHE_PREFIX}user:${userId}:ad:${adId}:day:${dayTimestamp}`;
    const userAdDayClicks = await cacheService.increment(userAdDayKey, 1, 24 * 60 * 60); // 24 hour TTL
    
    if (userAdDayClicks > THRESHOLDS.MAX_CLICKS_PER_AD_USER_DAY) {
      const adDayScore = Math.min(100, Math.round((userAdDayClicks / THRESHOLDS.MAX_CLICKS_PER_AD_USER_DAY) * 100));
      result.score = Math.max(result.score, adDayScore);
      result.details.userAdDayClicks = userAdDayClicks;
      result.details.userAdDayThreshold = THRESHOLDS.MAX_CLICKS_PER_AD_USER_DAY;
    }
    
    return result;
  } catch (error) {
    console.error('Error checking click frequency:', error);
    return {
      type: 'frequency',
      reason: 'Error checking click frequency',
      score: 0,
      weight: 3,
      details: { error: error.message }
    };
  }
}

/**
 * Check time interval between clicks
 * @param {string} userId - User ID
 * @returns {Promise<Object>} Check result
 */
async function checkClickInterval(userId) {
  try {
    const result = {
      type: 'interval',
      reason: 'Clicks too close together',
      score: 0,
      weight: 4,
      details: {}
    };
    
    // Get last click timestamp
    const lastClickKey = `${CLICK_CACHE_PREFIX}user:${userId}:last`;
    const lastClickTime = await cacheService.get(lastClickKey);
    
    if (lastClickTime) {
      const currentTime = Date.now();
      const interval = currentTime - parseInt(lastClickTime);
      
      if (interval < THRESHOLDS.MIN_CLICK_INTERVAL) {
        result.score = Math.min(100, Math.round((THRESHOLDS.MIN_CLICK_INTERVAL / interval) * 100));
        result.details.interval = interval;
        result.details.threshold = THRESHOLDS.MIN_CLICK_INTERVAL;
      }
    }
    
    // Update last click timestamp
    await cacheService.set(lastClickKey, Date.now().toString(), 60 * 60); // 1 hour TTL
    
    return result;
  } catch (error) {
    console.error('Error checking click interval:', error);
    return {
      type: 'interval',
      reason: 'Error checking click interval',
      score: 0,
      weight: 4,
      details: { error: error.message }
    };
  }
}

/**
 * Check click pattern for suspicious behavior
 * @param {string} userId - User ID
 * @returns {Promise<Object>} Check result
 */
async function checkClickPattern(userId) {
  try {
    const result = {
      type: 'pattern',
      reason: 'Suspicious click pattern',
      score: 0,
      weight: 2,
      details: {}
    };
    
    // Get recent clicks (last 10 minutes)
    const tenMinutesAgo = new Date(Date.now() - 10 * 60 * 1000);
    
    const clicks = await Analytics.find({
      userId,
      eventType: 'click',
      timestamp: { $gte: tenMinutesAgo }
    }).sort({ timestamp: 1 });
    
    if (clicks.length >= 3) {
      // Check for regular intervals (bot-like behavior)
      const intervals = [];
      
      for (let i = 1; i < clicks.length; i++) {
        const interval = clicks[i].timestamp - clicks[i-1].timestamp;
        intervals.push(interval);
      }
      
      // Calculate standard deviation of intervals
      const mean = intervals.reduce((sum, val) => sum + val, 0) / intervals.length;
      const variance = intervals.reduce((sum, val) => sum + Math.pow(val - mean, 2), 0) / intervals.length;
      const stdDev = Math.sqrt(variance);
      
      // If standard deviation is low, clicks are suspiciously regular
      if (stdDev < 200 && mean < 2000) { // Less than 200ms std dev and mean interval less than 2s
        result.score = Math.min(100, Math.round((200 / stdDev) * 50));
        result.details.stdDev = stdDev;
        result.details.mean = mean;
        result.details.clickCount = clicks.length;
      }
    }
    
    return result;
  } catch (error) {
    console.error('Error checking click pattern:', error);
    return {
      type: 'pattern',
      reason: 'Error checking click pattern',
      score: 0,
      weight: 2,
      details: { error: error.message }
    };
  }
}

/**
 * Check device reputation using fingerprinting
 * @param {string} deviceId - Device ID
 * @param {string} deviceFingerprint - Device fingerprint
 * @returns {Promise<Object>} Check result
 */
async function checkDeviceReputation(deviceId, deviceFingerprint) {
  try {
    const result = {
      type: 'device',
      reason: 'Suspicious device',
      score: 0,
      weight: 2,
      details: {}
    };
    
    // Get current hour timestamp (floor to hour)
    const hourTimestamp = Math.floor(Date.now() / (60 * 60 * 1000)) * (60 * 60 * 1000);
    
    // Check clicks per device per hour
    const deviceHourKey = `${DEVICE_CACHE_PREFIX}${deviceId}:hour:${hourTimestamp}`;
    const deviceHourClicks = await cacheService.increment(deviceHourKey, 1, 60 * 60); // 1 hour TTL
    
    if (deviceHourClicks > THRESHOLDS.MAX_CLICKS_PER_DEVICE_HOUR) {
      result.score = Math.min(100, Math.round((deviceHourClicks / THRESHOLDS.MAX_CLICKS_PER_DEVICE_HOUR) * 100));
      result.details.deviceHourClicks = deviceHourClicks;
      result.details.deviceHourThreshold = THRESHOLDS.MAX_CLICKS_PER_DEVICE_HOUR;
    }
    
    // Check device reputation score (from previous fraud detections)
    const deviceScoreKey = `${DEVICE_CACHE_PREFIX}${deviceId}:score`;
    const deviceScore = await cacheService.get(deviceScoreKey);
    
    if (deviceScore) {
      const parsedScore = parseInt(deviceScore);
      result.score = Math.max(result.score, parsedScore);
      result.details.reputationScore = parsedScore;
    }
    
    // Check device fingerprint reputation
    if (deviceFingerprint) {
      const fingerprintScoreKey = `${DEVICE_CACHE_PREFIX}fingerprint:${deviceFingerprint}:score`;
      const fingerprintScore = await cacheService.get(fingerprintScoreKey);
      
      if (fingerprintScore) {
        const parsedFingerprintScore = parseInt(fingerprintScore);
        result.score = Math.max(result.score, parsedFingerprintScore);
        result.details.fingerprintReputationScore = parsedFingerprintScore;
      }
      
      // Check clicks per fingerprint per hour
      const fingerprintHourKey = `${DEVICE_CACHE_PREFIX}fingerprint:${deviceFingerprint}:hour:${hourTimestamp}`;
      const fingerprintHourClicks = await cacheService.increment(fingerprintHourKey, 1, 60 * 60); // 1 hour TTL
      
      if (fingerprintHourClicks > THRESHOLDS.MAX_CLICKS_PER_DEVICE_HOUR) {
        const fingerprintScore = Math.min(100, Math.round((fingerprintHourClicks / THRESHOLDS.MAX_CLICKS_PER_DEVICE_HOUR) * 100));
        result.score = Math.max(result.score, fingerprintScore);
        result.details.fingerprintHourClicks = fingerprintHourClicks;
        result.details.fingerprintHourThreshold = THRESHOLDS.MAX_CLICKS_PER_DEVICE_HOUR;
      }
    }
    
    return result;
  } catch (error) {
    console.error('Error checking device reputation:', error);
    return {
      type: 'device',
      reason: 'Error checking device reputation',
      score: 0,
      weight: 2,
      details: { error: error.message }
    };
  }
}

/**
 * Check IP reputation using fingerprinting
 * @param {string} ip - IP address
 * @param {string} ipFingerprint - IP fingerprint
 * @param {Object} ipInfo - IP information
 * @returns {Promise<Object>} Check result
 */
async function checkIpReputation(ip, ipFingerprint, ipInfo) {
  try {
    const result = {
      type: 'ip',
      reason: 'Suspicious IP address',
      score: 0,
      weight: 3,
      details: {}
    };
    
    // Get current hour timestamp (floor to hour)
    const hourTimestamp = Math.floor(Date.now() / (60 * 60 * 1000)) * (60 * 60 * 1000);
    
    // Check clicks per IP per hour
    const ipHourKey = `${IP_CACHE_PREFIX}${ip}:hour:${hourTimestamp}`;
    const ipHourClicks = await cacheService.increment(ipHourKey, 1, 60 * 60); // 1 hour TTL
    
    if (ipHourClicks > THRESHOLDS.MAX_CLICKS_PER_IP_HOUR) {
      result.score = Math.min(100, Math.round((ipHourClicks / THRESHOLDS.MAX_CLICKS_PER_IP_HOUR) * 100));
      result.details.ipHourClicks = ipHourClicks;
      result.details.ipHourThreshold = THRESHOLDS.MAX_CLICKS_PER_IP_HOUR;
    }
    
    // Check IP reputation score (from previous fraud detections)
    const ipScoreKey = `${IP_CACHE_PREFIX}${ip}:score`;
    const ipScore = await cacheService.get(ipScoreKey);
    
    if (ipScore) {
      const parsedScore = parseInt(ipScore);
      result.score = Math.max(result.score, parsedScore);
      result.details.reputationScore = parsedScore;
    }
    
    // Check IP fingerprint reputation
    if (ipFingerprint) {
      const fingerprintScoreKey = `${IP_CACHE_PREFIX}fingerprint:${ipFingerprint}:score`;
      const fingerprintScore = await cacheService.get(fingerprintScoreKey);
      
      if (fingerprintScore) {
        const parsedFingerprintScore = parseInt(fingerprintScore);
        result.score = Math.max(result.score, parsedFingerprintScore);
        result.details.fingerprintReputationScore = parsedFingerprintScore;
      }
    }
    
    // Check for proxy, VPN, or datacenter
    if (ipInfo) {
      if (ipInfo.proxy) {
        result.score = Math.max(result.score, 70); // High risk for proxies
        result.details.proxy = true;
        result.reason = 'Proxy detected';
      }
      
      if (ipInfo.vpn) {
        result.score = Math.max(result.score, 80); // Higher risk for VPNs
        result.details.vpn = true;
        result.reason = 'VPN detected';
      }
      
      if (ipInfo.datacenter) {
        result.score = Math.max(result.score, 90); // Highest risk for datacenters
        result.details.datacenter = true;
        result.reason = 'Datacenter IP detected';
      }
    }
    
    return result;
  } catch (error) {
    console.error('Error checking IP reputation:', error);
    return {
      type: 'ip',
      reason: 'Error checking IP reputation',
      score: 0,
      weight: 3,
      details: { error: error.message }
    };
  }
}

/**
 * Check click-through rate
 * @param {string} userId - User ID
 * @param {string} adId - Ad ID
 * @returns {Promise<Object>} Check result
 */
async function checkClickThroughRate(userId, adId) {
  try {
    const result = {
      type: 'ctr',
      reason: 'Abnormal click-through rate',
      score: 0,
      weight: 2,
      details: {}
    };
    
    // Get current day timestamp (floor to day)
    const dayTimestamp = Math.floor(Date.now() / (24 * 60 * 60 * 1000)) * (24 * 60 * 60 * 1000);
    
    // Get impression count for this user and ad today
    const impressionKey = `analytics:impression:user:${userId}:ad:${adId}:day:${dayTimestamp}`;
    const impressionCount = await cacheService.get(impressionKey) || '0';
    
    // Get click count for this user and ad today
    const clickKey = `analytics:click:user:${userId}:ad:${adId}:day:${dayTimestamp}`;
    const clickCount = await cacheService.get(clickKey) || '0';
    
    const impressions = parseInt(impressionCount);
    const clicks = parseInt(clickCount);
    
    if (impressions > 0 && clicks > 0) {
      const ctr = (clicks / impressions) * 100;
      
      if (ctr > THRESHOLDS.SUSPICIOUS_CTR_THRESHOLD) {
        result.score = Math.min(100, Math.round((ctr / THRESHOLDS.SUSPICIOUS_CTR_THRESHOLD) * 100));
        result.details.ctr = ctr;
        result.details.impressions = impressions;
        result.details.clicks = clicks;
        result.details.threshold = THRESHOLDS.SUSPICIOUS_CTR_THRESHOLD;
      }
    }
    
    return result;
  } catch (error) {
    console.error('Error checking click-through rate:', error);
    return {
      type: 'ctr',
      reason: 'Error checking click-through rate',
      score: 0,
      weight: 2,
      details: { error: error.message }
    };
  }
}

/**
 * Update reputation scores based on fraud detection result
 * @param {Object} clickData - Click data
 * @param {Object} fraudResult - Fraud detection result
 * @returns {Promise<void>}
 */
async function updateReputationScores(clickData, fraudResult) {
  try {
    const { userId, deviceId, ip, deviceFingerprint, ipFingerprint } = clickData;
    const { score } = fraudResult;
    
    if (score > 50) {
      // Update device reputation score
      const deviceScoreKey = `${DEVICE_CACHE_PREFIX}${deviceId}:score`;
      const currentDeviceScore = await cacheService.get(deviceScoreKey) || '0';
      const newDeviceScore = Math.min(100, Math.max(parseInt(currentDeviceScore), score));
      await cacheService.set(deviceScoreKey, newDeviceScore.toString(), 30 * 24 * 60 * 60); // 30 day TTL
      
      // Update IP reputation score
      const ipScoreKey = `${IP_CACHE_PREFIX}${ip}:score`;
      const currentIpScore = await cacheService.get(ipScoreKey) || '0';
      const newIpScore = Math.min(100, Math.max(parseInt(currentIpScore), score));
      await cacheService.set(ipScoreKey, newIpScore.toString(), 7 * 24 * 60 * 60); // 7 day TTL
      
      // Update device fingerprint reputation score
      if (deviceFingerprint) {
        const fingerprintScoreKey = `${DEVICE_CACHE_PREFIX}fingerprint:${deviceFingerprint}:score`;
        const currentFingerprintScore = await cacheService.get(fingerprintScoreKey) || '0';
        const newFingerprintScore = Math.min(100, Math.max(parseInt(currentFingerprintScore), score));
        await cacheService.set(fingerprintScoreKey, newFingerprintScore.toString(), 30 * 24 * 60 * 60); // 30 day TTL
      }
      
      // Update IP fingerprint reputation score
      if (ipFingerprint) {
        const fingerprintScoreKey = `${IP_CACHE_PREFIX}fingerprint:${ipFingerprint}:score`;
        const currentFingerprintScore = await cacheService.get(fingerprintScoreKey) || '0';
        const newFingerprintScore = Math.min(100, Math.max(parseInt(currentFingerprintScore), score));
        await cacheService.set(fingerprintScoreKey, newFingerprintScore.toString(), 7 * 24 * 60 * 60); // 7 day TTL
      }
    }
  } catch (error) {
    console.error('Error updating reputation scores:', error);
  }
}

/**
 * Process a click event and check for fraud
 * @param {Object} clickData - Click data
 * @returns {Promise<Object>} Processing result
 */
async function processClick(clickData) {
  try {
    // Enrich click data with fingerprinting information
    const context = clickData.context || {};
    const enrichedContext = await fingerprintService.enrichContext({
      ...context,
      deviceId: clickData.deviceId,
      ip: clickData.ip,
      userAgent: context.userAgent || 'unknown'
    });
    
    // Update click data with enriched context
    const enrichedClickData = {
      ...clickData,
      context: enrichedContext,
      deviceFingerprint: enrichedContext.deviceFingerprint,
      ipFingerprint: enrichedContext.ipFingerprint
    };
    
    // Detect click fraud
    const fraudResult = await detectClickFraud(enrichedClickData);
    
    // Update reputation scores
    await updateReputationScores(enrichedClickData, fraudResult);
    
    // Track fraud metrics
    if (fraudResult.isFraudulent) {
      monitoringService.incrementCounter('fraud_clicks');
      monitoringService.trackMetric('fraud_score', fraudResult.score);
      
      // Track additional metrics for fingerprinting
      if (enrichedContext.ipInfo) {
        if (enrichedContext.ipInfo.proxy) {
          monitoringService.incrementCounter('proxy_fraud_clicks');
        }
        if (enrichedContext.ipInfo.vpn) {
          monitoringService.incrementCounter('vpn_fraud_clicks');
        }
        if (enrichedContext.ipInfo.datacenter) {
          monitoringService.incrementCounter('datacenter_fraud_clicks');
        }
      }
      
      // Track suspicious activity
      await trackSuspiciousActivity(enrichedClickData, fraudResult);
    }
    
    return {
      allowed: !fraudResult.isFraudulent,
      fraudResult,
      enrichedContext
    };
  } catch (error) {
    console.error('Error processing click:', error);
    // In case of error, allow the click but log the error
    return {
      allowed: true,
      error: error.message
    };
  }
}

/**
 * Track suspicious activity based on fraud detection result
 * @param {Object} clickData - Click data
 * @param {Object} fraudResult - Fraud detection result
 * @returns {Promise<void>}
 */
async function trackSuspiciousActivity(clickData, fraudResult) {
  try {
    const { userId, deviceId, ip, adId } = clickData;
    const { score, reasons, details } = fraudResult;
    
    // Determine activity type and severity
    let activityType = suspiciousActivityService.ACTIVITY_TYPES.CLICK_FRAUD;
    let severity = suspiciousActivityService.SEVERITY_LEVELS.MEDIUM;
    
    // Check for specific fraud types
    if (details.ip && details.ip.details.proxy) {
      activityType = suspiciousActivityService.ACTIVITY_TYPES.PROXY_USAGE;
    } else if (details.ip && details.ip.details.vpn) {
      activityType = suspiciousActivityService.ACTIVITY_TYPES.VPN_USAGE;
    } else if (details.ip && details.ip.details.datacenter) {
      activityType = suspiciousActivityService.ACTIVITY_TYPES.DATACENTER_USAGE;
    } else if (details.pattern && details.pattern.score > 0) {
      activityType = suspiciousActivityService.ACTIVITY_TYPES.SUSPICIOUS_PATTERN;
    } else if (details.device && details.device.score > 0) {
      activityType = suspiciousActivityService.ACTIVITY_TYPES.SUSPICIOUS_DEVICE;
    } else if (details.ip && details.ip.score > 0) {
      activityType = suspiciousActivityService.ACTIVITY_TYPES.SUSPICIOUS_IP;
    }
    
    // Determine severity based on fraud score
    if (score >= 90) {
      severity = suspiciousActivityService.SEVERITY_LEVELS.CRITICAL;
    } else if (score >= 70) {
      severity = suspiciousActivityService.SEVERITY_LEVELS.HIGH;
    } else if (score >= 50) {
      severity = suspiciousActivityService.SEVERITY_LEVELS.MEDIUM;
    } else {
      severity = suspiciousActivityService.SEVERITY_LEVELS.LOW;
    }
    
    // Track suspicious activity
    await suspiciousActivityService.trackActivity(
      activityType,
      {
        userId,
        deviceId,
        ip,
        adId,
        score,
        reasons,
        details: JSON.stringify(details)
      },
      severity
    );
  } catch (error) {
    console.error('Error tracking suspicious activity:', error);
  }
}

/**
 * Get fraud detection statistics
 * @returns {Promise<Object>} Fraud detection statistics
 */
async function getStatistics() {
  try {
    const hourTimestamp = Math.floor(Date.now() / (60 * 60 * 1000)) * (60 * 60 * 1000);
    const dayTimestamp = Math.floor(Date.now() / (24 * 60 * 60 * 1000)) * (24 * 60 * 60 * 1000);
    
    // Get total clicks in the last hour
    const totalClicksHour = await Analytics.countDocuments({
      eventType: 'click',
      timestamp: { $gte: new Date(hourTimestamp) }
    });
    
    // Get total clicks in the last day
    const totalClicksDay = await Analytics.countDocuments({
      eventType: 'click',
      timestamp: { $gte: new Date(dayTimestamp) }
    });
    
    // Get fraud metrics from monitoring service
    const fraudClicksHour = await monitoringService.getCounter('fraud_clicks', 'hour');
    const fraudClicksDay = await monitoringService.getCounter('fraud_clicks', 'day');
    
    // Calculate fraud rates
    const fraudRateHour = totalClicksHour > 0 ? (fraudClicksHour / totalClicksHour) * 100 : 0;
    const fraudRateDay = totalClicksDay > 0 ? (fraudClicksDay / totalClicksDay) * 100 : 0;
    
    return {
      totalClicksHour,
      totalClicksDay,
      fraudClicksHour,
      fraudClicksDay,
      fraudRateHour,
      fraudRateDay
    };
  } catch (error) {
    console.error('Error getting fraud statistics:', error);
    return {
      error: error.message
    };
  }
}

module.exports = {
  detectClickFraud,
  processClick,
  getStatistics,
  THRESHOLDS
}; 