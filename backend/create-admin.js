require('dotenv').config();
const mongoose = require('mongoose');
const { v4: uuidv4 } = require('uuid');

// Replace with the email you're using for Google Sign-In
const ADMIN_EMAIL = 'ylalit0022@gmail.com'; // Update this with your actual email

async function createAdmin() {
  try {
    console.log('MongoDB URI:', process.env.MONGODB_URI);
    await mongoose.connect(process.env.MONGODB_URI);
    console.log('Connected to MongoDB');
    
    const db = mongoose.connection;
    
    // Create admins collection if it doesn't exist
    const collections = await db.db.listCollections().toArray();
    if (!collections.find(c => c.name === 'admins')) {
      console.log('Creating admins collection...');
      await db.createCollection('admins');
    }
    
    // Check if admin already exists
    const existingAdmin = await db.collection('admins').findOne({ email: ADMIN_EMAIL });
    if (existingAdmin) {
      console.log('Admin already exists:', existingAdmin);
    } else {
      // Create admin user
      const admin = {
        _id: uuidv4(),
        email: ADMIN_EMAIL,
        role: 'superAdmin',
        isAdmin: true,
        createdAt: new Date(),
        updatedAt: new Date()
      };
      
      const result = await db.collection('admins').insertOne(admin);
      console.log('Admin created:', result);
      console.log('Admin document:', admin);
    }
    
    // Verify admin was created
    const admins = await db.collection('admins').find({}).toArray();
    console.log('All admins:', JSON.stringify(admins, null, 2));
    
    mongoose.disconnect();
  } catch (err) {
    console.error('Error:', err);
  }
}

createAdmin(); 