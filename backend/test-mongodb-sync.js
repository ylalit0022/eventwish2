const mongoose = require('mongoose');
const User = require('./models/User');
require('dotenv').config();

console.log('MongoDB Sync Test');
console.log('=================');
console.log(`MongoDB URI exists: ${!!process.env.MONGODB_URI}`);

// Test connection with fallback URLs
const mongoUri = process.env.MONGODB_URI || 
                 process.env.MONGO_URI || 
                 'mongodb://localhost:27017/eventwish';

console.log(`Using MongoDB URI: ${mongoUri.substring(0, mongoUri.indexOf('?') > 0 ? mongoUri.indexOf('?') : 15)}...`);

// Connect to MongoDB
mongoose.connect(mongoUri, {
  useNewUrlParser: true,
  useUnifiedTopology: true,
  serverSelectionTimeoutMS: 30000,
  socketTimeoutMS: 45000,
  family: 4
})
.then(async () => {
  console.log('✅ MongoDB connected successfully');
  
  try {
    // Find users with Firebase UIDs
    const usersWithUid = await User.find({ uid: { $exists: true, $ne: null } })
                                  .limit(5)
                                  .sort({ lastOnline: -1 });
    
    console.log(`\nFound ${usersWithUid.length} users with Firebase UIDs:`);
    
    if (usersWithUid.length > 0) {
      usersWithUid.forEach(user => {
        console.log(`\n- UID: ${user.uid}`);
        console.log(`  Display Name: ${user.displayName || 'Not set'}`);
        console.log(`  Email: ${user.email || 'Not set'}`);
        console.log(`  Device ID: ${user.deviceId || 'Not set'}`);
        console.log(`  Last Online: ${new Date(user.lastOnline).toLocaleString()}`);
        
        // Check if this user has profile data that would typically come from Google Sign-In
        const hasGoogleData = user.email && (user.email.includes('@gmail.com') || user.profilePhoto);
        if (hasGoogleData) {
          console.log(`  Profile Photo: ${user.profilePhoto ? 'Yes' : 'No'}`);
          console.log(`  Likely synced from Google Sign-In: Yes`);
        } else {
          console.log(`  Likely synced from Google Sign-In: No`);
        }
      });
      
      // Check API compatibility - compare model fields
      console.log('\nAPI Compatibility Check:');
      
      // Required fields for Android <-> Server sync
      const requiredFields = [
        'uid', 'displayName', 'email', 'profilePhoto', 'deviceId', 'lastOnline'
      ];
      
      // Check if each required field exists in the User model
      const userSchema = User.schema.obj;
      const missingFields = [];
      
      requiredFields.forEach(field => {
        if (!userSchema[field]) {
          missingFields.push(field);
        }
      });
      
      if (missingFields.length === 0) {
        console.log('✅ All required fields are present in the User model');
      } else {
        console.log(`❌ Missing fields in User model: ${missingFields.join(', ')}`);
      }
      
      // Check for users with both UID and deviceId - these would have been migrated or synced
      const migratedUsers = await User.find({
        uid: { $exists: true, $ne: null },
        deviceId: { $exists: true, $ne: null }
      }).limit(5);
      
      console.log(`\nFound ${migratedUsers.length} users with both UID and deviceId (migrated/synced):`);
      if (migratedUsers.length > 0) {
        migratedUsers.forEach(user => {
          console.log(`- UID: ${user.uid}, Device ID: ${user.deviceId}`);
        });
      }
    } else {
      console.log('No users with Firebase UIDs found. MongoDB sync may not be working properly.');
    }
  } catch (error) {
    console.error('❌ Error querying users:', error);
  } finally {
    // Close the connection
    await mongoose.disconnect();
    console.log('\nMongoDB connection closed');
  }
})
.catch(err => {
  console.error('❌ MongoDB connection error:');
  console.error(`Error message: ${err.message}`);
  console.error(`Error name: ${err.name}`);
  console.error(`Error code: ${err.code}`);
  console.error(`Stack trace: ${err.stack}`);
}); 