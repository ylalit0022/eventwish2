const express = require('express');
const router = express.Router();
const { 
    createSharedWish, 
    getSharedWish,
    updateSharedWishPlatform,
    getWishAnalytics
} = require('../controllers/wishController');

// Try to load the SharedWish model
let SharedWish;
try {
    SharedWish = require('../models/SharedWish');
    console.log('Using SharedWish model from ../models/SharedWish in routes/wishes.js');
} catch (error) {
    console.error('Error loading SharedWish from ../models/SharedWish in routes/wishes.js:', error);
    try {
        SharedWish = require('../SharedWish');
        console.log('Using SharedWish model from ../SharedWish in routes/wishes.js');
    } catch (error) {
        console.error('Error loading SharedWish from ../SharedWish in routes/wishes.js:', error);
        throw new Error('Could not load SharedWish model in routes/wishes.js');
    }
}

// Create shared wish
router.post('/create', createSharedWish);

// Get shared wish by short code
router.get('/:shortCode', getSharedWish);

// Update shared wish platform
router.post('/:shortCode/share', async (req, res) => {
    try {
        const { shortCode } = req.params;
        const { platform } = req.body;
        
        if (!platform) {
            return res.status(400).json({ message: 'Platform is required' });
        }
        
        // Find the shared wish
        const wish = await SharedWish.findOne({ shortCode });
        
        if (!wish) {
            return res.status(404).json({ message: 'Shared wish not found' });
        }
        
        // Update the share history
        wish.shareHistory.push({
            platform,
            timestamp: new Date()
        });
        
        // Update share count and last shared time
        wish.shareCount = (wish.shareCount || 0) + 1;
        wish.lastSharedAt = new Date();
        wish.sharedVia = platform;
        
        await wish.save();
        
        return res.status(200).json({ 
            message: 'Share platform updated',
            shareCount: wish.shareCount
        });
    } catch (error) {
        console.error('Error updating share platform:', error);
        return res.status(500).json({ message: 'Error updating share platform' });
    }
});

// Get wish analytics
router.get('/:shortCode/analytics', async (req, res) => {
    try {
        const { shortCode } = req.params;
        
        // Find the shared wish
        const wish = await SharedWish.findOne({ shortCode });
        
        if (!wish) {
            return res.status(404).json({ message: 'Shared wish not found' });
        }
        
        // Prepare analytics data
        const analytics = {
            views: wish.views || 0,
            uniqueViews: wish.uniqueViews || 0,
            shareCount: wish.shareCount || 0,
            shareHistory: wish.shareHistory || [],
            lastSharedAt: wish.lastSharedAt,
            conversionSource: wish.conversionSource,
            referrer: wish.referrer,
            deviceInfo: wish.deviceInfo
        };
        
        return res.status(200).json(analytics);
    } catch (error) {
        console.error('Error getting wish analytics:', error);
        return res.status(500).json({ message: 'Error getting wish analytics' });
    }
});

// Track app installation from landing page
router.post('/:shortCode/analytics/install', async (req, res) => {
    try {
        const { shortCode } = req.params;
        
        const sharedWish = await SharedWish.findOne({ shortCode });
        
        if (!sharedWish) {
            return res.status(404).json({ message: 'Shared wish not found' });
        }
        
        // Update conversion source
        sharedWish.conversionSource = 'LANDING_PAGE';
        await sharedWish.save();
        
        res.status(200).json({ message: 'Installation tracked successfully' });
    } catch (error) {
        console.error('Error tracking installation:', error);
        res.status(500).json({ message: 'Error tracking installation' });
    }
});

// Generate shareable wish content based on templateId
router.get('/share/:templateId', async (req, res) => {
    try {
        const { templateId } = req.params;
        const { title, description, senderName, recipientName } = req.query;
        
        // Fetch the template
        const Template = require('../models/Template');
        const template = await Template.findById(templateId);
        
        if (!template) {
            return res.status(404).json({ message: 'Template not found' });
        }
        
        // Generate a unique short code for this share
        const shortCode = generateShortCode();
        
        // Create a shareable URL
        const shareableUrl = `https://eventwishes.onrender.com/wish/${shortCode}`;
        
        // Create a temporary wish record to track this share
        const sharedWish = new SharedWish({
            shortCode,
            template: templateId,
            title: title || 'EventWish Greeting',
            description: description || `A special wish from ${senderName || 'Someone'} to ${recipientName || 'you'}`,
            senderName: senderName || 'Someone',
            recipientName: recipientName || 'you',
            createdAt: new Date(),
            views: 0,
            uniqueViews: 0
        });
        
        await sharedWish.save();
        
        // Return the shareable data
        res.json({
            success: true,
            shareableUrl,
            shortCode,
            previewUrl: template.thumbnailUrl || '/images/default-preview.png',
            title: sharedWish.title,
            description: sharedWish.description,
            deepLink: `eventwish://wish/${shortCode}`
        });
    } catch (error) {
        console.error('Error generating shareable content:', error);
        res.status(500).json({ message: 'Error generating shareable content', error: error.message });
    }
});

// Helper function to generate a short code
function generateShortCode() {
    const characters = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';
    const length = 8;
    let result = '';
    
    for (let i = 0; i < length; i++) {
        result += characters.charAt(Math.floor(Math.random() * characters.length));
    }
    
    return result;
}

module.exports = router;
