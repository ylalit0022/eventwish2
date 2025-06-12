package com.ds.eventwish.data.repository;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.ds.eventwish.data.auth.AuthManager;
import com.ds.eventwish.data.model.response.BaseResponse;
import com.ds.eventwish.data.remote.ApiClient;
import com.ds.eventwish.data.remote.ApiService;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseUser;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import com.ds.eventwish.data.local.AppDatabase;
import com.ds.eventwish.data.local.dao.CategoryClickDao;
import com.ds.eventwish.data.local.dao.UserDao;
import com.ds.eventwish.data.local.entity.CategoryClickEntity;
import com.ds.eventwish.data.local.entity.UserEntity;
import com.ds.eventwish.data.model.User;
import com.ds.eventwish.util.AppExecutors;

/**
 * Repository class for managing user registration and activity tracking
 */
public class UserRepository {
    private static final String TAG = "UserRepository";
    private static final String PREF_DEVICE_ID = "device_id";
    private static final String PREF_USER_REGISTERED = "user_registered";
    private static final String PREF_LAST_CATEGORY_VISIT = "last_category_visit";
    private static final String PREF_LAST_ACTIVITY_UPDATE = "last_activity_update";
    private static final String PREF_NAME = "user_prefs";
    private static final String KEY_USER_ID = "user_id";
    
    // Minimum time between activity updates (5 minutes)
    private static final long MIN_ACTIVITY_UPDATE_INTERVAL = 5 * 60 * 1000;
    
    private ApiService apiService;
    private final Context context;
    private final SharedPreferences prefs;
    private final AuthManager authManager;
    
    private final MutableLiveData<Boolean> isRegistering = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isUpdatingActivity = new MutableLiveData<>(false);
    
    private static volatile UserRepository instance;
    
    /**
     * Get singleton instance of UserRepository
     * @param context Application context
     * @return UserRepository instance
     */
    public static synchronized UserRepository getInstance(Context context) {
        if (instance == null) {
            synchronized (UserRepository.class) {
                if (instance == null) {
                    instance = new UserRepository(context.getApplicationContext());
                }
            }
        }
        return instance;
    }
    
    /**
     * Private constructor
     * @param context Application context
     */
    private UserRepository(Context context) {
        this.context = context;
        this.authManager = AuthManager.getInstance();
        
        // Initialize apiService as null initially to avoid compiler error
        this.apiService = null;
        
        // Ensure SecureTokenManager is initialized before ApiClient
        try {
            // Initialize SecureTokenManager if not already done
            com.ds.eventwish.util.SecureTokenManager.init(context);
            
            // Initialize ApiClient
            ApiClient.init(context);
            this.apiService = ApiClient.getInstance();
        } catch (Exception e) {
            Log.e(TAG, "Error initializing ApiClient: " + e.getMessage());
        }
        
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        
        // Initialize device ID if not already set
        if (!prefs.contains(PREF_DEVICE_ID)) {
            generateAndSaveDeviceId();
        }
        
        // Create a dummy user if needed
        createDummyUserIfNeeded();
    }
    
    /**
     * Get current Firebase user ID
     * @return String user ID or null if not signed in
     */
    @Nullable
    public String getCurrentUserId() {
        FirebaseUser user = authManager.getCurrentUser();
        return user != null ? user.getUid() : null;
    }
    
    /**
     * Check if user is signed in
     * @return boolean indicating if user is signed in
     */
    public boolean isSignedIn() {
        return authManager.isSignedIn();
    }
    
    /**
     * Generate a unique device ID and save it in SharedPreferences
     */
    private void generateAndSaveDeviceId() {
        String deviceId;
        
        // Try to use Android ID first (most devices)
        String androidId = Settings.Secure.getString(
                context.getContentResolver(), Settings.Secure.ANDROID_ID);
        
        if (androidId != null && !androidId.isEmpty() && !"9774d56d682e549c".equals(androidId)) {
            // Android ID is available and not the known fake value on some devices
            deviceId = androidId;
        } else {
            // Fallback to generated UUID + device info
            deviceId = UUID.randomUUID().toString() + 
                    "_" + Build.MANUFACTURER + "_" + Build.MODEL;
            
            // Hash the deviceId for privacy
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] hash = digest.digest(deviceId.getBytes());
                StringBuilder hexString = new StringBuilder();
                
                for (byte b : hash) {
                    String hex = Integer.toHexString(0xff & b);
                    if (hex.length() == 1) hexString.append('0');
                    hexString.append(hex);
                }
                
                deviceId = hexString.toString();
            } catch (NoSuchAlgorithmException e) {
                Log.e(TAG, "Error hashing device ID", e);
            }
        }
        
        // Save the deviceId
        prefs.edit().putString(PREF_DEVICE_ID, deviceId).apply();
        Log.d(TAG, "Generated and saved new device ID: " + deviceId);
    }
    
    /**
     * Get the device ID
     * @return Device ID string
     */
    public String getDeviceId() {
        return prefs.getString(PREF_DEVICE_ID, null);
    }
    
    /**
     * Check if user is registered
     * @return true if registered, false otherwise
     */
    public boolean isUserRegistered() {
        return prefs.getBoolean(PREF_USER_REGISTERED, false);
    }
    
    /**
     * Register user with the server (if not already registered)
     */
    public void registerUserIfNeeded() {
        // Skip if already registered or currently registering
        if (isUserRegistered() || Boolean.TRUE.equals(isRegistering.getValue())) {
            return;
        }
        
        final String deviceId = getDeviceId();
        if (deviceId == null || deviceId.isEmpty()) {
            Log.e(TAG, "Cannot register user: Device ID is null or empty");
            return;
        }
        
        // Use postValue instead of setValue for background thread safety
        isRegistering.postValue(true);
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("deviceId", deviceId);
        
        apiService.registerDeviceUser(requestBody).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // Mark user as registered
                    prefs.edit().putBoolean(PREF_USER_REGISTERED, true).apply();
                    Log.d(TAG, "User registration successful: " + deviceId);
                } else {
                    Log.e(TAG, "User registration failed: " + response.code() + " " + 
                            (response.errorBody() != null ? response.errorBody().toString() : ""));
                }
                
                // Reset registering state
                isRegistering.postValue(false);
            }
            
            @Override
            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                Log.e(TAG, "User registration request failed", t);
                
                // Reset registering state
                isRegistering.postValue(false);
            }
        });
    }
    
    /**
     * Update user activity
     * @param category Optional category name (if a category was visited)
     */
    public void updateUserActivity(@Nullable String category) {
        // Skip if not registered or currently updating
        if (!isUserRegistered() || Boolean.TRUE.equals(isUpdatingActivity.getValue())) {
            if (!isUserRegistered()) {
                Log.d(TAG, "Cannot update activity: User not registered");
            }
            return;
        }
        
        final String deviceId = getDeviceId();
        if (deviceId == null || deviceId.isEmpty()) {
            Log.e(TAG, "Cannot update user activity: Device ID is null or empty");
            isUpdatingActivity.postValue(false);
            return;
        }
        
        // Check if sufficient time has passed since last update
        long lastUpdate = prefs.getLong(PREF_LAST_ACTIVITY_UPDATE, 0);
        long now = System.currentTimeMillis();
        
        // For category visits, also store the last visited category
        if (category != null) {
            String lastCategory = prefs.getString(PREF_LAST_CATEGORY_VISIT, null);
            long timeSinceLastUpdate = now - lastUpdate;
            
            // Rate limit category updates to prevent spam
            if (lastCategory != null && lastCategory.equals(category) && 
                    timeSinceLastUpdate < MIN_ACTIVITY_UPDATE_INTERVAL) {
                Log.d(TAG, "Skipping category update (rate limited): " + category);
                return;
            }
            
            // Save this category as last visited
            prefs.edit().putString(PREF_LAST_CATEGORY_VISIT, category).apply();
        } else {
            // For regular activity updates (no category), rate limit
            if (now - lastUpdate < MIN_ACTIVITY_UPDATE_INTERVAL) {
                Log.d(TAG, "Skipping activity update (rate limited)");
                return;
            }
        }
        
        // Use postValue instead of setValue for background thread safety
        isUpdatingActivity.postValue(true);
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("deviceId", deviceId);
        
        if (category != null && !category.isEmpty()) {
            requestBody.put("category", category);
            requestBody.put("source", "direct");
        }
        
        // Get auth token - use empty string as we're using device ID for legacy authentication
        String authToken = "";
        
        apiService.updateUserActivity(requestBody, authToken).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                if (response.isSuccessful()) {
                    // Save last update time
                    prefs.edit().putLong(PREF_LAST_ACTIVITY_UPDATE, System.currentTimeMillis()).apply();
                    
                    Log.d(TAG, "User activity update successful: " + deviceId + 
                            (category != null ? ", category: " + category : ""));
                } else {
                    Log.e(TAG, "User activity update failed: " + response.code() + " " + 
                            (response.errorBody() != null ? response.errorBody().toString() : ""));
                }
                
                // Reset updating state
                isUpdatingActivity.postValue(false);
            }
            
            @Override
            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                Log.e(TAG, "User activity update request failed", t);
                
                // Reset updating state
                isUpdatingActivity.postValue(false);
            }
        });
    }
    
    /**
     * Record a template view with its category
     * @param templateId The ID of the template that was viewed
     * @param category The category the template belongs to
     */
    public void recordTemplateView(String templateId, String category) {
        // Skip if not registered or currently updating
        if (!isUserRegistered()) {
            Log.d(TAG, "Cannot record template view: User not registered");
            return;
        }
        
        final String deviceId = getDeviceId();
        if (deviceId == null || deviceId.isEmpty() || templateId == null || category == null) {
            Log.e(TAG, "Cannot record template view: Missing required data");
            return;
        }
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("deviceId", deviceId);
        requestBody.put("templateId", templateId);
        requestBody.put("category", category);
        
        // Get auth token - use empty string as we're using device ID for legacy authentication
        String authToken = "";
        
        apiService.recordTemplateView(requestBody, authToken).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "Template view recorded: " + templateId + " in category: " + category);
                } else {
                    Log.e(TAG, "Failed to record template view: " + response.code() + " " + 
                            (response.errorBody() != null ? response.errorBody().toString() : ""));
                }
            }
            
            @Override
            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                Log.e(TAG, "Template view request failed", t);
            }
        });
    }
    
    /**
     * Track a template view - convenience method that calls recordTemplateView
     * @param templateId The ID of the template that was viewed
     * @param category The category the template belongs to
     */
    public void trackTemplateView(String templateId, String category) {
        // Record template view on server
        recordTemplateView(templateId, category);
        
        // Also track the category click locally
        if (category != null && !category.isEmpty()) {
            trackCategoryClick(category);
        }
    }
    
    /**
     * Get personalized recommendations for the user
     * @param callback Callback to receive recommendations
     */
    public void getRecommendations(final RecommendationsCallback callback) {
        // Skip if not registered
        if (!isUserRegistered()) {
            Log.d(TAG, "Cannot get recommendations: User not registered");
            if (callback != null) {
                callback.onFailure("User not registered");
            }
            return;
        }
        
        final String deviceId = getDeviceId();
        if (deviceId == null || deviceId.isEmpty()) {
            Log.e(TAG, "Cannot get recommendations: Device ID is null or empty");
            if (callback != null) {
                callback.onFailure("Invalid device ID");
            }
            return;
        }
        
        // Get auth token - use empty string as we're using device ID for legacy authentication
        String authToken = "";
        
        apiService.getUserRecommendations(deviceId, authToken).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject data = response.body();
                    Log.d(TAG, "Recommendations received: " + data);
                    if (callback != null) {
                        callback.onSuccess(data);
                    }
                } else {
                    Log.e(TAG, "Failed to get recommendations: " + response.code());
                    if (callback != null) {
                        callback.onFailure("Failed to get recommendations: " + response.code());
                    }
                }
            }
            
            @Override
            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                Log.e(TAG, "Recommendations request failed", t);
                if (callback != null) {
                    callback.onFailure("Request failed: " + t.getMessage());
                }
            }
        });
    }
    
    /**
     * Callback interface for recommendations
     */
    public interface RecommendationsCallback {
        void onSuccess(JsonObject recommendations);
        void onFailure(String errorMessage);
    }
    
    /**
     * Get registration status LiveData
     */
    public LiveData<Boolean> getRegistrationStatus() {
        return isRegistering;
    }
    
    /**
     * Get activity update status LiveData
     */
    public LiveData<Boolean> getActivityUpdateStatus() {
        return isUpdatingActivity;
    }
    
    /**
     * Track category click for the current user
     * @param category Category name that was clicked
     */
    public void trackCategoryClick(String category) {
        if (category == null || category.isEmpty() || "All".equalsIgnoreCase(category)) {
            Log.d(TAG, "Skipping category click tracking for null, empty, or 'All' category");
            return;
        }
        
        // Update server-side activity tracking
        updateUserActivity(category);
        
        // Run all database operations on a background thread
        AppExecutors.getInstance().diskIO().execute(() -> {
            try {
                // Get the current authenticated user
                UserEntity currentUser = getUserEntityFromDatabase();
                if (currentUser == null) {
                    Log.e(TAG, "Cannot track category click: No authenticated user found");
                    return;
                }
                
                // Get AppDatabase instance
                AppDatabase db = AppDatabase.getInstance(context);
                CategoryClickDao categoryClickDao = db.categoryClickDao();
                
                // Check if this category click already exists
                CategoryClickEntity existingClick = categoryClickDao.getByUserAndCategory(
                        currentUser.getUid(), category);
                
                if (existingClick != null) {
                    // Increment click count and update last clicked time
                    existingClick.incrementClickCount();
                    int updatedRows = categoryClickDao.update(existingClick);
                    Log.d(TAG, "Updated category click count for '" + category + "' to " + 
                            existingClick.getClickCount() + ", rows updated: " + updatedRows);
                } else {
                    // Create new category click
                    CategoryClickEntity newClick = new CategoryClickEntity(
                            currentUser.getUid(), category);
                    long rowId = categoryClickDao.insert(newClick);
                    Log.d(TAG, "Inserted new category click for '" + category + "' with ID: " + rowId);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error tracking category click", e);
            }
        });
    }
    
    /**
     * Get top clicked categories for the current user
     * @param limit Maximum number of categories to return
     * @return List of category click entities
     */
    public List<CategoryClickEntity> getTopClickedCategories(int limit) {
        UserEntity currentUser = getUserEntityFromDatabase();
        if (currentUser == null) {
            Log.e(TAG, "Cannot get top clicked categories: No authenticated user found");
            return new ArrayList<>();
        }
        
        try {
            AppDatabase db = AppDatabase.getInstance(context);
            CategoryClickDao categoryClickDao = db.categoryClickDao();
            return categoryClickDao.getTopCategoriesByUser(currentUser.getUid(), limit);
        } catch (Exception e) {
            Log.e(TAG, "Error getting top clicked categories", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Get all category clicks for the current user
     * @return LiveData list of category click entities
     */
    public LiveData<List<CategoryClickEntity>> getCategoryClicksLive() {
        UserEntity currentUser = getUserEntityFromDatabase();
        if (currentUser == null) {
            Log.e(TAG, "Cannot get category clicks: No authenticated user found");
            return new MutableLiveData<>(new ArrayList<>());
        }
        
        try {
            AppDatabase db = AppDatabase.getInstance(context);
            CategoryClickDao categoryClickDao = db.categoryClickDao();
            return categoryClickDao.getAllByUserLive(currentUser.getUid());
        } catch (Exception e) {
            Log.e(TAG, "Error getting category clicks", e);
            return new MutableLiveData<>(new ArrayList<>());
        }
    }
    
    /**
     * Get total clicks for the current user
     * @return Total number of clicks
     */
    public int getTotalCategoryClicks() {
        UserEntity currentUser = getUserEntityFromDatabase();
        if (currentUser == null) {
            Log.e(TAG, "Cannot get total clicks: No authenticated user found");
            return 0;
        }
        
        try {
            AppDatabase db = AppDatabase.getInstance(context);
            CategoryClickDao categoryClickDao = db.categoryClickDao();
            return categoryClickDao.getTotalClicksByUser(currentUser.getUid());
        } catch (Exception e) {
            Log.e(TAG, "Error getting total clicks", e);
            return 0;
        }
    }
    
    /**
     * Get the current authenticated user entity from database
     * @return UserEntity or null if not found
     */
    private UserEntity getUserEntityFromDatabase() {
        try {
            AppDatabase db = AppDatabase.getInstance(context);
            UserDao userDao = db.userDao();
            return userDao.getCurrentUser();
        } catch (Exception e) {
            Log.e(TAG, "Error getting current user", e);
            return null;
        }
    }
    
    /**
     * Create a dummy user if no authenticated user exists
     */
    private void createDummyUserIfNeeded() {
        AppExecutors.getInstance().diskIO().execute(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(context);
                UserDao userDao = db.userDao();
                
                // Check if we have an authenticated user
                UserEntity currentUser = userDao.getCurrentUser();
                
                if (currentUser == null) {
                    // No authenticated user, create a dummy one
                    String deviceId = getDeviceId();
                    if (deviceId != null && !deviceId.isEmpty()) {
                        UserEntity dummyUser = new UserEntity(deviceId);
                        dummyUser.setAuthenticated(true);
                        dummyUser.setLastLoginTime(System.currentTimeMillis());
                        
                        // Insert the user
                        long result = userDao.insert(dummyUser);
                        Log.d(TAG, "Created dummy user with ID: " + deviceId + ", result: " + result);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error creating dummy user", e);
            }
        });
    }

    /**
     * Set the current user ID
     */
    public void setCurrentUserId(String userId) {
        prefs.edit().putString(KEY_USER_ID, userId).apply();
    }

    /**
     * Clear the current user ID (logout)
     */
    public void clearCurrentUserId() {
        prefs.edit().remove(KEY_USER_ID).apply();
    }

    /**
     * Sync user data with MongoDB after successful Firebase authentication
     * @param firebaseUser Firebase user object
     * @return Task representing the operation
     */
    public Task<User> syncUserWithMongoDB(FirebaseUser firebaseUser) {
        if (firebaseUser == null) {
            return Tasks.forException(new IllegalArgumentException("Firebase user cannot be null"));
        }

        Log.d(TAG, "Syncing user with MongoDB: " + firebaseUser.getUid());

        // Create a TaskCompletionSource to convert from Retrofit callback to Task
        TaskCompletionSource<User> taskCompletionSource = new TaskCompletionSource<>();

        // Get device ID
        final String deviceId = getDeviceId();

        // Force refresh the token to make sure it's valid
        firebaseUser.getIdToken(true)
            .addOnSuccessListener(getTokenResult -> {
                String token = getTokenResult.getToken();
                Log.d(TAG, "Got fresh Firebase token for MongoDB sync: " + 
                      (token.length() > 10 ? token.substring(0, 10) + "..." : "invalid") +
                      ", token length: " + token.length());
                
                String authToken = "Bearer " + token;

                // Create data map for MongoDB update
                Map<String, Object> userData = new HashMap<>();
                userData.put("uid", firebaseUser.getUid());
                userData.put("deviceId", deviceId);
                userData.put("displayName", firebaseUser.getDisplayName() != null ? firebaseUser.getDisplayName() : "");
                userData.put("email", firebaseUser.getEmail() != null ? firebaseUser.getEmail() : "");
                userData.put("profilePhoto", firebaseUser.getPhotoUrl() != null ? firebaseUser.getPhotoUrl().toString() : "");
                userData.put("lastOnline", System.currentTimeMillis());

                // Use a background thread for the network operation
                AppExecutors.getInstance().networkIO().execute(() -> {
                    try {
                        // First try the profile update endpoint
                        Call<JsonObject> call = apiService.updateUserProfile(userData, authToken);
                        Response<JsonObject> response = call.execute();

                        if (response.isSuccessful() && response.body() != null) {
                            Log.d(TAG, "Successfully updated user profile in MongoDB");
                            JsonObject responseBody = response.body();
                            
                            // If response doesn't have user data, create it from Firebase user
                            if (!responseBody.has("user")) {
                                JsonObject userObj = new JsonObject();
                                userObj.addProperty("uid", firebaseUser.getUid());
                                userObj.addProperty("email", firebaseUser.getEmail());
                                userObj.addProperty("displayName", firebaseUser.getDisplayName());
                                if (firebaseUser.getPhotoUrl() != null) {
                                    userObj.addProperty("profilePhoto", firebaseUser.getPhotoUrl().toString());
                                }
                                responseBody.add("user", userObj);
                            }

                            // Create User object from response
                            User user = createUserFromJsonObject(responseBody);
                            
                            // Cache the user data locally
                            cacheUserData(user);
                            
                            // Set the current user ID
                            setCurrentUserId(firebaseUser.getUid());

                            // Complete the task with the user object
                            taskCompletionSource.setResult(user);
                        } else {
                            // Log detailed error information for debugging
                            String errorBody = response.errorBody() != null ? response.errorBody().string() : "No error body";
                            Log.e(TAG, "Failed to update user profile in MongoDB: HTTP " + response.code() + 
                                  ", message: " + errorBody);

                            // Try alternative approach if profile update fails
                            tryAlternativeAuthMethods(firebaseUser, userData, authToken, taskCompletionSource);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error syncing with MongoDB", e);
                        tryAlternativeAuthMethods(firebaseUser, userData, authToken, taskCompletionSource);
                    }
                });
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error getting authentication token", e);
                taskCompletionSource.setException(e);
            });

        return taskCompletionSource.getTask();
    }

    /**
     * Try alternative authentication methods if the primary method fails
     */
    private void tryAlternativeAuthMethods(FirebaseUser firebaseUser, Map<String, Object> userData, 
                                         String authToken, TaskCompletionSource<User> taskCompletionSource) {
        try {
            // Try the /auth endpoint which handles first-time authentication
            Call<JsonObject> authCall = apiService.authenticateWithFirebase(userData);
            Response<JsonObject> authResponse = authCall.execute();

            if (authResponse.isSuccessful() && authResponse.body() != null) {
                Log.d(TAG, "Successfully authenticated with Firebase in MongoDB");
                JsonObject responseBody = authResponse.body();

                // Check if this is a new user
                boolean isNewUser = responseBody.has("isNewUser") && 
                                   responseBody.get("isNewUser").getAsBoolean();
                
                Log.d(TAG, isNewUser ? "This is a new user" : "This is an existing user");

                // Create User object from response
                User user = createUserFromJsonObject(responseBody);
                
                // Cache the user data locally
                cacheUserData(user);
                
                // Set the current user ID
                setCurrentUserId(firebaseUser.getUid());

                // Complete the task with the user object
                taskCompletionSource.setResult(user);
            } else {
                // For 404 errors, this is likely because the endpoint doesn't exist yet
                if (authResponse.code() == 404) {
                    Log.w(TAG, "MongoDB endpoint not found (404). This is non-critical and can be ignored.");
                    
                    // Create a basic user object from Firebase data
                    User user = createUserFromFirebaseUser(firebaseUser);
                    taskCompletionSource.setResult(user);
                } else {
                    Log.e(TAG, "Failed to authenticate with Firebase in MongoDB: HTTP " + 
                          authResponse.code() + ", message: " + 
                          (authResponse.errorBody() != null ? authResponse.errorBody().string() : "No error body"));
                    
                    // Create a basic user object from Firebase data as fallback
                    User user = createUserFromFirebaseUser(firebaseUser);
                    taskCompletionSource.setResult(user);
                }
            }
        } catch (Exception authError) {
            Log.e(TAG, "Error trying alternate authentication approach", authError);
            
            // Create a basic user object from Firebase data as fallback
            User user = createUserFromFirebaseUser(firebaseUser);
            taskCompletionSource.setResult(user);
        }
    }

    /**
     * Create a User object from Firebase user data
     */
    private User createUserFromFirebaseUser(FirebaseUser firebaseUser) {
        User user = new User(firebaseUser.getPhoneNumber());
        user.setUid(firebaseUser.getUid());
        user.setDisplayName(firebaseUser.getDisplayName());
        user.setEmail(firebaseUser.getEmail());
        user.setProfilePhoto(firebaseUser.getPhotoUrl() != null ? firebaseUser.getPhotoUrl().toString() : null);
        user.setDeviceId(getDeviceId());
        user.setLastActive(System.currentTimeMillis());
        return user;
    }

    /**
     * Create a User object from JSON response
     */
    private User createUserFromJsonObject(JsonObject responseBody) {
        User user = new User();
        
        if (responseBody.has("user")) {
            JsonObject userObj = responseBody.getAsJsonObject("user");
            
            if (userObj.has("uid")) {
                user.setUid(userObj.get("uid").getAsString());
            }
            
            if (userObj.has("displayName")) {
                user.setDisplayName(userObj.get("displayName").getAsString());
            }
            
            if (userObj.has("email")) {
                user.setEmail(userObj.get("email").getAsString());
            }
            
            if (userObj.has("profilePhoto")) {
                user.setProfilePhoto(userObj.get("profilePhoto").getAsString());
            }
            
            if (userObj.has("deviceId")) {
                user.setDeviceId(userObj.get("deviceId").getAsString());
            } else {
                user.setDeviceId(getDeviceId());
            }
            
            if (userObj.has("lastOnline")) {
                try {
                    JsonElement lastOnlineElement = userObj.get("lastOnline");
                    if (lastOnlineElement.isJsonPrimitive() && lastOnlineElement.getAsJsonPrimitive().isNumber()) {
                        // It's a number (timestamp), parse as long
                        user.setLastActive(lastOnlineElement.getAsLong());
                    } else {
                        // It's probably a date string, use current time as fallback
                        Log.d(TAG, "lastOnline is not a number: " + lastOnlineElement);
                        user.setLastActive(System.currentTimeMillis());
                    }
                } catch (Exception e) {
                    Log.d(TAG, "Error parsing lastOnline: " + e.getMessage());
                    user.setLastActive(System.currentTimeMillis());
                }
            } else {
                user.setLastActive(System.currentTimeMillis());
            }
            
            // Parse likes array if available
            if (userObj.has("likes") && userObj.get("likes").isJsonArray()) {
                List<String> likes = new ArrayList<>();
                for (JsonElement like : userObj.getAsJsonArray("likes")) {
                    likes.add(like.getAsString());
                }
                user.setLikes(likes);
            }
            
            // Parse favorites array if available
            if (userObj.has("favorites") && userObj.get("favorites").isJsonArray()) {
                List<String> favorites = new ArrayList<>();
                for (JsonElement favorite : userObj.getAsJsonArray("favorites")) {
                    favorites.add(favorite.getAsString());
                }
                user.setFavorites(favorites);
            }
            
            // Parse recent templates if available
            if (userObj.has("recentTemplatesUsed") && userObj.get("recentTemplatesUsed").isJsonArray()) {
                List<String> recentTemplates = new ArrayList<>();
                for (JsonElement template : userObj.getAsJsonArray("recentTemplatesUsed")) {
                    recentTemplates.add(template.getAsString());
                }
                user.setRecentTemplatesUsed(recentTemplates);
            }
            
            // Parse user preferences
            if (userObj.has("preferredTheme")) {
                user.setPreferredTheme(userObj.get("preferredTheme").getAsString());
            }
            
            if (userObj.has("preferredLanguage")) {
                user.setPreferredLanguage(userObj.get("preferredLanguage").getAsString());
            }
            
            if (userObj.has("timezone")) {
                user.setTimezone(userObj.get("timezone").getAsString());
            }
        } else {
            // If no user object in response, create a basic one
            user = new User();
            user.setDeviceId(getDeviceId());
            user.setLastActive(System.currentTimeMillis());
        }
        
        return user;
    }

    /**
     * Cache user data locally for offline access
     */
    private void cacheUserData(User user) {
        AppExecutors.getInstance().diskIO().execute(() -> {
            try {
                // Save to Room database
                AppDatabase db = AppDatabase.getInstance(context);
                UserDao userDao = db.userDao();
                
                // Convert User to UserEntity
                UserEntity userEntity = new UserEntity(user.getUid());
                userEntity.setDisplayName(user.getDisplayName());
                userEntity.setEmail(user.getEmail());
                userEntity.setPhotoUrl(user.getProfilePhoto());
                // Store deviceId in phoneNumber field temporarily
                userEntity.setPhoneNumber(user.getDeviceId());
                userEntity.setLastLoginTime(user.getLastActive());
                userEntity.setAuthenticated(true);
                
                // Insert or update user
                userDao.insertOrUpdate(userEntity);
                
                Log.d(TAG, "User data cached locally: " + user.getUid());
            } catch (Exception e) {
                Log.e(TAG, "Error caching user data", e);
            }
        });
    }

    /**
     * Get user profile from MongoDB
     * @param uid User ID (Firebase UID)
     * @return LiveData<User> containing the user profile
     */
    public LiveData<User> getUserProfile(String uid) {
        MutableLiveData<User> result = new MutableLiveData<>();
        
        if (uid == null || uid.isEmpty()) {
            Log.e(TAG, "Cannot get user profile: UID is null or empty");
            result.postValue(null);
            return result;
        }
        
        // First check if we have a cached user
        AppExecutors.getInstance().diskIO().execute(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(context);
                UserDao userDao = db.userDao();
                UserEntity userEntity = userDao.getUserByUid(uid);
                
                if (userEntity != null) {
                    // Convert UserEntity to User
                    User user = new User();
                    user.setUid(userEntity.getUid());
                    user.setDisplayName(userEntity.getDisplayName());
                    user.setEmail(userEntity.getEmail());
                    user.setProfilePhoto(userEntity.getPhotoUrl());
                    user.setDeviceId(userEntity.getPhoneNumber());
                    user.setLastActive(userEntity.getLastLoginTime());
                    
                    // Post the cached user while we fetch from network
                    result.postValue(user);
                }
                
                // Fetch from network regardless of cache state
                fetchUserProfileFromNetwork(uid, result);
            } catch (Exception e) {
                Log.e(TAG, "Error getting cached user", e);
                // Try network if cache fails
                fetchUserProfileFromNetwork(uid, result);
            }
        });
        
        return result;
    }

    /**
     * Fetch user profile from network
     */
    private void fetchUserProfileFromNetwork(String uid, MutableLiveData<User> result) {
        // Get the current Firebase user to get the token
        FirebaseUser firebaseUser = authManager.getCurrentUser();
        
        if (firebaseUser == null) {
            Log.e(TAG, "Cannot fetch user profile: No authenticated Firebase user");
            return;
        }
        
        // Force refresh the token to make sure it's valid
        firebaseUser.getIdToken(true)
            .addOnSuccessListener(getTokenResult -> {
                String token = getTokenResult.getToken();
                Log.d(TAG, "Got fresh Firebase token for user profile: " + 
                      (token.length() > 10 ? token.substring(0, 10) + "..." : "invalid") +
                      ", token length: " + token.length());
                
                String authToken = "Bearer " + token;
                
                // Use a background thread for the network operation
                AppExecutors.getInstance().networkIO().execute(() -> {
                    try {
                        Call<JsonObject> call = apiService.getUserByUid(uid, authToken);
                        Response<JsonObject> response = call.execute();
                        
                        if (response.isSuccessful() && response.body() != null) {
                            JsonObject responseBody = response.body();
                            
                            // Create User object from response
                            User user = createUserFromJsonObject(responseBody);
                            
                            // Cache the user data locally
                            cacheUserData(user);
                            
                            // Post the result
                            result.postValue(user);
                            Log.d(TAG, "Successfully fetched user profile from MongoDB for UID: " + uid);
                        } else {
                            // Get error body for debugging
                            String errorBody = "";
                            try {
                                errorBody = response.errorBody() != null ? response.errorBody().string() : "No error body";
                            } catch (Exception e) {
                                errorBody = "Error reading error body: " + e.getMessage();
                            }
                            
                            Log.e(TAG, "Failed to get user profile: HTTP " + response.code() + 
                                  ", message: " + response.message() +
                                  "\nError body: " + errorBody);
                            // Note: We don't post null here to avoid overwriting cached data
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error fetching user profile", e);
                        // Note: We don't post null here to avoid overwriting cached data
                    }
                });
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error getting authentication token", e);
                // Note: We don't post null here to avoid overwriting cached data
            });
    }
} 