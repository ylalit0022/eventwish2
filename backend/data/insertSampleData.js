/**
 * Combined script to insert sample data for About and Contact sections
 * 
 * This script imports and runs both sample data insertion functions
 */

const mongoose = require('mongoose');
const dotenv = require('dotenv');
const { insertAboutSample } = require('./aboutSample');
const { insertContactSample } = require('./contactSample');
const logger = require('../config/logger');

// Load environment variables
dotenv.config();

/**
 * Insert all sample data
 */
const insertAllSampleData = async () => {
  try {
    console.log('Starting sample data insertion...');
    
    // Insert About sample data
    await insertAboutSample();
    
    // Insert Contact sample data
    await insertContactSample();
    
    console.log('All sample data inserted successfully!');
  } catch (error) {
    logger.error(`Error inserting sample data: ${error.message}`);
    console.error('Error inserting sample data:', error);
  }
};

// Run the script if executed directly
if (require.main === module) {
  // Connect to MongoDB
  mongoose.connect(process.env.MONGODB_URI, {
    useNewUrlParser: true,
    useUnifiedTopology: true
  })
  .then(async () => {
    console.log('Connected to MongoDB');
    await insertAllSampleData();
    console.log('Done!');
    process.exit(0);
  })
  .catch(err => {
    console.error('MongoDB connection error:', err);
    process.exit(1);
  });
} else {
  // Export for use in other scripts
  module.exports = { insertAllSampleData };
} 