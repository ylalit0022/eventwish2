const express = require('express');
const router = express.Router();
const abTestController = require('../controllers/abTestController');
const authMiddleware = require('../middleware/authMiddleware');

// Admin routes (protected)
router.post(
  '/',
  authMiddleware.verifyToken,
  authMiddleware.verifyAdmin,
  abTestController.createTest
);

router.put(
  '/:id',
  authMiddleware.verifyToken,
  authMiddleware.verifyAdmin,
  abTestController.updateTest
);

router.delete(
  '/:id',
  authMiddleware.verifyToken,
  authMiddleware.verifyAdmin,
  abTestController.deleteTest
);

router.post(
  '/:id/start',
  authMiddleware.verifyToken,
  authMiddleware.verifyAdmin,
  abTestController.startTest
);

router.post(
  '/:id/stop',
  authMiddleware.verifyToken,
  authMiddleware.verifyAdmin,
  abTestController.stopTest
);

router.get(
  '/',
  authMiddleware.verifyToken,
  authMiddleware.verifyAdmin,
  abTestController.getTests
);

router.get(
  '/:id',
  authMiddleware.verifyToken,
  authMiddleware.verifyAdmin,
  abTestController.getTest
);

router.get(
  '/:id/results',
  authMiddleware.verifyToken,
  authMiddleware.verifyAdmin,
  abTestController.getTestResults
);

// Client routes (protected with API key)
router.get(
  '/ad/:adType',
  authMiddleware.verifyApiKey,
  authMiddleware.verifyAppSignature,
  abTestController.getOptimalAdConfig
);

router.post(
  '/track/:testId/:variantId/:eventType',
  authMiddleware.verifyApiKey,
  authMiddleware.verifyAppSignature,
  abTestController.trackTestEvent
);

module.exports = router; 