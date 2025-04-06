# User API Documentation

This document outlines the implementation of the User API endpoints according to the User.js model.

## User Model

The User model (`models/User.js`) has the following structure:

- **deviceId**: String (required, unique, indexed) - Identifies the user's device
- **lastOnline**: Date - Timestamp of user's last activity
- **created**: Date - Timestamp when the user was first registered
- **categories**: Array of CategoryVisit objects:
  - **category**: String - Name of the category
  - **visitDate**: Date - Time when the category was visited
  - **visitCount**: Number - How many times the category was visited
  - **source**: String (enum: 'direct', 'template') - How the category was accessed

The model includes helper methods:
- **updateLastOnline()**: Updates the lastOnline timestamp
- **visitCategory(categoryName, source)**: Records a category visit
- **visitCategoryFromTemplate(categoryName, templateId)**: Records a template-sourced visit

## API Endpoints

### Register User

- **URL**: `/api/users/register`
- **Method**: `POST`
- **Description**: Registers a new user or returns an existing user
- **Request Body**:
  ```json
  {
    "deviceId": "test-device-uuid" // Required, min 8 chars, max 64 chars
  }
  ```
- **Success Response**:
  - **Code**: 201 (Created) for new users, 200 (OK) for existing users
  - **Content**:
    ```json
    {
      "success": true,
      "message": "User registered successfully", // or "User already exists"
      "user": {
        "deviceId": "test-device-uuid",
        "lastOnline": "2023-06-01T14:30:00.000Z",
        "created": "2023-06-01T14:30:00.000Z",
        "categories": []
      }
    }
    ```

### Get User Data

- **URL**: `/api/users/:deviceId`
- **Method**: `GET`
- **Description**: Retrieves user data by deviceId
- **URL Parameters**: `deviceId` - The device ID of the user
- **Success Response**:
  - **Code**: 200 (OK)
  - **Content**:
    ```json
    {
      "success": true,
      "user": {
        "deviceId": "test-device-uuid",
        "lastOnline": "2023-06-01T14:30:00.000Z",
        "created": "2023-06-01T14:30:00.000Z",
        "categories": [
          {
            "category": "birthday",
            "visitDate": "2023-06-01T14:35:00.000Z",
            "visitCount": 3,
            "source": "direct"
          }
        ]
      }
    }
    ```
- **Error Response**:
  - **Code**: 404 (Not Found)
  - **Content**:
    ```json
    {
      "success": false,
      "message": "User not found"
    }
    ```

### Update User Activity

- **URL**: `/api/users/activity`
- **Method**: `PUT`
- **Description**: Updates user's lastOnline timestamp and optionally records a category visit
- **Request Body**:
  ```json
  {
    "deviceId": "test-device-uuid", // Required
    "category": "birthday", // Optional
    "source": "direct" // Optional, defaults to "direct"
  }
  ```
- **Success Response**:
  - **Code**: 200 (OK)
  - **Content**:
    ```json
    {
      "success": true,
      "message": "User activity updated"
    }
    ```

### Record Template View

- **URL**: `/api/users/template-view`
- **Method**: `PUT`
- **Description**: Records a template view with its category
- **Request Body**:
  ```json
  {
    "deviceId": "test-device-uuid", // Required
    "templateId": "template-123", // Required
    "category": "birthday" // Required
  }
  ```
- **Success Response**:
  - **Code**: 200 (OK)
  - **Content**:
    ```json
    {
      "success": true,
      "message": "Template view recorded"
    }
    ```

## Testing

A test script is provided to verify the functionality:

```
node scripts/test-user-api.js [deviceId]
```

This script tests all the endpoints with a random or specified device ID.

## Implementation Details

The implementation follows the MVC pattern:

1. **Routes** (`routes/userRoutes.js`): Define API endpoints and connect them to controllers
2. **Controllers** (`controllers/userController.js`): Handle request/response logic
3. **Model** (`models/User.js`): Manage data storage and business logic
4. **Validators** (`middleware/validators.js`): Validate input data

The implementation fully supports the tracking of:
- User registration
- Activity tracking
- Category visits
- Template view analytics

This allows for future expansion of personalized content recommendations based on user behavior. 