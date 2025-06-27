package com.ds.eventwish.ui.template;

import android.app.Application;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.ds.eventwish.data.model.Template;
import com.ds.eventwish.data.repository.TemplateRepository;
import java.util.ArrayList;
import java.util.List;
import com.google.android.gms.tasks.Task;

public class TemplateViewModel extends AndroidViewModel {
    private static final String TAG = "TemplateViewModel";
    private final TemplateRepository repository;
    private final MutableLiveData<List<Template>> templates;
    private final MutableLiveData<Boolean> loading;
    private final MutableLiveData<String> error;

    public TemplateViewModel(@NonNull Application application) {
        super(application);
        repository = TemplateRepository.getInstance();
        templates = new MutableLiveData<>(new ArrayList<>());
        loading = new MutableLiveData<>(false);
        error = new MutableLiveData<>();
        
        // Observe repository templates
        repository.getTemplates().observeForever(templateList -> {
            if (templateList != null) {
                templates.postValue(templateList);
            }
        });
    }

    public LiveData<List<Template>> getTemplates() {
        return templates;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<String> getError() {
        return error;
    }

    public void loadTemplates(boolean forceRefresh) {
        loading.postValue(true);
        repository.loadTemplates(forceRefresh);
    }

    public void toggleLike(String templateId, boolean currentLikeState) {
        repository.toggleLike(templateId, !currentLikeState)
            .addOnSuccessListener(success -> {
                if (success) {
                    Log.d(TAG, "Like state updated successfully");
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error updating like state", e);
                error.postValue("Failed to update like state");
            });
    }

    public void toggleFavorite(String templateId, boolean currentFavoriteState) {
        repository.toggleFavorite(templateId, !currentFavoriteState)
            .addOnSuccessListener(success -> {
                if (success) {
                    Log.d(TAG, "Favorite state updated successfully");
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error updating favorite state", e);
                error.postValue("Failed to update favorite state");
            });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        // Clean up any resources
    }
} 