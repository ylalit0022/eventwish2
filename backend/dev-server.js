/**
 * Development Server with Environment Variables
 * 
 * This script sets environment variables directly in Node.js
 * before requiring the server.js file, to ensure they are
 * set before dotenv loads from .env.
 */

// Set environment variables first
process.env.NODE_ENV = 'development';
process.env.SKIP_AUTH = 'true';
process.env.PORT = '3005';

// Add a dummy Firebase service account
process.env.FIREBASE_SERVICE_ACCOUNT = JSON.stringify({
  type: 'service_account',
  project_id: 'test-project',
  private_key_id: 'dummy',
  private_key: '-----BEGIN PRIVATE KEY-----\nDummy\n-----END PRIVATE KEY-----\n',
  client_email: 'dummy@test-project.iam.gserviceaccount.com',
  client_id: '123456789',
  auth_uri: 'https://accounts.google.com/o/oauth2/auth',
  token_uri: 'https://oauth2.googleapis.com/token',
  auth_provider_x509_cert_url: 'https://www.googleapis.com/oauth2/v1/certs',
  client_x509_cert_url: 'https://www.googleapis.com/robot/v1/metadata/x509/dummy@test-project.iam.gserviceaccount.com'
});

// Log environment variables
console.log('=== Development Server ===');
console.log('Setting environment variables:');
console.log('NODE_ENV:', process.env.NODE_ENV);
console.log('SKIP_AUTH:', process.env.SKIP_AUTH);
console.log('PORT:', process.env.PORT);
console.log('=========================');

// Now require the server module
require('./server.js'); 