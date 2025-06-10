package com.ds.eventwish.data.remote;

import android.util.Log;
import androidx.annotation.NonNull;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * Manager class for template interactions like likes and favorites
 */
public class TemplateInteractionManager {
    private static final String TAG = "TemplateInteractionMgr";
    private static TemplateInteractionManager instance;
    private final FirebaseFirestore db;
    private final FirebaseAuth auth;
    private final FirestoreManager firestoreManager;

    private TemplateInteractionManager() {
        this.db = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
        this.firestoreManager = FirestoreManager.getInstance();
    }

    public static synchronized TemplateInteractionManager getInstance() {
        if (instance == null) {
            instance = new TemplateInteractionManager();
        }
        return instance;
    }

    private String getCurrentUserId() {
        if (auth.getCurrentUser() == null) {
            throw new IllegalStateException("User must be signed in");
        }
        return auth.getCurrentUser().getUid();
    }

    /**
     * Toggle like status for a template
     * 
     * @param templateId ID of the template to like/unlike
     * @return Task representing the operation
     */
    public Task<Void> toggleLike(@NonNull String templateId) {
        Log.d(TAG, "Starting toggleLike for template: " + templateId);
        
        String userId = getCurrentUserId();
        DocumentReference userLikeRef = db.collection("users").document(userId)
            .collection("likes").document(templateId);
        
        return userLikeRef.get().continueWithTask(task -> {
            if (!task.isSuccessful()) {
                Log.e(TAG, "Error checking like status", task.getException());
                return Tasks.forException(task.getException());
            }
            
            boolean isCurrentlyLiked = task.getResult().exists();
            Log.d(TAG, "Current like status: " + isCurrentlyLiked);
            
            if (isCurrentlyLiked) {
                // Unlike: delete the like document and decrement count
                Log.d(TAG, "Removing like for template: " + templateId);
                return userLikeRef.delete().continueWithTask(deleteTask -> {
                    if (!deleteTask.isSuccessful()) {
                        Log.e(TAG, "Error deleting like", deleteTask.getException());
                        return Tasks.forException(deleteTask.getException());
                    }
                    
                    // Safely decrement the like count
                    return firestoreManager.safeIncrementTemplateCounter(
                        templateId, "likeCount", -1);
                });
            } else {
                // Like: create the like document and increment count
                Log.d(TAG, "Adding like for template: " + templateId);
                Map<String, Object> likeData = new HashMap<>();
                likeData.put("templateId", templateId);
                likeData.put("timestamp", FieldValue.serverTimestamp());
                
                return userLikeRef.set(likeData).continueWithTask(setTask -> {
                    if (!setTask.isSuccessful()) {
                        Log.e(TAG, "Error creating like", setTask.getException());
                        return Tasks.forException(setTask.getException());
                    }
                    
                    // Safely increment the like count
                    return firestoreManager.safeIncrementTemplateCounter(
                        templateId, "likeCount", 1);
                });
            }
        })
        .addOnSuccessListener(aVoid -> {
            Log.d(TAG, "Successfully toggled like for template: " + templateId);
        })
        .addOnFailureListener(e -> {
            Log.e(TAG, "Error toggling like for template: " + templateId, e);
        });
    }

    /**
     * Toggle favorite status for a template
     * 
     * @param templateId ID of the template to favorite/unfavorite
     * @return Task representing the operation
     */
    public Task<Void> toggleFavorite(@NonNull String templateId) {
        Log.d(TAG, "Starting toggleFavorite for template: " + templateId);
        
        String userId = getCurrentUserId();
        DocumentReference userFavoriteRef = db.collection("users").document(userId)
            .collection("favorites").document(templateId);
        
        return userFavoriteRef.get().continueWithTask(task -> {
            if (!task.isSuccessful()) {
                Log.e(TAG, "Error checking favorite status", task.getException());
                return Tasks.forException(task.getException());
            }
            
            boolean isCurrentlyFavorited = task.getResult().exists();
            Log.d(TAG, "Current favorite status: " + isCurrentlyFavorited);
            
            if (isCurrentlyFavorited) {
                // Unfavorite: delete the favorite document and decrement count
                Log.d(TAG, "Removing favorite for template: " + templateId);
                return userFavoriteRef.delete().continueWithTask(deleteTask -> {
                    if (!deleteTask.isSuccessful()) {
                        Log.e(TAG, "Error deleting favorite", deleteTask.getException());
                        return Tasks.forException(deleteTask.getException());
                    }
                    
                    // Safely decrement the favorite count
                    return firestoreManager.safeIncrementTemplateCounter(
                        templateId, "favoriteCount", -1);
                });
            } else {
                // Favorite: create the favorite document and increment count
                Log.d(TAG, "Adding favorite for template: " + templateId);
                Map<String, Object> favoriteData = new HashMap<>();
                favoriteData.put("templateId", templateId);
                favoriteData.put("timestamp", FieldValue.serverTimestamp());
                
                return userFavoriteRef.set(favoriteData).continueWithTask(setTask -> {
                    if (!setTask.isSuccessful()) {
                        Log.e(TAG, "Error creating favorite", setTask.getException());
                        return Tasks.forException(setTask.getException());
                    }
                    
                    // Safely increment the favorite count
                    return firestoreManager.safeIncrementTemplateCounter(
                        templateId, "favoriteCount", 1);
                });
            }
        })
        .addOnSuccessListener(aVoid -> {
            Log.d(TAG, "Successfully toggled favorite for template: " + templateId);
        })
        .addOnFailureListener(e -> {
            Log.e(TAG, "Error toggling favorite for template: " + templateId, e);
        });
    }

    /**
     * Check if user has liked a template
     * 
     * @param templateId ID of the template to check
     * @return Task that resolves to true if liked, false otherwise
     */
    public Task<Boolean> checkLikeStatus(@NonNull String templateId) {
        String userId = getCurrentUserId();
        return db.collection("users").document(userId)
            .collection("likes").document(templateId)
            .get()
            .continueWith(task -> {
                if (!task.isSuccessful()) {
                    Log.e(TAG, "Error checking like status", task.getException());
                    throw task.getException();
                }
                return task.getResult().exists();
            });
    }

    /**
     * Check if user has favorited a template
     * 
     * @param templateId ID of the template to check
     * @return Task that resolves to true if favorited, false otherwise
     */
    public Task<Boolean> checkFavoriteStatus(@NonNull String templateId) {
        String userId = getCurrentUserId();
        return db.collection("users").document(userId)
            .collection("favorites").document(templateId)
            .get()
            .continueWith(task -> {
                if (!task.isSuccessful()) {
                    Log.e(TAG, "Error checking favorite status", task.getException());
                    throw task.getException();
                }
                return task.getResult().exists();
            });
    }
} 