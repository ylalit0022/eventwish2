const mongoose = require('mongoose');
const Template = require('../models/Template');
require('dotenv').config({ path: require('path').resolve(__dirname, '../.env') });

/**
 * Creates a CategoryIcon object from a URL string
 * @param {String} url - The icon URL
 * @param {String} category - The category name
 * @returns {Object} - CategoryIcon compatible object
 */
function createCategoryIconObject(url, category) {
    if (!url) return null;
    
    return {
        id: `auto_${new mongoose.Types.ObjectId()}`,
        category,
        categoryIcon: url
    };
}

async function checkAndFixTemplateIcons() {
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
        console.log(`Found ${templates.length} templates`);
        
        // Check and record issues with categoryIcon field
        let urlCount = 0;
        let objectCount = 0;
        let missingCount = 0;
        
        templates.forEach((template, index) => {
            console.log(`\n[${index + 1}] Template: ${template.title}`);
            console.log(`- Category: ${template.category}`);
            
            if (!template.categoryIcon) {
                console.log('- CategoryIcon: Missing');
                missingCount++;
            } else if (typeof template.categoryIcon === 'string') {
                console.log(`- CategoryIcon: URL String (${template.categoryIcon})`);
                urlCount++;
            } else {
                console.log(`- CategoryIcon: Object (${JSON.stringify(template.categoryIcon)})`);
                objectCount++;
            }
        });
        
        console.log("\nSummary:");
        console.log(`- URL Strings: ${urlCount}`);
        console.log(`- Objects: ${objectCount}`);
        console.log(`- Missing: ${missingCount}`);
        
        // Ask for confirmation before fixing
        console.log("\nWould you like to fix the templates? (Manually run with --fix flag)");
        
        // Check if --fix flag is provided
        if (process.argv.includes('--fix')) {
            console.log("\nFixing templates...");
            
            // Update each template with URL categoryIcon to use object format
            for (const template of templates) {
                if (typeof template.categoryIcon === 'string' && template.categoryIcon) {
                    const iconUrl = template.categoryIcon;
                    const categoryName = template.category;
                    
                    // Convert to CategoryIcon object
                    const categoryIconObj = createCategoryIconObject(iconUrl, categoryName);
                    
                    // Update in database
                    await Template.updateOne(
                        { _id: template._id },
                        { $set: { categoryIconObj: categoryIconObj, categoryIcon: iconUrl } }
                    );
                    
                    console.log(`Updated template: ${template.title}`);
                }
            }
            
            console.log("Templates updated successfully");
        }

    } catch (error) {
        console.error("Error checking templates:", error);
    } finally {
        if (connection) {
            await mongoose.connection.close();
            console.log("\nDatabase connection closed");
        }
    }
}

// Run the check
checkAndFixTemplateIcons(); 