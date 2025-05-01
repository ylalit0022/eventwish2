/**
 * Test script for sponsored ad tracking
 * 
 * Run with: node scripts/testSponsoredAdTracking.js
 */

const mongoose = require('mongoose');
const SponsoredAd = require('../models/SponsoredAd');
require('dotenv').config();

// Test constants
const TEST_DEVICE_ID = 'test-device-' + Date.now(); // Unique test device ID
const TEST_IMPRESSIONS = 3;  // Number of test impressions to record
const TEST_CLICKS = 2;       // Number of test clicks to record

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
        console.log('Starting sponsored ad tracking tests...');
        
        // Create a test device ID
        const testDeviceId = 'test-device-' + Date.now();
        console.log(`Using test device ID: ${testDeviceId}`);
        
        // Create a test ad
        const testAd = new SponsoredAd({
            title: 'Test Sponsored Ad',
            description: 'Test ad for tracking verification',
            image_url: 'https://example.com/image.jpg',
            redirect_url: 'https://example.com',
            location: 'home_bottom',
            priority: 5,
            frequency_cap: 5,  // Cap at 5 impressions
            daily_frequency_cap: 3,  // Cap at 3 impressions per day
            start_date: new Date(),
            end_date: new Date(Date.now() + 7 * 24 * 60 * 60 * 1000) // 7 days from now
        });
        
        await testAd.save();
        console.log(`Created test ad with ID: ${testAd._id}`);
        
        // Record impressions
        console.log('\nTesting impression tracking:');
        for (let i = 1; i <= 4; i++) {
            console.log(`Recording impression ${i}...`);
            await testAd.recordImpression(testDeviceId);
            
            // Reload the ad to see updated counts
            const updatedAd = await SponsoredAd.findById(testAd._id);
            
            console.log(`- Total impressions: ${updatedAd.impression_count}`);
            console.log(`- Device impressions: ${updatedAd.device_impressions.get(testDeviceId) || 0}`);
            
            // Get daily impressions
            const today = new Date().toISOString().split('T')[0];
            let deviceDailyData = updatedAd.device_daily_impressions.get(testDeviceId);
            let todayImpressions = deviceDailyData ? deviceDailyData.get(today) || 0 : 0;
            
            console.log(`- Today's impressions: ${todayImpressions}`);
            
            // Check if frequency capping works
            if (i >= 3) {
                console.log(`Testing daily frequency cap (${updatedAd.daily_frequency_cap})...`);
                const beforeCount = updatedAd.impression_count;
                
                // This should not increment if we've hit the daily cap
                await updatedAd.recordImpression(testDeviceId);
                
                const afterAd = await SponsoredAd.findById(testAd._id);
                if (beforeCount === afterAd.impression_count) {
                    console.log('✅ Daily frequency cap worked - impression was not recorded');
                } else {
                    console.log('❌ Daily frequency cap failed - impression was recorded');
                }
                
                break; // Exit the loop
            }
        }
        
        // Record clicks
        console.log('\nTesting click tracking:');
        for (let i = 1; i <= 2; i++) {
            console.log(`Recording click ${i}...`);
            await testAd.recordClick(testDeviceId);
            
            // Reload the ad to see updated counts
            const updatedAd = await SponsoredAd.findById(testAd._id);
            
            console.log(`- Total clicks: ${updatedAd.click_count}`);
            console.log(`- Device clicks: ${updatedAd.device_clicks.get(testDeviceId) || 0}`);
        }
        
        // Test getActiveAds with frequency capping
        console.log('\nTesting getActiveAds with frequency capping:');
        const activeAds = await SponsoredAd.getActiveAds('home_bottom', testDeviceId);
        console.log(`Found ${activeAds.length} active ads for test device`);
        
        // Check if our test ad is frequency capped
        const testAdInResults = activeAds.find(ad => ad._id.toString() === testAd._id.toString());
        if (testAdInResults) {
            console.log('Test ad is in active ads results');
            
            if (testAdInResults.metrics && testAdInResults.metrics.is_daily_frequency_capped) {
                console.log('✅ Test ad is correctly marked as daily frequency capped');
            } else {
                console.log('❌ Test ad should be daily frequency capped but is not');
            }
        } else {
            console.log('✅ Test ad is correctly filtered out due to frequency capping');
        }
        
        // Clean up
        console.log('\nCleaning up...');
        await SponsoredAd.findByIdAndDelete(testAd._id);
        console.log('Test ad deleted');
        
        console.log('\nTests completed successfully!');
        
    } catch (error) {
        console.error('Test error:', error);
    } finally {
        // Close the MongoDB connection
        await mongoose.connection.close();
        console.log('MongoDB connection closed');
        process.exit(0);
    }
} 