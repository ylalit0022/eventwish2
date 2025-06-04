/**
 * Sample data for About content
 * 
 * This file contains sample HTML content for the About section with external links
 * that will redirect to a browser when clicked.
 */

const mongoose = require('mongoose');
const About = require('../models/About');
const logger = require('../config/logger');

/**
 * Insert sample About content into the database
 */
const insertAboutSample = async () => {
  try {
    // Check if there's already active content
    const existingAbout = await About.getActive();
    
    if (existingAbout) {
      logger.info('About content already exists, skipping sample data insertion');
      return;
    }
    
    // Sample About content with external links
    const aboutContent = new About({
      title: 'About EventWish',
      htmlCode: `
        <div class="about-container">
          <h1>Welcome to EventWish</h1>
          
          <div class="about-section">
            <h2>Our Mission</h2>
            <p>EventWish is designed to help you create beautiful, personalized wishes for all your special occasions. 
            Whether it's birthdays, anniversaries, or holidays, we've got you covered with creative templates.</p>
          </div>
          
          <div class="about-section">
            <h2>Features</h2>
            <ul>
              <li>Hundreds of customizable templates</li>
              <li>Easy sharing on social media</li>
              <li>Festival reminders and notifications</li>
              <li>Personalized wish creation</li>
            </ul>
          </div>
          
          <div class="about-section">
            <h2>External Resources</h2>
            <p>Learn more about digital greetings:</p>
            <ul>
              <li><a href="https://www.wikipedia.org/wiki/Greeting_card" target="_blank">History of Greeting Cards</a></li>
              <li><a href="https://www.canva.com/create/cards/" target="_blank">Design Inspiration</a></li>
              <li><a href="https://www.pexels.com/" target="_blank">Free Stock Photos</a></li>
            </ul>
          </div>
          
          <div class="about-section">
            <h2>Our Team</h2>
            <p>We are a dedicated team of developers and designers passionate about creating meaningful digital experiences.</p>
            <p>Visit our <a href="https://github.com/trending" target="_blank">GitHub</a> to see other projects.</p>
          </div>
          
          <div class="about-section">
            <h2>Privacy Policy</h2>
            <p>We value your privacy. Read our full <a href="https://www.privacypolicygenerator.info/" target="_blank">Privacy Policy</a>.</p>
          </div>
          
          <div class="about-section">
            <h2>Version</h2>
            <p>EventWish v2.0.1</p>
            <p>Last Updated: June 2025</p>
          </div>
        </div>
        
        <style>
          .about-container {
            font-family: 'Arial', sans-serif;
            color: #333;
            max-width: 800px;
            margin: 0 auto;
            padding: 20px;
          }
          
          h1 {
            color: #4a90e2;
            text-align: center;
            margin-bottom: 30px;
          }
          
          h2 {
            color: #4a90e2;
            border-bottom: 1px solid #eee;
            padding-bottom: 10px;
            margin-top: 30px;
          }
          
          .about-section {
            margin-bottom: 30px;
          }
          
          ul {
            padding-left: 20px;
          }
          
          li {
            margin-bottom: 8px;
          }
          
          a {
            color: #4a90e2;
            text-decoration: none;
            font-weight: bold;
          }
          
          a:hover {
            text-decoration: underline;
          }
        </style>
      `,
      isActive: true
    });
    
    await aboutContent.save();
    logger.info('Sample About content inserted successfully');
    console.log('Sample About content inserted successfully');
    
  } catch (error) {
    logger.error(`Error inserting sample About content: ${error.message}`);
    console.error('Error inserting sample About content:', error);
  }
};

module.exports = { insertAboutSample };

// Run directly if this script is executed directly
if (require.main === module) {
  const mongoose = require('mongoose');
  const dotenv = require('dotenv');
  
  // Load environment variables
  dotenv.config();
  
  // Connect to MongoDB
  mongoose.connect(process.env.MONGODB_URI, {
    useNewUrlParser: true,
    useUnifiedTopology: true
  })
  .then(async () => {
    console.log('Connected to MongoDB');
    await insertAboutSample();
    console.log('Done!');
    process.exit(0);
  })
  .catch(err => {
    console.error('MongoDB connection error:', err);
    process.exit(1);
  });
} 