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
        logger.info('GET /api/contact - Fetching active contact content');
        const contact = await Contact.getActive();
        
        if (!contact) {
            logger.warn('GET /api/contact - No active contact content found');
            return res.status(404).json({
                success: false,
                message: 'No active contact content found'
            });
        }

        logger.info('GET /api/contact - Successfully retrieved contact content');
        res.json({
            success: true,
            data: contact
        });
    } catch (error) {
        logger.error(`Error fetching contact content: ${error.message}`, { error });
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
        logger.error(`Error creating contact content: ${error.message}`, { error });
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
        logger.error(`Error updating contact content: ${error.message}`, { error });
        res.status(500).json({
            success: false,
            message: 'Error updating contact content',
            error: error.message
        });
    }
});

// Create initial contact content if none exists
const createInitialContactContent = async () => {
    try {
        const existingContent = await Contact.getActive();
        if (!existingContent) {
            const initialContact = new Contact({
                title: 'Contact Us',
                htmlCode: `
                    <h1>Contact Us</h1>
                    <p>We'd love to hear from you! If you have any questions, suggestions, or feedback about EventWish, please don't hesitate to reach out.</p>
                    <h2>Support Email</h2>
                    <p><a href="mailto:support@eventwish.com">support@eventwish.com</a></p>
                    <h2>Follow Us</h2>
                    <p>Stay updated with our latest features and announcements:</p>
                    <ul>
                        <li><a href="https://twitter.com/eventwish">Twitter</a></li>
                        <li><a href="https://facebook.com/eventwish">Facebook</a></li>
                        <li><a href="https://instagram.com/eventwish">Instagram</a></li>
                    </ul>
                `,
                isActive: true
            });
            await initialContact.save();
            logger.info('Created initial contact content');
        }
    } catch (error) {
        logger.error(`Error creating initial contact content: ${error.message}`, { error });
    }
};

// Call the function to create initial content
createInitialContactContent();

module.exports = router; 