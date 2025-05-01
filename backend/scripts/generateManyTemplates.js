/**
 * Script to generate up to 500 templates with different categories
 * 
 * This script:
 * 1. Creates a variety of category icons if they don't exist
 * 2. Generates a large number of templates with different content
 * 3. Assigns templates to various categories
 */

const mongoose = require('mongoose');
const Template = require('../models/Template');
const CategoryIcon = require('../models/CategoryIcon');
require('dotenv').config();

// Define an extended set of categories with icon URLs
const categories = [
  { name: 'Birthday', icon: 'https://cdn-icons-png.flaticon.com/512/3159/3159213.png' },
  { name: 'Wedding', icon: 'https://cdn-icons-png.flaticon.com/512/2955/2955010.png' },
  { name: 'Anniversary', icon: 'https://cdn-icons-png.flaticon.com/512/3656/3656949.png' },
  { name: 'Graduation', icon: 'https://cdn-icons-png.flaticon.com/512/3526/3526283.png' },
  { name: 'Holiday', icon: 'https://cdn-icons-png.flaticon.com/512/4213/4213147.png' },
  { name: 'Baby Shower', icon: 'https://cdn-icons-png.flaticon.com/512/3631/3631657.png' },
  { name: 'Engagement', icon: 'https://cdn-icons-png.flaticon.com/512/2138/2138381.png' },
  { name: 'Retirement', icon: 'https://cdn-icons-png.flaticon.com/512/2058/2058676.png' },
  { name: 'Farewell', icon: 'https://cdn-icons-png.flaticon.com/512/6213/6213351.png' },
  { name: 'Thanksgiving', icon: 'https://cdn-icons-png.flaticon.com/512/7137/7137213.png' },
  { name: 'Christmas', icon: 'https://cdn-icons-png.flaticon.com/512/4363/4363765.png' },
  { name: 'New Year', icon: 'https://cdn-icons-png.flaticon.com/512/3127/3127136.png' },
  { name: 'Valentine', icon: 'https://cdn-icons-png.flaticon.com/512/1029/1029183.png' },
  { name: 'Easter', icon: 'https://cdn-icons-png.flaticon.com/512/9696/9696619.png' },
  { name: 'Halloween', icon: 'https://cdn-icons-png.flaticon.com/512/8283/8283768.png' },
  { name: 'Business', icon: 'https://cdn-icons-png.flaticon.com/512/2190/2190448.png' },
  { name: 'Conference', icon: 'https://cdn-icons-png.flaticon.com/512/3429/3429433.png' },
  { name: 'Workshop', icon: 'https://cdn-icons-png.flaticon.com/512/4384/4384358.png' },
  { name: 'Seminar', icon: 'https://cdn-icons-png.flaticon.com/512/6614/6614677.png' },
  { name: 'Party', icon: 'https://cdn-icons-png.flaticon.com/512/3225/3225284.png' }
];

// Colors for templates
const colors = [
  '#FF6B6B', '#4ECDC4', '#1A535C', '#FF9F1C', '#2EC4B6', 
  '#E71D36', '#011627', '#FDFFFC', '#2EC4B6', '#E71D36',
  '#5E60CE', '#64DFDF', '#80FFDB', '#7400B8', '#6930C3',
  '#5E60CE', '#48BFE3', '#56CFE1', '#72EFDD', '#80FFDB',
  '#001219', '#005F73', '#0A9396', '#94D2BD', '#E9D8A6',
  '#EE9B00', '#CA6702', '#BB3E03', '#AE2012', '#9B2226'
];

// Fonts for templates
const fonts = [
  'Arial, sans-serif',
  'Helvetica, sans-serif',
  'Georgia, serif',
  'Times New Roman, serif',
  'Courier New, monospace',
  'Verdana, sans-serif',
  'Trebuchet MS, sans-serif',
  'Impact, sans-serif',
  'Comic Sans MS, cursive',
  'Palatino, serif'
];

// Title patterns
const titlePatterns = [
  'Elegant {category}', 
  'Modern {category}', 
  'Classic {category}', 
  'Vibrant {category}', 
  'Minimalist {category}',
  'Creative {category}',
  'Professional {category}',
  'Playful {category}',
  'Sophisticated {category}',
  'Rustic {category}',
  'Premium {category}',
  'Artistic {category}',
  'Simple {category}',
  'Bold {category}',
  'Fancy {category}'
];

// Generate a random placeholder image
function getRandomPlaceholderImage(category, index) {
  const colors = ['87CEEB', 'FAFAD2', 'C0C0C0', '000000', 'FF0000', 'FFEB3B', '4CAF50', '2196F3', '9C27B0', 'FF9800'];
  const randomColor = colors[Math.floor(Math.random() * colors.length)];
  const textColor = randomColor === '000000' ? 'FFFFFF' : '000000';
  return `https://via.placeholder.com/400x300/${randomColor}/${textColor}?text=${encodeURIComponent(category + ' ' + index)}`;
}

// Generate random HTML template content
function generateTemplateContent(category, index, titlePattern) {
  const font = fonts[Math.floor(Math.random() * fonts.length)];
  const color = colors[Math.floor(Math.random() * colors.length)];
  
  const title = titlePattern.replace('{category}', category);
  
  const html = `
    <div class="${category.toLowerCase()}-template-${index}">
      <h1>${title}</h1>
      <p>This is a template for ${category.toLowerCase()} events.</p>
      <div class="details">
        <p>Date: [Event Date]</p>
        <p>Time: [Event Time]</p>
        <p>Location: [Event Location]</p>
      </div>
      <div class="message">
        <p>You're invited to join us for this special occasion!</p>
      </div>
      <div class="footer">
        <p>RSVP: [Contact Information]</p>
      </div>
    </div>
  `;
  
  const css = `
    .${category.toLowerCase()}-template-${index} {
      font-family: ${font};
      color: ${color};
      text-align: center;
      padding: 20px;
      max-width: 600px;
      margin: 0 auto;
      border: 1px solid #ddd;
      border-radius: 8px;
    }
    
    .${category.toLowerCase()}-template-${index} h1 {
      margin-bottom: 20px;
      font-size: 24px;
    }
    
    .${category.toLowerCase()}-template-${index} .details {
      margin: 20px 0;
      padding: 10px;
      background-color: rgba(255,255,255,0.8);
      border-radius: 4px;
    }
    
    .${category.toLowerCase()}-template-${index} .message {
      font-style: italic;
      margin: 15px 0;
    }
    
    .${category.toLowerCase()}-template-${index} .footer {
      margin-top: 30px;
      font-size: 14px;
      border-top: 1px solid #eee;
      padding-top: 10px;
    }
  `;
  
  const js = `console.log("${category} template ${index} loaded!");`;
  
  return { html, css, js };
}

// Create category icons if they don't exist
async function createCategoryIcons() {
  console.log('Creating category icons...');
  const categoryIcons = {};
  
  // Process each category
  for (const category of categories) {
    // Check if this category icon already exists
    let icon = await CategoryIcon.findOne({ category: category.name });
    
    if (!icon) {
      // Create a new icon
      console.log(`Creating new icon for category: ${category.name}`);
      icon = new CategoryIcon({
        id: new mongoose.Types.ObjectId().toString(),
        category: category.name,
        categoryIcon: category.icon,
        iconType: 'URL'
      });
      
      await icon.save();
    } else {
      console.log(`Found existing icon for category: ${category.name}`);
    }
    
    // Store the icon reference
    categoryIcons[category.name] = icon;
  }
  
  return categoryIcons;
}

// Generate templates
async function generateTemplates(categoryIcons, count = 500) {
  console.log(`Generating ${count} templates...`);
  
  const templateCount = await Template.countDocuments();
  console.log(`Current template count: ${templateCount}`);
  
  // Initialize counters
  let created = 0;
  let skipped = 0;
  const templatesPerCategory = {};
  
  // Calculate templates per category
  const categoryNames = Object.keys(categoryIcons);
  const baseTemplatesPerCategory = Math.floor(count / categoryNames.length);
  const remainder = count % categoryNames.length;
  
  categoryNames.forEach((category, index) => {
    templatesPerCategory[category] = baseTemplatesPerCategory + (index < remainder ? 1 : 0);
  });
  
  // Create templates for each category
  for (const category of categoryNames) {
    console.log(`Creating ${templatesPerCategory[category]} templates for category: ${category}`);
    
    const icon = categoryIcons[category];
    
    for (let i = 1; i <= templatesPerCategory[category]; i++) {
      const titlePattern = titlePatterns[Math.floor(Math.random() * titlePatterns.length)];
      const title = `${titlePattern.replace('{category}', category)} ${i}`;
      
      // Skip if template with this title already exists
      const existingTemplate = await Template.findOne({ title });
      if (existingTemplate) {
        console.log(`Template already exists: ${title}`);
        skipped++;
        continue;
      }
      
      // Generate content
      const content = generateTemplateContent(category, i, titlePattern);
      
      // Create new template
      const template = new Template({
        title,
        category,
        htmlContent: content.html,
        cssContent: content.css,
        jsContent: content.js,
        previewUrl: getRandomPlaceholderImage(category, i),
        status: true,
        categoryIcon: icon._id
      });
      
      await template.save();
      created++;
      
      // Log progress every 50 templates
      if (created % 50 === 0) {
        console.log(`Progress: ${created} templates created`);
      }
    }
  }
  
  return { created, skipped };
}

// Run the script
async function run() {
  try {
    console.log('Connecting to MongoDB...');
    await mongoose.connect(process.env.MONGODB_URI, {
      useNewUrlParser: true,
      useUnifiedTopology: true
    });
    console.log('MongoDB connected');
    
    // Create category icons
    const categoryIcons = await createCategoryIcons();
    console.log(`Created/found ${Object.keys(categoryIcons).length} category icons`);
    
    // Generate templates
    const result = await generateTemplates(categoryIcons, 500);
    console.log(`Template generation complete: ${result.created} created, ${result.skipped} skipped`);
    
    // Count templates by category
    const categories = await Template.aggregate([
      { $group: { _id: '$category', count: { $sum: 1 } } },
      { $sort: { count: -1 } }
    ]);
    
    console.log('\nTemplates by category:');
    categories.forEach(cat => {
      console.log(`${cat._id}: ${cat.count} templates`);
    });
    
    const totalTemplates = await Template.countDocuments();
    console.log(`\nTotal templates in database: ${totalTemplates}`);
    
  } catch (error) {
    console.error('Error:', error);
  } finally {
    await mongoose.disconnect();
    console.log('MongoDB disconnected');
  }
}

// Run the script
if (require.main === module) {
  run()
    .then(() => console.log('Script completed'))
    .catch(err => console.error('Script failed:', err));
}

module.exports = { run }; 