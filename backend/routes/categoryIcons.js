const express = require('express');
const router = express.Router();
const categoryIconController = require('../controllers/categoryIconController');

// Get all category icons
router.get('/', categoryIconController.getAllCategoryIcons);

// Create a new category icon
router.post('/', categoryIconController.createCategoryIcon);

// Duplicate a category icon
router.post('/:id/duplicate', categoryIconController.duplicateCategoryIcon);

module.exports = router;