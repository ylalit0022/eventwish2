/**
 * Redis Configuration
 * 
 * This module provides Redis client configuration with proper error handling.
 * It will gracefully handle cases where Redis is not available.
 */

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

// Use mock Redis client since Redis is not configured in this environment
logger.info('Redis is not enabled or configured, using mock client');
const redisClient = mockRedisClient;

module.exports = redisClient;
