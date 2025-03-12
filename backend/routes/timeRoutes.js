const express = require('express');
const router = express.Router();

/**
 * @route   GET /api/time
 * @desc    Get current server time
 * @access  Public
 */
router.get('/', (req, res) => {
    try {
        // Get current server timestamp in milliseconds
        const timestamp = Date.now();
        const date = new Date(timestamp).toISOString();
        
        res.json({
            success: true,
            timestamp: timestamp,
            date: date
        });
    } catch (error) {
        console.error('Error in time endpoint:', error);
        res.status(500).json({
            success: false,
            message: 'Error getting server time'
        });
    }
});

module.exports = router;

 