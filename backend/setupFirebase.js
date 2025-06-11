try {
  const admin = require('firebase-admin');
  console.log('Firebase Admin SDK is properly installed.');
  
  // Check if Firebase app is initialized
  if (admin.apps.length > 0) {
    console.log('Firebase app is already initialized.');
  } else {
    console.log('Firebase app is not initialized yet.');
    
    // Check if we have environment variables for initialization
    if (process.env.FIREBASE_SERVICE_ACCOUNT) {
      console.log('FIREBASE_SERVICE_ACCOUNT environment variable is set.');
    } else {
      console.log('FIREBASE_SERVICE_ACCOUNT environment variable is not set.');
      console.log('You will need to provide service account credentials to use Firebase authentication.');
    }
  }
} catch (error) {
  console.error('Error loading Firebase Admin SDK:', error);
  console.log('Please make sure you have installed the firebase-admin package:');
  console.log('npm install firebase-admin --save');
} 