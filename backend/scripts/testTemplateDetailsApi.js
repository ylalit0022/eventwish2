/**
 * Test script for template details API
 * 
 * This script tests the /templates/:id endpoint to ensure that:
 * 1. Template IDs are correctly returned as strings
 * 2. The API responds properly to template lookup requests
 * 3. JSON fields are consistent with Android client expectations
 * 
 * Run with: node scripts/testTemplateDetailsApi.js [templateId]
 */

const axios = require('axios');
require('dotenv').config();
const { MongoClient, ObjectId } = require('mongodb');
const mongoose = require('mongoose');
const logger = console;

// Configuration - Use the production URL since we're not running a local server
const BASE_URL = process.env.API_BASE_URL || 'https://eventwish2.onrender.com/api';
const MONGODB_URI = process.env.MONGODB_URI;

// Check if template ID is provided as command line argument
const providedTemplateId = process.argv[2];

async function runTests() {
    try {
        console.log('\n=== TEMPLATE API ID HANDLING TESTS ===\n');
        
        // If no template ID provided, find one from the database
        let templateId = providedTemplateId;
        if (!templateId) {
            // Connect to MongoDB to get a sample template ID
            console.log('No template ID provided, fetching one from database...');
            
            const client = await MongoClient.connect(MONGODB_URI, {
                useNewUrlParser: true,
                useUnifiedTopology: true
            });
            
            const db = client.db();
            const templatesCollection = db.collection('templates');
            
            // Find one active template
            const template = await templatesCollection.findOne({ status: true });
            
            if (!template) {
                console.error('No active templates found in database');
                process.exit(1);
            }
            
            templateId = template._id.toString();
            console.log(`Using template ID: ${templateId}`);
            
            // Close MongoDB connection
            await client.close();
        }
        
        // Test 1: Fetch template by ID using API
        console.log('\nTest 1: Fetch template by ID via API');
        console.log(`API URL: ${BASE_URL}/templates/${templateId}`);
        
        const response = await axios.get(`${BASE_URL}/templates/${templateId}`);
        
        console.log(`API Response Status: ${response.status}`);
        
        if (response.status !== 200) {
            console.error(`API returned error status: ${response.status}`);
            console.error(response.data);
            process.exit(1);
        }
        
        const template = response.data;
        
        // Test 2: Verify ID field is present and is a string
        console.log('\nTest 2: Verify ID fields are strings');
        
        console.log(`- Template ID: ${template.id}`);
        console.log(`- Template ID type: ${typeof template.id}`);
        console.log(`- Template ID is string: ${typeof template.id === 'string' ? '✅ Yes' : '❌ No'}`);
        
        // Check for presence of _id field (should be removed in response)
        console.log(`- _id field present: ${template._id !== undefined ? '❌ Yes (Should be removed)' : '✅ No (Correct)'}`);
        
        // Test 3: Verify all required fields for Android client are present
        console.log('\nTest 3: Verify required fields for Android client');
        
        const requiredFields = [
            'id', 'title', 'category', 'htmlContent', 'cssContent', 'jsContent', 'previewUrl'
        ];
        
        let missingFields = [];
        requiredFields.forEach(field => {
            if (template[field] === undefined) {
                missingFields.push(field);
            }
        });
        
        if (missingFields.length === 0) {
            console.log(`- All required fields present: ✅ Yes`);
        } else {
            console.log(`- Missing fields: ❌ ${missingFields.join(', ')}`);
        }
        
        // Test 4: Test categoryIcon ID handling
        console.log('\nTest 4: Verify categoryIcon ID handling');
        
        if (template.categoryIcon) {
            console.log(`- categoryIcon present: ✅ Yes`);
            console.log(`- categoryIcon.id: ${template.categoryIcon.id}`);
            console.log(`- categoryIcon.id type: ${typeof template.categoryIcon.id}`);
            console.log(`- categoryIcon.id is string: ${typeof template.categoryIcon.id === 'string' ? '✅ Yes' : '❌ No'}`);
            
            // Check for presence of _id field in categoryIcon (should be removed in response)
            console.log(`- categoryIcon._id present: ${template.categoryIcon._id !== undefined ? '❌ Yes (Should be removed)' : '✅ No (Correct)'}`);
        } else {
            console.log(`- categoryIcon present: ❌ No (Test skipped)`);
        }
        
        // Summary
        const allTestsPassed = 
            (typeof template.id === 'string') && 
            (template._id === undefined) && 
            (missingFields.length === 0) && 
            (!template.categoryIcon || typeof template.categoryIcon.id === 'string') &&
            (!template.categoryIcon || template.categoryIcon._id === undefined);
        
        console.log('\nTest Summary:');
        console.log(`- All ID handling tests passed: ${allTestsPassed ? '✅ Yes' : '❌ No'}`);
        console.log('- Full API response:');
        console.log(JSON.stringify(template, null, 2));
        
        console.log('\n=== TEMPLATE API ID HANDLING TESTS COMPLETED ===\n');
    } catch (error) {
        console.error('Error running tests:', error);
        
        if (error.response) {
            console.error('API Error Response:');
            console.error(`Status: ${error.response.status}`);
            console.error(`Data: ${JSON.stringify(error.response.data, null, 2)}`);
        }
    }
}

runTests().catch(err => {
    console.error('Test error:', err);
    process.exit(1);
}); 