const express = require('express');
const router = express.Router();
const segmentController = require('../controllers/segmentController');
const authMiddleware = require('../middleware/authMiddleware');

// All segment routes require admin authentication
router.use(authMiddleware.verifyToken);
router.use(authMiddleware.verifyAdmin);

// Create a new segment
router.post('/', segmentController.createSegment);

// Update an existing segment
router.put('/:id', segmentController.updateSegment);

// Delete a segment
router.delete('/:id', segmentController.deleteSegment);

// Get a segment by ID
router.get('/:id', segmentController.getSegment);

// Get all segments with optional filters
router.get('/', segmentController.getSegments);

// Test if a user context matches a segment
router.post('/:id/test', segmentController.testSegment);

module.exports = router; 