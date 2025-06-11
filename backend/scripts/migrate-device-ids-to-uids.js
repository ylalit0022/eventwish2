/**
 * Migration script to ensure all users have Firebase UIDs
 * 
 * This script:
 * 1. Identifies users who only have deviceId but no uid
 * 2. Generates temporary UIDs for them
 * 3. Updates their records with these UIDs
 * 
 * Usage:
 * node scripts/migrate-device-ids-to-uids.js
 */

require('dotenv').config();
const mongoose = require('mongoose');
const crypto = require('crypto');
const User = require('../models/User');
const logger = require('../utils/logger');

// Function to generate a temporary Firebase-like UID
function generateTemporaryUid() {
    // Firebase UIDs are typically 28 characters, alphanumeric
    return 'temp_' + crypto.randomBytes(12).toString('hex');
}

async function migrateUsers() {
    try {
        logger.info('Starting user migration from deviceId to Firebase UID');
        
        // Connect to MongoDB
        await mongoose.connect(process.env.MONGODB_URI, {
            useNewUrlParser: true,
            useUnifiedTopology: true
        });
        
        logger.info('Connected to MongoDB');
        
        // Find users without uid
        const usersWithoutUid = await User.find({
            $or: [
                { uid: { $exists: false } },
                { uid: null }
            ]
        });
        
        logger.info(`Found ${usersWithoutUid.length} users without Firebase UID`);
        
        // Update each user with a temporary UID
        let updated = 0;
        let errors = 0;
        
        for (const user of usersWithoutUid) {
            try {
                // Generate a temporary UID
                const tempUid = generateTemporaryUid();
                
                // Update the user
                user.uid = tempUid;
                await user.save();
                
                logger.info(`Updated user with deviceId ${user.deviceId} to have temporary UID ${tempUid}`);
                updated++;
            } catch (error) {
                logger.error(`Error updating user ${user.deviceId}: ${error.message}`);
                errors++;
            }
        }
        
        logger.info(`Migration complete: ${updated} users updated, ${errors} errors`);
        
        // Disconnect from MongoDB
        await mongoose.disconnect();
        logger.info('Disconnected from MongoDB');
        
    } catch (error) {
        logger.error(`Migration failed: ${error.message}`);
        process.exit(1);
    }
}

// Run the migration
migrateUsers()
    .then(() => {
        logger.info('Migration script completed successfully');
        process.exit(0);
    })
    .catch(error => {
        logger.error(`Migration script failed: ${error.message}`);
        process.exit(1);
    }); 