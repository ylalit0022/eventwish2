/**
 * Browser-based MongoDB Connection Test
 * 
 * This script tests the connection to MongoDB from the browser using fetch API
 * Run this in the browser console to diagnose connection issues
 */

// Configuration
const SERVER_URL = 'http://localhost:3001'; // Change this if your server runs on a different port

// Test MongoDB connection via API endpoint
async function testMongoDBConnection() {
  console.log('=== MongoDB Connection Test ===');
  console.log('Testing connection to MongoDB via API');
  console.log('Server URL:', SERVER_URL);
  
  try {
    // Create a timeout for the fetch request
    const controller = new AbortController();
    const { signal } = controller;
    
    // Set a timeout of 5 seconds
    const timeout = setTimeout(() => {
      controller.abort();
    }, 5000);
    
    console.log(`Making request to ${SERVER_URL}/api/admin/diagnostics/mongodb`);
    
    // Make the request with authorization header
    const response = await fetch(`${SERVER_URL}/api/admin/diagnostics/mongodb`, {
      method: 'GET',
      headers: {
        'Authorization': 'Bearer dev-token',
        'X-Dev-Email': 'ylalit0022@gmail.com',
        'X-Dev-Admin': 'true'
      },
      signal
    });
    
    // Clear the timeout
    clearTimeout(timeout);
    
    // Check if response is ok
    if (!response.ok) {
      console.error(`❌ API returned status: ${response.status} ${response.statusText}`);
      const text = await response.text();
      console.error('Response:', text);
      return false;
    }
    
    // Parse the response
    const data = await response.json();
    console.log('API Response:', data);
    
    // Check MongoDB connection status from API response
    if (data.success) {
      console.log('✅ API reports MongoDB is connected');
      
      if (data.mongodb) {
        const { connectionState, connectionStateText } = data.mongodb;
        console.log(`MongoDB connection state: ${connectionState} (${connectionStateText})`);
        
        if (data.mongodb.collections) {
          console.log('Collections:', data.mongodb.collections.join(', '));
        }
      }
      return true;
    } else {
      console.log('❌ API reports MongoDB is not connected');
      if (data.error) {
        console.error('Error:', data.error);
      }
      return false;
    }
  } catch (error) {
    console.error('❌ Error making API request:', error.message);
    if (error.name === 'AbortError') {
      console.error('Request timed out after 5 seconds');
    }
    return false;
  }
}

// Test fetch to backend server (without MongoDB specific endpoint)
async function testServerConnection() {
  console.log('\n--- Testing Backend Server Connection ---');
  
  try {
    // Create a timeout for the fetch request
    const controller = new AbortController();
    const { signal } = controller;
    
    // Set a timeout of 5 seconds
    const timeout = setTimeout(() => {
      controller.abort();
    }, 5000);
    
    console.log(`Making request to ${SERVER_URL}/api/health`);
    
    // Make the request
    const response = await fetch(`${SERVER_URL}/api/health`, {
      method: 'GET',
      signal
    });
    
    // Clear the timeout
    clearTimeout(timeout);
    
    // Check if response is ok
    if (!response.ok) {
      console.error(`❌ API returned status: ${response.status} ${response.statusText}`);
      const text = await response.text();
      console.error('Response:', text);
      return false;
    }
    
    // Parse the response
    const data = await response.json();
    console.log('API Response:', data);
    
    console.log('✅ Backend server is responding');
    return true;
  } catch (error) {
    console.error('❌ Error making API request:', error.message);
    if (error.name === 'AbortError') {
      console.error('Request timed out after 5 seconds');
    }
    return false;
  }
}

// Test push notifications API
async function testPushNotificationsAPI() {
  console.log('\n--- Testing Push Notifications API ---');
  
  try {
    // Create a timeout for the fetch request
    const controller = new AbortController();
    const { signal } = controller;
    
    // Set a timeout of 5 seconds
    const timeout = setTimeout(() => {
      controller.abort();
    }, 5000);
    
    console.log(`Making request to ${SERVER_URL}/api/admin/push-notifications`);
    
    // Make the request with authorization header
    const response = await fetch(`${SERVER_URL}/api/admin/push-notifications`, {
      method: 'GET',
      headers: {
        'Authorization': 'Bearer dev-token',
        'X-Dev-Email': 'ylalit0022@gmail.com',
        'X-Dev-Admin': 'true'
      },
      signal
    });
    
    // Clear the timeout
    clearTimeout(timeout);
    
    // Check if response is ok
    if (!response.ok) {
      console.error(`❌ API returned status: ${response.status} ${response.statusText}`);
      const text = await response.text();
      console.error('Response:', text);
      return false;
    }
    
    // Parse the response
    const data = await response.json();
    console.log('API Response:', data);
    
    console.log('✅ Push notifications API is responding');
    return true;
  } catch (error) {
    console.error('❌ Error making API request:', error.message);
    if (error.name === 'AbortError') {
      console.error('Request timed out after 5 seconds');
    }
    return false;
  }
}

// Run all tests
async function runTests() {
  try {
    // Test server connection first
    const serverConnected = await testServerConnection();
    
    if (serverConnected) {
      // Test MongoDB connection
      const mongoConnected = await testMongoDBConnection();
      
      // Test push notifications API
      await testPushNotificationsAPI();
    }
    
    console.log('\n=== Tests completed ===');
  } catch (error) {
    console.error('Error running tests:', error);
  }
}

// Run the tests
runTests();

// Export the test functions for use in the browser console
window.testMongoDBConnection = testMongoDBConnection;
window.testServerConnection = testServerConnection;
window.testPushNotificationsAPI = testPushNotificationsAPI;
window.runTests = runTests; 