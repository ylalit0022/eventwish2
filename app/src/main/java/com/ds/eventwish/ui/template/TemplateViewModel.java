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

    public LiveData<Boolean> getLikeState(String templateId) {
        if (!likeStates.containsKey(templateId)) {
            likeStates.put(templateId, templateRepository.getLikeState(templateId));
        }
        return likeStates.get(templateId);
    }

    public LiveData<Boolean> getFavoriteState(String templateId) {
        if (!favoriteStates.containsKey(templateId)) {
            favoriteStates.put(templateId, templateRepository.getFavoriteState(templateId));
        }
        return favoriteStates.get(templateId);
    }

    public LiveData<Integer> getLikeCount(String templateId) {
        if (!likeCounts.containsKey(templateId)) {
            likeCounts.put(templateId, templateRepository.getLikeCount(templateId));
        }
        return likeCounts.get(templateId);
    }

    public void toggleLike(String templateId) {
        // Get current state
        Boolean currentState = getLikeState(templateId).getValue();
        boolean newState = currentState == null ? true : !currentState;
        
        // Track analytics
        AnalyticsUtils.getInstance().trackTemplateInteraction(
            templateId,
            newState ? "like" : "unlike"
        );
        
        // Update through repository with new state
        templateRepository.toggleLike(templateId, newState);
    }

    public void toggleFavorite(String templateId) {
        // Get current state
        Boolean currentState = getFavoriteState(templateId).getValue();
        boolean newState = currentState == null ? true : !currentState;
        
        // Track analytics
        AnalyticsUtils.getInstance().trackTemplateInteraction(
            templateId,
            newState ? "favorite" : "unfavorite"
        );
        
        // Update through repository with new state
        templateRepository.toggleFavorite(templateId, newState);
    }

    private com.ds.eventwish.ui.template.Template toUiModel(com.ds.eventwish.data.model.Template dataTemplate) {
        return new com.ds.eventwish.ui.template.Template(
            dataTemplate.getId(),
            dataTemplate.getTitle(),
            dataTemplate.getCategoryId(),
            dataTemplate.getPreviewUrl(),
            dataTemplate.isLiked(),
            dataTemplate.isFavorited(),
            dataTemplate.getLikeCount()
        );
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        // Clear all cached LiveData
        likeStates.clear();
        favoriteStates.clear();
        likeCounts.clear();
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