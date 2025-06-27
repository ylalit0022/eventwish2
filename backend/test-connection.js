const mongoose = require('mongoose');

async function testConnection() {
    try {
        console.log('🔄 Testing MongoDB Atlas connection...');
        console.log('📡 URI:', process.env.MONGODB_URI ? 'Set' : 'Not set');
        
        if (!process.env.MONGODB_URI) {
            console.error('❌ MONGODB_URI environment variable not set');
            process.exit(1);
        }
        
        await mongoose.connect(process.env.MONGODB_URI, {
            serverSelectionTimeoutMS: 10000,
            connectTimeoutMS: 10000,
        });
        
        console.log('✅ Connected to MongoDB Atlas successfully!');
        
        // Test a simple query
        const collections = await mongoose.connection.db.listCollections().toArray();
        console.log('📊 Available collections:', collections.map(c => c.name).join(', '));
        
        await mongoose.disconnect();
        console.log('✅ Disconnected successfully');
        
    } catch (error) {
        console.error('❌ Connection error:', error.message);
        process.exit(1);
    }
}

testConnection(); 