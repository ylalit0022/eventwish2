const express = require('express');
const router = express.Router();
const jwt = require('jsonwebtoken');
const { validateRegistration } = require('../middleware/authMiddleware');
const logger = require('../config/logger');
const authController = require('../controllers/authController');

// Route to generate JWT token
router.post('/token', authController.generateToken);

/**
 * @route POST /api/auth/register-device
 * @desc Register a new device and get tokens
 */
router.post('/register-device', validateRegistration, async (req, res) => {
  try {
    const { deviceId, appSignature } = req.body;

    // Create user object (can be enhanced with more device info)
    const user = {
      deviceId,
      registeredAt: new Date()
    };

    // Generate tokens
    const token = jwt.sign({ user }, process.env.JWT_SECRET, { expiresIn: '1h' });
    const refreshToken = jwt.sign(
      { user, deviceId }, 
      process.env.REFRESH_TOKEN_SECRET, 
      { expiresIn: '7d' }
    );

    // Return tokens
    res.json({
      success: true,
      data: {
        token,
        refreshToken,
        expiresIn: 3600 // 1 hour in seconds
      }
    });
  } catch (error) {
    logger.error(`Error in device registration: ${error.message}`, { error });
    res.status(500).json({
      success: false,
      message: 'Server error during registration',
      error: 'REGISTRATION_ERROR'
    });
  }
});

/**
 * @route POST /api/auth/refresh-token
 * @desc Refresh access token using refresh token
 */
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

    // Verify refresh token
    jwt.verify(refreshToken, process.env.REFRESH_TOKEN_SECRET, (err, decoded) => {
      if (err) {
        return res.status(401).json({
          success: false,
          message: 'Invalid refresh token',
          error: 'REFRESH_TOKEN_INVALID'
        });
      }

      // Verify device ID matches
      if (decoded.deviceId !== deviceId) {
        return res.status(401).json({
          success: false,
          message: 'Device ID mismatch',
          error: 'DEVICE_ID_MISMATCH'
        });
      }

      // Generate new tokens
      const newToken = jwt.sign(
        { user: decoded.user }, 
        process.env.JWT_SECRET, 
        { expiresIn: '1h' }
      );
      
      const newRefreshToken = jwt.sign(
        { user: decoded.user, deviceId }, 
        process.env.REFRESH_TOKEN_SECRET, 
        { expiresIn: '7d' }
      );

      res.json({
        success: true,
        data: {
          token: newToken,
          refreshToken: newRefreshToken,
          expiresIn: 3600
        }
      });
    });
  } catch (error) {
    logger.error(`Error in token refresh: ${error.message}`, { error });
    res.status(500).json({
      success: false,
      message: 'Server error during token refresh',
      error: 'REFRESH_ERROR'
    });
  }
});

module.exports = router;