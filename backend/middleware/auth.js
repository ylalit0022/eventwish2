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
      } else if (req.params.uid) {
        // If no header but URL has uid parameter, use that in development mode
        req.uid = req.params.uid;
        req.user = { uid: req.uid };
        logger.info(`Development mode: Using UID ${req.uid} from URL params`);
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
    
    // For development only, allow skipping auth with SKIP_AUTH=true
    if ((process.env.NODE_ENV === 'development' || process.env.SKIP_AUTH === 'true') && 
        (!authHeader || !authHeader.startsWith('Bearer '))) {
      logger.warn('Development mode: Skipping authentication check');
      if (req.params.uid) {
        req.uid = req.params.uid;
        req.user = { uid: req.uid };
        logger.info(`Development mode: Using UID ${req.uid} from URL params`);
      }
      return next();
    }
    
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
    try {
      // Add a timeout for token verification to prevent hanging requests
      const decodedTokenPromise = admin.auth().verifyIdToken(idToken);
      
      // Create a timeout promise
      const timeoutPromise = new Promise((_, reject) => {
        setTimeout(() => reject(new Error('Token verification timed out')), 5000);
      });
      
      // Race the token verification against the timeout
      const decodedToken = await Promise.race([decodedTokenPromise, timeoutPromise]);
      
      // Check token expiration time
      const currentTime = Math.floor(Date.now() / 1000);
      if (decodedToken.exp && decodedToken.exp < currentTime) {
        logger.warn(`Token expired for user ${decodedToken.uid}`);
        return res.status(401).json({
          success: false,
          message: 'Authentication token expired. Please login again.'
        });
      }
      
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
    } catch (verifyError) {
      logger.error(`Token verification error: ${verifyError.message}`);
      
      // Handle specific Firebase Auth errors
      if (verifyError.code === 'auth/id-token-expired') {
        return res.status(401).json({
          success: false,
          message: 'Authentication token expired. Please login again.',
          code: 'TOKEN_EXPIRED'
        });
      } else if (verifyError.code === 'auth/id-token-revoked') {
        return res.status(401).json({
          success: false,
          message: 'Authentication token has been revoked. Please login again.',
          code: 'TOKEN_REVOKED'
        });
      } else if (verifyError.code === 'auth/invalid-id-token') {
        return res.status(401).json({
          success: false,
          message: 'Invalid authentication token. Please login again.',
          code: 'INVALID_TOKEN'
        });
      } else if (verifyError.message.includes('Unable to detect a Project Id')) {
        logger.error('Firebase Project ID not configured correctly');
        
        // In development, allow proceeding without authentication
        if (process.env.NODE_ENV === 'development' || process.env.SKIP_AUTH === 'true') {
          logger.warn('Development mode: Proceeding despite Project ID error');
          if (req.params.uid) {
            req.uid = req.params.uid;
            req.user = { uid: req.uid };
            logger.info(`Development mode: Using UID ${req.uid} from URL params`);
            return next();
          }
        }
        
        return res.status(500).json({
          success: false,
          message: 'Server authentication configuration error',
          code: 'SERVER_CONFIG_ERROR'
        });
      } else if (verifyError.message === 'Token verification timed out') {
        logger.error('Firebase token verification timed out');
        return res.status(503).json({
          success: false,
          message: 'Authentication service timeout',
          code: 'AUTH_TIMEOUT'
        });
      }
      
      // Generic error response
      return res.status(401).json({
        success: false,
        message: 'Authentication failed. Please login again.',
        code: 'AUTH_FAILED'
      });
    }
  } catch (error) {
    logger.error(`Authentication error: ${error.message}`);
    
    return res.status(500).json({
      success: false,
      message: 'Server error during authentication',
      code: 'SERVER_ERROR'
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
      // Add a timeout for token verification
      const decodedTokenPromise = admin.auth().verifyIdToken(idToken);
      
      // Create a timeout promise
      const timeoutPromise = new Promise((_, reject) => {
        setTimeout(() => reject(new Error('Token verification timed out')), 5000);
      });
      
      // Race the token verification against the timeout
      const decodedToken = await Promise.race([decodedTokenPromise, timeoutPromise]);
      
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
        message: 'API key required',
        code: 'API_KEY_REQUIRED'
      });
    }
    
    // Check against the API key in environment variables
    if (apiKey !== process.env.API_KEY) {
      logger.warn('API key verification failed: Invalid API key');
      return res.status(401).json({
        success: false,
        message: 'Invalid API key',
        code: 'INVALID_API_KEY'
      });
    }
    
    // Add rate limiting for API key requests in production
    if (process.env.NODE_ENV === 'production') {
      // This is a simple in-memory rate limiting implementation
      // In a real production environment, you would use Redis or similar
      const now = Date.now();
      const requestsPerMinute = 60; // Adjust as needed
      
      // Create a global request counter if it doesn't exist
      if (!global.apiKeyRequests) {
        global.apiKeyRequests = [];
      }
      
      // Clean up old requests (older than 1 minute)
      global.apiKeyRequests = global.apiKeyRequests.filter(timestamp => now - timestamp < 60000);
      
      // Add current request timestamp
      global.apiKeyRequests.push(now);
      
      // Check if we're over the limit
      if (global.apiKeyRequests.length > requestsPerMinute) {
        logger.warn('API key rate limit exceeded');
        return res.status(429).json({
          success: false,
          message: 'Rate limit exceeded. Please try again later.',
          code: 'RATE_LIMIT_EXCEEDED'
        });
      }
    }
    
    logger.info('API key verification succeeded');
    next();
  } catch (error) {
    logger.error(`API key verification error: ${error.message}`);
    res.status(500).json({
      success: false,
      message: 'Server error during API key verification',
      code: 'SERVER_ERROR'
    });
  }
};

module.exports = {
  verifyFirebaseToken,
  optionalFirebaseAuth,
  verifyApiKey
}; 