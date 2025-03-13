// Try to load the SharedWish model
let SharedWish;
try {
    SharedWish = require('../models/SharedWish');
    console.log('Using SharedWish model from ../models/SharedWish');
} catch (error) {
    console.error('Error loading SharedWish from ../models/SharedWish:', error);
    try {
        SharedWish = require('../SharedWish');
        console.log('Using SharedWish model from ../SharedWish');
    } catch (error) {
        console.error('Error loading SharedWish from ../SharedWish:', error);
        throw new Error('Could not load SharedWish model');
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
exports.createSharedWish = async (req, res) => {
    try {
        const { templateId, recipientName, senderName, customizedHtml, cssContent, jsContent } = req.body;
        
        if (!templateId) {
            return res.status(400).json({ 
                success: false,
                message: 'Template ID is required' 
            });
        }
        
        const shortCode = generateShortCode();
        
        // Create the shared wish with template field correctly set
        const sharedWish = new SharedWish({
            shortCode,
            template: templateId, // Map templateId to template field
            recipientName: recipientName || 'You',
            senderName: senderName || 'Someone',
            customizedHtml,
            cssContent,
            jsContent
        });

        console.log(`Creating shared wish with shortCode: ${shortCode}, template: ${templateId}`);
        
        await sharedWish.save();
        
        console.log(`Shared wish created successfully with shortCode: ${shortCode}`);
        
        // Return the created wish with success flag
        res.status(201).json({
            success: true,
            shortCode: shortCode,
            ...sharedWish.toObject()
        });
    } catch (error) {
        console.error('Error creating shared wish:', error);
        res.status(500).json({ 
            success: false,
            message: error.message 
        });
    }
};

// Get shared wish by short code
exports.getSharedWish = async (req, res) => {
    try {
        const { shortCode } = req.params;
        console.log(`Getting wish with shortCode: ${shortCode}`);
        
        const sharedWish = await SharedWish.findOne({ shortCode })
            .populate('template');

        if (!sharedWish) {
            console.log(`Wish not found with shortCode: ${shortCode}`);
            return res.status(404).json({ 
                success: false,
                message: 'Shared wish not found' 
            });
        }

        // Increment views
        sharedWish.views += 1;
        
        // Track unique views by IP
        const clientIp = req.headers['x-forwarded-for'] || req.connection.remoteAddress;
        if (clientIp && !sharedWish.viewerIps.includes(clientIp)) {
            sharedWish.viewerIps.push(clientIp);
            sharedWish.uniqueViews += 1;
        }
        
        // Track referrer if available
        if (req.headers.referer && !sharedWish.referrer) {
            sharedWish.referrer = req.headers.referer;
        }
        
        // Track device info if available
        if (req.headers['user-agent'] && !sharedWish.deviceInfo) {
            sharedWish.deviceInfo = req.headers['user-agent'];
        }
        
        await sharedWish.save();
        console.log(`Successfully retrieved wish with shortCode: ${shortCode}`);
        
        // Format the response to match what the app expects
        const response = {
            success: true,
            data: sharedWish
        };
        
        res.json(response);
    } catch (error) {
        console.error('Error getting shared wish:', error);
        res.status(500).json({ 
            success: false,
            message: 'Error getting shared wish', 
            error: error.message 
        });
    }
};

// Update shared wish with sharing platform
exports.updateSharedWishPlatform = async (req, res) => {
    try {
        const { shortCode } = req.params;
        const { platform } = req.body;
        
        if (!platform) {
            return res.status(400).json({ message: 'Platform is required' });
        }
        
        const sharedWish = await SharedWish.findOne({ shortCode });
        
        if (!sharedWish) {
            return res.status(404).json({ message: 'Shared wish not found' });
        }
        
        // Update sharing platform
        sharedWish.sharedVia = platform;
        sharedWish.lastSharedAt = new Date();
        sharedWish.shareCount += 1;
        
        // Add to share history
        sharedWish.shareHistory.push({
            platform,
            timestamp: new Date()
        });
        
        await sharedWish.save();
        
        res.json({ message: 'Shared wish updated successfully' });
    } catch (error) {
        res.status(500).json({ message: error.message });
    }
};

// Get sharing analytics for a wish
exports.getWishAnalytics = async (req, res) => {
    try {
        const { shortCode } = req.params;
        
        const sharedWish = await SharedWish.findOne({ shortCode });
        
        if (!sharedWish) {
            return res.status(404).json({ message: 'Shared wish not found' });
        }
        
        // Prepare analytics data
        const analytics = {
            views: sharedWish.views,
            uniqueViews: sharedWish.uniqueViews,
            shareCount: sharedWish.shareCount,
            lastSharedAt: sharedWish.lastSharedAt,
            shareHistory: sharedWish.shareHistory,
            conversionRate: sharedWish.uniqueViews > 0 ? 
                (sharedWish.shareCount / sharedWish.uniqueViews) : 0,
            platformBreakdown: {}
        };
        
        // Calculate platform breakdown
        if (sharedWish.shareHistory && sharedWish.shareHistory.length > 0) {
            sharedWish.shareHistory.forEach(share => {
                if (!analytics.platformBreakdown[share.platform]) {
                    analytics.platformBreakdown[share.platform] = 0;
                }
                analytics.platformBreakdown[share.platform] += 1;
            });
        }
        
        res.json(analytics);
    } catch (error) {
        res.status(500).json({ message: error.message });
    }
};
