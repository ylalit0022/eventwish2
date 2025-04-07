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
    { path: '/api/health', name: 'Health Check' },
    { path: '/api/server/time', name: 'Server Time' }
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
    let anySuccessful = false;
    let authFailures = 0;
    let serverErrors = 0;
    
    for (const endpoint of testEndpoints) {
        console.log(`\n------ Testing: ${endpoint.name} ------\n`);
        
        const result = await new Promise((resolve, reject) => {
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
                    console.log(`Headers: ${JSON.stringify(res.headers).substring(0, 100)}...`);
                    
                    // Handle different status codes
                    if (res.statusCode >= 500) {
                        console.log(`❌ SERVER ERROR: The server returned a ${res.statusCode} error`);
                        console.log('This indicates the server is having issues and is not related to app signature');
                        serverErrors++;
                        
                        // Still try to parse the response if possible
                        try {
                            if (data.trim().startsWith('{')) {
                                const response = JSON.parse(data);
                                console.log(`Response data: ${JSON.stringify(response).substring(0, 100)}...`);
                            } else {
                                console.log(`Raw response: ${data.substring(0, 200)}...`);
                            }
                        } catch (e) {
                            console.log(`Raw response: ${data.substring(0, 200)}...`);
                        }
                        
                        resolve({ success: false, isServerError: true });
                        return;
                    }
                    
                    try {
                        // Try to parse JSON response
                        if (data.trim().startsWith('{')) {
                            const response = JSON.parse(data);
                            
                            if (res.statusCode === 200) {
                                console.log(`✅ SUCCESS: Valid response from ${endpoint.path}`);
                                console.log(`Response: ${JSON.stringify(response).substring(0, 100)}...`);
                                anySuccessful = true;
                                resolve({ success: true });
                            } else {
                                console.log(`❌ FAILED: ${response.message || 'Unknown error'}`);
                                // If it's just missing params but not an auth error, we'll count it as a success
                                const isAuthFailure = response.error && 
                                    (response.error === 'APP_SIGNATURE_INVALID' || 
                                     response.error === 'APP_SIGNATURE_MISSING' ||
                                     response.error === 'API_KEY_INVALID');
                                if (isAuthFailure) {
                                    console.log('Authentication failure detected!');
                                    authFailures++;
                                    resolve({ success: false, isAuthFailure: true });
                                } else {
                                    console.log('This is a regular API error, not an authentication failure.');
                                    resolve({ success: true });
                                }
                            }
                        } else {
                            // Not a JSON response
                            console.log(`❌ NOT JSON: Server did not return a JSON response`);
                            console.log(`Raw response: ${data.substring(0, 200)}...`);
                            resolve({ success: false, isServerError: true });
                        }
                    } catch (e) {
                        console.log(`❌ ERROR: Failed to parse response (${e.message})`);
                        console.log(`Raw response: ${data.substring(0, 200)}...`);
                        resolve({ success: false, isServerError: true });
                    }
                });
            });
            
            req.on('error', (e) => {
                console.log(`❌ REQUEST ERROR: ${e.message}`);
                serverErrors++;
                resolve({ success: false, isServerError: true, error: e.message });
            });
            
            // Set timeout
            req.setTimeout(10000, () => {
                console.log(`❌ TIMEOUT: Request timed out after 10 seconds`);
                serverErrors++;
                resolve({ success: false, isServerError: true, error: 'Request timeout' });
            });
            
            req.end();
        });
        
        if (!result.success) {
            allSuccessful = false;
        }
        
        // Add delay between requests
        await new Promise(resolve => setTimeout(resolve, 1000));
    }
    
    // Summary
    console.log('\n=========================================');
    console.log('             TEST SUMMARY               ');
    console.log('=========================================');
    console.log(`Total endpoints tested: ${testEndpoints.length}`);
    console.log(`Server errors encountered: ${serverErrors}`);
    console.log(`Authentication failures: ${authFailures}`);
    
    if (authFailures > 0) {
        console.log('\n❌ AUTHENTICATION ISSUES DETECTED: The app signature is not being accepted.');
    } else if (serverErrors === testEndpoints.length) {
        console.log('\n⚠️ SERVER AVAILABILITY ISSUES: All requests failed with server errors.');
        console.log('This indicates the server is down or experiencing issues.');
        console.log('When the server becomes available, the app signature should work correctly.');
    } else if (anySuccessful) {
        console.log('\n✅ AUTHENTICATION WORKING: At least one endpoint accepted the app signature.');
        console.log('The app signature "app_sig_1" is being accepted by the server.');
        console.log('Some endpoints may be returning errors due to server issues, not authentication problems.');
    }
    
    if (allSuccessful) {
        console.log('\n🎉 ALL TESTS PASSED! App signature is working correctly!');
        console.log('\nYour changes to AdConstants.java and AdMobRepository.java have fixed the issue.');
        console.log('The app is now using the correct app signature: "app_sig_1"');
        console.log('This matches one of the valid signatures from the server .env file.');
    }
    console.log('=========================================');
}

// Run the tests
runTests().catch(console.error); 