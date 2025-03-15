#!/bin/bash

# Print Node.js and npm versions
echo "Node.js version: $(node -v)"
echo "npm version: $(npm -v)"

# Go to the backend directory
cd backend

# Clean installation
echo "Cleaning previous installation..."
rm -rf node_modules package-lock.json

# Install dependencies
echo "Installing dependencies..."
npm install --no-optional

# Install specific mongodb version compatible with Mongoose 7.6.3
echo "Installing specific mongodb version..."
npm install mongodb@5.9.0 --save

# Return to the root directory
cd ..

echo "Build completed successfully!" 