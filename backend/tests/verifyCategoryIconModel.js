/**
 * Verification script for CategoryIcon and Template models
 * 
 * This script verifies:
 * 1. The structure of CategoryIcon and Template models
 * 2. The toJSON transforms for preserving both _id and id fields
 * 3. The proper population of CategoryIcon references in Templates
 * 
 * No database connection is required.
 */

const CategoryIcon = require('../models/CategoryIcon');
const Template = require('../models/Template');
const mongoose = require('mongoose');

// Mock save methods to avoid database calls
CategoryIcon.prototype.save = function() {
  console.log(`Mock save called for CategoryIcon ${this._id}`);
  return Promise.resolve(this);
};

Template.prototype.save = function() {
  console.log(`Mock save called for Template ${this._id}`);
  return Promise.resolve(this);
};

// Create a test CategoryIcon instance
function createTestIcon() {
  const iconId = new mongoose.Types.ObjectId();
  
  const icon = new CategoryIcon({
    _id: iconId,
    id: `test-icon-${Date.now()}`,
    category: 'Test Category',
    categoryIcon: 'https://example.com/icon.png',
    iconType: 'URL',
    resourceName: ''
  });
  
  console.log('Created test icon instance:', icon._id.toString(), 'with custom id:', icon.id);
  return icon;
}

// Create a test Template instance with CategoryIcon reference
function createTestTemplate(categoryIcon) {
  const templateId = new mongoose.Types.ObjectId();
  
  const template = new Template({
    _id: templateId,
    title: 'Test Template',
    category: categoryIcon.category,
    htmlContent: '<div>Test content</div>',
    cssContent: '.test { color: red; }',
    jsContent: 'console.log("test");',
    previewUrl: 'https://example.com/preview.jpg',
    categoryIcon: categoryIcon._id
  });
  
  console.log('Created test template instance:', template._id.toString());
  return template;
}

// Manually "populate" the categoryIcon reference in a template
function populateTemplate(template, categoryIcon) {
  // Create a clone of the template to simulate Mongoose's populate behavior
  const populatedTemplate = template.toObject();
  
  // Replace the categoryIcon ObjectId with the actual CategoryIcon document
  populatedTemplate.categoryIcon = categoryIcon;
  
  // Create a new Template instance with the populated data
  // This isn't a perfect simulation of Mongoose populate, but works for our verification
  const result = new Template(populatedTemplate);
  
  // Set a flag to indicate this is a populated instance (not a real Mongoose feature, just for our test)
  result._populated = true;
  
  return result;
}

// Test CategoryIcon model
function testCategoryIcon() {
  console.log('\n=== Testing CategoryIcon model ===');
  const icon = createTestIcon();
  
  // Test basic properties
  console.log('Icon category:', icon.category);
  console.log('Icon URL:', icon.categoryIcon);
  
  // Test JSON serialization
  const iconJSON = icon.toJSON();
  console.log('Icon JSON:', {
    _id: iconJSON._id,
    id: iconJSON.id,
    category: iconJSON.category,
    iconType: iconJSON.iconType
  });
  
  // Verify _id and id are both present
  console.assert(iconJSON._id, 'Icon JSON should have _id field');
  console.assert(iconJSON.id, 'Icon JSON should have id field');
  
  // Check that both _id and id are strings in the JSON
  console.assert(typeof iconJSON._id === 'string', '_id should be a string in JSON');
  console.assert(typeof iconJSON.id === 'string', 'id should be a string in JSON');
  
  console.log('✅ CategoryIcon model test passed');
  return icon;
}

// Test Template model with CategoryIcon reference
function testTemplateWithIcon() {
  console.log('\n=== Testing Template with CategoryIcon reference ===');
  
  // Create test icon
  const icon = createTestIcon();
  
  // Create test template with reference to icon
  const template = createTestTemplate(icon);
  
  // Verify basic properties
  console.log('Template title:', template.title);
  console.log('Template category:', template.category);
  console.log('Template categoryIcon reference:', template.categoryIcon.toString());
  
  // Verify the reference is an ObjectId
  console.assert(template.categoryIcon instanceof mongoose.Types.ObjectId, 
                'Template categoryIcon should be an ObjectId');
  
  // Manually "populate" the template
  const populatedTemplate = populateTemplate(template, icon);
  
  // Verify populated reference
  console.log('Populated template categoryIcon:', populatedTemplate.categoryIcon.category);
  console.assert(populatedTemplate.categoryIcon instanceof mongoose.Document, 
                'Populated categoryIcon should be a Document');
  
  // Test JSON serialization
  const templateJSON = populatedTemplate.toJSON();
  console.log('Template JSON:', {
    id: templateJSON.id,
    title: templateJSON.title,
    category: templateJSON.category,
    categoryIcon: {
      _id: templateJSON.categoryIcon._id,
      id: templateJSON.categoryIcon.id,
      category: templateJSON.categoryIcon.category
    }
  });
  
  // Verify categoryIcon is properly included in JSON
  console.assert(templateJSON.categoryIcon, 'Template JSON should include categoryIcon');
  console.assert(templateJSON.categoryIcon._id, 'CategoryIcon in Template JSON should have _id field');
  console.assert(templateJSON.categoryIcon.id, 'CategoryIcon in Template JSON should have id field');
  
  console.log('✅ Template with CategoryIcon reference test passed');
  return { template, populatedTemplate, icon };
}

// Run all verification tests
async function runVerification() {
  try {
    console.log('Starting CategoryIcon and Template model verification...\n');
    
    // Test CategoryIcon model
    const icon = testCategoryIcon();
    
    // Test Template with CategoryIcon reference
    testTemplateWithIcon();
    
    console.log('\n✅ All verification tests passed successfully!');
  } catch (error) {
    console.error('❌ Verification failed:', error);
  }
}

// Run verification when script is executed directly
if (require.main === module) {
  runVerification();
}

module.exports = {
  runVerification
}; 