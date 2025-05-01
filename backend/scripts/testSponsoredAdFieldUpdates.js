/**
 * Test script to verify SponsoredAd field tracking and persistence
 * 
 * This script tests:
 * 1. Creation of sponsored ad records
 * 2. Tracking of impressions with device IDs
 * 3. Tracking of daily impressions with frequency caps
 * 4. Proper persistence of all tracking data to MongoDB
 */

const mongoose = require('mongoose');
const SponsoredAd = require('../models/SponsoredAd');
require('dotenv').config();

// Helper to generate unique test device IDs
function generateTestDeviceId() {
  return `test_device_${Date.now()}_${Math.floor(Math.random() * 10000)}`;
}

// Create a test sponsored ad
async function createTestSponsoredAd() {
  const testAd = new SponsoredAd({
    title: `Test Ad ${Date.now()}`,
    description: 'Test ad for field update verification',
    image_url: 'https://example.com/test.jpg',
    redirect_url: 'https://example.com/test',
    status: true,
    start_date: new Date(),
    end_date: new Date(Date.now() + 30 * 24 * 60 * 60 * 1000), // 30 days from now
    location: 'home_top',
    priority: 5,
    frequency_cap: 10,
    daily_frequency_cap: 5,
    click_count: 0,
    impression_count: 0,
    device_impressions: new Map(),
    device_clicks: new Map(),
    device_daily_impressions: {} // Initialize as empty object
  });
  
  await testAd.save();
  console.log(`Created test sponsored ad: ${testAd._id}`);
  return testAd;
}

// Track impressions for a specific device
async function trackImpressions(ad, deviceId, count) {
  console.log(`Tracking ${count} impressions for device ${deviceId} on ad ${ad._id}`);
  
  for (let i = 0; i < count; i++) {
    // Record impression using the model's method
    await ad.recordImpression(deviceId);
    console.log(`Recorded impression ${i+1}/${count}`);
  }
  
  // Reload the ad from the database to verify persistence
  const updatedAd = await SponsoredAd.findById(ad._id);
  return updatedAd;
}

// Test daily impression tracking and capping
async function testDailyImpressionTracking(ad, deviceId, attemptCount) {
  console.log(`\nTesting daily impression tracking for device ${deviceId}`);
  console.log(`Daily impression cap is set to: ${ad.daily_frequency_cap}`);
  
  let actualImpressions = 0;
  const today = new Date().toISOString().split('T')[0]; // YYYY-MM-DD
  
  for (let i = 0; i < attemptCount; i++) {
    const beforeCount = ad.impression_count;
    
    // Try to record impression (may be capped)
    const result = await ad.recordImpression(deviceId);
    
    // Check if impression was counted or capped
    if (ad.impression_count > beforeCount) {
      actualImpressions++;
      console.log(`Impression ${i+1}/${attemptCount}: ✅ Recorded`);
    } else {
      console.log(`Impression ${i+1}/${attemptCount}: ❌ Capped (daily limit reached)`);
    }
  }
  
  // Get the current daily count for this device
  const dailyImpressions = ad.device_daily_impressions || {};
  const deviceDailyImpressions = dailyImpressions[deviceId] || {};
  const todayCount = deviceDailyImpressions[today] || 0;
  
  console.log(`\nDaily impression results for device ${deviceId}:`);
  console.log(`- Attempted impressions: ${attemptCount}`);
  console.log(`- Actually recorded: ${actualImpressions}`);
  console.log(`- Recorded in daily tracking: ${todayCount}`);
  console.log(`- Daily cap: ${ad.daily_frequency_cap}`);
  console.log(`- Correctly capped: ${todayCount <= ad.daily_frequency_cap ? '✅ Yes' : '❌ No'}`);
  
  return {
    attempted: attemptCount,
    recorded: actualImpressions,
    dailyCount: todayCount
  };
}

// Run all tests
async function runTests() {
  try {
    // Connect to MongoDB
    console.log(`Connecting to MongoDB: ${process.env.MONGODB_URI || 'mongodb://localhost:27017/eventwish_test'}`);
    await mongoose.connect(process.env.MONGODB_URI, {
      useNewUrlParser: true,
      useUnifiedTopology: true
    });
    console.log('Connected to MongoDB');
    
    // Create test ad
    const testAd = await createTestSponsoredAd();
    console.log(`Initial ad state: impression_count=${testAd.impression_count}, device_daily_impressions=${JSON.stringify(testAd.device_daily_impressions || {})}`);
    
    // Test 1: Basic impression tracking
    console.log('\n=== Test 1: Basic Impression Tracking ===');
    const deviceId1 = generateTestDeviceId();
    console.log(`Using test device ID: ${deviceId1}`);
    
    const impressionCount = 3;
    const updatedAd = await trackImpressions(testAd, deviceId1, impressionCount);
    
    console.log(`\nAfter ${impressionCount} impressions:`);
    console.log(`- Total impression count: ${updatedAd.impression_count}`);
    console.log(`- Device impression count: ${updatedAd.device_impressions.get(deviceId1) || 0}`);
    
    const impressionsRecorded = updatedAd.impression_count === impressionCount;
    console.log(`- All impressions recorded correctly: ${impressionsRecorded ? '✅ Yes' : '❌ No'}`);
    
    // Test 2: Daily impression capping
    console.log('\n=== Test 2: Daily Impression Capping ===');
    const deviceId2 = generateTestDeviceId();
    console.log(`Using test device ID: ${deviceId2}`);
    
    // Try to record more impressions than the daily cap allows
    const attemptCount = updatedAd.daily_frequency_cap + 5; // Try 5 more than the cap
    const cappingResults = await testDailyImpressionTracking(updatedAd, deviceId2, attemptCount);
    
    const cappingWorks = cappingResults.dailyCount <= updatedAd.daily_frequency_cap;
    console.log(`\nDaily capping works correctly: ${cappingWorks ? '✅ Yes' : '❌ No'}`);
    
    // Test 3: Verify data persistence
    console.log('\n=== Test 3: Data Persistence Verification ===');
    
    // Reload the ad from the database
    const reloadedAd = await SponsoredAd.findById(updatedAd._id);
    
    console.log('Verifying data persistence after database reload:');
    console.log(`- Total impression count: ${reloadedAd.impression_count}`);
    
    // Check device impressions persistence
    const device1Impressions = reloadedAd.device_impressions.get(deviceId1) || 0;
    console.log(`- Device 1 impressions: ${device1Impressions}`);
    console.log(`- Device 1 impressions correct: ${device1Impressions === impressionCount ? '✅ Yes' : '❌ No'}`);
    
    // Check daily impression tracking persistence
    const today = new Date().toISOString().split('T')[0];
    const dailyImpressions = reloadedAd.device_daily_impressions || {};
    const device2DailyImpressions = dailyImpressions[deviceId2] || {};
    const device2TodayCount = device2DailyImpressions[today] || 0;
    
    console.log(`- Device 2 daily impressions: ${device2TodayCount}`);
    console.log(`- Daily impression tracking correct: ${device2TodayCount === cappingResults.dailyCount ? '✅ Yes' : '❌ No'}`);
    
    // Print the raw data for inspection
    console.log('\nRaw device_daily_impressions data:');
    console.log(JSON.stringify(reloadedAd.device_daily_impressions || {}, null, 2));
    
    // Final summary
    console.log('\n=== Test Summary ===');
    console.log(`- Basic impression tracking: ${impressionsRecorded ? '✅ Passed' : '❌ Failed'}`);
    console.log(`- Daily impression capping: ${cappingWorks ? '✅ Passed' : '❌ Failed'}`);
    console.log(`- Data persistence: ${(device1Impressions === impressionCount && device2TodayCount === cappingResults.dailyCount) ? '✅ Passed' : '❌ Failed'}`);
    
  } catch (error) {
    console.error('Error during testing:', error);
    console.error(error.stack);
  } finally {
    await mongoose.connection.close();
    console.log('MongoDB connection closed');
  }
}

// Run the tests
runTests().then(() => {
  console.log('All tests completed.');
}).catch(error => {
  console.error('Tests failed:', error);
  console.error(error.stack);
  process.exit(1);
}); 