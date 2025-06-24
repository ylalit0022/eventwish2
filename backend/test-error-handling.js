#!/usr/bin/env node

/**
 * Comprehensive Error Handling & Validation Test Suite
 * Tests the enhanced error handling system implemented in Phase 12
 */

const http = require('http');

// Test configuration
const config = {
    baseUrl: 'http://localhost:3001',
    timeout: 5000
};

// Test results tracking
const results = {
    passed: 0,
    failed: 0,
    total: 0
};

/**
 * Make HTTP request with promise
 */
function makeRequest(options, postData = null) {
    return new Promise((resolve, reject) => {
        const req = http.request(options, (res) => {
            let data = '';
            
            res.on('data', (chunk) => {
                data += chunk;
            });
            
            res.on('end', () => {
                try {
                    const response = JSON.parse(data);
                    resolve({
                        statusCode: res.statusCode,
                        headers: res.headers,
                        body: response
                    });
                } catch (error) {
                    resolve({
                        statusCode: res.statusCode,
                        headers: res.headers,
                        body: data
                    });
                }
            });
        });
        
        req.on('error', (error) => {
            reject(error);
        });
        
        req.setTimeout(config.timeout, () => {
            req.abort();
            reject(new Error('Request timeout'));
        });
        
        if (postData) {
            req.write(postData);
        }
        
        req.end();
    });
}

/**
 * Test helper function
 */
async function runTest(testName, testFunction) {
    results.total++;
    
    try {
        console.log(`\n🧪 Testing: ${testName}`);
        const result = await testFunction();
        
        if (result.success) {
            results.passed++;
            console.log(`✅ PASSED: ${testName}`);
        } else {
            results.failed++;
            console.log(`❌ FAILED: ${testName}`);
            console.log(`   Reason: ${result.reason}`);
        }
        
    } catch (error) {
        results.failed++;
        console.log(`❌ ERROR in ${testName}: ${error.message}`);
    }
}

/**
 * Test 1: Invalid ObjectId Handling
 */
async function testInvalidObjectId() {
    const options = {
        hostname: 'localhost',
        port: 3001,
        path: '/api/templates/core/content/invalid-id',
        method: 'GET'
    };
    
    try {
        const response = await makeRequest(options);
        
        if (response.statusCode === 400 && 
            response.body.success === false &&
            response.body.error &&
            response.body.error.code === 'VALIDATION_ERROR') {
            return {
                success: true
            };
        } else {
            return {
                success: false,
                reason: `Expected 400 validation error, got ${response.statusCode}`
            };
        }
    } catch (error) {
        return {
            success: false,
            reason: `Request failed: ${error.message}`
        };
    }
}

/**
 * Test 2: Route Not Found (404)
 */
async function testRouteNotFound() {
    const options = {
        hostname: 'localhost',
        port: 3001,
        path: '/api/nonexistent/route',
        method: 'GET'
    };
    
    try {
        const response = await makeRequest(options);
        
        if (response.statusCode === 404 && 
            response.body.success === false &&
            response.body.error &&
            response.body.error.code === 'NOT_FOUND_ERROR') {
            return {
                success: true
            };
        } else {
            return {
                success: false,
                reason: `Expected 404 not found, got ${response.statusCode}`
            };
        }
    } catch (error) {
        return {
            success: false,
            reason: `Request failed: ${error.message}`
        };
    }
}

/**
 * Test 3: Server Health Check
 */
async function testServerHealth() {
    const options = {
        hostname: 'localhost',
        port: 3001,
        path: '/api/health',
        method: 'GET'
    };
    
    try {
        const response = await makeRequest(options);
        
        if (response.statusCode === 200) {
            return {
                success: true
            };
        } else {
            return {
                success: false,
                reason: `Server health check failed with status ${response.statusCode}`
            };
        }
    } catch (error) {
        return {
            success: false,
            reason: `Health check failed: ${error.message}`
        };
    }
}

/**
 * Run all tests
 */
async function runAllTests() {
    console.log('🚀 Starting Enhanced Error Handling Test Suite');
    console.log(`📡 Testing against: ${config.baseUrl}`);
    console.log('=' .repeat(50));
    
    // Run all tests
    await runTest('Server Health Check', testServerHealth);
    await runTest('Invalid ObjectId Handling', testInvalidObjectId);
    await runTest('Route Not Found (404)', testRouteNotFound);
    
    // Print summary
    console.log('\n' + '=' .repeat(50));
    console.log('📊 TEST SUMMARY');
    console.log('=' .repeat(50));
    console.log(`Total Tests: ${results.total}`);
    console.log(`✅ Passed: ${results.passed}`);
    console.log(`❌ Failed: ${results.failed}`);
    console.log(`📈 Success Rate: ${((results.passed / results.total) * 100).toFixed(1)}%`);
    
    if (results.passed === results.total) {
        console.log('\n✅ All tests passed! Error handling system is working correctly.');
    } else {
        console.log('\n⚠️  Some tests failed. Please check the error handling implementation.');
    }
}

// Run tests
runAllTests().catch(error => {
    console.error('❌ Test suite failed to run:', error.message);
    process.exit(1);
}); 