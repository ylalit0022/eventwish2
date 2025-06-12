package com.ds.eventwish.ui.profile;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.ds.eventwish.data.local.AppDatabase;
import com.ds.eventwish.data.local.dao.UserDao;
import com.ds.eventwish.data.local.entity.UserEntity;
import com.ds.eventwish.data.model.Template;
import com.ds.eventwish.data.repository.TemplateRepository;
import com.ds.eventwish.data.repository.UserRepository;
import com.ds.eventwish.util.AppExecutors;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;

public class ProfileViewModel extends AndroidViewModel {
    
    private static final int DEFAULT_TEMPLATE_LIMIT = 5;
    
    private final MutableLiveData<String> username = new MutableLiveData<>();
    private final MutableLiveData<String> email = new MutableLiveData<>();
    private final TemplateRepository templateRepository;
    private final UserRepository userRepository;
    
    private LiveData<List<Template>> recentlyLikedTemplates;
    private LiveData<List<Template>> recentlyFavoritedTemplates;

    public ProfileViewModel(Application application) {
        super(application);
        // Initialize repositories
        TemplateRepository.init(application);
        templateRepository = TemplateRepository.getInstance();
        userRepository = UserRepository.getInstance(application);
        
        // Initialize with default values or load from repository
        username.setValue("User");
        email.setValue("user@example.com");
        
        // Load user profile data
        loadUserProfile();
    }

    private void loadUserProfile() {
        // Try to get user from local database
        AppExecutors.getInstance().diskIO().execute(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(getApplication());
                UserDao userDao = db.userDao();
                UserEntity userEntity = userDao.getCurrentUser();
                
                if (userEntity != null) {
                    // Update UI on main thread
                    new Handler(Looper.getMainLooper()).post(() -> {
                        username.setValue(userEntity.getDisplayName() != null ? 
                                          userEntity.getDisplayName() : "User");
                        email.setValue(userEntity.getEmail() != null ? 
                                      userEntity.getEmail() : "");
                    });
                } else {
                    // Fallback to Firebase user
                    FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
                    if (firebaseUser != null) {
                        new Handler(Looper.getMainLooper()).post(() -> {
                            username.setValue(firebaseUser.getDisplayName() != null ? 
                                             firebaseUser.getDisplayName() : "User");
                            email.setValue(firebaseUser.getEmail() != null ? 
                                         firebaseUser.getEmail() : "");
                        });
                    }
                }
            } catch (Exception e) {
                Log.e("ProfileViewModel", "Error loading user profile: " + e.getMessage());
                // Fallback to default values
                new Handler(Looper.getMainLooper()).post(() -> {
                    username.setValue("User");
                    email.setValue("");
                });
            }
        });
    }

    public LiveData<String> getUsername() {
        return username;
    }

    public LiveData<String> getEmail() {
        return email;
    }

    public void updateProfile(String username, String email) {
        this.username.setValue(username);
        this.email.setValue(email);
        // Save to repository or preferences
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
     * Refresh the user's liked and favorited templates
     */
    public void refreshUserInteractions() {
        recentlyLikedTemplates = templateRepository.getMostRecentlyLikedTemplates(DEFAULT_TEMPLATE_LIMIT);
        recentlyFavoritedTemplates = templateRepository.getMostRecentlyFavoritedTemplates(DEFAULT_TEMPLATE_LIMIT);
    }

    /**
     * Toggle the like state of a template
     * @param templateId ID of the template to toggle like state
     * @param newState The new like state (true = liked, false = not liked)
     */
    public void toggleTemplateLike(String templateId, boolean newState) {
        if (newState) {
            templateRepository.likeTemplate(templateId)
                .addOnSuccessListener(liked -> {
                    // Refresh liked templates after successful like
                    recentlyLikedTemplates = templateRepository.getMostRecentlyLikedTemplates(DEFAULT_TEMPLATE_LIMIT);
                });
        } else {
            templateRepository.unlikeTemplate(templateId)
                .addOnSuccessListener(unliked -> {
                    // Refresh liked templates after successful unlike
                    recentlyLikedTemplates = templateRepository.getMostRecentlyLikedTemplates(DEFAULT_TEMPLATE_LIMIT);
                });
        }
    }

    /**
     * Toggle the favorite state of a template
     * @param templateId ID of the template to toggle favorite state
     * @param newState The new favorite state (true = favorited, false = not favorited)
     */
    public void toggleTemplateFavorite(String templateId, boolean newState) {
        if (newState) {
            templateRepository.favoriteTemplate(templateId)
                .addOnSuccessListener(favorited -> {
                    // Refresh favorited templates after successful favorite
                    recentlyFavoritedTemplates = templateRepository.getMostRecentlyFavoritedTemplates(DEFAULT_TEMPLATE_LIMIT);
                });
        } else {
            templateRepository.unfavoriteTemplate(templateId)
                .addOnSuccessListener(unfavorited -> {
                    // Refresh favorited templates after successful unfavorite
                    recentlyFavoritedTemplates = templateRepository.getMostRecentlyFavoritedTemplates(DEFAULT_TEMPLATE_LIMIT);
                });
        }
    }
} 