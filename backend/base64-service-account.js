/**
 * Encode Firebase Service Account as Base64
 * 
 * This script reads the firebase-service-account.json file and encodes it as Base64
 * for use as an environment variable in Render.
 */

const fs = require('fs');
const path = require('path');

// Path to the service account file
const serviceAccountPath = path.join(__dirname, 'firebase-service-account.json');

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
  
  console.log('==== BASE64 ENCODED SERVICE ACCOUNT FOR RENDER ====');
  console.log(base64ServiceAccount);
  console.log('==================================================');
  console.log('\nCopy the above Base64 string and set it as the FIREBASE_SERVICE_ACCOUNT_BASE64 environment variable in Render');
  console.log('Also set FIREBASE_PROJECT_ID to:', serviceAccount.project_id);
  
} catch (error) {
  if (error.code === 'ENOENT') {
    console.error(`Error: Service account file not found at ${serviceAccountPath}`);
  } else {
    console.error('Error:', error.message);
  }
  process.exit(1);
} 