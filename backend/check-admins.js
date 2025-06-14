require('dotenv').config();
const mongoose = require('mongoose');

async function checkAdmins() {
  try {
    console.log('MongoDB URI:', process.env.MONGODB_URI);
    await mongoose.connect(process.env.MONGODB_URI);
    console.log('Connected to MongoDB');
    
    const db = mongoose.connection;
    
    // Check if admins collection exists
    const collections = await db.db.listCollections().toArray();
    console.log('Collections:', collections.map(c => c.name));
    
    // Check admin users
    const admins = await db.collection('admins').find({}).toArray();
    console.log('Admin users:', JSON.stringify(admins, null, 2));
    
    // Check if there's an adminUsers collection
    try {
      const adminUsers = await db.collection('adminUsers').find({}).toArray();
      console.log('Admin users (from adminUsers collection):', JSON.stringify(adminUsers, null, 2));
    } catch (err) {
      console.log('No adminUsers collection found');
    }
    
    // Check if admin info is in users collection
    const adminInUsers = await db.collection('users').find({ isAdmin: true }).toArray();
    console.log('Admin users (from users collection):', JSON.stringify(adminInUsers, null, 2));
    
    mongoose.disconnect();
  } catch (err) {
    console.error('Error:', err);
  }
}

checkAdmins(); 