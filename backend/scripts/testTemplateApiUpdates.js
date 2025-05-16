/**
 * Test script to verify Template field updates via API
 * 
 * This script simulates API requests to:
 * 1. Create a template via API call
 * 2. Update template fields via API call
 * 3. Verify changes persist in the database
 */

const mongoose = require('mongoose');
const Template = require('../models/Template');
const CategoryIcon = require('../models/CategoryIcon');
require('dotenv').config();

// Generate unique test title
const testTitle = `API Test Template ${Date.now()}`;

// Simulate the createTemplate controller function
async function simulateCreateTemplateEndpoint(templateData) {
  try {
    const template = new Template(templateData);
    await template.save();
    
    // Return JSON response like an API would
    return {
      success: true,
      message: 'Template created successfully',
      data: JSON.parse(JSON.stringify(template)) // Serialize like API response
    };
  } catch (error) {
    console.error('Error creating template:', error);
    return {
      success: false,
      message: error.message
    };
  }
}

// Simulate the updateTemplate controller function
async function simulateUpdateTemplateEndpoint(templateId, updateData) {
  try {
    // Find template by ID
    const template = await Template.findById(templateId);
    if (!template) {
      return {
        success: false,
        message: `Template not found with ID: ${templateId}`
      };
    }
    
    // Update template fields
    Object.keys(updateData).forEach(key => {
      template[key] = updateData[key];
    });
    
    // Save updates
    await template.save();
    
    // Return JSON response
    return {
      success: true,
      message: 'Template updated successfully',
      data: JSON.parse(JSON.stringify(template)) // Serialize like API response
    };
  } catch (error) {
    console.error('Error updating template:', error);
    return {
      success: false,
      message: error.message
    };
  }
}

// Simulate the getTemplate controller function
async function simulateGetTemplateEndpoint(templateId) {
  try {
    const template = await Template.findById(templateId).populate('categoryIcon');
    if (!template) {
      return {
        success: false,
        message: `Template not found with ID: ${templateId}`
      };
    }
    
    // Return API-like response
    return {
      success: true,
      data: JSON.parse(JSON.stringify(template))
    };
  } catch (error) {
    console.error('Error retrieving template:', error);
    return {
      success: false,
      message: error.message
    };
  }
}

async function testTemplateApiUpdates() {
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
    
    // Test 1: Create template via API
    console.log('\n--- Test 1: Create template via API ---');
    const templateData = {
      title: testTitle,
      category: 'API Test Category',
      htmlContent: '<div>API Test Content</div>',
      cssContent: '.api-test { color: blue; }',
      jsContent: 'console.log("API Test");',
      previewUrl: 'https://example.com/api-test.png',
      status: true,
      categoryIcon: icon._id
    };
    
    const createResponse = await simulateCreateTemplateEndpoint(templateData);
    if (!createResponse.success) {
      console.error(`❌ Create template failed: ${createResponse.message}`);
      return;
    }
    
    console.log(`✅ Template created via API: ${createResponse.data.title}`);
    console.log(`- Template ID: ${createResponse.data.id}`);
    
    const templateId = createResponse.data.id;
    
    // Verify in database
    const createdTemplate = await Template.findById(templateId);
    console.log('Verifying created template:');
    console.log(`- Title in database: ${createdTemplate.title}`);
    console.log(`- Title matches: ${createdTemplate.title === testTitle ? '✅ Yes' : '❌ No'}`);
    
    // Test 2: Update template via API
    console.log('\n--- Test 2: Update template via API ---');
    const updatedTitle = `${testTitle} Updated`;
    const updatedHtml = '<div>Updated API Content</div>';
    
    const updateData = {
      title: updatedTitle,
      htmlContent: updatedHtml,
      status: false // Also change status
    };
    
    const updateResponse = await simulateUpdateTemplateEndpoint(templateId, updateData);
    if (!updateResponse.success) {
      console.error(`❌ Update template failed: ${updateResponse.message}`);
      return;
    }
    
    console.log(`✅ Template updated via API: ${updateResponse.data.title}`);
    
    // Test 3: Verify updates in database
    console.log('\n--- Test 3: Verify database updates ---');
    const updatedTemplate = await Template.findById(templateId);
    
    console.log('Database version:');
    console.log(`- Title: ${updatedTemplate.title}`);
    console.log(`- HTML: ${updatedTemplate.htmlContent.substring(0, 30)}...`);
    console.log(`- Status: ${updatedTemplate.status}`);
    
    console.log('Verification:');
    console.log(`- Title updated: ${updatedTemplate.title === updatedTitle ? '✅ Yes' : '❌ No'}`);
    console.log(`- HTML updated: ${updatedTemplate.htmlContent === updatedHtml ? '✅ Yes' : '❌ No'}`);
    console.log(`- Status updated: ${updatedTemplate.status === false ? '✅ Yes' : '❌ No'}`);
    
    // Test 4: Use GET endpoint to verify serialization
    console.log('\n--- Test 4: Verify API GET response ---');
    const getResponse = await simulateGetTemplateEndpoint(templateId);
    
    if (!getResponse.success) {
      console.error(`❌ Get template failed: ${getResponse.message}`);
    } else {
      console.log('API GET response:');
      console.log(`- Title: ${getResponse.data.title}`);
      console.log(`- HTML: ${getResponse.data.htmlContent.substring(0, 30)}...`);
      console.log(`- Status: ${getResponse.data.status}`);
      
      console.log('Response verification:');
      console.log(`- Has id field: ${getResponse.data.id ? '✅ Yes' : '❌ No'}`);
      console.log(`- No _id field: ${getResponse.data._id ? '❌ Yes (should be removed)' : '✅ No (correctly removed)'}`);
      console.log(`- Title matches update: ${getResponse.data.title === updatedTitle ? '✅ Yes' : '❌ No'}`);
      console.log(`- HTML matches update: ${getResponse.data.htmlContent === updatedHtml ? '✅ Yes' : '❌ No'}`);
      console.log(`- Status matches update: ${getResponse.data.status === false ? '✅ Yes' : '❌ No'}`);
    }
    
    // Test 5: Clean up
    console.log('\n--- Test 5: Cleaning up ---');
    await Template.deleteOne({ _id: templateId });
    console.log('✅ Test template deleted');
    
    console.log('\n✅ All template API update tests completed!');
  } catch (error) {
    console.error('Error during testing:', error);
    console.error(error.stack);
  } finally {
    await mongoose.connection.close();
    console.log('MongoDB connection closed');
  }
}

// Run the test
testTemplateApiUpdates().then(() => {
  console.log('Test script completed.');
}).catch(error => {
  console.error('Test script failed:', error);
  console.error(error.stack);
  process.exit(1);
}); 