const express = require('express');
const router = express.Router();
const User = require('../../models/User');
const logger = require('../../utils/logger');
const { verifyFirebaseToken } = require('../../middleware/auth');

/**
 * @route   PUT /api/users/:uid/template-affinity
 * @desc    Update template affinity scores
 * @access  Private
 */
router.put('/:uid/template-affinity', verifyFirebaseToken, async (req, res) => {
    try {
        const { uid } = req.params;
        const { tag, score = 1 } = req.body;
        
        if (!tag) {
            return res.status(400).json({
                success: false,
                message: 'Tag is required'
            });
        }
        
        const user = await User.findOne({ uid });
        if (!user) {
            return res.status(404).json({
                success: false,
                message: 'User not found'
            });
        }
        
        await user.updateTemplateAffinity(tag, score);
        
        res.status(200).json({
            success: true,
            message: 'Template affinity updated',
            templateAffinity: user.templateAffinity
        });
    } catch (error) {
        logger.error(`Update template affinity error: ${error.message}`);
        res.status(500).json({
            success: false,
            message: 'Server error',
            error: error.message
        });
    }
});

/**
 * @route   GET /api/users/:uid/template-affinity/top
 * @desc    Get top template affinities
 * @access  Private
 */
router.get('/:uid/template-affinity/top', verifyFirebaseToken, async (req, res) => {
    try {
        const { uid } = req.params;
        const { limit = 10 } = req.query;
        
        const user = await User.findOne({ uid });
        if (!user) {
            return res.status(404).json({
                success: false,
                message: 'User not found'
            });
        }
        
        const topAffinities = user.templateAffinity
            .sort((a, b) => b.score - a.score)
            .slice(0, parseInt(limit));
        
        res.status(200).json({
            success: true,
            data: topAffinities
        });
    } catch (error) {
        logger.error(`Get top template affinity error: ${error.message}`);
        res.status(500).json({
            success: false,
            message: 'Server error',
            error: error.message
        });
    }
});

/**
 * @route   POST /api/users/:uid/ignored-templates
 * @desc    Add template to ignored list
 * @access  Private
 */
router.post('/:uid/ignored-templates', verifyFirebaseToken, async (req, res) => {
    try {
        const { uid } = req.params;
        const { templateId, score = 1 } = req.body;
        
        if (!templateId) {
            return res.status(400).json({
                success: false,
                message: 'Template ID is required'
            });
        }
        
        const user = await User.findOne({ uid });
        if (!user) {
            return res.status(404).json({
                success: false,
                message: 'User not found'
            });
        }
        
        await user.ignoreTemplate(templateId, score);
        
        res.status(200).json({
            success: true,
            message: 'Template added to ignored list',
            ignoredTemplates: user.ignoredTemplates
        });
    } catch (error) {
        logger.error(`Add ignored template error: ${error.message}`);
        res.status(500).json({
            success: false,
            message: 'Server error',
            error: error.message
        });
    }
});

/**
 * @route   DELETE /api/users/:uid/ignored-templates/:templateId
 * @desc    Remove template from ignored list
 * @access  Private
 */
router.delete('/:uid/ignored-templates/:templateId', verifyFirebaseToken, async (req, res) => {
    try {
        const { uid, templateId } = req.params;
        
        const user = await User.findOne({ uid });
        if (!user) {
            return res.status(404).json({
                success: false,
                message: 'User not found'
            });
        }
        
        user.ignoredTemplates = user.ignoredTemplates.filter(
            ignored => ignored.templateId.toString() !== templateId
        );
        
        await user.save();
        
        res.status(200).json({
            success: true,
            message: 'Template removed from ignored list'
        });
    } catch (error) {
        logger.error(`Remove ignored template error: ${error.message}`);
        res.status(500).json({
            success: false,
            message: 'Server error',
            error: error.message
        });
    }
});

module.exports = router; 