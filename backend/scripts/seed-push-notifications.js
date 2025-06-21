const mongoose = require('mongoose');
require('../config/env-loader');

// Define the PushNotification schema
const PushNotificationSchema = new mongoose.Schema({
  title: {
    type: String,
    required: true,
    trim: true
  },
  body: {
    type: String,
    required: true,
    trim: true
  },
  imageUrl: {
    type: String,
    trim: true,
    default: null
  },
  data: {
    type: Map,
    of: String,
    default: {}
  },
  deepLink: {
    type: String,
    trim: true,
    default: null
  },
  type: {
    type: String,
    enum: ['BULK', 'TOPIC', 'PERSONALIZED'],
    required: true
  },
  notificationType: {
    type: String,
    enum: ['GENERAL', 'PROMOTIONAL', 'TRANSACTIONAL', 'INACTIVITY', 'FESTIVAL'],
    default: 'GENERAL'
  },
  personalizationKeys: {
    type: [String],
    default: []
  },
  isTemplate: {
    type: Boolean,
    default: false
  },
  topic: {
    type: String,
    trim: true,
    default: null
  },
  targetUserIds: [{
    type: mongoose.Schema.Types.ObjectId,
    ref: 'User'
  }],
  status: {
    type: String,
    enum: ['DRAFT', 'SCHEDULED', 'SENDING', 'SENT', 'FAILED'],
    default: 'DRAFT'
  },
  scheduledFor: {
    type: Date,
    default: null
  },
  sentAt: {
    type: Date,
    default: null
  },
  lastUsedForInactivity: {
    type: Date,
    default: null
  },
  stats: {
    total: {
      type: Number,
      default: 0
    },
    success: {
      type: Number,
      default: 0
    },
    failure: {
      type: Number,
      default: 0
    },
    opens: {
      type: Number,
      default: 0
    },
    clicks: {
      type: Number,
      default: 0
    }
  },
  errorLog: [{
    userId: {
      type: mongoose.Schema.Types.ObjectId,
      ref: 'User'
    },
    error: {
      type: String
    },
    timestamp: {
      type: Date,
      default: Date.now
    },
    fcmError: {
      type: Object
    }
  }],
  createdBy: {
    type: String,
    required: true
  },
  createdAt: {
    type: Date,
    default: Date.now
  },
  updatedAt: {
    type: Date,
    default: Date.now
  }
});

// Create the model
const PushNotification = mongoose.model('PushNotification', PushNotificationSchema);

// Sample push notification data
const sampleNotifications = [
  {
    title: "Welcome to EventWish",
    body: "Thank you for joining EventWish! Explore our features to create amazing events.",
    type: "BULK",
    notificationType: "GENERAL",
    status: "SENT",
    sentAt: new Date(Date.now() - 7 * 24 * 60 * 60 * 1000), // 7 days ago
    stats: {
      total: 150,
      success: 142,
      failure: 8,
      opens: 98,
      clicks: 45
    },
    createdBy: "admin@eventwish.com",
    createdAt: new Date(Date.now() - 7 * 24 * 60 * 60 * 1000),
    updatedAt: new Date(Date.now() - 7 * 24 * 60 * 60 * 1000)
  },
  {
    title: "New Festival Templates Available",
    body: "Check out our new festival templates for Diwali celebrations!",
    type: "TOPIC",
    topic: "festivals",
    notificationType: "PROMOTIONAL",
    status: "SENT",
    sentAt: new Date(Date.now() - 3 * 24 * 60 * 60 * 1000), // 3 days ago
    stats: {
      total: 85,
      success: 82,
      failure: 3,
      opens: 64,
      clicks: 37
    },
    createdBy: "admin@eventwish.com",
    createdAt: new Date(Date.now() - 3 * 24 * 60 * 60 * 1000),
    updatedAt: new Date(Date.now() - 3 * 24 * 60 * 60 * 1000)
  },
  {
    title: "Your Event is Trending",
    body: "Congratulations! Your event is getting a lot of attention.",
    type: "PERSONALIZED",
    personalizationKeys: ["eventName", "viewCount"],
    notificationType: "TRANSACTIONAL",
    status: "SENT",
    sentAt: new Date(Date.now() - 1 * 24 * 60 * 60 * 1000), // 1 day ago
    stats: {
      total: 42,
      success: 40,
      failure: 2,
      opens: 35,
      clicks: 28
    },
    createdBy: "admin@eventwish.com",
    createdAt: new Date(Date.now() - 1 * 24 * 60 * 60 * 1000),
    updatedAt: new Date(Date.now() - 1 * 24 * 60 * 60 * 1000)
  },
  {
    title: "Weekend Special Offer",
    body: "Get 20% off on premium templates this weekend only!",
    type: "BULK",
    notificationType: "PROMOTIONAL",
    status: "SCHEDULED",
    scheduledFor: new Date(Date.now() + 1 * 24 * 60 * 60 * 1000), // 1 day in future
    stats: {
      total: 0,
      success: 0,
      failure: 0,
      opens: 0,
      clicks: 0
    },
    createdBy: "admin@eventwish.com",
    createdAt: new Date(),
    updatedAt: new Date()
  },
  {
    title: "We Miss You!",
    body: "It's been a while since you created an event. Check out our new templates!",
    type: "PERSONALIZED",
    notificationType: "INACTIVITY",
    status: "DRAFT",
    stats: {
      total: 0,
      success: 0,
      failure: 0,
      opens: 0,
      clicks: 0
    },
    createdBy: "admin@eventwish.com",
    createdAt: new Date(),
    updatedAt: new Date()
  }
];

// Connect to MongoDB and seed the data
async function seedDatabase() {
  try {
    console.log('Connecting to MongoDB...');
    await mongoose.connect(process.env.MONGODB_URI || 'mongodb://localhost:27017/eventwish');
    console.log('Connected to MongoDB');

    // Check if there are already push notifications
    const existingCount = await PushNotification.countDocuments();
    console.log(`Current push notifications count: ${existingCount}`);

    if (existingCount > 0) {
      console.log('Push notifications already exist. Skipping seeding.');
      return;
    }

    // Insert the sample notifications
    const result = await PushNotification.insertMany(sampleNotifications);
    console.log(`Successfully inserted ${result.length} push notifications`);

    // Verify the data was inserted
    const newCount = await PushNotification.countDocuments();
    console.log(`New push notifications count: ${newCount}`);
  } catch (error) {
    console.error('Error seeding database:', error);
  } finally {
    // Close the connection
    await mongoose.connection.close();
    console.log('MongoDB connection closed');
  }
}

// Run the seeding function
seedDatabase(); 