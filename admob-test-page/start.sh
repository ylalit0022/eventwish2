#!/bin/bash

# Start script for AdMob Test Page
echo "Starting AdMob Test Page..."

# Check if Node.js is installed
if ! command -v node &> /dev/null; then
    echo "Error: Node.js is not installed. Please install Node.js to run this application."
    exit 1
fi

# Check if npm is installed
if ! command -v npm &> /dev/null; then
    echo "Error: npm is not installed. Please install npm to run this application."
    exit 1
fi

# Install dependencies if node_modules doesn't exist
if [ ! -d "node_modules" ]; then
    echo "Installing dependencies..."
    npm install
fi

# Check if we should run in development mode
if [ "$1" == "--dev" ] || [ "$1" == "-d" ]; then
    echo "Starting development server..."
    npm run dev
else
    # Check if dist directory exists
    if [ ! -d "dist" ]; then
        echo "Building production version..."
        npm run build
    fi
    
    echo "Starting production server..."
    npm start
fi

# Exit with the npm start exit code
exit $? 