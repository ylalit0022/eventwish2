/**
 * Environment Variable Loader
 * 
 * This module loads environment variables from .env file
 * and provides fallbacks for development environments
 */

const dotenv = require('dotenv');
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');

// Load environment variables from .env file
dotenv.config();

// Debug environment mode
console.log('DEBUG: NODE_ENV value:', process.env.NODE_ENV);
console.log('DEBUG: Is development?', process.env.NODE_ENV !== 'production');
console.log('DEBUG: SKIP_AUTH value:', process.env.SKIP_AUTH);

/**
 * Generates a temporary secret for development
 * @returns {string} - Random hex string
 */
function generateTempSecret() {
  return crypto.randomBytes(32).toString('hex');
}

/**
 * Ensures critical environment variables are set
 * If not in production, generates temporary values with warnings
 */
function ensureCriticalEnvVars() {
  const isDevelopment = process.env.NODE_ENV !== 'production';
  const isProduction = process.env.NODE_ENV === 'production';
  
  // Critical environment variables that must be set
  const criticalVars = [
    'JWT_SECRET',
    'API_KEY',
    'INTERNAL_API_KEY'
  ];
  
  // Check and set fallbacks for critical variables
  criticalVars.forEach(varName => {
    if (!process.env[varName]) {
      if (isDevelopment) {
        // Generate temporary value for development
        const tempValue = generateTempSecret();
        process.env[varName] = tempValue;
        
        console.warn(`⚠️  WARNING: ${varName} not set in environment variables.`);
        console.warn(`⚠️  Using temporary value for development: ${tempValue.substring(0, 10)}...`);
        console.warn(`⚠️  Run 'node scripts/generate-secrets.js' to generate proper secrets.`);
      } else {
        // In production, throw an error if critical variables are missing
        throw new Error(`Critical environment variable ${varName} is not set. Cannot start in production mode.`);
      }
    }
  });
  
  // Handle Firebase configuration
  // First check for Base64 encoded service account
  if (process.env.FIREBASE_SERVICE_ACCOUNT_BASE64) {
    try {
      console.log('DEBUG: Found FIREBASE_SERVICE_ACCOUNT_BASE64');
      console.log('DEBUG: Base64 length:', process.env.FIREBASE_SERVICE_ACCOUNT_BASE64.length);
      console.log('DEBUG: First 50 chars:', process.env.FIREBASE_SERVICE_ACCOUNT_BASE64.substring(0, 50));
      console.log('DEBUG: Last 50 chars:', process.env.FIREBASE_SERVICE_ACCOUNT_BASE64.substring(process.env.FIREBASE_SERVICE_ACCOUNT_BASE64.length - 50));
      
      // Check for common issues with Base64 strings
      if (process.env.FIREBASE_SERVICE_ACCOUNT_BASE64.includes(' ')) {
        console.warn('WARNING: Base64 string contains spaces, which may cause decoding issues');
      }
      if (process.env.FIREBASE_SERVICE_ACCOUNT_BASE64.includes('\n')) {
        console.warn('WARNING: Base64 string contains newlines, which may cause decoding issues');
      }
      
      // Decode Base64 to JSON string
      let decodedServiceAccount;
      try {
        decodedServiceAccount = Buffer.from(process.env.FIREBASE_SERVICE_ACCOUNT_BASE64, 'base64').toString('utf8');
        console.log('DEBUG: Decoded Base64 successfully');
        console.log('DEBUG: Decoded JSON length:', decodedServiceAccount.length);
        console.log('DEBUG: First 50 chars of decoded JSON:', decodedServiceAccount.substring(0, 50));
      } catch (decodeError) {
        console.error('ERROR: Failed to decode Base64:', decodeError.message);
        throw new Error(`Failed to decode Base64: ${decodeError.message}`);
      }
      
      // Parse the JSON to validate
      let serviceAccount;
      try {
        serviceAccount = JSON.parse(decodedServiceAccount);
        console.log('DEBUG: Parsed JSON successfully');
      } catch (parseError) {
        console.error('ERROR: Failed to parse JSON:', parseError.message);
        throw new Error(`Failed to parse JSON: ${parseError.message}`);
      }
      
      // Validate required fields
      if (!serviceAccount.project_id || !serviceAccount.private_key || !serviceAccount.client_email) {
        console.error('ERROR: Missing required fields in service account');
        console.log('DEBUG: Fields present:', Object.keys(serviceAccount).join(', '));
        throw new Error('Missing required fields in decoded service account');
      }
      
      // Set the regular service account variable
      process.env.FIREBASE_SERVICE_ACCOUNT = decodedServiceAccount;
      
      console.log('✅ FIREBASE_SERVICE_ACCOUNT_BASE64 decoded and validated successfully');
      
      // Set project ID if not already set
      if (!process.env.FIREBASE_PROJECT_ID) {
        process.env.FIREBASE_PROJECT_ID = serviceAccount.project_id;
        console.log(`✅ FIREBASE_PROJECT_ID set from service account: ${serviceAccount.project_id}`);
      }
    } catch (error) {
      console.error('ERROR: Failed to process FIREBASE_SERVICE_ACCOUNT_BASE64:', error.message);
      
      // If FORCE_SKIP_AUTH is set, continue without Firebase authentication
      if (process.env.FORCE_SKIP_AUTH === 'true') {
        console.warn('⚠️  WARNING: FORCE_SKIP_AUTH=true is set. Continuing without Firebase authentication.');
        console.warn('⚠️  This is NOT RECOMMENDED for production environments!');
        process.env.SKIP_AUTH = 'true';
        return;
      }
      
      throw new Error(`Invalid FIREBASE_SERVICE_ACCOUNT_BASE64 format: ${error.message}`);
    }
  }
  // Then check for regular service account JSON
  else if (!process.env.FIREBASE_SERVICE_ACCOUNT) {
    if (isDevelopment && process.env.SKIP_AUTH === 'true') {
      console.warn('⚠️  WARNING: FIREBASE_SERVICE_ACCOUNT not set.');
      console.warn('⚠️  Firebase authentication will be disabled.');
      console.warn('⚠️  Running with SKIP_AUTH=true for development only!');
    } else if (isDevelopment) {
      console.warn('⚠️  WARNING: FIREBASE_SERVICE_ACCOUNT not set.');
      console.warn('⚠️  Firebase authentication may not work correctly.');
      console.warn('⚠️  Set SKIP_AUTH=true to disable authentication for development.');
    } else if (process.env.FORCE_SKIP_AUTH === 'true') {
      console.warn('⚠️  WARNING: FORCE_SKIP_AUTH=true is set. Continuing without Firebase authentication.');
      console.warn('⚠️  This is NOT RECOMMENDED for production environments!');
      process.env.SKIP_AUTH = 'true';
    } else {
      // In production, we must have Firebase credentials
      throw new Error('FIREBASE_SERVICE_ACCOUNT environment variable is required in production. You can also provide FIREBASE_SERVICE_ACCOUNT_BASE64.');
    }
  } else if (isProduction) {
    try {
      // Validate the service account JSON in production
      console.log('DEBUG: Validating FIREBASE_SERVICE_ACCOUNT');
      console.log('DEBUG: JSON length:', process.env.FIREBASE_SERVICE_ACCOUNT.length);
      
      const serviceAccount = JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT);
      if (!serviceAccount.project_id || !serviceAccount.private_key || !serviceAccount.client_email) {
        console.error('ERROR: Missing required fields in service account');
        console.log('DEBUG: Fields present:', Object.keys(serviceAccount).join(', '));
        throw new Error('Invalid service account format - missing required fields');
      }
      console.log('✅ FIREBASE_SERVICE_ACCOUNT validated successfully');
    } catch (error) {
      console.error('Error parsing FIREBASE_SERVICE_ACCOUNT:', error.message);
      
      // If FORCE_SKIP_AUTH is set, continue without Firebase authentication
      if (process.env.FORCE_SKIP_AUTH === 'true') {
        console.warn('⚠️  WARNING: FORCE_SKIP_AUTH=true is set. Continuing without Firebase authentication.');
        console.warn('⚠️  This is NOT RECOMMENDED for production environments!');
        process.env.SKIP_AUTH = 'true';
        return;
      }
      
      throw new Error(`Invalid FIREBASE_SERVICE_ACCOUNT format: ${error.message}`);
    }
  }
  
  // Handle Firebase Project ID
  if (!process.env.FIREBASE_PROJECT_ID) {
    if (isDevelopment) {
      // Set a default project ID for development
      process.env.FIREBASE_PROJECT_ID = 'eventwish-app';
      console.warn('⚠️  WARNING: FIREBASE_PROJECT_ID not set in environment variables.');
      console.warn(`⚠️  Using default project ID for development: ${process.env.FIREBASE_PROJECT_ID}`);
    } else if (process.env.FORCE_SKIP_AUTH === 'true') {
      // Set a default project ID when FORCE_SKIP_AUTH is true
      process.env.FIREBASE_PROJECT_ID = 'neweventwish';
      console.warn('⚠️  WARNING: FIREBASE_PROJECT_ID not set in environment variables.');
      console.warn(`⚠️  Using default project ID with FORCE_SKIP_AUTH: ${process.env.FIREBASE_PROJECT_ID}`);
    } else {
      // In production, we must have a project ID
      throw new Error('FIREBASE_PROJECT_ID environment variable is required in production.');
    }
  }
  
  // Prevent SKIP_AUTH in production unless FORCE_SKIP_AUTH is set
  if (process.env.SKIP_AUTH === 'true' && isProduction && process.env.FORCE_SKIP_AUTH !== 'true') {
    throw new Error('SECURITY ERROR: Cannot set SKIP_AUTH=true in production environment');
  }
  
  // Log environment mode
  console.log(`🌍 Environment: ${process.env.NODE_ENV || 'development'}`);
  
  // Production security checks
  if (isProduction) {
    // Check API key strength
    if (process.env.API_KEY && process.env.API_KEY.length < 32) {
      console.warn('⚠️  WARNING: API_KEY is less than 32 characters. Consider using a stronger key.');
    }
    
    // Check JWT secret strength
    if (process.env.JWT_SECRET && process.env.JWT_SECRET.length < 32) {
      console.warn('⚠️  WARNING: JWT_SECRET is less than 32 characters. Consider using a stronger secret.');
    }
    
    console.log('✅ Production environment checks completed');
  }
}

// Run the check
ensureCriticalEnvVars();

module.exports = {
  ensureCriticalEnvVars
}; 