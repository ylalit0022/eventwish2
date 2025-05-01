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
        console.log(`Connecting to MongoDB: ${process.env.MONGODB_URI || 'mongodb://localhost:27017/eventwish_test'}`);
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
            console.log('No active ads found. Creating a test ad...');
            // Create a test ad if none exists
            const newAd = new SponsoredAd({
                title: 'Test Sponsored Ad',
                image_url: 'https://example.com/ad.jpg',
                redirect_url: 'https://example.com',
                start_date: new Date(),
                end_date: new Date(Date.now() + 30 * 24 * 60 * 60 * 1000), // 30 days from now
                location: 'home_top',
                priority: 5,
                frequency_cap: 5,
                daily_frequency_cap: 2,
                description: 'Test ad for tracking functionality',
                status: true
            });
            
            await newAd.save();
            console.log(`Created test ad with ID: ${newAd._id}`);
            return testAdTracking(); // Restart the test with the new ad
        }

        console.log(`\nTesting with ad: ${ad.title} (ID: ${ad._id})`);
        console.log('Initial state:');
        console.log(`- Impression count: ${ad.impression_count}`);
        console.log(`- Click count: ${ad.click_count}`);
        
        // Debug current Map states
        console.log('\nCurrent Map states:');
        console.log(`- device_impressions type: ${typeof ad.device_impressions}`);
        console.log(`- device_clicks type: ${typeof ad.device_clicks}`);
        console.log(`- device_daily_impressions type: ${typeof ad.device_daily_impressions}`);
        
        // Initialize Maps if they don't exist or aren't the right type
        if (!ad.device_impressions || !(ad.device_impressions instanceof Map)) {
            console.log('- Initializing device_impressions Map');
            ad.device_impressions = new Map();
            ad.markModified('device_impressions');
        }
        
        if (!ad.device_clicks || !(ad.device_clicks instanceof Map)) {
            console.log('- Initializing device_clicks Map');
            ad.device_clicks = new Map();
            ad.markModified('device_clicks');
        }
        
        if (!ad.device_daily_impressions || !(ad.device_daily_impressions instanceof Map)) {
            console.log('- Initializing device_daily_impressions Map');
            ad.device_daily_impressions = new Map();
            ad.markModified('device_daily_impressions');
        }
        
        // Save the initialized maps
        await ad.save();
        console.log('- Maps initialized and saved');

        // Record impressions
        console.log(`\nRecording ${TEST_IMPRESSIONS} impressions for device ${TEST_DEVICE_ID}...`);
        for (let i = 0; i < TEST_IMPRESSIONS; i++) {
            try {
                await ad.recordImpression(TEST_DEVICE_ID);
                console.log(`  - Recorded impression ${i+1}`);
            } catch (error) {
                console.error(`  - Error recording impression ${i+1}:`, error);
                console.error(error.stack);
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
                console.error(error.stack);
            }
        }

        // Reload ad from database to verify changes
        const updatedAd = await SponsoredAd.findById(ad._id);
        console.log('\nVerification:');
        console.log(`- Updated impression count: ${updatedAd.impression_count} (Expected: ${ad.impression_count + TEST_IMPRESSIONS})`);
        console.log(`- Updated click count: ${updatedAd.click_count} (Expected: ${ad.click_count + TEST_CLICKS})`);
        
        // Check if Maps exist and are the correct type
        console.log('- Map verification:');
        console.log(`  * device_impressions type: ${typeof updatedAd.device_impressions}`);
        console.log(`  * device_impressions is Map: ${updatedAd.device_impressions instanceof Map}`);
        console.log(`  * device_clicks type: ${typeof updatedAd.device_clicks}`);
        console.log(`  * device_clicks is Map: ${updatedAd.device_clicks instanceof Map}`);
        console.log(`  * device_daily_impressions type: ${typeof updatedAd.device_daily_impressions}`);
        console.log(`  * device_daily_impressions is Map: ${updatedAd.device_daily_impressions instanceof Map}`);
        
        // Try-catch for safely accessing Map data
        try {
            // Check device-specific tracking
            const deviceImpressions = updatedAd.device_impressions.get(TEST_DEVICE_ID) || 0;
            const deviceClicks = updatedAd.device_clicks.get(TEST_DEVICE_ID) || 0;
            console.log(`- Device impressions: ${deviceImpressions} (Expected: ${TEST_IMPRESSIONS})`);
            console.log(`- Device clicks: ${deviceClicks} (Expected: ${TEST_CLICKS})`);
            
            // Check daily impressions
            const today = new Date().toISOString().split('T')[0];
            // Access device_daily_impressions as an Object, not a Map
            const dailyImpressions = updatedAd.device_daily_impressions[TEST_DEVICE_ID] || {};
            
            console.log('- Daily impressions for device:');
            console.log(JSON.stringify(dailyImpressions, null, 2));
            
            const todayImpressions = dailyImpressions[today] || 0;
            console.log(`- Today's impressions (${today}): ${todayImpressions} (Expected: ${TEST_IMPRESSIONS})`);
            
            // Verify counts match expected values
            if (deviceImpressions !== TEST_IMPRESSIONS) {
                console.error(`❌ Error: Device impressions count ${deviceImpressions} doesn't match expected ${TEST_IMPRESSIONS}`);
            } else {
                console.log('✅ Device impressions count verified');
            }
            
            if (deviceClicks !== TEST_CLICKS) {
                console.error(`❌ Error: Device clicks count ${deviceClicks} doesn't match expected ${TEST_CLICKS}`);
            } else {
                console.log('✅ Device clicks count verified');
            }
            
            if (todayImpressions !== TEST_IMPRESSIONS) {
                console.error(`❌ Error: Today's impressions count ${todayImpressions} doesn't match expected ${TEST_IMPRESSIONS}`);
            } else {
                console.log('✅ Today\'s impressions count verified');
            }
        } catch (error) {
            console.error('Error accessing Map data:', error);
            console.error(error.stack);
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
                        
                        // Verify metrics data is as expected
                        const expectedTotalImpressions = TEST_IMPRESSIONS;
                        const expectedDailyImpressions = TEST_IMPRESSIONS;
                        
                        if (testAdInResults.metrics.device_impressions !== expectedTotalImpressions) {
                            console.error(`❌ Error: Metrics total impressions ${testAdInResults.metrics.device_impressions} doesn't match expected ${expectedTotalImpressions}`);
                        } else {
                            console.log('✅ Metrics total impressions verified');
                        }
                        
                        if (testAdInResults.metrics.device_daily_impressions !== expectedDailyImpressions) {
                            console.error(`❌ Error: Metrics daily impressions ${testAdInResults.metrics.device_daily_impressions} doesn't match expected ${expectedDailyImpressions}`);
                        } else {
                            console.log('✅ Metrics daily impressions verified');
                        }
                    } else {
                        console.error('❌ Error: Metrics object not found in test ad');
                    }
                } else {
                    console.log('- Test ad NOT found in active ads (may be filtered due to frequency capping)');
                    
                    // Check ad frequency capping settings
                    console.log(`- Ad frequency cap: ${updatedAd.frequency_cap}`);
                    console.log(`- Ad daily frequency cap: ${updatedAd.daily_frequency_cap}`);
                    console.log(`- Recorded impressions: ${TEST_IMPRESSIONS}`);
                    
                    // Verify this is the expected behavior
                    if (updatedAd.frequency_cap > 0 && updatedAd.frequency_cap <= TEST_IMPRESSIONS) {
                        console.log('✅ Correctly filtered out due to total frequency cap');
                    } else if (updatedAd.daily_frequency_cap > 0 && updatedAd.daily_frequency_cap <= TEST_IMPRESSIONS) {
                        console.log('✅ Correctly filtered out due to daily frequency cap');
                    } else {
                        console.error(`❌ Error: Ad should not be filtered by frequency capping yet`);
                    }
                }
            } else {
                console.log('- No active ads found');
            }
        } catch (error) {
            console.error('Error in getActiveAds test:', error);
            console.error(error.stack);
        }

        console.log('\nTest completed successfully!');

    } catch (error) {
        console.error('Error during test:', error);
        console.error(error.stack);
    } finally {
        try {
            // Close MongoDB connection
            await mongoose.connection.close();
            console.log('\nMongoDB connection closed');
        } catch (err) {
            console.error('Error closing MongoDB connection:', err);
            console.error(err.stack);
        }
    }
}

// Run the test
testAdTracking().then(() => {
    console.log('Test script completed.');
}).catch(error => {
    console.error('Test script failed:', error);
    console.error(error.stack);
    process.exit(1);
}); 