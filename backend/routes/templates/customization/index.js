const express = require('express');
const router = express.Router();

// Import sub-routers
const optionsRouter = require('./options');
const permissionsRouter = require('./permissions');

// Use sub-routers with appropriate prefixes
router.use('/options', optionsRouter);
router.use('/permissions', permissionsRouter);

module.exports = router; 