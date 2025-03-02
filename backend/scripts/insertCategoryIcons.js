const mongoose = require('mongoose');
const CategoryIcon = require('../models/CategoryIcon');
require('dotenv').config({ path: require('path').resolve(__dirname, '../.env') });

// Sample category icons data
const categoryIcons = [
    { category: 'Birthday', categoryIcon: 'https://currentedu365.in/wp-content/uploads/2024/09/jb-2-1024x576.png' },
    { category: 'Anniversary', categoryIcon: 'https://currentedu365.in/wp-content/uploads/2024/09/jb-2-1024x576.png' },
    { category: 'Wedding', categoryIcon: 'https://currentedu365.in/wp-content/uploads/2024/09/jb-2-1024x576.png' },
    { category: 'Graduation', categoryIcon: 'https://currentedu365.in/wp-content/uploads/2024/09/jb-2-1024x576.png' },
    { category: 'Christmas', categoryIcon: 'https://currentedu365.in/wp-content/uploads/2024/09/jb-2-1024x576.png' },
    { category: 'New Year', categoryIcon: 'https://currentedu365.in/wp-content/uploads/2024/09/jb-2-1024x576.png' },
    { category: 'Valentine', categoryIcon: 'https://currentedu365.in/wp-content/uploads/2024/09/jb-2-1024x576.png' },
    { category: 'Cultural', categoryIcon: 'https://currentedu365.in/wp-content/uploads/2024/09/jb-2-1024x576.png' },
    { category: 'Other', categoryIcon: 'https://currentedu365.in/wp-content/uploads/2024/09/jb-2-1024x576.png' }
];

// Function to insert category icons
const insertCategoryIcons = async () => {
    let connection;
    try {
        // Connect to database
        connection = await mongoose.connect(process.env.MONGODB_URI, {
            useNewUrlParser: true,
            useUnifiedTopology: true
        });
        console.log('MongoDB connected successfully');

        // Drop existing collection and recreate indexes
        const collections = await mongoose.connection.db.collections();
        const categoryIconCollection = collections.find(collection => collection.collectionName === 'categoryicons');
        if (categoryIconCollection) {
            await categoryIconCollection.drop();
            console.log('Dropped existing collection');
        }

        // Insert records one by one to ensure all are inserted
        const results = [];
        for (const icon of categoryIcons) {
            const newIcon = new CategoryIcon(icon);
            const savedIcon = await newIcon.save();
            results.push(savedIcon);
            console.log(`Inserted: ${icon.category}`);
        }

        console.log(`\nSuccessfully inserted ${results.length} category icons`);
        console.log('\nInserted category icons:');
        results.forEach(icon => {
            console.log(`- ${icon.category}: ${icon.categoryIcon}`);
        });

    } catch (error) {
        console.error('Error:', error.message);
    } finally {
        if (connection) {
            await mongoose.connection.close();
            console.log('\nDatabase connection closed');
        }
    }
};

// Run the insertion function
insertCategoryIcons();