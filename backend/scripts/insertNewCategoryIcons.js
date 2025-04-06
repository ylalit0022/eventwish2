const mongoose = require('mongoose');
const CategoryIcon = require('../models/CategoryIcon');
require('dotenv').config({ path: '../.env' });

// MongoDB connection URL from environment variables
const mongoURI = process.env.MONGODB_URI;

// Sample category icons data
const categoryIcons = [
    {
        category: 'Birthday',
        categoryIcon: 'https://example.com/icons/birthday.png'
    },
    {
        category: 'Wedding',
        categoryIcon: 'https://example.com/icons/wedding.png'
    },
    // Add more category icons as needed
];

// Function to insert category icons
async function insertCategoryIcons() {
    try {
        // Connect to MongoDB
        await mongoose.connect(mongoURI, {
            useNewUrlParser: true,
            useUnifiedTopology: true
        });
        console.log('Connected to MongoDB successfully');

        // Insert each category icon
        for (const iconData of categoryIcons) {
            // Check if category already exists
            const existingIcon = await CategoryIcon.findOne({ category: iconData.category });
            
            if (existingIcon) {
                console.log(`Category ${iconData.category} already exists. Updating icon...`);
                await CategoryIcon.findOneAndUpdate(
                    { category: iconData.category },
                    { categoryIcon: iconData.categoryIcon },
                    { new: true }
                );
                console.log(`Updated icon for category: ${iconData.category}`);
            } else {
                const newCategoryIcon = new CategoryIcon(iconData);
                await newCategoryIcon.save();
                console.log(`Inserted new category icon: ${iconData.category}`);
            }
        }

        console.log('Category icons insertion completed successfully');
    } catch (error) {
        console.error('Error inserting category icons:', error);
    } finally {
        // Close the MongoDB connection
        await mongoose.connection.close();
        console.log('MongoDB connection closed');
    }
}

// Run the insertion function
insertCategoryIcons();