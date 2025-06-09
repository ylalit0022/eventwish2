package com.ds.eventwish.data.remote;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ds.eventwish.data.model.NotificationPreference;
import com.ds.eventwish.data.model.UserPreferences;
import com.google.android.gms.tasks.Task;
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
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;
import com.ds.eventwish.util.SecureTokenManager;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    // Interface for template updates
    public interface OnTemplateUpdateListener {
        void onTemplateUpdated(long likeCount, long favoriteCount);
    }

    private static volatile FirestoreManager instance;
    private final FirebaseFirestore db;
    private String fcmToken;
    private Map<String, ListenerRegistration> templateListeners = new HashMap<>();

    // Private constructor to enforce singleton pattern
    private FirestoreManager() {
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
    public static FirestoreManager getInstance() {
        if (instance == null) {
            synchronized (FirestoreManager.class) {
                if (instance == null) {
                    instance = new FirestoreManager();
                }
            }
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
     * Get current user ID from Firebase Auth
     */
    private Task<String> getUserId() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            return Tasks.forResult(user.getUid());
        }
        
        // No user, try to find existing user document by FCM token
        if (fcmToken != null && !fcmToken.isEmpty()) {
            return db.collection(COLLECTION_USERS)
                .whereEqualTo("fcmToken", fcmToken)
                .get()
                .continueWith(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        // Found existing user document
                        return task.getResult().getDocuments().get(0).getId();
                    }
                    // Create new user document with random ID
                    DocumentReference newUserRef = db.collection(COLLECTION_USERS).document();
                    Map<String, Object> userData = new HashMap<>();
                    userData.put("fcmToken", fcmToken);
                    userData.put("createdAt", FieldValue.serverTimestamp());
                    userData.put("lastUpdated", FieldValue.serverTimestamp());
                    
                    // Use set() with merge to handle race conditions
                    newUserRef.set(userData, SetOptions.merge());
                    return newUserRef.getId();
                });
        }
        
        Log.e(TAG, "No user ID available (neither auth nor FCM token)");
        return Tasks.forException(new IllegalStateException("No user ID available"));
    }

    /**
     * Set FCM token for user identification
     */
    public Task<Void> setFcmToken(String token) {
        Log.d(TAG, "Setting FCM token: " + token);
        this.fcmToken = token;
        
        return getUserId().continueWithTask(task -> {
            if (!task.isSuccessful() || task.getResult() == null) {
                return Tasks.forException(new IllegalStateException("No user ID available"));
            }
            
            String userId = task.getResult();
            if (token == null) {
                Log.e(TAG, "Attempted to set null FCM token");
                return Tasks.forException(new IllegalArgumentException("FCM token cannot be null"));
            }
            
            // Update user document
            DocumentReference userRef = db.collection(COLLECTION_USERS).document(userId);
            Map<String, Object> userData = new HashMap<>();
            userData.put("fcmToken", token);
            userData.put("lastUpdated", FieldValue.serverTimestamp());
            
            return userRef.set(userData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> Log.d(TAG, "User document updated successfully"))
                .addOnFailureListener(e -> Log.e(TAG, "Error updating user document", e));
        });
    }

    /**
     * Get user document reference
     */
    private Task<DocumentReference> getUserRef() {
        return getUserId().continueWith(task -> {
            if (!task.isSuccessful() || task.getResult() == null) {
                throw new IllegalStateException("No user ID available");
            }
            return db.collection(COLLECTION_USERS).document(task.getResult());
        });
    }

    /**
     * Get user preferences document reference
     */
    private Task<DocumentReference> getUserPreferencesRef() {
        return getUserRef().continueWith(task -> 
            task.getResult().collection(COLLECTION_PREFERENCES).document("settings")
        );
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
     * Toggle like status for a template with atomic transaction
     */
    public Task<Void> toggleLike(@NonNull String templateId) {
        return getUserId().continueWithTask(task -> {
            if (!task.isSuccessful()) {
                Log.e(TAG, "Cannot toggle like - No authenticated user", task.getException());
                throw task.getException();
            }
            
            String userId = task.getResult();
            Log.d(TAG, String.format("Toggling like for template %s with user %s", templateId, userId));
            
            DocumentReference templateRef = db.collection(COLLECTION_TEMPLATES).document(templateId);
            DocumentReference likeRef = db.collection(COLLECTION_USERS)
                .document(userId)
                .collection(COLLECTION_LIKES)
                .document(templateId);
            
            // First ensure template exists
            return ensureTemplateExists(templateId).continueWithTask(task1 -> {
                if (!task1.isSuccessful()) {
                    Log.e(TAG, "Failed to ensure template exists", task1.getException());
                    return Tasks.forException(task1.getException());
                }
                
                return performLikeToggle(likeRef);
            });
        });
    }
    
    private Task<Void> ensureTemplateExists(String templateId) {
        DocumentReference templateRef = db.collection(COLLECTION_TEMPLATES).document(templateId);
        return templateRef.get().continueWithTask(task -> {
            if (task.isSuccessful() && !task.getResult().exists()) {
                Log.d(TAG, String.format("Creating template document %s with initial data", templateId));
                Map<String, Object> initialData = new HashMap<>();
                initialData.put(FIELD_LIKE_COUNT, 0L);  // Use Long instead of int
                initialData.put(FIELD_FAVORITE_COUNT, 0L);  // Use Long instead of int
                initialData.put("createdAt", FieldValue.serverTimestamp());
                initialData.put("updatedAt", FieldValue.serverTimestamp());
                return templateRef.set(initialData);
            }
            return Tasks.forResult(null);
        });
    }

    private Task<Void> performLikeToggle(DocumentReference likeRef) {
        String templateId = likeRef.getId();
        DocumentReference templateRef = db.collection(COLLECTION_TEMPLATES).document(templateId);
        
        return getUserId().continueWithTask(task -> {
            if (!task.isSuccessful()) {
                Log.e(TAG, "Failed to get user ID", task.getException());
                throw task.getException();
            }
            
            String userId = task.getResult();
            Log.d(TAG, String.format("Starting like toggle for template %s - User: %s", templateId, userId));
            
            return likeRef.get().continueWithTask(likeTask -> {
                if (!likeTask.isSuccessful()) {
                    return Tasks.forException(likeTask.getException());
                }
                
                DocumentSnapshot likeDoc = likeTask.getResult();
                boolean isLiked = likeDoc.exists();
                
                if (isLiked) {
                    // Unlike - delete the like document and decrement count
                    Log.d(TAG, String.format("Removing like - Template: %s, User: %s", templateId, userId));
                    return likeRef.delete()
                        .continueWithTask(deleteTask -> updateLikeCount(templateRef, false));
                } else {
                    // Like - create the like document and increment count
                    Log.d(TAG, String.format("Adding like - Template: %s, User: %s", templateId, userId));
                    Map<String, Object> likeData = new HashMap<>();
                    likeData.put("templateId", templateId);
                    likeData.put("userId", userId);
                    likeData.put("timestamp", FieldValue.serverTimestamp());
                    
                    return likeRef.set(likeData)
                        .continueWithTask(setTask -> updateLikeCount(templateRef, true));
                }
            });
        });
    }
    
    private Task<Void> updateLikeCount(DocumentReference templateRef, boolean increment) {
        return db.runTransaction((Transaction.Function<Void>) transaction -> {
            DocumentSnapshot snapshot = transaction.get(templateRef);
            Long currentCount = snapshot.getLong(FIELD_LIKE_COUNT);
            if (currentCount == null) {
                currentCount = 0L;
            }
            
            long newCount = currentCount + (increment ? 1L : -1L);
            if (newCount < 0L) newCount = 0L;
            
            Log.d(TAG, String.format("Updating like count - Template: %s, Current: %d, New: %d", 
                templateRef.getId(), currentCount, newCount));
                
            // Only update likeCount and required fields
            Map<String, Object> updates = new HashMap<>();
            updates.put(FIELD_LIKE_COUNT, newCount);
            updates.put("updatedAt", FieldValue.serverTimestamp());
            updates.put("userId", fcmToken);
            
            transaction.update(templateRef, updates);
            return null;
        });
    }

    /**
     * Toggle favorite status for a template with atomic transaction
     */
    public Task<Void> toggleFavorite(@NonNull String templateId) {
        return getUserId().continueWithTask(task -> {
            if (!task.isSuccessful()) {
                Log.e(TAG, "Cannot toggle favorite - No authenticated user", task.getException());
                throw task.getException();
            }
            
            String userId = task.getResult();
            Log.d(TAG, String.format("Toggling favorite for template %s with user %s", templateId, userId));
            
            DocumentReference templateRef = db.collection(COLLECTION_TEMPLATES).document(templateId);
            DocumentReference favoriteRef = db.collection(COLLECTION_USERS)
                .document(userId)
                .collection(COLLECTION_FAVORITES)
                .document(templateId);
            
            // First ensure template exists
            return ensureTemplateExists(templateId).continueWithTask(task1 -> {
                if (!task1.isSuccessful()) {
                    Log.e(TAG, "Failed to ensure template exists", task1.getException());
                    return Tasks.forException(task1.getException());
                }
                
                return performFavoriteToggle(favoriteRef);
            });
        });
    }
    
    private Task<Void> performFavoriteToggle(DocumentReference favoriteRef) {
        String templateId = favoriteRef.getId();
        DocumentReference templateRef = db.collection(COLLECTION_TEMPLATES).document(templateId);
        
        return getUserId().continueWithTask(task -> {
            if (!task.isSuccessful()) {
                Log.e(TAG, "Failed to get user ID", task.getException());
                throw task.getException();
            }
            
            String userId = task.getResult();
            Log.d(TAG, String.format("Starting favorite toggle for template %s - User: %s", templateId, userId));
            
            return favoriteRef.get().continueWithTask(favoriteTask -> {
                if (!favoriteTask.isSuccessful()) {
                    return Tasks.forException(favoriteTask.getException());
                }
                
                DocumentSnapshot favoriteDoc = favoriteTask.getResult();
                boolean isFavorited = favoriteDoc.exists();
                
                if (isFavorited) {
                    // Unfavorite - delete the favorite document and decrement count
                    Log.d(TAG, String.format("Removing favorite - Template: %s, User: %s", templateId, userId));
                    return favoriteRef.delete()
                        .continueWithTask(deleteTask -> updateFavoriteCount(templateRef, false));
                } else {
                    // Favorite - create the favorite document and increment count
                    Log.d(TAG, String.format("Adding favorite - Template: %s, User: %s", templateId, userId));
                    Map<String, Object> favoriteData = new HashMap<>();
                    favoriteData.put("templateId", templateId);
                    favoriteData.put("userId", userId);
                    favoriteData.put("timestamp", FieldValue.serverTimestamp());
                    
                    return favoriteRef.set(favoriteData)
                        .continueWithTask(setTask -> updateFavoriteCount(templateRef, true));
                }
            });
        });
    }

    private Task<Void> updateFavoriteCount(DocumentReference templateRef, boolean increment) {
        return db.runTransaction((Transaction.Function<Void>) transaction -> {
            DocumentSnapshot snapshot = transaction.get(templateRef);
            Long currentCount = snapshot.getLong(FIELD_FAVORITE_COUNT);
            if (currentCount == null) {
                currentCount = 0L;
            }
            
            long newCount = currentCount + (increment ? 1L : -1L);
            if (newCount < 0L) newCount = 0L;
            
            Log.d(TAG, String.format("Updating favorite count - Template: %s, Current: %d, New: %d", 
                templateRef.getId(), currentCount, newCount));
                
            // Only update favoriteCount and required fields
            Map<String, Object> updates = new HashMap<>();
            updates.put(FIELD_FAVORITE_COUNT, newCount);
            updates.put("updatedAt", FieldValue.serverTimestamp());
            updates.put("userId", fcmToken);
            
            transaction.update(templateRef, updates);
            return null;
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
} 