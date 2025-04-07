/**
 * Simple App Signature Test Script
 * 
 * This script tests signature formats with the AdMob server
 * to determine which signature format is expected.
 */

const https = require('https');

// Configuration
const API_KEY = 'ew_dev_c1ce47afeff9fa8b7b1aa165562cb915';
const DEVICE_ID = '93e81b95f9f2c74a4b0124f784fe34327066c5c0f8a4c51c00a11a68c831b49e';
const SERVER_URL = 'eventwish2.onrender.com';
const ENDPOINT = '/api/admob/units';

// Test signatures
const signatures = [
    { name: 'app_sig_1', value: 'app_sig_1' },
    { name: 'app_sig_2', value: 'app_sig_2' },
    { name: 'API key itself', value: API_KEY },
    { name: 'test_signature', value: 'test_signature' },
    { name: 'ew_dev_signature', value: 'ew_dev_signature' },
    { name: 'eventwish_dev_signature', value: 'eventwish_dev_signature' }
];

// Test a single signature
function testSignature(signature) {
    return new Promise((resolve, reject) => {
        console.log(`Testing: ${signature.name} = "${signature.value}"`);
        
        const options = {
            hostname: SERVER_URL,
            path: ENDPOINT,
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
                'x-api-key': API_KEY,
                'x-device-id': DEVICE_ID,
                'x-app-signature': signature.value
            }
        };
        
        const req = https.request(options, (res) => {
            let data = '';
            
            res.on('data', (chunk) => {
                data += chunk;
            });
            
            res.on('end', () => {
                console.log(`  Status: ${res.statusCode}`);
                try {
                    // Try to parse as JSON
                    const response = JSON.parse(data);
                    
                    if (res.statusCode === 200 && response.success === true) {
                        console.log(`  ✅ SUCCESS: Signature accepted!`);
                        
                        // Update Android code
                        const javaCode = `
// In AdConstants.java, update the DEV_SIGNATURE constant:
public static final String DEV_SIGNATURE = "${signature.value}";

// In AdMobRepository.java, simplify the prepareHeaders method to use this constant`;
                        
                        console.log(javaCode);
                    } else {
                        console.log(`  ❌ FAILED: ${response.message || 'Unknown error'}`);
                    }
                } catch (e) {
                    // Not JSON, probably HTML
                    console.log(`  ❌ ERROR: Response not JSON (${e.message})`);
                    if (data.indexOf("DOCTYPE") >= 0) {
                        console.log("  Received HTML instead of JSON - endpoint might not exist");
                    }
                }
                resolve();
            });
        });
        
        req.on('error', (e) => {
            console.log(`  ❌ REQUEST ERROR: ${e.message}`);
            resolve();
        });
        
        req.end();
    });
}

// Test without signature to see error format
async function testWithoutSignature() {
    console.log('=== Testing Without App Signature ===');
    
    return new Promise((resolve, reject) => {
        const options = {
            hostname: SERVER_URL,
            path: ENDPOINT,
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
                'x-api-key': API_KEY,
                'x-device-id': DEVICE_ID
            }
        };
        
        const req = https.request(options, (res) => {
            let data = '';
            
            res.on('data', (chunk) => {
                data += chunk;
            });
            
            res.on('end', () => {
                console.log(`  Status: ${res.statusCode}`);
                try {
                    const response = JSON.parse(data);
                    console.log(`  Error message: ${response.message}`);
                    console.log(`  Error code: ${response.error}`);
                } catch (e) {
                    console.log(`  Response is not valid JSON: ${e.message}`);
                }
                resolve();
            });
        });
        
        req.on('error', (e) => {
            console.log(`  REQUEST ERROR: ${e.message}`);
            resolve();
        });
        
        req.end();
    });
}

// Main function
async function main() {
    console.log('=== App Signature Test ===');
    console.log(`API Key: ${API_KEY}`);
    console.log(`Device ID: ${DEVICE_ID}`);
    console.log(`Server: ${SERVER_URL}${ENDPOINT}`);
    console.log('========================\n');
    
    // Test without signature first
    await testWithoutSignature();
    
    console.log('\n=== Testing Signatures ===');
    
    for (const signature of signatures) {
        await testSignature(signature);
        // Delay between requests
        await new Promise(resolve => setTimeout(resolve, 500));
    }
    
    console.log('\n=== Testing Complete ===');
    console.log('If no signatures worked, please try these solutions:');
    console.log('1. Check if VALID_APP_SIGNATURES is correctly set in your backend .env file');
    console.log('2. Add "ew_dev_c1ce47afeff9fa8b7b1aa165562cb915" to VALID_APP_SIGNATURES');
    console.log('3. Try updating your Android app to use "app_sig_1" as the signature');
}

// Run the main function
main().catch(console.error); 