const express = require('express');
const router = express.Router();
const categoryIconController = require('../controllers/categoryIconController');

// Get all category icons
router.get('/', categoryIconController.getAllCategoryIcons);

// Create a new category icon
router.post('/', categoryIconController.createCategoryIcon);

module.exports = router;