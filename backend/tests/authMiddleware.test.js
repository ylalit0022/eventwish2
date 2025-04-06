const request = require('supertest');
const express = require('express');
const mongoose = require('mongoose');
const { MongoMemoryServer } = require('mongodb-memory-server');
const jwt = require('jsonwebtoken');
const authMiddleware = require('../middleware/authMiddleware');
const { ApiKey } = require('../models/ApiKey');
const { AppSignature } = require('../models/AppSignature');

// Mock dependencies
jest.mock('../config/logger');

// Create test app
const app = express();
app.use(express.json());

// Setup test routes
app.get('/api/test/token', authMiddleware.verifyToken, (req, res) => {
  res.status(200).json({ success: true, user: req.user });
});

app.get('/api/test/apikey', authMiddleware.verifyApiKey, (req, res) => {
  res.status(200).json({ success: true, apiKey: req.apiKey });
});

app.get('/api/test/signature', authMiddleware.verifyAppSignature, (req, res) => {
  res.status(200).json({ success: true, appSignature: req.appSignature });
});

app.get('/api/test/ratelimit', authMiddleware.rateLimiter, (req, res) => {
  res.status(200).json({ success: true });
});

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
  await ApiKey.deleteMany({});
  await AppSignature.deleteMany({});
  
  // Reset mocks
  jest.clearAllMocks();
});

describe('Authentication Middleware', () => {
  describe('Token Verification', () => {
    it('should allow access with valid token', async () => {
      // Create a valid token
      const user = { id: '123', role: 'admin' };
      const token = jwt.sign(user, process.env.JWT_SECRET || 'test-secret', { expiresIn: '1h' });
      
      const response = await request(app)
        .get('/api/test/token')
        .set('Authorization', `Bearer ${token}`);
      
      expect(response.status).toBe(200);
      expect(response.body.success).toBe(true);
      expect(response.body.user).toMatchObject(user);
    });
    
    it('should deny access with invalid token', async () => {
      // Create an invalid token
      const token = 'invalid-token';
      
      const response = await request(app)
        .get('/api/test/token')
        .set('Authorization', `Bearer ${token}`);
      
      expect(response.status).toBe(401);
      expect(response.body.success).toBe(false);
      expect(response.body.message).toBe('Invalid token');
    });
    
    it('should deny access with expired token', async () => {
      // Create an expired token
      const user = { id: '123', role: 'admin' };
      const token = jwt.sign(user, process.env.JWT_SECRET || 'test-secret', { expiresIn: '0s' });
      
      // Wait for token to expire
      await new Promise(resolve => setTimeout(resolve, 10));
      
      const response = await request(app)
        .get('/api/test/token')
        .set('Authorization', `Bearer ${token}`);
      
      expect(response.status).toBe(401);
      expect(response.body.success).toBe(false);
      expect(response.body.message).toBe('Token expired');
    });
    
    it('should deny access with missing token', async () => {
      const response = await request(app)
        .get('/api/test/token');
      
      expect(response.status).toBe(401);
      expect(response.body.success).toBe(false);
      expect(response.body.message).toBe('No token provided');
    });
    
    it('should deny access with malformed authorization header', async () => {
      const response = await request(app)
        .get('/api/test/token')
        .set('Authorization', 'InvalidFormat');
      
      expect(response.status).toBe(401);
      expect(response.body.success).toBe(false);
      expect(response.body.message).toBe('Invalid authorization format');
    });
  });
  
  describe('API Key Verification', () => {
    it('should allow access with valid API key', async () => {
      // Create a valid API key in the database
      const apiKey = new ApiKey({
        key: 'valid-api-key',
        appId: 'test-app',
        status: true,
        permissions: ['read', 'write']
      });
      await apiKey.save();
      
      const response = await request(app)
        .get('/api/test/apikey')
        .set('X-API-Key', 'valid-api-key');
      
      expect(response.status).toBe(200);
      expect(response.body.success).toBe(true);
      expect(response.body.apiKey.appId).toBe('test-app');
      expect(response.body.apiKey.permissions).toContain('read');
      expect(response.body.apiKey.permissions).toContain('write');
    });
    
    it('should deny access with invalid API key', async () => {
      const response = await request(app)
        .get('/api/test/apikey')
        .set('X-API-Key', 'invalid-api-key');
      
      expect(response.status).toBe(401);
      expect(response.body.success).toBe(false);
      expect(response.body.message).toBe('Invalid API key');
    });
    
    it('should deny access with inactive API key', async () => {
      // Create an inactive API key in the database
      const apiKey = new ApiKey({
        key: 'inactive-api-key',
        appId: 'test-app',
        status: false,
        permissions: ['read']
      });
      await apiKey.save();
      
      const response = await request(app)
        .get('/api/test/apikey')
        .set('X-API-Key', 'inactive-api-key');
      
      expect(response.status).toBe(401);
      expect(response.body.success).toBe(false);
      expect(response.body.message).toBe('API key is inactive');
    });
    
    it('should deny access with missing API key', async () => {
      const response = await request(app)
        .get('/api/test/apikey');
      
      expect(response.status).toBe(401);
      expect(response.body.success).toBe(false);
      expect(response.body.message).toBe('No API key provided');
    });
    
    it('should update last used timestamp on successful verification', async () => {
      // Create a valid API key in the database
      const apiKey = new ApiKey({
        key: 'valid-api-key',
        appId: 'test-app',
        status: true,
        permissions: ['read'],
        lastUsed: new Date(Date.now() - 86400000) // 1 day ago
      });
      await apiKey.save();
      
      const response = await request(app)
        .get('/api/test/apikey')
        .set('X-API-Key', 'valid-api-key');
      
      expect(response.status).toBe(200);
      
      // Check that lastUsed was updated
      const updatedApiKey = await ApiKey.findOne({ key: 'valid-api-key' });
      const lastUsedDiff = new Date() - updatedApiKey.lastUsed;
      expect(lastUsedDiff).toBeLessThan(5000); // Less than 5 seconds ago
    });
  });
  
  describe('App Signature Verification', () => {
    it('should allow access with valid app signature', async () => {
      // Create a valid app signature in the database
      const appSignature = new AppSignature({
        signature: 'valid-signature',
        appId: 'test-app',
        status: true
      });
      await appSignature.save();
      
      const response = await request(app)
        .get('/api/test/signature')
        .set('X-App-Signature', 'valid-signature');
      
      expect(response.status).toBe(200);
      expect(response.body.success).toBe(true);
      expect(response.body.appSignature.appId).toBe('test-app');
    });
    
    it('should deny access with invalid app signature', async () => {
      const response = await request(app)
        .get('/api/test/signature')
        .set('X-App-Signature', 'invalid-signature');
      
      expect(response.status).toBe(401);
      expect(response.body.success).toBe(false);
      expect(response.body.message).toBe('Invalid app signature');
    });
    
    it('should deny access with inactive app signature', async () => {
      // Create an inactive app signature in the database
      const appSignature = new AppSignature({
        signature: 'inactive-signature',
        appId: 'test-app',
        status: false
      });
      await appSignature.save();
      
      const response = await request(app)
        .get('/api/test/signature')
        .set('X-App-Signature', 'inactive-signature');
      
      expect(response.status).toBe(401);
      expect(response.body.success).toBe(false);
      expect(response.body.message).toBe('App signature is inactive');
    });
    
    it('should deny access with missing app signature', async () => {
      const response = await request(app)
        .get('/api/test/signature');
      
      expect(response.status).toBe(401);
      expect(response.body.success).toBe(false);
      expect(response.body.message).toBe('No app signature provided');
    });
  });
  
  describe('Rate Limiting', () => {
    it('should allow requests within rate limit', async () => {
      // Make multiple requests within the rate limit
      const promises = [];
      for (let i = 0; i < 5; i++) {
        promises.push(
          request(app)
            .get('/api/test/ratelimit')
            .set('X-Forwarded-For', '192.168.1.1')
        );
      }
      
      const responses = await Promise.all(promises);
      
      // All requests should succeed
      responses.forEach(response => {
        expect(response.status).toBe(200);
        expect(response.body.success).toBe(true);
      });
    });
    
    it('should block requests exceeding rate limit', async () => {
      // Make more requests than the rate limit allows
      const promises = [];
      for (let i = 0; i < 15; i++) {
        promises.push(
          request(app)
            .get('/api/test/ratelimit')
            .set('X-Forwarded-For', '192.168.1.2')
        );
      }
      
      const responses = await Promise.all(promises);
      
      // Count successful and rate-limited responses
      const successCount = responses.filter(r => r.status === 200).length;
      const limitedCount = responses.filter(r => r.status === 429).length;
      
      // Some requests should succeed and some should be rate-limited
      expect(successCount).toBeGreaterThan(0);
      expect(limitedCount).toBeGreaterThan(0);
      expect(successCount + limitedCount).toBe(15);
      
      // Check rate-limited response
      const limitedResponse = responses.find(r => r.status === 429);
      expect(limitedResponse.body.success).toBe(false);
      expect(limitedResponse.body.message).toContain('Too many requests');
    });
    
    it('should track rate limits separately by IP', async () => {
      // Make requests from two different IPs
      const promises1 = [];
      for (let i = 0; i < 10; i++) {
        promises1.push(
          request(app)
            .get('/api/test/ratelimit')
            .set('X-Forwarded-For', '192.168.1.3')
        );
      }
      
      const promises2 = [];
      for (let i = 0; i < 5; i++) {
        promises2.push(
          request(app)
            .get('/api/test/ratelimit')
            .set('X-Forwarded-For', '192.168.1.4')
        );
      }
      
      const responses1 = await Promise.all(promises1);
      const responses2 = await Promise.all(promises2);
      
      // Count rate-limited responses for first IP
      const limitedCount1 = responses1.filter(r => r.status === 429).length;
      
      // Count rate-limited responses for second IP
      const limitedCount2 = responses2.filter(r => r.status === 429).length;
      
      // First IP should have some rate-limited responses
      expect(limitedCount1).toBeGreaterThan(0);
      
      // Second IP should not have any rate-limited responses
      expect(limitedCount2).toBe(0);
    });
  });
  
  describe('Combined Authentication', () => {
    it('should work with multiple authentication methods', async () => {
      // Create valid credentials
      const user = { id: '123', role: 'admin' };
      const token = jwt.sign(user, process.env.JWT_SECRET || 'test-secret', { expiresIn: '1h' });
      
      const apiKey = new ApiKey({
        key: 'valid-api-key',
        appId: 'test-app',
        status: true,
        permissions: ['read']
      });
      await apiKey.save();
      
      // Create a custom route with multiple authentication methods
      app.get('/api/test/combined', 
        authMiddleware.verifyToken, 
        authMiddleware.verifyApiKey, 
        (req, res) => {
          res.status(200).json({ 
            success: true, 
            user: req.user,
            apiKey: req.apiKey
          });
        }
      );
      
      const response = await request(app)
        .get('/api/test/combined')
        .set('Authorization', `Bearer ${token}`)
        .set('X-API-Key', 'valid-api-key');
      
      expect(response.status).toBe(200);
      expect(response.body.success).toBe(true);
      expect(response.body.user).toMatchObject(user);
      expect(response.body.apiKey.appId).toBe('test-app');
    });
    
    it('should fail if any authentication method fails', async () => {
      // Create valid token but invalid API key
      const user = { id: '123', role: 'admin' };
      const token = jwt.sign(user, process.env.JWT_SECRET || 'test-secret', { expiresIn: '1h' });
      
      // Create a custom route with multiple authentication methods
      app.get('/api/test/combined-fail', 
        authMiddleware.verifyToken, 
        authMiddleware.verifyApiKey, 
        (req, res) => {
          res.status(200).json({ success: true });
        }
      );
      
      const response = await request(app)
        .get('/api/test/combined-fail')
        .set('Authorization', `Bearer ${token}`)
        .set('X-API-Key', 'invalid-api-key');
      
      expect(response.status).toBe(401);
      expect(response.body.success).toBe(false);
      expect(response.body.message).toBe('Invalid API key');
    });
  });
}); 