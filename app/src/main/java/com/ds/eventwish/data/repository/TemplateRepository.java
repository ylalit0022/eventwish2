package com.ds.eventwish.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.ds.eventwish.data.model.Template;
import com.ds.eventwish.data.model.response.TemplateResponse;
import com.ds.eventwish.data.remote.ApiClient;
import com.ds.eventwish.data.remote.ApiService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TemplateRepository {
    private static TemplateRepository instance;
    private final ApiService apiService;
    private final MutableLiveData<List<Template>> templates = new MutableLiveData<>();
    private final MutableLiveData<Map<String, Integer>> categories = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private boolean hasMorePages = true;
    private int currentPage = 1;
    private String currentCategory = null;
    private static final int PAGE_SIZE = 20;
    private Call<TemplateResponse> currentCall;

    private TemplateRepository() {
        apiService = ApiClient.getClient();
        templates.setValue(new ArrayList<>());
    }

    public static synchronized TemplateRepository getInstance() {
        if (instance == null) {
            instance = new TemplateRepository();
        }
        return instance;
    }

    public LiveData<List<Template>> getTemplates() {
        return templates;
    }

    public LiveData<Map<String, Integer>> getCategories() {
        return categories;
    }

    public LiveData<String> getError() {
        return error;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public boolean isLoading() {
        return loading.getValue() != null && loading.getValue();
    }

    public boolean hasMorePages() {
        return hasMorePages;
    }

    public String getCurrentCategory() {
        return currentCategory;
    }

    public void cancelCurrentCall() {
        if (currentCall != null && !currentCall.isCanceled()) {
            currentCall.cancel();
        }
    }

    private void setCurrentCall(Call<TemplateResponse> call) {
        cancelCurrentCall();
        currentCall = call;
    }

    public void setCategory(String category) {
        if ((category == null && currentCategory == null) || 
            (category != null && category.equals(currentCategory))) {
            return;
        }
        
        android.util.Log.d("TemplateRepository", "Changing category from " + 
            (currentCategory != null ? currentCategory : "All") + " to " + 
            (category != null ? category : "All"));
            
        currentCategory = category;
        currentPage = 1;
        hasMorePages = true;
        templates.setValue(new ArrayList<>());
        loadTemplates(true);
    }

    public void loadTemplates(boolean forceRefresh) {
        if (loading.getValue()) {
            return;
        }
        
        loading.setValue(true);
        
        // If this is a forced refresh, reset pagination
        if (forceRefresh) {
            currentPage = 1;
            hasMorePages = true;
            if (templates.getValue() != null) {
                templates.getValue().clear();
                // Notify observers of the empty list to clear the UI
                templates.setValue(new ArrayList<>());
            } else {
                templates.setValue(new ArrayList<>());
            }
        }
        
        // If we've already loaded all pages, don't make another request
        if (!hasMorePages && currentPage > 1) {
            loading.setValue(false);
            return;
        }
        
        // Log request details
        android.util.Log.d("TemplateRepository", "Loading templates - page: " + currentPage + 
                          ", category: " + (currentCategory != null ? currentCategory : "All") + 
                          ", forceRefresh: " + forceRefresh);

        Call<TemplateResponse> call;
        if (currentCategory != null) {
            call = apiService.getTemplatesByCategory(currentCategory, currentPage, PAGE_SIZE);
        } else {
            call = apiService.getTemplates(currentPage, PAGE_SIZE);
        }

        setCurrentCall(call);
        call.enqueue(new Callback<TemplateResponse>() {
            @Override
            public void onResponse(Call<TemplateResponse> call, Response<TemplateResponse> response) {
                loading.setValue(false);

                if (response.isSuccessful() && response.body() != null) {
                    TemplateResponse templateResponse = response.body();
                    List<Template> currentList = templates.getValue();
                    if (currentList == null) currentList = new ArrayList<>();
                    
                    if (forceRefresh) {
                        currentList = new ArrayList<>(templateResponse.getTemplates());
                    } else {
                        currentList.addAll(templateResponse.getTemplates());
                    }
                    
                    // Log all template IDs for debugging
                    for (Template template : templateResponse.getTemplates()) {
                        android.util.Log.d("TemplateRepository", "Template: " + template.getTitle() + 
                                          ", ID: " + template.getId() + 
                                          ", Created: " + template.getCreatedAt());
                    }
                    
                    templates.setValue(currentList);
                    
                    // Log categories for debugging
                    Map<String, Integer> categoryMap = templateResponse.getCategories();
                    if (categoryMap != null) {
                        android.util.Log.d("TemplateRepository", "Categories received: " + categoryMap.size());
                        for (Map.Entry<String, Integer> entry : categoryMap.entrySet()) {
                            android.util.Log.d("TemplateRepository", "Category: " + entry.getKey() + ", Count: " + entry.getValue());
                        }
                    } else {
                        android.util.Log.d("TemplateRepository", "Categories map is null");
                        // Add mock categories if none are returned from API
                        categoryMap = new HashMap<>();
                        categoryMap.put("Birthday", 10);
                        categoryMap.put("Wedding", 8);
                        categoryMap.put("Anniversary", 6);
                        categoryMap.put("Graduation", 5);
                        categoryMap.put("Holiday", 7);
                        categoryMap.put("Congratulations", 4);
                        android.util.Log.d("TemplateRepository", "Added mock categories: " + categoryMap.size());
                    }
                    
                    categories.setValue(categoryMap);
                    hasMorePages = templateResponse.isHasMore();
                    currentPage++;
                } else {
                    error.setValue("Failed to load templates");
                }
            }

            @Override
            public void onFailure(Call<TemplateResponse> call, Throwable t) {
                loading.setValue(false);
                error.setValue(t.getMessage());
            }
        });
    }

    /**
     * Set the current page for pagination
     */
    public void setCurrentPage(int page) {
        this.currentPage = page;
    }
    
    /**
     * Get the current page
     */
    public int getCurrentPage() {
        return currentPage;
    }
}
