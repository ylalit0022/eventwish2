const mongoose = require('mongoose');
const Template = require('../models/Template');
require('dotenv').config({ path: require('path').resolve(__dirname, '../.env') });

// Demo templates with direct categoryIcon URLs
const demoTemplates = [
    {
        title: "Birthday Special",
        category: "Birthday",
        htmlContent: `<div class="birthday-demo">
            <h1>Happy Birthday!</h1>
            <div class="cake"></div>
            <p>May your day be filled with joy!</p>
        </div>`,
        cssContent: `.birthday-demo {
            background: linear-gradient(135deg, #ff7eb3, #ff758c);
            padding: 20px;
            text-align: center;
            color: white;
            border-radius: 10px;
        }
        .birthday-demo .cake {
            width: 60px;
            height: 40px;
            background: #fff;
            margin: 20px auto;
            border-radius: 5px;
            position: relative;
        }`,
        jsContent: "",
        previewUrl: "https://currentedu365.in/wp-content/uploads/2024/09/birthday-preview.png",
        status: true,
        // Direct URL for category icon
        categoryIcon: "https://currentedu365.in/wp-content/uploads/2024/09/birthday-icon.png"
    },
    {
        title: "Anniversary Wishes",
        category: "Anniversary",
        htmlContent: `<div class="anniversary-demo">
            <h1>Happy Anniversary!</h1>
            <div class="rings"></div>
            <p>Celebrating your special journey together!</p>
        </div>`,
        cssContent: `.anniversary-demo {
            background: linear-gradient(to right, #8e44ad, #3498db);
            padding: 25px;
            text-align: center;
            color: white;
            border-radius: 12px;
        }
        .anniversary-demo .rings {
            display: flex;
            justify-content: center;
            gap: 10px;
            margin: 20px 0;
        }`,
        jsContent: "",
        previewUrl: "https://currentedu365.in/wp-content/uploads/2024/09/anniversary-preview.png",
        status: true,
        // Direct URL for category icon
        categoryIcon: "https://currentedu365.in/wp-content/uploads/2024/09/anniversary-icon.png"
    },
    {
        title: "Graduation Congratulations",
        category: "Graduation",
        htmlContent: `<div class="graduation-demo">
            <h1>Congratulations, Graduate!</h1>
            <div class="cap"></div>
            <p>Your hard work has paid off!</p>
        </div>`,
        cssContent: `.graduation-demo {
            background: linear-gradient(to bottom, #2c3e50, #4a69bd);
            padding: 30px;
            text-align: center;
            color: white;
            border-radius: 15px;
        }
        .graduation-demo .cap {
            width: 70px;
            height: 20px;
            background: #000;
            margin: 20px auto;
            position: relative;
        }`,
        jsContent: "",
        previewUrl: "https://currentedu365.in/wp-content/uploads/2024/09/graduation-preview.png",
        status: true,
        // Direct URL for category icon
        categoryIcon: "https://currentedu365.in/wp-content/uploads/2024/09/graduation-icon.png"
    }
];

/**
 * Insert templates with direct URLs for categoryIcon into the main templates collection
 */
async function insertTemplates() {
    let connection;
    try {
        // Connect to MongoDB
        console.log("Connecting to MongoDB...");
        connection = await mongoose.connect(process.env.MONGODB_URI, {
            useNewUrlParser: true,
            useUnifiedTopology: true
        });
        console.log("Connected to MongoDB successfully");

        // Use the main Template model directly
        console.log("Inserting templates into main collection...");
        
        for (const templateData of demoTemplates) {
            const template = new Template(templateData);
            await template.save();
            
            // Verify the saved template
            const savedTemplate = await Template.findById(template._id);
            console.log(`Template saved: ${savedTemplate.title}`);
            console.log(`- Category: ${savedTemplate.category}`);
            console.log(`- CategoryIcon (direct URL): ${savedTemplate.categoryIcon}`);
            console.log("----------------------------");
        }

        // Count templates to verify insertion
        const count = await Template.countDocuments();
        console.log(`Total templates in collection: ${count}`);
        
        // Retrieve and validate one template to confirm structure
        const sampleTemplate = await Template.findOne({ title: demoTemplates[0].title });
        console.log("\nSample template structure:");
        console.log(JSON.stringify({
            id: sampleTemplate._id,
            title: sampleTemplate.title,
            category: sampleTemplate.category,
            categoryIcon: sampleTemplate.categoryIcon,
            categoryIconType: typeof sampleTemplate.categoryIcon
        }, null, 2));

        console.log("\nTemplates successfully inserted into the database");

    } catch (error) {
        console.error("Error inserting templates:", error);
    } finally {
        if (connection) {
            await mongoose.connection.close();
            console.log("Database connection closed");
        }
    }
}

// Run the insertion
insertTemplates(); 