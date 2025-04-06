const express = require('express');
const router = express.Router();
const flashyMessageController = require('../controllers/flashyMessageController');

// Create a new flashy message
router.post('/', flashyMessageController.createFlashyMessage);

// Get all flashy messages
router.get('/', flashyMessageController.getAllFlashyMessages);

// Get active flashy messages
router.get('/active', flashyMessageController.getActiveFlashyMessages);

// Update a flashy message
router.put('/:id', flashyMessageController.updateFlashyMessage);

// Delete a flashy message
router.delete('/:id', flashyMessageController.deleteFlashyMessage);

// Send a flashy message immediately
router.post('/:id/send', flashyMessageController.sendFlashyMessage);

module.exports = router; 