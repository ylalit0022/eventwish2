const monitoringService = require('../services/monitoringService');
const { AdMob } = require('../models/AdMob');
const mongoose = require('mongoose');
const { MongoMemoryServer } = require('mongodb-memory-server');

// Mock dependencies
jest.mock('../config/logger');
jest.mock('../services/alertService', () => ({
  sendAlert: jest.fn().mockResolvedValue(true)
}));

let mongoServer;

beforeAll(async () => {
  // Start in-memory MongoDB server
  mongoServer = await MongoMemoryServer.create();
  const uri = mongoServer.getUri();
  
  // Connect to in-memory database
  await mongoose.connect(uri, {
    useNewUrlParser: true,
    useUnifiedTopology: true
  });
});

afterAll(async () => {
  // Disconnect from database and stop server
  await mongoose.disconnect();
  await mongoServer.stop();
});

beforeEach(() => {
  // Reset mocks
  jest.clearAllMocks();
  
  // Reset metrics
  monitoringService.resetMetrics();
});

describe('Monitoring Service', () => {
  describe('Request Tracking', () => {
    it('should track request successfully', () => {
      const req = {
        method: 'GET',
        originalUrl: '/api/admob/config',
        ip: '192.168.1.1',
        headers: {
          'user-agent': 'Mozilla/5.0'
        }
      };
      
      monitoringService.trackRequest(req);
      
      const metrics = monitoringService.getMetrics();
      expect(metrics.totalRequests).toBe(1);
      expect(metrics.requestsByEndpoint['/api/admob/config']).toBe(1);
      expect(metrics.requestsByMethod.GET).toBe(1);
    });
    
    it('should track multiple requests', () => {
      // Track GET request
      monitoringService.trackRequest({
        method: 'GET',
        originalUrl: '/api/admob/config',
        ip: '192.168.1.1'
      });
      
      // Track POST request
      monitoringService.trackRequest({
        method: 'POST',
        originalUrl: '/api/admob/impression',
        ip: '192.168.1.1'
      });
      
      // Track another GET request
      monitoringService.trackRequest({
        method: 'GET',
        originalUrl: '/api/admob/config',
        ip: '192.168.1.2'
      });
      
      const metrics = monitoringService.getMetrics();
      expect(metrics.totalRequests).toBe(3);
      expect(metrics.requestsByEndpoint['/api/admob/config']).toBe(2);
      expect(metrics.requestsByEndpoint['/api/admob/impression']).toBe(1);
      expect(metrics.requestsByMethod.GET).toBe(2);
      expect(metrics.requestsByMethod.POST).toBe(1);
    });
    
    it('should track unique IPs', () => {
      // Track requests from different IPs
      monitoringService.trackRequest({
        method: 'GET',
        originalUrl: '/api/admob/config',
        ip: '192.168.1.1'
      });
      
      monitoringService.trackRequest({
        method: 'GET',
        originalUrl: '/api/admob/config',
        ip: '192.168.1.2'
      });
      
      monitoringService.trackRequest({
        method: 'GET',
        originalUrl: '/api/admob/config',
        ip: '192.168.1.1' // Duplicate IP
      });
      
      const metrics = monitoringService.getMetrics();
      expect(metrics.uniqueIPs).toBe(2);
    });
    
    it('should handle missing request properties', () => {
      // Track request with missing properties
      monitoringService.trackRequest({});
      
      const metrics = monitoringService.getMetrics();
      expect(metrics.totalRequests).toBe(1);
      expect(metrics.requestsByEndpoint['unknown']).toBe(1);
      expect(metrics.requestsByMethod['unknown']).toBe(1);
    });
  });
  
  describe('Response Tracking', () => {
    it('should track response successfully', () => {
      const req = {
        method: 'GET',
        originalUrl: '/api/admob/config'
      };
      
      const res = {
        statusCode: 200
      };
      
      const startTime = Date.now() - 150; // 150ms ago
      
      monitoringService.trackResponse(req, res, startTime);
      
      const metrics = monitoringService.getMetrics();
      expect(metrics.responsesByStatusCode['200']).toBe(1);
      expect(metrics.totalResponseTime).toBeGreaterThanOrEqual(150);
      expect(metrics.responseCount).toBe(1);
      expect(metrics.averageResponseTime).toBeGreaterThanOrEqual(150);
    });
    
    it('should track multiple responses with different status codes', () => {
      const startTime = Date.now() - 100;
      
      // Track 200 response
      monitoringService.trackResponse(
        { method: 'GET', originalUrl: '/api/admob/config' },
        { statusCode: 200 },
        startTime
      );
      
      // Track 404 response
      monitoringService.trackResponse(
        { method: 'GET', originalUrl: '/api/not-found' },
        { statusCode: 404 },
        startTime
      );
      
      // Track 500 response
      monitoringService.trackResponse(
        { method: 'POST', originalUrl: '/api/error' },
        { statusCode: 500 },
        startTime
      );
      
      const metrics = monitoringService.getMetrics();
      expect(metrics.responsesByStatusCode['200']).toBe(1);
      expect(metrics.responsesByStatusCode['404']).toBe(1);
      expect(metrics.responsesByStatusCode['500']).toBe(1);
      expect(metrics.responseCount).toBe(3);
      
      // Check error tracking
      expect(metrics.errorCount).toBe(1); // Only 500 is counted as error
    });
    
    it('should calculate average response time correctly', () => {
      // Track response with 100ms
      monitoringService.trackResponse(
        { method: 'GET' },
        { statusCode: 200 },
        Date.now() - 100
      );
      
      // Track response with 300ms
      monitoringService.trackResponse(
        { method: 'GET' },
        { statusCode: 200 },
        Date.now() - 300
      );
      
      const metrics = monitoringService.getMetrics();
      expect(metrics.responseCount).toBe(2);
      expect(metrics.totalResponseTime).toBeGreaterThanOrEqual(400);
      expect(metrics.averageResponseTime).toBeGreaterThanOrEqual(200);
    });
    
    it('should handle missing response properties', () => {
      monitoringService.trackResponse({}, {}, Date.now());
      
      const metrics = monitoringService.getMetrics();
      expect(metrics.responsesByStatusCode['unknown']).toBe(1);
      expect(metrics.responseCount).toBe(1);
    });
  });
  
  describe('Cache Tracking', () => {
    it('should track cache hit', () => {
      monitoringService.trackCacheHit('admob_config');
      
      const metrics = monitoringService.getMetrics();
      expect(metrics.cacheHits).toBe(1);
      expect(metrics.cacheHitsByKey['admob_config']).toBe(1);
      expect(metrics.cacheHitRate).toBe(100); // 1 hit / 1 total * 100
    });
    
    it('should track cache miss', () => {
      monitoringService.trackCacheMiss('admob_config');
      
      const metrics = monitoringService.getMetrics();
      expect(metrics.cacheMisses).toBe(1);
      expect(metrics.cacheMissesByKey['admob_config']).toBe(1);
      expect(metrics.cacheHitRate).toBe(0); // 0 hits / 1 total * 100
    });
    
    it('should calculate cache hit rate correctly', () => {
      // 3 hits and 2 misses
      monitoringService.trackCacheHit('key1');
      monitoringService.trackCacheHit('key2');
      monitoringService.trackCacheHit('key1');
      monitoringService.trackCacheMiss('key3');
      monitoringService.trackCacheMiss('key2');
      
      const metrics = monitoringService.getMetrics();
      expect(metrics.cacheHits).toBe(3);
      expect(metrics.cacheMisses).toBe(2);
      expect(metrics.cacheHitRate).toBe(60); // 3 hits / 5 total * 100
      
      // Check by key
      expect(metrics.cacheHitsByKey['key1']).toBe(2);
      expect(metrics.cacheHitsByKey['key2']).toBe(1);
      expect(metrics.cacheMissesByKey['key2']).toBe(1);
      expect(metrics.cacheMissesByKey['key3']).toBe(1);
    });
  });
  
  describe('Ad Operation Tracking', () => {
    let testAd;
    
    beforeEach(async () => {
      // Create a test ad
      testAd = await new AdMob({
        adName: 'Test Banner Ad',
        adType: 'Banner',
        adUnitCode: 'ca-app-pub-1234567890123456/1234567890',
        status: true
      }).save();
    });
    
    afterEach(async () => {
      // Clear database after each test
      await AdMob.deleteMany({});
    });
    
    it('should track ad impression', () => {
      monitoringService.trackAdImpression(testAd._id.toString(), 'Banner');
      
      const metrics = monitoringService.getAdMetrics();
      expect(metrics.totalImpressions).toBe(1);
      expect(metrics.impressionsByType['Banner']).toBe(1);
      expect(metrics.impressionsByAdId[testAd._id.toString()]).toBe(1);
    });
    
    it('should track ad click', () => {
      monitoringService.trackAdClick(testAd._id.toString(), 'Banner');
      
      const metrics = monitoringService.getAdMetrics();
      expect(metrics.totalClicks).toBe(1);
      expect(metrics.clicksByType['Banner']).toBe(1);
      expect(metrics.clicksByAdId[testAd._id.toString()]).toBe(1);
    });
    
    it('should track multiple ad operations', () => {
      // Track impressions
      monitoringService.trackAdImpression(testAd._id.toString(), 'Banner');
      monitoringService.trackAdImpression(testAd._id.toString(), 'Banner');
      
      // Track clicks
      monitoringService.trackAdClick(testAd._id.toString(), 'Banner');
      
      const metrics = monitoringService.getAdMetrics();
      expect(metrics.totalImpressions).toBe(2);
      expect(metrics.totalClicks).toBe(1);
      expect(metrics.impressionsByType['Banner']).toBe(2);
      expect(metrics.clicksByType['Banner']).toBe(1);
      expect(metrics.impressionsByAdId[testAd._id.toString()]).toBe(2);
      expect(metrics.clicksByAdId[testAd._id.toString()]).toBe(1);
    });
    
    it('should calculate CTR correctly', () => {
      // 5 impressions and 2 clicks
      for (let i = 0; i < 5; i++) {
        monitoringService.trackAdImpression(testAd._id.toString(), 'Banner');
      }
      
      monitoringService.trackAdClick(testAd._id.toString(), 'Banner');
      monitoringService.trackAdClick(testAd._id.toString(), 'Banner');
      
      const metrics = monitoringService.getAdMetrics();
      expect(metrics.totalCTR).toBe(40); // 2 clicks / 5 impressions * 100
      expect(metrics.ctrByType['Banner']).toBe(40);
      expect(metrics.ctrByAdId[testAd._id.toString()]).toBe(40);
    });
    
    it('should track different ad types', async () => {
      // Create another ad with different type
      const interstitialAd = await new AdMob({
        adName: 'Test Interstitial Ad',
        adType: 'Interstitial',
        adUnitCode: 'ca-app-pub-1234567890123456/0987654321',
        status: true
      }).save();
      
      // Track impressions for both ad types
      monitoringService.trackAdImpression(testAd._id.toString(), 'Banner');
      monitoringService.trackAdImpression(interstitialAd._id.toString(), 'Interstitial');
      
      // Track clicks for both ad types
      monitoringService.trackAdClick(testAd._id.toString(), 'Banner');
      monitoringService.trackAdClick(interstitialAd._id.toString(), 'Interstitial');
      
      const metrics = monitoringService.getAdMetrics();
      expect(metrics.totalImpressions).toBe(2);
      expect(metrics.totalClicks).toBe(2);
      
      // Check by type
      expect(metrics.impressionsByType['Banner']).toBe(1);
      expect(metrics.impressionsByType['Interstitial']).toBe(1);
      expect(metrics.clicksByType['Banner']).toBe(1);
      expect(metrics.clicksByType['Interstitial']).toBe(1);
      
      // Check by ID
      expect(metrics.impressionsByAdId[testAd._id.toString()]).toBe(1);
      expect(metrics.impressionsByAdId[interstitialAd._id.toString()]).toBe(1);
      expect(metrics.clicksByAdId[testAd._id.toString()]).toBe(1);
      expect(metrics.clicksByAdId[interstitialAd._id.toString()]).toBe(1);
    });
  });
  
  describe('Error Tracking', () => {
    it('should track error successfully', () => {
      const error = new Error('Test error');
      error.stack = 'Error: Test error\n    at Object.<anonymous> (/app/test.js:10:10)';
      
      monitoringService.trackError(error, { endpoint: '/api/test' });
      
      const metrics = monitoringService.getMetrics();
      expect(metrics.errorCount).toBe(1);
      expect(metrics.errorsByEndpoint['/api/test']).toBe(1);
      
      // Check error details
      const errorDetails = metrics.recentErrors[0];
      expect(errorDetails.message).toBe('Test error');
      expect(errorDetails.stack).toContain('Error: Test error');
      expect(errorDetails.context.endpoint).toBe('/api/test');
      expect(errorDetails.timestamp).toBeDefined();
    });
    
    it('should limit recent errors to 100', () => {
      // Track 110 errors
      for (let i = 0; i < 110; i++) {
        monitoringService.trackError(new Error(`Error ${i}`));
      }
      
      const metrics = monitoringService.getMetrics();
      expect(metrics.errorCount).toBe(110);
      expect(metrics.recentErrors.length).toBe(100);
      expect(metrics.recentErrors[0].message).toBe('Error 109'); // Most recent first
    });
    
    it('should track errors by type', () => {
      monitoringService.trackError(new TypeError('Type error'));
      monitoringService.trackError(new RangeError('Range error'));
      monitoringService.trackError(new Error('Generic error'));
      
      const metrics = monitoringService.getMetrics();
      expect(metrics.errorCount).toBe(3);
      expect(metrics.errorsByType['TypeError']).toBe(1);
      expect(metrics.errorsByType['RangeError']).toBe(1);
      expect(metrics.errorsByType['Error']).toBe(1);
    });
  });
  
  describe('Alert Triggering', () => {
    const alertService = require('../services/alertService');
    
    it('should trigger alert when error threshold is reached', () => {
      // Configure threshold
      monitoringService.setAlertThresholds({
        errorCount: 5
      });
      
      // Track 6 errors
      for (let i = 0; i < 6; i++) {
        monitoringService.trackError(new Error(`Error ${i}`));
      }
      
      // Check if alert was triggered
      expect(alertService.sendAlert).toHaveBeenCalledWith(
        'Error threshold exceeded',
        expect.objectContaining({
          metric: 'errorCount',
          value: 6,
          threshold: 5
        })
      );
    });
    
    it('should trigger alert when response time threshold is reached', () => {
      // Configure threshold
      monitoringService.setAlertThresholds({
        averageResponseTime: 100
      });
      
      // Track slow response
      monitoringService.trackResponse(
        { method: 'GET', originalUrl: '/api/test' },
        { statusCode: 200 },
        Date.now() - 150
      );
      
      // Check if alert was triggered
      expect(alertService.sendAlert).toHaveBeenCalledWith(
        'Response time threshold exceeded',
        expect.objectContaining({
          metric: 'averageResponseTime',
          value: expect.any(Number),
          threshold: 100
        })
      );
    });
    
    it('should not trigger alert when below threshold', () => {
      // Configure threshold
      monitoringService.setAlertThresholds({
        errorCount: 5
      });
      
      // Track only 3 errors
      for (let i = 0; i < 3; i++) {
        monitoringService.trackError(new Error(`Error ${i}`));
      }
      
      // Check that alert was not triggered
      expect(alertService.sendAlert).not.toHaveBeenCalled();
    });
    
    it('should not trigger multiple alerts for the same threshold', () => {
      // Configure threshold
      monitoringService.setAlertThresholds({
        errorCount: 5
      });
      
      // Track 6 errors
      for (let i = 0; i < 6; i++) {
        monitoringService.trackError(new Error(`Error ${i}`));
      }
      
      // Reset mock
      alertService.sendAlert.mockClear();
      
      // Track another error
      monitoringService.trackError(new Error('Another error'));
      
      // Check that alert was not triggered again
      expect(alertService.sendAlert).not.toHaveBeenCalled();
    });
    
    it('should reset alert state when metrics are reset', () => {
      // Configure threshold
      monitoringService.setAlertThresholds({
        errorCount: 5
      });
      
      // Track 6 errors
      for (let i = 0; i < 6; i++) {
        monitoringService.trackError(new Error(`Error ${i}`));
      }
      
      // Reset metrics
      monitoringService.resetMetrics();
      
      // Reset mock
      alertService.sendAlert.mockClear();
      
      // Track 6 more errors
      for (let i = 0; i < 6; i++) {
        monitoringService.trackError(new Error(`Error ${i}`));
      }
      
      // Check that alert was triggered again
      expect(alertService.sendAlert).toHaveBeenCalled();
    });
  });
  
  describe('Metrics Reset', () => {
    it('should reset all metrics', () => {
      // Track some metrics
      monitoringService.trackRequest({ method: 'GET', originalUrl: '/api/test' });
      monitoringService.trackResponse(
        { method: 'GET', originalUrl: '/api/test' },
        { statusCode: 200 },
        Date.now() - 100
      );
      monitoringService.trackCacheHit('test');
      monitoringService.trackError(new Error('Test error'));
      monitoringService.trackAdImpression('ad123', 'Banner');
      
      // Reset metrics
      monitoringService.resetMetrics();
      
      // Check that metrics were reset
      const metrics = monitoringService.getMetrics();
      expect(metrics.totalRequests).toBe(0);
      expect(metrics.responseCount).toBe(0);
      expect(metrics.cacheHits).toBe(0);
      expect(metrics.errorCount).toBe(0);
      
      const adMetrics = monitoringService.getAdMetrics();
      expect(adMetrics.totalImpressions).toBe(0);
    });
  });
  
  describe('Middleware', () => {
    it('should create request tracking middleware', () => {
      const middleware = monitoringService.trackRequestMiddleware();
      
      // Mock request and response
      const req = {
        method: 'GET',
        originalUrl: '/api/test',
        ip: '192.168.1.1'
      };
      
      const res = {
        on: jest.fn().mockImplementation((event, callback) => {
          if (event === 'finish') {
            // Simulate response finish
            res.statusCode = 200;
            callback();
          }
          return res;
        }),
        statusCode: 200
      };
      
      const next = jest.fn();
      
      // Call middleware
      middleware(req, res, next);
      
      // Check that next was called
      expect(next).toHaveBeenCalled();
      
      // Check that request was tracked
      const metrics = monitoringService.getMetrics();
      expect(metrics.totalRequests).toBe(1);
      
      // Simulate response finish
      const finishCallback = res.on.mock.calls.find(call => call[0] === 'finish')[1];
      finishCallback();
      
      // Check that response was tracked
      expect(metrics.responsesByStatusCode['200']).toBe(1);
    });
  });
}); 