/**
 * Development Server Starter
 * 
 * Starts the server with development environment variables
 */

const { spawn } = require('child_process');
const path = require('path');

// Set environment variables
process.env.NODE_ENV = 'development';
process.env.SKIP_AUTH = 'true';

// Log the environment variables
console.log('Starting server with environment:');
console.log('NODE_ENV:', process.env.NODE_ENV);
console.log('SKIP_AUTH:', process.env.SKIP_AUTH);

// Start the server
const server = spawn('node', ['server.js'], {
  env: process.env,
  stdio: 'inherit'
});

// Handle server exit
server.on('close', (code) => {
  console.log(`Server process exited with code ${code}`);
});

// Handle errors
server.on('error', (err) => {
  console.error('Failed to start server:', err);
}); 