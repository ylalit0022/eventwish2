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
import com.ds.eventwish.utils.AnalyticsUtils;
import com.google.android.gms.tasks.Task;
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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

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
     * Get current user ID from Firebase Auth
     */
    private Task<String> getUserId() {
        FirebaseUser user = authManager.getCurrentUser();
        if (user != null) {
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
        DocumentReference templateRef = db.collection(COLLECTION_TEMPLATES).document(templateId);
        
        return db.runTransaction(transaction -> {
            // First check if template exists, if not create it
            DocumentSnapshot templateSnapshot = transaction.get(templateRef);
            if (!templateSnapshot.exists()) {
                // Create template document with initial stats
                Map<String, Object> templateData = new HashMap<>();
                templateData.put("id", templateId);
                templateData.put("likeCount", 0);
                templateData.put("favoriteCount", 0);
                templateData.put("lastUpdated", FieldValue.serverTimestamp());
                transaction.set(templateRef, templateData);
            }
            
            // Check if user has liked this template
            DocumentSnapshot likeSnapshot = transaction.get(likeRef);
            boolean isLiked = likeSnapshot.exists();
            
            if (isLiked) {
                // Unlike: Delete like document and decrement count
                transaction.delete(likeRef);
                transaction.update(templateRef, "likeCount", FieldValue.increment(-1));
                transaction.update(templateRef, "lastUpdated", FieldValue.serverTimestamp());
                Log.d(TAG, "Removed like for template: " + templateId);
            } else {
                // Like: Create like document and increment count
                Map<String, Object> likeData = new HashMap<>();
                likeData.put("templateId", templateId);
                likeData.put("userId", userId);
                likeData.put("timestamp", FieldValue.serverTimestamp());
                transaction.set(likeRef, likeData);
                transaction.update(templateRef, "likeCount", FieldValue.increment(1));
                transaction.update(templateRef, "lastUpdated", FieldValue.serverTimestamp());
                Log.d(TAG, "Added like for template: " + templateId);
            }
            
            return !isLiked; // Return new like state
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
            Log.e(TAG, "Error toggling template like", e);
            // Track error event
            Bundle params = new Bundle();
            params.putString("template_id", templateId);
            params.putString("error", e.getMessage());
            AnalyticsUtils.getInstance().logEvent("template_like_error", params);
        });
    }
    
    /**
     * Update template like count
     */
    private Task<Void> updateTemplateLikeCount(String templateId, int increment) {
        DocumentReference templateRef = db.collection(COLLECTION_TEMPLATES).document(templateId);
        Map<String, Object> updates = new HashMap<>();
        updates.put(FIELD_LIKE_COUNT, FieldValue.increment(increment));
        updates.put("lastUpdated", FieldValue.serverTimestamp());
        
        return templateRef.update(updates)
            .addOnSuccessListener(aVoid -> Log.d(TAG, "Template like count updated successfully"))
            .addOnFailureListener(e -> Log.e(TAG, "Failed to update template like count", e));
    }

    /**
     * Toggle favorite status for a template with atomic transaction
     */
    public Task<Boolean> toggleFavorite(String templateId) {
        Log.d(TAG, "Toggling favorite for template: " + templateId);
        
        // Get current user from AuthManager
        FirebaseUser user = AuthManager.getInstance().getCurrentUser();
        if (user == null) {
            Log.e(TAG, "Cannot toggle favorite: User not signed in");
            return Tasks.forException(new IllegalStateException("User not signed in"));
        }
        
        return isTemplateFavorited(templateId).continueWithTask(task -> {
            if (!task.isSuccessful()) {
                return Tasks.forException(task.getException());
            }
            
            boolean isCurrentlyFavorited = task.getResult();
            DocumentReference favoriteRef = db.collection(COLLECTION_USERS)
                    .document(user.getUid())
                    .collection(COLLECTION_FAVORITES)
                    .document(templateId);
            
            Map<String, Object> favoriteData = new HashMap<>();
            favoriteData.put("templateId", templateId);
            favoriteData.put("timestamp", FieldValue.serverTimestamp());
            
            if (isCurrentlyFavorited) {
                Log.d(TAG, "Removing favorite for template: " + templateId);
                return favoriteRef.delete()
                        .continueWith(deleteTask -> !isCurrentlyFavorited);
            } else {
                Log.d(TAG, "Adding favorite for template: " + templateId);
                return favoriteRef.set(favoriteData)
                        .continueWith(setTask -> !isCurrentlyFavorited);
            }
        });
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
} 