/**
 * Minimal Test Server
 * 
 * This script creates a minimal Express server for testing the environment variables
 */

// Set environment variables
process.env.NODE_ENV = 'development';
process.env.SKIP_AUTH = 'true';
process.env.PORT = '3007';

// Create a minimal Express server
const express = require('express');
const app = express();

// Add middleware
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// Add logging middleware
app.use((req, res, next) => {
  console.log(`${req.method} ${req.url}`);
  console.log('Headers:', JSON.stringify(req.headers));
  if (req.method === 'POST') {
    console.log('Body:', JSON.stringify(req.body));
  }
  next();
});

// Define test routes
app.get('/api/test/env', (req, res) => {
  res.json({
    success: true,
    env: {
      NODE_ENV: process.env.NODE_ENV,
      SKIP_AUTH: process.env.SKIP_AUTH,
      PORT: process.env.PORT
    }
  });
});

app.get('/api/users/profile', (req, res) => {
  res.json({
    success: true,
    message: 'This is a mock response from the test server',
    user: {
      uid: req.headers['x-firebase-uid'] || 'test-uid',
      displayName: 'Test User',
      email: 'test@example.com'
    }
  });
});

app.post('/api/users/profile', (req, res) => {
  try {
    // Log what we received
    console.log('POST /api/users/profile');
    console.log('Headers:', req.headers);
    console.log('Body:', req.body);
    
    // Create a default user
    const user = {
      uid: 'test-uid',
      displayName: 'Test User',
      email: 'test@example.com',
      lastOnline: new Date().toISOString()
    };
    
    // Add request body data if available
    if (req.body) {
      if (req.body.uid) user.uid = req.body.uid;
      if (req.body.displayName) user.displayName = req.body.displayName;
      if (req.body.email) user.email = req.body.email;
      if (req.body.profilePhoto) user.profilePhoto = req.body.profilePhoto;
    }
    
    res.json({
      success: true,
      message: 'Profile updated successfully (mock response)',
      user: user
    });
  } catch (error) {
    console.error('Error processing request:', error);
    res.status(500).json({
      success: false,
      message: 'Server error',
      error: error.message
    });
  }
});

// Simple route to test if server is running
app.get('/test', (req, res) => {
  res.json({ message: 'Test server is running' });
});

// Add likes route for testing
app.put('/api/users/:uid/likes/:templateId', (req, res) => {
  const { uid, templateId } = req.params;
  console.log(`Received like request for user ${uid} and template ${templateId}`);
  res.json({
    success: true,
    message: 'Test like endpoint called successfully',
    uid,
    templateId
  });
});

// Start server
const PORT = process.env.PORT || 3007;
app.listen(PORT, () => {
  console.log(`Test server running on port ${PORT}`);
  console.log(`Environment: NODE_ENV=${process.env.NODE_ENV}, SKIP_AUTH=${process.env.SKIP_AUTH}`);
}); 