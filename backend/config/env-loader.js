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
  
  // Set project ID if not already set
  if (!process.env.FIREBASE_PROJECT_ID) {
    if (isDevelopment) {
      // Set a default project ID for development
      process.env.FIREBASE_PROJECT_ID = 'eventwish-app';
      console.warn('⚠️  WARNING: FIREBASE_PROJECT_ID not set in environment variables.');
      console.warn(`⚠️  Using default project ID for development: ${process.env.FIREBASE_PROJECT_ID}`);
    } else {
      // In production, use the default project ID
      process.env.FIREBASE_PROJECT_ID = 'neweventwish';
      console.warn('⚠️  WARNING: FIREBASE_PROJECT_ID not set in environment variables.');
      console.warn(`⚠️  Using default project ID for production: ${process.env.FIREBASE_PROJECT_ID}`);
    }
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
      // In production, warn but allow the server to start
      console.warn('⚠️  WARNING: FIREBASE_SERVICE_ACCOUNT not set in production.');
      console.warn('⚠️  Firebase authentication will use application default credentials.');
      console.warn('⚠️  Ensure the service is running with proper Google Cloud credentials.');
    }
  } else if (process.env.FIREBASE_SERVICE_ACCOUNT && !process.env.SKIP_AUTH) {
    try {
      // Validate the service account JSON
      const serviceAccount = JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT);
      if (!serviceAccount.project_id || !serviceAccount.private_key || !serviceAccount.client_email) {
        throw new Error('Invalid service account format - missing required fields');
      }
      console.log('✅ FIREBASE_SERVICE_ACCOUNT validated successfully');
    } catch (error) {
      console.error('Error parsing FIREBASE_SERVICE_ACCOUNT:', error.message);
      
      if (isDevelopment) {
        console.warn('⚠️  WARNING: Continuing in development mode despite invalid service account.');
        console.warn('⚠️  Set SKIP_AUTH=true to disable authentication for development.');
      } else {
        console.warn('⚠️  WARNING: Invalid FIREBASE_SERVICE_ACCOUNT in production.');
        console.warn('⚠️  Firebase authentication will use application default credentials.');
      }
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