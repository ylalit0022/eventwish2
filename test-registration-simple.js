/**
 * Simple registration test script
 */

console.log('🔍 Testing Registration Endpoint...');

const https = require('https');

// Test data
const testData = {
    uid: "DgCSTkbsJwfSO0NTRuq6XVXLB3u2",
    deviceId: "f6be900a58176718",
    deviceModel: "SM-G973F",
    appVersion: "1.0.0"
};

console.log('📤 Sending test data:', JSON.stringify(testData, null, 2));

const postData = JSON.stringify(testData);

const options = {
    hostname: 'eventwish2.onrender.com',
    port: 443,
    path: '/api/users/register',
    method: 'POST',
    headers: {
        'Content-Type': 'application/json',
        'Authorization': 'Bearer dev-token',
        'Content-Length': Buffer.byteLength(postData)
    }
};

console.log('🌐 Making request to:', `https://${options.hostname}${options.path}`);

const req = https.request(options, (res) => {
    console.log('\n📊 Response Status:', res.statusCode);
    
    let data = '';
    
    res.on('data', (chunk) => {
        data += chunk;
    });
    
    res.on('end', () => {
        console.log('\n📄 Response Body:');
        console.log(data);
        
        try {
            const response = JSON.parse(data);
            console.log('\n🔍 Analysis:');
            
            if (res.statusCode === 400) {
                console.log('❌ 400 Bad Request');
                if (response.message) {
                    console.log('Error:', response.message);
                }
                if (response.errors) {
                    console.log('Validation errors:', response.errors);
                }
            } else if (res.statusCode === 401) {
                console.log('❌ 401 Unauthorized - Firebase token issue');
            } else if (res.statusCode === 500) {
                console.log('❌ 500 Server Error');
            } else if (res.statusCode === 200 || res.statusCode === 201) {
                console.log('✅ Success!');
            }
        } catch (e) {
            console.log('⚠️  Could not parse JSON response');
        }
        
        console.log('\n✅ Test completed.');
    });
});

req.on('error', (e) => {
    console.error('❌ Request error:', e.message);
});

req.write(postData);
req.end(); 