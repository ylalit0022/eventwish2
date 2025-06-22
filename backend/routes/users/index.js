const express = require('express');
const router = express.Router();

// Import all route modules
const profileRoutes = require('./profile');
const templateRoutes = require('./templates');
const activityRoutes = require('./activity');
const analyticsRoutes = require('./analytics');
const subscriptionRoutes = require('./subscription');
const aiRoutes = require('./ai');
const sessionRoutes = require('./sessions');
const notificationRoutes = require('./notifications');
const affinityRoutes = require('./affinity');
const draftsRoutes = require('./drafts');
const adminRoutes = require('./admin');
const referralRoutes = require('./referrals');
const cacheRoutes = require('./cache');
const healthRoutes = require('./health');
const preferencesRoutes = require('./preferences');

// Use route modules
router.use('/', profileRoutes);
router.use('/', templateRoutes);
router.use('/', activityRoutes);
router.use('/', analyticsRoutes);
router.use('/', subscriptionRoutes);
router.use('/', aiRoutes);
router.use('/', sessionRoutes);
router.use('/', notificationRoutes);
router.use('/', affinityRoutes);
router.use('/', draftsRoutes);
router.use('/', adminRoutes);
router.use('/', referralRoutes);
router.use('/', cacheRoutes);
router.use('/', healthRoutes);
router.use('/', preferencesRoutes);

module.exports = router; 