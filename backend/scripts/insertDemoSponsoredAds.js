const mongoose = require('mongoose');
const SponsoredAd = require('../models/SponsoredAd');
require('dotenv').config();

// Demo ad data with different priorities and locations
const demoAds = [
    // High Priority Ads (8-10)
    {
        image_url: "https://example.com/ads/premium_festival.jpg",
        redirect_url: "https://example.com/premium-festival-offer",
        status: true,
        start_date: new Date(),
        end_date: new Date(Date.now() + 30 * 24 * 60 * 60 * 1000), // 30 days from now
        location: "home_top",
        priority: 10,
        frequency_cap: 10,
        daily_frequency_cap: 3,
        title: "Premium Festival Special",
        description: "Exclusive festival offers just for you!"
    },
    {
        image_url: "https://example.com/ads/new_year.jpg",
        redirect_url: "https://example.com/new-year-special",
        status: true,
        start_date: new Date(),
        end_date: new Date(Date.now() + 15 * 24 * 60 * 60 * 1000), // 15 days from now
        location: "home_bottom",
        priority: 9,
        frequency_cap: 8,
        daily_frequency_cap: 2,
        title: "New Year Celebration",
        description: "Start your year with amazing deals!"
    },
    {
        image_url: "https://example.com/ads/premium_birthday.jpg",
        redirect_url: "https://example.com/premium-birthday",
        status: true,
        start_date: new Date(),
        end_date: new Date(Date.now() + 45 * 24 * 60 * 60 * 1000), // 45 days from now
        location: "category_below",
        priority: 8,
        frequency_cap: 12,
        daily_frequency_cap: 3,
        title: "Premium Birthday Collection",
        description: "Make birthdays extra special!"
    },

    // Medium Priority Ads (4-7)
    {
        image_url: "https://example.com/ads/wedding.jpg",
        redirect_url: "https://example.com/wedding-collection",
        status: true,
        start_date: new Date(),
        end_date: new Date(Date.now() + 60 * 24 * 60 * 60 * 1000), // 60 days from now
        location: "home_top",
        priority: 7,
        frequency_cap: 15,
        daily_frequency_cap: 4,
        title: "Wedding Collection",
        description: "Beautiful wedding wishes and templates"
    },
    {
        image_url: "https://example.com/ads/anniversary.jpg",
        redirect_url: "https://example.com/anniversary-wishes",
        status: true,
        start_date: new Date(),
        end_date: new Date(Date.now() + 30 * 24 * 60 * 60 * 1000),
        location: "home_bottom",
        priority: 6,
        frequency_cap: 20,
        daily_frequency_cap: 5,
        title: "Anniversary Special",
        description: "Celebrate love with perfect wishes"
    },
    {
        image_url: "https://example.com/ads/graduation.jpg",
        redirect_url: "https://example.com/graduation-wishes",
        status: true,
        start_date: new Date(),
        end_date: new Date(Date.now() + 45 * 24 * 60 * 60 * 1000),
        location: "category_below",
        priority: 5,
        frequency_cap: 18,
        daily_frequency_cap: 4,
        title: "Graduation Wishes",
        description: "Congratulate the graduates!"
    },

    // Low Priority Ads (1-3)
    {
        image_url: "https://example.com/ads/general_festival.jpg",
        redirect_url: "https://example.com/festival-wishes",
        status: true,
        start_date: new Date(),
        end_date: new Date(Date.now() + 90 * 24 * 60 * 60 * 1000), // 90 days from now
        location: "home_top",
        priority: 3,
        frequency_cap: 25,
        daily_frequency_cap: 6,
        title: "Festival Wishes",
        description: "Celebrate festivals with joy"
    },
    {
        image_url: "https://example.com/ads/general_birthday.jpg",
        redirect_url: "https://example.com/birthday-wishes",
        status: true,
        start_date: new Date(),
        end_date: new Date(Date.now() + 60 * 24 * 60 * 60 * 1000),
        location: "home_bottom",
        priority: 2,
        frequency_cap: 30,
        daily_frequency_cap: 7,
        title: "Birthday Collection",
        description: "Make every birthday special"
    },
    {
        image_url: "https://example.com/ads/general_wishes.jpg",
        redirect_url: "https://example.com/general-wishes",
        status: true,
        start_date: new Date(),
        end_date: new Date(Date.now() + 120 * 24 * 60 * 60 * 1000), // 120 days from now
        location: "category_below",
        priority: 1,
        frequency_cap: 35,
        daily_frequency_cap: 8,
        title: "General Wishes",
        description: "Perfect wishes for every occasion"
    }
];

async function insertDemoAds() {
    try {
        // Connect to MongoDB
        await mongoose.connect(process.env.MONGODB_URI, {
            useNewUrlParser: true,
            useUnifiedTopology: true
        });
        console.log('Connected to MongoDB');

        // Clear existing ads
        await SponsoredAd.deleteMany({});
        console.log('Cleared existing sponsored ads');

        // Insert demo ads
        const insertedAds = await SponsoredAd.insertMany(demoAds);
        console.log(`Successfully inserted ${insertedAds.length} demo sponsored ads`);

        // Print summary of inserted ads
        console.log('\nInserted Ads Summary:');
        console.log('---------------------');
        
        const priorityGroups = {
            'High Priority (8-10)': [],
            'Medium Priority (4-7)': [],
            'Low Priority (1-3)': []
        };

        insertedAds.forEach(ad => {
            const summary = {
                id: ad._id,
                title: ad.title,
                location: ad.location,
                priority: ad.priority
            };

            if (ad.priority >= 8) {
                priorityGroups['High Priority (8-10)'].push(summary);
            } else if (ad.priority >= 4) {
                priorityGroups['Medium Priority (4-7)'].push(summary);
            } else {
                priorityGroups['Low Priority (1-3)'].push(summary);
            }
        });

        // Print grouped summary
        Object.entries(priorityGroups).forEach(([group, ads]) => {
            console.log(`\n${group}:`);
            ads.forEach(ad => {
                console.log(`- ${ad.title} (ID: ${ad.id})`);
                console.log(`  Location: ${ad.location}, Priority: ${ad.priority}`);
            });
        });

    } catch (error) {
        console.error('Error inserting demo ads:', error);
    } finally {
        // Close MongoDB connection
        await mongoose.connection.close();
        console.log('\nMongoDB connection closed');
    }
}

// Run the script
insertDemoAds().then(() => {
    console.log('\nDemo ad insertion completed');
}).catch(error => {
    console.error('Script failed:', error);
}); 