# Deploying to Render.com

This guide provides step-by-step instructions for deploying the EventWish AdMob backend to Render.com.

## Prerequisites

- A Render.com account
- Your code pushed to a Git repository (GitHub, GitLab, etc.)
- Generated API keys and secrets (using the `generate-secrets.js` script)

## Step 1: Generate Secure Keys

Before deploying to Render.com, you need to generate secure keys. Run the following command locally:

```bash
cd backend
npm run generate-secrets
```

This will output values for `JWT_SECRET`, `API_KEY`, and `INTERNAL_API_KEY` that you'll use in the deployment.

## Step 2: Create a New Web Service on Render.com

1. Log in to your [Render.com Dashboard](https://dashboard.render.com/)
2. Click "New" and select "Web Service"
3. Connect your Git repository
4. Configure the service:
   - **Name**: Choose a name for your service (e.g., "eventwish-admob-backend")
   - **Region**: Choose a region close to your users
   - **Branch**: Select the branch you want to deploy (e.g., "main" or "master")
   - **Root Directory**: If your backend is in a subdirectory, specify it (e.g., "backend")
   - **Runtime**: Select "Node"
   - **Build Command**: `npm install`
   - **Start Command**: `npm start`

## Step 3: Set Environment Variables

1. Scroll down to the "Environment" section
2. Add the following environment variables:

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

## Step 4: Deploy Your Application

1. Click "Create Web Service" to start the deployment process
2. Render will clone your repository, install dependencies, and start your application
3. Once the deployment is complete, you'll see a URL for your service

## Step 5: Verify Deployment

1. Visit the URL provided by Render to verify that your application is running
2. Check the health endpoint: `https://your-service-url.onrender.com/api/health`
3. Check the logs in the Render dashboard for any errors

## Troubleshooting

If you encounter issues during deployment:

### Missing Dependencies

If you see errors about missing dependencies, check your package.json to ensure all dependencies are listed correctly.

### Environment Variables

If your application can't access environment variables, verify that they are set correctly in the Render dashboard.

### Database Connection

If your application can't connect to the database, check the MongoDB URI and ensure that your database is accessible from Render.com.

### Missing Files

If you see errors about missing files (like the one you encountered with cacheService.js), make sure all required files are included in your repository.

## Monitoring and Scaling

Render.com provides several features for monitoring and scaling your application:

1. **Logs**: View real-time logs in the Render dashboard
2. **Metrics**: Monitor CPU and memory usage
3. **Auto-scaling**: Configure auto-scaling for high-traffic periods
4. **Custom domains**: Set up custom domains for your service

## Security Best Practices

1. **Environment Variables**: Never commit sensitive environment variables to your repository
2. **API Keys**: Regularly rotate API keys and secrets
3. **HTTPS**: Render.com provides HTTPS by default, ensuring secure communication
4. **Rate Limiting**: Implement rate limiting to prevent abuse

## Next Steps

After deploying your application, consider:

1. Setting up monitoring and alerting
2. Implementing CI/CD for automated deployments
3. Configuring auto-scaling for high-traffic periods
4. Setting up a custom domain for your service 