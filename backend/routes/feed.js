const express = require('express');
const router = express.Router();
const Template = require('../models/firestore/Template');
const User = require('../models/firestore/User');
const logger = require('../config/logger');
const { optionalFirebaseAuth } = require('../middleware/auth');

/**
 * Centralized Feed API
 * Provides a single endpoint for all feed-related data
 */

/**
 * GET /api/feed - Get centralized feed data
 * @param {string} include - Comma-separated list of sections to include (templates,categories,trending,featured,recommendations)
 * @param {string} categories - Comma-separated list of categories to filter
 * @param {string} templateTypes - Comma-separated list of template types
 * @param {number} page - Page number for pagination (default: 1)
 * @param {number} limit - Number of items per page (default: 20)
 * @param {boolean} isPremium - Filter by premium templates
 * @param {boolean} isFeatured - Filter by featured templates
 */
router.get('/', optionalFirebaseAuth, async (req, res) => {
    try {
        const {
            include = 'templates,categories',
            categories,
            templateTypes,
            page = 1,
            limit = 20,
            isPremium,
            isFeatured
        } = req.query;

        const userId = req.user?.uid;
        const includeSections = include.split(',').map(s => s.trim());
        const pageNum = parseInt(page);
        const limitNum = parseInt(limit);

        logger.info(`Feed request - User: ${userId}, Include: ${includeSections.join(',')}, Page: ${pageNum}, Limit: ${limitNum}`);

        const feedData = {};

        // Get templates section
        if (includeSections.includes('templates')) {
            try {
                const filters = {};
                
                if (isPremium !== undefined) {
                    filters.isPremium = isPremium === 'true';
                }
                
                if (isFeatured !== undefined) {
                    filters.isFeatured = isFeatured === 'true';
                }

                if (categories) {
                    const categoryList = categories.split(',').map(c => c.trim());
                    // For multiple categories, we'll need to make separate calls and combine
                    if (categoryList.length === 1) {
                        filters.category = categoryList[0];
                    }
                }

                const templatesResult = await Template.getPaginated(pageNum, limitNum, filters);
                
                // If multiple categories specified, filter in memory
                if (categories && categories.split(',').length > 1) {
                    const categoryList = categories.split(',').map(c => c.trim());
                    templatesResult.templates = templatesResult.templates.filter(template =>
                        categoryList.includes(template.category)
                    );
                }

                feedData.templates = templatesResult;
                logger.info(`Retrieved ${templatesResult.templates.length} templates`);
            } catch (error) {
                logger.error(`Error getting templates: ${error.message}`);
                feedData.templates = { templates: [], page: pageNum, limit: limitNum, hasMore: false };
            }
        }

        // Get categories section
        if (includeSections.includes('categories')) {
            try {
                const categoriesData = await Template.getCategories();
                feedData.categories = categoriesData;
                logger.info(`Retrieved ${Object.keys(categoriesData).length} categories`);
            } catch (error) {
                logger.error(`Error getting categories: ${error.message}`);
                feedData.categories = {};
            }
        }

        // Get trending templates section
        if (includeSections.includes('trending')) {
            try {
                const trendingTemplates = await Template.getTrending(limitNum);
                feedData.trending = {
                    templates: trendingTemplates,
                    count: trendingTemplates.length
                };
                logger.info(`Retrieved ${trendingTemplates.length} trending templates`);
            } catch (error) {
                logger.error(`Error getting trending templates: ${error.message}`);
                feedData.trending = { templates: [], count: 0 };
            }
        }

        // Get featured templates section
        if (includeSections.includes('featured')) {
            try {
                const featuredTemplates = await Template.getFeatured(limitNum);
                feedData.featured = {
                    templates: featuredTemplates,
                    count: featuredTemplates.length
                };
                logger.info(`Retrieved ${featuredTemplates.length} featured templates`);
            } catch (error) {
                logger.error(`Error getting featured templates: ${error.message}`);
                feedData.featured = { templates: [], count: 0 };
            }
        }

        // Get user recommendations (if user is authenticated)
        if (includeSections.includes('recommendations') && userId) {
            try {
                const userRecommendations = await getUserRecommendations(userId, limitNum);
                feedData.recommendations = {
                    templates: userRecommendations,
                    count: userRecommendations.length
                };
                logger.info(`Retrieved ${userRecommendations.length} recommendations for user ${userId}`);
            } catch (error) {
                logger.error(`Error getting recommendations: ${error.message}`);
                feedData.recommendations = { templates: [], count: 0 };
            }
        }

        // Add metadata
        feedData.metadata = {
            timestamp: new Date().toISOString(),
            userId: userId || null,
            page: pageNum,
            limit: limitNum,
            includedSections: includeSections,
            totalSections: Object.keys(feedData).length - 1 // Exclude metadata
        };

        logger.info(`Feed response prepared with ${Object.keys(feedData).length - 1} sections`);
        res.json(feedData);

    } catch (error) {
        logger.error(`Feed error: ${error.message}`);
        res.status(500).json({
            error: 'Failed to get feed data',
            message: error.message,
            timestamp: new Date().toISOString()
        });
    }
});

/**
 * POST /api/feed/interaction - Record template interaction (like, favorite, view, etc.)
 */
router.post('/interaction', optionalFirebaseAuth, async (req, res) => {
    try {
        const { templateId, action, value } = req.body;
        const userId = req.user?.uid;

        if (!templateId || !action) {
            return res.status(400).json({
                error: 'Template ID and action are required'
            });
        }

        logger.info(`Template interaction - User: ${userId}, Template: ${templateId}, Action: ${action}, Value: ${value}`);

        // Handle different interaction types
        switch (action) {
            case 'like':
                await Template.incrementCounter(templateId, 'likeCount', value ? 1 : -1);
                if (userId) {
                    if (value) {
                        await User.addToLikes(userId, templateId);
                    } else {
                        await User.removeFromLikes(userId, templateId);
                    }
                }
                break;

            case 'favorite':
                await Template.incrementCounter(templateId, 'favoriteCount', value ? 1 : -1);
                if (userId) {
                    if (value) {
                        await User.addToFavorites(userId, templateId);
                    } else {
                        await User.removeFromFavorites(userId, templateId);
                    }
                }
                break;

            case 'view':
                await Template.incrementCounter(templateId, 'viewCount', 1);
                if (userId) {
                    await User.recordTemplateView(userId, templateId);
                }
                break;

            case 'share':
                await Template.incrementCounter(templateId, 'sharedCount', 1);
                break;

            case 'download':
                await Template.incrementCounter(templateId, 'downloadCount', 1);
                break;

            default:
                return res.status(400).json({
                    error: 'Invalid action type'
                });
        }

        res.json({
            success: true,
            templateId,
            action,
            value,
            timestamp: new Date().toISOString()
        });

    } catch (error) {
        logger.error(`Interaction error: ${error.message}`);
        res.status(500).json({
            error: 'Failed to record interaction',
            message: error.message
        });
    }
});

/**
 * Helper function to get user recommendations
 * @param {string} userId User ID
 * @param {number} limit Number of recommendations
 * @returns {Promise<Array>} Recommended templates
 */
async function getUserRecommendations(userId, limit = 10) {
    try {
        // Get user's interaction history
        const user = await User.getByUid(userId);
        if (!user) {
            return [];
        }

        // Get user's favorite categories
        const categoryVisits = user.categoryVisits || {};
        const topCategories = Object.entries(categoryVisits)
            .sort(([,a], [,b]) => (b.count || 0) - (a.count || 0))
            .slice(0, 3)
            .map(([category]) => category);

        if (topCategories.length === 0) {
            // Return trending templates if no user preferences
            return await Template.getTrending(limit);
        }

        // Get templates from user's preferred categories
        const recommendations = [];
        const templatesPerCategory = Math.ceil(limit / topCategories.length);

        for (const category of topCategories) {
            try {
                const categoryTemplates = await Template.getByCategory(category, templatesPerCategory);
                recommendations.push(...categoryTemplates.templates);
            } catch (error) {
                logger.error(`Error getting recommendations for category ${category}: ${error.message}`);
            }
        }

        // Shuffle and limit results
        const shuffled = recommendations.sort(() => 0.5 - Math.random());
        return shuffled.slice(0, limit);

    } catch (error) {
        logger.error(`Error getting user recommendations: ${error.message}`);
        return [];
    }
}

module.exports = router;
