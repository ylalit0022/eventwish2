/**
 * Test user sessions endpoint
 */

console.log('🔍 Testing User Sessions Endpoint...');

const https = require('https');

const uid = "DgCSTkbsJwfSO0NTRuq6XVXLB3u2";

const options = {
    hostname: 'eventwish2.onrender.com',
    port: 443,
    path: `/api/users/${uid}/sessions`,
    method: 'GET',
    headers: {
        'Content-Type': 'application/json',
        'Authorization': 'Bearer dev-token'
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
            console.log('\n🔍 Sessions Analysis:');
            
            if (response.success && response.sessions) {
                console.log(`📱 Total Active Sessions: ${response.totalSessions}`);
                console.log(`🕒 Last Online: ${response.lastOnline}`);
                console.log(`📲 Current Device: ${response.currentDeviceId}`);
                
                response.sessions.forEach((session, index) => {
                    console.log(`\n📱 Session ${index + 1}:`);
                    console.log(`  Device ID: ${session.deviceId}`);
                    console.log(`  Model: ${session.deviceModel}`);
                    console.log(`  Name: ${session.deviceName}`);
                    console.log(`  App Version: ${session.appVersion}`);
                    console.log(`  OS: ${session.osVersion}`);
                    console.log(`  Login Time: ${session.loginTimestamp}`);
                    console.log(`  Last Active: ${session.lastActiveTimestamp}`);
                    console.log(`  Is Current: ${session.isCurrentDevice ? '✅' : '❌'}`);
                });
            }
        } catch (e) {
            console.log('⚠️  Could not parse JSON response');
        }
        
        console.log('\n✅ Sessions test completed.');
    });
});

req.on('error', (e) => {
    console.error('❌ Request error:', e.message);
});

req.end(); 