/**
 * EventWish AdMob Client Example with Targeting
 * 
 * This example demonstrates how to integrate with the AdMob service
 * and provide user context for better ad targeting.
 */

// Configuration
const API_BASE_URL = 'https://eventwish2.onrender.com';
const API_KEY = '8da9c210aa3635693bf68f85c5a3bc070e97cf43fdf9893ecf8b8fb08d285c16';
const APP_SIGNATURE = 'app_sig_1';

/**
 * Get user context with as much information as possible
 * @returns {Object} User context
 */
function getUserContext() {
  // Get basic device information
  const deviceInfo = {
    deviceType: detectDeviceType(),
    platform: detectPlatform(),
    screenSize: `${window.screen.width}x${window.screen.height}`,
    connectionType: detectConnectionType()
  };
  
  // Get user identification (if available)
  const userInfo = {
    userId: localStorage.getItem('userId') || generateUserId(),
    deviceId: localStorage.getItem('deviceId') || generateDeviceId()
  };
  
  // Get location information (if available)
  const locationInfo = {
    country: localStorage.getItem('country') || 'unknown',
    language: navigator.language || 'en',
    timeZone: Intl.DateTimeFormat().resolvedOptions().timeZone || 'UTC'
  };
  
  // Get app information
  const appInfo = {
    appVersion: '1.0.0' // Replace with actual app version
  };
  
  // Get user behavior (from local storage)
  const behaviorInfo = {
    recentActivity: JSON.parse(localStorage.getItem('recentActivity') || '[]')
  };
  
  // Combine all context information
  return {
    ...userInfo,
    ...deviceInfo,
    ...locationInfo,
    ...appInfo,
    ...behaviorInfo,
    timestamp: new Date().toISOString()
  };
}

/**
 * Detect device type based on screen size and user agent
 * @returns {string} Device type
 */
function detectDeviceType() {
  const userAgent = navigator.userAgent.toLowerCase();
  const isMobile = /mobile|android|iphone|ipod|blackberry|windows phone/i.test(userAgent);
  const isTablet = /tablet|ipad/i.test(userAgent) || (window.screen.width >= 768 && window.screen.width < 1024);
  
  if (isTablet) return 'tablet';
  if (isMobile) return 'mobile';
  return 'desktop';
}

/**
 * Detect platform based on user agent
 * @returns {string} Platform
 */
function detectPlatform() {
  const userAgent = navigator.userAgent.toLowerCase();
  
  if (/android/i.test(userAgent)) return 'android';
  if (/iphone|ipad|ipod/i.test(userAgent)) return 'ios';
  if (/windows/i.test(userAgent)) return 'windows';
  if (/mac/i.test(userAgent)) return 'mac';
  if (/linux/i.test(userAgent)) return 'linux';
  
  return 'unknown';
}

/**
 * Detect connection type based on navigator.connection
 * @returns {string} Connection type
 */
function detectConnectionType() {
  if (navigator.connection) {
    if (navigator.connection.effectiveType) {
      return navigator.connection.effectiveType; // 4g, 3g, 2g, slow-2g
    }
    
    if (navigator.connection.type) {
      return navigator.connection.type; // wifi, cellular, none, etc.
    }
  }
  
  return 'unknown';
}

/**
 * Generate a random user ID
 * @returns {string} User ID
 */
function generateUserId() {
  const userId = 'user_' + Math.random().toString(36).substring(2, 15);
  localStorage.setItem('userId', userId);
  return userId;
}

/**
 * Generate a random device ID
 * @returns {string} Device ID
 */
function generateDeviceId() {
  const deviceId = 'device_' + Math.random().toString(36).substring(2, 15);
  localStorage.setItem('deviceId', deviceId);
  return deviceId;
}

/**
 * Track user activity
 * @param {string} activity - Activity to track
 */
function trackActivity(activity) {
  // Get recent activities
  const recentActivity = JSON.parse(localStorage.getItem('recentActivity') || '[]');
  
  // Add new activity
  recentActivity.push(activity);
  
  // Keep only the 10 most recent activities
  if (recentActivity.length > 10) {
    recentActivity.shift();
  }
  
  // Save updated activities
  localStorage.setItem('recentActivity', JSON.stringify(recentActivity));
}

/**
 * Get ad configuration based on user context
 * @param {string} adType - Ad type (e.g., 'Banner', 'Interstitial')
 * @returns {Promise<Object>} Ad configuration
 */
async function getAdConfig(adType) {
  try {
    // Get user context
    const userContext = getUserContext();
    
    // Build query string from user context
    const queryParams = Object.entries(userContext)
      .map(([key, value]) => {
        if (Array.isArray(value)) {
          return `${key}=${encodeURIComponent(JSON.stringify(value))}`;
        }
        return `${key}=${encodeURIComponent(value)}`;
      })
      .join('&');
    
    // Add ad type to query
    const fullQueryParams = `adType=${adType}&${queryParams}`;
    
    // Make request to ad config endpoint
    const response = await fetch(`${API_BASE_URL}/api/admob/config?${fullQueryParams}`, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
        'x-api-key': API_KEY,
        'x-app-signature': APP_SIGNATURE,
        ...Object.entries(userContext).reduce((acc, [key, value]) => {
          if (!Array.isArray(value) && typeof value !== 'object') {
            acc[`x-${key}`] = value;
          }
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
    console.error('Error getting ad configuration:', error);
    
    // Return default ad configuration
    return {
      id: 'default-ad-id',
      adName: 'Default Ad',
      adType,
      adUnitCode: `ca-app-pub-0000000000000000/0000000000`,
      parameters: {}
    };
  }
}

/**
 * Track ad impression
 * @param {string} adId - Ad ID
 */
async function trackImpression(adId) {
  try {
    // Get user context
    const userContext = getUserContext();
    
    // Track activity
    trackActivity('view_ad');
    
    // Make request to impression endpoint
    await fetch(`${API_BASE_URL}/api/admob/impression/${adId}`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'x-api-key': API_KEY,
        'x-app-signature': APP_SIGNATURE
      },
      body: JSON.stringify({
        context: userContext
      })
    });
    
    console.log(`Impression tracked for ad ${adId}`);
  } catch (error) {
    console.error('Error tracking impression:', error);
  }
}

/**
 * Track ad click
 * @param {string} adId - Ad ID
 */
async function trackClick(adId) {
  try {
    // Get user context
    const userContext = getUserContext();
    
    // Track activity
    trackActivity('click_ad');
    
    // Make request to click endpoint
    await fetch(`${API_BASE_URL}/api/admob/click/${adId}`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'x-api-key': API_KEY,
        'x-app-signature': APP_SIGNATURE
      },
      body: JSON.stringify({
        context: userContext
      })
    });
    
    console.log(`Click tracked for ad ${adId}`);
  } catch (error) {
    console.error('Error tracking click:', error);
  }
}

/**
 * Display an ad in the specified container
 * @param {string} adType - Ad type (e.g., 'Banner', 'Interstitial')
 * @param {string} containerId - ID of the container element
 */
async function displayAd(adType, containerId) {
  try {
    // Get container element
    const container = document.getElementById(containerId);
    if (!container) {
      console.error(`Container element with ID "${containerId}" not found`);
      return;
    }
    
    // Clear container
    container.innerHTML = '';
    
    // Show loading indicator
    container.innerHTML = '<div class="ad-loading">Loading ad...</div>';
    
    // Get ad configuration
    const adConfig = await getAdConfig(adType);
    
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
    
    // Track impression
    trackImpression(adConfig.id);
    
    // Add click event listener
    const ctaButton = adElement.querySelector('.ad-cta');
    if (ctaButton) {
      ctaButton.addEventListener('click', () => {
        // Track click
        trackClick(adConfig.id);
        
        // Handle click action
        console.log('Ad clicked:', adConfig);
        
        // Simulate a purchase (for demo purposes)
        if (Math.random() > 0.7) {
          trackActivity('purchase');
          console.log('Purchase tracked');
        }
      });
    }
  } catch (error) {
    console.error('Error displaying ad:', error);
  }
}

// Initialize when the page loads
window.addEventListener('DOMContentLoaded', () => {
  // Display banner ad
  displayAd('Banner', 'banner-ad-container');
  
  // Add event listener for refresh button
  const refreshButton = document.getElementById('refresh-ad-button');
  if (refreshButton) {
    refreshButton.addEventListener('click', () => {
      displayAd('Banner', 'banner-ad-container');
    });
  }
  
  // Add event listener for interstitial button
  const interstitialButton = document.getElementById('show-interstitial-button');
  if (interstitialButton) {
    interstitialButton.addEventListener('click', async () => {
      // Get ad configuration
      const adConfig = await getAdConfig('Interstitial');
      
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
      adContainer.style.backgroundColor = adConfig.parameters.backgroundColor || '#FFFFFF';
      adContainer.style.color = adConfig.parameters.textColor || '#000000';
      adContainer.style.padding = '20px';
      adContainer.style.borderRadius = '10px';
      adContainer.style.textAlign = 'center';
      
      // Create ad content
      adContainer.innerHTML = `
        <h2>${adConfig.adName}</h2>
        <p>Ad Unit Code: ${adConfig.adUnitCode}</p>
        <button class="ad-cta">Learn More</button>
      `;
      
      // Add close button to overlay
      overlay.appendChild(closeButton);
      
      // Add ad container to overlay
      overlay.appendChild(adContainer);
      
      // Add overlay to body
      document.body.appendChild(overlay);
      
      // Track impression
      trackImpression(adConfig.id);
      
      // Add click event listener
      const ctaButton = adContainer.querySelector('.ad-cta');
      if (ctaButton) {
        ctaButton.addEventListener('click', () => {
          // Track click
          trackClick(adConfig.id);
          
          // Handle click action
          console.log('Interstitial ad clicked:', adConfig);
          
          // Simulate a purchase (for demo purposes)
          if (Math.random() > 0.5) {
            trackActivity('purchase');
            console.log('Purchase tracked');
          }
        });
      }
      
      // Add close button event listener
      closeButton.addEventListener('click', () => {
        document.body.removeChild(overlay);
      });
    });
  }
}); 