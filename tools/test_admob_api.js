const fetch = require('node-fetch');

// API endpoint configuration
const API_BASE_URL = 'https://eventwish2.onrender.com/api'; // Updated with correct base URL
const AD_TYPES = ['app_open', 'banner', 'interstitial', 'rewarded'];

// Headers required for authentication
const headers = {
    'x-api-key': 'ew_dev_c1ce47afeff9fa8b7b1aa165562cb915',
    'x-app-signature': 'app_sig_1',
    'x-device-id': '93e81b95f9f2c74a4b0124f784fe34327066c5c0f8a4c51c00a11a68c831b49e',
    'Content-Type': 'application/json'
};

async function testAdMobEndpoint() {
    console.log('Testing AdMob API endpoints...\n');

    for (const adType of AD_TYPES) {
        try {
            console.log(`Testing endpoint for ad type: ${adType}`);
            const url = `${API_BASE_URL}/admob/units?adType=${adType}`;
            console.log(`URL: ${url}`);
            console.log('Headers:', headers);
            
            const response = await fetch(url, { headers });
            console.log(`Status: ${response.status}`);
            
            if (!response.ok) {
                console.log(`Error: ${response.statusText}`);
                if (response.status === 401) {
                    console.log('Authentication failed. Please check your API key and other credentials.');
                }
                continue;
            }

            const data = await response.json();
            console.log('Response:');
            console.log(JSON.stringify(data, null, 2));
            
        } catch (error) {
            console.error(`Error testing ${adType}:`, error.message);
        }
        console.log('\n-------------------\n');
    }
}

// Test with no ad type parameter to see default behavior
async function testDefaultEndpoint() {
    try {
        console.log('Testing endpoint with no ad type parameter');
        const url = `${API_BASE_URL}/admob/units`;
        console.log(`URL: ${url}`);
        console.log('Headers:', headers);
        
        const response = await fetch(url, { headers });
        console.log(`Status: ${response.status}`);
        
        if (!response.ok) {
            console.log(`Error: ${response.statusText}`);
            if (response.status === 401) {
                console.log('Authentication failed. Please check your API key and other credentials.');
            }
            return;
        }

        const data = await response.json();
        console.log('Response:');
        console.log(JSON.stringify(data, null, 2));
        
    } catch (error) {
        console.error('Error testing default endpoint:', error.message);
    }
}

// Run tests
console.log('Starting API tests...\n');
testAdMobEndpoint()
    .then(() => testDefaultEndpoint())
    .then(() => console.log('\nAPI tests completed'))
    .catch(error => console.error('Test execution failed:', error)); 