package com.ds.eventwish.ui.viewmodel;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.ds.eventwish.data.model.NotificationPreference;
import com.ds.eventwish.data.model.UserPreferences;
import com.ds.eventwish.data.repository.UserPreferencesRepository;
import com.google.android.gms.tasks.Task;

/**
 * ViewModel for managing user preferences UI state
 */
public class UserPreferencesViewModel extends AndroidViewModel {
    private final UserPreferencesRepository repository;
    private final LiveData<UserPreferences> userPreferences;
    private final LiveData<Boolean> isLoading;
    private final LiveData<String> error;
    private final MediatorLiveData<Boolean> isFavorited;
    private final MediatorLiveData<Boolean> isLiked;
    private String currentTemplateId;

    public UserPreferencesViewModel(Application application) {
        super(application);
        repository = UserPreferencesRepository.getInstance(application);
        userPreferences = repository.getUserPreferences();
        isLoading = repository.getIsLoading();
        error = repository.getError();
        isFavorited = new MediatorLiveData<>();
        isLiked = new MediatorLiveData<>();

        // Add user preferences as source for favorite/like state
        isFavorited.addSource(userPreferences, prefs -> {
            if (prefs != null && currentTemplateId != null) {
                isFavorited.setValue(prefs.isTemplateFavorited(currentTemplateId));
            }
        });

        isLiked.addSource(userPreferences, prefs -> {
            if (prefs != null && currentTemplateId != null) {
                isLiked.setValue(prefs.isTemplateLiked(currentTemplateId));
            }
        });
    }

    public LiveData<UserPreferences> getUserPreferences() {
        return userPreferences;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getError() {
        return error;
    }

    public LiveData<Boolean> getIsFavorited() {
        return isFavorited;
    }

    public LiveData<Boolean> getIsLiked() {
        return isLiked;
    }

    /**
     * Set current template ID and update favorite/like state
     */
    public void setCurrentTemplateId(String templateId) {
        this.currentTemplateId = templateId;
        if (templateId != null) {
            repository.isTemplateFavorited(templateId)
                    .addOnSuccessListener(isFavorited::setValue);
            repository.isTemplateLiked(templateId)
                    .addOnSuccessListener(isLiked::setValue);
        }
    }

    /**
     * Toggle template favorite state
     */
    public Task<Void> toggleFavorite() {
        if (currentTemplateId == null) {
            return null;
        }

        Boolean currentState = isFavorited.getValue();
        if (currentState == null || !currentState) {
            return repository.addToFavorites(currentTemplateId);
        } else {
            return repository.removeFromFavorites(currentTemplateId);
        }
    }

    /**
     * Toggle template like state
     */
    public Task<Void> toggleLike() {
        if (currentTemplateId == null) {
            return null;
        }

        Boolean currentState = isLiked.getValue();
        if (currentState == null || !currentState) {
            return repository.addToLikes(currentTemplateId);
        } else {
            return repository.removeFromLikes(currentTemplateId);
        }
    }

    /**
     * Update notification preferences
     */
    public Task<Void> updateNotificationPreference(NotificationPreference preference) {
        return repository.updateNotificationPreference(preference);
    }

    /**
     * Start listening for user preferences changes
     */
    public void startListening() {
        repository.startListening();
    }

    /**
     * Stop listening for user preferences changes
     */
    public void stopListening() {
        repository.stopListening();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        stopListening();
    }
} 