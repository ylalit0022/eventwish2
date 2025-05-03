/**
 * Sponsored Ad Rotation - Client Example
 * 
 * This example demonstrates how to use the ad rotation API endpoints
 * from a client application.
 */

const axios = require('axios');

// Base URL for the API (update this to match your environment)
const API_BASE_URL = 'http://localhost:3000/api';

// Sample device ID (in a real app, this would be a unique identifier for the device)
const DEVICE_ID = 'example-device-123';

// Track ads shown to this device
let shownAds = [];

/**
 * Fetch ads for rotation, excluding already shown ads
 * @param {String} location - Ad location (e.g., 'home_top', 'category_below')
 * @param {Number} limit - Maximum number of ads to fetch 
 * @returns {Promise<Array>} - List of ads
 */
async function fetchAdsForRotation(location, limit = 5) {
  try {
    const response = await axios.get(`${API_BASE_URL}/sponsored-ads/rotation`, {
      params: {
        location,
        limit,
        exclude: shownAds // Exclude ads we've already shown
      },
      headers: {
        'x-device-id': DEVICE_ID
      }
    });
    
    if (response.data.success && response.data.ads.length > 0) {
      return response.data.ads;
    }
    
    return [];
  } catch (error) {
    console.error('Error fetching ads for rotation:', error.message);
    return [];
  }
}

/**
 * Record an impression for an ad
 * @param {String} adId - ID of the ad viewed
 */
async function recordImpression(adId) {
  try {
    await axios.post(`${API_BASE_URL}/sponsored-ads/viewed/${adId}`, null, {
      headers: {
        'x-device-id': DEVICE_ID
      }
    });
    
    console.log(`Impression recorded for ad: ${adId}`);
    
    // Add to our list of shown ads
    if (!shownAds.includes(adId)) {
      shownAds.push(adId);
      
      // Keep list to reasonable size
      if (shownAds.length > 20) {
        shownAds = shownAds.slice(-20);
      }
    }
  } catch (error) {
    console.error(`Error recording impression for ad ${adId}:`, error.message);
  }
}

/**
 * Record a click for an ad
 * @param {String} adId - ID of the ad clicked
 */
async function recordClick(adId) {
  try {
    await axios.post(`${API_BASE_URL}/sponsored-ads/clicked/${adId}`, null, {
      headers: {
        'x-device-id': DEVICE_ID
      }
    });
    
    console.log(`Click recorded for ad: ${adId}`);
  } catch (error) {
    console.error(`Error recording click for ad ${adId}:`, error.message);
  }
}

/**
 * Simulate ad rotation 
 */
async function simulateAdRotation() {
  console.log('=== Starting Ad Rotation Simulation ===');
  
  // Simulate multiple rotation cycles
  for (let cycle = 1; cycle <= 5; cycle++) {
    console.log(`\n--- Rotation Cycle ${cycle} ---`);
    
    // Fetch ads for rotation
    const ads = await fetchAdsForRotation('home_top', 3);
    
    if (ads.length === 0) {
      console.log('No ads available for rotation');
      continue;
    }
    
    // Display information about fetched ads
    console.log(`Fetched ${ads.length} ads for rotation:`);
    ads.forEach((ad, index) => {
      console.log(`${index + 1}. ID: ${ad.id}, Title: ${ad.title}, Priority: ${ad.priority}`);
    });
    
    // Simulate displaying the first ad
    if (ads.length > 0) {
      const selectedAd = ads[0];
      console.log(`\nDisplaying ad: ${selectedAd.title}`);
      
      // Record impression
      await recordImpression(selectedAd.id);
      
      // Simulate a click with 30% probability
      if (Math.random() < 0.3) {
        console.log(`User clicked on ad: ${selectedAd.title}`);
        await recordClick(selectedAd.id);
      }
    }
    
    // In a real app, we would wait for some time before the next rotation
    console.log('\nWaiting for next rotation cycle...');
    await new Promise(resolve => setTimeout(resolve, 1000)); // Simulated delay
  }
  
  console.log('\n=== Ad Rotation Simulation Complete ===');
  console.log(`Total ads shown this session: ${shownAds.length}`);
}

// Run the simulation
simulateAdRotation().catch(error => {
  console.error('Simulation error:', error);
}); 