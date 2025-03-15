/**
 * AdMob Client Library
 * 
 * A lightweight client library for integrating with the server-side AdMob service.
 * This library handles ad requests, impression tracking, and click tracking.
 */
(function(window) {
  'use strict';
  
  // Default configuration
  const DEFAULT_CONFIG = {
    baseUrl: '/api/admob',
    appSignature: '',
    retryAttempts: 3,
    retryDelay: 1000,
    cacheExpiration: 300000, // 5 minutes in milliseconds
    offlineSupport: true
  };
  
  // AdMob client class
  class AdMobClient {
    /**
     * Create a new AdMobClient instance
     * @param {Object} config - Configuration options
     */
    constructor(config = {}) {
      // Merge default config with provided config
      this.config = { ...DEFAULT_CONFIG, ...config };
      
      // Validate required config
      if (!this.config.appSignature) {
        console.error('AdMobClient: App signature is required');
        throw new Error('App signature is required');
      }
      
      // Initialize cache
      this.cache = {};
      
      // Initialize offline queue
      this.offlineQueue = [];
      
      // Check for offline support
      if (this.config.offlineSupport) {
        this._initOfflineSupport();
      }
      
      // Log initialization
      console.log('AdMobClient: Initialized with config', this.config);
    }
    
    /**
     * Initialize offline support
     * @private
     */
    _initOfflineSupport() {
      // Load cached ads from localStorage
      try {
        const cachedAds = localStorage.getItem('admob_cached_ads');
        if (cachedAds) {
          this.cache = JSON.parse(cachedAds);
          console.log('AdMobClient: Loaded cached ads from localStorage');
        }
        
        // Load offline queue from localStorage
        const offlineQueue = localStorage.getItem('admob_offline_queue');
        if (offlineQueue) {
          this.offlineQueue = JSON.parse(offlineQueue);
          console.log('AdMobClient: Loaded offline queue from localStorage');
        }
      } catch (error) {
        console.error('AdMobClient: Error loading from localStorage', error);
      }
      
      // Process offline queue when online
      window.addEventListener('online', () => {
        console.log('AdMobClient: Device is online, processing offline queue');
        this._processOfflineQueue();
      });
      
      // Save cache and queue when offline
      window.addEventListener('offline', () => {
        console.log('AdMobClient: Device is offline, saving cache and queue');
        this._saveToLocalStorage();
      });
      
      // Save cache and queue before unload
      window.addEventListener('beforeunload', () => {
        this._saveToLocalStorage();
      });
    }
    
    /**
     * Save cache and offline queue to localStorage
     * @private
     */
    _saveToLocalStorage() {
      try {
        localStorage.setItem('admob_cached_ads', JSON.stringify(this.cache));
        localStorage.setItem('admob_offline_queue', JSON.stringify(this.offlineQueue));
      } catch (error) {
        console.error('AdMobClient: Error saving to localStorage', error);
      }
    }
    
    /**
     * Process offline queue
     * @private
     */
    _processOfflineQueue() {
      if (this.offlineQueue.length === 0) {
        return;
      }
      
      console.log(`AdMobClient: Processing ${this.offlineQueue.length} items in offline queue`);
      
      // Process each item in the queue
      const queue = [...this.offlineQueue];
      this.offlineQueue = [];
      
      queue.forEach(item => {
        if (item.type === 'impression') {
          this.trackImpression(item.adId, item.context);
        } else if (item.type === 'click') {
          this.trackClick(item.adId, item.context);
        }
      });
    }
    
    /**
     * Make an API request
     * @private
     * @param {string} endpoint - API endpoint
     * @param {string} method - HTTP method
     * @param {Object} data - Request data
     * @param {number} attempt - Current attempt number
     * @returns {Promise} - Promise that resolves with the response
     */
    async _makeRequest(endpoint, method = 'GET', data = null, attempt = 1) {
      try {
        // Build URL
        const url = `${this.config.baseUrl}${endpoint}`;
        
        // Set up request options
        const options = {
          method,
          headers: {
            'Content-Type': 'application/json',
            'x-app-signature': this.config.appSignature
          }
        };
        
        // Add body for POST requests
        if (method === 'POST' && data) {
          options.body = JSON.stringify(data);
        }
        
        // Make request
        console.log(`AdMobClient: Making ${method} request to ${url}`, options);
        const response = await fetch(url, options);
        
        // Check if response is ok
        if (!response.ok) {
          throw new Error(`HTTP error! Status: ${response.status}`);
        }
        
        // Parse response
        const responseData = await response.json();
        
        // Return response data
        return responseData;
      } catch (error) {
        console.error(`AdMobClient: Request failed (attempt ${attempt}/${this.config.retryAttempts})`, error);
        
        // Retry if attempts remaining
        if (attempt < this.config.retryAttempts) {
          // Calculate delay with exponential backoff
          const delay = this.config.retryDelay * Math.pow(2, attempt - 1);
          
          console.log(`AdMobClient: Retrying in ${delay}ms...`);
          
          // Wait for delay
          await new Promise(resolve => setTimeout(resolve, delay));
          
          // Retry request
          return this._makeRequest(endpoint, method, data, attempt + 1);
        }
        
        // No attempts remaining, throw error
        throw error;
      }
    }
    
    /**
     * Get ad configuration
     * @param {string} adType - Type of ad to retrieve
     * @param {Object} context - Additional context information
     * @returns {Promise} - Promise that resolves with the ad configuration
     */
    async getAdConfig(adType, context = {}) {
      try {
        // Check cache first
        const cacheKey = `ad_config_${adType}`;
        const cachedConfig = this.cache[cacheKey];
        
        if (cachedConfig && cachedConfig.timestamp > Date.now() - this.config.cacheExpiration) {
          console.log(`AdMobClient: Using cached ad config for type ${adType}`);
          return cachedConfig.data;
        }
        
        // Build query string
        const queryParams = new URLSearchParams({
          adType,
          ...context
        }).toString();
        
        // Make request
        const response = await this._makeRequest(`/config?${queryParams}`);
        
        // Cache response
        this.cache[cacheKey] = {
          timestamp: Date.now(),
          data: response.data.adConfig
        };
        
        // Save to localStorage if offline support is enabled
        if (this.config.offlineSupport) {
          this._saveToLocalStorage();
        }
        
        return response.data.adConfig;
      } catch (error) {
        console.error('AdMobClient: Error getting ad config', error);
        
        // Return cached config if available, even if expired
        const cacheKey = `ad_config_${adType}`;
        const cachedConfig = this.cache[cacheKey];
        
        if (cachedConfig) {
          console.log(`AdMobClient: Using expired cached ad config for type ${adType}`);
          return cachedConfig.data;
        }
        
        throw error;
      }
    }
    
    /**
     * Track ad impression
     * @param {string} adId - ID of the ad that was impressed
     * @param {Object} context - Additional context information
     * @returns {Promise} - Promise that resolves when the impression is tracked
     */
    async trackImpression(adId, context = {}) {
      try {
        // Check if offline
        if (!navigator.onLine) {
          console.log('AdMobClient: Device is offline, queueing impression');
          this.offlineQueue.push({
            type: 'impression',
            adId,
            context,
            timestamp: Date.now()
          });
          
          if (this.config.offlineSupport) {
            this._saveToLocalStorage();
          }
          
          return;
        }
        
        // Make request
        await this._makeRequest(`/impression/${adId}`, 'POST', {
          context
        });
        
        console.log(`AdMobClient: Impression tracked for ad ${adId}`);
      } catch (error) {
        console.error('AdMobClient: Error tracking impression', error);
        
        // Queue for later if offline support is enabled
        if (this.config.offlineSupport) {
          this.offlineQueue.push({
            type: 'impression',
            adId,
            context,
            timestamp: Date.now()
          });
          
          this._saveToLocalStorage();
        }
      }
    }
    
    /**
     * Track ad click
     * @param {string} adId - ID of the ad that was clicked
     * @param {Object} context - Additional context information
     * @returns {Promise} - Promise that resolves when the click is tracked
     */
    async trackClick(adId, context = {}) {
      try {
        // Check if offline
        if (!navigator.onLine) {
          console.log('AdMobClient: Device is offline, queueing click');
          this.offlineQueue.push({
            type: 'click',
            adId,
            context,
            timestamp: Date.now()
          });
          
          if (this.config.offlineSupport) {
            this._saveToLocalStorage();
          }
          
          return;
        }
        
        // Make request
        await this._makeRequest(`/click/${adId}`, 'POST', {
          context
        });
        
        console.log(`AdMobClient: Click tracked for ad ${adId}`);
      } catch (error) {
        console.error('AdMobClient: Error tracking click', error);
        
        // Queue for later if offline support is enabled
        if (this.config.offlineSupport) {
          this.offlineQueue.push({
            type: 'click',
            adId,
            context,
            timestamp: Date.now()
          });
          
          this._saveToLocalStorage();
        }
      }
    }
    
    /**
     * Get ad types
     * @returns {Promise} - Promise that resolves with the ad types
     */
    async getAdTypes() {
      try {
        // Check cache first
        const cacheKey = 'ad_types';
        const cachedTypes = this.cache[cacheKey];
        
        if (cachedTypes && cachedTypes.timestamp > Date.now() - this.config.cacheExpiration) {
          console.log('AdMobClient: Using cached ad types');
          return cachedTypes.data;
        }
        
        // Make request
        const response = await this._makeRequest('/types');
        
        // Cache response
        this.cache[cacheKey] = {
          timestamp: Date.now(),
          data: response.data.adTypes
        };
        
        // Save to localStorage if offline support is enabled
        if (this.config.offlineSupport) {
          this._saveToLocalStorage();
        }
        
        return response.data.adTypes;
      } catch (error) {
        console.error('AdMobClient: Error getting ad types', error);
        
        // Return cached types if available, even if expired
        const cacheKey = 'ad_types';
        const cachedTypes = this.cache[cacheKey];
        
        if (cachedTypes) {
          console.log('AdMobClient: Using expired cached ad types');
          return cachedTypes.data;
        }
        
        throw error;
      }
    }
    
    /**
     * Clear cache
     */
    clearCache() {
      this.cache = {};
      
      if (this.config.offlineSupport) {
        localStorage.removeItem('admob_cached_ads');
      }
      
      console.log('AdMobClient: Cache cleared');
    }
  }
  
  // Expose to window
  window.AdMobClient = AdMobClient;
  
})(window); 