import axios from 'axios';

// Get the server endpoint from localStorage or use the default
const getServerEndpoint = () => {
  const storedEndpoint = localStorage.getItem('server_endpoint');
  return storedEndpoint || '/api'; // Default to relative path for proxy
};

// Create axios instance with default config
const api = axios.create({
  baseURL: getServerEndpoint(),
  headers: {
    'Content-Type': 'application/json',
  },
});

// Add request interceptor for authentication
api.interceptors.request.use(
  (config) => {
    // Update baseURL on each request in case it was changed in settings
    config.baseURL = getServerEndpoint();
    
    // Add API key to all requests
    const apiKey = localStorage.getItem('api_key') || 'test-api-key';
    config.headers['x-api-key'] = apiKey;
    
    // Add app signature if available
    const appSignature = localStorage.getItem('app_signature');
    if (appSignature) {
      config.headers['x-app-signature'] = appSignature;
    }
    
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// AdMob API endpoints
const adMobApi = {
  // Get ad configuration
  getAdConfig: async (adType, context = {}) => {
    try {
      const response = await api.post('/admob/config', {
        format: adType,
        context
      });
      return response.data;
    } catch (error) {
      console.error('Error fetching ad config:', error);
      throw error;
    }
  },
  
  // Track ad impression
  trackImpression: async (adId, context = {}) => {
    try {
      const response = await api.post(`/admob/impression`, {
        adId,
        timestamp: new Date().toISOString(),
        ...context
      });
      return response.data;
    } catch (error) {
      console.error('Error tracking impression:', error);
      throw error;
    }
  },
  
  // Track ad click
  trackClick: async (adId, context = {}) => {
    try {
      const response = await api.post(`/admob/click`, {
        adId,
        timestamp: new Date().toISOString(),
        ...context
      });
      return response.data;
    } catch (error) {
      console.error('Error tracking click:', error);
      throw error;
    }
  },
  
  // Get ad types
  getAdTypes: async () => {
    try {
      const response = await api.get('/admob/types');
      return response.data;
    } catch (error) {
      console.error('Error fetching ad types:', error);
      throw error;
    }
  },
  
  // Get all active ads (admin endpoint)
  getActiveAds: async () => {
    try {
      const response = await api.get('/admob-ads');
      return response.data;
    } catch (error) {
      console.error('Error fetching active ads:', error);
      throw error;
    }
  },
  
  // Get analytics data for a specific ad
  getAdAnalytics: async (adId) => {
    try {
      const response = await api.get(`/analytics/ad/${adId}`);
      return response.data;
    } catch (error) {
      console.error('Error fetching ad analytics:', error);
      throw error;
    }
  },
  
  // Get summary analytics for all ads
  getAnalyticsSummary: async () => {
    try {
      const response = await api.get('/analytics/summary');
      return response.data;
    } catch (error) {
      console.error('Error fetching analytics summary:', error);
      throw error;
    }
  },
  
  // Get current monitoring metrics
  getMonitoringMetrics: async () => {
    try {
      const response = await api.get('/monitoring/metrics');
      return response.data;
    } catch (error) {
      console.error('Error fetching monitoring metrics:', error);
      throw error;
    }
  },
  
  // Get ad-specific monitoring metrics
  getAdMetrics: async () => {
    try {
      const response = await api.get('/monitoring/ad-metrics');
      return response.data;
    } catch (error) {
      console.error('Error fetching ad metrics:', error);
      throw error;
    }
  },
  
  // Simulate fraud detection
  simulateFraud: async (fraudType, params = {}) => {
    try {
      const response = await api.post('/testing/simulate-fraud', {
        fraudType,
        params
      });
      return response.data;
    } catch (error) {
      console.error('Error simulating fraud:', error);
      throw error;
    }
  },
  
  // Get fraud detection status
  getFraudDetectionStatus: async () => {
    try {
      const response = await api.get('/fraud-detection/status');
      return response.data;
    } catch (error) {
      console.error('Error fetching fraud detection status:', error);
      throw error;
    }
  },
  
  // Get A/B test configurations
  getABTests: async () => {
    try {
      const response = await api.get('/abtests');
      return response.data;
    } catch (error) {
      console.error('Error fetching A/B tests:', error);
      throw error;
    }
  },
  
  // Get A/B test results
  getABTestResults: async (testId) => {
    try {
      const response = await api.get(`/abtests/${testId}/results`);
      return response.data;
    } catch (error) {
      console.error('Error fetching A/B test results:', error);
      throw error;
    }
  }
};

export default adMobApi; 