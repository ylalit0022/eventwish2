/**
 * Test script to verify Template id field in API responses
 * 
 * This script simulates API endpoints to ensure they return templates
 * with proper id fields, which is critical for Android navigation.
 */

const mongoose = require('mongoose');
const Template = require('../models/Template');
const CategoryIcon = require('../models/CategoryIcon');
require('dotenv').config();

// Simulates the getTemplates controller function
async function simulateGetTemplatesEndpoint() {
  try {
    // This mimics the controller's logic
    const templates = await Template.find({ status: true })
      .populate({
        path: 'categoryIcon',
        select: '_id id category categoryIcon iconType resourceName'
      })
      .sort({ createdAt: -1 })
      .limit(5);
      
    // Prepare response similar to the actual API
    const response = {
      data: templates,
      page: 1,
      totalPages: 1,
      totalItems: templates.length,
      hasMore: false,
    };
    
    // Simulate JSON stringification that happens when API responds
    const jsonResponse = JSON.stringify(response);
    const parsedResponse = JSON.parse(jsonResponse);
    
    return parsedResponse;
  } catch (error) {
    console.error('Error simulating getTemplates endpoint:', error);
    throw error;
  }
}

// Simulates the getTemplateById controller function
async function simulateGetTemplateByIdEndpoint(templateId) {
  try {
    const template = await Template.findById(templateId)
      .populate({
        path: 'categoryIcon',
        select: '_id id category categoryIcon iconType resourceName'
      });
      
    if (!template) {
      throw new Error(`Template not found with ID: ${templateId}`);
    }
    
    // Simulate JSON stringification that happens when API responds
    const jsonResponse = JSON.stringify(template);
    const parsedTemplate = JSON.parse(jsonResponse);
    
    return parsedTemplate;
  } catch (error) {
    console.error(`Error simulating getTemplateById endpoint for ID ${templateId}:`, error);
    throw error;
  }
}

async function runApiTests() {
  try {
    // Connect to MongoDB
    console.log(`Connecting to MongoDB: ${process.env.MONGODB_URI || 'mongodb://localhost:27017/eventwish_test'}`);
    await mongoose.connect(process.env.MONGODB_URI, {
      useNewUrlParser: true,
      useUnifiedTopology: true
    });
    console.log('Connected to MongoDB');
    
    // Test 1: GetTemplates endpoint
    console.log('\n--- Testing getTemplates API simulation ---');
    const templatesResponse = await simulateGetTemplatesEndpoint();
    
    if (!templatesResponse.data || templatesResponse.data.length === 0) {
      console.log('No templates found in response');
    } else {
      console.log(`Found ${templatesResponse.data.length} templates in simulated response`);
      
      // Verify each template has id field
      const firstTemplate = templatesResponse.data[0];
      console.log('\nChecking first template in response:');
      console.log(`- Title: ${firstTemplate.title}`);
      console.log(`- Has id: ${firstTemplate.id ? '✅ Yes' : '❌ No'}`);
      
      if (firstTemplate.id) {
        console.log(`- ID type: ${typeof firstTemplate.id} (${typeof firstTemplate.id === 'string' ? '✅ Correct' : '❌ Wrong, should be string'})`);
      }
      
      // Check for _id (should not be present)
      if (firstTemplate._id) {
        console.log(`- ❌ WARNING: _id field present in API response: ${firstTemplate._id}`);
      } else {
        console.log(`- ✅ _id field correctly removed from API response`);
      }
      
      // Check categoryIcon if present
      if (firstTemplate.categoryIcon) {
        console.log('\nChecking categoryIcon in response:');
        console.log(`- CategoryIcon type: ${typeof firstTemplate.categoryIcon}`);
        
        if (typeof firstTemplate.categoryIcon === 'object') {
          console.log(`- CategoryIcon id: ${firstTemplate.categoryIcon.id || 'Missing'}`);
        } else {
          console.log(`- CategoryIcon raw value: ${firstTemplate.categoryIcon}`);
        }
      }
      
      // Save first template ID for the next test
      const templateId = firstTemplate.id;
      
      // Test 2: GetTemplateById endpoint
      console.log('\n--- Testing getTemplateById API simulation ---');
      try {
        const templateDetail = await simulateGetTemplateByIdEndpoint(templateId);
        
        console.log('Template detail response:');
        console.log(`- Title: ${templateDetail.title}`);
        console.log(`- Has id: ${templateDetail.id ? '✅ Yes' : '❌ No'}`);
        
        if (templateDetail.id) {
          console.log(`- ID type: ${typeof templateDetail.id} (${typeof templateDetail.id === 'string' ? '✅ Correct' : '❌ Wrong, should be string'})`);
          console.log(`- ID value: ${templateDetail.id}`);
          console.log(`- ID matches expected: ${templateDetail.id === templateId ? '✅ Yes' : '❌ No'}`);
        }
        
        // Check for _id (should not be present)
        if (templateDetail._id) {
          console.log(`- ❌ WARNING: _id field present in API response: ${templateDetail._id}`);
        } else {
          console.log(`- ✅ _id field correctly removed from API response`);
        }
      } catch (error) {
        console.log(`❌ Error in getTemplateById test: ${error.message}`);
      }
    }
    
    console.log('\n✅ API simulation tests completed!');
  } catch (error) {
    console.error('Error running API tests:', error);
    console.error(error.stack);
  } finally {
    await mongoose.connection.close();
    console.log('MongoDB connection closed');
  }
}

// Run the tests
runApiTests().then(() => {
  console.log('All tests completed.');
}).catch(error => {
  console.error('Tests failed:', error);
  console.error(error.stack);
  process.exit(1);
}); 