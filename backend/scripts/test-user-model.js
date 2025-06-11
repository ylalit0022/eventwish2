const mongoose = require('mongoose');
require('../config/env-loader');
const User = require('../models/User');

// Helper function to print user data
function printUserData(user) {
  console.log('\nUser Document Data:');
  console.log('==================');
  console.log(JSON.stringify({
    _id: user._id,
    uid: user.uid,
    deviceId: user.deviceId,
    displayName: user.displayName,
    email: user.email,
    preferredTheme: user.preferredTheme,
    preferredLanguage: user.preferredLanguage,
    timezone: user.timezone,
    pushPreferences: user.pushPreferences,
    lastActionOnTemplate: user.lastActionOnTemplate,
    categories: user.categories,
    created: user.created,
    lastOnline: user.lastOnline
  }, null, 2));
  console.log('==================\n');
}

// Connect to MongoDB
mongoose.connect(process.env.MONGODB_URI, {
  useNewUrlParser: true,
  useUnifiedTopology: true
})
.then(() => {
  console.log('Connected to MongoDB');
  testUserModel();
})
.catch(err => {
  console.error('MongoDB connection error:', err);
  process.exit(1);
});

async function testUserModel() {
  try {
    console.log('\n===== TESTING USER MODEL =====\n');
    
    // 1. Create a test user with Firebase UID
    console.log('1. Creating test user with Firebase UID...');
    const testUser = new User({
      uid: 'test_firebase_uid_' + Date.now(),
      deviceId: 'test_device_id_' + Date.now(),
      displayName: 'Test User',
      email: 'test@example.com',
      preferredTheme: 'dark',
      preferredLanguage: 'en',
      pushPreferences: {
        allowFestivalPush: true,
        allowPersonalPush: true
      },
      lastActionOnTemplate: 'VIEW'
    });
    
    await testUser.save();
    console.log('✅ Test user created successfully!');
    console.log('Initial user data:');
    printUserData(testUser);
    
    // 2. Find user by Firebase UID
    console.log('\n2. Finding user by Firebase UID...');
    const userByUid = await User.findOne({ uid: testUser.uid });
    if (userByUid) {
      console.log('✅ User found by Firebase UID!');
      console.log('User data when found by UID:');
      printUserData(userByUid);
    } else {
      console.error('❌ Failed to find user by Firebase UID');
    }
    
    // 3. Find user by Device ID
    console.log('\n3. Finding user by Device ID...');
    const userByDeviceId = await User.findOne({ deviceId: testUser.deviceId });
    if (userByDeviceId) {
      console.log('✅ User found by Device ID!');
      console.log('User data when found by Device ID:');
      printUserData(userByDeviceId);
    } else {
      console.error('❌ Failed to find user by Device ID');
    }
    
    // 4. Update user preferences
    console.log('\n4. Updating user preferences...');
    testUser.preferredTheme = 'light';
    testUser.preferredLanguage = 'hi';
    testUser.timezone = 'Asia/Kolkata';
    await testUser.save();
    
    const updatedUser = await User.findById(testUser._id);
    if (updatedUser.preferredTheme === 'light' && 
        updatedUser.preferredLanguage === 'hi' &&
        updatedUser.timezone === 'Asia/Kolkata') {
      console.log('✅ User preferences updated successfully!');
      console.log('User data after preference update:');
      printUserData(updatedUser);
    } else {
      console.error('❌ Failed to update user preferences');
    }
    
    // 5. Test category visit method
    console.log('\n5. Testing category visit method...');
    await testUser.visitCategory('birthday', 'direct');
    
    const userWithCategory = await User.findById(testUser._id);
    const birthdayCategory = userWithCategory.categories.find(c => c.category === 'birthday');
    
    if (birthdayCategory) {
      console.log('✅ Category visit recorded successfully!');
      console.log('User data after category visit:');
      printUserData(userWithCategory);
    } else {
      console.error('❌ Failed to record category visit');
    }
    
    // 6. Test setLastActiveTemplate method
    console.log('\n6. Testing setLastActiveTemplate method...');
    const fakeTemplateId = new mongoose.Types.ObjectId();
    await testUser.setLastActiveTemplate(fakeTemplateId, 'VIEW');
    
    const userWithTemplate = await User.findById(testUser._id);
    if (userWithTemplate.lastActiveTemplate && 
        userWithTemplate.lastActiveTemplate.toString() === fakeTemplateId.toString() &&
        userWithTemplate.lastActionOnTemplate === 'VIEW') {
      console.log('✅ Last active template set successfully!');
      console.log('User data after setting last active template:');
      printUserData(userWithTemplate);
    } else {
      console.error('❌ Failed to set last active template');
    }
    
    // Show all users in the collection
    console.log('\nAll users in collection:');
    const allUsers = await User.find({});
    console.log(`Total users in collection: ${allUsers.length}`);
    allUsers.forEach((user, index) => {
      console.log(`\nUser ${index + 1}:`);
      printUserData(user);
    });
    
    // 7. Clean up - delete test user
    console.log('\n7. Cleaning up - deleting test user...');
    await User.deleteOne({ _id: testUser._id });
    const deletedUser = await User.findById(testUser._id);
    
    if (!deletedUser) {
      console.log('✅ Test user deleted successfully!');
    } else {
      console.error('❌ Failed to delete test user');
    }
    
    console.log('\n===== USER MODEL TESTS COMPLETED =====\n');
    
    mongoose.connection.close();
    console.log('Database connection closed.');
  } catch (error) {
    console.error('Error testing User model:', error);
    mongoose.connection.close();
    process.exit(1);
  }
} 