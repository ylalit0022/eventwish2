#!/bin/bash

# Production Deployment Script for EventWish Backend
# This script validates required environment variables and prepares for production deployment

set -e  # Exit immediately if a command fails

echo "🚀 EventWish Backend Production Deployment"
echo "=========================================="

# Check for required environment variables
echo "Checking required environment variables..."

REQUIRED_VARS=(
  "FIREBASE_PROJECT_ID"
  "FIREBASE_SERVICE_ACCOUNT"
  "NODE_ENV"
  "JWT_SECRET"
  "API_KEY"
  "INTERNAL_API_KEY"
  "MONGODB_URI"
)

MISSING_VARS=()

for VAR in "${REQUIRED_VARS[@]}"; do
  if [ -z "${!VAR}" ]; then
    MISSING_VARS+=("$VAR")
  fi
done

if [ ${#MISSING_VARS[@]} -ne 0 ]; then
  echo "❌ Error: The following required environment variables are not set:"
  for VAR in "${MISSING_VARS[@]}"; do
    echo "  - $VAR"
  done
  echo "Please set these variables before deploying to production."
  exit 1
fi

# Verify NODE_ENV is set to production
if [ "$NODE_ENV" != "production" ]; then
  echo "❌ Error: NODE_ENV must be set to 'production' for production deployment."
  echo "Current value: $NODE_ENV"
  exit 1
fi

# Validate Firebase service account format
if ! echo "$FIREBASE_SERVICE_ACCOUNT" | jq -e . > /dev/null 2>&1; then
  echo "❌ Error: FIREBASE_SERVICE_ACCOUNT is not valid JSON."
  exit 1
fi

# Validate Firebase project ID matches service account
SERVICE_ACCOUNT_PROJECT_ID=$(echo "$FIREBASE_SERVICE_ACCOUNT" | jq -r '.project_id')
if [ "$SERVICE_ACCOUNT_PROJECT_ID" != "$FIREBASE_PROJECT_ID" ]; then
  echo "⚠️  Warning: FIREBASE_PROJECT_ID ($FIREBASE_PROJECT_ID) does not match the project_id in the service account ($SERVICE_ACCOUNT_PROJECT_ID)."
  echo "This may cause authentication issues. Do you want to continue anyway? (y/n)"
  read -r CONTINUE
  if [ "$CONTINUE" != "y" ]; then
    echo "Deployment cancelled."
    exit 1
  fi
fi

# Check for SKIP_AUTH (should not be set in production)
if [ "$SKIP_AUTH" == "true" ]; then
  echo "❌ Error: SKIP_AUTH is set to 'true'. This is not allowed in production."
  exit 1
fi

# Check MongoDB connection
echo "Verifying MongoDB connection..."
if ! node -e "
const mongoose = require('mongoose');
mongoose.connect(process.env.MONGODB_URI, {
  useNewUrlParser: true,
  useUnifiedTopology: true,
  serverSelectionTimeoutMS: 5000
})
.then(() => {
  console.log('✅ MongoDB connection successful');
  process.exit(0);
})
.catch(err => {
  console.error('❌ MongoDB connection failed:', err.message);
  process.exit(1);
});
"; then
  echo "❌ Error: Could not connect to MongoDB. Please check your MONGODB_URI."
  exit 1
fi

# Check for security best practices
echo "Performing security checks..."

# Check JWT_SECRET strength
if [ ${#JWT_SECRET} -lt 32 ]; then
  echo "⚠️  Warning: JWT_SECRET is less than 32 characters. Consider using a stronger secret."
fi

# Check API_KEY strength
if [ ${#API_KEY} -lt 32 ]; then
  echo "⚠️  Warning: API_KEY is less than 32 characters. Consider using a stronger key."
fi

# Check for .env file (should not be used in production)
if [ -f .env ]; then
  echo "⚠️  Warning: .env file found. In production, environment variables should be set in the hosting environment, not in a .env file."
  echo "Do you want to continue anyway? (y/n)"
  read -r CONTINUE
  if [ "$CONTINUE" != "y" ]; then
    echo "Deployment cancelled."
    exit 1
  fi
fi

# Install production dependencies
echo "Installing production dependencies..."
npm ci --only=production

# Run tests
echo "Running tests..."
npm test

# Build production assets if needed
echo "Building production assets..."
npm run build

echo "✅ Pre-deployment checks passed!"
echo "✅ Application is ready for production deployment."
echo ""
echo "Next steps:"
echo "1. Deploy the application to your production environment"
echo "2. Set up monitoring and logging"
echo "3. Configure automated backups for your MongoDB database"
echo "4. Set up alerts for authentication failures and errors"
echo ""
echo "For secure deployment, remember to:"
echo "- Use HTTPS for all connections"
echo "- Set up proper firewall rules"
echo "- Configure rate limiting for API endpoints"
echo "- Rotate service account keys regularly"
echo "- Monitor for suspicious authentication attempts"
echo ""
echo "🚀 Ready for deployment!" 