const admin = require('firebase-admin');
const path = require('path');
const fs = require('fs');

// Path to service account key file
const serviceAccountPath = path.join(__dirname, '../serviceAccountKey.json');

// Initialize Firebase Admin SDK
let firebaseInitialized = false;

const initializeFirebase = () => {
  if (firebaseInitialized) {
    return;
  }
  
  try {
    // Check if service account key file exists
    if (fs.existsSync(serviceAccountPath)) {
      // Initialize with service account file
      const serviceAccount = require(serviceAccountPath);
      
      admin.initializeApp({
        credential: admin.credential.cert(serviceAccount)
      });
      
      console.log('Firebase Admin SDK initialized with service account file');
    } else {
      // Initialize with environment variables
      // This is useful for deployment environments like Heroku
      const serviceAccountEnv = process.env.FIREBASE_SERVICE_ACCOUNT;
      
      if (serviceAccountEnv) {
        // Parse the JSON string from environment variable
        const serviceAccountJson = JSON.parse(serviceAccountEnv);
        
        admin.initializeApp({
          credential: admin.credential.cert(serviceAccountJson)
        });
        
        console.log('Firebase Admin SDK initialized with environment variables');
      } else {
        console.error('Firebase service account not found. FCM functionality will not work.');
        return;
      }
    }
    
    firebaseInitialized = true;
  } catch (error) {
    console.error('Error initializing Firebase Admin SDK:', error);
  }
};

// Initialize Firebase when this module is imported
initializeFirebase();

module.exports = {
  admin,
  messaging: admin.messaging
}; 