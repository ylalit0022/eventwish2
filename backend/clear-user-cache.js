const mongoose = require('mongoose');
const User = require('./models/User');

// Connect to MongoDB
mongoose.connect('mongodb+srv://lalit:Lalit%402022@cluster0.0tqcj.mongodb.net/eventwish?retryWrites=true&w=majority', {
    useNewUrlParser: true,
    useUnifiedTopology: true
});

async function clearUserCache() {
    try {
        const uid = 'DgCSTkbsJwfSO0NTRuq6XVXLB3u2'; // User ID from Android logs
        
        const result = await User.updateOne(
            { uid },
            {
                $unset: {
                    cachedHomeFeed: 1,
                    homeFeedLastGeneratedAt: 1
                }
            }
        );
        
        console.log(`✅ Cleared cache for user ${uid}:`, result);
        
        // Verify cache was cleared
        const user = await User.findOne({ uid }).lean();
        console.log('User cache status:', {
            hasCachedFeed: !!user.cachedHomeFeed,
            hasTimestamp: !!user.homeFeedLastGeneratedAt
        });
        
        process.exit(0);
    } catch (error) {
        console.error('❌ Error clearing cache:', error);
        process.exit(1);
    }
}

clearUserCache(); 