const request = require('supertest');
const mongoose = require('mongoose');
const { MongoMemoryServer } = require('mongodb-memory-server');
const app = require('../server');
const Coins = require('../models/Coins');
const { AdMob } = require('../models/AdMob');

let mongoServer;
const API_KEY = process.env.API_KEY || 'test-api-key';

// Setup before tests
beforeAll(async () => {
  // Create in-memory MongoDB server
  mongoServer = await MongoMemoryServer.create();
  const uri = mongoServer.getUri();
  
  // Connect to in-memory database
  await mongoose.connect(uri, {
    useNewUrlParser: true,
    useUnifiedTopology: true,
  });
  
  // Create test AdMob entry
  await AdMob.create({
    adName: 'Test Rewarded Ad',
    adUnitCode: 'ca-app-pub-3940256099942544/5224354917', // Test ad unit ID
    adType: 'Rewarded',
    status: true
  });
  
  console.log('Test database connected');
});

// Cleanup after tests
afterAll(async () => {
  await mongoose.connection.dropDatabase();
  await mongoose.connection.close();
  await mongoServer.stop();
  console.log('Test database disconnected');
});

// Clear database between tests
afterEach(async () => {
  await Coins.deleteMany({});
});

describe('Coins API', () => {
  const testDeviceId = 'test-device-123';
  
  describe('GET /api/coins/plan', () => {
    it('should return plan configuration', async () => {
      const res = await request(app).get('/api/coins/plan');
      
      expect(res.statusCode).toBe(200);
      expect(res.body.success).toBe(true);
      expect(res.body.plan).toHaveProperty('requiredCoins');
      expect(res.body.plan).toHaveProperty('coinsPerReward');
      expect(res.body.plan).toHaveProperty('defaultUnlockDuration');
    });
  });
  
  describe('GET /api/coins/:deviceId', () => {
    it('should create a new coins record if none exists', async () => {
      const res = await request(app)
        .get(`/api/coins/${testDeviceId}`)
        .set('x-api-key', API_KEY);
      
      expect(res.statusCode).toBe(200);
      expect(res.body.success).toBe(true);
      expect(res.body.coins).toBe(0);
      expect(res.body.isUnlocked).toBe(false);
      
      // Check database
      const coinsRecord = await Coins.findOne({ deviceId: testDeviceId });
      expect(coinsRecord).toBeTruthy();
      expect(coinsRecord.coins).toBe(0);
    });
    
    it('should return existing coins record', async () => {
      // Create coins record
      await Coins.create({
        deviceId: testDeviceId,
        coins: 50,
        isUnlocked: false
      });
      
      const res = await request(app)
        .get(`/api/coins/${testDeviceId}`)
        .set('x-api-key', API_KEY);
      
      expect(res.statusCode).toBe(200);
      expect(res.body.success).toBe(true);
      expect(res.body.coins).toBe(50);
    });
    
    it('should require API key', async () => {
      const res = await request(app)
        .get(`/api/coins/${testDeviceId}`);
      
      expect(res.statusCode).toBe(401);
    });
  });
  
  describe('POST /api/coins/:deviceId', () => {
    it('should add coins for rewarded ad', async () => {
      const res = await request(app)
        .post(`/api/coins/${testDeviceId}`)
        .set('x-api-key', API_KEY)
        .send({
          amount: 10,
          adUnitId: 'ca-app-pub-3940256099942544/5224354917',
          adName: 'Test Rewarded Ad',
          deviceInfo: { os: 'Android', version: '10' }
        });
      
      expect(res.statusCode).toBe(200);
      expect(res.body.success).toBe(true);
      expect(res.body.coins).toBe(10);
      expect(res.body.added).toBe(10);
      
      // Check database
      const coinsRecord = await Coins.findOne({ deviceId: testDeviceId });
      expect(coinsRecord).toBeTruthy();
      expect(coinsRecord.coins).toBe(10);
      expect(coinsRecord.rewardHistory).toHaveLength(1);
    });
    
    it('should prevent frequent reward claims', async () => {
      // Create coins record with recent reward
      await Coins.create({
        deviceId: testDeviceId,
        coins: 10,
        lastRewardTimestamp: new Date()
      });
      
      const res = await request(app)
        .post(`/api/coins/${testDeviceId}`)
        .set('x-api-key', API_KEY)
        .send({
          amount: 10,
          adUnitId: 'ca-app-pub-3940256099942544/5224354917'
        });
      
      expect(res.statusCode).toBe(429);
      expect(res.body.success).toBe(false);
    });
  });
  
  describe('POST /api/coins/:deviceId/unlock', () => {
    it('should unlock feature if enough coins', async () => {
      // Create coins record with sufficient coins
      await Coins.create({
        deviceId: testDeviceId,
        coins: 100,
        isUnlocked: false
      });
      
      const res = await request(app)
        .post(`/api/coins/${testDeviceId}/unlock`)
        .set('x-api-key', API_KEY)
        .send({});
      
      expect(res.statusCode).toBe(200);
      expect(res.body.success).toBe(true);
      expect(res.body.isUnlocked).toBe(true);
      expect(res.body.signature).toBeTruthy();
      expect(res.body.coins).toBe(0); // Coins deducted
      
      // Check database
      const coinsRecord = await Coins.findOne({ deviceId: testDeviceId });
      expect(coinsRecord).toBeTruthy();
      expect(coinsRecord.isUnlocked).toBe(true);
      expect(coinsRecord.unlockTimestamp).toBeTruthy();
    });
    
    it('should reject if not enough coins', async () => {
      // Create coins record with insufficient coins
      await Coins.create({
        deviceId: testDeviceId,
        coins: 50,
        isUnlocked: false
      });
      
      const res = await request(app)
        .post(`/api/coins/${testDeviceId}/unlock`)
        .set('x-api-key', API_KEY)
        .send({});
      
      expect(res.statusCode).toBe(400);
      expect(res.body.success).toBe(false);
      expect(res.body.message).toContain('Not enough coins');
    });
  });
  
  describe('POST /api/coins/validate', () => {
    it('should validate unlock signature', async () => {
      // Create a valid signature
      const crypto = require('crypto');
      const timestamp = Date.now();
      const duration = 30;
      const data = `${testDeviceId}:${timestamp}:${duration}`;
      const secret = process.env.JWT_SECRET || 'eventwish-coins-secret-key';
      const signature = crypto.createHmac('sha256', secret).update(data).digest('hex');
      
      const res = await request(app)
        .post('/api/coins/validate')
        .set('x-api-key', API_KEY)
        .send({
          deviceId: testDeviceId,
          timestamp,
          duration,
          signature
        });
      
      expect(res.statusCode).toBe(200);
      expect(res.body.success).toBe(true);
      expect(res.body.valid).toBe(true);
    });
    
    it('should reject invalid signature', async () => {
      const res = await request(app)
        .post('/api/coins/validate')
        .set('x-api-key', API_KEY)
        .send({
          deviceId: testDeviceId,
          timestamp: Date.now(),
          duration: 30,
          signature: 'invalid-signature'
        });
      
      expect(res.statusCode).toBe(401);
      expect(res.body.success).toBe(false);
      expect(res.body.valid).toBe(false);
    });
  });
  
  describe('POST /api/coins/report', () => {
    it('should report unlock to server', async () => {
      const timestamp = Date.now();
      const duration = 30;
      
      const res = await request(app)
        .post('/api/coins/report')
        .set('x-api-key', API_KEY)
        .send({
          deviceId: testDeviceId,
          timestamp,
          duration
        });
      
      expect(res.statusCode).toBe(200);
      expect(res.body.success).toBe(true);
      expect(res.body.signature).toBeTruthy();
      
      // Check database
      const coinsRecord = await Coins.findOne({ deviceId: testDeviceId });
      expect(coinsRecord).toBeTruthy();
      expect(coinsRecord.isUnlocked).toBe(true);
      expect(coinsRecord.unlockDuration).toBe(duration);
    });
  });
}); 