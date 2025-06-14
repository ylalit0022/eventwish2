const express = require('express');
const router = express.Router();
const { AdMob, adTypes } = require('../models/AdMob');
const { verifyApiKey } = require('../middleware/authMiddleware');

// Apply API key verification to all routes
router.use(verifyApiKey);

// Validate ad data middleware
const validateAdData = (req, res, next) => {
    try {
        const { adName, adUnitCode, adType } = req.body;

        // Skip validation for status-only updates
        if (req.method === 'PATCH' || (Object.keys(req.body).length === 1 && req.body.status !== undefined)) {
            return next();
        }

        const errors = {};
        let hasErrors = false;

        // Validate adName
        if (!adName || typeof adName !== 'string' || adName.trim().length === 0) {
            errors.adName = 'Ad name is required';
            hasErrors = true;
        } else if (adName.length > 100) {
            errors.adName = 'Ad name cannot exceed 100 characters';
            hasErrors = true;
        }

        // Validate adType
        if (!adType) {
            errors.adType = 'Ad type is required';
            hasErrors = true;
        } else if (!adTypes.includes(adType)) {
            errors.adType = `Ad type must be one of: ${adTypes.join(', ')}`;
            hasErrors = true;
        }

        // Validate adUnitCode
        if (!adUnitCode || typeof adUnitCode !== 'string' || adUnitCode.trim().length === 0) {
            errors.adUnitCode = 'Ad unit code is required';
            hasErrors = true;
        } else {
            const adUnitCodeRegex = /^ca-app-pub-\d{16}\/\d{10}$/;
            if (!adUnitCodeRegex.test(adUnitCode.trim())) {
                errors.adUnitCode = 'Ad unit code should be like: ca-app-pub-XXXXXXXXXXXXXXXX/YYYYYYYYYY';
                hasErrors = true;
            }
        }

        if (hasErrors) {
            return res.status(400).json({
                success: false,
                message: 'Validation failed',
                errors
            });
        }

        next();
    } catch (error) {
        console.error('Validation error:', error);
        res.status(500).json({
            success: false,
            message: 'Validation error occurred',
            error: error.message
        });
    }
};

// GET /api/admob-ads - Get all ads with filters and pagination
router.get('/', async (req, res) => {
    // ... existing code ...
});

// POST /api/admob-ads - Create new ad
router.post('/', validateAdData, async (req, res) => {
    // ... existing code ...
});

// PUT /api/admob-ads/:id - Update ad
router.put('/:id', validateAdData, async (req, res) => {
    // ... existing code ...
});

// DELETE /api/admob-ads/:id - Delete ad
router.delete('/:id', async (req, res) => {
    // ... existing code ...
});

// ... rest of the code ...

module.exports = router; 