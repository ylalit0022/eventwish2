# EventWish API Documentation

## Authentication

All endpoints use Firebase UID as the primary user identifier. This enables cross-device authentication and ensures consistent user profiles.

### Important Note
As of version 2.0, all endpoints now require Firebase UID instead of device ID. Please update your client applications accordingly.

## Core Concepts

- **Firebase UID**: The primary user identifier, obtained after Firebase Authentication
- **User Profile**: Contains preferences, engagement history, and user settings
- **Templates**: Design templates for creating wishes
- **Categories**: Template categories for organization

## User Endpoints

### Register User

```
POST /api/users/register
```

Registers a new user or updates an existing user.

**Request Body:**
```json
{
  "uid": "firebase-uid-123456",  // Required
  "displayName": "John Doe",     // Optional
  "email": "user@example.com",   // Optional
  "profilePhoto": "https://..."  // Optional
}
```

**Response:**
```json
{
  "success": true,
  "message": "User registered successfully",
  "user": {
    "uid": "firebase-uid-123456",
    "displayName": "John Doe",
    "email": "user@example.com",
    "profilePhoto": "https://...",
    "lastOnline": "2023-09-15T10:30:45Z",
    "created": "2023-09-15T10:30:45Z"
  }
}
```

### Update User Profile

```
POST /api/users/profile
```

Updates or creates a user profile after Firebase authentication.

**Request Body:**
```json
{
  "uid": "firebase-uid-123456",           // Required
  "displayName": "John Doe",              // Optional
  "email": "user@example.com",            // Optional
  "profilePhoto": "https://...",          // Optional
  "lastOnline": "2023-09-15T10:30:45Z"    // Optional
}
```

**Response:**
```json
{
  "success": true,
  "message": "User profile updated",
  "user": {
    "uid": "firebase-uid-123456",
    "displayName": "John Doe",
    "email": "user@example.com",
    "profilePhoto": "https://...",
    "lastOnline": "2023-09-15T10:30:45Z"
  }
}
```

### Update User Activity

```
PUT /api/users/activity
```

Updates user's last online status and optionally records category visits.

**Request Body:**
```json
{
  "uid": "firebase-uid-123456",   // Required
  "category": "birthday",         // Optional
  "source": "direct"              // Optional (default: "direct")
}
```

**Response:**
```json
{
  "success": true,
  "message": "User activity updated"
}
```

### Record Template View

```
PUT /api/users/template-view
```

Records when a user views a template.

**Request Body:**
```json
{
  "uid": "firebase-uid-123456",    // Required
  "templateId": "template-123",    // Required
  "category": "birthday"           // Required
}
```

**Response:**
```json
{
  "success": true,
  "message": "Template view recorded"
}
```

### Update User Preferences

```
PUT /api/users/preferences
```

Updates user preferences for theme, language, notifications, etc.

**Request Body:**
```json
{
  "uid": "firebase-uid-123456",             // Required
  "preferences": {                          // Required
    "preferredTheme": "dark",               // Optional
    "preferredLanguage": "en",              // Optional
    "timezone": "America/New_York",         // Optional
    "pushPreferences": {                    // Optional
      "allowFestivalPush": true,
      "allowPersonalPush": true
    },
    "topicSubscriptions": ["diwali", "holi"], // Optional
    "muteNotificationsUntil": "2023-09-16T10:30:45Z" // Optional
  }
}
```

**Response:**
```json
{
  "success": true,
  "message": "User preferences updated successfully",
  "user": {
    "preferredTheme": "dark",
    "preferredLanguage": "en",
    "timezone": "America/New_York",
    "pushPreferences": {
      "allowFestivalPush": true,
      "allowPersonalPush": true
    },
    "topicSubscriptions": ["diwali", "holi"],
    "muteNotificationsUntil": "2023-09-16T10:30:45Z"
  }
}
```

### Record User Engagement

```
POST /api/users/engagement
```

Records detailed user engagement metrics.

**Request Body:**
```json
{
  "uid": "firebase-uid-123456",    // Required
  "type": 2,                       // Required - 1: Category visit, 2: Template view, 3: Template use, 4: Like, 5: Favorite
  "templateId": "template-123",    // Required for types 2-5
  "category": "birthday",          // Required for types 1-5
  "timestamp": 1631698245000,      // Optional (default: current time)
  "source": "recommendation"       // Optional (default: "direct")
}
```

**Response:**
```json
{
  "success": true,
  "message": "Engagement recorded successfully"
}
```

### Batch Engagement Sync

```
POST /api/users/engagement/sync
```

Syncs multiple engagement records in a batch.

**Request Body:**
```json
{
  "uid": "firebase-uid-123456",    // Required
  "engagements": [                 // Required
    {
      "type": 2,
      "templateId": "template-123",
      "category": "birthday",
      "timestamp": 1631698245000
    },
    {
      "type": 4,
      "templateId": "template-456",
      "category": "anniversary"
    }
  ]
}
```

**Response:**
```json
{
  "success": true,
  "message": "Processed 2 of 2 engagement records"
}
```

### Get User

```
GET /api/users/:identifier
```

Gets user data. The identifier parameter should be the Firebase UID.

**Response:**
```json
{
  "success": true,
  "user": {
    "uid": "firebase-uid-123456",
    "displayName": "John Doe",
    "email": "user@example.com",
    "profilePhoto": "https://...",
    "lastOnline": "2023-09-15T10:30:45Z",
    "created": "2023-09-15T10:30:45Z",
    "categories": [...],
    "favorites": [...],
    "likes": [...],
    "recentTemplatesUsed": [...]
  }
}
```

### Get User's Favorite Templates

```
GET /api/users/:identifier/templates/favorites
```

Gets user's favorite templates. The identifier parameter should be the Firebase UID.

**Response:**
```json
{
  "success": true,
  "favorites": [
    {
      "id": "template-123",
      "name": "Birthday Wish",
      "category": "birthday",
      "imageUrl": "https://..."
    }
  ]
}
```

### Get User's Liked Templates

```
GET /api/users/:identifier/templates/likes
```

Gets user's liked templates. The identifier parameter should be the Firebase UID.

**Response:**
```json
{
  "success": true,
  "likes": [
    {
      "id": "template-123",
      "name": "Birthday Wish",
      "category": "birthday",
      "imageUrl": "https://..."
    }
  ]
}
```

### Get User's Recent Templates

```
GET /api/users/:identifier/templates/recent
```

Gets user's recently used templates. The identifier parameter should be the Firebase UID.

**Response:**
```json
{
  "success": true,
  "recentTemplates": [
    {
      "id": "template-123",
      "name": "Birthday Wish",
      "category": "birthday",
      "imageUrl": "https://..."
    }
  ]
}
```

### Get User Recommendations

```
GET /api/users/:identifier/recommendations
```

Gets personalized template recommendations for a user. The identifier parameter should be the Firebase UID.

**Query Parameters:**
- `limit` (optional): Maximum number of recommendations to return (default: 10)

**Response:**
```json
{
  "success": true,
  "recommendations": {
    "templates": [...],
    "topCategories": [
      {
        "category": "birthday",
        "score": 0.85,
        "weight": 10
      }
    ],
    "lastUpdated": "2023-09-15T10:30:45Z"
  }
}
```

## Migration Guide

### Migrating from Device ID to Firebase UID

If your application is currently using device IDs, follow these steps to migrate to Firebase UID:

1. **Update API Calls**: Replace all device ID parameters with Firebase UID in your API calls
2. **Authenticate with Firebase**: Ensure your client implements Firebase Authentication
3. **Update Local Storage**: If storing user IDs locally, update to store Firebase UID
4. **Handle Legacy Users**: For users who don't have a Firebase account, generate a temporary UID using our migration endpoints

### Migration Timeline

- **Sept 2023**: Both device ID and Firebase UID supported
- **Nov 2023**: Firebase UID preferred, device ID supported with warnings
- **Jan 2024**: Firebase UID required for all endpoints

## Client Implementation Example

```javascript
// Sample client code for user registration with Firebase UID
async function registerUser(firebaseUser) {
  const response = await fetch('https://api.eventwish.com/api/users/register', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      uid: firebaseUser.uid,
      displayName: firebaseUser.displayName,
      email: firebaseUser.email,
      profilePhoto: firebaseUser.photoURL
    })
  });
  
  return await response.json();
}
``` 