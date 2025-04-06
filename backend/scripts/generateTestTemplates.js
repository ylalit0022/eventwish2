/**
 * Script to generate 500 sample templates across available categories
 * for testing the recommendation algorithm
 */
const mongoose = require('mongoose');
const Template = require('../models/Template');
const CategoryIcon = require('../models/CategoryIcon');
const { faker } = require('@faker-js/faker');
require('dotenv').config({ path: require('path').resolve(__dirname, '../.env') });

// Connect to MongoDB
async function connectToMongoDB() {
    try {
        await mongoose.connect(process.env.MONGODB_URI, {
            useNewUrlParser: true,
            useUnifiedTopology: true
        });
        console.log('Connected to MongoDB');
    } catch (error) {
        console.error('Error connecting to MongoDB:', error);
        process.exit(1);
    }
}

// Define main categories used in the app
const CATEGORIES = [
    'Birthday',
    'Wedding',
    'Anniversary',
    'Graduation',
    'Religious',
    'National',
    'Cultural',
    'Holiday'
];

// Helper function to generate random HTML content
function generateHtmlContent(template, category) {
    return `<div class="${category.toLowerCase()}-template">
        <div class="header">
            <h1>${template.title}</h1>
        </div>
        <div class="content">
            <p>${faker.lorem.paragraph()}</p>
            <div class="decoration"></div>
            <p>${faker.lorem.paragraph()}</p>
        </div>
        <div class="footer">
            <p>${faker.lorem.sentence()}</p>
        </div>
    </div>`;
}

// Helper function to generate random CSS content
function generateCssContent(category) {
    const colors = {
        'Birthday': ['#ff9a9e', '#fad0c4', '#ffecd2'],
        'Wedding': ['#a1c4fd', '#c2e9fb', '#ffffff'],
        'Anniversary': ['#ff758c', '#ff7eb3', '#fdcbf1'],
        'Graduation': ['#4facfe', '#00f2fe', '#9face6'],
        'Religious': ['#f1f8ff', '#c8e1ff', '#e6fffa'],
        'National': ['#002868', '#bf0a30', '#ffffff'],
        'Cultural': ['#e63946', '#1d3557', '#f1faee'],
        'Holiday': ['#f8b195', '#f67280', '#c06c84']
    };
    
    const categoryColors = colors[category] || ['#f5f5f5', '#e0e0e0', '#bdbdbd'];
    
    return `.${category.toLowerCase()}-template {
        background: linear-gradient(135deg, ${categoryColors[0]}, ${categoryColors[1]});
        padding: 25px;
        border-radius: 12px;
        font-family: Arial, sans-serif;
        color: #333;
        max-width: 800px;
        margin: 0 auto;
        box-shadow: 0 4px 10px rgba(0,0,0,0.1);
    }
    .${category.toLowerCase()}-template .header {
        text-align: center;
        padding-bottom: 15px;
        border-bottom: 1px solid ${categoryColors[2]};
    }
    .${category.toLowerCase()}-template .content {
        padding: 20px 0;
    }
    .${category.toLowerCase()}-template .decoration {
        height: 30px;
        background: ${categoryColors[2]};
        margin: 20px 0;
        border-radius: 15px;
        opacity: 0.7;
    }
    .${category.toLowerCase()}-template .footer {
        text-align: center;
        font-style: italic;
        margin-top: 20px;
        border-top: 1px solid ${categoryColors[2]};
        padding-top: 15px;
    }`;
}

// Helper function to generate random JS content
function generateJsContent() {
    return `document.addEventListener('DOMContentLoaded', function() {
        // Simple animation for the decoration element
        const decoration = document.querySelector('.decoration');
        if (decoration) {
            decoration.addEventListener('click', function() {
                this.style.transform = this.style.transform === 'scale(1.2)' ? 'scale(1)' : 'scale(1.2)';
                this.style.transition = 'transform 0.5s ease';
            });
        }
        
        // Track when the template is viewed
        console.log('Template viewed at:', new Date().toISOString());
    });`;
}

// Helper function to generate a random date within the past year
function generateRandomDate() {
    const now = new Date();
    const oneYearAgo = new Date();
    oneYearAgo.setFullYear(now.getFullYear() - 1);
    
    return new Date(oneYearAgo.getTime() + Math.random() * (now.getTime() - oneYearAgo.getTime()));
}

// Generate 500 templates with proper category and categoryIcon relationships
async function generateTemplates() {
    try {
        // Get all existing category icons
        const categoryIcons = await CategoryIcon.find();
        console.log(`Found ${categoryIcons.length} category icons`);
        
        // Create a map of category name to icon ID
        const categoryIconMap = {};
        categoryIcons.forEach(icon => {
            categoryIconMap[icon.category] = icon._id;
        });
        
        // Check if we have icons for all categories
        for (const category of CATEGORIES) {
            if (!categoryIconMap[category]) {
                console.warn(`Warning: No category icon found for ${category}, creating one`);
                
                // Create a placeholder icon if missing
                const newIcon = new CategoryIcon({
                    category: category,
                    categoryIcon: `https://currentedu365.in/wp-content/uploads/2024/09/${category.toLowerCase()}-icon.png`
                });
                
                const savedIcon = await newIcon.save();
                categoryIconMap[category] = savedIcon._id;
                console.log(`Created new icon for ${category} with ID: ${savedIcon._id}`);
            }
        }
        
        // Generate 500 templates
        const templates = [];
        
        for (let i = 0; i < 500; i++) {
            // Select a random category with weighted distribution
            // More templates for popular categories
            let category;
            const rand = Math.random();
            
            if (rand < 0.20) {
                category = 'Birthday';
            } else if (rand < 0.35) {
                category = 'Wedding';
            } else if (rand < 0.50) {
                category = 'Anniversary';
            } else if (rand < 0.65) {
                category = 'Graduation';
            } else if (rand < 0.75) {
                category = 'Religious';
            } else if (rand < 0.85) {
                category = 'National';
            } else if (rand < 0.95) {
                category = 'Cultural';
            } else {
                category = 'Holiday';
            }
            
            // Generate a template with increasing ID numbers for easy identification
            const template = {
                title: `${category} Template ${i + 1}`,
                category: category,
                htmlContent: generateHtmlContent({ title: `${category} Template ${i + 1}` }, category),
                cssContent: generateCssContent(category),
                jsContent: generateJsContent(),
                previewUrl: `https://currentedu365.in/wp-content/uploads/2024/09/${category.toLowerCase()}-template-${(i % 5) + 1}.png`,
                status: true,
                categoryIcon: categoryIconMap[category]
            };
            
            templates.push(template);
        }
        
        // Create templates in batches to avoid overwhelming the database
        const BATCH_SIZE = 50;
        for (let i = 0; i < templates.length; i += BATCH_SIZE) {
            const batch = templates.slice(i, i + BATCH_SIZE);
            await Template.insertMany(batch);
            console.log(`Inserted templates ${i + 1} to ${Math.min(i + BATCH_SIZE, templates.length)}`);
        }
        
        console.log(`Successfully inserted ${templates.length} sample templates`);
    } catch (error) {
        console.error('Error generating templates:', error);
    }
}

// Function to verify templates were created correctly
async function verifyTemplates() {
    try {
        // Get count by category
        const categoryCounts = await Template.aggregate([
            { $group: { _id: "$category", count: { $sum: 1 } } },
            { $sort: { count: -1 } }
        ]);
        
        console.log("\nTemplate distribution by category:");
        categoryCounts.forEach(cat => {
            console.log(`${cat._id}: ${cat.count} templates`);
        });
        
        const totalCount = await Template.countDocuments();
        console.log(`\nTotal templates in database: ${totalCount}`);
        
        // Check for templates without category icons
        const missingIconCount = await Template.countDocuments({ categoryIcon: null });
        if (missingIconCount > 0) {
            console.warn(`Warning: ${missingIconCount} templates are missing category icon references`);
        } else {
            console.log("All templates have proper category icon references");
        }
    } catch (error) {
        console.error('Error verifying templates:', error);
    }
}

// Main function
async function main() {
    try {
        await connectToMongoDB();
        
        // Check if we should clear existing templates first
        const args = process.argv.slice(2);
        const shouldClear = args.includes('--clear');
        
        if (shouldClear) {
            console.log('Clearing existing templates...');
            await Template.deleteMany({});
            console.log('All templates cleared');
        }
        
        // Get current count
        const initialCount = await Template.countDocuments();
        console.log(`Current template count: ${initialCount}`);
        
        if (initialCount >= 500 && !shouldClear) {
            console.log('Database already has 500+ templates. Use --clear flag to replace them.');
            console.log('Running verification only...');
            await verifyTemplates();
        } else {
            // Generate new templates
            await generateTemplates();
            
            // Verify the templates were created correctly
            await verifyTemplates();
        }
        
        console.log('Process completed successfully!');
    } catch (error) {
        console.error('Error in main process:', error);
    } finally {
        // Close the database connection
        mongoose.connection.close();
        console.log('MongoDB connection closed');
    }
}

// Run the main function
main().catch(error => {
    console.error('Unhandled error:', error);
    process.exit(1);
}); 