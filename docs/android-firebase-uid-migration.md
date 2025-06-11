# Android Firebase UID Migration Guide

This guide provides step-by-step instructions for migrating your EventWish Android app from using device IDs to Firebase UIDs for user identification.

## Overview

Firebase Authentication provides a more secure and reliable way to identify users across multiple devices. This migration involves:

1. Updating API client code to send Firebase UID
2. Ensuring Firebase Authentication is properly implemented
3. Updating local storage to use Firebase UID
4. Testing with the updated backend

## Prerequisites

- Android Studio 4.2+
- Firebase Authentication SDK
- EventWish Android app codebase

## Step 1: Update Dependencies

Ensure you have the latest Firebase dependencies in your `app/build.gradle`:

```gradle
dependencies {
    // Firebase Authentication
    implementation 'com.google.firebase:firebase-auth:22.1.2'
    
    // Firebase Core
    implementation 'com.google.firebase:firebase-core:21.1.1'
    
    // Other Firebase dependencies as needed
}
```

## Step 2: Ensure Firebase Authentication Implementation

Make sure your app has Firebase Authentication properly implemented:

```java
// Initialize Firebase Auth
private FirebaseAuth mAuth;

@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    
    // Initialize Firebase Auth
    mAuth = FirebaseAuth.getInstance();
}

@Override
public void onStart() {
    super.onStart();
    // Check if user is signed in
    FirebaseUser currentUser = mAuth.getCurrentUser();
    if (currentUser != null) {
        // User is signed in
        updateUI(currentUser);
    } else {
        // No user is signed in, initiate sign-in flow
        signInAnonymously();
    }
}

private void signInAnonymously() {
    mAuth.signInAnonymously()
            .addOnCompleteListener(this, task -> {
                if (task.isSuccessful()) {
                    // Sign in success
                    FirebaseUser user = mAuth.getCurrentUser();
                    updateUI(user);
                } else {
                    // Sign in failed
                    Log.e(TAG, "Anonymous authentication failed", task.getException());
                    Toast.makeText(this, "Authentication failed", Toast.LENGTH_SHORT).show();
                }
            });
}

private void updateUI(FirebaseUser user) {
    if (user != null) {
        // User is signed in, update UI
        String uid = user.getUid();
        // Use the UID for API calls
    }
}
```

## Step 3: Update API Client Code

Update your API service classes to use Firebase UID instead of device ID:

### Before:

```java
// Old implementation using device ID
public Call<BaseResponse<Void>> updateUserProfile(String deviceId, Map<String, Object> userData) {
    userData.put("deviceId", deviceId);
    return apiService.updateUserProfile(userData);
}
```

### After:

```java
// New implementation using Firebase UID
public Call<BaseResponse<Void>> updateUserProfile(FirebaseUser user) {
    if (user == null) {
        return Tasks.forException(new IllegalArgumentException("User cannot be null"));
    }
    
    Map<String, Object> userData = new HashMap<>();
    userData.put("uid", user.getUid());
    userData.put("displayName", user.getDisplayName() != null ? user.getDisplayName() : "");
    userData.put("email", user.getEmail() != null ? user.getEmail() : "");
    userData.put("profilePhoto", user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : "");
    userData.put("lastOnline", System.currentTimeMillis());
    
    return apiService.updateUserProfile(userData);
}
```

## Step 4: Update ApiService Interface

Update your Retrofit API service interface to use Firebase UID:

### Before:
```java
@POST("api/users/register")
Call<JsonObject> registerDeviceUser(@Body Map<String, Object> requestBody);

@PUT("api/users/activity")
Call<JsonObject> updateUserActivity(@Body Map<String, Object> body);
```

### After:
```java
@POST("api/users/register")
Call<JsonObject> registerUser(@Body Map<String, Object> requestBody);

@PUT("api/users/activity")
Call<JsonObject> updateUserActivity(@Body Map<String, Object> body);
```

## Step 5: Update Data Repository Classes

Update your repository classes to use Firebase UID:

```java
public class UserRepository {
    private final ApiService apiService;
    private final FirebaseAuth firebaseAuth;
    
    public UserRepository(ApiService apiService) {
        this.apiService = apiService;
        this.firebaseAuth = FirebaseAuth.getInstance();
    }
    
    public Task<Void> registerUser() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null) {
            return Tasks.forException(new IllegalArgumentException("User not authenticated"));
        }
        
        TaskCompletionSource<Void> taskCompletionSource = new TaskCompletionSource<>();
        
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                Map<String, Object> userData = new HashMap<>();
                userData.put("uid", user.getUid());
                userData.put("displayName", user.getDisplayName());
                userData.put("email", user.getEmail());
                userData.put("profilePhoto", user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : null);
                
                Call<JsonObject> call = apiService.registerUser(userData);
                Response<JsonObject> response = call.execute();
                
                if (response.isSuccessful()) {
                    taskCompletionSource.setResult(null);
                } else {
                    taskCompletionSource.setException(new Exception("Failed to register user: " + response.code()));
                }
            } catch (Exception e) {
                taskCompletionSource.setException(e);
            }
        });
        
        return taskCompletionSource.getTask();
    }
    
    // Other repository methods following the same pattern
}
```

## Step 6: Update Local Storage

If you're storing the user ID locally, update to use Firebase UID:

```java
public class UserPreferences {
    private static final String PREF_NAME = "user_prefs";
    private static final String KEY_USER_ID = "user_id";
    
    private final SharedPreferences preferences;
    private final FirebaseAuth firebaseAuth;
    
    public UserPreferences(Context context) {
        this.preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.firebaseAuth = FirebaseAuth.getInstance();
    }
    
    public String getUserId() {
        // Always prefer Firebase UID if available
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null) {
            return user.getUid();
        }
        
        // Fall back to stored ID only if Firebase auth fails
        return preferences.getString(KEY_USER_ID, null);
    }
    
    public void storeUserId(String userId) {
        preferences.edit().putString(KEY_USER_ID, userId).apply();
    }
}
```

## Step 7: Handle Anonymous Authentication Transition

Ensure your app handles users who may not have a Firebase account:

```java
private void ensureUserAuthenticated() {
    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    if (user == null) {
        // No user is signed in, initiate anonymous sign-in
        FirebaseAuth.getInstance().signInAnonymously()
                .addOnSuccessListener(authResult -> {
                    // User is now authenticated anonymously
                    syncUserProfile(authResult.getUser());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Anonymous authentication failed", e);
                });
    } else {
        // User is already signed in
        syncUserProfile(user);
    }
}

private void syncUserProfile(FirebaseUser user) {
    userRepository.updateUserProfile(user)
            .addOnSuccessListener(aVoid -> Log.d(TAG, "User profile synced successfully"))
            .addOnFailureListener(e -> Log.e(TAG, "User profile sync failed", e));
}
```

## Step 8: Update Analytics and Tracking

If you're using analytics, update your event tracking to use Firebase UID:

```java
private void trackEvent(String eventName, Bundle params) {
    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    if (user != null) {
        // Set user property
        FirebaseAnalytics.getInstance(this).setUserId(user.getUid());
    }
    
    // Track event
    FirebaseAnalytics.getInstance(this).logEvent(eventName, params);
}
```

## Step 9: Testing

Test your implementation thoroughly:

1. **Clean Install Test**: Uninstall the app and reinstall to verify new user registration
2. **Migration Test**: Update an existing installation to verify migration
3. **API Integration Test**: Verify all API calls work with Firebase UID
4. **Edge Case Test**: Test offline functionality and error handling

## Common Issues and Solutions

### Firebase Authentication Failures

If you encounter authentication failures:

1. Verify your `google-services.json` file is correctly configured
2. Ensure Firebase Authentication is enabled in the Firebase Console
3. Check if Anonymous Authentication is enabled in the Firebase Console

### API Call Failures

If API calls fail after migration:

1. Verify Firebase UID is being passed correctly in the request
2. Check network requests using Charles Proxy or similar tools
3. Ensure the backend is updated to handle Firebase UID

### Multiple Devices

If users have issues across multiple devices:

1. Implement Firebase Sign-In with credentials for cross-device identity
2. Use Firebase Cloud Messaging (FCM) to sync user state

## Migration Timeline

To ensure a smooth transition:

1. **Week 1-2**: Update app code to use Firebase UID while maintaining device ID support
2. **Week 3**: Test with a small subset of users (beta testing)
3. **Week 4**: Gradually roll out to all users
4. **Week 6-8**: Remove device ID support entirely

## Conclusion

By following this guide, you've successfully migrated your EventWish Android app from device IDs to Firebase UIDs. This improves security, enables cross-device authentication, and provides a more robust user identification system.

If you encounter any issues during migration, please contact the backend team for assistance. 