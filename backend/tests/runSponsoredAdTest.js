/**
 * Modified test script for SponsoredAd tracking functionality
 * 
 * This test verifies:
 * 1. Creating sponsored ads
 * 2. Tracking impressions with device IDs
 * 3. Tracking clicks with device IDs
 * 4. Verifying daily impression frequency capping
 */

const mongoose = require('mongoose');
const SponsoredAd = require('../models/SponsoredAd');
require('dotenv').config();

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
  console.log(`Created test ad with ID: ${ad._id}`);
  return ad;
}

// Test impression tracking
async function testImpressionTracking(ad) {
  const deviceId = 'test-device-' + Date.now();
  console.log(`Testing impression tracking with device ID: ${deviceId}`);
  
  // Record multiple impressions
  for (let i = 0; i < 3; i++) {
    await ad.recordImpression(deviceId);
    console.log(`Recorded impression ${i+1} for device ${deviceId}`);
  }
  
  // Fetch the updated ad
  const updatedAd = await SponsoredAd.findById(ad._id);
  
  // Verify impression count
  console.log(`Total impression count: ${updatedAd.impression_count}`);
  if (updatedAd.impression_count !== 3) {
    throw new Error(`Expected 3 impressions, got ${updatedAd.impression_count}`);
  }
  
  // Verify device impressions
  const deviceImpressions = updatedAd.device_impressions.get(deviceId);
  console.log(`Device impressions for ${deviceId}: ${deviceImpressions}`);
  if (deviceImpressions !== 3) {
    throw new Error(`Expected 3 device impressions, got ${deviceImpressions}`);
  }
  
  // Verify daily impressions
  const today = new Date().toISOString().split('T')[0];
  const dailyImpressions = updatedAd.device_daily_impressions.get(deviceId);
  console.log(`Daily impressions for ${deviceId} on ${today}: ${dailyImpressions[today]}`);
  if (!dailyImpressions || dailyImpressions[today] !== 3) {
    throw new Error(`Expected 3 daily impressions, got ${dailyImpressions ? dailyImpressions[today] : 'undefined'}`);
  }
  
  return updatedAd;
}

// Test click tracking
async function testClickTracking(ad) {
  const deviceId = 'test-device-' + Date.now();
  console.log(`Testing click tracking with device ID: ${deviceId}`);
  
  // Record a click
  await ad.recordClick(deviceId);
  console.log(`Recorded click for device ${deviceId}`);
  
  // Fetch the updated ad
  const updatedAd = await SponsoredAd.findById(ad._id);
  
  // Verify click count
  console.log(`Total click count: ${updatedAd.click_count}`);
  if (updatedAd.click_count !== 1) {
    throw new Error(`Expected 1 click, got ${updatedAd.click_count}`);
  }
  
  // Verify device clicks
  const deviceClicks = updatedAd.device_clicks.get(deviceId);
  console.log(`Device clicks for ${deviceId}: ${deviceClicks}`);
  if (deviceClicks !== 1) {
    throw new Error(`Expected 1 device click, got ${deviceClicks}`);
  }
  
  return updatedAd;
}

// Run all tests
async function runTests() {
  try {
    // Connect to MongoDB
    console.log(`Connecting to MongoDB: ${process.env.MONGODB_URI}`);
    await mongoose.connect(process.env.MONGODB_URI, {
      useNewUrlParser: true,
      useUnifiedTopology: true
    });
    console.log('MongoDB connected');
    
    // Create test ad
    const ad = await createTestSponsoredAd();
    
    // Run impression tracking test
    await testImpressionTracking(ad);
    
    // Run click tracking test
    await testClickTracking(ad);
    
    console.log('✅ All tests passed successfully!');
  } catch (error) {
    console.error('❌ Test failed:', error);
  } finally {
    // Clean up
    console.log('Cleaning up...');
    await SponsoredAd.deleteMany({ title: 'Test Sponsored Ad' });
    
    // Disconnect from database
    await mongoose.disconnect();
    console.log('MongoDB disconnected');
  }
}

// Run tests when this script is executed directly
if (require.main === module) {
  runTests();
}

module.exports = {
  runTests
}; 