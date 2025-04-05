package com.ds.eventwish.ui.home;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import com.ds.eventwish.data.model.Template;
import com.ds.eventwish.data.model.Resource;
import com.ds.eventwish.data.repository.TemplateRepository;
import com.ds.eventwish.utils.TemplateUpdateManager;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.HashMap;
import android.app.Application;
import androidx.annotation.NonNull;
import com.ds.eventwish.data.repository.UserRepository;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.ds.eventwish.EventWishApplication;
import com.ds.eventwish.data.repository.RecommendationEngine;
import com.ds.eventwish.utils.AppExecutors;
import com.ds.eventwish.utils.LogUtils;
import com.ds.eventwish.BuildConfig;

public class HomeViewModel extends ViewModel {
    private static final String TAG = "HomeViewModel";
    private final TemplateRepository repository;
    private final MutableLiveData<Boolean> hasNewTemplates = new MutableLiveData<>(false);
    private TemplateUpdateManager updateManager;
    private final MutableLiveData<Set<String>> newTemplateIds = new MutableLiveData<>(new HashSet<>());
    private final Set<String> viewedTemplateIds = new HashSet<>();
    private static final String PREF_VIEWED_TEMPLATES = "viewed_template_ids";
    private static final String PREF_LAST_CHECK_TIME = "last_check_time";
    private static final String PREF_SELECTED_CATEGORY = "selected_category";
    private static final String PREF_SELECTED_SORT = "selected_sort";
    private static final String PREF_SELECTED_TIME_FILTER = "selected_time_filter";
    
    // Filter parameters
    private String selectedCategory = null;
    private SortOption selectedSortOption = SortOption.TRENDING;
    private TimeFilter selectedTimeFilter = TimeFilter.ALL_TIME;
    
    // Store the last visible position
    private int lastVisiblePosition = 0;

    // Add a field to track time of last refresh
    private long lastRefreshTime = 0;
    private static final long REFRESH_THRESHOLD = 60000; // 1 minute

    private Context appContext;

    // Add this constant
    private static final int VISIBLE_THRESHOLD = 5;
    
    // Add stale data state
    private final MutableLiveData<Boolean> staleData = new MutableLiveData<>(false);

    // Add a MutableLiveData for Category objects
    private final MutableLiveData<List<com.ds.eventwish.ui.home.Category>> categoryObjects = new MutableLiveData<>(new ArrayList<>());
    
    // Track if this is the first load (vs. pagination)
    private boolean isFirstLoad = true;
    
    // Flag to check if more pages are available for pagination
    private boolean hasMoreData = true;

    // Enum for sort options
    public enum SortOption {
        TRENDING("trending"),
        NEWEST("newest"),
        OLDEST("oldest"),
        MOST_USED("most_used"),           // Keep this for backward compatibility
        POPULAR("popular"),
        RECOMMENDED("recommended");
        
        private final String value;
        
        SortOption(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return value;
        }
        
        // For backward compatibility
        public String getDisplayName() {
            return value;
        }
    }
    
    // Enum for time filters
    public enum TimeFilter {
        TODAY("Today"),
        THIS_WEEK("This Week"),
        THIS_MONTH("This Month"),
        ALL_TIME("All Time");
        
        private final String displayName;
        
        TimeFilter(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }

    // Observer reference to avoid creating multiple observers
    private androidx.lifecycle.Observer<Map<String, Integer>> categoriesObserver;

    private final MutableLiveData<List<Template>> recommendedTemplates = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoadingRecommendations = new MutableLiveData<>(false);
    private final MutableLiveData<String> recommendationsError = new MutableLiveData<>();
    private UserRepository userRepository;
    private RecommendationEngine recommendationEngine;

    // Add LiveData for pagination status
    private final MutableLiveData<Boolean> paginationSuccess = new MutableLiveData<>();
    
    // Add tracking for pagination attempts
    private int paginationFailureCount = 0;
    private static final int MAX_PAGINATION_FAILURES = 3;

    /**
     * Variables needed for pagination
     */
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private boolean hasMore = true;
    private int currentPage = 1;
    private final TemplateRepository templateRepository;
    private final MutableLiveData<List<Template>> templates = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> error = new MutableLiveData<>();

    // Add LiveData to track recommended template IDs
    private final MutableLiveData<Set<String>> recommendedTemplateIds = new MutableLiveData<>(new HashSet<>());

    /**
     * Constructor
     */
    public HomeViewModel() {
        repository = TemplateRepository.getInstance();
        
        // Initialize UserRepository
        userRepository = UserRepository.getInstance(EventWishApplication.getInstance());
        
        // Setup the categories observer once
        categoriesObserver = categoriesMap -> {
            if (categoriesMap != null && !categoriesMap.isEmpty()) {
                Log.d(TAG, "Categories updated from repository, updating category objects");
                updateCategoryObjects(categoriesMap);
            }
        };
        
        // Observe categories
        repository.getCategories().observeForever(categoriesObserver);
        
        // Load initial data
        loadTemplates(true);

        templateRepository = repository;
    }

    public void init(Context context) {
        this.appContext = context.getApplicationContext();
        this.updateManager = TemplateUpdateManager.getInstance(context);
        
        // Initialize the RecommendationEngine
        this.recommendationEngine = RecommendationEngine.getInstance(context);
        
        // Load saved preferences
        SharedPreferences prefs = context.getSharedPreferences("home_prefs", Context.MODE_PRIVATE);
        selectedCategory = prefs.getString(PREF_SELECTED_CATEGORY, null);
        String sortOption = prefs.getString(PREF_SELECTED_SORT, SortOption.TRENDING.name());
        String timeFilter = prefs.getString(PREF_SELECTED_TIME_FILTER, TimeFilter.ALL_TIME.name());
        
        try {
            selectedSortOption = SortOption.valueOf(sortOption);
        } catch (IllegalArgumentException e) {
            selectedSortOption = SortOption.TRENDING;
        }
        
        try {
            selectedTimeFilter = TimeFilter.valueOf(timeFilter);
        } catch (IllegalArgumentException e) {
            selectedTimeFilter = TimeFilter.ALL_TIME;
        }
        
        // Load viewed template IDs
        String viewedIds = prefs.getString(PREF_VIEWED_TEMPLATES, "");
        if (!viewedIds.isEmpty()) {
            String[] ids = viewedIds.split(",");
            for (String id : ids) {
                if (!id.isEmpty()) {
                    viewedTemplateIds.add(id);
                }
            }
        }
        
        // Log the initial state
        Log.d(TAG, "Initializing HomeViewModel with category: " + 
              (selectedCategory != null ? selectedCategory : "All"));
        
        // Load templates with current filters
        loadTemplates(false);
        
        // Explicitly load categories to ensure they're available
        loadCategories();
    }

    public LiveData<List<Template>> getTemplates() {
        return repository.getTemplates();
    }

    public LiveData<String> getError() {
        return repository.getError();
    }

    public LiveData<Map<String, Integer>> getCategories() {
        return repository.getCategories();
    }

    // New method for getting Category objects
    public LiveData<List<com.ds.eventwish.ui.home.Category>> getCategoryObjects() {
        return categoryObjects;
    }

    /**
     * Convert category map to category objects and update the LiveData
     */
    private void updateCategoryObjects(Map<String, Integer> categoriesMap) {
        Log.d(TAG, "Updating category objects with " + categoriesMap.size() + " categories");
        
        try {
            List<com.ds.eventwish.ui.home.Category> categories = new ArrayList<>();
            
            // Get CategoryIconRepository instance
            com.ds.eventwish.data.repository.CategoryIconRepository iconRepository = 
                com.ds.eventwish.data.repository.CategoryIconRepository.getInstance();
            
            // Add "All" category first with a special icon
            com.ds.eventwish.data.model.CategoryIcon allIcon = 
                iconRepository.getCategoryIconByCategory("all");
            
            String allIconUrl = (allIcon != null && allIcon.getCategoryIcon() != null) ? 
                allIcon.getCategoryIcon() : "";
                
            categories.add(new com.ds.eventwish.ui.home.Category(
                null,
                "All",
                allIconUrl
            ));
            
            Log.d(TAG, "Added 'All' category with icon URL: " + allIconUrl);
            
            // Add all other categories
            for (Map.Entry<String, Integer> entry : categoriesMap.entrySet()) {
                String categoryName = entry.getKey();
                Integer count = entry.getValue();
                
                // Skip empty categories
                if (count == null || count <= 0) {
                    Log.d(TAG, "Skipping empty category: " + categoryName);
                    continue;
                }
                
                // Get icon from repository
                com.ds.eventwish.data.model.CategoryIcon icon = 
                    iconRepository.getCategoryIconByCategory(categoryName);
                
                String iconUrl = "";
                if (icon != null && icon.getCategoryIcon() != null) {
                    iconUrl = icon.getCategoryIcon();
                    Log.d(TAG, "Found icon for category " + categoryName + ": " + iconUrl);
                } else {
                    Log.d(TAG, "No icon found for category: " + categoryName + ", using default");
                }
                
                Log.d(TAG, "Adding category: " + categoryName + " with " + count + " templates");
                
                // Create category object and add to list
                categories.add(new com.ds.eventwish.ui.home.Category(
                    categoryName.toLowerCase(),
                    categoryName,
                    iconUrl
                ));
            }
            
            Log.d(TAG, "Setting " + categories.size() + " category objects to LiveData");
            categoryObjects.postValue(categories);
        } catch (Exception e) {
            Log.e(TAG, "Error updating category objects", e);
        }
    }

    /**
     * Load categories from the repository
     */
    public void loadCategories() {
        Log.d(TAG, "loadCategories called");
        
        // Use a safe approach to handle categories
        try {
            Map<String, Integer> categoriesMap = repository.getCategories().getValue();
            
            if (categoriesMap != null && !categoriesMap.isEmpty()) {
                Log.d(TAG, "Categories loaded from repository: " + categoriesMap.size());
                updateCategoryObjects(categoriesMap);
            } else {
                Log.d(TAG, "No categories available from repository, fetching...");
                // Load categories by loading templates
                loadTemplates(true);
                
                // Check if we need to observe temporarily
                if (categoriesObserver == null) {
                    Log.e(TAG, "Categories observer is null, this shouldn't happen");
                } else {
                    Log.d(TAG, "Categories observer is already set up");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading categories", e);
        }
    }

    /**
     * Get loading state as LiveData
     * @return LiveData with loading state
     */
    public LiveData<Boolean> getLoading() {
        return repository.getLoading();
    }

    public LiveData<Boolean> getHasNewTemplates() {
        return hasNewTemplates;
    }

    /**
     * Set the selected category
     * @param category The category to filter by, or null for all categories
     */
    public void setSelectedCategory(String category) {
        if ((selectedCategory == null && category == null) ||
                (selectedCategory != null && selectedCategory.equals(category))) {
            Log.d(TAG, "Category unchanged, skipping: " + category);
            return;
        }
        
        Log.d(TAG, "Setting category: " + category);
        selectedCategory = category;
        
        // Reset flags for new category selection
        isFirstLoad = true;
        hasMoreData = true;
        
        try {
            if (appContext != null) {
                // Save the selected category
                SharedPreferences prefs = appContext.getSharedPreferences("home_prefs", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString(PREF_SELECTED_CATEGORY, category);
                editor.apply();
            }
            
            if (repository != null) {
                // Post a small delay to avoid UI thread overload with rapid clicks
                AppExecutors.getInstance().mainThread().execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Log.d(TAG, "Loading templates for category: " + category);
                            // Set loading true for immediate UI feedback
                            repository.setLoading(true);
                            // Reset categories states
                            repository.setCategory(category);
                            // Reset page counter
                            repository.setCurrentPage(1);
                            // Load first page
                            repository.loadTemplates(true);
                        } catch (Exception e) {
                            Log.e(TAG, "Error loading templates for category: " + e.getMessage(), e);
                            // Set loading false to cancel loading indicator
                            repository.setLoading(false);
                            // Set error message for user
                            repository.clearError(); // First clear any previous errors
                            repository.setErrorMessage("Error changing category: " + e.getMessage());
                        }
                    }
                });
            } else {
                Log.e(TAG, "Repository is null in setSelectedCategory");
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception in setSelectedCategory: " + e.getMessage(), e);
            
            // Ensure loading indicator is hidden in case of error
            if (repository != null) {
                repository.setLoading(false);
                repository.setErrorMessage("Error: " + e.getMessage());
            }
        }
    }

    /**
     * Clear templates list to prepare for new data
     */
    private void clearTemplates() {
        // We should use the repository's clearTemplates method instead
        if (repository != null) {
            repository.clearTemplates();
            Log.d(TAG, "Cleared templates list");
        }
    }

    /**
     * Set the sort option
     * @param sortOption The sort option to use
     */
    public void setSortOption(SortOption sortOption) {
        if (sortOption == selectedSortOption) {
            return; // No change
        }
        
        selectedSortOption = sortOption;
        
        // Save the selected sort option
        if (appContext != null) {
            SharedPreferences prefs = appContext.getSharedPreferences("home_prefs", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(PREF_SELECTED_SORT, sortOption.name());
            editor.apply();
        }
        
        // Reload templates with the new filter
        loadTemplates(true);
    }

    /**
     * Set the time filter
     * @param timeFilter The time filter to use
     */
    public void setTimeFilter(TimeFilter timeFilter) {
        if (timeFilter == selectedTimeFilter) {
            return; // No change
        }
        
        selectedTimeFilter = timeFilter;
        
        // Save the selected time filter
        if (appContext != null) {
            SharedPreferences prefs = appContext.getSharedPreferences("home_prefs", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(PREF_SELECTED_TIME_FILTER, timeFilter.name());
            editor.apply();
        }
        
        // Reload templates with the new filter
        loadTemplates(true);
    }

    /**
     * Load templates with the current filters
     * @param refresh Whether to force a refresh from the server
     */
    public void loadTemplates(boolean refresh) {
        Log.d(TAG, "loadTemplates called with refresh=" + refresh);
        
        // If forced refresh, clear the cache and reset pagination
        if (refresh) {
            Log.d(TAG, "Force refresh triggered for templates");
            repository.clearCache();
            repository.setCurrentPage(1);
            isFirstLoad = true;
            hasMoreData = true;
        }
        
        // Remember when we last refreshed
        lastRefreshTime = System.currentTimeMillis();
        
        // Set the category filter if needed
        if (selectedCategory != null) {
            Log.d(TAG, "Applying category filter: " + selectedCategory);
            repository.setCategory(selectedCategory);
        } else {
            Log.d(TAG, "No category filter applied");
            repository.setCategory(null);
        }
        
        // Apply sort option
        Log.d(TAG, "Applying sort option: " + selectedSortOption.name());
        applySortOption(selectedSortOption);
        
        // Apply time filter
        Log.d(TAG, "Applying time filter: " + selectedTimeFilter.name());
        applyTimeFilter(selectedTimeFilter);
        
        // Fetch templates
        Log.d(TAG, "Loading templates from repository");
        repository.loadTemplates(refresh);
        
        // After loading initial templates, set first load to false
        isFirstLoad = false;
    }

    /**
     * Apply the sort option to the repository
     * @param sortOption The sort option to apply
     */
    private void applySortOption(SortOption sortOption) {
        // Implement this method based on your TemplateRepository implementation
        // For example, you might set a sort parameter in the repository
        // repository.setSortOption(sortOption);
    }

    /**
     * Apply the time filter to the repository
     * @param timeFilter The time filter to apply
     */
    private void applyTimeFilter(TimeFilter timeFilter) {
        // Implement this method based on your TemplateRepository implementation
        // For example, you might set a time filter parameter in the repository
        // repository.setTimeFilter(timeFilter);
    }

    public void loadMoreIfNeeded(int lastVisibleItem, int totalItemCount) {
        if (repository.isLoading() || !repository.hasMorePages()) {
            return;
        }
        
        if (lastVisibleItem + VISIBLE_THRESHOLD >= totalItemCount) {
            // Load more templates
            // If loadMoreTemplates() doesn't exist, use loadTemplates(false) instead
            repository.loadTemplates(false);
        }
    }

    /**
     * Get the current category filter
     * @return The selected category, or null if no category is selected
     */
    public String getCurrentCategory() {
        return selectedCategory;
    }

    /**
     * Get the current sort option
     * @return The selected sort option
     */
    public SortOption getCurrentSortOption() {
        return selectedSortOption;
    }

    /**
     * Get the current time filter
     * @return The selected time filter
     */
    public TimeFilter getCurrentTimeFilter() {
        return selectedTimeFilter;
    }

    /**
     * Get all available sort options
     * @return Array of all sort options
     */
    public SortOption[] getSortOptions() {
        return SortOption.values();
    }

    /**
     * Get all available time filters
     * @return Array of all time filters
     */
    public TimeFilter[] getTimeFilters() {
        return TimeFilter.values();
    }

    public void checkForNewTemplates(List<Template> templates) {
        if (templates == null || templates.isEmpty() || appContext == null) {
            Log.w(TAG, "Cannot check for new templates: templates list is empty or context is null");
            return;
        }
        
        // Get the last check time to determine what's new
        SharedPreferences prefs = appContext.getSharedPreferences("home_prefs", Context.MODE_PRIVATE);
        long lastCheckTime = prefs.getLong(PREF_LAST_CHECK_TIME, 0);
        
        // If it's the first time we're checking, consider everything as viewed
        if (lastCheckTime == 0) {
            Log.d(TAG, "First time checking for new templates, marking all as viewed");
            lastCheckTime = System.currentTimeMillis();
            prefs.edit().putLong(PREF_LAST_CHECK_TIME, lastCheckTime).apply();
            
            // Update the viewed template IDs
            for (Template template : templates) {
                viewedTemplateIds.add(template.getId());
            }
            
            // Save the viewed template IDs
            saveViewedTemplateIds();
            return;
        }
        
        Set<String> newIds = new HashSet<>();
        for (Template template : templates) {
            // A template is new if it's not in viewed templates
            if (!viewedTemplateIds.contains(template.getId())) {
                Log.d(TAG, "Found new template: " + template.getId() + " - " + template.getTitle());
                newIds.add(template.getId());
            }
        }
        
        if (!newIds.isEmpty()) {
            Log.d(TAG, "Found " + newIds.size() + " new templates");
            hasNewTemplates.setValue(true);
            newTemplateIds.setValue(newIds);
        } else {
            Log.d(TAG, "No new templates found");
            hasNewTemplates.setValue(false);
            newTemplateIds.setValue(new HashSet<>());
        }
    }

    private void saveViewedTemplateIds() {
        if (appContext == null) {
            Log.e(TAG, "Cannot save viewed template IDs: context is null");
            return;
        }
        
        SharedPreferences prefs = appContext.getSharedPreferences("home_prefs", Context.MODE_PRIVATE);
        StringBuilder sb = new StringBuilder();
        for (String id : viewedTemplateIds) {
            sb.append(id).append(",");
        }
        prefs.edit().putString(PREF_VIEWED_TEMPLATES, sb.toString()).apply();
        Log.d(TAG, "Saved " + viewedTemplateIds.size() + " viewed template IDs");
    }

    public LiveData<Set<String>> getNewTemplateIds() {
        return newTemplateIds;
    }

    public void clearNewTemplatesFlag() {
        hasNewTemplates.setValue(false);
        
        // Save the last check time
        if (appContext != null) {
            SharedPreferences prefs = appContext.getSharedPreferences("home_prefs", Context.MODE_PRIVATE);
            prefs.edit().putLong(PREF_LAST_CHECK_TIME, System.currentTimeMillis()).apply();
        }
    }

    public void saveScrollPosition(int position) {
        this.lastVisiblePosition = position;
    }

    public int getLastVisiblePosition() {
        return lastVisiblePosition;
    }

    public void saveCurrentPage(int page) {
        if (repository instanceof TemplateRepository) {
            ((TemplateRepository) repository).setCurrentPage(page);
        }
    }
    
    public int getCurrentPage() {
        if (repository instanceof TemplateRepository) {
            return ((TemplateRepository) repository).getCurrentPage();
        }
        return 0;
    }

    public boolean shouldRefreshOnReturn() {
        long currentTime = System.currentTimeMillis();
        return (currentTime - lastRefreshTime) > REFRESH_THRESHOLD;
    }

    public String getSelectedCategory() {
        return selectedCategory;
    }

    /**
     * Clear the template cache and load fresh data
     */
    public void clearCacheAndRefresh() {
        Log.d(TAG, "Clearing cache and refreshing templates");
        repository.clearCache();
        
        // Reset stale data state
        setStaleData(false);
        
        // Reset filters to default
        selectedSortOption = SortOption.TRENDING;
        selectedTimeFilter = TimeFilter.ALL_TIME;
        
        // Don't reset category selection to maintain user's context
        
        // Load fresh data
        loadTemplates(true);
        
        // Clear new templates flag
        clearNewTemplatesFlag();
    }

    /**
     * Get the stale data state as LiveData
     * @return LiveData with stale data state
     */
    public LiveData<Boolean> getStaleData() {
        return staleData;
    }

    /**
     * Set the stale data state
     * @param isStale Whether the data is stale
     */
    public void setStaleData(boolean isStale) {
        Log.d(TAG, "Setting stale data state: " + isStale);
        staleData.setValue(isStale);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        repository.cancelCurrentCall();
        
        // Remove the observer when ViewModel is cleared
        if (categoriesObserver != null) {
            repository.getCategories().removeObserver(categoriesObserver);
        }
    }

    // Add a method to set the selected template for navigation
    public void setSelectedTemplate(Template template) {
        // This method is used for navigating to the template customize screen
        // No state needs to be maintained in this ViewModel
        // Navigation is handled in the Fragment
    }

    public void markTemplateAsViewed(String templateId) {
        if (templateId == null || templateId.isEmpty() || appContext == null) {
            Log.w(TAG, "Cannot mark template as viewed: invalid ID or null context");
            return;
        }
        
        Log.d(TAG, "Marking template as viewed: " + templateId);
        viewedTemplateIds.add(templateId);
        
        // Update the new template IDs
        Set<String> currentNewIds = newTemplateIds.getValue();
        if (currentNewIds != null && currentNewIds.contains(templateId)) {
            currentNewIds.remove(templateId);
            newTemplateIds.setValue(currentNewIds);
            
            // If all new templates have been viewed, clear the flag
            if (currentNewIds.isEmpty()) {
                hasNewTemplates.setValue(false);
            }
        }
        
        // Save the viewed template IDs
        saveViewedTemplateIds();
    }

    /**
     * Check if the ViewModel has any templates loaded
     * @return true if templates are loaded, false otherwise
     */
    public boolean hasTemplates() {
        List<Template> templates = getTemplates().getValue();
        return templates != null && !templates.isEmpty();
    }
    
    /**
     * Check if a refresh is needed based on time threshold
     * @return true if refresh is needed, false otherwise
     */
    public boolean isRefreshNeeded() {
        long currentTime = System.currentTimeMillis();
        return (currentTime - lastRefreshTime) > REFRESH_THRESHOLD;
    }
    
    /**
     * Get whether the repository needs to be refreshed
     * @return LiveData with refresh needed state
     */
    public LiveData<Boolean> getRefreshNeeded() {
        // Assuming the repository has a getRefreshNeeded method
        // If not, you may need to implement this differently
        return new MutableLiveData<>(isRefreshNeeded());
    }
    
    /**
     * Refresh the UI after a feature change
     * This is called when a premium feature is unlocked
     */
    public void refreshForFeatureChange() {
        Log.d(TAG, "Refreshing UI after feature change");
        // Reload templates with current filters
        loadTemplates(true);
        
        // Refresh categories if needed
        loadCategories();
    }

    /**
     * Refresh templates from the repository
     */
    public void refreshTemplates() {
        Log.d(TAG, "Refreshing templates");
        loadTemplates(true);
    }

    /**
     * Reset the selected category to default (null)
     */
    public void resetSelectedCategory() {
        Log.d(TAG, "Resetting selected category");
        selectedCategory = null;
        if (appContext != null) {
            SharedPreferences prefs = appContext.getSharedPreferences("home_prefs", Context.MODE_PRIVATE);
            prefs.edit().remove(PREF_SELECTED_CATEGORY).apply();
        }
    }

    /**
     * Get personalized recommendations
     */
    public void getPersonalizedRecommendations() {
        if (recommendationEngine == null) {
            Log.e(TAG, "RecommendationEngine not initialized");
            recommendationsError.setValue("Recommendation engine not initialized");
            return;
        }
        
        isLoadingRecommendations.setValue(true);
        
        AppExecutors.getInstance().networkIO().execute(() -> {
            try {
                Log.d(TAG, "Fetching personalized recommendations");
                List<Template> recommendations = recommendationEngine.getPersonalizedRecommendations();
                
                recommendedTemplates.postValue(recommendations);
                
                // Extract and store template IDs for recommendations
                if (recommendations != null && !recommendations.isEmpty()) {
                    Set<String> ids = new HashSet<>();
                    for (Template template : recommendations) {
                        ids.add(template.getId());
                    }
                    recommendedTemplateIds.postValue(ids);
                    Log.d(TAG, "Extracted " + ids.size() + " recommended template IDs");
                }
                
                isLoadingRecommendations.postValue(false);
            } catch (Exception e) {
                Log.e(TAG, "Error getting recommendations", e);
                recommendationsError.postValue("Error fetching recommendations: " + e.getMessage());
                isLoadingRecommendations.postValue(false);
            }
        });
    }
    
    /**
     * Get recommended templates LiveData
     */
    public LiveData<List<Template>> getRecommendedTemplates() {
        return recommendedTemplates;
    }
    
    /**
     * Get recommendations loading state
     */
    public LiveData<Boolean> isRecommendationsLoading() {
        return isLoadingRecommendations;
    }
    
    /**
     * Get recommendations error state
     */
    public LiveData<String> getRecommendationsError() {
        return recommendationsError;
    }

    /**
     * Handle template click event, tracking the template view
     * @param template The template that was clicked
     */
    public void onTemplateClick(Template template) {
        if (template == null) {
            Log.w(TAG, "Cannot track template click: template is null");
            return;
        }
        
        String templateId = template.getId();
        String category = template.getCategory();
        
        if (templateId == null || category == null) {
            Log.w(TAG, "Cannot track template click: templateId or category is null");
            return;
        }
        
        Log.d(TAG, "Template clicked: " + templateId + " - " + template.getTitle() + 
              " (recommended: " + template.isRecommended() + ")");
            
        // Mark the template as viewed to remove NEW badge if needed
        markTemplateAsViewed(templateId);
    }

    /**
     * Check if recommendations should be refreshed when returning to HomeFragment
     * This is useful after a user has clicked on a template or visited another fragment
     * @return true if recommendations should be refreshed
     */
    public boolean shouldRefreshRecommendations() {
        // For now, use the same logic as general refresh
        // In the future, this could have a separate threshold specific to recommendations
        return isRefreshNeeded();
    }

    /**
     * Load more templates with better error handling and LiveData response
     * Use this method for pagination to avoid resetting the list
     */
    public void loadMoreTemplates() {
        Log.d(TAG, "loadMoreTemplates called");
        
        if (!hasMoreData || repository.isLoading()) {
            Log.d(TAG, "Skipping loadMoreTemplates - hasMoreData=" + hasMoreData + 
                  ", isLoading=" + repository.isLoading());
            return;
        }
        
        // This is not a first load
        isFirstLoad = false;
        
        // Get the current page
        int currentPage = repository.getCurrentPage();
        
        // Increment page and load
        repository.setCurrentPage(currentPage + 1);
        repository.loadTemplates(false);
        
        // Update pagination success status for UI feedback
        paginationSuccess.setValue(true);
        
        Log.d(TAG, "Loading more templates - page " + (currentPage + 1));
    }
    
    /**
     * Check if this is the first load of templates
     * @return true if this is the first load, false if it's pagination
     */
    public boolean isFirstLoad() {
        return isFirstLoad;
    }
    
    /**
     * Check if there are more pages available for loading
     * @return true if more pages are available for loading
     */
    public boolean hasMorePages() {
        return hasMoreData && repository.hasMorePages();
    }

    /**
     * Clear error message
     */
    public void clearError() {
        repository.clearError();
    }

    /**
     * Set the list of recommended template IDs
     * @param ids Set of template IDs that are recommended
     */
    public void setRecommendedTemplateIds(Set<String> ids) {
        if (ids == null) {
            recommendedTemplateIds.setValue(new HashSet<>());
        } else {
            recommendedTemplateIds.setValue(new HashSet<>(ids));
        }
        Log.d(TAG, "Set recommended template IDs: " + (ids != null ? ids.size() : 0));
    }
    
    /**
     * Get the LiveData for recommended template IDs
     * @return LiveData containing a set of recommended template IDs
     */
    public LiveData<Set<String>> getRecommendedTemplateIds() {
        return recommendedTemplateIds;
    }
}
