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
        // Check if we already have categories
        Map<String, Integer> existingCategories = repository.getCategories().getValue();
        
        if (existingCategories == null || existingCategories.isEmpty()) {
            // No categories available, load them by loading templates
            Log.d(TAG, "No categories available, loading templates to get categories");
            loadTemplates(true);
        } else {
            // Categories already available, just notify observers
            Log.d(TAG, "Categories already available (" + existingCategories.size() + "), notifying observers");
            
            // We can't directly call setValue on the LiveData returned by repository.getCategories()
            // Instead, we'll reload templates with the current filters to refresh categories
            // but with a flag to avoid clearing existing data
            repository.loadTemplates(false);
        }
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
        // Record refresh time
        if (refresh) {
            lastRefreshTime = System.currentTimeMillis();
        }
        
        // Apply filters
        // Since the repository doesn't support Map<String, String> filters yet,
        // we'll apply the filters one by one
        
        // Apply category filter
        repository.setCategory(selectedCategory);
        
        // Apply sort option (you may need to implement this in TemplateRepository)
        applySortOption(selectedSortOption);
        
        // Apply time filter (you may need to implement this in TemplateRepository)
        applyTimeFilter(selectedTimeFilter);
        
        // Load templates
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
        // Clear the cache
        repository.clearCache();
        
        // Reset filters to default
        selectedSortOption = SortOption.TRENDING;
        selectedTimeFilter = TimeFilter.ALL_TIME;
        
        // Don't reset category selection to maintain user's context
        
        // Load fresh data
        loadTemplates(true);
        
        // Clear new templates flag
        clearNewTemplatesFlag();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        repository.cancelCurrentCall();
    }
}
