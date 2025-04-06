const express = require('express');
const router = express.Router();
const { verifyTokenWithRefresh, verifyApiKey } = require('../middleware/authMiddleware');
const logger = require('../config/logger');
const Coins = require('../models/Coins');
const jwt = require('jsonwebtoken');

// Apply API key verification to all routes
router.use(verifyApiKey);

// Log all incoming requests with headers
router.use((req, res, next) => {
    logger.debug(`Coins route accessed: ${req.method} ${req.path}`, {
        body: req.body,
        headers: {
            'x-api-key': req.header('x-api-key') ? '[PRESENT]' : '[MISSING]',
            'authorization': req.header('authorization') ? '[PRESENT]' : '[MISSING]'
        }
    });
    next();
});

// Device registration endpoint (moved from /auth/register to /coins/register)
router.post('/register', async (req, res) => {
    try {
        const { deviceId, deviceInfo } = req.body;
        const apiKey = req.header('x-api-key');
        
        logger.debug('Registration request:', { 
            deviceId,
            hasDeviceInfo: !!deviceInfo,
            apiKey: apiKey ? '***' + apiKey.slice(-4) : 'none'
        });

        if (!deviceId) {
            return res.status(400).json({
                success: false,
                message: 'Device ID is required',
                error: 'MISSING_DEVICE_ID'
            });
        }

        // Find or create coins document
        let coinsDoc = await Coins.findOne({ deviceId });
        
        if (coinsDoc) {
            logger.debug('Existing device found:', { deviceId });
        } else {
            coinsDoc = new Coins({ 
                deviceId,
                coins: 0,
                auth: {
                    deviceInfo: deviceInfo || {}
                }
            });
            logger.debug('Creating new device record:', { deviceId });
        }

        // Generate tokens
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
        await coinsDoc.updateAuthTokens(token, refreshToken);
        logger.debug('Auth tokens updated:', { deviceId });

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
        logger.error('Registration error:', error);
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

// Add a new endpoint to check auth status
router.get('/auth-status', async (req, res) => {
    try {
        const deviceId = req.header('x-device-id');
        if (!deviceId) {
            return res.status(400).json({
                success: false,
                message: 'Device ID required',
                error: 'MISSING_DEVICE_ID'
            });
        }

        const coinsDoc = await Coins.findOne({ deviceId });
        if (!coinsDoc) {
            return res.status(404).json({
                success: false,
                message: 'Device not found',
                error: 'DEVICE_NOT_FOUND'
            });
        }

        res.json({
            success: true,
            data: {
                isAuthenticated: coinsDoc.auth.isAuthenticated,
                tokenExpiry: coinsDoc.auth.tokenExpiry,
                refreshTokenExpiry: coinsDoc.auth.refreshTokenExpiry
            }
        });
    } catch (error) {
        logger.error('Auth status check error:', error);
        res.status(500).json({
            success: false,
            message: 'Server error checking auth status',
            error: 'AUTH_CHECK_ERROR'
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
