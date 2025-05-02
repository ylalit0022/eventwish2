const mongoose = require('mongoose');
const Template = require('../models/Template');
const CategoryIcon = require('../models/CategoryIcon');
require('dotenv').config({ path: require('path').resolve(__dirname, '../.env') });

/**
 * Migration script to update templates to use direct categoryIcon URLs
 * instead of ObjectId references to the CategoryIcon collection
 */
async function migrateCategoryIconsToUrls() {
    let connection;
    try {
        // Connect to MongoDB
        console.log('Connecting to MongoDB...');
        connection = await mongoose.connect(process.env.MONGODB_URI, {
            useNewUrlParser: true,
            useUnifiedTopology: true
        });
        console.log('Connected to MongoDB successfully');

        // Get count of templates to migrate
        const totalTemplates = await Template.countDocuments();
        console.log(`Found ${totalTemplates} templates to potentially migrate`);

        // Find all templates with categoryIcon as ObjectId
        const templates = await Template.find({});
        console.log(`Retrieved ${templates.length} templates for processing`);

        let migratedCount = 0;
        let skippedCount = 0;
        let errorCount = 0;

        // Process each template
        for (const template of templates) {
            try {
                // Skip templates that already have string URLs
                if (typeof template.categoryIcon === 'string') {
                    if (template.categoryIcon.startsWith('http')) {
                        console.log(`Skipping template "${template.title}" - already has URL: ${template.categoryIcon}`);
                        skippedCount++;
                        continue;
                    }
                }

                // Skip templates with no categoryIcon
                if (!template.categoryIcon) {
                    console.log(`Skipping template "${template.title}" - no categoryIcon to migrate`);
                    skippedCount++;
                    continue;
                }

                // If categoryIcon is an ObjectId reference, find the corresponding CategoryIcon
                let iconUrl = null;
                if (mongoose.Types.ObjectId.isValid(template.categoryIcon)) {
                    const categoryIcon = await CategoryIcon.findById(template.categoryIcon);
                    
                    if (categoryIcon) {
                        iconUrl = categoryIcon.categoryIcon;
                        console.log(`Found icon URL "${iconUrl}" for template "${template.title}"`);
                    } else {
                        console.log(`Warning: No CategoryIcon found for id ${template.categoryIcon} in template "${template.title}"`);
                    }
                }

                // Update the template with the direct URL
                if (iconUrl) {
                    template.categoryIcon = iconUrl;
                    await template.save();
                    console.log(`✅ Updated template "${template.title}" with direct URL: ${iconUrl}`);
                    migratedCount++;
                } else {
                    // If we couldn't find a URL, set to null to avoid reference issues
                    template.categoryIcon = null;
                    await template.save();
                    console.log(`⚠️ Updated template "${template.title}" with null categoryIcon (no URL found)`);
                    errorCount++;
                }
            } catch (error) {
                console.error(`Error processing template "${template.title}":`, error.message);
                errorCount++;
            }
        }

        // Print migration summary
        console.log('\n====== MIGRATION SUMMARY ======');
        console.log(`Total templates: ${totalTemplates}`);
        console.log(`Migrated successfully: ${migratedCount}`);
        console.log(`Skipped (already URLs or no icon): ${skippedCount}`);
        console.log(`Errors/null icons: ${errorCount}`);
        console.log('==============================\n');

    } catch (error) {
        console.error('Migration error:', error);
    } finally {
        if (connection) {
            await mongoose.connection.close();
            console.log('Database connection closed');
        }
    }
}

// Run the migration
migrateCategoryIconsToUrls(); 