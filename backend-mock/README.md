# EventWish Mock Authentication Server

This is a mock server for testing the Firebase Phone Authentication flow in the EventWish app without requiring a real backend deployment.

## Features

- Simulated authentication endpoints
- JWT token generation and validation
- In-memory user database
- Password reset flow
- Detailed request logging

## Setup

1. Install dependencies:
```
npm install
```

2. Start the server:
```
npm start
```

The server will run on http://localhost:3001.

## Connecting from a Real Device

To connect from a real Android device:

1. Make sure your PC and Android device are on the same WiFi network.

2. Find your PC's IP address:
   - On Windows: Open Command Prompt and run `ipconfig`
   - On Mac: Open Terminal and run `ifconfig`
   - On Linux: Open Terminal and run `ip addr show`

3. Update the `REAL_DEVICE_LOCAL_URL` in `ApiClient.java` with your IP address:
   ```java
   private static final String REAL_DEVICE_LOCAL_URL = "http://YOUR_IP_ADDRESS:3001/api/";
   ```

4. Allow incoming connections to port 3001 in your firewall settings

5. Test the connection by opening a browser on your device and navigating to:
   ```
   http://YOUR_IP_ADDRESS:3001
   ```

## API Endpoints

### Authentication
- `POST /api/auth/register`: Register a new user
  ```json
  {
    "phoneNumber": "+1234567890",
    "password": "securePassword",
    "firebaseUid": "firebase123",
    "idToken": "firebase-token",
    "displayName": "John Doe",
    "email": "john@example.com",
    "photoUrl": "https://example.com/photo.jpg"
  }
  ```
  
- `POST /api/auth/login`: Login with credentials
  ```json
  {
    "phoneNumber": "+1234567890",
    "password": "securePassword"
  }
  ```
  
- `POST /api/auth/refresh`: Refresh authentication token
  ```json
  {
    "refreshToken": "your-refresh-token"
  }
  ```
  
- `POST /api/auth/logout`: Logout user (requires Authorization header)
  
- `GET /api/auth/me`: Get current user info (requires Authorization header)

### Password Reset
- `POST /api/auth/password/reset/send-code`: Request password reset code
  ```json
  {
    "phoneNumber": "+1234567890"
  }
  ```
  
- `POST /api/auth/password/reset`: Reset password with verification code
  ```json
  {
    "phoneNumber": "+1234567890",
    "verificationCode": "123456",
    "newPassword": "newSecurePassword"
  }
  ```

## Testing

For automated testing, run:
```
npm test
```

This will execute a complete auth flow test against the API.

## Development

For hot-reloading during development:
```
npm run dev
```

## Notes

- This is a mock server for testing purposes only
- All data is stored in memory and will be lost when the server is restarted
- The server always accepts "123456" as a valid verification code
- The server does not actually verify Firebase tokens 