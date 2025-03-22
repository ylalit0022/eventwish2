const express = require('express');
const router = express.Router();
const authController = require('../controllers/authController');

// Route to generate JWT token
router.post('/token', authController.generateToken);

module.exports = router;