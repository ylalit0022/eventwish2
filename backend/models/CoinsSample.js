const mongoose = require('mongoose');
const Coins = require('./Coins');
const dotenv = require('dotenv');

// Load environment variables
dotenv.config();

// MongoDB connection string
const MONGODB_URI = process.env.MONGODB_URI || 'mongodb+srv://ylalit0022:jBRgqv6BBfj2lYaG@cluster0.3d1qt.mongodb.net/eventwishes?retryWrites=true&w=majority';

// Sample coins plan data
const coinsPlan = {
  unlockCost: 50,
  unlockDuration: 30, // days
  rewardPerAd: 5,
  maxRewardsPerDay: 10,
  cooldownPeriod: 300, // seconds
  availableFeatures: ['html_editing', 'premium_templates'],
  enableServerValidation: true
};

// Sample user coins data
const userCoinsRecords = [
  {
    deviceId: 'test-device-001',
    coins: 25,
    timestamp: new Date(),
    unlocked: false,
    unlockHistory: [],
    rewardHistory: [
      {
        adUnitId: 'ca-app-pub-3940256099942544/5224354917',
        adName: 'Rewarded Ad',
        coinsEarned: 5,
        timestamp: new Date(Date.now() - 24 * 60 * 60 * 1000) // 1 day ago
      },
      {
        adUnitId: 'ca-app-pub-3940256099942544/5224354917',
        adName: 'Rewarded Ad',
        coinsEarned: 5,
        timestamp: new Date(Date.now() - 12 * 60 * 60 * 1000) // 12 hours ago
      },
      {
        adUnitId: 'ca-app-pub-3940256099942544/5224354917',
        adName: 'Rewarded Ad',
        coinsEarned: 5,
        timestamp: new Date(Date.now() - 6 * 60 * 60 * 1000) // 6 hours ago
      },
      {
        adUnitId: 'ca-app-pub-3940256099942544/5224354917',
        adName: 'Rewarded Ad',
        coinsEarned: 5,
        timestamp: new Date(Date.now() - 2 * 60 * 60 * 1000) // 2 hours ago
      },
      {
        adUnitId: 'ca-app-pub-3940256099942544/5224354917',
        adName: 'Rewarded Ad',
        coinsEarned: 5,
        timestamp: new Date(Date.now() - 1 * 60 * 60 * 1000) // 1 hour ago
      }
    ]
  },
  {
    deviceId: 'test-device-002',
    coins: 60,
    timestamp: new Date(),
    unlocked: true,
    unlockTimestamp: new Date(Date.now() - 5 * 24 * 60 * 60 * 1000), // 5 days ago
    unlockExpiry: new Date(Date.now() + 25 * 24 * 60 * 60 * 1000), // 25 days from now
    signature: 'sample-signature-for-validation',
    unlockHistory: [
      {
        timestamp: new Date(Date.now() - 5 * 24 * 60 * 60 * 1000), // 5 days ago
        cost: 50,
        duration: 30,
        signature: 'sample-signature-for-validation'
      }
    ],
    rewardHistory: [
      {
        adUnitId: 'ca-app-pub-3940256099942544/5224354917',
        adName: 'Rewarded Ad',
        coinsEarned: 5,
        timestamp: new Date(Date.now() - 15 * 24 * 60 * 60 * 1000) // 15 days ago
      },
      {
        adUnitId: 'ca-app-pub-3940256099942544/5224354917',
        adName: 'Rewarded Ad',
        coinsEarned: 5,
        timestamp: new Date(Date.now() - 14 * 24 * 60 * 60 * 1000) // 14 days ago
      },
      {
        adUnitId: 'ca-app-pub-3940256099942544/5224354917',
        adName: 'Rewarded Ad',
        coinsEarned: 5,
        timestamp: new Date(Date.now() - 10 * 24 * 60 * 60 * 1000) // 10 days ago
      },
      {
        adUnitId: 'ca-app-pub-3940256099942544/5224354917',
        adName: 'Rewarded Ad',
        coinsEarned: 5,
        timestamp: new Date(Date.now() - 7 * 24 * 60 * 60 * 1000) // 7 days ago
      },
      {
        adUnitId: 'ca-app-pub-3940256099942544/5224354917',
        adName: 'Rewarded Ad',
        coinsEarned: 5,
        timestamp: new Date(Date.now() - 6 * 24 * 60 * 60 * 1000) // 6 days ago
      },
      {
        adUnitId: 'ca-app-pub-3940256099942544/5224354917',
        adName: 'Rewarded Ad',
        coinsEarned: 5,
        timestamp: new Date(Date.now() - 6 * 24 * 60 * 60 * 1000) // 6 days ago
      },
      {
        adUnitId: 'ca-app-pub-3940256099942544/5224354917',
        adName: 'Rewarded Ad',
        coinsEarned: 5,
        timestamp: new Date(Date.now() - 5 * 24 * 60 * 60 * 1000) // 5 days ago
      },
      {
        adUnitId: 'ca-app-pub-3940256099942544/5224354917',
        adName: 'Rewarded Ad',
        coinsEarned: 5,
        timestamp: new Date(Date.now() - 4 * 24 * 60 * 60 * 1000) // 4 days ago
      },
      {
        adUnitId: 'ca-app-pub-3940256099942544/5224354917',
        adName: 'Rewarded Ad',
        coinsEarned: 5,
        timestamp: new Date(Date.now() - 3 * 24 * 60 * 60 * 1000) // 3 days ago
      },
      {
        adUnitId: 'ca-app-pub-3940256099942544/5224354917',
        adName: 'Rewarded Ad',
        coinsEarned: 5,
        timestamp: new Date(Date.now() - 2 * 24 * 60 * 60 * 1000) // 2 days ago
      },
      {
        adUnitId: 'ca-app-pub-3940256099942544/5224354917',
        adName: 'Rewarded Ad',
        coinsEarned: 5,
        timestamp: new Date(Date.now() - 1 * 24 * 60 * 60 * 1000) // 1 day ago
      },
      {
        adUnitId: 'ca-app-pub-3940256099942544/5224354917',
        adName: 'Rewarded Ad',
        coinsEarned: 5,
        timestamp: new Date() // today
      }
    ]
  },
  {
    deviceId: 'test-device-003',
    coins: 0,
    timestamp: new Date(),
    unlocked: false,
    unlockHistory: [],
    rewardHistory: []
  }
];

// Function to seed Coins data
async function seedCoinsData() {
  try {
    // Connect to MongoDB only if not already connected
    if (mongoose.connection.readyState !== 1) {
      console.log('Connecting to MongoDB...');
      await mongoose.connect(MONGODB_URI);
      console.log('Connected to MongoDB');
    } else {
      console.log('Already connected to MongoDB');
    }

    // Clear existing Coins data
    console.log('Clearing existing Coins data...');
    await Coins.deleteMany({});

    // Insert user coins records
    console.log('Inserting user coins records...');
    
    // Process each record individually
    for (const record of userCoinsRecords) {
      try {
        const coinDoc = new Coins({
          deviceId: record.deviceId,
          coins: record.coins,
          isUnlocked: record.unlocked,
          unlockTimestamp: record.unlockTimestamp || null,
          unlockDuration: 30,
          unlockSignature: record.signature || null,
          rewardHistory: record.rewardHistory,
          plan: {
            requiredCoins: coinsPlan.unlockCost,
            coinsPerReward: coinsPlan.rewardPerAd,
            defaultUnlockDuration: coinsPlan.unlockDuration
          }
        });
        
        await coinDoc.save();
        console.log(`Inserted coin record for device: ${record.deviceId}`);
      } catch (err) {
        console.error(`Error saving record for device ${record.deviceId}:`, err.message);
      }
    }

    console.log('Coins seeding completed successfully!');
  } catch (error) {
    console.error('Error seeding Coins data:', error);
  } finally {
    // Close MongoDB connection only if we opened it
    if (mongoose.connection.readyState === 1 && require.main === module) {
      await mongoose.connection.close();
      console.log('MongoDB connection closed');
    }
  }
}

// Export the seed function and data for use in other scripts
module.exports = {
  seedCoinsData,
  coinsPlan,
  userCoinsRecords
};

// If this file is run directly, execute the seed function
if (require.main === module) {
  seedCoinsData();
} 