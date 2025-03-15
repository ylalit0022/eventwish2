/**
 * Simple Environment Variables Check Script
 */

// Load environment variables
require('dotenv').config();

// Define required environment variables
const requiredVars = [
  'PORT',
  'NODE_ENV',
  'JWT_SECRET',
  'API_KEY',
  'INTERNAL_API_KEY',
  'MONGODB_URI'
];

// Optional variables
const optionalVars = [
  'REDIS_URL',
  'LOG_LEVEL',
  'VALID_APP_SIGNATURES'
];

console.log('\n=== ENVIRONMENT VARIABLES CHECK ===\n');

// Check required variables
let missingVars = [];

for (const varName of requiredVars) {
  if (!process.env[varName]) {
    console.log(`❌ Missing required variable: ${varName}`);
    missingVars.push(varName);
  } else {
    console.log(`✅ ${varName} is set`);
  }
}

console.log('\n--- Optional Variables ---');

// Check optional variables
for (const varName of optionalVars) {
  if (!process.env[varName]) {
    console.log(`⚠️ Missing optional variable: ${varName}`);
  } else {
    console.log(`✅ ${varName} is set`);
  }
}

// Print summary
console.log('\n--- Summary ---');
if (missingVars.length > 0) {
  console.log(`❌ Missing ${missingVars.length} required variables: ${missingVars.join(', ')}`);
  console.log('Please check ENV_SETUP.md for instructions on setting up environment variables.\n');
  process.exit(1);
} else {
  console.log('✅ All required environment variables are set.\n');
  process.exit(0);
} 