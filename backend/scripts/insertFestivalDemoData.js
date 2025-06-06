const path = require('path');
require('dotenv').config({ path: path.join(__dirname, '../.env') });
const mongoose = require('mongoose');
const Festival = require('../models/Festival');
const festivalDemoData = require('../data/festivalDemoData');

// Suppress the deprecation warning
mongoose.set('strictQuery', false);

// Check if environment variables are loaded
if (!process.env.MONGODB_URI) {
    console.error('Error: MONGODB_URI not found in environment variables');
    console.error('Make sure .env file exists and contains MONGODB_URI');
    process.exit(1);
}

// MongoDB Atlas connection string from .env
const MONGODB_URI = process.env.MONGODB_URI;

console.log('Attempting to connect to MongoDB Atlas...');
// Hide credentials in logs
console.log('Connection string:', MONGODB_URI.replace(/mongodb\+srv:\/\/([^:]+):([^@]+)@/, 'mongodb+srv://[username]:[password]@'));

async function insertFestivalDemoData() {
    try {
        // Connect to MongoDB Atlas with more detailed options
        await mongoose.connect(MONGODB_URI, {
            useNewUrlParser: true,
            useUnifiedTopology: true,
            serverSelectionTimeoutMS: 10000, // Increased timeout for Atlas connection
            socketTimeoutMS: 45000, // Increased socket timeout for Atlas
            family: 4 // Use IPv4, skip trying IPv6
        });
        console.log('Connected to MongoDB Atlas successfully');

        // Delete existing festivals (optional - comment out if you want to keep existing data)
        await Festival.deleteMany({});
        console.log('Cleared existing festival data');

        // Insert demo data
        const result = await Festival.insertMany(festivalDemoData);
        console.log(`Successfully inserted ${result.length} festivals`);

        // Print inserted festivals
        console.log('\nInserted Festivals:');
        result.forEach((festival, index) => {
            console.log(`${index + 1}. ${festival.name} (${festival.category}) - ${festival.date.toDateString()}`);
        });

    } catch (error) {
        console.error('Error details:', {
            name: error.name,
            message: error.message,
            code: error.code,
            // If there's a connection error, show more details
            connectionError: error.reason ? {
                type: error.reason.type,
                servers: error.reason.servers ? Array.from(error.reason.servers.keys()) : []
            } : undefined
        });
        
        // Provide helpful suggestions based on the error
        if (error.name === 'MongooseServerSelectionError') {
            console.log('\nTroubleshooting suggestions for MongoDB Atlas:');
            console.log('1. Check your internet connection');
            console.log('2. Verify the connection string in .env file is correct');
            console.log('3. Make sure your IP address is whitelisted in MongoDB Atlas');
            console.log('4. Check if the database user credentials are correct');
            console.log('5. Ensure the cluster is running in MongoDB Atlas dashboard');
            console.log('\nCurrent working directory:', process.cwd());
            console.log('.env file path:', path.join(__dirname, '../.env'));
        }
    } finally {
        try {
            await mongoose.connection.close();
            console.log('\nDatabase connection closed');
        } catch (err) {
            console.error('Error closing connection:', err);
        }
    }
}

// Run the insertion script
insertFestivalDemoData(); 