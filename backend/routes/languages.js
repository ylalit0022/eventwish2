const express = require('express');
const router = express.Router();
const { 
    getAllLanguages, 
    getLanguageByCode,
    createLanguage,
    updateLanguage,
    deleteLanguage
} = require('../controllers/languageController');
const { verifyFirebaseToken } = require('../middlewares/authMiddleware');
const { isAdmin } = require('../middlewares/adminMiddleware');

// Public routes
router.get('/', getAllLanguages);
router.get('/:code', getLanguageByCode);

// Admin routes - protected
router.post('/', verifyFirebaseToken, isAdmin, createLanguage);
router.put('/:code', verifyFirebaseToken, isAdmin, updateLanguage);
router.delete('/:code', verifyFirebaseToken, isAdmin, deleteLanguage);

module.exports = router; 