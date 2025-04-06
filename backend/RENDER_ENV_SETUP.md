# Setting Up Environment Variables on Render.com

This guide provides step-by-step instructions for setting up environment variables for your EventWish AdMob backend on Render.com.

## Prerequisites

- A Render.com account
- Your application deployed on Render.com
- Generated API keys and secrets (using the `generate-secrets.js` script)

## Step 1: Generate Secure Keys

Before setting up your environment variables on Render.com, you need to generate secure keys. Run the following command locally:

```bash
cd backend
npm run generate-secrets
```

This will output values for `JWT_SECRET`, `API_KEY`, and `INTERNAL_API_KEY` that you'll use in the next steps.

## Step 2: Access Your Service on Render.com

1. Log in to your [Render.com Dashboard](https://dashboard.render.com/)
2. Select your EventWish AdMob backend service
3. Navigate to the "Environment" tab in the left sidebar

## Step 3: Add Environment Variables

Add the following environment variables:

### Required Variables

| Key | Value | Description |
|-----|-------|-------------|
| `NODE_ENV` | `production` | Sets the application to production mode |
| `PORT` | `10000` | The port Render will use (Render may override this) |
| `JWT_SECRET` | `your-generated-jwt-secret` | Secret for JWT token generation and validation |
| `API_KEY` | `your-generated-api-key` | API key for authenticating API requests |
| `INTERNAL_API_KEY` | `your-generated-internal-api-key` | API key for internal health checks |
| `MONGODB_URI` | `your-mongodb-connection-string` | Connection string for your MongoDB database |

### Optional Variables

| Key | Value | Description |
|-----|-------|-------------|
| `LOG_LEVEL` | `info` | Logging level (error, warn, info, http, verbose, debug, silly) |
| `REDIS_URL` | `your-redis-url` | URL for Redis cache (if used) |
| `VALID_APP_SIGNATURES` | `sig1,sig2,sig3` | Comma-separated list of valid app signatures |
| `LOAD_BALANCER_ENABLED` | `true` | Enable load balancing for high traffic |
| `RATE_LIMIT_WINDOW_MS` | `15000` | Rate limiting window in milliseconds |
| `RATE_LIMIT_MAX_REQUESTS` | `100` | Maximum requests per window |

## Step 4: Save Environment Variables

After adding all the necessary environment variables, click the "Save Changes" button at the bottom of the page.

## Step 5: Redeploy Your Application

For the changes to take effect, you need to redeploy your application:

1. Go to the "Manual Deploy" section
2. Click "Deploy latest commit" or select a specific commit to deploy

## Step 6: Verify Environment Variables

After deployment, you can verify that your environment variables are correctly set by checking the logs:

1. Go to the "Logs" tab
2. Look for startup logs that indicate environment variables are loaded
3. Check for any errors related to missing environment variables

## Troubleshooting

If you encounter issues with environment variables on Render.com:

1. **Variables not being recognized**: Make sure you've saved the changes and redeployed the application.
2. **Sensitive data exposure**: Double-check that you haven't accidentally logged sensitive values.
3. **Application crashes on startup**: Verify that all required environment variables are set.
4. **Database connection issues**: Ensure your MongoDB URI is correct and the database is accessible from Render.com.

## Security Best Practices

- Regularly rotate your secrets, especially in production environments
- Use different secrets for development and production environments
- Monitor for unauthorized access to your API endpoints
- Consider using Render.com's secret files feature for larger secrets

## Next Steps

After setting up your environment variables, you should:

1. Test your application to ensure it's working correctly
2. Set up monitoring to detect any issues
3. Implement a process for rotating secrets regularly

For more information on environment variables and security, refer to the [Render.com documentation](https://render.com/docs/environment-variables). 