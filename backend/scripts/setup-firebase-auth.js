#!/usr/bin/env node

/**
 * Firebase Authentication Setup Script for Production
 * 
 * This script helps set up the necessary environment variables for Firebase authentication
 * in a production environment. It performs validation and security checks to ensure
 * proper configuration for secure deployment.
 */

const fs = require('fs');
const path = require('path');
const readline = require('readline');
const { execSync } = require('child_process');
const crypto = require('crypto');

// Create readline interface for user input
const rl = readline.createInterface({
  input: process.stdin,
  output: process.stdout
});

// Function to ask a question and get a response
const question = (query) => new Promise((resolve) => rl.question(query, resolve));

// Function to validate a Firebase service account
function validateServiceAccount(serviceAccount) {
  try {
    const parsed = JSON.parse(serviceAccount);
    
    // Check for required fields
    const requiredFields = ['type', 'project_id', 'private_key_id', 'private_key', 'client_email', 'client_id'];
    const missingFields = requiredFields.filter(field => !parsed[field]);
    
    if (missingFields.length > 0) {
      return {
        valid: false,
        error: `Missing required fields: ${missingFields.join(', ')}`
      };
    }
    
    // Check for correct type
    if (parsed.type !== 'service_account') {
      return {
        valid: false,
        error: `Invalid type: ${parsed.type}. Expected 'service_account'`
      };
    }
    
    // Check private key format
    if (!parsed.private_key.startsWith('-----BEGIN PRIVATE KEY-----')) {
      return {
        valid: false,
        error: 'Invalid private key format'
      };
    }
    
    // Check client email format
    if (!parsed.client_email.endsWith('.gserviceaccount.com')) {
      return {
        valid: false,
        error: `Invalid client email format: ${parsed.client_email}`
      };
    }
    
    return {
      valid: true,
      projectId: parsed.project_id
    };
  } catch (error) {
    return {
      valid: false,
      error: `Invalid JSON format: ${error.message}`
    };
  }
}

// Function to check if a string is a valid Firebase project ID
function validateProjectId(projectId) {
  // Firebase project IDs must be between 6-30 characters
  // and can only contain lowercase letters, numbers and hyphens
  const regex = /^[a-z0-9-]{6,30}$/;
  return regex.test(projectId);
}

// Function to generate a secure random string
function generateSecureString(length = 32) {
  return crypto.randomBytes(length).toString('hex');
}

// Main function to run the setup
async function setup() {
  console.log('\n🔥 Firebase Authentication Production Setup 🔥\n');
  
  // Check for existing environment variables
  const envVars = {
    FIREBASE_PROJECT_ID: process.env.FIREBASE_PROJECT_ID || '',
    FIREBASE_SERVICE_ACCOUNT: process.env.FIREBASE_SERVICE_ACCOUNT ? '[SET]' : '',
    NODE_ENV: process.env.NODE_ENV || 'development'
  };
  
  console.log('Current Environment Variables:');
  console.log(`- FIREBASE_PROJECT_ID: ${envVars.FIREBASE_PROJECT_ID || '[NOT SET]'}`);
  console.log(`- FIREBASE_SERVICE_ACCOUNT: ${envVars.FIREBASE_SERVICE_ACCOUNT || '[NOT SET]'}`);
  console.log(`- NODE_ENV: ${envVars.NODE_ENV}`);
  console.log('');
  
  // Confirm production setup
  const confirmProduction = await question('This script will set up Firebase authentication for PRODUCTION. Continue? (y/n): ');
  
  if (confirmProduction.toLowerCase() !== 'y') {
    console.log('Setup cancelled.');
    rl.close();
    return;
  }
  
  // Set NODE_ENV to production
  envVars.NODE_ENV = 'production';
  console.log('✅ NODE_ENV set to: production');
  
  // Firebase Service Account setup (required for production)
  console.log('\n📝 Firebase Service Account Setup (REQUIRED for production)\n');
  
  let serviceAccountValid = false;
  let serviceAccountJson = '';
  let projectIdFromServiceAccount = '';
  
  while (!serviceAccountValid) {
    const filePath = await question('Enter the path to your service account JSON file: ');
    
    try {
      if (fs.existsSync(filePath)) {
        serviceAccountJson = fs.readFileSync(filePath, 'utf8');
        
        // Validate the service account
        const validation = validateServiceAccount(serviceAccountJson);
        
        if (validation.valid) {
          serviceAccountValid = true;
          projectIdFromServiceAccount = validation.projectId;
          console.log(`✅ Service account validated successfully`);
          console.log(`📝 Project ID from service account: ${projectIdFromServiceAccount}`);
        } else {
          console.log(`❌ Invalid service account: ${validation.error}`);
          const retry = await question('Would you like to try another file? (y/n): ');
          if (retry.toLowerCase() !== 'y') {
            console.log('❌ A valid service account is required for production. Exiting setup.');
            rl.close();
            return;
          }
        }
      } else {
        console.log('❌ Error: File not found');
        const retry = await question('Would you like to try another file? (y/n): ');
        if (retry.toLowerCase() !== 'y') {
          console.log('❌ A valid service account is required for production. Exiting setup.');
          rl.close();
          return;
        }
      }
    } catch (e) {
      console.log('❌ Error reading file:');
      console.error(e.message);
      const retry = await question('Would you like to try another file? (y/n): ');
      if (retry.toLowerCase() !== 'y') {
        console.log('❌ A valid service account is required for production. Exiting setup.');
        rl.close();
        return;
      }
    }
  }
  
  // Set the service account
  envVars.FIREBASE_SERVICE_ACCOUNT = serviceAccountJson;
  
  // Firebase Project ID setup
  console.log('\n📝 Firebase Project ID Setup\n');
  
  // If we have a project ID from the service account, suggest using it
  if (projectIdFromServiceAccount) {
    const useProjectId = await question(`Use project ID "${projectIdFromServiceAccount}" from service account? (y/n): `);
    
    if (useProjectId.toLowerCase() === 'y') {
      envVars.FIREBASE_PROJECT_ID = projectIdFromServiceAccount;
      console.log(`✅ FIREBASE_PROJECT_ID set to: ${projectIdFromServiceAccount}`);
    } else {
      // Ask for a custom project ID
      let projectIdValid = false;
      
      while (!projectIdValid) {
        const projectId = await question('Enter your Firebase Project ID: ');
        
        if (!projectId) {
          console.log('❌ Project ID cannot be empty');
        } else if (!validateProjectId(projectId)) {
          console.log('❌ Invalid project ID format. Must be 6-30 characters and contain only lowercase letters, numbers, and hyphens');
        } else {
          envVars.FIREBASE_PROJECT_ID = projectId;
          projectIdValid = true;
          console.log(`✅ FIREBASE_PROJECT_ID set to: ${projectId}`);
          
          // Warn if it doesn't match the service account
          if (projectId !== projectIdFromServiceAccount) {
            console.log(`⚠️  WARNING: The project ID you entered (${projectId}) does not match the one in your service account (${projectIdFromServiceAccount})`);
            console.log('⚠️  This may cause authentication issues if they are not related projects');
            
            const confirm = await question('Are you sure you want to use a different project ID? (y/n): ');
            if (confirm.toLowerCase() !== 'y') {
              envVars.FIREBASE_PROJECT_ID = projectIdFromServiceAccount;
              console.log(`✅ FIREBASE_PROJECT_ID reset to: ${projectIdFromServiceAccount}`);
            }
          }
        }
      }
    }
  } else {
    // No project ID from service account, ask for one
    let projectIdValid = false;
    
    while (!projectIdValid) {
      const projectId = await question('Enter your Firebase Project ID: ');
      
      if (!projectId) {
        console.log('❌ Project ID cannot be empty');
      } else if (!validateProjectId(projectId)) {
        console.log('❌ Invalid project ID format. Must be 6-30 characters and contain only lowercase letters, numbers, and hyphens');
      } else {
        envVars.FIREBASE_PROJECT_ID = projectId;
        projectIdValid = true;
        console.log(`✅ FIREBASE_PROJECT_ID set to: ${projectId}`);
      }
    }
  }
  
  // Security checks
  console.log('\n🔒 Performing Security Checks\n');
  
  // Check if we have all required variables for production
  const missingVars = [];
  if (!envVars.FIREBASE_SERVICE_ACCOUNT) missingVars.push('FIREBASE_SERVICE_ACCOUNT');
  if (!envVars.FIREBASE_PROJECT_ID) missingVars.push('FIREBASE_PROJECT_ID');
  
  if (missingVars.length > 0) {
    console.log(`❌ Missing required environment variables for production: ${missingVars.join(', ')}`);
    console.log('❌ Production setup cannot continue without these variables');
    rl.close();
    return;
  }
  
  console.log('✅ All required environment variables are set');
  
  // Generate commands to set environment variables
  console.log('\n📋 Commands to set these environment variables in production:\n');
  
  // For Linux/macOS
  console.log('# Linux/macOS:');
  console.log(`export NODE_ENV="production"`);
  console.log(`export FIREBASE_PROJECT_ID="${envVars.FIREBASE_PROJECT_ID}"`);
  console.log(`export FIREBASE_SERVICE_ACCOUNT='${envVars.FIREBASE_SERVICE_ACCOUNT}'`);
  
  console.log('\n# Windows PowerShell:');
  console.log(`$env:NODE_ENV = "production"`);
  console.log(`$env:FIREBASE_PROJECT_ID = "${envVars.FIREBASE_PROJECT_ID}"`);
  console.log(`$env:FIREBASE_SERVICE_ACCOUNT = '${envVars.FIREBASE_SERVICE_ACCOUNT}'`);
  
  // Ask if user wants to create a local .env file
  console.log('\n⚠️  WARNING: Storing credentials in a .env file is not recommended for production environments');
  console.log('⚠️  Instead, use your hosting provider\'s environment variable or secrets management system');
  
  const createEnvFile = await question('\nDo you want to save these to a local .env file anyway? (y/n): ');
  
  if (createEnvFile.toLowerCase() === 'y') {
    const envPath = path.join(__dirname, '..', '.env');
    
    // Check if file exists
    let envContent = '';
    if (fs.existsSync(envPath)) {
      envContent = fs.readFileSync(envPath, 'utf8');
      console.log(`Found existing .env file at ${envPath}`);
    }
    
    // Update NODE_ENV
    if (envContent.includes('NODE_ENV=')) {
      envContent = envContent.replace(/NODE_ENV=.*(\r?\n|$)/g, `NODE_ENV=production$1`);
    } else {
      envContent += `\nNODE_ENV=production`;
    }
    
    // Update FIREBASE_PROJECT_ID
    if (envContent.includes('FIREBASE_PROJECT_ID=')) {
      envContent = envContent.replace(/FIREBASE_PROJECT_ID=.*(\r?\n|$)/g, `FIREBASE_PROJECT_ID=${envVars.FIREBASE_PROJECT_ID}$1`);
    } else {
      envContent += `\nFIREBASE_PROJECT_ID=${envVars.FIREBASE_PROJECT_ID}`;
    }
    
    // Update FIREBASE_SERVICE_ACCOUNT
    // For the .env file, we need to escape newlines and quotes
    const escapedJson = envVars.FIREBASE_SERVICE_ACCOUNT
      .replace(/\\/g, '\\\\')
      .replace(/"/g, '\\"')
      .replace(/\n/g, '\\n');
    
    if (envContent.includes('FIREBASE_SERVICE_ACCOUNT=')) {
      envContent = envContent.replace(/FIREBASE_SERVICE_ACCOUNT=.*(\r?\n|$)/g, `FIREBASE_SERVICE_ACCOUNT="${escapedJson}"$1`);
    } else {
      envContent += `\nFIREBASE_SERVICE_ACCOUNT="${escapedJson}"`;
    }
    
    // Remove SKIP_AUTH if present (not allowed in production)
    if (envContent.includes('SKIP_AUTH=')) {
      envContent = envContent.replace(/SKIP_AUTH=.*(\r?\n|$)/g, '');
    }
    
    // Write to file
    try {
      fs.writeFileSync(envPath, envContent.trim() + '\n');
      console.log(`✅ Environment variables saved to ${envPath}`);
      console.log('⚠️  WARNING: This file contains sensitive credentials. Do not commit it to version control!');
      
      // Set restrictive permissions on the file
      try {
        fs.chmodSync(envPath, 0o600); // Read/write for owner only
        console.log('✅ File permissions set to restrict access (owner read/write only)');
      } catch (permError) {
        console.log('⚠️  Could not set restrictive file permissions. Please restrict access to this file manually.');
      }
    } catch (e) {
      console.log('❌ Error writing to .env file:');
      console.error(e.message);
    }
  }
  
  console.log('\n📝 Production Setup Summary:');
  console.log(`- NODE_ENV: production`);
  console.log(`- FIREBASE_PROJECT_ID: ${envVars.FIREBASE_PROJECT_ID}`);
  console.log(`- FIREBASE_SERVICE_ACCOUNT: [SET]`);
  
  console.log('\n🔥 Firebase Authentication Production Setup Complete! 🔥');
  console.log('\n🔒 Security Recommendations:');
  console.log('1. Store credentials securely using your hosting provider\'s secrets management');
  console.log('2. Rotate service account keys regularly (every 90 days recommended)');
  console.log('3. Set up monitoring for authentication failures');
  console.log('4. Keep Firebase Admin SDK updated to the latest version');
  console.log('5. Review Firebase Authentication logs periodically');
  
  console.log('\nNow restart your server for the changes to take effect.');
  
  // Close the readline interface
  rl.close();
}

// Run the setup
setup().catch(error => {
  console.error('Error during setup:', error);
  rl.close();
}); 