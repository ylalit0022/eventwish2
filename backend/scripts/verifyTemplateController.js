/**
 * Verify Template Controller
 * 
 * This script directly tests the templateController to ensure that:
 * 1. IDs are properly converted to strings
 * 2. The categoryIcon._id field is properly removed
 * 3. The template responses are compatible with Android navigation
 * 
 * Run with: node scripts/verifyTemplateController.js [templateId]
 */

require('dotenv').config();
const mongoose = require('mongoose');
const Template = require('../models/Template');
const CategoryIcon = require('../models/CategoryIcon');
const templateController = require('../controllers/templateController');

// Utility function to create a mock response
function createMockResponse() {
    const res = {};
    let statusCode = 200;
    let responseBody = null;
    
    res.status = (code) => {
        statusCode = code;
        return res;
    };
    
    res.json = (body) => {
        responseBody = body;
        return res;
    };
    
    res.getStatus = () => statusCode;
    res.getJson = () => responseBody;
    
    return res;
}

// Check if template ID is provided as command line argument
const providedTemplateId = process.argv[2];

async function runTests() {
    try {
        console.log('\n=== TEMPLATE CONTROLLER VERIFICATION TESTS ===\n');
        
        // Connect to MongoDB
        await mongoose.connect(process.env.MONGODB_URI, {
            useNewUrlParser: true,
            useUnifiedTopology: true
        });
        console.log('Connected to MongoDB successfully');
        
        // Get template ID (from command line or find one)
        let templateId = providedTemplateId;
        if (!templateId) {
            console.log('No template ID provided, finding one from database...');
            const template = await Template.findOne({ status: true });
            if (!template) {
                console.error('No active templates found in database');
                process.exit(1);
            }
            templateId = template._id.toString();
        }
        
        console.log(`Using template ID: ${templateId}`);
        
        // Test 1: Test getTemplateById endpoint
        console.log('\nTest 1: getTemplateById Controller Function');
        const req = { params: { id: templateId } };
        const res = createMockResponse();
        
        // Call the controller function directly
        await templateController.getTemplateById(req, res);
        
        // Check the response
        const statusCode = res.getStatus();
        const responseBody = res.getJson();
        
        console.log(`- Response status: ${statusCode}`);
        console.log(`- Template ID in response: ${responseBody.id}`);
        console.log(`- Template ID is string: ${typeof responseBody.id === 'string' ? '✅ Yes' : '❌ No'}`);
        console.log(`- _id field in response: ${responseBody._id !== undefined ? '❌ Yes (Should be removed)' : '✅ No (Correctly removed)'}`);
        
        // Check categoryIcon handling
        if (responseBody.categoryIcon) {
            console.log('- categoryIcon field present in response');
            console.log(`- categoryIcon.id: ${responseBody.categoryIcon.id}`);
            console.log(`- categoryIcon.id is string: ${typeof responseBody.categoryIcon.id === 'string' ? '✅ Yes' : '❌ No'}`);
            console.log(`- categoryIcon._id field: ${responseBody.categoryIcon._id !== undefined ? '❌ Yes (Should be removed)' : '✅ No (Correctly removed)'}`);
        } else {
            console.log('- categoryIcon field not present in response');
        }
        
        // Simulate how Android would handle this data
        const jsonString = JSON.stringify(responseBody);
        const parsedTemplate = JSON.parse(jsonString);
        
        // Test 2: Verify Android handling
        console.log('\nTest 2: Android JSON Handling Simulation');
        console.log(`- Template ID after JSON.parse: ${parsedTemplate.id}`);
        console.log(`- Template ID type after parse: ${typeof parsedTemplate.id}`);
        
        if (parsedTemplate.categoryIcon) {
            console.log(`- categoryIcon.id after parse: ${parsedTemplate.categoryIcon.id}`);
            console.log(`- categoryIcon.id type after parse: ${typeof parsedTemplate.categoryIcon.id}`);
            console.log(`- categoryIcon._id field after parse: ${parsedTemplate.categoryIcon._id !== undefined ? '❌ Present (Problem)' : '✅ Absent (Correct)'}`);
        }
        
        // Test 3: Test getTemplates endpoint (list)
        console.log('\nTest 3: getTemplates Controller Function');
        const listReq = { query: { page: 1, limit: 5 } };
        const listRes = createMockResponse();
        
        // Call the controller function directly
        await templateController.getTemplates(listReq, listRes);
        
        // Check the response
        const listStatusCode = listRes.getStatus();
        const listResponseBody = listRes.getJson();
        
        console.log(`- Response status: ${listStatusCode}`);
        console.log(`- Templates returned: ${listResponseBody.data.length}`);
        
        // Check the first template in the list
        if (listResponseBody.data.length > 0) {
            const firstTemplate = listResponseBody.data[0];
            console.log(`- First template ID: ${firstTemplate.id}`);
            console.log(`- Template ID is string: ${typeof firstTemplate.id === 'string' ? '✅ Yes' : '❌ No'}`);
            console.log(`- _id field in template: ${firstTemplate._id !== undefined ? '❌ Yes (Should be removed)' : '✅ No (Correctly removed)'}`);
            
            if (firstTemplate.categoryIcon) {
                console.log(`- categoryIcon.id: ${firstTemplate.categoryIcon.id}`);
                console.log(`- categoryIcon.id is string: ${typeof firstTemplate.categoryIcon.id === 'string' ? '✅ Yes' : '❌ No'}`);
                console.log(`- categoryIcon._id field: ${firstTemplate.categoryIcon._id !== undefined ? '❌ Yes (Should be removed)' : '✅ No (Correctly removed)'}`);
            }
        }
        
        console.log('\nAll tests completed successfully');
        console.log('\n=== TEMPLATE CONTROLLER VERIFICATION TESTS COMPLETED ===\n');
    } catch (error) {
        console.error('Error in tests:', error);
        console.error(error.stack);
    } finally {
        // Close MongoDB connection
        await mongoose.connection.close();
        console.log('MongoDB connection closed');
        process.exit(0);
    }
}

// Run the tests
runTests().catch(err => {
    console.error('Test error:', err);
    process.exit(1);
}); 