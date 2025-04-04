const mongoose = require('mongoose');
const User = require('../models/User');
const Template = require('../models/Template');
const cacheService = require('./cacheService');
const logger = require('../utils/logger');

// Cache keys
const CACHE_KEY_USER_RECOMMENDATIONS = 'user_recommendations_';
const CACHE_TTL = 60 * 30; // 30 minutes

/**
 * Calculate score for a category based on user interaction
 * @param {Object} categoryData - The category visit data
 * @param {Number} maxVisitCount - The maximum visit count among all categories
 * @param {Number} oldestVisitDate - The oldest visit date timestamp
 * @param {Number} newestVisitDate - The newest visit date timestamp
 * @returns {Number} The calculated score
 */
const calculateCategoryScore = (categoryData, maxVisitCount, oldestVisitDate, newestVisitDate) => {
    try {
        // Default weights
        const VISIT_COUNT_WEIGHT = 50;
        const RECENCY_WEIGHT = 30;
        const SOURCE_WEIGHT_TEMPLATE = 20;
        const SOURCE_WEIGHT_DIRECT = 10;
        
        // Calculate countScore (normalized by max visit count)
        const countScore = (categoryData.visitCount / maxVisitCount) * VISIT_COUNT_WEIGHT;
        
        // Calculate recencyScore (normalized between oldest and newest)
        const visitDate = new Date(categoryData.visitDate).getTime();
        const timeDiff = newestVisitDate - oldestVisitDate;
        // Avoid division by zero if all visits are at the same time
        const recencyScore = timeDiff === 0 
            ? RECENCY_WEIGHT 
            : ((visitDate - oldestVisitDate) / timeDiff) * RECENCY_WEIGHT;
        
        // Calculate source score
        const sourceScore = categoryData.source === 'template' 
            ? SOURCE_WEIGHT_TEMPLATE 
            : SOURCE_WEIGHT_DIRECT;
        
        // Calculate total score
        const totalScore = countScore + recencyScore + sourceScore;
        
        logger.debug(`Category score calculation: ${categoryData.category}, count: ${countScore}, recency: ${recencyScore}, source: ${sourceScore}, total: ${totalScore}`);
        
        return totalScore;
        
    } catch (error) {
        logger.error(`Error calculating category score: ${error.message}`);
        // Return a default value if calculation fails
        return 0;
    }
};

/**
 * Get personalized recommendations for a user
 * @param {String} deviceId - The user's device ID
 * @param {Number} limit - Maximum number of recommendations to return
 * @returns {Promise<Object>} Object containing recommendations data
 */
const getRecommendationsForUser = async (deviceId, limit = 10) => {
    try {
        // Check cache first
        const cacheKey = `${CACHE_KEY_USER_RECOMMENDATIONS}${deviceId}`;
        const cachedRecommendations = await cacheService.get(cacheKey);
        
        if (cachedRecommendations) {
            logger.info(`Using cached recommendations for user: ${deviceId}`);
            return JSON.parse(cachedRecommendations);
        }
        
        // Get user data
        const user = await User.findOne({ deviceId });
        
        if (!user || !user.categories || user.categories.length === 0) {
            logger.info(`No category history for user: ${deviceId}, using default recommendations`);
            return await getDefaultRecommendations(limit);
        }
        
        // Calculate scores for each category
        const categories = [...user.categories];
        
        // Find max visit count and date ranges for normalization
        const maxVisitCount = Math.max(...categories.map(cat => cat.visitCount));
        const visitDates = categories.map(cat => new Date(cat.visitDate).getTime());
        const oldestVisitDate = Math.min(...visitDates);
        const newestVisitDate = Math.max(...visitDates);
        
        // Calculate scores for each category
        const scoredCategories = categories.map(category => ({
            category: category.category,
            score: calculateCategoryScore(
                category, 
                maxVisitCount, 
                oldestVisitDate, 
                newestVisitDate
            ),
            visitCount: category.visitCount,
            visitDate: category.visitDate
        }));
        
        // Sort by score (descending)
        scoredCategories.sort((a, b) => b.score - a.score);
        
        // Select top categories (up to 3)
        const topCategories = scoredCategories.slice(0, 3);
        
        // Get templates for top categories
        const recommendedTemplates = await getTemplatesForCategories(
            topCategories.map(c => c.category), 
            Math.ceil(limit / topCategories.length)
        );
        
        // Prepare response
        const recommendations = {
            templates: recommendedTemplates,
            topCategories: topCategories.map(cat => ({
                category: cat.category,
                score: cat.score,
                weight: cat.visitCount
            })),
            lastUpdated: new Date()
        };
        
        // Cache the results
        await cacheService.set(
            cacheKey, 
            JSON.stringify(recommendations), 
            CACHE_TTL
        );
        
        return recommendations;
        
    } catch (error) {
        logger.error(`Error getting recommendations: ${error.message}`);
        // Return default recommendations if there's an error
        return await getDefaultRecommendations(limit);
    }
};

/**
 * Get templates for a list of categories
 * @param {Array<String>} categories - List of category names
 * @param {Number} limitPerCategory - Maximum templates per category
 * @returns {Promise<Array>} Array of template objects
 */
const getTemplatesForCategories = async (categories, limitPerCategory = 5) => {
    try {
        // Get templates for each category
        const templatePromises = categories.map(category => 
            Template.find({ category, status: true })
                .sort({ createdAt: -1 })
                .limit(limitPerCategory)
        );
        
        const categoryResults = await Promise.all(templatePromises);
        
        // Flatten and mix results, ensuring diversity
        const templates = [];
        let maxIndex = 0;
        
        // Find the maximum length of results
        categoryResults.forEach(result => {
            if (result.length > maxIndex) {
                maxIndex = result.length;
            }
        });
        
        // Interleave results to ensure diversity
        for (let i = 0; i < maxIndex; i++) {
            for (let j = 0; j < categoryResults.length; j++) {
                if (categoryResults[j][i]) {
                    templates.push(categoryResults[j][i]);
                }
            }
        }
        
        return templates;
        
    } catch (error) {
        logger.error(`Error getting templates for categories: ${error.message}`);
        return [];
    }
};

/**
 * Get default recommendations for new users or fallback
 * @param {Number} limit - Maximum number of recommendations
 * @returns {Promise<Object>} Default recommendations
 */
const getDefaultRecommendations = async (limit = 10) => {
    try {
        // Try to get from cache
        const cacheKey = 'default_recommendations';
        const cachedRecommendations = await cacheService.get(cacheKey);
        
        if (cachedRecommendations) {
            return JSON.parse(cachedRecommendations);
        }
        
        // Get top/trending templates
        const templates = await Template.find({ status: true })
            .sort({ createdAt: -1 })
            .limit(limit);
        
        const recommendations = {
            templates,
            topCategories: [],
            isDefault: true,
            lastUpdated: new Date()
        };
        
        // Cache for 1 hour
        await cacheService.set(
            cacheKey, 
            JSON.stringify(recommendations), 
            60 * 60
        );
        
        return recommendations;
        
    } catch (error) {
        logger.error(`Error getting default recommendations: ${error.message}`);
        return {
            templates: [],
            topCategories: [],
            isDefault: true,
            error: true
        };
    }
};

/**
 * Invalidate recommendations cache for a user
 * @param {String} deviceId - The user's device ID
 */
const invalidateUserRecommendations = async (deviceId) => {
    try {
        const cacheKey = `${CACHE_KEY_USER_RECOMMENDATIONS}${deviceId}`;
        await cacheService.del(cacheKey);
        logger.info(`Invalidated recommendations cache for user: ${deviceId}`);
    } catch (error) {
        logger.error(`Error invalidating recommendations cache: ${error.message}`);
    }
};

module.exports = {
    getRecommendationsForUser,
    getTemplatesForCategories,
    getDefaultRecommendations,
    invalidateUserRecommendations
}; 