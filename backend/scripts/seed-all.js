const mongoose = require('mongoose');
const dotenv = require('dotenv');
const { seedAdMobData } = require('../models/AdMobSample');
const { seedCoinsData } = require('../models/CoinsSample');
const path = require('path');
const fs = require('fs');

// Load environment variables
dotenv.config();

// MongoDB connection string
const MONGODB_URI = process.env.MONGODB_URI || 'mongodb+srv://ylalit0022:jBRgqv6BBfj2lYaG@cluster0.3d1qt.mongodb.net/eventwishes?retryWrites=true&w=majority';

// Import the seed-data script dynamically
const seedCategoryFestivalTemplate = require('./seed-data');

// Main function to run all seed scripts
async function seedAllData() {
  try {
    console.log('Starting complete database seeding process...');
    
    // Connect to MongoDB
    console.log('Connecting to MongoDB...');
    await mongoose.connect(MONGODB_URI);
    console.log('Connected to MongoDB successfully');
    
    // Run all seed functions in sequence
    console.log('\n=== Seeding Category Icons, Festivals, and Templates ===');
    await seedCategoryFestivalTemplate();
    
    console.log('\n=== Seeding AdMob Data ===');
    await seedAdMobData();
    
    console.log('\n=== Seeding Coins Data ===');
    await seedCoinsData();
    
    console.log('\n=== All seeding completed successfully! ===');
    
  } catch (error) {
    console.error('Error in seeding process:', error);
  } finally {
    // Close MongoDB connection
    try {
      await mongoose.connection.close();
      console.log('MongoDB connection closed');
    } catch (err) {
      console.error('Error closing MongoDB connection:', err);
    }
  }
}

// Run the seed function
seedAllData(); 