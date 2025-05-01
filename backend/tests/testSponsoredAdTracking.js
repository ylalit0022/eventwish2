/**
 * Test script for SponsoredAd tracking functionality
 * 
 * This test verifies:
 * 1. Creating sponsored ads
 * 2. Tracking impressions with device IDs
 * 3. Tracking clicks with device IDs
 * 4. Verifying daily impression frequency capping
 */

const mongoose = require('mongoose');
const SponsoredAd = require('../models/SponsoredAd');
const logger = require('../utils/logger') || console;

// Connect to the test database
async function connectToDatabase() {
  try {
    const connectionString = process.env.MONGODB_URI || 'mongodb://localhost:27017/eventwish_test';
    logger.info(`Connecting to MongoDB: ${connectionString}`);
    await mongoose.connect(connectionString);
    logger.info('MongoDB connected');
  } catch (err) {
    logger.error('MongoDB connection error:', err);
    process.exit(1);
  }
}

// Create a test sponsored ad
async function createTestSponsoredAd() {
  // Delete previous test ads
  await SponsoredAd.deleteMany({ title: 'Test Sponsored Ad' });
  
  // Create a new test ad
  const ad = new SponsoredAd({
    title: 'Test Sponsored Ad',
    image_url: 'https://example.com/ad.jpg',
    redirect_url: 'https://example.com',
    start_date: new Date(),
    end_date: new Date(Date.now() + 30 * 24 * 60 * 60 * 1000), // 30 days from now
    location: 'home_top',
    priority: 5,
    frequency_cap: 5,
    daily_frequency_cap: 2,
    description: 'Test ad for tracking functionality'
  });
  
  await ad.save();
  logger.info(`Created test ad with ID: ${ad._id}`);
  return ad;
}

// Test impression tracking
async function testImpressionTracking(ad) {
  const deviceId = 'test-device-' + Date.now();
  logger.info(`Testing impression tracking with device ID: ${deviceId}`);
  
  // Record multiple impressions
  for (let i = 0; i < 3; i++) {
    await ad.recordImpression(deviceId);
    logger.info(`Recorded impression ${i+1} for device ${deviceId}`);
  }
  
  // Fetch the updated ad
  const updatedAd = await SponsoredAd.findById(ad._id);
  
  // Verify impression count
  logger.info(`Total impression count: ${updatedAd.impression_count}`);
  if (updatedAd.impression_count !== 3) {
    throw new Error(`Expected 3 impressions, got ${updatedAd.impression_count}`);
  }
  
  // Verify device impressions
  const deviceImpressions = updatedAd.device_impressions.get(deviceId);
  logger.info(`Device impressions for ${deviceId}: ${deviceImpressions}`);
  if (deviceImpressions !== 3) {
    throw new Error(`Expected 3 device impressions, got ${deviceImpressions}`);
  }
  
  // Verify daily impressions
  const today = new Date().toISOString().split('T')[0];
  const dailyImpressions = updatedAd.device_daily_impressions.get(deviceId);
  logger.info(`Daily impressions for ${deviceId} on ${today}: ${dailyImpressions[today]}`);
  if (!dailyImpressions || dailyImpressions[today] !== 3) {
    throw new Error(`Expected 3 daily impressions, got ${dailyImpressions ? dailyImpressions[today] : 'undefined'}`);
  }
  
  return updatedAd;
}

// Test click tracking
async function testClickTracking(ad) {
  const deviceId = 'test-device-' + Date.now();
  logger.info(`Testing click tracking with device ID: ${deviceId}`);
  
  // Record a click
  await ad.recordClick(deviceId);
  logger.info(`Recorded click for device ${deviceId}`);
  
  // Fetch the updated ad
  const updatedAd = await SponsoredAd.findById(ad._id);
  
  // Verify click count
  logger.info(`Total click count: ${updatedAd.click_count}`);
  if (updatedAd.click_count !== 1) {
    throw new Error(`Expected 1 click, got ${updatedAd.click_count}`);
  }
  
  // Verify device clicks
  const deviceClicks = updatedAd.device_clicks.get(deviceId);
  logger.info(`Device clicks for ${deviceId}: ${deviceClicks}`);
  if (deviceClicks !== 1) {
    throw new Error(`Expected 1 device click, got ${deviceClicks}`);
  }
  
  return updatedAd;
}

// Test frequency capping
async function testFrequencyCapping(ad) {
  const deviceId = 'test-device-frequency-' + Date.now();
  logger.info(`Testing frequency capping with device ID: ${deviceId}`);
  
  // Record exactly the daily cap number of impressions
  for (let i = 0; i < ad.daily_frequency_cap; i++) {
    await ad.recordImpression(deviceId);
    logger.info(`Recorded impression ${i+1} for device ${deviceId}`);
  }
  
  // Get active ads for this device ID
  const activeAds = await SponsoredAd.getActiveAds('home_top', deviceId);
  logger.info(`Found ${activeAds.length} active ads for device ${deviceId}`);
  
  // Check if this ad is included in active ads (it shouldn't be, due to frequency capping)
  const found = activeAds.find(activeAd => activeAd._id.toString() === ad._id.toString());
  
  if (found) {
    throw new Error('Ad should be frequency capped but was returned in active ads');
  } else {
    logger.info('Frequency capping is working correctly - ad not returned when capped');
  }
}

// Run all tests
async function runTests() {
  try {
    // Connect to database
    await connectToDatabase();
    
    // Create test ad
    const ad = await createTestSponsoredAd();
    
    // Run impression tracking test
    await testImpressionTracking(ad);
    
    // Run click tracking test
    await testClickTracking(ad);
    
    // Run frequency capping test
    await testFrequencyCapping(ad);
    
    logger.info('✅ All tests passed successfully!');
  } catch (error) {
    logger.error('❌ Test failed:', error);
  } finally {
    // Clean up
    logger.info('Cleaning up...');
    await SponsoredAd.deleteMany({ title: 'Test Sponsored Ad' });
    
    // Disconnect from database
    await mongoose.disconnect();
    logger.info('MongoDB disconnected');
  }
}

// Run tests when this script is executed directly
if (require.main === module) {
  runTests();
}

module.exports = {
  runTests
}; 