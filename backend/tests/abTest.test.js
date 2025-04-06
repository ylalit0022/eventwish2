const mongoose = require('mongoose');
const { MongoMemoryServer } = require('mongodb-memory-server');
const { ABTest } = require('../models/ABTest');
const { AdMob } = require('../models/AdMob');
const abTestService = require('../services/abTestService');
const abTestController = require('../controllers/abTestController');
const cacheService = require('../services/cacheService');

// Mock dependencies
jest.mock('../config/logger', () => ({
  info: jest.fn(),
  error: jest.fn(),
  debug: jest.fn(),
  warn: jest.fn()
}));

jest.mock('../services/analyticsService', () => ({
  trackImpression: jest.fn().mockResolvedValue(true),
  trackClick: jest.fn().mockResolvedValue(true)
}));

jest.mock('../services/adMobService', () => ({
  getActiveAdByType: jest.fn().mockImplementation(async (adType) => {
    return {
      _id: 'default-ad-id',
      adName: 'Default Ad',
      adType,
      adUnitCode: 'default-ad-unit-code',
      parameters: { key: 'value' }
    };
  })
}));

// Mock request and response objects
const mockRequest = (params = {}, query = {}, body = {}, user = null, headers = {}) => ({
  params,
  query,
  body,
  user,
  headers,
  ip: '127.0.0.1'
});

const mockResponse = () => {
  const res = {};
  res.status = jest.fn().mockReturnValue(res);
  res.json = jest.fn().mockReturnValue(res);
  return res;
};

describe('A/B Test Functionality', () => {
  let mongoServer;
  
  beforeAll(async () => {
    // Start in-memory MongoDB server
    mongoServer = await MongoMemoryServer.create();
    const uri = mongoServer.getUri();
    
    // Connect to in-memory database
    await mongoose.connect(uri);
    
    // Clear cache
    cacheService.flushAll();
  });
  
  afterAll(async () => {
    // Disconnect from in-memory database
    await mongoose.disconnect();
    
    // Stop in-memory MongoDB server
    await mongoServer.stop();
  });
  
  beforeEach(async () => {
    // Clear database collections
    await ABTest.deleteMany({});
    await AdMob.deleteMany({});
    
    // Clear cache
    cacheService.flushAll();
  });
  
  describe('ABTest Model', () => {
    it('should create a valid A/B test', async () => {
      // Create test data
      const testData = {
        name: 'Test Campaign',
        description: 'Test description',
        adType: 'banner',
        status: 'draft',
        variants: [
          {
            name: 'Control',
            description: 'Control variant',
            weight: 50,
            adConfig: {
              adId: new mongoose.Types.ObjectId(),
              parameters: { key1: 'value1' }
            }
          },
          {
            name: 'Variant B',
            description: 'Test variant',
            weight: 50,
            adConfig: {
              adId: new mongoose.Types.ObjectId(),
              parameters: { key2: 'value2' }
            }
          }
        ],
        targetingRules: [
          {
            type: 'platform',
            operator: 'equals',
            value: 'android'
          }
        ],
        metrics: ['impressions', 'clicks'],
        trafficAllocation: 100
      };
      
      // Create test
      const test = new ABTest(testData);
      await test.save();
      
      // Verify test was created
      const savedTest = await ABTest.findById(test._id);
      expect(savedTest).toBeTruthy();
      expect(savedTest.name).toBe('Test Campaign');
      expect(savedTest.variants.length).toBe(2);
      expect(savedTest.status).toBe('draft');
    });
    
    it('should require at least two variants', async () => {
      // Create test data with only one variant
      const testData = {
        name: 'Test Campaign',
        description: 'Test description',
        adType: 'banner',
        status: 'draft',
        variants: [
          {
            name: 'Control',
            description: 'Control variant',
            weight: 100,
            adConfig: {
              adId: new mongoose.Types.ObjectId(),
              parameters: { key1: 'value1' }
            }
          }
        ],
        metrics: ['impressions', 'clicks'],
        trafficAllocation: 100
      };
      
      // Create test
      const test = new ABTest(testData);
      
      // Expect validation error
      await expect(test.save()).rejects.toThrow();
    });
    
    it('should require variant weights to sum to 100', async () => {
      // Create test data with variant weights that don't sum to 100
      const testData = {
        name: 'Test Campaign',
        description: 'Test description',
        adType: 'banner',
        status: 'draft',
        variants: [
          {
            name: 'Control',
            description: 'Control variant',
            weight: 40,
            adConfig: {
              adId: new mongoose.Types.ObjectId(),
              parameters: { key1: 'value1' }
            }
          },
          {
            name: 'Variant B',
            description: 'Test variant',
            weight: 40,
            adConfig: {
              adId: new mongoose.Types.ObjectId(),
              parameters: { key2: 'value2' }
            }
          }
        ],
        metrics: ['impressions', 'clicks'],
        trafficAllocation: 100
      };
      
      // Create test
      const test = new ABTest(testData);
      
      // Expect validation error
      await expect(test.save()).rejects.toThrow();
    });
  });
  
  describe('ABTest Service', () => {
    let testAd1, testAd2;
    
    beforeEach(async () => {
      // Create test ads
      testAd1 = new AdMob({
        adName: 'Test Ad 1',
        adType: 'banner',
        adUnitCode: 'test-ad-unit-1',
        status: 'active',
        parameters: { key1: 'value1' }
      });
      await testAd1.save();
      
      testAd2 = new AdMob({
        adName: 'Test Ad 2',
        adType: 'banner',
        adUnitCode: 'test-ad-unit-2',
        status: 'active',
        parameters: { key2: 'value2' }
      });
      await testAd2.save();
    });
    
    it('should create a test', async () => {
      // Create test data
      const testData = {
        name: 'Test Campaign',
        description: 'Test description',
        adType: 'banner',
        status: 'draft',
        variants: [
          {
            name: 'Control',
            description: 'Control variant',
            weight: 50,
            adConfig: {
              adId: testAd1._id,
              parameters: { key1: 'value1' }
            }
          },
          {
            name: 'Variant B',
            description: 'Test variant',
            weight: 50,
            adConfig: {
              adId: testAd2._id,
              parameters: { key2: 'value2' }
            }
          }
        ],
        metrics: ['impressions', 'clicks'],
        trafficAllocation: 100
      };
      
      // Create test
      const test = await abTestService.createTest(testData);
      
      // Verify test was created
      expect(test).toBeTruthy();
      expect(test.name).toBe('Test Campaign');
      expect(test.variants.length).toBe(2);
      expect(test.status).toBe('draft');
    });
    
    it('should get a test by ID', async () => {
      // Create test data
      const testData = {
        name: 'Test Campaign',
        description: 'Test description',
        adType: 'banner',
        status: 'draft',
        variants: [
          {
            name: 'Control',
            description: 'Control variant',
            weight: 50,
            adConfig: {
              adId: testAd1._id,
              parameters: { key1: 'value1' }
            }
          },
          {
            name: 'Variant B',
            description: 'Test variant',
            weight: 50,
            adConfig: {
              adId: testAd2._id,
              parameters: { key2: 'value2' }
            }
          }
        ],
        metrics: ['impressions', 'clicks'],
        trafficAllocation: 100
      };
      
      // Create test
      const test = await abTestService.createTest(testData);
      
      // Get test by ID
      const retrievedTest = await abTestService.getTestById(test._id);
      
      // Verify test was retrieved
      expect(retrievedTest).toBeTruthy();
      expect(retrievedTest.name).toBe('Test Campaign');
      expect(retrievedTest._id.toString()).toBe(test._id.toString());
    });
    
    it('should update a test', async () => {
      // Create test data
      const testData = {
        name: 'Test Campaign',
        description: 'Test description',
        adType: 'banner',
        status: 'draft',
        variants: [
          {
            name: 'Control',
            description: 'Control variant',
            weight: 50,
            adConfig: {
              adId: testAd1._id,
              parameters: { key1: 'value1' }
            }
          },
          {
            name: 'Variant B',
            description: 'Test variant',
            weight: 50,
            adConfig: {
              adId: testAd2._id,
              parameters: { key2: 'value2' }
            }
          }
        ],
        metrics: ['impressions', 'clicks'],
        trafficAllocation: 100
      };
      
      // Create test
      const test = await abTestService.createTest(testData);
      
      // Update test
      const updatedTest = await abTestService.updateTest(test._id, {
        name: 'Updated Campaign',
        description: 'Updated description'
      });
      
      // Verify test was updated
      expect(updatedTest).toBeTruthy();
      expect(updatedTest.name).toBe('Updated Campaign');
      expect(updatedTest.description).toBe('Updated description');
    });
    
    it('should not update an active test (except for status)', async () => {
      // Create test data
      const testData = {
        name: 'Test Campaign',
        description: 'Test description',
        adType: 'banner',
        status: 'active',
        startDate: new Date(),
        variants: [
          {
            name: 'Control',
            description: 'Control variant',
            weight: 50,
            adConfig: {
              adId: testAd1._id,
              parameters: { key1: 'value1' }
            }
          },
          {
            name: 'Variant B',
            description: 'Test variant',
            weight: 50,
            adConfig: {
              adId: testAd2._id,
              parameters: { key2: 'value2' }
            }
          }
        ],
        metrics: ['impressions', 'clicks'],
        trafficAllocation: 100
      };
      
      // Create test
      const test = new ABTest(testData);
      await test.save();
      
      // Try to update test
      await expect(abTestService.updateTest(test._id, {
        name: 'Updated Campaign'
      })).rejects.toThrow('Cannot update active test');
      
      // Update status only
      const updatedTest = await abTestService.updateTest(test._id, {
        status: 'completed'
      });
      
      // Verify status was updated
      expect(updatedTest).toBeTruthy();
      expect(updatedTest.status).toBe('completed');
    });
    
    it('should delete a test', async () => {
      // Create test data
      const testData = {
        name: 'Test Campaign',
        description: 'Test description',
        adType: 'banner',
        status: 'draft',
        variants: [
          {
            name: 'Control',
            description: 'Control variant',
            weight: 50,
            adConfig: {
              adId: testAd1._id,
              parameters: { key1: 'value1' }
            }
          },
          {
            name: 'Variant B',
            description: 'Test variant',
            weight: 50,
            adConfig: {
              adId: testAd2._id,
              parameters: { key2: 'value2' }
            }
          }
        ],
        metrics: ['impressions', 'clicks'],
        trafficAllocation: 100
      };
      
      // Create test
      const test = await abTestService.createTest(testData);
      
      // Delete test
      await abTestService.deleteTest(test._id);
      
      // Verify test was deleted
      const deletedTest = await ABTest.findById(test._id);
      expect(deletedTest).toBeNull();
    });
    
    it('should not delete an active test', async () => {
      // Create test data
      const testData = {
        name: 'Test Campaign',
        description: 'Test description',
        adType: 'banner',
        status: 'active',
        startDate: new Date(),
        variants: [
          {
            name: 'Control',
            description: 'Control variant',
            weight: 50,
            adConfig: {
              adId: testAd1._id,
              parameters: { key1: 'value1' }
            }
          },
          {
            name: 'Variant B',
            description: 'Test variant',
            weight: 50,
            adConfig: {
              adId: testAd2._id,
              parameters: { key2: 'value2' }
            }
          }
        ],
        metrics: ['impressions', 'clicks'],
        trafficAllocation: 100
      };
      
      // Create test
      const test = new ABTest(testData);
      await test.save();
      
      // Try to delete test
      await expect(abTestService.deleteTest(test._id)).rejects.toThrow('Cannot delete active test');
      
      // Verify test was not deleted
      const notDeletedTest = await ABTest.findById(test._id);
      expect(notDeletedTest).toBeTruthy();
    });
    
    it('should get active tests by ad type', async () => {
      // Create test data for active test
      const activeTestData = {
        name: 'Active Campaign',
        description: 'Active test description',
        adType: 'banner',
        status: 'active',
        startDate: new Date(),
        variants: [
          {
            name: 'Control',
            description: 'Control variant',
            weight: 50,
            adConfig: {
              adId: testAd1._id,
              parameters: { key1: 'value1' }
            }
          },
          {
            name: 'Variant B',
            description: 'Test variant',
            weight: 50,
            adConfig: {
              adId: testAd2._id,
              parameters: { key2: 'value2' }
            }
          }
        ],
        metrics: ['impressions', 'clicks'],
        trafficAllocation: 100
      };
      
      // Create test data for draft test
      const draftTestData = {
        name: 'Draft Campaign',
        description: 'Draft test description',
        adType: 'banner',
        status: 'draft',
        variants: [
          {
            name: 'Control',
            description: 'Control variant',
            weight: 50,
            adConfig: {
              adId: testAd1._id,
              parameters: { key1: 'value1' }
            }
          },
          {
            name: 'Variant B',
            description: 'Test variant',
            weight: 50,
            adConfig: {
              adId: testAd2._id,
              parameters: { key2: 'value2' }
            }
          }
        ],
        metrics: ['impressions', 'clicks'],
        trafficAllocation: 100
      };
      
      // Create tests
      const activeTest = new ABTest(activeTestData);
      await activeTest.save();
      
      const draftTest = new ABTest(draftTestData);
      await draftTest.save();
      
      // Get active tests
      const activeTests = await abTestService.getActiveTestsByAdType('banner');
      
      // Verify only active test was returned
      expect(activeTests).toBeTruthy();
      expect(activeTests.length).toBe(1);
      expect(activeTests[0].name).toBe('Active Campaign');
      expect(activeTests[0].status).toBe('active');
    });
    
    it('should evaluate targeting rules correctly', () => {
      // Create test with targeting rules
      const test = {
        targetingRules: [
          {
            type: 'platform',
            operator: 'equals',
            value: 'android'
          },
          {
            type: 'country',
            operator: 'equals',
            value: 'US'
          }
        ]
      };
      
      // Create context that matches rules
      const matchingContext = {
        platform: 'android',
        country: 'US'
      };
      
      // Create context that doesn't match rules
      const nonMatchingContext = {
        platform: 'ios',
        country: 'US'
      };
      
      // Evaluate rules
      const matchingResult = abTestService.evaluateTargetingRules(test, matchingContext);
      const nonMatchingResult = abTestService.evaluateTargetingRules(test, nonMatchingContext);
      
      // Verify results
      expect(matchingResult).toBe(true);
      expect(nonMatchingResult).toBe(false);
    });
    
    it('should select variant based on weights', () => {
      // Create test with variants
      const test = {
        id: 'test-id',
        variants: [
          {
            _id: 'variant-1',
            name: 'Control',
            weight: 70
          },
          {
            _id: 'variant-2',
            name: 'Variant B',
            weight: 30
          }
        ]
      };
      
      // Select variant for multiple users
      const variantCounts = {
        'variant-1': 0,
        'variant-2': 0
      };
      
      for (let i = 0; i < 1000; i++) {
        const userId = `user-${i}`;
        const variant = abTestService.selectVariant(test, userId);
        variantCounts[variant._id]++;
      }
      
      // Verify distribution is roughly according to weights
      // (Allow for some statistical variation)
      expect(variantCounts['variant-1']).toBeGreaterThan(600);
      expect(variantCounts['variant-1']).toBeLessThan(800);
      expect(variantCounts['variant-2']).toBeGreaterThan(200);
      expect(variantCounts['variant-2']).toBeLessThan(400);
    });
    
    it('should track test events', async () => {
      // Create test data
      const testData = {
        name: 'Test Campaign',
        description: 'Test description',
        adType: 'banner',
        status: 'active',
        startDate: new Date(),
        variants: [
          {
            name: 'Control',
            description: 'Control variant',
            weight: 50,
            adConfig: {
              adId: testAd1._id,
              parameters: { key1: 'value1' }
            }
          },
          {
            name: 'Variant B',
            description: 'Test variant',
            weight: 50,
            adConfig: {
              adId: testAd2._id,
              parameters: { key2: 'value2' }
            }
          }
        ],
        metrics: ['impressions', 'clicks'],
        trafficAllocation: 100
      };
      
      // Create test
      const test = await abTestService.createTest(testData);
      
      // Track impression
      await abTestService.trackTestEvent(test._id, test.variants[0]._id, 'impressions');
      
      // Track click
      await abTestService.trackTestEvent(test._id, test.variants[0]._id, 'clicks');
      
      // Get updated test
      const updatedTest = await ABTest.findById(test._id);
      
      // Verify events were tracked
      expect(updatedTest.results).toBeTruthy();
      expect(updatedTest.results.get('impressions')).toBeTruthy();
      expect(updatedTest.results.get('impressions').total).toBe(1);
      expect(updatedTest.results.get('clicks')).toBeTruthy();
      expect(updatedTest.results.get('clicks').total).toBe(1);
    });
    
    it('should calculate test results', async () => {
      // Create test data
      const testData = {
        name: 'Test Campaign',
        description: 'Test description',
        adType: 'banner',
        status: 'active',
        startDate: new Date(),
        variants: [
          {
            name: 'Control',
            description: 'Control variant',
            weight: 50,
            adConfig: {
              adId: testAd1._id,
              parameters: { key1: 'value1' }
            }
          },
          {
            name: 'Variant B',
            description: 'Test variant',
            weight: 50,
            adConfig: {
              adId: testAd2._id,
              parameters: { key2: 'value2' }
            }
          }
        ],
        metrics: ['impressions', 'clicks'],
        trafficAllocation: 100
      };
      
      // Create test
      const test = await abTestService.createTest(testData);
      
      // Track impressions and clicks for control
      for (let i = 0; i < 1000; i++) {
        await abTestService.trackTestEvent(test._id, test.variants[0]._id, 'impressions');
      }
      for (let i = 0; i < 20; i++) {
        await abTestService.trackTestEvent(test._id, test.variants[0]._id, 'clicks');
      }
      
      // Track impressions and clicks for variant
      for (let i = 0; i < 1000; i++) {
        await abTestService.trackTestEvent(test._id, test.variants[1]._id, 'impressions');
      }
      for (let i = 0; i < 30; i++) {
        await abTestService.trackTestEvent(test._id, test.variants[1]._id, 'clicks');
      }
      
      // Calculate results
      const results = await abTestService.calculateTestResults(test._id);
      
      // Verify results
      expect(results).toBeTruthy();
      expect(results.variants.length).toBe(2);
      expect(results.variants[0].metrics.impressions).toBe(1000);
      expect(results.variants[0].metrics.clicks).toBe(20);
      expect(results.variants[0].metrics.ctr).toBe(2);
      expect(results.variants[1].metrics.impressions).toBe(1000);
      expect(results.variants[1].metrics.clicks).toBe(30);
      expect(results.variants[1].metrics.ctr).toBe(3);
      expect(results.variants[1].metrics.improvement).toBe(50);
      expect(results.variants[1].metrics.significant).toBe(true);
    });
  });
  
  describe('ABTest Controller', () => {
    let testAd1, testAd2;
    
    beforeEach(async () => {
      // Create test ads
      testAd1 = new AdMob({
        adName: 'Test Ad 1',
        adType: 'banner',
        adUnitCode: 'test-ad-unit-1',
        status: 'active',
        parameters: { key1: 'value1' }
      });
      await testAd1.save();
      
      testAd2 = new AdMob({
        adName: 'Test Ad 2',
        adType: 'banner',
        adUnitCode: 'test-ad-unit-2',
        status: 'active',
        parameters: { key2: 'value2' }
      });
      await testAd2.save();
    });
    
    it('should create a test', async () => {
      // Create request
      const req = mockRequest({}, {}, {
        name: 'Test Campaign',
        description: 'Test description',
        adType: 'banner',
        variants: [
          {
            name: 'Control',
            description: 'Control variant',
            weight: 50,
            adConfig: {
              adId: testAd1._id,
              parameters: { key1: 'value1' }
            }
          },
          {
            name: 'Variant B',
            description: 'Test variant',
            weight: 50,
            adConfig: {
              adId: testAd2._id,
              parameters: { key2: 'value2' }
            }
          }
        ],
        metrics: ['impressions', 'clicks'],
        trafficAllocation: 100
      }, { id: 'admin-user' });
      
      // Create response
      const res = mockResponse();
      
      // Call controller
      await abTestController.createTest(req, res);
      
      // Verify response
      expect(res.status).toHaveBeenCalledWith(201);
      expect(res.json).toHaveBeenCalled();
      expect(res.json.mock.calls[0][0].success).toBe(true);
      expect(res.json.mock.calls[0][0].data).toBeTruthy();
      expect(res.json.mock.calls[0][0].data.name).toBe('Test Campaign');
    });
    
    it('should get optimal ad config', async () => {
      // Create test data
      const testData = {
        name: 'Test Campaign',
        description: 'Test description',
        adType: 'banner',
        status: 'active',
        startDate: new Date(),
        variants: [
          {
            name: 'Control',
            description: 'Control variant',
            weight: 50,
            adConfig: {
              adId: testAd1._id,
              parameters: { key1: 'value1' }
            }
          },
          {
            name: 'Variant B',
            description: 'Test variant',
            weight: 50,
            adConfig: {
              adId: testAd2._id,
              parameters: { key2: 'value2' }
            }
          }
        ],
        metrics: ['impressions', 'clicks'],
        trafficAllocation: 100
      };
      
      // Create test
      const test = await abTestService.createTest(testData);
      
      // Create request
      const req = mockRequest({ adType: 'banner' }, { userId: 'test-user' });
      
      // Create response
      const res = mockResponse();
      
      // Call controller
      await abTestController.getOptimalAdConfig(req, res);
      
      // Verify response
      expect(res.status).toHaveBeenCalledWith(200);
      expect(res.json).toHaveBeenCalled();
      expect(res.json.mock.calls[0][0].success).toBe(true);
      expect(res.json.mock.calls[0][0].data).toBeTruthy();
      expect(res.json.mock.calls[0][0].data.isTest).toBe(true);
    });
    
    it('should track test event', async () => {
      // Create test data
      const testData = {
        name: 'Test Campaign',
        description: 'Test description',
        adType: 'banner',
        status: 'active',
        startDate: new Date(),
        variants: [
          {
            name: 'Control',
            description: 'Control variant',
            weight: 50,
            adConfig: {
              adId: testAd1._id,
              parameters: { key1: 'value1' }
            }
          },
          {
            name: 'Variant B',
            description: 'Test variant',
            weight: 50,
            adConfig: {
              adId: testAd2._id,
              parameters: { key2: 'value2' }
            }
          }
        ],
        metrics: ['impressions', 'clicks'],
        trafficAllocation: 100
      };
      
      // Create test
      const test = await abTestService.createTest(testData);
      
      // Create request
      const req = mockRequest({
        testId: test._id,
        variantId: test.variants[0]._id,
        eventType: 'impressions'
      }, { userId: 'test-user' });
      
      // Create response
      const res = mockResponse();
      
      // Call controller
      await abTestController.trackTestEvent(req, res);
      
      // Verify response
      expect(res.status).toHaveBeenCalledWith(200);
      expect(res.json).toHaveBeenCalled();
      expect(res.json.mock.calls[0][0].success).toBe(true);
      
      // Verify event was tracked
      const updatedTest = await ABTest.findById(test._id);
      expect(updatedTest.results).toBeTruthy();
      expect(updatedTest.results.get('impressions')).toBeTruthy();
      expect(updatedTest.results.get('impressions').total).toBe(1);
    });
  });
}); 