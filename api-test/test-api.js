import fetch from 'node-fetch';

// Configuration
const config = {
    // Your actual user ID and device ID
    userId: 'DgCSTkbsJwfSO0NTRuq6XVXLB3u2',
    deviceId: 'f6be900a58176718',
    baseUrl: 'https://eventwish2.onrender.com'
};

// Get Multi-section Feed
async function getMultiSectionFeed(token) {
    try {
        console.log('Headers being sent:', {
            'Authorization': `Bearer ${token}`,
            'x-firebase-uid': config.userId,
            'x-device-id': config.deviceId
        });
        
        const response = await fetch(
            `${config.baseUrl}/api/feed?include=personalized,trending,fresh,categories&refresh=false`,
            {
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'x-firebase-uid': config.userId,
                    'x-device-id': config.deviceId
                }
            }
        );
        const data = await response.json();
        if (!response.ok) {
            throw new Error(data.error?.message || 'Failed to get feed');
        }
        return data;
    } catch (error) {
        console.error('Error getting feed:', error.message);
        throw error;
    }
}

// Get Default Feed (no auth required)
async function getDefaultFeed() {
    try {
        const response = await fetch(
            `${config.baseUrl}/api/feed/default?include=trending,fresh`
        );
        const data = await response.json();
        if (!response.ok) {
            throw new Error(data.error?.message || 'Failed to get default feed');
        }
        return data;
    } catch (error) {
        console.error('Error getting default feed:', error.message);
        throw error;
    }
}

// Main test function
async function testApi(idToken) {
    console.log('🚀 Starting API test...\n');

    try {
        // 1. Get Default Feed (no auth required)
        console.log('Getting default feed...');
        const defaultFeed = await getDefaultFeed();
        console.log('✅ Got default feed:');
        console.log(JSON.stringify(defaultFeed, null, 2), '\n');

        // 2. Get Multi-section Feed (with auth)
        console.log('Getting multi-section feed...');
        const feed = await getMultiSectionFeed(idToken);
        console.log('✅ Got multi-section feed:');
        console.log(JSON.stringify(feed, null, 2), '\n');

    } catch (error) {
        console.error('❌ Test failed:', error.message);
    }
}

// Check if token is provided as command line argument
const idToken = process.argv[2];
if (!idToken) {
    console.error('Please provide your Firebase ID token as a command line argument:');
    console.error('npm test -- YOUR_ID_TOKEN');
    process.exit(1);
}

// Run the test with the provided token
testApi(idToken); 