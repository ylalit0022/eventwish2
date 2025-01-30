package com.ds.eventwish.ui.templates;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import com.ds.eventwish.data.model.Template;
import com.ds.eventwish.data.repository.TemplateRepository;
import java.util.List;
import java.util.Map;

public class TemplateViewModel extends ViewModel {
    private final TemplateRepository repository;

    public TemplateViewModel() {
        repository = TemplateRepository.getInstance();
    }

    public LiveData<List<Template>> getTemplates() {
        return repository.getTemplates();
    }

    public LiveData<Map<String, Integer>> getCategories() {
        return repository.getCategories();
    }

    public LiveData<String> getError() {
        return repository.getError();
    }

    public void loadTemplates(boolean refresh) {
        repository.loadTemplates(refresh);
    }

    public void setCategory(String category) {
        repository.setCategory(category);
    }

    public boolean isLoading() {
        return repository.isLoading();
    }

    public boolean hasMorePages() {
        return repository.hasMorePages();
    }

    public void loadMoreIfNeeded(int lastVisibleItemPosition, int totalItemCount) {
        if (!isLoading() && hasMorePages() && 
            lastVisibleItemPosition >= totalItemCount - 5) {
            loadTemplates(false);
        }
    }
}
