package com.ds.eventwish.ui.template;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.annotation.NonNull;
import com.ds.eventwish.data.model.Template;
import com.ds.eventwish.data.repository.TemplateInteractionRepository;
import com.ds.eventwish.data.repository.TemplateRepository;
import com.ds.eventwish.utils.AnalyticsUtils;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import android.util.Log;

public class TemplateViewModel extends AndroidViewModel {
    private static final String TAG = "TemplateViewModel";

    private final TemplateRepository templateRepository;
    private final TemplateInteractionRepository interactionRepository;
    private final Map<String, LiveData<Boolean>> likeStates;
    private final Map<String, LiveData<Boolean>> favoriteStates;
    private final Map<String, LiveData<Integer>> likeCounts;
    private final MutableLiveData<List<com.ds.eventwish.ui.template.Template>> templates;
    private final MutableLiveData<Boolean> isLoading;
    private final MutableLiveData<String> error;

    public TemplateViewModel(@NonNull Application application) {
        super(application);
        TemplateRepository.init(application);
        templateRepository = TemplateRepository.getInstance();
        interactionRepository = TemplateInteractionRepository.getInstance(application);
        likeStates = new ConcurrentHashMap<>();
        favoriteStates = new ConcurrentHashMap<>();
        likeCounts = new ConcurrentHashMap<>();
        templates = new MutableLiveData<>();
        isLoading = new MutableLiveData<>(false);
        error = new MutableLiveData<>();

        // Transform repository templates to UI templates
        templateRepository.getTemplates().observeForever(dataTemplates -> {
            if (dataTemplates != null) {
                List<com.ds.eventwish.ui.template.Template> uiTemplates = dataTemplates.stream()
                    .map(this::toUiModel)
                    .collect(Collectors.toList());
                templates.setValue(uiTemplates);
            }
        });
    }

    public LiveData<List<com.ds.eventwish.ui.template.Template>> getTemplates() {
        return templates;
    }

    public void loadTemplates() {
        isLoading.setValue(true);
        templateRepository.loadTemplates(true);
    }

    public void loadTemplatesForCategory(String categoryId) {
        isLoading.setValue(true);
        templateRepository.setCategory(categoryId, true);
    }

    public void toggleLike(com.ds.eventwish.ui.template.Template uiTemplate) {
        Log.d(TAG, String.format("Toggling like for template: %s, current state: %b", 
            uiTemplate.getId(), uiTemplate.isLiked()));
            
        com.ds.eventwish.data.model.Template dataTemplate = toDataModel(uiTemplate);
        interactionRepository.toggleLike(dataTemplate)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, String.format("Like toggled successfully for template: %s, refreshing templates", 
                    uiTemplate.getId()));
                // Refresh templates to maintain state
                templateRepository.loadTemplates(false);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, String.format("Failed to toggle like for template: %s, error: %s", 
                    uiTemplate.getId(), e.getMessage()));
                error.setValue("Failed to update like status");
            });
    }

    public void toggleFavorite(com.ds.eventwish.ui.template.Template uiTemplate) {
        Log.d(TAG, String.format("Toggling favorite for template: %s, current state: %b", 
            uiTemplate.getId(), uiTemplate.isFavorited()));
            
        com.ds.eventwish.data.model.Template dataTemplate = toDataModel(uiTemplate);
        interactionRepository.toggleFavorite(dataTemplate)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, String.format("Favorite toggled successfully for template: %s, refreshing templates", 
                    uiTemplate.getId()));
                // Refresh templates to maintain state
                templateRepository.loadTemplates(false);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, String.format("Failed to toggle favorite for template: %s, error: %s", 
                    uiTemplate.getId(), e.getMessage()));
                error.setValue("Failed to update favorite status");
            });
    }

    private com.ds.eventwish.ui.template.Template toUiModel(com.ds.eventwish.data.model.Template dataModel) {
        return new com.ds.eventwish.ui.template.Template(
            dataModel.getId(),
            dataModel.getName(),
            dataModel.getCategoryId(),
            dataModel.getPreviewUrl(),
            dataModel.isLiked(),
            dataModel.isFavorited(),
            dataModel.getLikeCount()
        );
    }

    private com.ds.eventwish.data.model.Template toDataModel(com.ds.eventwish.ui.template.Template uiModel) {
        com.ds.eventwish.data.model.Template template = new com.ds.eventwish.data.model.Template();
        template.setId(uiModel.getId());
        template.setTitle(uiModel.getName());
        template.setCategoryId(uiModel.getCategoryId());
        template.setPreviewUrl(uiModel.getImageUrl());
        template.setLiked(uiModel.isLiked());
        template.setFavorited(uiModel.isFavorited());
        template.setLikeCount(uiModel.getLikeCount());
        return template;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        // Remove the observer when the ViewModel is cleared
        templateRepository.getTemplates().removeObserver(dataTemplates -> {
            if (dataTemplates != null) {
                List<com.ds.eventwish.ui.template.Template> uiTemplates = dataTemplates.stream()
                    .map(this::toUiModel)
                    .collect(Collectors.toList());
                templates.setValue(uiTemplates);
            }
        });
    }
} 