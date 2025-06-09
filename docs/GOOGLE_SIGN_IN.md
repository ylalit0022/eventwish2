# Google Sign-In Implementation

## Overview
EventWish uses Google Sign-In for user authentication, integrated with Firebase Authentication and Firestore for data persistence.

## Components

### 1. AuthManager
- Singleton class managing authentication state
- Handles Google Sign-In flow
- Manages Firebase Authentication
- Provides silent sign-in support

### 2. FirestoreManager
- Manages Firestore operations
- Handles user document creation/updates
- Manages template interactions
- Handles FCM token updates

### 3. UserPreferencesRepository
- Manages user preferences
- Syncs with Firestore
- Handles offline persistence

## Implementation Details

### Authentication Flow
1. User opens app
2. SplashActivity attempts silent sign-in
3. If successful, proceeds to MainActivity
4. If failed, shows Google Sign-In button
5. On sign-in success:
   - Creates/updates user document
   - Syncs preferences
   - Updates FCM token

### Data Structure
```
/users/{userId}/
  - fcmToken: string
  - lastUpdated: timestamp
  - preferences/
    - notifications: map
    - templates: map
  - likes/
    - {templateId}: timestamp
  - favorites/
    - {templateId}: timestamp
```

### Security Rules
- User document access restricted to owner
- Template interactions require authentication
- FCM token updates validated

## Testing

### Integration Tests
1. User Authentication
   - Test sign-in flow
   - Test token refresh
   - Test sign-out flow

2. Data Synchronization
   - Test user document creation
   - Test preferences sync
   - Test template interactions

3. Error Handling
   - Test network failures
   - Test permission errors
   - Test concurrent updates

### Manual Testing Checklist
- [ ] Fresh install sign-in
- [ ] Returning user sign-in
- [ ] Offline behavior
- [ ] Token refresh
- [ ] Permission handling
- [ ] Data sync verification

## Troubleshooting

### Common Issues
1. Sign-in Failures
   - Check Google Play Services
   - Verify OAuth configuration
   - Check network connectivity

2. Data Sync Issues
   - Check Firestore rules
   - Verify user document structure
   - Check FCM token validity

3. Permission Errors
   - Verify Firebase project setup
   - Check security rules
   - Validate user authentication

## Migration Guide

### From Anonymous Auth
1. Backup user data
2. Sign in with Google
3. Migrate data to new user document
4. Update FCM token
5. Clean up old data

### Steps for Migration
```java
// Example migration code
AuthMigrationHelper.migrateUserData()
    .addOnSuccessListener(...)
    .addOnFailureListener(...);
```

## Best Practices

1. Error Handling
   - Always handle sign-in failures
   - Provide user feedback
   - Implement retry mechanisms

2. Data Management
   - Use transactions for atomic updates
   - Implement proper caching
   - Handle offline scenarios

3. Security
   - Validate user permissions
   - Secure sensitive data
   - Implement proper token management

## API Reference

### AuthManager
```java
AuthManager.getInstance().initialize(context)
AuthManager.getInstance().silentSignIn()
AuthManager.getInstance().handleSignInResult()
AuthManager.getInstance().signOut()
```

### FirestoreManager
```java
FirestoreManager.getInstance().getUserPreferences()
FirestoreManager.getInstance().updateUserPreferences()
FirestoreManager.getInstance().setFcmToken()
```

### UserPreferencesRepository
```java
userPreferencesRepository.updatePreferences()
userPreferencesRepository.getPreferences()
userPreferencesRepository.syncWithFirestore()
``` 