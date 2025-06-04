#!/bin/bash

# Script to deploy updated routes to the server
# This script assumes you have SSH access to the server and proper permissions

echo "Deploying updated About and Contact routes..."

# Step 1: Backup current files
echo "Backing up current files..."
mkdir -p backup
cp -f routes/aboutRoutes.js backup/aboutRoutes.js.bak
cp -f routes/contactRoutes.js backup/contactRoutes.js.bak

# Step 2: Run tests to ensure models work
echo "Running tests..."
node test-routes.js

# Step 3: Restart the server
echo "Restarting the server..."
if [ -f "pm2.json" ]; then
  pm2 reload pm2.json
else
  # Fallback to manual restart
  echo "PM2 config not found, attempting manual restart..."
  if pgrep -x "node" > /dev/null; then
    pkill -f "node server.js"
    nohup node server.js > server.log 2>&1 &
    echo "Server restarted manually"
  else
    echo "Node process not found, starting server..."
    nohup node server.js > server.log 2>&1 &
  fi
fi

echo "Deployment completed!"
echo "Check server logs for any issues."
echo "You can test the routes with:"
echo "  curl -s -X GET https://eventwish2.onrender.com/api/about"
echo "  curl -s -X GET https://eventwish2.onrender.com/api/contact" 