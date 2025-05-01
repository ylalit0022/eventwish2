/**
 * Verification script for SponsoredAd model
 * 
 * This script verifies the structure and methods of the SponsoredAd model
 * without requiring a database connection.
 */

const SponsoredAd = require('../models/SponsoredAd');
const mongoose = require('mongoose');

// Mock the mongoose Model's save method to avoid database calls
SponsoredAd.prototype.save = function() {
  console.log(`Mock save called for ad ${this._id}`);
  return Promise.resolve(this);
};

// Create a test SponsoredAd instance
function createTestAd() {
  // Create a fake ObjectId
  const fakeId = new mongoose.Types.ObjectId();
  
  const ad = new SponsoredAd({
    _id: fakeId,
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
  
  console.log('Created test ad instance:', ad._id.toString());
  return ad;
}

// Test impression tracking method
async function testRecordImpression() {
  console.log('\n=== Testing recordImpression method ===');
  const ad = createTestAd();
  const deviceId = 'test-device-123';
  
  // Record impression
  await ad.recordImpression(deviceId);
  
  // Verify impression count
  console.log(`Total impression count: ${ad.impression_count}`);
  console.assert(ad.impression_count === 1, 'Impression count should be 1');
  
  // Verify device impression
  const deviceImpression = ad.device_impressions.get(deviceId);
  console.log(`Device impression count: ${deviceImpression}`);
  console.assert(deviceImpression === 1, 'Device impression count should be 1');
  
  // Verify daily impression
  const today = new Date().toISOString().split('T')[0];
  const dailyImpressions = ad.device_daily_impressions.get(deviceId);
  console.log(`Daily impressions for ${deviceId}:`, dailyImpressions);
  console.assert(
    dailyImpressions && typeof dailyImpressions === 'object',
    'Daily impressions should be an object'
  );
  console.assert(
    dailyImpressions[today] === 1,
    `Today's impression count should be 1, got ${dailyImpressions[today]}`
  );
  
  // Record a second impression
  await ad.recordImpression(deviceId);
  
  // Verify updated counts
  console.log(`Total impression count after second impression: ${ad.impression_count}`);
  console.assert(ad.impression_count === 2, 'Impression count should be 2');
  
  console.log(`Device impression count after second impression: ${ad.device_impressions.get(deviceId)}`);
  console.assert(ad.device_impressions.get(deviceId) === 2, 'Device impression count should be 2');
  
  console.log(`Daily impressions after second impression:`, ad.device_daily_impressions.get(deviceId));
  console.assert(
    ad.device_daily_impressions.get(deviceId)[today] === 2,
    `Today's impression count should be 2, got ${ad.device_daily_impressions.get(deviceId)[today]}`
  );
  
  console.log('✅ recordImpression test passed');
  return ad;
}

// Test click tracking method
async function testRecordClick() {
  console.log('\n=== Testing recordClick method ===');
  const ad = createTestAd();
  const deviceId = 'test-device-123';
  
  // Record click
  await ad.recordClick(deviceId);
  
  // Verify click count
  console.log(`Total click count: ${ad.click_count}`);
  console.assert(ad.click_count === 1, 'Click count should be 1');
  
  // Verify device click
  const deviceClick = ad.device_clicks.get(deviceId);
  console.log(`Device click count: ${deviceClick}`);
  console.assert(deviceClick === 1, 'Device click count should be 1');
  
  // Record a second click
  await ad.recordClick(deviceId);
  
  // Verify updated counts
  console.log(`Total click count after second click: ${ad.click_count}`);
  console.assert(ad.click_count === 2, 'Click count should be 2');
  
  console.log(`Device click count after second click: ${ad.device_clicks.get(deviceId)}`);
  console.assert(ad.device_clicks.get(deviceId) === 2, 'Device click count should be 2');
  
  console.log('✅ recordClick test passed');
  return ad;
}

// Test multiple device tracking
async function testMultipleDevices() {
  console.log('\n=== Testing multiple device tracking ===');
  const ad = createTestAd();
  const devices = ['device-1', 'device-2', 'device-3'];
  
  // Record impressions for multiple devices
  for (const deviceId of devices) {
    await ad.recordImpression(deviceId);
    console.log(`Recorded impression for device ${deviceId}`);
  }
  
  // Verify impression counts
  console.log(`Total impression count: ${ad.impression_count}`);
  console.assert(ad.impression_count === devices.length, 
                 `Impression count should be ${devices.length}`);
  
  // Verify device impressions map
  console.log('Device impressions map size:', ad.device_impressions.size);
  console.assert(ad.device_impressions.size === devices.length, 
                 `Device impressions map should contain ${devices.length} entries`);
  
  // Verify daily impressions map
  console.log('Daily impressions map size:', ad.device_daily_impressions.size);
  console.assert(ad.device_daily_impressions.size === devices.length, 
                 `Daily impressions map should contain ${devices.length} entries`);
  
  // Record clicks for first two devices
  await ad.recordClick(devices[0]);
  await ad.recordClick(devices[1]);
  
  // Verify click counts
  console.log(`Total click count: ${ad.click_count}`);
  console.assert(ad.click_count === 2, 'Click count should be 2');
  
  // Verify device clicks map
  console.log('Device clicks map size:', ad.device_clicks.size);
  console.assert(ad.device_clicks.size === 2, 'Device clicks map should contain 2 entries');
  
  console.log('✅ Multiple device tracking test passed');
  return ad;
}

// Run all verification tests
async function runVerification() {
  try {
    console.log('Starting SponsoredAd model verification...\n');
    
    // Test impression tracking
    await testRecordImpression();
    
    // Test click tracking
    await testRecordClick();
    
    // Test multiple devices
    await testMultipleDevices();
    
    console.log('\n✅ All verification tests passed successfully!');
  } catch (error) {
    console.error('❌ Verification failed:', error);
  }
}

// Run verification when script is executed directly
if (require.main === module) {
  runVerification();
}

module.exports = {
  runVerification
}; 