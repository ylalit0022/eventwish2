const User = require('../models/User');
const Template = require('../models/Template');
const userProfileService = require('./userProfileService');
const templateScoringService = require('./templateScoringService');
const logger = require('../utils/logger');

/**
 * Core Feed Service - Orchestrates personalized feed generation
 * Handles the main logic for creating personalized feeds for users
 */
class FeedService {
    constructor() {
        this.defaultPageSize = 20;
        this.maxPageSize = 50;
        this.feedComposition = {
            personalized: 0.6,  // 60% personalized content
            trending: 0.2,      // 20% trending content
            fresh: 0.1,         // 10% fresh content
            serendipity: 0.1    // 10% serendipity content
        };
    }

    /**
     * Generate personalized feed for a user
     * @param {string} uid - User Firebase UID
     * @param {Object} options - Feed generation options
     * @returns {Object} Personalized feed with templates and metadata
     */
    async generatePersonalizedFeed(uid, options = {}) {
        const startTime = Date.now();
        
        try {
            logger.info(`🎯 Generating personalized feed for user: ${uid}`);
            
            // Extract options with defaults
            const {
                page = 1,
                limit = this.defaultPageSize,
                refresh = false,
                categories = null,
                templateTypes = null
            } = options;
            
            // Validate and sanitize inputs
            const pageSize = Math.min(limit, this.maxPageSize);
            const skipCount = (page - 1) * pageSize;
            
            // Step 1: Get user profile and preferences
            const userProfile = await userProfileService.getUserProfile(uid);
            if (!userProfile) {
                logger.warn(`⚠️ User profile not found for uid: ${uid}, using default feed`);
                return await this.generateDefaultFeed(options);
            }
            
            // Step 2: Check for cached feed (if not refresh)
            if (!refresh) {
                const cachedFeed = await this.getCachedFeed(uid, page, pageSize);
                if (cachedFeed) {
                    logger.info(`📋 Returning cached feed for user: ${uid}, page: ${page}`);
                    return cachedFeed;
                }
            }
            
            // Step 3: Calculate feed composition based on user preferences
            const composition = this.calculateFeedComposition(userProfile);
            const templateCounts = this.calculateTemplateCounts(pageSize, composition);
            
            logger.info(`🎨 Feed composition for ${uid}:`, templateCounts);
            
            // Step 4: Fetch candidate templates for each category
            const candidateTemplates = await this.fetchCandidateTemplates(
                userProfile, 
                templateCounts, 
                { categories, templateTypes }
            );
            
            // Step 5: Score and rank all candidates
            const scoredTemplates = await templateScoringService.scoreTemplatesForUser(
                candidateTemplates,
                userProfile
            );
            
            // Step 6: Apply diversity and serendipity
            const diversifiedTemplates = this.applyDiversityFilters(scoredTemplates, userProfile);
            
            // Step 7: Paginate and format results
            const paginatedTemplates = diversifiedTemplates.slice(skipCount, skipCount + pageSize);
            
            // Step 8: Create feed response
            const feedResponse = {
                templates: paginatedTemplates,
                pagination: {
                    page: page,
                    limit: pageSize,
                    total: diversifiedTemplates.length,
                    hasMore: (skipCount + pageSize) < diversifiedTemplates.length
                },
                metadata: {
                    generatedAt: new Date(),
                    userProfile: {
                        uid: userProfile.uid,
                        preferences: userProfile.preferences,
                        composition: composition
                    },
                    performance: {
                        generationTime: Date.now() - startTime,
                        candidateCount: candidateTemplates.length,
                        finalCount: paginatedTemplates.length
                    }
                }
            };
            
            // Step 9: Cache the feed for future requests
            await this.cacheFeed(uid, feedResponse, page);
            
            logger.info(`✅ Generated personalized feed for ${uid} in ${Date.now() - startTime}ms`);
            return feedResponse;
            
        } catch (error) {
            logger.error(`❌ Error generating personalized feed for ${uid}:`, error);
            
            // Fallback to default feed on error
            logger.info(`🔄 Falling back to default feed for ${uid}`);
            return await this.generateDefaultFeed(options);
        }
    }
    
    /**
     * Calculate feed composition based on user profile
     * @param {Object} userProfile - User profile with preferences
     * @returns {Object} Adjusted composition percentages
     */
    calculateFeedComposition(userProfile) {
        const baseComposition = { ...this.feedComposition };
        
        // Adjust based on user engagement patterns
        if (userProfile.engagementLevel === 'high') {
            baseComposition.personalized += 0.1;
            baseComposition.serendipity -= 0.1;
        } else if (userProfile.engagementLevel === 'low') {
            baseComposition.trending += 0.1;
            baseComposition.personalized -= 0.1;
        }
        
        // Adjust for new users (less than 7 days old)
        if (userProfile.isNewUser) {
            baseComposition.trending += 0.15;
            baseComposition.fresh += 0.05;
            baseComposition.personalized -= 0.2;
        }
        
        // Adjust for premium users
        if (userProfile.isPremium) {
            baseComposition.fresh += 0.05;
            baseComposition.serendipity += 0.05;
            baseComposition.trending -= 0.1;
        }
        
        return baseComposition;
    }
    
    /**
     * Calculate template counts for each category
     * @param {number} totalCount - Total templates needed
     * @param {Object} composition - Feed composition percentages
     * @returns {Object} Template counts for each category
     */
    calculateTemplateCounts(totalCount, composition) {
        return {
            personalized: Math.round(totalCount * composition.personalized),
            trending: Math.round(totalCount * composition.trending),
            fresh: Math.round(totalCount * composition.fresh),
            serendipity: Math.round(totalCount * composition.serendipity)
        };
    }
    
    /**
     * Fetch candidate templates for scoring
     * @param {Object} userProfile - User profile with preferences
     * @param {Object} templateCounts - Required template counts per category
     * @param {Object} filters - Additional filters
     * @returns {Array} Array of candidate templates
     */
    async fetchCandidateTemplates(userProfile, templateCounts, filters = {}) {
        const candidates = [];
        
        try {
            // Fetch personalized candidates based on user preferences
            if (templateCounts.personalized > 0) {
                const personalizedCandidates = await this.fetchPersonalizedCandidates(
                    userProfile, 
                    templateCounts.personalized * 3, // Fetch 3x for better selection
                    filters
                );
                candidates.push(...personalizedCandidates.map(t => ({ ...t, category: 'personalized' })));
            }
            
            // Fetch trending candidates
            if (templateCounts.trending > 0) {
                const trendingCandidates = await this.fetchTrendingCandidates(
                    templateCounts.trending * 2,
                    filters
                );
                candidates.push(...trendingCandidates.map(t => ({ ...t, category: 'trending' })));
            }
            
            // Fetch fresh candidates
            if (templateCounts.fresh > 0) {
                const freshCandidates = await this.fetchFreshCandidates(
                    templateCounts.fresh * 2,
                    filters
                );
                candidates.push(...freshCandidates.map(t => ({ ...t, category: 'fresh' })));
            }
            
            // Fetch serendipity candidates
            if (templateCounts.serendipity > 0) {
                const serendipityCandidates = await this.fetchSerendipityCandidates(
                    templateCounts.serendipity * 2,
                    userProfile,
                    filters
                );
                candidates.push(...serendipityCandidates.map(t => ({ ...t, category: 'serendipity' })));
            }
            
            logger.info(`📊 Fetched ${candidates.length} candidate templates`);
            return candidates;
            
        } catch (error) {
            logger.error('❌ Error fetching candidate templates:', error);
            throw error;
        }
    }
    
    /**
     * Fetch personalized candidates based on user preferences
     */
    async fetchPersonalizedCandidates(userProfile, count, filters) {
        const query = {
            status: true,
            isFlagged: false,
            moderationStatus: 'approved'
        };
        
        // Add category filters based on user preferences
        if (userProfile.topCategories && userProfile.topCategories.length > 0) {
            query.category = { $in: userProfile.topCategories };
        }
        
        // Add tag filters based on user affinity
        if (userProfile.topTags && userProfile.topTags.length > 0) {
            query.tags = { $in: userProfile.topTags };
        }
        
        // Exclude ignored templates
        if (userProfile.ignoredTemplates && userProfile.ignoredTemplates.length > 0) {
            query._id = { $nin: userProfile.ignoredTemplates };
        }
        
        // Apply additional filters
        if (filters.categories) {
            query.category = { $in: filters.categories };
        }
        if (filters.templateTypes) {
            query.templateType = { $in: filters.templateTypes };
        }
        
        return await Template.find(query)
            .sort({ weeklyTrendingScore: -1, createdAt: -1 })
            .limit(count)
            .lean();
    }
    
    /**
     * Fetch trending candidates
     */
    async fetchTrendingCandidates(count, filters) {
        const query = {
            status: true,
            isFlagged: false,
            moderationStatus: 'approved',
            weeklyTrendingScore: { $gt: 0 }
        };
        
        // Apply filters
        if (filters.categories) {
            query.category = { $in: filters.categories };
        }
        if (filters.templateTypes) {
            query.templateType = { $in: filters.templateTypes };
        }
        
        return await Template.find(query)
            .sort({ weeklyTrendingScore: -1, isTrending: -1 })
            .limit(count)
            .lean();
    }
    
    /**
     * Fetch fresh candidates (recently created)
     */
    async fetchFreshCandidates(count, filters) {
        const query = {
            status: true,
            isFlagged: false,
            moderationStatus: 'approved',
            createdAt: { $gte: new Date(Date.now() - 7 * 24 * 60 * 60 * 1000) } // Last 7 days
        };
        
        // Apply filters
        if (filters.categories) {
            query.category = { $in: filters.categories };
        }
        if (filters.templateTypes) {
            query.templateType = { $in: filters.templateTypes };
        }
        
        return await Template.find(query)
            .sort({ createdAt: -1, visibilityScore: -1 })
            .limit(count)
            .lean();
    }
    
    /**
     * Fetch serendipity candidates (random discovery)
     */
    async fetchSerendipityCandidates(count, userProfile, filters) {
        const query = {
            status: true,
            isFlagged: false,
            moderationStatus: 'approved'
        };
        
        // Exclude user's top categories for discovery
        if (userProfile.topCategories && userProfile.topCategories.length > 0) {
            query.category = { $nin: userProfile.topCategories };
        }
        
        // Exclude ignored templates
        if (userProfile.ignoredTemplates && userProfile.ignoredTemplates.length > 0) {
            query._id = { $nin: userProfile.ignoredTemplates };
        }
        
        // Apply filters
        if (filters.categories) {
            query.category = { $in: filters.categories };
        }
        if (filters.templateTypes) {
            query.templateType = { $in: filters.templateTypes };
        }
        
        // Use aggregation for random sampling
        return await Template.aggregate([
            { $match: query },
            { $sample: { size: count } }
        ]);
    }
    
    /**
     * Apply diversity filters to avoid repetitive content
     */
    applyDiversityFilters(templates, userProfile) {
        const diversified = [];
        const seenCategories = new Set();
        const seenTags = new Set();
        const maxSameCategory = 3;
        const maxSameTag = 2;
        
        for (const template of templates) {
            let categoryCount = 0;
            let tagOverlap = 0;
            
            // Count same category
            for (const existing of diversified) {
                if (existing.category === template.category) {
                    categoryCount++;
                }
            }
            
            // Count tag overlap
            if (template.tags) {
                for (const tag of template.tags) {
                    if (seenTags.has(tag)) {
                        tagOverlap++;
                    }
                }
            }
            
            // Apply diversity rules
            if (categoryCount < maxSameCategory && tagOverlap < maxSameTag) {
                diversified.push(template);
                seenCategories.add(template.category);
                if (template.tags) {
                    template.tags.forEach(tag => seenTags.add(tag));
                }
            }
        }
        
        return diversified;
    }
    
    /**
     * Get cached feed if available and not expired
     */
    async getCachedFeed(uid, page, pageSize) {
        try {
            const user = await User.findOne({ uid }).lean();
            if (!user || !user.cachedHomeFeed || !user.homeFeedLastGeneratedAt) {
                return null;
            }
            
            // Check if cache is expired (30 minutes)
            const cacheAge = Date.now() - user.homeFeedLastGeneratedAt.getTime();
            const cacheExpiry = 30 * 60 * 1000; // 30 minutes
            
            if (cacheAge > cacheExpiry) {
                return null;
            }
            
            // Return cached feed with pagination
            const startIndex = (page - 1) * pageSize;
            const endIndex = startIndex + pageSize;
            const templates = user.cachedHomeFeed.slice(startIndex, endIndex);
            
            return {
                templates,
                pagination: {
                    page,
                    limit: pageSize,
                    total: user.cachedHomeFeed.length,
                    hasMore: endIndex < user.cachedHomeFeed.length
                },
                metadata: {
                    cached: true,
                    generatedAt: user.homeFeedLastGeneratedAt,
                    cacheAge: Math.round(cacheAge / 1000) // in seconds
                }
            };
            
        } catch (error) {
            logger.error('❌ Error getting cached feed:', error);
            return null;
        }
    }
    
    /**
     * Cache feed for future requests
     */
    async cacheFeed(uid, feedResponse, page) {
        try {
            // Only cache the first page
            if (page === 1) {
                await User.updateOne(
                    { uid },
                    {
                        $set: {
                            cachedHomeFeed: feedResponse.templates,
                            homeFeedLastGeneratedAt: new Date()
                        }
                    }
                );
                logger.info(`💾 Cached feed for user: ${uid}`);
            }
        } catch (error) {
            logger.error('❌ Error caching feed:', error);
            // Don't throw error, caching is not critical
        }
    }
    
    /**
     * Generate default feed for fallback scenarios
     */
    async generateDefaultFeed(options = {}) {
        const { page = 1, limit = this.defaultPageSize } = options;
        const pageSize = Math.min(limit, this.maxPageSize);
        const skipCount = (page - 1) * pageSize;
        
        try {
            logger.info('🔄 Generating default feed');
            
            const templates = await Template.find({
                status: true,
                isFlagged: false,
                moderationStatus: 'approved'
            })
            .sort({ weeklyTrendingScore: -1, isFeatured: -1, createdAt: -1 })
            .skip(skipCount)
            .limit(pageSize)
            .lean();
            
            const totalCount = await Template.countDocuments({
                status: true,
                isFlagged: false,
                moderationStatus: 'approved'
            });
            
            return {
                templates,
                pagination: {
                    page,
                    limit: pageSize,
                    total: totalCount,
                    hasMore: (skipCount + pageSize) < totalCount
                },
                metadata: {
                    generatedAt: new Date(),
                    feedType: 'default',
                    fallback: true
                }
            };
            
        } catch (error) {
            logger.error('❌ Error generating default feed:', error);
            throw error;
        }
    }
    
    /**
     * Invalidate cached feed for a user
     */
    async invalidateUserFeedCache(uid) {
        try {
            await User.updateOne(
                { uid },
                {
                    $unset: {
                        cachedHomeFeed: 1,
                        homeFeedLastGeneratedAt: 1
                    }
                }
            );
            logger.info(`🗑️ Invalidated feed cache for user: ${uid}`);
        } catch (error) {
            logger.error('❌ Error invalidating feed cache:', error);
        }
    }
    
    /**
     * Get feed analytics for monitoring
     */
    async getFeedAnalytics(uid) {
        try {
            const user = await User.findOne({ uid }).lean();
            if (!user) {
                return null;
            }
            
            return {
                cacheStatus: {
                    hasCachedFeed: !!user.cachedHomeFeed,
                    cacheSize: user.cachedHomeFeed ? user.cachedHomeFeed.length : 0,
                    lastGenerated: user.homeFeedLastGeneratedAt,
                    cacheAge: user.homeFeedLastGeneratedAt ? 
                        Date.now() - user.homeFeedLastGeneratedAt.getTime() : null
                },
                userProfile: {
                    engagementCount: user.engagementLog ? user.engagementLog.length : 0,
                    categoryCount: user.categories ? user.categories.length : 0,
                    affinityCount: user.templateAffinity ? user.templateAffinity.length : 0,
                    ignoredCount: user.ignoredTemplates ? user.ignoredTemplates.length : 0
                }
            };
        } catch (error) {
            logger.error('❌ Error getting feed analytics:', error);
            return null;
        }
    }
}

module.exports = new FeedService(); 