const express = require('express');
const router = express.Router();
const feedService = require('../services/feedService');
const userProfileService = require('../services/userProfileService');
const templateScoringService = require('../services/templateScoringService');
const { verifyFirebaseToken, optionalFirebaseAuth } = require('../middleware/auth');
const { validateDeviceId } = require('../middleware/validators');
const logger = require('../utils/logger');

/**
 * Personalized Feed Routes
 * Provides AI-powered, personalized template recommendations
 */

/**
 * GET /api/feed
 * Get personalized feed for authenticated user
 * Query params: page, limit, refresh, categories, templateTypes
 */
router.get('/', verifyFirebaseToken, async (req, res) => {
    const startTime = Date.now();
    
    try {
        const uid = req.user.uid;
        const {
            page = 1,
            limit = 20,
            refresh = false,
            categories,
            templateTypes
        } = req.query;

        logger.info(`🎯 Feed request from user: ${uid}`, {
            page: parseInt(page),
            limit: parseInt(limit),
            refresh: refresh === 'true',
            categories: categories ? categories.split(',') : null,
            templateTypes: templateTypes ? templateTypes.split(',') : null
        });

        // Validate parameters
        const pageNum = Math.max(1, parseInt(page) || 1);
        const limitNum = Math.min(50, Math.max(1, parseInt(limit) || 20));
        const refreshFlag = refresh === 'true' || refresh === true;

        // Parse filters
        const filters = {};
        if (categories) {
            filters.categories = categories.split(',').map(c => c.trim()).filter(Boolean);
        }
        if (templateTypes) {
            filters.templateTypes = templateTypes.split(',').map(t => t.trim()).filter(Boolean);
        }

        // Generate personalized feed
        const feedResponse = await feedService.generatePersonalizedFeed(uid, {
            page: pageNum,
            limit: limitNum,
            refresh: refreshFlag,
            ...filters
        });

        // Add request metadata
        feedResponse.metadata.requestId = req.id || `feed_${Date.now()}`;
        feedResponse.metadata.responseTime = Date.now() - startTime;
        feedResponse.metadata.endpoint = '/api/feed';

        logger.info(`✅ Feed generated for ${uid} in ${Date.now() - startTime}ms`, {
            templatesCount: feedResponse.templates.length,
            page: pageNum,
            cached: feedResponse.metadata.cached || false
        });

        res.json({
            success: true,
            data: feedResponse,
            message: 'Personalized feed generated successfully'
        });

    } catch (error) {
        logger.error('❌ Error generating personalized feed:', error);
        
        res.status(500).json({
            success: false,
            error: 'Failed to generate personalized feed',
            message: 'An error occurred while generating your personalized feed. Please try again.',
            requestId: req.id || `feed_error_${Date.now()}`
        });
    }
});

/**
 * GET /api/feed/default
 * Get default feed for unauthenticated users or fallback
 * Query params: page, limit, categories, templateTypes
 */
router.get('/default', optionalFirebaseAuth, async (req, res) => {
    const startTime = Date.now();
    
    try {
        const {
            page = 1,
            limit = 20,
            categories,
            templateTypes
        } = req.query;

        logger.info('🔄 Default feed request', {
            page: parseInt(page),
            limit: parseInt(limit),
            authenticated: !!req.user
        });

        // Validate parameters
        const pageNum = Math.max(1, parseInt(page) || 1);
        const limitNum = Math.min(50, Math.max(1, parseInt(limit) || 20));

        // Parse filters
        const filters = {};
        if (categories) {
            filters.categories = categories.split(',').map(c => c.trim()).filter(Boolean);
        }
        if (templateTypes) {
            filters.templateTypes = templateTypes.split(',').map(t => t.trim()).filter(Boolean);
        }

        // Generate default feed
        const feedResponse = await feedService.generateDefaultFeed({
            page: pageNum,
            limit: limitNum,
            ...filters
        });

        // Add request metadata
        feedResponse.metadata.requestId = req.id || `default_feed_${Date.now()}`;
        feedResponse.metadata.responseTime = Date.now() - startTime;
        feedResponse.metadata.endpoint = '/api/feed/default';

        logger.info(`✅ Default feed generated in ${Date.now() - startTime}ms`, {
            templatesCount: feedResponse.templates.length,
            page: pageNum
        });

        res.json({
            success: true,
            data: feedResponse,
            message: 'Default feed generated successfully'
        });

    } catch (error) {
        logger.error('❌ Error generating default feed:', error);
        
        res.status(500).json({
            success: false,
            error: 'Failed to generate default feed',
            message: 'An error occurred while generating the feed. Please try again.',
            requestId: req.id || `default_feed_error_${Date.now()}`
        });
    }
});

/**
 * GET /api/feed/profile/:uid
 * Get user profile for debugging/analytics (admin only)
 */
router.get('/profile/:uid', verifyFirebaseToken, async (req, res) => {
    try {
        const { uid } = req.params;
        const requestingUid = req.user.uid;

        // Only allow users to see their own profile or admin access
        if (uid !== requestingUid && !req.user.isAdmin) {
            return res.status(403).json({
                success: false,
                error: 'Forbidden',
                message: 'You can only access your own profile'
            });
        }

        logger.info(`📊 Profile request for user: ${uid} by ${requestingUid}`);

        const userProfile = await userProfileService.getUserProfile(uid);
        
        if (!userProfile) {
            return res.status(404).json({
                success: false,
                error: 'Profile not found',
                message: 'User profile not found'
            });
        }

        res.json({
            success: true,
            data: userProfile,
            message: 'User profile retrieved successfully'
        });

    } catch (error) {
        logger.error('❌ Error retrieving user profile:', error);
        
        res.status(500).json({
            success: false,
            error: 'Failed to retrieve user profile',
            message: 'An error occurred while retrieving the user profile'
        });
    }
});

/**
 * DELETE /api/feed/cache/:uid
 * Invalidate feed cache for a user
 */
router.delete('/cache/:uid', verifyFirebaseToken, async (req, res) => {
    try {
        const { uid } = req.params;
        const requestingUid = req.user.uid;

        // Only allow users to invalidate their own cache or admin access
        if (uid !== requestingUid && !req.user.isAdmin) {
            return res.status(403).json({
                success: false,
                error: 'Forbidden',
                message: 'You can only invalidate your own cache'
            });
        }

        logger.info(`🗑️ Cache invalidation request for user: ${uid} by ${requestingUid}`);

        // Invalidate both feed cache and profile cache
        await feedService.invalidateUserFeedCache(uid);
        userProfileService.invalidateUserProfile(uid);

        res.json({
            success: true,
            message: 'Feed cache invalidated successfully'
        });

    } catch (error) {
        logger.error('❌ Error invalidating feed cache:', error);
        
        res.status(500).json({
            success: false,
            error: 'Failed to invalidate feed cache',
            message: 'An error occurred while invalidating the feed cache'
        });
    }
});

/**
 * GET /api/feed/analytics/:uid
 * Get feed analytics for a user
 */
router.get('/analytics/:uid', verifyFirebaseToken, async (req, res) => {
    try {
        const { uid } = req.params;
        const requestingUid = req.user.uid;

        // Only allow users to see their own analytics or admin access
        if (uid !== requestingUid && !req.user.isAdmin) {
            return res.status(403).json({
                success: false,
                error: 'Forbidden',
                message: 'You can only access your own analytics'
            });
        }

        logger.info(`📈 Analytics request for user: ${uid} by ${requestingUid}`);

        const analytics = await feedService.getFeedAnalytics(uid);
        
        if (!analytics) {
            return res.status(404).json({
                success: false,
                error: 'Analytics not found',
                message: 'Feed analytics not found for this user'
            });
        }

        res.json({
            success: true,
            data: analytics,
            message: 'Feed analytics retrieved successfully'
        });

    } catch (error) {
        logger.error('❌ Error retrieving feed analytics:', error);
        
        res.status(500).json({
            success: false,
            error: 'Failed to retrieve feed analytics',
            message: 'An error occurred while retrieving feed analytics'
        });
    }
});

/**
 * POST /api/feed/feedback
 * Submit feedback on feed recommendations
 */
router.post('/feedback', verifyFirebaseToken, async (req, res) => {
    try {
        const uid = req.user.uid;
        const {
            templateId,
            action, // 'like', 'dislike', 'not_interested', 'report'
            reason,
            metadata
        } = req.body;

        // Validate required fields
        if (!templateId || !action) {
            return res.status(400).json({
                success: false,
                error: 'Missing required fields',
                message: 'templateId and action are required'
            });
        }

        logger.info(`📝 Feed feedback from user: ${uid}`, {
            templateId,
            action,
            reason
        });

        // This would integrate with the existing engagement logging
        // For now, we'll log the feedback for future ML training
        logger.info('🎯 Feed feedback logged for ML training:', {
            uid,
            templateId,
            action,
            reason,
            metadata,
            timestamp: new Date()
        });

        res.json({
            success: true,
            message: 'Feedback submitted successfully'
        });

    } catch (error) {
        logger.error('❌ Error submitting feed feedback:', error);
        
        res.status(500).json({
            success: false,
            error: 'Failed to submit feedback',
            message: 'An error occurred while submitting feedback'
        });
    }
});

/**
 * GET /api/feed/config
 * Get current feed configuration (admin only)
 */
router.get('/config', verifyFirebaseToken, async (req, res) => {
    try {
        // Check if user is admin
        if (!req.user.isAdmin) {
            return res.status(403).json({
                success: false,
                error: 'Forbidden',
                message: 'Admin access required'
            });
        }

        const config = {
            scoring: templateScoringService.getScoringConfig(),
            caching: userProfileService.getCacheStats(),
            feedComposition: {
                personalized: 0.6,
                trending: 0.2,
                fresh: 0.1,
                serendipity: 0.1
            }
        };

        res.json({
            success: true,
            data: config,
            message: 'Feed configuration retrieved successfully'
        });

    } catch (error) {
        logger.error('❌ Error retrieving feed config:', error);
        
        res.status(500).json({
            success: false,
            error: 'Failed to retrieve feed configuration',
            message: 'An error occurred while retrieving feed configuration'
        });
    }
});

/**
 * PUT /api/feed/config/scoring
 * Update scoring weights (admin only, for A/B testing)
 */
router.put('/config/scoring', verifyFirebaseToken, async (req, res) => {
    try {
        // Check if user is admin
        if (!req.user.isAdmin) {
            return res.status(403).json({
                success: false,
                error: 'Forbidden',
                message: 'Admin access required'
            });
        }

        const { weights } = req.body;
        
        if (!weights) {
            return res.status(400).json({
                success: false,
                error: 'Missing weights',
                message: 'Scoring weights are required'
            });
        }

        logger.info('📊 Updating scoring weights:', weights);

        templateScoringService.updateScoringWeights(weights);

        res.json({
            success: true,
            message: 'Scoring weights updated successfully'
        });

    } catch (error) {
        logger.error('❌ Error updating scoring weights:', error);
        
        res.status(500).json({
            success: false,
            error: 'Failed to update scoring weights',
            message: error.message || 'An error occurred while updating scoring weights'
        });
    }
});

/**
 * GET /api/feed/health
 * Health check endpoint for feed service
 */
router.get('/health', async (req, res) => {
    try {
        const health = {
            status: 'healthy',
            timestamp: new Date(),
            services: {
                feedService: 'operational',
                userProfileService: 'operational',
                templateScoringService: 'operational'
            },
            cache: userProfileService.getCacheStats()
        };

        res.json({
            success: true,
            data: health,
            message: 'Feed service is healthy'
        });

    } catch (error) {
        logger.error('❌ Feed service health check failed:', error);
        
        res.status(500).json({
            success: false,
            data: {
                status: 'unhealthy',
                timestamp: new Date(),
                error: error.message
            },
            message: 'Feed service health check failed'
        });
    }
});

module.exports = router; 