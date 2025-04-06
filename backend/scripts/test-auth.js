/**
 * Authentication Middleware Test Script
 * 
 * This script tests the authentication middleware by making requests to
 * protected endpoints with and without the API key.
 * 
 * Run with: node scripts/test-auth.js
 */

const axios = require('axios');
require('dotenv').config();

// Configuration
const BASE_URL = 'http://localhost:3000'; // Change to your server URL
const API_KEY = process.env.API_KEY;
const INTERNAL_API_KEY = process.env.INTERNAL_API_KEY;

// Test endpoints
const endpoints = [
  { url: '/api/health', auth: false, description: 'Public health endpoint' },
  { url: '/api/health/database', auth: true, description: 'Protected database health endpoint' },
  { url: '/api/health/admob', auth: true, description: 'Protected AdMob health endpoint' },
  { url: '/api/admob/config', auth: false, description: 'Public AdMob config endpoint' }
];

/**
 * Makes a request to the specified endpoint
 * @param {string} url - The endpoint URL
 * @param {boolean} useAuth - Whether to include the API key
 * @param {string} description - Description of the endpoint
 * @returns {Promise} - Promise resolving to the test result
 */
async function testEndpoint(url, useAuth, description) {
  const fullUrl = `${BASE_URL}${url}`;
  const headers = useAuth ? { 'x-api-key': API_KEY } : {};
  
  console.log(`\nTesting: ${description}`);
  console.log(`URL: ${fullUrl}`);
  console.log(`Auth: ${useAuth ? 'Yes' : 'No'}`);
  
  try {
    const response = await axios.get(fullUrl, { headers });
    return {
      success: true,
      status: response.status,
      data: response.data,
      message: 'Request successful'
    };
  } catch (error) {
    return {
      success: false,
      status: error.response?.status || 500,
      data: error.response?.data || {},
      message: error.message
    };
  }
}

/**
 * Tests all endpoints
 */
async function runTests() {
  console.log('=== AUTHENTICATION MIDDLEWARE TEST ===');
  console.log(`Server: ${BASE_URL}`);
  console.log(`API Key: ${API_KEY ? API_KEY.substring(0, 4) + '...' : 'Not set'}`);
  
  // Test each endpoint with correct authentication
  console.log('\n--- TESTING WITH CORRECT AUTHENTICATION ---');
  for (const endpoint of endpoints) {
    const result = await testEndpoint(endpoint.url, endpoint.auth, endpoint.description);
    console.log(`Status: ${result.status}`);
    console.log(`Success: ${result.success}`);
    console.log(`Message: ${result.message}`);
    if (result.data && typeof result.data === 'object') {
      console.log('Response:', JSON.stringify(result.data, null, 2).substring(0, 200) + '...');
    }
  }
  
  // Test protected endpoints without authentication
  console.log('\n--- TESTING PROTECTED ENDPOINTS WITHOUT AUTHENTICATION ---');
  for (const endpoint of endpoints) {
    if (endpoint.auth) {
      const result = await testEndpoint(endpoint.url, false, `${endpoint.description} (without auth)`);
      console.log(`Status: ${result.status}`);
      console.log(`Success: ${result.success}`);
      console.log(`Message: ${result.message}`);
      if (result.data && typeof result.data === 'object') {
        console.log('Response:', JSON.stringify(result.data, null, 2).substring(0, 200) + '...');
      }
    }
  }
  
  // Test with incorrect API key
  console.log('\n--- TESTING WITH INCORRECT API KEY ---');
  for (const endpoint of endpoints) {
    if (endpoint.auth) {
      const fullUrl = `${BASE_URL}${endpoint.url}`;
      const headers = { 'x-api-key': 'invalid_api_key' };
      
      console.log(`\nTesting: ${endpoint.description} (invalid key)`);
      console.log(`URL: ${fullUrl}`);
      
      try {
        const response = await axios.get(fullUrl, { headers });
        console.log(`Status: ${response.status}`);
        console.log(`Success: true (unexpected)`);
        console.log('Response:', JSON.stringify(response.data, null, 2).substring(0, 200) + '...');
      } catch (error) {
        console.log(`Status: ${error.response?.status || 500}`);
        console.log(`Success: false (expected)`);
        console.log(`Message: ${error.message}`);
        if (error.response?.data) {
          console.log('Response:', JSON.stringify(error.response.data, null, 2).substring(0, 200) + '...');
        }
      }
    }
  }
  
  console.log('\n=== TEST SUMMARY ===');
  console.log('All tests completed. Check the results above to verify authentication middleware is working correctly.');
  console.log('If protected endpoints return 401 without authentication and 200 with authentication, the middleware is working correctly.');
}

// Run the tests
runTests().catch(error => {
  console.error('Error running tests:', error.message);
}); 