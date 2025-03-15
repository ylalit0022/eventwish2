/**
 * Debug Environment Variables
 */

// Load environment variables
require('dotenv').config();

console.log('Environment Variables:');
console.log('---------------------');
console.log('PORT:', process.env.PORT);
console.log('NODE_ENV:', process.env.NODE_ENV);
console.log('JWT_SECRET:', process.env.JWT_SECRET ? 'Set (hidden)' : 'Not set');
console.log('API_KEY:', process.env.API_KEY ? 'Set (hidden)' : 'Not set');
console.log('INTERNAL_API_KEY:', process.env.INTERNAL_API_KEY ? 'Set (hidden)' : 'Not set');
console.log('MONGODB_URI:', process.env.MONGODB_URI ? 'Set (hidden)' : 'Not set');
console.log('REDIS_URL:', process.env.REDIS_URL || 'Not set');
console.log('LOG_LEVEL:', process.env.LOG_LEVEL || 'Not set');
console.log('VALID_APP_SIGNATURES:', process.env.VALID_APP_SIGNATURES || 'Not set'); 