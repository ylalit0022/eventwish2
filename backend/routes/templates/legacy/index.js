const express = require('express');
const router = express.Router();

// Import sub-routers
const compatibilityRouter = require('./compatibility');
const referencesRouter = require('./references');
const experimentsRouter = require('./experiments');

// Use sub-routers with appropriate prefixes
router.use('/compatibility', compatibilityRouter);
router.use('/references', referencesRouter);
router.use('/experiments', experimentsRouter);

module.exports = router; 