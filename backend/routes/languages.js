const express = require('express');
const router = express.Router();
const { 
    getAllLanguages, 
    getLanguageByCode,
    createLanguage,
    updateLanguage,
    deleteLanguage
} = require('../controllers/languageController');
const { verifyFirebaseToken } = require('../middleware/authMiddleware');
const { verifyAdmin } = require('../middleware/authMiddleware');

// Public routes
router.get('/', getAllLanguages);
router.get('/:code', getLanguageByCode);

// Admin routes - protected
router.post('/', verifyFirebaseToken, verifyAdmin(), createLanguage);
router.put('/:code', verifyFirebaseToken, verifyAdmin(), updateLanguage);
router.delete('/:code', verifyFirebaseToken, verifyAdmin(), deleteLanguage);

module.exports = router; 