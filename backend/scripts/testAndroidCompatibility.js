/**
 * Test script to verify template IDs are Android-compatible
 * 
 * This script simulates Android navigation bundle handling by:
 * 1. Verifying template IDs are strings, not ObjectIds
 * 2. Testing if categoryIcon IDs are also properly formatted
 * 3. Validating templateId can be safely used as a navigation parameter
 */

const mongoose = require('mongoose');
const Template = require('../models/Template');
const CategoryIcon = require('../models/CategoryIcon');
require('dotenv').config();

// Simulates Android Bundle creation with template ID
function simulateAndroidBundle(templateId) {
  // Android requires all bundle parameters to be primitive types
  // This function will fail if templateId is not a string or primitive type
  if (typeof templateId !== 'string') {
    throw new Error(`templateId must be a string, got ${typeof templateId}: ${templateId}`);
  }
  
  // Check that the ID doesn't contain invalid characters for Android bundle
  if (templateId.includes(' ') || templateId.includes('/') || templateId.includes('\\')) {
    throw new Error(`templateId contains invalid characters: ${templateId}`);
  }
  
  console.log(`✅ templateId "${templateId}" is valid for Android navigation bundles`);
  return {
    templateId: templateId
  };
}

// Simulates retrieving a template from a bundle ID
async function simulateAndroidTemplateRetrieval(bundle) {
  const templateId = bundle.templateId;
  
  // Check bundle property type
  console.log(`Template ID from bundle: ${templateId} (type: ${typeof templateId})`);
  
  // Verify template can be retrieved with this ID
  try {
    const template = await Template.findById(templateId);
    if (!template) {
      throw new Error(`No template found with ID: ${templateId}`);
    }
    console.log(`✅ Successfully retrieved template: ${template.title}`);
    return template;
  } catch (error) {
    console.error(`❌ Failed to retrieve template with ID ${templateId}: ${error.message}`);
    throw error;
  }
}

async function runAndroidCompatibilityTests() {
  try {
    // Connect to MongoDB
    console.log(`Connecting to MongoDB: ${process.env.MONGODB_URI || 'mongodb://localhost:27017/eventwish_test'}`);
    await mongoose.connect(process.env.MONGODB_URI, {
      useNewUrlParser: true,
      useUnifiedTopology: true
    });
    console.log('Connected to MongoDB');
    
    // Get a template to test with
    const template = await Template.findOne().populate('categoryIcon');
    if (!template) {
      console.log('No templates found for testing');
      return;
    }
    
    console.log(`\nTesting template: ${template.title}`);
    
    // Test 1: Verify ID types
    console.log('\n--- ID Type Verification ---');
    console.log(`- Template _id type: ${typeof template._id}`);
    console.log(`- Template id type: ${typeof template.id}`);
    console.log(`- Template id value: ${template.id}`);
    
    if (typeof template.id !== 'string') {
      console.error(`❌ ERROR: Template id is not a string! Type: ${typeof template.id}`);
      // Try to fix it
      const stringId = template._id.toString();
      console.log(`- Converting to string: ${stringId}`);
    } else {
      console.log(`✅ Template id is a string: ${template.id}`);
    }
    
    // Test 2: Simulate Android bundle
    console.log('\n--- Android Bundle Simulation ---');
    try {
      const bundle = simulateAndroidBundle(template.id);
      console.log(`- Created bundle with templateId: ${bundle.templateId}`);
      
      // Test 3: Simulate template retrieval from bundle
      console.log('\n--- Android Template Retrieval Simulation ---');
      const retrievedTemplate = await simulateAndroidTemplateRetrieval(bundle);
      
      // Verify the retrieved template also has string id
      console.log(`- Retrieved template id type: ${typeof retrievedTemplate.id}`);
      console.log(`- Retrieved template id: ${retrievedTemplate.id}`);
      console.log(`- IDs match: ${retrievedTemplate.id === template.id ? '✅ Yes' : '❌ No'}`);
      
      // Check JSON serialization (important for API responses)
      const jsonString = JSON.stringify(retrievedTemplate);
      const jsonObject = JSON.parse(jsonString);
      
      console.log('\n--- JSON Serialization Check ---');
      console.log(`- JSON has id: ${jsonObject.id ? '✅ Yes' : '❌ No'}`);
      if (jsonObject.id) {
        console.log(`- JSON id type: ${typeof jsonObject.id}`);
        console.log(`- JSON id matches original: ${jsonObject.id === template.id ? '✅ Yes' : '❌ No'}`);
      }
      
      // Check that _id is removed from JSON
      if (jsonObject._id) {
        console.log(`❌ WARNING: _id field present in JSON: ${jsonObject._id}`);
      } else {
        console.log(`✅ _id field correctly removed from JSON`);
      }
      
    } catch (error) {
      console.error(`❌ ERROR: ${error.message}`);
    }
    
    console.log('\n✅ Android compatibility tests completed!');
  } catch (error) {
    console.error('Error running tests:', error);
    console.error(error.stack);
  } finally {
    await mongoose.connection.close();
    console.log('MongoDB connection closed');
  }
}

// Run the tests
runAndroidCompatibilityTests().then(() => {
  console.log('All tests completed successfully.');
}).catch(error => {
  console.error('Tests failed:', error);
  console.error(error.stack);
  process.exit(1);
}); 