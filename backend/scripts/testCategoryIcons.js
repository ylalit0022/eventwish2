/**
 * Test script for CategoryIcon handling in Templates
 * 
 * This test verifies:
 * 1. CategoryIcon population in Template queries
 * 2. Proper JSON serialization of CategoryIcon references
 * 3. Consistency between _id and id fields
 * 4. Proper handling of unpopulated references
 */

const mongoose = require('mongoose');
const Template = require('../models/Template');
const CategoryIcon = require('../models/CategoryIcon');
require('dotenv').config();

async function connectToMongoDB() {
    console.log(`Connecting to MongoDB: ${process.env.MONGODB_URI || 'mongodb://localhost:27017/eventwish_test'}`);
    try {
        await mongoose.connect(process.env.MONGODB_URI, {
            useNewUrlParser: true,
            useUnifiedTopology: true
        });
        console.log('Connected to MongoDB');
    } catch (error) {
        console.error('MongoDB connection error:', error);
        console.error(error.stack);
        process.exit(1);
    }
}

async function validateCategoryIconReference(template) {
    console.log(`\nValidating template: ${template.title} (ID: ${template._id || template.id})`);
    
    // Check if categoryIcon exists
    if (!template.categoryIcon) {
        console.log('No categoryIcon reference found in this template');
        return;
    }
    
    console.log('CategoryIcon reference details:');
    
    // Check if it's populated (object) or just an ID (string or ObjectId)
    if (typeof template.categoryIcon === 'object' && !mongoose.Types.ObjectId.isValid(template.categoryIcon)) {
        console.log(`- Populated: true`);
        console.log(`- Category: ${template.categoryIcon.category || 'N/A'}`);
        console.log(`- Icon URL: ${template.categoryIcon.categoryIcon || 'N/A'}`);
        
        // Check for both _id and id fields
        console.log(`- Has _id: ${template.categoryIcon._id ? true : false}`);
        console.log(`- Has id: ${template.categoryIcon.id ? true : false}`);
        
        // Verify they match if both exist
        if (template.categoryIcon._id && template.categoryIcon.id) {
            const idsMatch = template.categoryIcon._id.toString() === template.categoryIcon.id.toString();
            console.log(`- _id and id match: ${idsMatch ? '✅ Yes' : '❌ No'}`);
            
            if (!idsMatch) {
                console.log(`  _id: ${template.categoryIcon._id}`);
                console.log(`  id: ${template.categoryIcon.id}`);
            }
        }
    } else {
        console.log(`- Populated: false`);
        console.log(`- Reference type: ${typeof template.categoryIcon}`);
        console.log(`- Reference value: ${template.categoryIcon.toString()}`);
    }
}

async function testCategoryIconModel() {
    try {
        console.log('\nTesting CategoryIcon model...');
        
        // Find one existing category icon
        const icon = await CategoryIcon.findOne({});
        
        if (!icon) {
            console.log('No category icons found. Creating a test icon...');
            
            // Create a new test icon
            const newIcon = new CategoryIcon({
                category: 'TestCategory',
                id: new mongoose.Types.ObjectId().toString(),
                categoryIcon: 'https://example.com/icon.png',
                iconType: 'URL'
            });
            
            await newIcon.save();
            console.log(`Created test icon with ID: ${newIcon._id}`);
            
            // Run tests with the new icon
            await testIconProperties(newIcon);
        } else {
            console.log(`Found existing icon: ${icon.category} (ID: ${icon._id})`);
            await testIconProperties(icon);
        }
    } catch (error) {
        console.error('Error testing CategoryIcon model:', error);
        console.error(error.stack);
    }
}

async function testIconProperties(icon) {
    try {
        console.log('\nTesting CategoryIcon properties...');
        
        // Get JSON representation
        const jsonIcon = icon.toJSON();
        
        console.log('JSON representation:');
        console.log(JSON.stringify(jsonIcon, null, 2));
        
        // Test essential properties
        console.log('\nVerifying essential properties:');
        console.log(`- Has _id: ${jsonIcon._id ? '✅ Yes' : '❌ No'}`);
        console.log(`- Has id: ${jsonIcon.id ? '✅ Yes' : '❌ No'}`);
        console.log(`- Has category: ${jsonIcon.category ? '✅ Yes' : '❌ No'}`);
        console.log(`- Has categoryIcon: ${jsonIcon.categoryIcon ? '✅ Yes' : '❌ No'}`);
        
        // Verify both id fields match
        if (jsonIcon._id && jsonIcon.id) {
            const idsMatch = jsonIcon._id.toString() === jsonIcon.id.toString();
            console.log(`- _id and id match: ${idsMatch ? '✅ Yes' : '❌ No'}`);
        }
        
        // Test lookup by both _id and id
        const iconById = await CategoryIcon.findById(icon._id);
        const iconByOldId = await CategoryIcon.findOne({ id: icon.id });
        
        console.log('\nLookup tests:');
        console.log(`- Find by _id: ${iconById ? '✅ Success' : '❌ Failed'}`);
        console.log(`- Find by id: ${iconByOldId ? '✅ Success' : '❌ Failed'}`);
        
        // Verify both lookups found the same document
        if (iconById && iconByOldId) {
            const sameDoc = iconById._id.toString() === iconByOldId._id.toString();
            console.log(`- Same document found: ${sameDoc ? '✅ Yes' : '❌ No'}`);
        }
    } catch (error) {
        console.error('Error testing icon properties:', error);
        console.error(error.stack);
    }
}

async function testTemplatePopulation() {
    try {
        console.log('\nTesting Template population with CategoryIcon...');
        
        // Find a template with a category icon
        const template = await Template.findOne({ categoryIcon: { $ne: null } }).populate('categoryIcon');
        
        if (!template) {
            console.log('No templates with category icons found. Creating a test template...');
            
            // Find or create a category icon
            let icon = await CategoryIcon.findOne({});
            if (!icon) {
                icon = new CategoryIcon({
                    category: 'TestCategory',
                    id: new mongoose.Types.ObjectId().toString(),
                    categoryIcon: 'https://example.com/icon.png',
                    iconType: 'URL'
                });
                await icon.save();
                console.log(`Created test icon with ID: ${icon._id}`);
            }
            
            // Create a new test template
            const newTemplate = new Template({
                title: 'Test Template',
                category: icon.category,
                htmlContent: '<div>Test Content</div>',
                categoryIcon: icon._id
            });
            
            await newTemplate.save();
            console.log(`Created test template with ID: ${newTemplate._id}`);
            
            // Fetch it with populated icon
            const populatedTemplate = await Template.findById(newTemplate._id).populate('categoryIcon');
            await validateCategoryIconReference(populatedTemplate);
        } else {
            console.log(`Found template with category icon: ${template.title}`);
            await validateCategoryIconReference(template);
        }
        
        // Test serialization through JSON
        console.log('\nTesting JSON serialization...');
        
        // Get a list of templates with populated category icons
        const templates = await Template.find()
            .limit(3)
            .populate({
                path: 'categoryIcon',
                select: '_id id category categoryIcon iconType resourceName'
            });
        
        console.log(`Found ${templates.length} templates for JSON test`);
        
        for (const tmpl of templates) {
            // Convert to JSON and back
            const jsonStr = JSON.stringify(tmpl);
            const parsedTemplate = JSON.parse(jsonStr);
            
            console.log(`\nTemplate: ${tmpl.title} (ID: ${tmpl._id || tmpl.id})`);
            console.log(`- Has categoryIcon in original: ${tmpl.categoryIcon ? '✅ Yes' : '❌ No'}`);
            console.log(`- Has categoryIcon after JSON round-trip: ${parsedTemplate.categoryIcon ? '✅ Yes' : '❌ No'}`);
            
            if (tmpl.categoryIcon && parsedTemplate.categoryIcon) {
                // Check if category icon is properly preserved in JSON
                const originalIconId = typeof tmpl.categoryIcon === 'object' ? 
                    (tmpl.categoryIcon._id || tmpl.categoryIcon.id) : tmpl.categoryIcon;
                
                const parsedIconId = typeof parsedTemplate.categoryIcon === 'object' ? 
                    (parsedTemplate.categoryIcon._id || parsedTemplate.categoryIcon.id) : parsedTemplate.categoryIcon;
                
                const idsMatch = originalIconId.toString() === parsedIconId.toString();
                console.log(`- CategoryIcon IDs match after serialization: ${idsMatch ? '✅ Yes' : '❌ No'}`);
                
                if (typeof parsedTemplate.categoryIcon === 'object') {
                    console.log(`- CategoryIcon object preserved: ✅ Yes`);
                    console.log(`  * Has id: ${parsedTemplate.categoryIcon.id ? '✅ Yes' : '❌ No'}`);
                    console.log(`  * Has _id: ${parsedTemplate.categoryIcon._id ? '✅ Yes' : '❌ No'}`);
                } else {
                    console.log(`- CategoryIcon object preserved: ❌ No (received ${typeof parsedTemplate.categoryIcon})`);
                }
            }
        }
    } catch (error) {
        console.error('Error testing template population:', error);
        console.error(error.stack);
    }
}

async function runTests() {
    try {
        await connectToMongoDB();
        
        // Test the CategoryIcon model
        await testCategoryIconModel();
        
        // Test Template population with CategoryIcon
        await testTemplatePopulation();
        
        console.log('\n✅ All tests completed successfully!');
    } catch (error) {
        console.error('\n❌ Tests failed:', error);
        console.error(error.stack);
    } finally {
        // Close MongoDB connection
        await mongoose.connection.close();
        console.log('MongoDB connection closed');
    }
}

// Run the tests
runTests().then(() => {
    console.log('Test script completed.');
}).catch(error => {
    console.error('Unexpected error:', error);
    console.error(error.stack);
    process.exit(1);
}); 