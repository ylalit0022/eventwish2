const mongoose = require('mongoose');
const fs = require('fs');
require('dotenv').config({ path: require('path').resolve(__dirname, '../.env') });

// Check which models exist
const modelFiles = fs.readdirSync(require('path').resolve(__dirname, '../models'));
console.log('Available model files:', modelFiles);

// Try to load the Category model if it exists
let Category;
try {
    Category = require('../models/Category');
    console.log('Category model loaded successfully');
} catch (error) {
    console.error('Error loading Category model:', error.message);
    
    // Create a temporary schema if needed
    const categorySchema = new mongoose.Schema({
        name: String,
        displayName: String,
        description: String,
        displayOrder: Number,
        icon: String,
        isVisible: Boolean
    });
    Category = mongoose.model('Category', categorySchema, 'categories');
    console.log('Created temporary Category model');
}

async function checkCategories() {
    let connection;
    try {
        // Connect to MongoDB
        console.log("Connecting to MongoDB...");
        connection = await mongoose.connect(process.env.MONGODB_URI, {
            useNewUrlParser: true,
            useUnifiedTopology: true
        });
        console.log("Connected to MongoDB successfully");

        // Check if categories collection exists
        const collections = await mongoose.connection.db.listCollections().toArray();
        const collectionNames = collections.map(c => c.name);
        console.log('Available collections:', collectionNames);
        
        if (!collectionNames.includes('categories')) {
            console.log('Categories collection does not exist!');
            return;
        }

        // Get all categories
        const categories = await Category.find();
        console.log(`Found ${categories.length} categories in database`);
        
        // Log details for each category
        categories.forEach((category, index) => {
            console.log(`\n[${index + 1}] Category: ${category.name || 'unnamed'}`);
            console.log(`- ID: ${category._id}`);
            console.log(`- Display Name: ${category.displayName || 'N/A'}`);
            console.log(`- Display Order: ${category.displayOrder || 'N/A'}`);
            console.log(`- Visible: ${category.isVisible !== false ? 'Yes' : 'No'}`);
            
            // Check icon field
            if (category.icon) {
                console.log(`- Icon: ${typeof category.icon === 'string' ? category.icon : JSON.stringify(category.icon)}`);
            } else {
                console.log('- Icon: Not set');
            }
        });

        // Also check templates to see what categories are referenced
        const Template = require('../models/Template');
        const templates = await Template.find();
        
        console.log(`\nFound ${templates.length} templates`);
        
        // Create a map of category usage in templates
        const categoryUsage = {};
        templates.forEach(template => {
            const category = template.category;
            if (!categoryUsage[category]) {
                categoryUsage[category] = 0;
            }
            categoryUsage[category]++;
        });
        
        console.log('\nCategory usage in templates:');
        Object.entries(categoryUsage).forEach(([category, count]) => {
            console.log(`- ${category}: ${count} template(s)`);
        });

    } catch (error) {
        console.error("Error checking categories:", error);
    } finally {
        if (connection) {
            await mongoose.connection.close();
            console.log("\nDatabase connection closed");
        }
    }
}

// Run the check
checkCategories(); 