const mongoose = require('mongoose');
const Template = require('../models/Template');
const CategoryIcon = require('../models/CategoryIcon');
require('dotenv').config({ path: require('path').resolve(__dirname, '../.env') });

async function createCategoryIcons() {
    let connection;
    try {
        // Connect to MongoDB
        console.log("Connecting to MongoDB...");
        connection = await mongoose.connect(process.env.MONGODB_URI, {
            useNewUrlParser: true,
            useUnifiedTopology: true
        });
        console.log("Connected to MongoDB successfully");

        // Get all templates
        const templates = await Template.find();
        console.log(`Found ${templates.length} templates in database`);
        
        // Extract unique categories and their icon URLs
        const categoryMap = {};
        templates.forEach(template => {
            const category = template.category;
            if (!category) return;
            
            // If we already have this category but new template has a valid icon, update it
            if (categoryMap[category] && template.categoryIcon) {
                categoryMap[category] = template.categoryIcon;
            }
            // If we don't have this category yet, add it
            else if (!categoryMap[category]) {
                categoryMap[category] = template.categoryIcon || null;
            }
        });
        
        // Show categories we found
        console.log("\nFound the following unique categories:");
        Object.entries(categoryMap).forEach(([category, iconUrl]) => {
            console.log(`- ${category}: ${iconUrl || 'No icon'}`);
        });
        
        // Check if any CategoryIcons already exist
        const existingCategoryIcons = await CategoryIcon.find();
        console.log(`\nFound ${existingCategoryIcons.length} existing CategoryIcon entries`);
        
        // Create CategoryIcon objects for categories that don't have one
        let createdCount = 0;
        let updatedCount = 0;
        const errors = [];
        
        // Process each category
        for (const [category, iconUrl] of Object.entries(categoryMap)) {
            // Skip if no icon URL
            if (!iconUrl) {
                console.log(`Skipping ${category} - no icon URL`);
                continue;
            }
            
            try {
                // Check if category icon already exists
                const existingIcon = existingCategoryIcons.find(icon => icon.category === category);
                
                if (existingIcon) {
                    console.log(`Updating existing icon for ${category}`);
                    existingIcon.categoryIcon = iconUrl;
                    await existingIcon.save();
                    updatedCount++;
                } else {
                    console.log(`Creating new icon for ${category}`);
                    const newCategoryIcon = new CategoryIcon({
                        id: `cat_${new mongoose.Types.ObjectId()}`,
                        category: category,
                        categoryIcon: iconUrl,
                        iconType: 'URL',
                        resourceName: ''
                    });
                    await newCategoryIcon.save();
                    createdCount++;
                }
            } catch (error) {
                console.error(`Error processing category ${category}:`, error.message);
                errors.push({ category, error: error.message });
            }
        }
        
        console.log("\nResults:");
        console.log(`- Created ${createdCount} new CategoryIcon entries`);
        console.log(`- Updated ${updatedCount} existing CategoryIcon entries`);
        
        if (errors.length > 0) {
            console.log(`- Encountered ${errors.length} errors:`);
            errors.forEach(err => console.log(`  * ${err.category}: ${err.error}`));
        }
        
        // Now let's update the templates to use CategoryIcon references
        if (process.argv.includes('--update-templates')) {
            console.log("\nUpdating templates to use CategoryIcon references...");
            
            // Get fresh list of CategoryIcons
            const allCategoryIcons = await CategoryIcon.find();
            const iconMap = {};
            allCategoryIcons.forEach(icon => {
                iconMap[icon.category] = icon._id;
            });
            
            // Update templates
            const templateUpdates = [];
            for (const template of templates) {
                const category = template.category;
                if (category && iconMap[category]) {
                    console.log(`Updating template "${template.title}" to use CategoryIcon reference`);
                    template.categoryIconObj = iconMap[category];
                    templateUpdates.push(template.save());
                }
            }
            
            // Wait for all updates to complete
            await Promise.all(templateUpdates);
            console.log(`Updated ${templateUpdates.length} templates`);
        }

    } catch (error) {
        console.error("Error creating category icons:", error);
    } finally {
        if (connection) {
            await mongoose.connection.close();
            console.log("\nDatabase connection closed");
        }
    }
}

// Run the function
createCategoryIcons(); 