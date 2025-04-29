/**
 * Seed Sponsored Ads
 * 
 * This script seeds the database with sample sponsored ad data.
 */

const mongoose = require('mongoose');
const logger = require('../config/logger');
require('../models/SponsoredAd');

const SponsoredAd = mongoose.model('SponsoredAd');

/**
 * Seed sponsored ads data
 */
const seedSponsoredAds = async () => {
  try {
    // Check if ads already exist
    const count = await SponsoredAd.countDocuments();
    
    if (count > 0) {
      logger.info(`Skipping sponsored ads seed: ${count} ads already exist`);
      return;
    }
    
    // Sample ad data
    const sampleAds = [
      {
        image_url: 'https://eventwish2.onrender.com/static/ads/greeting-app-ad-1.jpg',
        redirect_url: 'https://www.example.com/special-offer',
        status: true,
        start_date: new Date(),
        end_date: new Date(Date.now() + 30 * 24 * 60 * 60 * 1000), // 30 days from now
        location: 'category_below',
        priority: 5,
        title: 'Special Offer',
        description: 'Get 20% off on premium templates'
      },
      {
        image_url: 'https://eventwish2.onrender.com/static/ads/greeting-app-ad-2.jpg',
        redirect_url: 'https://www.example.com/new-templates',
        status: true,
        start_date: new Date(),
        end_date: new Date(Date.now() + 60 * 24 * 60 * 60 * 1000), // 60 days from now
        location: 'category_below',
        priority: 3,
        title: 'New Templates',
        description: 'Check out our new festival templates'
      },
      {
        image_url: 'https://eventwish2.onrender.com/static/ads/greeting-app-ad-3.jpg',
        redirect_url: 'https://www.example.com/premium',
        status: true,
        start_date: new Date(),
        end_date: new Date(Date.now() + 45 * 24 * 60 * 60 * 1000), // 45 days from now
        location: 'home_bottom',
        priority: 4,
        title: 'Go Premium',
        description: 'Upgrade to premium for exclusive templates'
      }
    ];
    
    // Insert ads
    await SponsoredAd.insertMany(sampleAds);
    
    logger.info(`Successfully seeded ${sampleAds.length} sponsored ads`);
  } catch (error) {
    logger.error(`Error seeding sponsored ads: ${error.message}`);
  }
};

// Export the function for use in the main seed script
module.exports = seedSponsoredAds;

// If this script is run directly, execute the seed function
if (require.main === module) {
  // Load environment variables
  require('../config/env-loader');
  
  // Connect to MongoDB
  mongoose.connect(process.env.MONGODB_URI, {
    useNewUrlParser: true,
    useUnifiedTopology: true
  })
    .then(() => {
      logger.info('Connected to MongoDB');
      return seedSponsoredAds();
    })
    .then(() => {
      logger.info('Seed completed');
      process.exit(0);
    })
    .catch((err) => {
      logger.error(`MongoDB connection error: ${err.message}`);
      process.exit(1);
    });
} 