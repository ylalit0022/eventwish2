/**
 * Redis Configuration
 * 
 * This module provides Redis client configuration with proper error handling.
 * It will gracefully handle cases where Redis is not available.
 */

const { createClient } = require('redis');
const logger = require('./logger');

// Mock Redis client for environments where Redis is not available
const mockRedisClient = {
  isConnected: false,
  on: () => {},
  connect: async () => { return Promise.resolve(); },
  disconnect: async () => { return Promise.resolve(); },
  get: async () => { return null; },
  set: async () => { return null; },
  del: async () => { return null; },
  exists: async () => { return 0; },
  ping: async () => { return 'PONG'; }
};

// Initialize Redis client if Redis is enabled
let redisClient;

try {
  // Check if Redis is enabled and configuration is available
  if (process.env.REDIS_ENABLED === 'true' && process.env.REDIS_HOST) {
    redisClient = createClient({
      url: `redis://${process.env.REDIS_PASSWORD ? process.env.REDIS_PASSWORD + '@' : ''}${process.env.REDIS_HOST}:${process.env.REDIS_PORT || 6379}/${process.env.REDIS_DB || 0}`,
      socket: {
        reconnectStrategy: (retries) => {
          // Exponential backoff with max delay of 10 seconds
          const delay = Math.min(Math.pow(2, retries) * 100, 10000);
          logger.info(`Redis reconnecting in ${delay}ms...`);
          return delay;
        }
      }
    });

    // Set up event handlers
    redisClient.on('connect', () => {
      logger.info('Redis client connected');
      redisClient.isConnected = true;
    });

    redisClient.on('error', (err) => {
      logger.error(`Redis client error: ${err.message}`, { error: err });
      redisClient.isConnected = false;
    });

    redisClient.on('reconnecting', () => {
      logger.info('Redis client reconnecting');
      redisClient.isConnected = false;
    });

    redisClient.on('end', () => {
      logger.info('Redis client disconnected');
      redisClient.isConnected = false;
    });

    // Connect to Redis
    (async () => {
      try {
        await redisClient.connect();
      } catch (error) {
        logger.error(`Failed to connect to Redis: ${error.message}`, { error });
        // Fall back to mock client if connection fails
        redisClient = mockRedisClient;
      }
    })();
  } else {
    logger.info('Redis is not enabled or configured, using mock client');
    redisClient = mockRedisClient;
  }
} catch (error) {
  logger.error(`Error initializing Redis client: ${error.message}`, { error });
  redisClient = mockRedisClient;
}

module.exports = redisClient; 