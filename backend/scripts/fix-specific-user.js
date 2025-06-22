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
        console.log('✅ MongoDB connected for user fix');
    } catch (error) {
        console.error('❌ MongoDB connection error:', error);
        process.exit(1);
    }
};

// Fix specific user
const fixSpecificUser = async () => {
    try {
        const userId = 'DgCSTkbsJwfSO0NTRuq6XVXLB3u2';
        console.log(`🔄 Fixing user ${userId}...`);

        // Find the user first
        const user = await User.findOne({ uid: userId });
        if (!user) {
            console.log(`❌ User ${userId} not found`);
            return;
        }

        console.log('Current subscription data:', JSON.stringify(user.subscription, null, 2));

        // Update the user directly with proper enum values
        const updateResult = await User.updateOne(
            { uid: userId },
            { 
                $set: { 
                    'subscription.planLevel': 'NONE',
                    'subscription.plan': 'NONE'
                }
            }
        );

        console.log(`✅ Updated user ${userId}:`, updateResult);

        // Verify the fix
        const updatedUser = await User.findOne({ uid: userId });
        console.log('Updated subscription data:', JSON.stringify(updatedUser.subscription, null, 2));

        // Also fix any other users with empty string values
        const allEmptyUsers = await User.find({
            $or: [
                { 'subscription.planLevel': '' },
                { 'subscription.plan': '' },
                { 'subscriptionOffer.planAssigned': '' }
            ]
        });

        console.log(`Found ${allEmptyUsers.length} users with empty enum values`);

        for (const user of allEmptyUsers) {
            console.log(`Fixing user ${user.uid}...`);
            await User.updateOne(
                { uid: user.uid },
                { 
                    $set: { 
                        'subscription.planLevel': user.subscription?.planLevel === '' ? 'NONE' : user.subscription?.planLevel,
                        'subscription.plan': user.subscription?.plan === '' ? 'NONE' : user.subscription?.plan,
                        'subscriptionOffer.planAssigned': user.subscriptionOffer?.planAssigned === '' ? 'NONE' : user.subscriptionOffer?.planAssigned
                    }
                }
            );
        }

        console.log('✅ All users fixed!');

    } catch (error) {
        console.error('❌ Fix error:', error);
        throw error;
    }
};

// Main execution
const runFix = async () => {
    try {
        await connectDB();
        await fixSpecificUser();
        console.log('✅ Fix completed successfully!');
        process.exit(0);
    } catch (error) {
        console.error('❌ Fix failed:', error);
        process.exit(1);
    }
};

// Run if called directly
if (require.main === module) {
    runFix();
}

module.exports = { fixSpecificUser }; 