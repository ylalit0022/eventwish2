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
     * Set FCM token for user identification
     */
    public void setFcmToken(String token) {
        Log.d(TAG, "Setting FCM token: " + token);
        this.fcmToken = token;
        
        // Create or update user document
        if (token != null) {
            DocumentReference userRef = db.collection(COLLECTION_USERS).document(token);
            Map<String, Object> userData = new HashMap<>();
            userData.put("fcmToken", token);
            userData.put("lastUpdated", FieldValue.serverTimestamp());
            
            userRef.set(userData)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "User document created/updated successfully"))
                .addOnFailureListener(e -> Log.e(TAG, "Error creating/updating user document", e));
        } else {
            Log.e(TAG, "Attempted to set null FCM token");
        }
    }

    /**
     * Check if FCM token is set
     */
    private Task<Void> ensureTokenSet() {
        if (fcmToken == null || fcmToken.isEmpty()) {
            Log.e(TAG, "FCM token not set. Please set token before performing operations.");
            return Tasks.forException(new IllegalStateException("FCM token not set"));
        }
        return Tasks.forResult(null);
    }

    /**
     * Get user preferences document reference
     */
    private DocumentReference getUserPreferencesRef() {
        if (fcmToken == null) {
            throw new IllegalStateException("FCM token not set");
        }
        return db.collection(COLLECTION_USERS)
                .document(fcmToken)
                .collection(COLLECTION_PREFERENCES)
                .document("settings");
    }

    /**
     * Get user preferences
     */
    public Task<DocumentSnapshot> getUserPreferences() {
        return ensureTokenSet()
            .continueWithTask(task -> {
                if (!task.isSuccessful()) {
                    return Tasks.forException(task.getException());
                }
                return getUserPreferencesRef().get();
            });
    }

    /**
     * Update user preferences
     */
    public Task<Void> updateUserPreferences(Map<String, Object> data) {
        return ensureTokenSet().continueWithTask(task -> {
            if (!task.isSuccessful()) {
                return Tasks.forException(task.getException());
            }
            return getUserPreferencesRef().set(data, SetOptions.merge());
        });
    }

    /**
     * Add template to favorites
     */
    public Task<Void> addToFavorites(String templateId) {
        return ensureTokenSet()
            .continueWithTask(task -> {
                if (!task.isSuccessful()) {
                    return Tasks.forException(task.getException());
                }
                
                DocumentReference favoriteRef = db.collection(COLLECTION_USERS)
                        .document(fcmToken)
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
        return ensureTokenSet()
            .continueWithTask(task -> {
                if (!task.isSuccessful()) {
                    return Tasks.forException(task.getException());
                }
                
                return db.collection(COLLECTION_USERS)
                        .document(fcmToken)
                        .collection(COLLECTION_FAVORITES)
                        .document(templateId)
                        .delete();
            });
    }

    /**
     * Add template to likes
     */
    public Task<Void> addToLikes(String templateId) {
        return ensureTokenSet()
            .continueWithTask(task -> {
                if (!task.isSuccessful()) {
                    return Tasks.forException(task.getException());
                }
                
                DocumentReference likeRef = db.collection(COLLECTION_USERS)
                        .document(fcmToken)
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
        return ensureTokenSet()
            .continueWithTask(task -> {
                if (!task.isSuccessful()) {
                    return Tasks.forException(task.getException());
                }
                
                return db.collection(COLLECTION_USERS)
                        .document(fcmToken)
                        .collection(COLLECTION_LIKES)
                        .document(templateId)
                        .delete();
            });
    }

    /**
     * Check if template is favorited
     */
    public Task<Boolean> isTemplateFavorited(@NonNull String templateId) {
        if (fcmToken == null) {
            Log.e(TAG, "Cannot check favorite status - FCM token is null");
            return Tasks.forException(new IllegalStateException("FCM token is null"));
        }
        
        return db.collection(COLLECTION_USERS)
            .document(fcmToken)
            .get()
            .continueWith(task -> {
                if (!task.isSuccessful()) {
                    Log.e(TAG, "Error checking favorite status", task.getException());
                    throw task.getException();
                }
                
                DocumentSnapshot document = task.getResult();
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
    }

    /**
     * Check if template is liked
     */
    public Task<Boolean> isTemplateLiked(@NonNull String templateId) {
        if (fcmToken == null) {
            Log.e(TAG, "Cannot check like status - FCM token is null");
            return Tasks.forException(new IllegalStateException("FCM token is null"));
        }
        
        return db.collection(COLLECTION_USERS)
            .document(fcmToken)
            .get()
            .continueWith(task -> {
                if (!task.isSuccessful()) {
                    Log.e(TAG, "Error checking like status", task.getException());
                    throw task.getException();
                }
                
                DocumentSnapshot document = task.getResult();
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
    }

    /**
     * Update notification preferences
     */
    public Task<Void> updateNotificationPreferences(NotificationPreference preferences) {
        DocumentReference prefsRef = getUserPreferencesRef();
        Map<String, Object> updates = new HashMap<>();
        updates.put("notificationPreferences." + preferences.getType(), preferences);
        return prefsRef.set(updates, SetOptions.merge());
    }

    /**
     * Add notification record
     */
    public Task<DocumentReference> addNotification(Map<String, Object> notification) {
        return db.collection(COLLECTION_USERS)
                .document(fcmToken)
                .collection(COLLECTION_NOTIFICATIONS)
                .add(notification);
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
    public DocumentReference getFavoriteRef(String templateId) {
        return db.collection(COLLECTION_USERS)
                .document(fcmToken)
                .collection(COLLECTION_FAVORITES)
                .document(templateId);
    }

    /**
     * Get like document reference
     */
    public DocumentReference getLikeRef(String templateId) {
        return db.collection(COLLECTION_USERS)
                .document(fcmToken)
                .collection(COLLECTION_LIKES)
                .document(templateId);
    }

    /**
     * Get current user ID
     */
    public String getCurrentUserId() {
        if (fcmToken == null) {
            Log.e(TAG, "FCM token is null");
        }
        return fcmToken;
    }

    /**
     * Check if user is signed in
     */
    public boolean isUserSignedIn() {
        return fcmToken != null && !fcmToken.isEmpty();
    }

    /**
     * Toggle like status for a template with atomic transaction
     */
    public Task<Void> toggleLike(@NonNull String templateId) {
        Log.d(TAG, String.format("Toggling like for template %s with token %s", templateId, fcmToken));
        
        if (fcmToken == null) {
            Log.e(TAG, "Cannot toggle like - FCM token is null");
            return Tasks.forException(new IllegalStateException("FCM token is null"));
        }
        
        DocumentReference templateRef = db.collection(COLLECTION_TEMPLATES).document(templateId);
        DocumentReference likeRef = db.collection(COLLECTION_USERS)
            .document(fcmToken)
            .collection(COLLECTION_LIKES)
            .document(templateId);
        
        // First ensure template exists
        return ensureTemplateExists(templateId).continueWithTask(task -> {
            if (!task.isSuccessful()) {
                Log.e(TAG, "Failed to ensure template exists", task.getException());
                return Tasks.forException(task.getException());
            }
            
            return performLikeToggle(likeRef);
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
        
        if (fcmToken == null) {
            Log.e(TAG, "Cannot toggle like - FCM token is null");
            return Tasks.forException(new IllegalStateException("FCM token is null"));
        }
        
        Log.d(TAG, String.format("Starting like toggle for template %s - User: %s", templateId, fcmToken));
        
        return ensureTemplateExists(templateId).continueWithTask(task -> {
            if (!task.isSuccessful()) {
                Log.e(TAG, "Failed to ensure template exists", task.getException());
                return Tasks.forException(task.getException());
            }
            
            return likeRef.get().continueWithTask(likeTask -> {
                if (!likeTask.isSuccessful()) {
                    return Tasks.forException(likeTask.getException());
                }
                
                DocumentSnapshot likeDoc = likeTask.getResult();
                boolean isLiked = likeDoc.exists();
                
                if (isLiked) {
                    // Unlike - delete the like document and decrement count
                    Log.d(TAG, String.format("Removing like - Template: %s, User: %s", templateId, fcmToken));
                    return likeRef.delete()
                        .continueWithTask(deleteTask -> updateLikeCount(templateRef, false));
                } else {
                    // Like - create the like document and increment count
                    Log.d(TAG, String.format("Adding like - Template: %s, User: %s", templateId, fcmToken));
                    Map<String, Object> likeData = new HashMap<>();
                    likeData.put("templateId", templateId);
                    likeData.put("userId", fcmToken);
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
        Log.d(TAG, String.format("Toggling favorite for template %s with token %s", templateId, fcmToken));
        
        if (fcmToken == null) {
            Log.e(TAG, "Cannot toggle favorite - FCM token is null");
            return Tasks.forException(new IllegalStateException("FCM token is null"));
        }
        
        DocumentReference templateRef = db.collection(COLLECTION_TEMPLATES).document(templateId);
        DocumentReference favoriteRef = db.collection(COLLECTION_USERS)
            .document(fcmToken)
            .collection(COLLECTION_FAVORITES)
            .document(templateId);
        
        // First ensure template exists
        return ensureTemplateExists(templateId).continueWithTask(task -> {
            if (!task.isSuccessful()) {
                Log.e(TAG, "Failed to ensure template exists", task.getException());
                return Tasks.forException(task.getException());
            }
            
            return performFavoriteToggle(favoriteRef);
        });
    }
    
    private Task<Void> performFavoriteToggle(DocumentReference favoriteRef) {
        String templateId = favoriteRef.getId();
        DocumentReference templateRef = db.collection(COLLECTION_TEMPLATES).document(templateId);
        
        if (fcmToken == null) {
            Log.e(TAG, "Cannot toggle favorite - FCM token is null");
            return Tasks.forException(new IllegalStateException("FCM token is null"));
        }
        
        Log.d(TAG, String.format("Starting favorite toggle for template %s - User: %s", templateId, fcmToken));
        
        return ensureTemplateExists(templateId).continueWithTask(task -> {
            if (!task.isSuccessful()) {
                Log.e(TAG, "Failed to ensure template exists", task.getException());
                return Tasks.forException(task.getException());
            }
            
            return favoriteRef.get().continueWithTask(favoriteTask -> {
                if (!favoriteTask.isSuccessful()) {
                    return Tasks.forException(favoriteTask.getException());
                }
                
                DocumentSnapshot favoriteDoc = favoriteTask.getResult();
                boolean isFavorited = favoriteDoc.exists();
                
                if (isFavorited) {
                    // Unfavorite - delete the favorite document and decrement count
                    Log.d(TAG, String.format("Removing favorite - Template: %s, User: %s", templateId, fcmToken));
                    return favoriteRef.delete()
                        .continueWithTask(deleteTask -> updateFavoriteCount(templateRef, false));
                } else {
                    // Favorite - create the favorite document and increment count
                    Log.d(TAG, String.format("Adding favorite - Template: %s, User: %s", templateId, fcmToken));
                    Map<String, Object> favoriteData = new HashMap<>();
                    favoriteData.put("templateId", templateId);
                    favoriteData.put("userId", fcmToken);
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
        if (fcmToken == null) {
            Log.e(TAG, "Cannot get user interactions - FCM token is null");
            return Tasks.forException(new IllegalStateException("FCM token is null"));
        }

        // Just get likes since that's what the code expects
        return db.collection(COLLECTION_USERS)
                .document(fcmToken)
                .collection(COLLECTION_LIKES)
                .get();
    }
} 