/**
 * Development Server Starter with Explicit Environment Variables
 */

const { spawn } = require('child_process');
const path = require('path');

// Define environment variables
const env = {
  ...process.env,  // Keep existing environment variables
  NODE_ENV: 'development',
  SKIP_AUTH: 'true',
  PORT: '3005'
};

console.log('Starting server with environment:');
console.log('NODE_ENV:', env.NODE_ENV);
console.log('SKIP_AUTH:', env.SKIP_AUTH);
console.log('PORT:', env.PORT);

// Start server process
const serverProcess = spawn('node', ['server.js'], {
  env,
  stdio: 'inherit'  // Show server output in the console
});

// Handle process events
serverProcess.on('error', (err) => {
  console.error('Failed to start server:', err);
});

serverProcess.on('exit', (code, signal) => {
  if (code) {
    console.error(`Server process exited with code ${code}`);
  } else if (signal) {
    console.error(`Server process killed with signal ${signal}`);
  } else {
    console.log('Server process exited normally');
  }
}); 