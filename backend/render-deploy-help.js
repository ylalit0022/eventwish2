/**
 * Render Deployment Helper
 * 
 * This script helps prepare environment variables for Render deployment.
 * It generates the necessary environment variables for the Render dashboard.
 */

const fs = require('fs');
const path = require('path');
const crypto = require('crypto');

// Generate secure random values for secrets
function generateSecret(length = 32) {
  return crypto.randomBytes(length).toString('hex');
}

// Path to the service account file
const serviceAccountPath = path.join(__dirname, 'firebase-service-account.json');

// Generate environment variables
try {
  // Read the service account file
  const serviceAccountContent = fs.readFileSync(serviceAccountPath, 'utf8');
  
  // Parse the JSON to validate it
  const serviceAccount = JSON.parse(serviceAccountContent);
  
  // Ensure required fields are present
  if (!serviceAccount.project_id || !serviceAccount.private_key || !serviceAccount.client_email) {
    console.error('Error: Service account file is missing required fields');
    process.exit(1);
  }
  
  // Convert to Base64
  const base64ServiceAccount = Buffer.from(serviceAccountContent).toString('base64');
  
  // Generate secure values for other environment variables
  const jwtSecret = generateSecret();
  const apiKey = generateSecret();
  const internalApiKey = generateSecret();
  
  console.log('==== RENDER ENVIRONMENT VARIABLES ====');
  console.log('');
  console.log('# Firebase Configuration');
  console.log(`FIREBASE_PROJECT_ID=${serviceAccount.project_id}`);
  console.log(`FIREBASE_SERVICE_ACCOUNT_BASE64=${base64ServiceAccount}`);
  console.log('');
  console.log('# API Security');
  console.log(`JWT_SECRET=${jwtSecret}`);
  console.log(`API_KEY=${apiKey}`);
  console.log(`INTERNAL_API_KEY=${internalApiKey}`);
  console.log('');
  console.log('# Server Configuration');
  console.log('NODE_ENV=production');
  console.log('PORT=3007');
  console.log('');
  console.log('==== END OF ENVIRONMENT VARIABLES ====');
  console.log('');
  console.log('Copy these values to your Render environment variables.');
  console.log('Make sure to remove any SKIP_AUTH variable if it exists.');
  
} catch (error) {
  if (error.code === 'ENOENT') {
    console.error(`Error: Service account file not found at ${serviceAccountPath}`);
  } else {
    console.error('Error:', error.message);
  }
  process.exit(1);
} 