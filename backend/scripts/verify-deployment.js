const axios = require('axios');

const BASE_URL = 'https://eventwish2.onrender.com/api';
const TEST_DEVICE_ID = 'test-deployment-verification';

async function verifyDeployment() {
  try {
    console.log('Verifying API deployment...');
    
    // 1. Check server time endpoint
    console.log('\n1. Testing server time endpoint...');
    const timeResponse = await axios.get(`${BASE_URL}/server/time`);
    console.log(`✅ Server time: ${timeResponse.data.formatted}`);
    
    // 2. Test user registration endpoint
    console.log('\n2. Testing user registration endpoint...');
    const registerResponse = await axios.post(`${BASE_URL}/users/register`, {
      deviceId: TEST_DEVICE_ID
    });
    console.log(`✅ Registration endpoint: ${registerResponse.status}`);
    
    // 3. Test getting user by device ID
    console.log('\n3. Testing get user endpoint...');
    const getUserResponse = await axios.get(`${BASE_URL}/users/${TEST_DEVICE_ID}`);
    console.log(`✅ Get user endpoint: ${getUserResponse.status}`);
    
    // 4. Test the new recommendations endpoint
    console.log('\n4. Testing recommendations endpoint...');
    const recommendationsResponse = await axios.get(`${BASE_URL}/users/${TEST_DEVICE_ID}/recommendations`);
    console.log(`✅ Recommendations endpoint: ${recommendationsResponse.status}`);
    
    console.log('\n🎉 Deployment verification successful! All endpoints are working.');
  } catch (error) {
    console.error('\n❌ Deployment verification failed!');
    
    if (error.response) {
      console.error(`Status: ${error.response.status}`);
      console.error('Response:', JSON.stringify(error.response.data, null, 2));
    } else if (error.request) {
      console.error('No response received. The server might be down or still deploying.');
    } else {
      console.error('Error:', error.message);
    }
    
    process.exit(1);
  }
}

// Allow time for the deployment to complete
console.log('Waiting 10 seconds before testing endpoints...');
setTimeout(verifyDeployment, 10000); 