package com.ds.eventwish.test;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.ds.eventwish.R;
import com.ds.eventwish.data.model.User;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Activity that runs MongoDB sync model compatibility tests directly in the app.
 * This implementation runs the tests using reflection without depending on JUnit.
 */
public class UserModelTestActivity extends AppCompatActivity {

    private static final String TAG = "UserModelTest";
    private TextView tvTestResults;
    private Button btnRunTests;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_model_test);

        tvTestResults = findViewById(R.id.tvTestResults);
        btnRunTests = findViewById(R.id.btnRunTests);

        btnRunTests.setOnClickListener(v -> runUserModelTests());
    }

    /**
     * Run the user model tests on a background thread and update UI with results
     */
    private void runUserModelTests() {
        btnRunTests.setEnabled(false);
        tvTestResults.setText("Running tests...\n");

        executorService.execute(() -> {
            StringBuilder resultBuilder = new StringBuilder();
            resultBuilder.append("Running MongoDB Sync Tests...\n\n");
            
            List<String> failedTests = new ArrayList<>();
            int totalTests = 0;
            long startTime = System.currentTimeMillis();

            try {
                // Run the tests directly instead of using JUnit
                totalTests = 3; // Number of tests we're running
                
                // Test 1: Required fields
                if (testRequiredFields()) {
                    resultBuilder.append("✅ Required fields test passed\n");
                } else {
                    resultBuilder.append("❌ Required fields test failed\n");
                    failedTests.add("testRequiredFields");
                }
                
                // Test 2: Social interaction fields
                if (testSocialFields()) {
                    resultBuilder.append("✅ Social fields test passed\n");
                } else {
                    resultBuilder.append("❌ Social fields test failed\n");
                    failedTests.add("testSocialFields");
                }
                
                // Test 3: Social interaction methods
                if (testSocialMethods()) {
                    resultBuilder.append("✅ Social methods test passed\n");
                } else {
                    resultBuilder.append("❌ Social methods test failed\n");
                    failedTests.add("testSocialMethods");
                }
                
                // Summary
                long endTime = System.currentTimeMillis();
                int passedTests = totalTests - failedTests.size();
                
                resultBuilder.append("\nTest Summary:\n");
                resultBuilder.append("Passed: ").append(passedTests).append("/").append(totalTests).append("\n");
                
                if (failedTests.isEmpty()) {
                    resultBuilder.append("\n✅ All tests passed! (").append(totalTests).append(" tests)\n");
                } else {
                    resultBuilder.append("\n❌ Test failures: ").append(failedTests.size())
                            .append(" of ").append(totalTests).append(" tests failed\n\n");
                    
                    resultBuilder.append("Failed tests:\n");
                    for (String test : failedTests) {
                        resultBuilder.append("- ").append(test).append("\n");
                    }
                }
                
                // Add completion time
                resultBuilder.append("\nTests completed in ").append(endTime - startTime).append("ms");

            } catch (Exception e) {
                Log.e(TAG, "Error running tests", e);
                resultBuilder.append("❌ Exception running tests: ")
                        .append(e.getMessage())
                        .append("\n\n");

                // Get stack trace
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                resultBuilder.append("Stack trace: \n").append(sw.toString());
            }

            // Update UI on main thread
            final String finalResult = resultBuilder.toString();
            mainHandler.post(() -> {
                tvTestResults.setText(finalResult);
                btnRunTests.setEnabled(true);
            });
        });
    }
    
    /**
     * Test that User model contains all required fields for MongoDB sync
     */
    private boolean testRequiredFields() {
        try {
            // Essential fields for MongoDB sync
            Set<String> requiredFields = new HashSet<>(Arrays.asList(
                "uid",
                "deviceId",
                "displayName",
                "email",
                "profilePhoto",
                "lastActive"
            ));
            
            // Get all fields from User model
            Field[] fields = User.class.getDeclaredFields();
            Set<String> actualFields = new HashSet<>();
            
            for (Field field : fields) {
                actualFields.add(field.getName());
            }
            
            // Verify all required fields exist
            for (String requiredField : requiredFields) {
                if (!actualFields.contains(requiredField)) {
                    Log.e(TAG, "User model is missing required field: " + requiredField);
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error in testRequiredFields", e);
            return false;
        }
    }
    
    /**
     * Test that User model contains social interaction fields (likes, favorites)
     */
    private boolean testSocialFields() {
        try {
            // Social interaction fields
            Set<String> socialFields = new HashSet<>(Arrays.asList(
                "favorites",
                "likes"
            ));
            
            // Get all fields from User model
            Field[] fields = User.class.getDeclaredFields();
            Set<String> actualFields = new HashSet<>();
            
            for (Field field : fields) {
                actualFields.add(field.getName());
            }
            
            // Verify social fields exist
            for (String socialField : socialFields) {
                if (!actualFields.contains(socialField)) {
                    Log.e(TAG, "User model is missing social interaction field: " + socialField);
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error in testSocialFields", e);
            return false;
        }
    }
    
    /**
     * Test that User model contains methods for social interactions
     */
    private boolean testSocialMethods() {
        try {
            // Essential methods for social interactions
            Set<String> requiredMethods = new HashSet<>(Arrays.asList(
                "addFavorite",
                "removeFavorite",
                "isFavorite",
                "addLike",
                "removeLike",
                "isLiked"
            ));
            
            // Get all methods from User class
            Method[] methods = User.class.getDeclaredMethods();
            Set<String> actualMethods = new HashSet<>();
            
            for (Method method : methods) {
                actualMethods.add(method.getName());
            }
            
            // Verify all required methods exist
            for (String requiredMethod : requiredMethods) {
                if (!actualMethods.contains(requiredMethod)) {
                    Log.e(TAG, "User model is missing required method: " + requiredMethod);
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error in testSocialMethods", e);
            return false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
} 