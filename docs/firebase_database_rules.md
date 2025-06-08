# Firebase Database Rules Documentation

## Overview

This document outlines the Firebase Database rules for the EventWish app, focusing on user data, likes, and favorites management.

## Database Structure

```javascript
{
  "users": {
    "$userId": {
      "profile": {
        "name": "string",
        "email": "string",
        "createdAt": "timestamp",
        "updatedAt": "timestamp"
      }
    }
  },
  "likes": {
    "$userId": {
      "$templateId": {
        "timestamp": "timestamp"
      }
    }
  },
  "favorites": {
    "$userId": {
      "$templateId": {
        "timestamp": "timestamp"
      }
    }
  },
  "templates": {
    "$templateId": {
      "title": "string",
      "description": "string",
      "category": "string",
      "likeCount": "number",
      "favoriteCount": "number",
      "createdAt": "timestamp",
      "updatedAt": "timestamp"
    }
  }
}
```

## Database Rules

### Firestore Rules
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Helper functions
    function isAuthenticated() {
      return request.auth != null;
    }
    
    function isOwner(userId) {
      return request.auth.uid == userId;
    }

    // User profiles
    match /users/{userId} {
      allow read: if true;
      allow create: if isAuthenticated() && isOwner(userId);
      allow update: if isAuthenticated() && isOwner(userId);
      allow delete: if false; // Prevent user deletion
    }

    // Likes
    match /likes/{userId}/{templateId} {
      allow read: if true;
      allow write: if isAuthenticated() && isOwner(userId);
    }

    // Favorites
    match /favorites/{userId}/{templateId} {
      allow read: if true;
      allow write: if isAuthenticated() && isOwner(userId);
    }

    // Templates
    match /templates/{templateId} {
      allow read: if true;
      allow write: if false; // Only admin can modify templates
    }
  }
}
```

## Implementation Guide

### 1. User Profile Management

```kotlin
// Create/Update user profile
fun updateUserProfile(name: String) {
    val user = hashMapOf(
        "name" to name,
        "updatedAt" to FieldValue.serverTimestamp()
    )
    
    db.collection("users")
        .document(auth.currentUser?.uid ?: "")
        .set(user, SetOptions.merge())
}

// Get user profile
fun getUserProfile(userId: String) {
    db.collection("users")
        .document(userId)
        .get()
        .addOnSuccessListener { document ->
            val name = document.getString("name")
            // Handle the data
        }
}
```

### 2. Like Management

```kotlin
// Add/Remove like
fun toggleLike(templateId: String) {
    val userId = auth.currentUser?.uid ?: return
    val likeRef = db.collection("likes")
        .document(userId)
        .collection("templates")
        .document(templateId)
        
    db.runTransaction { transaction ->
        val snapshot = transaction.get(likeRef)
        if (snapshot.exists()) {
            // Remove like
            transaction.delete(likeRef)
            // Decrement like count
            val templateRef = db.collection("templates").document(templateId)
            transaction.update(templateRef, "likeCount", FieldValue.increment(-1))
        } else {
            // Add like
            transaction.set(likeRef, hashMapOf(
                "timestamp" to FieldValue.serverTimestamp()
            ))
            // Increment like count
            val templateRef = db.collection("templates").document(templateId)
            transaction.update(templateRef, "likeCount", FieldValue.increment(1))
        }
    }
}

// Check if user liked template
fun checkIfLiked(templateId: String) {
    val userId = auth.currentUser?.uid ?: return
    db.collection("likes")
        .document(userId)
        .collection("templates")
        .document(templateId)
        .get()
        .addOnSuccessListener { document ->
            val isLiked = document.exists()
            // Handle the result
        }
}
```

### 3. Favorite Management

```kotlin
// Add/Remove favorite
fun toggleFavorite(templateId: String) {
    val userId = auth.currentUser?.uid ?: return
    val favoriteRef = db.collection("favorites")
        .document(userId)
        .collection("templates")
        .document(templateId)
        
    db.runTransaction { transaction ->
        val snapshot = transaction.get(favoriteRef)
        if (snapshot.exists()) {
            // Remove favorite
            transaction.delete(favoriteRef)
            // Decrement favorite count
            val templateRef = db.collection("templates").document(templateId)
            transaction.update(templateRef, "favoriteCount", FieldValue.increment(-1))
        } else {
            // Add favorite
            transaction.set(favoriteRef, hashMapOf(
                "timestamp" to FieldValue.serverTimestamp()
            ))
            // Increment favorite count
            val templateRef = db.collection("templates").document(templateId)
            transaction.update(templateRef, "favoriteCount", FieldValue.increment(1))
        }
    }
}

// Get user favorites
fun getUserFavorites() {
    val userId = auth.currentUser?.uid ?: return
    db.collection("favorites")
        .document(userId)
        .collection("templates")
        .orderBy("timestamp", Query.Direction.DESCENDING)
        .get()
        .addOnSuccessListener { documents ->
            val templateIds = documents.map { it.id }
            // Fetch template details
            // Handle the results
        }
}
```

## Best Practices

1. **Security**
   - Always validate user authentication
   - Use server timestamps for consistency
   - Implement proper error handling
   - Use transactions for atomic operations

2. **Performance**
   - Use composite indexes for complex queries
   - Implement pagination for large lists
   - Cache frequently accessed data
   - Use batch operations when possible

3. **Data Structure**
   - Keep documents small
   - Use subcollections for scalability
   - Denormalize data when necessary
   - Use counters for aggregations

## Error Handling

```kotlin
fun handleDatabaseOperation(operation: suspend () -> Unit) {
    try {
        operation()
    } catch (e: FirebaseFirestoreException) {
        when (e.code) {
            FirebaseFirestoreException.Code.PERMISSION_DENIED -> {
                // Handle authentication/authorization errors
            }
            FirebaseFirestoreException.Code.NOT_FOUND -> {
                // Handle missing document errors
            }
            else -> {
                // Handle other errors
            }
        }
    }
}
```

## Monitoring and Maintenance

1. **Firebase Console**
   - Monitor read/write operations
   - Track query performance
   - Set up alerts for quota usage
   - Review security rules regularly

2. **Client-Side**
   - Implement proper error handling
   - Add retry logic for failures
   - Cache data appropriately
   - Log important operations

## Additional Resources

- [Firebase Documentation](https://firebase.google.com/docs/firestore)
- [Security Rules](https://firebase.google.com/docs/firestore/security/get-started)
- [Transactions and Batched Writes](https://firebase.google.com/docs/firestore/manage-data/transactions)
- [Query Data](https://firebase.google.com/docs/firestore/query-data/queries) 