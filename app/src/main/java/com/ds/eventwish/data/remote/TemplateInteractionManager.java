package com.ds.eventwish.data.remote;

import android.util.Log;
import androidx.annotation.NonNull;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Transaction;

import java.util.HashMap;
import java.util.Map;

public class TemplateInteractionManager {
    private static final String TAG = "TemplateInteractionMgr";
    private static TemplateInteractionManager instance;
    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    private TemplateInteractionManager() {
        this.db = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
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

    public Task<Void> toggleLike(@NonNull String templateId) {
        return db.runTransaction((Transaction.Function<Void>) transaction -> {
            String userId = getCurrentUserId();
            
            // References
            DocumentReference templateRef = db.collection("templates").document(templateId);
            DocumentReference userLikeRef = db.collection("users").document(userId)
                .collection("likes").document(templateId);
            
            // Check current like state
            boolean isLiked = transaction.get(userLikeRef).exists();
            
            if (isLiked) {
                // Unlike
                transaction.delete(userLikeRef);
                transaction.update(templateRef, "likeCount", FieldValue.increment(-1));
            } else {
                // Like
                Map<String, Object> likeData = new HashMap<>();
                likeData.put("templateId", templateId);
                likeData.put("timestamp", FieldValue.serverTimestamp());
                
                transaction.set(userLikeRef, likeData);
                transaction.update(templateRef, "likeCount", FieldValue.increment(1));
            }
            
            transaction.update(templateRef, "lastUpdated", FieldValue.serverTimestamp());
            
            return null;
        }).addOnSuccessListener(aVoid -> {
            Log.d(TAG, "Successfully toggled like for template: " + templateId);
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error toggling like for template: " + templateId, e);
        });
    }

    public Task<Void> toggleFavorite(@NonNull String templateId) {
        return db.runTransaction((Transaction.Function<Void>) transaction -> {
            String userId = getCurrentUserId();
            
            // References
            DocumentReference templateRef = db.collection("templates").document(templateId);
            DocumentReference userFavoriteRef = db.collection("users").document(userId)
                .collection("favorites").document(templateId);
            
            // Check current favorite state
            boolean isFavorited = transaction.get(userFavoriteRef).exists();
            
            if (isFavorited) {
                // Unfavorite
                transaction.delete(userFavoriteRef);
                transaction.update(templateRef, "favoriteCount", FieldValue.increment(-1));
            } else {
                // Favorite
                Map<String, Object> favoriteData = new HashMap<>();
                favoriteData.put("templateId", templateId);
                favoriteData.put("timestamp", FieldValue.serverTimestamp());
                
                transaction.set(userFavoriteRef, favoriteData);
                transaction.update(templateRef, "favoriteCount", FieldValue.increment(1));
            }
            
            transaction.update(templateRef, "lastUpdated", FieldValue.serverTimestamp());
            
            return null;
        }).addOnSuccessListener(aVoid -> {
            Log.d(TAG, "Successfully toggled favorite for template: " + templateId);
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error toggling favorite for template: " + templateId, e);
        });
    }

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