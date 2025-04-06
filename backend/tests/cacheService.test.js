const cacheService = require('../services/cacheService');
const monitoringService = require('../services/monitoringService');

// Mock dependencies
jest.mock('../config/logger');
jest.mock('../services/monitoringService', () => ({
  trackCacheHit: jest.fn(),
  trackCacheMiss: jest.fn()
}));

describe('Cache Service', () => {
  beforeEach(() => {
    // Clear cache before each test
    cacheService.flushAll();
    
    // Reset mocks
    jest.clearAllMocks();
  });
  
  describe('Basic Cache Operations', () => {
    it('should set and get a value', () => {
      const key = 'test-key';
      const value = { name: 'Test Value', data: [1, 2, 3] };
      
      cacheService.set(key, value);
      const cachedValue = cacheService.get(key);
      
      expect(cachedValue).toEqual(value);
      expect(monitoringService.trackCacheHit).toHaveBeenCalledWith(key);
    });
    
    it('should return null for non-existent key', () => {
      const cachedValue = cacheService.get('non-existent-key');
      
      expect(cachedValue).toBeNull();
      expect(monitoringService.trackCacheMiss).toHaveBeenCalledWith('non-existent-key');
    });
    
    it('should set value with TTL and expire it', async () => {
      const key = 'expiring-key';
      const value = 'This will expire';
      
      // Set with 100ms TTL
      cacheService.set(key, value, 100);
      
      // Value should exist initially
      expect(cacheService.get(key)).toBe(value);
      
      // Wait for expiration
      await new Promise(resolve => setTimeout(resolve, 150));
      
      // Value should be expired
      expect(cacheService.get(key)).toBeNull();
      expect(monitoringService.trackCacheMiss).toHaveBeenCalledWith(key);
    });
    
    it('should check if key exists', () => {
      const key = 'existing-key';
      
      // Key should not exist initially
      expect(cacheService.has(key)).toBe(false);
      
      // Set a value
      cacheService.set(key, 'value');
      
      // Key should exist now
      expect(cacheService.has(key)).toBe(true);
    });
    
    it('should delete a key', () => {
      const key = 'delete-key';
      
      // Set a value
      cacheService.set(key, 'value');
      expect(cacheService.has(key)).toBe(true);
      
      // Delete the key
      cacheService.del(key);
      
      // Key should not exist anymore
      expect(cacheService.has(key)).toBe(false);
      expect(cacheService.get(key)).toBeNull();
    });
    
    it('should get multiple keys', () => {
      // Set multiple values
      cacheService.set('key1', 'value1');
      cacheService.set('key2', 'value2');
      cacheService.set('key3', 'value3');
      
      // Get multiple keys
      const values = cacheService.mget(['key1', 'key3', 'non-existent']);
      
      expect(values).toEqual({
        key1: 'value1',
        key3: 'value3',
        'non-existent': null
      });
      
      // Verify monitoring calls
      expect(monitoringService.trackCacheHit).toHaveBeenCalledWith('key1');
      expect(monitoringService.trackCacheHit).toHaveBeenCalledWith('key3');
      expect(monitoringService.trackCacheMiss).toHaveBeenCalledWith('non-existent');
    });
    
    it('should set multiple keys', () => {
      // Set multiple values
      cacheService.mset({
        key1: 'value1',
        key2: 'value2',
        key3: 'value3'
      });
      
      // Verify values
      expect(cacheService.get('key1')).toBe('value1');
      expect(cacheService.get('key2')).toBe('value2');
      expect(cacheService.get('key3')).toBe('value3');
    });
    
    it('should set multiple keys with TTL', async () => {
      // Set multiple values with TTL
      cacheService.mset({
        key1: 'value1',
        key2: 'value2'
      }, 100);
      
      // Values should exist initially
      expect(cacheService.get('key1')).toBe('value1');
      expect(cacheService.get('key2')).toBe('value2');
      
      // Wait for expiration
      await new Promise(resolve => setTimeout(resolve, 150));
      
      // Values should be expired
      expect(cacheService.get('key1')).toBeNull();
      expect(cacheService.get('key2')).toBeNull();
    });
  });
  
  describe('Cache Keys Management', () => {
    it('should list all keys', () => {
      // Set multiple values
      cacheService.set('key1', 'value1');
      cacheService.set('prefix:key2', 'value2');
      cacheService.set('prefix:key3', 'value3');
      
      // Get all keys
      const keys = cacheService.keys();
      
      expect(keys).toContain('key1');
      expect(keys).toContain('prefix:key2');
      expect(keys).toContain('prefix:key3');
      expect(keys.length).toBe(3);
    });
    
    it('should list keys by pattern', () => {
      // Set multiple values
      cacheService.set('key1', 'value1');
      cacheService.set('prefix:key2', 'value2');
      cacheService.set('prefix:key3', 'value3');
      cacheService.set('other:key4', 'value4');
      
      // Get keys by pattern
      const prefixKeys = cacheService.keys('prefix:*');
      
      expect(prefixKeys).toContain('prefix:key2');
      expect(prefixKeys).toContain('prefix:key3');
      expect(prefixKeys).not.toContain('key1');
      expect(prefixKeys).not.toContain('other:key4');
      expect(prefixKeys.length).toBe(2);
    });
    
    it('should flush all keys', () => {
      // Set multiple values
      cacheService.set('key1', 'value1');
      cacheService.set('key2', 'value2');
      
      // Flush all
      cacheService.flushAll();
      
      // No keys should exist
      expect(cacheService.keys().length).toBe(0);
      expect(cacheService.get('key1')).toBeNull();
      expect(cacheService.get('key2')).toBeNull();
    });
    
    it('should delete keys by pattern', () => {
      // Set multiple values
      cacheService.set('key1', 'value1');
      cacheService.set('prefix:key2', 'value2');
      cacheService.set('prefix:key3', 'value3');
      cacheService.set('other:key4', 'value4');
      
      // Delete keys by pattern
      cacheService.delByPattern('prefix:*');
      
      // Prefix keys should be deleted
      expect(cacheService.has('prefix:key2')).toBe(false);
      expect(cacheService.has('prefix:key3')).toBe(false);
      
      // Other keys should still exist
      expect(cacheService.has('key1')).toBe(true);
      expect(cacheService.has('other:key4')).toBe(true);
    });
  });
  
  describe('Cache Statistics', () => {
    it('should track cache size', () => {
      // Set multiple values
      cacheService.set('key1', 'value1');
      cacheService.set('key2', 'value2');
      
      // Get stats
      const stats = cacheService.getStats();
      
      expect(stats.keys).toBe(2);
      expect(stats.vsize).toBeGreaterThan(0);
    });
    
    it('should track hits and misses', () => {
      // Set a value
      cacheService.set('key1', 'value1');
      
      // Hit
      cacheService.get('key1');
      
      // Miss
      cacheService.get('non-existent');
      
      // Get stats
      const stats = cacheService.getStats();
      
      expect(stats.hits).toBe(1);
      expect(stats.misses).toBe(1);
    });
  });
  
  describe('Advanced Cache Features', () => {
    it('should support getOrSet for lazy loading', async () => {
      const key = 'lazy-key';
      const factory = jest.fn().mockResolvedValue('lazy-loaded-value');
      
      // First call should invoke factory
      const value1 = await cacheService.getOrSet(key, factory);
      expect(value1).toBe('lazy-loaded-value');
      expect(factory).toHaveBeenCalledTimes(1);
      expect(monitoringService.trackCacheMiss).toHaveBeenCalledWith(key);
      
      // Second call should use cached value
      const value2 = await cacheService.getOrSet(key, factory);
      expect(value2).toBe('lazy-loaded-value');
      expect(factory).toHaveBeenCalledTimes(1); // Still only called once
      expect(monitoringService.trackCacheHit).toHaveBeenCalledWith(key);
    });
    
    it('should support getOrSet with TTL', async () => {
      const key = 'lazy-expiring-key';
      const factory = jest.fn().mockResolvedValue('lazy-loaded-value');
      
      // First call should invoke factory
      const value1 = await cacheService.getOrSet(key, factory, 100);
      expect(value1).toBe('lazy-loaded-value');
      expect(factory).toHaveBeenCalledTimes(1);
      
      // Wait for expiration
      await new Promise(resolve => setTimeout(resolve, 150));
      
      // Second call should invoke factory again
      const value2 = await cacheService.getOrSet(key, factory, 100);
      expect(value2).toBe('lazy-loaded-value');
      expect(factory).toHaveBeenCalledTimes(2); // Called again after expiration
    });
    
    it('should handle factory errors in getOrSet', async () => {
      const key = 'error-key';
      const error = new Error('Factory error');
      const factory = jest.fn().mockRejectedValue(error);
      
      // Call should propagate error
      await expect(cacheService.getOrSet(key, factory)).rejects.toThrow('Factory error');
      expect(factory).toHaveBeenCalledTimes(1);
      
      // Key should not be cached
      expect(cacheService.has(key)).toBe(false);
    });
    
    it('should support touch to update TTL', async () => {
      const key = 'touch-key';
      
      // Set with short TTL
      cacheService.set(key, 'value', 100);
      
      // Wait some time
      await new Promise(resolve => setTimeout(resolve, 50));
      
      // Touch to extend TTL
      cacheService.touch(key, 200);
      
      // Wait past original expiration
      await new Promise(resolve => setTimeout(resolve, 100));
      
      // Value should still exist
      expect(cacheService.get(key)).toBe('value');
      
      // Wait for new expiration
      await new Promise(resolve => setTimeout(resolve, 100));
      
      // Value should be expired
      expect(cacheService.get(key)).toBeNull();
    });
    
    it('should support ttl to get remaining time', async () => {
      const key = 'ttl-key';
      
      // Set with TTL
      cacheService.set(key, 'value', 1000);
      
      // Get TTL
      const ttl = cacheService.ttl(key);
      
      // TTL should be close to 1000ms
      expect(ttl).toBeGreaterThan(900);
      expect(ttl).toBeLessThanOrEqual(1000);
      
      // Non-existent key should return null
      expect(cacheService.ttl('non-existent')).toBeNull();
    });
  });
  
  describe('Namespaced Cache', () => {
    it('should create namespaced cache', () => {
      const namespace = 'test-namespace';
      const namespacedCache = cacheService.namespace(namespace);
      
      // Set value in namespaced cache
      namespacedCache.set('key', 'value');
      
      // Value should be accessible with namespace prefix
      expect(cacheService.get(`${namespace}:key`)).toBe('value');
      
      // Value should be accessible through namespaced cache
      expect(namespacedCache.get('key')).toBe('value');
    });
    
    it('should isolate namespaced caches', () => {
      const namespace1 = 'namespace1';
      const namespace2 = 'namespace2';
      
      const cache1 = cacheService.namespace(namespace1);
      const cache2 = cacheService.namespace(namespace2);
      
      // Set values in different namespaces
      cache1.set('key', 'value1');
      cache2.set('key', 'value2');
      
      // Values should be different
      expect(cache1.get('key')).toBe('value1');
      expect(cache2.get('key')).toBe('value2');
      
      // Values should be accessible with namespace prefix
      expect(cacheService.get(`${namespace1}:key`)).toBe('value1');
      expect(cacheService.get(`${namespace2}:key`)).toBe('value2');
    });
    
    it('should flush namespaced cache', () => {
      const namespace = 'flush-namespace';
      const namespacedCache = cacheService.namespace(namespace);
      
      // Set values in namespaced cache and root cache
      namespacedCache.set('key1', 'value1');
      namespacedCache.set('key2', 'value2');
      cacheService.set('root-key', 'root-value');
      
      // Flush namespaced cache
      namespacedCache.flushAll();
      
      // Namespaced values should be gone
      expect(namespacedCache.get('key1')).toBeNull();
      expect(namespacedCache.get('key2')).toBeNull();
      
      // Root values should still exist
      expect(cacheService.get('root-key')).toBe('root-value');
    });
    
    it('should list keys in namespaced cache', () => {
      const namespace = 'keys-namespace';
      const namespacedCache = cacheService.namespace(namespace);
      
      // Set values in namespaced cache and root cache
      namespacedCache.set('key1', 'value1');
      namespacedCache.set('key2', 'value2');
      cacheService.set('root-key', 'root-value');
      
      // List keys in namespaced cache
      const keys = namespacedCache.keys();
      
      // Should only include namespaced keys
      expect(keys).toContain('key1');
      expect(keys).toContain('key2');
      expect(keys).not.toContain('root-key');
      expect(keys.length).toBe(2);
    });
  });
  
  describe('Cache Invalidation', () => {
    it('should invalidate single key', () => {
      // Set values
      cacheService.set('key1', 'value1');
      cacheService.set('key2', 'value2');
      
      // Invalidate one key
      cacheService.invalidate('key1');
      
      // Invalidated key should be gone
      expect(cacheService.has('key1')).toBe(false);
      
      // Other key should still exist
      expect(cacheService.has('key2')).toBe(true);
    });
    
    it('should invalidate multiple keys', () => {
      // Set values
      cacheService.set('key1', 'value1');
      cacheService.set('key2', 'value2');
      cacheService.set('key3', 'value3');
      
      // Invalidate multiple keys
      cacheService.invalidateMultiple(['key1', 'key3']);
      
      // Invalidated keys should be gone
      expect(cacheService.has('key1')).toBe(false);
      expect(cacheService.has('key3')).toBe(false);
      
      // Other key should still exist
      expect(cacheService.has('key2')).toBe(true);
    });
    
    it('should invalidate by pattern', () => {
      // Set values
      cacheService.set('prefix:key1', 'value1');
      cacheService.set('prefix:key2', 'value2');
      cacheService.set('other:key3', 'value3');
      
      // Invalidate by pattern
      cacheService.invalidateByPattern('prefix:*');
      
      // Invalidated keys should be gone
      expect(cacheService.has('prefix:key1')).toBe(false);
      expect(cacheService.has('prefix:key2')).toBe(false);
      
      // Other key should still exist
      expect(cacheService.has('other:key3')).toBe(true);
    });
  });
}); 