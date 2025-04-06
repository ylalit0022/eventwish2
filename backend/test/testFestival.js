const mongoose = require('mongoose');
const Festival = require('../models/Festival');
const connectDB = require('../config/db');
const { addMinutes } = require('date-fns');
require('dotenv').config({ path: require('path').resolve(__dirname, '../.env') });

// Create a test festival that will trigger in 1-2 minutes
const createTestFestival = async () => {
    try {
        // Connect to MongoDB
        await mongoose.connect(process.env.MONGODB_URI, {
            useNewUrlParser: true,
            useUnifiedTopology: true
        });
        console.log('Connected to MongoDB successfully');

        // Create a date 2 minutes from now
        const festivalDate = addMinutes(new Date(), 2);
        
        const testFestival = new Festival({
            name: "Test Notification Festival 3",
            description: "This is a test festival to trigger notifications",
            date: festivalDate,
            category: "Test",
            imageUrl: "https://currentedu365.in/wp-content/uploads/2024/09/test-icon.png",
            isActive: true
        });

        // Save the festival
        await testFestival.save();
        
        console.log('Test festival created successfully!');
        console.log('Festival details:');
        console.log('Name:', testFestival.name);
        console.log('Date:', testFestival.date);
        console.log('Time until notification:', Math.round((festivalDate - new Date()) / 1000), 'seconds');
        
    } catch (error) {
        console.error('Error creating test festival:', error);
    } finally {
        // Close the connection
        await mongoose.connection.close();
        console.log('MongoDB connection closed');
        process.exit(0);
    }
};

// Run the creation
createTestFestival();

// Ensure the script is ready to run by checking the current directory and executing the script. 