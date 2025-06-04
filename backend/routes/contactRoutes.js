const express = require('express');
const router = express.Router();
const Contact = require('../models/Contact');
const logger = require('../config/logger');
const { verifyApiKey } = require('../middleware/authMiddleware');

/**
 * @route   GET /api/contact
 * @desc    Get active contact content
 * @access  Public
 */
router.get('/', async (req, res) => {
    try {
        const contact = await Contact.getActive();
        
        if (!contact) {
            return res.status(404).json({
                success: false,
                message: 'No active contact content found'
            });
        }

        res.json({
            success: true,
            data: contact
        });
    } catch (error) {
        logger.error(`Error fetching contact content: ${error.message}`);
        res.status(500).json({
            success: false,
            message: 'Error fetching contact content',
            error: error.message
        });
    }
});

/**
 * @route   POST /api/contact
 * @desc    Create new contact content
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

        const contact = new Contact({
            title,
            htmlCode,
            isActive: true
        });

        await contact.save();

        res.status(201).json({
            success: true,
            message: 'Contact content created successfully',
            data: contact
        });
    } catch (error) {
        logger.error(`Error creating contact content: ${error.message}`);
        res.status(500).json({
            success: false,
            message: 'Error creating contact content',
            error: error.message
        });
    }
});

/**
 * @route   PUT /api/contact/:id
 * @desc    Update contact content
 * @access  Private
 */
router.put('/:id', verifyApiKey, async (req, res) => {
    try {
        const { title, htmlCode, isActive } = req.body;
        const contactId = req.params.id;

        const contact = await Contact.findById(contactId);

        if (!contact) {
            return res.status(404).json({
                success: false,
                message: 'Contact content not found'
            });
        }

        if (title) contact.title = title;
        if (htmlCode) contact.htmlCode = htmlCode;
        if (typeof isActive === 'boolean') contact.isActive = isActive;

        await contact.save();

        res.json({
            success: true,
            message: 'Contact content updated successfully',
            data: contact
        });
    } catch (error) {
        logger.error(`Error updating contact content: ${error.message}`);
        res.status(500).json({
            success: false,
            message: 'Error updating contact content',
            error: error.message
        });
    }
});

module.exports = router; 