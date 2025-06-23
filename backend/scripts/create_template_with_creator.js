#!/usr/bin/env node

/**
 * Template Creator Upload Script
 * 
 * This script creates templates with real creator UID information
 * Usage: node scripts/create_template_with_creator.js
 */

require('dotenv').config();
const mongoose = require('mongoose');
const Template = require('../models/Template');
const User = require('../models/User');
const logger = require('../utils/logger');

// Database connection
const connectDB = async () => {
    try {
        const mongoURI = process.env.MONGODB_URI;
        if (!mongoURI) {
            throw new Error('MONGODB_URI environment variable is not set');
        }
        
        console.log('🔄 Connecting to MongoDB...');
        await mongoose.connect(mongoURI);
        console.log('✅ MongoDB Connected for template creation');
    } catch (error) {
        console.error('❌ MongoDB connection failed:', error.message);
        process.exit(1);
    }
};

// Sample creator users with real Firebase UIDs
const sampleCreators = [
    {
        uid: 'firebase_uid_sarah_12345',
        displayName: 'Sarah Johnson',
        email: 'sarah.johnson@example.com',
        profilePhoto: 'https://i.pravatar.cc/150?img=1',
        deviceId: 'device_sarah_001',
        lastOnline: new Date(),
        subscription: {
            status: 'premium',
            type: 'monthly',
            startDate: new Date(),
            endDate: new Date(Date.now() + 30 * 24 * 60 * 60 * 1000) // 30 days from now
        }
    },
    {
        uid: 'firebase_uid_mike_67890',
        displayName: 'Mike Chen',
        email: 'mike.chen@example.com',
        profilePhoto: 'https://i.pravatar.cc/150?img=2',
        deviceId: 'device_mike_002',
        lastOnline: new Date(),
        subscription: {
            status: 'free',
            type: 'free'
        }
    },
    {
        uid: 'firebase_uid_alex_54321',
        displayName: 'Alex Rodriguez',
        email: 'alex.rodriguez@example.com',
        profilePhoto: 'https://i.pravatar.cc/150?img=3',
        deviceId: 'device_alex_003',
        lastOnline: new Date(),
        subscription: {
            status: 'premium',
            type: 'yearly',
            startDate: new Date(),
            endDate: new Date(Date.now() + 365 * 24 * 60 * 60 * 1000) // 1 year from now
        }
    },
    {
        uid: 'firebase_uid_emma_98765',
        displayName: 'Emma Wilson',
        email: 'emma.wilson@example.com',
        profilePhoto: 'https://i.pravatar.cc/150?img=4',
        deviceId: 'device_emma_004',
        lastOnline: new Date(),
        subscription: {
            status: 'free',
            type: 'free'
        }
    },
    {
        uid: 'firebase_uid_david_11111',
        displayName: 'David Kim',
        email: 'david.kim@example.com',
        profilePhoto: 'https://i.pravatar.cc/150?img=5',
        deviceId: 'device_david_005',
        lastOnline: new Date(),
        subscription: {
            status: 'premium',
            type: 'monthly',
            startDate: new Date(),
            endDate: new Date(Date.now() + 30 * 24 * 60 * 60 * 1000)
        }
    }
];

// Sample templates with comprehensive data
const sampleTemplates = [
    {
        title: "Birthday Celebration Wishes",
        category: "birthday",
        htmlContent: `<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Birthday Celebration</title>
    <style>
        body { font-family: 'Arial', sans-serif; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); margin: 0; padding: 20px; }
        .card { background: white; border-radius: 15px; padding: 30px; max-width: 500px; margin: 0 auto; box-shadow: 0 10px 30px rgba(0,0,0,0.3); }
        .header { text-align: center; color: #667eea; font-size: 2.5em; margin-bottom: 20px; }
        .message { font-size: 1.2em; line-height: 1.6; color: #333; text-align: center; }
        .recipient { color: #764ba2; font-weight: bold; }
        .sender { margin-top: 30px; text-align: right; color: #667eea; font-style: italic; }
    </style>
</head>
<body>
    <div class="card">
        <div class="header">🎉 Happy Birthday! 🎂</div>
        <div class="message">
            <p>Dear <span class="recipient">[Recipient Name]</span>,</p>
            <p>Wishing you a day filled with happiness and a year filled with joy! May all your dreams come true on this special day.</p>
            <p>Hope your birthday is as amazing as you are!</p>
        </div>
        <div class="sender">
            With love,<br>
            <strong>[Sender Name]</strong>
        </div>
    </div>
</body>
</html>`,
        cssContent: "",
        jsContent: "",
        previewUrl: "https://blogger.googleusercontent.com/img/b/R29vZ2xl/AVvXsEjA6AHGVtn4EP_MGtO60i7VMfPQieJmtJg3roHunj-7nd76bhLIap71jddY5L-af4Z6b9BDQcxFqJsQz5yfgntFXpg-JH5yEsyFd3odImyFsRuGoHoIfmLsLdritDb7BVBj0C9vfeQZVfQ8wzPZ6CPN7xBk5ZwL6n2n7PkUAjz-zJAw6golqTKjLcaB5bGb/s1280/birthday_celebration_card.jpeg",
        tags: ["birthday", "celebration", "wishes", "party"],
        styleTags: ["colorful", "modern", "elegant"],
        searchKeywords: ["birthday", "celebration", "wishes", "party", "happy"],
        festivalTag: "",
        isPremium: false,
        isFeatured: true,
        templateType: "html"
    },
    {
        title: "Good Morning Sunshine",
        category: "good morning",
        htmlContent: `<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Good Morning</title>
    <style>
        body { font-family: 'Georgia', serif; background: linear-gradient(45deg, #ffeaa7, #fdcb6e); margin: 0; padding: 20px; }
        .card { background: rgba(255,255,255,0.95); border-radius: 20px; padding: 40px; max-width: 400px; margin: 0 auto; box-shadow: 0 15px 35px rgba(0,0,0,0.2); }
        .header { text-align: center; color: #e17055; font-size: 3em; margin-bottom: 25px; }
        .message { font-size: 1.1em; line-height: 1.7; color: #2d3436; text-align: center; }
        .highlight { color: #e17055; font-weight: bold; }
        .footer { margin-top: 25px; text-align: center; color: #636e72; font-style: italic; }
    </style>
</head>
<body>
    <div class="card">
        <div class="header">🌅 Good Morning! ☀️</div>
        <div class="message">
            <p>Hello <span class="highlight">[Recipient Name]</span>,</p>
            <p>Rise and shine! May your day be filled with beautiful moments and endless possibilities.</p>
            <p>Have a wonderful day ahead!</p>
        </div>
        <div class="footer">
            Warm wishes,<br>
            <strong>[Sender Name]</strong>
        </div>
    </div>
</body>
</html>`,
        cssContent: "",
        jsContent: "",
        previewUrl: "https://blogger.googleusercontent.com/img/b/R29vZ2xl/AVvXsEjRM79XmhA42KonqWpv74YE8iMgtlf7ikXpnE3U2b4uGv-KHYxmWXv6TuDDewhTdhlFx_SEP_KRoHkTAsgZb0EGvUd8mPWDHKZ-l8Chn1yz3lXcYWIog3K1D2ViUnCdW-UrNvMumvU5fwAZm8J7kG7UVUs5PZIVoZGwufOk5TEzaXF1yDbugIRU6KvwbwCg/s1256/good_morning_sunshine.jpeg",
        tags: ["good morning", "sunshine", "greeting", "motivational"],
        styleTags: ["warm", "cheerful", "bright"],
        searchKeywords: ["good morning", "sunrise", "greeting", "day", "sunshine"],
        festivalTag: "",
        isPremium: false,
        isFeatured: false,
        templateType: "html"
    },
    {
        title: "Raksha Bandhan Special",
        category: "Raksha Bandhan",
        htmlContent: `<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Raksha Bandhan</title>
    <style>
        body { font-family: 'Verdana', sans-serif; background: linear-gradient(135deg, #ff7675, #fd79a8); margin: 0; padding: 20px; }
        .card { background: #fff; border-radius: 25px; padding: 35px; max-width: 450px; margin: 0 auto; box-shadow: 0 20px 40px rgba(0,0,0,0.3); border: 3px solid #fdcb6e; }
        .header { text-align: center; color: #e84393; font-size: 2.8em; margin-bottom: 30px; text-shadow: 2px 2px 4px rgba(0,0,0,0.1); }
        .message { font-size: 1.15em; line-height: 1.8; color: #2d3436; text-align: center; }
        .special { color: #e84393; font-weight: bold; text-decoration: underline; }
        .blessing { margin: 20px 0; padding: 15px; background: #ffeaa7; border-radius: 10px; font-style: italic; }
        .signature { margin-top: 30px; text-align: right; color: #e84393; font-weight: bold; }
    </style>
</head>
<body>
    <div class="card">
        <div class="header">🎀 Raksha Bandhan 🎀</div>
        <div class="message">
            <p>Dear <span class="special">[Brother/Sister Name]</span>,</p>
            <div class="blessing">
                On this sacred festival of Raksha Bandhan, I pray for your happiness, success, and prosperity. May our bond of love grow stronger with each passing day.
            </div>
            <p>You are not just my sibling, but my best friend and protector. Thank you for always being there for me!</p>
        </div>
        <div class="signature">
            With endless love,<br>
            <strong>[Your Name]</strong>
        </div>
    </div>
</body>
</html>`,
        cssContent: "",
        jsContent: "",
        previewUrl: "https://blogger.googleusercontent.com/img/b/R29vZ2xl/AVvXsEi8YnyYdewYpPNCXWiNU5OlMiidc3O_SzFZsjFS6aRPvseUV0v2iaLBUPbrJWwOyXaqTYL0RCrfF2aI1bNOrMJvXpz1deSgRaPHbLss3G7Hj4PYZ0Xs8QAT3gNB2UjGHudteyjuTXHClPrn03BeSQYnrceIisw2vPfBqy8QoxkjYfCYNC_isw3uffOwBr5e/s320/raksha_bandhan_special.jpeg",
        tags: ["raksha bandhan", "festival", "brother", "sister", "family"],
        styleTags: ["traditional", "colorful", "festive"],
        searchKeywords: ["raksha bandhan", "festival", "brother", "sister", "family", "bond"],
        festivalTag: "Raksha Bandhan",
        isPremium: true,
        isFeatured: true,
        templateType: "html"
    },
    {
        title: "Thank You Card",
        category: "thank you",
        htmlContent: `<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Thank You</title>
    <style>
        body { font-family: 'Times New Roman', serif; background: linear-gradient(120deg, #a8edea, #fed6e3); margin: 0; padding: 20px; }
        .card { background: #fff; border-radius: 15px; padding: 30px; max-width: 420px; margin: 0 auto; box-shadow: 0 12px 25px rgba(0,0,0,0.2); border-left: 5px solid #00b894; }
        .header { text-align: center; color: #00b894; font-size: 2.5em; margin-bottom: 25px; }
        .message { font-size: 1.1em; line-height: 1.6; color: #2d3436; }
        .gratitude { color: #00b894; font-weight: bold; }
        .closing { margin-top: 25px; text-align: center; color: #636e72; font-style: italic; }
    </style>
</head>
<body>
    <div class="card">
        <div class="header">🙏 Thank You 💝</div>
        <div class="message">
            <p>Dear <span class="gratitude">[Recipient Name]</span>,</p>
            <p>I wanted to take a moment to express my heartfelt gratitude for everything you've done. Your kindness and support mean the world to me.</p>
            <p>Thank you for being such an amazing person!</p>
        </div>
        <div class="closing">
            With sincere appreciation,<br>
            <strong>[Your Name]</strong>
        </div>
    </div>
</body>
</html>`,
        cssContent: "",
        jsContent: "",
        previewUrl: "https://blogger.googleusercontent.com/img/b/R29vZ2xl/AVvXsEjA6AHGVtn4EP_MGtO60i7VMfPQieJmtJg3roHunj-7nd76bhLIap71jddY5L-af4Z6b9BDQcxFqJsQz5yfgntFXpg-JH5yEsyFd3odImyFsRuGoHoIfmLsLdritDb7BVBj0C9vfeQZVfQ8wzPZ6CPN7xBk5ZwL6n2n7PkUAjz-zJAw6golqTKjLcaB5bGb/s1280/thank_you_card.jpeg",
        tags: ["thank you", "gratitude", "appreciation", "thanks"],
        styleTags: ["elegant", "simple", "heartfelt"],
        searchKeywords: ["thank you", "gratitude", "appreciation", "thanks", "grateful"],
        festivalTag: "",
        isPremium: false,
        isFeatured: false,
        templateType: "html"
    },
    {
        title: "Wedding Anniversary Celebration",
        category: "anniversary",
        htmlContent: `<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Wedding Anniversary</title>
    <style>
        body { font-family: 'Palatino', serif; background: linear-gradient(45deg, #ffeaa7, #fab1a0); margin: 0; padding: 20px; }
        .card { background: linear-gradient(135deg, #fff, #f8f9fa); border-radius: 20px; padding: 40px; max-width: 500px; margin: 0 auto; box-shadow: 0 15px 30px rgba(0,0,0,0.3); border: 2px solid #fdcb6e; }
        .header { text-align: center; color: #e17055; font-size: 2.8em; margin-bottom: 30px; text-shadow: 1px 1px 2px rgba(0,0,0,0.1); }
        .message { font-size: 1.2em; line-height: 1.7; color: #2d3436; text-align: center; }
        .couple { color: #e17055; font-weight: bold; font-size: 1.1em; }
        .verse { margin: 25px 0; padding: 20px; background: rgba(253, 203, 110, 0.2); border-radius: 15px; font-style: italic; border-left: 4px solid #fdcb6e; }
        .wishes { margin-top: 30px; text-align: right; color: #e17055; font-weight: bold; }
    </style>
</head>
<body>
    <div class="card">
        <div class="header">💍 Happy Anniversary! 💕</div>
        <div class="message">
            <p>Dear <span class="couple">[Couple Names]</span>,</p>
            <div class="verse">
                "Love is not about how many days, months, or years you have been together. Love is about how much you love each other every single day."
            </div>
            <p>Congratulations on another year of love, laughter, and beautiful memories together. May your bond continue to grow stronger with each passing year!</p>
        </div>
        <div class="wishes">
            Best wishes for many more years of happiness,<br>
            <strong>[Your Name]</strong>
        </div>
    </div>
</body>
</html>`,
        cssContent: "",
        jsContent: "",
        previewUrl: "https://blogger.googleusercontent.com/img/b/R29vZ2xl/AVvXsEjRM79XmhA42KonqWpv74YE8iMgtlf7ikXpnE3U2b4uGv-KHYxmWXv6TuDDewhTdhlFx_SEP_KRoHkTAsgZb0EGvUd8mPWDHKZ-l8Chn1yz3lXcYWIog3K1D2ViUnCdW-UrNvMumvU5fwAZm8J7kG7UVUs5PZIVoZGwufOk5TEzaXF1yDbugIRU6KvwbwCg/s1256/wedding_anniversary.jpeg",
        tags: ["anniversary", "wedding", "love", "celebration", "couple"],
        styleTags: ["romantic", "elegant", "classic"],
        searchKeywords: ["anniversary", "wedding", "love", "celebration", "couple", "marriage"],
        festivalTag: "",
        isPremium: true,
        isFeatured: false,
        templateType: "html"
    }
];

// Create or update users
const createUsers = async () => {
    console.log('\n🔄 Creating/updating creator users...');
    const createdUsers = [];
    
    for (const userData of sampleCreators) {
        try {
            // Check if user already exists
            let user = await User.findOne({ uid: userData.uid });
            
            if (user) {
                console.log(`✅ User ${userData.displayName} (${userData.uid}) already exists`);
                createdUsers.push(user);
            } else {
                // Create new user
                user = new User(userData);
                await user.save();
                console.log(`✅ Created user: ${userData.displayName} (${userData.uid})`);
                createdUsers.push(user);
            }
        } catch (error) {
            console.error(`❌ Error creating user ${userData.displayName}:`, error.message);
        }
    }
    
    return createdUsers;
};

// Create templates with creator information
const createTemplates = async (users) => {
    console.log('\n🔄 Creating templates with creator information...');
    const createdTemplates = [];
    
    for (let i = 0; i < sampleTemplates.length; i++) {
        const templateData = sampleTemplates[i];
        const creator = users[i % users.length]; // Cycle through creators
        
        try {
            // Check if template already exists
            const existingTemplate = await Template.findOne({ 
                title: templateData.title,
                category: templateData.category 
            });
            
            if (existingTemplate) {
                console.log(`⚠️  Template "${templateData.title}" already exists, skipping...`);
                continue;
            }
            
            // Create template with creator information
            const template = new Template({
                ...templateData,
                // Creator information (both ObjectId and UID)
                creatorId: creator._id,
                creatorUid: creator.uid,
                generatedByUser: creator._id,
                generatedByUserUid: creator.uid,
                
                // Template metadata
                status: true,
                moderationStatus: 'approved',
                price: templateData.isPremium ? 99 : 0,
                
                // Engagement metrics (simulate some activity)
                usageCount: Math.floor(Math.random() * 100),
                likes: Math.floor(Math.random() * 50),
                favorites: Math.floor(Math.random() * 30),
                viewCount: Math.floor(Math.random() * 200),
                sharedCount: Math.floor(Math.random() * 25),
                downloadCount: Math.floor(Math.random() * 75),
                rating: Math.floor(Math.random() * 5) + 1,
                ratingCount: Math.floor(Math.random() * 20),
                
                // Timestamps
                createdAt: new Date(Date.now() - Math.floor(Math.random() * 30) * 24 * 60 * 60 * 1000), // Random date in last 30 days
                updatedAt: new Date()
            });
            
            await template.save();
            console.log(`✅ Created template: "${templateData.title}" by ${creator.displayName} (${creator.uid})`);
            createdTemplates.push(template);
            
        } catch (error) {
            console.error(`❌ Error creating template "${templateData.title}":`, error.message);
        }
    }
    
    return createdTemplates;
};

// Display summary
const displaySummary = (users, templates) => {
    console.log('\n📊 CREATION SUMMARY');
    console.log('===================');
    console.log(`👥 Users created/updated: ${users.length}`);
    console.log(`📄 Templates created: ${templates.length}`);
    console.log('\n👥 Creator Details:');
    
    users.forEach(user => {
        const userTemplates = templates.filter(t => t.creatorUid === user.uid);
        console.log(`   • ${user.displayName} (${user.uid}): ${userTemplates.length} templates`);
    });
    
    console.log('\n📄 Template Details:');
    templates.forEach(template => {
        const creator = users.find(u => u.uid === template.creatorUid);
        console.log(`   • "${template.title}" by ${creator?.displayName || 'Unknown'} (${template.category})`);
    });
    
    console.log('\n🔗 API Testing Commands:');
    if (templates.length > 0) {
        const sampleTemplate = templates[0];
        console.log(`   • Single creator profile: curl -X GET "http://localhost:3001/api/templates/${sampleTemplate._id}/creator"`);
        console.log(`   • Batch creator profiles: curl -X POST "http://localhost:3001/api/templates/creators/batch" -H "Content-Type: application/json" -d '{"templateIds": ["${sampleTemplate._id}"]}'`);
    }
};

// Main execution function
const main = async () => {
    try {
        console.log('🚀 Starting Template Creator Upload Script');
        console.log('==========================================');
        
        // Connect to database
        await connectDB();
        
        // Create users
        const users = await createUsers();
        if (users.length === 0) {
            throw new Error('No users were created or found');
        }
        
        // Create templates
        const templates = await createTemplates(users);
        
        // Display summary
        displaySummary(users, templates);
        
        console.log('\n✅ Script completed successfully!');
        console.log('🎯 Templates with real creator UID information have been created.');
        console.log('📱 You can now test the creator profile endpoints.');
        
    } catch (error) {
        console.error('\n❌ Script failed:', error.message);
        console.error('Stack trace:', error.stack);
    } finally {
        // Close database connection
        await mongoose.connection.close();
        console.log('\n🔌 Database connection closed');
        process.exit(0);
    }
};

// Run the script
if (require.main === module) {
    main();
}

module.exports = { main, createUsers, createTemplates }; 