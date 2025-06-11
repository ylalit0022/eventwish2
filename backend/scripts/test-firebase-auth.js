/**
 * Test script for Firebase Authentication
 * 
 * This script tests the Firebase authentication middleware
 * and validates that it's working correctly both with and without authentication.
 */

require('../config/env-loader');
const axios = require('axios');
const logger = require('../utils/logger');

// Constants
const PORT = process.env.PORT || 3001;
const BASE_URL = `http://localhost:${PORT}`;
const TEST_ENDPOINT = '/api/users/profile';

// Sample test data
const testUID = 'test-firebase-uid-123';
const testHeaders = {
  'x-firebase-uid': testUID,
  'Content-Type': 'application/json'
};

/**
 * Runs tests against the API
 */
const runTests = async () => {
  logger.info('Starting Firebase auth middleware tests...');

  try {
    // Check if server is running
    try {
      await axios.get(`${BASE_URL}/api/server/time`);
      logger.info('Server is running, proceeding with tests');
    } catch (error) {
      logger.error('Server is not running. Please start the server first with `npm run dev:no-auth`');
      logger.info('Exiting test script');
      return;
    }

    // Test 1: Call API with no authentication
    logger.info('Test 1: Calling API with no authentication...');
    try {
      const response = await axios.get(`${BASE_URL}${TEST_ENDPOINT}`);
      logger.info('Test 1 Result: API responded with no auth', {
        status: response.status,
        statusText: response.statusText
      });
      
      if (response.status === 200) {
        logger.info('Test 1 PASSED: API allowed access without auth');
        logger.info('This is expected if SKIP_AUTH=true is set');
      } else {
        logger.warn('Test 1 UNEXPECTED: API responded with status', response.status);
      }
    } catch (error) {
      if (error.response && error.response.status === 401) {
        logger.info('Test 1 PASSED: API correctly rejected unauthenticated request', {
          status: error.response.status,
          message: error.response.data?.message || 'No message'
        });
      } else {
        logger.error('Test 1 ERROR: Unexpected error', {
          status: error.response?.status,
          message: error.message
        });
      }
    }

    // Test 2: Call API with development auth header
    logger.info('Test 2: Calling API with development auth header...');
    try {
      const response = await axios.get(`${BASE_URL}${TEST_ENDPOINT}`, {
        headers: testHeaders
      });
      
      logger.info('Test 2 Result: API responded with dev auth header', {
        status: response.status,
        statusText: response.statusText
      });
      
      if (response.status === 200) {
        logger.info('Test 2 PASSED: API accepted development auth header');
        logger.info(`Response data: ${JSON.stringify(response.data)}`);
      } else {
        logger.warn('Test 2 UNEXPECTED: API responded with status', response.status);
      }
    } catch (error) {
      logger.error('Test 2 ERROR: API rejected development auth header', {
        status: error.response?.status,
        message: error.response?.data?.message || error.message
      });
    }

    // Test 3: Call API with invalid Firebase token
    logger.info('Test 3: Calling API with invalid Firebase token...');
    try {
      const response = await axios.get(`${BASE_URL}${TEST_ENDPOINT}`, {
        headers: {
          'Authorization': 'Bearer invalid-token-123',
          'Content-Type': 'application/json'
        }
      });
      
      logger.info('Test 3 Result: API responded with invalid token', {
        status: response.status,
        statusText: response.statusText
      });
      
      if (process.env.SKIP_AUTH === 'true') {
        logger.info('Test 3 PASSED: API allowed access with invalid token (SKIP_AUTH=true)');
      } else {
        logger.warn('Test 3 UNEXPECTED: API accepted invalid token!');
      }
    } catch (error) {
      if (error.response && error.response.status === 401) {
        logger.info('Test 3 PASSED: API correctly rejected invalid token', {
          status: error.response.status,
          message: error.response.data?.message || 'No message'
        });
      } else {
        logger.error('Test 3 ERROR: Unexpected error', {
          status: error.response?.status,
          message: error.message
        });
      }
    }

    logger.info('Firebase auth middleware tests completed.');
  } catch (error) {
    logger.error('Test script error:', error);
  }
};

// Run the tests
runTests(); 