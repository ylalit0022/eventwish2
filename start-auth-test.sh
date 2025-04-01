#!/bin/bash

echo "Starting EventWish Firebase Authentication Test Environment"
echo "========================================================"

# Check if Node.js is installed
if ! command -v node &> /dev/null; then
  echo "ERROR: Node.js is not installed or not in your PATH"
  echo "Please install Node.js from https://nodejs.org/"
  exit 1
fi

# Navigate to backend-mock directory
cd backend-mock || {
  echo "ERROR: Cannot find backend-mock directory"
  echo "Please make sure you are running this script from the project root"
  exit 1
}

# Check if the directory is correctly set up
if [ ! -f "package.json" ]; then
  echo "ERROR: Cannot find backend-mock/package.json"
  echo "Please make sure you are running this script from the project root"
  exit 1
fi

# Install dependencies if not already installed
if [ ! -d "node_modules" ]; then
  echo "Installing dependencies..."
  npm install || {
    echo "ERROR: Failed to install dependencies"
    exit 1
  }
fi

# Start the server in the background
echo "Starting mock server..."
npm start &
SERVER_PID=$!

# Store PID for later cleanup
echo $SERVER_PID > server.pid

# Wait for server to start
echo "Waiting for server to start..."
sleep 5

# Run the tests
echo "Running authentication tests..."
npm test

# Cleanup
echo
echo "=========================================================="
echo "Tests completed. Stopping mock server..."
if [ -f "server.pid" ]; then
  kill $(cat server.pid)
  rm server.pid
  echo "Server stopped."
else
  echo "Warning: Could not find server PID file to stop the server."
  echo "You may need to manually stop the Node.js process."
fi

echo
echo "Test completed." 