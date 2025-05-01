/**
 * Test script to validate CategoryIcon population in Template responses
 */
const mongoose = require('mongoose');
require('dotenv').config();

const Template = require('../models/Template');
const CategoryIcon = require('../models/CategoryIcon');

async function testCategoryIconPopulation() {
    try {
        // Connect to MongoDB
        await mongoose.connect(process.env.MONGODB_URI, {
            useNewUrlParser: true,
            useUnifiedTopology: true
        });
        console.log('Connected to MongoDB');

        // 1. Test getting all templates with populated CategoryIcon
        console.log('\n====== Testing template list with populated CategoryIcon ======');
        const templates = await Template.find()
            .populate({
                path: 'categoryIcon',
                select: '_id id category categoryIcon iconType'
            })
            .limit(5);

        console.log(`Found ${templates.length} templates`);
        
        if (templates.length > 0) {
            console.log('First template example:');
            const template = templates[0];
            console.log(`- ID: ${template._id}`);
            console.log(`- Title: ${template.title}`);
            console.log(`- Category: ${template.category}`);
            
            if (template.categoryIcon) {
                if (typeof template.categoryIcon === 'object') {
                    console.log('✅ CategoryIcon is populated as object');
                    console.log(`- CategoryIcon ID: ${template.categoryIcon._id}`);
                    console.log(`- CategoryIcon Category: ${template.categoryIcon.category}`);
                    console.log(`- CategoryIcon URL: ${template.categoryIcon.categoryIcon}`);
                } else {
                    console.log('❌ CategoryIcon is not properly populated: ' + typeof template.categoryIcon);
                    console.log('Value:', template.categoryIcon);
                }
            } else {
                console.log('❌ CategoryIcon is null or undefined');
            }
        }
        
        // 2. Test getting a specific template by ID with populated CategoryIcon
        if (templates.length > 0) {
            const templateId = templates[0]._id;
            console.log(`\n====== Testing single template by ID ${templateId} ======`);
            
            const singleTemplate = await Template.findById(templateId)
                .populate({
                    path: 'categoryIcon',
                    select: '_id id category categoryIcon iconType'
                });
                
            if (singleTemplate) {
                console.log(`- ID: ${singleTemplate._id}`);
                console.log(`- Title: ${singleTemplate.title}`);
                
                if (singleTemplate.categoryIcon) {
                    if (typeof singleTemplate.categoryIcon === 'object') {
                        console.log('✅ CategoryIcon is populated as object');
                        console.log(`- CategoryIcon ID: ${singleTemplate.categoryIcon._id}`);
                        console.log(`- CategoryIcon Category: ${singleTemplate.categoryIcon.category}`);
                        console.log(`- CategoryIcon URL: ${singleTemplate.categoryIcon.categoryIcon}`);
                    } else {
                        console.log('❌ CategoryIcon is not properly populated: ' + typeof singleTemplate.categoryIcon);
                        console.log('Value:', singleTemplate.categoryIcon);
                    }
                } else {
                    console.log('❌ CategoryIcon is null or undefined');
                }
                
                // Convert to JSON to verify toJSON transform
                console.log('\nTesting template JSON serialization:');
                const templateJson = JSON.parse(JSON.stringify(singleTemplate));
                
                if (templateJson.categoryIcon) {
                    if (typeof templateJson.categoryIcon === 'object') {
                        console.log('✅ CategoryIcon is serialized as object');
                        console.log('- CategoryIcon in JSON:', templateJson.categoryIcon);
                    } else {
                        console.log('❌ CategoryIcon is not properly serialized: ' + typeof templateJson.categoryIcon);
                        console.log('Value:', templateJson.categoryIcon);
                    }
                } else {
                    console.log('❌ CategoryIcon is missing from JSON');
                }
            } else {
                console.log('❌ Template not found');
            }
        }
        
        // 3. Test CategoryIcon model directly
        console.log('\n====== Testing CategoryIcon model directly ======');
        const categoryIcons = await CategoryIcon.find().limit(5);
        
        console.log(`Found ${categoryIcons.length} category icons`);
        
        if (categoryIcons.length > 0) {
            console.log('First category icon example:');
            const categoryIcon = categoryIcons[0];
            console.log(`- _id: ${categoryIcon._id}`);
            console.log(`- id: ${categoryIcon.id}`);
            console.log(`- Category: ${categoryIcon.category}`);
            console.log(`- URL: ${categoryIcon.categoryIcon}`);
            
            // Convert to JSON to verify toJSON transform
            console.log('\nTesting CategoryIcon JSON serialization:');
            const iconJson = JSON.parse(JSON.stringify(categoryIcon));
            
            console.log('- JSON representation:', iconJson);
            console.log('- Has _id:', iconJson._id ? '✅' : '❌');
            console.log('- Has id:', iconJson.id ? '✅' : '❌');
        }

        console.log('\nTesting complete.');
    } catch (error) {
        console.error('Error during test:', error);
    } finally {
        // Close MongoDB connection
        await mongoose.connection.close();
        console.log('\nMongoDB connection closed');
    }
}

// Run the test
testCategoryIconPopulation()
    .then(() => console.log('Test script completed successfully.'))
    .catch(err => console.error('Test script failed:', err)); 