/**
 * Mock data for when the MongoDB connection is unavailable
 * This provides fallback data for the admin panel to display
 */

// Mock push notification stats
export const mockPushNotificationStats = {
  total: 5,
  sent: 3,
  pending: 2,
  failed: 0,
  successRate: 60
};

// Mock topics with counts
export const mockTopicsWithCounts = [
  { name: 'general', count: 150 },
  { name: 'updates', count: 120 },
  { name: 'promotions', count: 80 },
  { name: 'events', count: 45 }
];

// Mock user segments
export const mockUserSegments = [
  { name: 'active', count: 200, description: 'Users active in the last 7 days' },
  { name: 'inactive', count: 50, description: 'Users inactive for more than 7 days' },
  { name: 'new', count: 30, description: 'Users registered in the last 7 days' }
];

// Mock push notifications
export const mockPushNotifications = [
  {
    _id: 'mock-notification-1',
    title: 'Welcome to EventWish',
    body: 'Thank you for joining EventWish!',
    type: 'BULK',
    isSent: true,
    audienceSize: 100,
    createdAt: new Date(Date.now() - 7 * 24 * 60 * 60 * 1000).toISOString()
  },
  {
    _id: 'mock-notification-2',
    title: 'New Features Available',
    body: 'Check out our latest features!',
    type: 'TOPIC',
    topics: ['updates'],
    isSent: true,
    audienceSize: 120,
    createdAt: new Date(Date.now() - 3 * 24 * 60 * 60 * 1000).toISOString()
  },
  {
    _id: 'mock-notification-3',
    title: 'Upcoming Event',
    body: 'Don\'t miss our upcoming event!',
    type: 'TOPIC',
    topics: ['events'],
    isSent: false,
    scheduledAt: new Date(Date.now() + 2 * 24 * 60 * 60 * 1000).toISOString(),
    audienceSize: 45,
    createdAt: new Date(Date.now() - 1 * 24 * 60 * 60 * 1000).toISOString()
  }
];

// Mock pagination for push notifications
export const mockPushNotificationsPagination = {
  total: mockPushNotifications.length,
  page: 1,
  limit: 10,
  pages: 1
};

// Generate realistic mock data
const generateMockData = () => {
  // Current date for reference
  const now = new Date();
  
  // Generate notifications with realistic timestamps
  const notifications = [];
  const notificationTypes = ['TOPIC', 'DIRECT', 'PERSONALIZED', 'INACTIVITY'];
  const notificationStatuses = ['SENT', 'FAILED', 'DRAFT', 'SCHEDULED'];
  const topics = ['festivals', 'updates', 'promotions', 'events', 'news'];
  
  // Generate 20 mock notifications
  for (let i = 0; i < 20; i++) {
    const createdAt = new Date(now);
    createdAt.setDate(now.getDate() - Math.floor(Math.random() * 30)); // Random date within last 30 days
    
    const type = notificationTypes[Math.floor(Math.random() * notificationTypes.length)];
    const status = notificationStatuses[Math.floor(Math.random() * notificationStatuses.length)];
    const topic = topics[Math.floor(Math.random() * topics.length)];
    
    // Generate success and failure counts
    const totalRecipients = Math.floor(Math.random() * 1000) + 100;
    const successCount = Math.floor(Math.random() * totalRecipients);
    const failureCount = totalRecipients - successCount;
    
    notifications.push({
      _id: `mock-notification-${i}`,
      title: `Mock Notification ${i + 1}`,
      body: `This is a mock notification body for testing purposes. Type: ${type}`,
      type,
      status,
      topic: type === 'TOPIC' ? topic : undefined,
      imageUrl: Math.random() > 0.5 ? 'https://via.placeholder.com/300' : undefined,
      deepLink: Math.random() > 0.7 ? 'app://open/section/details' : undefined,
      createdAt: createdAt.toISOString(),
      updatedAt: new Date(createdAt.getTime() + Math.random() * 86400000).toISOString(), // Random time after creation
      isSent: status === 'SENT',
      stats: {
        total: totalRecipients,
        success: successCount,
        failure: failureCount
      },
      isMockData: true
    });
  }
  
  return {
    notifications,
    pagination: {
      total: notifications.length,
      page: 1,
      limit: 20,
      totalPages: 1
    }
  };
};

// Generate topics with counts
const generateTopicsWithCounts = () => {
  const topics = [
    { name: 'festivals', subscriberCount: 1245 },
    { name: 'updates', subscriberCount: 987 },
    { name: 'promotions', subscriberCount: 756 },
    { name: 'events', subscriberCount: 543 },
    { name: 'news', subscriberCount: 321 }
  ];
  
  return {
    success: true,
    topics,
    isMockData: true
  };
};

// Generate notification stats
const generateNotificationStats = () => {
  const total = 100;
  const sent = 75;
  const pending = 15;
  const failed = 10;
  const successRate = 75;
  
  // Generate recent notifications
  const recentNotifications = [];
  for (let i = 0; i < 5; i++) {
    recentNotifications.push({
      _id: `mock-recent-${i}`,
      title: `Recent Notification ${i + 1}`,
      type: i % 2 === 0 ? 'TOPIC' : 'DIRECT',
      status: i % 3 === 0 ? 'FAILED' : 'SENT',
      stats: {
        total: 100,
        success: i % 3 === 0 ? 70 : 100,
        failure: i % 3 === 0 ? 30 : 0
      }
    });
  }
  
  return {
    success: true,
    stats: {
      total,
      sent,
      pending,
      failed,
      successRate
    },
    recentNotifications,
    isMockData: true
  };
};

// Mock data exports
const mockData = {
  // Dynamic mock data
  pushNotifications: generateMockData(),
  topicsWithCounts: generateTopicsWithCounts(),
  pushNotificationStats: generateNotificationStats(),
  
  // Static mock data
  templates: {
    success: true,
    templates: [
      {
        _id: 'mock-template-1',
        name: 'Birthday Wish',
        content: 'Happy Birthday {{name}}! Wishing you a fantastic day.',
        isActive: true,
        createdAt: '2023-01-01T00:00:00.000Z',
        updatedAt: '2023-01-01T00:00:00.000Z'
      },
      {
        _id: 'mock-template-2',
        name: 'Welcome Message',
        content: 'Welcome to EventWish, {{name}}! We\'re glad you\'re here.',
        isActive: true,
        createdAt: '2023-01-02T00:00:00.000Z',
        updatedAt: '2023-01-02T00:00:00.000Z'
      }
    ],
    pagination: {
      total: 2,
      page: 1,
      limit: 10,
      totalPages: 1
    },
    isMockData: true
  }
};

export default mockData; 