/**
 * Script to create a test sponsored ad with valid dates
 */

const axios = require('axios');

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

// Get a valid user ID
async function getValidUserId() {
  try {
    const response = await api.get('/admin/users?limit=1');
    if (response.data.users && response.data.users.length > 0) {
      return response.data.users[0]._id;
    }
    return null;
  } catch (error) {
    console.error('Error fetching users:', error);
    return null;
  }
}

async function createTestAd() {
  try {
    // Get a valid user ID
    const userId = await getValidUserId();
    if (!userId) {
      console.error('Could not find a valid user ID');
      return;
    }
    
    console.log('Using user ID:', userId);
    
    // Create test ad with future dates
    const startDate = new Date();
    startDate.setDate(startDate.getDate() + 1); // Tomorrow
    
    const endDate = new Date();
    endDate.setDate(endDate.getDate() + 10); // 10 days from now
    
    const testAd = {
      uid: userId,
      title: 'Test Ad',
      description: 'Test description',
      image_url: 'https://example.com/image.jpg',
      redirect_url: 'https://example.com',
      location: 'home_top',
      priority: 5,
      status: true,
      start_date: startDate.toISOString(),
      end_date: endDate.toISOString()
    };
    
    console.log('Creating test ad with dates:');
    console.log('Start date:', testAd.start_date);
    console.log('End date:', testAd.end_date);
    
    const response = await api.post('/admin/sponsored-ads', testAd);
    
    console.log('Ad created successfully!');
    console.log('Response:', response.data);
    
    // Now try to update the ad
    const createdAd = response.data.sponsoredAd;
    
    console.log('\nNow updating the ad...');
    const updateResponse = await api.put(`/admin/sponsored-ads/${createdAd.id}`, {
      ...createdAd,
      title: 'Updated Test Ad'
    });
    
    console.log('Update successful!');
    console.log('Update response:', updateResponse.data);
    
    // Clean up - delete the test ad
    console.log('\nCleaning up - deleting test ad...');
    await api.delete(`/admin/sponsored-ads/${createdAd.id}`);
    console.log('Test ad deleted');
    
  } catch (error) {
    console.error('Error:', error);
    if (error.response) {
      console.error('Status:', error.response.status);
      console.error('Error data:', error.response.data);
    }
  }
}

// Run the test
createTestAd().catch(err => {
  console.error('Script error:', err);
}); 