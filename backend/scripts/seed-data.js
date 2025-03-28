const mongoose = require('mongoose');
const CategoryIcon = require('../models/CategoryIcon');
const Festival = require('../models/Festival');
const Template = require('../models/Template');
const dotenv = require('dotenv');

// Load environment variables
dotenv.config();

// MongoDB connection string
const MONGODB_URI = process.env.MONGODB_URI || 'mongodb://localhost:27017/eventwish';

// Sample category icons data
const categoryIcons = [
  {
    category: 'birthday',
    categoryIcon: 'https://example.com/icons/birthday.png'
  },
  {
    category: 'wedding',
    categoryIcon: 'https://example.com/icons/wedding.png'
  },
  {
    category: 'anniversary',
    categoryIcon: 'https://example.com/icons/anniversary.png'
  },
  {
    category: 'holiday',
    categoryIcon: 'https://example.com/icons/holiday.png'
  },
  {
    category: 'graduation',
    categoryIcon: 'https://example.com/icons/graduation.png'
  }
];

// Function to seed data
async function seedData() {
  try {
    // Connect to MongoDB only if not already connected
    if (mongoose.connection.readyState !== 1) {
      console.log('Connecting to MongoDB...');
      await mongoose.connect(MONGODB_URI);
      console.log('Connected to MongoDB');
    } else {
      console.log('Already connected to MongoDB');
    }

    // Clear existing data
    console.log('Clearing existing data...');
    await CategoryIcon.deleteMany({});
    await Festival.deleteMany({});
    await Template.deleteMany({});

    // Insert category icons
    console.log('Inserting category icons...');
    const insertedIcons = await CategoryIcon.insertMany(categoryIcons);
    console.log(`Inserted ${insertedIcons.length} category icons`);

    // Create a map of category to icon IDs for easy lookup
    const categoryToIconId = {};
    insertedIcons.forEach(icon => {
      categoryToIconId[icon.category] = icon._id;
    });

    // Create templates with references to category icons
    console.log('Creating templates...');
    const templates = [
      {
        title: 'Birthday Basic',
        category: 'birthday',
        htmlContent: '<div class="birthday-container"><h1>Happy Birthday!</h1><p>Wishing you a fantastic day filled with joy!</p></div>',
        cssContent: '.birthday-container { text-align: center; color: #ff4081; }',
        jsContent: 'console.log("Birthday template loaded");',
        previewUrl: 'https://example.com/previews/birthday_basic.png',
        status: true,
        categoryIcon: categoryToIconId['birthday']
      },
      {
        title: 'Wedding Elegant',
        category: 'wedding',
        htmlContent: '<div class="wedding-container"><h1>Congratulations!</h1><p>Celebrating your special day with love and best wishes.</p></div>',
        cssContent: '.wedding-container { text-align: center; color: #3f51b5; }',
        jsContent: 'console.log("Wedding template loaded");',
        previewUrl: 'https://example.com/previews/wedding_elegant.png',
        status: true,
        categoryIcon: categoryToIconId['wedding']
      },
      {
        title: 'Anniversary Gold',
        category: 'anniversary',
        htmlContent: '<div class="anniversary-container"><h1>Happy Anniversary!</h1><p>Celebrating your years of love and happiness.</p></div>',
        cssContent: '.anniversary-container { text-align: center; color: #ffd700; }',
        jsContent: 'console.log("Anniversary template loaded");',
        previewUrl: 'https://example.com/previews/anniversary_gold.png',
        status: true,
        categoryIcon: categoryToIconId['anniversary']
      },
      {
        title: 'Holiday Cheer',
        category: 'holiday',
        htmlContent: '<div class="holiday-container"><h1>Season\'s Greetings!</h1><p>Wishing you peace and joy this holiday season.</p></div>',
        cssContent: '.holiday-container { text-align: center; color: #2e7d32; }',
        jsContent: 'console.log("Holiday template loaded");',
        previewUrl: 'https://example.com/previews/holiday_cheer.png',
        status: true,
        categoryIcon: categoryToIconId['holiday']
      },
      {
        title: 'Graduation Caps',
        category: 'graduation',
        htmlContent: '<div class="graduation-container"><h1>Congratulations Graduate!</h1><p>Your hard work has paid off!</p></div>',
        cssContent: '.graduation-container { text-align: center; color: #000; }',
        jsContent: 'console.log("Graduation template loaded");',
        previewUrl: 'https://example.com/previews/graduation_caps.png',
        status: true,
        categoryIcon: categoryToIconId['graduation']
      }
    ];

    const insertedTemplates = await Template.insertMany(templates);
    console.log(`Inserted ${insertedTemplates.length} templates`);

    // Create a map of template IDs by category for festivals
    const templateIdsByCategory = {};
    insertedTemplates.forEach(template => {
      if (!templateIdsByCategory[template.category]) {
        templateIdsByCategory[template.category] = [];
      }
      templateIdsByCategory[template.category].push(template._id);
    });

    // Create festivals with references to category icons and templates
    console.log('Creating festivals...');
    const festivals = [
      {
        name: 'New Year Celebration',
        date: new Date('2025-01-01'),
        description: 'Celebrate the beginning of a new year with joy and happiness.',
        category: 'holiday',
        categoryIcon: categoryToIconId['holiday'],
        imageUrl: 'https://example.com/images/new_year.jpg',
        templates: templateIdsByCategory['holiday'],
        isActive: true
      },
      {
        name: 'Valentine\'s Day',
        date: new Date('2025-02-14'),
        description: 'Celebrate love and affection.',
        category: 'anniversary',
        categoryIcon: categoryToIconId['anniversary'],
        imageUrl: 'https://example.com/images/valentines.jpg',
        templates: templateIdsByCategory['anniversary'],
        isActive: true
      },
      {
        name: 'Graduation Season',
        date: new Date('2025-06-15'),
        description: 'Celebrating academic achievements and new beginnings.',
        category: 'graduation',
        categoryIcon: categoryToIconId['graduation'],
        imageUrl: 'https://example.com/images/graduation.jpg',
        templates: templateIdsByCategory['graduation'],
        isActive: true
      },
      {
        name: 'Wedding Season',
        date: new Date('2025-06-01'),
        description: 'Perfect time for weddings and celebrations.',
        category: 'wedding',
        categoryIcon: categoryToIconId['wedding'],
        imageUrl: 'https://example.com/images/wedding_season.jpg',
        templates: templateIdsByCategory['wedding'],
        isActive: true
      },
      {
        name: 'Birthday Bash',
        date: new Date('2025-07-15'),
        description: 'A special day to celebrate birthdays.',
        category: 'birthday',
        categoryIcon: categoryToIconId['birthday'],
        imageUrl: 'https://example.com/images/birthday_bash.jpg',
        templates: templateIdsByCategory['birthday'],
        isActive: true
      }
    ];

    const insertedFestivals = await Festival.insertMany(festivals);
    console.log(`Inserted ${insertedFestivals.length} festivals`);

    console.log('Seeding completed successfully!');
    return true; // Return true to indicate success
  } catch (error) {
    console.error('Error seeding data:', error);
    return false; // Return false to indicate failure
  } finally {
    // Close MongoDB connection only if we opened it and if this script is run directly
    if (mongoose.connection.readyState === 1 && require.main === module) {
      await mongoose.connection.close();
      console.log('MongoDB connection closed');
    }
  }
}

// Export the function
module.exports = seedData;

// Run the seed function only if this file is executed directly
if (require.main === module) {
  seedData();
} 