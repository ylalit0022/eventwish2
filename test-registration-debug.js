/**
 * Test script to debug registration 400 errors
 */

const https = require('https');

// Test data similar to what the Android app would send
const testData = {
    uid: "DgCSTkbsJwfSO0NTRuq6XVXLB3u2",
    deviceId: "f6be900a58176718",
    deviceModel: "SM-G973F",
    deviceName: "Samsung Galaxy S10",
    appVersion: "1.0.0",
    osVersion: "Android 13",
    displayName: "Test User",
    email: "test@example.com"
};

console.log('🔍 Testing Registration Endpoint\n');

// Test 1: Check if Firebase token validation is the issue
console.log('1. Testing with development token...');

const postData = JSON.stringify(testData);

const options = {
    hostname: 'eventwish2.onrender.com',
    port: 443,
    path: '/api/users/register',
    method: 'POST',
    headers: {
        'Content-Type': 'application/json',
        'Authorization': 'Bearer dev-token',
        'x-dev-email': 'admin@eventwish.com',
        'x-dev-admin': 'true',
        'Content-Length': Buffer.byteLength(postData)
    }
};

const req = https.request(options, (res) => {
    console.log(`Status Code: ${res.statusCode}`);
    console.log(`Headers:`, res.headers);
    
    let data = '';
    
    res.on('data', (chunk) => {
        data += chunk;
    });
    
    res.on('end', () => {
        console.log('Response Body:', data);
        
        try {
            const response = JSON.parse(data);
            console.log('\n📊 Analysis:');
            
            if (res.statusCode === 400) {
                console.log('❌ 400 Bad Request - Validation Error');
                console.log('Possible causes:');
                console.log('- Firebase UID validation failed');
                console.log('- Missing required fields');
                console.log('- Invalid data format');
                
                if (response.message) {
                    console.log(`Error message: ${response.message}`);
                }
                
                if (response.errors) {
                    console.log(`Validation errors: ${JSON.stringify(response.errors)}`);
                }
            } else if (res.statusCode === 401) {
                console.log('❌ 401 Unauthorized - Firebase Token Issue');
                console.log('The Firebase token verification is failing');
            } else if (res.statusCode === 500) {
                console.log('❌ 500 Internal Server Error - Server Issue');
                console.log('There\'s a server-side error during processing');
            } else if (res.statusCode === 200 || res.statusCode === 201) {
                console.log('✅ Registration successful!');
                console.log('The endpoint is working correctly');
            }
            
        } catch (e) {
            console.log('Failed to parse JSON response:', e.message);
        }
        
        console.log('\n📋 Next Steps:');
        console.log('1. Check the enhanced server logs for detailed error information');
        console.log('2. Verify Firebase token format in Android app');
        console.log('3. Ensure all required fields are being sent');
        console.log('4. Check if SKIP_AUTH is properly configured');
    });
});

req.on('error', (e) => {
    console.error(`Request error: ${e.message}`);
});

// Write data to request body
req.write(postData);
req.end();

console.log('Test request sent...\n'); 