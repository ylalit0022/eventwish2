package com.ds.eventwish.data.repository;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.ds.eventwish.data.auth.AuthManager;
import com.ds.eventwish.data.local.AppDatabase;
import com.ds.eventwish.data.local.dao.CategoryClickDao;
import com.ds.eventwish.data.local.dao.UserDao;
import com.ds.eventwish.data.local.entity.CategoryClickEntity;
import com.ds.eventwish.data.local.entity.UserEntity;
import com.ds.eventwish.data.model.DeviceSession;
import com.ds.eventwish.data.model.User;
import com.ds.eventwish.data.model.response.ApiResponse;
import com.ds.eventwish.data.model.response.SessionsResponse;
import com.ds.eventwish.data.remote.ApiClient;
import com.ds.eventwish.data.remote.ApiService;
import com.ds.eventwish.util.AppExecutors;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import com.ds.eventwish.data.local.AppDatabase;
import com.ds.eventwish.data.local.dao.CategoryClickDao;
import com.ds.eventwish.data.local.entity.CategoryClickEntity;
import com.ds.eventwish.data.local.entity.UserEntity;
import com.ds.eventwish.data.model.User;
import com.ds.eventwish.util.AppExecutors;
import com.ds.eventwish.utils.AuthStateManager;

import java.io.IOException;

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
            this.apiService = ApiClient.getClient();
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
     * Check if there are active sessions on other devices
     * @param userId User ID
     * @param callback Callback to receive result
     */
    public void checkActiveSessionsOnOtherDevices(String userId, ActiveSessionsCallback callback) {
        if (userId == null || userId.isEmpty()) {
            Log.e(TAG, "checkActiveSessionsOnOtherDevices: User ID is null or empty");
            callback.onError("User ID is null or empty");
            return;
        }
        
        // Get current device ID
        final String currentDeviceId = getDeviceId();
        Log.d(TAG, "checkActiveSessionsOnOtherDevices: Checking sessions for user " + userId + " from device " + currentDeviceId);
        
        authManager.getIdToken(new AuthManager.TokenCallback() {
            @Override
            public void onTokenReceived(String token) {
                String authToken = "Bearer " + token;
                Log.d(TAG, "checkActiveSessionsOnOtherDevices: Got auth token, making API call");
                
                Call<ApiResponse<SessionsResponse>> call = apiService.getUserSessions(userId, authToken);
                call.enqueue(new Callback<ApiResponse<SessionsResponse>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<SessionsResponse>> call, 
                                           @NonNull Response<ApiResponse<SessionsResponse>> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                            SessionsResponse sessionsResponse = response.body().getData();
                            if (sessionsResponse != null && sessionsResponse.getSessions() != null) {
                                // Convert to map of device ID -> DeviceSession
                                Map<String, DeviceSession> sessionsMap = new HashMap<>();
                                boolean hasOtherActiveSessions = false;
                                
                                Log.d(TAG, "checkActiveSessionsOnOtherDevices: Got " + sessionsResponse.getSessions().size() + " sessions");
                                
                                for (DeviceSession session : sessionsResponse.getSessions()) {
                                    String deviceId = session.getDeviceId();
                                    // Skip current device
                                    if (!deviceId.equals(currentDeviceId)) {
                                        sessionsMap.put(deviceId, session);
                                        hasOtherActiveSessions = true;
                                        Log.d(TAG, "checkActiveSessionsOnOtherDevices: Found other session on device " + 
                                              deviceId + " (" + session.getDeviceName() + ")");
                                    }
                                }
                                
                                if (hasOtherActiveSessions) {
                                    Log.i(TAG, "checkActiveSessionsOnOtherDevices: Found " + sessionsMap.size() + 
                                          " active sessions on other devices");
                                    callback.onActiveSessions(sessionsMap);
                                } else {
                                    Log.i(TAG, "checkActiveSessionsOnOtherDevices: No active sessions on other devices");
                                    callback.onNoActiveSessions();
                                }
                            } else {
                                Log.i(TAG, "checkActiveSessionsOnOtherDevices: No sessions data available");
                                callback.onNoActiveSessions();
                            }
                        } else {
                            // If the endpoint doesn't exist yet, consider it as no active sessions
                            if (response.code() == 404) {
                                Log.w(TAG, "checkActiveSessionsOnOtherDevices: API endpoint not found (404)");
                                callback.onNoActiveSessions();
                            } else {
                                String errorMessage = "Failed to check active sessions: HTTP " + response.code();
                                Log.e(TAG, errorMessage);
                                callback.onError(errorMessage);
                            }
                        }
                    }
                    
                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<SessionsResponse>> call, @NonNull Throwable t) {
                        Log.e(TAG, "Error checking active sessions", t);
                        callback.onError("Network error: " + t.getMessage());
                    }
                });
            }
            
            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "checkActiveSessionsOnOtherDevices: Authentication error: " + errorMessage);
                callback.onError("Authentication error: " + errorMessage);
            }
        });
    }
    
    /**
     * Callback for active sessions check
     */
    public interface ActiveSessionsCallback {
        void onActiveSessions(Map<String, DeviceSession> sessions);
        void onNoActiveSessions();
        void onError(String errorMessage);
    }
    
    /**
     * Invalidate sessions on other devices
     * @param userId User ID
     * @param deviceId Current device ID
     * @param callback Callback to receive result
     */
    public void invalidateOtherSessions(String userId, String deviceId, SimpleCallback callback) {
        if (userId == null || userId.isEmpty()) {
            Log.e(TAG, "invalidateOtherSessions: User ID is null or empty");
            callback.onError("User ID is null or empty");
            return;
        }
        
        if (deviceId == null || deviceId.isEmpty()) {
            Log.e(TAG, "invalidateOtherSessions: Device ID is null or empty");
            callback.onError("Device ID is null or empty");
            return;
        }

        Log.d(TAG, "invalidateOtherSessions: Invalidating other sessions for user " + userId + " from device " + deviceId);
        
        authManager.getIdToken(new AuthManager.TokenCallback() {
            @Override
            public void onTokenReceived(String token) {
                String authToken = "Bearer " + token;
                Log.d(TAG, "invalidateOtherSessions: Got auth token, making API call");
                
                Map<String, String> body = new HashMap<>();
                body.put("deviceId", deviceId);
                
                Call<ApiResponse<Void>> call = apiService.invalidateOtherSessions(userId, body, authToken);
                call.enqueue(new Callback<ApiResponse<Void>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<Void>> call, 
                                           @NonNull Response<ApiResponse<Void>> response) {
                        if (response.isSuccessful()) {
                            Log.i(TAG, "invalidateOtherSessions: Successfully invalidated other sessions");
                            callback.onSuccess();
                        } else {
                            // If the endpoint doesn't exist yet, consider it as success
                            if (response.code() == 404) {
                                Log.w(TAG, "invalidateOtherSessions: API endpoint not found (404), treating as success");
                                callback.onSuccess();
                            } else {
                                String errorMessage = "Failed to invalidate sessions: HTTP " + response.code();
                                Log.e(TAG, errorMessage);
                                callback.onError(errorMessage);
                            }
                        }
                    }
                    
                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<Void>> call, @NonNull Throwable t) {
                        Log.e(TAG, "Error invalidating sessions", t);
                        callback.onError("Network error: " + t.getMessage());
                    }
                });
            }
            
            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "invalidateOtherSessions: Authentication error: " + errorMessage);
                callback.onError("Authentication error: " + errorMessage);
            }
        });
    }
    
    /**
     * Simple callback interface
     */
    public interface SimpleCallback {
        void onSuccess();
        void onError(String errorMessage);
    }
    
    /**
     * Create a new DeviceSession object for the current device
     * @return User.DeviceSession object
     */
    private User.DeviceSession createDeviceSession() {
        String deviceModel = Build.MODEL;
        String deviceName = getDeviceName();
        String appVersion = getAppVersion();
        String osVersion = Build.VERSION.RELEASE;
        
        return new User.DeviceSession(
                getDeviceId(),
                deviceModel,
                deviceName,
                appVersion,
                osVersion,
                new Date(), // loginTimestamp
                new Date(), // lastActiveTimestamp
                true // isCurrentDevice
        );
    }
    
    /**
     * Simplified user synchronization with MongoDB
     * Uses device-based authentication first, then links with Firebase
     */
    public Task<User> syncUserWithMongoDB(FirebaseUser firebaseUser) {
        if (firebaseUser == null) {
            return Tasks.forException(new IllegalArgumentException("Firebase user cannot be null"));
        }
        
        final TaskCompletionSource<User> taskCompletionSource = new TaskCompletionSource<>();
        
        // First, ensure device user is registered
        if (!isUserRegistered()) {
            registerUserIfNeeded();
        }
        
        // Create a simple user object from Firebase data
        User user = createUserFromFirebaseUser(firebaseUser);
        
        // Store device info in SharedPreferences
        storeCurrentDeviceInfo(getDeviceId(), firebaseUser.getUid());
        
        // Cache user data
        cacheUserData(user);
        
        // Complete the task immediately with the created user
                    taskCompletionSource.setResult(user);
        
        // Optionally try to sync with backend in the background (non-blocking)
        AppExecutors.getInstance().networkIO().execute(() -> {
            tryBackgroundSync(firebaseUser, user);
        });
        
        return taskCompletionSource.getTask();
    }
    
    /**
     * Try to sync user data with backend in the background
     * This is non-blocking and won't affect the user experience if it fails
     */
    private void tryBackgroundSync(FirebaseUser firebaseUser, User user) {
        try {
            // Create minimal user data for background sync
            Map<String, Object> userData = new HashMap<>();
            userData.put("uid", firebaseUser.getUid());
            userData.put("deviceId", getDeviceId());
            userData.put("displayName", firebaseUser.getDisplayName());
            userData.put("email", firebaseUser.getEmail());
            userData.put("profilePhoto", firebaseUser.getPhotoUrl() != null ? firebaseUser.getPhotoUrl().toString() : null);
            userData.put("deviceModel", Build.MODEL);
            userData.put("deviceName", getDeviceName());
            userData.put("appVersion", getAppVersion());
            userData.put("osVersion", Build.VERSION.RELEASE);
            userData.put("loginTimestamp", System.currentTimeMillis());
            
            // Try to update user profile (best effort)
            authManager.getIdToken(new AuthManager.TokenCallback() {
                @Override
                public void onTokenReceived(String token) {
                    String authHeader = "Bearer " + token;
                    
                    Call<JsonObject> call = apiService.updateUserProfile(userData, authHeader);
                    call.enqueue(new Callback<JsonObject>() {
                        @Override
                        public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                            if (response.isSuccessful()) {
                                Log.d(TAG, "Background sync successful for user: " + firebaseUser.getUid());
                } else {
                                Log.w(TAG, "Background sync failed: " + response.code());
                            }
                        }
                        
                        @Override
                        public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                            Log.w(TAG, "Background sync network error: " + t.getMessage());
                        }
                    });
                }
                
                @Override
                public void onError(String errorMessage) {
                    Log.w(TAG, "Background sync auth error: " + errorMessage);
                }
            });
            
        } catch (Exception e) {
            Log.w(TAG, "Background sync exception: " + e.getMessage());
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
        user.setDeviceModel(Build.MANUFACTURER + " " + Build.MODEL);
        user.setDeviceName(getDeviceName());
        user.setAppVersion(getAppVersion());
        user.setOsVersion("Android " + Build.VERSION.RELEASE);
        user.setLastActive(System.currentTimeMillis());
        user.setLoginTimestamp(System.currentTimeMillis());
        
        // Create a device session
        User.DeviceSession session = new User.DeviceSession(
            getDeviceId(),
            Build.MANUFACTURER + " " + Build.MODEL,
            getDeviceName(),
            getAppVersion(),
            "Android " + Build.VERSION.RELEASE
        );
        
        // Add the device session to the user
        user.addDeviceSession(getDeviceId(), session);
        
        // Store current device info
        storeCurrentDeviceInfo(getDeviceId(), firebaseUser.getUid());
        
        // Update session activity in the background
        if (firebaseUser.getUid() != null) {
            updateSessionActivity(firebaseUser.getUid(), getDeviceId(), new SimpleCallback() {
                @Override
                public void onSuccess() {
                    Log.d(TAG, "Session activity updated successfully");
                }
                
                @Override
                public void onError(String errorMessage) {
                    Log.e(TAG, "Failed to update session activity: " + errorMessage);
                }
            });
        }
        
        return user;
    }
    
    /**
     * Get device name (manufacturer + model)
     * @return Device name string
     */
    private String getDeviceName() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        
        if (model.startsWith(manufacturer)) {
            return capitalize(model);
        }
        
        return capitalize(manufacturer) + " " + model;
    }
    
    /**
     * Capitalize first letter of string
     * @param s String to capitalize
     * @return Capitalized string
     */
    private String capitalize(String s) {
        if (s == null || s.length() == 0) {
            return "";
        }
        
        char first = s.charAt(0);
        if (Character.isUpperCase(first)) {
            return s;
        } else {
            return Character.toUpperCase(first) + s.substring(1);
        }
    }
    
    /**
     * Get app version
     * @return App version string
     */
    private String getAppVersion() {
        try {
            return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "Unknown";
        }
    }
    
    /**
     * Store current device information
     * @param deviceId Device ID
     * @param userId User ID
     */
    private void storeCurrentDeviceInfo(String deviceId, String userId) {
        SharedPreferences devicePrefs = context.getSharedPreferences("device_info", Context.MODE_PRIVATE);
        devicePrefs.edit()
            .putString("current_device_id", deviceId)
            .putString("current_user_id", userId)
            .putString("device_model", Build.MANUFACTURER + " " + Build.MODEL)
            .putString("device_name", getDeviceName())
            .putString("os_version", "Android " + Build.VERSION.RELEASE)
            .putString("app_version", getAppVersion())
            .putLong("last_login", System.currentTimeMillis())
            .apply();
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

    /**
     * Get user's active sessions
     * @param userId User ID
     * @param callback Callback to receive result
     */
    public void getUserSessions(String userId, SessionsCallback callback) {
        if (userId == null || userId.isEmpty()) {
            callback.onError("User ID is null or empty");
            return;
        }

        authManager.getIdToken(new AuthManager.TokenCallback() {
            @Override
            public void onTokenReceived(String token) {
                String authToken = "Bearer " + token;
                
                Call<ApiResponse<SessionsResponse>> call = apiService.getUserSessions(userId, authToken);
                call.enqueue(new Callback<ApiResponse<SessionsResponse>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<SessionsResponse>> call, 
                                           @NonNull Response<ApiResponse<SessionsResponse>> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                            SessionsResponse sessionsResponse = response.body().getData();
                            if (sessionsResponse != null && sessionsResponse.getSessions() != null 
                                    && !sessionsResponse.getSessions().isEmpty()) {
                                callback.onSessionsReceived(sessionsResponse.getSessions());
                            } else {
                                callback.onNoSessions();
                            }
                        } else {
                            // If the endpoint doesn't exist yet, consider it as no sessions
                            if (response.code() == 404) {
                                callback.onNoSessions();
                            } else {
                                String errorMessage = "Failed to get sessions: HTTP " + response.code();
                                Log.e(TAG, errorMessage);
                                callback.onError(errorMessage);
                            }
                        }
                    }
                    
                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<SessionsResponse>> call, @NonNull Throwable t) {
                        Log.e(TAG, "Error getting sessions", t);
                        callback.onError("Network error: " + t.getMessage());
                    }
                });
            }
            
            @Override
            public void onError(String errorMessage) {
                callback.onError("Authentication error: " + errorMessage);
            }
        });
    }

    /**
     * Update activity timestamp for a device session
     * @param userId Firebase UID
     * @param deviceId Device ID
     * @param callback Callback to handle the result
     */
    public void updateSessionActivity(String userId, String deviceId, SimpleCallback callback) {
        if (apiService == null) {
            Log.e(TAG, "updateSessionActivity: API service not initialized");
            callback.onError("API service not initialized");
            return;
        }

        if (userId == null || userId.isEmpty()) {
            Log.e(TAG, "updateSessionActivity: User ID is required");
            callback.onError("User ID is required");
            return;
        }

        if (deviceId == null || deviceId.isEmpty()) {
            Log.e(TAG, "updateSessionActivity: Device ID is required");
            callback.onError("Device ID is required");
            return;
        }

        // Use AppExecutors to perform network operation on background thread
        AppExecutors.getInstance().networkIO().execute(() -> {
            try {
                // Create auth header with token
                authManager.getIdToken(new AuthManager.TokenCallback() {
                    @Override
                    public void onTokenReceived(String token) {
                        String authToken = "Bearer " + token;

                        // Create minimal request body without subscription data
                        Map<String, Object> requestBody = new HashMap<>();
                        requestBody.put("deviceId", deviceId);
                        
                        // Log the request
                        Log.d(TAG, "updateSessionActivity: Updating session activity for user " + userId + " on device " + deviceId);

                        // Make API call
                        Call<ApiResponse<Void>> call = apiService.updateSessionActivity(userId, requestBody, authToken);
                        call.enqueue(new Callback<ApiResponse<Void>>() {
                            @Override
                            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                                    Log.d(TAG, "updateSessionActivity: Session activity updated successfully");
                                    callback.onSuccess();
                                } else {
                                    String errorBody = null;
                                    try {
                                        if (response.errorBody() != null) {
                                            errorBody = response.errorBody().string();
                                        }
                                    } catch (IOException e) {
                                        Log.e(TAG, "updateSessionActivity: Error reading error body", e);
                                    }
                                    
                                    if (response.body() != null) {
                                        Log.e(TAG, "updateSessionActivity: " + response.body().toString());
                                        callback.onError(response.body().getMessage());
                                    } else {
                                        Log.e(TAG, "updateSessionActivity: " + errorBody);
                                        callback.onError("Error updating session activity: " + response.code());
                                    }
                                }
                            }

                            @Override
                            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                                Log.e(TAG, "updateSessionActivity: Network error", t);
                                callback.onError("Network error: " + t.getMessage());
                            }
                        });
                    }

                    @Override
                    public void onError(String errorMessage) {
                        Log.e(TAG, "updateSessionActivity: Failed to get ID token: " + errorMessage);
                        callback.onError("Failed to get authentication token: " + errorMessage);
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "updateSessionActivity: Exception", e);
                callback.onError("Exception: " + e.getMessage());
            }
        });
    }

    /**
     * Callback interface for session operations
     */
    public interface SessionsCallback {
        void onSessionsReceived(List<DeviceSession> sessions);
        void onNoSessions();
        void onError(String errorMessage);
    }

    /**
     * Parse user data from API response
     */
    private User parseUserFromResponse(JsonObject jsonResponse, FirebaseUser firebaseUser) {
        try {
            if (jsonResponse.has("user")) {
                JsonObject userObject = jsonResponse.getAsJsonObject("user");
                return createUserFromJsonObject(userObject);
            } else {
                // If no user object in response, create from Firebase data
                return createFallbackUser(firebaseUser);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing user from response", e);
            return createFallbackUser(firebaseUser);
        }
    }
    
    /**
     * Create a fallback user object from Firebase user data
     */
    private User createFallbackUser(FirebaseUser firebaseUser) {
        User user = new User();
        user.setUid(firebaseUser.getUid());
        user.setDisplayName(firebaseUser.getDisplayName());
        user.setEmail(firebaseUser.getEmail());
        user.setPhoneNumber(firebaseUser.getPhoneNumber());
        user.setProfilePhoto(firebaseUser.getPhotoUrl() != null ? firebaseUser.getPhotoUrl().toString() : null);
        user.setDeviceId(getDeviceId());
        user.setDeviceModel(Build.MODEL);
        user.setDeviceName(getDeviceName());
        user.setAppVersion(getAppVersion());
        user.setOsVersion(Build.VERSION.RELEASE);
        user.setLoginTimestamp(System.currentTimeMillis());
        user.setLastActive(System.currentTimeMillis());
        user.setCoins(0);
        user.setUnlocked(false);
        user.setUnlockExpiry(0);
        
        // Create a basic subscription with valid enum values
        User.Subscription subscription = new User.Subscription();
        subscription.setPlan("MONTHLY"); // Valid enum: 'MONTHLY', 'QUARTERLY', 'HALF_YEARLY', 'YEARLY', ''
        subscription.setActive(false);
        user.setSubscription(subscription);
        
        return user;
    }

    /**
     * Update user profile in both Firebase and MongoDB
     * @param displayName New display name
     * @param email New email
     */
    public void updateUserProfile(String displayName, String email) {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) {
            Log.e(TAG, "Cannot update profile: No user logged in");
            return;
        }

        // Create user data for MongoDB update
        Map<String, Object> userData = new HashMap<>();
        userData.put("uid", firebaseUser.getUid());
        userData.put("displayName", displayName);
        userData.put("email", email);
        userData.put("deviceId", getDeviceId());
        userData.put("lastOnline", System.currentTimeMillis());

        // Get Firebase token and update MongoDB
        authManager.getIdToken(new AuthManager.TokenCallback() {
            @Override
            public void onTokenReceived(String token) {
                String authHeader = "Bearer " + token;
                Call<JsonObject> call = apiService.updateUserProfile(userData, authHeader);
                call.enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                        if (response.isSuccessful()) {
                            Log.d(TAG, "Profile updated successfully in MongoDB");
                            // Update local cache
                            updateLocalUserCache(displayName, email);
                        } else {
                            Log.e(TAG, "Failed to update profile in MongoDB: " + response.code());
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                        Log.e(TAG, "Network error updating profile: " + t.getMessage());
                    }
                });
            }

            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "Auth error updating profile: " + errorMessage);
            }
        });
    }

    private void updateLocalUserCache(String displayName, String email) {
        AppExecutors.getInstance().diskIO().execute(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(context);
                UserDao userDao = db.userDao();
                UserEntity userEntity = userDao.getCurrentUser();

                if (userEntity != null) {
                    userEntity.setDisplayName(displayName);
                    userEntity.setEmail(email);
                    userDao.insertOrUpdate(userEntity);
                    Log.d(TAG, "Local user cache updated");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error updating local user cache: " + e.getMessage());
            }
        });
    }
} 