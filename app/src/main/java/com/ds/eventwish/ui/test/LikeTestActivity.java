package com.ds.eventwish.ui.test;

import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.ds.eventwish.R;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class LikeTestActivity extends AppCompatActivity {
    private static final String TAG = "LikeTestActivity";
    // Using test collection and document for development
    private static final String TEST_COLLECTION = "test_templates";
    private static final String TEST_DOC_ID = "test_template_001";

    private TextView likeCountText;
    private ImageButton likeButton;
    private ImageButton favoriteButton;
    private FirebaseFirestore db;
    private DocumentReference templateRef;
    private ListenerRegistration likeListener;
    private boolean isLiked = false;
    private boolean isFavorited = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_like_test);

        Log.d(TAG, "onCreate: Initializing LikeTestActivity");

        // Initialize Firestore with test collection
        try {
            db = FirebaseFirestore.getInstance();
            templateRef = db.collection(TEST_COLLECTION).document(TEST_DOC_ID);
            Log.d(TAG, "Firestore initialized successfully with test collection");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing Firestore", e);
            Toast.makeText(this, "Failed to initialize Firebase", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize views
        initializeViews();

        // Create test document if it doesn't exist
        createTestDocument();

        // Set up click listeners
        setupClickListeners();

        // Set up Firestore listener
        setupFirestoreListener();
    }

    private void initializeViews() {
        try {
            likeCountText = findViewById(R.id.likeCountText);
            likeButton = findViewById(R.id.likeButton);
            favoriteButton = findViewById(R.id.favoriteButton);
            Log.d(TAG, "Views initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing views", e);
            Toast.makeText(this, "Failed to initialize UI", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void createTestDocument() {
        Map<String, Object> testData = new HashMap<>();
        testData.put("id", TEST_DOC_ID);
        testData.put("title", "Test Template");
        testData.put("likeCount", 0);
        testData.put("isFavorited", false);
        testData.put("createdAt", System.currentTimeMillis());
        testData.put("isTestData", true);

        templateRef.set(testData, SetOptions.merge())
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Test document created/updated successfully");
                // Initialize UI with test data
                updateLikeCount(0);
                updateFavoriteState(false);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error creating test document", e);
                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }

    private void setupClickListeners() {
        likeButton.setOnClickListener(v -> handleLikeClick());
        favoriteButton.setOnClickListener(v -> handleFavoriteClick());
        Log.d(TAG, "Click listeners set up successfully");
    }

    private void setupFirestoreListener() {
        Log.d(TAG, "Setting up Firestore listener for test document");
        likeListener = templateRef.addSnapshotListener((snapshot, e) -> {
            if (e != null) {
                Log.e(TAG, "Listen failed", e);
                return;
            }

            if (snapshot != null && snapshot.exists()) {
                Long likeCount = snapshot.getLong("likeCount");
                Boolean favorited = snapshot.getBoolean("isFavorited");
                
                Log.d(TAG, "Test document snapshot: likeCount=" + likeCount + ", favorited=" + favorited);
                
                if (likeCount != null) {
                    updateLikeCount(likeCount);
                }
                
                if (favorited != null) {
                    updateFavoriteState(favorited);
                }
            } else {
                Log.d(TAG, "Test document does not exist");
                createTestDocument();
            }
        });
    }

    private void handleLikeClick() {
        Log.d(TAG, "Like button clicked. Current state: " + isLiked);
        isLiked = !isLiked;
        likeButton.setSelected(isLiked);

        templateRef.get()
            .addOnSuccessListener(snapshot -> {
                Long currentLikes = snapshot.getLong("likeCount");
                if (currentLikes == null) {
                    currentLikes = 0L;
                    Log.w(TAG, "likeCount field not found in test document, defaulting to 0");
                }

                long newLikes = currentLikes + (isLiked ? 1 : -1);
                Log.d(TAG, "Updating test document like count: " + currentLikes + " -> " + newLikes);

                Map<String, Object> updates = new HashMap<>();
                updates.put("likeCount", newLikes);
                updates.put("lastUpdated", System.currentTimeMillis());

                templateRef.update(updates)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Test document like count updated successfully to " + newLikes);
                        updateLikeCount(newLikes);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error updating test document like count", e);
                        // Revert UI state
                        isLiked = !isLiked;
                        likeButton.setSelected(isLiked);
                        Toast.makeText(this, "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error getting test document", e);
                // Revert UI state
                isLiked = !isLiked;
                likeButton.setSelected(isLiked);
                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }

    private void handleFavoriteClick() {
        Log.d(TAG, "Favorite button clicked. Current state: " + isFavorited);
        isFavorited = !isFavorited;
        favoriteButton.setSelected(isFavorited);

        Map<String, Object> updates = new HashMap<>();
        updates.put("isFavorited", isFavorited);
        updates.put("lastUpdated", System.currentTimeMillis());

        templateRef.update(updates)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Test document favorite status updated successfully to " + isFavorited);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error updating test document favorite status", e);
                // Revert UI state
                isFavorited = !isFavorited;
                favoriteButton.setSelected(isFavorited);
                Toast.makeText(this, "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }

    private void updateLikeCount(long count) {
        Log.d(TAG, "Updating like count UI to: " + count);
        likeCountText.setText("Like Count: " + count);
    }

    private void updateFavoriteState(boolean favorited) {
        Log.d(TAG, "Updating favorite state UI to: " + favorited);
        isFavorited = favorited;
        favoriteButton.setSelected(favorited);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (likeListener != null) {
            likeListener.remove();
            Log.d(TAG, "Firestore listener removed");
        }
    }
} 