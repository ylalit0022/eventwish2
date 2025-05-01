/**
 * Test script for template ID handling
 * 
 * This script tests that the Template model properly handles template IDs as strings
 * Run with: node scripts/testTemplateIdHandling.js
 */

const mongoose = require('mongoose');
require('dotenv').config();
const Template = require('../models/Template');
const logger = console;

// Test constants
const TEST_TITLE = 'Test Template ' + Date.now();
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

async function runTests() {
    try {
        console.log('Starting template ID handling tests...');
        
        // Create a test template
        const testTemplate = new Template({
            title: TEST_TITLE,
            category: TEST_CATEGORY,
            htmlContent: '<h1>Test Template</h1>',
            cssContent: 'h1 { color: blue; }',
            jsContent: 'console.log("Test Template");',
            previewUrl: 'https://example.com/test-template.png'
        });
        
        await testTemplate.save();
        console.log(`Created test template with ID: ${testTemplate._id}`);
        
        // Test 1: Check that the _id is converted to id properly in toJSON
        console.log('\nTest 1: toJSON transformation');
        const jsonTemplate = testTemplate.toJSON();
        
        console.log(`- Template _id: ${testTemplate._id}`);
        console.log(`- Template id from toJSON: ${jsonTemplate.id}`);
        
        // Check if id is a string
        const idIsString = typeof jsonTemplate.id === 'string';
        console.log(`- Is id a string? ${idIsString ? '✅ Yes' : '❌ No'}`);
        
        // Test 2: Direct modification of id and verify it's still a string
        console.log('\nTest 2: Manual id handling');
        testTemplate.id = testTemplate._id;
        const manualIdIsString = typeof testTemplate.id === 'string';
        console.log(`- Manual id assignment: ${testTemplate.id}`);
        console.log(`- Is manually assigned id a string? ${manualIdIsString ? '✅ Yes' : '❌ No'}`);
        
        // Test 3: Finding the template by ID and verifying id field
        console.log('\nTest 3: Finding by ID');
        const foundTemplate = await Template.findById(testTemplate._id);
        const foundTemplateJson = foundTemplate.toJSON();
        
        console.log(`- Found template _id: ${foundTemplate._id}`);
        console.log(`- Found template id from toJSON: ${foundTemplateJson.id}`);
        
        // Check if found template's id is a string
        const foundIdIsString = typeof foundTemplateJson.id === 'string';
        console.log(`- Is found template's id a string? ${foundIdIsString ? '✅ Yes' : '❌ No'}`);
        
        // Test 4: Virtual id property
        console.log('\nTest 4: Virtual id property');
        console.log(`- Virtual id property: ${testTemplate.id}`);
        const virtualIdIsString = typeof testTemplate.id === 'string';
        console.log(`- Is virtual id a string? ${virtualIdIsString ? '✅ Yes' : '❌ No'}`);
        
        // Test 5: Test toObject transformation
        console.log('\nTest 5: toObject transformation');
        const objectTemplate = testTemplate.toObject();
        
        console.log(`- Template _id: ${testTemplate._id}`);
        console.log(`- Template id from toObject: ${objectTemplate.id}`);
        
        // Check if id is a string
        const objectIdIsString = typeof objectTemplate.id === 'string';
        console.log(`- Is id from toObject a string? ${objectIdIsString ? '✅ Yes' : '❌ No'}`);
        
        // Clean up
        console.log('\nCleaning up...');
        await Template.findByIdAndDelete(testTemplate._id);
        console.log('Test template deleted');
        
        // Final verification
        console.log('\nTest results:');
        if (idIsString && manualIdIsString && foundIdIsString && virtualIdIsString && objectIdIsString) {
            console.log('✅ All tests passed! Template IDs are properly handled as strings.');
        } else {
            console.log('❌ Some tests failed. Template ID handling needs improvement.');
        }
        
    } catch (error) {
        console.error('Test error:', error);
        console.error(error.stack);
    } finally {
        // Close the MongoDB connection
        await mongoose.connection.close();
        console.log('MongoDB connection closed');
        process.exit(0);
    }
} 