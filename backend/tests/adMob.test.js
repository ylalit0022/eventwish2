const mongoose = require('mongoose');
const { MongoMemoryServer } = require('mongodb-memory-server');
const request = require('supertest');
const express = require('express');
const { AdMob, adTypes } = require('../models/AdMob');
const adMobService = require('../services/adMobService');
const analyticsService = require('../services/analyticsService');
const monitoringService = require('../services/monitoringService');
const adMobController = require('../controllers/adMobController');
const { verifyToken, verifyApiKey, verifyClientApp } = require('../middleware/authMiddleware');

// Mock dependencies
jest.mock('../config/logger');
jest.mock('../services/analyticsService');

let mongoServer;
let app;

// Setup test app
const setupTestApp = () => {
  app = express();
  app.use(express.json());
  
  // Mock middleware
  const mockVerifyToken = (req, res, next) => {
    req.user = { id: 'test-user-id' };
    next();
  };
  
  const mockVerifyClientApp = (req, res, next) => {
    next();
  };
  
  // Setup routes
  const router = express.Router();
  router.get('/config', adMobController.getAdConfig);
  router.post('/impression/:adId', adMobController.trackImpression);
  router.post('/click/:adId', adMobController.trackClick);
  
  app.use('/api/admob', router);
  
  return app;
};

beforeAll(async () => {
  // Start in-memory MongoDB server
  mongoServer = await MongoMemoryServer.create();
  const uri = mongoServer.getUri();
  
  // Connect to in-memory database
  await mongoose.connect(uri, {
    useNewUrlParser: true,
    useUnifiedTopology: true
  });
  
  // Setup test app
  app = setupTestApp();
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

describe('AdMob Model', () => {
  it('should create a new ad successfully', async () => {
    const adData = {
      adName: 'Test Banner Ad',
      adType: 'Banner',
      adUnitCode: 'ca-app-pub-1234567890123456/1234567890',
      status: true
    };
    
    const ad = new AdMob(adData);
    const savedAd = await ad.save();
    
    expect(savedAd._id).toBeDefined();
    expect(savedAd.adName).toBe(adData.adName);
    expect(savedAd.adType).toBe(adData.adType);
    expect(savedAd.adUnitCode).toBe(adData.adUnitCode);
    expect(savedAd.status).toBe(adData.status);
    expect(savedAd.impressions).toBe(0);
    expect(savedAd.clicks).toBe(0);
    expect(savedAd.ctr).toBe(0);
    expect(savedAd.revenue).toBe(0);
  });
  
  it('should not allow duplicate ad unit codes', async () => {
    const adData = {
      adName: 'Test Banner Ad',
      adType: 'Banner',
      adUnitCode: 'ca-app-pub-1234567890123456/1234567890',
      status: true
    };
    
    await new AdMob(adData).save();
    
    const duplicateAd = new AdMob({
      adName: 'Another Test Banner Ad',
      adType: 'Banner',
      adUnitCode: 'ca-app-pub-1234567890123456/1234567890',
      status: true
    });
    
    await expect(duplicateAd.save()).rejects.toThrow();
  });
  
  it('should validate ad unit code format', async () => {
    const adData = {
      adName: 'Test Banner Ad',
      adType: 'Banner',
      adUnitCode: 'invalid-format',
      status: true
    };
    
    const ad = new AdMob(adData);
    
    await expect(ad.save()).rejects.toThrow();
  });
  
  it('should validate ad type', async () => {
    const adData = {
      adName: 'Test Banner Ad',
      adType: 'InvalidType',
      adUnitCode: 'ca-app-pub-1234567890123456/1234567890',
      status: true
    };
    
    const ad = new AdMob(adData);
    
    await expect(ad.save()).rejects.toThrow();
  });
});

describe('AdMob Service', () => {
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
  
  it('should get ad by ID', async () => {
    const ad = await adMobService.getAdById(testAd._id);
    
    expect(ad).toBeDefined();
    expect(ad.id).toBe(testAd.id);
    expect(ad.adName).toBe(testAd.adName);
  });
  
  it('should get active ads', async () => {
    // Create an inactive ad
    await new AdMob({
      adName: 'Inactive Ad',
      adType: 'Banner',
      adUnitCode: 'ca-app-pub-1234567890123456/0987654321',
      status: false
    }).save();
    
    const ads = await adMobService.getActiveAds();
    
    expect(ads).toBeDefined();
    expect(ads.length).toBe(1);
    expect(ads[0].id).toBe(testAd.id);
  });
  
  it('should get ads by type', async () => {
    // Create an ad of different type
    await new AdMob({
      adName: 'Interstitial Ad',
      adType: 'Interstitial',
      adUnitCode: 'ca-app-pub-1234567890123456/0987654321',
      status: true
    }).save();
    
    const bannerAds = await adMobService.getAdsByType('Banner');
    const interstitialAds = await adMobService.getAdsByType('Interstitial');
    
    expect(bannerAds).toBeDefined();
    expect(bannerAds.length).toBe(1);
    expect(bannerAds[0].id).toBe(testAd.id);
    
    expect(interstitialAds).toBeDefined();
    expect(interstitialAds.length).toBe(1);
    expect(interstitialAds[0].adType).toBe('Interstitial');
  });
  
  it('should get optimal ad configuration', async () => {
    const context = {
      deviceType: 'mobile',
      platform: 'android'
    };
    
    const adConfig = await adMobService.getOptimalAdConfig(context, 'Banner');
    
    expect(adConfig).toBeDefined();
    expect(adConfig.id).toBe(testAd.id);
  });
  
  it('should track impression', async () => {
    // Mock analytics service
    analyticsService.trackImpression.mockResolvedValue({});
    
    const context = {
      deviceType: 'mobile',
      platform: 'android'
    };
    
    await adMobService.trackImpression(testAd._id, context);
    
    expect(analyticsService.trackImpression).toHaveBeenCalledWith(testAd._id, context);
  });
  
  it('should track click', async () => {
    // Mock analytics service
    analyticsService.trackClick.mockResolvedValue({});
    
    const context = {
      deviceType: 'mobile',
      platform: 'android'
    };
    
    await adMobService.trackClick(testAd._id, context);
    
    expect(analyticsService.trackClick).toHaveBeenCalledWith(testAd._id, context);
  });
  
  it('should get ad types', async () => {
    const types = await adMobService.getAdTypes();
    
    expect(types).toBeDefined();
    expect(Array.isArray(types)).toBe(true);
    expect(types).toEqual(adTypes);
  });
});

describe('AdMob Controller', () => {
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
  
  it('should get ad configuration', async () => {
    const response = await request(app)
      .get('/api/admob/config')
      .query({
        adType: 'Banner',
        deviceType: 'mobile',
        platform: 'android'
      });
    
    expect(response.status).toBe(200);
    expect(response.body.success).toBe(true);
    expect(response.body.data).toBeDefined();
    expect(response.body.data.adConfig).toBeDefined();
    expect(response.body.data.adConfig.adType).toBe('Banner');
  });
  
  it('should return 400 if adType is missing', async () => {
    const response = await request(app)
      .get('/api/admob/config')
      .query({
        deviceType: 'mobile',
        platform: 'android'
      });
    
    expect(response.status).toBe(400);
    expect(response.body.success).toBe(false);
    expect(response.body.error).toBe('MISSING_AD_TYPE');
  });
  
  it('should track impression', async () => {
    // Mock analytics service
    analyticsService.trackImpression.mockResolvedValue({});
    
    const response = await request(app)
      .post(`/api/admob/impression/${testAd._id}`)
      .send({
        deviceType: 'mobile',
        platform: 'android'
      });
    
    expect(response.status).toBe(200);
    expect(response.body.success).toBe(true);
    expect(analyticsService.trackImpression).toHaveBeenCalled();
  });
  
  it('should track click', async () => {
    // Mock analytics service
    analyticsService.trackClick.mockResolvedValue({});
    
    const response = await request(app)
      .post(`/api/admob/click/${testAd._id}`)
      .send({
        deviceType: 'mobile',
        platform: 'android'
      });
    
    expect(response.status).toBe(200);
    expect(response.body.success).toBe(true);
    expect(analyticsService.trackClick).toHaveBeenCalled();
  });
});

describe('Authentication Middleware', () => {
  it('should verify token', () => {
    const req = {
      header: jest.fn().mockReturnValue('valid-token')
    };
    const res = {
      status: jest.fn().mockReturnThis(),
      json: jest.fn()
    };
    const next = jest.fn();
    
    // Mock jwt.verify
    const jwt = require('jsonwebtoken');
    jwt.verify = jest.fn().mockReturnValue({ user: { id: 'test-user-id' } });
    
    verifyToken(req, res, next);
    
    expect(req.header).toHaveBeenCalledWith('x-auth-token');
    expect(jwt.verify).toHaveBeenCalled();
    expect(req.user).toEqual({ id: 'test-user-id' });
    expect(next).toHaveBeenCalled();
  });
  
  it('should verify API key', () => {
    const req = {
      header: jest.fn().mockReturnValue('valid-api-key')
    };
    const res = {
      status: jest.fn().mockReturnThis(),
      json: jest.fn()
    };
    const next = jest.fn();
    
    // Mock process.env
    process.env.API_KEY = 'valid-api-key';
    
    verifyApiKey(req, res, next);
    
    expect(req.header).toHaveBeenCalledWith('x-api-key');
    expect(next).toHaveBeenCalled();
  });
  
  it('should verify client app', () => {
    const req = {
      header: jest.fn().mockReturnValue('valid-app-signature')
    };
    const res = {
      status: jest.fn().mockReturnThis(),
      json: jest.fn()
    };
    const next = jest.fn();
    
    // Mock process.env
    process.env.VALID_APP_SIGNATURES = 'valid-app-signature,another-signature';
    
    verifyClientApp(req, res, next);
    
    expect(req.header).toHaveBeenCalledWith('x-app-signature');
    expect(next).toHaveBeenCalled();
  });
}); 