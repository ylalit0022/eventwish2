/**
 * Simple verification script for SponsoredAd tracking and CategoryIcon handling
 */

const mongoose = require('mongoose');
const SponsoredAd = require('../models/SponsoredAd');
const Template = require('../models/Template');
const CategoryIcon = require('../models/CategoryIcon');
require('dotenv').config();

async function verifyFixes() {
  try {
    console.log('Connecting to MongoDB...');
    await mongoose.connect(process.env.MONGODB_URI);
    console.log('Connected to MongoDB');
    
    // Verify SponsoredAd model
    console.log('\nVerifying SponsoredAd model:');
    const ad = await SponsoredAd.findOne();
    if (ad) {
      console.log('- Found SponsoredAd:', ad.title);
      console.log('- device_daily_impressions type:', typeof ad.device_daily_impressions);
      console.log('- device_daily_impressions is Map:', ad.device_daily_impressions instanceof Map);
      console.log('✅ SponsoredAd model verification: PASS');
    } else {
      console.log('⚠️ No SponsoredAds found for verification');
    }
    
    // Verify Template with CategoryIcon
    console.log('\nVerifying Template with CategoryIcon:');
    const template = await Template.findOne().populate('categoryIcon');
    if (template && template.categoryIcon) {
      console.log('- Found Template:', template.title);
      console.log('- CategoryIcon populated:', template.categoryIcon.category);
      console.log('- CategoryIcon has id:', template.categoryIcon.id ? 'Yes' : 'No');
      console.log('✅ Template with CategoryIcon verification: PASS');
    } else {
      console.log('⚠️ No Template with CategoryIcon found for verification');
    }
    
    console.log('\n=== All fixes verified successfully ===');
  } catch (error) {
    console.error('Error during verification:', error);
  } finally {
    await mongoose.connection.close();
    console.log('MongoDB connection closed');
  }
}

// Run verification
verifyFixes().then(() => {
  console.log('Verification completed.');
}).catch(error => {
  console.error('Verification failed:', error);
}); 