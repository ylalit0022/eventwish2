const { ABTest } = require('../models/ABTest');
const { AdMob } = require('../models/AdMob');
const cacheService = require('./cacheService');
const logger = require('../config/logger');
const mongoose = require('mongoose');
const crypto = require('crypto');

// Cache namespace for A/B tests
const abTestCache = cacheService.namespace('abtest');

/**
 * A/B Test Service
 * Provides methods for managing and evaluating A/B tests
 */
const abTestService = {
  /**
   * Create a new A/B test
   * @param {Object} testData - Test data
   * @returns {Promise<Object>} Created test
   */
  async createTest(testData) {
    try {
      // Validate ad IDs in variants
      for (const variant of testData.variants) {
        const adExists = await AdMob.exists({ _id: variant.adConfig.adId });
        if (!adExists) {
          throw new Error(`Ad with ID ${variant.adConfig.adId} does not exist`);
        }
      }
      
      // Create test
      const test = new ABTest(testData);
      await test.save();
      
      // Invalidate cache
      this.invalidateCache();
      
      return test;
    } catch (error) {
      logger.error(`Error creating A/B test: ${error.message}`, { error });
      throw error;
    }
  },
  
  /**
   * Update an existing A/B test
   * @param {string} testId - Test ID
   * @param {Object} updateData - Update data
   * @returns {Promise<Object>} Updated test
   */
  async updateTest(testId, updateData) {
    try {
      // Validate test ID
      if (!mongoose.Types.ObjectId.isValid(testId)) {
        throw new Error('Invalid test ID format');
      }
      
      // Find test
      const test = await ABTest.findById(testId);
      if (!test) {
        throw new Error('Test not found');
      }
      
      // Prevent updating active tests (except for status)
      if (test.status === 'active' && Object.keys(updateData).some(key => key !== 'status')) {
        throw new Error('Cannot update active test (except for status)');
      }
      
      // Update test
      Object.assign(test, updateData);
      await test.save();
      
      // Invalidate cache
      this.invalidateCache(testId);
      
      return test;
    } catch (error) {
      logger.error(`Error updating A/B test: ${error.message}`, { error, testId });
      throw error;
    }
  },
  
  /**
   * Get an A/B test by ID
   * @param {string} testId - Test ID
   * @returns {Promise<Object>} Test
   */
  async getTestById(testId) {
    try {
      // Validate test ID
      if (!mongoose.Types.ObjectId.isValid(testId)) {
        throw new Error('Invalid test ID format');
      }
      
      // Check cache
      const cacheKey = `test_${testId}`;
      const cachedTest = abTestCache.get(cacheKey);
      if (cachedTest) {
        return cachedTest;
      }
      
      // Find test
      const test = await ABTest.findById(testId).populate('variants.adConfig.adId');
      if (!test) {
        throw new Error('Test not found');
      }
      
      // Cache test
      abTestCache.set(cacheKey, test, 3600); // Cache for 1 hour
      
      return test;
    } catch (error) {
      logger.error(`Error getting A/B test: ${error.message}`, { error, testId });
      throw error;
    }
  },
  
  /**
   * Get all A/B tests with optional filters
   * @param {Object} filters - Filters
   * @returns {Promise<Array>} Tests
   */
  async getTests(filters = {}) {
    try {
      // Build query
      const query = {};
      
      if (filters.status) {
        query.status = filters.status;
      }
      
      if (filters.adType) {
        query.adType = filters.adType;
      }
      
      // Check cache
      const cacheKey = `tests_${JSON.stringify(filters)}`;
      const cachedTests = abTestCache.get(cacheKey);
      if (cachedTests) {
        return cachedTests;
      }
      
      // Find tests
      const tests = await ABTest.find(query).sort({ createdAt: -1 });
      
      // Cache tests
      abTestCache.set(cacheKey, tests, 3600); // Cache for 1 hour
      
      return tests;
    } catch (error) {
      logger.error(`Error getting A/B tests: ${error.message}`, { error, filters });
      throw error;
    }
  },
  
  /**
   * Delete an A/B test
   * @param {string} testId - Test ID
   * @returns {Promise<boolean>} Success
   */
  async deleteTest(testId) {
    try {
      // Validate test ID
      if (!mongoose.Types.ObjectId.isValid(testId)) {
        throw new Error('Invalid test ID format');
      }
      
      // Find test
      const test = await ABTest.findById(testId);
      if (!test) {
        throw new Error('Test not found');
      }
      
      // Prevent deleting active tests
      if (test.status === 'active') {
        throw new Error('Cannot delete active test');
      }
      
      // Delete test
      await ABTest.deleteOne({ _id: testId });
      
      // Invalidate cache
      this.invalidateCache(testId);
      
      return true;
    } catch (error) {
      logger.error(`Error deleting A/B test: ${error.message}`, { error, testId });
      throw error;
    }
  },
  
  /**
   * Get active tests for a specific ad type
   * @param {string} adType - Ad type
   * @returns {Promise<Array>} Active tests
   */
  async getActiveTestsByAdType(adType) {
    try {
      // Check cache
      const cacheKey = `active_tests_${adType}`;
      const cachedTests = abTestCache.get(cacheKey);
      if (cachedTests) {
        return cachedTests;
      }
      
      // Find active tests
      const tests = await ABTest.find({
        adType,
        status: 'active',
        startDate: { $lte: new Date() },
        $or: [
          { endDate: { $exists: false } },
          { endDate: null },
          { endDate: { $gte: new Date() } }
        ]
      }).populate('variants.adConfig.adId');
      
      // Cache tests
      abTestCache.set(cacheKey, tests, 3600); // Cache for 1 hour
      
      return tests;
    } catch (error) {
      logger.error(`Error getting active tests: ${error.message}`, { error, adType });
      throw error;
    }
  },
  
  /**
   * Evaluate user context against targeting rules
   * @param {Object} test - A/B test
   * @param {Object} context - User context
   * @returns {boolean} Whether the user matches the targeting rules
   */
  evaluateTargetingRules(test, context) {
    // If no targeting rules, include all users
    if (!test.targetingRules || test.targetingRules.length === 0) {
      return true;
    }
    
    // Evaluate each rule
    return test.targetingRules.every(rule => {
      let fieldValue;
      
      // Get field value based on rule type
      switch (rule.type) {
        case 'device':
          fieldValue = context.deviceType;
          break;
        case 'platform':
          fieldValue = context.platform;
          break;
        case 'country':
          fieldValue = context.country;
          break;
        case 'language':
          fieldValue = context.language;
          break;
        case 'appVersion':
          fieldValue = context.appVersion;
          break;
        case 'custom':
          fieldValue = context.field ? context[rule.field] : undefined;
          break;
        default:
          return false;
      }
      
      // If field value is undefined, rule doesn't match
      if (fieldValue === undefined) {
        return false;
      }
      
      // Evaluate rule based on operator
      switch (rule.operator) {
        case 'equals':
          return fieldValue === rule.value;
        case 'notEquals':
          return fieldValue !== rule.value;
        case 'contains':
          return String(fieldValue).includes(String(rule.value));
        case 'notContains':
          return !String(fieldValue).includes(String(rule.value));
        case 'startsWith':
          return String(fieldValue).startsWith(String(rule.value));
        case 'endsWith':
          return String(fieldValue).endsWith(String(rule.value));
        case 'regex':
          try {
            const regex = new RegExp(rule.value);
            return regex.test(String(fieldValue));
          } catch (error) {
            logger.error(`Invalid regex in targeting rule: ${error.message}`, { error, rule });
            return false;
          }
        case 'greaterThan':
          return Number(fieldValue) > Number(rule.value);
        case 'lessThan':
          return Number(fieldValue) < Number(rule.value);
        default:
          return false;
      }
    });
  },
  
  /**
   * Determine if a user should be included in a test based on traffic allocation
   * @param {Object} test - A/B test
   * @param {string} userId - User ID
   * @returns {boolean} Whether the user is included in the test
   */
  isUserInTestTraffic(test, userId) {
    // If traffic allocation is 100%, include all users
    if (test.trafficAllocation === 100) {
      return true;
    }
    
    // Generate a deterministic hash based on test ID and user ID
    const hash = crypto.createHash('md5').update(`${test.id}:${userId}`).digest('hex');
    
    // Convert hash to a number between 0 and 99
    const hashValue = parseInt(hash.substring(0, 8), 16) % 100;
    
    // Include user if hash value is less than traffic allocation
    return hashValue < test.trafficAllocation;
  },
  
  /**
   * Select a variant for a user based on variant weights
   * @param {Object} test - A/B test
   * @param {string} userId - User ID
   * @returns {Object} Selected variant
   */
  selectVariant(test, userId) {
    // Generate a deterministic hash based on test ID and user ID
    const hash = crypto.createHash('md5').update(`${test.id}:${userId}`).digest('hex');
    
    // Convert hash to a number between 0 and 99
    const hashValue = parseInt(hash.substring(0, 8), 16) % 100;
    
    // Select variant based on weights
    let cumulativeWeight = 0;
    for (const variant of test.variants) {
      cumulativeWeight += variant.weight;
      if (hashValue < cumulativeWeight) {
        return variant;
      }
    }
    
    // Fallback to first variant
    return test.variants[0];
  },
  
  /**
   * Get optimal ad configuration for a user based on A/B tests
   * @param {string} adType - Ad type
   * @param {Object} context - User context
   * @returns {Promise<Object>} Ad configuration
   */
  async getOptimalAdConfig(adType, context) {
    try {
      // Get active tests for this ad type
      const activeTests = await this.getActiveTestsByAdType(adType);
      
      // If no active tests, return null
      if (activeTests.length === 0) {
        return null;
      }
      
      // Get user ID from context (or generate a temporary one)
      const userId = context.userId || context.deviceId || crypto.randomBytes(16).toString('hex');
      
      // Find the first test that matches the user
      for (const test of activeTests) {
        // Check if user matches targeting rules
        if (!this.evaluateTargetingRules(test, context)) {
          continue;
        }
        
        // Check if user is in test traffic
        if (!this.isUserInTestTraffic(test, userId)) {
          continue;
        }
        
        // Select variant for user
        const variant = this.selectVariant(test, userId);
        
        // Return ad configuration with test metadata
        return {
          adId: variant.adConfig.adId._id || variant.adConfig.adId,
          adName: variant.adConfig.adId.adName,
          adType: variant.adConfig.adId.adType,
          adUnitCode: variant.adConfig.adId.adUnitCode,
          parameters: variant.adConfig.parameters,
          testId: test._id,
          testName: test.name,
          variantId: variant._id,
          variantName: variant.name
        };
      }
      
      // No matching test found
      return null;
    } catch (error) {
      logger.error(`Error getting optimal ad config: ${error.message}`, { error, adType, context });
      return null;
    }
  },
  
  /**
   * Track a test event (impression, click, etc.)
   * @param {string} testId - Test ID
   * @param {string} variantId - Variant ID
   * @param {string} eventType - Event type
   * @param {Object} context - Event context
   * @returns {Promise<boolean>} Success
   */
  async trackTestEvent(testId, variantId, eventType, context = {}) {
    try {
      // Validate test ID
      if (!mongoose.Types.ObjectId.isValid(testId)) {
        throw new Error('Invalid test ID format');
      }
      
      // Find test
      const test = await ABTest.findById(testId);
      if (!test) {
        throw new Error('Test not found');
      }
      
      // Validate event type
      if (!test.metrics.includes(eventType)) {
        throw new Error(`Invalid event type: ${eventType}`);
      }
      
      // Find variant
      const variant = test.variants.id(variantId);
      if (!variant) {
        throw new Error('Variant not found');
      }
      
      // Initialize results if needed
      if (!test.results) {
        test.results = new Map();
      }
      
      // Initialize event counts if needed
      if (!test.results.get(eventType)) {
        test.results.set(eventType, {
          total: 0,
          byVariant: {}
        });
      }
      
      // Initialize variant counts if needed
      const eventData = test.results.get(eventType);
      if (!eventData.byVariant[variantId]) {
        eventData.byVariant[variantId] = 0;
      }
      
      // Increment counts
      eventData.total += 1;
      eventData.byVariant[variantId] += 1;
      
      // Update test
      await test.save();
      
      // Invalidate cache
      this.invalidateCache(testId);
      
      return true;
    } catch (error) {
      logger.error(`Error tracking test event: ${error.message}`, { error, testId, variantId, eventType });
      return false;
    }
  },
  
  /**
   * Calculate test results
   * @param {string} testId - Test ID
   * @returns {Promise<Object>} Test results
   */
  async calculateTestResults(testId) {
    try {
      // Validate test ID
      if (!mongoose.Types.ObjectId.isValid(testId)) {
        throw new Error('Invalid test ID format');
      }
      
      // Find test
      const test = await ABTest.findById(testId);
      if (!test) {
        throw new Error('Test not found');
      }
      
      // If no results, return empty object
      if (!test.results || test.results.size === 0) {
        return {
          testId: test._id,
          testName: test.name,
          variants: test.variants.map(variant => ({
            variantId: variant._id,
            variantName: variant.name,
            metrics: {}
          }))
        };
      }
      
      // Calculate results for each variant
      const variantResults = test.variants.map(variant => {
        const metrics = {};
        
        // Calculate metrics
        for (const metric of test.metrics) {
          const eventData = test.results.get(metric);
          if (!eventData) {
            metrics[metric] = 0;
            continue;
          }
          
          const variantCount = eventData.byVariant[variant._id] || 0;
          metrics[metric] = variantCount;
          
          // Calculate conversion rate for clicks
          if (metric === 'clicks' && test.results.has('impressions')) {
            const impressions = test.results.get('impressions').byVariant[variant._id] || 0;
            metrics.ctr = impressions > 0 ? (variantCount / impressions) * 100 : 0;
          }
        }
        
        return {
          variantId: variant._id,
          variantName: variant.name,
          metrics
        };
      });
      
      // Calculate statistical significance
      // (This is a simplified implementation - in a real-world scenario, you'd use a proper statistical test)
      if (test.results.has('impressions') && test.results.has('clicks') && test.variants.length >= 2) {
        const controlVariant = variantResults[0];
        const controlCTR = controlVariant.metrics.ctr || 0;
        
        for (let i = 1; i < variantResults.length; i++) {
          const variant = variantResults[i];
          const variantCTR = variant.metrics.ctr || 0;
          
          // Calculate relative improvement
          variant.metrics.improvement = controlCTR > 0 ? ((variantCTR - controlCTR) / controlCTR) * 100 : 0;
          
          // Simplified statistical significance calculation
          // (In a real implementation, you'd use a proper statistical test like chi-squared)
          const controlImpressions = controlVariant.metrics.impressions || 0;
          const controlClicks = controlVariant.metrics.clicks || 0;
          const variantImpressions = variant.metrics.impressions || 0;
          const variantClicks = variant.metrics.clicks || 0;
          
          if (controlImpressions > 100 && variantImpressions > 100) {
            const controlStdErr = Math.sqrt((controlCTR * (100 - controlCTR)) / controlImpressions);
            const variantStdErr = Math.sqrt((variantCTR * (100 - variantCTR)) / variantImpressions);
            const zScore = Math.abs(variantCTR - controlCTR) / Math.sqrt(Math.pow(controlStdErr, 2) + Math.pow(variantStdErr, 2));
            
            // z-score > 1.96 corresponds to p < 0.05 (95% confidence)
            variant.metrics.significant = zScore > 1.96;
            variant.metrics.confidence = Math.min(99.9, (1 - Math.exp(-0.5 * Math.pow(zScore, 2))) * 100);
          } else {
            variant.metrics.significant = false;
            variant.metrics.confidence = 0;
          }
        }
      }
      
      return {
        testId: test._id,
        testName: test.name,
        variants: variantResults
      };
    } catch (error) {
      logger.error(`Error calculating test results: ${error.message}`, { error, testId });
      throw error;
    }
  },
  
  /**
   * Invalidate cache for a specific test or all tests
   * @param {string} testId - Test ID (optional)
   */
  invalidateCache(testId = null) {
    if (testId) {
      // Invalidate specific test
      abTestCache.del(`test_${testId}`);
    }
    
    // Invalidate all tests
    const keys = abTestCache.keys();
    for (const key of keys) {
      if (key.startsWith('tests_') || key.startsWith('active_tests_')) {
        abTestCache.del(key);
      }
    }
  }
};

module.exports = abTestService; 