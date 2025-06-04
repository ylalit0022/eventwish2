/**
 * Test script to verify About and Contact routes
 * 
 * This script tests the About and Contact routes by making direct requests
 * to the database models to ensure they work as expected.
 */

// Load environment variables
require('dotenv').config();

// Import mongoose and models
const mongoose = require('mongoose');
const About = require('./models/About');
const Contact = require('./models/Contact');
const logger = require('./config/logger');

// Connect to MongoDB
mongoose.connect(process.env.MONGODB_URI)
  .then(() => {
    logger.info('Connected to MongoDB for testing');
    runTests();
  })
  .catch((err) => {
    logger.error(`MongoDB connection error: ${err}`);
    process.exit(1);
  });

// Run tests
async function runTests() {
  try {
    logger.info('Starting route tests');

    // Test About model
    logger.info('Testing About model');
    const aboutCount = await About.countDocuments();
    logger.info(`Found ${aboutCount} About documents`);

    // Create test About content if none exists
    if (aboutCount === 0) {
      logger.info('Creating test About content');
      const testAbout = new About({
        title: 'Test About',
        htmlCode: '<h1>Test About Content</h1><p>This is test content.</p>',
        isActive: true
      });
      await testAbout.save();
      logger.info('Test About content created');
    }

    // Test getActive method
    const activeAbout = await About.getActive();
    if (activeAbout) {
      logger.info('About.getActive() successful', { 
        id: activeAbout._id,
        title: activeAbout.title
      });
    } else {
      logger.error('About.getActive() returned null');
    }

    // Test Contact model
    logger.info('Testing Contact model');
    const contactCount = await Contact.countDocuments();
    logger.info(`Found ${contactCount} Contact documents`);

    // Create test Contact content if none exists
    if (contactCount === 0) {
      logger.info('Creating test Contact content');
      const testContact = new Contact({
        title: 'Test Contact',
        htmlCode: '<h1>Test Contact Content</h1><p>This is test content.</p>',
        isActive: true
      });
      await testContact.save();
      logger.info('Test Contact content created');
    }

    // Test getActive method
    const activeContact = await Contact.getActive();
    if (activeContact) {
      logger.info('Contact.getActive() successful', { 
        id: activeContact._id,
        title: activeContact.title
      });
    } else {
      logger.error('Contact.getActive() returned null');
    }

    logger.info('Tests completed successfully');
  } catch (error) {
    logger.error(`Test error: ${error.message}`, { error });
  } finally {
    // Close database connection
    mongoose.connection.close();
    logger.info('Database connection closed');
  }
} 