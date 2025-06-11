package com.ds.eventwish.data.remote;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ds.eventwish.data.auth.AuthManager;
import com.ds.eventwish.data.model.NotificationPreference;
import com.ds.eventwish.data.model.UserPreferences;
import com.ds.eventwish.data.model.Template;
import com.ds.eventwish.data.model.response.BaseResponse;
import com.ds.eventwish.utils.AnalyticsUtils;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.Transaction;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;
import com.ds.eventwish.utils.StringUtils;
import com.ds.eventwish.util.SecureTokenManager;
import com.google.firebase.firestore.FieldPath;
import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.NetworkType;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import androidx.work.BackoffPolicy;
import retrofit2.Call;
import retrofit2.Response;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Manager class for handling Firestore operations related to user preferences and interactions
 */
public class FirestoreManager {
    private static final String TAG = "FirestoreManager";
    private static final String COLLECTION_USERS = "users";
    private static final String COLLECTION_TEMPLATES = "templates";
    private static final String COLLECTION_PREFERENCES = "preferences";
    private static final String COLLECTION_FAVORITES = "favorites";
    private static final String COLLECTION_LIKES = "likes";
    private static final String COLLECTION_NOTIFICATIONS = "notifications";
    private static final String COLLECTION_TEMPLATE_INTERACTIONS = "template_interactions";
    private static final String FIELD_LIKE_COUNT = "likeCount";
    private static final String FIELD_FAVORITE_COUNT = "favoriteCount";
    private static final String FIELD_LIKED_TEMPLATES = "liked_templates";
    private static final String FIELD_FAVORITE_TEMPLATES = "favorite_templates";
    private static final String COLLECTION_USER_LIKES = "user_likes";
    private static final String COLLECTION_USER_FAVORITES = "user_favorites";
    private static final String FIELD_LIKED = "liked";
    private static final String FIELD_FAVORITED = "favorited";
    private static final String FIELD_NAME = "name";
    private static final String FIELD_EMAIL = "email";

    // Interface for template updates
    public interface OnTemplateUpdateListener {
        void onTemplateUpdated(long likeCount, long favoriteCount);
    }

    private static volatile FirestoreManager instance;
    private final FirebaseFirestore db;
    private final Context context;
    private final AuthManager authManager;
    private Map<String, ListenerRegistration> templateListeners = new ConcurrentHashMap<>();

    // Private constructor to enforce singleton pattern
    private FirestoreManager(Context context) {
        this.context = context.getApplicationContext();
        this.authManager = AuthManager.getInstance();
        db = FirebaseFirestore.getInstance();
        
        // Enable offline persistence
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build();
        db.setFirestoreSettings(settings);
        Log.d(TAG, "FirestoreManager initialized");
    }

    /**
     * Get singleton instance of FirestoreManager
     */
    public static FirestoreManager getInstance(Context context) {
        if (instance == null) {
            synchronized (FirestoreManager.class) {
                if (instance == null) {
                    instance = new FirestoreManager(context);
                }
            }
        }
        return instance;
    }

    /**
     * Get singleton instance of FirestoreManager (requires prior initialization)
     */
    public static FirestoreManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("FirestoreManager not initialized. Call getInstance(Context) first.");
        }
        return instance;
    }

    /**
     * Configure emulator usage for testing
     */
    public void useEmulator(String host, int port) {
        db.useEmulator(host, port);
        Log.d(TAG, String.format("Configured Firestore emulator at %s:%d", host, port));
    }

    /**
     * Check if the current user is signed in with Google
     * @return true if signed in with Google, false otherwise
     */
    public boolean isGoogleSignedIn() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            for (com.google.firebase.auth.UserInfo profile : user.getProviderData()) {
                if ("google.com".equals(profile.getProviderId())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Get current user ID from Firebase Auth
     */
    private Task<String> getUserId() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            // Check if this is a Google-signed-in user
            boolean isGoogleUser = isGoogleSignedIn();
            
            if (isGoogleUser) {
                Log.d(TAG, "User is authenticated with Google: " + user.getUid());
            } else {
                Log.d(TAG, "User is authenticated anonymously: " + user.getUid());
            }
            
            return Tasks.forResult(user.getUid());
        }
        
        Log.e(TAG, "No authenticated user available");
        return Tasks.forException(new IllegalStateException("User must be signed in"));
    }

    /**
     * Set FCM token for user identification
     */
    public Task<Void> setFcmToken(String token) {
        Log.d(TAG, "Setting FCM token: " + token);
        
        FirebaseUser user = authManager.getCurrentUser();
        if (user == null) {
            Log.d(TAG, "Skipping FCM token update - no authenticated user");
            return Tasks.forResult(null);
        }
        
        String userId = user.getUid();
        DocumentReference userRef = db.collection(COLLECTION_USERS).document(userId);
        
        // Only include fields that are allowed by security rules
        Map<String, Object> userData = new HashMap<>();
        userData.put("email", user.getEmail() != null ? user.getEmail() : "");
        userData.put("name", user.getDisplayName() != null ? user.getDisplayName() : "");
        userData.put("isOnline", true);
        userData.put("lastLogin", FieldValue.serverTimestamp());
        userData.put("lastUpdated", FieldValue.serverTimestamp());
        userData.put("notificationPreferences", new HashMap<>());
        userData.put("favoriteTemplates", new HashMap<>());
        userData.put("likedTemplates", new HashMap<>());
        
        return userRef.set(userData)
            .addOnSuccessListener(aVoid -> Log.d(TAG, "User document updated successfully"))
            .addOnFailureListener(e -> Log.e(TAG, "Error updating user document", e));
    }

    /**
     * Ensure user document exists before performing operations
     */
    private Task<DocumentReference> ensureUserDocument() {
        FirebaseUser user = authManager.getCurrentUser();
        if (user == null) {
            return Tasks.forException(new IllegalStateException("User must be signed in"));
        }
        
        String userId = user.getUid();
        DocumentReference userRef = db.collection(COLLECTION_USERS).document(userId);
        
        return userRef.get().continueWithTask(task -> {
            if (task.isSuccessful() && task.getResult().exists()) {
                return Tasks.forResult(userRef);
            }
            
            // Create user document if it doesn't exist
            Map<String, Object> userData = new HashMap<>();
            userData.put("email", user.getEmail() != null ? user.getEmail() : "");
            userData.put("name", user.getDisplayName() != null ? user.getDisplayName() : "");
            userData.put("isOnline", true);
            userData.put("lastLogin", FieldValue.serverTimestamp());
            userData.put("lastUpdated", FieldValue.serverTimestamp());
            userData.put("notificationPreferences", new HashMap<>());
            userData.put("favoriteTemplates", new HashMap<>());
            userData.put("likedTemplates", new HashMap<>());
            
            return userRef.set(userData).continueWith(setTask -> userRef);
        });
    }

    /**
     * Get user preferences document reference
     */
    private Task<DocumentReference> getUserPreferencesRef() {
        return getUserId().continueWith(task -> {
            if (!task.isSuccessful() || task.getResult() == null) {
                throw new IllegalStateException("No authenticated user");
            }
            return db.collection(COLLECTION_USERS).document(task.getResult()).collection(COLLECTION_PREFERENCES).document("settings");
        });
    }

    /**
     * Get user preferences
     */
    public Task<DocumentSnapshot> getUserPreferences() {
        return getUserPreferencesRef().continueWithTask(task -> {
            if (!task.isSuccessful()) {
                return Tasks.forException(task.getException());
            }
            return task.getResult().get();
        });
    }

    /**
     * Update user preferences
     */
    public Task<Void> updateUserPreferences(Map<String, Object> data) {
        return getUserPreferencesRef().continueWithTask(task -> {
            if (!task.isSuccessful()) {
                return Tasks.forException(task.getException());
            }
            return task.getResult().set(data, SetOptions.merge());
        });
    }

    /**
     * Add template to favorites
     */
    public Task<Void> addToFavorites(String templateId) {
        return getUserId().continueWithTask(task -> {
            if (!task.isSuccessful()) {
                return Tasks.forException(task.getException());
            }
            
            String userId = task.getResult();
            DocumentReference favoriteRef = db.collection(COLLECTION_USERS)
                    .document(userId)
                    .collection(COLLECTION_FAVORITES)
                    .document(templateId);

            Map<String, Object> data = new HashMap<>();
            data.put("templateId", templateId);
            data.put("timestamp", com.google.firebase.Timestamp.now());

            return favoriteRef.set(data);
        });
    }

    /**
     * Remove template from favorites
     */
    public Task<Void> removeFromFavorites(String templateId) {
        return getUserId().continueWithTask(task -> {
            if (!task.isSuccessful()) {
                return Tasks.forException(task.getException());
            }
            
            String userId = task.getResult();
            return db.collection(COLLECTION_USERS)
                    .document(userId)
                    .collection(COLLECTION_FAVORITES)
                    .document(templateId)
                    .delete();
        });
    }

    /**
     * Add template to likes
     */
    public Task<Void> addToLikes(String templateId) {
        return getUserId().continueWithTask(task -> {
            if (!task.isSuccessful()) {
                return Tasks.forException(task.getException());
            }
            
            String userId = task.getResult();
            DocumentReference likeRef = db.collection(COLLECTION_USERS)
                    .document(userId)
                    .collection(COLLECTION_LIKES)
                    .document(templateId);

            Map<String, Object> data = new HashMap<>();
            data.put("templateId", templateId);
            data.put("timestamp", com.google.firebase.Timestamp.now());

            return likeRef.set(data);
        });
    }

    /**
     * Remove template from likes
     */
    public Task<Void> removeFromLikes(String templateId) {
        return getUserId().continueWithTask(task -> {
            if (!task.isSuccessful()) {
                return Tasks.forException(task.getException());
            }
            
            String userId = task.getResult();
            return db.collection(COLLECTION_USERS)
                    .document(userId)
                    .collection(COLLECTION_LIKES)
                    .document(templateId)
                    .delete();
        });
    }

    /**
     * Check if template is favorited
     */
    public Task<Boolean> isTemplateFavorited(@NonNull String templateId) {
        return getUserId().continueWithTask(task -> {
            if (!task.isSuccessful()) {
                Log.e(TAG, "Error checking favorite status", task.getException());
                throw task.getException();
            }
            
            String userId = task.getResult();
            return db.collection(COLLECTION_USERS)
                .document(userId)
                .get()
                .continueWith(task1 -> {
                    if (!task1.isSuccessful()) {
                        Log.e(TAG, "Error checking favorite status", task1.getException());
                        throw task1.getException();
                    }
                    
                    DocumentSnapshot document = task1.getResult();
                    if (document == null || !document.exists()) {
                        Log.d(TAG, "User document does not exist");
                        return false;
                    }
                    
                    Map<String, Boolean> favoriteTemplates = 
                        (Map<String, Boolean>) document.get(FIELD_FAVORITE_TEMPLATES);
                    boolean isFavorited = favoriteTemplates != null && 
                        Boolean.TRUE.equals(favoriteTemplates.get(templateId));
                        
                    Log.d(TAG, String.format("Template %s favorite status: %b", templateId, isFavorited));
                    return isFavorited;
                });
        });
    }

    /**
     * Check if template is liked
     */
    public Task<Boolean> isTemplateLiked(@NonNull String templateId) {
        return getUserId().continueWithTask(task -> {
            if (!task.isSuccessful()) {
                Log.e(TAG, "Error checking like status", task.getException());
                throw task.getException();
            }
            
            String userId = task.getResult();
            return db.collection(COLLECTION_USERS)
                .document(userId)
                .get()
                .continueWith(task1 -> {
                    if (!task1.isSuccessful()) {
                        Log.e(TAG, "Error checking like status", task1.getException());
                        throw task1.getException();
                    }
                    
                    DocumentSnapshot document = task1.getResult();
                    if (document == null || !document.exists()) {
                        Log.d(TAG, "User document does not exist");
                        return false;
                    }
                    
                    Map<String, Boolean> likedTemplates = (Map<String, Boolean>) document.get(FIELD_LIKED_TEMPLATES);
                    boolean isLiked = likedTemplates != null && 
                        Boolean.TRUE.equals(likedTemplates.get(templateId));
                        
                    Log.d(TAG, String.format("Template %s like status: %b", templateId, isLiked));
                    return isLiked;
                });
        });
    }

    /**
     * Update notification preferences
     */
    public Task<Void> updateNotificationPreferences(NotificationPreference preferences) {
        return getUserPreferencesRef().continueWithTask(task -> {
            if (!task.isSuccessful()) {
                return Tasks.forException(task.getException());
            }
            DocumentReference prefsRef = task.getResult();
            Map<String, Object> updates = new HashMap<>();
            updates.put("notificationPreferences." + preferences.getType(), preferences);
            return prefsRef.set(updates, SetOptions.merge());
        });
    }

    /**
     * Add notification record
     */
    public Task<DocumentReference> addNotification(Map<String, Object> notification) {
        return getUserId().continueWithTask(task -> {
            if (!task.isSuccessful()) {
                return Tasks.forException(task.getException());
            }
            
            String userId = task.getResult();
            return db.collection(COLLECTION_USERS)
                    .document(userId)
                    .collection(COLLECTION_NOTIFICATIONS)
                    .add(notification);
        });
    }

    /**
     * Get a new write batch
     */
    public WriteBatch batch() {
        return db.batch();
    }

    /**
     * Get server timestamp
     */
    public FieldValue getServerTimestamp() {
        return FieldValue.serverTimestamp();
    }

    /**
     * Get favorite document reference
     */
    public Task<DocumentReference> getFavoriteRef(String templateId) {
        return getUserId().continueWith(task -> {
            if (!task.isSuccessful() || task.getResult() == null) {
                throw new IllegalStateException("No user ID available");
            }
            String userId = task.getResult();
            return db.collection(COLLECTION_USERS)
                    .document(userId)
                    .collection(COLLECTION_FAVORITES)
                    .document(templateId);
        });
    }

    /**
     * Get like document reference
     */
    public Task<DocumentReference> getLikeRef(String templateId) {
        return getUserId().continueWith(task -> {
            if (!task.isSuccessful() || task.getResult() == null) {
                throw new IllegalStateException("No user ID available");
            }
            String userId = task.getResult();
            return db.collection(COLLECTION_USERS)
                    .document(userId)
                    .collection(COLLECTION_LIKES)
                    .document(templateId);
        });
    }

    /**
     * Get current user ID
     */
    public String getCurrentUserId() {
        return getUserId().continueWith(task -> {
            if (!task.isSuccessful() || task.getResult() == null) {
                Log.e(TAG, "No authenticated user");
                return null;
            }
            return task.getResult();
        }).getResult(null);
    }

    /**
     * Check if user is signed in
     */
    public boolean isUserSignedIn() {
        try {
            return getUserId().isSuccessful() && getUserId().getResult() != null;
        } catch (Exception e) {
            Log.e(TAG, "Error checking user sign in status", e);
            return false;
        }
    }

    /**
     * Toggle like status for a template
     */
    public Task<Boolean> toggleLike(String templateId) {
        Log.d(TAG, "Toggling like for template: " + templateId);
        
        FirebaseUser user = authManager.getCurrentUser();
        if (user == null) {
            Log.e(TAG, "Cannot toggle like: User not signed in");
            return Tasks.forException(new IllegalStateException("User not signed in"));
        }
        
        String userId = user.getUid();
        DocumentReference userRef = db.collection(COLLECTION_USERS).document(userId);
        DocumentReference likeRef = userRef.collection(COLLECTION_LIKES).document(templateId);
        
        // First check if user has already liked this template
        return likeRef.get().continueWithTask(likeTask -> {
            if (!likeTask.isSuccessful()) {
                throw likeTask.getException();
            }
            
            boolean isCurrentlyLiked = likeTask.getResult().exists();
            
            // Use a transaction to ensure atomicity
            return db.runTransaction(transaction -> {
                // Get the current template document
                DocumentReference templateRef = db.collection(COLLECTION_TEMPLATES).document(templateId);
                DocumentSnapshot templateSnapshot = transaction.get(templateRef);
                
                // Get current like count or default to 0
                Long currentLikeCount = templateSnapshot.getLong(FIELD_LIKE_COUNT);
                long likeCount = currentLikeCount != null ? currentLikeCount : 0;
                
                // Update like count based on current state
                long newLikeCount = isCurrentlyLiked ? Math.max(0, likeCount - 1) : likeCount + 1;
                
                // Update the template document with new count
                transaction.update(templateRef, FIELD_LIKE_COUNT, newLikeCount);
                
                // Update user's like status
                if (isCurrentlyLiked) {
                    // Unlike: Delete like document
                    transaction.delete(likeRef);
                } else {
                    // Like: Create like document
                    Map<String, Object> likeData = new HashMap<>();
                    likeData.put("templateId", templateId);
                    likeData.put("userId", userId);
                    likeData.put("timestamp", FieldValue.serverTimestamp());
                    transaction.set(likeRef, likeData);
                }
                
                // Return the new like state
                return !isCurrentlyLiked;
            });
        })
        .addOnSuccessListener(isLiked -> {
            Log.d(TAG, String.format("Successfully %s template %s", isLiked ? "liked" : "unliked", templateId));
            // Track analytics event
            Bundle params = new Bundle();
            params.putString("template_id", templateId);
            params.putString("action", isLiked ? "like" : "unlike");
            AnalyticsUtils.getInstance().logEvent("template_like_toggled", params);
        })
        .addOnFailureListener(e -> {
            Log.e(TAG, "Error toggling template like - Template: " + templateId + 
                  ", User: " + userId, e);
            // Track error event
            Bundle params = new Bundle();
            params.putString("template_id", templateId);
            params.putString("user_id", userId);
            params.putString("error", e.getMessage());
            AnalyticsUtils.getInstance().logEvent("template_like_error", params);
            
            // Schedule a retry for failed operations
            scheduleRetry("like", templateId, userId);
        });
    }
    
    /**
     * Schedule a retry for failed operations
     */
    private void scheduleRetry(String operationType, String templateId, String userId) {
        // Create a work request for retry
        OneTimeWorkRequest retryWork = new OneTimeWorkRequest.Builder(RetryWorker.class)
            .setInputData(new androidx.work.Data.Builder()
                .putString("operation", operationType)
                .putString("template_id", templateId)
                .putString("user_id", userId)
                .build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .setConstraints(new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build())
            .build();
        
        // Enqueue the work
        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                "retry_" + operationType + "_" + templateId,
                ExistingWorkPolicy.REPLACE,
                retryWork);
    }
    
    /**
     * Worker class for retrying failed operations
     */
    public static class RetryWorker extends Worker {
        public RetryWorker(@NonNull Context context, @NonNull WorkerParameters params) {
            super(context, params);
        }
        
        @NonNull
        @Override
        public Result doWork() {
            String operation = getInputData().getString("operation");
            String templateId = getInputData().getString("template_id");
            String userId = getInputData().getString("user_id");
            
            if (operation == null || templateId == null || userId == null) {
                return Result.failure();
            }
            
            FirestoreManager manager = FirestoreManager.getInstance();
            
            try {
                if ("like".equals(operation)) {
                    // Retry the like operation
                    Tasks.await(manager.toggleLike(templateId));
                    return Result.success();
                } else if ("favorite".equals(operation)) {
                    // Retry the favorite operation
                    Tasks.await(manager.toggleFavorite(templateId));
                    return Result.success();
                }
            } catch (Exception e) {
                Log.e("RetryWorker", "Failed to retry operation: " + operation, e);
                return Result.retry();
            }
            
            return Result.failure();
        }
    }

    /**
     * Toggle favorite status for a template
     */
    public Task<Boolean> toggleFavorite(String templateId) {
        Log.d(TAG, "Toggling favorite for template: " + templateId);
        
        // Get current user from AuthManager
        FirebaseUser user = AuthManager.getInstance().getCurrentUser();
        if (user == null) {
            Log.e(TAG, "Cannot toggle favorite: User not signed in");
            return Tasks.forException(new IllegalStateException("User not signed in"));
        }
        
        String userId = user.getUid();
        DocumentReference favoriteRef = db.collection(COLLECTION_USERS)
                .document(userId)
                .collection(COLLECTION_FAVORITES)
                .document(templateId);
        
        // First check if user has already favorited this template
        return favoriteRef.get().continueWithTask(favoriteTask -> {
            if (!favoriteTask.isSuccessful()) {
                throw favoriteTask.getException();
            }
            
            boolean isCurrentlyFavorited = favoriteTask.getResult().exists();
            
            // Use the safe counter increment method instead of direct update
            long increment = isCurrentlyFavorited ? -1 : 1;
            return safeIncrementTemplateCounter(templateId, FIELD_FAVORITE_COUNT, increment)
                .continueWithTask(updateTask -> {
                    if (!updateTask.isSuccessful()) {
                        throw updateTask.getException();
                    }
                    
                    return updateFavoriteStatus(favoriteRef, isCurrentlyFavorited, templateId, userId);
                });
        })
        .addOnSuccessListener(isFavorited -> {
            Log.d(TAG, String.format("Successfully %s template %s", 
                  isFavorited ? "favorited" : "unfavorited", templateId));
            // Track analytics event
            Bundle params = new Bundle();
            params.putString("template_id", templateId);
            params.putString("action", isFavorited ? "favorite" : "unfavorite");
            AnalyticsUtils.getInstance().logEvent("template_favorite_toggled", params);
        })
        .addOnFailureListener(e -> {
            Log.e(TAG, "Error toggling template favorite - Template: " + templateId + 
                  ", User: " + userId, e);
            // Track error event
            Bundle params = new Bundle();
            params.putString("template_id", templateId);
            params.putString("user_id", userId);
            params.putString("error", e.getMessage());
            AnalyticsUtils.getInstance().logEvent("template_favorite_error", params);
        });
    }
    
    /**
     * Helper method to update the favorite status
     */
    private Task<Boolean> updateFavoriteStatus(DocumentReference favoriteRef, boolean isCurrentlyFavorited, 
                                              String templateId, String userId) {
        if (isCurrentlyFavorited) {
            // Unfavorite: Delete favorite document
            Log.d(TAG, "Removing favorite for template: " + templateId);
            return favoriteRef.delete().continueWith(task -> false);
        } else {
            // Favorite: Create favorite document
            Log.d(TAG, "Adding favorite for template: " + templateId);
            Map<String, Object> favoriteData = new HashMap<>();
            favoriteData.put("templateId", templateId);
            favoriteData.put("userId", userId);
            favoriteData.put("timestamp", FieldValue.serverTimestamp());
            return favoriteRef.set(favoriteData).continueWith(task -> true);
        }
    }

    public Task<Void> ensureTemplateExists(String templateId) {
        DocumentReference templateRef = db.collection(COLLECTION_TEMPLATES).document(templateId);
        return templateRef.get().continueWithTask(task -> {
            if (task.isSuccessful() && task.getResult().exists()) {
                return Tasks.forResult(null);
            }
            
            // Create template document with required fields
            Map<String, Object> data = new HashMap<>();
            data.put(FIELD_LIKE_COUNT, 0L);
            data.put(FIELD_FAVORITE_COUNT, 0L);
            data.put("lastUpdated", FieldValue.serverTimestamp());
            
            return templateRef.set(data);
        });
    }

    public void observeTemplate(String templateId, OnTemplateUpdateListener listener) {
        Log.d(TAG, String.format("Starting template observation - Template: %s", templateId));
        
        DocumentReference templateRef = db.collection(COLLECTION_TEMPLATES).document(templateId);
        ListenerRegistration registration = templateRef.addSnapshotListener((snapshot, e) -> {
            if (e != null) {
                Log.e(TAG, String.format("Error observing template %s: %s", templateId, e.getMessage()), e);
                return;
            }

            if (snapshot != null && snapshot.exists()) {
                Long likeCount = snapshot.getLong(FIELD_LIKE_COUNT);
                Long favoriteCount = snapshot.getLong(FIELD_FAVORITE_COUNT);
                Log.d(TAG, String.format("Template update - ID: %s, Likes: %d, Favorites: %d", 
                    templateId, 
                    likeCount != null ? likeCount : 0,
                    favoriteCount != null ? favoriteCount : 0));
                
                listener.onTemplateUpdated(
                    likeCount != null ? likeCount : 0,
                    favoriteCount != null ? favoriteCount : 0
                );
            } else {
                Log.w(TAG, String.format("Template document does not exist: %s", templateId));
            }
        });
        
        // Store registration for cleanup
        templateListeners.put(templateId, registration);
        Log.d(TAG, String.format("Template observation registered - Template: %s", templateId));
    }

    public void stopObservingTemplate(String templateId) {
        Log.d(TAG, String.format("Stopping template observation - Template: %s", templateId));
        ListenerRegistration registration = templateListeners.remove(templateId);
        if (registration != null) {
            registration.remove();
            Log.d(TAG, String.format("Template observation stopped - Template: %s", templateId));
        } else {
            Log.w(TAG, String.format("No active observation found for template: %s", templateId));
        }
    }

    public Task<QuerySnapshot> getUserInteractions() {
        return getUserId().continueWithTask(task -> {
            if (!task.isSuccessful()) {
                Log.e(TAG, "Cannot get user interactions - No authenticated user", task.getException());
                throw task.getException();
            }
            
            String userId = task.getResult();
            return db.collection(COLLECTION_USERS)
                    .document(userId)
                    .collection(COLLECTION_LIKES)
                    .get();
        });
    }

    /**
     * Update user online status
     */
    public Task<Void> updateOnlineStatus(boolean isOnline) {
        return getUserId().continueWithTask(task -> {
            if (!task.isSuccessful()) {
                return Tasks.forException(task.getException());
            }
            
            String userId = task.getResult();
            DocumentReference userRef = db.collection(COLLECTION_USERS).document(userId);
            Map<String, Object> updates = new HashMap<>();
            updates.put("isOnline", isOnline);
            updates.put("lastUpdated", FieldValue.serverTimestamp());
            
            if (isOnline) {
                updates.put("lastLogin", FieldValue.serverTimestamp());
            }
            
            return userRef.set(updates, SetOptions.merge());
        });
    }

    /**
     * Update user profile
     */
    public Task<Void> updateUserProfile(String name, String email) {
        return getUserId().continueWithTask(task -> {
            if (!task.isSuccessful() || task.getResult() == null) {
                return Tasks.forException(new IllegalStateException("No user ID available"));
            }
            
            String userId = task.getResult();
            DocumentReference userRef = db.collection(COLLECTION_USERS).document(userId);
            
            Map<String, Object> updates = new HashMap<>();
            if (name != null) {
                updates.put(FIELD_NAME, StringUtils.toCamelCase(name));
            }
            if (email != null) {
                updates.put(FIELD_EMAIL, email);
            }
            updates.put("lastUpdated", FieldValue.serverTimestamp());
            
            return userRef.set(updates, SetOptions.merge());
        });
    }

    public Task<List<Template>> getLikedTemplates() {
        return getUserId().continueWithTask(task -> {
            if (!task.isSuccessful() || task.getResult() == null) {
                return Tasks.forResult(Collections.emptyList());
            }
            
            String userId = task.getResult();
            return db.collection(COLLECTION_USERS)
                .document(userId)
                .get()
                .continueWithTask(docTask -> {
                    if (!docTask.isSuccessful() || !docTask.getResult().exists()) {
                        return Tasks.forResult(Collections.emptyList());
                    }
                    
                    List<String> likedTemplates = (List<String>) docTask.getResult().get("likedTemplates");
                    if (likedTemplates == null || likedTemplates.isEmpty()) {
                        return Tasks.forResult(Collections.emptyList());
                    }
                    
                    return db.collection(COLLECTION_TEMPLATES)
                        .whereIn(FieldPath.documentId(), likedTemplates)
                        .get()
                        .continueWith(templatesTask -> {
                            if (!templatesTask.isSuccessful()) {
                                return Collections.emptyList();
                            }
                            
                            List<Template> templates = new ArrayList<>();
                            for (DocumentSnapshot doc : templatesTask.getResult()) {
                                Template template = doc.toObject(Template.class);
                                if (template != null) {
                                    template.setId(doc.getId());
                                    template.setLiked(true);
                                    templates.add(template);
                                }
                            }
                            return templates;
                        });
                });
        });
    }

    public Task<List<Template>> getFavoriteTemplates() {
        return getUserId().continueWithTask(task -> {
            if (!task.isSuccessful() || task.getResult() == null) {
                return Tasks.forResult(Collections.emptyList());
            }
            
            String userId = task.getResult();
            return db.collection(COLLECTION_USERS)
                .document(userId)
                .get()
                .continueWithTask(docTask -> {
                    if (!docTask.isSuccessful() || !docTask.getResult().exists()) {
                        return Tasks.forResult(Collections.emptyList());
                    }
                    
                    List<String> favoriteTemplates = (List<String>) docTask.getResult().get("favoriteTemplates");
                    if (favoriteTemplates == null || favoriteTemplates.isEmpty()) {
                        return Tasks.forResult(Collections.emptyList());
                    }
                    
                    return db.collection(COLLECTION_TEMPLATES)
                        .whereIn(FieldPath.documentId(), favoriteTemplates)
                        .get()
                        .continueWith(templatesTask -> {
                            if (!templatesTask.isSuccessful()) {
                                return Collections.emptyList();
                            }
                            
                            List<Template> templates = new ArrayList<>();
                            for (DocumentSnapshot doc : templatesTask.getResult()) {
                                Template template = doc.toObject(Template.class);
                                if (template != null) {
                                    template.setId(doc.getId());
                                    template.setFavorited(true);
                                    templates.add(template);
                                }
                            }
                            return templates;
                        });
                });
        });
    }

    /**
     * Update template like and favorite counts
     * This updates the counts directly in the templates collection
     */
    public Task<Void> updateTemplateCounts(String templateId, int likeCount, int favoriteCount) {
        Log.d(TAG, "Updating template counts - Template: " + templateId + 
              ", Likes: " + likeCount + ", Favorites: " + favoriteCount);
        
        DocumentReference templateRef = db.collection(COLLECTION_TEMPLATES).document(templateId);
        
        Map<String, Object> updates = new HashMap<>();
        updates.put(FIELD_LIKE_COUNT, likeCount);
        updates.put(FIELD_FAVORITE_COUNT, favoriteCount);
        updates.put("lastUpdated", FieldValue.serverTimestamp());
        
        return templateRef.set(updates, SetOptions.merge())
            .addOnSuccessListener(aVoid -> 
                Log.d(TAG, "Successfully updated template counts for " + templateId))
            .addOnFailureListener(e -> 
                Log.e(TAG, "Failed to update template counts for " + templateId, e));
    }
    
    /**
     * Get current template counts directly from templates collection
     */
    public Task<Map<String, Long>> getTemplateCounts(String templateId) {
        Log.d(TAG, "Getting counts for template: " + templateId);
        
        DocumentReference templateRef = db.collection(COLLECTION_TEMPLATES).document(templateId);
        
        return templateRef.get().continueWith(task -> {
            Map<String, Long> counts = new HashMap<>();
            
            if (!task.isSuccessful()) {
                Log.e(TAG, "Error getting template counts", task.getException());
                counts.put("likeCount", 0L);
                counts.put("favoriteCount", 0L);
                return counts;
            }
            
            DocumentSnapshot snapshot = task.getResult();
            if (snapshot == null || !snapshot.exists()) {
                Log.d(TAG, "Template document does not exist: " + templateId);
                counts.put("likeCount", 0L);
                counts.put("favoriteCount", 0L);
                return counts;
            }
            
            Long likeCount = snapshot.getLong(FIELD_LIKE_COUNT);
            Long favoriteCount = snapshot.getLong(FIELD_FAVORITE_COUNT);
            
            counts.put("likeCount", likeCount != null ? likeCount : 0L);
            counts.put("favoriteCount", favoriteCount != null ? favoriteCount : 0L);
            
            Log.d(TAG, "Template " + templateId + " counts - Likes: " + counts.get("likeCount") + 
                  ", Favorites: " + counts.get("favoriteCount"));
            
            return counts;
        });
    }
    
    /**
     * Count users who have liked/favorited a template and update counts
     */
    public Task<Void> syncTemplateCounts(String templateId) {
        Log.d(TAG, "Syncing counts for template: " + templateId);
        
        // Count users who liked this template
        Task<QuerySnapshot> likesTask = db.collectionGroup(COLLECTION_LIKES)
            .whereEqualTo("templateId", templateId)
            .get();
        
        // Count users who favorited this template
        Task<QuerySnapshot> favoritesTask = db.collectionGroup(COLLECTION_FAVORITES)
            .whereEqualTo("templateId", templateId)
            .get();
        
        // Wait for both counts and then update the template document
        return Tasks.whenAllSuccess(likesTask, favoritesTask)
            .continueWithTask(task -> {
                if (!task.isSuccessful()) {
                    return Tasks.forException(task.getException());
                }
                
                List<Object> results = task.getResult();
                QuerySnapshot likeSnapshots = (QuerySnapshot) results.get(0);
                QuerySnapshot favoriteSnapshots = (QuerySnapshot) results.get(1);
                
                int likeCount = likeSnapshots.size();
                int favoriteCount = favoriteSnapshots.size();
                
                Log.d(TAG, "Actual counts for template " + templateId + 
                      " - Likes: " + likeCount + ", Favorites: " + favoriteCount);
                
                // Update template document with accurate counts
                return updateTemplateCounts(templateId, likeCount, favoriteCount);
            });
    }

    /**
     * Ensure the templates collection exists and has at least one document
     * This should be called during app initialization
     */
    public Task<Void> ensureTemplatesCollectionExists() {
        Log.d(TAG, "Ensuring templates collection exists");
        
        // Check if the collection exists with any documents
        return db.collection(COLLECTION_TEMPLATES).limit(1).get()
            .continueWithTask(task -> {
                if (task.isSuccessful() && !task.getResult().isEmpty()) {
                    Log.d(TAG, "Templates collection exists with documents");
                    return Tasks.forResult(null);
                }
                
                // If we reach here, we need to create a placeholder document
                Log.d(TAG, "Creating placeholder document in templates collection");
                DocumentReference placeholderRef = db.collection(COLLECTION_TEMPLATES).document("placeholder");
                
                Map<String, Object> placeholderData = new HashMap<>();
                placeholderData.put("id", "placeholder");
                placeholderData.put("likeCount", 0);
                placeholderData.put("favoriteCount", 0);
                placeholderData.put("isPlaceholder", true);
                placeholderData.put("created", FieldValue.serverTimestamp());
                
                return placeholderRef.set(placeholderData);
            })
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Templates collection initialization complete");
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Failed to initialize templates collection", e);
            });
    }
    
    /**
     * Create template document if it doesn't exist
     * This ensures we have a place to store like/favorite counts
     */
    public Task<Void> createTemplateDocument(String templateId, String templateName, String imageUrl) {
        Log.d(TAG, "Creating/updating template document: " + templateId);
        
        DocumentReference templateRef = db.collection(COLLECTION_TEMPLATES).document(templateId);
        
        return templateRef.get().continueWithTask(task -> {
            if (!task.isSuccessful()) {
                Log.e(TAG, "Error checking template document", task.getException());
                throw task.getException();
            }
            
            Map<String, Object> templateData = new HashMap<>();
            
            if (!task.getResult().exists()) {
                // Template doesn't exist, create it with initial data
                templateData.put("id", templateId);
                templateData.put("name", templateName != null ? templateName : "Template " + templateId);
                templateData.put("likeCount", 0);
                templateData.put("favoriteCount", 0);
                
                if (imageUrl != null && !imageUrl.isEmpty()) {
                    templateData.put("imageUrl", imageUrl);
                }
            } else {
                // Template exists, only update certain fields if provided
                if (templateName != null && !templateName.isEmpty()) {
                    templateData.put("name", templateName);
                }
                
                if (imageUrl != null && !imageUrl.isEmpty()) {
                    templateData.put("imageUrl", imageUrl);
                }
            }
            
            templateData.put("lastUpdated", FieldValue.serverTimestamp());
            
            // Use set with merge to avoid overwriting existing fields
            return templateRef.set(templateData, SetOptions.merge());
        });
    }
    
    /**
     * Initialize the templates collection with template counts
     * This method will scan all users' likes and favorites and create template documents with accurate counts
     */
    public Task<Void> initializeTemplateCountsFromUserData() {
        Log.d(TAG, "Initializing template counts from user data");
        
        // First ensure the templates collection exists
        return ensureTemplatesCollectionExists().continueWithTask(task -> {
            if (!task.isSuccessful()) {
                Log.e(TAG, "Failed to ensure templates collection exists", task.getException());
                throw task.getException();
            }
            
            // Get all unique template IDs from user likes
            return db.collectionGroup(COLLECTION_LIKES).get();
        }).continueWithTask(likesTask -> {
            if (!likesTask.isSuccessful()) {
                Log.e(TAG, "Failed to query user likes", likesTask.getException());
                throw likesTask.getException();
            }
            
            // Get all unique template IDs from user favorites
            return db.collectionGroup(COLLECTION_FAVORITES).get().continueWithTask(favoritesTask -> {
                if (!favoritesTask.isSuccessful()) {
                    Log.e(TAG, "Failed to query user favorites", favoritesTask.getException());
                    throw favoritesTask.getException();
                }
                
                // Collect all unique template IDs
                Set<String> templateIds = new HashSet<>();
                
                for (DocumentSnapshot doc : likesTask.getResult()) {
                    String templateId = doc.getString("templateId");
                    if (templateId != null && !templateId.isEmpty()) {
                        templateIds.add(templateId);
                    }
                }
                
                for (DocumentSnapshot doc : favoritesTask.getResult()) {
                    String templateId = doc.getString("templateId");
                    if (templateId != null && !templateId.isEmpty()) {
                        templateIds.add(templateId);
                    }
                }
                
                Log.d(TAG, "Found " + templateIds.size() + " unique templates with likes/favorites");
                
                // Process each template to calculate and store counts
                List<Task<Void>> updateTasks = new ArrayList<>();
                
                for (String templateId : templateIds) {
                    Task<Void> updateTask = syncTemplateCounts(templateId);
                    updateTasks.add(updateTask);
                }
                
                // Wait for all template updates to complete
                return Tasks.whenAll(updateTasks);
            });
        });
    }

    /**
     * Utility method to safely update a template document
     * This ensures the document exists before updating it
     * 
     * @param templateId The template ID
     * @param updates The fields to update
     * @return A task that completes when the update is done
     */
    public Task<Void> safeUpdateTemplate(String templateId, Map<String, Object> updates) {
        Log.d(TAG, "Safe updating template: " + templateId);
        DocumentReference templateRef = db.collection(COLLECTION_TEMPLATES).document(templateId);
        
        // Always use set with merge to handle non-existent documents
        return templateRef.set(updates, SetOptions.merge())
            .addOnSuccessListener(aVoid -> 
                Log.d(TAG, "Successfully updated template: " + templateId))
            .addOnFailureListener(e -> 
                Log.e(TAG, "Failed to update template: " + templateId, e));
    }
    
    /**
     * Utility method to safely increment a counter in a template document
     * 
     * @param templateId The template ID
     * @param field The counter field name
     * @param increment The amount to increment
     * @return A task that completes when the update is done
     */
    public Task<Void> safeIncrementTemplateCounter(String templateId, String field, long increment) {
        Log.d(TAG, "Safe incrementing " + field + " by " + increment + " for template: " + templateId);
        
        DocumentReference templateRef = db.collection(COLLECTION_TEMPLATES).document(templateId);
        
        // Use a transaction for atomicity and thread safety
        return db.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(templateRef);
            
            Map<String, Object> updates = new HashMap<>();
            
            if (snapshot.exists()) {
                // Document exists, get current value and calculate new value
                Long currentValue = snapshot.getLong(field);
                long current = currentValue != null ? currentValue : 0L;
                
                // Calculate new value, ensuring it's never negative
                long newValue = Math.max(0L, current + increment);
                
                // Instead of using increment, set the exact value to ensure it's never negative
                updates.put(field, newValue);
                Log.d(TAG, "Setting " + field + " to exact value: " + newValue + " for template: " + templateId);
            } else {
                // Document doesn't exist, create it with initial values
                updates.put("id", templateId);
                
                // For new documents, always set positive counts for the requested field
                // and 0 for other fields
                if (field.equals(FIELD_LIKE_COUNT)) {
                    // If we're trying to add a like, set to 1, otherwise 0
                    updates.put(FIELD_LIKE_COUNT, increment > 0 ? 1L : 0L);
                    updates.put(FIELD_FAVORITE_COUNT, 0L);
                } else if (field.equals(FIELD_FAVORITE_COUNT)) {
                    // If we're trying to add a favorite, set to 1, otherwise 0
                    updates.put(FIELD_FAVORITE_COUNT, increment > 0 ? 1L : 0L);
                    updates.put(FIELD_LIKE_COUNT, 0L);
                }
                
                Log.d(TAG, "Creating new template document: " + templateId);
            }
            
            updates.put("lastUpdated", FieldValue.serverTimestamp());
            
            // Use set with merge to handle both cases
            transaction.set(templateRef, updates, SetOptions.merge());
            
            // Return null to indicate success for the transaction
            return null;
        })
        .continueWith(task -> {
            if (task.isSuccessful()) {
                Log.d(TAG, "Successfully set " + field + " for template: " + templateId);
            } else {
                Log.e(TAG, "Failed to set " + field + " for template: " + templateId, task.getException());
            }
            return null; // This ensures the return type is Task<Void>
        });
    }

    /**
     * Update user profile in MongoDB after Google Sign-In
     * @param user Firebase user object
     * @return Task representing the operation
     */
    public Task<Void> updateUserProfileInMongoDB(FirebaseUser user) {
        if (user == null) {
            return Tasks.forException(new IllegalArgumentException("User cannot be null"));
        }
        
        // Get device ID
        String deviceId;
        try {
            deviceId = SecureTokenManager.getInstance().getDeviceId();
        } catch (Exception e) {
            Log.e(TAG, "Error getting device ID", e);
            deviceId = "unknown_device";
        }
        
        // Create data map for MongoDB update
        Map<String, Object> userData = new HashMap<>();
        userData.put("uid", user.getUid());
        userData.put("deviceId", deviceId);
        userData.put("displayName", user.getDisplayName() != null ? user.getDisplayName() : "");
        userData.put("email", user.getEmail() != null ? user.getEmail() : "");
        userData.put("profilePhoto", user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : "");
        userData.put("lastOnline", System.currentTimeMillis());
        
        // Use ApiClient if available
        if (ApiClient.isInitialized()) {
            // Create a TaskCompletionSource to convert from Executor to Task
            TaskCompletionSource<Void> taskCompletionSource = new TaskCompletionSource<>();
            
            // First get the authentication token
            user.getIdToken(true)
                .addOnSuccessListener(getTokenResult -> {
                    String authToken = getTokenResult.getToken();
                    // Use a background thread for the network operation
                    Executors.newSingleThreadExecutor().execute(() -> {
                        try {
                            ApiService apiService = ApiClient.getClient();
                            Call<BaseResponse<Void>> call = apiService.updateUserProfile(userData, "Bearer " + authToken);
                            Response<BaseResponse<Void>> response = call.execute();
                            
                            if (response.isSuccessful()) {
                                Log.d(TAG, "Successfully updated user profile in MongoDB");
                                taskCompletionSource.setResult(null);
                            } else {
                                // Log the error but don't throw an exception for 404 errors
                                if (response.code() == 404) {
                                    Log.w(TAG, "MongoDB profile endpoint not found (404). This is non-critical and can be ignored.");
                                    // Complete the task successfully since this is non-critical
                                    taskCompletionSource.setResult(null);
                                } else {
                                    // For other errors, log and complete with exception
                                    Log.e(TAG, "Failed to update MongoDB: " + response.code());
                                    taskCompletionSource.setException(new Exception("Failed to update MongoDB: " + response.code()));
                                }
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error updating MongoDB", e);
                            // Complete the task successfully even if there's an error, since this is non-critical
                            taskCompletionSource.setResult(null);
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting authentication token", e);
                    // Complete the task successfully even if there's an error, since this is non-critical
                    taskCompletionSource.setResult(null);
                });
            
            return taskCompletionSource.getTask();
        } else {
            Log.e(TAG, "ApiClient not initialized, cannot update MongoDB");
            // Return a successful task since this is non-critical
            return Tasks.forResult(null);
        }
    }
} 