const mongoose = require('mongoose');
const Template = require('../models/Template');
const CategoryIcon = require('../models/CategoryIcon');
require('dotenv').config({ path: require('path').resolve(__dirname, '../.env') });

async function verifyTemplateIcons() {
    let connection;
    try {
        // Connect to MongoDB
        console.log("Connecting to MongoDB...");
        connection = await mongoose.connect(process.env.MONGODB_URI, {
            useNewUrlParser: true,
            useUnifiedTopology: true
        });
        console.log("Connected to MongoDB successfully");

        // Get all templates with populated categoryIconObj
        const templates = await Template.find().populate('categoryIconObj');
        console.log(`Found ${templates.length} templates in database`);
        
        // Log details for each template
        templates.forEach((template, index) => {
            console.log(`\n[${index + 1}] Template: ${template.title}`);
            console.log(`- Category: ${template.category}`);
            console.log(`- CategoryIcon URL: ${template.categoryIcon}`);
            console.log(`- CategoryIconObj ID: ${template.categoryIconObj ? template.categoryIconObj._id : 'Not set'}`);
            
            if (template.categoryIconObj) {
                console.log(`- CategoryIconObj: Found`);
                console.log(`  - ID: ${template.categoryIconObj._id}`);
                console.log(`  - Category: ${template.categoryIconObj.category}`);
                console.log(`  - Icon URL: ${template.categoryIconObj.categoryIcon}`);
            } else {
                console.log(`- CategoryIconObj: Not found`);
            }
        });

    } catch (error) {
        console.error("Error verifying template icons:", error);
    } finally {
        if (connection) {
            await mongoose.connection.close();
            console.log("\nDatabase connection closed");
        }
    }
}

// Run the verification
verifyTemplateIcons(); 