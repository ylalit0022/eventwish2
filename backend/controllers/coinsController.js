const Coins = require('../models/Coins');
const { AdMob } = require('../models/AdMob');
const logger = require('../config/logger');
const crypto = require('crypto');

/**
 * Get current server time
 * @param {*} req - Request object
 * @param {*} res - Response object
 */
exports.getServerTime = async (req, res) => {
    try {
        const timestamp = Date.now();
        const date = new Date(timestamp).toISOString();
        
        res.json({
            success: true,
            timestamp,
            date,
            serverTime: date,
            timeZone: Intl.DateTimeFormat().resolvedOptions().timeZone
        });
    } catch (error) {
        logger.error('Error in getServerTime:', error);
        res.status(500).json({
            success: false,
            message: 'Error getting server time'
        });
    }
};

/**
 * Get coins for a device
 * @param {*} req - Request object
 * @param {*} res - Response object
 */
exports.getCoins = async (req, res) => {
    try {
        const { deviceId } = req.params;
        
        if (!deviceId) {
            return res.status(400).json({
                success: false,
                message: 'Device ID is required'
            });
        }
        
        // Find or create coins record
        let coins = await Coins.findOne({ deviceId });
        
        if (!coins) {
            coins = new Coins({
                deviceId,
                coins: 0,
                isUnlocked: false
            });
            await coins.save();
        }
        
        // Check if unlock has expired
        if (coins.isUnlocked && !coins.checkUnlockStatus()) {
            coins.isUnlocked = false;
            coins.unlockTimestamp = null;
            coins.unlockSignature = null;
            await coins.save();
        }
        
        // Get plan configuration
        const planConfig = getPlanConfiguration();
        
        res.json({
            success: true,
            coins: coins.coins,
            isUnlocked: coins.isUnlocked,
            remainingTime: coins.remainingTime,
            unlockExpiry: coins.unlockExpiry,
            lastReward: coins.lastRewardTimestamp,
            plan: planConfig
        });
    } catch (error) {
        logger.error('Error in getCoins:', error);
        res.status(500).json({
            success: false,
            message: 'Error getting coins'
        });
    }
};

/**
 * Add coins for a device
 * @param {*} req - Request object
 * @param {*} res - Response object
 */
exports.addCoins = async (req, res) => {
    try {
        const { deviceId } = req.params;
        const { amount, adUnitId, adName, deviceInfo } = req.body;
        
        if (!deviceId || !amount || !adUnitId) {
            return res.status(400).json({
                success: false,
                message: 'Device ID, amount, and adUnitId are required'
            });
        }
        
        // Validate the ad unit exists and is a rewarded ad
        const adUnit = await AdMob.findOne({ adUnitCode: adUnitId });
        
        if (!adUnit) {
            return res.status(404).json({
                success: false,
                message: 'Ad unit not found'
            });
        }
        
        if (adUnit.adType !== 'Rewarded') {
            return res.status(400).json({
                success: false,
                message: 'Only rewarded ads can give coins'
            });
        }
        
        // Find or create coins record
        let coins = await Coins.findOne({ deviceId });
        
        if (!coins) {
            coins = new Coins({
                deviceId,
                coins: 0,
                isUnlocked: false
            });
        }
        
        // Check for fraudulent rewarded ad claims (e.g., too frequent)
        const now = new Date();
        if (coins.lastRewardTimestamp) {
            const timeSinceLastReward = now.getTime() - coins.lastRewardTimestamp.getTime();
            
            // Minimum 30 seconds between rewards to prevent fraud
            if (timeSinceLastReward < 30000) {
                logger.warn(`Suspicious reward activity: ${deviceId} claimed reward too quickly (${timeSinceLastReward}ms)`);
                return res.status(429).json({
                    success: false,
                    message: 'Please wait before claiming another reward',
                    waitTime: Math.ceil((30000 - timeSinceLastReward) / 1000)
                });
            }
        }
        
        // Add coins
        await coins.addCoins(amount, adUnitId, adName || adUnit.adName, deviceInfo || {});
        
        // Update ad unit analytics
        adUnit.impressions += 1;
        if (adUnit.impressionData.length >= 100) {
            adUnit.impressionData.shift(); // Remove oldest entry if we have too many
        }
        adUnit.impressionData.push({
            timestamp: now,
            context: {
                deviceId,
                reward: true,
                amount
            }
        });
        await adUnit.save();
        
        // Get plan configuration
        const planConfig = getPlanConfiguration();
        
        res.json({
            success: true,
            coins: coins.coins,
            added: amount,
            isUnlocked: coins.isUnlocked,
            remainingTime: coins.remainingTime,
            unlockExpiry: coins.unlockExpiry,
            plan: planConfig
        });
    } catch (error) {
        logger.error('Error in addCoins:', error);
        res.status(500).json({
            success: false,
            message: 'Error adding coins'
        });
    }
};

/**
 * Unlock feature using coins
 * @param {*} req - Request object
 * @param {*} res - Response object
 */
exports.unlockFeature = async (req, res) => {
    try {
        const { deviceId } = req.params;
        const { duration } = req.body;
        
        if (!deviceId) {
            return res.status(400).json({
                success: false,
                message: 'Device ID is required'
            });
        }
        
        // Find coins record
        let coins = await Coins.findOne({ deviceId });
        
        if (!coins) {
            return res.status(404).json({
                success: false,
                message: 'Coins record not found for this device'
            });
        }
        
        // Get plan configuration
        const planConfig = getPlanConfiguration();
        
        // Check if user has enough coins
        if (coins.coins < planConfig.requiredCoins) {
            return res.status(400).json({
                success: false,
                message: 'Not enough coins to unlock feature',
                required: planConfig.requiredCoins,
                current: coins.coins
            });
        }
        
        // Deduct coins
        coins.coins -= planConfig.requiredCoins;
        
        // Set unlock duration (from request or plan default)
        const unlockDuration = duration || planConfig.defaultUnlockDuration;
        
        // Get current server time for accurate unlock timestamp
        const serverTimestamp = Date.now();
        
        // Unlock feature
        await coins.unlockFeature(unlockDuration, serverTimestamp);
        
        // Generate a secure signature for the unlock
        const signature = generateUnlockSignature(deviceId, serverTimestamp, unlockDuration);
        
        res.json({
            success: true,
            isUnlocked: true,
            unlockDuration,
            remainingTime: coins.remainingTime,
            unlockExpiry: coins.unlockExpiry,
            timestamp: serverTimestamp,
            signature,
            coins: coins.coins
        });
    } catch (error) {
        logger.error('Error in unlockFeature:', error);
        res.status(500).json({
            success: false,
            message: 'Error unlocking feature'
        });
    }
};

/**
 * Validate an unlock signature to detect time manipulation
 * @param {*} req - Request object
 * @param {*} res - Response object
 */
exports.validateUnlock = async (req, res) => {
    try {
        const { deviceId, timestamp, duration, signature } = req.body;
        
        if (!deviceId || !timestamp || !duration || !signature) {
            return res.status(400).json({
                success: false,
                message: 'Missing required parameters'
            });
        }
        
        // Validate the signature
        const isValid = Coins.validateSignature(deviceId, timestamp, duration, signature);
        
        if (!isValid) {
            return res.status(401).json({
                success: false,
                valid: false,
                message: 'Invalid signature',
                timestamp: Date.now()
            });
        }
        
        // Check if the unlock period has expired
        const unlockTimestamp = new Date(parseInt(timestamp));
        const expiryDate = new Date(unlockTimestamp);
        expiryDate.setDate(expiryDate.getDate() + parseInt(duration));
        
        const now = new Date();
        const isExpired = now > expiryDate;
        
        res.json({
            success: true,
            valid: !isExpired,
            expired: isExpired,
            timestamp: Date.now(),
            unlockExpiry: expiryDate,
            remainingTime: Math.max(0, expiryDate.getTime() - now.getTime())
        });
    } catch (error) {
        logger.error('Error in validateUnlock:', error);
        res.status(500).json({
            success: false,
            message: 'Error validating unlock',
            timestamp: Date.now()
        });
    }
};

/**
 * Report an unlock from the client to the server
 * @param {*} req - Request object
 * @param {*} res - Response object
 */
exports.reportUnlock = async (req, res) => {
    try {
        const { deviceId, timestamp, duration } = req.body;
        
        if (!deviceId || !timestamp || !duration) {
            return res.status(400).json({
                success: false,
                message: 'Missing required parameters'
            });
        }
        
        // Find coins record
        let coins = await Coins.findOne({ deviceId });
        
        if (!coins) {
            // Create new coins record
            coins = new Coins({
                deviceId,
                isUnlocked: true,
                unlockTimestamp: new Date(parseInt(timestamp)),
                unlockDuration: parseInt(duration)
            });
        } else {
            // Update existing record
            coins.isUnlocked = true;
            coins.unlockTimestamp = new Date(parseInt(timestamp));
            coins.unlockDuration = parseInt(duration);
        }
        
        // Generate signature
        coins.unlockSignature = coins._generateSignature();
        await coins.save();
        
        // Create a signature to return to the client
        const signature = generateUnlockSignature(deviceId, timestamp, duration);
        
        res.json({
            success: true,
            message: 'Unlock reported successfully',
            signature,
            timestamp: Date.now(),
            serverTime: new Date().toISOString()
        });
    } catch (error) {
        logger.error('Error in reportUnlock:', error);
        res.status(500).json({
            success: false,
            message: 'Error reporting unlock'
        });
    }
};

/**
 * Get the plan configuration
 * @param {*} req - Request object
 * @param {*} res - Response object
 */
exports.getPlanConfiguration = async (req, res) => {
    try {
        // Get the plan configuration
        const planConfig = getPlanConfiguration();
        
        res.json({
            success: true,
            plan: planConfig
        });
    } catch (error) {
        logger.error('Error in getPlanConfiguration:', error);
        res.status(500).json({
            success: false,
            message: 'Error getting plan configuration'
        });
    }
};

/**
 * Generate a secure signature for unlock validation
 * @param {string} deviceId - Device ID
 * @param {number} timestamp - Timestamp of unlock
 * @param {number} duration - Duration of unlock in days
 * @returns {string} - HMAC signature
 */
function generateUnlockSignature(deviceId, timestamp, duration) {
    const data = `${deviceId}:${timestamp}:${duration}`;
    const secret = process.env.JWT_SECRET || 'eventwish-coins-secret-key';
    
    return crypto.createHmac('sha256', secret).update(data).digest('hex');
}

/**
 * Get the plan configuration values
 * This would ideally come from a database or config file in a production system
 * @returns {Object} - Plan configuration
 */
function getPlanConfiguration() {
    return {
        requiredCoins: 100,           // Coins needed to unlock feature
        coinsPerReward: 10,           // Coins earned per rewarded ad
        defaultUnlockDuration: 30,    // Default unlock duration in days
        rewardCooldown: 30            // Minimum seconds between rewards
    };
}

/**
 * Check for time manipulation by comparing timestamps
 * @param {number} clientTime - Client timestamp
 * @param {number} storedTime - Previously stored timestamp
 * @param {number} maxDiff - Maximum allowed difference in seconds
 * @returns {boolean} - True if time manipulation is detected
 */
function detectTimeManipulation(clientTime, storedTime, maxDiff = 3600) {
    // If we don't have a stored time, we can't detect manipulation
    if (!storedTime) return false;
    
    const clientDate = new Date(clientTime);
    const storedDate = new Date(storedTime);
    
    // Calculate the difference in seconds
    const diffSeconds = Math.abs((clientDate.getTime() - storedDate.getTime()) / 1000);
    
    // Check if the difference is suspiciously large (greater than maxDiff seconds)
    return diffSeconds > maxDiff;
} 