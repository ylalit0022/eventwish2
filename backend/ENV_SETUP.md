# Environment Variables Setup Guide

This guide explains how to set up environment variables for both local development and deployment on Render.com.

## Local Development Setup

### Step 1: Generate Secret Keys

Run the following command to generate secure random values for JWT_SECRET, API_KEY, and INTERNAL_API_KEY:

```bash
node scripts/generate-secrets.js
```

This will output values that you can copy to your `.env` file.

### Step 2: Create or Update .env File

Create a `.env` file in the root of the backend directory with the following variables:

```
# Server Configuration
PORT=3000
NODE_ENV=development

# Security
JWT_SECRET=your_generated_jwt_secret
API_KEY=your_generated_api_key
INTERNAL_API_KEY=your_generated_internal_api_key

# Database
MONGODB_URI=mongodb://localhost:27017/your_database_name

# Other Configuration
# Add any other environment variables your application needs
```

### Step 3: Start Your Application

The application will automatically load these environment variables when it starts.

## Render.com Deployment Setup

### Step 1: Generate Secret Keys

Run the script to generate secure keys:

```bash
node scripts/generate-secrets.js
```

Copy the values from the "For Render.com environment variables" section.

### Step 2: Set Environment Variables on Render.com

1. Log in to your Render.com dashboard
2. Select your service
3. Go to the "Environment" tab
4. Add the following environment variables:
   - `NODE_ENV`: Set to `production`
   - `JWT_SECRET`: Paste your generated JWT_SECRET value
   - `API_KEY`: Paste your generated API_KEY value
   - `INTERNAL_API_KEY`: Paste your generated INTERNAL_API_KEY value
   - `MONGODB_URI`: Your MongoDB connection string
   - Add any other environment variables your application needs

### Step 3: Deploy Your Application

After setting the environment variables, deploy your application. The environment variables will be available to your application at runtime.

## Security Best Practices

1. **Never commit your `.env` file to version control**. Make sure it's listed in your `.gitignore` file.
2. **Regularly rotate your secrets**, especially in production environments.
3. **Use different secrets for development and production** environments.
4. **Limit access** to your environment variables in production.
5. **Monitor for unauthorized access** to your API endpoints.

## Troubleshooting

If you encounter issues with environment variables:

1. **Check that the variables are correctly set** in your `.env` file or on Render.com.
2. **Verify that the environment loader is being required** at the beginning of your application.
3. **Check for typos** in environment variable names.
4. **Restart your application** after changing environment variables.
5. **Check the logs** for any warnings or errors related to missing environment variables.

## Automatic Fallbacks

In development mode, if critical environment variables are missing, the application will generate temporary values and display warnings. This is intended for development convenience only and should not be relied upon in production.

For production deployments, the application will throw an error if critical environment variables are missing, preventing the application from starting with insecure defaults. 