/**
 * Sponsored Ad Rotation and Fair Distribution Tests
 * 
 * This test suite verifies the implementation of ad rotation and fair distribution.
 */

const mongoose = require('mongoose');
const SponsoredAd = require('../models/SponsoredAd');
const request = require('supertest');
const app = require('../server'); // Adjust path if needed

describe('Sponsored Ad Rotation', () => {
  let testAds = [];
  
  beforeAll(async () => {
    // Create test ads with different priorities and impression counts
    await SponsoredAd.deleteMany({}); // Clear existing ads
    
    // Create test ads with varying priorities and impressions
    testAds = await Promise.all([
      new SponsoredAd({
        image_url: 'https://example.com/ad1.jpg',
        redirect_url: 'https://example.com/1',
        title: 'High Priority Low Impressions',
        priority: 10,
        impression_count: 10,
        location: 'home_top',
        start_date: new Date(),
        end_date: new Date(Date.now() + 30 * 24 * 60 * 60 * 1000), // 30 days from now
      }).save(),
      
      new SponsoredAd({
        image_url: 'https://example.com/ad2.jpg',
        redirect_url: 'https://example.com/2',
        title: 'Medium Priority Medium Impressions',
        priority: 5,
        impression_count: 50,
        location: 'home_top',
        start_date: new Date(),
        end_date: new Date(Date.now() + 30 * 24 * 60 * 60 * 1000),
      }).save(),
      
      new SponsoredAd({
        image_url: 'https://example.com/ad3.jpg',
        redirect_url: 'https://example.com/3',
        title: 'Low Priority No Impressions',
        priority: 1,
        impression_count: 0,
        location: 'home_top',
        start_date: new Date(),
        end_date: new Date(Date.now() + 30 * 24 * 60 * 60 * 1000),
      }).save(),
    ]);
  });
  
  afterAll(async () => {
    // Clean up
    await SponsoredAd.deleteMany({});
    await mongoose.connection.close();
  });
  
  describe('Model Methods', () => {
    test('getAdsForRotation excludes specified ads', async () => {
      // Get all ads for 'home_top' location
      const allAds = await SponsoredAd.getAdsForRotation('home_top');
      expect(allAds.length).toBe(3);
      
      // Exclude the first ad
      const excludeFirstAd = await SponsoredAd.getAdsForRotation('home_top', [testAds[0]._id]);
      expect(excludeFirstAd.length).toBe(2);
      expect(excludeFirstAd.map(ad => ad._id.toString())).not.toContain(testAds[0]._id.toString());
    });
    
    test('applyFairDistribution weights ads by priority and impressions', async () => {
      // Run the distribution multiple times to test probabilistic behavior
      const runs = 100;
      const selectedCounts = { 0: 0, 1: 0, 2: 0 };
      
      for (let i = 0; i < runs; i++) {
        const ads = await SponsoredAd.getActiveAds('home_top');
        const distributed = SponsoredAd.applyFairDistribution(ads, 1);
        
        // Find which ad was selected
        for (let j = 0; j < testAds.length; j++) {
          if (distributed[0]._id.toString() === testAds[j]._id.toString()) {
            selectedCounts[j]++;
          }
        }
      }
      
      // High priority ad with low impressions should be selected most often
      expect(selectedCounts[0]).toBeGreaterThan(selectedCounts[1]);
      // Low priority ad with no impressions might get selected more than medium due to impression weighting
      expect(selectedCounts[2]).toBeGreaterThan(0);
      
      console.log('Selection distribution:', selectedCounts);
    });
  });
  
  describe('API Endpoints', () => {
    test('GET /api/sponsored-ads/rotation returns ads excluding specified IDs', async () => {
      const response = await request(app)
        .get('/api/sponsored-ads/rotation')
        .query({ 
          location: 'home_top',
          exclude: testAds[0]._id.toString()
        });
      
      expect(response.status).toBe(200);
      expect(response.body.success).toBe(true);
      expect(response.body.ads.length).toBeGreaterThan(0);
      expect(response.body.ads.map(ad => ad.id)).not.toContain(testAds[0]._id.toString());
    });
    
    test('GET /api/sponsored-ads/fair-distribution returns weighted ads', async () => {
      const response = await request(app)
        .get('/api/sponsored-ads/fair-distribution')
        .query({ location: 'home_top' });
      
      expect(response.status).toBe(200);
      expect(response.body.success).toBe(true);
      expect(response.body.ads.length).toBeGreaterThan(0);
    });
  });
}); 