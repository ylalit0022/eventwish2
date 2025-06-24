const express = require("express");
const router = express.Router();
const User = require("../../models/User");
const logger = require("../../utils/logger");

/**
 * @route   GET /api/users/:uid/block-status
 * @desc    Check if a user is blocked and get blocking details
 * @access  Public (needed for client-side blocking check)
 */
router.get("/:uid/block-status", async (req, res) => {
    try {
        const { uid } = req.params;
        
        const user = await User.findOne({ uid });
        if (!user) {
            return res.status(404).json({
                success: false,
                message: "User not found"
            });
        }
        
        const isBlocked = user.isCurrentlyBlocked();
        
        res.status(200).json({
            success: true,
            isBlocked: isBlocked,
            blockInfo: isBlocked ? {
                reason: user.blockInfo?.reason || "Account has been blocked",
                blockedAt: user.blockInfo?.blockedAt,
                blockExpiresAt: user.blockInfo?.blockExpiresAt,
                contactEmail: "support@eventwish.com"
            } : null
        });
    } catch (error) {
        logger.error(`Check block status error: ${error.message}`);
        res.status(500).json({
            success: false,
            message: "Server error",
            error: error.message
        });
    }
});

module.exports = router;
