/**
 * Simple API Test Script
 */

const axios = require('axios');

// Test simple endpoint
async function testEnv() {
  try {
    console.log('Testing /api/test/env endpoint...');
    const response = await axios.get('http://localhost:3007/api/test/env');
    console.log('Response:', response.data);
  } catch (error) {
    console.error('Error:', error.message);
  }
}

// Test user profile endpoint
async function testUserProfile() {
  try {
    console.log('Testing /api/users/profile endpoint...');
    
    const userData = {
      uid: 'test-uid-123',
      displayName: 'Test User',
      email: 'test@example.com'
    };
    
    const response = await axios.post(
      'http://localhost:3007/api/users/profile',
      userData,
      {
        headers: {
          'Content-Type': 'application/json',
          'x-firebase-uid': 'test-uid-123'
        }
      }
    );
    
    console.log('Response:', response.data);
  } catch (error) {
    console.error('Error:', error.response ? error.response.data : error.message);
  }
}

// Run tests
async function runTests() {
  await testEnv();
  await testUserProfile();
}

// Execute tests
runTests(); 