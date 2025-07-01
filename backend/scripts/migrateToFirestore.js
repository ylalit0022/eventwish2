const mongoose = require('mongoose');
const { admin } = require('../config/firebase');
const logger = require('../config/logger');
const User = require('../models/firestore/User');
const MongoUser = require('../models/User');

// MongoDB connection string
const MONGODB_URI = process.env.MONGODB_URI || 'mongodb://localhost:27017/eventwish';

/**
 * Migrate a single user from MongoDB to Firestore
 * @param {Object} mongoUser MongoDB user document
 * @returns {Promise<void>}
 */
async function migrateUser(mongoUser) {
    try {
        // Convert MongoDB _id to string if it exists
        const userData = mongoUser.toObject();
        if (userData._id) {
            userData._id = userData._id.toString();
        }

        // Create user in Firestore
        await User.create({
            uid: userData.uid,
            ...userData
        });

        logger.info(`Successfully migrated user: ${userData.uid}`);
    } catch (error) {
        logger.error(`Error migrating user ${mongoUser.uid}: ${error.message}`);
        throw error;
    }
}

/**
 * Main migration function
 */
async function migrate() {
    try {
        // Connect to MongoDB
        await mongoose.connect(MONGODB_URI, {
            useNewUrlParser: true,
            useUnifiedTopology: true
        });
        logger.info('Connected to MongoDB');

        // Get all users from MongoDB
        const users = await MongoUser.find({});
        logger.info(`Found ${users.length} users to migrate`);

        // Migrate users in batches of 500
        const batchSize = 500;
        for (let i = 0; i < users.length; i += batchSize) {
            const batch = users.slice(i, i + batchSize);
            await Promise.all(batch.map(user => migrateUser(user)));
            logger.info(`Migrated batch ${Math.floor(i / batchSize) + 1} of ${Math.ceil(users.length / batchSize)}`);
        }

        logger.info('Migration completed successfully');
    } catch (error) {
        logger.error(`Migration failed: ${error.message}`);
        process.exit(1);
    } finally {
        // Close MongoDB connection
        await mongoose.disconnect();
        logger.info('Disconnected from MongoDB');
        process.exit(0);
    }
}

// Run migration
migrate(); 