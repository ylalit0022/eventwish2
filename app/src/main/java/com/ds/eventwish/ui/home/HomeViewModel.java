package com.ds.eventwish.ui.home;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.ds.eventwish.data.model.Template;
import com.ds.eventwish.data.repository.TemplateRepository;
import java.util.List;
import java.util.Map;

public class HomeViewModel extends ViewModel {
    private final TemplateRepository repository;
    private final MutableLiveData<List<Template>> templates;
    private final MutableLiveData<Map<String, Integer>> categories;
    private final MutableLiveData<String> error;
    private String currentSearchQuery = "";

    public HomeViewModel() {
        repository = TemplateRepository.getInstance();
        templates = new MutableLiveData<>();
        categories = new MutableLiveData<>();
        error = new MutableLiveData<>();
        
        // Observe repository data
        repository.getTemplates().observeForever(templates::setValue);
        repository.getCategories().observeForever(categories::setValue);
        repository.getError().observeForever(error::setValue);
        
        // Initial load
        loadTemplates(true);
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
        return repository.getLoading();
    }

    public void loadTemplates(boolean refresh) {
        repository.loadTemplates(refresh);
    }

    public void setCategory(String category) {
        repository.setCategory(category);
    }

    public void setSearchQuery(String query) {
        currentSearchQuery = query;
        // Implement search functionality here if needed
    }

    public void loadMoreIfNeeded(int lastVisibleItemPosition, int totalItemCount) {
        if (!repository.isLoading() && repository.hasMorePages() && 
            lastVisibleItemPosition >= totalItemCount - 5) {
            loadTemplates(false);
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        // Remove observers to prevent memory leaks
        repository.getTemplates().removeObserver(templates::setValue);
        repository.getCategories().removeObserver(categories::setValue);
        repository.getError().removeObserver(error::setValue);
    }
}
