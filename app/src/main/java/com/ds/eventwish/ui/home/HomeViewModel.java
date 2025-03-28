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

    // Enum for sort options
    public enum SortOption {
        TRENDING("Trending"),
        NEWEST("Newest"),
        OLDEST("Oldest"),
        MOST_USED("Most Used");
        
        private final String displayName;
        
        SortOption(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
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

    /**
     * Constructor
     */
    public HomeViewModel() {
        repository = TemplateRepository.getInstance();
        
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
    }

    public void init(Context context) {
        this.appContext = context.getApplicationContext();
        this.updateManager = TemplateUpdateManager.getInstance(context);
        
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

    public LiveData<Boolean> getLoading() {
        return repository.getLoading();
    }

    public LiveData<Boolean> getHasNewTemplates() {
        return hasNewTemplates;
    }

    /**
     * Set the selected category by ID
     * @param categoryId The category ID to filter by, or null for all categories
     */
    public void setSelectedCategory(String categoryId) {
        // Handle "all" category specially
        if (categoryId != null && categoryId.equals("all")) {
            setCategory(null);
        } else {
            // For other categories, use the original method
            setCategory(categoryId);
        }
    }

    /**
     * Set the category filter
     * @param category The category to filter by, or null for all categories
     */
    public void setCategory(String category) {
        if ((category == null && selectedCategory == null) ||
            (category != null && category.equals(selectedCategory))) {
            return; // No change
        }
        
        Log.d(TAG, "Setting category filter from " + 
              (selectedCategory != null ? selectedCategory : "All") + " to " + 
              (category != null ? category : "All"));
        
        selectedCategory = category;
        
        // Save the selected category
        if (appContext != null) {
            SharedPreferences prefs = appContext.getSharedPreferences("home_prefs", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            if (category == null) {
                editor.remove(PREF_SELECTED_CATEGORY);
            } else {
                editor.putString(PREF_SELECTED_CATEGORY, category);
            }
            editor.apply();
        }
        
        // Explicitly set category in repository
        if (category == null) {
            repository.setCategory(null); // Ensure repository filter is cleared
        } else {
            repository.setCategory(category);
        }
        
        // Reload templates with the new filter
        loadTemplates(true);
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
        
        // If forced refresh, clear the cache
        if (refresh) {
            Log.d(TAG, "Force refresh triggered for templates");
            repository.clearCache();
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
}
