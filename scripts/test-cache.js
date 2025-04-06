/**
 * Cache Service Test Script
 * 
 * This script tests the cacheService functionality to ensure it works correctly.
 * 
 * Run with: node scripts/test-cache.js
 */

const cacheService = require('../services/cacheService');

// Test basic functionality
console.log('=== TESTING CACHE SERVICE ===');

// Create namespaced caches
const userCache = cacheService.namespace('users');
const configCache = cacheService.namespace('config');

// Test setting and getting values
console.log('\n--- Testing Basic Set/Get ---');
userCache.set('user1', { id: 1, name: 'John Doe' });
configCache.set('theme', 'dark');

console.log('User cache - user1:', userCache.get('user1'));
console.log('Config cache - theme:', configCache.get('theme'));

// Test namespace isolation
console.log('\n--- Testing Namespace Isolation ---');
console.log('User cache has theme?', userCache.has('theme'));
console.log('Config cache has user1?', configCache.has('user1'));

// Test keys method
console.log('\n--- Testing Keys Method ---');
console.log('User cache keys:', userCache.keys());
console.log('Config cache keys:', configCache.keys());

// Test stats
console.log('\n--- Testing Stats ---');
console.log('User cache stats:', userCache.stats());
console.log('Global cache stats:', cacheService.getStats());

// Test deletion
console.log('\n--- Testing Deletion ---');
console.log('Deleting user1:', userCache.del('user1'));
console.log('User1 after deletion:', userCache.get('user1'));

// Test flushing
console.log('\n--- Testing Flush ---');
configCache.set('setting1', 'value1');
configCache.set('setting2', 'value2');
console.log('Config cache keys before flush:', configCache.keys());
console.log('Flushing config cache:', configCache.flush());
console.log('Config cache keys after flush:', configCache.keys());

// Test TTL
console.log('\n--- Testing TTL (short timeout) ---');
const ttlCache = cacheService.namespace('ttl');
ttlCache.set('shortLived', 'This will expire soon', 2); // 2 seconds TTL
console.log('Short lived value (initial):', ttlCache.get('shortLived'));

setTimeout(() => {
  console.log('Short lived value (after 3s):', ttlCache.get('shortLived'));
  console.log('\n=== CACHE SERVICE TEST COMPLETE ===');
}, 3000);

// Keep the script running for the timeout
console.log('Waiting for TTL test to complete...'); 