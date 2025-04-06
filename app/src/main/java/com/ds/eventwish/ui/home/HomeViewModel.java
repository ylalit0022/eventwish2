package com.ds.eventwish.ui.home;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import com.ds.eventwish.data.model.Template;
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
import java.util.concurrent.TimeUnit;
import java.lang.reflect.Field;

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

    private boolean forceRefreshOnReturn = false;

    // Add these at the top of the class with other fields
    private final MutableLiveData<Boolean> templatesPreloaded = new MutableLiveData<>(false);
    private boolean isFirstLoad = true;

    // Add this at the top of the class with other fields
    private boolean isPaginationInProgress = false;

    // Add these with other state fields
    private final MutableLiveData<Boolean> categoriesPreloaded = new MutableLiveData<>(false);
    private String lastSelectedCategory = null;

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

    public HomeViewModel() {
        repository = TemplateRepository.getInstance();
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

    /**
     * Load categories from the repository
     */
    public void loadCategories() {
        // If categories are already loaded, just return
        if (categoriesPreloaded.getValue() == Boolean.TRUE) {
            Log.d(TAG, "Categories already loaded, skipping API call");
            
            // Notify observers of existing categories
            repository.notifyCategoriesObservers();
            return;
        }
        
        // Check if we already have categories
        Map<String, Integer> existingCategories = repository.getCategories().getValue();
        
        if (existingCategories == null || existingCategories.isEmpty()) {
            // No categories available, load them by loading templates
            Log.d(TAG, "No categories available, loading templates to get categories");
            loadTemplates(true);
            
            // Add some default categories to ensure UI has data during initial load
            Map<String, Integer> defaultCategories = new HashMap<>();
            defaultCategories.put("Birthday", 10);
            defaultCategories.put("Wedding", 8);
            defaultCategories.put("Anniversary", 6);
            defaultCategories.put("Graduation", 5);
            defaultCategories.put("Holiday", 7);
            defaultCategories.put("Congratulations", 4);
            defaultCategories.put("Party", 9);
            
            // Update categories LiveData with defaults until API returns
            if (repository.getCategories().getValue() == null ||
                repository.getCategories().getValue().isEmpty()) {
                Log.d(TAG, "Setting default categories until API response");
                
                // We need to use reflection to access the MutableLiveData in repository
                try {
                    Field categoriesField = TemplateRepository.class.getDeclaredField("categories");
                    categoriesField.setAccessible(true);
                    MutableLiveData<Map<String, Integer>> categoriesLiveData = 
                        (MutableLiveData<Map<String, Integer>>) categoriesField.get(repository);
                    if (categoriesLiveData != null) {
                        categoriesLiveData.setValue(defaultCategories);
                        Log.d(TAG, "Default categories set successfully");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Failed to set default categories via reflection", e);
                    // Fallback - notify observers to at least trigger UI update
                    repository.notifyCategoriesObservers();
                }
            }
        } else {
            // Categories already available, just notify observers
            Log.d(TAG, "Categories already available (" + existingCategories.size() + "), notifying observers");
            
            // We can't directly call setValue on the LiveData returned by repository.getCategories()
            // Instead, we'll reload templates with the current filters to refresh categories
            // but with a flag to avoid clearing existing data
            repository.loadTemplates(false);
        }
        
        // Mark categories as loaded
        categoriesPreloaded.setValue(true);
    }

    public LiveData<Boolean> getLoading() {
        return repository.getLoading();
    }

    public LiveData<Boolean> getHasNewTemplates() {
        return hasNewTemplates;
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
        
        // Store the previous category for restoration if needed
        lastSelectedCategory = selectedCategory;
        
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
        
        // Reload templates with the new filter
        loadTemplates(true);
        
        // Mark categories as loaded
        categoriesPreloaded.setValue(true);
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
     * Load templates from the repository
     * @param clearExisting Whether to clear existing templates
     */
    public void loadTemplates(boolean clearExisting) {
        // If we're not clearing existing data and templates are already loaded, just return
        if (!clearExisting && templatesPreloaded.getValue() == Boolean.TRUE && !isFirstLoad) {
            Log.d(TAG, "Templates already loaded, skipping API call");
            return;
        }
        
        isFirstLoad = false;
        
        if (appContext == null) {
            Log.e(TAG, "Cannot load templates: app context is null");
            return;
        }
        
        Log.d(TAG, "Loading templates with category: " + 
              (selectedCategory != null ? selectedCategory : "All") + 
              ", sort: " + selectedSortOption + 
              ", time: " + selectedTimeFilter + 
              ", clearExisting: " + clearExisting);
        
        // Set category filter first
        repository.setCategory(selectedCategory);
        
        // Apply sort option
        applySortOption(selectedSortOption);
        
        // Apply time filter
        applyTimeFilter(selectedTimeFilter);
        
        // Load templates with the current filters
        repository.loadTemplates(clearExisting);
        
        // Record time of last refresh
        lastRefreshTime = System.currentTimeMillis();
        
        // Mark templates as preloaded
        templatesPreloaded.setValue(true);
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

    /**
     * Check if pagination is currently in progress
     * @return true if loading more templates, false otherwise
     */
    public boolean isPaginationInProgress() {
        return isPaginationInProgress;
    }

    /**
     * Load more templates if needed based on scroll position
     * @param lastVisibleItem Position of the last visible item
     * @param totalItemCount Total number of items in the adapter
     */
    public void loadMoreIfNeeded(int lastVisibleItem, int totalItemCount) {
        if (repository.isLoading() || !repository.hasMorePages()) {
            return;
        }
        
        if (lastVisibleItem + VISIBLE_THRESHOLD >= totalItemCount) {
            // Set pagination flag to true
            isPaginationInProgress = true;
            
            // Load more templates
            Log.d(TAG, "Loading more templates, pagination in progress");
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
            return;
        }
        
        Set<String> newIds = new HashSet<>();
        for (Template template : templates) {
            if (!viewedTemplateIds.contains(template.getId())) {
                newIds.add(template.getId());
            }
        }
        
        if (!newIds.isEmpty()) {
            hasNewTemplates.setValue(true);
            newTemplateIds.setValue(newIds);
        }
    }

    public void markTemplateAsViewed(String templateId) {
        if (templateId == null || templateId.isEmpty() || appContext == null) {
            return;
        }
        
        viewedTemplateIds.add(templateId);
        
        // Find the template's category from the current list if available
        String category = null;
        List<Template> currentTemplates = getTemplates().getValue();
        if (currentTemplates != null) {
            for (Template template : currentTemplates) {
                if (templateId.equals(template.getId())) {
                    category = template.getCategory();
                    break;
                }
            }
        }
        
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
        SharedPreferences prefs = appContext.getSharedPreferences("home_prefs", Context.MODE_PRIVATE);
        StringBuilder sb = new StringBuilder();
        for (String id : viewedTemplateIds) {
            sb.append(id).append(",");
        }
        prefs.edit().putString(PREF_VIEWED_TEMPLATES, sb.toString()).apply();
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

    /**
     * Get the selected category
     * @return The selected category, or null for all categories
     */
    public String getSelectedCategory() {
        return selectedCategory;
    }

    /**
     * Check if templates should be refreshed when returning to this fragment
     * @return true if a refresh is needed on return
     */
    public boolean shouldRefreshOnReturn() {
        // If forced refresh is set, use it once and reset
        if (forceRefreshOnReturn) {
            forceRefreshOnReturn = false;
            return true;
        }
        
        // If templates aren't preloaded, we should load them
        if (templatesPreloaded.getValue() != Boolean.TRUE) {
            return true;
        }
        
        // Only refresh if it's been more than 5 minutes since last refresh
        long currentTime = System.currentTimeMillis();
        long refreshAge = currentTime - lastRefreshTime;
        
        return refreshAge > TimeUnit.MINUTES.toMillis(5);
    }

    /**
     * Force a refresh when returning to this fragment
     */
    public void setForceRefreshOnReturn() {
        this.forceRefreshOnReturn = true;
    }

    /**
     * Clear cache and force a refresh of templates
     */
    public void clearCacheAndRefresh() {
        // Clear the cache
        repository.clearCache();
        
        // Mark templates as not preloaded
        templatesPreloaded.setValue(false);
        
        // Reset first load flag
        isFirstLoad = true;
        
        // Reset filters to default
        selectedSortOption = SortOption.TRENDING;
        selectedTimeFilter = TimeFilter.ALL_TIME;
        
        // Don't reset category selection to maintain user's context
        
        // Force refresh with cleared cache
        loadTemplates(true);
        
        // Clear new templates flag
        clearNewTemplatesFlag();
    }

    /**
     * Set the pagination in progress flag
     * @param inProgress Whether pagination is in progress
     */
    public void setPaginationInProgress(boolean inProgress) {
        this.isPaginationInProgress = inProgress;
        
        if (!inProgress) {
            Log.d(TAG, "Pagination completed");
        }
    }

    /**
     * Check if categories are already loaded
     * @return true if categories have been loaded
     */
    public boolean areCategoriesLoaded() {
        return categoriesPreloaded.getValue() != null && categoriesPreloaded.getValue();
    }

    /**
     * Force a reload of categories
     */
    public void forceReloadCategories() {
        categoriesPreloaded.setValue(false);
        loadCategories();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        repository.cancelCurrentCall();
    }
}
