const express = require('express');
const router = express.Router();
const tokenController = require('../controllers/tokenController');

// Register a new device token
router.post('/register', tokenController.registerToken);

// Get all active device tokens
router.get('/active', tokenController.getActiveTokens);

// Delete a device token
router.post('/delete', tokenController.deleteToken);

module.exports = router; 