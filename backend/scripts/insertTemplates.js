const mongoose = require('mongoose');
const Template = require('../models/Template');
const CategoryIcon = require('../models/CategoryIcon');
require('dotenv').config({ path: require('path').resolve(__dirname, '../.env') });

const templates = [
    // Birthday Templates
    {
        title: "Birthday Celebration",
        category: "Birthday",
        htmlContent: `<div class="birthday-template-1">
            <h1>Happy Birthday!</h1>
            <div class="cake">
                <div class="candle"></div>
            </div>
            <div class="content">
                <p>Wishing you a day filled with joy and laughter!</p>
            </div>
        </div>`,
        cssContent: `.birthday-template-1 {
            background: linear-gradient(135deg, #ffd1ff, #fae1dd);
            padding: 30px;
            border-radius: 15px;
            text-align: center;
            color: #2b2d42;
        }
        .birthday-template-1 .cake {
            width: 100px;
            height: 80px;
            background: #ff758f;
            margin: 20px auto;
            border-radius: 10px;
            position: relative;
        }
        .birthday-template-1 .candle {
            width: 8px;
            height: 30px;
            background: #ffd60a;
            position: absolute;
            top: -20px;
            left: 50%;
            transform: translateX(-50%);
            border-radius: 4px;
        }`,
        jsContent: `document.querySelector('.birthday-template-1 .candle').addEventListener('click', function() {
            this.style.background = '#smoke';
            this.style.animation = 'flicker 0.5s infinite';
        });`,
        previewUrl: "https://currentedu365.in/wp-content/uploads/2024/09/birthday-template-1.png",
        status: true
    },
    // Wedding Template
    {
        title: "Elegant Wedding",
        category: "Wedding",
        htmlContent: `<div class="wedding-template-1">
            <div class="rings"></div>
            <h1>Save the Date</h1>
            <div class="content">
                <p>Join us in celebrating our special day</p>
            </div>
        </div>`,
        cssContent: `.wedding-template-1 {
            background: linear-gradient(to right, #f8f9fa, #e9ecef);
            padding: 40px;
            border-radius: 12px;
            text-align: center;
            color: #495057;
            border: 2px solid #gold;
        }
        .wedding-template-1 .rings {
            width: 60px;
            height: 60px;
            border: 3px solid #gold;
            border-radius: 50%;
            margin: 20px auto;
            position: relative;
        }
        .wedding-template-1 h1 {
            font-family: 'Playfair Display', serif;
            color: #2b2d42;
        }`,
        jsContent: `document.querySelector('.wedding-template-1 .rings').addEventListener('mouseover', function() {
            this.style.transform = 'rotate(360deg)';
            this.style.transition = 'transform 1s ease';
        });`,
        previewUrl: "https://currentedu365.in/wp-content/uploads/2024/09/wedding-template-1.png",
        status: true
    },
    // Cultural Template
    {
        title: "Cultural Festival",
        category: "Cultural",
        htmlContent: `<div class="cultural-template-1">
            <div class="pattern"></div>
            <h1>Cultural Celebration</h1>
            <div class="content">
                <p>Experience the richness of our heritage</p>
            </div>
        </div>`,
        cssContent: `.cultural-template-1 {
            background: linear-gradient(45deg, #e63946, #1d3557);
            padding: 25px;
            border-radius: 8px;
            color: white;
            text-align: center;
        }
        .cultural-template-1 .pattern {
            height: 50px;
            background: repeating-linear-gradient(45deg, rgba(255,255,255,0.1) 0px, rgba(255,255,255,0.1) 10px, transparent 10px, transparent 20px);
            margin-bottom: 20px;
        }`,
        jsContent: `document.querySelector('.cultural-template-1 .pattern').addEventListener('click', function() {
            this.style.animation = 'slide 10s linear infinite';
        });`,
        previewUrl: "https://currentedu365.in/wp-content/uploads/2024/09/cultural-template-1.png",
        status: true
    },
    // Religious Template
    {
        title: "Sacred Celebration",
        category: "Religious",
        htmlContent: `<div class="religious-template-1">
            <div class="sacred-symbol"></div>
            <h1>Blessed Celebration</h1>
            <div class="content">
                <p>May peace and blessings be with you</p>
            </div>
        </div>`,
        cssContent: `.religious-template-1 {
            background: linear-gradient(to bottom, #f1f8ff, #c8e1ff);
            padding: 35px;
            border-radius: 20px;
            text-align: center;
            color: #1a365d;
        }
        .religious-template-1 .sacred-symbol {
            width: 70px;
            height: 70px;
            margin: 20px auto;
            background: #4a5568;
            clip-path: polygon(50% 0%, 100% 50%, 50% 100%, 0% 50%);
        }`,
        jsContent: `document.querySelector('.religious-template-1 .sacred-symbol').addEventListener('click', function() {
            this.style.transform = 'scale(1.2)';
            this.style.transition = 'transform 0.5s ease';
            setTimeout(() => {
                this.style.transform = 'scale(1)';
            }, 500);
        });`,
        previewUrl: "https://currentedu365.in/wp-content/uploads/2024/09/religious-template-1.png",
        status: true
    },
    // National Template
    {
        title: "National Pride",
        category: "National",
        htmlContent: `<div class="national-template-1">
            <div class="flag-wave"></div>
            <h1>National Celebration</h1>
            <div class="content">
                <p>Celebrating our nation's glory</p>
            </div>
        </div>`,
        cssContent: `.national-template-1 {
            background: linear-gradient(45deg, #002868, #bf0a30);
            padding: 30px;
            border-radius: 15px;
            text-align: center;
            color: white;
        }
        .national-template-1 .flag-wave {
            height: 60px;
            background: linear-gradient(white 33.33%, #bf0a30 33.33%, #bf0a30 66.66%, white 66.66%);
            margin: 20px 0;
            animation: wave 3s ease-in-out infinite;
        }`,
        jsContent: `document.querySelector('.national-template-1 .flag-wave').addEventListener('mouseover', function() {
            this.style.animation = 'wave 1s ease-in-out infinite';
        });
        document.querySelector('.national-template-1 .flag-wave').addEventListener('mouseout', function() {
            this.style.animation = 'wave 3s ease-in-out infinite';
        });`,
        previewUrl: "https://currentedu365.in/wp-content/uploads/2024/09/national-template-1.png",
        status: true
    },
    // Anniversary Template
    {
        title: "Love Anniversary",
        category: "Anniversary",
        htmlContent: `<div class="anniversary-template-1">
            <div class="hearts">
                <div class="heart"></div>
                <div class="heart"></div>
            </div>
            <h1>Happy Anniversary</h1>
            <div class="content">
                <p>Celebrating years of love and togetherness</p>
            </div>
        </div>`,
        cssContent: `.anniversary-template-1 {
            background: linear-gradient(to right bottom, #ff758c, #ff7eb3);
            padding: 35px;
            border-radius: 25px;
            text-align: center;
            color: white;
        }
        .anniversary-template-1 .hearts {
            display: flex;
            justify-content: center;
            gap: 20px;
        }
        .anniversary-template-1 .heart {
            width: 30px;
            height: 30px;
            background: white;
            transform: rotate(45deg);
            position: relative;
        }
        .anniversary-template-1 .heart:before,
        .anniversary-template-1 .heart:after {
            content: '';
            width: 30px;
            height: 30px;
            background: white;
            border-radius: 50%;
            position: absolute;
        }
        .anniversary-template-1 .heart:before {
            left: -15px;
        }
        .anniversary-template-1 .heart:after {
            top: -15px;
        }`,
        jsContent: `document.querySelectorAll('.anniversary-template-1 .heart').forEach(heart => {
            heart.addEventListener('click', function() {
                this.style.transform = 'rotate(45deg) scale(1.2)';
                this.style.transition = 'transform 0.3s ease';
                setTimeout(() => {
                    this.style.transform = 'rotate(45deg) scale(1)';
                }, 300);
            });
        });`,
        previewUrl: "https://currentedu365.in/wp-content/uploads/2024/09/anniversary-template-1.png",
        status: true
    },
    // Graduation Template
    {
        title: "Graduation Success",
        category: "Graduation",
        htmlContent: `<div class="graduation-template-1">
            <div class="cap">
                <div class="top"></div>
                <div class="tassel"></div>
            </div>
            <h1>Congratulations Graduate!</h1>
            <div class="content">
                <p>Celebrating your academic achievement</p>
            </div>
        </div>`,
        cssContent: `.graduation-template-1 {
            background: linear-gradient(to bottom right, #1a237e, #311b92);
            padding: 40px;
            border-radius: 15px;
            text-align: center;
            color: white;
        }
        .graduation-template-1 .cap {
            position: relative;
            width: 80px;
            margin: 20px auto;
        }
        .graduation-template-1 .top {
            width: 80px;
            height: 80px;
            background: #000;
            transform: rotate(45deg);
        }
        .graduation-template-1 .tassel {
            position: absolute;
            width: 4px;
            height: 40px;
            background: #ffd700;
            top: 40px;
            left: 50%;
            transform: translateX(-50%);
        }`,
        jsContent: `document.querySelector('.graduation-template-1 .cap').addEventListener('click', function() {
            this.style.transform = 'translateY(-20px)';
            this.style.transition = 'transform 0.3s ease';
            setTimeout(() => {
                this.style.transform = 'translateY(0)';
            }, 300);
        });`,
        previewUrl: "https://currentedu365.in/wp-content/uploads/2024/09/graduation-template-1.png",
        status: true
    }
];

async function insertTemplates() {
    try {
        // Connect to MongoDB
        await mongoose.connect(process.env.MONGODB_URI, {
            useNewUrlParser: true,
            useUnifiedTopology: true
        });
        console.log('Connected to MongoDB successfully');

        // Clear existing templates
        await Template.deleteMany({});
        console.log('Cleared existing templates');

        for (const templateData of templates) {
            // Find the corresponding category icon
            const categoryIcon = await CategoryIcon.findOne({ 
                category: templateData.category 
            });

            if (!categoryIcon) {
                console.log(`Category icon not found for category: ${templateData.category}`);
                continue;
            }

            // Create template with category icon reference
            const template = new Template({
                ...templateData,
                categoryIcon: categoryIcon._id
            });

            await template.save();
            console.log(`Template saved: ${template.title}`);
        }

        console.log('All templates inserted successfully');
    } catch (error) {
        console.error('Error inserting templates:', error);
    } finally {
        mongoose.connection.close();
    }
}

insertTemplates();