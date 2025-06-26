const logger = require('../utils/logger');

/**
 * Template Scoring Service - Scores templates based on user preferences
 * Implements sophisticated ranking algorithms for personalized feeds
 */
class TemplateScoringService {
    constructor() {
        this.scoringWeights = {
            personalPreference: 0.35,  // 35% - User's personal preferences
            popularity: 0.25,          // 25% - Template popularity metrics
            freshness: 0.15,           // 15% - How recent the template is
            quality: 0.15,             // 15% - Template quality indicators
            diversity: 0.10            // 10% - Diversity bonus/penalty
        };
        
        this.popularityWeights = {
            likes: 0.3,
            shares: 0.25,
            favorites: 0.2,
            views: 0.15,
            trending: 0.1
        };
    }

    /**
     * Score templates for a specific user
     * @param {Array} templates - Array of template candidates
     * @param {Object} userProfile - User profile with preferences
     * @returns {Array} Scored and sorted templates
     */
    async scoreTemplatesForUser(templates, userProfile) {
        const startTime = Date.now();
        
        try {
            logger.info(`🎯 Scoring ${templates.length} templates for user: ${userProfile.uid}`);
            
            if (!templates || templates.length === 0) {
                return [];
            }

            // Score each template
            const scoredTemplates = [];
            for (const template of templates) {
                const score = await this.calculateTemplateScore(template, userProfile);
                scoredTemplates.push({
                    ...template,
                    personalizedScore: score.total,
                    scoreBreakdown: score.breakdown
                });
            }

            // Sort by personalized score (descending)
            scoredTemplates.sort((a, b) => b.personalizedScore - a.personalizedScore);

            logger.info(`✅ Scored ${templates.length} templates in ${Date.now() - startTime}ms`);
            return scoredTemplates;

        } catch (error) {
            logger.error('❌ Error scoring templates:', error);
            // Return templates with default scoring
            return templates.map(template => ({
                ...template,
                personalizedScore: template.weeklyTrendingScore || 0,
                scoreBreakdown: { error: true }
            }));
        }
    }

    /**
     * Calculate comprehensive score for a template
     * @param {Object} template - Template object
     * @param {Object} userProfile - User profile
     * @returns {Object} Score object with total and breakdown
     */
    async calculateTemplateScore(template, userProfile) {
        const breakdown = {
            personalPreference: 0,
            popularity: 0,
            freshness: 0,
            quality: 0,
            diversity: 0
        };

        try {
            // 1. Personal Preference Score (35%)
            breakdown.personalPreference = this.calculatePersonalPreferenceScore(template, userProfile);

            // 2. Popularity Score (25%)
            breakdown.popularity = this.calculatePopularityScore(template);

            // 3. Freshness Score (15%)
            breakdown.freshness = this.calculateFreshnessScore(template);

            // 4. Quality Score (15%)
            breakdown.quality = this.calculateQualityScore(template);

            // 5. Diversity Score (10%)
            breakdown.diversity = this.calculateDiversityScore(template, userProfile);

            // Calculate weighted total
            const total = 
                (breakdown.personalPreference * this.scoringWeights.personalPreference) +
                (breakdown.popularity * this.scoringWeights.popularity) +
                (breakdown.freshness * this.scoringWeights.freshness) +
                (breakdown.quality * this.scoringWeights.quality) +
                (breakdown.diversity * this.scoringWeights.diversity);

            return {
                total: Math.max(0, Math.min(100, total)), // Clamp between 0-100
                breakdown
            };

        } catch (error) {
            logger.error('❌ Error calculating template score:', error);
            return {
                total: 0,
                breakdown: { error: true }
            };
        }
    }

    /**
     * Calculate personal preference score based on user's interests
     */
    calculatePersonalPreferenceScore(template, userProfile) {
        let score = 0;
        let maxPossibleScore = 0;

        try {
            // Category preference (40% of personal preference)
            if (template.category && userProfile.preferences.categories) {
                const categoryScore = userProfile.preferences.categories[template.category] || 0;
                score += categoryScore * 40;
                maxPossibleScore += 40;
            }

            // Tag affinity (35% of personal preference)
            if (template.tags && template.tags.length > 0 && userProfile.preferences.tags) {
                let tagScore = 0;
                let tagCount = 0;
                
                for (const tag of template.tags) {
                    if (userProfile.preferences.tags[tag]) {
                        tagScore += userProfile.preferences.tags[tag];
                        tagCount++;
                    }
                }
                
                if (tagCount > 0) {
                    const averageTagScore = tagScore / tagCount;
                    score += averageTagScore * 35;
                }
                maxPossibleScore += 35;
            }

            // Template type preference (15% of personal preference)
            if (template.templateType && userProfile.preferences.templateTypes) {
                const typeScore = userProfile.preferences.templateTypes[template.templateType] || 0;
                score += typeScore * 15;
                maxPossibleScore += 15;
            }

            // Creator preference (10% of personal preference)
            if (template.creatorUid && userProfile.preferences.creators) {
                const creatorScore = userProfile.preferences.creators[template.creatorUid] || 0;
                score += creatorScore * 10;
                maxPossibleScore += 10;
            }

            // Normalize to 0-100 scale
            return maxPossibleScore > 0 ? (score / maxPossibleScore) * 100 : 0;

        } catch (error) {
            logger.error('❌ Error calculating personal preference score:', error);
            return 0;
        }
    }

    /**
     * Calculate popularity score based on engagement metrics
     */
    calculatePopularityScore(template) {
        try {
            let score = 0;

            // Likes score (30%)
            const likesScore = this.normalizeMetric(template.likesCount || 0, 1000); // Max expected: 1000
            score += likesScore * this.popularityWeights.likes;

            // Shares score (25%)
            const sharesScore = this.normalizeMetric(template.sharesCount || 0, 500); // Max expected: 500
            score += sharesScore * this.popularityWeights.shares;

            // Favorites score (20%)
            const favoritesScore = this.normalizeMetric(template.favoritesCount || 0, 200); // Max expected: 200
            score += favoritesScore * this.popularityWeights.favorites;

            // Views score (15%)
            const viewsScore = this.normalizeMetric(template.viewsCount || 0, 10000); // Max expected: 10000
            score += viewsScore * this.popularityWeights.views;

            // Trending score (10%)
            const trendingScore = this.normalizeMetric(template.weeklyTrendingScore || 0, 100); // Max: 100
            score += trendingScore * this.popularityWeights.trending;

            return score * 100; // Convert to 0-100 scale

        } catch (error) {
            logger.error('❌ Error calculating popularity score:', error);
            return 0;
        }
    }

    /**
     * Calculate freshness score based on creation date
     */
    calculateFreshnessScore(template) {
        try {
            if (!template.createdAt) {
                return 0;
            }

            const now = Date.now();
            const templateAge = now - new Date(template.createdAt).getTime();
            const daysSinceCreation = templateAge / (1000 * 60 * 60 * 24);

            // Freshness scoring curve
            if (daysSinceCreation <= 1) return 100;      // Last 24 hours: 100%
            if (daysSinceCreation <= 3) return 90;       // Last 3 days: 90%
            if (daysSinceCreation <= 7) return 75;       // Last week: 75%
            if (daysSinceCreation <= 14) return 60;      // Last 2 weeks: 60%
            if (daysSinceCreation <= 30) return 40;      // Last month: 40%
            if (daysSinceCreation <= 90) return 20;      // Last 3 months: 20%
            return 10; // Older than 3 months: 10%

        } catch (error) {
            logger.error('❌ Error calculating freshness score:', error);
            return 0;
        }
    }

    /**
     * Calculate quality score based on various quality indicators
     */
    calculateQualityScore(template) {
        try {
            let score = 0;
            let factors = 0;

            // Featured status (25%)
            if (template.isFeatured) {
                score += 25;
            }
            factors++;

            // Moderation status (25%)
            if (template.moderationStatus === 'approved') {
                score += 25;
            } else if (template.moderationStatus === 'pending') {
                score += 10;
            }
            factors++;

            // Engagement ratio (25%)
            const totalEngagements = (template.likesCount || 0) + 
                                   (template.sharesCount || 0) + 
                                   (template.favoritesCount || 0);
            const views = template.viewsCount || 1;
            const engagementRate = totalEngagements / views;
            
            if (engagementRate > 0.1) score += 25;      // >10% engagement
            else if (engagementRate > 0.05) score += 20; // >5% engagement
            else if (engagementRate > 0.02) score += 15; // >2% engagement
            else if (engagementRate > 0.01) score += 10; // >1% engagement
            else score += 5;                             // <1% engagement
            factors++;

            // Creator reputation (25%)
            if (template.creatorUid) {
                // This could be enhanced with actual creator reputation data
                score += 15; // Default score for having a creator
            }
            factors++;

            return factors > 0 ? score / factors : 0;

        } catch (error) {
            logger.error('❌ Error calculating quality score:', error);
            return 0;
        }
    }

    /**
     * Calculate diversity score to promote content variety
     */
    calculateDiversityScore(template, userProfile) {
        try {
            let score = 50; // Start with neutral score

            // Penalize if user has seen too much of this category recently
            if (template.category && userProfile.preferences.categories) {
                const categoryPreference = userProfile.preferences.categories[template.category] || 0;
                
                // If user has very high preference (>0.8), slightly reduce diversity score
                // to avoid echo chamber effect
                if (categoryPreference > 0.8) {
                    score -= 20;
                } else if (categoryPreference < 0.2) {
                    // Boost score for exploring new categories
                    score += 30;
                }
            }

            // Boost for exploring new template types
            if (template.templateType && userProfile.preferences.templateTypes) {
                const typePreference = userProfile.preferences.templateTypes[template.templateType] || 0;
                if (typePreference === 0) {
                    score += 20; // Boost for trying new template types
                }
            }

            // Boost for new creators
            if (template.creatorUid && userProfile.preferences.creators) {
                const creatorPreference = userProfile.preferences.creators[template.creatorUid] || 0;
                if (creatorPreference === 0) {
                    score += 15; // Boost for discovering new creators
                }
            }

            return Math.max(0, Math.min(100, score));

        } catch (error) {
            logger.error('❌ Error calculating diversity score:', error);
            return 50; // Neutral score on error
        }
    }

    /**
     * Normalize a metric to 0-1 scale using logarithmic scaling
     */
    normalizeMetric(value, maxExpected) {
        if (value <= 0) return 0;
        if (value >= maxExpected) return 1;
        
        // Use logarithmic scaling for better distribution
        const logValue = Math.log(value + 1);
        const logMax = Math.log(maxExpected + 1);
        return logValue / logMax;
    }

    /**
     * Apply temporal boost for seasonal/trending content
     */
    applyTemporalBoost(templates, currentDate = new Date()) {
        try {
            const month = currentDate.getMonth() + 1; // 1-12
            const day = currentDate.getDate();
            
            for (const template of templates) {
                let boost = 0;
                
                // Festival/seasonal boost
                if (template.tags) {
                    for (const tag of template.tags) {
                        boost += this.getSeasonalBoost(tag, month, day);
                    }
                }
                
                // Apply boost
                if (boost > 0) {
                    template.personalizedScore = Math.min(100, template.personalizedScore + boost);
                    template.scoreBreakdown.temporalBoost = boost;
                }
            }
            
            return templates;
            
        } catch (error) {
            logger.error('❌ Error applying temporal boost:', error);
            return templates;
        }
    }

    /**
     * Get seasonal boost for specific tags
     */
    getSeasonalBoost(tag, month, day) {
        const seasonalTags = {
            // Festivals
            'diwali': { months: [10, 11], boost: 15 },
            'holi': { months: [3], boost: 15 },
            'christmas': { months: [12], boost: 15 },
            'new year': { months: [1, 12], boost: 15 },
            'valentine': { months: [2], boost: 10 },
            'birthday': { months: [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12], boost: 5 }, // Always relevant
            
            // Seasons
            'summer': { months: [4, 5, 6], boost: 10 },
            'winter': { months: [11, 12, 1, 2], boost: 10 },
            'monsoon': { months: [6, 7, 8, 9], boost: 10 },
            
            // Days of week (would need day of week calculation)
            'weekend': { boost: 5 },
            'monday': { boost: 3 }
        };
        
        const tagLower = tag.toLowerCase();
        if (seasonalTags[tagLower]) {
            const tagConfig = seasonalTags[tagLower];
            if (!tagConfig.months || tagConfig.months.includes(month)) {
                return tagConfig.boost;
            }
        }
        
        return 0;
    }

    /**
     * Apply business rules and constraints
     */
    applyBusinessRules(templates, userProfile) {
        try {
            for (const template of templates) {
                let adjustment = 0;
                
                // Premium content boost for premium users
                if (template.isPremium && userProfile.isPremium) {
                    adjustment += 10;
                } else if (template.isPremium && !userProfile.isPremium) {
                    adjustment -= 20; // Penalize premium content for free users
                }
                
                // Language preference
                if (template.language && userProfile.demographics.language) {
                    if (template.language === userProfile.demographics.language) {
                        adjustment += 5;
                    } else {
                        adjustment -= 10;
                    }
                }
                
                // Apply adjustment
                if (adjustment !== 0) {
                    template.personalizedScore = Math.max(0, Math.min(100, 
                        template.personalizedScore + adjustment));
                    template.scoreBreakdown.businessRules = adjustment;
                }
            }
            
            return templates;
            
        } catch (error) {
            logger.error('❌ Error applying business rules:', error);
            return templates;
        }
    }

    /**
     * Get scoring statistics for monitoring
     */
    getScoringStats(templates) {
        if (!templates || templates.length === 0) {
            return null;
        }
        
        const scores = templates.map(t => t.personalizedScore || 0);
        
        return {
            count: templates.length,
            averageScore: scores.reduce((a, b) => a + b, 0) / scores.length,
            minScore: Math.min(...scores),
            maxScore: Math.max(...scores),
            scoreDistribution: {
                high: scores.filter(s => s >= 80).length,
                medium: scores.filter(s => s >= 50 && s < 80).length,
                low: scores.filter(s => s < 50).length
            }
        };
    }

    /**
     * Update scoring weights (for A/B testing)
     */
    updateScoringWeights(newWeights) {
        try {
            // Validate weights sum to 1.0
            const totalWeight = Object.values(newWeights).reduce((a, b) => a + b, 0);
            if (Math.abs(totalWeight - 1.0) > 0.01) {
                throw new Error('Scoring weights must sum to 1.0');
            }
            
            this.scoringWeights = { ...this.scoringWeights, ...newWeights };
            logger.info('📊 Updated scoring weights:', this.scoringWeights);
            
        } catch (error) {
            logger.error('❌ Error updating scoring weights:', error);
        }
    }

    /**
     * Get current scoring configuration
     */
    getScoringConfig() {
        return {
            scoringWeights: this.scoringWeights,
            popularityWeights: this.popularityWeights
        };
    }
}

module.exports = new TemplateScoringService(); 