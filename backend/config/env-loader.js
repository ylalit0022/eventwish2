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
  if (!process.env.FIREBASE_SERVICE_ACCOUNT) {
    if (isDevelopment && process.env.SKIP_AUTH === 'true') {
      console.warn('⚠️  WARNING: FIREBASE_SERVICE_ACCOUNT not set.');
      console.warn('⚠️  Firebase authentication will be disabled.');
      console.warn('⚠️  Running with SKIP_AUTH=true for development only!');
    } else if (isDevelopment) {
      console.warn('⚠️  WARNING: FIREBASE_SERVICE_ACCOUNT not set.');
      console.warn('⚠️  Firebase authentication may not work correctly.');
      console.warn('⚠️  Set SKIP_AUTH=true to disable authentication for development.');
    } else {
      // In production, we'll log a warning but not crash the app
      // This allows for gradual rollout of Firebase auth
      console.warn('⚠️  WARNING: FIREBASE_SERVICE_ACCOUNT not set in production.');
      console.warn('⚠️  Firebase authentication will not work!');
    }
  }
  
  // Log environment mode
  console.log(`🌍 Environment: ${process.env.NODE_ENV || 'development'}`);
}

// Run the check
ensureCriticalEnvVars();

module.exports = {
  ensureCriticalEnvVars
}; 