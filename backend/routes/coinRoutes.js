const express = require('express');
const router = express.Router();
const coinController = require('../controllers/coinController');

/**
 * @swagger
 * /api/coins/register:
 *   post:
 *     summary: Register or verify a user
 *     tags: [Coins]
 *     requestBody:
 *       required: true
 *       content:
 *         application/json:
 *           schema:
 *             type: object
 *             required:
 *               - deviceId
 *             properties:
 *               deviceId:
 *                 type: string
 *               appVersion:
 *                 type: string
 *     responses:
 *       200:
 *         description: User verified successfully
 */
router.post('/register', coinController.registerUser);

/**
 * @swagger
 * /api/coins/status/{deviceId}:
 *   get:
 *     summary: Get user's coin balance and premium status
 *     tags: [Coins]
 *     parameters:
 *       - in: path
 *         name: deviceId
 *         schema:
 *           type: string
 *         required: true
 *         description: Device ID of the user
 *     responses:
 *       200:
 *         description: User status retrieved successfully
 */
router.get('/status/:deviceId', coinController.getUserStatus);

/**
 * @swagger
 * /api/coins/add-from-ad:
 *   post:
 *     summary: Add coins after watching an ad
 *     tags: [Coins]
 *     requestBody:
 *       required: true
 *       content:
 *         application/json:
 *           schema:
 *             type: object
 *             required:
 *               - deviceId
 *             properties:
 *               deviceId:
 *                 type: string
 *     responses:
 *       200:
 *         description: Coins added successfully
 *       429:
 *         description: Please wait before watching another ad
 */
router.post('/add-from-ad', coinController.addCoinsFromAd);

/**
 * @swagger
 * /api/coins/purchase-premium:
 *   post:
 *     summary: Spend coins to purchase premium
 *     tags: [Coins]
 *     requestBody:
 *       required: true
 *       content:
 *         application/json:
 *           schema:
 *             type: object
 *             required:
 *               - deviceId
 *               - premiumOption
 *             properties:
 *               deviceId:
 *                 type: string
 *               premiumOption:
 *                 type: string
 *                 enum: [day, week, month]
 *     responses:
 *       200:
 *         description: Premium purchased successfully
 *       400:
 *         description: Insufficient coins or invalid request
 */
router.post('/purchase-premium', coinController.purchasePremium);

/**
 * @swagger
 * /api/coins/increment-image-count:
 *   post:
 *     summary: Increment image capture count
 *     tags: [Coins]
 *     requestBody:
 *       required: true
 *       content:
 *         application/json:
 *           schema:
 *             type: object
 *             required:
 *               - deviceId
 *             properties:
 *               deviceId:
 *                 type: string
 *     responses:
 *       200:
 *         description: Image count incremented successfully
 *       403:
 *         description: Daily free image limit reached
 */
router.post('/increment-image-count', coinController.incrementImageCount);

/**
 * @swagger
 * /api/coins/verify-premium/{deviceId}:
 *   get:
 *     summary: Verify premium status
 *     tags: [Coins]
 *     parameters:
 *       - in: path
 *         name: deviceId
 *         schema:
 *           type: string
 *         required: true
 *         description: Device ID of the user
 *     responses:
 *       200:
 *         description: Premium status verified successfully
 */
router.get('/verify-premium/:deviceId', coinController.verifyPremiumStatus);

/**
 * @swagger
 * /api/coins/transaction-history/{deviceId}:
 *   get:
 *     summary: Get transaction history
 *     tags: [Coins]
 *     parameters:
 *       - in: path
 *         name: deviceId
 *         schema:
 *           type: string
 *         required: true
 *         description: Device ID of the user
 *       - in: query
 *         name: limit
 *         schema:
 *           type: integer
 *           default: 20
 *         description: Number of transactions to return
 *       - in: query
 *         name: offset
 *         schema:
 *           type: integer
 *           default: 0
 *         description: Offset for pagination
 *     responses:
 *       200:
 *         description: Transaction history retrieved successfully
 *       404:
 *         description: User not found
 */
router.get('/transaction-history/:deviceId', coinController.getTransactionHistory);

module.exports = router; 