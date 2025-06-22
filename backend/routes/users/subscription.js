const express = require('express');
const router = express.Router();
const User = require('../../models/User');
const logger = require('../../utils/logger');
const { verifyFirebaseToken } = require('../../middleware/auth');

/**
 * @route   PUT /api/users/:uid/subscription
 * @desc    Update user subscription details
 * @access  Private
 */
router.put('/:uid/subscription', verifyFirebaseToken, async (req, res) => {
    try {
        const { uid } = req.params;
        const subscriptionData = req.body;
        
        const user = await User.findOne({ uid });
        if (!user) {
            return res.status(404).json({
                success: false,
                message: 'User not found'
            });
        }
        
        // Update subscription fields
        if (!user.subscription) {
            user.subscription = {};
        }
        
        Object.assign(user.subscription, subscriptionData);
        await user.save();
        
        res.status(200).json({
            success: true,
            message: 'Subscription updated successfully',
            subscription: user.subscription
        });
    } catch (error) {
        logger.error(`Update subscription error: ${error.message}`);
        res.status(500).json({
            success: false,
            message: 'Server error',
            error: error.message
        });
    }
});

/**
 * @route   POST /api/users/:uid/subscription-history
 * @desc    Add subscription history entry
 * @access  Private
 */
router.post('/:uid/subscription-history', verifyFirebaseToken, async (req, res) => {
    try {
        const { uid } = req.params;
        const { plan, startedAt, endedAt } = req.body;
        
        if (!plan || !startedAt || !endedAt) {
            return res.status(400).json({
                success: false,
                message: 'Plan, startedAt, and endedAt are required'
            });
        }
        
        const user = await User.findOne({ uid });
        if (!user) {
            return res.status(404).json({
                success: false,
                message: 'User not found'
            });
        }
        
        if (!user.subscriptionHistory) {
            user.subscriptionHistory = [];
        }
        
        user.subscriptionHistory.push({
            plan,
            startedAt: new Date(startedAt),
            endedAt: new Date(endedAt)
        });
        
        await user.save();
        
        res.status(200).json({
            success: true,
            message: 'Subscription history added successfully',
            subscriptionHistory: user.subscriptionHistory
        });
    } catch (error) {
        logger.error(`Add subscription history error: ${error.message}`);
        res.status(500).json({
            success: false,
            message: 'Server error',
            error: error.message
        });
    }
});

/**
 * @route   PUT /api/users/:uid/subscription-offer
 * @desc    Update user subscription offer
 * @access  Private
 */
router.put('/:uid/subscription-offer', verifyFirebaseToken, async (req, res) => {
    try {
        const { uid } = req.params;
        const offerData = req.body;
        
        const user = await User.findOne({ uid });
        if (!user) {
            return res.status(404).json({
                success: false,
                message: 'User not found'
            });
        }
        
        user.subscriptionOffer = offerData;
        await user.save();
        
        res.status(200).json({
            success: true,
            message: 'Subscription offer updated successfully',
            subscriptionOffer: user.subscriptionOffer
        });
    } catch (error) {
        logger.error(`Update subscription offer error: ${error.message}`);
        res.status(500).json({
            success: false,
            message: 'Server error',
            error: error.message
        });
    }
});

module.exports = router; 