package com.ds.eventwish.ui.profile;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.ds.eventwish.data.auth.AuthManager;
import com.ds.eventwish.data.local.AppDatabase;
import com.ds.eventwish.data.local.dao.UserDao;
import com.ds.eventwish.data.local.entity.UserEntity;
import com.ds.eventwish.data.model.Template;
import com.ds.eventwish.data.repository.TemplateRepository;
import com.ds.eventwish.data.repository.UserRepository;
import com.ds.eventwish.util.AppExecutors;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

import java.util.Collections;
import java.util.List;

public class ProfileViewModel extends AndroidViewModel {

    private static final int DEFAULT_TEMPLATE_LIMIT = 5;

    private final MutableLiveData<String> username = new MutableLiveData<>();
    private final MutableLiveData<String> email = new MutableLiveData<>();
    private final MutableLiveData<String> profilePhoto = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final TemplateRepository templateRepository;
    private final UserRepository userRepository;
    private final AuthManager authManager;
    private final MutableLiveData<Boolean> isRefreshing = new MutableLiveData<>(false);

    private LiveData<List<Template>> recentlyLikedTemplates;
    private LiveData<List<Template>> recentlyFavoritedTemplates;

    private final MutableLiveData<List<Template>> likedTemplates = new MutableLiveData<>();
    private final MutableLiveData<List<Template>> favoriteTemplates = new MutableLiveData<>();

    public ProfileViewModel(Application application) {
        super(application);
        // Initialize repositories
        TemplateRepository.init(application);
        templateRepository = TemplateRepository.getInstance();
        userRepository = UserRepository.getInstance(application);
        authManager = AuthManager.getInstance();

        // Initialize with default values or load from repository
        username.setValue("User");
        email.setValue("user@example.com");

        // Load user profile data
        loadUserProfile();
    }

    public void refreshUserData() {
        isRefreshing.setValue(true);
        
        // Clear cached data
        recentlyLikedTemplates = null;
        recentlyFavoritedTemplates = null;
        
        // Reload user profile
        loadUserProfile();
        
        // Reload templates
        loadLikedTemplates();
        loadFavoriteTemplates();
        
        // Notify refresh complete after a short delay
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            isRefreshing.setValue(false);
        }, 1000);
    }
    
    public LiveData<Boolean> isRefreshing() {
        return isRefreshing;
    }

    private void loadUserProfile() {
        FirebaseUser currentUser = authManager.getCurrentUser();
        if (currentUser == null) {
            Log.e("ProfileViewModel", "No authenticated user found");
            return;
        }

        String uid = currentUser.getUid();
        userRepository.getUserProfile(uid).observeForever(user -> {
            if (user != null) {
                username.setValue(user.getDisplayName() != null ? user.getDisplayName() : "User");
                email.setValue(user.getEmail() != null ? user.getEmail() : "");
                profilePhoto.setValue(user.getProfilePhoto());
            } else {
                // Fallback to Firebase user data if MongoDB data is not available
                username.setValue(currentUser.getDisplayName() != null ? 
                               currentUser.getDisplayName() : "User");
                email.setValue(currentUser.getEmail() != null ? 
                            currentUser.getEmail() : "");
                profilePhoto.setValue(currentUser.getPhotoUrl() != null ? 
                                   currentUser.getPhotoUrl().toString() : null);
            }
        });
    }

    public LiveData<String> getUsername() {
        return username;
    }

    public LiveData<String> getEmail() {
        return email;
    }

    public LiveData<String> getProfilePhoto() {
        return profilePhoto;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public void updateProfile(String username, String email) {
        if (username == null || username.trim().isEmpty()) {
            errorMessage.setValue("Username cannot be empty");
            return;
        }

        // Update UI immediately
        this.username.setValue(username);
        this.email.setValue(email);

        // Update in Firebase and MongoDB
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser != null) {
            // Update Firebase profile
            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(username)
                .build();

            firebaseUser.updateProfile(profileUpdates)
                .addOnSuccessListener(aVoid -> {
                    // After Firebase update, sync with MongoDB and local cache
                    userRepository.updateUserProfile(username, email);
                    errorMessage.setValue(null); // Clear any previous errors
                })
                .addOnFailureListener(e -> {
                    Log.e("ProfileViewModel", "Error updating Firebase profile: " + e.getMessage());
                    errorMessage.setValue("Failed to update profile: " + e.getMessage());
                });
        } else {
            errorMessage.setValue("No user is currently signed in");
        }
    }

    /**
     * Get the user's most recently liked templates
     * @param limit Maximum number of templates to return
     * @return LiveData containing a list of the user's most recently liked templates
     */
    public LiveData<List<Template>> getMostRecentlyLikedTemplates(int limit) {
        if (recentlyLikedTemplates == null) {
            recentlyLikedTemplates = templateRepository.getMostRecentlyLikedTemplates(limit);
        }
        return recentlyLikedTemplates;
    }

    /**
     * Get the user's most recently liked templates with default limit
     * @return LiveData containing a list of the user's most recently liked templates
     */
    public LiveData<List<Template>> getMostRecentlyLikedTemplates() {
        return getMostRecentlyLikedTemplates(DEFAULT_TEMPLATE_LIMIT);
    }

    /**
     * Get the user's most recently favorited templates
     * @param limit Maximum number of templates to return
     * @return LiveData containing a list of the user's most recently favorited templates
     */
    public LiveData<List<Template>> getMostRecentlyFavoritedTemplates(int limit) {
        if (recentlyFavoritedTemplates == null) {
            recentlyFavoritedTemplates = templateRepository.getMostRecentlyFavoritedTemplates(limit);
        }
        return recentlyFavoritedTemplates;
    }

    /**
     * Get the user's most recently favorited templates with default limit
     * @return LiveData containing a list of the user's most recently favorited templates
     */
    public LiveData<List<Template>> getMostRecentlyFavoritedTemplates() {
        return getMostRecentlyFavoritedTemplates(DEFAULT_TEMPLATE_LIMIT);
    }

    /**
     * Get the count of user's posts
     * @return LiveData containing the count of user's posts
     */
    public LiveData<Integer> getPostsCount() {
        return templateRepository.getUserTemplatesCount();
    }

    /**
     * Get the count of user's likes
     * @return LiveData containing the count of user's likes
     */
    public LiveData<Integer> getLikesCount() {
        return templateRepository.getUserLikesCount();
    }

    /**
     * Get the count of user's favorites
     * @return LiveData containing the count of user's favorites
     */
    public LiveData<Integer> getFavoritesCount() {
        return templateRepository.getUserFavoritesCount();
    }

    /**
     * Get the user's liked templates
     */
    public LiveData<List<Template>> getLikedTemplates() {
        if (likedTemplates.getValue() == null) {
            loadLikedTemplates();
        }
        return likedTemplates;
    }

    /**
     * Get the user's favorite templates
     */
    public LiveData<List<Template>> getFavoriteTemplates() {
        if (favoriteTemplates.getValue() == null) {
            loadFavoriteTemplates();
        }
        return favoriteTemplates;
    }

    /**
     * Load the user's liked templates
     */
    private void loadLikedTemplates() {
        // For now, use empty list as placeholder
        // In a real app, this would fetch from repository
        likedTemplates.setValue(Collections.emptyList());
    }

    /**
     * Load the user's favorite templates
     */
    private void loadFavoriteTemplates() {
        // For now, use empty list as placeholder
        // In a real app, this would fetch from repository
        favoriteTemplates.setValue(Collections.emptyList());
    }
}
