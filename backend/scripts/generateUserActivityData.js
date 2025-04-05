/**
 * Script to generate simulated user activity data for testing the recommendation algorithm
 * Creates user profiles with varying patterns of category visits and template views
 */
const mongoose = require('mongoose');
const User = require('../models/User');
const Template = require('../models/Template');
const crypto = require('crypto');
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

// Define categories
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

// User profile types to generate
const USER_TYPES = [
    {
        name: 'Balanced',
        description: 'User who interacts with all categories relatively evenly',
        distribution: new Map(CATEGORIES.map(cat => [cat, 1/CATEGORIES.length])) // Equal distribution
    },
    {
        name: 'BirthdayFan',
        description: 'User who mostly focuses on birthday templates',
        distribution: new Map([
            ['Birthday', 0.6],  // 60% on birthday
            ['Anniversary', 0.1],
            ['Wedding', 0.1],
            ...CATEGORIES.filter(c => !['Birthday', 'Anniversary', 'Wedding'].includes(c))
                .map(cat => [cat, 0.2/5])  // Remaining 20% distributed among other 5 categories
        ])
    },
    {
        name: 'WeddingPlanner',
        description: 'User who focuses on wedding and anniversary content',
        distribution: new Map([
            ['Wedding', 0.5],
            ['Anniversary', 0.3],
            ...CATEGORIES.filter(c => !['Wedding', 'Anniversary'].includes(c))
                .map(cat => [cat, 0.2/6])  // Remaining 20% distributed among other 6 categories
        ])
    },
    {
        name: 'CulturalEnthusiast',
        description: 'User who focuses on cultural and religious content',
        distribution: new Map([
            ['Cultural', 0.4],
            ['Religious', 0.3],
            ['National', 0.1],
            ...CATEGORIES.filter(c => !['Cultural', 'Religious', 'National'].includes(c))
                .map(cat => [cat, 0.2/5])  // Remaining 20% distributed among other 5 categories
        ])
    },
    {
        name: 'Occasional',
        description: 'User who only occasionally uses the app with few interactions',
        distribution: new Map(CATEGORIES.map(cat => [cat, 1/CATEGORIES.length])), // Equal but few
        lowActivity: true
    }
];

// Helper function to generate template interactions and category visits for a user
async function generateUserActivity(userType, templates) {
    // Generate a deterministic but random-looking device ID
    const deviceId = crypto.createHash('sha256')
        .update(`${userType.name}-${Date.now()}-${Math.random()}`)
        .digest('hex');
    
    // Create base user
    const user = new User({
        deviceId,
        lastOnline: new Date(),
        created: new Date(Date.now() - Math.random() * 30 * 24 * 60 * 60 * 1000), // Random time in last 30 days
        categories: []
    });
    
    // Determine how many interactions to create based on user type
    const interactionCount = userType.lowActivity ? 
        Math.floor(5 + Math.random() * 10) : // 5-15 interactions for low activity
        Math.floor(50 + Math.random() * 150); // 50-200 interactions for regular users
    
    console.log(`Generating ${interactionCount} interactions for ${userType.name} user`);
    
    // Create interactions based on the distribution
    const interactions = [];
    
    for (let i = 0; i < interactionCount; i++) {
        // Pick a category based on the user's distribution
        const category = pickRandomCategory(userType.distribution);
        
        // Find templates matching this category
        const categoryTemplates = templates.filter(t => t.category === category);
        
        if (categoryTemplates.length === 0) {
            console.warn(`No templates found for category ${category}, skipping interaction`);
            continue;
        }
        
        // Pick a random template from this category
        const template = categoryTemplates[Math.floor(Math.random() * categoryTemplates.length)];
        
        // Generate a random date in the past 30 days, with more recent ones being more likely
        const recencyBias = Math.pow(Math.random(), 0.5); // Bias toward more recent dates
        const date = new Date(Date.now() - recencyBias * 30 * 24 * 60 * 60 * 1000);
        
        // Determine if this is a direct category visit or a template view
        const isTemplateView = Math.random() < 0.7; // 70% chance of being a template view
        
        const interaction = {
            category,
            visitDate: date,
            visitCount: 1,
            source: isTemplateView ? 'template' : 'direct'
        };
        
        interactions.push(interaction);
    }
    
    // Sort interactions by date, oldest first
    interactions.sort((a, b) => a.visitDate - b.visitDate);
    
    // Combine identical categories and count them properly
    const categoryMap = new Map();
    
    interactions.forEach(interaction => {
        const key = `${interaction.category}-${interaction.source}`;
        
        if (categoryMap.has(key)) {
            const existing = categoryMap.get(key);
            existing.visitCount++;
            // Update to most recent date
            if (interaction.visitDate > existing.visitDate) {
                existing.visitDate = interaction.visitDate;
            }
        } else {
            categoryMap.set(key, { ...interaction });
        }
    });
    
    // Add to user's categories
    user.categories = Array.from(categoryMap.values());
    
    return user;
}

// Helper function to pick a random category based on a distribution
function pickRandomCategory(distribution) {
    const rand = Math.random();
    let cumulativeProbability = 0;
    
    for (const [category, probability] of distribution.entries()) {
        cumulativeProbability += probability;
        if (rand < cumulativeProbability) {
            return category;
        }
    }
    
    // Fallback to a random category if something goes wrong with probabilities
    return CATEGORIES[Math.floor(Math.random() * CATEGORIES.length)];
}

// Generate users of each type
async function generateUsers() {
    try {
        // Get templates from the database
        const templates = await Template.find({}, { category: 1 }).lean();
        console.log(`Found ${templates.length} templates`);
        
        if (templates.length === 0) {
            console.error('No templates found in the database. Please run generateTestTemplates.js first.');
            return;
        }
        
        const usersToCreate = [];
        
        // Create 10 users of each type
        for (const userType of USER_TYPES) {
            console.log(`Generating 10 users of type: ${userType.name}`);
            
            for (let i = 0; i < 10; i++) {
                const user = await generateUserActivity(userType, templates);
                usersToCreate.push(user);
            }
        }
        
        // Create users in the database
        console.log(`\nSaving ${usersToCreate.length} users to the database...`);
        await User.insertMany(usersToCreate);
        
        console.log(`Successfully created ${usersToCreate.length} users with simulated activity`);
    } catch (error) {
        console.error('Error generating users:', error);
    }
}

// Function to verify user data
async function verifyUserData() {
    try {
        // Count users by activity pattern
        const userCount = await User.countDocuments();
        console.log(`\nTotal users in database: ${userCount}`);
        
        // Get average interactions per user
        const stats = await User.aggregate([
            { $project: { numCategories: { $size: "$categories" } } },
            { $group: { _id: null, avg: { $avg: "$numCategories" } } }
        ]);
        
        if (stats.length > 0) {
            console.log(`Average interactions per user: ${Math.round(stats[0].avg)}`);
        }
        
        // Find users with most and least interactions
        const mostActive = await User.aggregate([
            { $project: { deviceId: 1, numCategories: { $size: "$categories" } } },
            { $sort: { numCategories: -1 } },
            { $limit: 1 }
        ]);
        
        const leastActive = await User.aggregate([
            { $project: { deviceId: 1, numCategories: { $size: "$categories" } } },
            { $sort: { numCategories: 1 } },
            { $limit: 1 }
        ]);
        
        if (mostActive.length > 0) {
            console.log(`Most active user has ${mostActive[0].numCategories} interactions`);
        }
        
        if (leastActive.length > 0) {
            console.log(`Least active user has ${leastActive[0].numCategories} interactions`);
        }
        
        // Get category distribution
        const categoryStats = await User.aggregate([
            { $unwind: "$categories" },
            { $group: { _id: "$categories.category", count: { $sum: 1 } } },
            { $sort: { count: -1 } }
        ]);
        
        console.log("\nCategory interaction distribution:");
        categoryStats.forEach(cat => {
            console.log(`${cat._id}: ${cat.count} interactions`);
        });
        
        // Get source distribution
        const sourceStats = await User.aggregate([
            { $unwind: "$categories" },
            { $group: { _id: "$categories.source", count: { $sum: 1 } } },
            { $sort: { count: -1 } }
        ]);
        
        console.log("\nInteraction source distribution:");
        sourceStats.forEach(source => {
            console.log(`${source._id}: ${source.count} interactions`);
        });
    } catch (error) {
        console.error('Error verifying user data:', error);
    }
}

// Main function
async function main() {
    try {
        await connectToMongoDB();
        
        // Check if we should clear existing users first
        const args = process.argv.slice(2);
        const shouldClear = args.includes('--clear');
        
        if (shouldClear) {
            console.log('Clearing existing user activity data...');
            await User.deleteMany({});
            console.log('All user activity data cleared');
        }
        
        // Get current count
        const initialCount = await User.countDocuments();
        console.log(`Current user count: ${initialCount}`);
        
        if (initialCount >= 50 && !shouldClear) {
            console.log('Database already has 50+ users. Use --clear flag to replace them.');
            console.log('Running verification only...');
            await verifyUserData();
        } else {
            // Generate users
            await generateUsers();
            
            // Verify the user data
            await verifyUserData();
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