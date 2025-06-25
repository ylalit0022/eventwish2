/**
 * MongoDB Script to Insert Sample Templates with HTML, VIDEO, and IMAGE Types
 * 
 * This script creates sample templates for testing the new template type system
 * in the EventWish app. It includes templates for all three supported types:
 * - HTML: Interactive greeting cards with CSS and JavaScript
 * - VIDEO: Video-based greeting templates
 * - IMAGE: Static image-based greeting templates
 * 
 * Usage:
 * 1. Connect to your MongoDB instance
 * 2. Select the eventwish database: use eventwish
 * 3. Run this script: load('scripts/insert_sample_templates.js')
 * 
 * Or run directly with mongosh:
 * mongosh "mongodb://your-connection-string/eventwish" scripts/insert_sample_templates.js
 */

// Switch to the eventwish database
use('eventwish');

// Sample HTML Templates
const htmlTemplates = [
    {
        _id: ObjectId(),
        name: "Birthday Celebration Card",
        category: "birthday",
        templateType: "html",
        htmlContent: `
            <div style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); padding: 40px; border-radius: 20px; text-align: center; color: white; font-family: Arial, sans-serif;">
                <h1 style="font-size: 2.5em; margin-bottom: 20px; text-shadow: 2px 2px 4px rgba(0,0,0,0.3);">🎉 Happy Birthday!</h1>
                <div style="background: rgba(255,255,255,0.1); padding: 20px; border-radius: 15px; margin: 20px 0;">
                    <p style="font-size: 1.2em; margin: 10px 0;">Dear <span class="recipient-name" style="font-weight: bold; color: #ffd700;">Recipient Name</span>,</p>
                    <p style="font-size: 1.1em; line-height: 1.6;">Wishing you a day filled with happiness and a year filled with joy!</p>
                    <p style="font-size: 1em; margin-top: 20px;">With love,<br><span class="sender-name" style="font-weight: bold; color: #ffd700;">Sender Name</span></p>
                </div>
                <div style="margin-top: 30px;">
                    <span style="font-size: 2em;">🎂🎈🎁</span>
                </div>
            </div>
        `,
        cssContent: `
            @keyframes bounce {
                0%, 20%, 50%, 80%, 100% { transform: translateY(0); }
                40% { transform: translateY(-10px); }
                60% { transform: translateY(-5px); }
            }
            .recipient-name, .sender-name {
                animation: bounce 2s infinite;
                display: inline-block;
            }
            div:hover {
                transform: scale(1.02);
                transition: transform 0.3s ease;
            }
        `,
        jsContent: `
            document.addEventListener('DOMContentLoaded', function() {
                console.log('Birthday template loaded');
                // Add sparkle effect
                setInterval(function() {
                    const sparkles = ['✨', '⭐', '💫', '🌟'];
                    const sparkle = sparkles[Math.floor(Math.random() * sparkles.length)];
                    const div = document.createElement('div');
                    div.innerHTML = sparkle;
                    div.style.position = 'absolute';
                    div.style.left = Math.random() * 100 + '%';
                    div.style.top = Math.random() * 100 + '%';
                    div.style.animation = 'bounce 1s ease-out';
                    document.body.appendChild(div);
                    setTimeout(() => div.remove(), 1000);
                }, 2000);
            });
        `,
        imageUrl: "https://images.unsplash.com/photo-1513475382585-d06e58bcb0e0?w=400",
        previewUrl: "https://images.unsplash.com/photo-1513475382585-d06e58bcb0e0?w=400",
        likes: 150,
        favorites: 85,
        shares: 42,
        status: true,
        isPremium: false,
        isFeatured: true,
        createdAt: new Date(),
        tags: ["birthday", "celebration", "interactive", "colorful"],
        styleTags: ["gradient", "modern", "animated"],
        searchKeywords: ["birthday", "celebration", "party", "wishes"]
    },
    {
        _id: ObjectId(),
        name: "Thank You Card",
        category: "thank you",
        templateType: "html",
        htmlContent: `
            <div style="background: #f8f9fa; padding: 30px; border: 3px solid #28a745; border-radius: 15px; text-align: center; font-family: Georgia, serif;">
                <h2 style="color: #28a745; font-size: 2em; margin-bottom: 20px;">Thank You! 🙏</h2>
                <div style="background: white; padding: 25px; border-radius: 10px; box-shadow: 0 4px 8px rgba(0,0,0,0.1);">
                    <p style="font-size: 1.1em; color: #333; margin: 15px 0;">Dear <span class="recipient-name" style="color: #28a745; font-weight: bold;">Recipient Name</span>,</p>
                    <p style="font-size: 1em; line-height: 1.8; color: #555;">Your kindness and support mean the world to me. Thank you for being such an amazing person!</p>
                    <p style="font-size: 1em; margin-top: 20px; color: #333;">Gratefully yours,<br><span class="sender-name" style="color: #28a745; font-weight: bold;">Sender Name</span></p>
                </div>
                <div style="margin-top: 20px; font-size: 1.5em;">💚✨💚</div>
            </div>
        `,
        cssContent: `
            .recipient-name, .sender-name {
                text-decoration: underline;
                text-decoration-color: #28a745;
            }
            div:hover {
                box-shadow: 0 8px 16px rgba(40, 167, 69, 0.2);
                transition: box-shadow 0.3s ease;
            }
        `,
        jsContent: `
            console.log('Thank you template loaded');
        `,
        imageUrl: "https://images.unsplash.com/photo-1516975080664-ed2fc6a32937?w=400",
        previewUrl: "https://images.unsplash.com/photo-1516975080664-ed2fc6a32937?w=400",
        likes: 89,
        favorites: 56,
        shares: 23,
        status: true,
        isPremium: false,
        isFeatured: false,
        createdAt: new Date(),
        tags: ["thank you", "gratitude", "appreciation"],
        styleTags: ["clean", "elegant", "green"],
        searchKeywords: ["thank you", "thanks", "gratitude", "appreciation"]
    }
];

// Sample VIDEO Templates
const videoTemplates = [
    {
        _id: ObjectId(),
        name: "Animated Birthday Wishes",
        category: "birthday",
        templateType: "video",
        videoUrl: "https://sample-videos.com/zip/10/mp4/SampleVideo_1280x720_1mb.mp4",
        imageUrl: "https://images.unsplash.com/photo-1464349095431-e9a21285b5f3?w=400",
        previewUrl: "https://images.unsplash.com/photo-1464349095431-e9a21285b5f3?w=400",
        htmlContent: null, // Video templates don't need HTML content
        cssContent: null,
        jsContent: null,
        likes: 234,
        favorites: 145,
        shares: 78,
        status: true,
        isPremium: true,
        isFeatured: true,
        createdAt: new Date(),
        tags: ["birthday", "video", "animated", "premium"],
        styleTags: ["animated", "colorful", "dynamic"],
        searchKeywords: ["birthday", "video", "animation", "celebration"],
        description: "Animated video birthday card with confetti and balloons"
    },
    {
        _id: ObjectId(),
        name: "Wedding Anniversary Video",
        category: "anniversary",
        templateType: "video",
        videoUrl: "https://sample-videos.com/zip/10/mp4/SampleVideo_1280x720_2mb.mp4",
        imageUrl: "https://images.unsplash.com/photo-1511285560929-80b456fea0bc?w=400",
        previewUrl: "https://images.unsplash.com/photo-1511285560929-80b456fea0bc?w=400",
        htmlContent: null,
        cssContent: null,
        jsContent: null,
        likes: 178,
        favorites: 92,
        shares: 45,
        status: true,
        isPremium: true,
        isFeatured: false,
        createdAt: new Date(),
        tags: ["anniversary", "wedding", "video", "romantic"],
        styleTags: ["elegant", "romantic", "golden"],
        searchKeywords: ["anniversary", "wedding", "love", "celebration"],
        description: "Romantic anniversary video with golden effects"
    },
    {
        _id: ObjectId(),
        name: "New Year Countdown",
        category: "new year",
        templateType: "video",
        videoUrl: "https://sample-videos.com/zip/10/mp4/SampleVideo_1280x720_1mb.mp4",
        imageUrl: "https://images.unsplash.com/photo-1467810563316-b5476525c0f9?w=400",
        previewUrl: "https://images.unsplash.com/photo-1467810563316-b5476525c0f9?w=400",
        htmlContent: null,
        cssContent: null,
        jsContent: null,
        likes: 312,
        favorites: 198,
        shares: 156,
        status: true,
        isPremium: false,
        isFeatured: true,
        createdAt: new Date(),
        tags: ["new year", "countdown", "video", "fireworks"],
        styleTags: ["sparkly", "festive", "dark"],
        searchKeywords: ["new year", "countdown", "fireworks", "celebration"],
        description: "Spectacular New Year countdown with fireworks"
    }
];

// Sample IMAGE Templates
const imageTemplates = [
    {
        _id: ObjectId(),
        name: "Floral Mother's Day Card",
        category: "mother's day",
        templateType: "image",
        imageUrl: "https://images.unsplash.com/photo-1490750967868-88aa4486c946?w=800",
        previewUrl: "https://images.unsplash.com/photo-1490750967868-88aa4486c946?w=400",
        videoUrl: null, // Image templates don't need video
        htmlContent: null, // Image templates don't need HTML content
        cssContent: null,
        jsContent: null,
        likes: 267,
        favorites: 134,
        shares: 89,
        status: true,
        isPremium: false,
        isFeatured: true,
        createdAt: new Date(),
        tags: ["mothers day", "floral", "image", "elegant"],
        styleTags: ["floral", "pink", "elegant", "soft"],
        searchKeywords: ["mothers day", "mom", "flowers", "love"],
        description: "Beautiful floral design for Mother's Day greetings"
    },
    {
        _id: ObjectId(),
        name: "Graduation Celebration",
        category: "graduation",
        templateType: "image",
        imageUrl: "https://images.unsplash.com/photo-1523050854058-8df90110c9f1?w=800",
        previewUrl: "https://images.unsplash.com/photo-1523050854058-8df90110c9f1?w=400",
        videoUrl: null,
        htmlContent: null,
        cssContent: null,
        jsContent: null,
        likes: 156,
        favorites: 78,
        shares: 34,
        status: true,
        isPremium: false,
        isFeatured: false,
        createdAt: new Date(),
        tags: ["graduation", "achievement", "image", "celebration"],
        styleTags: ["academic", "blue", "formal", "achievement"],
        searchKeywords: ["graduation", "degree", "achievement", "success"],
        description: "Congratulatory image for graduation achievements"
    },
    {
        _id: ObjectId(),
        name: "Christmas Greetings",
        category: "christmas",
        templateType: "image",
        imageUrl: "https://images.unsplash.com/photo-1512389142860-9c449e58a543?w=800",
        previewUrl: "https://images.unsplash.com/photo-1512389142860-9c449e58a543?w=400",
        videoUrl: null,
        htmlContent: null,
        cssContent: null,
        jsContent: null,
        likes: 445,
        favorites: 298,
        shares: 167,
        status: true,
        isPremium: false,
        isFeatured: true,
        createdAt: new Date(),
        tags: ["christmas", "holiday", "image", "festive"],
        styleTags: ["festive", "red", "green", "traditional"],
        searchKeywords: ["christmas", "holiday", "xmas", "festive"],
        description: "Traditional Christmas greeting with festive elements"
    },
    {
        _id: ObjectId(),
        name: "Get Well Soon",
        category: "get well",
        templateType: "image",
        imageUrl: "https://images.unsplash.com/photo-1544367567-0f2fcb009e0b?w=800",
        previewUrl: "https://images.unsplash.com/photo-1544367567-0f2fcb009e0b?w=400",
        videoUrl: null,
        htmlContent: null,
        cssContent: null,
        jsContent: null,
        likes: 89,
        favorites: 45,
        shares: 12,
        status: true,
        isPremium: false,
        isFeatured: false,
        createdAt: new Date(),
        tags: ["get well", "health", "image", "caring"],
        styleTags: ["soft", "peaceful", "light", "caring"],
        searchKeywords: ["get well", "health", "recovery", "care"],
        description: "Gentle get well soon image with soothing colors"
    }
];

// Combine all templates
const allTemplates = [...htmlTemplates, ...videoTemplates, ...imageTemplates];

try {
    // Insert all templates
    const result = db.templates.insertMany(allTemplates);
    
    print("✅ Successfully inserted sample templates!");
    print(`📊 Inserted ${result.insertedIds.length} templates:`);
    print(`   - ${htmlTemplates.length} HTML templates`);
    print(`   - ${videoTemplates.length} VIDEO templates`);
    print(`   - ${imageTemplates.length} IMAGE templates`);
    
    // Print summary by type
    print("\n📋 Template Summary by Type:");
    
    const htmlCount = db.templates.countDocuments({templateType: "html"});
    const videoCount = db.templates.countDocuments({templateType: "video"});
    const imageCount = db.templates.countDocuments({templateType: "image"});
    
    print(`   HTML Templates: ${htmlCount}`);
    print(`   VIDEO Templates: ${videoCount}`);
    print(`   IMAGE Templates: ${imageCount}`);
    print(`   Total Templates: ${htmlCount + videoCount + imageCount}`);
    
    // Print sample queries for testing
    print("\n🔍 Sample Queries for Testing:");
    print("   // Get all HTML templates");
    print('   db.templates.find({templateType: "html"}).limit(5);');
    print("\n   // Get all VIDEO templates");
    print('   db.templates.find({templateType: "video"}).limit(5);');
    print("\n   // Get all IMAGE templates");
    print('   db.templates.find({templateType: "image"}).limit(5);');
    print("\n   // Get featured templates of each type");
    print('   db.templates.find({isFeatured: true});');
    print("\n   // Get premium templates");
    print('   db.templates.find({isPremium: true});');
    
} catch (error) {
    print("❌ Error inserting templates:");
    print(error);
}

// Create indexes for better performance
try {
    db.templates.createIndex({templateType: 1});
    db.templates.createIndex({category: 1});
    db.templates.createIndex({isFeatured: 1});
    db.templates.createIndex({isPremium: 1});
    db.templates.createIndex({status: 1});
    db.templates.createIndex({createdAt: -1});
    db.templates.createIndex({likes: -1});
    db.templates.createIndex({favorites: -1});
    
    print("\n📈 Created performance indexes:");
    print("   - templateType index");
    print("   - category index");
    print("   - isFeatured index");
    print("   - isPremium index");
    print("   - status index");
    print("   - createdAt index (descending)");
    print("   - likes index (descending)");
    print("   - favorites index (descending)");
    
} catch (error) {
    print("⚠️ Warning: Could not create some indexes:");
    print(error);
}

print("\n🎉 Sample data insertion complete!");
print("🚀 Ready to test template type system in EventWish app!");

/**
 * Usage Examples:
 * 
 * 1. Test HTML Templates:
 *    - These have interactive content with CSS and JavaScript
 *    - Should render in WebView with full editing capabilities
 *    - Names should be replaceable via JavaScript
 * 
 * 2. Test VIDEO Templates:
 *    - These have videoUrl field populated
 *    - Should render in video player with controls
 *    - Names should be overlaid or shown separately
 * 
 * 3. Test IMAGE Templates:
 *    - These have imageUrl field populated
 *    - Should render as images with zoom capabilities
 *    - Names should be overlaid or shown separately
 * 
 * 4. API Testing:
 *    - Use these template IDs to test favorite/unfavorite
 *    - Test template type detection in TemplateDetailFragment
 *    - Verify type-specific rendering in HomeFragment
 */ 