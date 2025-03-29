const express = require('express');
const router = express.Router();
const { verifyTokenWithRefresh } = require('../middleware/authMiddleware');
const logger = require('../config/logger');
const Coins = require('../models/Coins');
const jwt = require('jsonwebtoken');

// Add authentication endpoint
router.post('/auth', async (req, res) => {
    try {
        const { deviceId } = req.body;
        
        // Find or create coins document for device
        let coinsDoc = await Coins.findOne({ deviceId });
        if (!coinsDoc) {
            coinsDoc = new Coins({ deviceId });
            await coinsDoc.save();
        }

        // Generate new tokens
        const token = jwt.sign(
            { deviceId, userId: coinsDoc._id },
            process.env.JWT_SECRET,
            { expiresIn: '1h' }
        );
        
        const refreshToken = jwt.sign(
            { deviceId, userId: coinsDoc._id },
            process.env.REFRESH_TOKEN_SECRET,
            { expiresIn: '7d' }
        );

        // Update coins document with new tokens
        await coinsDoc.updateAuthTokens(token, refreshToken);

        // Return tokens
        res.json({
            success: true,
            data: {
                token,
                refreshToken,
                expiresIn: 3600,
                coins: coinsDoc.coins,
                isUnlocked: coinsDoc.isUnlocked
            }
        });
    } catch (error) {
        logger.error('Authentication error:', error);
        res.status(500).json({
            success: false,
            message: 'Authentication failed',
            error: 'AUTH_ERROR'
        });
    }
});

// Apply token verification to all routes except auth
router.use((req, res, next) => {
    if (req.path === '/auth') {
        return next();
    }
    verifyTokenWithRefresh(req, res, next);
});

// ... rest of coins routes implementation ...
