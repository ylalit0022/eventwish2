/**
 * Suspicious Activity Monitoring Service
 * 
 * This service provides methods for monitoring and alerting on suspicious
 * activities related to ad interactions, such as potential click fraud,
 * abnormal traffic patterns, and other security concerns.
 */

const mongoose = require('mongoose');
const monitoringService = require('./monitoringService');
const cacheService = require('./cacheService');
const Analytics = require('../models/Analytics');

// Cache key prefixes
const SUSPICIOUS_CACHE_PREFIX = 'suspicious:';

// Activity types
const ACTIVITY_TYPES = {
  CLICK_FRAUD: 'click_fraud',
  IMPRESSION_FRAUD: 'impression_fraud',
  ABNORMAL_TRAFFIC: 'abnormal_traffic',
  PROXY_USAGE: 'proxy_usage',
  VPN_USAGE: 'vpn_usage',
  DATACENTER_USAGE: 'datacenter_usage',
  SUSPICIOUS_DEVICE: 'suspicious_device',
  SUSPICIOUS_IP: 'suspicious_ip',
  SUSPICIOUS_USER: 'suspicious_user',
  SUSPICIOUS_PATTERN: 'suspicious_pattern'
};

// Severity levels
const SEVERITY_LEVELS = {
  LOW: 'low',
  MEDIUM: 'medium',
  HIGH: 'high',
  CRITICAL: 'critical'
};

/**
 * Track a suspicious activity
 * @param {string} activityType - Type of suspicious activity
 * @param {Object} data - Activity data
 * @param {string} severity - Severity level
 * @returns {Promise<void>}
 */
async function trackActivity(activityType, data, severity = SEVERITY_LEVELS.MEDIUM) {
  try {
    // Validate activity type
    if (!Object.values(ACTIVITY_TYPES).includes(activityType)) {
      throw new Error(`Invalid activity type: ${activityType}`);
    }
    
    // Validate severity level
    if (!Object.values(SEVERITY_LEVELS).includes(severity)) {
      throw new Error(`Invalid severity level: ${severity}`);
    }
    
    // Create activity record
    const activity = {
      type: activityType,
      data,
      severity,
      timestamp: Date.now()
    };
    
    // Track in monitoring service
    monitoringService.trackMetric(`suspicious_activity_${activityType}`, activity);
    monitoringService.incrementCounter(`suspicious_activity_${activityType}_count`);
    monitoringService.incrementCounter(`suspicious_activity_${severity}_count`);
    
    // Store in database for long-term analysis
    await new Analytics({
      eventType: 'suspicious_activity',
      eventData: {
        activityType,
        severity,
        ...data
      },
      timestamp: new Date()
    }).save();
    
    // Alert on high severity activities
    if (severity === SEVERITY_LEVELS.HIGH || severity === SEVERITY_LEVELS.CRITICAL) {
      await triggerAlert(activity);
    }
    
    // Update reputation scores if applicable
    if (data.userId) {
      await updateUserReputation(data.userId, activityType, severity);
    }
    
    if (data.deviceId) {
      await updateDeviceReputation(data.deviceId, activityType, severity);
    }
    
    if (data.ip) {
      await updateIpReputation(data.ip, activityType, severity);
    }
  } catch (error) {
    console.error('Error tracking suspicious activity:', error);
  }
}

/**
 * Trigger an alert for a suspicious activity
 * @param {Object} activity - Suspicious activity
 * @returns {Promise<void>}
 */
async function triggerAlert(activity) {
  try {
    // Log alert
    console.warn('SUSPICIOUS ACTIVITY ALERT:', activity);
    
    // Track alert in monitoring service
    monitoringService.trackEvent('suspicious_activity_alert', activity);
    
    // TODO: Implement notification system (email, SMS, etc.)
    // This would typically integrate with a notification service
  } catch (error) {
    console.error('Error triggering alert:', error);
  }
}

/**
 * Update user reputation based on suspicious activity
 * @param {string} userId - User ID
 * @param {string} activityType - Type of suspicious activity
 * @param {string} severity - Severity level
 * @returns {Promise<void>}
 */
async function updateUserReputation(userId, activityType, severity) {
  try {
    // Get current reputation score
    const reputationKey = `${SUSPICIOUS_CACHE_PREFIX}user:${userId}:reputation`;
    const currentScore = await cacheService.get(reputationKey) || '0';
    
    // Calculate score adjustment based on severity
    let adjustment = 0;
    switch (severity) {
      case SEVERITY_LEVELS.LOW:
        adjustment = 1;
        break;
      case SEVERITY_LEVELS.MEDIUM:
        adjustment = 5;
        break;
      case SEVERITY_LEVELS.HIGH:
        adjustment = 15;
        break;
      case SEVERITY_LEVELS.CRITICAL:
        adjustment = 30;
        break;
    }
    
    // Update reputation score (higher is worse)
    const newScore = Math.min(100, parseInt(currentScore) + adjustment);
    await cacheService.set(reputationKey, newScore.toString(), 30 * 24 * 60 * 60); // 30 day TTL
    
    // Track activity count for this user
    const activityCountKey = `${SUSPICIOUS_CACHE_PREFIX}user:${userId}:${activityType}:count`;
    await cacheService.increment(activityCountKey, 1, 30 * 24 * 60 * 60); // 30 day TTL
  } catch (error) {
    console.error('Error updating user reputation:', error);
  }
}

/**
 * Update device reputation based on suspicious activity
 * @param {string} deviceId - Device ID
 * @param {string} activityType - Type of suspicious activity
 * @param {string} severity - Severity level
 * @returns {Promise<void>}
 */
async function updateDeviceReputation(deviceId, activityType, severity) {
  try {
    // Get current reputation score
    const reputationKey = `${SUSPICIOUS_CACHE_PREFIX}device:${deviceId}:reputation`;
    const currentScore = await cacheService.get(reputationKey) || '0';
    
    // Calculate score adjustment based on severity
    let adjustment = 0;
    switch (severity) {
      case SEVERITY_LEVELS.LOW:
        adjustment = 1;
        break;
      case SEVERITY_LEVELS.MEDIUM:
        adjustment = 5;
        break;
      case SEVERITY_LEVELS.HIGH:
        adjustment = 15;
        break;
      case SEVERITY_LEVELS.CRITICAL:
        adjustment = 30;
        break;
    }
    
    // Update reputation score (higher is worse)
    const newScore = Math.min(100, parseInt(currentScore) + adjustment);
    await cacheService.set(reputationKey, newScore.toString(), 30 * 24 * 60 * 60); // 30 day TTL
    
    // Track activity count for this device
    const activityCountKey = `${SUSPICIOUS_CACHE_PREFIX}device:${deviceId}:${activityType}:count`;
    await cacheService.increment(activityCountKey, 1, 30 * 24 * 60 * 60); // 30 day TTL
  } catch (error) {
    console.error('Error updating device reputation:', error);
  }
}

/**
 * Update IP reputation based on suspicious activity
 * @param {string} ip - IP address
 * @param {string} activityType - Type of suspicious activity
 * @param {string} severity - Severity level
 * @returns {Promise<void>}
 */
async function updateIpReputation(ip, activityType, severity) {
  try {
    // Get current reputation score
    const reputationKey = `${SUSPICIOUS_CACHE_PREFIX}ip:${ip}:reputation`;
    const currentScore = await cacheService.get(reputationKey) || '0';
    
    // Calculate score adjustment based on severity
    let adjustment = 0;
    switch (severity) {
      case SEVERITY_LEVELS.LOW:
        adjustment = 1;
        break;
      case SEVERITY_LEVELS.MEDIUM:
        adjustment = 5;
        break;
      case SEVERITY_LEVELS.HIGH:
        adjustment = 15;
        break;
      case SEVERITY_LEVELS.CRITICAL:
        adjustment = 30;
        break;
    }
    
    // Update reputation score (higher is worse)
    const newScore = Math.min(100, parseInt(currentScore) + adjustment);
    await cacheService.set(reputationKey, newScore.toString(), 7 * 24 * 60 * 60); // 7 day TTL
    
    // Track activity count for this IP
    const activityCountKey = `${SUSPICIOUS_CACHE_PREFIX}ip:${ip}:${activityType}:count`;
    await cacheService.increment(activityCountKey, 1, 7 * 24 * 60 * 60); // 7 day TTL
  } catch (error) {
    console.error('Error updating IP reputation:', error);
  }
}

/**
 * Get suspicious activities for a specific entity
 * @param {string} entityType - Entity type (user, device, ip)
 * @param {string} entityId - Entity ID
 * @param {Object} options - Query options
 * @param {number} options.limit - Maximum number of activities to return
 * @param {number} options.skip - Number of activities to skip
 * @param {string} options.sortBy - Field to sort by
 * @param {string} options.sortOrder - Sort order (asc, desc)
 * @returns {Promise<Array>} Suspicious activities
 */
async function getActivities(entityType, entityId, options = {}) {
  try {
    // Set default options
    const limit = options.limit || 20;
    const skip = options.skip || 0;
    const sortBy = options.sortBy || 'timestamp';
    const sortOrder = options.sortOrder === 'asc' ? 1 : -1;
    
    // Build query
    const query = {
      eventType: 'suspicious_activity'
    };
    
    // Add entity filter
    if (entityType && entityId) {
      query[`eventData.${entityType}Id`] = entityId;
    }
    
    // Execute query
    const activities = await Analytics.find(query)
      .sort({ [sortBy]: sortOrder })
      .skip(skip)
      .limit(limit);
    
    return activities;
  } catch (error) {
    console.error('Error getting suspicious activities:', error);
    return [];
  }
}

/**
 * Get reputation score for a specific entity
 * @param {string} entityType - Entity type (user, device, ip)
 * @param {string} entityId - Entity ID
 * @returns {Promise<number>} Reputation score
 */
async function getReputationScore(entityType, entityId) {
  try {
    // Validate entity type
    if (!['user', 'device', 'ip'].includes(entityType)) {
      throw new Error(`Invalid entity type: ${entityType}`);
    }
    
    // Get reputation score
    const reputationKey = `${SUSPICIOUS_CACHE_PREFIX}${entityType}:${entityId}:reputation`;
    const score = await cacheService.get(reputationKey) || '0';
    
    return parseInt(score);
  } catch (error) {
    console.error('Error getting reputation score:', error);
    return 0;
  }
}

/**
 * Check if an entity is suspicious
 * @param {string} entityType - Entity type (user, device, ip)
 * @param {string} entityId - Entity ID
 * @param {number} threshold - Reputation score threshold
 * @returns {Promise<boolean>} Whether the entity is suspicious
 */
async function isSuspicious(entityType, entityId, threshold = 50) {
  try {
    const score = await getReputationScore(entityType, entityId);
    return score >= threshold;
  } catch (error) {
    console.error('Error checking if entity is suspicious:', error);
    return false;
  }
}

/**
 * Analyze traffic patterns for anomalies
 * @param {string} entityType - Entity type (user, device, ip)
 * @param {string} entityId - Entity ID
 * @returns {Promise<Object>} Analysis result
 */
async function analyzeTrafficPatterns(entityType, entityId) {
  try {
    // Get recent activities
    const recentActivities = await Analytics.find({
      [`eventData.${entityType}Id`]: entityId,
      timestamp: { $gte: new Date(Date.now() - 24 * 60 * 60 * 1000) } // Last 24 hours
    }).sort({ timestamp: 1 });
    
    // Calculate activity frequency
    const activityCount = recentActivities.length;
    const activityFrequency = activityCount / 24; // Activities per hour
    
    // Calculate time intervals between activities
    const intervals = [];
    for (let i = 1; i < recentActivities.length; i++) {
      const interval = recentActivities[i].timestamp - recentActivities[i-1].timestamp;
      intervals.push(interval);
    }
    
    // Calculate standard deviation of intervals
    let stdDev = 0;
    if (intervals.length > 0) {
      const mean = intervals.reduce((sum, val) => sum + val, 0) / intervals.length;
      const variance = intervals.reduce((sum, val) => sum + Math.pow(val - mean, 2), 0) / intervals.length;
      stdDev = Math.sqrt(variance);
    }
    
    // Determine if pattern is suspicious
    const isFrequencySuspicious = activityFrequency > 10; // More than 10 activities per hour
    const isPatternSuspicious = stdDev < 1000 && intervals.length >= 5; // Regular intervals with low deviation
    
    // Return analysis result
    return {
      activityCount,
      activityFrequency,
      intervalStdDev: stdDev,
      isFrequencySuspicious,
      isPatternSuspicious,
      isSuspicious: isFrequencySuspicious || isPatternSuspicious
    };
  } catch (error) {
    console.error('Error analyzing traffic patterns:', error);
    return {
      error: error.message,
      isSuspicious: false
    };
  }
}

/**
 * Get suspicious activity dashboard data
 * @returns {Promise<Object>} Dashboard data
 */
async function getDashboardData() {
  try {
    // Get counts for different activity types
    const activityCounts = {};
    for (const type of Object.values(ACTIVITY_TYPES)) {
      activityCounts[type] = await monitoringService.getCounter(`suspicious_activity_${type}_count`);
    }
    
    // Get counts for different severity levels
    const severityCounts = {};
    for (const level of Object.values(SEVERITY_LEVELS)) {
      severityCounts[level] = await monitoringService.getCounter(`suspicious_activity_${level}_count`);
    }
    
    // Get recent activities
    const recentActivities = await Analytics.find({
      eventType: 'suspicious_activity',
      timestamp: { $gte: new Date(Date.now() - 24 * 60 * 60 * 1000) } // Last 24 hours
    })
      .sort({ timestamp: -1 })
      .limit(20);
    
    // Get top suspicious users
    const topSuspiciousUsers = await Analytics.aggregate([
      {
        $match: {
          eventType: 'suspicious_activity',
          'eventData.userId': { $exists: true }
        }
      },
      {
        $group: {
          _id: '$eventData.userId',
          count: { $sum: 1 }
        }
      },
      {
        $sort: { count: -1 }
      },
      {
        $limit: 10
      }
    ]);
    
    // Get top suspicious IPs
    const topSuspiciousIps = await Analytics.aggregate([
      {
        $match: {
          eventType: 'suspicious_activity',
          'eventData.ip': { $exists: true }
        }
      },
      {
        $group: {
          _id: '$eventData.ip',
          count: { $sum: 1 }
        }
      },
      {
        $sort: { count: -1 }
      },
      {
        $limit: 10
      }
    ]);
    
    // Return dashboard data
    return {
      activityCounts,
      severityCounts,
      recentActivities,
      topSuspiciousUsers,
      topSuspiciousIps
    };
  } catch (error) {
    console.error('Error getting dashboard data:', error);
    return {
      error: error.message
    };
  }
}

module.exports = {
  ACTIVITY_TYPES,
  SEVERITY_LEVELS,
  trackActivity,
  getActivities,
  getReputationScore,
  isSuspicious,
  analyzeTrafficPatterns,
  getDashboardData
}; 