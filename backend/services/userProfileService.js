const User = require('../models/User');
const Template = require('../models/Template');
const logger = require('../utils/logger');

/**
 * User Profile Service - Analyzes user behavior and builds preference vectors
 * Extracts user preferences from engagement data for personalization
 */
class UserProfileService {
    constructor() {
        this.cacheExpiry = 15 * 60 * 1000; // 15 minutes cache
        this.profileCache = new Map();
    }

    /**
     * Get comprehensive user profile for personalization
     * @param {string} uid - User Firebase UID
     * @returns {Object} User profile with preferences and behavior patterns
     */
    async getUserProfile(uid) {
        try {
            // Check cache first
            const cachedProfile = this.getCachedProfile(uid);
            if (cachedProfile) {
                logger.info(`📋 Returning cached profile for user: ${uid}`);
                return cachedProfile;
            }

            logger.info(`🔍 Building user profile for: ${uid}`);
            const startTime = Date.now();

            // Fetch user data
            const user = await User.findOne({ uid }).lean();
            if (!user) {
                logger.warn(`⚠️ User not found: ${uid}`);
                return null;
            }

            // Build comprehensive profile
            const profile = {
                uid: user.uid,
                preferences: await this.extractUserPreferences(user),
                behavior: await this.analyzeBehaviorPatterns(user),
                demographics: this.extractDemographics(user),
                engagement: await this.calculateEngagementMetrics(user),
                temporal: this.analyzeTemporalPatterns(user),
                metadata: {
                    profileBuiltAt: new Date(),
                    buildTime: Date.now() - startTime,
                    dataPoints: this.countDataPoints(user)
                }
            };

            // Add derived properties
            profile.isNewUser = this.isNewUser(user);
            profile.isPremium = this.isPremiumUser(user);
            profile.engagementLevel = this.calculateEngagementLevel(profile.engagement);
            profile.topCategories = this.getTopCategories(profile.preferences.categories);
            profile.topTags = this.getTopTags(profile.preferences.tags);
            profile.ignoredTemplates = this.getIgnoredTemplateIds(user);

            // Cache the profile
            this.cacheProfile(uid, profile);

            logger.info(`✅ Built user profile for ${uid} in ${Date.now() - startTime}ms`);
            return profile;

        } catch (error) {
            logger.error(`❌ Error building user profile for ${uid}:`, error);
            throw error;
        }
    }

    /**
     * Extract user preferences from engagement data
     */
    async extractUserPreferences(user) {
        const preferences = {
            categories: {},
            tags: {},
            templateTypes: {},
            styles: {},
            creators: {}
        };

        try {
            // Extract category preferences
            if (user.categories && user.categories.length > 0) {
                for (const categoryVisit of user.categories) {
                    const score = this.calculateCategoryScore(categoryVisit);
                    preferences.categories[categoryVisit.category] = score;
                }
            }

            // Extract tag affinity
            if (user.templateAffinity && user.templateAffinity.length > 0) {
                for (const affinity of user.templateAffinity) {
                    preferences.tags[affinity.tag] = affinity.score;
                }
            }

            // Extract preferences from engagement log
            if (user.engagementLog && user.engagementLog.length > 0) {
                await this.extractEngagementPreferences(user.engagementLog, preferences);
            }

            // Extract preferences from likes and favorites
            await this.extractLikesAndFavoritesPreferences(user, preferences);

            // Extract AI style preferences
            if (user.aiUsage && user.aiUsage.stylePreferences) {
                for (const style of user.aiUsage.stylePreferences) {
                    preferences.styles[style] = (preferences.styles[style] || 0) + 1;
                }
            }

            // Normalize scores
            this.normalizePreferences(preferences);

            return preferences;

        } catch (error) {
            logger.error('❌ Error extracting user preferences:', error);
            return preferences;
        }
    }

    /**
     * Calculate category preference score
     */
    calculateCategoryScore(categoryVisit) {
        const visitCount = categoryVisit.visitCount || 1;
        const recencyWeight = this.calculateRecencyWeight(categoryVisit.visitDate);
        const sourceWeight = categoryVisit.source === 'template' ? 1.2 : 1.0;
        
        return visitCount * recencyWeight * sourceWeight;
    }

    /**
     * Calculate recency weight (more recent = higher weight)
     */
    calculateRecencyWeight(date) {
        if (!date) return 0.5;
        
        const daysSince = (Date.now() - new Date(date).getTime()) / (1000 * 60 * 60 * 24);
        
        if (daysSince <= 1) return 1.0;
        if (daysSince <= 7) return 0.8;
        if (daysSince <= 30) return 0.6;
        if (daysSince <= 90) return 0.4;
        return 0.2;
    }

    /**
     * Extract preferences from engagement log
     */
    async extractEngagementPreferences(engagementLog, preferences) {
        try {
            // Get template IDs from recent engagements
            const recentEngagements = engagementLog
                .filter(log => this.isRecentEngagement(log.timestamp))
                .slice(-100); // Last 100 engagements

            if (recentEngagements.length === 0) return;

            // Fetch templates for analysis
            const templateIds = recentEngagements.map(log => log.templateId).filter(Boolean);
            const templates = await Template.find({ _id: { $in: templateIds } }).lean();
            const templateMap = new Map(templates.map(t => [t._id.toString(), t]));

            // Analyze engagement patterns
            for (const engagement of recentEngagements) {
                const template = templateMap.get(engagement.templateId.toString());
                if (!template) continue;

                const actionWeight = this.getActionWeight(engagement.action);
                const recencyWeight = this.calculateRecencyWeight(engagement.timestamp);
                const score = actionWeight * recencyWeight;

                // Update category preferences
                if (template.category) {
                    preferences.categories[template.category] = 
                        (preferences.categories[template.category] || 0) + score;
                }

                // Update tag preferences
                if (template.tags && template.tags.length > 0) {
                    for (const tag of template.tags) {
                        preferences.tags[tag] = (preferences.tags[tag] || 0) + score;
                    }
                }

                // Update template type preferences
                if (template.templateType) {
                    preferences.templateTypes[template.templateType] = 
                        (preferences.templateTypes[template.templateType] || 0) + score;
                }

                // Update creator preferences
                if (template.creatorUid) {
                    preferences.creators[template.creatorUid] = 
                        (preferences.creators[template.creatorUid] || 0) + score;
                }
            }

        } catch (error) {
            logger.error('❌ Error extracting engagement preferences:', error);
        }
    }

    /**
     * Get weight for different engagement actions
     */
    getActionWeight(action) {
        const weights = {
            'FAVORITE': 3.0,
            'FAV': 3.0,
            'LIKE': 2.0,
            'SHARE': 2.5,
            'VIEW': 1.0,
            'UNFAVORITE': -2.0,
            'UNFAV': -2.0,
            'UNLIKE': -1.0
        };
        return weights[action] || 1.0;
    }

    /**
     * Check if engagement is recent (within last 90 days)
     */
    isRecentEngagement(timestamp) {
        if (!timestamp) return false;
        const daysSince = (Date.now() - new Date(timestamp).getTime()) / (1000 * 60 * 60 * 24);
        return daysSince <= 90;
    }

    /**
     * Extract preferences from likes and favorites
     */
    async extractLikesAndFavoritesPreferences(user, preferences) {
        try {
            const templateIds = [
                ...(user.likes || []),
                ...(user.favorites || [])
            ];

            if (templateIds.length === 0) return;

            const templates = await Template.find({ _id: { $in: templateIds } }).lean();

            for (const template of templates) {
                const isFavorite = user.favorites && user.favorites.includes(template._id);
                const isLiked = user.likes && user.likes.includes(template._id);
                
                const score = (isFavorite ? 3.0 : 0) + (isLiked ? 2.0 : 0);

                // Update preferences
                if (template.category) {
                    preferences.categories[template.category] = 
                        (preferences.categories[template.category] || 0) + score;
                }

                if (template.tags && template.tags.length > 0) {
                    for (const tag of template.tags) {
                        preferences.tags[tag] = (preferences.tags[tag] || 0) + score;
                    }
                }

                if (template.templateType) {
                    preferences.templateTypes[template.templateType] = 
                        (preferences.templateTypes[template.templateType] || 0) + score;
                }
            }

        } catch (error) {
            logger.error('❌ Error extracting likes/favorites preferences:', error);
        }
    }

    /**
     * Normalize preference scores
     */
    normalizePreferences(preferences) {
        for (const category of Object.keys(preferences)) {
            const scores = Object.values(preferences[category]);
            if (scores.length === 0) continue;

            const maxScore = Math.max(...scores);
            if (maxScore > 0) {
                for (const key of Object.keys(preferences[category])) {
                    preferences[category][key] = preferences[category][key] / maxScore;
                }
            }
        }
    }

    /**
     * Analyze user behavior patterns
     */
    async analyzeBehaviorPatterns(user) {
        const patterns = {
            activityLevel: 'medium',
            preferredTimeOfDay: null,
            sessionLength: 'medium',
            explorationTendency: 'medium',
            loyaltyLevel: 'medium'
        };

        try {
            // Analyze activity level
            patterns.activityLevel = this.analyzeActivityLevel(user);
            
            // Analyze preferred time (if we had timestamp data)
            patterns.preferredTimeOfDay = this.analyzePreferredTime(user);
            
            // Analyze exploration vs exploitation
            patterns.explorationTendency = this.analyzeExplorationTendency(user);
            
            // Analyze loyalty to creators/categories
            patterns.loyaltyLevel = this.analyzeLoyaltyLevel(user);

            return patterns;

        } catch (error) {
            logger.error('❌ Error analyzing behavior patterns:', error);
            return patterns;
        }
    }

    /**
     * Analyze user activity level
     */
    analyzeActivityLevel(user) {
        const engagementCount = user.engagementLog ? user.engagementLog.length : 0;
        const accountAge = this.getAccountAgeInDays(user);
        const activityRate = accountAge > 0 ? engagementCount / accountAge : 0;

        if (activityRate > 5) return 'high';
        if (activityRate > 1) return 'medium';
        return 'low';
    }

    /**
     * Analyze exploration tendency
     */
    analyzeExplorationTendency(user) {
        const categoryCount = user.categories ? user.categories.length : 0;
        const tagCount = user.templateAffinity ? user.templateAffinity.length : 0;
        
        const diversityScore = categoryCount + (tagCount * 0.5);
        
        if (diversityScore > 15) return 'high';
        if (diversityScore > 5) return 'medium';
        return 'low';
    }

    /**
     * Analyze loyalty level
     */
    analyzeLoyaltyLevel(user) {
        // Analyze repeat interactions with same categories/creators
        if (!user.engagementLog || user.engagementLog.length < 10) {
            return 'low';
        }

        // This would require more complex analysis of repeat patterns
        return 'medium';
    }

    /**
     * Extract user demographics
     */
    extractDemographics(user) {
        return {
            language: user.preferredLanguage || 'en',
            timezone: user.timezone || 'Asia/Kolkata',
            accountAge: this.getAccountAgeInDays(user),
            subscriptionLevel: user.subscription ? user.subscription.planLevel : 'NONE',
            topicSubscriptions: user.topicSubscriptions || []
        };
    }

    /**
     * Calculate engagement metrics
     */
    async calculateEngagementMetrics(user) {
        const metrics = {
            totalEngagements: 0,
            recentEngagements: 0,
            likesCount: 0,
            favoritesCount: 0,
            sharesCount: 0,
            viewsCount: 0,
            engagementRate: 0,
            averageSessionGap: 0
        };

        try {
            metrics.likesCount = user.likes ? user.likes.length : 0;
            metrics.favoritesCount = user.favorites ? user.favorites.length : 0;
            
            if (user.engagementLog && user.engagementLog.length > 0) {
                metrics.totalEngagements = user.engagementLog.length;
                
                // Count recent engagements (last 30 days)
                const thirtyDaysAgo = new Date(Date.now() - 30 * 24 * 60 * 60 * 1000);
                metrics.recentEngagements = user.engagementLog.filter(
                    log => new Date(log.timestamp) > thirtyDaysAgo
                ).length;

                // Count by action type
                for (const log of user.engagementLog) {
                    switch (log.action) {
                        case 'SHARE':
                            metrics.sharesCount++;
                            break;
                        case 'VIEW':
                            metrics.viewsCount++;
                            break;
                    }
                }

                // Calculate engagement rate
                const accountAge = this.getAccountAgeInDays(user);
                metrics.engagementRate = accountAge > 0 ? metrics.totalEngagements / accountAge : 0;
            }

            return metrics;

        } catch (error) {
            logger.error('❌ Error calculating engagement metrics:', error);
            return metrics;
        }
    }

    /**
     * Analyze temporal patterns
     */
    analyzeTemporalPatterns(user) {
        return {
            lastActiveDate: user.lastActive,
            lastOnlineDate: user.lastOnline,
            accountCreated: user.created,
            daysSinceLastActive: this.getDaysSince(user.lastActive),
            daysSinceCreated: this.getAccountAgeInDays(user),
            isActiveUser: this.isActiveUser(user)
        };
    }

    /**
     * Helper methods
     */
    getAccountAgeInDays(user) {
        if (!user.created) return 0;
        return Math.floor((Date.now() - new Date(user.created).getTime()) / (1000 * 60 * 60 * 24));
    }

    getDaysSince(date) {
        if (!date) return Infinity;
        return Math.floor((Date.now() - new Date(date).getTime()) / (1000 * 60 * 60 * 24));
    }

    isNewUser(user) {
        return this.getAccountAgeInDays(user) <= 7;
    }

    isPremiumUser(user) {
        return user.subscription && 
               user.subscription.isActive && 
               user.subscription.planLevel !== 'NONE';
    }

    isActiveUser(user) {
        return this.getDaysSince(user.lastActive) <= 7;
    }

    calculateEngagementLevel(engagement) {
        if (engagement.engagementRate > 3) return 'high';
        if (engagement.engagementRate > 1) return 'medium';
        return 'low';
    }

    getTopCategories(categories, limit = 5) {
        return Object.entries(categories)
            .sort((a, b) => b[1] - a[1])
            .slice(0, limit)
            .map(entry => entry[0]);
    }

    getTopTags(tags, limit = 10) {
        return Object.entries(tags)
            .sort((a, b) => b[1] - a[1])
            .slice(0, limit)
            .map(entry => entry[0]);
    }

    getIgnoredTemplateIds(user) {
        if (!user.ignoredTemplates) return [];
        return user.ignoredTemplates.map(ignored => ignored.templateId);
    }

    countDataPoints(user) {
        return {
            categories: user.categories ? user.categories.length : 0,
            engagements: user.engagementLog ? user.engagementLog.length : 0,
            likes: user.likes ? user.likes.length : 0,
            favorites: user.favorites ? user.favorites.length : 0,
            affinities: user.templateAffinity ? user.templateAffinity.length : 0,
            ignored: user.ignoredTemplates ? user.ignoredTemplates.length : 0
        };
    }

    analyzePreferredTime(user) {
        // Placeholder for time analysis
        // Would require timestamp analysis from engagement log
        return null;
    }

    /**
     * Cache management
     */
    getCachedProfile(uid) {
        const cached = this.profileCache.get(uid);
        if (!cached) return null;

        const age = Date.now() - cached.timestamp;
        if (age > this.cacheExpiry) {
            this.profileCache.delete(uid);
            return null;
        }

        return cached.profile;
    }

    cacheProfile(uid, profile) {
        this.profileCache.set(uid, {
            profile,
            timestamp: Date.now()
        });

        // Clean up old cache entries periodically
        if (this.profileCache.size > 1000) {
            this.cleanupCache();
        }
    }

    cleanupCache() {
        const now = Date.now();
        for (const [uid, cached] of this.profileCache.entries()) {
            if (now - cached.timestamp > this.cacheExpiry) {
                this.profileCache.delete(uid);
            }
        }
    }

    /**
     * Invalidate user profile cache
     */
    invalidateUserProfile(uid) {
        this.profileCache.delete(uid);
        logger.info(`🗑️ Invalidated profile cache for user: ${uid}`);
    }

    /**
     * Get cache statistics
     */
    getCacheStats() {
        return {
            size: this.profileCache.size,
            maxSize: 1000,
            expiryTime: this.cacheExpiry / 1000 / 60 // in minutes
        };
    }
}

module.exports = new UserProfileService(); 