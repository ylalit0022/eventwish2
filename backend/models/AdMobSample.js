const mongoose = require('mongoose');
const { AdMob } = require('./AdMob');
const dotenv = require('dotenv');

// Load environment variables
dotenv.config();

// MongoDB connection string
const MONGODB_URI = process.env.MONGODB_URI || 'mongodb+srv://ylalit0022:jBRgqv6BBfj2lYaG@cluster0.3d1qt.mongodb.net/eventwishes?retryWrites=true&w=majority';

// Sample AdMob data
const adMobSamples = [
  {
    adUnitId: 'ca-app-pub-3940256099942544/6300978111',
    adName: 'Banner Ad',
    adType: 'banner',
    status: true,
    adMobSetup: {
      appId: 'ca-app-pub-3940256099942544~3347511713',
      platform: 'android'
    },
    testMode: true,
    refreshRate: 60,
    analytics: {
      enabled: true,
      trackImpressions: true,
      trackClicks: true
    },
    targeting: {
      keywords: ['test', 'sample', 'banner'],
      contentUrl: 'eventwish.com/sample',
      gender: 'all',
      age: {
        min: 18,
        max: 65
      }
    },
    frequency: {
      maxImpressions: 5,
      maxImpressionsPer: 'day',
      cooldownPeriod: 300
    },
    createdBy: 'system'
  },
  {
    adUnitId: 'ca-app-pub-3940256099942544/1033173712',
    adName: 'Interstitial Ad',
    adType: 'interstitial',
    status: true,
    adMobSetup: {
      appId: 'ca-app-pub-3940256099942544~3347511713',
      platform: 'android'
    },
    testMode: true,
    refreshRate: 0,
    analytics: {
      enabled: true,
      trackImpressions: true,
      trackClicks: true
    },
    targeting: {
      keywords: ['test', 'sample', 'interstitial'],
      contentUrl: 'eventwish.com/sample',
      gender: 'all',
      age: {
        min: 18,
        max: 65
      }
    },
    frequency: {
      maxImpressions: 3,
      maxImpressionsPer: 'day',
      cooldownPeriod: 600
    },
    createdBy: 'system'
  },
  {
    adUnitId: 'ca-app-pub-3940256099942544/5224354917',
    adName: 'Rewarded Ad',
    adType: 'rewarded',
    status: true,
    adMobSetup: {
      appId: 'ca-app-pub-3940256099942544~3347511713',
      platform: 'android'
    },
    testMode: true,
    refreshRate: 0,
    analytics: {
      enabled: true,
      trackImpressions: true,
      trackClicks: true,
      trackRewards: true
    },
    targeting: {
      keywords: ['test', 'sample', 'rewarded'],
      contentUrl: 'eventwish.com/sample',
      gender: 'all',
      age: {
        min: 18,
        max: 65
      }
    },
    rewardConfig: {
      rewardAmount: 5,
      rewardType: 'coins'
    },
    frequency: {
      maxImpressions: 10,
      maxImpressionsPer: 'day',
      cooldownPeriod: 120
    },
    createdBy: 'system'
  },
  {
    adUnitId: 'ca-app-pub-3940256099942544/2247696110',
    adName: 'Native Ad',
    adType: 'native',
    status: true,
    adMobSetup: {
      appId: 'ca-app-pub-3940256099942544~3347511713',
      platform: 'android'
    },
    testMode: true,
    refreshRate: 120,
    analytics: {
      enabled: true,
      trackImpressions: true,
      trackClicks: true
    },
    targeting: {
      keywords: ['test', 'sample', 'native'],
      contentUrl: 'eventwish.com/sample',
      gender: 'all',
      age: {
        min: 18,
        max: 65
      }
    },
    frequency: {
      maxImpressions: 8,
      maxImpressionsPer: 'day',
      cooldownPeriod: 180
    },
    createdBy: 'system'
  },
  {
    adUnitId: 'ca-app-pub-3940256099942544/3419835294',
    adName: 'App Open Ad',
    adType: 'app_open',
    status: true,
    adMobSetup: {
      appId: 'ca-app-pub-3940256099942544~3347511713',
      platform: 'android'
    },
    testMode: true,
    refreshRate: 0,
    analytics: {
      enabled: true,
      trackImpressions: true,
      trackClicks: false
    },
    targeting: {
      keywords: ['test', 'sample', 'app_open'],
      contentUrl: 'eventwish.com/sample',
      gender: 'all',
      age: {
        min: 18,
        max: 65
      }
    },
    frequency: {
      maxImpressions: 3,
      maxImpressionsPer: 'day',
      cooldownPeriod: 3600
    },
    createdBy: 'system'
  }
];

// Function to seed AdMob data
async function seedAdMobData() {
  try {
    // Connect to MongoDB only if not already connected
    if (mongoose.connection.readyState !== 1) {
      console.log('Connecting to MongoDB...');
      await mongoose.connect(MONGODB_URI);
      console.log('Connected to MongoDB');
    } else {
      console.log('Already connected to MongoDB');
    }

    // Check if AdMob model has deleteMany method
    if (typeof AdMob.deleteMany !== 'function') {
      console.error('AdMob model does not have deleteMany method. Make sure the schema is correctly defined.');
      throw new Error('AdMob model is not properly configured');
    }

    // Clear existing AdMob data
    console.log('Clearing existing AdMob data...');
    await AdMob.deleteMany({});

    // Insert AdMob samples
    console.log('Inserting AdMob samples...');
    const updatedSamples = adMobSamples.map(sample => ({
      adName: sample.adName,
      adUnitCode: sample.adUnitId,
      adType: mapAdType(sample.adType),
      status: sample.status,
      targetingCriteria: sample.targeting || {},
      parameters: sample.adMobSetup || {}
    }));

    const insertedAdMobs = await AdMob.insertMany(updatedSamples);
    console.log(`Inserted ${insertedAdMobs.length} AdMob samples`);

    console.log('AdMob seeding completed successfully!');
  } catch (error) {
    console.error('Error seeding AdMob data:', error);
  } finally {
    // Close MongoDB connection only if we opened it
    if (mongoose.connection.readyState === 1 && require.main === module) {
      await mongoose.connection.close();
      console.log('MongoDB connection closed');
    }
  }
}

// Helper function to map ad types to the enum values
function mapAdType(type) {
  const typeMap = {
    'banner': 'Banner',
    'interstitial': 'Interstitial',
    'rewarded': 'Rewarded',
    'native': 'Native',
    'app_open': 'App Open'
  };
  return typeMap[type] || type;
}

// Export the seed function and data for use in other scripts
module.exports = {
  seedAdMobData,
  adMobSamples
};

// If this file is run directly, execute the seed function
if (require.main === module) {
  seedAdMobData();
} 