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
import java.util.Collections;
import java.lang.StringBuilder;
import com.ds.eventwish.utils.AnalyticsUtils;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

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

    // Add a field to track time of last end message display
    private long lastEndMessageTime = 0;
    private static final long END_MESSAGE_DISPLAY_INTERVAL = 60000; // 1 minute

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
        
        // Load lastEndMessageTime to prevent repeated toasts
        lastEndMessageTime = prefs.getLong("last_end_message_time", 0);
        
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

        Log.d(TAG, "Setting category from " + 
              (selectedCategory != null ? selectedCategory : "All") + 
              " to " + (category != null ? category : "All"));
              
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
        
        // Create a named observer for templates
        androidx.lifecycle.Observer<List<Template>> templatesObserver = new androidx.lifecycle.Observer<List<Template>>() {
            @Override
            public void onChanged(List<Template> templates) {
                if (templates != null && !templates.isEmpty()) {
                    // Create a new list to avoid modifying the repository's list directly
                    List<Template> sortedTemplates = new ArrayList<>(templates);
                    
                    // Sort by creation date (newest first)
                    Collections.sort(sortedTemplates, (t1, t2) -> {
                        long time1 = t1.getCreatedAtTimestamp();
                        long time2 = t2.getCreatedAtTimestamp();
                        // Sort in descending order (newest first)
                        return Long.compare(time2, time1);
                    });
                    
                    Log.d(TAG, "Sorted " + sortedTemplates.size() + " templates by creation date (newest first)");
                    
                    // Check for new templates in the sorted list
                    checkForNewTemplates(sortedTemplates);
                    
                    // Remove observer to avoid multiple callbacks
                    repository.getTemplates().removeObserver(this);
                }
            }
        };
        
        // Add the observer to get templates when they're loaded
        repository.getTemplates().observeForever(templatesObserver);
        
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
     * Check if there are more pages of templates to load
     * @return true if more pages are available, false if at the end
     */
    public boolean hasMorePagesToLoad() {
        // Check with repository to see if there are more pages
        return repository.hasMorePages();
    }

    /**
     * Load more templates if needed based on scroll position
     * @param lastVisibleItem Position of the last visible item
     * @param totalItemCount Total number of items in the adapter
     */
    public void loadMoreIfNeeded(int lastVisibleItem, int totalItemCount) {
        if (isPaginationInProgress || repository.isLoading()) {
            Log.d(TAG, "Already loading, skipping loadMoreIfNeeded");
            return;
        }

        // Check if we're near the end of the list and have more pages
        if (lastVisibleItem + VISIBLE_THRESHOLD >= totalItemCount && hasMorePagesToLoad()) {
            Log.d(TAG, "Loading more templates, pagination in progress");
            
            // Set pagination flag to true
            isPaginationInProgress = true;
            
            // Load more templates
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
        
        // First check for completely new template IDs we haven't seen before
        Set<String> allKnownIds = new HashSet<>(viewedTemplateIds);
        
        // Load previously seen template IDs from SharedPreferences
        String savedIdsString = prefs.getString("all_template_ids", "");
        if (!savedIdsString.isEmpty()) {
            String[] savedIds = savedIdsString.split(",");
            Collections.addAll(allKnownIds, savedIds);
        }
        
        // Current template IDs for saving later
        Set<String> currentIds = new HashSet<>();
        
        for (Template template : templates) {
            if (template.getId() == null || template.getId().isEmpty()) {
                continue;
            }
            
            // Add to current IDs for tracking
            currentIds.add(template.getId());
            
            // Check if this is a completely new template we haven't seen before
            boolean isNewId = !allKnownIds.contains(template.getId());
            
            // A template is "new" if:
            // 1. It hasn't been viewed before
            // 2. AND (It's a completely new ID OR it's been created/updated since last check OR it's a recent template)
            if (!viewedTemplateIds.contains(template.getId())) {
                long templateTime = 0;
                try {
                    templateTime = template.getCreatedAtTimestamp();
                } catch (Exception e) {
                    Log.e(TAG, "Error getting timestamp for template: " + template.getId(), e);
                }
                
                // Force templateTime to current time if it's zero or negative (invalid)
                if (templateTime <= 0) {
                    Log.d(TAG, "Template has invalid timestamp, using current time: " + template.getId());
                    templateTime = currentTime;
                }
                
                // Debug log to see what's happening with this template
                Log.d(TAG, "Template " + template.getId() + 
                      " created at: " + templateTime + 
                      ", last check: " + lastCheckTime + 
                      ", is new ID: " + isNewId +
                      ", is new: " + (isNewId || templateTime > lastCheckTime || templateTime > newThreshold));
                
                // Mark as new if it's a new ID or has a recent timestamp
                if (isNewId || templateTime > lastCheckTime || templateTime > newThreshold) {
                    newIds.add(template.getId());
                    Log.d(TAG, "Marking as new: " + template.getId() + " created at: " + templateTime);
                }
            }
        }
        
        // Save the current template IDs for future comparisons
        StringBuilder idsBuilder = new StringBuilder();
        for (String id : currentIds) {
            idsBuilder.append(id).append(",");
        }
        prefs.edit().putString("all_template_ids", idsBuilder.toString()).apply();
        
        Log.d(TAG, "Found " + newIds.size() + " new templates");
        
        // Always update LiveData with current new templates, even if empty
        // This ensures UI is always in sync with our data
        hasNewTemplates.setValue(!newIds.isEmpty());
        newTemplateIds.setValue(newIds);
        
        // Only update the last check time if we're not finding any new templates
        // This ensures that if new templates are added, they'll still be detected on next check
        if (newIds.isEmpty()) {
            prefs.edit().putLong(PREF_LAST_CHECK_TIME, currentTime).apply();
            Log.d(TAG, "Updated last check time to: " + currentTime);
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

        // Save last end message time
        editor.putLong("last_end_message_time", lastEndMessageTime);

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

    /**
     * Set the last visible position in the templates list
     * @param position Position to save
     */
    public void setLastVisiblePosition(int position) {
        Log.d(TAG, "Setting last visible position: " + position);
        this.lastVisiblePosition = position;
        
        // Also save to SharedPreferences for persistence
        if (appContext != null) {
            SharedPreferences prefs = appContext.getSharedPreferences("home_prefs", Context.MODE_PRIVATE);
            prefs.edit().putInt("last_visible_position", position).apply();
        }
    }

    /**
     * Set the timestamp when the end message was last shown
     * @param timestamp The current time in milliseconds
     */
    public void setLastEndMessageTime(long timestamp) {
        this.lastEndMessageTime = timestamp;
        // Also save to SharedPreferences for persistence
        if (appContext != null) {
            SharedPreferences prefs = appContext.getSharedPreferences("home_prefs", Context.MODE_PRIVATE);
            prefs.edit().putLong("last_end_message_time", timestamp).apply();
        }
    }
    
    /**
     * Check if enough time has passed since the last end message was shown
     * @return true if it's OK to show the message again, false otherwise
     */
    public boolean canShowEndMessage() {
        long now = System.currentTimeMillis();
        return (now - lastEndMessageTime) > END_MESSAGE_DISPLAY_INTERVAL;
    }

    // Add method to get current Snackbar
    public Snackbar getCurrentSnackbar() {
        return currentSnackbar;
    }

    /**
     * Handle template like interaction
     * @param template The template that was liked
     * @return Task that completes when the operation is done
     */
    public Task<Boolean> handleTemplateLike(Template template) {
        if (template == null || template.getId() == null) {
            return Tasks.forException(new IllegalArgumentException("Template or template ID is null"));
        }
        
        // Get the new state (opposite of current)
        boolean newLikeState = !template.isLiked();
        
        // Track the event with appropriate action
        if (newLikeState) {
            AnalyticsUtils.getInstance().trackTemplateLike(template.getId(), "home_feed");
        } else {
            AnalyticsUtils.getInstance().trackTemplateUnlike(template.getId(), "home_feed");
        }
        
        // Update the template in repository and return the task
        return repository.toggleLike(template.getId(), newLikeState);
    }

    /**
     * Handle template favorite interaction
     * @param template The template that was favorited
     * @return Task that completes when the operation is done
     */
    public Task<Boolean> handleTemplateFavorite(Template template) {
        if (template == null || template.getId() == null) {
            return Tasks.forException(new IllegalArgumentException("Template or template ID is null"));
        }
        
        // Get the new state (opposite of current)
        boolean newFavoriteState = !template.isFavorited();
        
        // Track the event with appropriate action
        if (newFavoriteState) {
            AnalyticsUtils.getInstance().trackTemplateFavorite(template.getId(), "home_feed");
        } else {
            AnalyticsUtils.getInstance().trackTemplateUnfavorite(template.getId(), "home_feed");
        }
        
        // Update the template in repository and return the task
        return repository.toggleFavorite(template.getId(), newFavoriteState);
    }
}