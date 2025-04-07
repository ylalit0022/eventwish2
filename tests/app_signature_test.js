/**
 * App Signature Test Script
 * 
 * This script tests different signature formats with the AdMob server
 * to determine which signature format is expected.
 */

const https = require('https');
const crypto = require('crypto');

// Configuration
const API_KEY = 'ew_dev_c1ce47afeff9fa8b7b1aa165562cb915';
const DEVICE_ID = '93e81b95f9f2c74a4b0124f784fe34327066c5c0f8a4c51c00a11a68c831b49e';
const APP_PACKAGE = 'com.ds.eventwish';
const SECRET_KEY = 'eventwish_secret_key_v1';
const SERVER_URL = 'eventwish2.onrender.com';
const ENDPOINT = '/api/admob/units';

// Generate all possible signature formats
function generateSignatureFormats() {
    const signatures = [];
    
    // Format 1: The API key itself 
    signatures.push({
        name: 'API key as signature',
        value: API_KEY
    });
    
    // Format 2: app_sig_1 from .env
    signatures.push({
        name: 'app_sig_1 from .env',
        value: 'app_sig_1'
    });
    
    // Format 3: app_sig_2 from .env
    signatures.push({
        name: 'app_sig_2 from .env',
        value: 'app_sig_2'
    });

    // Format 4: Plain text format from .env
    signatures.push({
        name: 'Plain text format - VALID_APP_SIGNATURES',
        value: 'ew_dev_signature'
    });
    
    // Format 5: HMAC-SHA256 of API_KEY:DEVICE_ID using SECRET_KEY
    const hmacData = `${API_KEY}:${DEVICE_ID}`;
    const hmacSignature = crypto.createHmac('sha256', SECRET_KEY)
        .update(hmacData)
        .digest('hex');
    signatures.push({
        name: 'HMAC-SHA256 of API_KEY:DEVICE_ID',
        value: hmacSignature
    });
    
    // Format 6: API_KEY:APP_PACKAGE
    signatures.push({
        name: 'API_KEY:APP_PACKAGE',
        value: `${API_KEY}:${APP_PACKAGE}`
    });
    
    // Format 7: SHA-256 hash of API_KEY:APP_PACKAGE:SECRET_KEY (Base64 encoded)
    const hashData = `${API_KEY}:${APP_PACKAGE}:${SECRET_KEY}`;
    const hashSignature = crypto.createHash('sha256')
        .update(hashData)
        .digest('base64');
    signatures.push({
        name: 'SHA-256 of API_KEY:APP_PACKAGE:SECRET_KEY (Base64)',
        value: hashSignature
    });
    
    // Format 8: SHA-256 hash of API_KEY:APP_PACKAGE:SECRET_KEY (hex encoded)
    const hashSignatureHex = crypto.createHash('sha256')
        .update(hashData)
        .digest('hex');
    signatures.push({
        name: 'SHA-256 of API_KEY:APP_PACKAGE:SECRET_KEY (Hex)',
        value: hashSignatureHex
    });
    
    // Format 9: API_KEY:test:DEVICE_ID
    signatures.push({
        name: 'API_KEY:test:DEVICE_ID',
        value: `${API_KEY}:test:${DEVICE_ID}`
    });
    
    // Format 10: SHA-256 hash of API_KEY:test:DEVICE_ID (Base64 encoded)
    const testModeData = `${API_KEY}:test:${DEVICE_ID}`;
    const testModeSignature = crypto.createHash('sha256')
        .update(testModeData)
        .digest('base64');
    signatures.push({
        name: 'SHA-256 of API_KEY:test:DEVICE_ID (Base64)',
        value: testModeSignature
    });

    // Format 11: Just the word "test_signature"
    signatures.push({
        name: 'test_signature',
        value: 'test_signature'
    });

    return signatures;
}

// Test a single signature format
function testSignature(signature, index, total) {
    return new Promise((resolve, reject) => {
        console.log(`[${index+1}/${total}] Testing signature: ${signature.name}`);
        
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
                    const response = JSON.parse(data);
                    const result = {
                        name: signature.name,
                        value: signature.value,
                        statusCode: res.statusCode,
                        success: response.success,
                        message: response.message || null,
                        error: response.error || null
                    };
                    
                    if (res.statusCode === 200 && response.success) {
                        console.log(`  ✅ SUCCESS: Signature accepted!`);
                        console.log(`  Value: ${signature.value}`);
                    } else {
                        console.log(`  ❌ FAILED: ${response.message || 'Unknown error'}`);
                    }
                    
                    resolve(result);
                } catch (e) {
                    console.log(`  ❌ ERROR parsing response: ${e.message}`);
                    resolve({
                        name: signature.name,
                        value: signature.value,
                        statusCode: res.statusCode,
                        success: false,
                        message: 'Error parsing response',
                        error: e.message
                    });
                }
            });
        });
        
        req.on('error', (e) => {
            console.log(`  ❌ REQUEST ERROR: ${e.message}`);
            resolve({
                name: signature.name,
                value: signature.value,
                statusCode: 0,
                success: false,
                message: 'Request error',
                error: e.message
            });
        });
        
        req.end();
    });
}

// Test all signatures and report results
async function testAllSignatures() {
    console.log('=== App Signature Test ===');
    console.log(`API Key: ${API_KEY}`);
    console.log(`Device ID: ${DEVICE_ID}`);
    console.log(`Server: ${SERVER_URL}${ENDPOINT}`);
    console.log('========================\n');
    
    const signatures = generateSignatureFormats();
    const results = [];
    
    for (let i = 0; i < signatures.length; i++) {
        const result = await testSignature(signatures[i], i, signatures.length);
        results.push(result);
        
        // Add small delay between requests to avoid rate limiting
        await new Promise(resolve => setTimeout(resolve, 500));
    }
    
    console.log('\n=== Results Summary ===');
    const successful = results.filter(r => r.success);
    
    if (successful.length > 0) {
        console.log(`\n✅ SUCCESSFUL SIGNATURES (${successful.length}):`);
        successful.forEach(r => {
            console.log(`- ${r.name}: ${r.value}`);
        });
    } else {
        console.log('\n❌ NO SUCCESSFUL SIGNATURES FOUND');
    }
    
    // Generate curl commands for the successful signatures
    if (successful.length > 0) {
        console.log('\n=== CURL Commands for Successful Signatures ===');
        successful.forEach(r => {
            const curlCmd = `curl -X GET "https://${SERVER_URL}${ENDPOINT}" \\
  -H "Content-Type: application/json" \\
  -H "x-api-key: ${API_KEY}" \\
  -H "x-device-id: ${DEVICE_ID}" \\
  -H "x-app-signature: ${r.value}"`;
            
            console.log(`\n${curlCmd}\n`);
        });
    }
    
    return results;
}

// Run the tests
testAllSignatures()
    .then(() => console.log('Testing complete!'))
    .catch(err => console.error('Error running tests:', err)); 