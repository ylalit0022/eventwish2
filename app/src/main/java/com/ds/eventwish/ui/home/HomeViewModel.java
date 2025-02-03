package com.ds.eventwish.ui.home;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import android.util.Log;
import com.ds.eventwish.data.model.Template;
import com.ds.eventwish.data.model.response.TemplateResponse;
import com.ds.eventwish.data.remote.ApiClient;
import com.ds.eventwish.data.remote.ApiService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeViewModel extends ViewModel {
    private static final String TAG = "HomeViewModel";
    private final ApiService apiService;
    private final MutableLiveData<List<Template>> templates = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<Map<String, Integer>> categories = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private String currentCategory = null;
    private String searchQuery = "";
    private boolean hasMorePages = true;
    private int currentPage = 1;
    private static final int PAGE_SIZE = 100;
    private Call<TemplateResponse> currentCall;
    private String apiEndpoint = "";

    public HomeViewModel() {
        apiService = ApiClient.getClient();
        loadTemplates(true);
    }

    public LiveData<List<Template>> getTemplates() {
        return templates;
    }

    public LiveData<String> getError() {
        return error;
    }

    public LiveData<Map<String, Integer>> getCategories() {
        return categories;
    }

    public LiveData<Boolean> isLoading() {
        return loading;
    }

    public void setCategory(String category) {
        Log.d(TAG, "Setting category: " + category + ", current: " + currentCategory);
    
        if ((category == null && currentCategory != null) || 
            (category != null && !category.equals(currentCategory))) {
            currentCategory = category;
            Log.d(TAG, "Category changed, resetting and reloading");
    
            resetAndReload();
            loadTemplates(false);
            
            // Reload all categories to ensure they are available
            loadCategories();
        } else {
            Log.d(TAG, "Category unchanged, skipping reload");
        }
    }

    public void loadCategories() {
        Log.d(TAG, "Loading categories...");

        Call<TemplateResponse> call = apiService.getTemplates(1, PAGE_SIZE);
        call.enqueue(new Callback<TemplateResponse>() {
            @Override
            public void onResponse(Call<TemplateResponse> call, Response<TemplateResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    categories.setValue(response.body().getCategories());
                    Log.d(TAG, "Categories loaded successfully");
                } else {
                    Log.e(TAG, "Failed to load categories: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<TemplateResponse> call, Throwable t) {
                Log.e(TAG, "Network error loading categories", t);
            }
        });
    }

// old mthod EditText
//    public void setSearchQuery(String query) {
//        if (!query.equals(searchQuery)) {
//            searchQuery = query;
//            resetAndReload();
//        }
//    }


    public void setSearchQuery(String query) {
        if (query == null || query.trim().isEmpty()) {
            searchQuery = "";
        } else {
            searchQuery = query.trim();
        }
        resetAndReload();
        loadTemplates(false);
    }


    private void resetAndReload() {
        currentPage = 1;
        hasMorePages = true;
        templates.setValue(new ArrayList<>());
        cancelCurrentCall();
        loadTemplates(false);
    }

    private void cancelCurrentCall() {
        if (currentCall != null && !currentCall.isCanceled()) {
            currentCall.cancel();
        }
        loading.setValue(false);
    }

    public void loadTemplates(boolean forceRefresh) {
        if (forceRefresh) {
            resetAndReload();
            Log.d(TAG, "Skip loading - loading: " + loading.getValue() +
            ", hasMore: " + hasMorePages +
            ", category: " + currentCategory);
            return;
        }

        if (loading.getValue() == Boolean.TRUE || !hasMorePages) {
            Log.d(TAG, "Skip loading - loading: " + loading.getValue() + ", hasMore: " + hasMorePages);
            return;
        }

        loading.setValue(true);
        Log.d(TAG, "Loading page " + currentPage);

        cancelCurrentCall();

        Call<TemplateResponse> call;
        if (currentCategory != null) {
            apiEndpoint = "templates/category/" + currentCategory; // Base URL + endpoint
            call = apiService.getTemplatesByCategory(currentCategory, currentPage, PAGE_SIZE);
            Log.d(TAG, "Calling category endpoint: " + apiEndpoint +
              "?page=" + currentPage +
              "&limit=" + PAGE_SIZE);
        } else {
            apiEndpoint = "templates";
            call = apiService.getTemplates(currentPage, PAGE_SIZE);
            Log.d(TAG, "Calling all templates endpoint: " + apiEndpoint +
              "?page=" + currentPage +
              "&limit=" + PAGE_SIZE);
        }

        currentCall = call;
        call.enqueue(new Callback<TemplateResponse>() {
            @Override
            public void onResponse(Call<TemplateResponse> call, Response<TemplateResponse> response) {
                Log.d(TAG, "API Response for " + apiEndpoint + ": " + response.code());
                if (call.isCanceled()) {
                    Log.e(TAG, "API Error: " + response.code() +
                          " URL: " + call.request().url() +
                          " Message: " + response.message());
                    return;
                }

                loading.setValue(false);

                Log.d(TAG, "API Response: " + response.code() +
                          " for category: " + currentCategory);

                if (response.isSuccessful() && response.body() != null) {
                    TemplateResponse templateResponse = response.body();
                    List<Template> currentTemplates = templates.getValue();
                    List<Template> newTemplates = templateResponse.getTemplates();

                    // Apply search filter
                    if (!searchQuery.isEmpty()) {
                        newTemplates = filterTemplates(newTemplates, searchQuery);
                    }

                    Log.d(TAG, "Got response for page " + currentPage +
                          " - hasMore: " + templateResponse.isHasMore() +
                          " - items: " + newTemplates.size());

                    // Update categories if this is the first page
                    if (currentPage == 1) {
                        categories.setValue(templateResponse.getCategories());
                    }

                    // Update UI with filtered data
                    templates.setValue(newTemplates);
                    hasMorePages = templateResponse.isHasMore();

                    List<Template> updatedList;
                    if (currentTemplates != null && currentPage > 1) {
                        updatedList = new ArrayList<>(currentTemplates);
                        updatedList.addAll(newTemplates);
                    } else {
                        updatedList = new ArrayList<>(newTemplates);
                    }

                    templates.setValue(updatedList);
                    currentPage++;

                    Log.d(TAG, "Updated list size: " + updatedList.size() +
                          " - Next page: " + currentPage +
                          " - Has more: " + hasMorePages);
                } else {
                    Log.e(TAG, "API error: " + response.code());
                    error.setValue("Failed to load templates");
                    hasMorePages = false;
                }
            }

            @Override
            public void onFailure(Call<TemplateResponse> call, Throwable t) {
                if (call.isCanceled()) {
                    Log.d(TAG, "Call was canceled");
                    return;
                }

                loading.setValue(false);
                Log.e(TAG, "Network error", t);
                error.setValue("Network error: " + t.getMessage());
                hasMorePages = false;
            }
        });
    }

    private List<Template> filterTemplates(List<Template> templates, String query) {
        List<Template> filteredList = new ArrayList<>();
        String lowerCaseQuery = query.toLowerCase();

        for (Template template : templates) {
            if (template.getTitle().toLowerCase().contains(lowerCaseQuery)) {
                filteredList.add(template);
            }
        }

        return filteredList;
    }


    public void loadMoreIfNeeded(int lastVisibleItem, int totalItemCount) {
        if (loading.getValue() != Boolean.TRUE && hasMorePages && 
            lastVisibleItem >= totalItemCount - 4) {
            Log.d(TAG, "Loading more - last visible: " + lastVisibleItem + 
                  " - total: " + totalItemCount);
            loadTemplates(false);
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        cancelCurrentCall();
    }
}
