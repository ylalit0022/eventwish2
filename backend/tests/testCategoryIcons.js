/**
 * Test script for CategoryIcon handling in Templates
 * 
 * This test verifies:
 * 1. Creating CategoryIcons
 * 2. Referencing CategoryIcons in Templates
 * 3. Proper population of CategoryIcon references
 * 4. Consistent handling of _id and id fields in JSON serialization
 */

const mongoose = require('mongoose');
const CategoryIcon = require('../models/CategoryIcon');
const Template = require('../models/Template');
const logger = require('../utils/logger') || console;

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

// Create a test category icon
async function createTestCategoryIcon() {
  // Delete previous test icons
  await CategoryIcon.deleteMany({ category: 'Test Category' });
  
  // Create a new test icon
  const icon = new CategoryIcon({
    id: 'test-icon-' + Date.now(),
    category: 'Test Category',
    categoryIcon: 'https://example.com/icon.png',
    iconType: 'URL',
    resourceName: ''
  });
  
  await icon.save();
  logger.info(`Created test icon with ID: ${icon._id} and custom id: ${icon.id}`);
  
  // Verify that both _id and id are accessible
  const iconJSON = icon.toJSON();
  logger.info(`Icon JSON _id: ${iconJSON._id}, id: ${iconJSON.id}`);
  
  if (!iconJSON._id || !iconJSON.id) {
    throw new Error('Icon JSON should contain both _id and id fields');
  }
  
  return icon;
}

// Create a test template with a category icon reference
async function createTestTemplate(categoryIcon) {
  // Delete previous test templates
  await Template.deleteMany({ title: 'Test Template' });
  
  // Create a new test template
  const template = new Template({
    title: 'Test Template',
    category: categoryIcon.category,
    htmlContent: '<div>Test content</div>',
    cssContent: '.test { color: red; }',
    jsContent: 'console.log("test");',
    previewUrl: 'https://example.com/preview.jpg',
    categoryIcon: categoryIcon._id
  });
  
  await template.save();
  logger.info(`Created test template with ID: ${template._id} referencing icon: ${template.categoryIcon}`);
  return template;
}

// Test direct CategoryIcon model behavior
async function testCategoryIconModel() {
  logger.info('Testing CategoryIcon model behavior');
  
  // Create icon for testing
  const icon = await createTestCategoryIcon();
  
  // Fetch the icon from database
  const fetchedIcon = await CategoryIcon.findById(icon._id);
  
  // Verify basic properties
  if (fetchedIcon.category !== icon.category) {
    throw new Error(`Expected category '${icon.category}', got '${fetchedIcon.category}'`);
  }
  
  if (fetchedIcon.categoryIcon !== icon.categoryIcon) {
    throw new Error(`Expected categoryIcon URL '${icon.categoryIcon}', got '${fetchedIcon.categoryIcon}'`);
  }
  
  // Verify JSON serialization
  const iconJSON = fetchedIcon.toJSON();
  
  if (!iconJSON._id) {
    throw new Error('Icon JSON missing _id field');
  }
  
  if (!iconJSON.id) {
    throw new Error('Icon JSON missing id field');
  }
  
  if (iconJSON._id.toString() !== fetchedIcon._id.toString()) {
    throw new Error(`Icon JSON _id mismatch: ${iconJSON._id} vs ${fetchedIcon._id}`);
  }
  
  if (iconJSON.id !== fetchedIcon.id) {
    throw new Error(`Icon JSON id mismatch: ${iconJSON.id} vs ${fetchedIcon.id}`);
  }
  
  logger.info('✅ CategoryIcon model test passed');
  return fetchedIcon;
}

// Test Template with CategoryIcon reference
async function testTemplateWithCategoryIcon(icon) {
  logger.info('Testing Template with CategoryIcon reference');
  
  // Create template with icon reference
  const template = await createTestTemplate(icon);
  
  // Fetch the template with populated icon
  const fetchedTemplate = await Template.findById(template._id)
    .populate({
      path: 'categoryIcon',
      select: '_id id category categoryIcon iconType resourceName'
    });
  
  // Verify icon is properly populated
  if (!fetchedTemplate.categoryIcon) {
    throw new Error('Template categoryIcon not populated');
  }
  
  if (fetchedTemplate.categoryIcon._id.toString() !== icon._id.toString()) {
    throw new Error(`Expected icon _id '${icon._id}', got '${fetchedTemplate.categoryIcon._id}'`);
  }
  
  if (fetchedTemplate.categoryIcon.id !== icon.id) {
    throw new Error(`Expected icon id '${icon.id}', got '${fetchedTemplate.categoryIcon.id}'`);
  }
  
  // Verify JSON serialization
  const templateJSON = fetchedTemplate.toJSON();
  
  if (!templateJSON.categoryIcon) {
    throw new Error('Template JSON missing categoryIcon');
  }
  
  if (!templateJSON.categoryIcon._id) {
    throw new Error('Template JSON categoryIcon missing _id field');
  }
  
  if (!templateJSON.categoryIcon.id) {
    throw new Error('Template JSON categoryIcon missing id field');
  }
  
  if (templateJSON.categoryIcon._id.toString() !== icon._id.toString()) {
    throw new Error(`Template JSON categoryIcon _id mismatch: ${templateJSON.categoryIcon._id} vs ${icon._id}`);
  }
  
  if (templateJSON.categoryIcon.id !== icon.id) {
    throw new Error(`Template JSON categoryIcon id mismatch: ${templateJSON.categoryIcon.id} vs ${icon.id}`);
  }
  
  logger.info('✅ Template with CategoryIcon reference test passed');
  return templateJSON;
}

// Run all tests
async function runTests() {
  try {
    // Connect to database
    await connectToDatabase();
    
    // Test CategoryIcon model
    const icon = await testCategoryIconModel();
    
    // Test Template with CategoryIcon reference
    await testTemplateWithCategoryIcon(icon);
    
    logger.info('✅ All tests passed successfully!');
  } catch (error) {
    logger.error('❌ Test failed:', error);
  } finally {
    // Clean up
    logger.info('Cleaning up...');
    await Template.deleteMany({ title: 'Test Template' });
    await CategoryIcon.deleteMany({ category: 'Test Category' });
    
    // Disconnect from database
    await mongoose.disconnect();
    logger.info('MongoDB disconnected');
  }
}

// Run tests when this script is executed directly
if (require.main === module) {
  runTests();
}

module.exports = {
  runTests
}; 