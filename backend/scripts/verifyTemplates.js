/**
 * Script to verify the templates created in the database
 * 
 * This script:
 * 1. Connects to the database
 * 2. Counts templates by category
 * 3. Retrieves sample templates to verify their structure
 */

const mongoose = require('mongoose');
const Template = require('../models/Template');
const CategoryIcon = require('../models/CategoryIcon');
require('dotenv').config();

async function verifyTemplates() {
  try {
    console.log('Connecting to MongoDB...');
    await mongoose.connect(process.env.MONGODB_URI, {
      useNewUrlParser: true,
      useUnifiedTopology: true
    });
    console.log('MongoDB connected');
    
    // Get total count
    const totalCount = await Template.countDocuments();
    console.log(`Total templates in database: ${totalCount}`);
    
    // Count by category
    const categoryCounts = await Template.aggregate([
      { $group: { _id: '$category', count: { $sum: 1 } } },
      { $sort: { count: -1 } }
    ]);
    
    console.log('\nTemplates by category:');
    categoryCounts.forEach(cat => {
      console.log(`${cat._id}: ${cat.count} templates`);
    });
    
    // Display sample templates from each category
    console.log('\nSample templates from each category:');
    
    for (const categoryCount of categoryCounts) {
      const category = categoryCount._id;
      
      // Get a random template from this category
      const templates = await Template.find({ category })
        .populate('categoryIcon')
        .limit(1);
      
      if (templates.length > 0) {
        const template = templates[0];
        console.log(`\nCategory: ${category}`);
        console.log(`Title: ${template.title}`);
        console.log(`Preview URL: ${template.previewUrl}`);
        
        if (template.categoryIcon) {
          console.log(`Icon: ${template.categoryIcon.category}`);
          console.log(`Icon URL: ${template.categoryIcon.categoryIcon}`);
        } else {
          console.log('Icon: None');
        }
        
        // Show a snippet of the HTML content
        const htmlSnippet = template.htmlContent.substring(0, 100) + '...';
        console.log(`HTML snippet: ${htmlSnippet}`);
      }
    }
    
    // Check for any invalid templates (missing required fields)
    const invalidTemplates = await Template.find({ 
      $or: [
        { title: { $exists: false } },
        { category: { $exists: false } },
        { htmlContent: { $exists: false } }
      ]
    }).limit(10);
    
    if (invalidTemplates.length > 0) {
      console.log('\nWARNING: Found invalid templates:');
      invalidTemplates.forEach(template => {
        console.log(`ID: ${template._id}, Title: ${template.title || 'MISSING'}`);
      });
    } else {
      console.log('\nNo invalid templates found');
    }
    
    // Check for proper CategoryIcon references
    const templatesWithoutIcons = await Template.find({ categoryIcon: { $exists: false } }).limit(10);
    
    if (templatesWithoutIcons.length > 0) {
      console.log('\nWARNING: Found templates without category icons:');
      templatesWithoutIcons.forEach(template => {
        console.log(`ID: ${template._id}, Title: ${template.title}, Category: ${template.category}`);
      });
    } else {
      console.log('\nAll templates have category icon references');
    }
    
  } catch (error) {
    console.error('Error verifying templates:', error);
  } finally {
    await mongoose.disconnect();
    console.log('\nMongoDB disconnected');
  }
}

// Run the verification
if (require.main === module) {
  verifyTemplates()
    .then(() => console.log('Verification completed'))
    .catch(err => console.error('Verification failed:', err));
}

module.exports = { verifyTemplates }; 