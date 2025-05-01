/**
 * Comprehensive test script to verify recommended templates functionality
 * 
 * This script thoroughly tests:
 * 1. Getting recommended templates alongside category templates
 * 2. Ensuring template IDs are properly converted to strings in both sets
 * 3. Verifying that templates can be accessed using the IDs from recommendations
 * 4. Testing Android compatibility with both sets of templates
 */

const mongoose = require('mongoose');
const Template = require('../models/Template');
const CategoryIcon = require('../models/CategoryIcon');
require('dotenv').config();

// Simulate the actual endpoint that returns both category templates and recommended templates
async function simulateRecommendedEndpoint(category) {
  try {
    // Find templates in the specified category
    const categoryTemplates = await Template.find({ 
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
        categoryTemplates: categoryTemplates,
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

// Verify if a template ID can be used to fetch the template (simulates Android app behavior)
async function verifyTemplateId(templateId) {
  try {
    if (typeof templateId !== 'string') {
      return {
        success: false,
        error: `Invalid ID type: ${typeof templateId}, must be a string`
      };
    }
    
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

// Simulate Android navigation bundle with template ID
function simulateAndroidBundle(templateId) {
  if (typeof templateId !== 'string') {
    return {
      success: false,
      error: `templateId must be a string, got ${typeof templateId}: ${templateId}`
    };
  }
  
  // Check that the ID doesn't contain invalid characters for Android bundle
  if (templateId.includes(' ') || templateId.includes('/') || templateId.includes('\\')) {
    return {
      success: false,
      error: `templateId contains invalid characters: ${templateId}`
    };
  }
  
  return {
    success: true,
    bundle: { templateId: templateId }
  };
}

async function testComprehensively() {
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
    
    // Test with the first category
    const testCategory = categories[0];
    console.log(`\n=== Testing with category: ${testCategory} ===`);
    
    // Get response from simulated endpoint
    const response = await simulateRecommendedEndpoint(testCategory);
    if (response.error) {
      console.error(`❌ Failed to get templates: ${response.error}`);
      return;
    }
    
    const { categoryTemplates, recommendedTemplates } = response.data;
    
    console.log(`\n- Found ${categoryTemplates.length} templates in category "${testCategory}"`);
    console.log(`- Found ${recommendedTemplates.length} recommended templates from other categories`);
    
    // Verify category templates
    console.log('\n=== Verifying category templates ===');
    let categoryTemplatesValid = true;
    
    for (let i = 0; i < categoryTemplates.length; i++) {
      const template = categoryTemplates[i];
      console.log(`\nCategory Template ${i+1}: ${template.title}`);
      
      // Check ID type
      const idType = typeof template.id;
      console.log(`- ID type: ${idType} (${idType === 'string' ? '✅ string' : '❌ not string!'})`);
      
      // Check if _id is removed
      if (template._id) {
        console.log(`- ❌ _id field is present: ${template._id}`);
      } else {
        console.log(`- ✅ _id field properly removed`);
      }
      
      // Verify the ID works for fetching
      const verification = await verifyTemplateId(template.id);
      console.log(`- Can fetch with ID: ${verification.success ? '✅ Yes' : '❌ No'}`);
      
      if (!verification.success) {
        categoryTemplatesValid = false;
        console.log(`  Error: ${verification.error}`);
      }
      
      // Test Android compatibility
      const androidTest = simulateAndroidBundle(template.id);
      console.log(`- Android bundle compatible: ${androidTest.success ? '✅ Yes' : '❌ No'}`);
      
      if (!androidTest.success) {
        categoryTemplatesValid = false;
        console.log(`  Error: ${androidTest.error}`);
      }
    }
    
    // Verify recommended templates
    console.log('\n=== Verifying recommended templates ===');
    let recommendedTemplatesValid = true;
    
    for (let i = 0; i < recommendedTemplates.length; i++) {
      const template = recommendedTemplates[i];
      console.log(`\nRecommended Template ${i+1}: ${template.title}`);
      
      // Check ID type
      const idType = typeof template.id;
      console.log(`- ID type: ${idType} (${idType === 'string' ? '✅ string' : '❌ not string!'})`);
      
      // Check if _id is removed
      if (template._id) {
        console.log(`- ❌ _id field is present: ${template._id}`);
      } else {
        console.log(`- ✅ _id field properly removed`);
      }
      
      // Verify the ID works for fetching
      const verification = await verifyTemplateId(template.id);
      console.log(`- Can fetch with ID: ${verification.success ? '✅ Yes' : '❌ No'}`);
      
      if (!verification.success) {
        recommendedTemplatesValid = false;
        console.log(`  Error: ${verification.error}`);
      }
      
      // Test Android compatibility
      const androidTest = simulateAndroidBundle(template.id);
      console.log(`- Android bundle compatible: ${androidTest.success ? '✅ Yes' : '❌ No'}`);
      
      if (!androidTest.success) {
        recommendedTemplatesValid = false;
        console.log(`  Error: ${androidTest.error}`);
      }
      
      // Test categoryIcon if present
      if (template.categoryIcon) {
        console.log(`- CategoryIcon present: ✅ Yes`);
        
        if (typeof template.categoryIcon === 'object') {
          console.log(`  * CategoryIcon id present: ${template.categoryIcon.id ? '✅ Yes' : '❌ No'}`);
          console.log(`  * CategoryIcon id type: ${typeof template.categoryIcon.id}`);
        } else {
          console.log(`  * CategoryIcon is not an object, type: ${typeof template.categoryIcon}`);
        }
      }
    }
    
    // Summary
    console.log('\n=== Test Summary ===');
    console.log(`- Category templates valid: ${categoryTemplatesValid ? '✅ Yes' : '❌ No'}`);
    console.log(`- Recommended templates valid: ${recommendedTemplatesValid ? '✅ Yes' : '❌ No'}`);
    
    if (categoryTemplatesValid && recommendedTemplatesValid) {
      console.log('\n✅ All templates have proper string IDs and are Android-compatible!');
    } else {
      console.log('\n❌ Some templates have issues with IDs. See details above.');
    }
    
  } catch (error) {
    console.error('Error during testing:', error);
    console.error(error.stack);
  } finally {
    await mongoose.connection.close();
    console.log('MongoDB connection closed');
  }
}

// Run the test
testComprehensively().then(() => {
  console.log('Test script completed.');
}).catch(error => {
  console.error('Test script failed:', error);
  console.error(error.stack);
  process.exit(1);
}); 