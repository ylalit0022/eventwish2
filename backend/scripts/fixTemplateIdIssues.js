/**
 * Script to fix Template ID issues in the database
 * 
 * This script:
 * 1. Ensures all templates have proper string IDs
 * 2. Fixes any templates with missing or incorrect id fields
 * 3. Ensures CategoryIcon references are properly formatted
 * 4. Verifies toJSON and toObject transforms work correctly
 */

const mongoose = require('mongoose');
const Template = require('../models/Template');
const CategoryIcon = require('../models/CategoryIcon');
require('dotenv').config();

async function fixTemplateIdIssues() {
  try {
    // Connect to MongoDB
    console.log(`Connecting to MongoDB: ${process.env.MONGODB_URI || 'mongodb://localhost:27017/eventwish_test'}`);
    await mongoose.connect(process.env.MONGODB_URI, {
      useNewUrlParser: true,
      useUnifiedTopology: true
    });
    console.log('Connected to MongoDB');
    
    // Get all templates to process
    const templates = await Template.find();
    console.log(`Found ${templates.length} templates to process`);
    
    let fixedCount = 0;
    let alreadyCorrectCount = 0;
    let errorCount = 0;
    
    // Process each template
    for (let i = 0; i < templates.length; i++) {
      const template = templates[i];
      console.log(`\nProcessing template ${i+1}/${templates.length}: ${template.title}`);
      
      try {
        let needsSave = false;
        
        // Check if id field exists and is a string
        if (!template.id) {
          console.log(`- Adding missing id field`);
          template.id = template._id.toString();
          needsSave = true;
        } else if (typeof template.id !== 'string') {
          console.log(`- Converting id from ${typeof template.id} to string`);
          template.id = template._id.toString();
          needsSave = true;
        } else if (template.id !== template._id.toString()) {
          console.log(`- Fixing incorrect id value (${template.id} vs ${template._id.toString()})`);
          template.id = template._id.toString();
          needsSave = true;
        } else {
          console.log(`- id field is already correct: ${template.id}`);
        }
        
        // Test toJSON transform
        const jsonObj = JSON.parse(JSON.stringify(template));
        if (!jsonObj.id) {
          console.log(`- Problem with toJSON transform: missing id field`);
          // The issue is likely in the model definition, not this specific instance
        } else if (typeof jsonObj.id !== 'string') {
          console.log(`- Problem with toJSON transform: id is not a string (${typeof jsonObj.id})`);
          // The issue is likely in the model definition, not this specific instance
        } else if (jsonObj._id) {
          console.log(`- Problem with toJSON transform: _id field not removed`);
          // The issue is likely in the model definition, not this specific instance
        } else {
          console.log(`- toJSON transform works correctly`);
        }
        
        // Test toObject transform if available
        if (template.toObject) {
          const objResult = template.toObject();
          if (!objResult.id) {
            console.log(`- Problem with toObject transform: missing id field`);
            // The issue is likely in the model definition, not this specific instance
          } else if (typeof objResult.id !== 'string') {
            console.log(`- Problem with toObject transform: id is not a string (${typeof objResult.id})`);
            // The issue is likely in the model definition, not this specific instance
          } else {
            console.log(`- toObject transform works correctly`);
          }
        }
        
        // If template has categoryIcon, ensure proper handling
        if (template.categoryIcon) {
          // If it's a populated object
          if (typeof template.categoryIcon === 'object' && !Array.isArray(template.categoryIcon)) {
            if (template.categoryIcon._id && !template.categoryIcon.id) {
              console.log(`- Adding missing id to populated categoryIcon`);
              template.categoryIcon.id = template.categoryIcon._id.toString();
              needsSave = true;
            }
          }
          // No need to fix string references
        }
        
        // Save if needed
        if (needsSave) {
          await template.save();
          console.log(`✅ Template saved with fixed fields`);
          fixedCount++;
        } else {
          console.log(`✓ No fixes needed for this template`);
          alreadyCorrectCount++;
        }
      } catch (error) {
        console.error(`❌ Error processing template ${template._id}: ${error.message}`);
        errorCount++;
      }
    }
    
    // Summary
    console.log('\n=== Fix Summary ===');
    console.log(`Total templates processed: ${templates.length}`);
    console.log(`Templates already correct: ${alreadyCorrectCount}`);
    console.log(`Templates fixed: ${fixedCount}`);
    console.log(`Errors encountered: ${errorCount}`);
    
    if (errorCount > 0) {
      console.log('\n⚠️ Some templates had errors. Please check the logs above.');
    } else if (fixedCount > 0) {
      console.log('\n✅ All template ID issues fixed successfully!');
    } else {
      console.log('\n✅ All templates were already correct!');
    }
    
  } catch (error) {
    console.error('Error fixing template ID issues:', error);
    console.error(error.stack);
  } finally {
    await mongoose.connection.close();
    console.log('MongoDB connection closed');
  }
}

// Run the fix script
fixTemplateIdIssues().then(() => {
  console.log('Fix script completed.');
}).catch(error => {
  console.error('Fix script failed:', error);
  console.error(error.stack);
  process.exit(1);
}); 