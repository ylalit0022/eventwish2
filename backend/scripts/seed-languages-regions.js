const mongoose = require('mongoose');
const Language = require('../models/Language');
const Region = require('../models/Region');
const dotenv = require('dotenv');

// Load environment variables
dotenv.config();

// MongoDB connection string
const MONGODB_URI = process.env.MONGODB_URI || 'mongodb://localhost:27017/eventwish';

// Sample languages data
const languagesData = [
  {
    code: 'en',
    name: 'English',
    nativeName: 'English',
    isRTL: false,
    isActive: true,
    displayOrder: 1
  },
  {
    code: 'es',
    name: 'Spanish',
    nativeName: 'Español',
    isRTL: false,
    isActive: true,
    displayOrder: 2
  },
  {
    code: 'fr',
    name: 'French',
    nativeName: 'Français',
    isRTL: false,
    isActive: true,
    displayOrder: 3
  },
  {
    code: 'de',
    name: 'German',
    nativeName: 'Deutsch',
    isRTL: false,
    isActive: true,
    displayOrder: 4
  },
  {
    code: 'hi',
    name: 'Hindi',
    nativeName: 'हिन्दी',
    isRTL: false,
    isActive: true,
    displayOrder: 5
  },
  {
    code: 'ar',
    name: 'Arabic',
    nativeName: 'العربية',
    isRTL: true,
    isActive: true,
    displayOrder: 6
  },
  {
    code: 'zh',
    name: 'Chinese',
    nativeName: '中文',
    isRTL: false,
    isActive: true,
    displayOrder: 7
  },
  {
    code: 'ja',
    name: 'Japanese',
    nativeName: '日本語',
    isRTL: false,
    isActive: true,
    displayOrder: 8
  }
];

// Sample regions data
const regionsData = [
  {
    code: 'US',
    name: 'United States',
    continent: 'North America',
    flagIcon: 'https://example.com/flags/us.png',
    isActive: true,
    displayOrder: 1,
    localization: { en: 'United States', es: 'Estados Unidos', fr: 'États-Unis' }
  },
  {
    code: 'IN',
    name: 'India',
    continent: 'Asia',
    flagIcon: 'https://example.com/flags/in.png',
    isActive: true,
    displayOrder: 2,
    localization: { en: 'India', hi: 'भारत', fr: 'Inde' }
  },
  {
    code: 'GB',
    name: 'United Kingdom',
    continent: 'Europe',
    flagIcon: 'https://example.com/flags/gb.png',
    isActive: true,
    displayOrder: 3,
    localization: { en: 'United Kingdom', fr: 'Royaume-Uni', de: 'Vereinigtes Königreich' }
  },
  {
    code: 'CA',
    name: 'Canada',
    continent: 'North America',
    flagIcon: 'https://example.com/flags/ca.png',
    isActive: true,
    displayOrder: 4,
    localization: { en: 'Canada', fr: 'Canada', es: 'Canadá' }
  },
  {
    code: 'AU',
    name: 'Australia',
    continent: 'Oceania',
    flagIcon: 'https://example.com/flags/au.png',
    isActive: true,
    displayOrder: 5,
    localization: { en: 'Australia', fr: 'Australie', de: 'Australien' }
  },
  {
    code: 'GLOBAL',
    name: 'Global',
    continent: 'Global',
    flagIcon: 'https://example.com/flags/global.png',
    isActive: true,
    displayOrder: 0,
    localization: { 
      en: 'Global', 
      es: 'Global', 
      fr: 'Mondial', 
      de: 'Global',
      hi: 'वैश्विक',
      ar: 'عالمي',
      zh: '全球',
      ja: 'グローバル'
    }
  }
];

// Function to seed language and region data
async function seedLanguagesRegions() {
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
    console.log('Clearing existing language and region data...');
    await Language.deleteMany({});
    await Region.deleteMany({});

    // Insert languages
    console.log('Inserting languages...');
    const insertedLanguages = await Language.insertMany(languagesData);
    console.log(`Inserted ${insertedLanguages.length} languages`);

    // Insert regions
    console.log('Inserting regions...');
    const insertedRegions = await Region.insertMany(regionsData);
    console.log(`Inserted ${insertedRegions.length} regions`);

    console.log('Language and region data seeded successfully!');
    
    return {
      languages: insertedLanguages,
      regions: insertedRegions
    };
  } catch (error) {
    console.error('Error seeding language and region data:', error);
    throw error;
  }
}

// If this script is run directly (not imported)
if (require.main === module) {
  seedLanguagesRegions()
    .then(() => {
      console.log('Language and region seeding completed');
      mongoose.connection.close();
    })
    .catch(err => {
      console.error('Error in language and region seeding process:', err);
      mongoose.connection.close();
    });
} else {
  // Export for use in other scripts
  module.exports = seedLanguagesRegions;
} 