const mongoose = require('mongoose');
const Language = require('../models/Language');
const Region = require('../models/Region');
const dotenv = require('dotenv');

// Load environment variables
dotenv.config();

// MongoDB connection string
const MONGODB_URI = process.env.MONGODB_URI || 'mongodb://localhost:27017/eventwish';

async function checkLanguagesAndRegions() {
  try {
    // Connect to MongoDB
    if (mongoose.connection.readyState !== 1) {
      console.log('Connecting to MongoDB...');
      await mongoose.connect(MONGODB_URI);
      console.log('Connected to MongoDB');
    } else {
      console.log('Already connected to MongoDB');
    }

    // Count and fetch languages
    const languageCount = await Language.countDocuments();
    console.log(`\n=== LANGUAGES ===`);
    console.log(`Total languages in database: ${languageCount}`);
    
    if (languageCount > 0) {
      const languages = await Language.find().sort({ displayOrder: 1 });
      console.log('\nLanguage list:');
      languages.forEach(lang => {
        console.log(`- ${lang.code}: ${lang.name} (${lang.nativeName}) ${lang.isRTL ? 'RTL' : 'LTR'} - ${lang.isActive ? 'Active' : 'Inactive'}`);
      });
    }

    // Count and fetch regions
    const regionCount = await Region.countDocuments();
    console.log(`\n=== REGIONS ===`);
    console.log(`Total regions in database: ${regionCount}`);
    
    if (regionCount > 0) {
      const regions = await Region.find().lean().sort({ displayOrder: 1 });
      console.log('\nRegion list:');
      regions.forEach(region => {
        console.log(`- ${region.code}: ${region.name} (${region.continent}) - ${region.isActive ? 'Active' : 'Inactive'}`);
        if (region.localization) {
          const localizations = [];
          for (const [lang, name] of Object.entries(region.localization)) {
            localizations.push(`${lang}: ${name}`);
          }
          if (localizations.length > 0) {
            console.log(`  Localized names: ${localizations.join(', ')}`);
          }
        }
      });
    }

  } catch (error) {
    console.error('Error checking language and region data:', error);
  } finally {
    // Close MongoDB connection
    await mongoose.connection.close();
    console.log('\nMongoDB connection closed');
  }
}

// Run the check function
checkLanguagesAndRegions(); 