const express = require('express');
const router = express.Router();
const { 
    createSharedWish, 
    getSharedWish 
} = require('../controllers/wishController');

// Create shared wish
router.post('/create', createSharedWish);

// Get shared wish by short code
router.get('/:shortCode', getSharedWish);

module.exports = router;
