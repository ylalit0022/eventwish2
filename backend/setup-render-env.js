/**
 * Script to help set up Firebase Service Account for Render deployment
 */

const fs = require('fs');
const path = require('path');

console.log('🔧 Firebase Service Account Setup for Render\n');

// Check if firebase-service-account.json exists
const serviceAccountPath = path.join(__dirname, 'firebase-service-account.json');

if (!fs.existsSync(serviceAccountPath)) {
  console.log('❌ firebase-service-account.json not found');
  console.log('📋 To set up Firebase authentication on Render:');
  console.log('');
  console.log('1. Download your Firebase service account JSON from:');
  console.log('   https://console.firebase.google.com/project/neweventwish/settings/serviceaccounts/adminsdk');
  console.log('');
  console.log('2. Save it as backend/firebase-service-account.json');
  console.log('');
  console.log('3. Run this script again to get the environment variable value');
  console.log('');
  console.log('4. Set FIREBASE_SERVICE_ACCOUNT in Render dashboard with the generated value');
  process.exit(1);
}

try {
  // Read and validate the service account file
  const serviceAccountContent = fs.readFileSync(serviceAccountPath, 'utf8');
  const serviceAccount = JSON.parse(serviceAccountContent);
  
  // Validate required fields
  const requiredFields = ['type', 'project_id', 'private_key_id', 'private_key', 'client_email', 'client_id', 'auth_uri', 'token_uri'];
  const missingFields = requiredFields.filter(field => !serviceAccount[field]);
  
  if (missingFields.length > 0) {
    console.log('❌ Invalid service account file - missing fields:', missingFields.join(', '));
    process.exit(1);
  }
  
  // Create the environment variable value (minified JSON)
  const envValue = JSON.stringify(serviceAccount);
  
  console.log('✅ Firebase service account validated successfully');
  console.log('📊 Project ID:', serviceAccount.project_id);
  console.log('📧 Client Email:', serviceAccount.client_email);
  console.log('');
  console.log('🔑 Environment Variable for Render:');
  console.log('');
  console.log('Variable Name: FIREBASE_SERVICE_ACCOUNT');
  console.log('Variable Value:');
  console.log('─'.repeat(80));
  console.log(envValue);
  console.log('─'.repeat(80));
  console.log('');
  console.log('📋 Steps to set up on Render:');
  console.log('1. Go to your Render dashboard');
  console.log('2. Select your EventWish backend service');
  console.log('3. Go to Environment tab');
  console.log('4. Add new environment variable:');
  console.log('   Name: FIREBASE_SERVICE_ACCOUNT');
  console.log('   Value: (copy the value above)');
  console.log('5. Deploy the service');
  console.log('');
  console.log('⚠️  Note: Keep this value secure and never commit it to version control!');
  
} catch (error) {
  console.log('❌ Error reading service account file:', error.message);
  console.log('');
  console.log('Make sure the file is valid JSON format.');
  process.exit(1);
} 