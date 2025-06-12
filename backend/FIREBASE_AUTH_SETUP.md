# Firebase Authentication Setup Guide for Production

## Overview

This document explains how to set up Firebase Authentication for the EventWish backend server in a production environment. It addresses the "Unable to detect a Project Id in the current environment" error and provides best practices for secure deployment.

## Prerequisites

1. A Firebase project created in the [Firebase Console](https://console.firebase.google.com/)
2. Admin access to your Firebase project
3. The backend server codebase
4. A secure environment for storing credentials

## Production Environment Variables

The backend requires the following environment variables for Firebase authentication in production:

| Variable | Description | Required in Production |
|----------|-------------|-----------|
| `FIREBASE_SERVICE_ACCOUNT` | JSON string of your Firebase service account credentials | **Required** |
| `FIREBASE_PROJECT_ID` | Your Firebase project ID | **Required** |
| `NODE_ENV` | Must be set to 'production' | **Required** |

## Production Setup Instructions

### 1. Generate Firebase Service Account Credentials

1. Go to your Firebase project in the [Firebase Console](https://console.firebase.google.com/)
2. Click on Project Settings (gear icon)
3. Go to the "Service accounts" tab
4. Click "Generate new private key"
5. Save the JSON file securely

### 2. Set Environment Variables in Production

#### Using Environment Variables in Your Production Environment

Set these environment variables in your production environment (e.g., Render, Heroku, AWS, etc.):

```
FIREBASE_SERVICE_ACCOUNT=<full JSON content of your service account file>
FIREBASE_PROJECT_ID=<your-project-id>
NODE_ENV=production
```

#### Using a .env File (Not Recommended for Production)

If you must use a .env file in production (not recommended for security reasons):

1. Create a .env file in the backend directory
2. Add the following content:

```
FIREBASE_SERVICE_ACCOUNT={"type":"service_account","project_id":"your-project-id","private_key_id":"...","private_key":"...","client_email":"...","client_id":"...","auth_uri":"...","token_uri":"...","auth_provider_x509_cert_url":"...","client_x509_cert_url":"..."}
FIREBASE_PROJECT_ID=your-project-id
NODE_ENV=production
```

### 3. Verify Service Account Permissions

Ensure your service account has the necessary permissions:

1. In the Firebase Console, go to Project Settings > Service accounts
2. Verify that the service account has the "Firebase Authentication Admin" role
3. If needed, adjust permissions in the Google Cloud Console

## Security Best Practices

1. **Never commit service account credentials to version control**
2. **Use environment variables or secret management systems**
   - Use your hosting provider's secrets/environment variable management
   - Consider using a vault service like HashiCorp Vault or AWS Secrets Manager
3. **Restrict the service account's permissions**
   - Follow the principle of least privilege
   - Only grant permissions needed for token verification
4. **Rotate service account keys periodically**
   - Create a new key before disabling the old one
   - Update environment variables with the new key
   - Disable the old key after confirming the new one works
5. **Monitor authentication logs**
   - Set up alerts for authentication failures
   - Review logs regularly for suspicious activity
6. **Implement proper error handling**
   - Use appropriate HTTP status codes
   - Provide clear error messages to clients
   - Log detailed errors server-side but return limited info to clients

## Troubleshooting Production Issues

### "Unable to detect a Project Id in the current environment" Error

In production, this error occurs when:

1. `FIREBASE_SERVICE_ACCOUNT` environment variable is missing or invalid
2. `FIREBASE_PROJECT_ID` environment variable is missing
3. The service account JSON is malformed

**Solution:**

1. Verify both environment variables are set correctly
2. Check the format of your service account JSON
3. Ensure the service account has not been revoked or disabled
4. Restart your server after updating environment variables

### Other Common Production Issues

1. **Token verification timeout**
   - Check network connectivity between your server and Firebase
   - Increase the timeout value in the auth middleware if needed
   - Consider implementing a circuit breaker pattern

2. **Rate limiting errors**
   - Firebase has rate limits for authentication operations
   - Implement caching for token verification results
   - Add exponential backoff for retries

3. **Token expired errors**
   - Ensure your server's clock is synchronized (use NTP)
   - Implement proper token refresh on the client side

## Client-Side Implementation

For robustness in production, the Android client should:

1. Implement token refresh before expiration
2. Handle 401/403 errors with proper user feedback
3. Store actions locally when authentication fails
4. Implement a background sync mechanism
5. Add retry logic with exponential backoff

## Monitoring and Maintenance

1. Set up monitoring for authentication failures
2. Create alerts for unusual authentication patterns
3. Regularly rotate service account keys (every 90 days recommended)
4. Keep Firebase Admin SDK updated to the latest version
5. Review Firebase Authentication logs periodically

## Additional Resources

- [Firebase Admin SDK Documentation](https://firebase.google.com/docs/admin/setup)
- [Firebase Authentication Documentation](https://firebase.google.com/docs/auth)
- [Google Cloud Authentication Documentation](https://cloud.google.com/docs/authentication)
- [Firebase Security Best Practices](https://firebase.google.com/docs/projects/security-best-practices) 