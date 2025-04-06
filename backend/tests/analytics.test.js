const mongoose = require('mongoose');
const { MongoMemoryServer } = require('mongodb-memory-server');
const { AdMob } = require('../models/AdMob');
const analyticsService = require('../services/analyticsService');

// Mock dependencies
jest.mock('../config/logger');

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

beforeEach(async () => {
  // Clear database before each test
  await AdMob.deleteMany({});
  
  // Reset mocks
  jest.clearAllMocks();
});

describe('Analytics Service', () => {
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
  
  describe('Impression Tracking', () => {
    it('should track impression successfully', async () => {
      const context = {
        deviceType: 'mobile',
        platform: 'android',
        ip: '192.168.1.1',
        userAgent: 'Mozilla/5.0'
      };
      
      const result = await analyticsService.trackImpression(testAd._id, context);
      
      // Verify result
      expect(result).toBeDefined();
      expect(result.type).toBe('impression');
      expect(result.adId).toEqual(testAd._id);
      expect(result.adType).toBe(testAd.adType);
      expect(result.adName).toBe(testAd.adName);
      expect(result.context).toMatchObject(context);
      
      // Verify ad was updated
      const updatedAd = await AdMob.findById(testAd._id);
      expect(updatedAd.impressions).toBe(1);
      expect(updatedAd.impressionData).toBeDefined();
      expect(updatedAd.impressionData.length).toBe(1);
      expect(updatedAd.impressionData[0].context).toMatchObject(context);
    });
    
    it('should handle multiple impressions', async () => {
      const context1 = { deviceType: 'mobile', platform: 'android' };
      const context2 = { deviceType: 'tablet', platform: 'ios' };
      
      await analyticsService.trackImpression(testAd._id, context1);
      await analyticsService.trackImpression(testAd._id, context2);
      
      // Verify ad was updated
      const updatedAd = await AdMob.findById(testAd._id);
      expect(updatedAd.impressions).toBe(2);
      expect(updatedAd.impressionData.length).toBe(2);
      expect(updatedAd.impressionData[0].context.deviceType).toBe('tablet');
      expect(updatedAd.impressionData[1].context.deviceType).toBe('mobile');
    });
    
    it('should limit impression data to 100 entries', async () => {
      // Track 110 impressions
      for (let i = 0; i < 110; i++) {
        await analyticsService.trackImpression(testAd._id, { index: i });
      }
      
      // Verify ad was updated
      const updatedAd = await AdMob.findById(testAd._id);
      expect(updatedAd.impressions).toBe(110);
      expect(updatedAd.impressionData.length).toBe(100);
      expect(updatedAd.impressionData[0].context.index).toBe(109);
      expect(updatedAd.impressionData[99].context.index).toBe(10);
    });
    
    it('should handle invalid ad ID', async () => {
      await expect(analyticsService.trackImpression('invalid-id', {}))
        .rejects.toThrow('Invalid ad ID format');
    });
    
    it('should handle non-existent ad', async () => {
      const nonExistentId = new mongoose.Types.ObjectId();
      await expect(analyticsService.trackImpression(nonExistentId, {}))
        .rejects.toThrow('Ad not found');
    });
  });
  
  describe('Click Tracking', () => {
    it('should track click successfully', async () => {
      const context = {
        deviceType: 'mobile',
        platform: 'android',
        ip: '192.168.1.1',
        userAgent: 'Mozilla/5.0'
      };
      
      // Track an impression first
      await analyticsService.trackImpression(testAd._id, {});
      
      const result = await analyticsService.trackClick(testAd._id, context);
      
      // Verify result
      expect(result).toBeDefined();
      expect(result.type).toBe('click');
      expect(result.adId).toEqual(testAd._id);
      expect(result.adType).toBe(testAd.adType);
      expect(result.adName).toBe(testAd.adName);
      expect(result.context).toMatchObject(context);
      
      // Verify ad was updated
      const updatedAd = await AdMob.findById(testAd._id);
      expect(updatedAd.impressions).toBe(1);
      expect(updatedAd.clicks).toBe(1);
      expect(updatedAd.ctr).toBe(100); // 1 click / 1 impression * 100
      expect(updatedAd.clickData).toBeDefined();
      expect(updatedAd.clickData.length).toBe(1);
      expect(updatedAd.clickData[0].context).toMatchObject(context);
    });
    
    it('should calculate CTR correctly', async () => {
      // Track 10 impressions
      for (let i = 0; i < 10; i++) {
        await analyticsService.trackImpression(testAd._id, {});
      }
      
      // Track 2 clicks
      await analyticsService.trackClick(testAd._id, {});
      await analyticsService.trackClick(testAd._id, {});
      
      // Verify ad was updated
      const updatedAd = await AdMob.findById(testAd._id);
      expect(updatedAd.impressions).toBe(10);
      expect(updatedAd.clicks).toBe(2);
      expect(updatedAd.ctr).toBe(20); // 2 clicks / 10 impressions * 100
    });
    
    it('should limit click data to 100 entries', async () => {
      // Track an impression first
      await analyticsService.trackImpression(testAd._id, {});
      
      // Track 110 clicks
      for (let i = 0; i < 110; i++) {
        await analyticsService.trackClick(testAd._id, { index: i });
      }
      
      // Verify ad was updated
      const updatedAd = await AdMob.findById(testAd._id);
      expect(updatedAd.clicks).toBe(110);
      expect(updatedAd.clickData.length).toBe(100);
      expect(updatedAd.clickData[0].context.index).toBe(109);
      expect(updatedAd.clickData[99].context.index).toBe(10);
    });
  });
  
  describe('Revenue Tracking', () => {
    it('should track revenue successfully', async () => {
      const amount = 10.5;
      const currency = 'USD';
      
      const result = await analyticsService.trackRevenue(testAd._id, amount, currency);
      
      // Verify result
      expect(result).toBeDefined();
      expect(result.adId).toEqual(testAd._id);
      expect(result.adName).toBe(testAd.adName);
      expect(result.revenue).toBe(amount);
      expect(result.currency).toBe(currency);
      
      // Verify ad was updated
      const updatedAd = await AdMob.findById(testAd._id);
      expect(updatedAd.revenue).toBe(amount);
      expect(updatedAd.revenueData).toBeDefined();
      expect(updatedAd.revenueData.length).toBe(1);
      expect(updatedAd.revenueData[0].amount).toBe(amount);
      expect(updatedAd.revenueData[0].currency).toBe(currency);
    });
    
    it('should accumulate revenue', async () => {
      await analyticsService.trackRevenue(testAd._id, 10, 'USD');
      await analyticsService.trackRevenue(testAd._id, 20, 'USD');
      
      // Verify ad was updated
      const updatedAd = await AdMob.findById(testAd._id);
      expect(updatedAd.revenue).toBe(30);
      expect(updatedAd.revenueData.length).toBe(2);
    });
    
    it('should validate amount', async () => {
      await expect(analyticsService.trackRevenue(testAd._id, -10, 'USD'))
        .rejects.toThrow('Invalid revenue amount');
      
      await expect(analyticsService.trackRevenue(testAd._id, 'invalid', 'USD'))
        .rejects.toThrow('Invalid revenue amount');
    });
  });
  
  describe('Analytics Data Retrieval', () => {
    beforeEach(async () => {
      // Track some data
      await analyticsService.trackImpression(testAd._id, { deviceType: 'mobile' });
      await analyticsService.trackImpression(testAd._id, { deviceType: 'tablet' });
      await analyticsService.trackClick(testAd._id, { platform: 'android' });
      await analyticsService.trackRevenue(testAd._id, 15, 'USD');
    });
    
    it('should get ad analytics', async () => {
      const analytics = await analyticsService.getAdAnalytics(testAd._id);
      
      expect(analytics).toBeDefined();
      expect(analytics.adId).toEqual(testAd._id);
      expect(analytics.adName).toBe(testAd.adName);
      expect(analytics.adType).toBe(testAd.adType);
      expect(analytics.impressions).toBe(2);
      expect(analytics.clicks).toBe(1);
      expect(analytics.ctr).toBe(50); // 1 click / 2 impressions * 100
      expect(analytics.revenue).toBe(15);
      expect(analytics.recentImpressions.length).toBe(2);
      expect(analytics.recentClicks.length).toBe(1);
    });
    
    it('should get summary analytics', async () => {
      // Create another ad
      const anotherAd = await new AdMob({
        adName: 'Another Ad',
        adType: 'Interstitial',
        adUnitCode: 'ca-app-pub-1234567890123456/0987654321',
        status: true
      }).save();
      
      // Track some data for the other ad
      await analyticsService.trackImpression(anotherAd._id, { deviceType: 'desktop' });
      await analyticsService.trackClick(anotherAd._id, { platform: 'ios' });
      await analyticsService.trackRevenue(anotherAd._id, 25, 'USD');
      
      const summary = await analyticsService.getSummaryAnalytics();
      
      expect(summary).toBeDefined();
      expect(summary.totalAds).toBe(2);
      expect(summary.totalImpressions).toBe(3);
      expect(summary.totalClicks).toBe(2);
      expect(summary.totalRevenue).toBe(40);
      expect(summary.averageCtr).toBeCloseTo(66.67, 1); // 2 clicks / 3 impressions * 100
      
      // Check breakdown by ad type
      expect(summary.byAdType.Banner).toBeDefined();
      expect(summary.byAdType.Banner.impressions).toBe(2);
      expect(summary.byAdType.Banner.clicks).toBe(1);
      expect(summary.byAdType.Banner.revenue).toBe(15);
      
      expect(summary.byAdType.Interstitial).toBeDefined();
      expect(summary.byAdType.Interstitial.impressions).toBe(1);
      expect(summary.byAdType.Interstitial.clicks).toBe(1);
      expect(summary.byAdType.Interstitial.revenue).toBe(25);
      
      // Check breakdown by device
      expect(summary.byDevice.mobile).toBeDefined();
      expect(summary.byDevice.mobile.impressions).toBe(1);
      expect(summary.byDevice.tablet).toBeDefined();
      expect(summary.byDevice.tablet.impressions).toBe(1);
      expect(summary.byDevice.desktop).toBeDefined();
      expect(summary.byDevice.desktop.impressions).toBe(1);
      
      // Check breakdown by platform
      expect(summary.byPlatform.android).toBeDefined();
      expect(summary.byPlatform.android.clicks).toBe(1);
      expect(summary.byPlatform.ios).toBeDefined();
      expect(summary.byPlatform.ios.clicks).toBe(1);
      
      // Check ad performance array
      expect(summary.adPerformance.length).toBe(2);
    });
    
    it('should filter summary analytics by ad type', async () => {
      // Create another ad
      const anotherAd = await new AdMob({
        adName: 'Another Ad',
        adType: 'Interstitial',
        adUnitCode: 'ca-app-pub-1234567890123456/0987654321',
        status: true
      }).save();
      
      // Track some data for the other ad
      await analyticsService.trackImpression(anotherAd._id, {});
      
      const summary = await analyticsService.getSummaryAnalytics({ adType: 'Banner' });
      
      expect(summary).toBeDefined();
      expect(summary.totalAds).toBe(1);
      expect(summary.totalImpressions).toBe(2);
      expect(summary.adPerformance.length).toBe(1);
      expect(summary.adPerformance[0].adType).toBe('Banner');
    });
  });
  
  describe('Cache Management', () => {
    it('should invalidate analytics cache for a specific ad', async () => {
      // Mock cache.get and cache.del
      const originalGet = analyticsService.analyticsCache.get;
      const originalDel = analyticsService.analyticsCache.del;
      const originalKeys = analyticsService.analyticsCache.keys;
      
      analyticsService.analyticsCache.get = jest.fn().mockReturnValue(null);
      analyticsService.analyticsCache.del = jest.fn();
      analyticsService.analyticsCache.keys = jest.fn().mockReturnValue([
        'admob_analytics_summary_{}',
        'admob_analytics_summary_{"adType":"Banner"}'
      ]);
      
      // Call the method
      analyticsService.invalidateAnalyticsCache(testAd._id);
      
      // Verify cache.del was called
      expect(analyticsService.analyticsCache.del).toHaveBeenCalledWith(`admob_analytics_${testAd._id}`);
      expect(analyticsService.analyticsCache.del).toHaveBeenCalledWith('admob_analytics_summary_{}');
      expect(analyticsService.analyticsCache.del).toHaveBeenCalledWith('admob_analytics_summary_{"adType":"Banner"}');
      
      // Restore original methods
      analyticsService.analyticsCache.get = originalGet;
      analyticsService.analyticsCache.del = originalDel;
      analyticsService.analyticsCache.keys = originalKeys;
    });
    
    it('should invalidate all analytics caches', async () => {
      // Mock cache.keys and cache.del
      const originalKeys = analyticsService.analyticsCache.keys;
      const originalDel = analyticsService.analyticsCache.del;
      
      analyticsService.analyticsCache.keys = jest.fn().mockReturnValue([
        'admob_analytics_123',
        'admob_analytics_456',
        'admob_analytics_summary_{}',
        'other_cache_key'
      ]);
      analyticsService.analyticsCache.del = jest.fn();
      
      // Call the method
      analyticsService.invalidateAllAnalyticsCaches();
      
      // Verify cache.del was called for analytics keys only
      expect(analyticsService.analyticsCache.del).toHaveBeenCalledWith('admob_analytics_123');
      expect(analyticsService.analyticsCache.del).toHaveBeenCalledWith('admob_analytics_456');
      expect(analyticsService.analyticsCache.del).toHaveBeenCalledWith('admob_analytics_summary_{}');
      expect(analyticsService.analyticsCache.del).not.toHaveBeenCalledWith('other_cache_key');
      
      // Restore original methods
      analyticsService.analyticsCache.keys = originalKeys;
      analyticsService.analyticsCache.del = originalDel;
    });
  });
}); 