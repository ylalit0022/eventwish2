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
    console.log(`   - User ID: ${testUser._id}`);
    console.log(`   - Firebase UID: ${testUser.uid}`);
    console.log(`   - Device ID: ${testUser.deviceId}`);
    
    // 2. Find user by Firebase UID
    console.log('\n2. Finding user by Firebase UID...');
    const userByUid = await User.findOne({ uid: testUser.uid });
    if (userByUid) {
      console.log('✅ User found by Firebase UID!');
      console.log(`   - User ID: ${userByUid._id}`);
      console.log(`   - Name: ${userByUid.displayName}`);
    } else {
      console.error('❌ Failed to find user by Firebase UID');
    }
    
    // 3. Find user by Device ID
    console.log('\n3. Finding user by Device ID...');
    const userByDeviceId = await User.findOne({ deviceId: testUser.deviceId });
    if (userByDeviceId) {
      console.log('✅ User found by Device ID!');
      console.log(`   - User ID: ${userByDeviceId._id}`);
      console.log(`   - Name: ${userByDeviceId.displayName}`);
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
      console.log(`   - Theme: ${updatedUser.preferredTheme}`);
      console.log(`   - Language: ${updatedUser.preferredLanguage}`);
      console.log(`   - Timezone: ${updatedUser.timezone}`);
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
      console.log(`   - Category: ${birthdayCategory.category}`);
      console.log(`   - Visit count: ${birthdayCategory.visitCount}`);
      console.log(`   - Source: ${birthdayCategory.source}`);
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
      console.log(`   - Template ID: ${userWithTemplate.lastActiveTemplate}`);
      console.log(`   - Action: ${userWithTemplate.lastActionOnTemplate}`);
    } else {
      console.error('❌ Failed to set last active template');
    }
    
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