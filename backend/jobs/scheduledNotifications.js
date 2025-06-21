/**
 * Scheduled Notifications Job
 * 
 * This job runs every minute to check for notifications that are scheduled to be sent
 * and processes them when their scheduled time arrives.
 */

const logger = require('../config/logger');
const PushNotification = require('../models/PushNotification');
const pushNotificationService = require('../services/pushNotificationService');

/**
 * Process notifications that are scheduled to be sent now
 */
async function processScheduledNotifications() {
  try {
    const now = new Date();
    
    // Find notifications that are scheduled for now or in the past and still in SCHEDULED status
    const scheduledNotifications = await PushNotification.find({
      status: 'SCHEDULED',
      scheduledFor: { $lte: now }
    });
    
    if (scheduledNotifications.length === 0) {
      return { success: true, message: 'No scheduled notifications to process', count: 0 };
    }
    
    logger.info(`Processing ${scheduledNotifications.length} scheduled notifications`);
    
    // Process each notification
    const results = [];
    for (const notification of scheduledNotifications) {
      try {
        // Update status to SENDING
        notification.status = 'SENDING';
        await notification.save();
        
        let result;
        
        // Process based on notification type
        switch (notification.type) {
          case 'BULK':
            // Get all tokens
            const tokens = await pushNotificationService.getAllTokens();
            result = await pushNotificationService.sendToTokens(tokens, {
              title: notification.title,
              body: notification.body,
              imageUrl: notification.imageUrl
            }, notification.data || {});
            break;
            
          case 'TOPIC':
            // Send to topic
            result = await pushNotificationService.sendToTopic(notification.topic, {
              title: notification.title,
              body: notification.body,
              imageUrl: notification.imageUrl
            }, notification.data || {});
            break;
            
          case 'PERSONALIZED':
            // Send personalized notifications
            result = await pushNotificationService.sendPersonalizedNotifications(
              notification,
              notification.targetUserIds
            );
            break;
            
          default:
            logger.warn(`Unknown notification type: ${notification.type}`);
            result = { success: false, error: `Unknown notification type: ${notification.type}` };
        }
        
        // If not handled by sendPersonalizedNotifications (which updates the notification itself)
        if (notification.type !== 'PERSONALIZED') {
          // Update notification status and stats
          notification.status = result.success ? 'SENT' : 'FAILED';
          notification.sentAt = new Date();
          
          if (result.successCount !== undefined) {
            notification.stats.total = result.successCount + (result.failureCount || 0);
            notification.stats.success = result.successCount;
            notification.stats.failure = result.failureCount || 0;
          } else {
            notification.stats.total = 1;
            notification.stats.success = result.success ? 1 : 0;
            notification.stats.failure = result.success ? 0 : 1;
          }
          
          // Add error to log if failed
          if (!result.success && result.error) {
            notification.errorLog.push({
              error: result.error,
              timestamp: new Date()
            });
          }
          
          await notification.save();
        }
        
        results.push({
          notificationId: notification._id,
          success: result.success,
          type: notification.type
        });
        
      } catch (error) {
        logger.error(`Error processing scheduled notification ${notification._id}`, { error });
        
        // Update notification as failed
        notification.status = 'FAILED';
        notification.sentAt = new Date();
        notification.errorLog.push({
          error: error.message,
          timestamp: new Date()
        });
        
        await notification.save();
        
        results.push({
          notificationId: notification._id,
          success: false,
          error: error.message
        });
      }
    }
    
    const successCount = results.filter(r => r.success).length;
    logger.info(`Processed ${scheduledNotifications.length} scheduled notifications: ${successCount} succeeded, ${scheduledNotifications.length - successCount} failed`);
    
    return {
      success: true,
      count: scheduledNotifications.length,
      successCount,
      failureCount: scheduledNotifications.length - successCount,
      results
    };
    
  } catch (error) {
    logger.error('Error processing scheduled notifications', { error });
    return { success: false, error: error.message };
  }
}

module.exports = {
  processScheduledNotifications
}; 