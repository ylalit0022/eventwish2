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
    private String currentCategory = null;
    private String searchQuery = "";
    private boolean hasMorePages = true;
    private int currentPage = 1;
    private static final int PAGE_SIZE = 100;
    private Call<TemplateResponse> currentCall;
    private String apiEndpoint = "";

    public HomeViewModel() {
        apiService = ApiClient.getClient();
        loadTemplates(false);
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

    public void setCategory(String category) {
        Log.d(TAG, "Setting category: " + category + ", current: " + currentCategory);
    
        if ((category == null && currentCategory != null) || 
            (category != null && !category.equals(currentCategory))) {
            currentCategory = category;
            Log.d(TAG, "Category changed, resetting and reloading");
            categories.setValue(null);
            resetAndReload();
            loadCategories();
        } else {
            Log.d(TAG, "Category unchanged, skipping reload");
        }
    }

    private void loadCategories() {
        Call<TemplateResponse> call = apiService.getTemplates(1, PAGE_SIZE);
        call.enqueue(new Callback<TemplateResponse>() {
            @Override
            public void onResponse(Call<TemplateResponse> call, Response<TemplateResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    categories.setValue(response.body().getCategories());
                }
            }

            @Override
            public void onFailure(Call<TemplateResponse> call, Throwable t) {
                Log.e(TAG, "Failed to load categories", t);
            }
        });
    }

    public void loadTemplates(boolean forceRefresh) {
        if (forceRefresh) {
            resetAndReload();
            return;
        }

        if (!hasMorePages) {
            return;
        }

        Call<TemplateResponse> call;
        if (currentCategory != null) {
            apiEndpoint = "templates/category/" + currentCategory;
            call = apiService.getTemplatesByCategory(currentCategory, currentPage, PAGE_SIZE);
        } else {
            apiEndpoint = "templates";
            call = apiService.getTemplates(currentPage, PAGE_SIZE);
        }

        currentCall = call;
        call.enqueue(new Callback<TemplateResponse>() {
            @Override
            public void onResponse(Call<TemplateResponse> call, Response<TemplateResponse> response) {
                if (call.isCanceled()) return;

                if (response.isSuccessful() && response.body() != null) {
                    TemplateResponse templateResponse = response.body();
                    List<Template> currentTemplates = templates.getValue();
                    List<Template> newTemplates = templateResponse.getTemplates();

                    if (!searchQuery.isEmpty()) {
                        newTemplates = filterTemplates(newTemplates, searchQuery);
                    }

                    List<Template> updatedList;
                    if (currentTemplates != null && currentPage > 1) {
                        updatedList = new ArrayList<>(currentTemplates);
                        updatedList.addAll(newTemplates);
                    } else {
                        updatedList = new ArrayList<>(newTemplates);
                    }

                    templates.setValue(updatedList);
                    
                    if (currentCategory == null && (currentPage == 1 || categories.getValue() == null)) {
                        categories.setValue(templateResponse.getCategories());
                    }
                    
                    hasMorePages = templateResponse.isHasMore();
                    currentPage++;
                } else {
                    error.setValue("Failed to load templates");
                    hasMorePages = false;
                }
            }

            @Override
            public void onFailure(Call<TemplateResponse> call, Throwable t) {
                if (!call.isCanceled()) {
                    error.setValue("Network error: " + t.getMessage());
                    hasMorePages = false;
                }
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
        if (hasMorePages && lastVisibleItem >= totalItemCount - 4) {
            loadTemplates(false);
        }
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
    }

    public void setSearchQuery(String query) {
        if (query == null || query.trim().isEmpty()) {
            searchQuery = "";
        } else {
            searchQuery = query.trim();
        }
        resetAndReload();
        loadTemplates(false);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        cancelCurrentCall();
    }

    public String getCurrentCategory() {
        return currentCategory;
    }
}
