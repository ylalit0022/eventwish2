#!/bin/bash

# Print Node.js and npm versions
echo "Node.js version: $(node -v)"
echo "npm version: $(npm -v)"

# Install dependencies in the backend directory
cd backend
npm install --force

# Return to the root directory
cd ..

echo "Build completed successfully!" 