/**
 * Script to test User API endpoints
 * 
 * Usage: node test-user-api.js
 */

const axios = require('axios');
const { v4: uuidv4 } = require('uuid');

// Configuration
const API_BASE_URL = process.env.API_BASE_URL || 'http://localhost:3000/api';
const DEVICE_ID = process.argv[2] || `test-device-${uuidv4().substring(0, 8)}`;

// Log configuration
console.log('Testing User API with:');
console.log('Base URL:', API_BASE_URL);
console.log('Device ID:', DEVICE_ID);
console.log('-------------------');

// Function to make API requests
async function makeRequest(method, endpoint, data = null) {
    try {
        const url = `${API_BASE_URL}${endpoint}`;
        console.log(`${method} ${url}`);
        
        if (data) {
            console.log('Request data:', JSON.stringify(data, null, 2));
        }
        
        const response = await axios({
            method,
            url,
            data,
            headers: {
                'Content-Type': 'application/json'
            }
        });
        
        console.log(`Status: ${response.status}`);
        console.log('Response:', JSON.stringify(response.data, null, 2));
        return response.data;
    } catch (error) {
        console.error('Error:', error.message);
        if (error.response) {
            console.error('Response data:', error.response.data);
            console.error('Status:', error.response.status);
        }
        return null;
    }
}

// Main function to run tests
async function runTests() {
    console.log('\n1. Register user');
    await makeRequest('post', '/users/register', { deviceId: DEVICE_ID });
    
    console.log('\n2. Get user data');
    await makeRequest('get', `/users/${DEVICE_ID}`);
    
    console.log('\n3. Update user activity without category');
    await makeRequest('put', '/users/activity', { deviceId: DEVICE_ID });
    
    console.log('\n4. Update user activity with category');
    await makeRequest('put', '/users/activity', { 
        deviceId: DEVICE_ID,
        category: 'birthday',
        source: 'direct'
    });
    
    console.log('\n5. Record template view');
    await makeRequest('put', '/users/template-view', {
        deviceId: DEVICE_ID,
        templateId: 'template-123',
        category: 'birthday'
    });
    
    console.log('\n6. Get updated user data');
    await makeRequest('get', `/users/${DEVICE_ID}`);
}

// Run the tests
runTests()
    .then(() => console.log('\nTests completed'))
    .catch(err => console.error('\nTest failed:', err.message)); 