const SharedWish = require('../models/SharedWish');
const crypto = require('crypto');

// Generate short code
const generateShortCode = () => {
    return crypto.randomBytes(4).toString('base64')
        .replace(/[+/=]/g, '')  // remove non-url-safe chars
        + crypto.randomBytes(2).toString('hex'); // add some hex chars
};

// Create shared wish
exports.createSharedWish = async (req, res) => {
    try {
        const { templateId, recipientName, senderName, customizedHtml } = req.body;
        
        const shortCode = generateShortCode();
        
        const sharedWish = new SharedWish({
            shortCode,
            templateId,
            recipientName,
            senderName,
            customizedHtml
        });

        await sharedWish.save();
        res.status(201).json(sharedWish);
    } catch (error) {
        res.status(500).json({ message: error.message });
    }
};

// Get shared wish by short code
exports.getSharedWish = async (req, res) => {
    try {
        const { shortCode } = req.params;
        const sharedWish = await SharedWish.findOne({ shortCode })
            .populate('templateId');

        if (!sharedWish) {
            return res.status(404).json({ message: 'Shared wish not found' });
        }

        // Increment views
        sharedWish.views += 1;
        await sharedWish.save();

        res.json(sharedWish);
    } catch (error) {
        res.status(500).json({ message: error.message });
    }
};
