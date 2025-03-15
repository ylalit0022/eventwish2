/**
 * Secret Generator Script
 * 
 * This script generates secure random values for JWT_SECRET and API_KEY
 * Run with: node generate-secrets.js
 */

const crypto = require('crypto');

/**
 * Generates a secure random string of specified length
 * @param {number} length - Length of the string to generate
 * @returns {string} - Random string
 */
function generateSecureString(length = 64) {
  return crypto.randomBytes(length).toString('hex');
}

// Generate JWT_SECRET (64 bytes = 128 hex characters)
const jwtSecret = generateSecureString(64);

// Generate API_KEY (32 bytes = 64 hex characters)
const apiKey = generateSecureString(32);

// Generate INTERNAL_API_KEY for health checks (32 bytes = 64 hex characters)
const internalApiKey = generateSecureString(32);

console.log('\n=== GENERATED SECRETS ===');
console.log('\nFor local development (.env file):');
console.log('----------------------------');
console.log(`JWT_SECRET=${jwtSecret}`);
console.log(`API_KEY=${apiKey}`);
console.log(`INTERNAL_API_KEY=${internalApiKey}`);

console.log('\nFor Render.com environment variables:');
console.log('-----------------------------------');
console.log('Key: JWT_SECRET');
console.log(`Value: ${jwtSecret}`);
console.log('\nKey: API_KEY');
console.log(`Value: ${apiKey}`);
console.log('\nKey: INTERNAL_API_KEY');
console.log(`Value: ${internalApiKey}`);

console.log('\n=== IMPORTANT SECURITY NOTES ===');
console.log('1. Keep these values secret and never commit them to version control');
console.log('2. Store them securely in your password manager or secure note system');
console.log('3. If these values are compromised, generate new ones immediately');
console.log('4. For production, consider rotating these secrets periodically\n'); 