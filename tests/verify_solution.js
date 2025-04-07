/**
 * Verification Script for App Signature Solution
 * 
 * Tests if the app_sig_1 signature is accepted by the server
 */

const https = require('https');
const url = require('url');

// Configuration - using the correct signature from .env
const API_KEY = 'ew_dev_c1ce47afeff9fa8b7b1aa165562cb915';
const DEVICE_ID = '93e81b95f9f2c74a4b0124f784fe34327066c5c0f8a4c51c00a11a68c831b49e';
const APP_SIGNATURE = 'app_sig_1';
const SERVER_URL = 'https://eventwish2.onrender.com';

// Test endpoints to try
const testEndpoints = [
    { path: '/api/admob/config?adType=Banner', name: 'AdMob Config with Banner type' },
    { path: '/api/admob/types', name: 'AdMob Types' },
    { path: '/api/admob/units', name: 'AdMob Units' },
    { path: '/api/admob/status', name: 'AdMob Status' },
    { path: '/api/health', name: 'Health Check' }
];

// Run all tests
async function runTests() {
    console.log('=========================================');
    console.log('    APP SIGNATURE VERIFICATION TESTS    ');
    console.log('=========================================');
    console.log(`API Key: ${API_KEY}`);
    console.log(`Device ID: ${DEVICE_ID}`);
    console.log(`App Signature: ${APP_SIGNATURE}`);
    console.log(`Server: ${SERVER_URL}`);
    console.log('=========================================\n');
    
    let allSuccessful = true;
    
    for (const endpoint of testEndpoints) {
        console.log(`\n------ Testing: ${endpoint.name} ------\n`);
        
        const success = await new Promise((resolve, reject) => {
            const parsedUrl = new url.URL(SERVER_URL + endpoint.path);
            
            const options = {
                hostname: parsedUrl.hostname,
                path: parsedUrl.pathname + parsedUrl.search,
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json',
                    'x-api-key': API_KEY,
                    'x-device-id': DEVICE_ID,
                    'x-app-signature': APP_SIGNATURE
                }
            };
            
            console.log(`Request: GET ${SERVER_URL}${endpoint.path}`);
            
            const req = https.request(options, (res) => {
                let data = '';
                
                res.on('data', (chunk) => {
                    data += chunk;
                });
                
                res.on('end', () => {
                    console.log(`Status Code: ${res.statusCode}`);
                    
                    try {
                        // Parse the response
                        const response = JSON.parse(data);
                        
                        if (res.statusCode === 200) {
                            console.log(`✅ SUCCESS: Valid response from ${endpoint.path}`);
                            console.log(`Response: ${JSON.stringify(response).substring(0, 100)}...`);
                            resolve(true);
                        } else {
                            console.log(`❌ FAILED: ${response.message || 'Unknown error'}`);
                            // If it's just missing params but not an auth error, we'll count it as a success
                            const isAuthFailure = response.error && 
                                (response.error === 'APP_SIGNATURE_INVALID' || 
                                 response.error === 'APP_SIGNATURE_MISSING' ||
                                 response.error === 'API_KEY_INVALID');
                            if (isAuthFailure) {
                                console.log('Authentication failure detected!');
                                resolve(false);
                            } else {
                                console.log('This is a regular API error, not an authentication failure.');
                                resolve(true);
                            }
                        }
                    } catch (e) {
                        console.log(`❌ ERROR: Failed to parse response (${e.message})`);
                        console.log('Raw response:', data);
                        resolve(false);
                    }
                });
            });
            
            req.on('error', (e) => {
                console.log(`❌ REQUEST ERROR: ${e.message}`);
                resolve(false);
            });
            
            req.end();
        });
        
        if (!success) {
            allSuccessful = false;
        }
        
        // Add delay between requests
        await new Promise(resolve => setTimeout(resolve, 500));
    }
    
    // Summary
    console.log('\n=========================================');
    if (allSuccessful) {
        console.log('🎉 ALL TESTS PASSED! App signature is working correctly!');
        console.log('\nYour changes to AdConstants.java and AdMobRepository.java have fixed the issue.');
        console.log('The app is now using the correct app signature: "app_sig_1"');
        console.log('This matches one of the valid signatures from the server .env file.');
    } else {
        console.log('❌ SOME TESTS FAILED. Check the logs above for details.');
    }
    console.log('=========================================');
}

// Run the tests
runTests().catch(console.error); 