const express = require('express');
const router = express.Router();
const { 
    getTemplates, 
    getTemplatesByCategory, 
    getTemplateById 
} = require('../controllers/templateController');

// Get all templates with pagination
router.get('/', getTemplates);

// Get templates by category
router.get('/category/:category', getTemplatesByCategory);

// Get template by ID
router.get('/:id', getTemplateById);

module.exports = router;
