/**
 * Insert sample templates with properly resolved CategoryIcon references
 * 
 * This script will:
 * 1. Find existing CategoryIcons or create new ones
 * 2. Create sample templates with references to those icons
 * 3. Verify the population of CategoryIcon references
 */

const mongoose = require('mongoose');
const Template = require('../models/Template');
const CategoryIcon = require('../models/CategoryIcon');
require('dotenv').config();

// Sample category data with icons
const sampleCategories = [
  {
    category: 'Birthday',
    categoryIcon: 'https://cdn-icons-png.flaticon.com/512/3159/3159213.png',
    iconType: 'URL'
  },
  {
    category: 'Wedding',
    categoryIcon: 'https://cdn-icons-png.flaticon.com/512/2955/2955010.png',
    iconType: 'URL'
  },
  {
    category: 'Anniversary',
    categoryIcon: 'https://cdn-icons-png.flaticon.com/512/3656/3656949.png', 
    iconType: 'URL'
  },
  {
    category: 'Graduation',
    categoryIcon: 'https://cdn-icons-png.flaticon.com/512/3526/3526283.png',
    iconType: 'URL'
  },
  {
    category: 'Holiday',
    categoryIcon: 'https://cdn-icons-png.flaticon.com/512/4213/4213147.png',
    iconType: 'URL'
  }
];

// Sample template data
const sampleTemplates = [
  {
    title: 'Birthday Template 1',
    category: 'Birthday',
    htmlContent: '<div class="birthday-template">Happy Birthday!</div>',
    cssContent: '.birthday-template { color: blue; }',
    previewUrl: 'https://via.placeholder.com/400x300/87CEEB/000000?text=Birthday+Template',
    status: true
  },
  {
    title: 'Wedding Template 1',
    category: 'Wedding',
    htmlContent: '<div class="wedding-template">Congratulations on your wedding!</div>',
    cssContent: '.wedding-template { color: gold; }',
    previewUrl: 'https://via.placeholder.com/400x300/FAFAD2/000000?text=Wedding+Template',
    status: true
  },
  {
    title: 'Anniversary Template 1',
    category: 'Anniversary',
    htmlContent: '<div class="anniversary-template">Happy Anniversary!</div>',
    cssContent: '.anniversary-template { color: silver; }',
    previewUrl: 'https://via.placeholder.com/400x300/C0C0C0/000000?text=Anniversary+Template',
    status: true
  },
  {
    title: 'Graduation Template 1',
    category: 'Graduation',
    htmlContent: '<div class="graduation-template">Congratulations Graduate!</div>',
    cssContent: '.graduation-template { color: black; }',
    previewUrl: 'https://via.placeholder.com/400x300/000000/FFFFFF?text=Graduation+Template',
    status: true
  },
  {
    title: 'Holiday Template 1',
    category: 'Holiday',
    htmlContent: '<div class="holiday-template">Happy Holidays!</div>',
    cssContent: '.holiday-template { color: red; }',
    previewUrl: 'https://via.placeholder.com/400x300/FF0000/FFFFFF?text=Holiday+Template',
    status: true
  }
];

// Run the script
async function insertSampleData() {
  try {
    // Connect to MongoDB
    await mongoose.connect(process.env.MONGODB_URI, {
      useNewUrlParser: true,
      useUnifiedTopology: true
    });
    console.log('Connected to MongoDB');

    // Step 1: Create or find CategoryIcons
    const categoryIcons = {};
    
    for (const cat of sampleCategories) {
      // Check if this category icon already exists
      let icon = await CategoryIcon.findOne({ category: cat.category });
      
      if (!icon) {
        // Create a new icon if it doesn't exist
        console.log(`Creating new CategoryIcon for ${cat.category}`);
        icon = new CategoryIcon({
          id: new mongoose.Types.ObjectId().toString(), // Generate a string ID
          category: cat.category,
          categoryIcon: cat.categoryIcon,
          iconType: cat.iconType
        });
        
        await icon.save();
      } else {
        console.log(`Found existing CategoryIcon for ${cat.category}: ${icon._id}`);
      }
      
      // Store the icon reference to use when creating templates
      categoryIcons[cat.category] = icon;
    }
    
    // Step 2: Create Templates with CategoryIcon references
    const createdTemplates = [];
    
    for (const template of sampleTemplates) {
      const categoryIcon = categoryIcons[template.category];
      
      if (!categoryIcon) {
        console.warn(`No CategoryIcon found for category: ${template.category}, skipping template`);
        continue;
      }
      
      // Create a new template with proper CategoryIcon reference
      const newTemplate = new Template({
        title: template.title,
        category: template.category,
        htmlContent: template.htmlContent,
        cssContent: template.cssContent,
        previewUrl: template.previewUrl,
        status: template.status,
        categoryIcon: categoryIcon._id // Important: Reference the ObjectId here
      });
      
      await newTemplate.save();
      createdTemplates.push(newTemplate);
      console.log(`Created Template: ${newTemplate.title} with CategoryIcon: ${categoryIcon._id}`);
    }
    
    // Step 3: Verify the data with population
    console.log('\nVerifying templates with populated CategoryIcons:');
    
    for (const template of createdTemplates) {
      const populatedTemplate = await Template.findById(template._id)
        .populate({
          path: 'categoryIcon',
          select: '_id id category categoryIcon iconType'
        });
      
      console.log(`\nTemplate: ${populatedTemplate.title}`);
      console.log(`- Category: ${populatedTemplate.category}`);
      
      if (populatedTemplate.categoryIcon) {
        console.log('- CategoryIcon (populated):');
        console.log(`  * ID: ${populatedTemplate.categoryIcon._id}`);
        console.log(`  * Category: ${populatedTemplate.categoryIcon.category}`);
        console.log(`  * URL: ${populatedTemplate.categoryIcon.categoryIcon}`);
      } else {
        console.warn('- CategoryIcon not populated properly!');
      }
    }
    
    console.log('\nSample data insertion completed successfully');
    console.log(`Created ${createdTemplates.length} templates with proper CategoryIcon references`);
    
  } catch (error) {
    console.error('Error inserting sample data:', error);
  } finally {
    // Close MongoDB connection
    await mongoose.connection.close();
    console.log('MongoDB connection closed');
  }
}

// Execute the script
insertSampleData()
  .then(() => console.log('Script completed'))
  .catch(err => console.error('Script failed:', err)); 