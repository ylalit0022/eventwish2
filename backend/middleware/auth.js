let admin;
try {
  admin = require('firebase-admin');
} catch (error) {
  console.error('Firebase Admin SDK not available:', error.message);
}

const logger = require('../utils/logger');

/**
 * Middleware to verify Firebase authentication token
 * This ensures the user is authenticated with Firebase before accessing protected endpoints
 */
const verifyFirebaseToken = async (req, res, next) => {
  // If Firebase Admin is not available, skip authentication in development
  if (!admin || !admin.apps || admin.apps.length === 0) {
    if (process.env.NODE_ENV === 'development' || process.env.SKIP_AUTH === 'true') {
      logger.warn('Firebase authentication disabled. Proceeding without authentication.');
      // In development, we'll extract the UID from headers for testing
      if (req.headers['x-firebase-uid']) {
        req.uid = req.headers['x-firebase-uid'];
        req.user = { uid: req.uid };
        logger.info(`Development mode: Using UID ${req.uid} from headers`);
      }
      return next();
    } else {
      logger.error('Firebase Admin SDK not initialized. Authentication middleware cannot function.');
      return res.status(500).json({
        success: false,
        message: 'Authentication service unavailable'
      });
    }
  }

  try {
    const authHeader = req.headers.authorization;
    
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      logger.warn('Authentication failed: No token provided');
      return res.status(401).json({
        success: false,
        message: 'Authentication required. Please provide a valid token.'
      });
    }
    
    // Extract the token
    const idToken = authHeader.split('Bearer ')[1];
    
    if (!idToken) {
      logger.warn('Authentication failed: Empty token');
      return res.status(401).json({
        success: false,
        message: 'Authentication required. Please provide a valid token.'
      });
    }
    
    // Verify the token with Firebase Admin
    const decodedToken = await admin.auth().verifyIdToken(idToken);
    
    // Add the decoded token and UID to the request object
    req.user = decodedToken;
    req.uid = decodedToken.uid;
    
    // For endpoints that use req.body.uid, ensure it matches the token
    if (req.body.uid && req.body.uid !== decodedToken.uid) {
      logger.warn(`UID mismatch: Token UID ${decodedToken.uid} does not match request UID ${req.body.uid}`);
      return res.status(403).json({
        success: false,
        message: 'User ID in request does not match authenticated user'
      });
    }
    
    // For endpoints that use req.params.uid, ensure it matches the token
    if (req.params.uid && req.params.uid !== decodedToken.uid) {
      logger.warn(`UID mismatch: Token UID ${decodedToken.uid} does not match request UID ${req.params.uid}`);
      return res.status(403).json({
        success: false,
        message: 'User ID in request does not match authenticated user'
      });
    }
    
    logger.info(`User ${decodedToken.uid} authenticated successfully`);
    next();
  } catch (error) {
    logger.error(`Authentication error: ${error.message}`);
    
    // Handle specific Firebase Auth errors
    if (error.code === 'auth/id-token-expired') {
      return res.status(401).json({
        success: false,
        message: 'Authentication token expired. Please login again.'
      });
    } else if (error.code === 'auth/id-token-revoked') {
      return res.status(401).json({
        success: false,
        message: 'Authentication token has been revoked. Please login again.'
      });
    } else if (error.code === 'auth/invalid-id-token') {
      return res.status(401).json({
        success: false,
        message: 'Invalid authentication token. Please login again.'
      });
    }
    
    res.status(401).json({
      success: false,
      message: 'Authentication failed. Please login again.',
      error: error.message
    });
  }
};

/**
 * Optional authentication middleware
 * Verifies token if present, but allows the request to proceed even without authentication
 * Useful for endpoints that work differently for authenticated vs anonymous users
 */
const optionalFirebaseAuth = async (req, res, next) => {
  try {
    const authHeader = req.headers.authorization;
    
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      // No authentication provided, but that's okay for this middleware
      logger.info('No authentication token provided, proceeding as unauthenticated request');
      return next();
    }
    
    // Extract the token
    const idToken = authHeader.split('Bearer ')[1];
    
    if (!idToken) {
      // Empty token, but that's okay for this middleware
      logger.info('Empty authentication token, proceeding as unauthenticated request');
      return next();
    }
    
    // Try to verify the token
    try {
      const decodedToken = await admin.auth().verifyIdToken(idToken);
      
      // Add the decoded token and UID to the request object
      req.user = decodedToken;
      req.uid = decodedToken.uid;
      
      logger.info(`Optional auth: User ${decodedToken.uid} authenticated successfully`);
    } catch (tokenError) {
      // Token verification failed, but that's okay for this middleware
      logger.info(`Optional auth: Token verification failed, proceeding as unauthenticated request: ${tokenError.message}`);
    }
    
    next();
  } catch (error) {
    // For optional auth, we don't want to block the request even if something goes wrong
    logger.error(`Optional authentication error: ${error.message}`);
    next();
  }
};

/**
 * Simple API key verification middleware
 * This is used for endpoints that should be accessible via API key 
 * rather than requiring Firebase authentication
 */
const verifyApiKey = (req, res, next) => {
  try {
    const apiKey = req.headers['x-api-key'] || req.query.api_key;
    
    if (!apiKey) {
      logger.warn('API key verification failed: No API key provided');
      return res.status(401).json({
        success: false,
        message: 'API key required'
      });
    }
    
    // Check against the API key in environment variables
    if (apiKey !== process.env.API_KEY) {
      logger.warn('API key verification failed: Invalid API key');
      return res.status(401).json({
        success: false,
        message: 'Invalid API key'
      });
    }
    
    logger.info('API key verification succeeded');
    next();
  } catch (error) {
    logger.error(`API key verification error: ${error.message}`);
    res.status(500).json({
      success: false,
      message: 'Server error during API key verification'
    });
  }
};

module.exports = {
  verifyFirebaseToken,
  optionalFirebaseAuth,
  verifyApiKey
}; 