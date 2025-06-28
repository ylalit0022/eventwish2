const User = require('../models/User');
const Template = require('../models/Template');
const userProfileService = require('./userProfileService');
const templateScoringService = require('./templateScoringService');
const logger = require('../utils/logger');

/**
 * Core Feed Service - Orchestrates personalized feed generation
 * Handles multi-section feed generation with personalized, trending, fresh, and category sections
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
        // Multi-section configuration
        this.sectionConfig = {
            personalized: { title: "Just For You", maxItems: 8 },
            trending: { title: "Trending Now", maxItems: 6 },
            fresh: { title: "New Uploads", maxItems: 4 },
            categories: { title: "Popular Categories", maxItems: 5 },
            serendipity: { title: "Discover Something New", maxItems: 3 }
        };
    }

    /**
     * Generate multi-section feed for a user
     * @param {string} uid - User Firebase UID
     * @param {Object} options - Feed generation options
     * @returns {Object} Multi-section feed with different content types
     */
    async generateMultiSectionFeed(uid, options = {}) {
        const startTime = Date.now();
        
        try {
            logger.info(`🎯 Generating multi-section feed for user: ${uid}`);
            
            // Extract options with defaults
            const {
                include = 'personalized,trending,fresh,categories',
                refresh = false,
                categories = null,
                templateTypes = null
            } = options;
            
            // Parse included sections
            const includedSections = include.split(',').map(s => s.trim()).filter(Boolean);
            
            // Step 1: Get user profile and preferences
            const userProfile = await userProfileService.getUserProfile(uid);
            if (!userProfile) {
                logger.warn(`⚠️ User profile not found for uid: ${uid}, using default multi-section feed`);
                return await this.generateDefaultMultiSectionFeed(options);
            }
            
            // Step 2: Check for cached feed (if not refresh)
            if (!refresh) {
                const cachedFeed = await this.getCachedMultiSectionFeed(uid, includedSections);
                if (cachedFeed) {
                    logger.info(`📋 Returning cached multi-section feed for user: ${uid}`);
                    return cachedFeed;
                }
            }
            
            // Step 3: Generate sections based on included sections
            const sections = [];
            
            // Generate personalized section
            if (includedSections.includes('personalized')) {
                const personalizedSection = await this.generatePersonalizedSection(userProfile, options);
                if (personalizedSection.templates.length > 0) {
                    sections.push(personalizedSection);
                }
            }
            
            // Generate trending section
            if (includedSections.includes('trending')) {
                const trendingSection = await this.generateTrendingSection(userProfile, options);
                if (trendingSection.templates.length > 0) {
                    sections.push(trendingSection);
                }
            }
            
            // Generate fresh section
            if (includedSections.includes('fresh')) {
                const freshSection = await this.generateFreshSection(userProfile, options);
                if (freshSection.templates.length > 0) {
                    sections.push(freshSection);
                }
            }
            
            // Generate category sections
            if (includedSections.includes('categories')) {
                const categorySections = await this.generateCategorySections(userProfile, options);
                sections.push(...categorySections);
            }
            
            // Generate serendipity section
            if (includedSections.includes('serendipity')) {
                const serendipitySection = await this.generateSerendipitySection(userProfile, options);
                if (serendipitySection.templates.length > 0) {
                    sections.push(serendipitySection);
                }
            }
            
            // Step 4: Create multi-section feed response
            const feedResponse = {
                sections: sections,
                metadata: {
                    generatedAt: new Date(),
                    userProfile: {
                        uid: userProfile.uid,
                        preferences: userProfile.preferences
                    },
                    performance: {
                        generationTime: Date.now() - startTime,
                        sectionsCount: sections.length,
                        totalTemplates: sections.reduce((sum, section) => sum + section.templates.length, 0)
                    },
                    configuration: {
                        includedSections: includedSections,
                        sectionConfig: this.sectionConfig
                    }
                }
            };
            
            // Get available categories if requested and add to response
            if (includedSections.includes('categories')) {
                try {
                    const availableCategories = await this.getAvailableCategories();
                    feedResponse.categories = availableCategories;
                    
                    const categoryCount = Object.keys(availableCategories).length;
                    const totalCategoryTemplates = Object.values(availableCategories).reduce((sum, count) => sum + count, 0);
                    
                    logger.info(`📊 Categories fetched for user ${userProfile.uid}: ${categoryCount} categories with ${totalCategoryTemplates} total templates`, {
                        categories: availableCategories,
                        categoryCount: categoryCount,
                        totalTemplates: totalCategoryTemplates
                    });
                } catch (error) {
                    logger.error('❌ Error fetching categories for multi-section feed:', error);
                    feedResponse.categories = {};
                }
            }
            
            // Step 5: Cache the multi-section feed
            await this.cacheMultiSectionFeed(uid, feedResponse, includedSections);
            
            logger.info(`✅ Generated multi-section feed for ${uid} in ${Date.now() - startTime}ms`);
            return feedResponse;
            
        } catch (error) {
            logger.error(`❌ Error generating multi-section feed for ${uid}:`, error);
            
            // Fallback to default multi-section feed on error
            logger.info(`🔄 Falling back to default multi-section feed for ${uid}`);
            return await this.generateDefaultMultiSectionFeed(options);
        }
    }

    /**
     * Generate personalized section
     */
    async generatePersonalizedSection(userProfile, options = {}) {
        try {
            const maxItems = this.sectionConfig.personalized.maxItems;
            const personalizedCandidates = await this.fetchPersonalizedCandidates(
                userProfile, 
                maxItems * 2, // Fetch more for better selection
                options
            );
            
            const scoredTemplates = await templateScoringService.scoreTemplatesForUser(
                personalizedCandidates,
                userProfile
            );
            
            const topTemplates = scoredTemplates.slice(0, maxItems);
            
            return {
                type: "personalized",
                title: this.sectionConfig.personalized.title,
                templates: topTemplates,
                metadata: {
                    candidateCount: personalizedCandidates.length,
                    finalCount: topTemplates.length
                }
            };
        } catch (error) {
            logger.error('❌ Error generating personalized section:', error);
            return {
                type: "personalized",
                title: this.sectionConfig.personalized.title,
                templates: [],
                metadata: { error: error.message }
            };
        }
    }

    /**
     * Generate trending section
     */
    async generateTrendingSection(userProfile, options = {}) {
        try {
            const maxItems = this.sectionConfig.trending.maxItems;
            const trendingCandidates = await this.fetchTrendingCandidates(maxItems, options);
            
            return {
                type: "trending",
                title: this.sectionConfig.trending.title,
                templates: trendingCandidates.slice(0, maxItems),
                metadata: {
                    candidateCount: trendingCandidates.length,
                    finalCount: Math.min(trendingCandidates.length, maxItems)
                }
            };
        } catch (error) {
            logger.error('❌ Error generating trending section:', error);
            return {
                type: "trending",
                title: this.sectionConfig.trending.title,
                templates: [],
                metadata: { error: error.message }
            };
        }
    }

    /**
     * Generate fresh section
     */
    async generateFreshSection(userProfile, options = {}) {
        try {
            const maxItems = this.sectionConfig.fresh.maxItems;
            const freshCandidates = await this.fetchFreshCandidates(maxItems, options);
            
            return {
                type: "fresh",
                title: this.sectionConfig.fresh.title,
                templates: freshCandidates.slice(0, maxItems),
                metadata: {
                    candidateCount: freshCandidates.length,
                    finalCount: Math.min(freshCandidates.length, maxItems)
                }
            };
        } catch (error) {
            logger.error('❌ Error generating fresh section:', error);
            return {
                type: "fresh",
                title: this.sectionConfig.fresh.title,
                templates: [],
                metadata: { error: error.message }
            };
        }
    }

    /**
     * Generate category sections based on user preferences
     */
    async generateCategorySections(userProfile, options = {}) {
        try {
            const sections = [];
            const maxCategorySections = 2; // Limit to 2 category sections
            const maxItemsPerCategory = 4;
            
            // Get user's top categories
            const topCategories = userProfile.topCategories || [];
            const categoriesToShow = topCategories.slice(0, maxCategorySections);
            
            for (const category of categoriesToShow) {
                const categoryTemplates = await this.fetchCategoryTemplates(category, maxItemsPerCategory, options);
                
                if (categoryTemplates.length > 0) {
                    sections.push({
                        type: "category",
                        category: category,
                        title: `${category.charAt(0).toUpperCase() + category.slice(1)} Picks`,
                        templates: categoryTemplates,
                        metadata: {
                            candidateCount: categoryTemplates.length,
                            finalCount: categoryTemplates.length
                        }
                    });
                }
            }
            
            return sections;
        } catch (error) {
            logger.error('❌ Error generating category sections:', error);
            return [];
        }
    }

    /**
     * Generate serendipity section
     */
    async generateSerendipitySection(userProfile, options = {}) {
        try {
            const maxItems = this.sectionConfig.serendipity.maxItems;
            const serendipityCandidates = await this.fetchSerendipityCandidates(maxItems, userProfile, options);
            
            return {
                type: "serendipity",
                title: this.sectionConfig.serendipity.title,
                templates: serendipityCandidates.slice(0, maxItems),
                metadata: {
                    candidateCount: serendipityCandidates.length,
                    finalCount: Math.min(serendipityCandidates.length, maxItems)
                }
            };
        } catch (error) {
            logger.error('❌ Error generating serendipity section:', error);
            return {
                type: "serendipity",
                title: this.sectionConfig.serendipity.title,
                templates: [],
                metadata: { error: error.message }
            };
        }
    }

    /**
     * Fetch templates for a specific category
     */
    async fetchCategoryTemplates(category, count, filters = {}) {
        const query = {
            status: true,
            isFlagged: false,
            moderationStatus: 'approved',
            category: category
        };
        
        // Apply additional filters
        if (filters.templateTypes) {
            query.templateType = { $in: filters.templateTypes };
        }
        
        return await Template.find(query)
            .sort({ weeklyTrendingScore: -1, isFeatured: -1, createdAt: -1 })
            .limit(count)
            .lean();
    }

    /**
     * Generate default multi-section feed for fallback scenarios
     */
    async generateDefaultMultiSectionFeed(options = {}) {
        try {
            logger.info('🔄 Generating default multi-section feed');
            
            const {
                include = 'trending,fresh',
                categories = null,
                templateTypes = null
            } = options;
            
            const includedSections = include.split(',').map(s => s.trim()).filter(Boolean);
            const sections = [];
            
            // Generate trending section for default feed
            if (includedSections.includes('trending')) {
                const trendingTemplates = await this.fetchTrendingCandidates(6, { categories, templateTypes });
                if (trendingTemplates.length > 0) {
                    sections.push({
                        type: "trending",
                        title: "Trending Now",
                        templates: trendingTemplates,
                        metadata: {
                            candidateCount: trendingTemplates.length,
                            finalCount: trendingTemplates.length
                        }
                    });
                }
            }
            
            // Generate fresh section for default feed
            if (includedSections.includes('fresh')) {
                const freshTemplates = await this.fetchFreshCandidates(4, { categories, templateTypes });
                if (freshTemplates.length > 0) {
                    sections.push({
                        type: "fresh",
                        title: "New Uploads",
                        templates: freshTemplates,
                        metadata: {
                            candidateCount: freshTemplates.length,
                            finalCount: freshTemplates.length
                        }
                    });
                }
            }
            
            // Generate default category sections for unauthenticated users
            if (includedSections.includes('categories')) {
                const defaultCategorySections = await this.generateDefaultCategorySections(options);
                sections.push(...defaultCategorySections);
            }
            
            // Get available categories if requested
            let availableCategories = null;
            if (includedSections.includes('categories')) {
                availableCategories = await this.getAvailableCategories();
                
                if (availableCategories) {
                    const categoryCount = Object.keys(availableCategories).length;
                    const totalCategoryTemplates = Object.values(availableCategories).reduce((sum, count) => sum + count, 0);
                    
                    logger.info(`📊 Categories fetched for default feed: ${categoryCount} categories with ${totalCategoryTemplates} total templates`, {
                        categories: availableCategories,
                        categoryCount: categoryCount,
                        totalTemplates: totalCategoryTemplates
                    });
                }
            }
            
            const response = {
                sections: sections,
                metadata: {
                    generatedAt: new Date(),
                    feedType: 'default',
                    fallback: true,
                    configuration: {
                        includedSections: includedSections
                    }
                }
            };
            
            // Add categories to response if available
            if (availableCategories) {
                response.categories = availableCategories;
            }
            
            return response;
            
        } catch (error) {
            logger.error('❌ Error generating default multi-section feed:', error);
            throw error;
        }
    }

    /**
     * Generate default category sections for unauthenticated users
     */
    async generateDefaultCategorySections(options = {}) {
        try {
            const sections = [];
            const maxCategorySections = 2; // Limit to 2 category sections
            const maxItemsPerCategory = 4;
            
            // Get top categories based on template count and usage
            const topCategories = await this.getTopCategories(maxCategorySections);
            
            for (const categoryInfo of topCategories) {
                const categoryTemplates = await this.fetchCategoryTemplates(
                    categoryInfo.category, 
                    maxItemsPerCategory, 
                    options
                );
                
                if (categoryTemplates.length > 0) {
                    sections.push({
                        type: "category",
                        category: categoryInfo.category,
                        title: `${categoryInfo.category.charAt(0).toUpperCase() + categoryInfo.category.slice(1)} Picks`,
                        templates: categoryTemplates,
                        metadata: {
                            candidateCount: categoryTemplates.length,
                            finalCount: categoryTemplates.length,
                            totalInCategory: categoryInfo.count
                        }
                    });
                }
            }
            
            logger.info(`📂 Generated ${sections.length} default category sections`);
            return sections;
        } catch (error) {
            logger.error('❌ Error generating default category sections:', error);
            return [];
        }
    }

    /**
     * Get available categories with their template counts
     */
    async getAvailableCategories() {
        try {
            const Template = require('../models/Template');
            
            const categories = await Template.aggregate([
                {
                    $match: {
                        status: true,
                        isFlagged: false,
                        moderationStatus: 'approved',
                        category: { $exists: true, $ne: null, $ne: '' }
                    }
                },
                {
                    $group: {
                        _id: '$category',
                        count: { $sum: 1 }
                    }
                },
                {
                    $sort: { count: -1 }
                }
            ]);
            
            const categoriesObj = categories.reduce((acc, curr) => {
                if (curr._id) { // Only include non-null categories
                    acc[curr._id] = curr.count;
                }
                return acc;
            }, {});
            
            logger.info(`📊 Retrieved ${Object.keys(categoriesObj).length} available categories`);
            return categoriesObj;
        } catch (error) {
            logger.error('❌ Error getting available categories:', error);
            return {};
        }
    }

    /**
     * Get top categories based on template count and usage
     */
    async getTopCategories(limit = 5) {
        try {
            const Template = require('../models/Template');
            
            const topCategories = await Template.aggregate([
                {
                    $match: {
                        status: true,
                        isFlagged: false,
                        moderationStatus: 'approved',
                        category: { $exists: true, $ne: null, $ne: '' }
                    }
                },
                {
                    $group: {
                        _id: '$category',
                        count: { $sum: 1 },
                        totalUsage: { $sum: '$usageCount' },
                        avgRating: { $avg: '$rating' }
                    }
                },
                {
                    $addFields: {
                        score: {
                            $add: [
                                { $multiply: ['$count', 2] }, // Weight template count
                                { $divide: ['$totalUsage', 10] }, // Weight usage
                                { $multiply: ['$avgRating', 5] } // Weight rating
                            ]
                        }
                    }
                },
                {
                    $sort: { score: -1, count: -1 }
                },
                {
                    $limit: limit
                },
                {
                    $project: {
                        category: '$_id',
                        count: 1,
                        totalUsage: 1,
                        avgRating: 1,
                        score: 1,
                        _id: 0
                    }
                }
            ]);
            
            logger.info(`🏆 Retrieved top ${topCategories.length} categories`);
            return topCategories;
        } catch (error) {
            logger.error('❌ Error getting top categories:', error);
            return [];
        }
    }

    /**
     * Get cached multi-section feed if available and not expired
     */
    async getCachedMultiSectionFeed(uid, includedSections) {
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
            
            // Populate templates from cached templateIds
            const sections = [];
            for (const cachedSection of user.cachedHomeFeed) {
                if (includedSections.includes(cachedSection.type) && cachedSection.templateIds && cachedSection.templateIds.length > 0) {
                    try {
                        // Fetch templates by IDs
                        const templates = await Template.find({
                            _id: { $in: cachedSection.templateIds },
                            status: true,
                            isFlagged: false,
                            moderationStatus: 'approved'
                        }).lean();
                        
                        if (templates.length > 0) {
                            sections.push({
                                type: cachedSection.type,
                                title: this.sectionConfig[cachedSection.type]?.title || cachedSection.type,
                                templates: templates,
                                metadata: { fromCache: true }
                            });
                        }
                    } catch (error) {
                        logger.warn(`⚠️ Error fetching cached templates for section ${cachedSection.type}:`, error);
                        continue;
                    }
                }
            }
            
            if (sections.length === 0) {
                return null;
            }
            
            const cachedResponse = {
                sections: sections,
                metadata: {
                    cached: true,
                    generatedAt: user.homeFeedLastGeneratedAt,
                    cacheAge: Math.round(cacheAge / 1000), // in seconds
                    configuration: {
                        includedSections: includedSections
                    }
                }
            };
            
            // Add categories to cached response if requested
            if (includedSections.includes('categories')) {
                try {
                    const availableCategories = await this.getAvailableCategories();
                    cachedResponse.categories = availableCategories;
                    
                    const categoryCount = Object.keys(availableCategories).length;
                    const totalCategoryTemplates = Object.values(availableCategories).reduce((sum, count) => sum + count, 0);
                    
                    logger.info(`📊 Categories added to cached feed for user ${uid}: ${categoryCount} categories with ${totalCategoryTemplates} total templates`, {
                        categories: availableCategories,
                        categoryCount: categoryCount,
                        totalTemplates: totalCategoryTemplates
                    });
                } catch (error) {
                    logger.error('❌ Error fetching categories for cached feed:', error);
                    cachedResponse.categories = {};
                }
            }
            
            return cachedResponse;
            
        } catch (error) {
            logger.error('❌ Error getting cached multi-section feed:', error);
            return null;
        }
    }

    /**
     * Cache multi-section feed for future requests
     */
    async cacheMultiSectionFeed(uid, feedResponse, includedSections) {
        try {
            // Convert sections to the correct schema format: {type, templateIds}
            const cachedSections = feedResponse.sections.map(section => ({
                type: section.type,
                templateIds: section.templates.map(template => template._id)
            }));
            
            await User.updateOne(
                { uid },
                {
                    $set: {
                        cachedHomeFeed: cachedSections,
                        homeFeedLastGeneratedAt: new Date()
                    }
                }
            );
            logger.info(`💾 Cached multi-section feed for user: ${uid} with ${cachedSections.length} sections`);
        } catch (error) {
            logger.error('❌ Error caching multi-section feed:', error);
            // Don't throw error, caching is not critical
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