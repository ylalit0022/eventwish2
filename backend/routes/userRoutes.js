const express = require('express');
const router = express.Router();
const userController = require('../controllers/userController');
const userRecommendationController = require('../controllers/userRecommendationController');
const { validateDeviceId, validateCategory } = require('../middleware/validators');

/**
 * @route   POST /api/users/register
 * @desc    Register a new user with device ID
 * @access  Public
 */
router.post('/register', validateDeviceId, userController.registerUser);

/**
 * @route   GET /api/users/:deviceId
 * @desc    Get user data by device ID
 * @access  Public (should be restricted in production)
 */
router.get('/:deviceId', userController.getUserByDeviceId);

/**
 * @route   PUT /api/users/activity
 * @desc    Update user's last online timestamp and optionally record category visit
 * @access  Public
 */
router.put('/activity', validateDeviceId, validateCategory, userController.updateUserActivity);

/**
 * @route   PUT /api/users/template-view
 * @desc    Record a template view with its category
 * @access  Public
 */
router.put('/template-view', validateDeviceId, validateCategory, userController.recordTemplateView);

/**
 * @route   GET /api/users/:deviceId/recommendations
 * @desc    Get personalized template recommendations for a user
 * @access  Public
 */
router.get('/:deviceId/recommendations', userRecommendationController.getUserRecommendations);

module.exports = router; 