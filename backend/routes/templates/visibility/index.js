const express = require('express');
const router = express.Router();

// Import sub-routers
const rankingRouter = require('./ranking');
const boostingRouter = require('./boosting');
const blockingRouter = require('./blocking');
const typeRouter = require('./type');

// Use sub-routers with appropriate prefixes
router.use('/ranking', rankingRouter);
router.use('/boosting', boostingRouter);
router.use('/blocking', blockingRouter);
router.use('/type', typeRouter);

module.exports = router; 