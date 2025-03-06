require('dotenv').config({ path: require('path').resolve(__dirname, '../.env') });
const mongoose = require('mongoose');
const Festival = require('../models/Festival');
const CategoryIcon = require('../models/CategoryIcon');
const Template = require('../models/Template');
const { startOfDay, addDays } = require('date-fns');

// Sample category icons data with 10 categories
const categoryIcons = [
    { category: 'Birthday', categoryIcon: 'https://currentedu365.in/wp-content/uploads/2024/09/birthday-icon.png' },
    { category: 'Wedding', categoryIcon: 'https://currentedu365.in/wp-content/uploads/2024/09/wedding-icon.png' },
    { category: 'Cultural', categoryIcon: 'https://currentedu365.in/wp-content/uploads/2024/09/cultural-icon.png' },
    { category: 'Religious', categoryIcon: 'https://currentedu365.in/wp-content/uploads/2024/09/religious-icon.png' },
    { category: 'National', categoryIcon: 'https://currentedu365.in/wp-content/uploads/2024/09/national-icon.png' },
    { category: 'Anniversary', categoryIcon: 'https://currentedu365.in/wp-content/uploads/2024/09/anniversary-icon.png' },
    { category: 'Graduation', categoryIcon: 'https://currentedu365.in/wp-content/uploads/2024/09/graduation-icon.png' },
    { category: 'New Year', categoryIcon: 'https://currentedu365.in/wp-content/uploads/2024/09/newyear-icon.png' },
    { category: 'Valentine', categoryIcon: 'https://currentedu365.in/wp-content/uploads/2024/09/valentine-icon.png' },
    { category: 'Christmas', categoryIcon: 'https://currentedu365.in/wp-content/uploads/2024/09/christmas-icon.png' }
];

// Generate 100 templates (10 per category)
const generateTemplates = () => {
    const templates = [];
    categoryIcons.forEach(({ category }) => {
        for (let i = 1; i <= 10; i++) {
            templates.push({
                title: `${category} Template ${i}`,
                category,
                htmlContent: `<div class="${category.toLowerCase()}-template-${i}">
                    <h1>${category} Celebration ${i}</h1>
                    <div class="content">
                        <p class="message">Your special message here</p>
                        <div class="decoration"></div>
                    </div>
                </div>`,
                cssContent: `.${category.toLowerCase()}-template-${i} {
                    background: linear-gradient(45deg, #${Math.floor(Math.random()*16777215).toString(16)}, #${Math.floor(Math.random()*16777215).toString(16)});
                    padding: 20px;
                    border-radius: 10px;
                    text-align: center;
                }
                .${category.toLowerCase()}-template-${i} .content {
                    background: rgba(255,255,255,0.9);
                    margin: 15px;
                    padding: 20px;
                    border-radius: 8px;
                }
                .${category.toLowerCase()}-template-${i} .decoration {
                    width: 50px;
                    height: 50px;
                    margin: 10px auto;
                    background: #${Math.floor(Math.random()*16777215).toString(16)};
                    transform: rotate(45deg);
                }`,
                jsContent: `document.querySelector('.${category.toLowerCase()}-template-${i} .decoration').addEventListener('click', function() {
                    this.style.transform = 'rotate(' + (45 + 360) + 'deg)';
                    this.style.transition = 'transform 1s ease';
                });`,
                previewUrl: `https://currentedu365.in/wp-content/uploads/2024/09/${category.toLowerCase()}-template-${i}.jpg`,
                status: true,
                categoryIcon: `ic_${category.toLowerCase()}`
            });
        }
    });
    return templates;
};

// Generate 20 festivals across categories
const generateFestivals = () => [
    { name: "New Year's Day", description: "Celebration of the new year", category: "New Year" },
    { name: "Valentine's Day", description: "Day of love and romance", category: "Valentine" },
    { name: "Christmas", description: "Christian festival celebrating birth of Jesus", category: "Christmas" },
    { name: "Diwali", description: "Festival of Lights", category: "Cultural" },
    { name: "Eid", description: "Islamic festival marking the end of Ramadan", category: "Religious" },
    { name: "Independence Day", description: "National day of independence", category: "National" },
    { name: "Graduation Day", description: "Celebration of academic achievement", category: "Graduation" },
    { name: "Wedding Anniversary", description: "Celebration of marriage", category: "Anniversary" },
    { name: "Birthday Celebration", description: "Personal birthday celebration", category: "Birthday" },
    { name: "Wedding Day", description: "Celebration of marriage ceremony", category: "Wedding" },
    { name: "Holi", description: "Festival of Colors", category: "Cultural" },
    { name: "Easter", description: "Christian festival of resurrection", category: "Religious" },
    { name: "Republic Day", description: "National day of republic", category: "National" },
    { name: "Convocation", description: "Academic ceremony", category: "Graduation" },
    { name: "Silver Jubilee", description: "25th anniversary celebration", category: "Anniversary" },
    { name: "Sweet Sixteen", description: "Special birthday celebration", category: "Birthday" },
    { name: "Engagement", description: "Pre-wedding celebration", category: "Wedding" },
    { name: "Chinese New Year", description: "Lunar New Year celebration", category: "New Year" },
    { name: "Mother's Day", description: "Celebration of motherhood", category: "Cultural" },
    { name: "Father's Day", description: "Celebration of fatherhood", category: "Cultural" }
].map(festival => ({
    ...festival,
    imageUrl: `https://currentedu365.in/wp-content/uploads/2024/09/${festival.name.toLowerCase().replace(/[^a-z0-9]/g, '-')}.jpg`,
    isActive: true
}));

// Function to insert all data with proper relationships
async function insertAllData() {
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
        const templates = generateTemplates();
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
        const festivals = generateFestivals();
        const processedFestivals = festivals.map((festival, index) => ({
            ...festival,
            date: addDays(today, index * 7), // Spread festivals weekly
            categoryIcon: categoryIconMap[festival.category],
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

        console.log('\nSummary:');
        console.log(`- Categories: ${savedCategoryIcons.length}`);
        console.log(`- Templates: ${savedTemplates.length}`);
        console.log(`- Festivals: ${savedFestivals.length}`);

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
insertAllData();