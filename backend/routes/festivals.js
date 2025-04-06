const express = require('express');
const router = express.Router();
const { 
    getUpcomingFestivals,
    getFestivalsByCategory
} = require('../controllers/festivalController');

router.get('/upcoming', getUpcomingFestivals);
router.get('/category/:category', getFestivalsByCategory);

module.exports = router; 