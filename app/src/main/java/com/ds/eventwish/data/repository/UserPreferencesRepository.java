package com.ds.eventwish.data.repository;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.ds.eventwish.data.model.NotificationPreference;
import com.ds.eventwish.data.model.UserPreferences;
import com.ds.eventwish.data.remote.FirestoreManager;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.SetOptions;
import com.ds.eventwish.utils.AnalyticsUtils;
import com.ds.eventwish.utils.NetworkUtils;
import com.ds.eventwish.data.converter.UserPreferencesConverter;

import java.io.IOException;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.HashMap;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;

/**
 * Repository class for managing user preferences data
 */
public class UserPreferencesRepository {
    private static final String TAG = "UserPreferencesRepo";
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = 1000; // 1 second initial delay
    private static final long SYNC_INTERVAL_MS = 300000; // 5 minutes
    private static final int MAX_BATCH_SIZE = 50;

    private static volatile UserPreferencesRepository instance;
    private final FirestoreManager firestoreManager;
    private final MutableLiveData<UserPreferences> userPreferences;
    private final MutableLiveData<Boolean> isLoading;
    private final MutableLiveData<String> error;
    private ListenerRegistration preferencesListener;
    private final AnalyticsUtils analyticsUtils;
    private final Handler retryHandler;
    private final NetworkUtils networkUtils;
    private final Handler syncHandler;
    private long lastSyncTimestamp;
    private final Map<String, Long> pendingOperations;
    private boolean isSyncing;
    private final Context context;

    // Private constructor to enforce singleton pattern
    private UserPreferencesRepository(Context context) {
        this.context = context.getApplicationContext();
        firestoreManager = FirestoreManager.getInstance();
        userPreferences = new MutableLiveData<>();
        isLoading = new MutableLiveData<>(false);
        error = new MutableLiveData<>();
        analyticsUtils = AnalyticsUtils.getInstance();
        retryHandler = new Handler(Looper.getMainLooper());
        networkUtils = NetworkUtils.getInstance(context);
        syncHandler = new Handler(Looper.getMainLooper());
        pendingOperations = new ConcurrentHashMap<>();
        startPeriodicSync();
    }

    public static UserPreferencesRepository getInstance(Context context) {
        if (instance == null) {
            synchronized (UserPreferencesRepository.class) {
                if (instance == null) {
                    instance = new UserPreferencesRepository(context);
                }
            }
        }
        return instance;
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

    /**
     * Fetch user preferences from Firestore
     */
    public Task<UserPreferences> fetchUserPreferences() {
        isLoading.setValue(true);
        error.setValue(null);

        return firestoreManager.getUserPreferences()
            .continueWithTask(task -> {
                if (!task.isSuccessful()) {
                    throw task.getException();
                }
                return UserPreferencesConverter.fromDocument(task.getResult());
            })
            .addOnSuccessListener(preferences -> {
                userPreferences.setValue(preferences);
                isLoading.setValue(false);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error fetching user preferences", e);
                error.setValue("Failed to fetch preferences: " + e.getMessage());
                isLoading.setValue(false);
            });
    }

    /**
     * Update user preferences
     */
    public Task<Void> updateUserPreferences(UserPreferences preferences) {
        if (preferences == null) {
            return Tasks.forException(new IllegalArgumentException("Preferences cannot be null"));
        }

        Map<String, Object> data = UserPreferencesConverter.toDocument(preferences);
        data.put("updated_at", firestoreManager.getServerTimestamp());

        return firestoreManager.updateUserPreferences(data)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "User preferences updated successfully");
                Bundle params = new Bundle();
                params.putString("user_id", preferences.getUserId());
                AnalyticsUtils.logEvent("user_preferences_updated", params);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error updating user preferences", e);
                Bundle params = new Bundle();
                params.putString("error", e.getMessage());
                AnalyticsUtils.logEvent("user_preferences_update_failed", params);
            });
    }

    /**
     * Retry an operation with exponential backoff
     * @param operation Operation to retry
     * @param retryCount Current retry count
     * @param maxRetries Maximum number of retries
     * @param initialDelay Initial delay in milliseconds
     */
    private void retryWithBackoff(Runnable operation, int retryCount, int maxRetries, long initialDelay) {
        if (retryCount >= maxRetries) {
            Log.e(TAG, "Max retry attempts reached");
            error.setValue("Operation failed after " + maxRetries + " attempts");
            return;
        }

        long delay = initialDelay * (long) Math.pow(2, retryCount);
        Log.d(TAG, "Scheduling retry attempt " + (retryCount + 1) + " in " + delay + "ms");
        retryHandler.postDelayed(() -> {
            if (networkUtils.isNetworkAvailable()) {
                operation.run();
            } else {
                retryWithBackoff(operation, retryCount + 1, maxRetries, initialDelay);
            }
        }, delay);
    }

    /**
     * Validate user preferences data
     * @param preferences UserPreferences to validate
     * @return true if valid, false otherwise
     */
    private boolean validateUserPreferences(UserPreferences preferences) {
        if (preferences == null) {
            Log.e(TAG, "UserPreferences object is null");
            return false;
        }

        if (preferences.getUserId() == null || preferences.getUserId().isEmpty()) {
            Log.e(TAG, "UserPreferences has invalid user ID");
            return false;
        }

        // Validate notification preferences
        Map<String, NotificationPreference> notificationPrefs = preferences.getNotificationPreferences();
        if (notificationPrefs != null) {
            for (Map.Entry<String, NotificationPreference> entry : notificationPrefs.entrySet()) {
                if (entry.getKey() == null || entry.getValue() == null) {
                    Log.e(TAG, "Invalid notification preference entry");
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Handle network error with retry
     * @param operation Operation to retry
     * @param errorMessage Error message to log
     */
    private void handleNetworkError(Runnable operation, String errorMessage) {
        Log.e(TAG, errorMessage);
        error.setValue(errorMessage);
        
        if (networkUtils.isNetworkAvailable()) {
            // If network is available but operation failed, retry with backoff
            retryWithBackoff(operation, 0, MAX_RETRY_ATTEMPTS, RETRY_DELAY_MS);
        } else {
            // If no network, wait for network and retry
            error.setValue("No network connection. Will retry when connection is available.");
            networkUtils.addNetworkCallback(available -> {
                if (available) {
                    operation.run();
                }
            });
        }
    }

    /**
     * Update notification preferences for a specific type
     */
    public Task<Void> updateNotificationPreference(NotificationPreference preference) {
        if (preference == null) {
            String errorMsg = "Cannot update null notification preference";
            Log.e(TAG, errorMsg);
            error.setValue(errorMsg);
            return Tasks.forException(new IllegalArgumentException(errorMsg));
        }

        isLoading.setValue(true);
        error.setValue(null);

        Runnable updateOperation = () -> {
            firestoreManager.updateNotificationPreferences(preference)
                .addOnSuccessListener(aVoid -> {
                    UserPreferences current = userPreferences.getValue();
                    if (current != null) {
                        current.setNotificationPreference(preference.getType(), preference);
                        if (validateUserPreferences(current)) {
                            userPreferences.setValue(current);
                            
                            // Track notification preference update
                            Bundle params = new Bundle();
                            params.putString("preference_type", preference.getType());
                            params.putBoolean("enabled", preference.isEnabled());
                            params.putString("sound_enabled", String.valueOf(preference.isSoundEnabled()));
                            params.putString("vibration_enabled", String.valueOf(preference.isVibrationEnabled()));
                            analyticsUtils.logEvent("notification_preference_updated", params);
                        } else {
                            String errorMsg = "Invalid user preferences state after update";
                            Log.e(TAG, errorMsg);
                            error.setValue(errorMsg);
                        }
                    }
                    isLoading.setValue(false);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating notification preference", e);
                    handleNetworkError(
                        () -> updateNotificationPreference(preference),
                        "Failed to update notification preference: " + e.getMessage()
                    );
                    isLoading.setValue(false);
                    
                    // Track error
                    Bundle params = new Bundle();
                    params.putString("error_type", "notification_preference_update_failed");
                    params.putString("error_message", e.getMessage());
                    params.putString("preference_type", preference.getType());
                    analyticsUtils.logEvent("notification_preference_error", params);
                });
        };

        if (!networkUtils.isNetworkAvailable()) {
            handleNetworkError(updateOperation, "No network connection available");
            return Tasks.forException(new IOException("No network connection available"));
        }

        updateOperation.run();
        return Tasks.forResult(null);
    }

    /**
     * Start periodic synchronization
     */
    private void startPeriodicSync() {
        syncHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                synchronizeData();
                syncHandler.postDelayed(this, SYNC_INTERVAL_MS);
            }
        }, SYNC_INTERVAL_MS);
    }

    /**
     * Synchronize local data with server
     */
    private synchronized void synchronizeData() {
        if (isSyncing || !networkUtils.isNetworkAvailable()) {
            return;
        }

        isSyncing = true;
        isLoading.setValue(true);

        // Process pending operations in batches
        List<String> pendingOps = new ArrayList<>(pendingOperations.keySet());
        for (int i = 0; i < pendingOps.size(); i += MAX_BATCH_SIZE) {
            int end = Math.min(i + MAX_BATCH_SIZE, pendingOps.size());
            List<String> batch = pendingOps.subList(i, end);
            processPendingOperationsBatch(batch);
        }

        // Update last sync timestamp
        lastSyncTimestamp = System.currentTimeMillis() / 1000;
        handleSyncComplete(null);
    }

    /**
     * Process a batch of pending operations
     */
    private void processPendingOperationsBatch(List<String> operations) {
        WriteBatch batch = firestoreManager.batch();
        for (String operationId : operations) {
            Long timestamp = pendingOperations.get(operationId);
            if (timestamp != null && timestamp > lastSyncTimestamp) {
                // Add operation to batch
                addOperationToBatch(batch, operationId);
            }
        }

        // Commit batch
        batch.commit()
            .addOnSuccessListener(aVoid -> {
                for (String operationId : operations) {
                    pendingOperations.remove(operationId);
                }
            })
            .addOnFailureListener(e -> Log.e(TAG, "Failed to process batch", e));
    }

    /**
     * Add operation to batch
     */
    private void addOperationToBatch(WriteBatch batch, String operationId) {
        String[] parts = operationId.split(":");
        String operation = parts[0];
        String templateId = parts[1];

        Tasks.whenAll(
            firestoreManager.getFavoriteRef(templateId),
            firestoreManager.getLikeRef(templateId)
        ).addOnSuccessListener(voids -> {
            switch (operation) {
                case "favorite_add":
                    batch.set(firestoreManager.getFavoriteRef(templateId).getResult(), 
                        new HashMap<String, Object>() {{
                            put("templateId", templateId);
                            put("timestamp", FieldValue.serverTimestamp());
                        }});
                    break;
                case "favorite_remove":
                    batch.delete(firestoreManager.getFavoriteRef(templateId).getResult());
                    break;
                case "like_add":
                    batch.set(firestoreManager.getLikeRef(templateId).getResult(),
                        new HashMap<String, Object>() {{
                            put("templateId", templateId);
                            put("timestamp", FieldValue.serverTimestamp());
                        }});
                    break;
                case "like_remove":
                    batch.delete(firestoreManager.getLikeRef(templateId).getResult());
                    break;
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error getting document references for batch operation", e);
            error.setValue("Failed to get document references: " + e.getMessage());
        });
    }

    /**
     * Handle completion of sync operation
     */
    private void handleSyncComplete(String errorMessage) {
        isSyncing = false;
        isLoading.setValue(false);
        if (errorMessage != null) {
            error.setValue(errorMessage);
            Log.e(TAG, "Sync error: " + errorMessage);
        }
    }

    /**
     * Add operation to pending queue
     */
    private void addPendingOperation(String operation, String templateId) {
        String operationId = operation + ":" + templateId;
        pendingOperations.put(operationId, System.currentTimeMillis() / 1000);
    }

    /**
     * Handle empty states and provide default values
     */
    private UserPreferences getDefaultPreferences() {
        UserPreferences defaults = new UserPreferences();
        defaults.setUserId(firestoreManager.getCurrentUserId());
        
        // Set default notification preferences
        NotificationPreference templatePrefs = new NotificationPreference();
        templatePrefs.setType("template_updates");
        templatePrefs.setEnabled(true);
        templatePrefs.setSoundEnabled(true);
        templatePrefs.setVibrationEnabled(true);
        defaults.setNotificationPreference("template_updates", templatePrefs);

        NotificationPreference festivalPrefs = new NotificationPreference();
        festivalPrefs.setType("festival_reminders");
        festivalPrefs.setEnabled(true);
        festivalPrefs.setSoundEnabled(true);
        festivalPrefs.setVibrationEnabled(true);
        defaults.setNotificationPreference("festival_reminders", festivalPrefs);

        return defaults;
    }

    public Task<Void> addToFavorites(String templateId) {
        if (templateId == null || templateId.isEmpty()) {
            String errorMsg = "Cannot add null or empty template ID to favorites";
            Log.e(TAG, errorMsg);
            error.setValue(errorMsg);
            return Tasks.forException(new IllegalArgumentException(errorMsg));
        }

        // Add to pending operations
        addPendingOperation("favorite_add", templateId);

        Runnable addOperation = () -> {
            firestoreManager.addToFavorites(templateId)
                .addOnSuccessListener(aVoid -> {
                    UserPreferences current = userPreferences.getValue();
                    if (current == null) {
                        current = getDefaultPreferences();
                    }
                    current.addFavoriteTemplate(templateId);
                    if (validateUserPreferences(current)) {
                        userPreferences.setValue(current);
                        
                        // Track favorite added
                        Bundle params = new Bundle();
                        params.putString("template_id", templateId);
                        params.putString("interaction_type", "favorite_add");
                        analyticsUtils.logEvent("template_interaction", params);
                    } else {
                        String errorMsg = "Invalid user preferences state after adding favorite";
                        Log.e(TAG, errorMsg);
                        error.setValue(errorMsg);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error adding template to favorites", e);
                    handleNetworkError(
                        () -> addToFavorites(templateId),
                        "Failed to add template to favorites: " + e.getMessage()
                    );
                    
                    // Track error
                    Bundle params = new Bundle();
                    params.putString("error_type", "favorite_add_failed");
                    params.putString("error_message", e.getMessage());
                    params.putString("template_id", templateId);
                    analyticsUtils.logEvent("template_interaction_error", params);
                });
        };

        if (!networkUtils.isNetworkAvailable()) {
            handleNetworkError(addOperation, "No network connection available");
            return Tasks.forException(new IOException("No network connection available"));
        }

        addOperation.run();
        return Tasks.forResult(null);
    }

    /**
     * Remove template from favorites
     */
    public Task<Void> removeFromFavorites(String templateId) {
        return firestoreManager.removeFromFavorites(templateId)
                .addOnSuccessListener(aVoid -> {
                    UserPreferences current = userPreferences.getValue();
                    if (current != null) {
                        current.removeFavoriteTemplate(templateId);
                        userPreferences.setValue(current);
                        
                        // Track favorite removed
                        Bundle params = new Bundle();
                        params.putString("template_id", templateId);
                        params.putString("interaction_type", "favorite_remove");
                        analyticsUtils.logEvent("template_interaction", params);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error removing template from favorites", e);
                    // Track error
                    Bundle params = new Bundle();
                    params.putString("error_type", "favorite_remove_failed");
                    params.putString("error_message", e.getMessage());
                    params.putString("template_id", templateId);
                    analyticsUtils.logEvent("template_interaction_error", params);
                });
    }

    /**
     * Add template to likes
     */
    public Task<Void> addToLikes(String templateId) {
        return firestoreManager.addToLikes(templateId)
                .addOnSuccessListener(aVoid -> {
                    UserPreferences current = userPreferences.getValue();
                    if (current != null) {
                        current.addLikedTemplate(templateId);
                        userPreferences.setValue(current);
                        
                        // Track like added
                        Bundle params = new Bundle();
                        params.putString("template_id", templateId);
                        params.putString("interaction_type", "like_add");
                        analyticsUtils.logEvent("template_interaction", params);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error adding template to likes", e);
                    // Track error
                    Bundle params = new Bundle();
                    params.putString("error_type", "like_add_failed");
                    params.putString("error_message", e.getMessage());
                    params.putString("template_id", templateId);
                    analyticsUtils.logEvent("template_interaction_error", params);
                });
    }

    /**
     * Remove template from likes
     */
    public Task<Void> removeFromLikes(String templateId) {
        return firestoreManager.removeFromLikes(templateId)
                .addOnSuccessListener(aVoid -> {
                    UserPreferences current = userPreferences.getValue();
                    if (current != null) {
                        current.removeLikedTemplate(templateId);
                        userPreferences.setValue(current);
                        
                        // Track like removed
                        Bundle params = new Bundle();
                        params.putString("template_id", templateId);
                        params.putString("interaction_type", "like_remove");
                        analyticsUtils.logEvent("template_interaction", params);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error removing template from likes", e);
                    // Track error
                    Bundle params = new Bundle();
                    params.putString("error_type", "like_remove_failed");
                    params.putString("error_message", e.getMessage());
                    params.putString("template_id", templateId);
                    analyticsUtils.logEvent("template_interaction_error", params);
                });
    }

    /**
     * Check if template is favorited
     */
    public Task<Boolean> isTemplateFavorited(String templateId) {
        return firestoreManager.isTemplateFavorited(templateId);
    }

    /**
     * Check if template is liked
     */
    public Task<Boolean> isTemplateLiked(String templateId) {
        return firestoreManager.isTemplateLiked(templateId);
    }

    /**
     * Start listening for user preferences changes
     */
    public void startListening() {
        if (preferencesListener != null) {
            return;
        }

        fetchUserPreferences();
    }

    /**
     * Stop listening for user preferences changes
     */
    public void stopListening() {
        if (preferencesListener != null) {
            preferencesListener.remove();
            preferencesListener = null;
        }
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        retryHandler.removeCallbacksAndMessages(null);
        syncHandler.removeCallbacksAndMessages(null);
    }

    /**
     * Update template interaction
     */
    private Task<Void> updateTemplateInteraction(String templateId, boolean isFavorite, boolean isLike, boolean value) {
        WriteBatch batch = firestoreManager.batch();
        Map<String, Object> data = new HashMap<>();
        data.put("templateId", templateId);
        data.put("timestamp", firestoreManager.getServerTimestamp());

        Task<DocumentReference> refTask;
        if (isFavorite) {
            refTask = firestoreManager.getFavoriteRef(templateId);
        } else if (isLike) {
            refTask = firestoreManager.getLikeRef(templateId);
        } else {
            return Tasks.forException(new IllegalArgumentException("Must specify either favorite or like"));
        }

        return refTask.continueWithTask(task -> {
            if (!task.isSuccessful() || task.getResult() == null) {
                return Tasks.forException(new IllegalStateException("Failed to get document reference"));
            }

            DocumentReference ref = task.getResult();
            if (value) {
                batch.set(ref, data);
            } else {
                batch.delete(ref);
            }

            return batch.commit();
        }).addOnSuccessListener(aVoid -> {
            // Track interaction
            Bundle params = new Bundle();
            params.putString("template_id", templateId);
            params.putString("interaction_type", isFavorite ? "favorite" : "like");
            params.putBoolean("value", value);
            AnalyticsUtils.getInstance().logEvent("template_interaction", params);
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error updating template interaction", e);
            error.setValue("Failed to update template interaction: " + e.getMessage());
        });
    }
} 