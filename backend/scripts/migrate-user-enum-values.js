const mongoose = require('mongoose');
const User = require('../models/User');

// MongoDB connection
const connectDB = async () => {
    try {
        const mongoURI = process.env.MONGODB_URI || 'mongodb://localhost:27017/eventwish';
        await mongoose.connect(mongoURI, {
            useNewUrlParser: true,
            useUnifiedTopology: true,
        });
        console.log('✅ MongoDB connected for migration');
    } catch (error) {
        console.error('❌ MongoDB connection error:', error);
        process.exit(1);
    }
};

// Migration function
const migrateUserEnumValues = async () => {
    try {
        console.log('🔄 Starting user enum values migration...');

        // Update users with empty string planLevel to 'NONE'
        const planLevelResult = await User.updateMany(
            { 'subscription.planLevel': '' },
            { $set: { 'subscription.planLevel': 'NONE' } }
        );
        console.log(`✅ Updated ${planLevelResult.modifiedCount} users with empty planLevel to 'NONE'`);

        // Update users with empty string plan to 'NONE'
        const planResult = await User.updateMany(
            { 'subscription.plan': '' },
            { $set: { 'subscription.plan': 'NONE' } }
        );
        console.log(`✅ Updated ${planResult.modifiedCount} users with empty plan to 'NONE'`);

        // Update subscription offers with empty string planAssigned to 'NONE'
        const offerResult = await User.updateMany(
            { 'subscriptionOffer.planAssigned': '' },
            { $set: { 'subscriptionOffer.planAssigned': 'NONE' } }
        );
        console.log(`✅ Updated ${offerResult.modifiedCount} users with empty planAssigned to 'NONE'`);

        // Find and fix any null or undefined values
        const nullPlanLevelResult = await User.updateMany(
            { 
                $or: [
                    { 'subscription.planLevel': { $exists: false } },
                    { 'subscription.planLevel': null },
                    { 'subscription.planLevel': undefined }
                ]
            },
            { $set: { 'subscription.planLevel': 'NONE' } }
        );
        console.log(`✅ Updated ${nullPlanLevelResult.modifiedCount} users with null/undefined planLevel to 'NONE'`);

        const nullPlanResult = await User.updateMany(
            { 
                $or: [
                    { 'subscription.plan': { $exists: false } },
                    { 'subscription.plan': null },
                    { 'subscription.plan': undefined }
                ]
            },
            { $set: { 'subscription.plan': 'NONE' } }
        );
        console.log(`✅ Updated ${nullPlanResult.modifiedCount} users with null/undefined plan to 'NONE'`);

        // Verify migration
        const remainingEmptyPlanLevel = await User.countDocuments({ 'subscription.planLevel': '' });
        const remainingEmptyPlan = await User.countDocuments({ 'subscription.plan': '' });
        const remainingEmptyOffer = await User.countDocuments({ 'subscriptionOffer.planAssigned': '' });

        if (remainingEmptyPlanLevel === 0 && remainingEmptyPlan === 0 && remainingEmptyOffer === 0) {
            console.log('✅ Migration completed successfully! No empty enum values remaining.');
        } else {
            console.log(`⚠️  Warning: ${remainingEmptyPlanLevel + remainingEmptyPlan + remainingEmptyOffer} empty enum values still remain.`);
        }

        // Show final counts
        const totalUsers = await User.countDocuments();
        const basicUsers = await User.countDocuments({ 'subscription.planLevel': 'BASIC' });
        const premiumUsers = await User.countDocuments({ 'subscription.planLevel': 'PREMIUM' });
        const proUsers = await User.countDocuments({ 'subscription.planLevel': 'PRO' });
        const noneUsers = await User.countDocuments({ 'subscription.planLevel': 'NONE' });

        console.log('\n📊 Final Statistics:');
        console.log(`Total Users: ${totalUsers}`);
        console.log(`BASIC: ${basicUsers}`);
        console.log(`PREMIUM: ${premiumUsers}`);
        console.log(`PRO: ${proUsers}`);
        console.log(`NONE: ${noneUsers}`);

    } catch (error) {
        console.error('❌ Migration error:', error);
        throw error;
    }
};

// Main execution
const runMigration = async () => {
    try {
        await connectDB();
        await migrateUserEnumValues();
        console.log('✅ Migration completed successfully!');
        process.exit(0);
    } catch (error) {
        console.error('❌ Migration failed:', error);
        process.exit(1);
    }
};

// Run if called directly
if (require.main === module) {
    runMigration();
}

module.exports = { migrateUserEnumValues }; 