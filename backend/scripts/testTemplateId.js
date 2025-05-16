/**
 * Test script to verify Template id field consistency in API responses
 * 
 * This script verifies:
 * 1. Templates have both _id and id fields
 * 2. The id field is a string, not an ObjectId
 * 3. The id field matches the _id field (as a string)
 * 4. Template serializes correctly via JSON
 */

const mongoose = require('mongoose');
const Template = require('../models/Template');
const CategoryIcon = require('../models/CategoryIcon');
require('dotenv').config();

async function testTemplateId() {
  try {
    // Connect to MongoDB
    console.log(`Connecting to MongoDB: ${process.env.MONGODB_URI || 'mongodb://localhost:27017/eventwish_test'}`);
    await mongoose.connect(process.env.MONGODB_URI, {
      useNewUrlParser: true,
      useUnifiedTopology: true
    });
    console.log('Connected to MongoDB');

    // Find templates to test
    const templates = await Template.find().limit(5).populate('categoryIcon');
    
    if (templates.length === 0) {
      console.log('No templates found. Please ensure the database contains templates.');
      return;
    }
    
    console.log(`Found ${templates.length} templates for testing`);
    
    // Test each template
    for (let i = 0; i < templates.length; i++) {
      const template = templates[i];
      console.log(`\nTesting template ${i+1}: ${template.title}`);
      
      // Test 1: Check if _id and id both exist
      console.log(`- Has _id: ${template._id ? '✅ Yes' : '❌ No'}`);
      console.log(`- Has id: ${template.id ? '✅ Yes' : '❌ No'}`);
      
      if (!template._id || !template.id) {
        console.error('❌ ERROR: Template is missing _id or id field');
        continue;
      }
      
      // Test 2: Check if id is a string
      const idType = typeof template.id;
      console.log(`- id type: ${idType} (${idType === 'string' ? '✅ Correct' : '❌ Wrong, should be string'})`);
      
      // Test 3: Check if id matches _id
      const idMatches = template.id === template._id.toString();
      console.log(`- id matches _id: ${idMatches ? '✅ Yes' : '❌ No'}`);
      
      if (!idMatches) {
        console.log(`  _id: ${template._id.toString()}`);
        console.log(`  id:  ${template.id}`);
      }
      
      // Test 4: Test JSON serialization
      const jsonStr = JSON.stringify(template);
      const jsonObj = JSON.parse(jsonStr);
      
      console.log('- Testing JSON serialization:');
      console.log(`  * Has id in JSON: ${jsonObj.id ? '✅ Yes' : '❌ No'}`);
      
      if (jsonObj.id) {
        const jsonIdType = typeof jsonObj.id;
        console.log(`  * JSON id type: ${jsonIdType} (${jsonIdType === 'string' ? '✅ Correct' : '❌ Wrong, should be string'})`);
      }
      
      // Check if _id exists in JSON (should not be there, as toJSON transform removes it)
      if (jsonObj._id) {
        console.log(`  * ❌ WARNING: _id field is present in JSON output (should be removed in toJSON transform)`);
      } else {
        console.log(`  * ✅ _id field is correctly removed in JSON output`);
      }
      
      // Test 5: Simulate API response by converting to plain object
      console.log('- Testing API response simulation:');
      const responseObject = template.toObject ? template.toObject() : template;
      console.log(`  * Has id in object: ${responseObject.id ? '✅ Yes' : '❌ No'}`);
      
      if (responseObject.id) {
        const responseIdType = typeof responseObject.id;
        console.log(`  * Object id type: ${responseIdType} (${responseIdType === 'string' ? '✅ Correct' : '❌ Wrong, should be string'})`);
      }
      
      // Test 6: Verify CategoryIcon relation if present
      if (template.categoryIcon) {
        console.log('- Testing CategoryIcon reference:');
        const iconType = typeof template.categoryIcon;
        console.log(`  * CategoryIcon type: ${iconType}`);
        
        if (iconType === 'object') {
          // Check if the categoryIcon has an id
          console.log(`  * CategoryIcon has id: ${template.categoryIcon.id ? '✅ Yes' : '❌ No'}`);
          
          // Check if the JSON output preserves the CategoryIcon
          if (jsonObj.categoryIcon) {
            console.log(`  * CategoryIcon preserved in JSON: ✅ Yes`);
            console.log(`  * CategoryIcon id in JSON: ${jsonObj.categoryIcon.id || 'Missing'}`);
          } else {
            console.log(`  * CategoryIcon preserved in JSON: ❌ No`);
          }
        }
      }
    }
    
    console.log('\nAll template id tests completed!');
  } catch (error) {
    console.error('Error during testing:', error);
    console.error(error.stack);
  } finally {
    await mongoose.connection.close();
    console.log('MongoDB connection closed');
  }
}

// Run the test
testTemplateId().then(() => {
  console.log('Test script completed.');
}).catch(error => {
  console.error('Test script failed:', error);
  console.error(error.stack);
  process.exit(1);
}); 