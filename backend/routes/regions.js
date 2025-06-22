const express = require('express');
const router = express.Router();
const { 
    getAllRegions,
    getRegionsByContinent,
    getRegionByCode,
    createRegion,
    updateRegion,
    deleteRegion
} = require('../controllers/regionController');
const { verifyFirebaseToken } = require('../middleware/authMiddleware');
const { verifyAdmin } = require('../middleware/authMiddleware');

// Public routes
router.get('/', getAllRegions);
router.get('/continent/:continent', getRegionsByContinent);
router.get('/:code', getRegionByCode);

// Admin routes - protected
router.post('/', verifyFirebaseToken, verifyAdmin(), createRegion);
router.put('/:code', verifyFirebaseToken, verifyAdmin(), updateRegion);
router.delete('/:code', verifyFirebaseToken, verifyAdmin(), deleteRegion);

module.exports = router; 