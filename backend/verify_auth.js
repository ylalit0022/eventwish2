/**
 * Auth Verification Script
 * Tests authentication with the proper app signature
 */

const https = require('https');
const url = require('url');

// Configuration 
const API_KEY = 'ew_dev_c1ce47afeff9fa8b7b1aa165562cb915';
const DEVICE_ID = '93e81b95f9f2c74a4b0124f784fe34327066c5c0f8a4c51c00a11a68c831b49e';
const SERVER_URL = 'https://eventwish2.onrender.com';

// Try with these app signatures
const signatures = [
  "app_sig_1",
  "app_sig_2", 
  "ew_dev_c1ce47afeff9fa8b7b1aa165562cb915",
  "93e81b95f9f2c74a4b0124f784fe34327066c5c0f8a4c51c00a11a68c831b49e",
  "com.ds.eventwish",
  `${API_KEY}:com.ds.eventwish`,
  `${API_KEY}:${DEVICE_ID}`
];

// Test a simple endpoint
const endpoint = '/api/health';

async function testAuth() {
  console.log('=========================================');
  console.log('    TESTING APP SIGNATURE AUTH            ');
  console.log('=========================================');
  console.log(`API Key: ${API_KEY}`);
  console.log(`Device ID: ${DEVICE_ID}`);
  console.log(`Server: ${SERVER_URL}`);
  console.log(`Endpoint: ${endpoint}`);
  console.log('=========================================\n');
  
  let successFound = false;
  
  for (const signature of signatures) {
    console.log(`\n----- Testing with signature: "${signature}" -----\n`);
    
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
          'x-app-signature': signature
        },
        timeout: 8000 // 8 seconds timeout
      };
      
      console.log('Request headers:');
      console.log(JSON.stringify(options.headers, null, 2));
      
      const result = await new Promise((resolve) => {
        const req = https.request(options, (res) => {
          console.log(`Status Code: ${res.statusCode}`);
          
          let data = '';
          
          res.on('data', (chunk) => {
            data += chunk;
          });
          
          res.on('end', () => {
            if (res.statusCode === 200) {
              console.log(`✅ SUCCESS with signature: ${signature}`);
              successFound = true;
              
              try {
                const response = JSON.parse(data);
                console.log('Response:', JSON.stringify(response, null, 2).substring(0, 200));
              } catch (e) {
                console.log('Response (non-JSON):', data.substring(0, 200));
              }
              
              resolve({ success: true, signature });
            } 
            else if (res.statusCode === 401) {
              console.log(`❌ AUTH FAILED with signature: ${signature}`);
              
              try {
                const response = JSON.parse(data);
                console.log('Error response:', JSON.stringify(response, null, 2));
                
                if (response.error === 'APP_SIGNATURE_INVALID') {
                  console.log('The signature was rejected as invalid');
                } else if (response.error === 'APP_SIGNATURE_MISSING') {
                  console.log('The signature was not detected in the request');
                }
              } catch (e) {
                console.log('Response (non-JSON):', data);
              }
              
              resolve({ success: false, signature });
            }
            else {
              console.log(`⚠️ Got status ${res.statusCode} with signature: ${signature}`);
              
              try {
                const response = JSON.parse(data);
                console.log('Response:', JSON.stringify(response, null, 2).substring(0, 200));
              } catch (e) {
                console.log('Response (non-JSON):', data.substring(0, 200));
              }
              
              resolve({ success: false, statusCode: res.statusCode, signature });
            }
          });
        });
        
        req.on('error', (e) => {
          console.log(`❌ REQUEST ERROR: ${e.message}`);
          resolve({ success: false, error: e.message, signature });
        });
        
        req.on('timeout', () => {
          console.log('❌ REQUEST TIMEOUT');
          req.destroy();
          resolve({ success: false, error: 'timeout', signature });
        });
        
        req.end();
      });
      
      if (result.success) {
        console.log(`\n✅ FOUND WORKING SIGNATURE: "${signature}"`);
        break;
      }
      
    } catch (e) {
      console.log(`Error testing signature ${signature}:`, e.message);
    }
    
    // Add delay between requests
    await new Promise(resolve => setTimeout(resolve, 500));
  }
  
  console.log('\n=========================================');
  console.log('          AUTH TEST RESULTS            ');
  console.log('=========================================');
  
  if (successFound) {
    console.log('✅ Authentication test succeeded with at least one signature');
    console.log('\nUpdate your app code to use the working signature');
  } else {
    console.log('❌ All tested signatures failed');
    console.log('\nPossible issues:');
    console.log('1. The server may be down or unavailable');
    console.log('2. The API key may be incorrect');
    console.log('3. The authorization mechanism may have changed');
    console.log('4. Try testing with a local server instance');
  }
  
  console.log('\n=========================================');
}

// Run the tests
testAuth().catch(console.error); 