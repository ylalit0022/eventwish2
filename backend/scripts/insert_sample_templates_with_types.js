const mongoose = require('mongoose');
require('dotenv').config();

// Connect to MongoDB
mongoose.connect(process.env.MONGODB_URI || 'mongodb://localhost:27017/eventwish', {
    useNewUrlParser: true,
    useUnifiedTopology: true
});

const Template = require('../models/Template');

const sampleTemplates = [
    // HTML Templates
    {
        title: "Happy Birthday Celebration",
        category: "birthday",
        templateType: "html",
        htmlContent: `
            <div style="background: linear-gradient(135deg, #ff6b6b, #feca57); padding: 40px; border-radius: 20px; text-align: center; color: white; font-family: Arial, sans-serif;">
                <h1 style="font-size: 3em; margin-bottom: 20px; text-shadow: 2px 2px 4px rgba(0,0,0,0.3);">🎉 Happy Birthday! 🎉</h1>
                <p style="font-size: 1.5em; margin-bottom: 30px;">Wishing you a day filled with happiness and a year filled with joy!</p>
                <div style="background: rgba(255,255,255,0.2); padding: 20px; border-radius: 15px; margin: 20px 0;">
                    <p style="font-size: 1.2em; margin: 0;">🎂 May all your birthday wishes come true! 🎂</p>
                </div>
                <p style="font-size: 1em; opacity: 0.9;">Celebrate this special day with love and laughter!</p>
            </div>
        `,
        cssContent: `
            @keyframes bounce {
                0%, 20%, 50%, 80%, 100% { transform: translateY(0); }
                40% { transform: translateY(-10px); }
                60% { transform: translateY(-5px); }
            }
            h1 { animation: bounce 2s infinite; }
        `,
        previewUrl: "https://example.com/birthday-preview.jpg",
        isPremium: false,
        isFeatured: true,
        isTrending: true,
        likes: 156,
        favorites: 89,
        viewCount: 1240,
        sharedCount: 78,
        tags: ["birthday", "celebration", "colorful", "animated"],
        festivalTag: "birthday",
        customizationOptions: {
            allowNameEdit: true,
            allowPhotoEdit: true,
            allowBackgroundChange: true,
            allowMusic: false,
            allowThemeCustomization: true
        }
    },
    
    // VIDEO Template
    {
        title: "Diwali Festival Wishes",
        category: "diwali",
        templateType: "video",
        htmlContent: `
            <div style="background: #1a1a2e; padding: 30px; border-radius: 15px; text-align: center; color: #ffd700; font-family: 'Georgia', serif;">
                <h1 style="font-size: 2.5em; margin-bottom: 20px; text-shadow: 2px 2px 8px rgba(255,215,0,0.5);">✨ Happy Diwali ✨</h1>
                <div style="background: linear-gradient(45deg, #ff6b35, #f7931e); padding: 20px; border-radius: 10px; margin: 20px 0;">
                    <p style="font-size: 1.3em; color: white; margin: 0;">🪔 May the festival of lights illuminate your life with joy, prosperity, and happiness! 🪔</p>
                </div>
                <p style="font-size: 1.1em; margin-top: 20px;">Wishing you and your family a very Happy Diwali!</p>
            </div>
        `,
        videoUrl: "https://example.com/diwali-video.mp4",
        previewUrl: "https://example.com/diwali-thumbnail.jpg",
        isPremium: true,
        isFeatured: true,
        isTrending: false,
        likes: 234,
        favorites: 145,
        viewCount: 2100,
        sharedCount: 167,
        tags: ["diwali", "festival", "lights", "traditional"],
        festivalTag: "diwali",
        customizationOptions: {
            allowNameEdit: true,
            allowPhotoEdit: false,
            allowBackgroundChange: false,
            allowMusic: true,
            allowThemeCustomization: false
        }
    },
    
    // IMAGE Template
    {
        title: "Good Morning Sunshine",
        category: "good morning",
        templateType: "image",
        htmlContent: `
            <div style="background: linear-gradient(to right, #ffecd2, #fcb69f); padding: 35px; border-radius: 20px; text-align: center; color: #8b4513; font-family: 'Trebuchet MS', sans-serif;">
                <h1 style="font-size: 2.8em; margin-bottom: 15px; text-shadow: 1px 1px 3px rgba(139,69,19,0.3);">🌅 Good Morning! 🌅</h1>
                <p style="font-size: 1.4em; margin-bottom: 25px; font-weight: 500;">Rise and shine! Start your day with a smile!</p>
                <div style="background: rgba(255,255,255,0.4); padding: 15px; border-radius: 12px; margin: 15px 0;">
                    <p style="font-size: 1.1em; margin: 0; font-style: italic;">☀️ Every morning is a new beginning. Make it beautiful! ☀️</p>
                </div>
                <p style="font-size: 1em; opacity: 0.8;">Have a wonderful day ahead!</p>
            </div>
        `,
        imageUrl: "https://example.com/morning-sunshine.jpg",
        previewUrl: "https://example.com/morning-preview.jpg",
        isPremium: false,
        isFeatured: false,
        isTrending: true,
        likes: 89,
        favorites: 67,
        viewCount: 890,
        sharedCount: 45,
        tags: ["morning", "sunshine", "motivation", "daily"],
        festivalTag: "daily",
        customizationOptions: {
            allowNameEdit: true,
            allowPhotoEdit: true,
            allowBackgroundChange: true,
            allowMusic: false,
            allowThemeCustomization: true
        }
    },
    
    // HTML Template - Wedding
    {
        title: "Wedding Invitation Elegant",
        category: "wedding",
        templateType: "html",
        htmlContent: `
            <div style="background: linear-gradient(135deg, #667eea, #764ba2); padding: 50px; border-radius: 25px; text-align: center; color: white; font-family: 'Times New Roman', serif;">
                <div style="border: 3px solid rgba(255,255,255,0.3); padding: 40px; border-radius: 20px; background: rgba(255,255,255,0.1);">
                    <h1 style="font-size: 3.5em; margin-bottom: 10px; font-weight: 300; letter-spacing: 2px;">💍 Wedding Invitation 💍</h1>
                    <div style="width: 100px; height: 2px; background: white; margin: 20px auto;"></div>
                    <p style="font-size: 1.6em; margin: 30px 0; font-style: italic;">You are cordially invited to celebrate the union of</p>
                    <h2 style="font-size: 2.5em; margin: 20px 0; font-weight: 400; text-shadow: 2px 2px 4px rgba(0,0,0,0.3);">[Bride] & [Groom]</h2>
                    <div style="background: rgba(255,255,255,0.2); padding: 25px; border-radius: 15px; margin: 30px 0;">
                        <p style="font-size: 1.3em; margin: 10px 0;">📅 Date: [Wedding Date]</p>
                        <p style="font-size: 1.3em; margin: 10px 0;">🕒 Time: [Wedding Time]</p>
                        <p style="font-size: 1.3em; margin: 10px 0;">📍 Venue: [Wedding Venue]</p>
                    </div>
                    <p style="font-size: 1.1em; margin-top: 25px; opacity: 0.9;">Your presence will make our day even more special!</p>
                </div>
            </div>
        `,
        previewUrl: "https://example.com/wedding-preview.jpg",
        isPremium: true,
        isFeatured: true,
        isTrending: false,
        likes: 312,
        favorites: 198,
        viewCount: 2890,
        sharedCount: 234,
        tags: ["wedding", "invitation", "elegant", "formal"],
        festivalTag: "wedding",
        customizationOptions: {
            allowNameEdit: true,
            allowPhotoEdit: true,
            allowBackgroundChange: true,
            allowMusic: true,
            allowThemeCustomization: true
        }
    },
    
    // VIDEO Template - New Year
    {
        title: "New Year Countdown 2024",
        category: "new year",
        templateType: "video",
        htmlContent: `
            <div style="background: linear-gradient(45deg, #000428, #004e92); padding: 40px; border-radius: 20px; text-align: center; color: #ffd700; font-family: 'Arial Black', sans-serif;">
                <h1 style="font-size: 4em; margin-bottom: 20px; text-shadow: 3px 3px 6px rgba(0,0,0,0.5); animation: glow 2s ease-in-out infinite alternate;">🎊 2024 🎊</h1>
                <p style="font-size: 2em; margin-bottom: 30px; font-weight: bold;">HAPPY NEW YEAR!</p>
                <div style="background: rgba(255,215,0,0.2); padding: 25px; border-radius: 15px; border: 2px solid #ffd700;">
                    <p style="font-size: 1.4em; margin: 15px 0;">🥂 Cheers to new beginnings!</p>
                    <p style="font-size: 1.4em; margin: 15px 0;">✨ May this year bring you joy, success, and happiness!</p>
                    <p style="font-size: 1.4em; margin: 15px 0;">🎯 Here's to achieving all your dreams in 2024!</p>
                </div>
                <p style="font-size: 1.2em; margin-top: 25px; font-style: italic;">Wishing you a spectacular New Year!</p>
            </div>
        `,
        cssContent: `
            @keyframes glow {
                from { text-shadow: 3px 3px 6px rgba(0,0,0,0.5), 0 0 20px #ffd700; }
                to { text-shadow: 3px 3px 6px rgba(0,0,0,0.5), 0 0 30px #ffd700, 0 0 40px #ffd700; }
            }
        `,
        videoUrl: "https://example.com/newyear-countdown.mp4",
        previewUrl: "https://example.com/newyear-thumbnail.jpg",
        isPremium: false,
        isFeatured: true,
        isTrending: true,
        likes: 445,
        favorites: 267,
        viewCount: 3456,
        sharedCount: 289,
        tags: ["newyear", "2024", "countdown", "celebration"],
        festivalTag: "newyear",
        customizationOptions: {
            allowNameEdit: true,
            allowPhotoEdit: false,
            allowBackgroundChange: false,
            allowMusic: true,
            allowThemeCustomization: false
        }
    },
    
    // IMAGE Template - Thank You
    {
        title: "Heartfelt Thank You",
        category: "thank you",
        templateType: "image",
        htmlContent: `
            <div style="background: linear-gradient(135deg, #a8edea, #fed6e3); padding: 40px; border-radius: 25px; text-align: center; color: #2c3e50; font-family: 'Palatino', serif;">
                <h1 style="font-size: 3.2em; margin-bottom: 25px; text-shadow: 1px 1px 2px rgba(44,62,80,0.2);">💝 Thank You 💝</h1>
                <div style="background: rgba(255,255,255,0.6); padding: 30px; border-radius: 20px; margin: 25px 0; border: 1px solid rgba(44,62,80,0.1);">
                    <p style="font-size: 1.6em; margin-bottom: 20px; font-weight: 500;">Your kindness means the world to me!</p>
                    <p style="font-size: 1.3em; margin-bottom: 15px; line-height: 1.6;">🌸 Thank you for being such an amazing person 🌸</p>
                    <p style="font-size: 1.3em; margin-bottom: 15px; line-height: 1.6;">🙏 Your support and friendship are truly appreciated 🙏</p>
                    <p style="font-size: 1.3em; line-height: 1.6;">💖 Grateful to have you in my life 💖</p>
                </div>
                <p style="font-size: 1.1em; font-style: italic; opacity: 0.8;">With heartfelt gratitude and warm wishes</p>
            </div>
        `,
        imageUrl: "https://example.com/thankyou-flowers.jpg",
        previewUrl: "https://example.com/thankyou-preview.jpg",
        isPremium: false,
        isFeatured: false,
        isTrending: false,
        likes: 67,
        favorites: 45,
        viewCount: 567,
        sharedCount: 34,
        tags: ["thankyou", "gratitude", "appreciation", "heartfelt"],
        festivalTag: "general",
        customizationOptions: {
            allowNameEdit: true,
            allowPhotoEdit: true,
            allowBackgroundChange: true,
            allowMusic: false,
            allowThemeCustomization: true
        }
    }
];

async function insertSampleTemplates() {
    try {
        console.log('🚀 Starting template insertion...');
        
        // Insert sample templates
        const insertedTemplates = await Template.insertMany(sampleTemplates);
        console.log(`✅ Successfully inserted ${insertedTemplates.length} sample templates`);
        
        // Display summary
        const summary = {};
        insertedTemplates.forEach(template => {
            const type = template.templateType;
            if (!summary[type]) summary[type] = 0;
            summary[type]++;
        });
        
        console.log('\n📊 Template Summary:');
        Object.entries(summary).forEach(([type, count]) => {
            console.log(`   ${type.toUpperCase()}: ${count} templates`);
        });
        
        console.log('\n✨ Ready for testing favorite/unfavorite functionality!');
        
    } catch (error) {
        console.error('❌ Error inserting sample templates:', error);
    } finally {
        mongoose.connection.close();
        console.log('🔐 Database connection closed');
    }
}

// Run the script
insertSampleTemplates(); 