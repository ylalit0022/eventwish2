const express = require('express');
const router = express.Router();
const authController = require('../controllers/authController');
const authMiddleware = require('../middleware/authMiddleware');

/**
 * @route POST /api/auth/register
 * @desc Register a new user with Firebase phone authentication
 * @access Public
 */
router.post('/register', authController.register);

/**
 * @route POST /api/auth/login
 * @desc Login user with phone number and password
 * @access Public
 */
router.post('/login', authController.login);

/**
 * @route POST /api/auth/refresh-token
 * @desc Refresh JWT token
 * @access Public
 */
router.post('/refresh-token', authController.refreshToken);

/**
 * @route POST /api/auth/logout
 * @desc Logout user
 * @access Private
 */
router.post('/logout', authMiddleware.verifyToken, authController.logout);

/**
 * @route POST /api/auth/change-password
 * @desc Change user password
 * @access Private
 */
router.post('/change-password', authMiddleware.verifyToken, authController.changePassword);

/**
 * @route GET /api/auth/me
 * @desc Get current user info
 * @access Private
 */
router.get('/me', authMiddleware.verifyToken, (req, res) => {
    // User info is already decoded and attached to req.user by verifyToken middleware
    return res.status(200).json({ user: req.user });
});

/**
 * @route POST /auth/send-reset-code
 * @desc Send password reset code
 * @access Public
 */
router.post('/send-reset-code', authController.sendPasswordResetCode);

/**
 * @route POST /auth/reset-password
 * @desc Reset password with verification code
 * @access Public
 */
router.post('/reset-password', authController.resetPassword);

module.exports = router; 