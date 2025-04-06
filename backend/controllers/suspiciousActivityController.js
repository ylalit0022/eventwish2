/**
 * Suspicious Activity Controller
 * 
 * This controller handles HTTP requests related to suspicious activity monitoring,
 * including retrieving activity data and managing the monitoring dashboard.
 */

const suspiciousActivityService = require('../services/suspiciousActivityService');
const { validateApiKey } = require('../middleware/authMiddleware');

/**
 * Get suspicious activity dashboard data
 * @param {Object} req - Express request object
 * @param {Object} res - Express response object
 * @returns {Promise<void>}
 */
async function getDashboardData(req, res) {
  try {
    // Validate API key (admin only)
    if (!validateApiKey(req)) {
      return res.status(401).json({
        success: false,
        message: 'Unauthorized'
      });
    }
    
    // Get dashboard data
    const dashboardData = await suspiciousActivityService.getDashboardData();
    
    // Return dashboard data
    return res.json({
      success: true,
      data: dashboardData
    });
  } catch (error) {
    console.error('Error getting suspicious activity dashboard data:', error);
    return res.status(500).json({
      success: false,
      message: 'Error getting suspicious activity dashboard data'
    });
  }
}

/**
 * Get suspicious activities for a specific entity
 * @param {Object} req - Express request object
 * @param {Object} res - Express response object
 * @returns {Promise<void>}
 */
async function getActivities(req, res) {
  try {
    // Validate API key (admin only)
    if (!validateApiKey(req)) {
      return res.status(401).json({
        success: false,
        message: 'Unauthorized'
      });
    }
    
    // Get query parameters
    const { entityType, entityId } = req.params;
    const { limit, skip, sortBy, sortOrder } = req.query;
    
    // Validate entity type
    if (!['user', 'device', 'ip'].includes(entityType)) {
      return res.status(400).json({
        success: false,
        message: 'Invalid entity type'
      });
    }
    
    // Get activities
    const activities = await suspiciousActivityService.getActivities(
      entityType,
      entityId,
      {
        limit: limit ? parseInt(limit) : undefined,
        skip: skip ? parseInt(skip) : undefined,
        sortBy,
        sortOrder
      }
    );
    
    // Return activities
    return res.json({
      success: true,
      data: activities
    });
  } catch (error) {
    console.error('Error getting suspicious activities:', error);
    return res.status(500).json({
      success: false,
      message: 'Error getting suspicious activities'
    });
  }
}

/**
 * Get reputation score for a specific entity
 * @param {Object} req - Express request object
 * @param {Object} res - Express response object
 * @returns {Promise<void>}
 */
async function getReputationScore(req, res) {
  try {
    // Validate API key (admin only)
    if (!validateApiKey(req)) {
      return res.status(401).json({
        success: false,
        message: 'Unauthorized'
      });
    }
    
    // Get query parameters
    const { entityType, entityId } = req.params;
    
    // Validate entity type
    if (!['user', 'device', 'ip'].includes(entityType)) {
      return res.status(400).json({
        success: false,
        message: 'Invalid entity type'
      });
    }
    
    // Get reputation score
    const score = await suspiciousActivityService.getReputationScore(entityType, entityId);
    
    // Return reputation score
    return res.json({
      success: true,
      data: {
        entityType,
        entityId,
        score
      }
    });
  } catch (error) {
    console.error('Error getting reputation score:', error);
    return res.status(500).json({
      success: false,
      message: 'Error getting reputation score'
    });
  }
}

/**
 * Analyze traffic patterns for a specific entity
 * @param {Object} req - Express request object
 * @param {Object} res - Express response object
 * @returns {Promise<void>}
 */
async function analyzeTrafficPatterns(req, res) {
  try {
    // Validate API key (admin only)
    if (!validateApiKey(req)) {
      return res.status(401).json({
        success: false,
        message: 'Unauthorized'
      });
    }
    
    // Get query parameters
    const { entityType, entityId } = req.params;
    
    // Validate entity type
    if (!['user', 'device', 'ip'].includes(entityType)) {
      return res.status(400).json({
        success: false,
        message: 'Invalid entity type'
      });
    }
    
    // Analyze traffic patterns
    const analysis = await suspiciousActivityService.analyzeTrafficPatterns(entityType, entityId);
    
    // Return analysis
    return res.json({
      success: true,
      data: analysis
    });
  } catch (error) {
    console.error('Error analyzing traffic patterns:', error);
    return res.status(500).json({
      success: false,
      message: 'Error analyzing traffic patterns'
    });
  }
}

/**
 * Check if an entity is suspicious
 * @param {Object} req - Express request object
 * @param {Object} res - Express response object
 * @returns {Promise<void>}
 */
async function checkSuspicious(req, res) {
  try {
    // Validate API key
    if (!validateApiKey(req)) {
      return res.status(401).json({
        success: false,
        message: 'Unauthorized'
      });
    }
    
    // Get query parameters
    const { entityType, entityId } = req.params;
    const { threshold } = req.query;
    
    // Validate entity type
    if (!['user', 'device', 'ip'].includes(entityType)) {
      return res.status(400).json({
        success: false,
        message: 'Invalid entity type'
      });
    }
    
    // Check if entity is suspicious
    const isSuspicious = await suspiciousActivityService.isSuspicious(
      entityType,
      entityId,
      threshold ? parseInt(threshold) : undefined
    );
    
    // Return result
    return res.json({
      success: true,
      data: {
        entityType,
        entityId,
        isSuspicious
      }
    });
  } catch (error) {
    console.error('Error checking if entity is suspicious:', error);
    return res.status(500).json({
      success: false,
      message: 'Error checking if entity is suspicious'
    });
  }
}

module.exports = {
  getDashboardData,
  getActivities,
  getReputationScore,
  analyzeTrafficPatterns,
  checkSuspicious
}; 