const monitoringService = require('../services/monitoringService');
const logger = require('../config/logger');

/**
 * Get current metrics
 * @param {Object} req - Express request object
 * @param {Object} res - Express response object
 */
const getMetrics = (req, res) => {
  try {
    // Get metrics
    const metrics = monitoringService.getMetrics();
    
    // Return metrics
    return res.json({
      success: true,
      data: metrics
    });
  } catch (error) {
    logger.error(`Error getting metrics: ${error.message}`, { error });
    return res.status(500).json({
      success: false,
      message: 'Failed to get metrics',
      error: error.message
    });
  }
};

/**
 * Get ad-specific metrics
 * @param {Object} req - Express request object
 * @param {Object} res - Express response object
 */
const getAdMetrics = async (req, res) => {
  try {
    // Get ad metrics
    const metrics = await monitoringService.getAdMetrics();
    
    // Return metrics
    return res.json({
      success: true,
      data: metrics
    });
  } catch (error) {
    logger.error(`Error getting ad metrics: ${error.message}`, { error });
    return res.status(500).json({
      success: false,
      message: 'Failed to get ad metrics',
      error: error.message
    });
  }
};

/**
 * Reset metrics
 * @param {Object} req - Express request object
 * @param {Object} res - Express response object
 */
const resetMetrics = (req, res) => {
  try {
    // Reset metrics
    monitoringService.resetMetrics();
    
    // Return success
    return res.json({
      success: true,
      message: 'Metrics reset successfully'
    });
  } catch (error) {
    logger.error(`Error resetting metrics: ${error.message}`, { error });
    return res.status(500).json({
      success: false,
      message: 'Failed to reset metrics',
      error: error.message
    });
  }
};

/**
 * Track request middleware
 * @param {Object} req - Express request object
 * @param {Object} res - Express response object
 * @param {Function} next - Express next function
 */
const trackRequestMiddleware = (req, res, next) => {
  // Get start time
  const startTime = Date.now();
  
  // Store original end function
  const originalEnd = res.end;
  
  // Override end function
  res.end = function(chunk, encoding) {
    // Calculate response time
    const responseTime = Date.now() - startTime;
    
    // Track request
    monitoringService.trackRequest(
      req.originalUrl,
      res.statusCode >= 200 && res.statusCode < 400,
      responseTime
    );
    
    // Call original end function
    originalEnd.call(this, chunk, encoding);
  };
  
  next();
};

module.exports = {
  getMetrics,
  getAdMetrics,
  resetMetrics,
  trackRequestMiddleware
}; 