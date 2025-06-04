/**
 * Sample data for Contact content
 * 
 * This file contains sample HTML content for the Contact section with external links
 * that will redirect to a browser when clicked.
 */

const mongoose = require('mongoose');
const Contact = require('../models/Contact');
const logger = require('../config/logger');

/**
 * Insert sample Contact content into the database
 */
const insertContactSample = async () => {
  try {
    // Check if there's already active content
    const existingContact = await Contact.getActive();
    
    if (existingContact) {
      logger.info('Contact content already exists, skipping sample data insertion');
      return;
    }
    
    // Sample Contact content with external links
    const contactContent = new Contact({
      title: 'Contact Us',
      htmlCode: `
        <div class="contact-container">
          <h1>Get in Touch</h1>
          
          <div class="contact-section">
            <h2>Customer Support</h2>
            <p>We're here to help! If you have any questions or need assistance, please reach out to us.</p>
            <p><strong>Email:</strong> <a href="mailto:support@eventwish.example.com">support@eventwish.example.com</a></p>
            <p><strong>Response Time:</strong> Within 24 hours</p>
          </div>
          
          <div class="contact-section">
            <h2>Social Media</h2>
            <p>Connect with us on social media for updates, tips, and inspiration:</p>
            <ul class="social-links">
              <li><a href="https://www.facebook.com" target="_blank">Facebook</a></li>
              <li><a href="https://www.twitter.com" target="_blank">Twitter</a></li>
              <li><a href="https://www.instagram.com" target="_blank">Instagram</a></li>
              <li><a href="https://www.pinterest.com" target="_blank">Pinterest</a></li>
            </ul>
          </div>
          
          <div class="contact-section">
            <h2>Feedback</h2>
            <p>We value your feedback! Please share your thoughts and suggestions:</p>
            <p><a href="https://forms.gle/example" target="_blank">Submit Feedback Form</a></p>
          </div>
          
          <div class="contact-section">
            <h2>Report Issues</h2>
            <p>Found a bug or experiencing technical issues? Let us know:</p>
            <p><a href="https://github.com/issues" target="_blank">Report on GitHub</a></p>
            <p>Or email us at <a href="mailto:bugs@eventwish.example.com">bugs@eventwish.example.com</a></p>
          </div>
          
          <div class="contact-section">
            <h2>Business Inquiries</h2>
            <p>For partnerships, advertising, or other business opportunities:</p>
            <p><a href="mailto:business@eventwish.example.com">business@eventwish.example.com</a></p>
          </div>
          
          <div class="contact-section">
            <h2>FAQ</h2>
            <p>Check our <a href="https://www.eventwish.example.com/faq" target="_blank">Frequently Asked Questions</a> page for quick answers to common questions.</p>
          </div>
          
          <div class="contact-section">
            <h2>Privacy Concerns</h2>
            <p>Questions about your data or privacy? Contact our Data Protection Officer:</p>
            <p><a href="mailto:privacy@eventwish.example.com">privacy@eventwish.example.com</a></p>
            <p>View our <a href="https://www.privacypolicygenerator.info/" target="_blank">Privacy Policy</a></p>
          </div>
        </div>
        
        <style>
          .contact-container {
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
          
          .contact-section {
            margin-bottom: 30px;
          }
          
          ul.social-links {
            list-style-type: none;
            padding: 0;
            display: flex;
            flex-wrap: wrap;
            gap: 15px;
          }
          
          ul.social-links li {
            margin-bottom: 8px;
          }
          
          ul.social-links a {
            display: inline-block;
            padding: 8px 15px;
            background-color: #4a90e2;
            color: white;
            border-radius: 4px;
            text-decoration: none;
            transition: background-color 0.3s;
          }
          
          ul.social-links a:hover {
            background-color: #3a7bc8;
          }
          
          a {
            color: #4a90e2;
            text-decoration: none;
            font-weight: bold;
          }
          
          a:not(.social-links a):hover {
            text-decoration: underline;
          }
        </style>
      `,
      isActive: true
    });
    
    await contactContent.save();
    logger.info('Sample Contact content inserted successfully');
    console.log('Sample Contact content inserted successfully');
    
  } catch (error) {
    logger.error(`Error inserting sample Contact content: ${error.message}`);
    console.error('Error inserting sample Contact content:', error);
  }
};

module.exports = { insertContactSample };

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
    await insertContactSample();
    console.log('Done!');
    process.exit(0);
  })
  .catch(err => {
    console.error('MongoDB connection error:', err);
    process.exit(1);
  });
} 