/**
 * Script to fix existing CategoryIcon documents to ensure id field is set
 */

const mongoose = require('mongoose');
const CategoryIcon = require('../models/CategoryIcon');
require('dotenv').config();

async function fixCategoryIcons() {
    try {
        console.log('Connecting to MongoDB...');
        await mongoose.connect(process.env.MONGODB_URI);
        console.log('Connected to MongoDB');

        // Find all category icons
        const icons = await CategoryIcon.find();
        console.log(`Found ${icons.length} category icons`);

        // Update each icon to ensure id field is set
        let updatedCount = 0;
        for (const icon of icons) {
            if (!icon.id) {
                icon.id = icon._id.toString();
                await icon.save();
                updatedCount++;
                console.log(`Updated icon ${icon._id} with id ${icon.id}`);
            } else {
                console.log(`Icon ${icon._id} already has id ${icon.id}`);
            }
        }

        console.log(`\nUpdated ${updatedCount} icons to ensure id field is set`);
        
        // Direct database update as a fallback
        if (updatedCount === 0 && icons.length > 0) {
            console.log('\nTrying direct database update...');
            
            const db = mongoose.connection.db;
            const collection = db.collection('categoryicons');
            
            for (const icon of icons) {
                const result = await collection.updateOne(
                    { _id: icon._id, id: { $exists: false } },
                    { $set: { id: icon._id.toString() } }
                );
                
                if (result.modifiedCount > 0) {
                    updatedCount++;
                    console.log(`Updated icon ${icon._id} with direct DB update`);
                }
            }
            
            console.log(`\nUpdated ${updatedCount} icons with direct DB update`);
        }
        
        // Verify all icons have id field
        console.log('\nVerifying all icons have id field...');
        const verifyIcons = await CategoryIcon.find();
        
        let allValid = true;
        for (const icon of verifyIcons) {
            if (!icon.id) {
                console.log(`❌ Icon ${icon._id} still missing id field`);
                allValid = false;
            }
        }
        
        if (allValid) {
            console.log('✅ All icons have id field set');
        }
        
        // Test populated behavior
        console.log('\nTesting JSON serialization...');
        for (const icon of verifyIcons.slice(0, 3)) { // Check first 3 icons
            const jsonIcon = JSON.parse(JSON.stringify(icon));
            console.log(`Icon ${icon._id}:`);
            console.log(`- Original id: ${icon.id}`);
            console.log(`- JSON id: ${jsonIcon.id}`);
            console.log(`- Has id in JSON: ${jsonIcon.id ? '✅ Yes' : '❌ No'}`);
        }
        
        console.log('\nDone!');
    } catch (error) {
        console.error('Error:', error);
    } finally {
        await mongoose.connection.close();
        console.log('MongoDB connection closed');
    }
}

// Run the fixer
fixCategoryIcons().then(() => {
    console.log('Script completed successfully.');
}).catch(error => {
    console.error('Script failed:', error);
    process.exit(1);
}); 