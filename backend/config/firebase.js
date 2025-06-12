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
    // Only allow this in development, never in production
    if (process.env.SKIP_AUTH === 'true') {
      if (process.env.NODE_ENV === 'production') {
        throw new Error('SECURITY ERROR: Cannot skip authentication in production environment');
      }
      logger.warn('Firebase authentication disabled with SKIP_AUTH=true - THIS IS NOT SECURE FOR PRODUCTION');
      return admin;
    }

    // Get the Firebase project ID from environment variables
    const projectId = process.env.FIREBASE_PROJECT_ID;
    
    // Service account should be required in production
    const serviceAccount = process.env.FIREBASE_SERVICE_ACCOUNT;
    
    if (!serviceAccount && process.env.NODE_ENV === 'production') {
      throw new Error('FIREBASE_SERVICE_ACCOUNT environment variable is required in production');
    }
    
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
          credential: admin.credential.applicationDefault()
        };
        
        // Explicitly set project ID if provided to prevent "Unable to detect a Project Id" errors
        if (projectId) {
          appConfig.projectId = projectId;
          logger.info(`Using explicit project ID from environment: ${projectId}`);
        } else if (process.env.NODE_ENV === 'production') {
          throw new Error('FIREBASE_PROJECT_ID is required in production when using application default credentials');
        }
        
        admin.initializeApp(appConfig);
        logger.info('Firebase Admin SDK initialized with application default credentials');
      } catch (credError) {
        logger.error(`Error initializing with application default credentials: ${credError.message}`);
        
        if (process.env.NODE_ENV !== 'production') {
          // In development, initialize with just the project ID if available
          logger.warn('Using empty app configuration for development - NOT SECURE FOR PRODUCTION');
          admin.initializeApp({
            projectId: projectId || 'eventwish-app'
          });
          logger.info(`Development mode: Using project ID: ${projectId || 'eventwish-app'}`);
        } else {
          // In production, we must have valid credentials
          throw new Error(`Failed to initialize Firebase Admin SDK: ${credError.message}`);
        }
      }
    }

    return admin;
  } catch (error) {
    logger.error(`Error initializing Firebase Admin SDK: ${error.message}`);
    
    // In production, fail fast if Firebase can't be initialized
    if (process.env.NODE_ENV === 'production') {
      logger.error('FATAL: Cannot continue without Firebase authentication in production');
      process.exit(1);
    }
    
    throw error;
  }
};

// Initialize Firebase Admin when this module is imported
const firebaseAdmin = initializeFirebaseAdmin();

module.exports = {
  admin: firebaseAdmin,
  auth: firebaseAdmin.auth()
}; 