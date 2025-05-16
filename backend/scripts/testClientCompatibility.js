/**
 * Test script to simulate Android client handling of templates
 * 
 * This script tests:
 * 1. Simulating Android client API requests
 * 2. Processing template data as an Android client would
 * 3. Verifying field updates are correctly received by client
 */

const mongoose = require('mongoose');
const Template = require('../models/Template');
const CategoryIcon = require('../models/CategoryIcon');
require('dotenv').config();

// Generate a unique test title
const testTitle = `Client Test Template ${Date.now()}`;

// Simulate Android client templates list request
async function simulateAndroidGetTemplates() {
  try {
    // API would normally send this list data
    const templates = await Template.find({ status: true })
      .populate('categoryIcon')
      .limit(5);
    
    // Serialize data as it would be in API response
    const apiData = JSON.parse(JSON.stringify({
      data: templates,
      page: 1,
      totalPages: 1,
      hasMore: false
    }));
    
    // Android client would receive this serialized JSON
    return apiData;
  } catch (error) {
    console.error('Error simulating Android template list request:', error);
    return { error: error.message };
  }
}

// Simulate Android client template detail request
async function simulateAndroidGetTemplate(templateId) {
  try {
    const template = await Template.findById(templateId).populate('categoryIcon');
    if (!template) {
      return { error: `Template not found with ID: ${templateId}` };
    }
    
    // Serialize as API would
    const apiData = JSON.parse(JSON.stringify(template));
    return apiData;
  } catch (error) {
    console.error(`Error simulating Android template detail request for ID ${templateId}:`, error);
    return { error: error.message };
  }
}

// Simulate Android client handling template data
function simulateAndroidProcessTemplate(templateData) {
  // This simulates how Android client would process a template
  try {
    // Verify required fields exist (Android would crash if missing)
    if (!templateData.id) {
      throw new Error('Template is missing required id field');
    }
    
    if (!templateData.title) {
      throw new Error('Template is missing required title field');
    }
    
    if (!templateData.htmlContent) {
      throw new Error('Template is missing required htmlContent field');
    }
    
    // Android client would store this data in a local object
    const androidTemplateObject = {
      id: templateData.id,
      title: templateData.title,
      category: templateData.category || '',
      htmlContent: templateData.htmlContent,
      cssContent: templateData.cssContent || '',
      jsContent: templateData.jsContent || '',
      previewUrl: templateData.previewUrl || '',
      categoryIconId: templateData.categoryIcon ? 
        (typeof templateData.categoryIcon === 'object' ? templateData.categoryIcon.id : templateData.categoryIcon) 
        : null
    };
    
    return {
      success: true,
      templateObject: androidTemplateObject
    };
  } catch (error) {
    console.error('Error processing template on Android client:', error);
    return {
      success: false,
      error: error.message
    };
  }
}

// Simulate Android template update request
async function simulateAndroidUpdateTemplate(templateId, updateData) {
  try {
    // API would receive this request
    const template = await Template.findById(templateId);
    if (!template) {
      return { success: false, error: `Template not found with ID: ${templateId}` };
    }
    
    // Update template fields
    Object.keys(updateData).forEach(key => {
      template[key] = updateData[key];
    });
    
    // Save updates
    await template.save();
    
    // Serialize for response
    const updatedData = JSON.parse(JSON.stringify(template));
    
    return {
      success: true,
      data: updatedData
    };
  } catch (error) {
    console.error(`Error simulating Android update request:`, error);
    return { success: false, error: error.message };
  }
}

async function testClientCompatibility() {
  try {
    // Connect to MongoDB
    console.log(`Connecting to MongoDB: ${process.env.MONGODB_URI || 'mongodb://localhost:27017/eventwish_test'}`);
    await mongoose.connect(process.env.MONGODB_URI, {
      useNewUrlParser: true,
      useUnifiedTopology: true
    });
    console.log('Connected to MongoDB');
    
    // Get a category icon for the test
    const icon = await CategoryIcon.findOne();
    if (!icon) {
      console.error('❌ No category icons found. Please create one first.');
      return;
    }
    
    // Test 1: Create test template
    console.log('\n--- Test 1: Create test template ---');
    const template = new Template({
      title: testTitle,
      category: 'Client Test Category',
      htmlContent: '<div>Client Test Content</div>',
      cssContent: '.client-test { color: green; }',
      jsContent: 'console.log("Client Test");',
      previewUrl: 'https://example.com/client-test.png',
      status: true,
      categoryIcon: icon._id
    });
    
    await template.save();
    console.log(`✅ Created template: ${template.title} (ID: ${template.id})`);
    
    // Test 2: Simulate Android client list request
    console.log('\n--- Test 2: Simulate Android client list request ---');
    const listResponse = await simulateAndroidGetTemplates();
    
    if (listResponse.error) {
      console.error(`❌ Template list request failed: ${listResponse.error}`);
    } else {
      console.log(`✅ Template list request succeeded`);
      console.log(`- Received ${listResponse.data.length} templates`);
      
      // Check if our test template is in the list
      const foundTemplate = listResponse.data.find(t => t.id === template.id);
      if (foundTemplate) {
        console.log('- Found our test template in the list');
        
        // Process template as Android would
        const processResult = simulateAndroidProcessTemplate(foundTemplate);
        if (processResult.success) {
          console.log('- Android client successfully processed template');
          console.log(`- Processed title: ${processResult.templateObject.title}`);
        } else {
          console.error(`❌ Android client processing failed: ${processResult.error}`);
        }
      } else {
        console.log('- Test template not found in list (may not be in first 5 results)');
      }
    }
    
    // Test 3: Simulate Android client detail request
    console.log('\n--- Test 3: Simulate Android client detail request ---');
    const detailResponse = await simulateAndroidGetTemplate(template.id);
    
    if (detailResponse.error) {
      console.error(`❌ Template detail request failed: ${detailResponse.error}`);
    } else {
      console.log(`✅ Template detail request succeeded`);
      console.log(`- Received template: ${detailResponse.title}`);
      
      // Process template as Android would
      const processResult = simulateAndroidProcessTemplate(detailResponse);
      if (processResult.success) {
        console.log('- Android client successfully processed template detail');
        console.log(`- Android template object:`);
        console.log(`  * id: ${processResult.templateObject.id}`);
        console.log(`  * title: ${processResult.templateObject.title}`);
        console.log(`  * category: ${processResult.templateObject.category}`);
        console.log(`  * categoryIconId: ${processResult.templateObject.categoryIconId}`);
      } else {
        console.error(`❌ Android client processing failed: ${processResult.error}`);
      }
      
      // Save the Android object for next tests
      const androidTemplateObject = processResult.templateObject;
      
      // Test 4: Simulate Android update request
      console.log('\n--- Test 4: Simulate Android update request ---');
      
      // Android client would update some fields
      const updatedTitle = `${testTitle} Android Updated`;
      const updateData = {
        title: updatedTitle,
        htmlContent: '<div>Updated from Android</div>'
      };
      
      const updateResponse = await simulateAndroidUpdateTemplate(template.id, updateData);
      if (!updateResponse.success) {
        console.error(`❌ Android update request failed: ${updateResponse.error}`);
      } else {
        console.log(`✅ Android update request succeeded`);
        console.log(`- Updated title: ${updateResponse.data.title}`);
        
        // Verify Android can process the updated template
        const updatedProcessResult = simulateAndroidProcessTemplate(updateResponse.data);
        if (updatedProcessResult.success) {
          console.log('- Android client successfully processed updated template');
          console.log(`- New title in Android: ${updatedProcessResult.templateObject.title}`);
          console.log(`- Title match: ${updatedProcessResult.templateObject.title === updatedTitle ? '✅ Yes' : '❌ No'}`);
        } else {
          console.error(`❌ Android client processing of updated template failed: ${updatedProcessResult.error}`);
        }
      }
      
      // Test 5: Verify updates in database
      console.log('\n--- Test 5: Verify database updates from Android ---');
      const updatedTemplate = await Template.findById(template.id);
      
      console.log('Database state after Android update:');
      console.log(`- Title: ${updatedTemplate.title}`);
      console.log(`- HTML: ${updatedTemplate.htmlContent}`);
      
      console.log('Verification:');
      console.log(`- Title matches Android update: ${updatedTemplate.title === updatedTitle ? '✅ Yes' : '❌ No'}`);
      console.log(`- HTML matches Android update: ${updatedTemplate.htmlContent === updateData.htmlContent ? '✅ Yes' : '❌ No'}`);
    }
    
    // Clean up
    console.log('\n--- Cleaning up ---');
    await Template.deleteOne({ _id: template._id });
    console.log('✅ Test template deleted');
    
    console.log('\n✅ All client compatibility tests completed!');
  } catch (error) {
    console.error('Error during testing:', error);
    console.error(error.stack);
  } finally {
    await mongoose.connection.close();
    console.log('MongoDB connection closed');
  }
}

// Run the test
testClientCompatibility().then(() => {
  console.log('Test script completed.');
}).catch(error => {
  console.error('Test script failed:', error);
  console.error(error.stack);
  process.exit(1);
}); 