const mongoose = require('mongoose');
const SponsoredAd = require('../models/SponsoredAd');
require('dotenv').config();

// Test constants
const TEST_DEVICE_ID = 'test-device-' + Date.now(); // Unique test device ID
const TEST_IMPRESSIONS = 3;  // Number of test impressions to record
const TEST_CLICKS = 2;       // Number of test clicks to record

async function testAdTracking() {
    try {
        // Connect to MongoDB
        await mongoose.connect(process.env.MONGODB_URI, {
            useNewUrlParser: true,
            useUnifiedTopology: true
        });
        console.log('Connected to MongoDB');

        // Find one active ad to test with
        const now = new Date();
        const ad = await SponsoredAd.findOne({
            status: true,
            start_date: { $lte: now },
            end_date: { $gte: now }
        });

        if (!ad) {
            console.log('No active ads found. Please insert demo ads first.');
            return;
        }

        console.log(`\nTesting with ad: ${ad.title} (ID: ${ad._id})`);
        console.log('Initial state:');
        console.log(`- Impression count: ${ad.impression_count}`);
        console.log(`- Click count: ${ad.click_count}`);
        
        // Initialize Maps if they don't exist
        if (!ad.device_impressions) {
            ad.device_impressions = new Map();
            ad.markModified('device_impressions');
        }
        
        if (!ad.device_clicks) {
            ad.device_clicks = new Map();
            ad.markModified('device_clicks');
        }
        
        if (!ad.device_daily_impressions) {
            ad.device_daily_impressions = new Map();
            ad.markModified('device_daily_impressions');
        }
        
        // Save the initialized maps
        await ad.save();
        console.log('- Initialized tracking maps');

        // Record impressions
        console.log(`\nRecording ${TEST_IMPRESSIONS} impressions for device ${TEST_DEVICE_ID}...`);
        for (let i = 0; i < TEST_IMPRESSIONS; i++) {
            try {
                await ad.recordImpression(TEST_DEVICE_ID);
                console.log(`  - Recorded impression ${i+1}`);
            } catch (error) {
                console.error(`  - Error recording impression ${i+1}:`, error);
            }
        }

        // Record clicks
        console.log(`\nRecording ${TEST_CLICKS} clicks for device ${TEST_DEVICE_ID}...`);
        for (let i = 0; i < TEST_CLICKS; i++) {
            try {
                await ad.recordClick(TEST_DEVICE_ID);
                console.log(`  - Recorded click ${i+1}`);
            } catch (error) {
                console.error(`  - Error recording click ${i+1}:`, error);
            }
        }

        // Reload ad from database to verify changes
        const updatedAd = await SponsoredAd.findById(ad._id);
        console.log('\nVerification:');
        console.log(`- Updated impression count: ${updatedAd.impression_count} (Expected: ${ad.impression_count + TEST_IMPRESSIONS})`);
        console.log(`- Updated click count: ${updatedAd.click_count} (Expected: ${ad.click_count + TEST_CLICKS})`);
        
        // Check if Maps exist
        console.log('- Map existence check:');
        console.log(`  * device_impressions exists: ${updatedAd.device_impressions instanceof Map}`);
        console.log(`  * device_clicks exists: ${updatedAd.device_clicks instanceof Map}`);
        console.log(`  * device_daily_impressions exists: ${updatedAd.device_daily_impressions instanceof Map}`);
        
        // Try-catch for safely accessing Map data
        try {
            // Check device-specific tracking
            const deviceImpressions = updatedAd.device_impressions.get(TEST_DEVICE_ID) || 0;
            const deviceClicks = updatedAd.device_clicks.get(TEST_DEVICE_ID) || 0;
            console.log(`- Device impressions: ${deviceImpressions} (Expected: ${TEST_IMPRESSIONS})`);
            console.log(`- Device clicks: ${deviceClicks} (Expected: ${TEST_CLICKS})`);
            
            // Check daily impressions
            const today = new Date().toISOString().split('T')[0];
            const dailyImpressions = updatedAd.device_daily_impressions.get(TEST_DEVICE_ID) || {};
            console.log('- Daily impressions object:', JSON.stringify(dailyImpressions));
            const todayImpressions = dailyImpressions[today] || 0;
            console.log(`- Today's impressions (${today}): ${todayImpressions} (Expected: ${TEST_IMPRESSIONS})`);
        } catch (error) {
            console.error('Error accessing Map data:', error);
        }
        
        // Verify frequency capping with try-catch
        try {
            console.log('\nTesting getActiveAds with frequency capping:');
            const activeAds = await SponsoredAd.getActiveAds(null, TEST_DEVICE_ID);
            
            if (activeAds && activeAds.length > 0) {
                console.log(`- Found ${activeAds.length} active ads`);
                
                // Find our test ad in the active ads
                const testAdInResults = activeAds.find(a => a._id.toString() === updatedAd._id.toString());
                if (testAdInResults) {
                    console.log('- Test ad found in active ads');
                    console.log('- Frequency cap metrics:');
                    if (testAdInResults.metrics) {
                        console.log(`  * Total impressions: ${testAdInResults.metrics.device_impressions}`);
                        console.log(`  * Today's impressions: ${testAdInResults.metrics.device_daily_impressions}`);
                        console.log(`  * Remaining total impressions: ${testAdInResults.metrics.remaining_impressions}`);
                        console.log(`  * Remaining daily impressions: ${testAdInResults.metrics.remaining_daily_impressions}`);
                        console.log(`  * Is frequency capped: ${testAdInResults.metrics.is_frequency_capped}`);
                        console.log(`  * Is daily frequency capped: ${testAdInResults.metrics.is_daily_frequency_capped}`);
                    } else {
                        console.log('  * Metrics object not found in test ad');
                    }
                } else {
                    console.log('- Test ad NOT found in active ads (may be filtered due to frequency capping)');
                }
            } else {
                console.log('- No active ads found');
            }
        } catch (error) {
            console.error('Error in getActiveAds test:', error);
        }

        console.log('\nTest completed successfully!');

    } catch (error) {
        console.error('Error during test:', error);
    } finally {
        try {
            // Close MongoDB connection
            await mongoose.connection.close();
            console.log('\nMongoDB connection closed');
        } catch (err) {
            console.error('Error closing MongoDB connection:', err);
        }
    }
}

// Run the test
testAdTracking().then(() => {
    console.log('Test script completed.');
}).catch(error => {
    console.error('Test script failed:', error);
    process.exit(1);
}); 