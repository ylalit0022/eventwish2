const express = require('express');
const router = express.Router();
const User = require('../models/firestore/User');
const Template = require('../models/Template');
const SharedWish = require('../models/SharedWish');
const { AdMob, adTypes } = require('../models/AdMob');
const CategoryIcon = require('../models/CategoryIcon');
const { verifyFirebaseToken, verifyAdmin } = require('../middleware/authMiddleware');
const logger = require('../config/logger');
const multer = require('multer');
const csv = require('csv-parser');
const { Parser } = require('json2csv');
const fs = require('fs');
const path = require('path');
const { Readable } = require('stream');
const Festival = require('../models/Festival');
const About = require('../models/About');
const Contact = require('../models/Contact');
const SponsoredAd = require('../models/SponsoredAd');
const PushNotification = require('../models/PushNotification');
const categoryIconController = require('../controllers/categoryIconController');
const sponsoredAdController = require('../controllers/sponsoredAdController');

// Configure multer for file uploads
const upload = multer({
  storage: multer.memoryStorage(),
  limits: {
    fileSize: 5 * 1024 * 1024, // 5MB limit
  },
  fileFilter: (req, file, cb) => {
    // Accept only CSV files
    if (file.mimetype === 'text/csv' || file.mimetype === 'application/vnd.ms-excel') {
      cb(null, true);
    } else {
      cb(new Error('Only CSV files are allowed'), false);
    }
  }
});

/**
 * @route   GET /api/admin/verify
 * @desc    Verify admin status
 * @access  Public
 */
router.get('/verify', verifyFirebaseToken, async (req, res) => {
  try {
    const { getAdminRole } = require('../config/adminConfig');
    
    // Get user email from the Firebase decoded token
    const userEmail = req.user.email;
    
    // Check if user is an admin
    const adminRole = getAdminRole(userEmail);
    
    if (adminRole) {
      logger.info(`Admin verification successful for ${userEmail}`, { role: adminRole });
      
      // Add admin info to request for use in other middleware
      req.adminInfo = {
        email: userEmail,
        role: adminRole
      };
      
      return res.status(200).json({
        success: true,
        isAdmin: true,
        role: adminRole
      });
    } else {
      logger.warn(`Admin verification failed for ${userEmail}`);
      return res.status(403).json({
        success: false,
        isAdmin: false,
        message: 'User is not authorized for admin access'
      });
    }
  } catch (error) {
    logger.error(`Admin verification error: ${error.message}`, { error });
    return res.status(500).json({
      success: false,
      isAdmin: false,
      message: 'Server error verifying admin status'
    });
  }
});

/**
 * @route   GET /api/admin/users
 * @desc    Get all users with pagination, sorting and filtering
 * @access  Admin only
 */
router.get('/users', verifyFirebaseToken, async (req, res) => {
  try {
    // Verify admin status first
    const { getAdminRole } = require('../config/adminConfig');
    const userEmail = req.user.email;
    const adminRole = getAdminRole(userEmail);
    
    if (!adminRole) {
      return res.status(403).json({
        success: false,
        message: 'User is not authorized for admin access'
      });
    }
    
    // Add admin info to request
    req.adminInfo = {
      email: userEmail,
      role: adminRole
    };
    
    // Pagination parameters
    const page = parseInt(req.query.page) || 1;
    const limit = parseInt(req.query.limit) || 20;
    
    // Sorting parameters
    const sortField = req.query.sort || 'lastOnline';
    const sortOrder = req.query.order === 'asc' ? 'asc' : 'desc';
    
    // Filtering parameters
    const filters = {};
    
    // Filter by blocked status
    if (req.query.blocked === 'true') {
      filters.isBlocked = true;
    } else if (req.query.blocked === 'false') {
      filters.isBlocked = false;
    }
    
    // Filter by search query (on uid, email, displayName)
    if (req.query.q) {
      filters.searchQuery = req.query.q;
    }
    
    // Get paginated users
    const { users, totalUsers, totalPages } = await User.getPaginatedUsers({
      page,
      limit,
      sortField,
      sortOrder,
      filters
    });
    
    logger.info(`Admin user list retrieved by ${req.adminInfo.email}`, {
      page,
      limit,
      totalUsers,
      filters: JSON.stringify(filters)
    });
    
    return res.status(200).json({
      success: true,
      users,
      pagination: {
        total: totalUsers,
        page,
        limit,
        totalPages
      }
    });
  } catch (error) {
    logger.error(`Error retrieving users: ${error.message}`, { error });
    res.status(500).json({
      success: false,
      message: 'Server error retrieving users',
      error: error.message
    });
  }
});

/**
 * @route   GET /api/admin/users/:uid
 * @desc    Get a single user by UID
 * @access  Admin only
 */
router.get('/users/:uid', verifyFirebaseToken, async (req, res) => {
  try {
    // Verify admin status first
    const { getAdminRole } = require('../config/adminConfig');
    const userEmail = req.user.email;
    const adminRole = getAdminRole(userEmail);
    
    if (!adminRole) {
      return res.status(403).json({
        success: false,
        message: 'User is not authorized for admin access'
      });
    }
    
    // Add admin info to request
    req.adminInfo = {
      email: userEmail,
      role: adminRole
    };
    
    const { uid } = req.params;
    
    // Get user by uid
    const user = await User.getByUid(uid);
    if (!user) {
      return res.status(404).json({
        success: false,
        message: 'User not found'
      });
    }
    
    logger.info(`Admin retrieved user details for ${uid}`, {
      admin: req.adminInfo.email
    });
    
    return res.status(200).json({
      success: true,
      user
    });
  } catch (error) {
    logger.error(`Error retrieving user: ${error.message}`, { error });
    res.status(500).json({
      success: false,
      message: 'Server error retrieving user',
      error: error.message
    });
  }
});

/**
 * @route   PUT /api/admin/users/:uid/block
 * @desc    Block/unblock a user
 * @access  Admin only
 */
router.put('/users/:uid/block', verifyFirebaseToken, async (req, res) => {
  try {
    // Verify admin status first
    const { getAdminRole } = require('../config/adminConfig');
    const userEmail = req.user.email;
    const adminRole = getAdminRole(userEmail);
    
    if (!adminRole) {
      return res.status(403).json({
        success: false,
        message: 'User is not authorized for admin access'
      });
    }
    
    const { uid } = req.params;
    const { isBlocked, reason } = req.body;
    
    // Get user by uid
    const user = await User.getByUid(uid);
    if (!user) {
      return res.status(404).json({
        success: false,
        message: 'User not found'
      });
    }
    
    // Block/unblock user
    await User.setBlockStatus(uid, isBlocked, {
      reason,
      blockedBy: userEmail,
      blockedAt: new Date()
    });
    
    logger.info(`Admin ${isBlocked ? 'blocked' : 'unblocked'} user ${uid}`, {
      admin: userEmail,
      reason
    });
    
    return res.status(200).json({
      success: true,
      message: `User ${isBlocked ? 'blocked' : 'unblocked'} successfully`
    });
  } catch (error) {
    logger.error(`Error blocking/unblocking user: ${error.message}`, { error });
    res.status(500).json({
      success: false,
      message: 'Server error blocking/unblocking user',
      error: error.message
    });
  }
});

/**
 * @route   DELETE /api/admin/users/:uid
 * @desc    Delete a user
 * @access  Admin only
 */
router.delete('/users/:uid', verifyFirebaseToken, async (req, res) => {
  try {
    // Verify admin status first
    const { getAdminRole } = require('../config/adminConfig');
    const userEmail = req.user.email;
    const adminRole = getAdminRole(userEmail);
    
    if (!adminRole) {
      return res.status(403).json({
        success: false,
        message: 'User is not authorized for admin access'
      });
    }
    
    const { uid } = req.params;
    
    // Get user by uid
    const user = await User.getByUid(uid);
    if (!user) {
      return res.status(404).json({
        success: false,
        message: 'User not found'
      });
    }
    
    // Delete user
    await User.delete(uid);
    
    logger.info(`Admin deleted user ${uid}`, {
      admin: userEmail
    });
    
    return res.status(200).json({
      success: true,
      message: 'User deleted successfully'
    });
  } catch (error) {
    logger.error(`Error deleting user: ${error.message}`, { error });
    res.status(500).json({
      success: false,
      message: 'Server error deleting user',
      error: error.message
    });
  }
});

/**
 * @route   GET /api/admin/users/:uid/sessions
 * @desc    Get all sessions for a user
 * @access  Admin only
 */
router.get('/users/:uid/sessions', verifyFirebaseToken, async (req, res) => {
  try {
    // Verify admin status first
    const { getAdminRole } = require('../config/adminConfig');
    const userEmail = req.user.email;
    const adminRole = getAdminRole(userEmail);
    
    if (!adminRole) {
      return res.status(403).json({
        success: false,
        message: 'User is not authorized for admin access'
      });
    }
    
    const { uid } = req.params;
    
    // Get user by uid
    const user = await User.getByUid(uid);
    if (!user) {
      return res.status(404).json({
        success: false,
        message: 'User not found'
      });
    }
    
    // Get active sessions
    const sessions = await User.getActiveSessions(uid);
    
    logger.info(`Admin retrieved sessions for user ${uid}`, {
      admin: userEmail,
      sessionCount: sessions.length
    });
    
    return res.status(200).json({
      success: true,
      sessions
    });
  } catch (error) {
    logger.error(`Error retrieving user sessions: ${error.message}`, { error });
    res.status(500).json({
      success: false,
      message: 'Server error retrieving user sessions',
      error: error.message
    });
  }
});

/**
 * @route   DELETE /api/admin/users/:uid/sessions/:deviceId
 * @desc    Delete a specific session for a user
 * @access  Admin only
 */
router.delete('/users/:uid/sessions/:deviceId', verifyFirebaseToken, async (req, res) => {
  try {
    // Verify admin status first
    const { getAdminRole } = require('../config/adminConfig');
    const userEmail = req.user.email;
    const adminRole = getAdminRole(userEmail);
    
    if (!adminRole) {
      return res.status(403).json({
        success: false,
        message: 'User is not authorized for admin access'
      });
    }
    
    const { uid, deviceId } = req.params;
    
    // Get user by uid
    const user = await User.getByUid(uid);
    if (!user) {
      return res.status(404).json({
        success: false,
        message: 'User not found'
      });
    }
    
    // Remove device session
    await User.removeDeviceSession(uid, deviceId);
    
    logger.info(`Admin removed session for user ${uid} (Device: ${deviceId})`, {
      admin: userEmail
    });
    
    return res.status(200).json({
      success: true,
      message: 'Session removed successfully'
    });
  } catch (error) {
    logger.error(`Error removing user session: ${error.message}`, { error });
    res.status(500).json({
      success: false,
      message: 'Server error removing user session',
      error: error.message
    });
  }
});

/**
 * @route   DELETE /api/admin/users/:uid/sessions
 * @desc    Delete all sessions for a user
 * @access  Admin only
 */
router.delete('/users/:uid/sessions', verifyFirebaseToken, async (req, res) => {
  try {
    // Verify admin status first
    const { getAdminRole } = require('../config/adminConfig');
    const userEmail = req.user.email;
    const adminRole = getAdminRole(userEmail);
    
    if (!adminRole) {
      return res.status(403).json({
        success: false,
        message: 'User is not authorized for admin access'
      });
    }
    
    const { uid } = req.params;
    
    // Get user by uid
    const user = await User.getByUid(uid);
    if (!user) {
      return res.status(404).json({
        success: false,
        message: 'User not found'
      });
    }
    
    // Remove all device sessions
    await User.removeAllDeviceSessions(uid);
    
    logger.info(`Admin removed all sessions for user ${uid}`, {
      admin: userEmail
    });
    
    return res.status(200).json({
      success: true,
      message: 'All sessions removed successfully'
    });
  } catch (error) {
    logger.error(`Error removing user sessions: ${error.message}`, { error });
    res.status(500).json({
      success: false,
      message: 'Server error removing user sessions',
      error: error.message
    });
  }
});

module.exports = router; 