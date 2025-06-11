/**
 * API Testing Script
 * 
 * Tests API endpoints with proper headers and data
 */

const axios = require('axios');
const testUid = 'test-uid-123456789';
const PORT = process.env.PORT || 3005; // Use PORT from environment or default to 3005
const BASE_URL = `http://localhost:${PORT}`;

// Check server environment
async function checkEnvironment() {
  try {
    console.log('Checking server environment...');
    
    const response = await axios.get(`${BASE_URL}/api/test/env`);
    console.log('Server environment:', response.data.env);
    
    // Check if we're in development mode with auth disabled
    if (response.data.env.NODE_ENV !== 'development' || response.data.env.SKIP_AUTH !== 'true') {
      console.warn('WARNING: Server is not running in development mode with authentication disabled!');
      console.warn('Please restart the server with: NODE_ENV=development SKIP_AUTH=true node server.js');
      return false;
    }
    
    return true;
  } catch (error) {
    console.error('Error checking environment:', error.message);
    return false;
  }
}

// Test the users/profile endpoint
async function testUserProfile() {
  try {
    console.log('Testing /api/users/profile endpoint...');
    
    const response = await axios.post(
      `${BASE_URL}/api/users/profile`,
      {
        uid: testUid,
        displayName: 'Test User',
        email: 'test@example.com',
        profilePhoto: 'https://example.com/photo.jpg',
        lastOnline: Date.now()
      },
      {
        headers: {
          'Content-Type': 'application/json',
          'x-firebase-uid': testUid,
          'Authorization': `Bearer test-token-${testUid}`
        }
      }
    );
    
    console.log('Response:', response.data);
    return response.data;
  } catch (error) {
    console.error('Error:', error.response ? error.response.data : error.message);
    return null;
  }
}

// Run all tests
async function runTests() {
  // First check the environment
  const envOk = await checkEnvironment();
  if (!envOk) {
    console.error('Environment check failed. Aborting tests.');
    return;
  }
  
  // Run the tests
  await testUserProfile();
}

// Execute tests
runTests(); 