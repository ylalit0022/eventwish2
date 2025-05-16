/**
 * Test script to verify daily impressions handling in SponsoredAd model
 * 
 * This script focuses specifically on testing:
 * 1. Proper initialization of device_daily_impressions as an Object
 * 2. Correct recording of daily impressions
 * 3. Proper retrieval of daily impressions in getActiveAds
 */

const mongoose = require('mongoose');
const SponsoredAd = require('../models/SponsoredAd');
require('dotenv').config();

// Test constants
const TEST_DEVICE_ID = `test-device-${Date.now()}`;
const TEST_IMPRESSIONS = 3;

async function connectToMongoDB() {
    console.log(`Connecting to MongoDB: ${process.env.MONGODB_URI || 'mongodb://localhost:27017/eventwish_test'}`);
    await mongoose.connect(process.env.MONGODB_URI, {
        useNewUrlParser: true,
        useUnifiedTopology: true
    });
    console.log('Connected to MongoDB');
}

async function getOrCreateTestAd() {
    // Find one active ad to test with
    const now = new Date();
    const ad = await SponsoredAd.findOne({
        status: true,
        start_date: { $lte: now },
        end_date: { $gte: now }
    });

    if (!ad) {
        console.log('No active ads found. Creating a test ad...');
        // Create a test ad
        const newAd = new SponsoredAd({
            title: 'Test Daily Impressions Ad',
            image_url: 'https://example.com/ad.jpg',
            redirect_url: 'https://example.com',
            start_date: new Date(),
            end_date: new Date(Date.now() + 30 * 24 * 60 * 60 * 1000), // 30 days from now
            location: 'home_top',
            priority: 5,
            frequency_cap: 10,
            daily_frequency_cap: 5,
            description: 'Test ad for daily impressions tracking',
            status: true
        });
        
        await newAd.save();
        console.log(`Created test ad with ID: ${newAd._id}`);
        return newAd;
    }
    
    console.log(`Using existing ad: ${ad.title} (ID: ${ad._id})`);
    return ad;
}

async function testDailyImpressions() {
    try {
        await connectToMongoDB();
        
        // Get or create test ad
        const ad = await getOrCreateTestAd();
        
        // Check the initial state of device_daily_impressions
        console.log('\nChecking initial state:');
        console.log(`- device_daily_impressions type: ${typeof ad.device_daily_impressions}`);
        if (typeof ad.device_daily_impressions === 'object') {
            console.log(`- device_daily_impressions is Map: ${ad.device_daily_impressions instanceof Map}`);
        }
        console.log(`- device_daily_impressions content:`, ad.device_daily_impressions);
        
        // Reset/initialize device_daily_impressions to an empty object
        ad.device_daily_impressions = {};
        ad.markModified('device_daily_impressions');
        await ad.save();
        console.log('\nReset device_daily_impressions to empty object');
        
        // Record impressions
        console.log(`\nRecording ${TEST_IMPRESSIONS} impressions for device ${TEST_DEVICE_ID}...`);
        for (let i = 0; i < TEST_IMPRESSIONS; i++) {
            await ad.recordImpression(TEST_DEVICE_ID);
            console.log(`- Recorded impression ${i+1}`);
        }
        
        // Retrieve ad directly from database to verify changes
        const updatedAd = await SponsoredAd.findById(ad._id);
        
        // Check structure of device_daily_impressions
        console.log('\nVerifying device_daily_impressions structure:');
        console.log(`- Type: ${typeof updatedAd.device_daily_impressions}`);
        console.log(`- Content:`, updatedAd.device_daily_impressions);
        
        // Extract and check daily impressions for today
        const today = new Date().toISOString().split('T')[0];
        console.log(`\nVerifying daily impressions for today (${today}):`);
        
        if (updatedAd.device_daily_impressions) {
            const deviceData = updatedAd.device_daily_impressions[TEST_DEVICE_ID];
            console.log(`- Device data:`, deviceData);
            
            if (deviceData) {
                const todayImpressions = deviceData[today];
                console.log(`- Today's impressions: ${todayImpressions}`);
                
                if (todayImpressions === TEST_IMPRESSIONS) {
                    console.log('✅ Daily impressions correctly recorded');
                } else {
                    console.log(`❌ Daily impressions count ${todayImpressions} doesn't match expected ${TEST_IMPRESSIONS}`);
                }
            } else {
                console.log(`❌ No data found for device ${TEST_DEVICE_ID}`);
            }
        } else {
            console.log('❌ device_daily_impressions is empty or undefined');
        }
        
        // Test getActiveAds to verify daily impressions retrieval
        console.log('\nTesting getActiveAds for daily impressions retrieval:');
        const activeAds = await SponsoredAd.getActiveAds(null, TEST_DEVICE_ID);
        
        if (activeAds && activeAds.length > 0) {
            console.log(`- Found ${activeAds.length} active ads`);
            
            const testAdInResults = activeAds.find(a => a._id.toString() === updatedAd._id.toString());
            if (testAdInResults) {
                console.log('- Test ad found in active ads');
                console.log('- Metrics:');
                
                if (testAdInResults.metrics) {
                    console.log(`  * Today's impressions: ${testAdInResults.metrics.device_daily_impressions}`);
                    
                    if (testAdInResults.metrics.device_daily_impressions === TEST_IMPRESSIONS) {
                        console.log('✅ Daily impressions correctly retrieved in getActiveAds');
                    } else {
                        console.log(`❌ Daily impressions in metrics (${testAdInResults.metrics.device_daily_impressions}) doesn't match expected (${TEST_IMPRESSIONS})`);
                    }
                } else {
                    console.log('❌ No metrics found in getActiveAds result');
                }
            } else {
                console.log('❌ Test ad not found in active ads');
            }
        } else {
            console.log('❌ No active ads found');
        }
        
        console.log('\nTest completed!');
        
    } catch (error) {
        console.error('Error during test:', error);
        console.error(error.stack);
    } finally {
        await mongoose.connection.close();
        console.log('MongoDB connection closed');
    }
}

// Run the test
testDailyImpressions().then(() => {
    console.log('Test script completed.');
}).catch(error => {
    console.error('Test script failed:', error);
    console.error(error.stack);
    process.exit(1);
}); 