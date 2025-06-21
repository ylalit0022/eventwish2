const admin = require('firebase-admin');
const logger = require('../config/logger');
const User = require('../models/User');
const PushNotification = require('../models/PushNotification');

/**
 * Send a push notification to a specific FCM token
 * @param {string} token - FCM token to send to
 * @param {object} notification - Notification payload
 * @param {string} notification.title - Notification title
 * @param {string} notification.body - Notification body
 * @param {string} [notification.imageUrl] - Optional image URL
 * @param {object} [data] - Optional data payload
 * @returns {Promise<object>} - FCM response
 */
const sendToToken = async (token, notification, data = {}) => {
  try {
    const message = {
      token,
      notification: {
        title: notification.title,
        body: notification.body,
        imageUrl: notification.imageUrl
      },
      data,
      android: {
        notification: {
          sound: 'default',
          priority: 'high',
          clickAction: 'FLUTTER_NOTIFICATION_CLICK'
        }
      },
      apns: {
        payload: {
          aps: {
            sound: 'default',
            category: 'NOTIFICATION'
          }
        }
      }
    };

    const response = await admin.messaging().send(message);
    logger.info(`Successfully sent message to token: ${token}`, { messageId: response });
    return { success: true, messageId: response };
  } catch (error) {
    logger.error(`Error sending message to token: ${token}`, { error });
    return { success: false, error: error.message };
  }
};

/**
 * Send a push notification to multiple FCM tokens
 * @param {string[]} tokens - Array of FCM tokens
 * @param {object} notification - Notification payload
 * @param {string} notification.title - Notification title
 * @param {string} notification.body - Notification body
 * @param {string} [notification.imageUrl] - Optional image URL
 * @param {object} [data] - Optional data payload
 * @returns {Promise<object>} - FCM response with success and failure counts
 */
const sendToTokens = async (tokens, notification, data = {}) => {
  try {
    if (!tokens || tokens.length === 0) {
      return { success: false, error: 'No tokens provided' };
    }

    const message = {
      tokens,
      notification: {
        title: notification.title,
        body: notification.body,
        imageUrl: notification.imageUrl
      },
      data,
      android: {
        notification: {
          sound: 'default',
          priority: 'high',
          clickAction: 'FLUTTER_NOTIFICATION_CLICK'
        }
      },
      apns: {
        payload: {
          aps: {
            sound: 'default',
            category: 'NOTIFICATION'
          }
        }
      }
    };

    const response = await admin.messaging().sendMulticast(message);
    logger.info(`Multicast message sent with success: ${response.successCount}/${tokens.length}`, {
      successCount: response.successCount,
      failureCount: response.failureCount
    });

    return {
      success: true,
      successCount: response.successCount,
      failureCount: response.failureCount,
      responses: response.responses
    };
  } catch (error) {
    logger.error('Error sending multicast message', { error });
    return { success: false, error: error.message };
  }
};

/**
 * Send a push notification to a topic
 * @param {string} topic - Topic to send to
 * @param {object} notification - Notification payload
 * @param {string} notification.title - Notification title
 * @param {string} notification.body - Notification body
 * @param {string} [notification.imageUrl] - Optional image URL
 * @param {object} [data] - Optional data payload
 * @returns {Promise<object>} - FCM response
 */
const sendToTopic = async (topic, notification, data = {}) => {
  try {
    // Format topic name (remove spaces, lowercase)
    const formattedTopic = topic.replace(/\s+/g, '_').toLowerCase();

    const message = {
      topic: formattedTopic,
      notification: {
        title: notification.title,
        body: notification.body,
        imageUrl: notification.imageUrl
      },
      data,
      android: {
        notification: {
          sound: 'default',
          priority: 'high',
          clickAction: 'FLUTTER_NOTIFICATION_CLICK'
        }
      },
      apns: {
        payload: {
          aps: {
            sound: 'default',
            category: 'NOTIFICATION'
          }
        }
      }
    };

    const response = await admin.messaging().send(message);
    logger.info(`Successfully sent message to topic: ${formattedTopic}`, { messageId: response });
    return { success: true, messageId: response };
  } catch (error) {
    logger.error(`Error sending message to topic: ${topic}`, { error });
    return { success: false, error: error.message };
  }
};

/**
 * Subscribe a token to a topic
 * @param {string} token - FCM token
 * @param {string} topic - Topic to subscribe to
 * @returns {Promise<object>} - FCM response
 */
const subscribeToTopic = async (token, topic) => {
  try {
    // Format topic name (remove spaces, lowercase)
    const formattedTopic = topic.replace(/\s+/g, '_').toLowerCase();
    
    const response = await admin.messaging().subscribeToTopic(token, formattedTopic);
    logger.info(`Successfully subscribed token to topic: ${formattedTopic}`, response);
    return { success: true, response };
  } catch (error) {
    logger.error(`Error subscribing token to topic: ${topic}`, { error });
    return { success: false, error: error.message };
  }
};

/**
 * Unsubscribe a token from a topic
 * @param {string} token - FCM token
 * @param {string} topic - Topic to unsubscribe from
 * @returns {Promise<object>} - FCM response
 */
const unsubscribeFromTopic = async (token, topic) => {
  try {
    // Format topic name (remove spaces, lowercase)
    const formattedTopic = topic.replace(/\s+/g, '_').toLowerCase();
    
    const response = await admin.messaging().unsubscribeFromTopic(token, formattedTopic);
    logger.info(`Successfully unsubscribed token from topic: ${formattedTopic}`, response);
    return { success: true, response };
  } catch (error) {
    logger.error(`Error unsubscribing token from topic: ${topic}`, { error });
    return { success: false, error: error.message };
  }
};

/**
 * Get all FCM tokens from users
 * @returns {Promise<string[]>} - Array of FCM tokens
 */
const getAllTokens = async () => {
  try {
    const users = await User.find({ 'fcmTokens.token': { $exists: true, $ne: '' } })
      .select('fcmTokens');
    
    // Extract tokens from users
    const tokens = [];
    users.forEach(user => {
      if (user.fcmTokens && user.fcmTokens.length > 0) {
        user.fcmTokens.forEach(tokenObj => {
          if (tokenObj.token) {
            tokens.push(tokenObj.token);
          }
        });
      }
    });
    
    return tokens;
  } catch (error) {
    logger.error('Error getting all tokens', { error });
    throw error;
  }
};

/**
 * Get tokens for users subscribed to a specific topic
 * @param {string} topic - Topic to get tokens for
 * @returns {Promise<string[]>} - Array of FCM tokens
 */
const getTokensByTopic = async (topic) => {
  try {
    // Format topic name (remove spaces, lowercase)
    const formattedTopic = topic.replace(/\s+/g, '_').toLowerCase();
    
    const users = await User.find({ 
      'fcmTokens.subscribedTopics': formattedTopic,
      'fcmTokens.token': { $exists: true, $ne: '' }
    }).select('fcmTokens');
    
    // Extract tokens from users that are subscribed to the topic
    const tokens = [];
    users.forEach(user => {
      if (user.fcmTokens && user.fcmTokens.length > 0) {
        user.fcmTokens.forEach(tokenObj => {
          if (tokenObj.token && tokenObj.subscribedTopics && 
              tokenObj.subscribedTopics.includes(formattedTopic)) {
            tokens.push(tokenObj.token);
          }
        });
      }
    });
    
    return tokens;
  } catch (error) {
    logger.error(`Error getting tokens by topic: ${topic}`, { error });
    throw error;
  }
};

/**
 * Get tokens for specific users
 * @param {string[]} userIds - Array of user IDs
 * @returns {Promise<string[]>} - Array of FCM tokens
 */
const getTokensByUsers = async (userIds) => {
  try {
    const users = await User.find({ 
      _id: { $in: userIds },
      'fcmTokens.token': { $exists: true, $ne: '' }
    }).select('fcmTokens');
    
    // Extract tokens from users
    const tokens = [];
    users.forEach(user => {
      if (user.fcmTokens && user.fcmTokens.length > 0) {
        user.fcmTokens.forEach(tokenObj => {
          if (tokenObj.token) {
            tokens.push(tokenObj.token);
          }
        });
      }
    });
    
    return tokens;
  } catch (error) {
    logger.error('Error getting tokens by users', { error });
    throw error;
  }
};

/**
 * Process placeholders in a string with user data
 * @param {string} template - Template string with placeholders like {{displayName}}
 * @param {object} userData - User data object with values for placeholders
 * @returns {string} - Processed string with placeholders replaced
 */
const processPlaceholders = (template, userData) => {
  if (!template) return '';
  
  // Replace placeholders with user data
  return template.replace(/\{\{([^}]+)\}\}/g, (match, key) => {
    // Handle nested properties with dot notation (e.g., "categories.0.category")
    const value = key.split('.').reduce((obj, prop) => {
      return obj && obj[prop] !== undefined ? obj[prop] : null;
    }, userData);
    
    return value !== null && value !== undefined ? value : match;
  });
};

/**
 * Check if user can receive notifications
 * @param {object} user - User document
 * @returns {boolean} - Whether user can receive notifications
 */
const canReceiveNotifications = (user) => {
  // Check if user is blocked
  if (user.isBlocked) return false;
  
  // Check if notifications are muted
  if (user.muteNotificationsUntil && new Date(user.muteNotificationsUntil) > new Date()) {
    return false;
  }
  
  // Check if user has allowed personal push
  if (user.pushPreferences && user.pushPreferences.allowPersonalPush === false) {
    return false;
  }
  
  return true;
};

/**
 * Send personalized notifications to users
 * @param {object} notification - Notification object from database
 * @param {string[]} userIds - Array of user IDs to send to
 * @returns {Promise<object>} - Results of sending
 */
const sendPersonalizedNotifications = async (notification, userIds) => {
  try {
    // Get users with their data for personalization
    const users = await User.find({
      _id: { $in: userIds },
      'fcmTokens.token': { $exists: true, $ne: '' }
    });
    
    if (!users || users.length === 0) {
      logger.warn(`No eligible users found for personalized notification: ${notification._id}`);
      return { success: false, error: 'No eligible users found' };
    }
    
    let successCount = 0;
    let failureCount = 0;
    const errorLog = [];
    const batchSize = 500; // Firebase has a limit of 500 messages per batch
    
    // Process users in batches
    for (let i = 0; i < users.length; i += batchSize) {
      const userBatch = users.slice(i, i + batchSize);
      const messages = [];
      
      // Prepare personalized messages for each user
      for (const user of userBatch) {
        // Skip users who can't receive notifications
        if (!canReceiveNotifications(user)) {
          logger.info(`Skipping notification for user ${user._id} due to preferences/status`);
          continue;
        }
        
        // Get user's FCM tokens
        const tokens = user.fcmTokens
          .filter(tokenObj => tokenObj.token)
          .map(tokenObj => tokenObj.token);
        
        if (!tokens.length) continue;
        
        // Process placeholders in title and body
        const personalizedTitle = processPlaceholders(notification.title, user);
        const personalizedBody = processPlaceholders(notification.body, user);
        
        // Prepare data payload
        const data = {
          ...(notification.data || {}),
          notificationId: notification._id.toString(),
          deepLink: notification.deepLink || '',
          type: notification.notificationType || 'GENERAL'
        };
        
        // Add message for each token
        for (const token of tokens) {
          messages.push({
            token,
            notification: {
              title: personalizedTitle,
              body: personalizedBody,
              imageUrl: notification.imageUrl
            },
            data,
            android: {
              notification: {
                sound: 'default',
                priority: 'high',
                clickAction: 'FLUTTER_NOTIFICATION_CLICK'
              }
            },
            apns: {
              payload: {
                aps: {
                  sound: 'default',
                  category: 'NOTIFICATION'
                }
              }
            }
          });
        }
      }
      
      if (messages.length === 0) continue;
      
      // Send messages in this batch
      try {
        const response = await admin.messaging().sendEach(messages);
        successCount += response.successCount;
        failureCount += response.failureCount;
        
        // Log errors
        if (response.failureCount > 0) {
          response.responses.forEach((resp, idx) => {
            if (!resp.success) {
              const userIndex = Math.floor(idx / batchSize);
              const user = userBatch[userIndex < userBatch.length ? userIndex : 0];
              
              errorLog.push({
                userId: user._id,
                error: resp.error.message,
                timestamp: new Date(),
                fcmError: resp.error
              });
              
              logger.error(`Error sending personalized notification to user ${user._id}`, {
                error: resp.error
              });
            }
          });
        }
      } catch (error) {
        failureCount += messages.length;
        logger.error(`Error sending batch of personalized notifications`, { error });
        
        // Log a generic error for the batch
        errorLog.push({
          error: `Batch error: ${error.message}`,
          timestamp: new Date(),
          fcmError: error
        });
      }
    }
    
    // Update notification stats and error log in database
    await PushNotification.findByIdAndUpdate(notification._id, {
      $set: {
        status: successCount > 0 ? 'SENT' : 'FAILED',
        sentAt: new Date(),
        'stats.total': successCount + failureCount,
        'stats.success': successCount,
        'stats.failure': failureCount
      },
      $push: { errorLog: { $each: errorLog } }
    });
    
    return {
      success: successCount > 0,
      successCount,
      failureCount,
      errorCount: errorLog.length
    };
  } catch (error) {
    logger.error(`Error sending personalized notifications`, { error });
    return { success: false, error: error.message };
  }
};

/**
 * Send notifications to inactive users
 * @param {number} inactiveDays - Number of days of inactivity to trigger notification
 * @returns {Promise<Object>} Result object with success status and counts
 */
async function sendInactivityNotifications(inactiveDays = 3) {
  try {
    // Get inactivity notification configuration
    const inactivityNotificationsJob = require('../jobs/inactivityNotifications');
    const config = await inactivityNotificationsJob.getInactivityConfig();
    
    // Set cutoff date for inactivity
    const cutoffDate = new Date();
    cutoffDate.setDate(cutoffDate.getDate() - inactiveDays);
    
    logger.info(`Finding users inactive since ${cutoffDate.toISOString()}`);
    
    // Find users who haven't been active since cutoff date
    // and haven't received an inactivity notification in the last 7 days
    const lastNotificationCutoff = new Date();
    lastNotificationCutoff.setDate(lastNotificationCutoff.getDate() - 7);
    
    const inactiveUsers = await User.find({
      lastActive: { $lt: cutoffDate },
      $or: [
        { lastInactivityNotification: { $lt: lastNotificationCutoff } },
        { lastInactivityNotification: { $exists: false } }
      ],
      allowPush: true,
      fcmTokens: { $exists: true, $ne: [] }
    });
    
    if (inactiveUsers.length === 0) {
      logger.info('No inactive users found that need notifications');
      return { success: true, userCount: 0 };
    }
    
    logger.info(`Found ${inactiveUsers.length} inactive users`);
    
    // Create notification record
    const notificationTitle = config.notificationTitle || 'Hey {{displayName}}, we miss you!';
    const notificationBody = config.notificationBody || 'Check out new content since your last visit {{lastOnline}}';
    
    const notification = new PushNotification({
      title: notificationTitle,
      body: notificationBody,
      data: {
        type: 'INACTIVITY',
        deepLink: 'eventwish://home'
      },
      personalizationKeys: ['displayName', 'lastOnline', 'preferredLanguage', 'email'],
      isSent: false,
      sentAt: null,
      scheduledFor: new Date(),
      createdBy: 'system',
      notificationType: 'INACTIVITY',
      targetUserCount: inactiveUsers.length
    });
    
    await notification.save();
    
    // Send personalized notifications to each user
    let successCount = 0;
    let failureCount = 0;
    
    for (const user of inactiveUsers) {
      try {
        // Format lastOnline date for personalization
        let lastOnlineFormatted = 'a while ago';
        if (user.lastActive) {
          const lastActive = new Date(user.lastActive);
          lastOnlineFormatted = lastActive.toLocaleDateString();
        }
        
        // Create personalization data
        const personalizationData = {
          displayName: user.displayName || 'there',
          lastOnline: lastOnlineFormatted,
          preferredLanguage: user.preferredLanguage || 'en',
          email: user.email || ''
        };
        
        // Process placeholders
        const personalizedTitle = processPlaceholders(notification.title, personalizationData);
        const personalizedBody = processPlaceholders(notification.body, personalizationData);
        
        // Send notification
        const sent = await sendNotificationToUser(user, {
          title: personalizedTitle,
          body: personalizedBody,
          data: notification.data
        });
        
        if (sent) {
          successCount++;
          
          // Update user's lastInactivityNotification timestamp
          user.lastInactivityNotification = new Date();
          await user.save();
        } else {
          failureCount++;
          
          // Log error in notification record
          notification.errorLog.push({
            userId: user._id,
            error: 'Failed to send notification',
            timestamp: new Date()
          });
        }
      } catch (error) {
        failureCount++;
        logger.error(`Error sending inactivity notification to user ${user._id}:`, { error });
        
        // Log error in notification record
        notification.errorLog.push({
          userId: user._id,
          error: error.message,
          timestamp: new Date()
        });
      }
    }
    
    // Update notification record
    notification.isSent = true;
    notification.sentAt = new Date();
    notification.successCount = successCount;
    notification.failureCount = failureCount;
    await notification.save();
    
    logger.info(`Inactivity notifications sent: ${successCount} successful, ${failureCount} failed`);
    
    return {
      success: true,
      userCount: inactiveUsers.length,
      successCount,
      failureCount,
      notificationId: notification._id
    };
  } catch (error) {
    logger.error('Error sending inactivity notifications:', { error });
    return {
      success: false,
      error: error.message,
      message: 'Failed to send inactivity notifications'
    };
  }
}

module.exports = {
  sendToToken,
  sendToTokens,
  sendToTopic,
  subscribeToTopic,
  unsubscribeFromTopic,
  getAllTokens,
  getTokensByTopic,
  getTokensByUsers,
  processPlaceholders,
  canReceiveNotifications,
  sendPersonalizedNotifications,
  sendInactivityNotifications
}; 