const express = require('express');
const router = express.Router();

// Try to load the SharedWish model
let SharedWish;
try {
    SharedWish = require('../models/SharedWish');
    console.log('Using SharedWish model from ../models/SharedWish in routes/share.js');
} catch (error) {
    console.error('Error loading SharedWish from ../models/SharedWish in routes/share.js:', error);
    try {
        SharedWish = require('../SharedWish');
        console.log('Using SharedWish model from ../SharedWish in routes/share.js');
    } catch (error) {
        console.error('Error loading SharedWish from ../SharedWish in routes/share.js:', error);
        throw new Error('Could not load SharedWish model in routes/share.js');
    }
}

const crypto = require('crypto');

// Generate short code
const generateShortCode = () => {
    return crypto.randomBytes(4).toString('base64')
        .replace(/[+/=]/g, '')  // remove non-url-safe chars
        + crypto.randomBytes(2).toString('hex'); // add some hex chars
};

// Create shared wish
router.post('/', async (req, res) => {
    try {
        console.log('Received share request:', req.body);
        
        const { templateId, recipientName, senderName, customizedHtml, cssContent, jsContent, sharedVia } = req.body;
        
        if (!templateId) {
            return res.status(400).json({ message: 'Template ID is required' });
        }
        
        const shortCode = generateShortCode();
        
        // Ensure sharedVia is uppercase to match enum values
        const sharedViaUpperCase = sharedVia ? sharedVia.toUpperCase() : 'LINK';
        
        // Create a shareable URL
        const shareableUrl = `https://eventwish2.onrender.com/wish/${shortCode}`;
        
        const sharedWish = new SharedWish({
            shortCode,
            template: templateId,
            title: 'EventWish Greeting',
            description: `A special wish from ${senderName || 'Someone'} to ${recipientName || 'you'}`,
            recipientName: recipientName || 'you',
            senderName: senderName || 'Someone',
            customizedHtml: customizedHtml || '',
            cssContent: cssContent || '',
            jsContent: jsContent || '',
            sharedVia: sharedViaUpperCase,
            views: 0,
            uniqueViews: 0,
            shareCount: 0,
            shareHistory: [{
                platform: sharedViaUpperCase,
                timestamp: new Date()
            }],
            createdAt: new Date()
        });
        
        await sharedWish.save();
        
        res.status(200).json({
            shortCode,
            shareUrl: shareableUrl,
            message: 'Wish shared successfully'
        });
    } catch (error) {
        console.error('Error creating shared wish:', error);
        res.status(500).json({ message: 'Error creating shared wish', error: error.message });
    }
});

module.exports = router; 