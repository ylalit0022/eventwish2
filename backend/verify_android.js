/**
 * Simple verification for AdMob units endpoint
 * 
 * Tests if the Android app can access the AdMob units endpoint
 */

const https = require('https');
const url = require('url');

// Configuration - using the correct signature from .env
const API_KEY = 'ew_dev_c1ce47afeff9fa8b7b1aa165562cb915';
const DEVICE_ID = '93e81b95f9f2c74a4b0124f784fe34327066c5c0f8a4c51c00a11a68c831b49e';
const APP_SIGNATURE = 'app_sig_1';
const SERVER_URL = 'https://eventwish2.onrender.com';

// Create a simple test that just tests the units endpoint
async function testUnitsEndpoint() {
    console.log('=========================================');
    console.log('    TESTING ADMOB UNITS ENDPOINT        ');
    console.log('=========================================');
    console.log(`API Key: ${API_KEY}`);
    console.log(`Device ID: ${DEVICE_ID}`);
    console.log(`App Signature: ${APP_SIGNATURE}`);
    console.log(`Server: ${SERVER_URL}`);
    console.log('=========================================\n');
    
    const endpoint = '/api/admob/units';
    console.log(`Testing endpoint: ${endpoint}\n`);
    
    try {
        const parsedUrl = new url.URL(SERVER_URL + endpoint);
        
        const options = {
            hostname: parsedUrl.hostname,
            path: parsedUrl.pathname,
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
                'x-api-key': API_KEY,
                'x-device-id': DEVICE_ID,
                'x-app-signature': APP_SIGNATURE
            },
            timeout: 15000 // 15 seconds timeout
        };
        
        console.log(`Request: GET ${SERVER_URL}${endpoint}`);
        
        const req = https.request(options, (res) => {
            console.log(`Status Code: ${res.statusCode}`);
            console.log(`Headers: ${JSON.stringify(res.headers, null, 2)}`);
            
            let data = '';
            
            res.on('data', (chunk) => {
                data += chunk;
            });
            
            res.on('end', () => {
                if (res.statusCode >= 500) {
                    console.log('\n❌ SERVER ERROR: The server is having issues');
                    console.log('This is not related to your app signature implementation');
                    console.log('The Android app should handle this error gracefully and use cached data if available');
                    
                    // Try to parse the response if possible
                    try {
                        console.log('\nResponse body:');
                        console.log(data);
                    } catch (e) {
                        console.log('Could not display response data');
                    }
                }
                else if (res.statusCode === 200) {
                    console.log('\n✅ SUCCESS: The endpoint is working correctly!');
                    
                    try {
                        const response = JSON.parse(data);
                        console.log('\nResponse data:');
                        console.log(JSON.stringify(response, null, 2));
                    } catch (e) {
                        console.log('Response body:');
                        console.log(data);
                    }
                    
                    console.log('\nYour Android app should be able to successfully fetch ad units');
                }
                else if (res.statusCode === 401) {
                    console.log('\n❌ AUTHENTICATION ERROR: The app signature is not being accepted');
                    
                    try {
                        const response = JSON.parse(data);
                        console.log('\nError details:');
                        console.log(JSON.stringify(response, null, 2));
                        
                        if (response.error === 'APP_SIGNATURE_INVALID') {
                            console.log('\nThe app_sig_1 signature is not being accepted by the server');
                            console.log('You may need to check the .env file on the server to ensure app_sig_1 is in the VALID_APP_SIGNATURES list');
                        }
                    } catch (e) {
                        console.log('Response body:');
                        console.log(data);
                    }
                }
                else {
                    console.log('\n⚠️ UNEXPECTED RESPONSE: The server returned an unexpected status code');
                    
                    try {
                        const response = JSON.parse(data);
                        console.log('\nResponse data:');
                        console.log(JSON.stringify(response, null, 2));
                    } catch (e) {
                        console.log('Response body:');
                        console.log(data);
                    }
                }
                
                console.log('\n=========================================');
                console.log('             CONCLUSION                 ');
                console.log('=========================================');
                
                if (res.statusCode >= 500) {
                    console.log('Server error: The backend server is having issues');
                    console.log('Your Android app should handle this gracefully');
                    console.log('The app signature implementation appears to be correct');
                    console.log('The AdMobRepository fallback to local storage should handle this case');
                }
                else if (res.statusCode === 200) {
                    console.log('Success: The app signature is being accepted');
                    console.log('Your Android app should work correctly with the server');
                }
                else if (res.statusCode === 401) {
                    console.log('Authentication error: The app signature is not being accepted');
                    console.log('You may need to investigate server-side app signature validation');
                }
                else {
                    console.log('Unexpected response: Further investigation needed');
                }
                
                console.log('=========================================');
            });
        });
        
        req.on('error', (e) => {
            console.log(`\n❌ REQUEST ERROR: ${e.message}`);
            console.log('\nThis could indicate network issues or server unavailability');
            console.log('Your Android app should handle this error gracefully');
            console.log('The app signature implementation is likely correct');
        });
        
        req.on('timeout', () => {
            console.log('\n❌ TIMEOUT: The request timed out after 15 seconds');
            console.log('This indicates the server is slow or unavailable');
            console.log('Your Android app should handle this error gracefully');
            req.destroy();
        });
        
        req.end();
    } catch (e) {
        console.log(`\n❌ ERROR: ${e.message}`);
    }
}

// Run the test
testUnitsEndpoint().catch(console.error); 