const mongoose = require('mongoose');
const fs = require('fs');
require('dotenv').config({ path: require('path').resolve(__dirname, '../.env') });

// Try to load the CategoryIcon model
let CategoryIcon;
try {
    CategoryIcon = require('../models/CategoryIcon');
    console.log('CategoryIcon model loaded successfully');
} catch (error) {
    console.error('Error loading CategoryIcon model:', error.message);
    // Create a temporary schema
    const categoryIconSchema = new mongoose.Schema({
        category: String,
        categoryIcon: String,
        iconType: String,
        resourceName: String
    });
    CategoryIcon = mongoose.model('CategoryIcon', categoryIconSchema, 'categoryicons');
    console.log('Created temporary CategoryIcon model');
}

async function checkCategoryIcons() {
    let connection;
    try {
        // Connect to MongoDB
        console.log("Connecting to MongoDB...");
        connection = await mongoose.connect(process.env.MONGODB_URI, {
            useNewUrlParser: true,
            useUnifiedTopology: true
        });
        console.log("Connected to MongoDB successfully");

        // Get all category icons
        const categoryIcons = await CategoryIcon.find();
        console.log(`Found ${categoryIcons.length} category icons in database`);
        
        // Log details for each category icon
        categoryIcons.forEach((icon, index) => {
            console.log(`\n[${index + 1}] Category Icon: ${icon.category || 'unnamed'}`);
            console.log(`- ID: ${icon._id}`);
            console.log(`- Category: ${icon.category || 'N/A'}`);
            console.log(`- Icon URL: ${icon.categoryIcon || 'N/A'}`);
            console.log(`- Icon Type: ${icon.iconType || 'N/A'}`);
            console.log(`- Resource Name: ${icon.resourceName || 'N/A'}`);
        });

        // Now check templates to see how they reference categoryIcons
        const Template = require('../models/Template');
        const templates = await Template.find().limit(5); // Get just a few templates for analysis
        
        console.log('\nAnalyzing template categoryIcon references:');
        templates.forEach((template, index) => {
            console.log(`\n[${index + 1}] Template: ${template.title}`);
            console.log(`- CategoryIcon: ${typeof template.categoryIcon === 'string' ? template.categoryIcon : JSON.stringify(template.categoryIcon) || 'N/A'}`);
            console.log(`- Category: ${template.category || 'N/A'}`);
        });

    } catch (error) {
        console.error("Error checking category icons:", error);
    } finally {
        if (connection) {
            await mongoose.connection.close();
            console.log("\nDatabase connection closed");
        }
    }
}

// Run the check
checkCategoryIcons(); 