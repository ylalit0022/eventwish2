const mongoose = require('mongoose');
const User = require('../models/User');
const logger = require('../utils/logger');

/**
 * Script to fix malformed cachedHomeFeed data in existing user documents
 * The issue: cachedHomeFeed was storing full template objects instead of {type, templateIds}
 * This script clears the malformed data to allow proper validation
 */

async function fixCachedHomeFeedData() {
    try {
        // Connect to MongoDB
        await mongoose.connect(process.env.MONGODB_URI || 'mongodb://localhost:27017/eventwish');
        logger.info('📊 Connected to MongoDB for cachedHomeFeed data fix');

        // Find users with malformed cachedHomeFeed data
        const usersWithMalformedData = await User.find({
            cachedHomeFeed: { $exists: true, $ne: [] }
        }).lean();

        logger.info(`🔍 Found ${usersWithMalformedData.length} users with cachedHomeFeed data to check`);

        let fixedCount = 0;
        let errorCount = 0;

        for (const user of usersWithMalformedData) {
            try {
                // Check if cachedHomeFeed has the correct structure
                const hasCorrectStructure = user.cachedHomeFeed.every(item => 
                    item.type && Array.isArray(item.templateIds)
                );

                if (!hasCorrectStructure) {
                    // Clear malformed cachedHomeFeed data
                    await User.updateOne(
                        { _id: user._id },
                        {
                            $unset: {
                                cachedHomeFeed: "",
                                homeFeedLastGeneratedAt: ""
                            }
                        }
                    );
                    
                    fixedCount++;
                    logger.info(`✅ Fixed cachedHomeFeed for user: ${user.uid || user._id}`);
                } else {
                    logger.debug(`✓ User ${user.uid || user._id} already has correct cachedHomeFeed structure`);
                }
            } catch (error) {
                errorCount++;
                logger.error(`❌ Error fixing user ${user.uid || user._id}:`, error);
            }
        }

        logger.info(`🎉 Fix completed: ${fixedCount} users fixed, ${errorCount} errors`);
        
        // Verify the fix by checking for validation errors
        logger.info('🔍 Verifying fix by checking for remaining validation issues...');
        
        const testUser = await User.findOne({ cachedHomeFeed: { $exists: true, $ne: [] } });
        if (testUser) {
            try {
                await testUser.save();
                logger.info('✅ Validation test passed - no more schema errors');
            } catch (validationError) {
                logger.error('❌ Validation test failed - still have schema errors:', validationError);
            }
        } else {
            logger.info('📝 No users with cachedHomeFeed data found for validation test');
        }

    } catch (error) {
        logger.error('❌ Error in fix script:', error);
    } finally {
        await mongoose.disconnect();
        logger.info('📊 Disconnected from MongoDB');
    }
}

// Run the fix if this script is executed directly
if (require.main === module) {
    fixCachedHomeFeedData().then(() => {
        process.exit(0);
    }).catch(error => {
        logger.error('❌ Script failed:', error);
        process.exit(1);
    });
}

module.exports = fixCachedHomeFeedData; 