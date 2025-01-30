package com.ds.eventwish.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
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

public class TemplateRepository {
    private static TemplateRepository instance;
    private final ApiService apiService;
    private final MutableLiveData<List<Template>> templates = new MutableLiveData<>();
    private final MutableLiveData<Map<String, Integer>> categories = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private boolean isLoading = false;
    private boolean hasMorePages = true;
    private int currentPage = 1;
    private String currentCategory = null;
    private static final int PAGE_SIZE = 50;

    private TemplateRepository() {
        apiService = ApiClient.getClient();
        templates.setValue(new ArrayList<>());
    }

    public static TemplateRepository getInstance() {
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
        return isLoading;
    }

    public boolean hasMorePages() {
        return hasMorePages;
    }

    public void loadTemplates(boolean refresh) {
        if (isLoading || (!hasMorePages && !refresh)) return;
        if (refresh) {
            currentPage = 1;
            hasMorePages = true;
            templates.setValue(new ArrayList<>());
        }
        
        isLoading = true;
        loading.setValue(true);
        Call<TemplateResponse> call;
        if (currentCategory != null) {
            call = apiService.getTemplatesByCategory(currentCategory, currentPage, PAGE_SIZE);
        } else {
            call = apiService.getTemplates(currentPage, PAGE_SIZE);
        }

        call.enqueue(new Callback<TemplateResponse>() {
            @Override
            public void onResponse(Call<TemplateResponse> call, Response<TemplateResponse> response) {
                isLoading = false;
                loading.setValue(false);
                if (response.isSuccessful() && response.body() != null) {
                    TemplateResponse templateResponse = response.body();
                    List<Template> currentList = templates.getValue();
                    if (currentList == null) currentList = new ArrayList<>();
                    
                    if (refresh) {
                        currentList = new ArrayList<>(templateResponse.getTemplates());
                    } else {
                        currentList.addAll(templateResponse.getTemplates());
                    }
                    
                    templates.setValue(currentList);
                    categories.setValue(templateResponse.getCategories());
                    hasMorePages = templateResponse.isHasMore();
                    currentPage++;
                } else {
                    error.setValue("Failed to load templates");
                }
            }

            @Override
            public void onFailure(Call<TemplateResponse> call, Throwable t) {
                isLoading = false;
                loading.setValue(false);
                error.setValue(t.getMessage());
            }
        });
    }

    public void setCategory(String category) {
        if (category == null || !category.equals(currentCategory)) {
            currentCategory = category;
            loadTemplates(true);
        }
    }
}
