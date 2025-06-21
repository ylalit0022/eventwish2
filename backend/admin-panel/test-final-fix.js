/**
 * Test script to verify the final date validation fix
 * 
 * This script attempts to update the problematic sponsored ad
 * by setting the end date to be 2 days after the start date.
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

async function testFinalFix() {
  try {
    console.log(`Fetching sponsored ad with ID: ${AD_ID}`);
    
    // First, get the current ad data
    const getResponse = await api.get(`/admin/sponsored-ads/${AD_ID}`);
    const currentAd = getResponse.data.sponsoredAd;
    
    console.log('Current ad data:');
    console.log('Start date:', currentAd.start_date);
    console.log('End date:', currentAd.end_date);
    
    // Parse the start date
    const startDate = new Date(currentAd.start_date);
    
    // Set end date to 2 days after start date
    const endDate = new Date(startDate);
    endDate.setDate(startDate.getDate() + 2);
    
    // Create update payload with fixed dates
    const updatePayload = {
      ...currentAd,
      start_date: startDate.toISOString(),
      end_date: endDate.toISOString()
    };
    
    console.log('\nUpdate payload with fixed dates:');
    console.log('Start date:', updatePayload.start_date);
    console.log('End date:', updatePayload.end_date);
    
    // Try to update the ad
    console.log('\nAttempting to update ad...');
    const updateResponse = await api.put(`/admin/sponsored-ads/${AD_ID}`, updatePayload);
    
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
testFinalFix().catch(err => {
  console.error('Test script error:', err);
}); 