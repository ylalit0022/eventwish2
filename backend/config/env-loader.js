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
console.log('DEBUG: FORCE_SKIP_AUTH value:', process.env.FORCE_SKIP_AUTH);

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
  
  // EMERGENCY OVERRIDE FOR RENDER DEPLOYMENT
  // This is a temporary solution to get the service running
  if (isProduction) {
    console.log('EMERGENCY OVERRIDE: Setting SKIP_AUTH=true for Render deployment');
    process.env.SKIP_AUTH = 'true';
    
    // Set project ID if not already set
    if (!process.env.FIREBASE_PROJECT_ID) {
      process.env.FIREBASE_PROJECT_ID = 'neweventwish';
      console.log('EMERGENCY OVERRIDE: Setting FIREBASE_PROJECT_ID=neweventwish');
    }
    
    console.warn('⚠️  WARNING: Running with SKIP_AUTH=true in production.');
    console.warn('⚠️  This is a temporary solution and should be fixed properly.');
  }
  
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
      } else {
        // In production, throw an error if critical variables are missing
        throw new Error(`Critical environment variable ${varName} is not set. Cannot start in production mode.`);
      }
    }
  });
  
  // Handle Firebase configuration
  if (!process.env.FIREBASE_SERVICE_ACCOUNT && !process.env.SKIP_AUTH) {
    if (isDevelopment) {
      console.warn('⚠️  WARNING: FIREBASE_SERVICE_ACCOUNT not set.');
      console.warn('⚠️  Firebase authentication may not work correctly.');
      console.warn('⚠️  Set SKIP_AUTH=true to disable authentication for development.');
    } else {
      // In production, we must have Firebase credentials unless SKIP_AUTH is true
      throw new Error('FIREBASE_SERVICE_ACCOUNT environment variable is required in production unless SKIP_AUTH=true.');
    }
  } else if (process.env.FIREBASE_SERVICE_ACCOUNT && isProduction && !process.env.SKIP_AUTH) {
    try {
      // Validate the service account JSON in production
      const serviceAccount = JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT);
      if (!serviceAccount.project_id || !serviceAccount.private_key || !serviceAccount.client_email) {
        throw new Error('Invalid service account format - missing required fields');
      }
      console.log('✅ FIREBASE_SERVICE_ACCOUNT validated successfully');
    } catch (error) {
      console.error('Error parsing FIREBASE_SERVICE_ACCOUNT:', error.message);
      
      // If SKIP_AUTH is now true (from emergency override), continue
      if (process.env.SKIP_AUTH === 'true') {
        console.warn('⚠️  WARNING: Continuing with SKIP_AUTH=true despite invalid service account.');
      } else {
        throw new Error(`Invalid FIREBASE_SERVICE_ACCOUNT format: ${error.message}`);
      }
    }
  }
  
  // Handle Firebase Project ID
  if (!process.env.FIREBASE_PROJECT_ID) {
    if (isDevelopment) {
      // Set a default project ID for development
      process.env.FIREBASE_PROJECT_ID = 'eventwish-app';
      console.warn('⚠️  WARNING: FIREBASE_PROJECT_ID not set in environment variables.');
      console.warn(`⚠️  Using default project ID for development: ${process.env.FIREBASE_PROJECT_ID}`);
    } else if (process.env.SKIP_AUTH === 'true') {
      // Set a default project ID when SKIP_AUTH is true
      process.env.FIREBASE_PROJECT_ID = 'neweventwish';
      console.warn('⚠️  WARNING: FIREBASE_PROJECT_ID not set in environment variables.');
      console.warn(`⚠️  Using default project ID with SKIP_AUTH: ${process.env.FIREBASE_PROJECT_ID}`);
    } else {
      // In production, we must have a project ID
      throw new Error('FIREBASE_PROJECT_ID environment variable is required in production.');
    }
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