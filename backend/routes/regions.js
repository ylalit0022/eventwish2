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
const { verifyFirebaseToken } = require('../middlewares/authMiddleware');
const { isAdmin } = require('../middlewares/adminMiddleware');

// Public routes
router.get('/', getAllRegions);
router.get('/continent/:continent', getRegionsByContinent);
router.get('/:code', getRegionByCode);

// Admin routes - protected
router.post('/', verifyFirebaseToken, isAdmin, createRegion);
router.put('/:code', verifyFirebaseToken, isAdmin, updateRegion);
router.delete('/:code', verifyFirebaseToken, isAdmin, deleteRegion);

module.exports = router; 