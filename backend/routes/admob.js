const express = require('express');
const router = express.Router();
const { AdMob } = require('../models/AdMob');
const { Coins } = require('../models/Coins');
const { verifyApiKey } = require('../middleware/auth');
const { validateDeviceId } = require('../middleware/validation');
const { handleError } = require('../utils/errorHandler');

// Get available ad units for a device
router.get('/units', verifyApiKey, validateDeviceId, async (req, res) => {
    try {
        const deviceId = req.headers['x-device-id'];
        const adType = req.query.type; // Optional filter by ad type

        // Build query
        const query = { status: true };
        if (adType) {
            query.adType = adType;
        }

        // Get all active ad units
        const adUnits = await AdMob.find(query)
            .sort({ targetingPriority: -1 })
            .lean();

        // Check availability for each ad unit
        const availableUnits = await Promise.all(
            adUnits.map(async (unit) => {
                const status = await unit.canShowAd(deviceId);
                return {
                    ...unit,
                    canShow: status.canShow,
                    reason: status.reason,
                    nextAvailable: status.nextAvailable
                };
            })
        );

        // Filter out unavailable units
        const filteredUnits = availableUnits.filter(unit => unit.canShow);

        res.json({
            success: true,
            data: filteredUnits
        });
    } catch (error) {
        handleError(res, error);
    }
});

// Get ad status and cooldowns for a device
router.get('/status', verifyApiKey, validateDeviceId, async (req, res) => {
    try {
        const deviceId = req.headers['x-device-id'];
        const adType = req.query.type; // Optional filter by ad type

        // Build query
        const query = { status: true };
        if (adType) {
            query.adType = adType;
        }

        // Get all active ad units
        const adUnits = await AdMob.find(query)
            .sort({ targetingPriority: -1 })
            .lean();

        // Get user's coins
        const userCoins = await Coins.findOne({ deviceId });

        // Get status for each ad unit
        const statuses = await Promise.all(
            adUnits.map(async (unit) => {
                const status = await unit.canShowAd(deviceId);
                return {
                    adUnitId: unit._id,
                    adType: unit.adType,
                    canShow: status.canShow,
                    reason: status.reason,
                    nextAvailable: status.nextAvailable,
                    reward: userCoins ? {
                        canReward: !userCoins.lastRewardTimestamp || 
                                 new Date() > new Date(userCoins.lastRewardTimestamp.getTime() + 
                                 userCoins.plan.defaultUnlockDuration * 24 * 60 * 60 * 1000),
                        requiredCoins: userCoins.plan.requiredCoins,
                        coinsPerReward: userCoins.plan.coinsPerReward,
                        cooldownUntil: userCoins.lastRewardTimestamp ? 
                            new Date(userCoins.lastRewardTimestamp.getTime() + 
                            userCoins.plan.defaultUnlockDuration * 24 * 60 * 60 * 1000) : null
                    } : null
                };
            })
        );

        res.json({
            success: true,
            data: statuses
        });
    } catch (error) {
        handleError(res, error);
    }
});

// Handle reward redemption
router.post('/reward', verifyApiKey, validateDeviceId, async (req, res) => {
    try {
        const deviceId = req.headers['x-device-id'];
        const { adUnitId } = req.body;

        if (!adUnitId) {
            return res.status(400).json({
                success: false,
                error: 'Ad unit ID is required'
            });
        }

        // Get the ad unit
        const adUnit = await AdMob.findById(adUnitId);
        if (!adUnit) {
            return res.status(404).json({
                success: false,
                error: 'Ad unit not found'
            });
        }

        // Get user's coins
        const userCoins = await Coins.findOne({ deviceId });
        if (!userCoins) {
            return res.status(404).json({
                success: false,
                error: 'User coins record not found'
            });
        }

        // Check if reward is available
        const now = new Date();
        if (userCoins.lastRewardTimestamp) {
            const cooldownEnd = new Date(userCoins.lastRewardTimestamp.getTime() + 
                userCoins.plan.defaultUnlockDuration * 24 * 60 * 60 * 1000);
            
            if (now < cooldownEnd) {
                return res.status(400).json({
                    success: false,
                    error: 'Reward is not available',
                    nextAvailable: cooldownEnd
                });
            }
        }

        // Check if user has enough coins
        if (userCoins.coins < userCoins.plan.requiredCoins) {
            return res.status(400).json({
                success: false,
                error: 'Insufficient coins',
                required: userCoins.plan.requiredCoins,
                available: userCoins.coins
            });
        }

        // Deduct coins and add reward
        userCoins.coins -= userCoins.plan.requiredCoins;
        userCoins.coins += userCoins.plan.coinsPerReward;
        userCoins.lastRewardTimestamp = now;

        // Add to reward history
        userCoins.rewardHistory.push({
            timestamp: now,
            adUnitId: adUnitId,
            adName: adUnit.adName,
            coinsEarned: userCoins.plan.coinsPerReward,
            deviceInfo: {
                deviceId,
                timestamp: now
            }
        });

        await userCoins.save();

        // Start ad cooldown
        await adUnit.startCooldown(deviceId);

        res.json({
            success: true,
            data: {
                coinsDeducted: userCoins.plan.requiredCoins,
                coinsRewarded: userCoins.plan.coinsPerReward,
                newBalance: userCoins.coins,
                cooldownUntil: new Date(now.getTime() + 
                    userCoins.plan.defaultUnlockDuration * 24 * 60 * 60 * 1000)
            }
        });
    } catch (error) {
        handleError(res, error);
    }
});

// Record ad impression
router.post('/impression', verifyApiKey, validateDeviceId, async (req, res) => {
    try {
        const deviceId = req.headers['x-device-id'];
        const { adUnitId, context } = req.body;

        if (!adUnitId) {
            return res.status(400).json({
                success: false,
                error: 'Ad unit ID is required'
            });
        }

        // Get the ad unit
        const adUnit = await AdMob.findById(adUnitId);
        if (!adUnit) {
            return res.status(404).json({
                success: false,
                error: 'Ad unit not found'
            });
        }

        // Record the impression
        await adUnit.recordImpression(deviceId, context);

        res.json({
            success: true,
            message: 'Impression recorded successfully'
        });
    } catch (error) {
        handleError(res, error);
    }
});

// Record ad click
router.post('/click', verifyApiKey, validateDeviceId, async (req, res) => {
    try {
        const deviceId = req.headers['x-device-id'];
        const { adUnitId, context } = req.body;

        if (!adUnitId) {
            return res.status(400).json({
                success: false,
                error: 'Ad unit ID is required'
            });
        }

        // Get the ad unit
        const adUnit = await AdMob.findById(adUnitId);
        if (!adUnit) {
            return res.status(404).json({
                success: false,
                error: 'Ad unit not found'
            });
        }

        // Record the click
        await adUnit.recordClick(deviceId, context);

        res.json({
            success: true,
            message: 'Click recorded successfully'
        });
    } catch (error) {
        handleError(res, error);
    }
});

module.exports = router; 