/**
 * Test script for ID transformations in Template and CategoryIcon models
 * 
 * This script tests that ObjectIDs are properly converted to strings when
 * templates and category icons are serialized to JSON.
 * 
 * Run with: node scripts/testIdTransformations.js
 */

require('dotenv').config();
const mongoose = require('mongoose');
const Template = require('../models/Template');
const CategoryIcon = require('../models/CategoryIcon');
const logger = console;

// Test constants
const TEST_TEMPLATE_TITLE = 'Test Template ' + Date.now();
const TEST_CATEGORY = 'test';

// MongoDB connection
mongoose.connect(process.env.MONGODB_URI, {
    useNewUrlParser: true,
    useUnifiedTopology: true
}).then(() => {
    console.log('Connected to MongoDB successfully');
    runTests();
}).catch(err => {
    console.error('MongoDB connection error:', err);
    process.exit(1);
});

async function createTestCategoryIcon() {
    console.log('Creating test category icon...');
    const timestamp = Date.now();
    const categoryIcon = new CategoryIcon({
        id: new mongoose.Types.ObjectId().toString(), // Generate a string ID
        category: `${TEST_CATEGORY}${timestamp}`, // Ensure unique category
        categoryIcon: 'https://example.com/icon.png',
        iconType: 'URL',
        resourceName: 'test_icon'
    });
    
    await categoryIcon.save();
    console.log(`Created test category icon with ID: ${categoryIcon._id}`);
    return categoryIcon;
}

async function createTestTemplate(categoryIcon) {
    console.log('Creating test template...');
    const template = new Template({
        title: TEST_TEMPLATE_TITLE,
        category: TEST_CATEGORY,
        htmlContent: '<h1>Test Template</h1>',
        cssContent: 'h1 { color: blue; }',
        jsContent: 'console.log("Test Template");',
        previewUrl: 'https://example.com/test-template.png'
    });
    
    if (categoryIcon) {
        template.categoryIcon = categoryIcon._id;
    }
    
    await template.save();
    console.log(`Created test template with ID: ${template._id}`);
    return template;
}

async function runTests() {
    try {
        console.log('\n=== ID TRANSFORMATION TESTS ===\n');
        
        // Test 1: Create and verify CategoryIcon ID transformation
        console.log('\nTest 1: CategoryIcon ID Transformation');
        const categoryIcon = await createTestCategoryIcon();
        
        // Convert to JSON and check id field
        const categoryIconJson = categoryIcon.toJSON();
        console.log('CategoryIcon JSON:', JSON.stringify(categoryIconJson, null, 2));
        
        console.log(`- MongoDB _id: ${categoryIcon._id}`);
        console.log(`- JSON id: ${categoryIconJson.id}`);
        console.log(`- JSON id is string: ${typeof categoryIconJson.id === 'string' ? '✅ Yes' : '❌ No'}`);
        console.log(`- _id field in JSON: ${categoryIconJson._id !== undefined ? '❌ Yes (Should be removed)' : '✅ No (Correctly removed)'}`);
        
        // Test 2: Create and verify Template ID transformation
        console.log('\nTest 2: Template ID Transformation');
        const template = await createTestTemplate(categoryIcon);
        
        // Retrieve the template with populated categoryIcon
        const populatedTemplate = await Template.findById(template._id).populate('categoryIcon');
        console.log(`Retrieved template with ID: ${populatedTemplate._id}`);
        
        // Convert to JSON and check id fields
        const templateJson = populatedTemplate.toJSON();
        console.log('Template JSON:', JSON.stringify(templateJson, null, 2));
        
        console.log(`- MongoDB _id: ${populatedTemplate._id}`);
        console.log(`- JSON id: ${templateJson.id}`);
        console.log(`- JSON id is string: ${typeof templateJson.id === 'string' ? '✅ Yes' : '❌ No'}`);
        console.log(`- _id field in JSON: ${templateJson._id !== undefined ? '❌ Yes (Should be removed)' : '✅ No (Correctly removed)'}`);
        
        // Test 3: Verify populated categoryIcon in template
        console.log('\nTest 3: Populated CategoryIcon in Template');
        if (templateJson.categoryIcon) {
            console.log(`- categoryIcon present: ✅ Yes`);
            console.log(`- categoryIcon.id: ${templateJson.categoryIcon.id}`);
            console.log(`- categoryIcon.id is string: ${typeof templateJson.categoryIcon.id === 'string' ? '✅ Yes' : '❌ No'}`);
            console.log(`- _id field in categoryIcon: ${templateJson.categoryIcon._id !== undefined ? '❌ Yes (Should be removed)' : '✅ No (Correctly removed)'}`);
        } else {
            console.log(`- categoryIcon present: ❌ No (Test skipped)`);
        }
        
        // Test 4: Verify transformations are consistent with controller usage
        console.log('\nTest 4: Controller-like Usage Test');
        
        // This simulates how a controller would handle the template
        const controllerResponse = {
            data: templateJson,
            success: true
        };
        
        // Convert the entire response to JSON string and back (like res.json() would do)
        const responseJson = JSON.parse(JSON.stringify(controllerResponse));
        
        console.log(`- Template ID in response: ${responseJson.data.id}`);
        console.log(`- _id field in response: ${responseJson.data._id !== undefined ? '❌ Yes (Should be removed)' : '✅ No (Correctly removed)'}`);
        
        if (responseJson.data.categoryIcon) {
            console.log(`- categoryIcon.id in response: ${responseJson.data.categoryIcon.id}`);
            console.log(`- categoryIcon._id in response: ${responseJson.data.categoryIcon._id !== undefined ? '❌ Yes (Should be removed)' : '✅ No (Correctly removed)'}`);
        }
        
        // Clean up test data
        await Template.findByIdAndDelete(template._id);
        await CategoryIcon.findByIdAndDelete(categoryIcon._id);
        console.log('\nTest data cleaned up.');
        
        console.log('\n=== ID TRANSFORMATION TESTS COMPLETED ===\n');
    } catch (error) {
        console.error('Test error:', error);
        console.error(error.stack);
    } finally {
        // Close MongoDB connection
        await mongoose.connection.close();
        console.log('MongoDB connection closed');
        process.exit(0);
    }
} 