/**
 * Test script to verify our API-based solution
 * 
 * This script tests the new force-update route for sponsored ads.
 */

const axios = require('axios');

// The problematic ad ID
const AD_ID = '6816472563201ab63462c8bb';

// Create API client
const api = axios.create({
  baseURL: 'http://localhost:3001/api',
  headers: {
    'Content-Type': 'application/json',
    'Authorization': 'Bearer dev-token',
    'X-Dev-Email': 'ylalit0022@gmail.com',
    'X-Dev-Admin': 'true'
  }
});

async function testApiSolution() {
  try {
    console.log(`Fetching sponsored ad with ID: ${AD_ID}`);
    
    // First, get the current ad data
    const getResponse = await api.get(`/admin/sponsored-ads/${AD_ID}`);
    const currentAd = getResponse.data.sponsoredAd;
    
    console.log('Current ad data:');
    console.log('Start date:', currentAd.start_date);
    console.log('End date:', currentAd.end_date);
    
    // Create update payload
    const updatePayload = {
      ...currentAd,
      title: 'Updated Title ' + new Date().toISOString()
    };
    
    // Try to update the ad using the force-update route
    console.log('\nAttempting to update ad using force-update route...');
    const updateResponse = await api.put(`/admin/sponsored-ads/${AD_ID}/force-update`, updatePayload);
    
    console.log('\nUpdate successful!');
    console.log('Response:', updateResponse.data);
    
  } catch (error) {
    console.error('\nError updating sponsored ad:');
    if (error.response) {
      console.error('Status:', error.response.status);
      console.error('Error data:', error.response.data);
    } else {
      console.error('Error:', error.message);
    }
  }
}

// Run the test
testApiSolution().catch(err => {
  console.error('Test script error:', err);
}); 