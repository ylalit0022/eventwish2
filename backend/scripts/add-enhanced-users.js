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
  addEnhancedUsers();
})
.catch(err => {
  console.error('MongoDB connection error:', err);
  process.exit(1);
});

async function addEnhancedUsers() {
  try {
    // Create sample template IDs
    const sampleTemplateIds = Array(10).fill().map(() => new mongoose.Types.ObjectId());

    // Generate engagement log entries
    const generateEngagementLog = (templateIds, count = 10) => {
      const actions = ['VIEW', 'LIKE', 'FAV', 'SHARE'];
      return Array(count).fill().map((_, index) => ({
        action: actions[Math.floor(Math.random() * actions.length)],
        templateId: templateIds[Math.floor(Math.random() * templateIds.length)],
        timestamp: new Date(Date.now() - index * 24 * 60 * 60 * 1000) // Each entry 1 day apart
      }));
    };

    // Generate category visits
    const generateCategories = () => {
      const categories = ['Birthday', 'Wedding', 'Diwali', 'Christmas', 'NewYear', 'Anniversary', 'Graduation'];
      const sources = ['direct', 'template'];
      return categories.map(category => ({
        category,
        visitDate: new Date(Date.now() - Math.floor(Math.random() * 30) * 24 * 60 * 60 * 1000),
        visitCount: Math.floor(Math.random() * 20) + 1,
        source: sources[Math.floor(Math.random() * sources.length)]
      }));
    };

    // Enhanced sample users
    const users = [
      // 1. Super User - All features maxed out
      {
        uid: 'firebase_super_user_' + Date.now(),
        deviceId: 'device_super_' + Date.now(),
        displayName: 'Super User',
        email: 'super@example.com',
        phoneNumber: '+91987654321',
        profilePhoto: 'https://example.com/photos/super.jpg',
        coins: 1000,
        isUnlocked: true,
        unlockExpiry: new Date(Date.now() + 365 * 24 * 60 * 60 * 1000), // 1 year from now
        lastOnline: new Date(),
        created: new Date(Date.now() - 365 * 24 * 60 * 60 * 1000), // 1 year ago
        subscription: {
          isActive: true,
          plan: 'YEARLY',
          startedAt: new Date(Date.now() - 180 * 24 * 60 * 60 * 1000),
          expiresAt: new Date(Date.now() + 180 * 24 * 60 * 60 * 1000)
        },
        adsAllowed: false,
        pushPreferences: {
          allowFestivalPush: true,
          allowPersonalPush: true
        },
        topicSubscriptions: [
          'diwali', 'christmas', 'newyear', 'birthday', 'wedding',
          'anniversary', 'graduation', 'promotion', 'festival'
        ],
        preferredTheme: 'dark',
        preferredLanguage: 'en',
        timezone: 'Asia/Kolkata',
        muteNotificationsUntil: new Date(Date.now() + 2 * 60 * 60 * 1000),
        referredBy: {
          referredBy: 'FOUNDER2023',
          referralCode: 'SUPER123'
        },
        referralCode: 'SUPER123',
        recentTemplatesUsed: sampleTemplateIds.slice(0, 5),
        favorites: sampleTemplateIds.slice(2, 8),
        likes: sampleTemplateIds.slice(1, 7),
        categories: generateCategories(),
        lastActiveTemplate: sampleTemplateIds[0],
        lastActionOnTemplate: 'VIEW',
        engagementLog: generateEngagementLog(sampleTemplateIds, 20)
      },

      // 2. Power User - Heavy engagement
      {
        uid: 'firebase_power_user_' + Date.now(),
        deviceId: 'device_power_' + Date.now(),
        displayName: 'Power User',
        email: 'power@example.com',
        phoneNumber: '+91987654322',
        profilePhoto: 'https://example.com/photos/power.jpg',
        coins: 500,
        isUnlocked: true,
        unlockExpiry: new Date(Date.now() + 90 * 24 * 60 * 60 * 1000), // 90 days from now
        lastOnline: new Date(),
        created: new Date(Date.now() - 45 * 24 * 60 * 60 * 1000),
        subscription: {
          isActive: true,
          plan: 'HALF_YEARLY',
          startedAt: new Date(Date.now() - 90 * 24 * 60 * 60 * 1000),
          expiresAt: new Date(Date.now() + 90 * 24 * 60 * 60 * 1000)
        },
        adsAllowed: false,
        pushPreferences: {
          allowFestivalPush: true,
          allowPersonalPush: true
        },
        topicSubscriptions: ['diwali', 'christmas', 'newyear', 'birthday'],
        preferredTheme: 'light',
        preferredLanguage: 'hi',
        timezone: 'Asia/Kolkata',
        referralCode: 'POWER789',
        recentTemplatesUsed: sampleTemplateIds.slice(3, 8),
        favorites: sampleTemplateIds.slice(0, 5),
        likes: sampleTemplateIds.slice(2, 9),
        categories: generateCategories().slice(0, 4),
        lastActiveTemplate: sampleTemplateIds[1],
        lastActionOnTemplate: 'FAV',
        engagementLog: generateEngagementLog(sampleTemplateIds, 15)
      },

      // 3. Social User - Heavy sharing
      {
        uid: 'firebase_social_user_' + Date.now(),
        deviceId: 'device_social_' + Date.now(),
        displayName: 'Social User',
        email: 'social@example.com',
        phoneNumber: '+91987654323',
        coins: 250,
        isUnlocked: true,
        unlockExpiry: new Date(Date.now() + 30 * 24 * 60 * 60 * 1000), // 30 days from now
        lastOnline: new Date(),
        created: new Date(Date.now() - 15 * 24 * 60 * 60 * 1000),
        subscription: {
          isActive: true,
          plan: 'QUARTERLY',
          startedAt: new Date(Date.now() - 45 * 24 * 60 * 60 * 1000),
          expiresAt: new Date(Date.now() + 45 * 24 * 60 * 60 * 1000)
        },
        adsAllowed: false,
        pushPreferences: {
          allowFestivalPush: true,
          allowPersonalPush: true
        },
        topicSubscriptions: ['birthday', 'wedding', 'anniversary'],
        preferredTheme: 'light',
        preferredLanguage: 'en',
        timezone: 'America/New_York',
        referredBy: {
          referredBy: 'SUPER123',
          referralCode: 'SOCIAL456'
        },
        referralCode: 'SOCIAL456',
        recentTemplatesUsed: sampleTemplateIds.slice(1, 4),
        favorites: sampleTemplateIds.slice(0, 3),
        likes: sampleTemplateIds.slice(2, 5),
        categories: generateCategories().slice(0, 3),
        lastActiveTemplate: sampleTemplateIds[2],
        lastActionOnTemplate: 'SHARE',
        engagementLog: generateEngagementLog(sampleTemplateIds, 10)
      },

      // 4. New User - Limited engagement but full profile
      {
        uid: 'firebase_new_user_' + Date.now(),
        deviceId: 'device_new_' + Date.now(),
        displayName: 'New User',
        email: 'new@example.com',
        phoneNumber: '+91987654324',
        profilePhoto: 'https://example.com/photos/new.jpg',
        coins: 50,
        isUnlocked: false,
        unlockExpiry: null,
        lastOnline: new Date(),
        created: new Date(Date.now() - 7 * 24 * 60 * 60 * 1000), // 1 week ago
        subscription: {
          isActive: false,
          plan: '',
          startedAt: null,
          expiresAt: null
        },
        adsAllowed: true,
        pushPreferences: {
          allowFestivalPush: true,
          allowPersonalPush: true
        },
        topicSubscriptions: ['birthday'],
        preferredTheme: 'light',
        preferredLanguage: 'en',
        timezone: 'Europe/London',
        referredBy: {
          referredBy: 'POWER789',
          referralCode: 'NEW111'
        },
        referralCode: 'NEW111',
        recentTemplatesUsed: [sampleTemplateIds[0]],
        favorites: [sampleTemplateIds[1]],
        likes: [sampleTemplateIds[2]],
        categories: generateCategories().slice(0, 1),
        lastActiveTemplate: sampleTemplateIds[0],
        lastActionOnTemplate: 'VIEW',
        engagementLog: generateEngagementLog(sampleTemplateIds, 3)
      },

      // 5. Inactive User - Expired subscription
      {
        uid: 'firebase_inactive_user_' + Date.now(),
        deviceId: 'device_inactive_' + Date.now(),
        displayName: 'Inactive User',
        email: 'inactive@example.com',
        phoneNumber: '+91987654325',
        coins: 0,
        isUnlocked: false,
        unlockExpiry: new Date(Date.now() - 15 * 24 * 60 * 60 * 1000), // Expired 15 days ago
        lastOnline: new Date(Date.now() - 90 * 24 * 60 * 60 * 1000),
        created: new Date(Date.now() - 180 * 24 * 60 * 60 * 1000),
        subscription: {
          isActive: false,
          plan: 'MONTHLY',
          startedAt: new Date(Date.now() - 45 * 24 * 60 * 60 * 1000),
          expiresAt: new Date(Date.now() - 15 * 24 * 60 * 60 * 1000)
        },
        adsAllowed: true,
        pushPreferences: {
          allowFestivalPush: false,
          allowPersonalPush: false
        },
        topicSubscriptions: [],
        preferredTheme: 'system',
        preferredLanguage: 'hi',
        timezone: 'Asia/Kolkata',
        muteNotificationsUntil: new Date(Date.now() + 7 * 24 * 60 * 60 * 1000),
        referralCode: 'INACTIVE999',
        recentTemplatesUsed: [sampleTemplateIds[9]],
        favorites: [],
        likes: [sampleTemplateIds[8]],
        categories: generateCategories().slice(0, 2),
        lastActiveTemplate: sampleTemplateIds[9],
        lastActionOnTemplate: 'VIEW',
        engagementLog: generateEngagementLog(sampleTemplateIds, 5)
      },

      // 6. Anonymous User - Minimal data
      {
        deviceId: 'device_anon_' + Date.now(),
        phoneNumber: null,
        coins: 0,
        isUnlocked: false,
        unlockExpiry: null,
        lastOnline: new Date(),
        created: new Date(),
        adsAllowed: true,
        pushPreferences: {
          allowFestivalPush: true,
          allowPersonalPush: true
        },
        preferredTheme: 'light',
        preferredLanguage: 'en',
        timezone: 'UTC',
        categories: [],
        engagementLog: [],
        lastActionOnTemplate: null,
        lastActiveTemplate: null,
        subscription: {
          isActive: false,
          plan: '',
          startedAt: null,
          expiresAt: null
        }
      }
    ];

    // Insert the enhanced users
    const result = await User.insertMany(users);
    console.log(`✅ Successfully added ${result.length} enhanced users!`);

    // Display the created users with detailed statistics
    console.log('\nCreated Users:');
    result.forEach((user, index) => {
      console.log(`\nUser ${index + 1}: ${user.displayName}`);
      console.log('==================');
      console.log(JSON.stringify({
        _id: user._id,
        uid: user.uid,
        deviceId: user.deviceId,
        profile: {
          displayName: user.displayName,
          email: user.email,
          phoneNumber: user.phoneNumber,
          hasPhoto: !!user.profilePhoto
        },
        coins: {
          balance: user.coins,
          isUnlocked: user.isUnlocked,
          unlockExpiry: user.unlockExpiry
        },
        subscription: user.subscription,
        engagement: {
          totalCategories: user.categories.length,
          totalVisits: user.categories.reduce((sum, cat) => sum + cat.visitCount, 0),
          favorites: user.favorites?.length || 0,
          likes: user.likes?.length || 0,
          recentTemplates: user.recentTemplatesUsed?.length || 0,
          engagementLog: user.engagementLog?.length || 0
        },
        preferences: {
          theme: user.preferredTheme,
          language: user.preferredLanguage,
          timezone: user.timezone,
          pushPreferences: user.pushPreferences,
          topicSubscriptions: user.topicSubscriptions?.length || 0
        },
        referral: {
          hasReferrer: !!user.referredBy?.referredBy,
          referralCode: user.referralCode
        },
        activity: {
          created: user.created,
          lastOnline: user.lastOnline,
          lastAction: user.lastActionOnTemplate
        }
      }, null, 2));
      console.log('==================');
    });

    mongoose.connection.close();
    console.log('\nDatabase connection closed.');
  } catch (error) {
    console.error('Error adding enhanced users:', error);
    mongoose.connection.close();
    process.exit(1);
  }
} 