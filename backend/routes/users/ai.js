const express = require('express');
const router = express.Router();
const User = require('../../models/User');
const logger = require('../../utils/logger');
const { verifyFirebaseToken } = require('../../middleware/auth');

/**
 * @route   PUT /api/users/:uid/ai-usage
 * @desc    Update AI usage data
 * @access  Private
 */
router.put('/:uid/ai-usage', verifyFirebaseToken, async (req, res) => {
    try {
        const { uid } = req.params;
        const { monthlyGenerationCount, quota, recentPrompts, stylePreferences } = req.body;
        
        const user = await User.findOne({ uid });
        if (!user) {
            return res.status(404).json({
                success: false,
                message: 'User not found'
            });
        }
        
        if (!user.aiUsage) {
            user.aiUsage = {};
        }
        
        if (monthlyGenerationCount !== undefined) {
            user.aiUsage.monthlyGenerationCount = monthlyGenerationCount;
            user.aiUsage.lastGenerationAt = new Date();
        }
        
        if (quota !== undefined) user.aiUsage.quota = quota;
        if (recentPrompts) user.aiUsage.recentPrompts = recentPrompts;
        if (stylePreferences) user.aiUsage.stylePreferences = stylePreferences;
        
        await user.save();
        
        res.status(200).json({
            success: true,
            message: 'AI usage updated successfully',
            aiUsage: user.aiUsage
        });
    } catch (error) {
        logger.error(`Update AI usage error: ${error.message}`);
        res.status(500).json({
            success: false,
            message: 'Server error',
            error: error.message
        });
    }
});

/**
 * @route   POST /api/users/:uid/ai-usage/increment
 * @desc    Increment AI generation count
 * @access  Private
 */
router.post('/:uid/ai-usage/increment', verifyFirebaseToken, async (req, res) => {
    try {
        const { uid } = req.params;
        const { prompt, style } = req.body;
        
        const user = await User.findOne({ uid });
        if (!user) {
            return res.status(404).json({
                success: false,
                message: 'User not found'
            });
        }
        
        if (!user.aiUsage) {
            user.aiUsage = {
                monthlyGenerationCount: 0,
                quota: 5,
                recentPrompts: [],
                stylePreferences: []
            };
        }
        
        // Check quota
        if (user.aiUsage.monthlyGenerationCount >= user.aiUsage.quota) {
            return res.status(403).json({
                success: false,
                message: 'AI generation quota exceeded',
                quota: user.aiUsage.quota,
                used: user.aiUsage.monthlyGenerationCount
            });
        }
        
        // Increment count
        user.aiUsage.monthlyGenerationCount += 1;
        user.aiUsage.lastGenerationAt = new Date();
        
        // Add prompt to recent prompts (keep last 10)
        if (prompt) {
            if (!user.aiUsage.recentPrompts) user.aiUsage.recentPrompts = [];
            user.aiUsage.recentPrompts.unshift(prompt);
            if (user.aiUsage.recentPrompts.length > 10) {
                user.aiUsage.recentPrompts = user.aiUsage.recentPrompts.slice(0, 10);
            }
        }
        
        // Track style preference
        if (style) {
            if (!user.aiUsage.stylePreferences) user.aiUsage.stylePreferences = [];
            if (!user.aiUsage.stylePreferences.includes(style)) {
                user.aiUsage.stylePreferences.push(style);
            }
        }
        
        await user.save();
        
        res.status(200).json({
            success: true,
            message: 'AI generation count incremented',
            aiUsage: user.aiUsage,
            remaining: user.aiUsage.quota - user.aiUsage.monthlyGenerationCount
        });
    } catch (error) {
        logger.error(`Increment AI usage error: ${error.message}`);
        res.status(500).json({
            success: false,
            message: 'Server error',
            error: error.message
        });
    }
});

module.exports = router; 