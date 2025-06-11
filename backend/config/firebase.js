const admin = require('firebase-admin');
const logger = require('../utils/logger');

/**
 * Initialize Firebase Admin SDK
 * This will be used for verifying Firebase ID tokens
 */
const initializeFirebaseAdmin = () => {
  try {
    // Check if Firebase Admin is already initialized
    if (admin.apps.length > 0) {
      logger.info('Firebase Admin SDK already initialized');
      return admin;
    }

    // If running with authentication disabled, return uninitialized admin
    if (process.env.SKIP_AUTH === 'true') {
      logger.info('Firebase authentication disabled with SKIP_AUTH=true');
      return admin;
    }

    // If running in development/test mode, we may want to use a service account file
    // If in production, we'll rely on Google's Application Default Credentials
    const serviceAccount = process.env.FIREBASE_SERVICE_ACCOUNT;
    
    if (serviceAccount) {
      try {
        // Initialize with explicit service account credentials
        admin.initializeApp({
          credential: admin.credential.cert(JSON.parse(serviceAccount))
        });
        logger.info('Firebase Admin SDK initialized with service account');
      } catch (parseError) {
        logger.error(`Error parsing service account: ${parseError.message}`);
        
        if (process.env.NODE_ENV !== 'production') {
          logger.info('Using empty app configuration for development');
          admin.initializeApp({});
        } else {
          throw parseError;
        }
      }
    } else {
      // Try to initialize with application default credentials
      try {
        // Initialize with application default credentials
        // This works in Google Cloud and when GOOGLE_APPLICATION_CREDENTIALS env var is set
        admin.initializeApp({
          credential: admin.credential.applicationDefault()
        });
        logger.info('Firebase Admin SDK initialized with application default credentials');
      } catch (credError) {
        logger.error(`Error initializing with application default credentials: ${credError.message}`);
        
        if (process.env.NODE_ENV !== 'production') {
          logger.info('Using empty app configuration for development');
          admin.initializeApp({});
        } else {
          throw credError;
        }
      }
    }

    return admin;
  } catch (error) {
    logger.error(`Error initializing Firebase Admin SDK: ${error.message}`);
    throw error;
  }
};

// Initialize Firebase Admin when this module is imported
const firebaseAdmin = initializeFirebaseAdmin();

module.exports = {
  admin: firebaseAdmin,
  auth: firebaseAdmin.auth()
}; 