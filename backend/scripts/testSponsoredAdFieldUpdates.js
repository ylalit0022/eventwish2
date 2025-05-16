/**
 * Test Script for SponsoredAd Field Updates
 * 
 * This script tests the handling of device_daily_impressions field in SponsoredAd model
 * to ensure proper type handling between plain objects and Maps.
 * 
 * Run with: node scripts/testSponsoredAdFieldUpdates.js
 */

const mongoose = require('mongoose');
require('dotenv').config();
const SponsoredAd = require('../models/SponsoredAd');
const logger = console;

// Test constants
const TEST_DEVICE_ID = 'test-device-' + Date.now();
const TODAY = new Date().toISOString().split('T')[0]; // YYYY-MM-DD format

// Connect to MongoDB
mongoose.connect(process.env.MONGODB_URI, {
    useNewUrlParser: true,
    useUnifiedTopology: true
}).then(() => {
    console.log('Connected to MongoDB successfully');
    runTests();
}).catch(err => {
    console.error('MongoDB connection error:', err);
    process.exit(1);
});

/**
 * Run all tests
 */
async function runTests() {
    try {
        console.log('\n=== SPONSORED AD FIELD UPDATES TEST ===\n');
        
        // Create a test sponsored ad
        const testAd = new SponsoredAd({
            title: 'Test Field Updates ' + Date.now(),
            image_url: 'https://example.com/test-image.png',
            redirect_url: 'https://example.com/redirect',
            start_date: new Date(),
            end_date: new Date(Date.now() + 86400000 * 30), // 30 days from now
            location: 'home_top',
            priority: 5,
            frequency_cap: 10,
            daily_frequency_cap: 5
        });
        
        await testAd.save();
        console.log(`Created test sponsored ad with ID: ${testAd._id}`);
        
        // Test 1: Create device_daily_impressions with plain object, mimicking potential issue
        console.log('\nTest 1: Creating device_daily_impressions with plain object');
        
        // Directly modify the document to simulate the issue where device_daily_impressions
        // contains a plain object instead of a Map for a device
        
        // First, let's record a normal impression via the proper method
        await testAd.recordImpression(TEST_DEVICE_ID);
        console.log(`- Recorded normal impression for device ${TEST_DEVICE_ID}`);
        
        // Now, let's fetch the ad from the database and check the structure
        let updatedAd = await SponsoredAd.findById(testAd._id);
        console.log(`- Total impression count: ${updatedAd.impression_count}`);
        
        // Get device_daily_impressions and verify it's a Map
        const deviceDailyData = updatedAd.device_daily_impressions.get(TEST_DEVICE_ID);
        console.log(`- device_daily_impressions is Map? ${updatedAd.device_daily_impressions instanceof Map}`);
        console.log(`- Device daily data for ${TEST_DEVICE_ID} is Map? ${deviceDailyData instanceof Map}`);
        console.log(`- Today's impressions count: ${deviceDailyData.get(TODAY) || 0}`);
        
        // Test 2: Simulate corrupted data by setting a plain object for daily impressions
        console.log('\nTest 2: Simulating corrupted data with plain object');
        
        // Create a plain object to simulate corrupted data
        const plainObject = {};
        plainObject[TODAY] = 2; // Set today's count to 2
        
        // Force set the plain object for a new device ID
        const CORRUPTED_DEVICE_ID = 'corrupted-device-' + Date.now();
        updatedAd.device_daily_impressions.set(CORRUPTED_DEVICE_ID, plainObject);
        updatedAd.markModified('device_daily_impressions');
        await updatedAd.save();
        
        console.log(`- Set corrupted data (plain object) for device ${CORRUPTED_DEVICE_ID}`);
        
        // Test 3: Now attempt to record an impression for the corrupted device
        console.log('\nTest 3: Recording impression for device with corrupted data');
        
        try {
            // This should automatically fix the corrupted data structure
            await updatedAd.recordImpression(CORRUPTED_DEVICE_ID);
            console.log(`- Successfully recorded impression for corrupted device`);
            
            // Get the updated ad and check if the fix worked
            const fixedAd = await SponsoredAd.findById(testAd._id);
            
            // Check if device_daily_impressions is Map
            console.log(`- device_daily_impressions is Map? ${fixedAd.device_daily_impressions instanceof Map}`);
            
            // Get the fixed device data
            const fixedDeviceData = fixedAd.device_daily_impressions.get(CORRUPTED_DEVICE_ID);
            
            // Check if the previously corrupted entry is now a Map
            console.log(`- Fixed device data is Map? ${fixedDeviceData instanceof Map}`);
            
            // Check if today's count was correctly transferred and incremented
            console.log(`- Today's impression count: ${fixedDeviceData.get(TODAY) || 0}`);
            console.log(`- Expected count is 3 (2 from corrupt object + 1 new): ${fixedDeviceData.get(TODAY) === 3 ? '✅ Yes' : '❌ No'}`);
        } catch (error) {
            console.error(`❌ Error recording impression for corrupted device:`, error);
            console.error(error.stack);
        }
        
        // Test 4: Test with getActiveAds to ensure metrics handling
        console.log('\nTest 4: Testing getActiveAds with fixed data');
        
        const activeAds = await SponsoredAd.getActiveAds('home_top', CORRUPTED_DEVICE_ID);
        console.log(`- Found ${activeAds.length} active ads`);
        
        const ourAd = activeAds.find(ad => ad._id.toString() === testAd._id.toString());
        
        if (ourAd) {
            console.log(`- Our test ad found in results`);
            console.log(`- Ad metrics available? ${ourAd.metrics ? '✅ Yes' : '❌ No'}`);
            
            if (ourAd.metrics) {
                console.log(`  - Device impressions: ${ourAd.metrics.device_impressions}`);
                console.log(`  - Device daily impressions: ${ourAd.metrics.device_daily_impressions}`);
                console.log(`  - Expected daily impressions is 3: ${ourAd.metrics.device_daily_impressions === 3 ? '✅ Yes' : '❌ No'}`);
            }
        } else {
            console.log(`❌ Our test ad not found in results (possibly filtered out)`);
        }
        
        // Clean up
        console.log('\nCleaning up...');
        await SponsoredAd.findByIdAndDelete(testAd._id);
        console.log('Test ad deleted');
        
        console.log('\n=== SPONSORED AD FIELD UPDATES TEST COMPLETED ===\n');
    } catch (error) {
        console.error('Test error:', error);
        console.error(error.stack);
    } finally {
        await mongoose.connection.close();
        console.log('MongoDB connection closed');
        process.exit(0);
    }
} 