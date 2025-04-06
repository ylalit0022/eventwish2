const { messaging } = require('../config/firebase');
const DeviceToken = require('../models/DeviceToken');
const FlashyMessage = require('../models/FlashyMessage');

/**
 * Send a notification to a single device
 * @param {string} token - FCM token
 * @param {object} notification - Notification object with title and body
 * @param {object} data - Data payload
 * @returns {Promise} - Promise that resolves with the messaging response
 */
const sendToDevice = async (token, notification, data = {}) => {
  try {
    const message = {
      token,
      notification,
      data,
      android: {
        priority: 'high',
        notification: {
          channelId: 'fcm_channel'
        }
      }
    };
    
    const response = await messaging().send(message);
    console.log('Successfully sent message:', response);
    
    // Update last used timestamp
    await DeviceToken.findOneAndUpdate(
      { token },
      { lastUsed: new Date() }
    );
    
    return response;
  } catch (error) {
    console.error('Error sending message:', error);
    
    // If the token is invalid, mark it as inactive
    if (error.code === 'messaging/invalid-registration-token' ||
        error.code === 'messaging/registration-token-not-registered') {
      await DeviceToken.findOneAndUpdate(
        { token },
        { active: false }
      );
    }
    
    throw error;
  }
};

/**
 * Send a notification to multiple devices
 * @param {array} tokens - Array of FCM tokens
 * @param {object} notification - Notification object with title and body
 * @param {object} data - Data payload
 * @returns {Promise} - Promise that resolves with the messaging response
 */
const sendToDevices = async (tokens, notification, data = {}) => {
  try {
    if (!tokens || tokens.length === 0) {
      console.warn('No tokens provided for sending notification');
      return null;
    }
    
    // Firebase can only send to 500 tokens at a time
    const maxTokensPerRequest = 500;
    const tokenBatches = [];
    
    // Split tokens into batches of 500
    for (let i = 0; i < tokens.length; i += maxTokensPerRequest) {
      tokenBatches.push(tokens.slice(i, i + maxTokensPerRequest));
    }
    
    // Send to each batch
    const responses = await Promise.all(
      tokenBatches.map(async (batchTokens) => {
        const message = {
          tokens: batchTokens,
          notification,
          data,
          android: {
            priority: 'high',
            notification: {
              channelId: 'fcm_channel'
            }
          }
        };
        
        return messaging().sendMulticast(message);
      })
    );
    
    // Update last used timestamp for all tokens
    await DeviceToken.updateMany(
      { token: { $in: tokens } },
      { lastUsed: new Date() }
    );
    
    // Process responses to find failed tokens
    const failedTokens = [];
    
    responses.forEach((response, batchIndex) => {
      if (response.failureCount > 0) {
        response.responses.forEach((resp, index) => {
          if (!resp.success) {
            const failedToken = tokenBatches[batchIndex][index];
            failedTokens.push({
              token: failedToken,
              error: resp.error
            });
            
            // If the token is invalid, mark it as inactive
            if (resp.error.code === 'messaging/invalid-registration-token' ||
                resp.error.code === 'messaging/registration-token-not-registered') {
              DeviceToken.findOneAndUpdate(
                { token: failedToken },
                { active: false }
              ).exec();
            }
          }
        });
      }
    });
    
    if (failedTokens.length > 0) {
      console.warn(`${failedTokens.length} tokens failed:`, failedTokens);
    }
    
    return responses;
  } catch (error) {
    console.error('Error sending multicast message:', error);
    throw error;
  }
};

/**
 * Send a notification to a topic
 * @param {string} topic - Topic name
 * @param {object} notification - Notification object with title and body
 * @param {object} data - Data payload
 * @returns {Promise} - Promise that resolves with the messaging response
 */
const sendToTopic = async (topic, notification, data = {}) => {
  try {
    const message = {
      topic,
      notification,
      data,
      android: {
        priority: 'high',
        notification: {
          channelId: 'fcm_channel'
        }
      }
    };
    
    const response = await messaging().send(message);
    console.log('Successfully sent message to topic:', response);
    return response;
  } catch (error) {
    console.error('Error sending message to topic:', error);
    throw error;
  }
};

/**
 * Send a flashy message to all active devices
 * @param {object} flashyMessage - FlashyMessage object
 * @returns {Promise} - Promise that resolves with the messaging response
 */
const sendFlashyMessage = async (flashyMessage) => {
  try {
    // Get all active tokens
    const devices = await DeviceToken.find({ active: true });
    const tokens = devices.map(device => device.token);
    
    if (tokens.length === 0) {
      console.warn('No active tokens found for sending flashy message');
      return null;
    }
    
    // Create data payload
    const data = {
      type: 'flashy_message',
      title: flashyMessage.title,
      message: flashyMessage.message,
      message_id: flashyMessage._id.toString()
    };
    
    // Send to all devices
    return sendToDevices(tokens, null, data);
  } catch (error) {
    console.error('Error sending flashy message:', error);
    throw error;
  }
};

/**
 * Get all active flashy messages
 * @returns {Promise} - Promise that resolves with an array of active flashy messages
 */
const getActiveFlashyMessages = async () => {
  const now = new Date();
  
  return FlashyMessage.find({
    active: true,
    startDate: { $lte: now },
    endDate: { $gte: now }
  }).sort({ priority: -1 });
};

/**
 * Create a new flashy message
 * @param {object} messageData - Flashy message data
 * @returns {Promise} - Promise that resolves with the created flashy message
 */
const createFlashyMessage = async (messageData) => {
  try {
    const flashyMessage = new FlashyMessage(messageData);
    await flashyMessage.save();
    
    // Send the flashy message to all devices
    if (flashyMessage.active) {
      await sendFlashyMessage(flashyMessage);
    }
    
    return flashyMessage;
  } catch (error) {
    console.error('Error creating flashy message:', error);
    throw error;
  }
};

module.exports = {
  sendToDevice,
  sendToDevices,
  sendToTopic,
  sendFlashyMessage,
  getActiveFlashyMessages,
  createFlashyMessage
}; 