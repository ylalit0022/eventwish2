const mongoose = require('mongoose');
const logger = require('../config/logger');
const { AdMob } = require('../models/AdMob');
const NodeCache = require('node-cache');

// Initialize cache with standard TTL of 1 hour and check period of 60 seconds
const analyticsCache = new NodeCache({ stdTTL: 3600, checkperiod: 60 });
const ANALYTICS_CACHE_KEY_PREFIX = 'admob_analytics_';

/**
 * Track ad impression in analytics
 * @param {string} adId - The ID of the ad that was impressed
 * @param {Object} context - Context of the impression (device, location, etc.)
 * @returns {Promise<Object>} - The updated analytics data
 */
const trackImpression = async (adId, context = {}) => {
  try {
    // Validate adId
    if (!mongoose.Types.ObjectId.isValid(adId)) {
      logger.warn(`Invalid ad ID format for analytics: ${adId}`);
      throw new Error('Invalid ad ID format');
    }
    
    // Get the ad
    const ad = await AdMob.findById(adId);
    
    if (!ad) {
      logger.warn(`Ad with ID ${adId} not found for analytics tracking`);
      throw new Error('Ad not found');
    }
    
    // Update impression count
    ad.impressions = (ad.impressions || 0) + 1;
    
    // Store context data for analytics
    const timestamp = new Date();
    const analyticsEntry = {
      type: 'impression',
      adId: ad._id,
      adType: ad.adType,
      adName: ad.adName,
      timestamp,
      context: {
        ...context,
        ip: context.ip || 'unknown',
        userAgent: context.userAgent || 'unknown',
        deviceType: context.deviceType || 'unknown',
        platform: context.platform || 'unknown'
      }
    };
    
    // Store analytics entry in database
    // In a production environment, we would use a dedicated analytics collection
    // For now, we'll just update the ad document
    
    // Add to impressionData array (limited to last 100 entries)
    if (!ad.impressionData) {
      ad.impressionData = [];
    }
    
    ad.impressionData.unshift({
      timestamp,
      context: analyticsEntry.context
    });
    
    // Limit array size to prevent document growth
    if (ad.impressionData.length > 100) {
      ad.impressionData = ad.impressionData.slice(0, 100);
    }
    
    // Save the updated ad
    await ad.save();
    
    // Invalidate analytics cache
    invalidateAnalyticsCache(adId);
    
    logger.debug(`Analytics: Impression tracked for ad ID: ${adId}`);
    
    return analyticsEntry;
  } catch (error) {
    logger.error(`Analytics error tracking impression: ${error.message}`);
    throw new Error(`Analytics failed to track impression: ${error.message}`);
  }
};

/**
 * Track ad click in analytics
 * @param {string} adId - The ID of the ad that was clicked
 * @param {Object} context - Context of the click (device, location, etc.)
 * @returns {Promise<Object>} - The updated analytics data
 */
const trackClick = async (adId, context = {}) => {
  try {
    // Validate adId
    if (!mongoose.Types.ObjectId.isValid(adId)) {
      logger.warn(`Invalid ad ID format for analytics: ${adId}`);
      throw new Error('Invalid ad ID format');
    }
    
    // Get the ad
    const ad = await AdMob.findById(adId);
    
    if (!ad) {
      logger.warn(`Ad with ID ${adId} not found for analytics tracking`);
      throw new Error('Ad not found');
    }
    
    // Update click count
    ad.clicks = (ad.clicks || 0) + 1;
    
    // Calculate CTR (Click-Through Rate)
    if (ad.impressions && ad.impressions > 0) {
      ad.ctr = (ad.clicks / ad.impressions) * 100;
    }
    
    // Store context data for analytics
    const timestamp = new Date();
    const analyticsEntry = {
      type: 'click',
      adId: ad._id,
      adType: ad.adType,
      adName: ad.adName,
      timestamp,
      context: {
        ...context,
        ip: context.ip || 'unknown',
        userAgent: context.userAgent || 'unknown',
        deviceType: context.deviceType || 'unknown',
        platform: context.platform || 'unknown'
      }
    };
    
    // Store analytics entry in database
    // In a production environment, we would use a dedicated analytics collection
    // For now, we'll just update the ad document
    
    // Add to clickData array (limited to last 100 entries)
    if (!ad.clickData) {
      ad.clickData = [];
    }
    
    ad.clickData.unshift({
      timestamp,
      context: analyticsEntry.context
    });
    
    // Limit array size to prevent document growth
    if (ad.clickData.length > 100) {
      ad.clickData = ad.clickData.slice(0, 100);
    }
    
    // Save the updated ad
    await ad.save();
    
    // Invalidate analytics cache
    invalidateAnalyticsCache(adId);
    
    logger.debug(`Analytics: Click tracked for ad ID: ${adId}`);
    
    return analyticsEntry;
  } catch (error) {
    logger.error(`Analytics error tracking click: ${error.message}`);
    throw new Error(`Analytics failed to track click: ${error.message}`);
  }
};

/**
 * Get analytics data for a specific ad
 * @param {string} adId - The ID of the ad to get analytics for
 * @returns {Promise<Object>} - The analytics data
 */
const getAdAnalytics = async (adId) => {
  try {
    // Check cache first
    const cacheKey = `${ANALYTICS_CACHE_KEY_PREFIX}${adId}`;
    const cachedAnalytics = analyticsCache.get(cacheKey);
    
    if (cachedAnalytics) {
      logger.debug(`Analytics cache hit for ad ID: ${adId}`);
      return cachedAnalytics;
    }
    
    // Validate adId
    if (!mongoose.Types.ObjectId.isValid(adId)) {
      logger.warn(`Invalid ad ID format for analytics: ${adId}`);
      throw new Error('Invalid ad ID format');
    }
    
    // Get the ad
    const ad = await AdMob.findById(adId);
    
    if (!ad) {
      logger.warn(`Ad with ID ${adId} not found for analytics`);
      throw new Error('Ad not found');
    }
    
    // Build analytics data
    const analytics = {
      adId: ad._id,
      adName: ad.adName,
      adType: ad.adType,
      impressions: ad.impressions || 0,
      clicks: ad.clicks || 0,
      ctr: ad.ctr || 0,
      revenue: ad.revenue || 0,
      recentImpressions: ad.impressionData || [],
      recentClicks: ad.clickData || [],
      createdAt: ad.createdAt,
      updatedAt: ad.updatedAt
    };
    
    // Store in cache
    analyticsCache.set(cacheKey, analytics);
    
    return analytics;
  } catch (error) {
    logger.error(`Analytics error getting ad analytics: ${error.message}`);
    throw new Error(`Failed to get ad analytics: ${error.message}`);
  }
};

/**
 * Get summary analytics for all ads
 * @param {Object} filters - Filters to apply (adType, dateRange, etc.)
 * @returns {Promise<Array>} - Array of analytics data
 */
const getSummaryAnalytics = async (filters = {}) => {
  try {
    // Build cache key based on filters
    const filterKey = JSON.stringify(filters);
    const cacheKey = `${ANALYTICS_CACHE_KEY_PREFIX}summary_${filterKey}`;
    
    // Check cache first
    const cachedAnalytics = analyticsCache.get(cacheKey);
    
    if (cachedAnalytics) {
      logger.debug('Analytics cache hit for summary analytics');
      return cachedAnalytics;
    }
    
    // Build query based on filters
    const query = {};
    
    if (filters.adType) {
      query.adType = filters.adType;
    }
    
    if (filters.status !== undefined) {
      query.status = filters.status;
    }
    
    // Get all ads matching the query
    const ads = await AdMob.find(query);
    
    // Build summary analytics
    const summary = {
      totalAds: ads.length,
      totalImpressions: 0,
      totalClicks: 0,
      averageCtr: 0,
      totalRevenue: 0,
      byAdType: {},
      byDevice: {},
      byPlatform: {},
      adPerformance: []
    };
    
    // Process each ad
    ads.forEach(ad => {
      // Add to totals
      summary.totalImpressions += ad.impressions || 0;
      summary.totalClicks += ad.clicks || 0;
      summary.totalRevenue += ad.revenue || 0;
      
      // Add to adType breakdown
      if (!summary.byAdType[ad.adType]) {
        summary.byAdType[ad.adType] = {
          impressions: 0,
          clicks: 0,
          ctr: 0,
          revenue: 0
        };
      }
      
      summary.byAdType[ad.adType].impressions += ad.impressions || 0;
      summary.byAdType[ad.adType].clicks += ad.clicks || 0;
      summary.byAdType[ad.adType].revenue += ad.revenue || 0;
      
      // Calculate CTR for this ad type
      if (summary.byAdType[ad.adType].impressions > 0) {
        summary.byAdType[ad.adType].ctr = 
          (summary.byAdType[ad.adType].clicks / summary.byAdType[ad.adType].impressions) * 100;
      }
      
      // Process impression data for device and platform breakdown
      if (ad.impressionData && ad.impressionData.length > 0) {
        ad.impressionData.forEach(impression => {
          const deviceType = impression.context?.deviceType || 'unknown';
          const platform = impression.context?.platform || 'unknown';
          
          // Add to device breakdown
          if (!summary.byDevice[deviceType]) {
            summary.byDevice[deviceType] = {
              impressions: 0,
              clicks: 0
            };
          }
          
          summary.byDevice[deviceType].impressions += 1;
          
          // Add to platform breakdown
          if (!summary.byPlatform[platform]) {
            summary.byPlatform[platform] = {
              impressions: 0,
              clicks: 0
            };
          }
          
          summary.byPlatform[platform].impressions += 1;
        });
      }
      
      // Process click data for device and platform breakdown
      if (ad.clickData && ad.clickData.length > 0) {
        ad.clickData.forEach(click => {
          const deviceType = click.context?.deviceType || 'unknown';
          const platform = click.context?.platform || 'unknown';
          
          // Add to device breakdown
          if (summary.byDevice[deviceType]) {
            summary.byDevice[deviceType].clicks += 1;
          }
          
          // Add to platform breakdown
          if (summary.byPlatform[platform]) {
            summary.byPlatform[platform].clicks += 1;
          }
        });
      }
      
      // Add to ad performance array
      summary.adPerformance.push({
        adId: ad._id,
        adName: ad.adName,
        adType: ad.adType,
        impressions: ad.impressions || 0,
        clicks: ad.clicks || 0,
        ctr: ad.ctr || 0,
        revenue: ad.revenue || 0
      });
    });
    
    // Calculate overall average CTR
    if (summary.totalImpressions > 0) {
      summary.averageCtr = (summary.totalClicks / summary.totalImpressions) * 100;
    }
    
    // Sort ad performance by impressions (descending)
    summary.adPerformance.sort((a, b) => b.impressions - a.impressions);
    
    // Store in cache
    analyticsCache.set(cacheKey, summary);
    
    return summary;
  } catch (error) {
    logger.error(`Analytics error getting summary analytics: ${error.message}`);
    throw new Error(`Failed to get summary analytics: ${error.message}`);
  }
};

/**
 * Track ad revenue
 * @param {string} adId - The ID of the ad
 * @param {number} amount - Revenue amount
 * @param {string} currency - Currency code (default: USD)
 * @returns {Promise<Object>} - The updated analytics data
 */
const trackRevenue = async (adId, amount, currency = 'USD') => {
  try {
    // Validate adId
    if (!mongoose.Types.ObjectId.isValid(adId)) {
      logger.warn(`Invalid ad ID format for revenue tracking: ${adId}`);
      throw new Error('Invalid ad ID format');
    }
    
    // Validate amount
    if (typeof amount !== 'number' || isNaN(amount) || amount < 0) {
      logger.warn(`Invalid revenue amount: ${amount}`);
      throw new Error('Invalid revenue amount');
    }
    
    // Get the ad
    const ad = await AdMob.findById(adId);
    
    if (!ad) {
      logger.warn(`Ad with ID ${adId} not found for revenue tracking`);
      throw new Error('Ad not found');
    }
    
    // Update revenue
    ad.revenue = (ad.revenue || 0) + amount;
    
    // Store revenue data
    const timestamp = new Date();
    
    // Add to revenueData array (limited to last 100 entries)
    if (!ad.revenueData) {
      ad.revenueData = [];
    }
    
    ad.revenueData.unshift({
      timestamp,
      amount,
      currency
    });
    
    // Limit array size to prevent document growth
    if (ad.revenueData.length > 100) {
      ad.revenueData = ad.revenueData.slice(0, 100);
    }
    
    // Save the updated ad
    await ad.save();
    
    // Invalidate analytics cache
    invalidateAnalyticsCache(adId);
    
    logger.debug(`Analytics: Revenue tracked for ad ID: ${adId}, amount: ${amount} ${currency}`);
    
    return {
      adId: ad._id,
      adName: ad.adName,
      revenue: ad.revenue,
      currency
    };
  } catch (error) {
    logger.error(`Analytics error tracking revenue: ${error.message}`);
    throw new Error(`Analytics failed to track revenue: ${error.message}`);
  }
};

/**
 * Invalidate analytics cache for a specific ad
 * @param {string} adId - The ID of the ad to invalidate in cache
 */
const invalidateAnalyticsCache = (adId) => {
  try {
    const cacheKey = `${ANALYTICS_CACHE_KEY_PREFIX}${adId}`;
    analyticsCache.del(cacheKey);
    
    // Also invalidate summary cache
    const summaryKeys = analyticsCache.keys().filter(key => 
      key.startsWith(`${ANALYTICS_CACHE_KEY_PREFIX}summary_`));
    
    summaryKeys.forEach(key => analyticsCache.del(key));
    
    logger.debug(`Analytics cache invalidated for ad ID: ${adId}`);
  } catch (error) {
    logger.error(`Error invalidating analytics cache: ${error.message}`);
  }
};

/**
 * Invalidate all analytics caches
 */
const invalidateAllAnalyticsCaches = () => {
  try {
    const keys = analyticsCache.keys();
    const analyticsKeys = keys.filter(key => key.startsWith(ANALYTICS_CACHE_KEY_PREFIX));
    analyticsKeys.forEach(key => analyticsCache.del(key));
    
    logger.debug('All analytics caches invalidated');
  } catch (error) {
    logger.error(`Error invalidating all analytics caches: ${error.message}`);
  }
};

module.exports = {
  trackImpression,
  trackClick,
  getAdAnalytics,
  getSummaryAnalytics,
  trackRevenue,
  invalidateAnalyticsCache,
  invalidateAllAnalyticsCaches
}; 