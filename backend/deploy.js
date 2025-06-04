/**
 * Deployment script for About and Contact routes
 * 
 * This script verifies the models and creates initial content if needed.
 * It can be run directly on the server to ensure the routes work correctly.
 */

// Load environment variables
require('dotenv').config();

// Import mongoose and models
const mongoose = require('mongoose');
const About = require('./models/About');
const Contact = require('./models/Contact');

console.log('Starting deployment script...');

// Connect to MongoDB
console.log('Connecting to MongoDB...');
mongoose.connect(process.env.MONGODB_URI)
  .then(() => {
    console.log('Connected to MongoDB');
    deploy();
  })
  .catch((err) => {
    console.error(`MongoDB connection error: ${err}`);
    process.exit(1);
  });

// Deploy function
async function deploy() {
  try {
    console.log('Checking About model...');
    const aboutCount = await About.countDocuments();
    console.log(`Found ${aboutCount} About documents`);

    // Create initial About content if none exists
    if (aboutCount === 0) {
      console.log('Creating initial About content...');
      const initialAbout = new About({
        title: 'About EventWish',
        htmlCode: `
          <h1>About EventWish</h1>
          <p>EventWish is an app for creating and sharing beautiful event wishes with your friends and family.</p>
          <p>Features include:</p>
          <ul>
            <li>Beautiful templates for various occasions</li>
            <li>Easy customization options</li>
            <li>Simple sharing to social media</li>
            <li>Reminders for important dates</li>
          </ul>
          <p>Version: 1.0.0</p>
        `,
        isActive: true
      });
      await initialAbout.save();
      console.log('Initial About content created successfully');
    }

    console.log('Checking Contact model...');
    const contactCount = await Contact.countDocuments();
    console.log(`Found ${contactCount} Contact documents`);

    // Create initial Contact content if none exists
    if (contactCount === 0) {
      console.log('Creating initial Contact content...');
      const initialContact = new Contact({
        title: 'Contact Us',
        htmlCode: `
          <h1>Contact Us</h1>
          <p>We'd love to hear from you! If you have any questions, suggestions, or feedback about EventWish, please don't hesitate to reach out.</p>
          <h2>Support Email</h2>
          <p><a href="mailto:support@eventwish.com">support@eventwish.com</a></p>
          <h2>Follow Us</h2>
          <p>Stay updated with our latest features and announcements:</p>
          <ul>
            <li><a href="https://twitter.com/eventwish">Twitter</a></li>
            <li><a href="https://facebook.com/eventwish">Facebook</a></li>
            <li><a href="https://instagram.com/eventwish">Instagram</a></li>
          </ul>
        `,
        isActive: true
      });
      await initialContact.save();
      console.log('Initial Contact content created successfully');
    }

    // Verify models work correctly
    console.log('Verifying About.getActive()...');
    const activeAbout = await About.getActive();
    if (activeAbout) {
      console.log('About.getActive() successful:', { 
        id: activeAbout._id,
        title: activeAbout.title
      });
    } else {
      console.error('About.getActive() returned null');
    }

    console.log('Verifying Contact.getActive()...');
    const activeContact = await Contact.getActive();
    if (activeContact) {
      console.log('Contact.getActive() successful:', { 
        id: activeContact._id,
        title: activeContact.title
      });
    } else {
      console.error('Contact.getActive() returned null');
    }

    console.log('Deployment completed successfully');
    console.log('');
    console.log('Next steps:');
    console.log('1. Restart the server to apply changes');
    console.log('2. Test the endpoints:');
    console.log('   - GET /api/about');
    console.log('   - GET /api/contact');
  } catch (error) {
    console.error(`Deployment error: ${error.message}`, error);
  } finally {
    // Close database connection
    mongoose.connection.close();
    console.log('Database connection closed');
    process.exit(0);
  }
} 