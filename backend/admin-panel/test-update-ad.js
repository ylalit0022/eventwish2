/**
 * Test script to update a sponsored ad with properly formatted dates
 * 
 * This script attempts to update an existing sponsored ad with dates
 * formatted to ensure the end_date is after start_date validation passes.
 */

const axios = require('axios');

// Replace with your actual ad ID
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

// Format dates properly
function formatDates(startDateStr, endDateStr) {
  // Parse dates
  const startDate = new Date(startDateStr);
  const endDate = new Date(endDateStr);
  
  // Ensure end date is at least one day after start date
  if (endDate <= startDate) {
    // If end date is not after start date, set it to one day after
    endDate.setDate(startDate.getDate() + 1);
  }
  
  return {
    start_date: startDate.toISOString(),
    end_date: endDate.toISOString()
  };
}

async function testUpdateAd() {
  try {
    console.log(`Fetching sponsored ad with ID: ${AD_ID}`);
    
    // First, get the current ad data
    const getResponse = await api.get(`/admin/sponsored-ads/${AD_ID}`);
    const currentAd = getResponse.data.sponsoredAd;
    
    console.log('Current ad data:', currentAd);
    
    // Create update payload with properly formatted dates
    const updatePayload = {
      ...currentAd,
      // Use the same dates, but ensure they're properly formatted
      ...formatDates(currentAd.start_date, currentAd.end_date)
    };
    
    console.log('\nUpdate payload dates:');
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
testUpdateAd().catch(err => {
  console.error('Test script error:', err);
}); 