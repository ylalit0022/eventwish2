const express = require('express');
const router = express.Router();
const User = require('../../models/User');
const Template = require('../../models/Template');
const logger = require('../../utils/logger');
const { verifyFirebaseToken } = require('../../middleware/auth');

/**
 * @route   PUT /api/users/:uid/block
 * @desc    Block a user (Admin only)
 * @access  Private (Admin)
 */
router.put('/:uid/block', verifyFirebaseToken, async (req, res) => {
    try {
        const { uid } = req.params;
        const { reason, expiresAt, notes } = req.body;
        const adminUid = req.user.uid; // From Firebase token
        
        // TODO: Add admin verification middleware
        
        const user = await User.findOne({ uid });
        if (!user) {
            return res.status(404).json({
                success: false,
                message: 'User not found'
            });
        }
        
        await user.blockUser(
            adminUid,
            reason || 'Blocked by administrator',
            expiresAt ? new Date(expiresAt) : null,
            notes || ''
        );
        
        res.status(200).json({
            success: true,
            message: 'User blocked successfully',
            blockInfo: user.blockInfo
        });
    } catch (error) {
        logger.error(`Block user error: ${error.message}`);
        res.status(500).json({
            success: false,
            message: 'Server error',
            error: error.message
        });
    }
});

/**
 * @route   PUT /api/users/:uid/unblock
 * @desc    Unblock a user (Admin only)
 * @access  Private (Admin)
 */
router.put('/:uid/unblock', verifyFirebaseToken, async (req, res) => {
    try {
        const { uid } = req.params;
        
        // TODO: Add admin verification middleware
        
        const user = await User.findOne({ uid });
        if (!user) {
            return res.status(404).json({
                success: false,
                message: 'User not found'
            });
        }
        
        await user.unblockUser();
        
        res.status(200).json({
            success: true,
            message: 'User unblocked successfully'
        });
    } catch (error) {
        logger.error(`Unblock user error: ${error.message}`);
        res.status(500).json({
            success: false,
            message: 'Server error',
            error: error.message
        });
    }
});

/**
 * @route   POST /api/users/:uid/bulk-update
 * @desc    Bulk update multiple user fields
 * @access  Private
 */
router.post('/:uid/bulk-update', verifyFirebaseToken, async (req, res) => {
    try {
        const { uid } = req.params;
        const updateData = req.body;
        
        const user = await User.findOne({ uid });
        if (!user) {
            return res.status(404).json({
                success: false,
                message: 'User not found'
            });
        }
        
        // Update allowed fields
        const allowedFields = [
            'displayName', 'email', 'profilePhoto', 'preferredTheme', 
            'preferredLanguage', 'timezone', 'pushPreferences', 
            'topicSubscriptions', 'muteNotificationsUntil'
        ];
        
        for (const field of allowedFields) {
            if (updateData[field] !== undefined) {
                user[field] = updateData[field];
            }
        }
        
        await user.save();
        
        res.status(200).json({
            success: true,
            message: 'User updated successfully',
            user: {
                uid: user.uid,
                displayName: user.displayName,
                email: user.email,
                profilePhoto: user.profilePhoto,
                preferredTheme: user.preferredTheme,
                preferredLanguage: user.preferredLanguage,
                timezone: user.timezone,
                pushPreferences: user.pushPreferences,
                topicSubscriptions: user.topicSubscriptions,
                muteNotificationsUntil: user.muteNotificationsUntil
            }
        });
    } catch (error) {
        logger.error(`Bulk update user error: ${error.message}`);
        res.status(500).json({
            success: false,
            message: 'Server error',
            error: error.message
        });
    }
});

/**
 * @route   DELETE /api/users/:uid
 * @desc    Delete user account (GDPR compliance)
 * @access  Private
 */
router.delete('/:uid', verifyFirebaseToken, async (req, res) => {
    try {
        const { uid } = req.params;
        
        const user = await User.findOne({ uid });
        if (!user) {
            return res.status(404).json({
                success: false,
                message: 'User not found'
            });
        }
        
        // Remove user from all templates' likes and favorites
        await Template.updateMany(
            { $or: [{ likes: uid }, { favorites: uid }] },
            { $pull: { likes: uid, favorites: uid } }
        );
        
        // Delete user document
        await User.findOneAndDelete({ uid });
        
        logger.info(`User account deleted: ${uid}`);
        
        res.status(200).json({
            success: true,
            message: 'User account deleted successfully'
        });
    } catch (error) {
        logger.error(`Delete user error: ${error.message}`);
        res.status(500).json({
            success: false,
            message: 'Server error',
            error: error.message
        });
    }
});

/**
 * @route   GET /api/users/:uid/block-status
 * @desc    Check if a user is blocked and get blocking details
 * @access  Public (needed for client-side blocking check)
 */
router.get('/:uid/block-status', async (req, res) => {
    try {
        const { uid } = req.params;
        
        const user = await User.findOne({ uid });
        if (!user) {
            return res.status(404).json({
                success: false,
                message: 'User not found'
            });
        }
        
        const isBlocked = user.isCurrentlyBlocked();
        
        res.status(200).json({
            success: true,
            isBlocked: isBlocked,
            blockInfo: isBlocked ? {
                reason: user.blockInfo?.reason || 'Account has been blocked',
                blockedAt: user.blockInfo?.blockedAt,
                blockExpiresAt: user.blockInfo?.blockExpiresAt,
                contactEmail: 'support@eventwish.com'
            } : null
        });
    } catch (error) {
        logger.error(`Check block status error: ${error.message}`);
        res.status(500).json({
            success: false,
            message: 'Server error',
            error: error.message
        });
    }
});

/**
 * @route   GET /api/users/blocked
 * @desc    Get all blocked users (Admin only)
 * @access  Private (Admin)
 */
router.get('/blocked', verifyFirebaseToken, async (req, res) => {
    try {
        // TODO: Add admin verification middleware
        
        const blockedUsers = await User.find({ isBlocked: true })
            .select('uid displayName email blockInfo')
            .sort({ 'blockInfo.blockedAt': -1 });
        
        res.status(200).json({
            success: true,
            users: blockedUsers,
            count: blockedUsers.length
        });
    } catch (error) {
        logger.error(`Get blocked users error: ${error.message}`);
        res.status(500).json({
            success: false,
            message: 'Server error',
            error: error.message
        });
    }
});

module.exports = router; 