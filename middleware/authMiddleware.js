const jwt = require('jsonwebtoken');

/**
 * Middleware to verify API key
 * @param {Object} req - Express request object
 * @param {Object} res - Express response object
 * @param {Function} next - Express next function
 */
const verifyApiKey = (req, res, next) => {
  try {
    const apiKey = req.header('x-api-key');
    console.log('API Key verification:', { 
        hasKey: !!apiKey,
        keyLength: apiKey ? apiKey.length : 0
    });
    
    if (!apiKey) {
      console.warn('Missing API key in request');
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
      console.warn('Invalid API key detected', {
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

    console.log('API key validated successfully');
    next();
  } catch (error) {
    console.error('API key verification error:', error);
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
    
    // Verify app signature
    const validSignatures = process.env.VALID_APP_SIGNATURES 
      ? process.env.VALID_APP_SIGNATURES.split(',') 
      : [];
    
    if (!validSignatures.includes(appSignature)) {
      console.warn('Invalid app signature');
      return res.status(401).json({
        success: false,
        message: 'App signature is not valid',
        error: 'APP_SIGNATURE_INVALID'
      });
    }
    
    next();
  } catch (error) {
    console.error(`App signature middleware error: ${error.message}`, { error });
    return res.status(500).json({
      success: false,
      message: 'Server error during app signature verification',
      error: 'AUTH_SERVER_ERROR'
    });
  }
};

/**
 * Middleware to verify device ID
 * @param {Object} req - Express request object
 * @param {Object} res - Express response object
 * @param {Function} next - Express next function
 */
const validateDeviceId = (req, res, next) => {
    const deviceId = req.headers['x-device-id'] || req.body.deviceId;
    
    // Check if deviceId exists
    if (!deviceId) {
        console.warn('Device ID validation failed: Missing deviceId');
        return res.status(400).json({
            success: false,
            message: 'Device ID is required'
        });
    }
    
    // Check if deviceId is a string
    if (typeof deviceId !== 'string') {
        console.warn(`Device ID validation failed: Invalid type ${typeof deviceId}`);
        return res.status(400).json({
            success: false,
            message: 'Device ID must be a string'
        });
    }
    
    // Check minimum length
    if (deviceId.trim().length < 8) {
        console.warn(`Device ID validation failed: Too short ${deviceId.length}`);
        return res.status(400).json({
            success: false,
            message: 'Device ID must be at least 8 characters long'
        });
    }
    
    // Check maximum length
    if (deviceId.trim().length > 64) {
        console.warn(`Device ID validation failed: Too long ${deviceId.length}`);
        return res.status(400).json({
            success: false,
            message: 'Device ID cannot exceed 64 characters'
        });
    }
    
    // Validate format (alphanumeric plus some common separators)
    const validDeviceIdPattern = /^[a-zA-Z0-9._\-:]+$/;
    if (!validDeviceIdPattern.test(deviceId)) {
        console.warn(`Device ID validation failed: Invalid format ${deviceId}`);
        return res.status(400).json({
            success: false,
            message: 'Device ID contains invalid characters'
        });
    }
    
    // All checks passed
    next();
};

module.exports = {
    verifyApiKey,
    verifyClientApp,
    validateDeviceId
}; 