const express = require('express');
const router = express.Router();
const { AdMob, adTypes } = require('../models/AdMob');

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
    try {
        const page = parseInt(req.query.page) || 1;
        const limit = parseInt(req.query.limit) || 10;
        const search = req.query.search || '';
        const adType = req.query.adType;
        const status = req.query.status !== undefined ? req.query.status === 'true' : undefined;

        // Build query
        const query = {};
        if (search) {
            query.$or = [
                { adName: { $regex: search, $options: 'i' } },
                { adUnitCode: { $regex: search, $options: 'i' } }
            ];
        }
        if (adType) query.adType = adType;
        if (status !== undefined) query.status = status;

        // Execute query with pagination
        const [ads, total] = await Promise.all([
            AdMob.find(query)
                .sort({ createdAt: -1 })
                .skip((page - 1) * limit)
                .limit(limit),
            AdMob.countDocuments(query)
        ]);

        res.json({
            success: true,
            data: {
                ads,
                pagination: {
                    total,
                    page,
                    pageSize: limit,
                    totalPages: Math.ceil(total / limit)
                }
            }
        });
    } catch (error) {
        console.error('Error fetching ads:', error);
        res.status(500).json({
            success: false,
            message: 'Failed to fetch ads',
            error: error.message
        });
    }
});

// POST /api/admob-ads - Create new ad
router.post('/', validateAdData, async (req, res) => {
    try {
        const { adName, adType, adUnitCode, status } = req.body;

        // Create new ad
        const newAd = new AdMob({
            adName: adName.trim(),
            adType,
            adUnitCode: adUnitCode.trim(),
            status: status !== undefined ? status : true
        });

        // Save to database
        const savedAd = await newAd.save();

        res.status(201).json({
            success: true,
            message: 'Ad created successfully',
            data: savedAd
        });
    } catch (error) {
        console.error('Error creating ad:', error);

        // Handle duplicate key error
        if (error.message === 'Ad unit code already exists') {
            return res.status(400).json({
                success: false,
                message: 'Ad unit code already exists',
                errors: {
                    adUnitCode: 'This Ad unit code is already in use'
                }
            });
        }

        // Handle validation errors
        if (error.name === 'ValidationError') {
            const errors = {};
            for (let field in error.errors) {
                errors[field] = error.errors[field].message;
            }
            return res.status(400).json({
                success: false,
                message: 'Validation failed',
                errors
            });
        }

        res.status(500).json({
            success: false,
            message: 'Failed to create ad',
            error: error.message
        });
    }
});

// PUT /api/admob-ads/:id - Update ad
router.put('/:id', validateAdData, async (req, res) => {
    try {
        const { adName, adType, adUnitCode, status } = req.body;

        const updatedAd = await AdMob.findByIdAndUpdate(
            req.params.id,
            {
                adName: adName.trim(),
                adType,
                adUnitCode: adUnitCode.trim(),
                status: status !== undefined ? status : true
            },
            { new: true, runValidators: true }
        );

        if (!updatedAd) {
            return res.status(404).json({
                success: false,
                message: 'Ad not found'
            });
        }

        res.json({
            success: true,
            message: 'Ad updated successfully',
            data: updatedAd
        });
    } catch (error) {
        console.error('Error updating ad:', error);

        // Handle duplicate key error
        if (error.message === 'Ad unit code already exists') {
            return res.status(400).json({
                success: false,
                message: 'Ad unit code already exists',
                errors: {
                    adUnitCode: 'This Ad unit code is already in use'
                }
            });
        }

        // Handle validation errors
        if (error.name === 'ValidationError') {
            const errors = {};
            for (let field in error.errors) {
                errors[field] = error.errors[field].message;
            }
            return res.status(400).json({
                success: false,
                message: 'Validation failed',
                errors
            });
        }

        res.status(500).json({
            success: false,
            message: 'Failed to update ad',
            error: error.message
        });
    }
});

// DELETE /api/admob-ads/:id - Delete ad
router.delete('/:id', async (req, res) => {
    try {
        const deletedAd = await AdMob.findByIdAndDelete(req.params.id);
        
        if (!deletedAd) {
            return res.status(404).json({
                success: false,
                message: 'Ad not found'
            });
        }

        res.json({
            success: true,
            message: 'Ad deleted successfully'
        });
    } catch (error) {
        console.error('Error deleting ad:', error);
        res.status(500).json({
            success: false,
            message: 'Failed to delete ad',
            error: error.message
        });
    }
});

// GET /api/admob-ads/types - Get all ad types
router.get('/types', (req, res) => {
    res.json({
        success: true,
        data: adTypes
    });
});

// PATCH /api/admob-ads/:id/status - Toggle ad status
router.patch('/:id/status', async (req, res) => {
    try {
        const { status } = req.body;
        
        // Validate status
        if (typeof status !== 'boolean') {
            return res.status(400).json({
                success: false,
                message: 'Invalid status value',
                errors: {
                    status: 'Status must be a boolean value'
                }
            });
        }

        const updatedAd = await AdMob.findByIdAndUpdate(
            req.params.id,
            { status },
            { new: true, runValidators: true }
        );

        if (!updatedAd) {
            return res.status(404).json({
                success: false,
                message: 'Ad not found'
            });
        }

        res.json({
            success: true,
            message: 'Ad status updated successfully',
            data: updatedAd
        });
    } catch (error) {
        console.error('Error updating ad status:', error);
        res.status(500).json({
            success: false,
            message: 'Failed to update ad status',
            error: error.message
        });
    }
});

module.exports = router;
