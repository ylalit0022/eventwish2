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
import com.google.android.material.snackbar.Snackbar;
import android.view.View;
import java.util.LinkedHashMap;
import java.util.Objects;

public class HomeViewModel extends ViewModel {
    private static final String TAG = "HomeViewModel";
    private final TemplateRepository repository;
    private final MutableLiveData<Boolean> hasNewTemplates = new MutableLiveData<>(false);
    private TemplateUpdateManager updateManager;
    private final MutableLiveData<Set<String>> newTemplateIds = new MutableLiveData<>(new HashSet<>());
    private final Set<String> viewedTemplateIds = new HashSet<>();
    
    // Add LiveData to track recommended template IDs
    private final MutableLiveData<Set<String>> recommendedTemplateIds = new MutableLiveData<>(new HashSet<>());
    
    private static final String PREF_VIEWED_TEMPLATES = "viewed_template_ids";
    private static final String PREF_LAST_CHECK_TIME = "last_check_time";
    private static final String PREF_SELECTED_CATEGORY = "selected_category";
    private static final String PREF_SELECTED_SORT = "selected_sort";
    private static final String PREF_SELECTED_TIME_FILTER = "selected_time_filter";
    
    // Filter parameters
    private String selectedCategory = null;
    private final MutableLiveData<SortOption> sortOption = new MutableLiveData<>(SortOption.TRENDING);
    private final MutableLiveData<TimeFilter> timeFilter = new MutableLiveData<>(TimeFilter.ALL);
    
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
    
    // Field to hold the current Snackbar
    private Snackbar currentSnackbar = null;

    // Add a field to track time of last category change
    private long lastCategoryChangeTime = 0;
    private static final long CATEGORY_CHANGE_THRESHOLD = 3000; // 3 seconds

    private final MutableLiveData<Map<String, Integer>> categories = new MutableLiveData<>(new LinkedHashMap<>());
    private boolean categoriesLoaded = false; // Track if categories have been loaded

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
        String timeFilter = prefs.getString(PREF_SELECTED_TIME_FILTER, TimeFilter.ALL.name());
        
        try {
            this.sortOption.setValue(SortOption.valueOf(sortOption));
        } catch (IllegalArgumentException e) {
            this.sortOption.setValue(SortOption.TRENDING);
        }
        
        try {
            this.timeFilter.setValue(TimeFilter.valueOf(timeFilter));
        } catch (IllegalArgumentException e) {
            this.timeFilter.setValue(TimeFilter.ALL);
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
     * Load categories for the UI
     */
    public void loadCategories() {
        // If categories are already loaded, don't reload
        if (categoriesLoaded && categories.getValue() != null && !categories.getValue().isEmpty()) {
            Log.d(TAG, "Categories already loaded, skipping fetch");
            return;
        }

        Log.d(TAG, "Loading categories from repository");
        repository.getCategories(new TemplateRepository.CategoriesCallback() {
            @Override
            public void onSuccess(Map<String, Integer> categoryMap) {
                repository.notifyCategoriesObservers();
                categories.setValue(categoryMap);
                categoriesLoaded = true; // Mark categories as loaded
                Log.d(TAG, "Categories loaded successfully: " + categoryMap.size());
            }

            @Override
            public void onError(String message) {
                repository.notifyCategoriesObservers();
                Log.e(TAG, "Error loading categories: " + message);
            }
        });
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
        if (Objects.equals(selectedCategory, category)) {
            Log.d(TAG, "Same category selected, skipping reload");
            return;
        }

        selectedCategory = category;
        lastCategoryChangeTime = System.currentTimeMillis();
        
        // Save the selected category
        saveTemplateState();
        
        // Load templates for the new category
        loadTemplates(true);
    }

    /**
     * Check if the selected category has changed recently
     * @return true if category changed within threshold time
     */
    public boolean hasSelectedCategoryChangedRecently() {
        return System.currentTimeMillis() - lastCategoryChangeTime < CATEGORY_CHANGE_THRESHOLD;
    }

    /**
     * Set the sort option
     * @param option The sort option to use
     */
    public void setSortOption(SortOption option) {
        if (option != sortOption.getValue()) {
            sortOption.setValue(option);
            loadTemplates(true);
        }
    }

    /**
     * Set the time filter
     * @param filter The time filter to use
     */
    public void setTimeFilter(TimeFilter filter) {
        if (filter != timeFilter.getValue()) {
            timeFilter.setValue(filter);
            loadTemplates(true);
        }
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
              ", sort: " + sortOption.getValue() + 
              ", time: " + timeFilter.getValue() + 
              ", clearExisting: " + clearExisting);
        
        // Set category filter first
        repository.setCategory(selectedCategory);
        
        // Apply sort option
        applySortOption(sortOption.getValue());
        
        // Apply time filter
        applyTimeFilter(timeFilter.getValue());
        
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
        return sortOption.getValue();
    }

    /**
     * Get the current time filter
     * @return The selected time filter
     */
    public TimeFilter getCurrentTimeFilter() {
        return timeFilter.getValue();
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
        
        // Get the current time for tracking recent changes
        long currentTime = System.currentTimeMillis();
        // Define a threshold for "new" templates (e.g., added in the last 3 days)
        long newThreshold = currentTime - (3 * 24 * 60 * 60 * 1000); // 3 days in milliseconds
        
        // Load last check time
        SharedPreferences prefs = appContext.getSharedPreferences("home_prefs", Context.MODE_PRIVATE);
        long lastCheckTime = prefs.getLong(PREF_LAST_CHECK_TIME, 0);
        
        // If this is the first check, set a reasonable time (e.g., 7 days ago)
        if (lastCheckTime == 0) {
            lastCheckTime = currentTime - (7 * 24 * 60 * 60 * 1000); // 7 days ago
        }
        
        Log.d(TAG, "Checking for new templates - Last check time: " + lastCheckTime);
        
        for (Template template : templates) {
            if (template.getId() == null || template.getId().isEmpty()) {
                continue;
            }
            
            // A template is "new" if:
            // 1. It hasn't been viewed before
            // 2. AND (It's been created/updated since last check OR it's a recent template)
            if (!viewedTemplateIds.contains(template.getId())) {
                long templateTime = template.getCreatedAtTimestamp();
                if (templateTime > lastCheckTime || templateTime > newThreshold) {
                    newIds.add(template.getId());
                    Log.d(TAG, "Marking as new: " + template.getId() + " created at: " + templateTime);
                }
            }
        }
        
        Log.d(TAG, "Found " + newIds.size() + " new templates");
        
        if (!newIds.isEmpty()) {
            hasNewTemplates.setValue(true);
            newTemplateIds.setValue(newIds);
            
            // Force a notification by setting a value
            if (newTemplateIds.getValue() != null) {
                newTemplateIds.setValue(new HashSet<>(newIds));
            }
        }
        
        // Update the last check time
        prefs.edit().putLong(PREF_LAST_CHECK_TIME, currentTime).apply();
        Log.d(TAG, "Updated last check time to: " + currentTime);
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
        categoriesLoaded = false;
        repository.clearCache();
        loadCategories();
        loadTemplates(true);
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
        return categoriesLoaded;
    }

    /**
     * Force reload categories from the server
     */
    public void forceReloadCategories() {
        categoriesLoaded = false;
        loadCategories();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        
        // Cancel all ongoing API calls
        repository.cancelCurrentCall();
        repository.cancelTemplateCall();
        
        // Clear any references that might prevent garbage collection
        dismissCurrentSnackbar();
        
        Log.d(TAG, "HomeViewModel cleared, all API calls cancelled");
    }

    /**
     * Store a reference to the current Snackbar showing pagination loading status
     * @param snackbar The Snackbar instance
     */
    public void setCurrentSnackbar(Snackbar snackbar) {
        // Dismiss any existing Snackbar first
        dismissCurrentSnackbar();
        this.currentSnackbar = snackbar;
    }
    
    /**
     * Dismiss the current Snackbar if it exists
     */
    public void dismissCurrentSnackbar() {
        if (currentSnackbar != null) {
            try {
                // Get the Snackbar view for animation
                View snackbarView = currentSnackbar.getView();
                
                // Fade out animation before dismissing
                snackbarView.animate()
                    .alpha(0f)
                    .setDuration(250)
                    .withEndAction(() -> {
                        try {
                            currentSnackbar.dismiss();
                        } catch (Exception e) {
                            Log.e(TAG, "Error dismissing Snackbar: " + e.getMessage());
                        } finally {
                            currentSnackbar = null;
                        }
                    })
                    .start();
            } catch (Exception e) {
                Log.e(TAG, "Error animating Snackbar: " + e.getMessage());
                // Fallback to direct dismissal without animation
                try {
                    currentSnackbar.dismiss();
                } catch (Exception e2) {
                    Log.e(TAG, "Error dismissing Snackbar: " + e2.getMessage());
                } finally {
                    currentSnackbar = null;
                }
            }
        }
    }

    /**
     * Get the LiveData for recommended template IDs
     */
    public LiveData<Set<String>> getRecommendedTemplateIds() {
        return recommendedTemplateIds;
    }
    
    /**
     * Set recommended template IDs
     */
    public void setRecommendedTemplateIds(Set<String> ids) {
        if (ids == null) {
            ids = new HashSet<>();
        }
        recommendedTemplateIds.setValue(ids);
    }
    
    /**
     * Add a template ID to the recommended set
     */
    public void addRecommendedTemplateId(String id) {
        if (id == null || id.isEmpty()) {
            return;
        }
        
        Set<String> currentIds = recommendedTemplateIds.getValue();
        if (currentIds == null) {
            currentIds = new HashSet<>();
        }
        
        currentIds.add(id);
        recommendedTemplateIds.setValue(currentIds);
    }

    private String searchQuery = "";
    private boolean isFullscreenMode = false;

    /**
     * Set fullscreen mode
     * @param isFullscreen true for fullscreen mode, false for normal mode
     */
    public void setFullscreenMode(boolean isFullscreen) {
        this.isFullscreenMode = isFullscreen;
    }

    /**
     * Check if fullscreen mode is enabled
     * @return true if fullscreen mode is enabled
     */
    public boolean isFullscreenMode() {
        return isFullscreenMode;
    }

    private void saveTemplateState() {
        SharedPreferences prefs = appContext.getSharedPreferences("home_prefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        // Save sort option and time filter
        editor.putString(PREF_SELECTED_SORT, sortOption.getValue().name());
        editor.putString(PREF_SELECTED_TIME_FILTER, timeFilter.getValue().name());

        // Save category and scroll position
        editor.putString(PREF_SELECTED_CATEGORY, selectedCategory);
        editor.putInt("last_visible_position", lastVisiblePosition);

        // Save viewed template IDs
        Gson gson = new Gson();
        String viewedIdsJson = gson.toJson(viewedTemplateIds);
        editor.putString(PREF_VIEWED_TEMPLATES, viewedIdsJson);

        // Save last check time
        editor.putLong(PREF_LAST_CHECK_TIME, lastRefreshTime);

        // Save fullscreen mode
        editor.putBoolean("is_fullscreen_mode", isFullscreenMode);

        // Save search query if any
        editor.putString("search_query", searchQuery);

        editor.apply();
        Log.d(TAG, "Template state saved: category=" + selectedCategory + 
              ", position=" + lastVisiblePosition + 
              ", viewedCount=" + viewedTemplateIds.size());
    }

    public LiveData<SortOption> getSortOption() {
        return sortOption;
    }

    public LiveData<TimeFilter> getTimeFilter() {
        return timeFilter;
    }

    public void loadTemplates() {
        loadTemplates(false);
    }

    // Method to force refresh categories if needed (e.g., after error)
    public void forceRefreshCategories() {
        categoriesLoaded = false;
        loadCategories();
    }

    /**
     * Clear the error state in the repository
     * Call this when returning to the fragment if templates are already loaded
     */
    public void clearErrorState() {
        if (repository != null) {
            repository.clearError();
        }
    }

    /**
     * Check if templates are currently loaded
     * @return true if templates are loaded, false otherwise
     */
    public boolean hasLoadedTemplates() {
        List<Template> loadedTemplates = getTemplates().getValue();
        return loadedTemplates != null && !loadedTemplates.isEmpty();
    }
}