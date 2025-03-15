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
 * Middleware to verify API key
 * @param {Object} req - Express request object
 * @param {Object} res - Express response object
 * @param {Function} next - Express next function
 */
const verifyApiKey = (req, res, next) => {
  try {
    // Get API key from header
    const apiKey = req.header('x-api-key');
    
    // Check if API key exists
    if (!apiKey) {
      return res.status(401).json({
        success: false,
        message: 'No API key, authorization denied',
        error: 'API_KEY_MISSING'
      });
    }
    
    // Verify API key
    if (apiKey !== process.env.API_KEY) {
      logger.warn('Invalid API key');
      return res.status(401).json({
        success: false,
        message: 'API key is not valid',
        error: 'API_KEY_INVALID'
      });
    }
    
    next();
  } catch (error) {
    logger.error(`API key middleware error: ${error.message}`, { error });
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
 * @param {Object} req - Express request object
 * @param {Object} res - Express response object
 * @param {Function} next - Express next function
 */
const verifyAdmin = (req, res, next) => {
  try {
    // Check if user exists in request (set by verifyToken)
    if (!req.user) {
      return res.status(401).json({
        success: false,
        message: 'User not authenticated',
        error: 'AUTH_USER_MISSING'
      });
    }
    
    // Check if user has admin role
    if (!req.user.isAdmin) {
      logger.warn('User is not an admin', { userId: req.user.id });
      return res.status(403).json({
        success: false,
        message: 'Access denied: Admin role required',
        error: 'AUTH_ADMIN_REQUIRED'
      });
    }
    
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

/**
 * Middleware to verify app signature
 * Alias for verifyClientApp for better naming consistency
 */
const verifyAppSignature = verifyClientApp;

module.exports = {
  verifyToken,
  verifyApiKey,
  verifyClientApp,
  verifyAdmin,
  verifyAppSignature
}; 