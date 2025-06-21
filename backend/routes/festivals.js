const express = require('express');
const router = express.Router();
const { 
    getUpcomingFestivals,
    getFestivalsByCategory,
    duplicateFestival
} = require('../controllers/festivalController');

router.get('/upcoming', getUpcomingFestivals);
router.get('/category/:category', getFestivalsByCategory);

// Duplicate a festival
router.post('/:id/duplicate', duplicateFestival);

module.exports = router; 