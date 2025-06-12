/**
 * Generate Minimal Firebase Service Account JSON
 * 
 * This script creates a minimal service account JSON with only the required fields.
 */

const fs = require('fs');
const path = require('path');

// Path to the service account file
const serviceAccountPath = path.join(__dirname, 'firebase-service-account.json');

try {
  // Read the service account file
  const serviceAccountContent = fs.readFileSync(serviceAccountPath, 'utf8');
  
  // Parse the JSON
  const serviceAccount = JSON.parse(serviceAccountContent);
  
  // Create a minimal version with only required fields
  const minimalServiceAccount = {
    type: serviceAccount.type,
    project_id: serviceAccount.project_id,
    private_key_id: serviceAccount.private_key_id,
    private_key: serviceAccount.private_key,
    client_email: serviceAccount.client_email,
    client_id: serviceAccount.client_id,
    auth_uri: serviceAccount.auth_uri,
    token_uri: serviceAccount.token_uri,
    auth_provider_x509_cert_url: serviceAccount.auth_provider_x509_cert_url,
    client_x509_cert_url: serviceAccount.client_x509_cert_url
  };
  
  // Convert to JSON string
  const minimalJson = JSON.stringify(minimalServiceAccount);
  
  // Convert to Base64
  const base64ServiceAccount = Buffer.from(minimalJson).toString('base64');
  
  console.log('==== MINIMAL SERVICE ACCOUNT JSON ====');
  console.log(minimalJson);
  console.log('');
  console.log('==== BASE64 ENCODED MINIMAL SERVICE ACCOUNT ====');
  console.log(base64ServiceAccount);
  console.log('');
  console.log('==== RENDER ENVIRONMENT VARIABLES ====');
  console.log('');
  console.log('FIREBASE_PROJECT_ID=' + serviceAccount.project_id);
  console.log('FIREBASE_SERVICE_ACCOUNT_BASE64=' + base64ServiceAccount);
  
} catch (error) {
  if (error.code === 'ENOENT') {
    console.error(`Error: Service account file not found at ${serviceAccountPath}`);
  } else {
    console.error('Error:', error.message);
  }
  process.exit(1);
} 