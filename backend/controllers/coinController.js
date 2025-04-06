const CropAppUser = require('../models/CropAppUser');
const logger = require('../config/logger');

// Constants
const FREE_IMAGE_LIMIT = 3;
const COINS_PER_AD = 10;
const PREMIUM_COST_PER_DAY = 15;
const PREMIUM_COST_PER_WEEK = 80;
const PREMIUM_COST_PER_MONTH = 250;
const AD_COOLDOWN_MINUTES = 3;

// Helper function to get or create a user by deviceId
const getOrCreateUser = async (deviceId) => {
    try {
        let user = await CropAppUser.findOne({ deviceId });
        
        if (!user) {
            logger.info(`Creating new user with deviceId: ${deviceId}`);
            user = new CropAppUser({ deviceId });
            await user.save();
        }
        
        // Check and update premium status
        user.checkPremiumStatus();
        
        // Check and reset daily image count if it's a new day
        user.checkAndResetDailyCount();
        
        await user.save();
        return user;
    } catch (error) {
        logger.error(`Error in getOrCreateUser: ${error.message}`, { deviceId, error });
        throw error;
    }
};

// Register or verify a user
exports.registerUser = async (req, res) => {
    try {
        const { deviceId, appVersion } = req.body;
        
        if (!deviceId) {
            return res.status(400).json({ 
                success: false,
                message: 'Device ID is required'
            });
        }
        
        const user = await getOrCreateUser(deviceId);
        
        // Update app version if provided
        if (appVersion) {
            user.appVersion = appVersion;
            await user.save();
        }
        
        return res.status(200).json({
            success: true,
            message: 'User verified successfully',
            data: {
                deviceId: user.deviceId,
                coinBalance: user.coinBalance,
                isPremium: user.isPremium,
                premiumExpiryDate: user.premiumExpiryDate,
                dailyImageCount: user.dailyImageCount,
                remainingFreeImages: Math.max(0, FREE_IMAGE_LIMIT - user.dailyImageCount),
                lastAdWatched: user.lastAdWatched
            }
        });
    } catch (error) {
        logger.error(`Error in registerUser: ${error.message}`, { error });
        return res.status(500).json({ 
            success: false,
            message: 'Server error',
            error: error.message
        });
    }
};

// Get user's coin balance and premium status
exports.getUserStatus = async (req, res) => {
    try {
        const { deviceId } = req.params;
        
        if (!deviceId) {
            return res.status(400).json({ 
                success: false,
                message: 'Device ID is required'
            });
        }
        
        const user = await getOrCreateUser(deviceId);
        
        // Calculate cooldown status for ads
        let canWatchAd = true;
        let cooldownRemaining = 0;
        
        if (user.lastAdWatched) {
            const now = new Date();
            const lastAdTime = new Date(user.lastAdWatched);
            const diffMs = now - lastAdTime;
            const cooldownMs = AD_COOLDOWN_MINUTES * 60 * 1000;
            
            if (diffMs < cooldownMs) {
                canWatchAd = false;
                cooldownRemaining = Math.ceil((cooldownMs - diffMs) / 1000); // seconds remaining
            }
        }
        
        return res.status(200).json({
            success: true,
            data: {
                deviceId: user.deviceId,
                coinBalance: user.coinBalance,
                isPremium: user.isPremium,
                premiumExpiryDate: user.premiumExpiryDate,
                dailyImageCount: user.dailyImageCount,
                remainingFreeImages: Math.max(0, FREE_IMAGE_LIMIT - user.dailyImageCount),
                canCaptureMore: user.isPremium || (user.dailyImageCount < FREE_IMAGE_LIMIT),
                canWatchAd,
                cooldownRemaining,
                premiumOptions: {
                    day: {
                        coins: PREMIUM_COST_PER_DAY,
                        days: 1
                    },
                    week: {
                        coins: PREMIUM_COST_PER_WEEK,
                        days: 7
                    },
                    month: {
                        coins: PREMIUM_COST_PER_MONTH,
                        days: 30
                    }
                }
            }
        });
    } catch (error) {
        logger.error(`Error in getUserStatus: ${error.message}`, { error });
        return res.status(500).json({ 
            success: false,
            message: 'Server error',
            error: error.message
        });
    }
};

// Add coins after watching an ad
exports.addCoinsFromAd = async (req, res) => {
    try {
        const { deviceId } = req.body;
        
        if (!deviceId) {
            return res.status(400).json({ 
                success: false,
                message: 'Device ID is required'
            });
        }
        
        const user = await getOrCreateUser(deviceId);
        
        // Check if cooldown period has passed
        if (user.lastAdWatched) {
            const now = new Date();
            const lastAdTime = new Date(user.lastAdWatched);
            const diffMs = now - lastAdTime;
            const cooldownMs = AD_COOLDOWN_MINUTES * 60 * 1000;
            
            if (diffMs < cooldownMs) {
                const remainingSeconds = Math.ceil((cooldownMs - diffMs) / 1000);
                return res.status(429).json({
                    success: false,
                    message: `Please wait before watching another ad`,
                    cooldownRemaining: remainingSeconds
                });
            }
        }
        
        // Add coins and record ad watched
        user.addCoins(COINS_PER_AD, 'AD_REWARD', { timestamp: new Date() });
        user.recordAdWatched();
        await user.save();
        
        return res.status(200).json({
            success: true,
            message: `Added ${COINS_PER_AD} coins to your balance`,
            data: {
                coinBalance: user.coinBalance,
                coinsAdded: COINS_PER_AD,
                lastAdWatched: user.lastAdWatched
            }
        });
    } catch (error) {
        logger.error(`Error in addCoinsFromAd: ${error.message}`, { error });
        return res.status(500).json({ 
            success: false,
            message: 'Server error',
            error: error.message
        });
    }
};

// Spend coins to purchase premium
exports.purchasePremium = async (req, res) => {
    try {
        const { deviceId, premiumOption } = req.body;
        
        if (!deviceId) {
            return res.status(400).json({ 
                success: false,
                message: 'Device ID is required'
            });
        }
        
        if (!premiumOption || !['day', 'week', 'month'].includes(premiumOption)) {
            return res.status(400).json({
                success: false,
                message: 'Valid premium option is required: day, week, or month'
            });
        }
        
        const user = await getOrCreateUser(deviceId);
        
        // Determine cost and duration based on selected option
        let cost = 0;
        let days = 0;
        
        switch (premiumOption) {
            case 'day':
                cost = PREMIUM_COST_PER_DAY;
                days = 1;
                break;
            case 'week':
                cost = PREMIUM_COST_PER_WEEK;
                days = 7;
                break;
            case 'month':
                cost = PREMIUM_COST_PER_MONTH;
                days = 30;
                break;
        }
        
        // Check if user has enough coins
        if (user.coinBalance < cost) {
            return res.status(400).json({
                success: false,
                message: 'Insufficient coins',
                data: {
                    coinBalance: user.coinBalance,
                    requiredCoins: cost,
                    shortfall: cost - user.coinBalance
                }
            });
        }
        
        // Spend coins and activate premium
        user.spendCoins(cost, 'PREMIUM_PURCHASE', days, { option: premiumOption });
        const expiryDate = user.activatePremium(days);
        await user.save();
        
        return res.status(200).json({
            success: true,
            message: `Premium activated for ${days} day(s)`,
            data: {
                coinBalance: user.coinBalance,
                coinsSpent: cost,
                isPremium: user.isPremium,
                premiumExpiryDate: expiryDate,
                premiumDurationDays: days
            }
        });
    } catch (error) {
        logger.error(`Error in purchasePremium: ${error.message}`, { error });
        return res.status(500).json({ 
            success: false,
            message: 'Server error',
            error: error.message
        });
    }
};

// Increment image capture count
exports.incrementImageCount = async (req, res) => {
    try {
        const { deviceId } = req.body;
        
        if (!deviceId) {
            return res.status(400).json({ 
                success: false,
                message: 'Device ID is required'
            });
        }
        
        const user = await getOrCreateUser(deviceId);
        
        // Check if user can capture more images
        const canCaptureMore = user.isPremium || (user.dailyImageCount < FREE_IMAGE_LIMIT);
        
        if (!canCaptureMore) {
            return res.status(403).json({
                success: false,
                message: 'Daily free image limit reached',
                data: {
                    isPremium: user.isPremium,
                    dailyImageCount: user.dailyImageCount,
                    freeLimit: FREE_IMAGE_LIMIT
                }
            });
        }
        
        // Increment count and save
        const newCount = user.incrementImageCount();
        await user.save();
        
        return res.status(200).json({
            success: true,
            message: 'Image count incremented',
            data: {
                dailyImageCount: newCount,
                remainingFreeImages: user.isPremium ? "unlimited" : Math.max(0, FREE_IMAGE_LIMIT - newCount),
                isPremium: user.isPremium
            }
        });
    } catch (error) {
        logger.error(`Error in incrementImageCount: ${error.message}`, { error });
        return res.status(500).json({ 
            success: false,
            message: 'Server error',
            error: error.message
        });
    }
};

// Verify premium status
exports.verifyPremiumStatus = async (req, res) => {
    try {
        const { deviceId } = req.params;
        
        if (!deviceId) {
            return res.status(400).json({ 
                success: false,
                message: 'Device ID is required'
            });
        }
        
        const user = await getOrCreateUser(deviceId);
        
        // Check premium status
        const isPremium = user.checkPremiumStatus();
        await user.save(); // Save in case premium status changed
        
        // Calculate remaining time
        let remainingTime = 0;
        if (isPremium && user.premiumExpiryDate) {
            remainingTime = Math.max(0, user.premiumExpiryDate - new Date());
        }
        
        return res.status(200).json({
            success: true,
            data: {
                isPremium,
                premiumExpiryDate: user.premiumExpiryDate,
                remainingTimeMs: remainingTime
            }
        });
    } catch (error) {
        logger.error(`Error in verifyPremiumStatus: ${error.message}`, { error });
        return res.status(500).json({ 
            success: false,
            message: 'Server error',
            error: error.message
        });
    }
};

// Get transaction history
exports.getTransactionHistory = async (req, res) => {
    try {
        const { deviceId } = req.params;
        const { limit = 20, offset = 0 } = req.query;
        
        if (!deviceId) {
            return res.status(400).json({ 
                success: false,
                message: 'Device ID is required'
            });
        }
        
        const user = await CropAppUser.findOne({ deviceId });
        
        if (!user) {
            return res.status(404).json({
                success: false,
                message: 'User not found'
            });
        }
        
        // Get transactions with pagination
        const transactions = user.transactions
            .sort((a, b) => b.timestamp - a.timestamp) // Sort by newest first
            .slice(parseInt(offset), parseInt(offset) + parseInt(limit)); // Apply pagination
        
        return res.status(200).json({
            success: true,
            data: {
                transactions,
                total: user.transactions.length,
                offset: parseInt(offset),
                limit: parseInt(limit)
            }
        });
    } catch (error) {
        logger.error(`Error in getTransactionHistory: ${error.message}`, { error });
        return res.status(500).json({ 
            success: false,
            message: 'Server error',
            error: error.message
        });
    }
}; 