/**
 * Inactivity Notifications Cron Job
 * 
 * This job can run automatically on schedule or be triggered manually.
 * It uses the pushNotificationService to find users who haven't been active 
 * for a configurable number of days and sends them personalized notifications
 * to encourage them to return to the app.
 */

const logger = require('../config/logger');
const pushNotificationService = require('../services/pushNotificationService');
const mongoose = require('mongoose');
const Config = require('../models/Config');

// Default configuration
const DEFAULT_CONFIG = {
  inactivityThresholdDays: 3,
  isAutomaticSendEnabled: true,
  automaticSendHour: 10, // 10 AM
  lastRun: null,
  notificationTitle: 'Hey {{displayName}}, we miss you!',
  notificationBody: 'Check out new content since your last visit {{lastOnline}}',
};

/**
 * Get inactivity notification configuration
 * @returns {Promise<Object>} Configuration object
 */
async function getInactivityConfig() {
  try {
    // Try to get config from database
    let config = await Config.findOne({ key: 'inactivityNotifications' });
    
    // If config doesn't exist, create it with defaults
    if (!config) {
      config = new Config({
        key: 'inactivityNotifications',
        value: DEFAULT_CONFIG,
        description: 'Configuration for inactivity notifications'
      });
      await config.save();
      logger.info('Created default inactivity notification config');
    }
    
    return config.value;
  } catch (error) {
    logger.error('Error getting inactivity config, using defaults', { error });
    return DEFAULT_CONFIG;
  }
}

/**
 * Update inactivity notification configuration
 * @param {Object} newConfig - New configuration values
 * @param {string} updatedBy - User who updated the config
 * @returns {Promise<Object>} Updated configuration
 */
async function updateInactivityConfig(newConfig, updatedBy = 'system') {
  try {
    // Get existing config or create new one
    let config = await Config.findOne({ key: 'inactivityNotifications' });
    
    if (!config) {
      config = new Config({
        key: 'inactivityNotifications',
        value: { ...DEFAULT_CONFIG },
        description: 'Configuration for inactivity notifications'
      });
    }
    
    // Update with new values, preserving defaults for any missing fields
    config.value = {
      ...config.value,
      ...newConfig
    };
    
    config.lastUpdated = new Date();
    config.updatedBy = updatedBy;
    
    await config.save();
    logger.info('Updated inactivity notification config', { 
      updatedBy, 
      newConfig: JSON.stringify(newConfig) 
    });
    
    return config.value;
  } catch (error) {
    logger.error('Error updating inactivity config', { error, newConfig });
    throw error;
  }
}

/**
 * Main job function to send inactivity notifications
 * @param {Object} options - Optional parameters
 * @param {number} options.inactivityThresholdDays - Days of inactivity to trigger notification
 * @param {boolean} options.forceRun - Force run even if automatic sending is disabled
 * @param {string} options.triggeredBy - Who triggered the job (system or user ID)
 * @returns {Promise<Object>} Result of sending notifications
 */
async function runInactivityNotificationsJob(options = {}) {
  try {
    const triggeredBy = options.triggeredBy || 'system';
    logger.info(`Starting inactivity notifications job (triggered by: ${triggeredBy})`);
    
    // Get configuration
    const config = await getInactivityConfig();
    
    // Check if automatic sending is enabled (unless force run)
    if (!config.isAutomaticSendEnabled && !options.forceRun && triggeredBy === 'system') {
      logger.info('Automatic inactivity notifications are disabled, skipping job');
      return { 
        success: true, 
        message: 'Automatic inactivity notifications are disabled', 
        skipped: true 
      };
    }
    
    // Use provided threshold or config value
    const inactivityThresholdDays = options.inactivityThresholdDays || 
                                   config.inactivityThresholdDays || 
                                   DEFAULT_CONFIG.inactivityThresholdDays;
    
    // Send notifications
    const result = await pushNotificationService.sendInactivityNotifications(inactivityThresholdDays);
    
    // Update last run timestamp
    await updateInactivityConfig({ 
      lastRun: new Date().toISOString()
    }, triggeredBy);
    
    if (result.success) {
      logger.info('Inactivity notifications job completed successfully', {
        userCount: result.userCount || 0,
        successCount: result.successCount || 0,
        failureCount: result.failureCount || 0,
        notificationId: result.notificationId,
        inactivityThresholdDays,
        triggeredBy
      });
    } else {
      logger.warn('Inactivity notifications job completed with warnings', {
        error: result.error,
        message: result.message,
        inactivityThresholdDays,
        triggeredBy
      });
    }
    
    return result;
  } catch (error) {
    logger.error('Error running inactivity notifications job', { 
      error, 
      options 
    });
    throw error;
  }
}

// Export functions for use in the scheduler and API
module.exports = {
  runInactivityNotificationsJob,
  getInactivityConfig,
  updateInactivityConfig
}; 