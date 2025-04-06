#!/bin/bash

# Exit on error
set -e

echo "Starting deployment script for EventWish backend..."

# Check if git is installed
if ! command -v git &> /dev/null; then
    echo "Error: git is not installed"
    exit 1
fi

# Check if deployment environment is specified
ENVIRONMENT=${1:-production}
echo "Deploying to $ENVIRONMENT environment"

# Define the branch to deploy based on environment
if [ "$ENVIRONMENT" = "production" ]; then
    BRANCH="main"
elif [ "$ENVIRONMENT" = "staging" ]; then
    BRANCH="develop"
else
    BRANCH=$ENVIRONMENT
fi

# Get current directory
CURRENT_DIR=$(pwd)

# Check if we're in the backend directory, if not navigate to it
if [[ ! "$CURRENT_DIR" == *"backend"* ]]; then
    echo "Navigating to backend directory..."
    cd backend || { echo "Error: backend directory not found"; exit 1; }
fi

# Pull latest changes from the repository
echo "Pulling latest changes from $BRANCH branch..."
git checkout $BRANCH
git pull

# Install dependencies
echo "Installing dependencies..."
npm install

# Run tests to ensure everything is working
echo "Running tests..."
npm test

# Check if the tests passed
if [ $? -ne 0 ]; then
    echo "Tests failed. Aborting deployment."
    exit 1
fi

# Build the application if needed
if [ -f "package.json" ] && grep -q '"build"' package.json; then
    echo "Building the application..."
    npm run build
fi

# Deploy to the hosting service if Render CLI is installed
if command -v render &> /dev/null; then
    echo "Deploying to Render..."
    render deploy
else
    echo "Render CLI not installed. Manual deployment required."
    echo "Visit https://dashboard.render.com to deploy manually."
fi

# Check for a post-deployment script
if [ -f "post-deploy.sh" ]; then
    echo "Running post-deployment script..."
    bash post-deploy.sh
fi

echo "Deployment script completed successfully."

# Print deployment summary
echo "=== Deployment Summary ==="
echo "Environment: $ENVIRONMENT"
echo "Branch: $BRANCH"
echo "Timestamp: $(date)"
echo "========================="

exit 0 