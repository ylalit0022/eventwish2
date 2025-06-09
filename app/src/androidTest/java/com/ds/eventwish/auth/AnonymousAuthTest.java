package com.ds.eventwish.auth;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.ds.eventwish.data.remote.FirestoreManager;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class AnonymousAuthTest {
    private static final String TAG = "AnonymousAuthTest";
    private static final String PREF_NAME = "EventWish";
    private static final String KEY_FCM_TOKEN = "fcm_token";
    private static final long TIMEOUT = 60; // 60 seconds timeout for real device
    private static final int MAX_RETRIES = 3;

    private Context context;
    private SharedPreferences prefs;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private FirestoreManager firestoreManager;

    @Before
    public void setUp() {
        try {
            // Get application context first
            context = ApplicationProvider.getApplicationContext();
            if (context == null) {
                throw new IllegalStateException("Failed to get application context");
            }

            // Initialize SharedPreferences
            prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            if (prefs == null) {
                throw new IllegalStateException("Failed to initialize SharedPreferences");
            }

            // Initialize Firebase components with retries
            for (int i = 0; i < MAX_RETRIES; i++) {
                try {
                    auth = FirebaseAuth.getInstance();
                    db = FirebaseFirestore.getInstance();
                    firestoreManager = FirestoreManager.getInstance();
                    break;
                } catch (Exception e) {
                    if (i == MAX_RETRIES - 1) {
                        throw new IllegalStateException("Failed to initialize Firebase components after " + MAX_RETRIES + " attempts", e);
                    }
                    Log.w(TAG, "Retry " + (i + 1) + " of " + MAX_RETRIES + " for Firebase initialization");
                    Thread.sleep(1000); // Wait 1 second before retry
                }
            }
            
            Log.d(TAG, "Test setup completed successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error in setUp: " + e.getMessage(), e);
            throw new RuntimeException("Test setup failed", e);
        }
    }

    @After
    public void tearDown() {
        try {
            // Clear test data
            if (prefs != null) {
                prefs.edit().clear().apply();
            }
            Log.d(TAG, "Test cleanup completed successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error in tearDown: " + e.getMessage(), e);
        }
    }

    @Test
    public void testFCMTokenStorage() {
        try {
            // Test FCM token storage with retry mechanism
            String testToken = "test_fcm_token_" + System.currentTimeMillis();
            
            // Store token with verification
            prefs.edit().putString(KEY_FCM_TOKEN, testToken).apply();
            Thread.sleep(100); // Small delay to ensure write completes
            
            String storedToken = prefs.getString(KEY_FCM_TOKEN, null);
            assertNotNull("FCM token should be stored", storedToken);
            assertTrue("Stored token should match test token", storedToken.equals(testToken));
            
            Log.d(TAG, "FCM token storage test passed successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error in testFCMTokenStorage: " + e.getMessage(), e);
            fail("Test failed: " + e.getMessage());
        }
    }

    @Test
    public void testTokenValidation() {
        try {
            // Test various token scenarios
            String[] testTokens = {
                "valid_token_" + System.currentTimeMillis(),
                "", // Empty token
                "test_token_with_special_chars_!@#$%"
            };

            for (String token : testTokens) {
                prefs.edit().putString(KEY_FCM_TOKEN, token).apply();
                Thread.sleep(100); // Small delay to ensure write completes
                
                String storedToken = prefs.getString(KEY_FCM_TOKEN, null);
                assertNotNull("Token should be stored: " + token, storedToken);
                assertTrue("Stored token should match test token: " + token, storedToken.equals(token));
                
                Log.d(TAG, "Token validation passed for: " + token);
            }
            
            Log.d(TAG, "Token validation test passed successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error in testTokenValidation: " + e.getMessage(), e);
            fail("Test failed: " + e.getMessage());
        }
    }
} 