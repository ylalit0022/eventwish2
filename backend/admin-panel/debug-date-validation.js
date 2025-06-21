/**
 * Debug script to test date validation with the SponsoredAd model
 * 
 * This script tests different date formatting approaches to ensure
 * the end_date is always after start_date in the SponsoredAd model.
 */

const axios = require('axios');

// Test sponsored ad data
const testData = {
  uid: '60f1a5b5b5b5b5b5b5b5b5b5', // Placeholder user ID
  title: 'Test Sponsored Ad',
  description: 'Test description',
  image_url: 'https://example.com/image.jpg',
  redirect_url: 'https://example.com',
  location: 'home_top',
  priority: 5,
  status: true
};

// Different date formatting approaches to test
const dateTests = [
  {
    name: 'Same day with time components',
    start_date: new Date('2023-10-15T00:00:00.000Z'),
    end_date: new Date('2023-10-15T23:59:59.999Z')
  },
  {
    name: 'Different days',
    start_date: new Date('2023-10-15'),
    end_date: new Date('2023-10-16')
  },
  {
    name: 'ISO string format',
    start_date: new Date('2023-10-15').toISOString(),
    end_date: new Date('2023-10-16').toISOString()
  },
  {
    name: 'Start with time at beginning, end with time at end',
    start_date: (() => {
      const date = new Date('2023-10-15');
      date.setHours(0, 0, 0, 0);
      return date.toISOString();
    })(),
    end_date: (() => {
      const date = new Date('2023-10-15');
      date.setHours(23, 59, 59, 999);
      return date.toISOString();
    })()
  }
];

// Create API client with proper error handling
const api = axios.create({
  baseURL: 'http://localhost:3001/api',
  headers: {
    'Content-Type': 'application/json',
    'Authorization': 'Bearer dev-token',
    'X-Dev-Email': 'ylalit0022@gmail.com',
    'X-Dev-Admin': 'true'
  }
});

// Test each date format
async function testDateFormats() {
  console.log('Starting date validation tests...\n');
  
  for (const test of dateTests) {
    console.log(`\n=== Testing: ${test.name} ===`);
    console.log(`Start date: ${test.start_date}`);
    console.log(`End date: ${test.end_date}`);
    
    const testPayload = {
      ...testData,
      start_date: test.start_date,
      end_date: test.end_date
    };
    
    try {
      console.log('Sending request...');
      const response = await api.post('/admin/sponsored-ads', testPayload);
      console.log('SUCCESS! Server accepted the dates');
      console.log('Response:', response.data);
      
      // Clean up - delete the created ad
      if (response.data && response.data.success && response.data.sponsoredAd && response.data.sponsoredAd._id) {
        try {
          await api.delete(`/admin/sponsored-ads/${response.data.sponsoredAd._id}`);
          console.log(`Deleted test ad with ID: ${response.data.sponsoredAd._id}`);
        } catch (deleteErr) {
          console.error('Error deleting test ad:', deleteErr.message);
        }
      }
    } catch (error) {
      console.log('FAILED! Server rejected the dates');
      if (error.response) {
        console.log('Status:', error.response.status);
        console.log('Error data:', error.response.data);
      } else {
        console.log('Error:', error.message);
      }
    }
  }
  
  console.log('\nAll tests completed.');
}

// Run the tests
testDateFormats().catch(err => {
  console.error('Test script error:', err);
}); 