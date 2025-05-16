/**
 * Script to insert sample templates with category icons
 * 
 * This script:
 * 1. Creates sample category icons
 * 2. Creates sample templates that reference the icons
 * 3. Verifies the references are working correctly
 */

const mongoose = require('mongoose');
const CategoryIcon = require('../models/CategoryIcon');
const Template = require('../models/Template');
const logger = require('../utils/logger') || console;

// Sample category icons data
const sampleIcons = [
  {
    id: 'birthday-icon',
    category: 'Birthday',
    categoryIcon: 'https://cdn-icons-png.flaticon.com/512/3159/3159408.png',
    iconType: 'URL'
  },
  {
    id: 'wedding-icon',
    category: 'Wedding',
    categoryIcon: 'https://cdn-icons-png.flaticon.com/512/4290/4290854.png',
    iconType: 'URL'
  },
  {
    id: 'holiday-icon',
    category: 'Holiday',
    categoryIcon: 'https://cdn-icons-png.flaticon.com/512/3393/3393035.png',
    iconType: 'URL'
  },
  {
    id: 'graduation-icon',
    category: 'Graduation',
    categoryIcon: 'https://cdn-icons-png.flaticon.com/512/2995/2995459.png',
    iconType: 'URL'
  },
  {
    id: 'anniversary-icon',
    category: 'Anniversary',
    categoryIcon: 'https://cdn-icons-png.flaticon.com/512/3198/3198362.png',
    iconType: 'URL'
  }
];

// Sample template data (without categoryIcon references - will add later)
const sampleTemplates = [
  {
    title: 'Birthday Celebration',
    category: 'Birthday',
    htmlContent: '<div class="birthday-card"><h1>Happy Birthday!</h1><p>Wishing you a fantastic birthday!</p></div>',
    cssContent: '.birthday-card { color: #ff6b6b; text-align: center; }',
    jsContent: 'console.log("Birthday template loaded");',
    previewUrl: 'https://example.com/previews/birthday1.jpg'
  },
  {
    title: 'Wedding Invitation',
    category: 'Wedding',
    htmlContent: '<div class="wedding-invite"><h1>You\'re Invited!</h1><p>Please join us for our special day.</p></div>',
    cssContent: '.wedding-invite { color: #5e60ce; text-align: center; }',
    jsContent: 'console.log("Wedding template loaded");',
    previewUrl: 'https://example.com/previews/wedding1.jpg'
  },
  {
    title: 'Holiday Greetings',
    category: 'Holiday',
    htmlContent: '<div class="holiday-card"><h1>Season\'s Greetings!</h1><p>Wishing you joy and happiness.</p></div>',
    cssContent: '.holiday-card { color: #43aa8b; text-align: center; }',
    jsContent: 'console.log("Holiday template loaded");',
    previewUrl: 'https://example.com/previews/holiday1.jpg'
  },
  {
    title: 'Graduation Announcement',
    category: 'Graduation',
    htmlContent: '<div class="grad-card"><h1>I Did It!</h1><p>Proud to announce my graduation.</p></div>',
    cssContent: '.grad-card { color: #277da1; text-align: center; }',
    jsContent: 'console.log("Graduation template loaded");',
    previewUrl: 'https://example.com/previews/graduation1.jpg'
  },
  {
    title: 'Anniversary Celebration',
    category: 'Anniversary',
    htmlContent: '<div class="anniv-card"><h1>Happy Anniversary!</h1><p>Celebrating our special day.</p></div>',
    cssContent: '.anniv-card { color: #f9c74f; text-align: center; }',
    jsContent: 'console.log("Anniversary template loaded");',
    previewUrl: 'https://example.com/previews/anniversary1.jpg'
  }
];

// Connect to the test database
async function connectToDatabase() {
  try {
    const connectionString = process.env.MONGODB_URI || 'mongodb://localhost:27017/eventwish_test';
    logger.info(`Connecting to MongoDB: ${connectionString}`);
    await mongoose.connect(connectionString);
    logger.info('MongoDB connected');
  } catch (err) {
    logger.error('MongoDB connection error:', err);
    process.exit(1);
  }
}

// Create sample category icons
async function createCategoryIcons() {
  logger.info('Creating sample category icons...');
  
  const iconPromises = sampleIcons.map(async (iconData) => {
    try {
      // Check if icon already exists
      const existingIcon = await CategoryIcon.findOne({ id: iconData.id });
      if (existingIcon) {
        logger.info(`Icon already exists: ${iconData.id}`);
        return existingIcon;
      }
      
      // Create new icon
      const icon = new CategoryIcon(iconData);
      await icon.save();
      logger.info(`Created icon: ${icon.id} (${icon._id}) for category: ${icon.category}`);
      return icon;
    } catch (error) {
      logger.error(`Error creating icon ${iconData.id}:`, error);
      throw error;
    }
  });
  
  return Promise.all(iconPromises);
}

// Create sample templates
async function createTemplates(icons) {
  logger.info('Creating sample templates...');
  
  // Create a map of category to icon for easy lookup
  const categoryIconMap = {};
  icons.forEach(icon => {
    categoryIconMap[icon.category] = icon;
  });
  
  const templatePromises = sampleTemplates.map(async (templateData) => {
    try {
      // Check if template already exists
      const existingTemplate = await Template.findOne({ title: templateData.title });
      if (existingTemplate) {
        logger.info(`Template already exists: ${templateData.title}`);
        return existingTemplate;
      }
      
      // Add categoryIcon reference
      const categoryIcon = categoryIconMap[templateData.category];
      if (categoryIcon) {
        templateData.categoryIcon = categoryIcon._id;
      }
      
      // Create new template
      const template = new Template(templateData);
      await template.save();
      logger.info(`Created template: ${template.title} (${template._id}) with categoryIcon: ${template.categoryIcon}`);
      return template;
    } catch (error) {
      logger.error(`Error creating template ${templateData.title}:`, error);
      throw error;
    }
  });
  
  return Promise.all(templatePromises);
}

// Verify references by fetching templates with populated categoryIcon
async function verifyReferences() {
  logger.info('Verifying references...');
  
  // Get all templates with populated categoryIcon
  const templates = await Template.find()
    .populate({
      path: 'categoryIcon',
      select: '_id id category categoryIcon iconType resourceName'
    });
  
  templates.forEach(template => {
    const hasIcon = template.categoryIcon !== null && template.categoryIcon !== undefined;
    logger.info(`Template: ${template.title} (${template._id}) - Has icon: ${hasIcon}`);
    
    if (hasIcon) {
      logger.info(`  Icon: ${template.categoryIcon.category} (${template.categoryIcon._id})`);
      logger.info(`  Icon URL: ${template.categoryIcon.categoryIcon}`);
      
      // Verify _id and id are both present in the icon
      const templateJSON = template.toJSON();
      const iconJSON = templateJSON.categoryIcon;
      
      if (!iconJSON._id) {
        logger.error(`  ERROR: Icon JSON missing _id field`);
      }
      
      if (!iconJSON.id) {
        logger.error(`  ERROR: Icon JSON missing id field`);
      }
    } else {
      logger.warn(`  WARNING: No icon found for template ${template.title}`);
    }
  });
  
  return templates;
}

// Run the insertion script
async function run() {
  try {
    // Connect to database
    await connectToDatabase();
    
    // Create sample category icons
    const icons = await createCategoryIcons();
    logger.info(`Created ${icons.length} category icons`);
    
    // Create sample templates with references to icons
    const templates = await createTemplates(icons);
    logger.info(`Created ${templates.length} templates`);
    
    // Verify references
    await verifyReferences();
    
    logger.info('✅ Sample data insertion completed successfully!');
  } catch (error) {
    logger.error('❌ Error inserting sample data:', error);
  } finally {
    // Disconnect from database
    await mongoose.disconnect();
    logger.info('MongoDB disconnected');
  }
}

// Run the script when executed directly
if (require.main === module) {
  run();
}

module.exports = {
  run
}; 