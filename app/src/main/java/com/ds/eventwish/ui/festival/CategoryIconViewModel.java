package com.ds.eventwish.ui.festival;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import com.ds.eventwish.data.model.CategoryIcon;
import com.ds.eventwish.data.repository.CategoryIconRepository;
import java.util.List;

public class CategoryIconViewModel extends ViewModel {
    private final CategoryIconRepository repository;

    public CategoryIconViewModel() {
        repository = CategoryIconRepository.getInstance();
        loadCategoryIcons();
    }

    public LiveData<List<CategoryIcon>> getCategoryIcons() {
        return repository.getCategoryIcons();
    }

    public LiveData<String> getError() {
        return repository.getError();
    }

    public LiveData<Boolean> isLoading() {
        return repository.getLoading();
    }

    private void loadCategoryIcons() {
        repository.loadCategoryIcons();
    }
}