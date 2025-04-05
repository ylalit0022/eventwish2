package com.ds.eventwish.data.repository;

import android.content.Context;
import android.util.Log;

import com.ds.eventwish.data.local.AppDatabase;
import com.ds.eventwish.data.local.dao.EngagementDataDao;
import com.ds.eventwish.data.model.EngagementData;
import com.ds.eventwish.data.model.Template;
import com.ds.eventwish.utils.AppExecutors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Engine for generating personalized template recommendations
 * based on user engagement data and preferences
 */
public class RecommendationEngine {
    private static final String TAG = "RecommendationEngine";
    
    // Constants for recommendation generation
    private static final int MAX_RECOMMENDATIONS = 10;
    private static final int DEFAULT_CATEGORY_LIMIT = 5;
    private static final long RECENT_CUTOFF_DAYS = 30;
    
    // Scoring weights
    private static final float WEIGHT_CATEGORY_VISIT = 0.7f;
    private static final float WEIGHT_TEMPLATE_VIEW = 1.0f;
    private static final float WEIGHT_TEMPLATE_USE = 1.5f;
    private static final float WEIGHT_EXPLICIT_LIKE = 2.0f;
    private static final float WEIGHT_EXPLICIT_DISLIKE = -1.0f;
    
    // Recency factors
    private static final float RECENCY_VERY_RECENT = 1.0f;  // < 1 day
    private static final float RECENCY_RECENT = 0.8f;       // 1-7 days
    private static final float RECENCY_MEDIUM = 0.6f;       // 8-14 days
    private static final float RECENCY_OLD = 0.4f;          // 15-30 days
    private static final float RECENCY_VERY_OLD = 0.2f;     // > 30 days
    
    // Engagement source factors
    private static final float SOURCE_DIRECT = 1.0f;
    private static final float SOURCE_RECOMMENDATION = 1.2f;  // Boost recommendations that worked
    private static final float SOURCE_SEARCH = 0.9f;
    
    // Singleton instance
    private static volatile RecommendationEngine instance;
    
    // Dependencies
    private final Context context;
    private final EngagementDataDao engagementDataDao;
    private final AppExecutors executors;
    private final UserRepository userRepository;
    
    // Cache of category weights for faster recommendations
    private Map<String, Float> cachedCategoryWeights;
    private long cacheTimestamp;
    private static final long CACHE_TTL_MS = TimeUnit.MINUTES.toMillis(5);
    
    /**
     * Get singleton instance of RecommendationEngine
     * @param context Application context
     * @return RecommendationEngine instance
     */
    public static synchronized RecommendationEngine getInstance(Context context) {
        if (instance == null) {
            instance = new RecommendationEngine(context.getApplicationContext());
        }
        return instance;
    }
    
    /**
     * Private constructor
     * @param context Application context
     */
    private RecommendationEngine(Context context) {
        this.context = context.getApplicationContext();
        AppDatabase database = AppDatabase.getInstance(context);
        this.engagementDataDao = database.engagementDataDao();
        this.executors = AppExecutors.getInstance();
        this.userRepository = UserRepository.getInstance(context);
        this.cachedCategoryWeights = new HashMap<>();
        this.cacheTimestamp = 0;
        
        Log.d(TAG, "RecommendationEngine initialized");
    }
    
    /**
     * Generate personalized recommendations based on user engagement history
     * 
     * @param availableTemplates List of all available templates to recommend from
     * @param limit Maximum number of recommendations to return
     * @return List of recommended templates sorted by relevance
     */
    public List<Template> generateRecommendations(List<Template> availableTemplates, int limit) {
        if (availableTemplates == null || availableTemplates.isEmpty()) {
            Log.d(TAG, "Cannot generate recommendations: No templates available");
            return new ArrayList<>();
        }
        
        // Limit to the requested number or MAX_RECOMMENDATIONS, whichever is smaller
        int actualLimit = Math.min(limit, MAX_RECOMMENDATIONS);
        Log.d(TAG, "Generating up to " + actualLimit + " recommendations from " + 
              availableTemplates.size() + " available templates");
        
        // Get category weights
        Map<String, Float> categoryWeights = getCategoryWeights();
        Log.d(TAG, "Using category weights: " + categoryWeights);
        
        // Get recently viewed templates to avoid recommending them again
        List<String> recentlyViewedIds = getRecentlyViewedTemplateIds();
        Log.d(TAG, "Excluding " + recentlyViewedIds.size() + " recently viewed templates");
        
        // Score and rank templates
        List<ScoredTemplate> scoredTemplates = scoreTemplates(
            availableTemplates, categoryWeights, recentlyViewedIds);
        
        // Sort by score (descending)
        Collections.sort(scoredTemplates, new Comparator<ScoredTemplate>() {
            @Override
            public int compare(ScoredTemplate t1, ScoredTemplate t2) {
                return Float.compare(t2.getScore(), t1.getScore()); // Descending order
            }
        });
        
        // Apply interleaving to ensure category diversity
        List<ScoredTemplate> interleavedTemplates = interleaveByCategory(scoredTemplates);
        
        // Take top N recommendations
        List<ScoredTemplate> topRecommendations = interleavedTemplates.subList(
            0, Math.min(actualLimit, interleavedTemplates.size()));
        
        // Convert back to Template list and mark as recommended
        List<Template> result = new ArrayList<>();
        for (ScoredTemplate scored : topRecommendations) {
            Template template = scored.getTemplate();
            template.setRecommended(true);
            result.add(template);
            Log.d(TAG, "Recommending template: " + template.getTitle() + 
                  " (score: " + scored.getScore() + ")");
        }
        
        return result;
    }
    
    /**
     * Get weights for each category based on user engagement
     * @return Map of category to weight (0.0 to 1.0)
     */
    private Map<String, Float> getCategoryWeights() {
        // Check if cache is still valid
        long now = System.currentTimeMillis();
        if (!cachedCategoryWeights.isEmpty() && 
            (now - cacheTimestamp) < CACHE_TTL_MS) {
            return new HashMap<>(cachedCategoryWeights);
        }
        
        // Calculate new weights
        Map<String, Float> weights = new HashMap<>();
        
        try {
            // Get all engagement data
            List<EngagementData> engagements = engagementDataDao.getAll();
            
            if (engagements.isEmpty()) {
                Log.d(TAG, "No engagement data available for calculating weights");
                return weights;
            }
            
            // Calculate scores for each category
            Map<String, Float> categoryScores = new HashMap<>();
            float totalScore = 0;
            
            for (EngagementData engagement : engagements) {
                if (engagement.getCategory() == null) continue;
                
                // Calculate this engagement's contribution to the category score
                float score = calculateEngagementScore(engagement);
                
                // Add to category total
                float currentScore = categoryScores.getOrDefault(engagement.getCategory(), 0.0f);
                categoryScores.put(engagement.getCategory(), currentScore + score);
                totalScore += score;
            }
            
            // Normalize to get weights (0.0 to 1.0)
            if (totalScore > 0) {
                for (Map.Entry<String, Float> entry : categoryScores.entrySet()) {
                    weights.put(entry.getKey(), entry.getValue() / totalScore);
                }
            }
            
            // Update cache
            cachedCategoryWeights = new HashMap<>(weights);
            cacheTimestamp = now;
            
        } catch (Exception e) {
            Log.e(TAG, "Error calculating category weights", e);
        }
        
        return weights;
    }
    
    /**
     * Calculate the score for a single engagement record
     * @param engagement The engagement record
     * @return Calculated score
     */
    private float calculateEngagementScore(EngagementData engagement) {
        // Base score based on engagement type
        float typeWeight;
        switch (engagement.getType()) {
            case EngagementData.TYPE_CATEGORY_VISIT:
                typeWeight = WEIGHT_CATEGORY_VISIT;
                break;
            case EngagementData.TYPE_TEMPLATE_VIEW:
                typeWeight = WEIGHT_TEMPLATE_VIEW;
                break;
            case EngagementData.TYPE_TEMPLATE_USE:
                typeWeight = WEIGHT_TEMPLATE_USE;
                break;
            case EngagementData.TYPE_EXPLICIT_LIKE:
                typeWeight = WEIGHT_EXPLICIT_LIKE;
                break;
            case EngagementData.TYPE_EXPLICIT_DISLIKE:
                typeWeight = WEIGHT_EXPLICIT_DISLIKE;
                break;
            default:
                typeWeight = 1.0f;
                break;
        }
        
        // Engagement score factor (1-5 scale)
        float engagementFactor = engagement.getEngagementScore() / 3.0f; // Normalize around 3
        
        // Recency factor
        float recencyFactor = calculateRecencyFactor(engagement.getTimestamp());
        
        // Source factor
        float sourceFactor = 1.0f;
        if (engagement.getSource() != null) {
            switch (engagement.getSource()) {
                case EngagementData.SOURCE_DIRECT:
                    sourceFactor = SOURCE_DIRECT;
                    break;
                case EngagementData.SOURCE_RECOMMENDATION:
                    sourceFactor = SOURCE_RECOMMENDATION;
                    break;
                case EngagementData.SOURCE_SEARCH:
                    sourceFactor = SOURCE_SEARCH;
                    break;
                default:
                    sourceFactor = 1.0f;
                    break;
            }
        }
        
        // Combined score
        return typeWeight * engagementFactor * recencyFactor * sourceFactor;
    }
    
    /**
     * Calculate recency factor based on timestamp
     * @param timestamp Engagement timestamp
     * @return Recency factor (0.0 to 1.0)
     */
    private float calculateRecencyFactor(long timestamp) {
        long now = System.currentTimeMillis();
        long ageMs = now - timestamp;
        
        // Convert to days
        long ageDays = TimeUnit.MILLISECONDS.toDays(ageMs);
        
        if (ageDays < 1) {
            return RECENCY_VERY_RECENT;
        } else if (ageDays < 7) {
            return RECENCY_RECENT;
        } else if (ageDays < 14) {
            return RECENCY_MEDIUM;
        } else if (ageDays < 30) {
            return RECENCY_OLD;
        } else {
            return RECENCY_VERY_OLD;
        }
    }
    
    /**
     * Get list of recently viewed template IDs
     * @return List of template IDs
     */
    private List<String> getRecentlyViewedTemplateIds() {
        List<String> recentIds = new ArrayList<>();
        
        try {
            // Get most recently viewed templates
            List<EngagementDataDao.TemplateCount> mostViewed = 
                engagementDataDao.getMostViewedTemplates(5);
            
            // Extract template IDs
            for (EngagementDataDao.TemplateCount template : mostViewed) {
                if (template.template_id != null) {
                    recentIds.add(template.template_id);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting recently viewed templates", e);
        }
        
        return recentIds;
    }
    
    /**
     * Score all available templates based on user preferences
     * @param templates Available templates
     * @param categoryWeights Category preference weights
     * @param recentlyViewedIds Recently viewed template IDs to downrank
     * @return List of scored templates
     */
    private List<ScoredTemplate> scoreTemplates(
            List<Template> templates,
            Map<String, Float> categoryWeights,
            List<String> recentlyViewedIds) {
        
        List<ScoredTemplate> scoredTemplates = new ArrayList<>();
        
        for (Template template : templates) {
            // Calculate base score from category weight
            float categoryWeight = categoryWeights.getOrDefault(template.getCategory(), 0.1f);
            
            // Base score is primarily from category preference
            float score = categoryWeight;
            
            // Downrank if recently viewed (for diversity)
            if (recentlyViewedIds.contains(template.getId())) {
                score *= 0.5f;
            }
            
            // Add randomness factor (0.9-1.1) to break ties and add diversity
            float randomFactor = 0.9f + (float) (Math.random() * 0.2);
            score *= randomFactor;
            
            scoredTemplates.add(new ScoredTemplate(template, score));
        }
        
        return scoredTemplates;
    }
    
    /**
     * Interleave templates by category to ensure diversity in recommendations
     * @param scoredTemplates List of scored templates
     * @return Reordered list with category diversity
     */
    private List<ScoredTemplate> interleaveByCategory(List<ScoredTemplate> scoredTemplates) {
        if (scoredTemplates.size() <= 1) {
            return scoredTemplates;
        }
        
        // Group templates by category
        Map<String, List<ScoredTemplate>> templatesByCategory = new HashMap<>();
        
        for (ScoredTemplate scored : scoredTemplates) {
            String category = scored.getTemplate().getCategory();
            if (!templatesByCategory.containsKey(category)) {
                templatesByCategory.put(category, new ArrayList<>());
            }
            templatesByCategory.get(category).add(scored);
        }
        
        // Create interleaved result
        List<ScoredTemplate> result = new ArrayList<>();
        List<String> categories = new ArrayList<>(templatesByCategory.keySet());
        
        // Sort categories by highest scoring template in each
        Collections.sort(categories, new Comparator<String>() {
            @Override
            public int compare(String c1, String c2) {
                float maxScore1 = getMaxScore(templatesByCategory.get(c1));
                float maxScore2 = getMaxScore(templatesByCategory.get(c2));
                return Float.compare(maxScore2, maxScore1); // Descending order
            }
        });
        
        // Take templates from each category in rotation
        int currentIndex = 0;
        while (result.size() < scoredTemplates.size()) {
            // Get the next category in rotation
            String category = categories.get(currentIndex % categories.size());
            List<ScoredTemplate> categoryTemplates = templatesByCategory.get(category);
            
            // If this category still has templates, take the highest scoring one
            if (!categoryTemplates.isEmpty()) {
                ScoredTemplate highest = categoryTemplates.remove(0);
                result.add(highest);
            }
            
            // If this category is now empty, remove it from rotation
            if (categoryTemplates.isEmpty()) {
                templatesByCategory.remove(category);
                categories.remove(category);
                
                // If no more categories, we're done
                if (categories.isEmpty()) {
                    break;
                }
            } else {
                // Move to next category
                currentIndex++;
            }
        }
        
        return result;
    }
    
    /**
     * Get the maximum score from a list of scored templates
     */
    private float getMaxScore(List<ScoredTemplate> templates) {
        float maxScore = 0;
        for (ScoredTemplate template : templates) {
            if (template.getScore() > maxScore) {
                maxScore = template.getScore();
            }
        }
        return maxScore;
    }
    
    /**
     * Get personalized recommendations for the current user
     * @return List of recommended templates
     */
    public List<Template> getPersonalizedRecommendations() {
        Log.d(TAG, "Getting personalized recommendations");
        
        try {
            // Get all available templates from repository
            TemplateRepository templateRepository = TemplateRepository.getInstance();
            List<Template> allTemplates = templateRepository.getTemplatesSync();
            
            if (allTemplates == null || allTemplates.isEmpty()) {
                Log.w(TAG, "No templates available for recommendations");
                return new ArrayList<>();
            }
            
            // Generate recommendations with the available templates
            return generateRecommendations(allTemplates, MAX_RECOMMENDATIONS);
        } catch (Exception e) {
            Log.e(TAG, "Error generating personalized recommendations", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Helper class to associate templates with scores
     */
    private static class ScoredTemplate {
        private final Template template;
        private final float score;
        
        ScoredTemplate(Template template, float score) {
            this.template = template;
            this.score = score;
        }
        
        Template getTemplate() {
            return template;
        }
        
        float getScore() {
            return score;
        }
    }
} 