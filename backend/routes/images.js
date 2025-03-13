const express = require('express');
const router = express.Router();
const { fetchImage } = require('../controllers/imageController');

// Route to fetch an image from a URL
router.get('/fetch', fetchImage);

module.exports = router; 