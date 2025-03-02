require('dotenv').config();
const mongoose = require('mongoose');
const connectDB = require('./config/db');
const Festival = require('./models/Festival');
const Template = require('./models/Template');
const { startOfDay, addDays } = require('date-fns');
const CategoryIcon = require('./models/CategoryIcon');

// Connect to MongoDB
connectDB();

// Test data for templates
const templates = [
    {
        title: "Valentine's Card",
        category: "Love",
        htmlContent: "<div class='valentine-card'><h1>Happy Valentine's Day</h1><p>To my special someone</p><div class='heart'></div></div>",
        cssContent: ".valentine-card { background-color: #ffebee; padding: 20px; border-radius: 10px; text-align: center; } " +
            ".heart { width: 50px; height: 50px; background-color: #e91e63; transform: rotate(45deg); margin: 20px auto; } " +
            ".heart:before, .heart:after { content: ''; width: 50px; height: 50px; background-color: #e91e63; border-radius: 50%; position: absolute; } " +
            ".heart:before { left: -25px; } .heart:after { top: -25px; }",
        jsContent: "document.querySelector('.heart').addEventListener('click', function() { this.style.transform = 'scale(1.2) rotate(45deg)'; });",
        previewUrl: "https://img.freepik.com/free-vector/flat-design-valentine-s-day-card_23-2149187098.jpg",
        status: true,
        categoryIcon: "ic_love"
    },
    {
        title: "Love Poem",
        category: "Love",
        htmlContent: "<div class='poem'><h2>My Love</h2><p class='verse'>Roses are red,<br>Violets are blue,<br>Sugar is sweet,<br>And so are you.</p></div>",
        cssContent: ".poem { font-family: 'Courier New', monospace; padding: 20px; background-color: #fff8e1; border-left: 4px solid #ff8a80; } " +
            ".verse { line-height: 1.8; font-style: italic; }",
        jsContent: "let verses = document.querySelectorAll('.verse'); verses.forEach(v => v.addEventListener('mouseover', function() { this.style.color = '#e91e63'; }));" +
            "verses.forEach(v => v.addEventListener('mouseout', function() { this.style.color = 'black'; }));",
        previewUrl: "https://img.freepik.com/free-vector/hand-drawn-valentine-s-day-poem_23-2149187144.jpg",
        status: true,
        categoryIcon: "ic_love"
    },
    {
        title: "Holi Wishes",
        category: "Cultural",
        htmlContent: "<div class='holi-card'><h1>Happy Holi!</h1><p>May your life be as colorful as the festival of Holi</p><div class='colors'><span></span><span></span><span></span><span></span></div></div>",
        cssContent: ".holi-card { text-align: center; padding: 20px; background: linear-gradient(to right, #f5f7fa, #e8f4f8); } " +
            ".colors { display: flex; justify-content: center; margin-top: 20px; } " +
            ".colors span { width: 30px; height: 30px; border-radius: 50%; margin: 0 5px; } " +
            ".colors span:nth-child(1) { background-color: #FF5722; } " +
            ".colors span:nth-child(2) { background-color: #9C27B0; } " +
            ".colors span:nth-child(3) { background-color: #4CAF50; } " +
            ".colors span:nth-child(4) { background-color: #2196F3; }",
        jsContent: "document.querySelectorAll('.colors span').forEach(span => { " +
            "  span.addEventListener('click', function() { " +
            "    this.style.transform = 'scale(1.5)'; " +
            "    setTimeout(() => this.style.transform = 'scale(1)', 500); " +
            "  }); " +
            "});",
        previewUrl: "https://img.freepik.com/free-vector/flat-holi-festival-illustration_23-2149289255.jpg",
        status: true,
        categoryIcon: "ic_cultural"
    },
    {
        title: "Earth Day Pledge",
        category: "Environmental",
        htmlContent: "<div class='earth-card'><h1>Earth Day</h1><p>Our planet, our responsibility</p>" +
            "<div class='pledge'><h3>My Earth Day Pledge</h3><ul id='pledges'>" +
            "<li>Plant a tree</li><li>Reduce plastic use</li><li>Save water</li><li>Use renewable energy</li>" +
            "</ul></div></div>",
        cssContent: ".earth-card { background-color: #e8f5e9; padding: 20px; border-radius: 8px; } " +
            ".pledge { background-color: white; padding: 15px; border-left: 4px solid #4CAF50; margin-top: 20px; } " +
            ".pledge ul { padding-left: 20px; } " +
            ".pledge li { margin: 8px 0; }",
        jsContent: "document.querySelectorAll('#pledges li').forEach(li => { " +
            "  li.addEventListener('click', function() { " +
            "    this.style.textDecoration = this.style.textDecoration === 'line-through' ? 'none' : 'line-through'; " +
            "  }); " +
            "});",
        previewUrl: "https://img.freepik.com/free-vector/hand-drawn-earth-day-illustration_23-2149301781.jpg",
        status: true,
        categoryIcon: "ic_environmental"
    },
    {
        title: "New Year Wishes",
        category: "Cultural",
        htmlContent: "<div class='new-year-card'><h1>Happy New Year 2025!</h1><p>Wishing you joy, peace, and prosperity</p>" +
            "<div class='fireworks'></div></div>",
        cssContent: ".new-year-card { text-align: center; padding: 30px; background: linear-gradient(to bottom, #000428, #004e92); color: white; } " +
            ".fireworks { height: 100px; position: relative; }",
        jsContent: "function createFirework() { " +
            "  const fireworks = document.querySelector('.fireworks'); " +
            "  const firework = document.createElement('div'); " +
            "  firework.style.position = 'absolute'; " +
            "  firework.style.width = '5px'; " +
            "  firework.style.height = '5px'; " +
            "  firework.style.borderRadius = '50%'; " +
            "  firework.style.backgroundColor = `rgb(${Math.random()*255},${Math.random()*255},${Math.random()*255})`; " +
            "  firework.style.left = `${Math.random() * 100}%`; " +
            "  firework.style.top = `${Math.random() * 100}%`; " +
            "  fireworks.appendChild(firework); " +
            "  setTimeout(() => firework.remove(), 1000); " +
            "} " +
            "setInterval(createFirework, 300);",
        previewUrl: "https://img.freepik.com/free-vector/gradient-happy-new-year-2023-background_23-2149856295.jpg",
        status: true,
        categoryIcon: "ic_cultural"
    }
];

// Function to create festivals with templates
const createFestivalsWithTemplates = async () => {
    try {
        // Clear existing data
        await Festival.deleteMany({});
        await Template.deleteMany({});
        
        console.log('Cleared existing data');
        
        // Insert templates
        const savedTemplates = await Template.insertMany(templates);
        console.log(`Inserted ${savedTemplates.length} templates`);
        
        // Group templates by category for easier reference
        const templatesByCategory = {};
        savedTemplates.forEach(template => {
            if (!templatesByCategory[template.category]) {
                templatesByCategory[template.category] = [];
            }
            templatesByCategory[template.category].push(template._id);
        });
        
        // Create festivals
        const today = startOfDay(new Date());
        
        // First, get all category icons to reference them
        const categoryIcons = await CategoryIcon.find();
        const categoryIconMap = {};
        categoryIcons.forEach(icon => {
            categoryIconMap[icon.category] = icon._id;
        });
        
        const festivals = [
            {
                name: "Valentine's Day",
                date: addDays(today, 1),
                description: "Day of Love",
                category: "Cultural",
                categoryIcon: categoryIconMap["Cultural"],
                imageUrl: "https://currentedu365.in/wp-content/uploads/2024/09/nwes-7-1024x576.png",
                templates: templatesByCategory["Love"] || [],
                isActive: true
            },
            {
                name: "Holi Festival",
                date: addDays(today, 3),
                description: "Festival of Colors",
                category: "Cultural",
                categoryIcon: categoryIconMap["Cultural"],
                imageUrl: "https://upload.wikimedia.org/wikipedia/commons/thumb/c/c1/Holi_Celebration_at_Barsana.jpg/1200px-Holi_Celebration_at_Barsana.jpg",
                templates: templatesByCategory["Cultural"] || [],
                isActive: true
            },
            {
                name: "Earth Day",
                date: addDays(today, 5),
                description: "Environmental Awareness Day",
                category: "Environmental",
                categoryIcon: categoryIconMap["Other"],
                imageUrl: "https://www.un.org/sites/un2.un.org/files/field/image/earthday.jpg",
                templates: templatesByCategory["Environmental"] || [],
                isActive: true
            },
            {
                name: "New Year's Day",
                date: addDays(today, -30),
                description: "Celebration of the New Year",
                category: "Cultural",
                categoryIcon: categoryIconMap["New Year"],
                imageUrl: "https://img.freepik.com/free-vector/gradient-happy-new-year-2023-background_23-2149856295.jpg",
                templates: templatesByCategory["Cultural"] || [],
                isActive: true
            },
            {
                name: "Independence Day",
                date: addDays(today, -15),
                description: "National Independence Day",
                category: "National",
                categoryIcon: categoryIconMap["Other"],
                imageUrl: "https://img.freepik.com/free-vector/gradient-15th-august-indian-independence-day-illustration_23-2149466252.jpg",
                templates: templatesByCategory["Cultural"] || [],
                isActive: true
            }
        ];
        
        const savedFestivals = await Festival.insertMany(festivals);
        console.log(`Inserted ${savedFestivals.length} festivals`);
        
        console.log('Test data insertion complete!');
    } catch (error) {
        console.error('Error inserting test data:', error);
    } finally {
        // Close the database connection
        mongoose.connection.close();
        console.log('Database connection closed');
    }
};

// Run the function
createFestivalsWithTemplates();
