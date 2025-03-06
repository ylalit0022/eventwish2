require('dotenv').config({ path: require('path').resolve(__dirname, '../.env') });
const mongoose = require('mongoose');
const Festival = require('../models/Festival');
const CategoryIcon = require('../models/CategoryIcon');
const Template = require('../models/Template');
const { startOfDay, addDays } = require('date-fns');

// Sample festival data
const festivals = [
    {
        name: "Diwali",
        description: "Festival of Lights",
        category: "Cultural",
        imageUrl: "https://currentedu365.in/wp-content/uploads/2024/09/nwes-7-1024x576.png",
        isActive: true
    },
    {
        name: "Christmas",
        description: "Christian festival celebrating the birth of Jesus",
        category: "Cultural",
        imageUrl: "https://currentedu365.in/wp-content/uploads/2024/09/nwes-7-1024x576.png",
        isActive: true
    },
    {
        name: "Eid",
        description: "Islamic festival marking the end of Ramadan",
        category: "Cultural",
        imageUrl: "https://currentedu365.in/wp-content/uploads/2024/09/nwes-7-1024x576.png",
        isActive: true
    }
];

// Function to insert festivals
async function insertFestivals() {
    try {
        // Connect to MongoDB
        await mongoose.connect(process.env.MONGODB_URI, {
            useNewUrlParser: true,
            useUnifiedTopology: true
        });
        console.log('Connected to MongoDB successfully');

        // Get category icons for reference
        const categoryIcons = await CategoryIcon.find();
        const categoryIconMap = {};
        categoryIcons.forEach(icon => {
            categoryIconMap[icon.category] = icon._id;
        });

        // Get templates for reference
        const templates = await Template.find({ status: true });
        const templatesByCategory = {};
        templates.forEach(template => {
            if (!templatesByCategory[template.category]) {
                templatesByCategory[template.category] = [];
            }
            templatesByCategory[template.category].push(template._id);
        });

        // Clear existing festivals
        await Festival.deleteMany({});
        console.log('Cleared existing festivals');

        // Set dates relative to today
        const today = startOfDay(new Date());
        const processedFestivals = festivals.map((festival, index) => ({
            ...festival,
            date: addDays(today, index * 7), // Spread festivals weekly
            categoryIcon: categoryIconMap[festival.category] || categoryIconMap['Other'],
            templates: templatesByCategory[festival.category] || []
        }));

        // Insert festivals
        const result = await Festival.insertMany(processedFestivals);
        console.log(`Successfully inserted ${result.length} festivals`);

        // Display inserted festivals
        console.log('\nInserted festivals:');
        result.forEach(festival => {
            console.log(`- ${festival.name} (${festival.category}): ${festival.date.toLocaleDateString()} with ${festival.templates.length} templates`);
        });

    } catch (error) {
        console.error('Error inserting festivals:', error.message);
    } finally {
        await mongoose.connection.close();
        console.log('\nDatabase connection closed');
    }
}

// Run the insertion
insertFestivals();