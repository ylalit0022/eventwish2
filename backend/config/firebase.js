const admin = require('firebase-admin');
const logger = require('./logger');

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

    // If running with authentication disabled, return null
    // Only allow this in development, never in production
    if (process.env.SKIP_AUTH === 'true') {
      if (process.env.NODE_ENV === 'production') {
        logger.warn('SECURITY WARNING: Running with SKIP_AUTH=true in production environment');
      }
      logger.warn('Firebase authentication disabled with SKIP_AUTH=true - THIS IS NOT SECURE FOR PRODUCTION');
      return null;
    }

    // Get the Firebase project ID from environment variables
    const projectId = process.env.FIREBASE_PROJECT_ID;
    
    // Service account is preferred but not required in production
    const serviceAccount = process.env.FIREBASE_SERVICE_ACCOUNT;
    
    if (serviceAccount) {
      try {
        // Initialize with explicit service account credentials
        const serviceAccountObj = JSON.parse(serviceAccount);
        admin.initializeApp({
          credential: admin.credential.cert(serviceAccountObj),
          // If projectId is explicitly provided, use it as a fallback
          projectId: serviceAccountObj.project_id || projectId
        });
        logger.info(`Firebase Admin SDK initialized with service account, project ID: ${serviceAccountObj.project_id || projectId || 'unknown'}`);
      } catch (parseError) {
        logger.error(`Error parsing service account: ${parseError.message}`);
        
        if (process.env.NODE_ENV !== 'production') {
          logger.warn('Using empty app configuration for development - NOT SECURE FOR PRODUCTION');
          admin.initializeApp({
            projectId: projectId || 'eventwish-app'
          });
          logger.info(`Development mode: Using project ID: ${projectId || 'eventwish-app'}`);
        } else {
          // In production, we must have valid credentials
          throw new Error(`Failed to initialize Firebase Admin SDK: ${parseError.message}`);
        }
      }
    } else {
      // Try to initialize with application default credentials
      try {
        // Initialize with application default credentials
        // This works in Google Cloud and when GOOGLE_APPLICATION_CREDENTIALS env var is set
        const appConfig = {
          projectId: projectId || 'neweventwish'
        };
        
        admin.initializeApp(appConfig);
        logger.info(`Firebase Admin SDK initialized with application default credentials, project ID: ${projectId || 'neweventwish'}`);
      } catch (credError) {
        logger.error(`Error initializing Firebase with default credentials: ${credError.message}`);
        
        // Try to initialize with just the project ID as a fallback
        try {
          admin.initializeApp({
            projectId: projectId || 'neweventwish'
          });
          logger.warn(`Firebase initialized with project ID only: ${projectId || 'neweventwish'} - Authentication may be limited`);
        } catch (fallbackError) {
          logger.error(`Failed to initialize Firebase with fallback configuration: ${fallbackError.message}`);
          
          if (process.env.NODE_ENV === 'production') {
            // In production, log the error but don't exit - let the app continue with limited functionality
            logger.error('WARNING: Firebase authentication will not work properly. Some features may be disabled.');
            return null;
          } else {
            throw new Error(`Failed to initialize Firebase Admin SDK: ${fallbackError.message}`);
          }
        }
      }
    }

    return admin;
  } catch (error) {
    logger.error(`Error initializing Firebase Admin SDK: ${error.message}`);
    
    // In production, log the error but don't exit - let the app continue with limited functionality
    if (process.env.NODE_ENV === 'production') {
      logger.error('WARNING: Firebase authentication initialization failed. Some features may be disabled.');
      return null;
    }
    
    throw error;
  }
};

// Initialize Firebase Admin when this module is imported
const firebaseAdmin = initializeFirebaseAdmin();

// Export the admin object, but handle the case where it might be null (SKIP_AUTH=true)
module.exports = {
  admin: firebaseAdmin,
  auth: firebaseAdmin ? firebaseAdmin.auth() : null,
  // Provide a mock auth object when running with SKIP_AUTH=true
  getMockAuth: () => ({
    verifyIdToken: async () => ({ uid: 'mock-uid-12345', email: 'mock@example.com' }),
    getUser: async () => ({ uid: 'mock-uid-12345', email: 'mock@example.com', displayName: 'Mock User' })
  })
}; 