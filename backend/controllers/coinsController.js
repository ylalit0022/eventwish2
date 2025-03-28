const Coins = require('../models/Coins');
const { AdMob } = require('../models/AdMob');
const logger = require('../config/logger');
const { coinsLogger } = require('../logs/logger-setup');
const crypto = require('crypto');

// Set of known fraudulent device IDs
const blacklistedDeviceIds = new Set();

// Map to keep track of suspicious devices
const suspiciousDevices = new Map();

/**
 * Validate device ID format and check blacklist
 * @param {string} deviceId - Device ID to validate
 * @returns {object} Validation result
 */
function validateDeviceId(deviceId) {
    // Basic validation
    if (!deviceId || typeof deviceId !== 'string') {
        return { valid: false, reason: 'missing_device_id' };
    }
    
    // Length validation
    if (deviceId.length < 10 || deviceId.length > 100) {
        return { valid: false, reason: 'invalid_length' };
    }
    
    // Format validation (this would depend on your expected format)
    // Example: Android IDs are 16 characters hexadecimal
    const validFormatRegex = /^[a-zA-Z0-9_\-\.]+$/;
    if (!validFormatRegex.test(deviceId)) {
        return { valid: false, reason: 'invalid_format' };
    }
    
    // Check blacklist
    if (blacklistedDeviceIds.has(deviceId)) {
        return { valid: false, reason: 'blacklisted' };
    }
    
    return { valid: true };
}

/**
 * Track suspicious activity for a device
 * @param {string} deviceId - Device ID
 * @param {string} activityType - Type of suspicious activity
 * @param {object} details - Additional details
 */
function trackSuspiciousActivity(deviceId, activityType, details = {}) {
    if (!suspiciousDevices.has(deviceId)) {
        suspiciousDevices.set(deviceId, {
            firstDetection: Date.now(),
            activities: [],
            score: 0
        });
    }
    
    const record = suspiciousDevices.get(deviceId);
    record.activities.push({
        timestamp: Date.now(),
        type: activityType,
        details
    });
    
    // Increment suspicion score based on activity type
    switch (activityType) {
        case 'time_manipulation':
            record.score += 10;
            break;
        case 'quick_rewards':
            record.score += 5;
            break;
        case 'invalid_signature':
            record.score += 15;
            break;
        case 'suspicious_device_info':
            record.score += 8;
            break;
        case 'security_violation':
            record.score += 30;
            break;
        default:
            record.score += 1;
    }
    
    // If score exceeds threshold, blacklist the device
    if (record.score >= 50) {
        blacklistedDeviceIds.add(deviceId);
        coinsLogger.fraud(deviceId, 'blacklisted', `Device blacklisted due to high suspicion score: ${record.score}`, {
            activities: record.activities,
            firstDetection: new Date(record.firstDetection).toISOString()
        });
    }
    
    return record.score;
}

/**
 * Get current server time
 * @param {*} req - Request object
 * @param {*} res - Response object
 */
exports.getServerTime = async (req, res) => {
    try {
        const timestamp = Date.now();
        const date = new Date(timestamp).toISOString();
        const clientTime = req.headers['x-request-time'] ? parseInt(req.headers['x-request-time']) : null;
        
        // Log time sync if client time is provided
        if (clientTime) {
            const offset = timestamp - clientTime;
            const deviceId = req.headers['x-device-id'] || 'unknown';
            
            coinsLogger.timeSync(deviceId, clientTime, timestamp, offset, {
                clientTimeISO: new Date(clientTime).toISOString(),
                serverTimeISO: date,
                userAgent: req.headers['user-agent']
            });
            
            // Check for suspicious time difference
            if (Math.abs(offset) > 300000) { // 5 minutes
                coinsLogger.timeSuspicious(deviceId, `Large time offset detected: ${offset}ms`, {
                    offset,
                    clientTime,
                    serverTime: timestamp
                });
            }
        }
        
        res.json({
            success: true,
            timestamp,
            date,
            serverTime: date,
            timeZone: Intl.DateTimeFormat().resolvedOptions().timeZone,
            clientTime: clientTime,
            offset: clientTime ? timestamp - clientTime : null
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
        
        // Validate device ID
        const validation = validateDeviceId(deviceId);
        if (!validation.valid) {
            coinsLogger.fraud(deviceId || 'unknown', 'invalid_device_id', `Invalid device ID: ${validation.reason}`, {
                deviceId,
                userAgent: req.headers['user-agent']
            });
            
            return res.status(400).json({
                success: false,
                message: 'Invalid device ID',
                reason: validation.reason
            });
        }
        
        // Find or create coins record
        let coins = await Coins.findOne({ deviceId });
        let isNewRecord = false;
        
        if (!coins) {
            coins = new Coins({
                deviceId,
                coins: 0,
                isUnlocked: false
            });
            await coins.save();
            isNewRecord = true;
            
            logger.info(`Created new coins record for device: ${deviceId}`);
        }
        
        // Check if the device has been flagged for security violations
        if (coins.securityViolations && coins.securityViolations.length > 0) {
            logger.warn(`Device with security violations: ${deviceId}, violations: ${coins.securityViolations.length}`);
            
            // Add to suspicious devices tracker
            trackSuspiciousActivity(deviceId, 'security_violation', {
                violationsCount: coins.securityViolations.length,
                latestViolation: coins.securityViolations[coins.securityViolations.length - 1]
            });
        }
        
        // Check if unlock has expired
        if (coins.isUnlocked && !coins.checkUnlockStatus()) {
            coins.isUnlocked = false;
            coins.unlockTimestamp = null;
            coins.unlockSignature = null;
            await coins.save();
            
            coinsLogger.validate(deviceId, false, "Unlock expired", {
                expiredAt: coins.unlockExpiry
            });
        }
        
        // Get plan configuration
        const planConfig = getPlanConfiguration();
        
        // Log access with client timestamp if provided
        const clientTime = req.headers['x-request-time'] ? parseInt(req.headers['x-request-time']) : null;
        if (clientTime) {
            const serverTime = Date.now();
            const offset = serverTime - clientTime;
            
            // Update time offset in database
            if (Math.abs(offset) > 1000) { // Only update if difference is significant (>1s)
                coins.timeOffset = offset;
                coins.lastSyncTimestamp = new Date(serverTime);
                await coins.save();
                
                coinsLogger.timeSync(deviceId, clientTime, serverTime, offset);
                
                // Check for suspicious time difference
                if (Math.abs(offset) > 300000) { // 5 minutes
                    trackSuspiciousActivity(deviceId, 'time_manipulation', {
                        offset,
                        clientTime,
                        serverTime
                    });
                    
                    coinsLogger.timeSuspicious(deviceId, `Large time offset detected: ${offset}ms`, {
                        offset,
                        clientTime,
                        serverTime
                    });
                }
            }
        }
        
        res.json({
            success: true,
            coins: coins.coins,
            isUnlocked: coins.isUnlocked,
            remainingTime: coins.remainingTime,
            unlockExpiry: coins.unlockExpiry,
            lastReward: coins.lastRewardTimestamp,
            plan: planConfig,
            isNewRecord
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
            logger.warn(`Ad unit not found: ${adUnitId} for device: ${deviceId}`);
            return res.status(404).json({
                success: false,
                message: 'Ad unit not found'
            });
        }
        
        if (adUnit.adType !== 'Rewarded') {
            coinsLogger.fraud(deviceId, 'add_coins', `Attempted to get coins with non-rewarded ad type: ${adUnit.adType}`, {
                adUnitId,
                adType: adUnit.adType
            });
            
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
                coinsLogger.fraud(deviceId, 'quick_reward', `Claimed reward too quickly: ${timeSinceLastReward}ms since last reward`, {
                    timeSinceLastReward,
                    lastRewardTime: coins.lastRewardTimestamp
                });
                
                return res.status(429).json({
                    success: false,
                    message: 'Please wait before claiming another reward',
                    waitTime: Math.ceil((30000 - timeSinceLastReward) / 1000)
                });
            }
        }
        
        // Add coins
        await coins.addCoins(amount, adUnitId, adName || adUnit.adName, deviceInfo || {});
        
        // Log reward
        coinsLogger.reward(deviceId, amount, adUnitId, `Current total: ${coins.coins} coins`, {
            adName: adName || adUnit.adName,
            deviceInfo: deviceInfo || {}
        });
        
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
            coinsLogger.fraud(deviceId, 'unlock_insufficient', `Attempted to unlock with insufficient coins: ${coins.coins}/${planConfig.requiredCoins}`, {
                currentCoins: coins.coins,
                requiredCoins: planConfig.requiredCoins
            });
            
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
        
        // Log unlock
        coinsLogger.unlock(deviceId, unlockDuration, `Used ${planConfig.requiredCoins} coins`, {
            usedCoins: planConfig.requiredCoins,
            remainingCoins: coins.coins,
            unlockTimestamp: new Date(serverTimestamp).toISOString(),
            unlockExpiry: coins.unlockExpiry,
            signature: signature.substring(0, 8) + '...' // Log truncated signature for security
        });
        
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
        const clientTime = req.headers['x-request-time'] ? parseInt(req.headers['x-request-time']) : Date.now();
        
        if (!deviceId || !timestamp || !duration || !signature) {
            return res.status(400).json({
                success: false,
                message: 'Missing required parameters'
            });
        }
        
        // Validate the signature
        const isValid = Coins.validateSignature(deviceId, timestamp, duration, signature);
        
        if (!isValid) {
            coinsLogger.validate(deviceId, false, "Invalid signature provided", {
                providedSignature: signature.substring(0, 8) + '...',
                timestamp,
                duration
            });
            
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
        
        // Calculate time difference to detect manipulation
        const serverTime = Date.now();
        const clientServerDiff = Math.abs(serverTime - clientTime); 
        
        // Check for potential time manipulation
        if (clientServerDiff > 300000) { // 5 minutes
            coinsLogger.timeSuspicious(deviceId, `Large client-server time difference detected: ${clientServerDiff}ms during unlock validation`, {
                clientTime,
                serverTime,
                timeDiff: clientServerDiff,
                unlockTimestamp: timestamp,
                expiryDate
            });
        }
        
        // Log validation outcome
        coinsLogger.validate(deviceId, !isExpired, isExpired ? "Unlock expired" : "Unlock valid", {
            unlockTimestamp: unlockTimestamp.toISOString(),
            expiryDate: expiryDate.toISOString(),
            remainingTime: isExpired ? 0 : expiryDate.getTime() - now.getTime(),
            clientServerDiff
        });
        
        res.json({
            success: true,
            valid: !isExpired,
            expired: isExpired,
            timestamp: serverTime,
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
        const { deviceId, timestamp, duration, deviceInfo, securityViolation } = req.body;
        
        // Validate device ID
        const validation = validateDeviceId(deviceId);
        if (!validation.valid) {
            coinsLogger.fraud(deviceId || 'unknown', 'invalid_device_id', `Invalid device ID in report: ${validation.reason}`, {
                deviceId,
                userAgent: req.headers['user-agent']
            });
            
            return res.status(400).json({
                success: false,
                message: 'Invalid device ID',
                reason: validation.reason
            });
        }
        
        // Find coins record
        let coins = await Coins.findOne({ deviceId });
        
        if (!coins) {
            // Create new coins record
            coins = new Coins({
                deviceId,
                isUnlocked: securityViolation ? false : true,
                unlockTimestamp: securityViolation ? null : new Date(parseInt(timestamp)),
                unlockDuration: securityViolation ? 0 : parseInt(duration)
            });
        } else if (securityViolation) {
            // Handle security violation report
            if (!coins.securityViolations) {
                coins.securityViolations = [];
            }
            
            coins.securityViolations.push({
                timestamp: Date.now(),
                deviceInfo: deviceInfo || {},
                isRooted: deviceInfo?.isRooted || false,
                isEmulator: deviceInfo?.isEmulator || false
            });
            
            // Revoke access if security violation is reported
            coins.isUnlocked = false;
            coins.unlockTimestamp = null;
            coins.unlockDuration = 0;
            
            // Track the security violation
            const score = trackSuspiciousActivity(deviceId, 'security_violation', deviceInfo || {});
            
            // Log the security violation
            coinsLogger.security(deviceId, 'device_security_violation', `Security violation reported from client`, {
                deviceInfo: deviceInfo || {},
                suspicionScore: score
            });
            
            await coins.save();
            
            return res.json({
                success: true,
                message: 'Security violation reported',
                timestamp: Date.now()
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
        
        // Log report
        coinsLogger.unlock(deviceId, duration, "Unlock reported from client", {
            unlockTimestamp: new Date(parseInt(timestamp)).toISOString(),
            unlockExpiry: coins.unlockExpiry,
            signature: signature.substring(0, 8) + '...',
            deviceInfo: deviceInfo || {}
        });
        
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