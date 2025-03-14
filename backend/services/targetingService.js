/**
 * Targeting Service
 * Provides methods for targeting users based on demographics and behavior
 */

const mongoose = require('mongoose');
const logger = require('../config/logger');
const cacheService = require('./cacheService');

// Cache namespace for targeting
const targetingCache = cacheService.namespace('targeting');

/**
 * User Segment Types
 */
const SegmentType = {
  DEMOGRAPHIC: 'demographic', // Based on user demographics (age, gender, location)
  BEHAVIORAL: 'behavioral',   // Based on user behavior (actions, preferences)
  CONTEXTUAL: 'contextual',   // Based on current context (time, weather, etc.)
  CUSTOM: 'custom'            // Custom segments defined by admins
};

/**
 * Targeting Service
 */
const targetingService = {
  /**
   * Evaluate if a user matches a set of targeting criteria
   * @param {Object} criteria - Targeting criteria
   * @param {Object} userContext - User context
   * @returns {boolean} Whether the user matches the criteria
   */
  evaluateCriteria(criteria, userContext) {
    // If no criteria, include all users
    if (!criteria || Object.keys(criteria).length === 0) {
      return true;
    }

    // Evaluate each criterion
    return Object.entries(criteria).every(([key, value]) => {
      // Get field value from user context
      const fieldValue = userContext[key];

      // If field value is undefined, criterion doesn't match
      if (fieldValue === undefined) {
        return false;
      }

      // If value is an object with operator and value
      if (value && typeof value === 'object' && value.operator && value.value !== undefined) {
        return this.evaluateOperator(value.operator, fieldValue, value.value);
      }

      // Simple equality check
      return fieldValue === value;
    });
  },

  /**
   * Evaluate an operator against field value and target value
   * @param {string} operator - Operator (e.g., 'equals', 'contains', 'greaterThan')
   * @param {*} fieldValue - Field value from user context
   * @param {*} targetValue - Target value to compare against
   * @returns {boolean} Whether the condition is met
   */
  evaluateOperator(operator, fieldValue, targetValue) {
    switch (operator) {
      case 'equals':
        return fieldValue === targetValue;
      case 'notEquals':
        return fieldValue !== targetValue;
      case 'contains':
        return String(fieldValue).includes(String(targetValue));
      case 'notContains':
        return !String(fieldValue).includes(String(targetValue));
      case 'startsWith':
        return String(fieldValue).startsWith(String(targetValue));
      case 'endsWith':
        return String(fieldValue).endsWith(String(targetValue));
      case 'regex':
        try {
          const regex = new RegExp(targetValue);
          return regex.test(String(fieldValue));
        } catch (error) {
          logger.error(`Invalid regex in targeting criterion: ${error.message}`, { error });
          return false;
        }
      case 'greaterThan':
        return Number(fieldValue) > Number(targetValue);
      case 'lessThan':
        return Number(fieldValue) < Number(targetValue);
      case 'greaterThanOrEqual':
        return Number(fieldValue) >= Number(targetValue);
      case 'lessThanOrEqual':
        return Number(fieldValue) <= Number(targetValue);
      case 'in':
        return Array.isArray(targetValue) && targetValue.includes(fieldValue);
      case 'notIn':
        return Array.isArray(targetValue) && !targetValue.includes(fieldValue);
      default:
        logger.warn(`Unknown operator: ${operator}`);
        return false;
    }
  },

  /**
   * Get segments that a user belongs to
   * @param {Object} userContext - User context
   * @returns {Promise<Array>} Segments
   */
  async getUserSegments(userContext) {
    try {
      // Check cache
      const cacheKey = `segments_${JSON.stringify(userContext)}`;
      const cachedSegments = targetingCache.get(cacheKey);
      if (cachedSegments) {
        return cachedSegments;
      }

      // TODO: Implement segment retrieval from database
      // For now, return demo segments based on user context
      const segments = [];

      // Demographic segments
      if (userContext.country) {
        segments.push({
          id: `country_${userContext.country}`,
          name: `Country: ${userContext.country}`,
          type: SegmentType.DEMOGRAPHIC
        });
      }

      if (userContext.language) {
        segments.push({
          id: `language_${userContext.language}`,
          name: `Language: ${userContext.language}`,
          type: SegmentType.DEMOGRAPHIC
        });
      }

      if (userContext.deviceType) {
        segments.push({
          id: `device_${userContext.deviceType}`,
          name: `Device: ${userContext.deviceType}`,
          type: SegmentType.DEMOGRAPHIC
        });
      }

      if (userContext.platform) {
        segments.push({
          id: `platform_${userContext.platform}`,
          name: `Platform: ${userContext.platform}`,
          type: SegmentType.DEMOGRAPHIC
        });
      }

      // Behavioral segments (based on user history)
      if (userContext.recentActivity) {
        if (userContext.recentActivity.includes('purchase')) {
          segments.push({
            id: 'recent_purchaser',
            name: 'Recent Purchaser',
            type: SegmentType.BEHAVIORAL
          });
        }

        if (userContext.recentActivity.includes('view_ad')) {
          segments.push({
            id: 'ad_viewer',
            name: 'Ad Viewer',
            type: SegmentType.BEHAVIORAL
          });
        }

        if (userContext.recentActivity.includes('click_ad')) {
          segments.push({
            id: 'ad_clicker',
            name: 'Ad Clicker',
            type: SegmentType.BEHAVIORAL
          });
        }
      }

      // Contextual segments
      const now = new Date();
      const hour = now.getHours();

      if (hour >= 5 && hour < 12) {
        segments.push({
          id: 'time_morning',
          name: 'Morning',
          type: SegmentType.CONTEXTUAL
        });
      } else if (hour >= 12 && hour < 17) {
        segments.push({
          id: 'time_afternoon',
          name: 'Afternoon',
          type: SegmentType.CONTEXTUAL
        });
      } else if (hour >= 17 && hour < 21) {
        segments.push({
          id: 'time_evening',
          name: 'Evening',
          type: SegmentType.CONTEXTUAL
        });
      } else {
        segments.push({
          id: 'time_night',
          name: 'Night',
          type: SegmentType.CONTEXTUAL
        });
      }

      // Cache segments
      targetingCache.set(cacheKey, segments, 3600); // Cache for 1 hour

      return segments;
    } catch (error) {
      logger.error(`Error getting user segments: ${error.message}`, { error });
      return [];
    }
  },

  /**
   * Check if a user belongs to a specific segment
   * @param {string} segmentId - Segment ID
   * @param {Object} userContext - User context
   * @returns {Promise<boolean>} Whether the user belongs to the segment
   */
  async isUserInSegment(segmentId, userContext) {
    try {
      const segments = await this.getUserSegments(userContext);
      return segments.some(segment => segment.id === segmentId);
    } catch (error) {
      logger.error(`Error checking if user is in segment: ${error.message}`, { error });
      return false;
    }
  },

  /**
   * Get the best ad for a user based on targeting
   * @param {Array} ads - Available ads
   * @param {Object} userContext - User context
   * @returns {Object} Best ad for the user
   */
  async getBestAdForUser(ads, userContext) {
    try {
      // If no ads, return null
      if (!ads || ads.length === 0) {
        return null;
      }

      // If only one ad, return it
      if (ads.length === 1) {
        return ads[0];
      }

      // Get user segments
      const userSegments = await this.getUserSegments(userContext);
      const userSegmentIds = userSegments.map(segment => segment.id);

      // Score each ad based on targeting
      const scoredAds = await Promise.all(ads.map(async ad => {
        let score = 0;

        // Base score
        score += 1;

        // Score based on targeting criteria
        if (ad.targetingCriteria) {
          // Direct criteria match
          if (this.evaluateCriteria(ad.targetingCriteria, userContext)) {
            score += 5;
          }

          // Segment match
          if (ad.targetingCriteria.segments) {
            const targetSegments = Array.isArray(ad.targetingCriteria.segments)
              ? ad.targetingCriteria.segments
              : [ad.targetingCriteria.segments];

            const matchingSegments = targetSegments.filter(segmentId => 
              userSegmentIds.includes(segmentId)
            );

            score += matchingSegments.length * 2;
          }
        }

        // Score based on performance for this user's segments
        // TODO: Implement performance-based scoring using analytics data

        return {
          ad,
          score
        };
      }));

      // Sort ads by score (descending)
      scoredAds.sort((a, b) => b.score - a.score);

      // Return the highest-scoring ad
      return scoredAds[0].ad;
    } catch (error) {
      logger.error(`Error getting best ad for user: ${error.message}`, { error });
      
      // Fallback to first ad
      return ads[0];
    }
  },

  /**
   * Enrich user context with additional data
   * @param {Object} userContext - Basic user context
   * @returns {Promise<Object>} Enriched user context
   */
  async enrichUserContext(userContext) {
    try {
      // Clone the original context
      const enrichedContext = { ...userContext };

      // Add time-based context
      const now = new Date();
      enrichedContext.timestamp = now.getTime();
      enrichedContext.hour = now.getHours();
      enrichedContext.dayOfWeek = now.getDay();
      enrichedContext.weekend = now.getDay() === 0 || now.getDay() === 6;

      // TODO: Add more enrichment sources
      // - User history from database
      // - IP-based geolocation
      // - Device capabilities
      // - App usage patterns

      return enrichedContext;
    } catch (error) {
      logger.error(`Error enriching user context: ${error.message}`, { error });
      return userContext;
    }
  },

  /**
   * Invalidate cache for a specific user or all users
   * @param {string} userId - User ID (optional)
   */
  invalidateCache(userId = null) {
    if (userId) {
      // Find and invalidate cache entries for this user
      const keys = targetingCache.keys();
      for (const key of keys) {
        if (key.includes(userId)) {
          targetingCache.del(key);
        }
      }
    } else {
      // Invalidate all targeting cache
      targetingCache.flushAll();
    }
  }
};

module.exports = targetingService; 