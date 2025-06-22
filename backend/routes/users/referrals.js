const express = require('express');
const router = express.Router();
const User = require('../../models/User');
const logger = require('../../utils/logger');
const { validateFirebaseUid } = require('../../middleware/validators');
const { verifyFirebaseToken } = require('../../middleware/auth');

/**
 * @route   POST /api/users/:uid/referral
 * @desc    Generate or update referral code for a user
 * @access  Private
 */
router.post('/:uid/referral', validateFirebaseUid, verifyFirebaseToken, async (req, res) => {
    try {
        const { uid } = req.params;
        
        // Find user by uid
        let user = await User.findOne({ uid });
        
        if (!user) {
            logger.warn(`Referral code generation attempted for non-existent user: UID ${uid}`);
            return res.status(404).json({
                success: false,
                message: 'User not found'
            });
        }
        
        // Generate a unique referral code if one doesn't exist
        if (!user.referralCode) {
            // Create a referral code based on UID and timestamp
            const timestamp = new Date().getTime().toString().slice(-6);
            const uidSuffix = uid.slice(-4);
            const referralCode = `EW-${timestamp}${uidSuffix}`.toUpperCase();
            
            user.referralCode = referralCode;
            await user.save();
            
            logger.info(`Generated referral code ${referralCode} for user ${uid}`);
        }
        
        res.status(200).json({
            success: true,
            referralCode: user.referralCode
        });
    } catch (error) {
        logger.error(`Referral code generation error: ${error.message}`);
        res.status(500).json({
            success: false,
            message: 'Server error generating referral code',
            error: error.message
        });
    }
});

/**
 * @route   POST /api/users/:uid/apply-referral
 * @desc    Apply a referral code to a user
 * @access  Private
 */
router.post('/:uid/apply-referral', validateFirebaseUid, verifyFirebaseToken, async (req, res) => {
    try {
        const { uid } = req.params;
        const { referralCode } = req.body;
        
        if (!referralCode) {
            return res.status(400).json({
                success: false,
                message: 'Referral code is required'
            });
        }
        
        // Find user by uid
        let user = await User.findOne({ uid });
        
        if (!user) {
            logger.warn(`Referral application attempted for non-existent user: UID ${uid}`);
            return res.status(404).json({
                success: false,
                message: 'User not found'
            });
        }
        
        // Check if user already has a referral
        if (user.referredBy && user.referredBy.referredBy) {
            return res.status(400).json({
                success: false,
                message: 'User already has a referral applied'
            });
        }
        
        // Find the referring user by referral code
        const referrer = await User.findOne({ referralCode });
        
        if (!referrer) {
            return res.status(404).json({
                success: false,
                message: 'Invalid referral code'
            });
        }
        
        // Prevent self-referral
        if (referrer.uid === uid) {
            return res.status(400).json({
                success: false,
                message: 'Cannot use your own referral code'
            });
        }
        
        // Apply the referral
        user.referredBy = {
            referredBy: referrer.uid,
            referralCode: referralCode
        };
        
        await user.save();
        logger.info(`User ${uid} applied referral code from user ${referrer.uid}`);
        
        res.status(200).json({
            success: true,
            message: 'Referral applied successfully'
        });
    } catch (error) {
        logger.error(`Referral application error: ${error.message}`);
        res.status(500).json({
            success: false,
            message: 'Server error applying referral',
            error: error.message
        });
    }
});

module.exports = router; 