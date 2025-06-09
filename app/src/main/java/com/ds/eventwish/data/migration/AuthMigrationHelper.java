package com.ds.eventwish.data.migration;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper utility for migrating from FCM token-based user identification to Anonymous Auth
 */
public class AuthMigrationHelper {
    private static final String TAG = "AuthMigrationHelper";
    private static final String PREF_NAME = "EventWish";
    private static final String KEY_FCM_TOKEN = "fcm_token";
    private static final String KEY_MIGRATION_COMPLETE = "auth_migration_complete";
    private static final String COLLECTION_USERS = "users";

    private final Context context;
    private final FirebaseFirestore db;
    private final FirebaseAuth auth;
    private final SharedPreferences prefs;

    public AuthMigrationHelper(Context context) {
        this.context = context.getApplicationContext();
        this.db = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Check if migration is needed
     */
    public boolean isMigrationNeeded() {
        return !prefs.getBoolean(KEY_MIGRATION_COMPLETE, false);
    }

    /**
     * Migrate user data from FCM token to Anonymous Auth UID
     */
    public Task<Void> migrateUserData() {
        if (!isMigrationNeeded()) {
            return Tasks.forResult(null);
        }

        String oldToken = prefs.getString(KEY_FCM_TOKEN, null);
        FirebaseUser user = auth.getCurrentUser();

        if (oldToken == null || user == null) {
            Log.e(TAG, "Cannot migrate: Missing token or user");
            return Tasks.forException(new IllegalStateException("Missing token or user"));
        }

        String newUid = user.getUid();
        Log.d(TAG, String.format("Starting migration from token %s to UID %s", oldToken, newUid));

        return db.collection(COLLECTION_USERS).document(oldToken).get()
            .continueWithTask(task -> {
                if (!task.isSuccessful()) {
                    Log.e(TAG, "Failed to get old user document", task.getException());
                    return Tasks.forException(task.getException());
                }

                DocumentSnapshot oldDoc = task.getResult();
                if (!oldDoc.exists()) {
                    Log.d(TAG, "No old user document to migrate");
                    markMigrationComplete();
                    return Tasks.forResult(null);
                }

                // Start batch write
                WriteBatch batch = db.batch();

                // Copy user document
                DocumentReference newUserRef = db.collection(COLLECTION_USERS).document(newUid);
                batch.set(newUserRef, oldDoc.getData());

                // Copy subcollections
                List<Task<QuerySnapshot>> subcollectionTasks = new ArrayList<>();
                String[] subcollections = {"preferences", "favorites", "likes", "notifications"};

                for (String subcollection : subcollections) {
                    subcollectionTasks.add(
                        db.collection(COLLECTION_USERS)
                            .document(oldToken)
                            .collection(subcollection)
                            .get()
                    );
                }

                return Tasks.whenAllSuccess(subcollectionTasks)
                    .continueWithTask(subTask -> {
                        List<QuerySnapshot> results = new ArrayList<>();
                        for (Object result : subTask.getResult()) {
                            results.add((QuerySnapshot) result);
                        }
                        
                        for (int i = 0; i < results.size(); i++) {
                            QuerySnapshot querySnapshot = results.get(i);
                            String subcollection = subcollections[i];
                            
                            querySnapshot.getDocuments().forEach(doc -> {
                                DocumentReference newRef = newUserRef
                                    .collection(subcollection)
                                    .document(doc.getId());
                                batch.set(newRef, doc.getData());
                            });
                        }

                        // Delete old user document and its subcollections last
                        batch.delete(oldDoc.getReference());

                        return batch.commit();
                    });
            })
            .continueWithTask(task -> {
                if (task.isSuccessful()) {
                    Log.d(TAG, "Migration completed successfully");
                    markMigrationComplete();
                    return Tasks.forResult(null);
                } else {
                    Log.e(TAG, "Migration failed", task.getException());
                    return Tasks.forException(task.getException());
                }
            });
    }

    /**
     * Mark migration as complete
     */
    private void markMigrationComplete() {
        prefs.edit().putBoolean(KEY_MIGRATION_COMPLETE, true).apply();
        Log.d(TAG, "Migration marked as complete");
    }
} 