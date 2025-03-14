/**
 * Fraud Controller
 * 
 * This controller handles HTTP requests related to fraud detection,
 * including checking for fraud and retrieving fraud statistics.
 */

const fraudDetectionService = require('../services/fraudDetectionService');
const monitoringService = require('../services/monitoringService');
const { validateApiKey } = require('../middleware/authMiddleware');

/**
 * Check if a click is potentially fraudulent
 * @param {Object} req - Express request object
 * @param {Object} res - Express response object
 * @returns {Promise<void>}
 */
async function checkFraud(req, res) {
  try {
    // Validate request
    if (!req.body || !req.body.userId || !req.body.deviceId || !req.body.adId) {
      return res.status(400).json({
        success: false,
        message: 'Missing required fields'
      });
    }
    
    // Get client IP
    const ip = req.headers['x-forwarded-for'] || req.connection.remoteAddress;
    
    // Get user agent
    const userAgent = req.headers['user-agent'] || 'unknown';
    
    // Prepare click data
    const clickData = {
      ...req.body,
      ip,
      timestamp: Date.now(),
      context: {
        ...req.body.context,
        userAgent,
        headers: {
          ...req.headers,
          // Remove sensitive headers
          authorization: undefined,
          cookie: undefined
        },
        // Add screen information if available
        screen: req.body.context && req.body.context.screen ? req.body.context.screen : undefined,
        // Add referrer if available
        referrer: req.headers.referer || req.headers.referrer || 'unknown'
      }
    };
    
    // Process click and check for fraud
    const result = await fraudDetectionService.processClick(clickData);
    
    // Track metrics
    monitoringService.incrementCounter('fraud_checks');
    
    // Return result
    return res.json({
      success: true,
      allowed: result.allowed,
      score: result.fraudResult ? result.fraudResult.score : 0,
      reasons: result.fraudResult && result.fraudResult.isFraudulent ? result.fraudResult.reasons : []
    });
  } catch (error) {
    console.error('Error checking fraud:', error);
    return res.status(500).json({
      success: false,
      message: 'Error checking fraud'
    });
  }
}

/**
 * Get fraud detection statistics
 * @param {Object} req - Express request object
 * @param {Object} res - Express response object
 * @returns {Promise<void>}
 */
async function getStatistics(req, res) {
  try {
    // Validate API key (admin only)
    if (!validateApiKey(req)) {
      return res.status(401).json({
        success: false,
        message: 'Unauthorized'
      });
    }
    
    // Get statistics
    const totalClicks = await monitoringService.getCounter('total_clicks');
    const fraudClicks = await monitoringService.getCounter('fraud_clicks');
    const proxyFraudClicks = await monitoringService.getCounter('proxy_fraud_clicks');
    const vpnFraudClicks = await monitoringService.getCounter('vpn_fraud_clicks');
    const datacenterFraudClicks = await monitoringService.getCounter('datacenter_fraud_clicks');
    const fraudRate = totalClicks > 0 ? (fraudClicks / totalClicks) * 100 : 0;
    
    // Get average fraud score
    const fraudScores = await monitoringService.getMetricValues('fraud_score');
    const avgFraudScore = fraudScores.length > 0
      ? fraudScores.reduce((sum, score) => sum + score, 0) / fraudScores.length
      : 0;
    
    // Return statistics
    return res.json({
      success: true,
      statistics: {
        totalClicks,
        fraudClicks,
        fraudRate: parseFloat(fraudRate.toFixed(2)),
        avgFraudScore: parseFloat(avgFraudScore.toFixed(2)),
        fingerprintStats: {
          proxyFraudClicks,
          vpnFraudClicks,
          datacenterFraudClicks
        }
      }
    });
  } catch (error) {
    console.error('Error getting fraud statistics:', error);
    return res.status(500).json({
      success: false,
      message: 'Error getting fraud statistics'
    });
  }
}

/**
 * Get fraud detection thresholds
 * @param {Object} req - Express request object
 * @param {Object} res - Express response object
 * @returns {Promise<void>}
 */
async function getThresholds(req, res) {
  try {
    // Validate API key (admin only)
    if (!validateApiKey(req)) {
      return res.status(401).json({
        success: false,
        message: 'Unauthorized'
      });
    }
    
    // Get thresholds
    const thresholds = await fraudDetectionService.getThresholds();
    
    // Return thresholds
    return res.json({
      success: true,
      thresholds
    });
  } catch (error) {
    console.error('Error getting fraud thresholds:', error);
    return res.status(500).json({
      success: false,
      message: 'Error getting fraud thresholds'
    });
  }
}

/**
 * Update fraud detection thresholds
 * @param {Object} req - Express request object
 * @param {Object} res - Express response object
 * @returns {Promise<void>}
 */
async function updateThresholds(req, res) {
  try {
    // Validate API key (admin only)
    if (!validateApiKey(req)) {
      return res.status(401).json({
        success: false,
        message: 'Unauthorized'
      });
    }
    
    // Validate request
    if (!req.body || typeof req.body !== 'object') {
      return res.status(400).json({
        success: false,
        message: 'Invalid thresholds'
      });
    }
    
    // Update thresholds
    const updatedThresholds = await fraudDetectionService.updateThresholds(req.body);
    
    // Return updated thresholds
    return res.json({
      success: true,
      thresholds: updatedThresholds
    });
  } catch (error) {
    console.error('Error updating fraud thresholds:', error);
    return res.status(500).json({
      success: false,
      message: 'Error updating fraud thresholds'
    });
  }
}

module.exports = {
  checkFraud,
  getStatistics,
  getThresholds,
  updateThresholds
}; 