/**
 * Simple test script to verify About and Contact models
 */

// Load environment variables
require('dotenv').config();

// Import mongoose and models
const mongoose = require('mongoose');
const About = require('./models/About');
const Contact = require('./models/Contact');

// Connect to MongoDB
console.log('Connecting to MongoDB...');
mongoose.connect(process.env.MONGODB_URI)
  .then(() => {
    console.log('Connected to MongoDB for testing');
    runTests();
  })
  .catch((err) => {
    console.error(`MongoDB connection error: ${err}`);
    process.exit(1);
  });

// Run tests
async function runTests() {
  try {
    console.log('Starting route tests');

    // Test About model
    console.log('Testing About model');
    const aboutCount = await About.countDocuments();
    console.log(`Found ${aboutCount} About documents`);

    // Create test About content if none exists
    if (aboutCount === 0) {
      console.log('Creating test About content');
      const testAbout = new About({
        title: 'Test About',
        htmlCode: '<h1>Test About Content</h1><p>This is test content.</p>',
        isActive: true
      });
      await testAbout.save();
      console.log('Test About content created');
    }

    // Test getActive method
    const activeAbout = await About.getActive();
    if (activeAbout) {
      console.log('About.getActive() successful:', { 
        id: activeAbout._id,
        title: activeAbout.title
      });
    } else {
      console.error('About.getActive() returned null');
    }

    // Test Contact model
    console.log('Testing Contact model');
    const contactCount = await Contact.countDocuments();
    console.log(`Found ${contactCount} Contact documents`);

    // Create test Contact content if none exists
    if (contactCount === 0) {
      console.log('Creating test Contact content');
      const testContact = new Contact({
        title: 'Test Contact',
        htmlCode: '<h1>Test Contact Content</h1><p>This is test content.</p>',
        isActive: true
      });
      await testContact.save();
      console.log('Test Contact content created');
    }

    // Test getActive method
    const activeContact = await Contact.getActive();
    if (activeContact) {
      console.log('Contact.getActive() successful:', { 
        id: activeContact._id,
        title: activeContact.title
      });
    } else {
      console.error('Contact.getActive() returned null');
    }

    console.log('Tests completed successfully');
  } catch (error) {
    console.error(`Test error: ${error.message}`, error);
  } finally {
    // Close database connection
    mongoose.connection.close();
    console.log('Database connection closed');
    process.exit(0);
  }
} 