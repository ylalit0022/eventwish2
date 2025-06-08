package com.ds.eventwish.data.repository;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.ds.eventwish.data.model.Template;
import com.ds.eventwish.data.remote.FirestoreManager;
import com.ds.eventwish.utils.AnalyticsUtils;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.Transaction;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

/**
 * Repository class to handle template interactions (likes and favorites)
 */
public class TemplateInteractionRepository {
    private static final String TAG = "TemplateInteractionRepo";
    private static final String COLLECTION_USERS = "users";
    private static final String COLLECTION_TEMPLATES = "templates";
    private static final String COLLECTION_LIKES = "likes";
    private static final String COLLECTION_FAVORITES = "favorites";
    private static final String FIELD_LIKE_COUNT = "likeCount";
    private static final String FIELD_FAVORITE_COUNT = "favoriteCount";

    private static volatile TemplateInteractionRepository instance;
    private final FirebaseFirestore db;
    private final Context context;
    private final Map<String, ListenerRegistration> templateListeners;
    private final Map<String, MutableLiveData<Boolean>> likeStates;
    private final Map<String, MutableLiveData<Boolean>> favoriteStates;
    private final Map<String, MutableLiveData<Integer>> likeCounts;
    private final UserRepository userRepository;
    private final Handler mainHandler;
    private final FirestoreManager firestoreManager;

    /**
     * Private constructor for singleton pattern
     */
    private TemplateInteractionRepository(Context context) {
        this.context = context.getApplicationContext();
        this.db = FirebaseFirestore.getInstance();
        this.templateListeners = new ConcurrentHashMap<>();
        this.likeStates = new ConcurrentHashMap<>();
        this.favoriteStates = new ConcurrentHashMap<>();
        this.likeCounts = new ConcurrentHashMap<>();
        this.userRepository = UserRepository.getInstance(context);
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.firestoreManager = FirestoreManager.getInstance();
    }

    /**
     * Get singleton instance
     */
    public static TemplateInteractionRepository getInstance(Context context) {
        if (instance == null) {
            synchronized (TemplateInteractionRepository.class) {
                if (instance == null) {
                    instance = new TemplateInteractionRepository(context);
                }
            }
        }
        return instance;
    }

    /**
     * Start observing a template's interactions
     */
    public void startObservingTemplate(String templateId) {
        if (templateId == null || templateId.isEmpty()) {
            Log.e(TAG, "Cannot observe template with null or empty ID");
            return;
        }

        // Create LiveData objects if they don't exist
        likeStates.putIfAbsent(templateId, new MutableLiveData<>(false));
        favoriteStates.putIfAbsent(templateId, new MutableLiveData<>(false));
        likeCounts.putIfAbsent(templateId, new MutableLiveData<>(0));

        // Start listening to template document
        ListenerRegistration registration = db.collection(COLLECTION_TEMPLATES)
            .document(templateId)
            .addSnapshotListener((snapshot, e) -> {
                if (e != null) {
                    Log.e(TAG, "Error listening to template: " + templateId, e);
                    return;
                }

                if (snapshot != null && snapshot.exists()) {
                    updateTemplateState(templateId, snapshot);
                }
            });

        templateListeners.put(templateId, registration);
    }

    /**
     * Stop observing a template's interactions
     */
    public void stopObservingTemplate(String templateId) {
        ListenerRegistration registration = templateListeners.remove(templateId);
        if (registration != null) {
            registration.remove();
        }
    }

    /**
     * Toggle like state for a template
     */
    public Task<Void> toggleLike(Template template) {
        if (template == null || template.getId() == null) {
            Log.e(TAG, "Cannot toggle like: template or template ID is null");
            return Tasks.forException(new IllegalArgumentException("Template or template ID is null"));
        }

        String templateId = template.getId();
        Log.d(TAG, String.format("Starting like toggle operation - TemplateID: %s, Current state: %b", 
            templateId, template.isLiked()));
        logInteractionEvent(templateId, "LIKE_TOGGLE", "Started");

        return firestoreManager.toggleLike(templateId)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, String.format("Like toggle successful - TemplateID: %s", templateId));
                logInteractionEvent(templateId, "LIKE_TOGGLE", "Success");
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, String.format("Like toggle failed - TemplateID: %s, Error: %s", 
                    templateId, e.getMessage()), e);
                logInteractionEvent(templateId, "LIKE_TOGGLE", "Failed: " + e.getMessage());
                handleLikeError(templateId, e);
            });
    }

    private void updateLocalLikeState(String templateId, boolean isLiked) {
        // Update local state
        MutableLiveData<Boolean> likeState = likeStates.get(templateId);
        if (likeState != null) {
            mainHandler.post(() -> likeState.setValue(isLiked));
        }

        // Update like count
        MutableLiveData<Integer> count = likeCounts.get(templateId);
        if (count != null) {
            Integer currentCount = count.getValue();
            if (currentCount != null) {
                mainHandler.post(() -> count.setValue(isLiked ? currentCount + 1 : Math.max(0, currentCount - 1)));
            }
        }

        // Track the interaction
        Bundle params = new Bundle();
        params.putString("template_id", templateId);
        params.putString("interaction_type", isLiked ? "like_add" : "like_remove");
        AnalyticsUtils.getInstance().logEvent("template_interaction", params);
    }

    private void handleLikeError(String templateId, Exception e) {
        if (e instanceof FirebaseFirestoreException) {
            FirebaseFirestoreException ffe = (FirebaseFirestoreException) e;
            if (ffe.getCode() == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                Log.e(TAG, "Permission denied while updating template: " + templateId);
                // Store failed operation for retry
                storeFailedOperation(templateId, "like");
            }
        }
        // Track error
        Bundle params = new Bundle();
        params.putString("error_type", "like_operation_failed");
        params.putString("error_message", e.getMessage());
        params.putString("template_id", templateId);
        AnalyticsUtils.getInstance().logEvent("template_interaction_error", params);
    }

    /**
     * Toggle favorite state for a template
     */
    public Task<Void> toggleFavorite(Template template) {
        if (template == null || template.getId() == null) {
            Log.e(TAG, "Cannot toggle favorite: template or template ID is null");
            return Tasks.forException(new IllegalArgumentException("Template or template ID is null"));
        }

        String templateId = template.getId();
        Log.d(TAG, String.format("Starting favorite toggle operation - TemplateID: %s, Current state: %b", 
            templateId, template.isFavorited()));
        logInteractionEvent(templateId, "FAVORITE_TOGGLE", "Started");

        return firestoreManager.toggleFavorite(templateId)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, String.format("Favorite toggle successful - TemplateID: %s", templateId));
                logInteractionEvent(templateId, "FAVORITE_TOGGLE", "Success");
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, String.format("Favorite toggle failed - TemplateID: %s, Error: %s", 
                    templateId, e.getMessage()), e);
                logInteractionEvent(templateId, "FAVORITE_TOGGLE", "Failed: " + e.getMessage());
                handleFavoriteError(templateId, e);
            });
    }

    private void updateLocalFavoriteState(String templateId, boolean isFavorited) {
        MutableLiveData<Boolean> favoriteState = favoriteStates.get(templateId);
        if (favoriteState != null) {
            mainHandler.post(() -> favoriteState.setValue(isFavorited));
        }

        // Track the interaction
        Bundle params = new Bundle();
        params.putString("template_id", templateId);
        params.putString("interaction_type", isFavorited ? "favorite_add" : "favorite_remove");
        AnalyticsUtils.getInstance().logEvent("template_interaction", params);
    }

    private void handleFavoriteError(String templateId, Exception e) {
        if (e instanceof FirebaseFirestoreException) {
            FirebaseFirestoreException ffe = (FirebaseFirestoreException) e;
            if (ffe.getCode() == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                Log.e(TAG, "Permission denied while updating template: " + templateId);
                storeFailedOperation(templateId, "favorite");
            }
        }
        // Track error
        Bundle params = new Bundle();
        params.putString("error_type", "favorite_operation_failed");
        params.putString("error_message", e.getMessage());
        params.putString("template_id", templateId);
        AnalyticsUtils.getInstance().logEvent("template_interaction_error", params);
    }

    /**
     * Get like state for a template
     */
    public LiveData<Boolean> getLikeState(String templateId) {
        return likeStates.computeIfAbsent(templateId, k -> new MutableLiveData<>(false));
    }

    /**
     * Get favorite state for a template
     */
    public LiveData<Boolean> getFavoriteState(String templateId) {
        return favoriteStates.computeIfAbsent(templateId, k -> new MutableLiveData<>(false));
    }

    /**
     * Get like count for a template
     */
    public LiveData<Integer> getLikeCount(String templateId) {
        return likeCounts.computeIfAbsent(templateId, k -> new MutableLiveData<>(0));
    }

    /**
     * Update template state from snapshot
     */
    private void updateTemplateState(String templateId, DocumentSnapshot snapshot) {
        // Update like count
        Long likeCount = snapshot.getLong(FIELD_LIKE_COUNT);
        if (likeCount != null) {
            MutableLiveData<Integer> liveData = likeCounts.get(templateId);
            if (liveData != null) {
                liveData.postValue(likeCount.intValue());
            }
        }

                    // Update like and favorite states from user interactions
        String userId = userRepository.getCurrentUserId();
        if (userId != null) {
            // Get user's likes collection
            db.collection(COLLECTION_USERS)
                .document(userId)
                .collection(COLLECTION_LIKES)
                .document(templateId)
                .get()
                .addOnSuccessListener(likeDoc -> {
                    MutableLiveData<Boolean> likeState = likeStates.get(templateId);
                    if (likeState != null) {
                        likeState.postValue(likeDoc.exists());
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error getting like state", e));

            // Get user's favorites collection
            db.collection(COLLECTION_USERS)
                .document(userId)
                .collection(COLLECTION_FAVORITES)
                .document(templateId)
                .get()
                .addOnSuccessListener(favoriteDoc -> {
                    MutableLiveData<Boolean> favoriteState = favoriteStates.get(templateId);
                    if (favoriteState != null) {
                        favoriteState.postValue(favoriteDoc.exists());
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error getting favorite state", e));
        }
    }

    /**
     * Retry toggle favorite operation
     */
    private void retryToggleFavorite(Template template) {
        if (template == null) return;
        
        // Add exponential backoff retry
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            AtomicBoolean retrySuccessful = new AtomicBoolean(false);
            int maxRetries = 3;
            int retryCount = 0;
            
            while (!retrySuccessful.get() && retryCount < maxRetries) {
                try {
                    toggleFavorite(template);
                    retrySuccessful.set(true);
                } catch (Exception e) {
                    Log.e(TAG, "Retry attempt " + (retryCount + 1) + " failed", e);
                    retryCount++;
                    if (retryCount < maxRetries) {
                        try {
                            Thread.sleep(1000 * (long) Math.pow(2, retryCount));
                        } catch (InterruptedException ie) {
                            Log.e(TAG, "Sleep interrupted during retry", ie);
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
            }
            
            if (!retrySuccessful.get()) {
                // Store failed operation for later retry
                storeFailedOperation(template.getId(), "favorite");
            }
        }, 1000);
    }

    private void storeFailedOperation(String templateId, String operationType) {
        SharedPreferences prefs = context.getSharedPreferences("failed_operations", Context.MODE_PRIVATE);
        String operations = prefs.getString("pending_operations", "");
        
        // Store as JSON string
        try {
            JSONObject operation = new JSONObject();
            operation.put("templateId", templateId);
            operation.put("type", operationType);
            operation.put("timestamp", System.currentTimeMillis());
            
            JSONArray pendingOps;
            if (operations.isEmpty()) {
                pendingOps = new JSONArray();
            } else {
                pendingOps = new JSONArray(operations);
            }
            
            pendingOps.put(operation);
            prefs.edit().putString("pending_operations", pendingOps.toString()).apply();
            
            // Schedule retry worker
            scheduleRetryWorker();
        } catch (JSONException e) {
            Log.e(TAG, "Error storing failed operation", e);
        }
    }

    private void scheduleRetryWorker() {
        WorkManager workManager = WorkManager.getInstance(context);
        
        Constraints constraints = new Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build();
            
        OneTimeWorkRequest retryWork = new OneTimeWorkRequest.Builder(RetryOperationsWorker.class)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.MINUTES)
            .build();
            
        workManager.enqueueUniqueWork(
            "retry_template_operations",
            ExistingWorkPolicy.KEEP,
            retryWork
        );
    }

    /**
     * Clean up resources
     */
    public void cleanup() {
        for (ListenerRegistration registration : templateListeners.values()) {
            registration.remove();
        }
        templateListeners.clear();
        likeStates.clear();
        favoriteStates.clear();
        likeCounts.clear();
    }

    private void handleFirebaseError(Exception e, String operation) {
        if (e instanceof FirebaseFirestoreException) {
            FirebaseFirestoreException ffe = (FirebaseFirestoreException) e;
            switch (ffe.getCode()) {
                case PERMISSION_DENIED:
                    Log.e(TAG, "Permission denied for operation: " + operation);
                    // Handle authentication errors
                    break;
                case UNAVAILABLE:
                    Log.e(TAG, "Firebase unavailable for operation: " + operation);
                    // Handle offline/network errors
                    break;
                case ALREADY_EXISTS:
                    Log.e(TAG, "Document already exists for operation: " + operation);
                    // Handle duplicate operations
                    break;
                default:
                    Log.e(TAG, "Firebase error for operation: " + operation, e);
                    break;
            }
        }
    }

    /**
     * Worker class to retry failed template operations
     */
    public static class RetryOperationsWorker extends Worker {
        private static final String TAG = "RetryOperationsWorker";
        
        public RetryOperationsWorker(@NonNull Context context, @NonNull WorkerParameters params) {
            super(context, params);
        }
        
        @NonNull
        @Override
        public Result doWork() {
            Context context = getApplicationContext();
            SharedPreferences prefs = context.getSharedPreferences("failed_operations", Context.MODE_PRIVATE);
            String operations = prefs.getString("pending_operations", "");
            
            if (operations.isEmpty()) {
                return Result.success();
            }
            
            try {
                JSONArray pendingOps = new JSONArray(operations);
                TemplateInteractionRepository repo = TemplateInteractionRepository.getInstance(context);
                
                boolean allSuccess = true;
                JSONArray remainingOps = new JSONArray();
                
                for (int i = 0; i < pendingOps.length(); i++) {
                    JSONObject operation = pendingOps.getJSONObject(i);
                    String templateId = operation.getString("templateId");
                    String type = operation.getString("type");
                    
                    try {
                        if ("like".equals(type)) {
                            repo.toggleLike(new Template(templateId, "", "", "", false, false, 0));
                        } else if ("favorite".equals(type)) {
                            repo.toggleFavorite(new Template(templateId, "", "", "", false, false, 0));
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to retry operation: " + operation.toString(), e);
                        allSuccess = false;
                        remainingOps.put(operation);
                    }
                }
                
                // Update pending operations
                if (remainingOps.length() > 0) {
                    prefs.edit().putString("pending_operations", remainingOps.toString()).apply();
                    return Result.retry();
                } else {
                    prefs.edit().remove("pending_operations").apply();
                    return Result.success();
                }
                
            } catch (JSONException e) {
                Log.e(TAG, "Error processing pending operations", e);
                return Result.failure();
            }
        }
    }

    private void logInteractionEvent(String templateId, String action, String result) {
        Log.d(TAG, String.format("Template interaction - ID: %s, Action: %s, Result: %s", 
            templateId, action, result));
    }

    private void logFirebaseOperation(String operation, String templateId, String details) {
        Log.d(TAG, String.format("Firebase operation - Type: %s, Template ID: %s, Details: %s",
            operation, templateId, details));
    }

    private void retryOperation(Runnable operation, int attempt) {
        if (attempt > 3) {
            Log.e(TAG, "Max retry attempts reached");
            return;
        }

        long delay = (long) Math.pow(2, attempt - 1) * 1000; // Exponential backoff
        Log.d(TAG, "Scheduling retry attempt " + attempt + " in " + delay + "ms");

        mainHandler.postDelayed(() -> {
            Log.d(TAG, "Executing retry attempt " + attempt);
            try {
                operation.run();
            } catch (Exception e) {
                Log.e(TAG, "Error during retry attempt " + attempt, e);
                retryOperation(operation, attempt + 1);
            }
        }, delay);
    }
} 