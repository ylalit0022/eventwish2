/**
 * Inactivity Notifications Utility for Admin Panel
 * 
 * This utility provides functions for managing inactivity notifications
 * through the API rather than directly accessing the database.
 */

import { 
  getInactivityNotificationConfig,
  updateInactivityNotificationConfig,
  triggerInactivityNotificationsManual
} from '../api';

// Default configuration to use when MongoDB is not available
const DEFAULT_CONFIG = {
  inactivityThresholdDays: 3,
  isAutomaticSendEnabled: true,
  automaticSendHour: 10,
  notificationTitle: 'Hey {{displayName}}, we miss you!',
  notificationBody: 'Check out new content since your last visit {{lastOnline}}'
};

/**
 * Get current inactivity notification configuration
 * @returns {Promise<Object>} Configuration object
 */
export async function getConfig() {
  try {
    const response = await getInactivityNotificationConfig();
    
    // Return the full response instead of just response.config
    // This ensures we're returning the correct structure with success flag
    return response;
  } catch (error) {
    console.error('Error getting inactivity notification config:', error);
    // Return a structured error response instead of throwing
    return {
      success: false,
      message: error.message || 'Failed to load inactivity notification configuration',
      config: DEFAULT_CONFIG
    };
  }
}

/**
 * Update inactivity notification configuration
 * @param {Object} configData - Configuration data to update
 * @returns {Promise<Object>} Updated configuration
 */
export async function updateConfig(configData) {
  try {
    const response = await updateInactivityNotificationConfig(configData);
    
    // Return the full response instead of just response.config
    return response;
  } catch (error) {
    console.error('Error updating inactivity notification config:', error);
    // Return a structured error response instead of throwing
    return {
      success: false,
      message: error.message || 'Failed to update inactivity notification configuration',
      // Return the original config data so UI can still display what the user entered
      config: configData
    };
  }
}

/**
 * Trigger inactivity notifications manually
 * @param {number} inactivityThresholdDays - Days of inactivity to trigger notifications for
 * @returns {Promise<Object>} Result object
 */
export async function triggerManually(inactivityThresholdDays) {
  try {
    const response = await triggerInactivityNotificationsManual(inactivityThresholdDays);
    return response;
  } catch (error) {
    console.error('Error triggering inactivity notifications:', error);
    // Return a structured error response instead of throwing
    return {
      success: false,
      message: error.message || 'Failed to trigger inactivity notifications'
    };
  }
}

// Default export for all functions
export default {
  getConfig,
  updateConfig,
  triggerManually
}; 