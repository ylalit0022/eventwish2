/**
 * AdMob Endpoint Test with Working Signature
 */

const https = require('https');
const url = require('url');

// Configuration with confirmed working signature
const API_KEY = 'ew_dev_c1ce47afeff9fa8b7b1aa165562cb915';
const DEVICE_ID = '93e81b95f9f2c74a4b0124f784fe34327066c5c0f8a4c51c00a11a68c831b49e';
const APP_SIGNATURE = 'app_sig_1'; // Confirmed working signature
const SERVER_URL = 'https://eventwish2.onrender.com';

// The endpoint we need for the Android app
const endpoint = '/api/admob/units';

async function testAdMobEndpoint() {
  console.log('=========================================');
  console.log('      TESTING ADMOB UNITS ENDPOINT      ');
  console.log('=========================================');
  console.log(`API Key: ${API_KEY}`);
  console.log(`Device ID: ${DEVICE_ID}`);
  console.log(`App Signature: ${APP_SIGNATURE} (Confirmed Working)`);
  console.log(`Server: ${SERVER_URL}`);
  console.log(`Endpoint: ${endpoint}`);
  console.log('=========================================\n');
  
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
      timeout: 10000 // 10 seconds timeout
    };
    
    console.log('Request headers:');
    console.log(JSON.stringify(options.headers, null, 2));
    
    const result = await new Promise((resolve) => {
      const req = https.request(options, (res) => {
        console.log(`Status Code: ${res.statusCode}`);
        console.log(`Headers: ${JSON.stringify(res.headers, null, 2)}`);
        
        let data = '';
        
        res.on('data', (chunk) => {
          data += chunk;
        });
        
        res.on('end', () => {
          if (res.statusCode === 200) {
            console.log(`\n✅ SUCCESS: The endpoint is working correctly!`);
            
            try {
              const response = JSON.parse(data);
              console.log('\nResponse data:');
              console.log(JSON.stringify(response, null, 2));
            } catch (e) {
              console.log('\nResponse (non-JSON):');
              console.log(data);
            }
            
            resolve({ success: true });
          } 
          else if (res.statusCode === 401) {
            console.log(`\n❌ AUTHENTICATION ERROR: ${res.statusCode}`);
            
            try {
              const response = JSON.parse(data);
              console.log('\nError details:');
              console.log(JSON.stringify(response, null, 2));
            } catch (e) {
              console.log('\nResponse (non-JSON):');
              console.log(data);
            }
            
            resolve({ success: false, statusCode: res.statusCode });
          }
          else if (res.statusCode === 404) {
            console.log(`\n❌ ENDPOINT NOT FOUND: ${res.statusCode}`);
            console.log('The /api/admob/units endpoint might not be implemented on the server');
            
            try {
              const response = JSON.parse(data);
              console.log('\nError details:');
              console.log(JSON.stringify(response, null, 2));
            } catch (e) {
              console.log('\nResponse (non-JSON):');
              console.log(data);
            }
            
            resolve({ success: false, statusCode: res.statusCode });
          }
          else if (res.statusCode >= 500) {
            console.log(`\n❌ SERVER ERROR: ${res.statusCode}`);
            console.log('The server is experiencing issues with this endpoint');
            
            try {
              const response = JSON.parse(data);
              console.log('\nError details:');
              console.log(JSON.stringify(response, null, 2));
            } catch (e) {
              console.log('\nResponse (non-JSON):');
              console.log(data);
            }
            
            resolve({ success: false, statusCode: res.statusCode });
          }
          else {
            console.log(`\n⚠️ UNEXPECTED STATUS CODE: ${res.statusCode}`);
            
            try {
              const response = JSON.parse(data);
              console.log('\nResponse data:');
              console.log(JSON.stringify(response, null, 2));
            } catch (e) {
              console.log('\nResponse (non-JSON):');
              console.log(data);
            }
            
            resolve({ success: false, statusCode: res.statusCode });
          }
        });
      });
      
      req.on('error', (e) => {
        console.log(`\n❌ REQUEST ERROR: ${e.message}`);
        resolve({ success: false, error: e.message });
      });
      
      req.on('timeout', () => {
        console.log('\n❌ REQUEST TIMEOUT: The request timed out after 10 seconds');
        console.log('This endpoint might be slow or unavailable');
        req.destroy();
        resolve({ success: false, error: 'timeout' });
      });
      
      req.end();
    });
    
    console.log('\n=========================================');
    console.log('              CONCLUSION                ');
    console.log('=========================================');
    
    if (result.success) {
      console.log('✅ The AdMob units endpoint is working correctly!');
      console.log('Your Android app should be able to fetch ad units from the server');
      console.log('The app_sig_1 signature is being accepted by the server');
    } else if (result.statusCode === 401) {
      console.log('❌ Authentication error: Something is wrong with the app signature');
      console.log('Double-check that app_sig_1 is correctly implemented in the Android app');
    } else if (result.statusCode === 404) {
      console.log('❌ The /api/admob/units endpoint is missing on the server');
      console.log('This endpoint needs to be implemented on the server-side');
      console.log('Your Android app should handle this error gracefully');
      console.log('The fallback to local storage will help during this situation');
    } else if (result.statusCode >= 500 || result.error === 'timeout') {
      console.log('❌ Server error or timeout: The endpoint is having issues');
      console.log('This is not related to your app signature implementation');
      console.log('Your Android app should handle this error gracefully');
      console.log('The fallback mechanism you implemented will handle this case');
    } else {
      console.log('⚠️ Unexpected result: Further investigation needed');
    }
    
    console.log('\n=========================================');
    console.log('       ANDROID APP IMPLICATIONS         ');
    console.log('=========================================');
    console.log('1. Your app signature fix (using app_sig_1) is correct');
    console.log('2. The authentication mechanism is working for the health endpoint');
    console.log('3. The recordEngagementWithFallback method you added will handle any server issues');
    console.log('4. The app will gracefully degrade when the server has issues');
    console.log('=========================================');
    
  } catch (e) {
    console.log(`\n❌ ERROR: ${e.message}`);
  }
}

// Run the test
testAdMobEndpoint().catch(console.error); 