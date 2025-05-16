/**
 * Test script for SponsoredAd impression tracking
 * 
 * This script tests the SponsoredAd model's ability to properly track impressions
 * particularly focused on the device_daily_impressions map handling
 * 
 * Run with: node scripts/testSponsoredAdTracking.js
 */

const mongoose = require('mongoose');
require('dotenv').config();
const SponsoredAd = require('../models/SponsoredAd');
const logger = console;

// Test constants
const TEST_DEVICE_ID = 'test-device-' + Date.now();
const TEST_IMAGE_URL = 'https://example.com/test-ad-' + Date.now() + '.png';
const TEST_REDIRECT_URL = 'https://example.com/redirect-' + Date.now();

// MongoDB connection
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

async function runTests() {
    try {
        console.log('\n=== STARTING SPONSORED AD TRACKING TESTS ===\n');
        
        // Create a test sponsored ad
        const testAd = new SponsoredAd({
            title: 'Test Sponsored Ad ' + Date.now(),
            image_url: TEST_IMAGE_URL,
            redirect_url: TEST_REDIRECT_URL,
            start_date: new Date(),
            end_date: new Date(Date.now() + 86400000 * 30), // 30 days from now
            location: 'home_top',
            priority: 5,
            frequency_cap: 10,
            daily_frequency_cap: 3
        });
        
        await testAd.save();
        console.log(`Created test sponsored ad with ID: ${testAd._id}`);
        
        // Test 1: Record an impression and verify device_impressions
        console.log('\nTest 1: Recording a single impression');
        await testAd.recordImpression(TEST_DEVICE_ID);
        
        // Reload the ad from the database
        let updatedAd = await SponsoredAd.findById(testAd._id);
        
        console.log(`- Total impression count: ${updatedAd.impression_count}`);
        console.log(`- Device impression count: ${updatedAd.device_impressions.get(TEST_DEVICE_ID) || 0}`);
        
        const today = new Date().toISOString().split('T')[0]; // YYYY-MM-DD format
        
        // Directly check the structure of device_daily_impressions
        console.log('\nVerifying device_daily_impressions structure:');
        console.log(`- device_daily_impressions is Map? ${updatedAd.device_daily_impressions instanceof Map}`);
        
        const deviceDailyData = updatedAd.device_daily_impressions.get(TEST_DEVICE_ID);
        console.log(`- Device daily data for ${TEST_DEVICE_ID} is Map? ${deviceDailyData instanceof Map}`);
        
        if (deviceDailyData) {
            console.log(`- Today's (${today}) impressions: ${deviceDailyData.get(today) || 0}`);
        } else {
            console.log('❌ Device daily data is not properly initialized');
        }
        
        // Test 2: Record multiple impressions to test daily cap
        console.log('\nTest 2: Testing daily impression cap');
        
        // Record up to the daily cap (3 total impressions including the first one)
        for (let i = 0; i < 5; i++) {
            await updatedAd.recordImpression(TEST_DEVICE_ID);
            console.log(`- Recorded impression ${i+2} for device ${TEST_DEVICE_ID}`);
        }
        
        // Reload the ad again
        updatedAd = await SponsoredAd.findById(testAd._id);
        
        console.log(`- Total impression count: ${updatedAd.impression_count}`);
        console.log(`- Device impression count: ${updatedAd.device_impressions.get(TEST_DEVICE_ID) || 0}`);
        
        // Check daily impressions
        const updatedDeviceDailyData = updatedAd.device_daily_impressions.get(TEST_DEVICE_ID);
        
        if (updatedDeviceDailyData) {
            console.log(`- Today's (${today}) impressions: ${updatedDeviceDailyData.get(today) || 0}`);
            console.log(`- Capped at daily limit (3)? ${(updatedDeviceDailyData.get(today) || 0) <= 3 ? '✅ Yes' : '❌ No'}`);
        } else {
            console.log('❌ Device daily data is not properly initialized after multiple impressions');
        }
        
        // Test 3: Record a click and verify device_clicks
        console.log('\nTest 3: Recording a click');
        await updatedAd.recordClick(TEST_DEVICE_ID);
        
        // Reload the ad again
        updatedAd = await SponsoredAd.findById(testAd._id);
        
        console.log(`- Total click count: ${updatedAd.click_count}`);
        console.log(`- Device click count: ${updatedAd.device_clicks.get(TEST_DEVICE_ID) || 0}`);
        
        // Test 4: Get active ads with frequency capping
        console.log('\nTest 4: Testing getActiveAds with frequency capping');
        const activeAds = await SponsoredAd.getActiveAds('home_top', TEST_DEVICE_ID);
        
        console.log(`- Found ${activeAds.length} active ads`);
        const testAdInResults = activeAds.some(ad => ad._id.toString() === updatedAd._id.toString());
        console.log(`- Test ad found in results? ${testAdInResults ? '✅ Yes' : '❌ No (Likely capped)'}`);
        
        if (activeAds.length > 0) {
            const firstAd = activeAds[0];
            console.log('- First ad metrics available? ' + (firstAd.metrics ? '✅ Yes' : '❌ No'));
            if (firstAd.metrics) {
                console.log(`  - Device impressions: ${firstAd.metrics.device_impressions}`);
                console.log(`  - Daily impressions: ${firstAd.metrics.device_daily_impressions}`);
                console.log(`  - Frequency capped: ${firstAd.metrics.is_frequency_capped}`);
                console.log(`  - Daily frequency capped: ${firstAd.metrics.is_daily_frequency_capped}`);
            }
        }
        
        // Clean up
        console.log('\nCleaning up...');
        await SponsoredAd.findByIdAndDelete(testAd._id);
        console.log('Test ad deleted');
        
        console.log('\n=== SPONSORED AD TRACKING TESTS COMPLETED ===\n');
        
    } catch (error) {
        console.error('Test error:', error);
        console.error(error.stack);
    } finally {
        // Close the MongoDB connection
        await mongoose.connection.close();
        console.log('MongoDB connection closed');
        process.exit(0);
    }
} 