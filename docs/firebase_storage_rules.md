# Firebase Storage Rules Documentation

## Overview

This document outlines the Firebase Storage rules implementation for the EventWish app. These rules govern how users can interact with stored files, ensuring security, data integrity, and proper access control.

## Table of Contents

1. [Rule Structure](#rule-structure)
2. [Security Functions](#security-functions)
3. [Storage Paths](#storage-paths)
4. [Access Control](#access-control)
5. [File Validation](#file-validation)
6. [Implementation Guide](#implementation-guide)
7. [Best Practices](#best-practices)
8. [Monitoring and Maintenance](#monitoring-and-maintenance)

## Rule Structure

```javascript
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    // Rules are implemented here
  }
}
```

## Security Functions

### Authentication Check
```javascript
function isAuthenticated() {
  return request.auth != null;
}
```
Verifies that the user is logged in.

### Ownership Verification
```javascript
function isOwner(userId) {
  return request.auth.uid == userId;
}
```
Ensures the user owns the resource they're trying to access.

### Content Validation
```javascript
function isValidContentType() {
  return request.resource.contentType.matches('image/.*')
      || request.resource.contentType.matches('application/json');
}
```
Validates file types (images and JSON only).

### Size Validation
```javascript
function isValidFileSize() {
  return request.resource.size <= 5 * 1024 * 1024; // 5MB limit
}
```
Enforces a 5MB file size limit.

## Storage Paths

### User Profile Images
```javascript
match /users/{userId}/profile/{fileName} {
  allow read: if true;
  allow write: if isAuthenticated() 
      && isOwner(userId)
      && isValidContentType()
      && isValidFileSize();
}
```
- Public read access
- Write access restricted to authenticated owners
- File type and size validation

### Template Images
```javascript
match /templates/{templateId}/{fileName} {
  allow read: if true;
  allow write: if isAuthenticated()
      && isValidContentType()
      && isValidFileSize();
}
```
- Public read access
- Write access for authenticated users
- File validation enforced

### Shared Wish Previews
```javascript
match /wishes/{wishId}/preview/{fileName} {
  allow read: if true;
  allow write: if isAuthenticated()
      && isValidContentType()
      && isValidFileSize();
  allow delete: if isAuthenticated() 
      && (resource.metadata.ownerId == request.auth.uid);
}
```
- Public read access
- Authenticated write access
- Owner-only deletion

### Cached Resources
```javascript
match /cache/{userId}/{resourceType}/{fileName} {
  allow read: if isAuthenticated() && isOwner(userId);
  allow write: if isAuthenticated() && isOwner(userId);
  allow delete: if isAuthenticated() && isOwner(userId);
}
```
- User-specific cache storage
- Full access control for owners

### Temporary Files
```javascript
match /temp/{fileName} {
  allow read: if isAuthenticated();
  allow write: if isAuthenticated()
      && isValidContentType()
      && isValidFileSize()
      && request.resource.metadata.timestamp >= (request.time.toMillis() - 24 * 60 * 60 * 1000);
  allow delete: if isAuthenticated();
}
```
- 24-hour expiration
- Authenticated access
- File validation

## Access Control

### Public Access
- Template images
- Wish preview images
- User profile images

### Authenticated Access
- File uploads
- Temporary file operations
- Cache operations

### Owner-Only Access
- Profile image management
- Cache management
- Resource deletion

## File Validation

### Content Types
- Images (image/*)
- JSON (application/json)

### Size Limits
- Maximum file size: 5MB
- Enforced on all uploads

### Metadata
- Owner ID tracking
- Timestamp validation
- Custom metadata support

## Implementation Guide

1. **Firebase Console Setup**
   - Navigate to Firebase Console
   - Select your project
   - Go to Storage section
   - Click on "Rules" tab
   - Copy and paste the rules
   - Click "Publish"

2. **Client Implementation**
   ```kotlin
   // Initialize Firebase Storage
   val storage = FirebaseStorage.getInstance()
   
   // Create references
   val profileRef = storage.reference
       .child("users")
       .child(userId)
       .child("profile")
       .child(fileName)
   
   // Upload file
   profileRef.putFile(fileUri)
       .addOnSuccessListener { /* Handle success */ }
       .addOnFailureListener { /* Handle failure */ }
   ```

3. **Error Handling**
   ```kotlin
   try {
       // Attempt upload
       storageRef.putFile(fileUri)
   } catch (e: StorageException) {
       when (e.errorCode) {
           StorageException.ERROR_NOT_AUTHENTICATED -> // Handle auth error
           StorageException.ERROR_NOT_AUTHORIZED -> // Handle permission error
           StorageException.ERROR_QUOTA_EXCEEDED -> // Handle quota error
       }
   }
   ```

## Best Practices

1. **Security**
   - Always validate file types server-side
   - Implement client-side validation
   - Use signed URLs for sensitive content
   - Clean up temporary files regularly

2. **Performance**
   - Implement client-side caching
   - Use compression when possible
   - Consider using CDN for frequent access
   - Monitor bandwidth usage

3. **Maintenance**
   - Regular security audits
   - Monitor storage usage
   - Clean up unused files
   - Update rules as needed

## Monitoring and Maintenance

1. **Firebase Console Monitoring**
   - Enable detailed logging
   - Set up quota alerts
   - Monitor access patterns
   - Track error rates

2. **Regular Tasks**
   - Review access patterns
   - Update security rules
   - Clean up temporary files
   - Optimize storage usage

3. **Error Handling**
   - Log all errors
   - Set up alerts
   - Monitor client feedback
   - Update documentation

## Additional Resources

- [Firebase Storage Documentation](https://firebase.google.com/docs/storage)
- [Security Rules Language](https://firebase.google.com/docs/rules/rules-language)
- [Best Practices](https://firebase.google.com/docs/storage/security/best-practices)
- [Common Use Cases](https://firebase.google.com/docs/storage/security/user-security) 