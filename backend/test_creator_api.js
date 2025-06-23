const axios = require('axios');

const BASE_URL = 'http://localhost:3001/api';

async function testCreatorAPI() {
    console.log('🧪 Testing Creator Profile API Endpoints');
    console.log('==========================================');

    // Test 1: Single template creator profile (non-existent template)
    console.log('\n1. Testing single template creator profile (non-existent):');
    try {
        const response = await axios.get(`${BASE_URL}/templates/507f1f77bcf86cd799439011/creator`);
        console.log('✅ Response:', response.data);
    } catch (error) {
        console.log('❌ Error:', error.response?.data || error.message);
    }

    // Test 2: Batch creator profiles
    console.log('\n2. Testing batch creator profiles:');
    try {
        const response = await axios.post(`${BASE_URL}/templates/creators/batch`, {
            templateIds: ['507f1f77bcf86cd799439011', '507f1f77bcf86cd799439012']
        });
        console.log('✅ Response:', JSON.stringify(response.data, null, 2));
    } catch (error) {
        console.log('❌ Error:', error.response?.data || error.message);
    }

    // Test 3: Invalid template ID
    console.log('\n3. Testing invalid template ID:');
    try {
        const response = await axios.get(`${BASE_URL}/templates/invalid-id/creator`);
        console.log('✅ Response:', response.data);
    } catch (error) {
        console.log('❌ Error:', error.response?.data || error.message);
    }

    // Test 4: Creator statistics (non-existent user)
    console.log('\n4. Testing creator statistics (non-existent user):');
    try {
        const response = await axios.get(`${BASE_URL}/templates/creators/507f1f77bcf86cd799439011/stats`);
        console.log('✅ Response:', response.data);
    } catch (error) {
        console.log('❌ Error:', error.response?.data || error.message);
    }

    // Test 5: Batch with empty array
    console.log('\n5. Testing batch with empty array:');
    try {
        const response = await axios.post(`${BASE_URL}/templates/creators/batch`, {
            templateIds: []
        });
        console.log('✅ Response:', response.data);
    } catch (error) {
        console.log('❌ Error:', error.response?.data || error.message);
    }

    // Test 6: Batch with invalid IDs
    console.log('\n6. Testing batch with invalid IDs:');
    try {
        const response = await axios.post(`${BASE_URL}/templates/creators/batch`, {
            templateIds: ['invalid-id', 'another-invalid']
        });
        console.log('✅ Response:', response.data);
    } catch (error) {
        console.log('❌ Error:', error.response?.data || error.message);
    }

    console.log('\n🏁 Testing completed!');
}

// Run the tests
testCreatorAPI().catch(console.error); 