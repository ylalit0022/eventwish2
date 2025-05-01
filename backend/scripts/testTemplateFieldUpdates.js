/**
 * Test script to verify Template fields update correctly
 * 
 * This script tests:
 * 1. Creating a new template and verifying all fields are saved
 * 2. Updating template fields and ensuring changes persist
 * 3. Verifying field types and conversions work correctly
 */

const mongoose = require('mongoose');
const Template = require('../models/Template');
const CategoryIcon = require('../models/CategoryIcon');
require('dotenv').config();

// Generate a unique test title to avoid duplicates
const testTitle = `Test Template ${Date.now()}`;

async function testTemplateFieldUpdates() {
  try {
    // Connect to MongoDB
    console.log(`Connecting to MongoDB: ${process.env.MONGODB_URI || 'mongodb://localhost:27017/eventwish_test'}`);
    await mongoose.connect(process.env.MONGODB_URI, {
      useNewUrlParser: true,
      useUnifiedTopology: true
    });
    console.log('Connected to MongoDB');
    
    // Get a category icon for reference
    const icon = await CategoryIcon.findOne();
    if (!icon) {
      console.error('❌ No category icons found. Please create one first.');
      return;
    }
    
    // Test 1: Create a new template
    console.log('\n--- Test 1: Creating new template ---');
    const template = new Template({
      title: testTitle,
      category: 'Test Category',
      htmlContent: '<div>Test content</div>',
      cssContent: '.test { color: red; }',
      jsContent: 'console.log("Test");',
      previewUrl: 'https://example.com/preview.png',
      status: true,
      categoryIcon: icon._id
    });
    
    await template.save();
    console.log(`✅ Created template: ${template.title} (ID: ${template.id})`);
    
    // Verify all fields were saved correctly
    const createdTemplate = await Template.findById(template.id).populate('categoryIcon');
    console.log('Verifying created template:');
    console.log(`- Title: ${createdTemplate.title} (expected: ${testTitle})`);
    console.log(`- HTML content length: ${createdTemplate.htmlContent.length} characters`);
    console.log(`- CSS content length: ${createdTemplate.cssContent.length} characters`);
    console.log(`- JS content length: ${createdTemplate.jsContent.length} characters`);
    console.log(`- Category icon: ${createdTemplate.categoryIcon ? '✅ Present' : '❌ Missing'}`);
    
    // Test 2: Update the template
    console.log('\n--- Test 2: Updating template ---');
    const updatedTitle = `${testTitle} Updated`;
    const updatedHtml = '<div>Updated content</div>';
    
    createdTemplate.title = updatedTitle;
    createdTemplate.htmlContent = updatedHtml;
    await createdTemplate.save();
    console.log('✅ Template updated');
    
    // Verify updates were saved
    const updatedTemplate = await Template.findById(template.id);
    console.log('Verifying updated template:');
    console.log(`- Title: ${updatedTemplate.title}`);
    console.log(`- Title updated correctly: ${updatedTemplate.title === updatedTitle ? '✅ Yes' : '❌ No'}`);
    console.log(`- HTML updated correctly: ${updatedTemplate.htmlContent === updatedHtml ? '✅ Yes' : '❌ No'}`);
    
    // Test 3: Test categoryIcon updates
    console.log('\n--- Test 3: Testing categoryIcon updates ---');
    
    // Find another icon
    const anotherIcon = await CategoryIcon.findOne({ _id: { $ne: icon._id } });
    
    if (anotherIcon) {
      // Update to a different icon
      updatedTemplate.categoryIcon = anotherIcon._id;
      await updatedTemplate.save();
      
      // Verify icon was updated
      const templateWithNewIcon = await Template.findById(template.id).populate('categoryIcon');
      
      console.log('Verifying categoryIcon update:');
      console.log(`- Original icon ID: ${icon._id}`);
      console.log(`- New icon ID: ${anotherIcon._id}`);
      console.log(`- Current template icon ID: ${templateWithNewIcon.categoryIcon._id}`);
      console.log(`- Icon updated correctly: ${templateWithNewIcon.categoryIcon._id.toString() === anotherIcon._id.toString() ? '✅ Yes' : '❌ No'}`);
    } else {
      console.log('- Skipping icon update test (need at least two icons)');
    }
    
    // Test 4: Verify JSON serialization preserves updates
    console.log('\n--- Test 4: Verifying JSON serialization ---');
    
    const jsonTemplate = JSON.parse(JSON.stringify(updatedTemplate));
    console.log('JSON serialization results:');
    console.log(`- Has id: ${jsonTemplate.id ? '✅ Yes' : '❌ No'}`);
    console.log(`- Title matched: ${jsonTemplate.title === updatedTitle ? '✅ Yes' : '❌ No'}`);
    console.log(`- HTML matched: ${jsonTemplate.htmlContent === updatedHtml ? '✅ Yes' : '❌ No'}`);
    
    // Test 5: Clean up
    console.log('\n--- Test 5: Cleaning up ---');
    await Template.deleteOne({ _id: template._id });
    console.log(`✅ Test template deleted`);
    
    // Verify deletion
    const deletedTemplate = await Template.findById(template.id);
    console.log(`- Template deleted: ${deletedTemplate ? '❌ No' : '✅ Yes'}`);
    
    console.log('\n✅ All template field update tests completed!');
  } catch (error) {
    console.error('Error during testing:', error);
    console.error(error.stack);
  } finally {
    await mongoose.connection.close();
    console.log('MongoDB connection closed');
  }
}

// Run the test
testTemplateFieldUpdates().then(() => {
  console.log('Test script completed.');
}).catch(error => {
  console.error('Test script failed:', error);
  console.error(error.stack);
  process.exit(1);
}); 