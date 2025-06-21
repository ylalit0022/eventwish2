/**
 * MongoDB Connection Test Script
 * 
 * This script tests the connection to MongoDB using native fetch API
 * without relying on Axios or other libraries.
 */

const fetch = require('node-fetch');
const dotenv = require('dotenv');
const path = require('path');
const fs = require('fs');

// Load environment variables from .env file
dotenv.config();

// MongoDB URI from environment variables
const MONGODB_URI = process.env.MONGODB_URI;

// Backend server URL
const SERVER_URL = process.env.SERVER_URL || 'http://localhost:3001';

// Mask sensitive parts of the MongoDB URI for logging
const maskMongoURI = (uri) => {
  if (!uri) return 'undefined';
  try {
    // Replace username:password with ***:***
    return uri.replace(/\/\/(.*?)@/, '//***:***@');
  } catch (error) {
    return 'Error masking URI';
  }
};

console.log('=== MongoDB Connection Test ===');
console.log('MongoDB URI (masked):', maskMongoURI(MONGODB_URI));
console.log('Server URL:', SERVER_URL);

// Test direct MongoDB connection using mongoose
async function testDirectConnection() {
  console.log('\n--- Testing Direct MongoDB Connection ---');
  
  try {
    const mongoose = require('mongoose');
    
    // Set mongoose options
    mongoose.set('strictQuery', false);
    
    // Create connection with timeout
    const connectPromise = mongoose.connect(MONGODB_URI, {
      useNewUrlParser: true,
      useUnifiedTopology: true,
      serverSelectionTimeoutMS: 5000, // 5 second timeout
    });
    
    // Add timeout
    const timeoutPromise = new Promise((_, reject) => {
      setTimeout(() => reject(new Error('Connection timeout after 5 seconds')), 5000);
    });
    
    // Race the promises
    await Promise.race([connectPromise, timeoutPromise]);
    
    // Check connection state
    const connectionState = mongoose.connection.readyState;
    const connectionStateText = {
      0: 'disconnected',
      1: 'connected',
      2: 'connecting',
      3: 'disconnecting'
    }[connectionState] || 'unknown';
    
    console.log(`MongoDB connection state: ${connectionState} (${connectionStateText})`);
    
    if (connectionState === 1) {
      console.log('✅ Successfully connected to MongoDB!');
      
      // Get database information
      const dbName = mongoose.connection.db.databaseName;
      console.log('Database name:', dbName);
      
      // List collections
      const collections = await mongoose.connection.db.listCollections().toArray();
      console.log('Collections:', collections.map(c => c.name).join(', '));
      
      // Close connection
      await mongoose.connection.close();
      console.log('Connection closed');
    } else {
      console.log('❌ Not connected to MongoDB');
    }
  } catch (error) {
    console.error('❌ Error connecting to MongoDB:', error.message);
    if (error.name === 'MongoServerSelectionError') {
      console.error('Details:', error.reason);
    }
  }
}

// Test MongoDB connection via API endpoint
async function testAPIConnection() {
  console.log('\n--- Testing MongoDB Connection via API ---');
  
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
      return;
    }
    
    // Parse the response
    const data = await response.json();
    console.log('API Response:', JSON.stringify(data, null, 2));
    
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
    } else {
      console.log('❌ API reports MongoDB is not connected');
      if (data.error) {
        console.error('Error:', data.error);
      }
    }
  } catch (error) {
    console.error('❌ Error making API request:', error.message);
    if (error.name === 'AbortError') {
      console.error('Request timed out after 5 seconds');
    }
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
      return;
    }
    
    // Parse the response
    const data = await response.json();
    console.log('API Response:', JSON.stringify(data, null, 2));
    
    console.log('✅ Backend server is responding');
  } catch (error) {
    console.error('❌ Error making API request:', error.message);
    if (error.name === 'AbortError') {
      console.error('Request timed out after 5 seconds');
    }
  }
}

// Run all tests
async function runTests() {
  try {
    // Test server connection first
    await testServerConnection();
    
    // Test API connection
    await testAPIConnection();
    
    // Test direct connection
    await testDirectConnection();
    
    console.log('\n=== Tests completed ===');
  } catch (error) {
    console.error('Error running tests:', error);
  }
}

// Run the tests
runTests(); 