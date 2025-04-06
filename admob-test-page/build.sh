#!/bin/bash

# Build script for AdMob Test Page
echo "Building AdMob Test Page for production..."

# Check if Node.js is installed
if ! command -v node &> /dev/null; then
    echo "Error: Node.js is not installed. Please install Node.js to build this application."
    exit 1
fi

# Check if npm is installed
if ! command -v npm &> /dev/null; then
    echo "Error: npm is not installed. Please install npm to build this application."
    exit 1
fi

# Install dependencies if node_modules doesn't exist
if [ ! -d "node_modules" ]; then
    echo "Installing dependencies..."
    npm install
fi

# Create .env file for production
echo "Creating production environment configuration..."
cat > .env << EOL
NODE_ENV=production
API_URL=https://eventwish2.onrender.com
EOL

# Build the production version
echo "Building production version..."
npm run build

# Check if build was successful
if [ $? -eq 0 ]; then
    echo "Build completed successfully!"
    echo "Production files are available in the 'dist' directory."
    echo "To deploy, copy the contents of the 'dist' directory to your web server."
    
    # Create a simple server.js file for serving the app on Render
    echo "Creating server.js for Render deployment..."
    cat > server.js << EOL
const express = require('express');
const path = require('path');
const app = express();

// Serve static files from the dist directory
app.use(express.static(path.join(__dirname, 'dist')));

// For any request that doesn't match a static file, serve index.html
app.get('*', (req, res) => {
  res.sendFile(path.join(__dirname, 'dist', 'index.html'));
});

// Get port from environment variable or default to 3000
const PORT = process.env.PORT || 3000;

app.listen(PORT, () => {
  console.log(\`Server is running on port \${PORT}\`);
});
EOL

    # Add express to package.json if not already there
    if ! grep -q '"express":' package.json; then
        echo "Adding express dependency for server..."
        npm install --save express
    fi
    
    # Create a start script for Render
    if ! grep -q '"start":.*node server.js' package.json; then
        echo "Updating package.json start script for Render..."
        sed -i 's/"scripts": {/"scripts": {\n    "start": "node server.js",/g' package.json
    fi
    
    echo "Deployment files prepared successfully!"
else
    echo "Error: Build failed. Please check the error messages above."
    exit 1
fi

exit 0 