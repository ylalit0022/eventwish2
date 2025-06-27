package com.ds.eventwish.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.ds.eventwish.data.auth.AuthManager;
import com.ds.eventwish.data.model.User;
import com.ds.eventwish.data.repository.UserRepository;
import com.google.firebase.auth.FirebaseUser;

import java.lang.ref.WeakReference;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Singleton class to manage and cache current user data for template rendering
 * Provides efficient access to user name and profile photo for HTML placeholder replacement
 */
public class UserDataManager {
    private static final String TAG = "UserDataManager";
    private static UserDataManager instance;
    private static final Object LOCK = new Object();
    
    // Dependencies
    private WeakReference<Context> contextRef;
    private UserRepository userRepository;
    private AuthManager authManager;
    
    // Cached user data
    private final MutableLiveData<User> currentUser = new MutableLiveData<>();
    private String cachedUserName;
    private String cachedUserPhoto;
    private String cachedUserUid;
    private boolean isLoading = false;
    
    // Threading
    private final ExecutorService backgroundExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    
    // Observer for user data changes
    private Observer<User> userObserver;
    
    private UserDataManager(Context context) {
        this.contextRef = new WeakReference<>(context.getApplicationContext());
        this.userRepository = UserRepository.getInstance(context);
        this.authManager = AuthManager.getInstance();
        
        // Initialize with default values
        this.cachedUserName = "User";
        this.cachedUserPhoto = null;
        
        // Start loading user data
        loadUserData();
    }
    
    /**
     * Get singleton instance of UserDataManager
     */
    public static UserDataManager getInstance(Context context) {
        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    instance = new UserDataManager(context);
                }
            }
        }
        return instance;
    }
    
    /**
     * Get LiveData for current user
     */
    public LiveData<User> getCurrentUser() {
        return currentUser;
    }
    
    /**
     * Get cached user name for immediate use
     */
    public String getCachedUserName() {
        return cachedUserName != null ? cachedUserName : "User";
    }
    
    /**
     * Get cached user profile photo URL for immediate use
     */
    public String getCachedUserPhoto() {
        return cachedUserPhoto;
    }
    
    /**
     * Get cached user UID
     */
    public String getCachedUserUid() {
        return cachedUserUid;
    }
    
    /**
     * Check if user data is currently being loaded
     */
    public boolean isLoading() {
        return isLoading;
    }
    
    /**
     * Refresh user data from repository
     */
    public void refreshUserData() {
        Log.d(TAG, "Refreshing user data");
        loadUserData();
    }
    
    /**
     * Load user data from repository
     */
    private void loadUserData() {
        if (isLoading) {
            Log.d(TAG, "User data already loading, skipping");
            return;
        }
        
        FirebaseUser firebaseUser = authManager.getCurrentUser();
        if (firebaseUser == null) {
            Log.w(TAG, "No authenticated user found");
            setDefaultUserData();
            return;
        }
        
        String uid = firebaseUser.getUid();
        if (uid.equals(cachedUserUid)) {
            Log.d(TAG, "User data already loaded for UID: " + uid);
            return;
        }
        
        isLoading = true;
        cachedUserUid = uid;
        
        backgroundExecutor.execute(() -> {
            try {
                Log.d(TAG, "Loading user data for UID: " + uid);
                
                // Remove previous observer if exists
                if (userObserver != null) {
                    mainHandler.post(() -> {
                        LiveData<User> previousUserData = userRepository.getUserProfile(cachedUserUid);
                        if (previousUserData != null) {
                            previousUserData.removeObserver(userObserver);
                        }
                    });
                }
                
                // Get user profile data
                mainHandler.post(() -> {
                    LiveData<User> userData = userRepository.getUserProfile(uid);
                    
                    userObserver = new Observer<User>() {
                        @Override
                        public void onChanged(User user) {
                            backgroundExecutor.execute(() -> {
                                processUserData(user, firebaseUser);
                            });
                        }
                    };
                    
                    userData.observeForever(userObserver);
                });
                
            } catch (Exception e) {
                Log.e(TAG, "Error loading user data", e);
                mainHandler.post(() -> {
                    isLoading = false;
                    setFallbackUserData(firebaseUser);
                });
            }
        });
    }
    
    /**
     * Process loaded user data
     */
    private void processUserData(User user, FirebaseUser firebaseUser) {
        try {
            if (user != null) {
                // Use MongoDB user data
                String displayName = user.getDisplayName();
                String profilePhoto = user.getProfilePhoto();
                
                cachedUserName = (displayName != null && !displayName.trim().isEmpty()) 
                    ? displayName.trim() : "User";
                cachedUserPhoto = (profilePhoto != null && !profilePhoto.trim().isEmpty()) 
                    ? profilePhoto.trim() : null;
                
                Log.d(TAG, "User data loaded from MongoDB - Name: " + cachedUserName + 
                          ", Photo: " + (cachedUserPhoto != null ? "Available" : "None"));
                
                mainHandler.post(() -> {
                    currentUser.setValue(user);
                    isLoading = false;
                });
                
            } else {
                // Fallback to Firebase user data
                Log.d(TAG, "No MongoDB user data, using Firebase fallback");
                setFallbackUserData(firebaseUser);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing user data", e);
            mainHandler.post(() -> {
                isLoading = false;
                setFallbackUserData(firebaseUser);
            });
        }
    }
    
    /**
     * Set fallback user data from Firebase
     */
    private void setFallbackUserData(FirebaseUser firebaseUser) {
        try {
            if (firebaseUser != null) {
                String displayName = firebaseUser.getDisplayName();
                String photoUrl = firebaseUser.getPhotoUrl() != null ? 
                    firebaseUser.getPhotoUrl().toString() : null;
                
                cachedUserName = (displayName != null && !displayName.trim().isEmpty()) 
                    ? displayName.trim() : "User";
                cachedUserPhoto = (photoUrl != null && !photoUrl.trim().isEmpty()) 
                    ? photoUrl.trim() : null;
                
                Log.d(TAG, "Fallback user data set - Name: " + cachedUserName + 
                          ", Photo: " + (cachedUserPhoto != null ? "Available" : "None"));
                
                // Create a temporary User object for LiveData
                User tempUser = new User();
                tempUser.setDisplayName(cachedUserName);
                tempUser.setProfilePhoto(cachedUserPhoto);
                tempUser.setUid(firebaseUser.getUid());
                
                mainHandler.post(() -> {
                    currentUser.setValue(tempUser);
                    isLoading = false;
                });
            } else {
                setDefaultUserData();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting fallback user data", e);
            setDefaultUserData();
        }
    }
    
    /**
     * Set default user data when no user is available
     */
    private void setDefaultUserData() {
        cachedUserName = "User";
        cachedUserPhoto = null;
        cachedUserUid = null;
        
        Log.d(TAG, "Default user data set");
        
        mainHandler.post(() -> {
            currentUser.setValue(null);
            isLoading = false;
        });
    }
    
    /**
     * Check if user has profile photo
     */
    public boolean hasProfilePhoto() {
        return cachedUserPhoto != null && !cachedUserPhoto.trim().isEmpty();
    }
    
    /**
     * Get display name with fallback
     */
    public String getDisplayNameWithFallback() {
        String name = getCachedUserName();
        return (name != null && !name.trim().isEmpty()) ? name : "User";
    }
    
    /**
     * Clean up resources
     */
    public void cleanup() {
        Log.d(TAG, "Cleaning up UserDataManager");
        
        if (userObserver != null && cachedUserUid != null) {
            Context context = contextRef.get();
            if (context != null) {
                try {
                    LiveData<User> userData = userRepository.getUserProfile(cachedUserUid);
                    if (userData != null) {
                        userData.removeObserver(userObserver);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error removing observer during cleanup", e);
                }
            }
            userObserver = null;
        }
        
        if (!backgroundExecutor.isShutdown()) {
            backgroundExecutor.shutdown();
        }
        
        contextRef.clear();
    }
    
    /**
     * For debugging - get current state
     */
    public String getDebugInfo() {
        return "UserDataManager{" +
                "cachedUserName='" + cachedUserName + '\'' +
                ", hasPhoto=" + hasProfilePhoto() +
                ", isLoading=" + isLoading +
                ", cachedUserUid='" + cachedUserUid + '\'' +
                '}';
    }
} 