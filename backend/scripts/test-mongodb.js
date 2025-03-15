/**
 * MongoDB Connection Test Script
 * 
 * This script tests the connection to MongoDB and logs detailed information
 * about any errors that occur.
 */

require('../config/env-loader');
const mongoose = require('mongoose');

console.log('MongoDB Connection Test');
console.log('======================');
console.log('Node.js version:', process.version);
console.log('Mongoose version:', mongoose.version);
console.log('MongoDB URI exists:', !!process.env.MONGODB_URI);
console.log('Environment:', process.env.NODE_ENV);

const options = {
  useNewUrlParser: true,
  useUnifiedTopology: true,
  serverSelectionTimeoutMS: 30000,
  socketTimeoutMS: 45000,
  family: 4
};

console.log('Connection options:', JSON.stringify(options, null, 2));
console.log('Attempting to connect to MongoDB...');

mongoose.connect(process.env.MONGODB_URI, options)
  .then(() => {
    console.log('✅ Connected to MongoDB successfully!');
    console.log('Connection details:');
    console.log('- Host:', mongoose.connection.host);
    console.log('- Database name:', mongoose.connection.name);
    console.log('- Connection state:', mongoose.connection.readyState);
    
    // List collections
    return mongoose.connection.db.listCollections().toArray();
  })
  .then((collections) => {
    console.log('Collections in database:');
    collections.forEach(collection => {
      console.log(`- ${collection.name}`);
    });
    
    // Close connection
    return mongoose.connection.close();
  })
  .then(() => {
    console.log('Connection closed successfully');
    process.exit(0);
  })
  .catch((err) => {
    console.error('❌ MongoDB connection error:');
    console.error('Error message:', err.message);
    console.error('Error name:', err.name);
    console.error('Error code:', err.code);
    
    if (err.reason) {
      console.error('Error reason:', err.reason);
    }
    
    if (err.stack) {
      console.error('Stack trace:', err.stack);
    }
    
    process.exit(1);
  }); 