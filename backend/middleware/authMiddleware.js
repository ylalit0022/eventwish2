const jwt = require('jsonwebtoken');
const logger = require('../config/logger');

/**
 * Middleware to verify JWT token
 * @param {Object} req - Express request object
 * @param {Object} res - Express response object
 * @param {Function} next - Express next function
 */
const verifyToken = (req, res, next) => {
  try {
    // Get token from header
    const token = req.header('x-auth-token');
    
    // Check if token exists
    if (!token) {
      return res.status(401).json({
        success: false,
        message: 'No token, authorization denied',
        error: 'AUTH_TOKEN_MISSING'
      });
    }
    
    // Verify token
    try {
      const decoded = jwt.verify(token, process.env.JWT_SECRET);
      req.user = decoded.user;
      next();
    } catch (err) {
      logger.warn('Invalid token', { error: err.message });
      return res.status(401).json({
        success: false,
        message: 'Token is not valid',
        error: 'AUTH_TOKEN_INVALID'
      });
    }
  } catch (error) {
    logger.error(`Auth middleware error: ${error.message}`, { error });
    return res.status(500).json({
      success: false,
      message: 'Server error during authentication',
      error: 'AUTH_SERVER_ERROR'
    });
  }
};

/**
 * Enhanced token verification that handles expired tokens
 */
const verifyTokenWithRefresh = (req, res, next) => {
  try {
    const token = req.header('x-auth-token');
    const refreshToken = req.header('x-refresh-token');
    const deviceId = req.header('x-device-id');

    // Check if token exists
    if (!token) {
      return res.status(401).json({
        success: false,
        message: 'No token, authorization denied',
        error: 'AUTH_TOKEN_MISSING'
      });
    }

    try {
      // Try to verify the token
      const decoded = jwt.verify(token, process.env.JWT_SECRET);
      req.user = decoded.user;
      next();
    } catch (err) {
      // If token is expired and refresh token is provided, try to refresh
      if (err.name === 'TokenExpiredError' && refreshToken && deviceId) {
        try {
          // Verify refresh token
          const decodedRefresh = jwt.verify(refreshToken, process.env.REFRESH_TOKEN_SECRET);
          
          // Validate device ID matches
          if (decodedRefresh.deviceId !== deviceId) {
            logger.warn('Device ID mismatch during token refresh');
            return res.status(401).json({
              success: false,
              message: 'Invalid refresh token',
              error: 'REFRESH_TOKEN_INVALID'
            });
          }

          // Generate new tokens
          const newToken = generateToken(decodedRefresh.user);
          const newRefreshToken = generateRefreshToken(decodedRefresh.user, deviceId);

          // Set new tokens in response headers
          res.set('x-auth-token', newToken);
          res.set('x-refresh-token', newRefreshToken);

          // Set user in request
          req.user = decodedRefresh.user;
          next();
        } catch (refreshErr) {
          logger.warn('Refresh token error', { error: refreshErr.message });
          return res.status(401).json({
            success: false,
            message: 'Invalid refresh token',
            error: 'REFRESH_TOKEN_INVALID'
          });
        }
      } else {
        logger.warn('Invalid token', { error: err.message });
        return res.status(401).json({
          success: false,
          message: 'Token is not valid',
          error: 'AUTH_TOKEN_INVALID'
        });
      }
    }
  } catch (error) {
    logger.error(`Auth middleware error: ${error.message}`, { error });
    return res.status(500).json({
      success: false,
      message: 'Server error during authentication',
      error: 'AUTH_SERVER_ERROR'
    });
  }
};

/**
 * Generate JWT token
 */
const generateToken = (user) => {
  return jwt.sign({ user }, process.env.JWT_SECRET, { expiresIn: '1h' });
};

/**
 * Generate refresh token
 */
const generateRefreshToken = (user, deviceId) => {
  return jwt.sign({ user, deviceId }, process.env.REFRESH_TOKEN_SECRET, { expiresIn: '7d' });
};

/**
 * Middleware to verify API key
 * @param {Object} req - Express request object
 * @param {Object} res - Express response object
 * @param {Function} next - Express next function
 */
const verifyApiKey = (req, res, next) => {
  try {
    const apiKey = req.header('x-api-key');
    logger.debug('API Key verification:', { 
        hasKey: !!apiKey,
        keyLength: apiKey ? apiKey.length : 0
    });
    
    if (!apiKey) {
      logger.warn('Missing API key in request');
      return res.status(401).json({
        success: false,
        message: 'No API key provided',
        error: 'API_KEY_MISSING'
      });
    }

    // Get API key from environment
    const validApiKey = process.env.API_KEY;
    
    // Simple equality check for API key
    if (apiKey !== validApiKey) {
      logger.warn('Invalid API key detected', {
          receivedKeyLength: apiKey.length,
          validKeyLength: validApiKey.length,
          // Log last 4 chars for debugging
          receivedKeySuffix: apiKey.slice(-4),
          validKeySuffix: validApiKey.slice(-4)
      });
      
      return res.status(401).json({
        success: false,
        message: 'API key is not valid',
        error: 'API_KEY_INVALID'
      });
    }

    logger.debug('API key validated successfully');
    next();
  } catch (error) {
    logger.error('API key verification error:', error);
    return res.status(500).json({
      success: false,
      message: 'Server error during API key verification',
      error: 'AUTH_SERVER_ERROR'
    });
  }
};

/**
 * Middleware to verify client app
 * @param {Object} req - Express request object
 * @param {Object} res - Express response object
 * @param {Function} next - Express next function
 */
const verifyClientApp = (req, res, next) => {
  try {
    // Get app signature from header
    const appSignature = req.header('x-app-signature');
    
    // Check if app signature exists
    if (!appSignature) {
      return res.status(401).json({
        success: false,
        message: 'No app signature, authorization denied',
        error: 'APP_SIGNATURE_MISSING'
      });
    }
    
    // Verify app signature (simple implementation for now)
    // In a production environment, this would be more sophisticated
    const validSignatures = process.env.VALID_APP_SIGNATURES 
      ? process.env.VALID_APP_SIGNATURES.split(',') 
      : [];
    
    if (!validSignatures.includes(appSignature)) {
      logger.warn('Invalid app signature');
      return res.status(401).json({
        success: false,
        message: 'App signature is not valid',
        error: 'APP_SIGNATURE_INVALID'
      });
    }
    
    next();
  } catch (error) {
    logger.error(`App signature middleware error: ${error.message}`, { error });
    return res.status(500).json({
      success: false,
      message: 'Server error during app signature verification',
      error: 'AUTH_SERVER_ERROR'
    });
  }
};

/**
 * Middleware to verify admin role
 * @param {String} requiredPermission - Optional specific permission to check
 * @returns {Function} Express middleware function
 */
const verifyAdmin = (requiredPermission = null) => {
  return async (req, res, next) => {
    try {
      const { getAdminRole, hasPermission } = require('../config/adminConfig');
      
      // Check if user exists in request (set by verifyToken)
      if (!req.user) {
        return res.status(401).json({
          success: false,
          message: 'User not authenticated',
          error: 'AUTH_USER_MISSING'
        });
      }
      
      // Get user email from the Firebase decoded token
      const userEmail = req.user.email;
      
      if (!userEmail) {
        logger.warn('Admin verification failed: No email in user token', { userId: req.user.uid });
        return res.status(403).json({
          success: false,
          message: 'Access denied: No email in user profile',
          error: 'AUTH_EMAIL_MISSING'
        });
      }
      
      // Check if email is in admin whitelist and get role
      const role = getAdminRole(userEmail);
      
      if (!role) {
        logger.warn('User is not an admin', { 
          userId: req.user.uid,
          email: userEmail 
        });
        return res.status(403).json({
          success: false,
          message: 'Access denied: Not in admin whitelist',
          error: 'AUTH_ADMIN_REQUIRED'
        });
      }
      
      // If specific permission is required, check if the admin role has it
      if (requiredPermission && !hasPermission(role, requiredPermission)) {
        logger.warn('Admin lacks required permission', {
          userId: req.user.uid,
          email: userEmail,
          role,
          requiredPermission
        });
        return res.status(403).json({
          success: false,
          message: `Access denied: Required permission '${requiredPermission}' not granted for your role`,
          error: 'AUTH_PERMISSION_DENIED'
        });
      }
      
      // Add admin info to the request
      req.adminInfo = {
        role,
        email: userEmail
      };
      
      logger.debug('Admin verified successfully', { 
        userId: req.user.uid,
        email: userEmail,
        role
      });
      
      next();
    } catch (error) {
      logger.error(`Admin verification error: ${error.message}`, { error });
      return res.status(500).json({
        success: false,
        message: 'Server error during admin verification',
        error: 'AUTH_SERVER_ERROR'
      });
    }
  };
};

/**
 * Middleware to verify app signature
 * Alias for verifyClientApp for better naming consistency
 */
const verifyAppSignature = verifyClientApp;

/**
 * Middleware to validate registration
 * @param {Object} req - Express request object
 * @param {Object} res - Express response object
 * @param {Function} next - Express next function
 */
const validateRegistration = (req, res, next) => {
  try {
    const { deviceId, appSignature } = req.body;
    
    if (!deviceId || !appSignature) {
      return res.status(400).json({
        success: false,
        message: 'Device ID and app signature are required',
        error: 'MISSING_REQUIRED_FIELDS'
      });
    }

    // Check if signature format is valid (base64 encoded SHA-256)
    const signatureRegex = /^[A-Za-z0-9+/=]{44}$/;
    if (!signatureRegex.test(appSignature)) {
      return res.status(400).json({
        success: false,
        message: 'Invalid signature format',
        error: 'INVALID_SIGNATURE_FORMAT'
      });
    }

    next();
  } catch (error) {
    logger.error(`Registration validation error: ${error.message}`, { error });
    return res.status(500).json({
      success: false, 
      message: 'Validation error',
      error: 'VALIDATION_ERROR'
    });
  }
};

/**
 * Middleware to verify Firebase ID token
 * @param {Object} req - Express request object
 * @param {Object} res - Express response object
 * @param {Function} next - Express next function
 */
const verifyFirebaseToken = async (req, res, next) => {
  try {
    // Get token from Authorization header
    const authHeader = req.headers.authorization;
    
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      return res.status(401).json({
        success: false,
        message: 'No Firebase token provided',
        error: 'AUTH_TOKEN_MISSING'
      });
    }
    
    const idToken = authHeader.split('Bearer ')[1];
    
    // Check for development mode
    const isDevelopment = process.env.NODE_ENV === 'development' || process.env.SKIP_AUTH === 'true';
    const isDevToken = idToken === 'dev-token';
    const devEmail = req.headers['x-dev-email'];
    const isDevAdmin = req.headers['x-dev-admin'] === 'true';
    
    // Allow dev token in development mode
    if (isDevelopment && isDevToken && devEmail && isDevAdmin) {
      logger.info('Using development authentication mode', { email: devEmail });
      
      // Create a mock user object similar to Firebase decoded token
      req.user = {
        uid: 'dev-admin-uid',
        email: devEmail,
        email_verified: true,
        name: 'Development Admin',
        role: 'superAdmin',
        iat: Math.floor(Date.now() / 1000),
        exp: Math.floor(Date.now() / 1000) + 3600,
        iss: 'dev-auth'
      };
      
      return next();
    }
    
    // Initialize Firebase Admin if not already initialized
    const admin = require('firebase-admin');
    if (!admin.apps.length) {
      // Check if we have service account credentials
      if (process.env.FIREBASE_SERVICE_ACCOUNT) {
        try {
          const serviceAccount = JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT);
          admin.initializeApp({
            credential: admin.credential.cert(serviceAccount)
          });
        } catch (error) {
          logger.error('Failed to parse Firebase service account', { error });
          admin.initializeApp({
            projectId: process.env.FIREBASE_PROJECT_ID
          });
        }
      } else {
        // Initialize with application default credentials
        admin.initializeApp({
          projectId: process.env.FIREBASE_PROJECT_ID
        });
      }
    }
    
    // Verify the ID token
    const decodedToken = await admin.auth().verifyIdToken(idToken);
    
    // Add the decoded token to the request
    req.user = decodedToken;
    
    logger.debug('Firebase token verified successfully', { 
      uid: decodedToken.uid,
      email: decodedToken.email
    });
    
    next();
  } catch (error) {
    logger.error(`Firebase token verification error: ${error.message}`, { error });
    
    return res.status(401).json({
      success: false,
      message: 'Invalid Firebase token',
      error: 'AUTH_TOKEN_INVALID'
    });
  }
};

module.exports = {
  verifyToken,
  verifyTokenWithRefresh,
  generateToken,
  generateRefreshToken,
  verifyApiKey,
  verifyClientApp,
  verifyAdmin,
  verifyAppSignature,
  validateRegistration,
  verifyFirebaseToken
};