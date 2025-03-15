/**
 * Cache Service
 * 
 * Provides in-memory caching functionality with namespace support.
 * This service allows different parts of the application to cache data
 * in isolated namespaces to avoid key collisions.
 */

const NodeCache = require('node-cache');
const logger = require('../config/logger');

// Default TTL in seconds (30 minutes)
const DEFAULT_TTL = 30 * 60;

// Main cache instance
const mainCache = new NodeCache({
  stdTTL: DEFAULT_TTL,
  checkperiod: 120, // Check for expired keys every 2 minutes
  useClones: false // Store references to objects instead of cloning them
});

// Namespace cache map
const namespaces = new Map();

/**
 * Get a namespaced cache instance
 * @param {string} namespace - The namespace to use
 * @returns {Object} - A namespaced cache instance
 */
function namespace(namespace) {
  if (!namespace) {
    throw new Error('Namespace is required');
  }

  if (!namespaces.has(namespace)) {
    // Create a new namespaced cache
    namespaces.set(namespace, {
      /**
       * Set a value in the cache
       * @param {string} key - The key to store the value under
       * @param {*} value - The value to store
       * @param {number} [ttl] - Time to live in seconds
       * @returns {boolean} - True if the value was set successfully
       */
      set: (key, value, ttl = DEFAULT_TTL) => {
        const namespacedKey = `${namespace}:${key}`;
        const result = mainCache.set(namespacedKey, value, ttl);
        logger.debug(`Cache SET: ${namespacedKey}, TTL: ${ttl}s`);
        return result;
      },

      /**
       * Get a value from the cache
       * @param {string} key - The key to retrieve
       * @returns {*} - The cached value or undefined if not found
       */
      get: (key) => {
        const namespacedKey = `${namespace}:${key}`;
        const value = mainCache.get(namespacedKey);
        logger.debug(`Cache ${value !== undefined ? 'HIT' : 'MISS'}: ${namespacedKey}`);
        return value;
      },

      /**
       * Check if a key exists in the cache
       * @param {string} key - The key to check
       * @returns {boolean} - True if the key exists
       */
      has: (key) => {
        const namespacedKey = `${namespace}:${key}`;
        return mainCache.has(namespacedKey);
      },

      /**
       * Delete a value from the cache
       * @param {string} key - The key to delete
       * @returns {number} - Number of deleted entries
       */
      del: (key) => {
        const namespacedKey = `${namespace}:${key}`;
        const result = mainCache.del(namespacedKey);
        logger.debug(`Cache DEL: ${namespacedKey}`);
        return result;
      },

      /**
       * Flush all keys in this namespace
       * @returns {number} - Number of deleted entries
       */
      flush: () => {
        const keys = mainCache.keys().filter(key => key.startsWith(`${namespace}:`));
        const result = mainCache.del(keys);
        logger.debug(`Cache FLUSH: ${namespace}, Deleted: ${result} entries`);
        return result;
      },

      /**
       * Get all keys in this namespace
       * @returns {string[]} - Array of keys without the namespace prefix
       */
      keys: () => {
        const prefix = `${namespace}:`;
        return mainCache.keys()
          .filter(key => key.startsWith(prefix))
          .map(key => key.substring(prefix.length));
      },

      /**
       * Get stats for this namespace
       * @returns {Object} - Stats object
       */
      stats: () => {
        const keys = mainCache.keys().filter(key => key.startsWith(`${namespace}:`));
        return {
          keys: keys.length,
          hits: mainCache.getStats().hits,
          misses: mainCache.getStats().misses,
          namespace
        };
      }
    });
  }

  return namespaces.get(namespace);
}

/**
 * Flush all caches
 * @returns {number} - Number of deleted entries
 */
function flushAll() {
  const result = mainCache.flushAll();
  logger.debug('Cache FLUSH ALL');
  return result;
}

/**
 * Get cache stats
 * @returns {Object} - Cache stats
 */
function getStats() {
  return {
    ...mainCache.getStats(),
    namespaces: Array.from(namespaces.keys()),
    keys: mainCache.keys().length
  };
}

module.exports = {
  namespace,
  flushAll,
  getStats,
  DEFAULT_TTL
};
