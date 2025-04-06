const mongoose = require('mongoose');
const CategoryIcon = require('../models/CategoryIcon');
require('dotenv').config({ path: require('path').resolve(__dirname, '../.env') });

// MongoDB connection
mongoose.connect(process.env.MONGODB_URI)
    .then(() => console.log('MongoDB connected successfully'))
    .catch(err => console.error('MongoDB connection error:', err));

// Sample category icon data
const categoryIcons = [
    {
        category: 'Cultural',
        categoryIcon: 'https://currentedu365.in/wp-content/uploads/2024/09/jb-2-1024x576.png'
    },
    {
        category: 'Holi',
        categoryIcon: 'https://currentedu365.in/wp-content/uploads/2024/09/jb-2-1024x576.png'
    },
    {
        category: 'Birthday',
        categoryIcon: 'https://example.com/icons/birthday.png'
    },
    {
        category: 'Anniversary',
        categoryIcon: 'https://example.com/icons/anniversary.png'
    },
    {
        category: 'Wedding',
        categoryIcon: 'https://example.com/icons/wedding.png'
    }
];

// Function to insert category icons
async function insertCategoryIcons() {
    try {
        // Clear existing data
        await CategoryIcon.deleteMany({});
        console.log('Cleared existing category icons');

        // Insert new data
        const result = await CategoryIcon.insertMany(categoryIcons);
        console.log('Successfully inserted category icons:', result);

        // Verify the data
        const savedIcons = await CategoryIcon.find();
        console.log('\nVerifying inserted data:');
        console.log(JSON.stringify(savedIcons, null, 2));

    } catch (error) {
        console.error('Error inserting category icons:', error);
    } finally {
        // Close the connection
        mongoose.connection.close();
        console.log('\nMongoDB connection closed');
    }
}

// Run the insertion
insertCategoryIcons();