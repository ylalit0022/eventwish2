const express = require('express');
const router = express.Router();
const coinsController = require('../controllers/coinsController');
const { verifyApiKey } = require('../middleware/authMiddleware');

/**
 * @route   GET /api/coins/plan
 * @desc    Get plan configuration
 * @access  Public
 */
router.get('/plan', coinsController.getPlanConfiguration);

/**
 * @route   GET /api/coins/:deviceId
 * @desc    Get coins for a device
 * @access  Private
 */
router.get('/:deviceId', verifyApiKey, coinsController.getCoins);

/**
 * @route   POST /api/coins/:deviceId
 * @desc    Add coins for a device
 * @access  Private
 */
router.post('/:deviceId', verifyApiKey, coinsController.addCoins);

/**
 * @route   POST /api/coins/:deviceId/unlock
 * @desc    Unlock feature using coins
 * @access  Private
 */
router.post('/:deviceId/unlock', verifyApiKey, coinsController.unlockFeature);

/**
 * @route   POST /api/coins/validate
 * @desc    Validate an unlock signature
 * @access  Private
 */
router.post('/validate', verifyApiKey, coinsController.validateUnlock);

/**
 * @route   POST /api/coins/report
 * @desc    Report an unlock from client to server
 * @access  Private
 */
router.post('/report', verifyApiKey, coinsController.reportUnlock);

/**
 * @route   POST /api/coins/security
 * @desc    Report security violation from client to server
 * @access  Private
 */
router.post('/security', verifyApiKey, (req, res) => {
    // Set securityViolation flag to true
    req.body.securityViolation = true;
    // Forward to reportUnlock which handles security violations
    coinsController.reportUnlock(req, res);
});

/**
 * @route   POST /api/coins/reward
 * @desc    Track ad reward (proxy for addCoins with analytics)
 * @access  Private
 */
router.post('/reward', verifyApiKey, (req, res) => {
    // Extract deviceId from request body
    const { deviceId } = req.body;
    req.params.deviceId = deviceId;
    coinsController.addCoins(req, res);
});

module.exports = router; 