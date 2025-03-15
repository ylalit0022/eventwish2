# Deploying AdMob Test Page to Render

This guide provides instructions for deploying the AdMob Test Page to Render.com.

## Prerequisites

- A Render.com account
- Git repository with your AdMob Test Page code

## Deployment Steps

1. **Build the application for production**

   Run the build script to prepare the application for deployment:

   ```bash
   ./build.sh
   ```

   This script will:
   - Install dependencies
   - Create a production environment configuration
   - Build the React application
   - Create a server.js file for serving the app
   - Add Express as a dependency
   - Update the package.json start script

2. **Create a new Web Service on Render**

   - Log in to your Render dashboard
   - Click "New" and select "Web Service"
   - Connect your Git repository
   - Configure the service:
     - **Name**: AdMob Test Page (or your preferred name)
     - **Environment**: Node
     - **Build Command**: `npm install`
     - **Start Command**: `npm start`
     - **Plan**: Free (or select a paid plan for better performance)

3. **Set Environment Variables**

   In the Render dashboard, add the following environment variables:
   - `NODE_ENV`: `production`
   - `API_URL`: `https://eventwish2.onrender.com`

4. **Deploy the Service**

   Click "Create Web Service" to deploy your application.

## Accessing Your Deployed Application

Once deployed, your application will be available at the URL provided by Render (e.g., `https://admob-test-page.onrender.com`).

## Troubleshooting

If you encounter issues with the deployment:

1. **Check the Render logs**
   - Go to your Web Service in the Render dashboard
   - Click on "Logs" to view deployment and runtime logs

2. **Verify API connectivity**
   - Ensure the API server at `https://eventwish2.onrender.com` is running
   - Check that CORS is properly configured on the API server

3. **Test locally before deploying**
   - Run `npm start` locally to verify the application works
   - Use browser developer tools to check for any console errors

## Updating Your Deployment

To update your deployment:

1. Push changes to your Git repository
2. Render will automatically detect the changes and redeploy your application

## Additional Resources

- [Render Documentation](https://render.com/docs)
- [Express.js Documentation](https://expressjs.com/)
- [React Deployment Guide](https://create-react-app.dev/docs/deployment/) 