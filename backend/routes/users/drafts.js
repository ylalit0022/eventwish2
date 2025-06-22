const express = require('express');
const router = express.Router();
const User = require('../../models/User');
const logger = require('../../utils/logger');
const { verifyFirebaseToken } = require('../../middleware/auth');

/**
 * @route   POST /api/users/:uid/drafts
 * @desc    Save or update draft
 * @access  Private
 */
router.post('/:uid/drafts', verifyFirebaseToken, async (req, res) => {
    try {
        const { uid } = req.params;
        const { templateId, html } = req.body;
        
        if (!templateId || !html) {
            return res.status(400).json({
                success: false,
                message: 'Template ID and HTML content are required'
            });
        }
        
        const user = await User.findOne({ uid });
        if (!user) {
            return res.status(404).json({
                success: false,
                message: 'User not found'
            });
        }
        
        await user.saveDraft(templateId, html);
        
        res.status(200).json({
            success: true,
            message: 'Draft saved successfully',
            drafts: user.drafts
        });
    } catch (error) {
        logger.error(`Save draft error: ${error.message}`);
        res.status(500).json({
            success: false,
            message: 'Server error',
            error: error.message
        });
    }
});

/**
 * @route   GET /api/users/:uid/drafts
 * @desc    Get user's drafts
 * @access  Private
 */
router.get('/:uid/drafts', verifyFirebaseToken, async (req, res) => {
    try {
        const { uid } = req.params;
        
        const user = await User.findOne({ uid }).populate('drafts.templateId', 'name imageUrl category');
        if (!user) {
            return res.status(404).json({
                success: false,
                message: 'User not found'
            });
        }
        
        res.status(200).json({
            success: true,
            data: user.drafts
        });
    } catch (error) {
        logger.error(`Get drafts error: ${error.message}`);
        res.status(500).json({
            success: false,
            message: 'Server error',
            error: error.message
        });
    }
});

/**
 * @route   DELETE /api/users/:uid/drafts/:templateId
 * @desc    Delete specific draft
 * @access  Private
 */
router.delete('/:uid/drafts/:templateId', verifyFirebaseToken, async (req, res) => {
    try {
        const { uid, templateId } = req.params;
        
        const user = await User.findOne({ uid });
        if (!user) {
            return res.status(404).json({
                success: false,
                message: 'User not found'
            });
        }
        
        await user.deleteDraft(templateId);
        
        res.status(200).json({
            success: true,
            message: 'Draft deleted successfully'
        });
    } catch (error) {
        logger.error(`Delete draft error: ${error.message}`);
        res.status(500).json({
            success: false,
            message: 'Server error',
            error: error.message
        });
    }
});

module.exports = router; 