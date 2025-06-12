# Firebase Authentication Setup for EventWish Backend

This guide explains how to set up Firebase Authentication for the EventWish backend server in both development and production environments.

## Development Setup

For development, you can use the provided scripts that set up a development environment with a default project ID:

### Using PowerShell:

```powershell
.\start-dev-server.ps1
```

### Using Batch (Windows CMD):

```cmd
.\start-dev-server.bat
```

These scripts:
- Set `NODE_ENV` to "development"
- Set `FIREBASE_PROJECT_ID` to "eventwish-app"
- Enable `SKIP_AUTH` to bypass authentication checks
- Generate temporary secrets for development

## Production Setup

For production, you need a Firebase service account file and proper environment variables:

### Step 1: Get Firebase Service Account Credentials

1. Go to your Firebase project in the [Firebase Console](https://console.firebase.google.com/)
2. Click on Project Settings (gear icon)
3. Go to the "Service accounts" tab
4. Click "Generate new private key"
5. Save the JSON file as `firebase-service-account.json` in the backend directory

### Step 2: Set Required Environment Variables

For PowerShell, you can use the provided script:

```powershell
# Set these environment variables first
$env:JWT_SECRET = "your-secure-jwt-secret"
$env:API_KEY = "your-secure-api-key"
$env:INTERNAL_API_KEY = "your-secure-internal-api-key"

# Then run the script
.\start-prod-server.ps1
```

### Step 3: Start the Server

The `start-prod-server.ps1` script will:
- Verify the service account file exists and is valid
- Extract the project ID from the service account
- Set the required environment variables
- Start the server in production mode

## Manual Configuration

If you need to manually configure the environment variables:

### Required Environment Variables for Production:

| Variable | Description | Required? |
|----------|-------------|-----------|
| `FIREBASE_SERVICE_ACCOUNT` | JSON string of your Firebase service account credentials | **Required** |
| `FIREBASE_PROJECT_ID` | Your Firebase project ID | **Required** |
| `NODE_ENV` | Must be set to 'production' | **Required** |
| `JWT_SECRET` | Secret for JWT signing | **Required** |
| `API_KEY` | API key for external access | **Required** |
| `INTERNAL_API_KEY` | API key for internal services | **Required** |

### Setting Environment Variables Manually:

#### PowerShell:

```powershell
$env:NODE_ENV = "production"
$env:FIREBASE_PROJECT_ID = "your-project-id"
$env:FIREBASE_SERVICE_ACCOUNT = Get-Content -Raw firebase-service-account.json
$env:JWT_SECRET = "your-secure-jwt-secret"
$env:API_KEY = "your-secure-api-key"
$env:INTERNAL_API_KEY = "your-secure-internal-api-key"
```

#### Windows CMD:

```cmd
set NODE_ENV=production
set FIREBASE_PROJECT_ID=your-project-id
set FIREBASE_SERVICE_ACCOUNT=<contents of service account JSON>
set JWT_SECRET=your-secure-jwt-secret
set API_KEY=your-secure-api-key
set INTERNAL_API_KEY=your-secure-internal-api-key
```

#### Linux/macOS:

```bash
export NODE_ENV="production"
export FIREBASE_PROJECT_ID="your-project-id"
export FIREBASE_SERVICE_ACCOUNT="$(cat firebase-service-account.json)"
export JWT_SECRET="your-secure-jwt-secret"
export API_KEY="your-secure-api-key"
export INTERNAL_API_KEY="your-secure-internal-api-key"
```

## Troubleshooting

### "Unable to detect a Project Id in the current environment" Error

This error occurs when the Firebase Admin SDK cannot determine which Firebase project to use for token validation.

**Solution:**

1. Ensure `FIREBASE_PROJECT_ID` is set correctly
2. Verify the service account JSON is valid and contains a `project_id` field
3. For development, use the provided development scripts

### Other Common Issues

1. **Token verification timeout**:
   - Check network connectivity
   - Increase the timeout value in the auth middleware

2. **Rate limiting errors**:
   - Firebase has rate limits for authentication operations
   - Implement caching for token verification results

3. **Token expired errors**:
   - Ensure your server's clock is synchronized
   - Implement proper token refresh on the client side

## Security Best Practices

1. **Never commit service account credentials to version control**
2. **Use environment variables or secret management systems**
3. **Restrict the service account's permissions to only what's necessary**
4. **Rotate service account keys periodically**
5. **In production, use proper CI/CD secrets management**

## Additional Resources

- [Firebase Admin SDK Documentation](https://firebase.google.com/docs/admin/setup)
- [Firebase Authentication Documentation](https://firebase.google.com/docs/auth)
- [Google Cloud Authentication Documentation](https://cloud.google.com/docs/authentication) 