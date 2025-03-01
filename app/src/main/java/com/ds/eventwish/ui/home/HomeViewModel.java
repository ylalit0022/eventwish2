package com.ds.eventwish.ui.home;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import android.content.Context;
import android.util.Log;
import com.ds.eventwish.data.model.Template;
import com.ds.eventwish.data.repository.TemplateRepository;
import com.ds.eventwish.utils.TemplateUpdateManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HomeViewModel extends ViewModel {
    private static final String TAG = "HomeViewModel";
    private final TemplateRepository repository;
    private String searchQuery = "";
    private final MutableLiveData<Boolean> hasNewTemplates = new MutableLiveData<>(false);
    private TemplateUpdateManager updateManager;

    // Store the last visible position
    private int lastVisiblePosition = 0;

    public HomeViewModel() {
        repository = TemplateRepository.getInstance();
        if (repository.getTemplates().getValue() == null || repository.getTemplates().getValue().isEmpty()) {
            loadTemplates(false);
        }
    }

    public void init(Context context) {
        updateManager = TemplateUpdateManager.getInstance(context);
        // Check if we have new templates from a previous session
        hasNewTemplates.setValue(updateManager.hasNewTemplates());
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

    public LiveData<Boolean> getLoading() {
        return repository.getLoading();
    }

    public LiveData<Boolean> getHasNewTemplates() {
        return hasNewTemplates;
    }

    public void setCategory(String category) {
        if (category != null && category.equals("All")) {
            category = null;
        }
        
        // Log the category being set
        android.util.Log.d("HomeViewModel", "Setting category to: " + (category != null ? category : "All"));
        
        repository.setCategory(category);
    }

    /**
     * Load templates from the repository
     * @param refresh Whether to force a refresh
     */
    public void loadTemplates(boolean refresh) {
        Log.d(TAG, "Loading templates, refresh: " + refresh);
        if (refresh) {
            // Don't clear the new templates flag here, let the UI handle it
        }
        repository.loadTemplates(refresh);
    }

    /**
     * Load more templates if needed
     */
    public void loadMoreIfNeeded(int lastVisibleItem, int totalItemCount) {
        if (!repository.isLoading() && repository.hasMorePages() && 
            lastVisibleItem >= 0 && lastVisibleItem < totalItemCount) {
            Log.d(TAG, "Loading more templates");
            repository.loadTemplates(false);
        }
    }

    public void setSearchQuery(String query) {
        if ((query == null && searchQuery == null) || 
            (query != null && query.equals(searchQuery))) {
            return;
        }
        searchQuery = query;
        repository.setCategory(null);
        loadTemplates(true);
    }

    public String getCurrentCategory() {
        return repository.getCurrentCategory();
    }

    // Check for new templates and update the indicator
    public void checkForNewTemplates(List<Template> templates) {
        if (updateManager != null && templates != null && !templates.isEmpty()) {
            boolean hasNew = updateManager.checkForNewTemplates(templates);
            Log.d(TAG, "Checked for new templates: " + hasNew);
            
            // Only update the LiveData if there's a change to avoid unnecessary UI updates
            if (hasNew != hasNewTemplates.getValue()) {
                hasNewTemplates.setValue(hasNew);
                Log.d(TAG, "Updated new templates indicator: " + hasNew);
            }
        }
    }

    /**
     * Clear the new templates flag
     */
    public void clearNewTemplatesFlag() {
        if (updateManager != null) {
            // First update the UI
            hasNewTemplates.setValue(false);
            Log.d(TAG, "Set hasNewTemplates to false in UI");
            
            // Then update the persistent storage
            updateManager.resetNewTemplatesFlag();
            Log.d(TAG, "Cleared new templates flag in storage");
        }
    }

    public void saveScrollPosition(int position) {
        this.lastVisiblePosition = position;
        Log.d(TAG, "Saved scroll position: " + position);
    }

    public int getLastVisiblePosition() {
        return lastVisiblePosition;
    }

    /**
     * Save the current page for pagination
     */
    public void saveCurrentPage(int page) {
        repository.setCurrentPage(page);
        Log.d(TAG, "Saved current page: " + page);
    }
    
    /**
     * Get the current page
     */
    public int getCurrentPage() {
        return repository.getCurrentPage();
    }

    /**
     * Mark a template as viewed
     * @param templateId The ID of the template that was viewed
     */
    public void markTemplateAsViewed(String templateId) {
        if (updateManager != null && templateId != null && !templateId.isEmpty()) {
            updateManager.markTemplateAsViewed(templateId);
            Log.d(TAG, "Marked template as viewed: " + templateId);
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        repository.cancelCurrentCall();
    }
}
