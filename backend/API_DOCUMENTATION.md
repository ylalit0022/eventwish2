# Event Capture App API Documentation

## Base URL

The API is available at:
```
https://eventwish2.onrender.com/api
```

## User Routes

### Register User
- **URL**: `/users/register`
- **Method**: `POST`
- **Description**: Registers a new user with a device ID or updates the last online time for an existing user
- **Request Body**:
  ```json
  {
    "deviceId": "user-device-id-string"
  }
  ```
- **Success Response**:
  - For new users:
    ```json
    {
      "success": true,
      "message": "User registered successfully",
      "user": {
        "deviceId": "user-device-id-string",
        "lastOnline": "2025-04-06T08:21:58.332Z",
        "created": "2025-04-06T08:21:58.332Z",
        "categories": [],
        "_id": "67f239a64009545d0210140d",
        "createdAt": "2025-04-06T08:21:58.347Z",
        "updatedAt": "2025-04-06T08:21:58.347Z"
      }
    }
    ```
  - For existing users:
    ```json
    {
      "success": true,
      "message": "User already exists",
      "user": {
        // User details including updated lastOnline timestamp
      }
    }
    ```

### Get User by Device ID
- **URL**: `/users/:deviceId`
- **Method**: `GET`
- **Description**: Retrieves user data by device ID
- **URL Parameters**: 
  - `deviceId`: The device ID of the user
- **Success Response**:
  ```json
  {
    "success": true,
    "user": {
      "deviceId": "user-device-id-string",
      "lastOnline": "2025-04-06T08:21:58.332Z",
      "created": "2025-04-06T08:21:58.332Z",
      "categories": [
        {
          "category": "birthday",
          "visitDate": "2025-04-06T08:21:58.532Z",
          "visitCount": 1,
          "source": "direct",
          "_id": "67f239a64009545d02101411"
        }
      ],
      "_id": "67f239a64009545d0210140d",
      "createdAt": "2025-04-06T08:21:58.347Z",
      "updatedAt": "2025-04-06T08:21:58.347Z"
    }
  }
  ```

### Update User Activity
- **URL**: `/users/activity`
- **Method**: `PUT`
- **Description**: Updates user's last online timestamp and optionally records a category visit
- **Request Body**:
  ```json
  {
    "deviceId": "user-device-id-string",
    "category": "birthday" // Optional
  }
  ```
- **Success Response**:
  ```json
  {
    "success": true,
    "message": "User activity updated"
  }
  ```

### Record Template View
- **URL**: `/users/template-view`
- **Method**: `PUT`
- **Description**: Records a template view with its category
- **Request Body**:
  ```json
  {
    "deviceId": "user-device-id-string",
    "templateId": "template123",
    "category": "birthday"
  }
  ```
- **Success Response**:
  ```json
  {
    "success": true,
    "message": "Template view recorded"
  }
  ```

### Get User Recommendations
- **URL**: `/users/:deviceId/recommendations`
- **Method**: `GET`
- **Description**: Get personalized template recommendations for a user based on their activity history
- **URL Parameters**: 
  - `deviceId`: The device ID of the user
- **Query Parameters**:
  - `limit`: Maximum number of recommendations to return (default: 10)
- **Success Response**:
  ```json
  {
    "success": true,
    "recommendations": {
      "templates": [
        {
          "_id": "template123",
          "name": "Birthday Template",
          "category": "birthday",
          "thumbnailUrl": "https://example.com/thumbnail.jpg",
          "score": 0.85
        },
        // More templates...
      ],
      "categories": [
        {
          "name": "birthday",
          "count": 5
        },
        // More categories...
      ]
    }
  }
  ```

## Error Responses

All endpoints may return the following error responses:

### Not Found (404)
```json
{
  "success": false,
  "message": "User not found"
}
```

### Bad Request (400)
```json
{
  "success": false,
  "message": "Template ID and category are required"
}
```

### Server Error (500)
```json
{
  "success": false,
  "message": "Server error",
  "error": "An unexpected error occurred"
}
```

## Testing the API

The user routes API can be tested using standard HTTP client tools like Postman, curl, or any REST API client. 