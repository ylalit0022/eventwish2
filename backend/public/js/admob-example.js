/**
 * AdMob Client Example
 * 
 * This file demonstrates how to use the AdMobClient library.
 */
(function() {
  'use strict';
  
  // Initialize AdMobClient
  const adMobClient = new AdMobClient({
    appSignature: 'YOUR_APP_SIGNATURE', // Replace with your app signature
    baseUrl: '/api/admob', // Base URL for API requests
    retryAttempts: 3, // Number of retry attempts for failed requests
    retryDelay: 1000, // Initial delay between retries in milliseconds
    cacheExpiration: 300000, // Cache expiration time in milliseconds (5 minutes)
    offlineSupport: true // Enable offline support
  });
  
  /**
   * Load and display a banner ad
   * @param {string} containerId - ID of the container element
   */
  async function loadBannerAd(containerId) {
    try {
      // Get the container element
      const container = document.getElementById(containerId);
      
      if (!container) {
        console.error(`Container element with ID ${containerId} not found`);
        return;
      }
      
      // Show loading state
      container.innerHTML = '<div class="ad-loading">Loading ad...</div>';
      
      // Get device information
      const context = {
        deviceType: getDeviceType(),
        platform: getPlatform(),
        screenWidth: window.innerWidth,
        screenHeight: window.innerHeight,
        language: navigator.language
      };
      
      // Get ad configuration
      const adConfig = await adMobClient.getAdConfig('Banner', context);
      
      if (!adConfig) {
        console.error('Failed to get ad configuration');
        container.innerHTML = '<div class="ad-error">Ad could not be loaded</div>';
        return;
      }
      
      // Create ad element
      const adElement = document.createElement('div');
      adElement.className = 'admob-banner';
      adElement.dataset.adId = adConfig.id;
      
      // In a real implementation, you would use the Google AdMob SDK here
      // This is just a placeholder for demonstration purposes
      adElement.innerHTML = `
        <div class="admob-banner-content">
          <div class="admob-banner-header">Advertisement</div>
          <div class="admob-banner-body">
            <p>This is a sample banner ad</p>
            <button class="admob-banner-cta">Learn More</button>
          </div>
        </div>
      `;
      
      // Add click event listener
      adElement.querySelector('.admob-banner-cta').addEventListener('click', () => {
        // Track click
        adMobClient.trackClick(adConfig.id, {
          ...context,
          clickPosition: 'cta_button'
        });
        
        // Open ad URL (in a real implementation)
        // window.open(adConfig.targetUrl, '_blank');
        console.log('Ad clicked');
      });
      
      // Clear container and add ad element
      container.innerHTML = '';
      container.appendChild(adElement);
      
      // Track impression
      adMobClient.trackImpression(adConfig.id, context);
      
      console.log('Banner ad loaded successfully');
    } catch (error) {
      console.error('Error loading banner ad:', error);
      
      // Show error state
      const container = document.getElementById(containerId);
      if (container) {
        container.innerHTML = '<div class="ad-error">Ad could not be loaded</div>';
      }
    }
  }
  
  /**
   * Load and display an interstitial ad
   */
  async function loadInterstitialAd() {
    try {
      // Get device information
      const context = {
        deviceType: getDeviceType(),
        platform: getPlatform(),
        screenWidth: window.innerWidth,
        screenHeight: window.innerHeight,
        language: navigator.language
      };
      
      // Get ad configuration
      const adConfig = await adMobClient.getAdConfig('Interstitial', context);
      
      if (!adConfig) {
        console.error('Failed to get interstitial ad configuration');
        return;
      }
      
      // Create modal container
      const modalContainer = document.createElement('div');
      modalContainer.className = 'admob-interstitial-container';
      modalContainer.dataset.adId = adConfig.id;
      
      // In a real implementation, you would use the Google AdMob SDK here
      // This is just a placeholder for demonstration purposes
      modalContainer.innerHTML = `
        <div class="admob-interstitial-content">
          <div class="admob-interstitial-header">
            <span>Advertisement</span>
            <button class="admob-interstitial-close">&times;</button>
          </div>
          <div class="admob-interstitial-body">
            <p>This is a sample interstitial ad</p>
            <button class="admob-interstitial-cta">Learn More</button>
          </div>
        </div>
      `;
      
      // Add click event listener for CTA button
      modalContainer.querySelector('.admob-interstitial-cta').addEventListener('click', () => {
        // Track click
        adMobClient.trackClick(adConfig.id, {
          ...context,
          clickPosition: 'cta_button'
        });
        
        // Open ad URL (in a real implementation)
        // window.open(adConfig.targetUrl, '_blank');
        console.log('Interstitial ad clicked');
        
        // Close modal
        document.body.removeChild(modalContainer);
      });
      
      // Add click event listener for close button
      modalContainer.querySelector('.admob-interstitial-close').addEventListener('click', () => {
        // Close modal
        document.body.removeChild(modalContainer);
      });
      
      // Add to document
      document.body.appendChild(modalContainer);
      
      // Track impression
      adMobClient.trackImpression(adConfig.id, context);
      
      console.log('Interstitial ad loaded successfully');
    } catch (error) {
      console.error('Error loading interstitial ad:', error);
    }
  }
  
  /**
   * Get device type
   * @returns {string} - Device type
   */
  function getDeviceType() {
    const ua = navigator.userAgent;
    if (/tablet|ipad|playbook|silk/i.test(ua)) {
      return 'tablet';
    }
    if (/mobile|iphone|ipod|android|blackberry|opera mini|iemobile/i.test(ua)) {
      return 'mobile';
    }
    return 'desktop';
  }
  
  /**
   * Get platform
   * @returns {string} - Platform
   */
  function getPlatform() {
    const ua = navigator.userAgent;
    if (/android/i.test(ua)) {
      return 'android';
    }
    if (/iphone|ipad|ipod/i.test(ua)) {
      return 'ios';
    }
    if (/windows/i.test(ua)) {
      return 'windows';
    }
    if (/macintosh|mac os x/i.test(ua)) {
      return 'mac';
    }
    if (/linux/i.test(ua)) {
      return 'linux';
    }
    return 'unknown';
  }
  
  // Expose functions to window
  window.AdMobExample = {
    loadBannerAd,
    loadInterstitialAd
  };
  
})(); 