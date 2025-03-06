require('dotenv').config({ path: require('path').resolve(__dirname, '../.env') });
const mongoose = require('mongoose');
const Template = require('../models/Template');

// Sample template data generator functions
const categories = ['Birthday', 'Wedding', 'Anniversary', 'Cultural', 'Graduation', 'New Year', 'Valentine', 'Christmas', 'Other'];

const generateRandomTemplate = (index) => {
    const category = categories[Math.floor(Math.random() * categories.length)];
    const baseTitle = `${category} Template ${Math.floor(index / categories.length) + 1}`;
    
    return {
        title: baseTitle,
        category: category,
        htmlContent: `<div class="template-${index}">
            <h1>${baseTitle}</h1>
            <div class="content-area">
                <p class="message">Your special message here</p>
                <div class="decoration"></div>
            </div>
        </div>`,
        cssContent: `.template-${index} {
            background: linear-gradient(45deg, #${Math.floor(Math.random()*16777215).toString(16)}, #${Math.floor(Math.random()*16777215).toString(16)});
            padding: 20px;
            border-radius: 10px;
            text-align: center;
        }
        .template-${index} .content-area {
            background: rgba(255,255,255,0.9);
            margin: 15px;
            padding: 20px;
            border-radius: 8px;
        }
        .template-${index} .decoration {
            width: 50px;
            height: 50px;
            margin: 10px auto;
            background: #${Math.floor(Math.random()*16777215).toString(16)};
            transform: rotate(45deg);
        }`,
        jsContent: `document.querySelector('.template-${index} .decoration').addEventListener('click', function() {
            this.style.transform = 'rotate(' + (45 + 360) + 'deg)';
            this.style.transition = 'transform 1s ease';
        });`,
        previewUrl: `https://currentedu365.in/wp-content/uploads/2024/09/template-${index % 5 + 1}.jpg`,
        status: true,
        categoryIcon: `ic_${category.toLowerCase()}`
    };
};

// Function to insert templates
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

        // Generate and insert 100 templates
        const templates = Array.from({ length: 100 }, (_, index) => generateRandomTemplate(index));
        const result = await Template.insertMany(templates);

        console.log(`Successfully inserted ${result.length} templates`);

        // Display some sample data
        console.log('\nSample of inserted templates:');
        result.slice(0, 5).forEach(template => {
            console.log(`- ${template.title} (${template.category})`);
        });

    } catch (error) {
        console.error('Error inserting templates:', error.message);
    } finally {
        await mongoose.connection.close();
        console.log('\nDatabase connection closed');
    }
}

// Run the insertion
insertTemplates();