const mongoose = require('mongoose');
require('../config/env-loader');
const User = require('../models/User');

// Connect to MongoDB
mongoose.connect(process.env.MONGODB_URI, {
  useNewUrlParser: true,
  useUnifiedTopology: true
})
.then(() => {
  console.log('Connected to MongoDB');
  addSampleUsers();
})
.catch(err => {
  console.error('MongoDB connection error:', err);
  process.exit(1);
});

async function addSampleUsers() {
  try {
    // Sample template IDs (you should replace these with actual template IDs from your database)
    const sampleTemplateIds = [
      new mongoose.Types.ObjectId(),
      new mongoose.Types.ObjectId(),
      new mongoose.Types.ObjectId()
    ];

    // Sample users with different configurations
    const users = [
      // 1. Complete user with Firebase auth and all features
      {
        uid: 'firebase_complete_user_' + Date.now(),
        deviceId: 'device_complete_' + Date.now(),
        displayName: 'Complete User',
        email: 'complete@example.com',
        profilePhoto: 'https://example.com/photos/complete.jpg',
        lastOnline: new Date(),
        subscription: {
          isActive: true,
          plan: 'YEARLY',
          startedAt: new Date(),
          expiresAt: new Date(Date.now() + 365 * 24 * 60 * 60 * 1000)
        },
        adsAllowed: false,
        pushPreferences: {
          allowFestivalPush: true,
          allowPersonalPush: true
        },
        topicSubscriptions: ['diwali', 'christmas', 'newyear'],
        preferredTheme: 'dark',
        preferredLanguage: 'en',
        timezone: 'Asia/Kolkata',
        muteNotificationsUntil: new Date(Date.now() + 2 * 60 * 60 * 1000),
        referredBy: {
          referredBy: 'PROMO2023',
          referralCode: 'COMPLETE123'
        },
        referralCode: 'COMPLETE123',
        recentTemplatesUsed: sampleTemplateIds,
        favorites: [sampleTemplateIds[0], sampleTemplateIds[1]],
        likes: [sampleTemplateIds[1], sampleTemplateIds[2]],
        categories: [
          {
            category: 'Birthday',
            visitDate: new Date(),
            visitCount: 5,
            source: 'direct'
          },
          {
            category: 'Wedding',
            visitDate: new Date(),
            visitCount: 3,
            source: 'template'
          }
        ],
        lastActiveTemplate: sampleTemplateIds[0],
        lastActionOnTemplate: 'VIEW',
        engagementLog: [
          {
            action: 'VIEW',
            templateId: sampleTemplateIds[0],
            timestamp: new Date()
          },
          {
            action: 'LIKE',
            templateId: sampleTemplateIds[1],
            timestamp: new Date(Date.now() - 60 * 60 * 1000)
          }
        ]
      },

      // 2. Premium user with minimal Firebase auth
      {
        uid: 'firebase_premium_user_' + Date.now(),
        deviceId: 'device_premium_' + Date.now(),
        displayName: 'Premium User',
        email: 'premium@example.com',
        subscription: {
          isActive: true,
          plan: 'MONTHLY',
          startedAt: new Date(),
          expiresAt: new Date(Date.now() + 30 * 24 * 60 * 60 * 1000)
        },
        adsAllowed: false,
        pushPreferences: {
          allowFestivalPush: true,
          allowPersonalPush: false
        },
        preferredTheme: 'light',
        preferredLanguage: 'hi',
        timezone: 'Asia/Kolkata',
        categories: [
          {
            category: 'Diwali',
            visitDate: new Date(),
            visitCount: 2,
            source: 'direct'
          }
        ],
        lastActionOnTemplate: 'FAV'
      },

      // 3. Free user with device ID only
      {
        deviceId: 'device_free_' + Date.now(),
        displayName: 'Free User',
        adsAllowed: true,
        pushPreferences: {
          allowFestivalPush: true,
          allowPersonalPush: true
        },
        preferredTheme: 'light',
        preferredLanguage: 'en',
        timezone: 'America/New_York',
        categories: [
          {
            category: 'Christmas',
            visitDate: new Date(),
            visitCount: 1,
            source: 'direct'
          }
        ],
        lastActionOnTemplate: 'SHARE'
      },

      // 4. User with referral system
      {
        deviceId: 'device_referred_' + Date.now(),
        displayName: 'Referred User',
        referredBy: {
          referredBy: 'COMPLETE123',
          referralCode: 'REF789'
        },
        referralCode: 'REF789',
        adsAllowed: true,
        pushPreferences: {
          allowFestivalPush: true,
          allowPersonalPush: true
        },
        preferredTheme: 'dark',
        preferredLanguage: 'en',
        timezone: 'Europe/London',
        lastActionOnTemplate: 'LIKE'
      }
    ];

    // Insert the sample users
    const result = await User.insertMany(users);
    console.log(`✅ Successfully added ${result.length} sample users!`);

    // Display the created users
    console.log('\nCreated Users:');
    result.forEach((user, index) => {
      console.log(`\nUser ${index + 1}:`);
      console.log('==================');
      console.log(JSON.stringify({
        _id: user._id,
        uid: user.uid,
        deviceId: user.deviceId,
        displayName: user.displayName,
        email: user.email,
        subscription: user.subscription,
        pushPreferences: user.pushPreferences,
        categories: user.categories,
        referralInfo: {
          referredBy: user.referredBy,
          referralCode: user.referralCode
        },
        templateInteractions: {
          favorites: user.favorites?.length || 0,
          likes: user.likes?.length || 0,
          recentTemplates: user.recentTemplatesUsed?.length || 0
        }
      }, null, 2));
      console.log('==================');
    });

    mongoose.connection.close();
    console.log('\nDatabase connection closed.');
  } catch (error) {
    console.error('Error adding sample users:', error);
    mongoose.connection.close();
    process.exit(1);
  }
} 