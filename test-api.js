const axios = require('axios');

// Configuration
const config = {
    firebaseApiKey: 'AIzaSyDRXTiDWClDkwqWUjHyg49z0UW5R8p81qs',
    testUser: {
        email: 'test@eventwish.com',
        password: 'test123'
    },
    userId: 'DgCSTkbsJwfSO0NTRuq6XVXLB3u2',
    deviceId: 'f6be900a58176718',
    baseUrl: 'https://eventwish2.onrender.com'
};

// Get Firebase Auth Token
async function getFirebaseToken() {
    try {
        const response = await axios.post(
            `https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=${config.firebaseApiKey}`,
            {
                email: config.testUser.email,
                password: config.testUser.password,
                returnSecureToken: true
            }
        );
        return response.data.idToken;
    } catch (error) {
        console.error('Error getting Firebase token:', error.response?.data || error.message);
        throw error;
    }
}

// Get Multi-section Feed
async function getMultiSectionFeed(token) {
    try {
        const response = await axios.get(
            `${config.baseUrl}/api/feed`,
            {
                params: {
                    include: 'personalized,trending,fresh,categories',
                    refresh: false
                },
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'x-firebase-uid': config.userId,
                    'x-device-id': config.deviceId
                }
            }
        );
        return response.data;
    } catch (error) {
        console.error('Error getting feed:', error.response?.data || error.message);
        throw error;
    }
}

// Get Default Feed (no auth required)
async function getDefaultFeed() {
    try {
        const response = await axios.get(
            `${config.baseUrl}/api/feed/default`,
            {
                params: {
                    include: 'trending,fresh'
                }
            }
        );
        return response.data;
    } catch (error) {
        console.error('Error getting default feed:', error.response?.data || error.message);
        throw error;
    }
}

// Main test function
async function testApi() {
    console.log('🚀 Starting API test...\n');

    try {
        // 1. Get Firebase Token
        console.log('Getting Firebase token...');
        const token = await getFirebaseToken();
        console.log('✅ Got Firebase token\n');

        // 2. Get Default Feed (no auth required)
        console.log('Getting default feed...');
        const defaultFeed = await getDefaultFeed();
        console.log('✅ Got default feed:');
        console.log(JSON.stringify(defaultFeed, null, 2), '\n');

        // 3. Get Multi-section Feed (with auth)
        console.log('Getting multi-section feed...');
        const feed = await getMultiSectionFeed(token);
        console.log('✅ Got multi-section feed:');
        console.log(JSON.stringify(feed, null, 2), '\n');

    } catch (error) {
        console.error('❌ Test failed:', error.message);
    }
}

// Run the test
testApi(); 