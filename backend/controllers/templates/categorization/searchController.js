const Template = require('../../../models/Template');
const User = require('../../../models/User');
const mongoose = require('mongoose');
const logger = require('../../../utils/logger');

/**
 * Search Controller
 * Handles advanced search and filtering operations for templates
 * Combines all categorization fields for comprehensive search
 */

// Helper function to handle async operations
const handleAsyncOperation = async (operation, res, successMessage) => {
    try {
        const result = await operation();
        logger.info(`Search operation successful: ${successMessage}`);
        return res.status(200).json({
            success: true,
            message: successMessage,
            data: result,
            timestamp: new Date().toISOString()
        });
    } catch (error) {
        logger.error(`Search operation failed: ${error.message}`, { error: error.stack });
        
        if (error.name === 'ValidationError') {
            return res.status(400).json({
                success: false,
                error: 'VALIDATION_ERROR',
                message: 'Validation failed',
                details: Object.values(error.errors).map(err => ({
                    field: err.path,
                    message: err.message,
                    value: err.value
                })),
                timestamp: new Date().toISOString()
            });
        }
        
        return res.status(500).json({
            success: false,
            error: 'SERVER_ERROR',
            message: 'Internal server error occurred',
            timestamp: new Date().toISOString()
        });
    }
};

// Helper function to build search query
const buildSearchQuery = (filters) => {
    const query = { $and: [] };
    
    // Text search across multiple fields
    if (filters.text) {
        const textRegex = new RegExp(filters.text, 'i');
        query.$and.push({
            $or: [
                { title: textRegex },
                { description: textRegex },
                { tags: textRegex },
                { styleTags: textRegex },
                { searchKeywords: textRegex },
                { category: textRegex },
                { festivalTag: textRegex }
            ]
        });
    }
    
    // Category filter
    if (filters.category) {
        if (Array.isArray(filters.category)) {
            query.$and.push({ category: { $in: filters.category } });
        } else {
            query.$and.push({ category: new RegExp(filters.category, 'i') });
        }
    }
    
    // Festival tag filter
    if (filters.festivalTag) {
        if (Array.isArray(filters.festivalTag)) {
            query.$and.push({ festivalTag: { $in: filters.festivalTag } });
        } else {
            query.$and.push({ festivalTag: new RegExp(filters.festivalTag, 'i') });
        }
    }
    
    // Tags filter
    if (filters.tags && Array.isArray(filters.tags)) {
        query.$and.push({ tags: { $in: filters.tags } });
    }
    
    // Style tags filter
    if (filters.styleTags && Array.isArray(filters.styleTags)) {
        query.$and.push({ styleTags: { $in: filters.styleTags } });
    }
    
    // Search keywords filter
    if (filters.searchKeywords && Array.isArray(filters.searchKeywords)) {
        query.$and.push({ searchKeywords: { $in: filters.searchKeywords } });
    }
    
    // Creator filter
    if (filters.creatorId) {
        query.$and.push({ creatorId: filters.creatorId });
    }
    
    if (filters.creatorUid) {
        query.$and.push({ creatorUid: filters.creatorUid });
    }
    
    // AI generation filter
    if (filters.isAIGenerated !== undefined) {
        query.$and.push({ isAIGenerated: filters.isAIGenerated });
    }
    
    // Premium access filter
    if (filters.isPremium !== undefined) {
        query.$and.push({ isPremium: filters.isPremium });
    }
    
    // Usage count range
    if (filters.minUsage !== undefined || filters.maxUsage !== undefined) {
        const usageFilter = {};
        if (filters.minUsage !== undefined) usageFilter.$gte = parseInt(filters.minUsage);
        if (filters.maxUsage !== undefined) usageFilter.$lte = parseInt(filters.maxUsage);
        query.$and.push({ usageCount: usageFilter });
    }
    
    // Rating range
    if (filters.minRating !== undefined || filters.maxRating !== undefined) {
        const ratingFilter = {};
        if (filters.minRating !== undefined) ratingFilter.$gte = parseFloat(filters.minRating);
        if (filters.maxRating !== undefined) ratingFilter.$lte = parseFloat(filters.maxRating);
        query.$and.push({ rating: ratingFilter });
    }
    
    // Date range
    if (filters.startDate || filters.endDate) {
        const dateFilter = {};
        if (filters.startDate) dateFilter.$gte = new Date(filters.startDate);
        if (filters.endDate) dateFilter.$lte = new Date(filters.endDate);
        query.$and.push({ createdAt: dateFilter });
    }
    
    // Remove empty $and array
    if (query.$and.length === 0) {
        delete query.$and;
    }
    
    return query;
};

/**
 * Advanced template search with multiple filters
 */
const advancedSearch = async (req, res) => {
    const {
        filters = {},
        page = 1,
        limit = 20,
        sort = '-usageCount',
        populate = true
    } = req.body;
    
    await handleAsyncOperation(async () => {
        const query = buildSearchQuery(filters);
        const skip = (parseInt(page) - 1) * parseInt(limit);
        
        let templateQuery = Template.find(query)
            .sort(sort)
            .skip(skip)
            .limit(parseInt(limit));
        
        if (populate) {
            templateQuery = templateQuery
                .populate('creatorId', 'name email profileImage')
                .populate('relatedTemplates', 'title category')
                .populate('similarTemplates', 'title category');
        }
        
        const [templates, total] = await Promise.all([
            templateQuery.lean(),
            Template.countDocuments(query)
        ]);
        
        return {
            filters,
            templates,
            pagination: {
                page: parseInt(page),
                limit: parseInt(limit),
                total,
                pages: Math.ceil(total / parseInt(limit))
            }
        };
    }, res, 'Advanced search completed successfully');
};

/**
 * Text-based search across content and tags
 */
const textSearch = async (req, res) => {
    const {
        query: searchQuery,
        page = 1,
        limit = 20,
        sort = '-usageCount',
        fuzzy = false
    } = req.body;
    
    await handleAsyncOperation(async () => {
        if (!searchQuery || searchQuery.trim().length === 0) {
            throw new Error('Search query is required');
        }
        
        const skip = (parseInt(page) - 1) * parseInt(limit);
        let query;
        
        if (fuzzy) {
            // Use MongoDB text search for fuzzy matching
            query = { $text: { $search: searchQuery } };
        } else {
            // Use regex for exact matching
            const textRegex = new RegExp(searchQuery.trim(), 'i');
            query = {
                $or: [
                    { title: textRegex },
                    { description: textRegex },
                    { tags: textRegex },
                    { styleTags: textRegex },
                    { searchKeywords: textRegex },
                    { category: textRegex },
                    { festivalTag: textRegex }
                ]
            };
        }
        
        const [templates, total] = await Promise.all([
            Template.find(query)
                .select('title description category tags styleTags usageCount rating')
                .populate('creatorId', 'name email profileImage')
                .sort(sort)
                .skip(skip)
                .limit(parseInt(limit))
                .lean(),
            Template.countDocuments(query)
        ]);
        
        return {
            searchQuery,
            fuzzy,
            templates,
            pagination: {
                page: parseInt(page),
                limit: parseInt(limit),
                total,
                pages: Math.ceil(total / parseInt(limit))
            }
        };
    }, res, 'Text search completed successfully');
};

/**
 * Search templates by category with filters
 */
const categorySearch = async (req, res) => {
    const {
        category,
        additionalFilters = {},
        page = 1,
        limit = 20,
        sort = '-usageCount'
    } = req.body;
    
    await handleAsyncOperation(async () => {
        if (!category) {
            throw new Error('Category is required');
        }
        
        const filters = {
            ...additionalFilters,
            category
        };
        
        const query = buildSearchQuery(filters);
        const skip = (parseInt(page) - 1) * parseInt(limit);
        
        const [templates, total] = await Promise.all([
            Template.find(query)
                .select('title description category festivalTag tags usageCount rating')
                .populate('creatorId', 'name email profileImage')
                .sort(sort)
                .skip(skip)
                .limit(parseInt(limit))
                .lean(),
            Template.countDocuments(query)
        ]);
        
        return {
            category,
            additionalFilters,
            templates,
            pagination: {
                page: parseInt(page),
                limit: parseInt(limit),
                total,
                pages: Math.ceil(total / parseInt(limit))
            }
        };
    }, res, 'Category search completed successfully');
};

/**
 * Search templates by creator with filters
 */
const creatorSearch = async (req, res) => {
    const {
        creatorId,
        creatorUid,
        additionalFilters = {},
        page = 1,
        limit = 20,
        sort = '-createdAt'
    } = req.body;
    
    await handleAsyncOperation(async () => {
        if (!creatorId && !creatorUid) {
            throw new Error('Either creatorId or creatorUid is required');
        }
        
        const filters = {
            ...additionalFilters
        };
        
        if (creatorId) filters.creatorId = creatorId;
        if (creatorUid) filters.creatorUid = creatorUid;
        
        const query = buildSearchQuery(filters);
        const skip = (parseInt(page) - 1) * parseInt(limit);
        
        const [templates, total] = await Promise.all([
            Template.find(query)
                .select('title description category festivalTag tags usageCount rating createdAt')
                .populate('creatorId', 'name email profileImage')
                .sort(sort)
                .skip(skip)
                .limit(parseInt(limit))
                .lean(),
            Template.countDocuments(query)
        ]);
        
        return {
            creatorId,
            creatorUid,
            additionalFilters,
            templates,
            pagination: {
                page: parseInt(page),
                limit: parseInt(limit),
                total,
                pages: Math.ceil(total / parseInt(limit))
            }
        };
    }, res, 'Creator search completed successfully');
};

/**
 * Search templates by tags combination
 */
const tagsSearch = async (req, res) => {
    const {
        tags = [],
        styleTags = [],
        searchKeywords = [],
        matchType = 'any', // 'any' or 'all'
        page = 1,
        limit = 20,
        sort = '-usageCount'
    } = req.body;
    
    await handleAsyncOperation(async () => {
        if (tags.length === 0 && styleTags.length === 0 && searchKeywords.length === 0) {
            throw new Error('At least one tag type must be provided');
        }
        
        const skip = (parseInt(page) - 1) * parseInt(limit);
        let query;
        
        if (matchType === 'all') {
            // Must match all provided tags
            query = { $and: [] };
            if (tags.length > 0) query.$and.push({ tags: { $all: tags } });
            if (styleTags.length > 0) query.$and.push({ styleTags: { $all: styleTags } });
            if (searchKeywords.length > 0) query.$and.push({ searchKeywords: { $all: searchKeywords } });
        } else {
            // Match any of the provided tags
            query = { $or: [] };
            if (tags.length > 0) query.$or.push({ tags: { $in: tags } });
            if (styleTags.length > 0) query.$or.push({ styleTags: { $in: styleTags } });
            if (searchKeywords.length > 0) query.$or.push({ searchKeywords: { $in: searchKeywords } });
        }
        
        const [templates, total] = await Promise.all([
            Template.find(query)
                .select('title description category tags styleTags searchKeywords usageCount rating')
                .populate('creatorId', 'name email profileImage')
                .sort(sort)
                .skip(skip)
                .limit(parseInt(limit))
                .lean(),
            Template.countDocuments(query)
        ]);
        
        return {
            searchCriteria: { tags, styleTags, searchKeywords },
            matchType,
            templates,
            pagination: {
                page: parseInt(page),
                limit: parseInt(limit),
                total,
                pages: Math.ceil(total / parseInt(limit))
            }
        };
    }, res, 'Tags search completed successfully');
};

/**
 * Find similar templates to a given template
 */
const similarTemplatesSearch = async (req, res) => {
    const {
        templateId,
        limit = 10,
        includeRelated = true,
        threshold = 0.5
    } = req.body;
    
    await handleAsyncOperation(async () => {
        if (!templateId) {
            throw new Error('templateId is required');
        }
        
        const sourceTemplate = await Template.findById(templateId)
            .select('category tags styleTags searchKeywords festivalTag')
            .lean();
        
        if (!sourceTemplate) {
            throw new Error('Source template not found');
        }
        
        // Build similarity query
        const similarityQuery = {
            _id: { $ne: templateId },
            $or: []
        };
        
        if (sourceTemplate.category) {
            similarityQuery.$or.push({ category: sourceTemplate.category });
        }
        
        if (sourceTemplate.festivalTag) {
            similarityQuery.$or.push({ festivalTag: sourceTemplate.festivalTag });
        }
        
        if (sourceTemplate.tags && sourceTemplate.tags.length > 0) {
            similarityQuery.$or.push({ tags: { $in: sourceTemplate.tags } });
        }
        
        if (sourceTemplate.styleTags && sourceTemplate.styleTags.length > 0) {
            similarityQuery.$or.push({ styleTags: { $in: sourceTemplate.styleTags } });
        }
        
        if (sourceTemplate.searchKeywords && sourceTemplate.searchKeywords.length > 0) {
            similarityQuery.$or.push({ searchKeywords: { $in: sourceTemplate.searchKeywords } });
        }
        
        if (similarityQuery.$or.length === 0) {
            throw new Error('Source template has insufficient data for similarity matching');
        }
        
        let templates = await Template.find(similarityQuery)
            .select('title description category tags styleTags usageCount rating')
            .populate('creatorId', 'name email profileImage')
            .sort({ usageCount: -1, rating: -1 })
            .limit(parseInt(limit) * 2) // Get more to filter by similarity score
            .lean();
        
        // Calculate similarity scores
        templates = templates.map(template => {
            let score = 0;
            let maxScore = 0;
            
            // Category match
            if (sourceTemplate.category && template.category === sourceTemplate.category) {
                score += 3;
            }
            maxScore += 3;
            
            // Festival tag match
            if (sourceTemplate.festivalTag && template.festivalTag === sourceTemplate.festivalTag) {
                score += 2;
            }
            maxScore += 2;
            
            // Tags overlap
            if (sourceTemplate.tags && template.tags) {
                const overlap = sourceTemplate.tags.filter(tag => template.tags.includes(tag)).length;
                score += overlap;
                maxScore += Math.max(sourceTemplate.tags.length, template.tags.length);
            }
            
            // Style tags overlap
            if (sourceTemplate.styleTags && template.styleTags) {
                const overlap = sourceTemplate.styleTags.filter(tag => template.styleTags.includes(tag)).length;
                score += overlap;
                maxScore += Math.max(sourceTemplate.styleTags.length, template.styleTags.length);
            }
            
            const similarityScore = maxScore > 0 ? score / maxScore : 0;
            
            return {
                ...template,
                similarityScore: Math.round(similarityScore * 100) / 100
            };
        });
        
        // Filter by threshold and limit results
        templates = templates
            .filter(template => template.similarityScore >= threshold)
            .sort((a, b) => b.similarityScore - a.similarityScore)
            .slice(0, parseInt(limit));
        
        return {
            sourceTemplateId: templateId,
            threshold,
            similarTemplates: templates,
            totalFound: templates.length
        };
    }, res, 'Similar templates search completed successfully');
};

/**
 * Faceted search with aggregated results
 */
const facetedSearch = async (req, res) => {
    const {
        filters = {},
        facets = ['category', 'festivalTag', 'tags', 'styleTags'],
        page = 1,
        limit = 20,
        sort = '-usageCount'
    } = req.body;
    
    await handleAsyncOperation(async () => {
        const query = buildSearchQuery(filters);
        const skip = (parseInt(page) - 1) * parseInt(limit);
        
        // Build aggregation pipeline
        const pipeline = [
            { $match: query },
            {
                $facet: {
                    templates: [
                        { $sort: { [sort.replace('-', '')]: sort.startsWith('-') ? -1 : 1 } },
                        { $skip: skip },
                        { $limit: parseInt(limit) },
                        {
                            $lookup: {
                                from: 'users',
                                localField: 'creatorId',
                                foreignField: '_id',
                                as: 'creatorInfo'
                            }
                        }
                    ],
                    facets: [
                        {
                            $group: {
                                _id: null,
                                categories: { $addToSet: '$category' },
                                festivalTags: { $addToSet: '$festivalTag' },
                                allTags: { $push: '$tags' },
                                allStyleTags: { $push: '$styleTags' }
                            }
                        }
                    ],
                    total: [{ $count: 'count' }]
                }
            }
        ];
        
        const [result] = await Template.aggregate(pipeline);
        
        const templates = result.templates || [];
        const total = result.total[0]?.count || 0;
        const facetData = result.facets[0] || {};
        
        // Process facets
        const processedFacets = {};
        
        if (facets.includes('category')) {
            processedFacets.categories = (facetData.categories || [])
                .filter(cat => cat && cat !== null)
                .sort();
        }
        
        if (facets.includes('festivalTag')) {
            processedFacets.festivalTags = (facetData.festivalTags || [])
                .filter(tag => tag && tag !== null)
                .sort();
        }
        
        if (facets.includes('tags')) {
            const allTags = (facetData.allTags || []).flat();
            const tagCounts = {};
            allTags.forEach(tag => {
                if (tag) tagCounts[tag] = (tagCounts[tag] || 0) + 1;
            });
            processedFacets.tags = Object.entries(tagCounts)
                .sort(([,a], [,b]) => b - a)
                .slice(0, 50)
                .map(([tag, count]) => ({ tag, count }));
        }
        
        if (facets.includes('styleTags')) {
            const allStyleTags = (facetData.allStyleTags || []).flat();
            const styleTagCounts = {};
            allStyleTags.forEach(tag => {
                if (tag) styleTagCounts[tag] = (styleTagCounts[tag] || 0) + 1;
            });
            processedFacets.styleTags = Object.entries(styleTagCounts)
                .sort(([,a], [,b]) => b - a)
                .slice(0, 50)
                .map(([tag, count]) => ({ tag, count }));
        }
        
        return {
            filters,
            templates,
            facets: processedFacets,
            pagination: {
                page: parseInt(page),
                limit: parseInt(limit),
                total,
                pages: Math.ceil(total / parseInt(limit))
            }
        };
    }, res, 'Faceted search completed successfully');
};

/**
 * Get search suggestions for auto-complete
 */
const getSearchSuggestions = async (req, res) => {
    const { query } = req.params;
    const { limit = 10, types = ['category', 'festivalTag', 'tags'] } = req.query;
    
    await handleAsyncOperation(async () => {
        if (!query || query.trim().length < 2) {
            throw new Error('Query must be at least 2 characters long');
        }
        
        const searchRegex = new RegExp(query.trim(), 'i');
        const suggestions = {};
        
        const pipeline = [
            {
                $project: {
                    category: 1,
                    festivalTag: 1,
                    tags: 1,
                    styleTags: 1,
                    searchKeywords: 1
                }
            },
            {
                $group: {
                    _id: null,
                    categories: { $addToSet: '$category' },
                    festivalTags: { $addToSet: '$festivalTag' },
                    allTags: { $push: '$tags' },
                    allStyleTags: { $push: '$styleTags' },
                    allKeywords: { $push: '$searchKeywords' }
                }
            }
        ];
        
        const [result] = await Template.aggregate(pipeline);
        
        if (types.includes('category')) {
            suggestions.categories = (result.categories || [])
                .filter(cat => cat && searchRegex.test(cat))
                .slice(0, parseInt(limit));
        }
        
        if (types.includes('festivalTag')) {
            suggestions.festivalTags = (result.festivalTags || [])
                .filter(tag => tag && searchRegex.test(tag))
                .slice(0, parseInt(limit));
        }
        
        if (types.includes('tags')) {
            const allTags = (result.allTags || []).flat();
            suggestions.tags = [...new Set(allTags)]
                .filter(tag => tag && searchRegex.test(tag))
                .slice(0, parseInt(limit));
        }
        
        if (types.includes('styleTags')) {
            const allStyleTags = (result.allStyleTags || []).flat();
            suggestions.styleTags = [...new Set(allStyleTags)]
                .filter(tag => tag && searchRegex.test(tag))
                .slice(0, parseInt(limit));
        }
        
        if (types.includes('keywords')) {
            const allKeywords = (result.allKeywords || []).flat();
            suggestions.keywords = [...new Set(allKeywords)]
                .filter(keyword => keyword && searchRegex.test(keyword))
                .slice(0, parseInt(limit));
        }
        
        return {
            query,
            suggestions
        };
    }, res, 'Search suggestions retrieved successfully');
};

/**
 * Get available filter options for search
 */
const getFilterOptions = async (req, res) => {
    const { includeStats = true } = req.body;
    
    await handleAsyncOperation(async () => {
        const pipeline = [
            {
                $group: {
                    _id: null,
                    categories: { $addToSet: '$category' },
                    festivalTags: { $addToSet: '$festivalTag' },
                    allTags: { $push: '$tags' },
                    allStyleTags: { $push: '$styleTags' },
                    creators: { $addToSet: '$creatorId' },
                    minUsage: { $min: '$usageCount' },
                    maxUsage: { $max: '$usageCount' },
                    minRating: { $min: '$rating' },
                    maxRating: { $max: '$rating' },
                    minDate: { $min: '$createdAt' },
                    maxDate: { $max: '$createdAt' }
                }
            }
        ];
        
        const [result] = await Template.aggregate(pipeline);
        
        const filterOptions = {
            categories: (result.categories || []).filter(cat => cat).sort(),
            festivalTags: (result.festivalTags || []).filter(tag => tag).sort(),
            tags: [...new Set((result.allTags || []).flat())].filter(tag => tag).sort(),
            styleTags: [...new Set((result.allStyleTags || []).flat())].filter(tag => tag).sort(),
            ranges: {
                usage: {
                    min: result.minUsage || 0,
                    max: result.maxUsage || 0
                },
                rating: {
                    min: result.minRating || 0,
                    max: result.maxRating || 5
                },
                date: {
                    min: result.minDate,
                    max: result.maxDate
                }
            }
        };
        
        if (includeStats) {
            // Add statistics for popular filters
            const statsPromises = [
                Template.aggregate([
                    { $match: { category: { $exists: true, $ne: null } } },
                    { $group: { _id: '$category', count: { $sum: 1 } } },
                    { $sort: { count: -1 } },
                    { $limit: 20 }
                ]),
                Template.aggregate([
                    { $match: { festivalTag: { $exists: true, $ne: null } } },
                    { $group: { _id: '$festivalTag', count: { $sum: 1 } } },
                    { $sort: { count: -1 } },
                    { $limit: 20 }
                ])
            ];
            
            const [categoryStats, festivalStats] = await Promise.all(statsPromises);
            
            filterOptions.stats = {
                popularCategories: categoryStats.map(stat => ({
                    category: stat._id,
                    count: stat.count
                })),
                popularFestivals: festivalStats.map(stat => ({
                    festivalTag: stat._id,
                    count: stat.count
                }))
            };
        }
        
        return filterOptions;
    }, res, 'Filter options retrieved successfully');
};

/**
 * Search trending templates with categorization filters
 */
const trendingSearch = async (req, res) => {
    const {
        filters = {},
        page = 1,
        limit = 20,
        timeframe = 'week' // 'day', 'week', 'month'
    } = req.body;
    
    await handleAsyncOperation(async () => {
        const query = buildSearchQuery(filters);
        const skip = (parseInt(page) - 1) * parseInt(limit);
        
        // Add trending criteria based on timeframe
        const now = new Date();
        let startDate;
        
        switch (timeframe) {
            case 'day':
                startDate = new Date(now.getTime() - 24 * 60 * 60 * 1000);
                break;
            case 'month':
                startDate = new Date(now.getTime() - 30 * 24 * 60 * 60 * 1000);
                break;
            default: // week
                startDate = new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000);
        }
        
        // Use weekly metrics for trending calculation
        const trendingSort = { weeklyUsageCount: -1, weeklyLikes: -1, usageCount: -1 };
        
        const [templates, total] = await Promise.all([
            Template.find(query)
                .select('title description category tags weeklyUsageCount weeklyLikes usageCount rating')
                .populate('creatorId', 'name email profileImage')
                .sort(trendingSort)
                .skip(skip)
                .limit(parseInt(limit))
                .lean(),
            Template.countDocuments(query)
        ]);
        
        return {
            filters,
            timeframe,
            templates,
            pagination: {
                page: parseInt(page),
                limit: parseInt(limit),
                total,
                pages: Math.ceil(total / parseInt(limit))
            }
        };
    }, res, 'Trending search completed successfully');
};

/**
 * Search popular templates with categorization filters
 */
const popularSearch = async (req, res) => {
    const {
        filters = {},
        page = 1,
        limit = 20,
        metric = 'usage' // 'usage', 'likes', 'rating', 'favorites'
    } = req.body;
    
    await handleAsyncOperation(async () => {
        const query = buildSearchQuery(filters);
        const skip = (parseInt(page) - 1) * parseInt(limit);
        
        let sortField;
        switch (metric) {
            case 'likes':
                sortField = { likes: -1, usageCount: -1 };
                break;
            case 'rating':
                sortField = { rating: -1, ratingCount: -1, usageCount: -1 };
                break;
            case 'favorites':
                sortField = { favorites: -1, usageCount: -1 };
                break;
            default: // usage
                sortField = { usageCount: -1, rating: -1 };
        }
        
        const [templates, total] = await Promise.all([
            Template.find(query)
                .select('title description category tags usageCount likes favorites rating ratingCount')
                .populate('creatorId', 'name email profileImage')
                .sort(sortField)
                .skip(skip)
                .limit(parseInt(limit))
                .lean(),
            Template.countDocuments(query)
        ]);
        
        return {
            filters,
            metric,
            templates,
            pagination: {
                page: parseInt(page),
                limit: parseInt(limit),
                total,
                pages: Math.ceil(total / parseInt(limit))
            }
        };
    }, res, 'Popular search completed successfully');
};

/**
 * Search recent templates with categorization filters
 */
const recentSearch = async (req, res) => {
    const {
        filters = {},
        page = 1,
        limit = 20,
        timeframe = 'week' // 'day', 'week', 'month', 'all'
    } = req.body;
    
    await handleAsyncOperation(async () => {
        const query = buildSearchQuery(filters);
        
        // Add time filter
        if (timeframe !== 'all') {
            const now = new Date();
            let startDate;
            
            switch (timeframe) {
                case 'day':
                    startDate = new Date(now.getTime() - 24 * 60 * 60 * 1000);
                    break;
                case 'month':
                    startDate = new Date(now.getTime() - 30 * 24 * 60 * 60 * 1000);
                    break;
                default: // week
                    startDate = new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000);
            }
            
            query.createdAt = { $gte: startDate };
        }
        
        const skip = (parseInt(page) - 1) * parseInt(limit);
        
        const [templates, total] = await Promise.all([
            Template.find(query)
                .select('title description category tags createdAt usageCount rating')
                .populate('creatorId', 'name email profileImage')
                .sort({ createdAt: -1 })
                .skip(skip)
                .limit(parseInt(limit))
                .lean(),
            Template.countDocuments(query)
        ]);
        
        return {
            filters,
            timeframe,
            templates,
            pagination: {
                page: parseInt(page),
                limit: parseInt(limit),
                total,
                pages: Math.ceil(total / parseInt(limit))
            }
        };
    }, res, 'Recent search completed successfully');
};

/**
 * Get recommended templates based on user preferences
 */
const recommendedSearch = async (req, res) => {
    const {
        userId,
        preferences = {},
        page = 1,
        limit = 20,
        algorithm = 'hybrid' // 'collaborative', 'content', 'hybrid'
    } = req.body;
    
    await handleAsyncOperation(async () => {
        // This is a simplified recommendation system
        // In production, you would use more sophisticated algorithms
        
        const skip = (parseInt(page) - 1) * parseInt(limit);
        let query = {};
        
        // Content-based recommendations
        if (preferences.categories && preferences.categories.length > 0) {
            query.category = { $in: preferences.categories };
        }
        
        if (preferences.tags && preferences.tags.length > 0) {
            query.tags = { $in: preferences.tags };
        }
        
        if (preferences.festivalTags && preferences.festivalTags.length > 0) {
            query.festivalTag = { $in: preferences.festivalTags };
        }
        
        // Exclude templates user has already interacted with
        if (preferences.excludeTemplateIds && preferences.excludeTemplateIds.length > 0) {
            query._id = { $nin: preferences.excludeTemplateIds };
        }
        
        const [templates, total] = await Promise.all([
            Template.find(query)
                .select('title description category tags usageCount rating')
                .populate('creatorId', 'name email profileImage')
                .sort({ usageCount: -1, rating: -1 })
                .skip(skip)
                .limit(parseInt(limit))
                .lean(),
            Template.countDocuments(query)
        ]);
        
        return {
            userId,
            preferences,
            algorithm,
            templates,
            pagination: {
                page: parseInt(page),
                limit: parseInt(limit),
                total,
                pages: Math.ceil(total / parseInt(limit))
            }
        };
    }, res, 'Recommended search completed successfully');
};

/**
 * Export search results
 */
const exportSearchResults = async (req, res) => {
    const {
        filters = {},
        format = 'json', // 'json', 'csv'
        fields = ['title', 'description', 'category', 'tags', 'usageCount', 'rating']
    } = req.body;
    
    await handleAsyncOperation(async () => {
        const query = buildSearchQuery(filters);
        
        const templates = await Template.find(query)
            .select(fields.join(' '))
            .populate('creatorId', 'name email')
            .limit(1000) // Limit exports to prevent memory issues
            .lean();
        
        let exportData;
        
        if (format === 'csv') {
            // Convert to CSV format
            const csvHeaders = fields.join(',');
            const csvRows = templates.map(template => {
                return fields.map(field => {
                    let value = template[field];
                    if (Array.isArray(value)) {
                        value = value.join(';');
                    }
                    return `"${value || ''}"`;
                }).join(',');
            });
            
            exportData = [csvHeaders, ...csvRows].join('\n');
        } else {
            // JSON format
            exportData = templates;
        }
        
        return {
            filters,
            format,
            fields,
            exportData,
            totalRecords: templates.length
        };
    }, res, 'Search results exported successfully');
};

/**
 * Get search query statistics
 */
const getSearchStats = async (req, res) => {
    await handleAsyncOperation(async () => {
        const stats = await Template.aggregate([
            {
                $group: {
                    _id: null,
                    totalTemplates: { $sum: 1 },
                    categoriesCount: { $addToSet: '$category' },
                    festivalTagsCount: { $addToSet: '$festivalTag' },
                    avgUsageCount: { $avg: '$usageCount' },
                    avgRating: { $avg: '$rating' },
                    totalUsage: { $sum: '$usageCount' }
                }
            }
        ]);
        
        const result = stats[0] || {};
        
        return {
            totalTemplates: result.totalTemplates || 0,
            uniqueCategories: (result.categoriesCount || []).filter(cat => cat).length,
            uniqueFestivalTags: (result.festivalTagsCount || []).filter(tag => tag).length,
            averageUsageCount: Math.round((result.avgUsageCount || 0) * 100) / 100,
            averageRating: Math.round((result.avgRating || 0) * 100) / 100,
            totalUsage: result.totalUsage || 0
        };
    }, res, 'Search statistics retrieved successfully');
};

/**
 * Save search query for later use
 */
const saveSearchQuery = async (req, res) => {
    const {
        userId,
        queryName,
        filters,
        description
    } = req.body;
    
    await handleAsyncOperation(async () => {
        if (!userId || !queryName || !filters) {
            throw new Error('userId, queryName, and filters are required');
        }
        
        // This would typically save to a SavedSearches collection
        // For now, we'll return a mock implementation
        const savedSearch = {
            id: new mongoose.Types.ObjectId(),
            userId,
            queryName,
            filters,
            description,
            createdAt: new Date(),
            lastUsed: new Date()
        };
        
        return {
            savedSearch,
            message: 'Search query saved successfully'
        };
    }, res, 'Search query saved successfully');
};

/**
 * Get saved search queries for user
 */
const getSavedSearches = async (req, res) => {
    const { userId } = req.params;
    
    await handleAsyncOperation(async () => {
        if (!userId) {
            throw new Error('userId is required');
        }
        
        // Mock implementation - would query SavedSearches collection
        const savedSearches = [
            {
                id: new mongoose.Types.ObjectId(),
                userId,
                queryName: 'Birthday Templates',
                filters: { category: 'birthday' },
                description: 'All birthday-related templates',
                createdAt: new Date(),
                lastUsed: new Date()
            }
        ];
        
        return {
            userId,
            savedSearches
        };
    }, res, 'Saved searches retrieved successfully');
};

/**
 * Delete saved search query
 */
const deleteSavedSearch = async (req, res) => {
    const { searchId } = req.params;
    
    await handleAsyncOperation(async () => {
        if (!searchId) {
            throw new Error('searchId is required');
        }
        
        // Mock implementation - would delete from SavedSearches collection
        return {
            searchId,
            deleted: true
        };
    }, res, 'Saved search deleted successfully');
};

/**
 * Bulk search operations
 */
const bulkSearch = async (req, res) => {
    const { searches } = req.body;
    
    await handleAsyncOperation(async () => {
        if (!Array.isArray(searches) || searches.length === 0) {
            throw new Error('searches must be a non-empty array');
        }
        
        const results = await Promise.all(
            searches.map(async (search, index) => {
                try {
                    const query = buildSearchQuery(search.filters || {});
                    const templates = await Template.find(query)
                        .select('title description category tags usageCount rating')
                        .limit(search.limit || 10)
                        .lean();
                    
                    return {
                        searchIndex: index,
                        searchName: search.name || `Search ${index + 1}`,
                        success: true,
                        results: templates,
                        count: templates.length
                    };
                } catch (error) {
                    return {
                        searchIndex: index,
                        searchName: search.name || `Search ${index + 1}`,
                        success: false,
                        error: error.message
                    };
                }
            })
        );
        
        return {
            totalSearches: searches.length,
            results
        };
    }, res, 'Bulk search completed successfully');
};

/**
 * Rebuild search index
 */
const rebuildSearchIndex = async (req, res) => {
    await handleAsyncOperation(async () => {
        // This would typically rebuild text search indexes
        // For now, we'll return a mock implementation
        
        const totalTemplates = await Template.countDocuments();
        
        return {
            message: 'Search index rebuild initiated',
            totalTemplates,
            estimatedTime: `${Math.ceil(totalTemplates / 1000)} minutes`
        };
    }, res, 'Search index rebuild initiated successfully');
};

module.exports = {
    advancedSearch,
    textSearch,
    categorySearch,
    creatorSearch,
    tagsSearch,
    similarTemplatesSearch,
    facetedSearch,
    getSearchSuggestions,
    getFilterOptions,
    trendingSearch,
    popularSearch,
    recentSearch,
    recommendedSearch,
    exportSearchResults,
    getSearchStats,
    saveSearchQuery,
    getSavedSearches,
    deleteSavedSearch,
    bulkSearch,
    rebuildSearchIndex
}; 