/**
 * Test script for Firebase initialization
 * This script tests if Firebase Admin SDK can be initialized with the specified project ID
 */

// Set environment variables
process.env.NODE_ENV = 'development';
process.env.FIREBASE_PROJECT_ID = 'eventwish-app';
process.env.SKIP_AUTH = 'true';

// Import Firebase Admin
const admin = require('firebase-admin');

console.log('Environment variables:');
console.log('- NODE_ENV:', process.env.NODE_ENV);
console.log('- FIREBASE_PROJECT_ID:', process.env.FIREBASE_PROJECT_ID);
console.log('- SKIP_AUTH:', process.env.SKIP_AUTH);

// Initialize Firebase with the project ID
try {
  admin.initializeApp({
    projectId: process.env.FIREBASE_PROJECT_ID
  });
  
  console.log('✅ Firebase initialized successfully');
  console.log('- Project ID:', admin.app().options.projectId);
  
  // Try to get auth
  const auth = admin.auth();
  console.log('✅ Firebase Auth accessed successfully');
} catch (error) {
  console.error('❌ Firebase initialization error:', error.message);
}

// Test token verification (will be skipped in development mode)
console.log('\nTesting token verification:');
try {
  const mockToken = 'mock-token-for-testing';
  
  // This should be skipped in development mode with SKIP_AUTH=true
  console.log('- Attempting to verify a mock token...');
  admin.auth().verifyIdToken(mockToken)
    .then(decodedToken => {
      console.log('✅ Token verified:', decodedToken);
    })
    .catch(error => {
      console.error('❌ Token verification error:', error.message);
    });
} catch (error) {
  console.error('❌ Token verification test error:', error.message);
}

console.log('\nFirebase test completed'); 