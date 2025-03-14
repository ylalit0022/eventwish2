const { AdMob } = require('../models/AdMob');
const NodeCache = require('node-cache');
const mongoose = require('mongoose');
const logger = require('../config/logger');
const analyticsService = require('./analyticsService');
const targetingService = require('./targetingService');

// Initialize cache with standard TTL of 10 minutes and check period of 60 seconds
const adCache = new NodeCache({ stdTTL: 600, checkperiod: 60 });
const AD_CACHE_KEY_PREFIX = 'admob_ad_';
const AD_COLLECTION_CACHE_KEY = 'admob_ads_collection';
const AD_TYPES_CACHE_KEY = 'admob_ad_types';

/**
 * Get an ad by its ID with caching
 * @param {string} adId - The ID of the ad to retrieve
 * @returns {Promise<Object>} - The ad object
 */
const getAdById = async (adId) => {
  try {
    // Check cache first
    const cacheKey = `${AD_CACHE_KEY_PREFIX}${adId}`;
    const cachedAd = adCache.get(cacheKey);
    
    if (cachedAd) {
      logger.debug(`Cache hit for ad ID: ${adId}`);
      return cachedAd;
    }
    
    // If not in cache, fetch from database
    logger.debug(`Cache miss for ad ID: ${adId}, fetching from database`);
    const ad = await AdMob.findById(adId);
    
    if (!ad) {
      logger.warn(`Ad with ID ${adId} not found`);
      return null;
    }
    
    // Store in cache
    adCache.set(cacheKey, ad);
    return ad;
  } catch (error) {
    logger.error(`Error fetching ad by ID ${adId}: ${error.message}`);
    throw new Error(`Failed to fetch ad: ${error.message}`);
  }
};

/**
 * Get all active ads with caching
 * @returns {Promise<Array>} - Array of active ad objects
 */
const getActiveAds = async () => {
  try {
    // Check cache first
    const cachedAds = adCache.get(AD_COLLECTION_CACHE_KEY);
    
    if (cachedAds) {
      logger.debug('Cache hit for active ads collection');
      return cachedAds;
    }
    
    // If not in cache, fetch from database
    logger.debug('Cache miss for active ads collection, fetching from database');
    const ads = await AdMob.find({ status: true });
    
    // Store in cache
    adCache.set(AD_COLLECTION_CACHE_KEY, ads);
    return ads;
  } catch (error) {
    logger.error(`Error fetching active ads: ${error.message}`);
    throw new Error(`Failed to fetch active ads: ${error.message}`);
  }
};

/**
 * Get ads by type with caching
 * @param {string} adType - The type of ads to retrieve
 * @returns {Promise<Array>} - Array of ad objects of the specified type
 */
const getAdsByType = async (adType) => {
  try {
    // Check cache first
    const cacheKey = `${AD_CACHE_KEY_PREFIX}type_${adType}`;
    const cachedAds = adCache.get(cacheKey);
    
    if (cachedAds) {
      logger.debug(`Cache hit for ads of type: ${adType}`);
      return cachedAds;
    }
    
    // If not in cache, fetch from database
    logger.debug(`Cache miss for ads of type: ${adType}, fetching from database`);
    const ads = await AdMob.find({ adType, status: true });
    
    // Store in cache
    adCache.set(cacheKey, ads);
    return ads;
  } catch (error) {
    logger.error(`Error fetching ads by type ${adType}: ${error.message}`);
    throw new Error(`Failed to fetch ads by type: ${error.message}`);
  }
};

/**
 * Get optimal ad configuration based on user context
 * @param {Object} context - User context (device, platform, etc.)
 * @param {string} adType - The type of ad to retrieve
 * @returns {Promise<Object>} - The optimal ad configuration
 */
const getOptimalAdConfig = async (context, adType) => {
  try {
    // Get all active ads of the specified type
    const ads = await getAdsByType(adType);
    
    if (!ads || ads.length === 0) {
      logger.warn(`No active ads found for type: ${adType}`);
      return null;
    }
    
    // Enrich user context with additional data
    const enrichedContext = await targetingService.enrichUserContext(context);
    
    // Log the enriched context for future analysis
    logger.debug(`Enriched context for ad selection: ${JSON.stringify(enrichedContext)}`);
    
    // Use targeting service to get the best ad for this user
    const optimalAd = await targetingService.getBestAdForUser(ads, enrichedContext);
    
    // If targeting service couldn't determine the best ad, fall back to the first ad
    if (!optimalAd) {
      logger.debug(`Targeting service couldn't determine best ad, falling back to first ad`);
      return ads[0];
    }
    
    // Log the selected ad for analysis
    logger.debug(`Selected ad ${optimalAd.adName} (${optimalAd._id}) based on targeting criteria`);
    
    // Return the selected ad
    return optimalAd;
  } catch (error) {
    logger.error(`Error getting optimal ad config: ${error.message}`);
    throw new Error(`Failed to get optimal ad config: ${error.message}`);
  }
};

/**
 * Invalidate cache for a specific ad
 * @param {string} adId - The ID of the ad to invalidate in cache
 */
const invalidateAdCache = (adId) => {
  try {
    const cacheKey = `${AD_CACHE_KEY_PREFIX}${adId}`;
    adCache.del(cacheKey);
    adCache.del(AD_COLLECTION_CACHE_KEY);
    
    // Also invalidate type-based caches since we don't know which type this ad belongs to
    // In a production environment, we might want to be more selective
    const keys = adCache.keys();
    const typeKeys = keys.filter(key => key.startsWith(`${AD_CACHE_KEY_PREFIX}type_`));
    typeKeys.forEach(key => adCache.del(key));
    
    logger.debug(`Cache invalidated for ad ID: ${adId}`);
  } catch (error) {
    logger.error(`Error invalidating ad cache: ${error.message}`);
  }
};

/**
 * Invalidate all ad caches
 */
const invalidateAllAdCaches = () => {
  try {
    const keys = adCache.keys();
    const adKeys = keys.filter(key => key.startsWith(AD_CACHE_KEY_PREFIX) || key === AD_COLLECTION_CACHE_KEY);
    adKeys.forEach(key => adCache.del(key));
    
    logger.debug('All ad caches invalidated');
  } catch (error) {
    logger.error(`Error invalidating all ad caches: ${error.message}`);
  }
};

/**
 * Track ad impression
 * @param {string} adId - The ID of the ad that was impressed
 * @param {Object} context - Context of the impression (device, location, etc.)
 * @returns {Promise<Object>} - The updated ad object
 */
const trackImpression = async (adId, context = {}) => {
  try {
    // Validate adId
    if (!mongoose.Types.ObjectId.isValid(adId)) {
      logger.warn(`Invalid ad ID format: ${adId}`);
      throw new Error('Invalid ad ID format');
    }
    
    // Track impression in analytics service
    await analyticsService.trackImpression(adId, context);
    
    // Invalidate ad cache
    invalidateAdCache(adId);
    
    logger.info(`Ad impression tracked for ad ID: ${adId}, context: ${JSON.stringify(context)}`);
    
    // Return the updated ad
    return await getAdById(adId);
  } catch (error) {
    logger.error(`Error tracking ad impression: ${error.message}`);
    throw new Error(`Failed to track ad impression: ${error.message}`);
  }
};

/**
 * Track ad click
 * @param {string} adId - The ID of the ad that was clicked
 * @param {Object} context - Context of the click (device, location, etc.)
 * @returns {Promise<Object>} - The updated ad object
 */
const trackClick = async (adId, context = {}) => {
  try {
    // Validate adId
    if (!mongoose.Types.ObjectId.isValid(adId)) {
      logger.warn(`Invalid ad ID format: ${adId}`);
      throw new Error('Invalid ad ID format');
    }
    
    // Track click in analytics service
    await analyticsService.trackClick(adId, context);
    
    // Invalidate ad cache
    invalidateAdCache(adId);
    
    logger.info(`Ad click tracked for ad ID: ${adId}, context: ${JSON.stringify(context)}`);
    
    // Return the updated ad
    return await getAdById(adId);
  } catch (error) {
    logger.error(`Error tracking ad click: ${error.message}`);
    throw new Error(`Failed to track ad click: ${error.message}`);
  }
};

/**
 * Get ad types
 * @returns {Promise<Array>} - Array of ad types
 */
const getAdTypes = async () => {
  try {
    // Check cache first
    const cachedTypes = adCache.get(AD_TYPES_CACHE_KEY);
    
    if (cachedTypes) {
      logger.debug('Cache hit for ad types');
      return cachedTypes;
    }
    
    // If not in cache, fetch from module
    logger.debug('Cache miss for ad types, fetching from module');
    const { adTypes } = require('../models/AdMob');
    
    // Store in cache
    adCache.set(AD_TYPES_CACHE_KEY, adTypes);
    return adTypes;
  } catch (error) {
    logger.error(`Error fetching ad types: ${error.message}`);
    throw new Error(`Failed to fetch ad types: ${error.message}`);
  }
};

module.exports = {
  getAdById,
  getActiveAds,
  getAdsByType,
  getOptimalAdConfig,
  invalidateAdCache,
  invalidateAllAdCaches,
  trackImpression,
  trackClick,
  getAdTypes
}; 