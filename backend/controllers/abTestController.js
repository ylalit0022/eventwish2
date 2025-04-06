const abTestService = require('../services/abTestService');
const adMobService = require('../services/adMobService');
const analyticsService = require('../services/analyticsService');
const logger = require('../config/logger');
const { validateObjectId } = require('../utils/validators');

/**
 * A/B Test Controller
 * Handles HTTP requests related to A/B tests
 */
const abTestController = {
  /**
   * Create a new A/B test
   * @param {Object} req - Express request object
   * @param {Object} res - Express response object
   */
  async createTest(req, res) {
    try {
      const testData = req.body;
      
      // Validate required fields
      if (!testData.name || !testData.adType || !testData.variants || testData.variants.length < 2) {
        return res.status(400).json({
          success: false,
          message: 'Missing required fields or insufficient variants'
        });
      }
      
      // Validate variants
      for (const variant of testData.variants) {
        if (!variant.name || !variant.adConfig || !variant.adConfig.adId) {
          return res.status(400).json({
            success: false,
            message: 'Invalid variant configuration'
          });
        }
      }
      
      // Set creator
      testData.createdBy = req.user ? req.user.id : null;
      
      // Create test
      const test = await abTestService.createTest(testData);
      
      // Log creation
      logger.info(`A/B test created: ${test.name}`, {
        testId: test._id,
        userId: req.user ? req.user.id : null
      });
      
      return res.status(201).json({
        success: true,
        data: test
      });
    } catch (error) {
      logger.error(`Error creating A/B test: ${error.message}`, { error });
      return res.status(500).json({
        success: false,
        message: 'Failed to create A/B test',
        error: error.message
      });
    }
  },
  
  /**
   * Update an existing A/B test
   * @param {Object} req - Express request object
   * @param {Object} res - Express response object
   */
  async updateTest(req, res) {
    try {
      const { id } = req.params;
      const updateData = req.body;
      
      // Validate ID
      if (!validateObjectId(id)) {
        return res.status(400).json({
          success: false,
          message: 'Invalid test ID format'
        });
      }
      
      // Set updater
      updateData.updatedBy = req.user ? req.user.id : null;
      updateData.updatedAt = new Date();
      
      // Update test
      const test = await abTestService.updateTest(id, updateData);
      
      // Log update
      logger.info(`A/B test updated: ${test.name}`, {
        testId: test._id,
        userId: req.user ? req.user.id : null
      });
      
      return res.status(200).json({
        success: true,
        data: test
      });
    } catch (error) {
      logger.error(`Error updating A/B test: ${error.message}`, { error });
      
      // Handle specific errors
      if (error.message === 'Test not found') {
        return res.status(404).json({
          success: false,
          message: 'A/B test not found'
        });
      }
      
      if (error.message.includes('Cannot update active test')) {
        return res.status(400).json({
          success: false,
          message: 'Cannot update active test (except for status)'
        });
      }
      
      return res.status(500).json({
        success: false,
        message: 'Failed to update A/B test',
        error: error.message
      });
    }
  },
  
  /**
   * Get an A/B test by ID
   * @param {Object} req - Express request object
   * @param {Object} res - Express response object
   */
  async getTest(req, res) {
    try {
      const { id } = req.params;
      
      // Validate ID
      if (!validateObjectId(id)) {
        return res.status(400).json({
          success: false,
          message: 'Invalid test ID format'
        });
      }
      
      // Get test
      const test = await abTestService.getTestById(id);
      
      return res.status(200).json({
        success: true,
        data: test
      });
    } catch (error) {
      logger.error(`Error getting A/B test: ${error.message}`, { error });
      
      // Handle specific errors
      if (error.message === 'Test not found') {
        return res.status(404).json({
          success: false,
          message: 'A/B test not found'
        });
      }
      
      return res.status(500).json({
        success: false,
        message: 'Failed to get A/B test',
        error: error.message
      });
    }
  },
  
  /**
   * Get all A/B tests with optional filters
   * @param {Object} req - Express request object
   * @param {Object} res - Express response object
   */
  async getTests(req, res) {
    try {
      const { status, adType } = req.query;
      
      // Build filters
      const filters = {};
      
      if (status) {
        filters.status = status;
      }
      
      if (adType) {
        filters.adType = adType;
      }
      
      // Get tests
      const tests = await abTestService.getTests(filters);
      
      return res.status(200).json({
        success: true,
        count: tests.length,
        data: tests
      });
    } catch (error) {
      logger.error(`Error getting A/B tests: ${error.message}`, { error });
      return res.status(500).json({
        success: false,
        message: 'Failed to get A/B tests',
        error: error.message
      });
    }
  },
  
  /**
   * Delete an A/B test
   * @param {Object} req - Express request object
   * @param {Object} res - Express response object
   */
  async deleteTest(req, res) {
    try {
      const { id } = req.params;
      
      // Validate ID
      if (!validateObjectId(id)) {
        return res.status(400).json({
          success: false,
          message: 'Invalid test ID format'
        });
      }
      
      // Delete test
      await abTestService.deleteTest(id);
      
      // Log deletion
      logger.info(`A/B test deleted: ${id}`, {
        testId: id,
        userId: req.user ? req.user.id : null
      });
      
      return res.status(200).json({
        success: true,
        message: 'A/B test deleted successfully'
      });
    } catch (error) {
      logger.error(`Error deleting A/B test: ${error.message}`, { error });
      
      // Handle specific errors
      if (error.message === 'Test not found') {
        return res.status(404).json({
          success: false,
          message: 'A/B test not found'
        });
      }
      
      if (error.message.includes('Cannot delete active test')) {
        return res.status(400).json({
          success: false,
          message: 'Cannot delete active test'
        });
      }
      
      return res.status(500).json({
        success: false,
        message: 'Failed to delete A/B test',
        error: error.message
      });
    }
  },
  
  /**
   * Start an A/B test
   * @param {Object} req - Express request object
   * @param {Object} res - Express response object
   */
  async startTest(req, res) {
    try {
      const { id } = req.params;
      
      // Validate ID
      if (!validateObjectId(id)) {
        return res.status(400).json({
          success: false,
          message: 'Invalid test ID format'
        });
      }
      
      // Update test status to active
      const test = await abTestService.updateTest(id, {
        status: 'active',
        startDate: new Date(),
        updatedBy: req.user ? req.user.id : null
      });
      
      // Log start
      logger.info(`A/B test started: ${test.name}`, {
        testId: test._id,
        userId: req.user ? req.user.id : null
      });
      
      return res.status(200).json({
        success: true,
        data: test
      });
    } catch (error) {
      logger.error(`Error starting A/B test: ${error.message}`, { error });
      
      // Handle specific errors
      if (error.message === 'Test not found') {
        return res.status(404).json({
          success: false,
          message: 'A/B test not found'
        });
      }
      
      return res.status(500).json({
        success: false,
        message: 'Failed to start A/B test',
        error: error.message
      });
    }
  },
  
  /**
   * Stop an A/B test
   * @param {Object} req - Express request object
   * @param {Object} res - Express response object
   */
  async stopTest(req, res) {
    try {
      const { id } = req.params;
      
      // Validate ID
      if (!validateObjectId(id)) {
        return res.status(400).json({
          success: false,
          message: 'Invalid test ID format'
        });
      }
      
      // Update test status to completed
      const test = await abTestService.updateTest(id, {
        status: 'completed',
        endDate: new Date(),
        updatedBy: req.user ? req.user.id : null
      });
      
      // Log stop
      logger.info(`A/B test stopped: ${test.name}`, {
        testId: test._id,
        userId: req.user ? req.user.id : null
      });
      
      return res.status(200).json({
        success: true,
        data: test
      });
    } catch (error) {
      logger.error(`Error stopping A/B test: ${error.message}`, { error });
      
      // Handle specific errors
      if (error.message === 'Test not found') {
        return res.status(404).json({
          success: false,
          message: 'A/B test not found'
        });
      }
      
      return res.status(500).json({
        success: false,
        message: 'Failed to stop A/B test',
        error: error.message
      });
    }
  },
  
  /**
   * Get optimal ad configuration for a user based on A/B tests
   * @param {Object} req - Express request object
   * @param {Object} res - Express response object
   */
  async getOptimalAdConfig(req, res) {
    try {
      const { adType } = req.params;
      
      // Validate ad type
      if (!adType) {
        return res.status(400).json({
          success: false,
          message: 'Ad type is required'
        });
      }
      
      // Get user context from request
      const context = {
        userId: req.query.userId || req.headers['x-user-id'],
        deviceId: req.query.deviceId || req.headers['x-device-id'],
        deviceType: req.query.deviceType || req.headers['x-device-type'],
        platform: req.query.platform || req.headers['x-platform'],
        country: req.query.country || req.headers['x-country'],
        language: req.query.language || req.headers['x-language'],
        appVersion: req.query.appVersion || req.headers['x-app-version'],
        ip: req.ip
      };
      
      // Get optimal ad config
      const adConfig = await abTestService.getOptimalAdConfig(adType, context);
      
      // If no ad config from A/B test, get default ad config
      if (!adConfig) {
        const defaultAd = await adMobService.getActiveAdByType(adType);
        
        if (!defaultAd) {
          return res.status(404).json({
            success: false,
            message: `No active ads found for type: ${adType}`
          });
        }
        
        return res.status(200).json({
          success: true,
          data: {
            adId: defaultAd._id,
            adName: defaultAd.adName,
            adType: defaultAd.adType,
            adUnitCode: defaultAd.adUnitCode,
            parameters: defaultAd.parameters,
            isTest: false
          }
        });
      }
      
      // Return ad config from A/B test
      return res.status(200).json({
        success: true,
        data: {
          ...adConfig,
          isTest: true
        }
      });
    } catch (error) {
      logger.error(`Error getting optimal ad config: ${error.message}`, { error });
      return res.status(500).json({
        success: false,
        message: 'Failed to get optimal ad configuration',
        error: error.message
      });
    }
  },
  
  /**
   * Track a test event (impression, click, etc.)
   * @param {Object} req - Express request object
   * @param {Object} res - Express response object
   */
  async trackTestEvent(req, res) {
    try {
      const { testId, variantId, eventType } = req.params;
      
      // Validate parameters
      if (!validateObjectId(testId) || !validateObjectId(variantId) || !eventType) {
        return res.status(400).json({
          success: false,
          message: 'Invalid parameters'
        });
      }
      
      // Get context from request
      const context = {
        userId: req.query.userId || req.headers['x-user-id'],
        deviceId: req.query.deviceId || req.headers['x-device-id'],
        ip: req.ip,
        userAgent: req.headers['user-agent'],
        referer: req.headers['referer']
      };
      
      // Track event
      const success = await abTestService.trackTestEvent(testId, variantId, eventType, context);
      
      // Also track in analytics service
      if (success && (eventType === 'impressions' || eventType === 'clicks')) {
        try {
          // Get test and variant details
          const test = await abTestService.getTestById(testId);
          const variant = test.variants.id(variantId);
          
          if (variant && variant.adConfig && variant.adConfig.adId) {
            const adId = variant.adConfig.adId._id || variant.adConfig.adId;
            
            // Track in analytics service
            if (eventType === 'impressions') {
              await analyticsService.trackImpression(adId, context);
            } else if (eventType === 'clicks') {
              await analyticsService.trackClick(adId, context);
            }
          }
        } catch (analyticsError) {
          logger.error(`Error tracking event in analytics service: ${analyticsError.message}`, { analyticsError });
          // Continue execution even if analytics tracking fails
        }
      }
      
      if (!success) {
        return res.status(404).json({
          success: false,
          message: 'Failed to track event'
        });
      }
      
      return res.status(200).json({
        success: true,
        message: 'Event tracked successfully'
      });
    } catch (error) {
      logger.error(`Error tracking test event: ${error.message}`, { error });
      return res.status(500).json({
        success: false,
        message: 'Failed to track test event',
        error: error.message
      });
    }
  },
  
  /**
   * Get test results
   * @param {Object} req - Express request object
   * @param {Object} res - Express response object
   */
  async getTestResults(req, res) {
    try {
      const { id } = req.params;
      
      // Validate ID
      if (!validateObjectId(id)) {
        return res.status(400).json({
          success: false,
          message: 'Invalid test ID format'
        });
      }
      
      // Calculate results
      const results = await abTestService.calculateTestResults(id);
      
      return res.status(200).json({
        success: true,
        data: results
      });
    } catch (error) {
      logger.error(`Error getting test results: ${error.message}`, { error });
      
      // Handle specific errors
      if (error.message === 'Test not found') {
        return res.status(404).json({
          success: false,
          message: 'A/B test not found'
        });
      }
      
      return res.status(500).json({
        success: false,
        message: 'Failed to get test results',
        error: error.message
      });
    }
  }
};

module.exports = abTestController; 