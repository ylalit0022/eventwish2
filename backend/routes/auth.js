const express = require('express');
const router = express.Router();
const jwt = require('jsonwebtoken');
const { validateRegistration } = require('../middleware/authMiddleware');
const logger = require('../config/logger');
const authController = require('../controllers/authController');
const User = require('../models/firestore/User');

// Route to generate JWT token
router.post('/token', authController.generateToken);

/**
 * @route POST /api/auth/register-device
 * @desc Register a new device and get tokens
 */
router.post('/register-device', validateRegistration, async (req, res) => {
  try {
    const { deviceId, appSignature, deviceModel, deviceName, appVersion, osVersion } = req.body;

    // Create user object with device info
    const user = {
      deviceId,
      registeredAt: new Date(),
      deviceModel: deviceModel || 'Unknown',
      deviceName: deviceName || 'Unknown Device',
      appVersion: appVersion || '1.0.0',
      osVersion: osVersion || 'Unknown',
      lastOnline: new Date()
    };

    // Create user in Firestore
    const uid = await User.create(user);

    // Add initial device session
    await User.addDeviceSession(uid, {
      deviceId,
      deviceModel: user.deviceModel,
      deviceName: user.deviceName,
      appVersion: user.appVersion,
      osVersion: user.osVersion
    });

    // Generate tokens
    const token = jwt.sign({ user: { ...user, uid } }, process.env.JWT_SECRET, { expiresIn: '1h' });
    const refreshToken = jwt.sign(
      { user: { ...user, uid }, deviceId }, 
      process.env.REFRESH_TOKEN_SECRET, 
      { expiresIn: '7d' }
    );

    logger.info(`New device registered: ${deviceId} (UID: ${uid})`);

    // Return tokens
    return res.json({
      success: true,
      data: {
        token,
        refreshToken,
        expiresIn: 3600, // 1 hour in seconds
        uid
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
    jwt.verify(refreshToken, process.env.REFRESH_TOKEN_SECRET, async (err, decoded) => {
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

      // Get user from Firestore
      const user = await User.getByUid(decoded.user.uid);
      if (!user) {
        return res.status(401).json({
          success: false,
          message: 'User not found',
          error: 'USER_NOT_FOUND'
        });
      }

      // Update device session activity
      await User.updateDeviceSessionActivity(decoded.user.uid, deviceId);

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

      logger.info(`Token refreshed for user: ${decoded.user.uid} (Device: ${deviceId})`);

      return res.json({
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