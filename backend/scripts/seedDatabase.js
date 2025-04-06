const mongoose = require('mongoose');
const Coins = require('../models/Coins');
const seedData = require('../data/seedCoins');
const logger = require('../config/logger');

const seedDatabase = async () => {
    try {
        // Connect to MongoDB
        await mongoose.connect(process.env.MONGODB_URI, {
            useNewUrlParser: true,
            useUnifiedTopology: true
        });
        
        logger.info('Connected to MongoDB');

        // Check if device already exists
        const existingDevice = await Coins.findOne({ deviceId: seedData.deviceId });
        
        if (existingDevice) {
            logger.info('Device already exists in database');
            return;
        }

        // Create new coins document
        const coinsDoc = new Coins(seedData);
        await coinsDoc.save();
        
        logger.info('Seed data inserted successfully');
        logger.debug('Inserted document:', coinsDoc);

    } catch (error) {
        logger.error('Error seeding database:', error);
    } finally {
        // Close database connection if running standalone
        if (require.main === module) {
            await mongoose.connection.close();
            logger.info('Database connection closed');
        }
    }
};

// Run if called directly (node seedDatabase.js)
if (require.main === module) {
    // Load environment variables
    require('dotenv').config({ path: '../.env' });
    
    seedDatabase().then(() => {
        process.exit(0);
    }).catch(err => {
        console.error('Seeding failed:', err);
        process.exit(1);
    });
}

module.exports = seedDatabase;
