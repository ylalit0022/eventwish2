const express = require('express');
const router = express.Router();
const { verifyTokenWithRefresh } = require('../middleware/authMiddleware');
const logger = require('../config/logger');

// Apply enhanced token verification to all coins routes
router.use(verifyTokenWithRefresh);

// ... rest of coins routes implementation ...
