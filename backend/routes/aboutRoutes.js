const express = require('express');
const router = express.Router();
const About = require('../models/About');
const logger = require('../config/logger');
const { verifyApiKey } = require('../middleware/authMiddleware');

/**
 * @route   GET /api/about
 * @desc    Get active about content
 * @access  Public
 */
router.get('/', async (req, res) => {
    try {
        const about = await About.getActive();
        
        if (!about) {
            return res.status(404).json({
                success: false,
                message: 'No active about content found'
            });
        }

        res.json({
            success: true,
            data: about
        });
    } catch (error) {
        logger.error(`Error fetching about content: ${error.message}`);
        res.status(500).json({
            success: false,
            message: 'Error fetching about content',
            error: error.message
        });
    }
});

/**
 * @route   POST /api/about
 * @desc    Create new about content
 * @access  Private
 */
router.post('/', verifyApiKey, async (req, res) => {
    try {
        const { title, htmlCode } = req.body;

        if (!title || !htmlCode) {
            return res.status(400).json({
                success: false,
                message: 'Title and HTML content are required'
            });
        }

        const about = new About({
            title,
            htmlCode,
            isActive: true
        });

        await about.save();

        res.status(201).json({
            success: true,
            message: 'About content created successfully',
            data: about
        });
    } catch (error) {
        logger.error(`Error creating about content: ${error.message}`);
        res.status(500).json({
            success: false,
            message: 'Error creating about content',
            error: error.message
        });
    }
});

/**
 * @route   PUT /api/about/:id
 * @desc    Update about content
 * @access  Private
 */
router.put('/:id', verifyApiKey, async (req, res) => {
    try {
        const { title, htmlCode, isActive } = req.body;
        const aboutId = req.params.id;

        const about = await About.findById(aboutId);

        if (!about) {
            return res.status(404).json({
                success: false,
                message: 'About content not found'
            });
        }

        if (title) about.title = title;
        if (htmlCode) about.htmlCode = htmlCode;
        if (typeof isActive === 'boolean') about.isActive = isActive;

        await about.save();

        res.json({
            success: true,
            message: 'About content updated successfully',
            data: about
        });
    } catch (error) {
        logger.error(`Error updating about content: ${error.message}`);
        res.status(500).json({
            success: false,
            message: 'Error updating about content',
            error: error.message
        });
    }
});

module.exports = router; 