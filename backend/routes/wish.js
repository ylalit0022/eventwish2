const express = require('express');
const router = express.Router();
const wishController = require('../controllers/wishController');

// Log that this route is being loaded
console.log('Loading wish route from ../models/SharedWish');

// Get a shared wish by shortCode
router.get('/:shortCode', async (req, res) => {
    try {
        const shortCode = req.params.shortCode;
        console.log(`Received request for wish with shortCode: ${shortCode}`);
        
        // Forward to the wishController's getSharedWish method
        const result = await wishController.getSharedWish(req, res);
        return result;
    } catch (error) {
        console.error('Error in wish route:', error);
        return res.status(500).json({ 
            success: false, 
            message: 'Internal server error',
            error: error.message
        });
    }
});

module.exports = router; 