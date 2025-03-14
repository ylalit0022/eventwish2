/**
 * EventWish AdMob A/B Testing Integration Example
 * 
 * This example demonstrates how to integrate with the A/B testing functionality
 * to get optimal ad configurations and track events.
 */

// Configuration
const API_BASE_URL = 'https://api.eventwish.com';
const API_KEY = 'your-api-key';
const APP_SIGNATURE = 'your-app-signature';

// User context
const userContext = {
  userId: 'user-123',
  deviceId: 'device-456',
  deviceType: 'mobile',
  platform: 'android',
  country: 'US',
  language: 'en',
  appVersion: '1.0.0'
};

/**
 * Get optimal ad configuration for a specific ad type
 * @param {string} adType - Ad type (e.g., 'banner', 'interstitial', 'rewarded')
 * @returns {Promise<Object>} Ad configuration
 */
async function getOptimalAdConfig(adType) {
  try {
    // Build query string from user context
    const queryParams = Object.entries(userContext)
      .map(([key, value]) => `${key}=${encodeURIComponent(value)}`)
      .join('&');
    
    // Make request to A/B test endpoint
    const response = await fetch(`${API_BASE_URL}/api/abtest/ad/${adType}?${queryParams}`, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
        'x-api-key': API_KEY,
        'x-app-signature': APP_SIGNATURE,
        ...Object.entries(userContext).reduce((acc, [key, value]) => {
          acc[`x-${key}`] = value;
          return acc;
        }, {})
      }
    });
    
    // Parse response
    const data = await response.json();
    
    // Check for success
    if (!data.success) {
      throw new Error(data.message || 'Failed to get ad configuration');
    }
    
    // Return ad configuration
    return data.data;
  } catch (error) {
    console.error('Error getting optimal ad configuration:', error);
    
    // Fallback to default ad configuration
    return {
      adId: 'default-ad-id',
      adName: 'Default Ad',
      adType,
      adUnitCode: `default-${adType}-ad-unit-code`,
      parameters: {},
      isTest: false
    };
  }
}

/**
 * Track a test event (impression, click, etc.)
 * @param {string} testId - Test ID
 * @param {string} variantId - Variant ID
 * @param {string} eventType - Event type (e.g., 'impressions', 'clicks')
 * @returns {Promise<boolean>} Success
 */
async function trackTestEvent(testId, variantId, eventType) {
  try {
    // Make request to track event endpoint
    const response = await fetch(`${API_BASE_URL}/api/abtest/track/${testId}/${variantId}/${eventType}`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'x-api-key': API_KEY,
        'x-app-signature': APP_SIGNATURE,
        'x-user-id': userContext.userId,
        'x-device-id': userContext.deviceId
      }
    });
    
    // Parse response
    const data = await response.json();
    
    // Return success
    return data.success;
  } catch (error) {
    console.error(`Error tracking ${eventType} event:`, error);
    return false;
  }
}

/**
 * Display an ad based on the provided configuration
 * @param {Object} adConfig - Ad configuration
 * @param {string} containerId - ID of the container element
 */
function displayAd(adConfig, containerId) {
  // Get container element
  const container = document.getElementById(containerId);
  if (!container) {
    console.error(`Container element with ID "${containerId}" not found`);
    return;
  }
  
  // Clear container
  container.innerHTML = '';
  
  // Create ad element
  const adElement = document.createElement('div');
  adElement.className = 'ad';
  adElement.style.backgroundColor = adConfig.parameters.backgroundColor || '#FFFFFF';
  adElement.style.color = adConfig.parameters.textColor || '#000000';
  adElement.style.padding = '10px';
  adElement.style.borderRadius = '5px';
  adElement.style.margin = '10px 0';
  adElement.style.textAlign = 'center';
  
  // Create ad content
  adElement.innerHTML = `
    <h3>${adConfig.adName}</h3>
    <p>Ad Unit Code: ${adConfig.adUnitCode}</p>
    <button class="ad-cta">Click Me</button>
  `;
  
  // Add ad to container
  container.appendChild(adElement);
  
  // Track impression if ad is part of a test
  if (adConfig.isTest) {
    trackTestEvent(adConfig.testId, adConfig.variantId, 'impressions');
    
    // Add click event listener
    const ctaButton = adElement.querySelector('.ad-cta');
    if (ctaButton) {
      ctaButton.addEventListener('click', () => {
        // Track click
        trackTestEvent(adConfig.testId, adConfig.variantId, 'clicks');
        
        // Handle click action
        console.log('Ad clicked:', adConfig);
      });
    }
  }
}

/**
 * Initialize ads on the page
 */
async function initializeAds() {
  try {
    // Get banner ad configuration
    const bannerAdConfig = await getOptimalAdConfig('banner');
    displayAd(bannerAdConfig, 'banner-ad-container');
    
    // Get interstitial ad configuration
    const interstitialAdConfig = await getOptimalAdConfig('interstitial');
    
    // Add button to show interstitial ad
    const interstitialButton = document.getElementById('show-interstitial-button');
    if (interstitialButton) {
      interstitialButton.addEventListener('click', () => {
        // Create interstitial ad overlay
        const overlay = document.createElement('div');
        overlay.className = 'ad-overlay';
        overlay.style.position = 'fixed';
        overlay.style.top = '0';
        overlay.style.left = '0';
        overlay.style.width = '100%';
        overlay.style.height = '100%';
        overlay.style.backgroundColor = 'rgba(0, 0, 0, 0.8)';
        overlay.style.display = 'flex';
        overlay.style.justifyContent = 'center';
        overlay.style.alignItems = 'center';
        overlay.style.zIndex = '1000';
        
        // Create close button
        const closeButton = document.createElement('button');
        closeButton.textContent = 'Close';
        closeButton.style.position = 'absolute';
        closeButton.style.top = '10px';
        closeButton.style.right = '10px';
        closeButton.style.padding = '5px 10px';
        closeButton.style.backgroundColor = '#FFFFFF';
        closeButton.style.border = 'none';
        closeButton.style.borderRadius = '5px';
        closeButton.style.cursor = 'pointer';
        
        // Create ad container
        const adContainer = document.createElement('div');
        adContainer.style.width = '80%';
        adContainer.style.maxWidth = '500px';
        adContainer.style.backgroundColor = interstitialAdConfig.parameters.backgroundColor || '#FFFFFF';
        adContainer.style.color = interstitialAdConfig.parameters.textColor || '#000000';
        adContainer.style.padding = '20px';
        adContainer.style.borderRadius = '10px';
        adContainer.style.textAlign = 'center';
        
        // Create ad content
        adContainer.innerHTML = `
          <h2>${interstitialAdConfig.adName}</h2>
          <p>Ad Unit Code: ${interstitialAdConfig.adUnitCode}</p>
          <button class="ad-cta">Learn More</button>
        `;
        
        // Add close button to overlay
        overlay.appendChild(closeButton);
        
        // Add ad container to overlay
        overlay.appendChild(adContainer);
        
        // Add overlay to body
        document.body.appendChild(overlay);
        
        // Track impression if ad is part of a test
        if (interstitialAdConfig.isTest) {
          trackTestEvent(interstitialAdConfig.testId, interstitialAdConfig.variantId, 'impressions');
          
          // Add click event listener
          const ctaButton = adContainer.querySelector('.ad-cta');
          if (ctaButton) {
            ctaButton.addEventListener('click', () => {
              // Track click
              trackTestEvent(interstitialAdConfig.testId, interstitialAdConfig.variantId, 'clicks');
              
              // Handle click action
              console.log('Interstitial ad clicked:', interstitialAdConfig);
            });
          }
        }
        
        // Add close button event listener
        closeButton.addEventListener('click', () => {
          document.body.removeChild(overlay);
        });
      });
    }
  } catch (error) {
    console.error('Error initializing ads:', error);
  }
}

// Initialize ads when the page loads
window.addEventListener('DOMContentLoaded', initializeAds); 