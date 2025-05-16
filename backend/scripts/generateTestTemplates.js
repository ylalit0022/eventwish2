/**
 * Simple script to generate and verify templates with category icons
 */

const mongoose = require('mongoose');
const Template = require('../models/Template');
const CategoryIcon = require('../models/CategoryIcon');
require('dotenv').config();

// Main function
async function generateTestTemplates() {
  try {
    // Connect to MongoDB
    console.log('Connecting to MongoDB...');
    await mongoose.connect(process.env.MONGODB_URI);
    console.log('Connected to MongoDB');

    // Create 5 test templates with category icons
    console.log('Creating test templates...');
    
    // Find or create a category icon
    let icon = await CategoryIcon.findOne();
    if (!icon) {
      icon = new CategoryIcon({
        category: 'Test',
        id: new mongoose.Types.ObjectId().toString(),
        categoryIcon: 'https://example.com/icon.png',
        iconType: 'URL'
      });
      await icon.save();
    }
    
    // Create templates
    for (let i = 1; i <= 5; i++) {
      const template = new Template({
        title: `Test Template ${i}`,
        category: icon.category,
        htmlContent: '<div>Test Content</div>',
        cssContent: '.test { color: blue; }',
        jsContent: '',
        status: true,
        categoryIcon: icon._id
      });
      
      await template.save();
      console.log(`Created template ${i}: ${template.title}`);
    }
    
    // Verify templates
    const templates = await Template.find()
      .limit(5)
      .populate('categoryIcon');
    
    console.log('\nVerifying templates:');
    templates.forEach(t => {
      console.log(`- ${t.title}: CategoryIcon ${t.categoryIcon ? 'exists' : 'missing'}`);
      if (t.categoryIcon) {
        console.log(`  Category: ${t.categoryIcon.category}`);
        console.log(`  Icon has _id: ${t.categoryIcon._id ? 'yes' : 'no'}`);
        console.log(`  Icon has id: ${t.categoryIcon.id ? 'yes' : 'no'}`);
      }
    });
    
    console.log('\nDone!');
  } catch (error) {
    console.error('Error:', error);
  } finally {
    await mongoose.connection.close();
  }
}

// Run the generator
generateTestTemplates(); 