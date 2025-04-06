require('dotenv').config({ path: require('path').resolve(__dirname, '../.env') });
const mongoose = require('mongoose');
const Festival = require('../models/Festival');
const CategoryIcon = require('../models/CategoryIcon');
const Template = require('../models/Template');
const { startOfDay, addDays } = require('date-fns');

// Sample category icons data
const categoryIcons = [
    {
        category: 'Cultural',
        categoryIcon: 'https://currentedu365.in/wp-content/uploads/2024/09/cultural-icon.png'
    },
    {
        category: 'Religious',
        categoryIcon: 'https://currentedu365.in/wp-content/uploads/2024/09/religious-icon.png'
    },
    {
        category: 'National',
        categoryIcon: 'https://currentedu365.in/wp-content/uploads/2024/09/national-icon.png'
    },
    {
        category: 'Other',
        categoryIcon: 'https://currentedu365.in/wp-content/uploads/2024/09/other-icon.png'
    }
];

// Sample templates data
const templates = [
    {
        title: 'Cultural Celebration',
        category: 'Cultural',
        htmlContent: '<div class="cultural-template"><h1>Cultural Celebration</h1></div>',
        cssContent: '.cultural-template { text-align: center; }',
        jsContent: '',
        previewUrl: 'https://currentedu365.in/wp-content/uploads/2024/09/cultural-preview.jpg',
        status: true,
        categoryIcon: 'ic_cultural'
    },
    {
        title: 'Religious Festival',
        category: 'Religious',
        htmlContent: '<div class="religious-template"><h1>Religious Festival</h1></div>',
        cssContent: '.religious-template { text-align: center; }',
        jsContent: '',
        previewUrl: 'https://currentedu365.in/wp-content/uploads/2024/09/religious-preview.jpg',
        status: true,
        categoryIcon: 'ic_religious'
    }
];

// Sample festivals data
const festivals = [
    {
        name: 'Diwali',
        description: 'Festival of Lights',
        category: 'Cultural',
        imageUrl: 'https://currentedu365.in/wp-content/uploads/2024/09/diwali.jpg',
        isActive: true
    },
    {
        name: 'Eid',
        description: 'Islamic festival marking the end of Ramadan',
        category: 'Religious',
        imageUrl: 'https://currentedu365.in/wp-content/uploads/2024/09/eid.jpg',
        isActive: true
    }
];

// Function to insert all data with proper relationships
async function insertLinkedData() {
    let connection;
    try {
        // Connect to MongoDB
        connection = await mongoose.connect(process.env.MONGODB_URI, {
            useNewUrlParser: true,
            useUnifiedTopology: true
        });
        console.log('Connected to MongoDB successfully');

        // Step 1: Insert Category Icons
        console.log('\nStep 1: Inserting Category Icons...');
        await CategoryIcon.deleteMany({});
        const savedCategoryIcons = await CategoryIcon.insertMany(categoryIcons);
        console.log(`Inserted ${savedCategoryIcons.length} category icons`);

        // Create a map of category to categoryIcon _id
        const categoryIconMap = {};
        savedCategoryIcons.forEach(icon => {
            categoryIconMap[icon.category] = icon._id;
        });

        // Step 2: Insert Templates
        console.log('\nStep 2: Inserting Templates...');
        await Template.deleteMany({});
        const savedTemplates = await Template.insertMany(templates);
        console.log(`Inserted ${savedTemplates.length} templates`);

        // Create a map of category to template _ids
        const templatesByCategory = {};
        savedTemplates.forEach(template => {
            if (!templatesByCategory[template.category]) {
                templatesByCategory[template.category] = [];
            }
            templatesByCategory[template.category].push(template._id);
        });

        // Step 3: Insert Festivals with references
        console.log('\nStep 3: Inserting Festivals with references...');
        await Festival.deleteMany({});

        const today = startOfDay(new Date());
        const processedFestivals = festivals.map((festival, index) => ({
            ...festival,
            date: addDays(today, index * 7),
            categoryIcon: categoryIconMap[festival.category] || categoryIconMap['Other'],
            templates: templatesByCategory[festival.category] || []
        }));

        const savedFestivals = await Festival.insertMany(processedFestivals);

        // Step 4: Display results with populated references
        console.log('\nInserted Festivals with their relationships:');
        const populatedFestivals = await Festival.find()
            .populate('categoryIcon')
            .populate('templates');

        populatedFestivals.forEach(festival => {
            console.log(`\nFestival: ${festival.name}`);
            console.log(`Category: ${festival.category}`);
            console.log(`Category Icon: ${festival.categoryIcon.categoryIcon}`);
            console.log(`Number of Templates: ${festival.templates.length}`);
            console.log(`Date: ${festival.date.toLocaleDateString()}`);
        });

    } catch (error) {
        console.error('Error:', error.message);
    } finally {
        if (connection) {
            await mongoose.connection.close();
            console.log('\nDatabase connection closed');
        }
    }
}

// Run the insertion
insertLinkedData();