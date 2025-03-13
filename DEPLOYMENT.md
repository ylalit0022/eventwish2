# Deployment Guide for EventWish

This guide provides instructions for deploying the EventWish application to Render.

## Prerequisites

- A Render account (https://render.com)
- Git repository with your EventWish code

## Deployment Steps

### 1. Push your code to a Git repository

Make sure your code is pushed to a Git repository (GitHub, GitLab, etc.) that Render can access.

### 2. Create a new Web Service in Render

1. Log in to your Render dashboard
2. Click "New" and select "Web Service"
3. Connect your Git repository
4. Configure the service:
   - **Name**: eventwish-backend (or your preferred name)
   - **Environment**: Node
   - **Build Command**: `npm install`
   - **Start Command**: `npm start`

### 3. Configure Environment Variables

Add the following environment variables in the Render dashboard:

- `NODE_ENV`: `production`
- `PORT`: `3000`
- `MONGODB_URI`: Your MongoDB connection string

### 4. Deploy the Service

Click "Create Web Service" to deploy your application.

## Troubleshooting

### Common Issues

1. **Missing package.json**: Ensure the package.json file is in the root directory or properly referenced.
2. **Environment Variables**: Make sure all required environment variables are set in the Render dashboard.
3. **Build Errors**: Check the build logs for any errors during the build process.

### Logs

You can view logs in the Render dashboard to diagnose issues:

1. Go to your Web Service in the Render dashboard
2. Click on the "Logs" tab
3. Review the logs for any errors or warnings

## Updating the Deployment

When you push changes to your Git repository, Render will automatically rebuild and deploy your application.

## Additional Resources

- [Render Documentation](https://render.com/docs)
- [Node.js on Render](https://render.com/docs/deploy-node-express-app) 