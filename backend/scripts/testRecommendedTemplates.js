/**
 * Test script to verify recommended templates functionality
 * 
 * This script tests:
 * 1. Getting recommended templates based on a category
 * 2. Ensuring template IDs are properly handled in the response
 * 3. Verifying that templates can be accessed using recommended IDs
 */

const mongoose = require('mongoose');
const Template = require('../models/Template');
const CategoryIcon = require('../models/CategoryIcon');
require('dotenv').config();

// Simulate the getRecommendedTemplates controller function
async function simulateRecommendedTemplatesEndpoint(category) {
  try {
    // Find templates in the specified category
    const templates = await Template.find({ 
      category: category,
      status: true 
    })
    .populate('categoryIcon')
    .limit(5);
    
    // Find templates in other categories for recommendations
    const recommendedTemplates = await Template.find({ 
      category: { $ne: category },
      status: true 
    })
    .populate('categoryIcon')
    .limit(5);
    
    // Prepare response with both sets
    const response = {
      data: {
        categoryTemplates: templates,
        recommendedTemplates: recommendedTemplates
      }
    };
    
    // Serialize like an API response would
    return JSON.parse(JSON.stringify(response));
  } catch (error) {
    console.error('Error getting recommended templates:', error);
    return { error: error.message };
  }
}

// Verify if a template ID can be used to fetch the template
async function verifyTemplateId(templateId) {
  try {
    const template = await Template.findById(templateId);
    return {
      success: !!template,
      template: template ? JSON.parse(JSON.stringify(template)) : null
    };
  } catch (error) {
    console.error(`Error verifying template ID ${templateId}:`, error);
    return {
      success: false,
      error: error.message
    };
  }
}

async function testRecommendedTemplates() {
  try {
    // Connect to MongoDB
    console.log(`Connecting to MongoDB: ${process.env.MONGODB_URI || 'mongodb://localhost:27017/eventwish_test'}`);
    await mongoose.connect(process.env.MONGODB_URI, {
      useNewUrlParser: true,
      useUnifiedTopology: true
    });
    console.log('Connected to MongoDB');
    
    // Find existing categories for testing
    const categories = await Template.distinct('category');
    if (categories.length === 0) {
      console.error('❌ No template categories found. Please create some templates first.');
      return;
    }
    
    console.log(`Found ${categories.length} categories: ${categories.join(', ')}`);
    
    // Test 1: Get recommended templates for a specific category
    console.log('\n--- Test 1: Get recommended templates ---');
    const testCategory = categories[0];
    console.log(`Using category: ${testCategory}`);
    
    const response = await simulateRecommendedTemplatesEndpoint(testCategory);
    if (response.error) {
      console.error(`❌ Failed to get recommended templates: ${response.error}`);
      return;
    }
    
    const { categoryTemplates, recommendedTemplates } = response.data;
    
    console.log(`- Found ${categoryTemplates.length} templates in category "${testCategory}"`);
    console.log(`- Found ${recommendedTemplates.length} recommended templates`);
    
    // Test 2: Verify template IDs in both sets
    console.log('\n--- Test 2: Verify template IDs ---');
    
    // Check category templates
    console.log('Checking category templates:');
    let allCategoryIdsValid = true;
    
    for (let i = 0; i < categoryTemplates.length; i++) {
      const template = categoryTemplates[i];
      console.log(`\nTemplate ${i+1}: ${template.title}`);
      
      // Verify ID fields
      const hasId = !!template.id;
      const idType = typeof template.id;
      const hasNoMongoId = !template._id;
      
      console.log(`- Has id: ${hasId ? '✅ Yes' : '❌ No'}`);
      console.log(`- id type: ${idType} (${idType === 'string' ? '✅ Correct' : '❌ Wrong, should be string'})`);
      console.log(`- _id field removed: ${hasNoMongoId ? '✅ Yes' : '❌ No (should be removed)'}`);
      
      // Verify ID can fetch the template
      if (hasId) {
        const verification = await verifyTemplateId(template.id);
        console.log(`- ID can fetch template: ${verification.success ? '✅ Yes' : '❌ No'}`);
        
        if (!verification.success) {
          allCategoryIdsValid = false;
          console.error(`  Error: ${verification.error}`);
        }
      } else {
        allCategoryIdsValid = false;
      }
      
      // Check categoryIcon if present
      if (template.categoryIcon) {
        const iconType = typeof template.categoryIcon;
        const iconHasId = iconType === 'object' && template.categoryIcon.id;
        
        console.log(`- Has categoryIcon: ✅ Yes`);
        console.log(`- categoryIcon type: ${iconType}`);
        console.log(`- categoryIcon has id: ${iconHasId ? '✅ Yes' : '❌ No'}`);
      } else {
        console.log(`- Has categoryIcon: ❌ No`);
      }
    }
    
    // Check recommended templates
    console.log('\nChecking recommended templates:');
    let allRecommendedIdsValid = true;
    
    for (let i = 0; i < recommendedTemplates.length; i++) {
      const template = recommendedTemplates[i];
      console.log(`\nRecommended Template ${i+1}: ${template.title}`);
      
      // Verify ID fields
      const hasId = !!template.id;
      const idType = typeof template.id;
      const hasNoMongoId = !template._id;
      
      console.log(`- Has id: ${hasId ? '✅ Yes' : '❌ No'}`);
      console.log(`- id type: ${idType} (${idType === 'string' ? '✅ Correct' : '❌ Wrong, should be string'})`);
      console.log(`- _id field removed: ${hasNoMongoId ? '✅ Yes' : '❌ No (should be removed)'}`);
      
      // Verify ID can fetch the template
      if (hasId) {
        const verification = await verifyTemplateId(template.id);
        console.log(`- ID can fetch template: ${verification.success ? '✅ Yes' : '❌ No'}`);
        
        if (!verification.success) {
          allRecommendedIdsValid = false;
          console.error(`  Error: ${verification.error}`);
        }
      } else {
        allRecommendedIdsValid = false;
      }
    }
    
    // Test 3: Verify overall results
    console.log('\n--- Test 3: Overall verification ---');
    console.log(`- All category template IDs valid: ${allCategoryIdsValid ? '✅ Yes' : '❌ No'}`);
    console.log(`- All recommended template IDs valid: ${allRecommendedIdsValid ? '✅ Yes' : '❌ No'}`);
    
    // Test Android app compatibility
    console.log('\n--- Test 4: Verify Android compatibility ---');
    
    if (recommendedTemplates.length > 0) {
      const androidTest = recommendedTemplates[0];
      
      // Simulate Android app receiving the template ID
      console.log(`Android would receive template ID: ${androidTest.id}`);
      
      // Simulate Android app fetching template with the ID
      console.log('Testing if Android can fetch template with this ID:');
      
      // Verify ID is a string (Android requirement)
      if (typeof androidTest.id !== 'string') {
        console.error(`❌ Android compatibility issue: ID is not a string (${typeof androidTest.id})`);
      } else {
        console.log(`✅ ID is a string, Android compatible`);
        
        // Verify ID can be used to fetch template
        const androidVerification = await verifyTemplateId(androidTest.id);
        console.log(`- Android can fetch template: ${androidVerification.success ? '✅ Yes' : '❌ No'}`);
        
        if (androidVerification.success) {
          console.log(`- Template title: ${androidVerification.template.title}`);
        } else {
          console.error(`  Error: ${androidVerification.error}`);
        }
      }
    } else {
      console.log('Skipping Android test (no recommended templates)');
    }
    
    console.log('\n✅ All recommended templates tests completed!');
  } catch (error) {
    console.error('Error during testing:', error);
    console.error(error.stack);
  } finally {
    await mongoose.connection.close();
    console.log('MongoDB connection closed');
  }
}

// Run the test
testRecommendedTemplates().then(() => {
  console.log('Test script completed.');
}).catch(error => {
  console.error('Test script failed:', error);
  console.error(error.stack);
  process.exit(1);
}); 