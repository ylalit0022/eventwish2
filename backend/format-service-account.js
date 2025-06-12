/**
 * Format Firebase Service Account for Render
 * 
 * This script reads the firebase-service-account.json file and formats it
 * to be used as an environment variable in Render.
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
  
  // Format the JSON for environment variable
  // 1. Remove all newlines from the private_key
  serviceAccount.private_key = serviceAccount.private_key.replace(/\\n/g, "\\\\n");
  
  // 2. Stringify the JSON with proper escaping
  const formattedJson = JSON.stringify(serviceAccount);
  
  console.log('==== FORMATTED SERVICE ACCOUNT FOR RENDER ====');
  console.log(formattedJson);
  console.log('=============================================');
  console.log('\nCopy the above JSON string and set it as the FIREBASE_SERVICE_ACCOUNT environment variable in Render');
  console.log('Also set FIREBASE_PROJECT_ID to:', serviceAccount.project_id);
  
} catch (error) {
  if (error.code === 'ENOENT') {
    console.error(`Error: Service account file not found at ${serviceAccountPath}`);
  } else {
    console.error('Error:', error.message);
  }
  process.exit(1);
} 