#!/usr/bin/env bash
# Build script for Render.com deployment

set -e

# Log the Node.js and npm versions
echo "Node version: $(node -v)"
echo "NPM version: $(npm -v)"

# Clean install dependencies
echo "Installing dependencies..."
npm ci

# Verify key dependencies are installed
echo "Verifying dependencies..."
for pkg in swagger-parser fb-watchman lodash.includes path-is-absolute lodash.isinteger homedir-polyfill swagger-ui-dist onetime base64-js color-string find-up node-int64 component-emitter; do
  if [ -d "node_modules/$pkg" ]; then
    echo "✓ $pkg is installed"
  else
    echo "✗ $pkg is missing, installing..."
    npm install $pkg
  fi
done

# Build the application if needed
echo "Build completed successfully" 