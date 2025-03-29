const express = require('express');
const router = express.Router();
const { verifyTokenWithRefresh } = require('../middleware/authMiddleware');
const logger = require('../config/logger');
const Coins = require('../models/Coins');
const jwt = require('jsonwebtoken');

// Device registration and authentication endpoint
router.post('/register', async (req, res) => {
    try {
        const { deviceId, deviceInfo } = req.body;
        
        if (!deviceId) {
            return res.status(400).json({
                success: false,
                message: 'Device ID is required',
                error: 'MISSING_DEVICE_ID'
            });
        }

        let coinsDoc = await Coins.findOne({ deviceId });
        if (!coinsDoc) {
            coinsDoc = new Coins({ 
                deviceId,
                coins: 0,
                auth: {
                    deviceInfo: deviceInfo || {}
                }
            });
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

        // Update auth tokens
        coinsDoc.auth.token = token;
        coinsDoc.auth.refreshToken = refreshToken;
        coinsDoc.auth.tokenExpiry = new Date(Date.now() + 3600000); // 1 hour
        coinsDoc.auth.refreshTokenExpiry = new Date(Date.now() + 604800000); // 7 days
        coinsDoc.auth.isAuthenticated = true;
        
        await coinsDoc.save();

        // Return success response
        res.json({
            success: true,
            data: {
                token,
                refreshToken,
                expiresIn: 3600,
                coins: coinsDoc.coins,
                isUnlocked: coinsDoc.isUnlocked,
                deviceId: coinsDoc.deviceId
            }
        });
    } catch (error) {
        logger.error('Device registration error:', error);
        res.status(500).json({
            success: false,
            message: 'Registration failed',
            error: 'REGISTRATION_ERROR'
        });
    }
});

// Refresh token endpoint
router.post('/refresh-token', async (req, res) => {
    try {
        const { refreshToken, deviceId } = req.body;

        if (!refreshToken || !deviceId) {
            return res.status(400).json({
                success: false,
                message: 'Refresh token and device ID required',
                error: 'MISSING_REQUIRED_FIELDS'
            });
        }

        // Find the coins document
        const coinsDoc = await Coins.findOne({ deviceId });
        if (!coinsDoc) {
            return res.status(404).json({
                success: false,
                message: 'Device not found',
                error: 'DEVICE_NOT_FOUND'
            });
        }

        // Verify stored refresh token matches
        if (coinsDoc.auth.refreshToken !== refreshToken) {
            return res.status(401).json({
                success: false,
                message: 'Invalid refresh token',
                error: 'INVALID_REFRESH_TOKEN'
            });
        }

        // Generate new tokens
        const newToken = jwt.sign(
            { deviceId, userId: coinsDoc._id },
            process.env.JWT_SECRET,
            { expiresIn: '1h' }
        );
        
        const newRefreshToken = jwt.sign(
            { deviceId, userId: coinsDoc._id },
            process.env.REFRESH_TOKEN_SECRET,
            { expiresIn: '7d' }
        );

        // Update tokens in database
        await coinsDoc.updateAuthTokens(newToken, newRefreshToken);

        // Return new tokens
        res.json({
            success: true,
            data: {
                token: newToken,
                refreshToken: newRefreshToken,
                expiresIn: 3600,
                coins: coinsDoc.coins,
                isUnlocked: coinsDoc.isUnlocked
            }
        });
    } catch (error) {
        logger.error('Token refresh error:', error);
        res.status(500).json({
            success: false,
            message: 'Token refresh failed',
            error: 'REFRESH_ERROR'
        });
    }
});

// Apply token verification to all other routes
router.use((req, res, next) => {
    if (req.path === '/register' || req.path === '/refresh-token') {
        return next();
    }
    verifyTokenWithRefresh(req, res, next);
});

// ... rest of coins routes implementation ...
