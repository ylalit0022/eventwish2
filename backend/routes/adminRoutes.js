const express = require('express');
const router = express.Router();
const mongoose = require('mongoose');
const User = require('../models/User');
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
    const skip = (page - 1) * limit;
    
    // Sorting parameters
    const sortField = req.query.sort || 'lastOnline';
    const sortOrder = req.query.order === 'asc' ? 1 : -1;
    const sort = { [sortField]: sortOrder };
    
    // Filtering parameters
    const filter = {};
    
    // Filter by blocked status
    if (req.query.blocked === 'true') {
      filter.isBlocked = true;
    } else if (req.query.blocked === 'false') {
      filter.isBlocked = false;
    }
    
    // Filter by search query (on uid, email, displayName)
    if (req.query.q) {
      const searchQuery = req.query.q;
      filter.$or = [
        { uid: { $regex: searchQuery, $options: 'i' } },
        { email: { $regex: searchQuery, $options: 'i' } },
        { displayName: { $regex: searchQuery, $options: 'i' } }
      ];
    }
    
    // Execute query with pagination and filters
    const users = await User.find(filter)
      .sort(sort)
      .skip(skip)
      .limit(limit)
      .select('uid email displayName profilePhoto isBlocked blockInfo created lastOnline preferredTheme preferredLanguage');
    
    // Get total count for pagination
    const totalUsers = await User.countDocuments(filter);
    const totalPages = Math.ceil(totalUsers / limit);
    
    logger.info(`Admin user list retrieved by ${req.adminInfo.email}`, {
      page,
      limit,
      totalUsers,
      filters: JSON.stringify(filter)
    });
    
    res.status(200).json({
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
    
    const user = await User.findOne({ uid });
    
    if (!user) {
      return res.status(404).json({
        success: false,
        message: 'User not found'
      });
    }
    
    logger.info(`Admin viewed user ${uid}`, { 
      admin: req.adminInfo.email,
      role: req.adminInfo.role
    });
    
    res.status(200).json({
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
 * @route   GET /api/admin/users/by-id/:id
 * @desc    Get a single user by MongoDB ObjectId
 * @access  Admin only
 */
router.get('/users/by-id/:id', verifyFirebaseToken, async (req, res) => {
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
    
    const { id } = req.params;
    
    // Validate ID parameter
    if (!id || id === 'undefined' || id === 'null') {
      logger.warn(`Invalid User ID provided: "${id}"`);
      return res.status(400).json({
        success: false,
        message: 'Invalid User ID provided',
        error: 'ID parameter is missing or invalid'
      });
    }

    // Validate that ID is a valid MongoDB ObjectId
    if (!mongoose.Types.ObjectId.isValid(id)) {
      logger.warn(`Invalid MongoDB ObjectId format: "${id}"`);
      return res.status(400).json({
        success: false,
        message: 'Invalid User ID format',
        error: 'ID is not a valid MongoDB ObjectId'
      });
    }
    
    const user = await User.findById(id).select('uid email displayName profilePhoto');
    
    if (!user) {
      return res.status(404).json({
        success: false,
        message: 'User not found'
      });
    }
    
    logger.info(`Admin viewed user by ObjectId ${id}`, { 
      admin: req.adminInfo.email,
      role: req.adminInfo.role
    });
    
    res.status(200).json({
      success: true,
      user
    });
  } catch (error) {
    logger.error(`Error retrieving user by ObjectId: ${error.message}`, { error });
    res.status(500).json({
      success: false,
      message: 'Server error retrieving user',
      error: error.message
    });
  }
});

/**
 * @route   PUT /api/admin/users/:uid
 * @desc    Update user data
 * @access  Admin only
 */
router.put('/users/:uid', verifyFirebaseToken, async (req, res) => {
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
    const updates = req.body;
    
    // Security: remove fields that shouldn't be updated by admin
    delete updates.uid;
    delete updates._id;
    delete updates.__v;
    delete updates.blockInfo; // Use the specific block endpoint instead
    delete updates.isBlocked;  // Use the specific block endpoint instead
    
    const user = await User.findOne({ uid });
    
    if (!user) {
      return res.status(404).json({
        success: false,
        message: 'User not found'
      });
    }
    
    // Update user data
    Object.keys(updates).forEach(key => {
      user[key] = updates[key];
    });
    
    await user.save();
    
    logger.info(`User ${uid} updated by admin ${req.adminInfo.email}`, {
      updatedFields: Object.keys(updates),
      adminRole: req.adminInfo.role
    });
    
    res.status(200).json({
      success: true,
      message: 'User updated successfully',
      user
    });
  } catch (error) {
    logger.error(`Error updating user: ${error.message}`, { error });
    res.status(500).json({
      success: false,
      message: 'Server error updating user',
      error: error.message
    });
  }
});

/**
 * @route   POST /api/admin/users/:uid/block
 * @desc    Block a user
 * @access  Admin only
 */
router.post('/users/:uid/block', verifyFirebaseToken, async (req, res) => {
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
    const { reason, blockExpiresAt, notes } = req.body;
    
    const user = await User.findOne({ uid });
    
    if (!user) {
      return res.status(404).json({
        success: false,
        message: 'User not found'
      });
    }
    
    // Block the user
    await user.blockUser(
      req.user.uid, // Admin UID
      reason || 'Blocked by administrator',
      blockExpiresAt ? new Date(blockExpiresAt) : null,
      notes || ''
    );
    
    logger.info(`User ${uid} blocked by admin ${req.adminInfo.email}`, {
      reason,
      expiresAt: blockExpiresAt,
      adminRole: req.adminInfo.role
    });
    
    res.status(200).json({
      success: true,
      message: 'User blocked successfully',
      user: {
        uid: user.uid,
        email: user.email,
        displayName: user.displayName,
        isBlocked: true,
        blockInfo: user.blockInfo
      }
    });
  } catch (error) {
    logger.error(`Error blocking user: ${error.message}`, { error });
    res.status(500).json({
      success: false,
      message: 'Server error blocking user',
      error: error.message
    });
  }
});

/**
 * @route   POST /api/admin/users/:uid/unblock
 * @desc    Unblock a user
 * @access  Admin only
 */
router.post('/users/:uid/unblock', verifyFirebaseToken, async (req, res) => {
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
    
    const user = await User.findOne({ uid });
    
    if (!user) {
      return res.status(404).json({
        success: false,
        message: 'User not found'
      });
    }
    
    // Unblock the user
    await user.unblockUser();
    
    logger.info(`User ${uid} unblocked by admin ${req.adminInfo.email}`, {
      adminRole: req.adminInfo.role
    });
    
    res.status(200).json({
      success: true,
      message: 'User unblocked successfully',
      user: {
        uid: user.uid,
        email: user.email,
        displayName: user.displayName,
        isBlocked: false
      }
    });
  } catch (error) {
    logger.error(`Error unblocking user: ${error.message}`, { error });
    res.status(500).json({
      success: false,
      message: 'Server error unblocking user',
      error: error.message
    });
  }
});

/**
 * @route   GET /api/admin/users/stats
 * @desc    Get user statistics
 * @access  Admin only
 */
router.get('/users/stats', verifyFirebaseToken, async (req, res) => {
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
    
    // Get count of all users
    const totalUsers = await User.countDocuments({});
    
    // Get count of users created in the last 30 days
    const thirtyDaysAgo = new Date();
    thirtyDaysAgo.setDate(thirtyDaysAgo.getDate() - 30);
    const newUsers = await User.countDocuments({
      created: { $gte: thirtyDaysAgo }
    });
    
    // Get count of active users in the last 7 days
    const sevenDaysAgo = new Date();
    sevenDaysAgo.setDate(sevenDaysAgo.getDate() - 7);
    const activeUsers = await User.countDocuments({
      lastOnline: { $gte: sevenDaysAgo }
    });
    
    // Get count of blocked users
    const blockedUsers = await User.countDocuments({
      isBlocked: true
    });
    
    // Get distribution of users by theme preference
    const themeStats = await User.aggregate([
      { $group: { _id: '$preferredTheme', count: { $sum: 1 } } }
    ]);
    
    // Get distribution of users by language preference
    const languageStats = await User.aggregate([
      { $group: { _id: '$preferredLanguage', count: { $sum: 1 } } }
    ]);
    
    // Format theme stats into an object
    const themeDistribution = {};
    themeStats.forEach(theme => {
      themeDistribution[theme._id || 'undefined'] = theme.count;
    });
    
    // Format language stats into an object
    const languageDistribution = {};
    languageStats.forEach(lang => {
      languageDistribution[lang._id || 'undefined'] = lang.count;
    });
    
    logger.info(`User stats retrieved by admin ${req.adminInfo.email}`, {
      adminRole: req.adminInfo.role
    });
    
    res.status(200).json({
      success: true,
      stats: {
        totalUsers,
        newUsers,
        activeUsers,
        blockedUsers,
        themeDistribution,
        languageDistribution
      }
    });
  } catch (error) {
    logger.error(`Error retrieving user stats: ${error.message}`, { error });
    res.status(500).json({
      success: false,
      message: 'Server error retrieving user statistics',
      error: error.message
    });
  }
});

/**
 * @route   POST /api/admin/users/bulk-unblock
 * @desc    Unblock multiple users
 * @access  Admin only
 */
router.post('/users/bulk-unblock', verifyFirebaseToken, async (req, res) => {
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
    
    const { uids } = req.body;
    
    if (!uids || !Array.isArray(uids) || uids.length === 0) {
      return res.status(400).json({
        success: false,
        message: 'No user UIDs provided'
      });
    }
    
    // Find all users that match the UIDs
    const users = await User.find({ uid: { $in: uids }, isBlocked: true });
    
    if (users.length === 0) {
      return res.status(404).json({
        success: false,
        message: 'No matching blocked users found'
      });
    }
    
    // Unblock each user
    const unblockPromises = users.map(user => user.unblockUser());
    
    await Promise.all(unblockPromises);
    
    logger.info(`${users.length} users unblocked in bulk by admin ${req.adminInfo.email}`, {
      adminRole: req.adminInfo.role,
      userCount: users.length
    });
    
    res.status(200).json({
      success: true,
      message: `${users.length} users unblocked successfully`,
      unblockedCount: users.length
    });
  } catch (error) {
    logger.error(`Error in bulk unblock: ${error.message}`, { error });
    res.status(500).json({
      success: false,
      message: 'Server error during bulk unblock operation',
      error: error.message
    });
  }
});

/**
 * @route   GET /api/admin/dashboard/stats
 * @desc    Get dashboard statistics
 * @access  Admin only
 */
router.get('/dashboard/stats', verifyFirebaseToken, async (req, res) => {
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
    
    // Get user statistics
    const totalUsers = await User.countDocuments();
    const activeUsers = await User.countDocuments({ isBlocked: { $ne: true } });
    const blockedUsers = await User.countDocuments({ isBlocked: true });
    
    // Get new users today
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const newUsersToday = await User.countDocuments({ created: { $gte: today } });
    
    // Get users by platform
    const usersByPlatform = await User.aggregate([
      {
        $group: {
          _id: '$platform',
          count: { $sum: 1 }
        }
      }
    ]);
    
    // Format platform data
    const platformData = {
      android: 0,
      ios: 0
    };
    
    usersByPlatform.forEach(platform => {
      if (platform._id === 'android') platformData.android = platform.count;
      if (platform._id === 'ios') platformData.ios = platform.count;
    });
    
    // Get users by country
    const usersByCountry = await User.aggregate([
      {
        $group: {
          _id: '$country',
          count: { $sum: 1 }
        }
      },
      {
        $sort: { count: -1 }
      },
      {
        $limit: 5
      }
    ]);
    
    // Format country data
    const countryData = {};
    let otherCount = 0;
    
    usersByCountry.forEach((country, index) => {
      if (index < 4 && country._id) {
        countryData[country._id] = country.count;
      } else {
        otherCount += country.count;
      }
    });
    
    if (otherCount > 0) {
      countryData['Other'] = otherCount;
    }
    
    // Get user activity for last 7 days
    const last7Days = [];
    const dayLabels = [];
    
    for (let i = 6; i >= 0; i--) {
      const date = new Date();
      date.setDate(date.getDate() - i);
      date.setHours(0, 0, 0, 0);
      
      const nextDate = new Date(date);
      nextDate.setDate(nextDate.getDate() + 1);
      
      const count = await User.countDocuments({
        lastOnline: {
          $gte: date,
          $lt: nextDate
        }
      });
      
      last7Days.push(count);
      dayLabels.push(date.toLocaleDateString('en-US', { weekday: 'short' }));
    }
    
    logger.info(`Admin dashboard stats retrieved by ${req.adminInfo.email}`);
    
    res.status(200).json({
      success: true,
      stats: {
        totalUsers,
        activeUsers,
        blockedUsers,
        newUsersToday,
        usersByPlatform: platformData,
        usersByCountry: countryData,
        userActivityLast7Days: {
          labels: dayLabels,
          data: last7Days
        }
      }
    });
  } catch (error) {
    logger.error(`Error retrieving dashboard stats: ${error.message}`, { error });
    res.status(500).json({
      success: false,
      message: 'Server error retrieving dashboard statistics',
      error: error.message
    });
  }
});

/**
 * @route   GET /api/admin/templates
 * @desc    Get all templates with pagination, sorting and filtering
 * @access  Admin only
 */
router.get('/templates', verifyFirebaseToken, async (req, res) => {
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
    const limit = parseInt(req.query.limit) || 10;
    const skip = (page - 1) * limit;
    
    // Sorting parameters
    const sortField = req.query.sort || 'createdAt';
    const sortOrder = req.query.order === 'asc' ? 1 : -1;
    const sort = { [sortField]: sortOrder };
    
    // Filtering parameters
    const filter = {};
    
    // Filter by category
    if (req.query.category) {
      filter.category = req.query.category;
    }
    
    // Filter by premium status
    if (req.query.isPremium === 'true' || req.query.isPremium === true) {
      filter.isPremium = true;
    }
    
    // Filter by search query (on title, category, festivalTag)
    if (req.query.q) {
      const searchQuery = req.query.q;
      filter.$or = [
        { title: { $regex: searchQuery, $options: 'i' } },
        { category: { $regex: searchQuery, $options: 'i' } },
        { festivalTag: { $regex: searchQuery, $options: 'i' } }
      ];
    }
    
    // Execute query with pagination and filters
    const templates = await Template.find(filter)
      .sort(sort)
      .skip(skip)
      .limit(limit);
    
    // Get total count for pagination
    const totalItems = await Template.countDocuments(filter);
    
    // Get categories count
    const categories = await Template.aggregate([
      { $group: { _id: '$category', count: { $sum: 1 } } }
    ]);

    const categoriesObj = categories.reduce((acc, curr) => {
      acc[curr._id] = curr.count;
      return acc;
    }, {});
    
    logger.info(`Admin template list retrieved by ${req.adminInfo.email}`, {
      page,
      limit,
      totalItems,
      filters: JSON.stringify(filter)
    });
    
    res.status(200).json({
      success: true,
      data: templates,
      totalItems,
      page,
      limit,
      categories: categoriesObj
    });
  } catch (error) {
    logger.error(`Error retrieving templates: ${error.message}`, { error });
    res.status(500).json({
      success: false,
      message: 'Server error retrieving templates',
      error: error.message
    });
  }
});

/**
 * @route   GET /api/admin/templates/:id
 * @desc    Get a single template by ID
 * @access  Admin only
 */
router.get('/templates/:id', verifyFirebaseToken, async (req, res) => {
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
    
    const { id } = req.params;
    
    const template = await Template.findById(id)
      .populate('language')
      .populate('region');
    
    if (!template) {
      return res.status(404).json({
        success: false,
        message: 'Template not found'
      });
    }
    
    logger.info(`Admin viewed template ${id}`, { 
      admin: req.adminInfo.email,
      role: req.adminInfo.role
    });
    
    res.status(200).json({
      success: true,
      template
    });
  } catch (error) {
    logger.error(`Error retrieving template: ${error.message}`, { error });
    res.status(500).json({
      success: false,
      message: 'Server error retrieving template',
      error: error.message
    });
  }
});

/**
 * @route   POST /api/admin/templates
 * @desc    Create a new template
 * @access  Admin only
 */
router.post('/templates', verifyFirebaseToken, async (req, res) => {
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
    
    // Create new template
    const template = new Template(req.body);
    
    // Save template
    await template.save();
    
    logger.info(`Admin created template ${template._id}`, { 
      admin: req.adminInfo.email,
      role: req.adminInfo.role,
      templateId: template._id
    });
    
    res.status(201).json({
      success: true,
      message: 'Template created successfully',
      template
    });
  } catch (error) {
    logger.error(`Error creating template: ${error.message}`, { error });
    res.status(500).json({
      success: false,
      message: 'Server error creating template',
      error: error.message
    });
  }
});

/**
 * @route   PUT /api/admin/templates/:id
 * @desc    Update a template
 * @access  Admin only
 */
router.put('/templates/:id', verifyFirebaseToken, async (req, res) => {
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
    
    const { id } = req.params;
    
    // Validate and sanitize the data
    const templateData = { ...req.body };
    
    // Handle ObjectId references - remove null/empty values instead of converting to empty strings
    if (templateData.language === null || templateData.language === '') {
      console.log('Removing null/empty language field');
      delete templateData.language;
    } else if (templateData.language !== undefined && typeof templateData.language !== 'string') {
      console.log('Converting language field from', typeof templateData.language, 'to string:', templateData.language);
      templateData.language = String(templateData.language);
    }
    
    if (templateData.region === null || templateData.region === '') {
      console.log('Removing null/empty region field');
      delete templateData.region;
    } else if (templateData.region !== undefined && typeof templateData.region !== 'string') {
      console.log('Converting region field from', typeof templateData.region, 'to string:', templateData.region);
      templateData.region = String(templateData.region);
    }
    
    if (templateData.variationOf === null || templateData.variationOf === '') {
      console.log('Removing null/empty variationOf field');
      delete templateData.variationOf;
    } else if (templateData.variationOf !== undefined && typeof templateData.variationOf !== 'string') {
      console.log('Converting variationOf field from', typeof templateData.variationOf, 'to string:', templateData.variationOf);
      templateData.variationOf = String(templateData.variationOf);
    }
    
    // Ensure relatedTemplates are strings and remove null/empty values
    if (templateData.relatedTemplates && Array.isArray(templateData.relatedTemplates)) {
      templateData.relatedTemplates = templateData.relatedTemplates
        .filter(id => id !== null && id !== undefined && id !== '')
        .map(id => typeof id === 'string' ? id : String(id));
      
      console.log('Filtered and processed relatedTemplates:', templateData.relatedTemplates);
    }
    
    // Log the data being sent to MongoDB for debugging
    console.log(`Template update data for ${id}:`, { 
      language: templateData.language,
      languageType: typeof templateData.language,
      region: templateData.region,
      regionType: typeof templateData.region,
      variationOf: templateData.variationOf,
      variationOfType: typeof templateData.variationOf
    });
    
    try {
      // Find and update template
      const template = await Template.findByIdAndUpdate(
        id,
        templateData,
        { new: true, runValidators: true }
      );
      
      if (!template) {
        return res.status(404).json({
          success: false,
          message: 'Template not found'
        });
      }
      
      logger.info(`Admin updated template ${id}`, { 
        admin: req.adminInfo.email,
        role: req.adminInfo.role
      });
      
      res.status(200).json({
        success: true,
        message: 'Template updated successfully',
        template
      });
    } catch (updateError) {
      console.error('MongoDB update error details:', updateError);
      
      // Check for specific MongoDB error types
      if (updateError.name === 'CastError') {
        console.error('Cast error details:', {
          kind: updateError.kind,
          path: updateError.path,
          value: updateError.value,
          reason: updateError.reason
        });
        return res.status(400).json({
          success: false,
          message: `Invalid data format for field: ${updateError.path}`,
          error: updateError.message
        });
      }
      
      if (updateError.name === 'ValidationError') {
        const validationErrors = Object.keys(updateError.errors).reduce((acc, key) => {
          acc[key] = updateError.errors[key].message;
          return acc;
        }, {});
        
        console.error('Validation error details:', validationErrors);
        
        return res.status(400).json({
          success: false,
          message: 'Validation error',
          errors: validationErrors
        });
      }
      
      // Re-throw to be caught by the outer catch
      throw updateError;
    }
  } catch (error) {
    logger.error(`Error updating template: ${error.message}`, { error });
    console.error('Full error object:', error);
    
    // Try to extract more specific error details
    let errorMessage = error.message;
    if (error.codeName) {
      errorMessage = `${error.codeName}: ${errorMessage}`;
    }
    
    res.status(500).json({
      success: false,
      message: 'Server error updating template',
      error: errorMessage
    });
  }
});

/**
 * @route   DELETE /api/admin/templates/:id
 * @desc    Delete a template
 * @access  Admin only
 */
router.delete('/templates/:id', verifyFirebaseToken, async (req, res) => {
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
    
    const { id } = req.params;
    
    // Find and delete template
    const template = await Template.findByIdAndDelete(id);
    
    if (!template) {
      return res.status(404).json({
        success: false,
        message: 'Template not found'
      });
    }
    
    logger.info(`Admin deleted template ${id}`, { 
      admin: req.adminInfo.email,
      role: req.adminInfo.role
    });
    
    res.status(200).json({
      success: true,
      message: 'Template deleted successfully'
    });
  } catch (error) {
    logger.error(`Error deleting template: ${error.message}`, { error });
    res.status(500).json({
      success: false,
      message: 'Server error deleting template',
      error: error.message
    });
  }
});

/**
 * @route   PATCH /api/admin/templates/:id/toggle-status
 * @desc    Toggle template status
 * @access  Admin only
 */
router.patch('/templates/:id/toggle-status', verifyFirebaseToken, async (req, res) => {
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
    
    const { id } = req.params;
    
    // Find template
    const template = await Template.findById(id);
    
    if (!template) {
      return res.status(404).json({
        success: false,
        message: 'Template not found'
      });
    }
    
    // Toggle status
    template.status = !template.status;
    await template.save();
    
    logger.info(`Admin toggled template status ${id} to ${template.status}`, { 
      admin: req.adminInfo.email,
      role: req.adminInfo.role
    });
    
    res.status(200).json({
      success: true,
      message: `Template status toggled to ${template.status ? 'active' : 'inactive'}`,
      template
    });
  } catch (error) {
    logger.error(`Error toggling template status: ${error.message}`, { error });
    res.status(500).json({
      success: false,
      message: 'Server error toggling template status',
      error: error.message
    });
  }
});

/**
 * @route   GET /api/admin/templates/export-csv
 * @desc    Export templates as CSV
 * @access  Admin only
 */
router.get('/templates/export-csv', verifyFirebaseToken, async (req, res) => {
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
    
    // Filtering parameters
    const filter = {};
    
    // Filter by category
    if (req.query.category) {
      filter.category = req.query.category;
    }
    
    // Filter by premium status
    if (req.query.isPremium === 'true' || req.query.isPremium === true) {
      filter.isPremium = true;
    }
    
    // Filter by search query (on title, category, festivalTag)
    if (req.query.q) {
      const searchQuery = req.query.q;
      filter.$or = [
        { title: { $regex: searchQuery, $options: 'i' } },
        { category: { $regex: searchQuery, $options: 'i' } },
        { festivalTag: { $regex: searchQuery, $options: 'i' } }
      ];
    }
    
    logger.info('Fetching templates for CSV export', { filter });
    
    // Get templates with specific field selection to avoid large data
    const templates = await Template.find(filter)
      .select('title category previewUrl status isPremium festivalTag tags categoryIcon usageCount likes favorites')
      .lean();
    
    logger.info(`Found ${templates.length} templates for CSV export`);
    
    // Create CSV content manually instead of using json2csv to avoid serialization issues
    const fields = ['title', 'category', 'previewUrl', 'status', 'isPremium', 'festivalTag', 'tags', 'categoryIcon', 'usageCount', 'likes', 'favorites'];
    let csvContent = fields.join(',') + '\n';
    
    templates.forEach(template => {
      const row = fields.map(field => {
        let value = template[field];
        
        // Handle special cases
        if (field === 'status') {
          value = template.status ? 'Active' : 'Inactive';
        } else if (field === 'isPremium') {
          value = template.isPremium ? 'Yes' : 'No';
        } else if (field === 'tags' && Array.isArray(value)) {
          value = value.join(', ');
        }
        
        // Handle null/undefined values
        if (value === undefined || value === null) {
          return '';
        }
        
        // Escape quotes and wrap strings in quotes
        if (typeof value === 'string') {
          return `"${value.replace(/"/g, '""')}"`;
        }
        
        return value;
      }).join(',');
      
      csvContent += row + '\n';
    });
    
    logger.info(`CSV generated successfully, size: ${csvContent.length} bytes`);
    
    // Set headers for file download
    res.setHeader('Content-Type', 'text/csv');
    res.setHeader('Content-Disposition', `attachment; filename=templates_${Date.now()}.csv`);
    
    logger.info(`Admin exported templates CSV`, { 
      admin: req.adminInfo.email,
      role: req.adminInfo.role,
      count: templates.length
    });
    
    return res.send(csvContent);
  } catch (error) {
    logger.error(`Error exporting templates CSV: ${error.message}`, { 
      error: error,
      stack: error.stack
    });
    return res.status(500).json({
      success: false,
      message: 'Server error exporting templates CSV',
      error: error.message
    });
  }
});

/**
 * @route   POST /api/admin/templates/import-csv
 * @desc    Import templates from CSV
 * @access  Admin only
 */
router.post('/templates/import-csv', verifyFirebaseToken, upload.single('file'), async (req, res) => {
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
    
    // Check if file was uploaded
    if (!req.file) {
      return res.status(400).json({
        success: false,
        message: 'No file uploaded'
      });
    }
    
    // Process CSV file
    const results = [];
    const errors = [];
    let created = 0;
    let updated = 0;
    
    // Create readable stream from buffer
    const stream = Readable.from(req.file.buffer.toString());
    
    // Process CSV stream
    await new Promise((resolve, reject) => {
      stream
        .pipe(csv())
        .on('data', (data) => results.push(data))
        .on('error', (error) => reject(error))
        .on('end', () => resolve());
    });
    
    // Process each row
    for (const row of results) {
      try {
        // Process tags (convert from comma-separated string to array)
        if (row.tags) {
          row.tags = row.tags.split(',').map(tag => tag.trim()).filter(tag => tag);
        } else {
          row.tags = [];
        }
        
        // Convert string boolean values to actual booleans
        if (row.status === 'true' || row.status === 'false') {
          row.status = row.status === 'true';
        }
        
        if (row.isPremium === 'true' || row.isPremium === 'false') {
          row.isPremium = row.isPremium === 'true';
        }
        
        // Convert numeric string values to numbers
        if (row.usageCount) row.usageCount = parseInt(row.usageCount) || 0;
        if (row.likes) row.likes = parseInt(row.likes) || 0;
        if (row.favorites) row.favorites = parseInt(row.favorites) || 0;
        
        // Check if template with this title already exists
        const existingTemplate = await Template.findOne({ title: row.title });
        
        if (existingTemplate) {
          // Update existing template
          Object.assign(existingTemplate, row);
          await existingTemplate.save();
          updated++;
        } else {
          // Create new template
          await Template.create(row);
          created++;
        }
      } catch (error) {
        errors.push({
          row,
          error: error.message
        });
        logger.error(`Error processing CSV row: ${error.message}`, { 
          error,
          row: JSON.stringify(row)
        });
      }
    }
    
    logger.info(`Admin imported templates CSV`, { 
      admin: req.adminInfo.email,
      role: req.adminInfo.role,
      total: results.length,
      created,
      updated,
      errors: errors.length
    });
    
    res.status(200).json({
      success: true,
      message: 'Templates imported successfully',
      total: results.length,
      created,
      updated,
      errors: errors.length
    });
  } catch (error) {
    logger.error(`Error importing templates CSV: ${error.message}`, { error });
    res.status(500).json({
      success: false,
      message: 'Server error importing templates CSV',
      error: error.message
    });
  }
});

/**
 * @route   GET /api/admin/admob
 * @desc    Get all AdMob ads with pagination, sorting and filtering
 * @access  Admin only
 */
router.get('/admob', verifyFirebaseToken, async (req, res) => {
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
    const skip = (page - 1) * limit;
    
    // Sorting parameters
    const sortField = req.query.sort || 'createdAt';
    const sortOrder = req.query.order === 'asc' ? 1 : -1;
    const sort = { [sortField]: sortOrder };
    
    // Filtering parameters
    const filter = {};
    
    // Filter by search query (on adName, adUnitCode)
    if (req.query.q) {
      const searchQuery = req.query.q;
      filter.$or = [
        { adName: { $regex: searchQuery, $options: 'i' } },
        { adUnitCode: { $regex: searchQuery, $options: 'i' } }
      ];
    }
    
    // Filter by ad type
    if (req.query.adType) {
      filter.adType = req.query.adType;
    }
    
    // Execute query with pagination and filters
    const ads = await AdMob.find(filter)
      .sort(sort)
      .skip(skip)
      .limit(limit)
      .select('adName adType adUnitCode status impressions clicks ctr revenue');
    
    // Get total count for pagination
    const totalAds = await AdMob.countDocuments(filter);
    const totalPages = Math.ceil(totalAds / limit);
    
    logger.info(`Admin AdMob list retrieved by ${req.adminInfo.email}`, {
      page,
      limit,
      totalAds,
      filters: JSON.stringify(filter)
    });
    
    res.status(200).json({
      success: true,
      data: ads,
      totalItems: totalAds,
      page,
      limit,
      totalPages,
      adTypes
    });
  } catch (error) {
    logger.error(`Error retrieving AdMob ads: ${error.message}`, { error });
    res.status(500).json({
      success: false,
      message: 'Server error retrieving AdMob ads',
      error: error.message
    });
  }
});

/**
 * @route   GET /api/admin/admob/:id
 * @desc    Get a single AdMob ad by ID
 * @access  Admin only
 */
router.get('/admob/:id', verifyFirebaseToken, async (req, res) => {
  try {
    logger.info(`GET /admob/:id route called with ID: "${req.params.id}"`);
    
    // Verify admin status first
    const { getAdminRole } = require('../config/adminConfig');
    const userEmail = req.user.email;
    const adminRole = getAdminRole(userEmail);
    
    if (!adminRole) {
      logger.warn(`Unauthorized access attempt to /admob/:id by ${userEmail}`);
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
    
    const { id } = req.params;
    logger.info(`Extracted ID parameter: "${id}", type: ${typeof id}`);
    
    // Validate ID parameter
    if (!id || id === 'undefined' || id === 'null') {
      logger.warn(`Invalid AdMob ID provided: "${id}"`);
      return res.status(400).json({
        success: false,
        message: 'Invalid AdMob ID provided',
        error: 'ID parameter is missing or invalid'
      });
    }

    // Validate that ID is a valid MongoDB ObjectId
    if (!mongoose.Types.ObjectId.isValid(id)) {
      logger.warn(`Invalid MongoDB ObjectId format: "${id}"`);
      return res.status(400).json({
        success: false,
        message: 'Invalid AdMob ID format',
        error: 'ID is not a valid MongoDB ObjectId'
      });
    }
    
    logger.info(`Looking up AdMob with ID: ${id}`);
    const ad = await AdMob.findById(id);
    
    if (!ad) {
      logger.warn(`AdMob ad not found with ID: ${id}`);
      return res.status(404).json({
        success: false,
        message: 'AdMob ad not found'
      });
    }
    
    logger.info(`Admin retrieved AdMob ad ${id}`, { 
      admin: req.adminInfo.email,
      role: req.adminInfo.role
    });
    
    res.status(200).json({
      success: true,
      data: ad,
      adTypes
    });
  } catch (error) {
    logger.error(`Error retrieving AdMob ad: ${error.message}`, { error });
    res.status(500).json({
      success: false,
      message: 'Server error retrieving AdMob ad',
      error: error.message
    });
  }
});

/**
 * @route   POST /api/admin/admob
 * @desc    Create a new AdMob ad
 * @access  Admin only
 */
router.post('/admob', verifyFirebaseToken, async (req, res) => {
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
    
    // Check if ad unit code already exists before creating
    const { adUnitCode } = req.body;
    if (adUnitCode) {
      const existingAd = await AdMob.findOne({ adUnitCode }).collation({ locale: 'en', strength: 2 });
      if (existingAd) {
        return res.status(400).json({
          success: false,
          message: 'Validation error',
          error: 'Ad unit code already exists'
        });
      }
    }
    
    // Create new AdMob ad
    const newAd = new AdMob(req.body);
    
    // Save to database
    await newAd.save();
    
    logger.info(`Admin created new AdMob ad: ${newAd._id}`, { 
      admin: req.adminInfo.email,
      role: req.adminInfo.role,
      adName: newAd.adName,
      adType: newAd.adType
    });
    
    res.status(201).json({
      success: true,
      message: 'AdMob ad created successfully',
      data: newAd
    });
  } catch (error) {
    logger.error(`Error creating AdMob ad: ${error.message}`, { error });
    
    // Handle validation errors
    if (error.name === 'ValidationError') {
      return res.status(400).json({
        success: false,
        message: 'Validation error',
        error: Object.values(error.errors).map(err => err.message).join(', ')
      });
    }
    
    // Handle duplicate key errors
    if (error.message === 'Ad unit code already exists' || 
        (error.name === 'MongoServerError' && error.code === 11000)) {
      return res.status(400).json({
        success: false,
        message: 'Validation error',
        error: 'Ad unit code already exists'
      });
    }
    
    res.status(500).json({
      success: false,
      message: 'Server error creating AdMob ad',
      error: error.message
    });
  }
});

/**
 * @route   PUT /api/admin/admob/:id
 * @desc    Update an AdMob ad
 * @access  Admin only
 */
router.put('/admob/:id', verifyFirebaseToken, async (req, res) => {
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
    
    const { id } = req.params;
    
    // Validate ID parameter
    if (!id || id === 'undefined' || id === 'null') {
      return res.status(400).json({
        success: false,
        message: 'Invalid AdMob ID provided',
        error: 'ID parameter is missing or invalid'
      });
    }

    // Validate that ID is a valid MongoDB ObjectId
    if (!mongoose.Types.ObjectId.isValid(id)) {
      return res.status(400).json({
        success: false,
        message: 'Invalid AdMob ID format',
        error: 'ID is not a valid MongoDB ObjectId'
      });
    }
    
    // Find and update AdMob ad
    const ad = await AdMob.findByIdAndUpdate(
      id, 
      req.body, 
      { new: true, runValidators: true }
    );
    
    if (!ad) {
      return res.status(404).json({
        success: false,
        message: 'AdMob ad not found'
      });
    }
    
    logger.info(`Admin updated AdMob ad ${id}`, { 
      admin: req.adminInfo.email,
      role: req.adminInfo.role
    });
    
    res.status(200).json({
      success: true,
      message: 'AdMob ad updated successfully',
      data: ad
    });
  } catch (error) {
    logger.error(`Error updating AdMob ad: ${error.message}`, { error });
    
    // Handle validation errors
    if (error.name === 'ValidationError') {
      return res.status(400).json({
        success: false,
        message: 'Validation error',
        error: Object.values(error.errors).map(err => err.message).join(', ')
      });
    }
    
    res.status(500).json({
      success: false,
      message: 'Server error updating AdMob ad',
      error: error.message
    });
  }
});

/**
 * @route   DELETE /api/admin/admob/:id
 * @desc    Delete an AdMob ad
 * @access  Admin only
 */
router.delete('/admob/:id', verifyFirebaseToken, async (req, res) => {
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
    
    const { id } = req.params;
    
    // Validate ID parameter
    if (!id || id === 'undefined' || id === 'null') {
      return res.status(400).json({
        success: false,
        message: 'Invalid AdMob ID provided',
        error: 'ID parameter is missing or invalid'
      });
    }

    // Validate that ID is a valid MongoDB ObjectId
    if (!mongoose.Types.ObjectId.isValid(id)) {
      return res.status(400).json({
        success: false,
        message: 'Invalid AdMob ID format',
        error: 'ID is not a valid MongoDB ObjectId'
      });
    }
    
    // Find and delete AdMob ad
    const ad = await AdMob.findByIdAndDelete(id);
    
    if (!ad) {
      return res.status(404).json({
        success: false,
        message: 'AdMob ad not found'
      });
    }
    
    logger.info(`Admin deleted AdMob ad ${id}`, { 
      admin: req.adminInfo.email,
      role: req.adminInfo.role,
      adName: ad.adName
    });
    
    res.status(200).json({
      success: true,
      message: 'AdMob ad deleted successfully'
    });
  } catch (error) {
    logger.error(`Error deleting AdMob ad: ${error.message}`, { error });
    res.status(500).json({
      success: false,
      message: 'Server error deleting AdMob ad',
      error: error.message
    });
  }
});

/**
 * @route   PATCH /api/admin/admob/:id/toggle-status
 * @desc    Toggle AdMob ad status
 * @access  Admin only
 */
router.patch('/admob/:id/toggle-status', verifyFirebaseToken, async (req, res) => {
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
    
    const { id } = req.params;
    
    // Validate ID parameter
    if (!id || id === 'undefined' || id === 'null') {
      return res.status(400).json({
        success: false,
        message: 'Invalid AdMob ID provided',
        error: 'ID parameter is missing or invalid'
      });
    }

    // Validate that ID is a valid MongoDB ObjectId
    if (!mongoose.Types.ObjectId.isValid(id)) {
      return res.status(400).json({
        success: false,
        message: 'Invalid AdMob ID format',
        error: 'ID is not a valid MongoDB ObjectId'
      });
    }
    
    // Find AdMob ad
    const ad = await AdMob.findById(id);
    
    if (!ad) {
      return res.status(404).json({
        success: false,
        message: 'AdMob ad not found'
      });
    }
    
    // Toggle status
    ad.status = !ad.status;
    await ad.save();
    
    logger.info(`Admin toggled AdMob ad status ${id} to ${ad.status}`, { 
      admin: req.adminInfo.email,
      role: req.adminInfo.role
    });
    
    res.status(200).json({
      success: true,
      message: `AdMob ad status toggled to ${ad.status ? 'active' : 'inactive'}`,
      adMob: ad
    });
  } catch (error) {
    logger.error(`Error toggling AdMob ad status: ${error.message}`, { error });
    res.status(500).json({
      success: false,
      message: 'Server error toggling AdMob ad status',
      error: error.message
    });
  }
});

/**
 * @route   GET /api/admin/shared-wishes
 * @desc    Get all shared wishes with pagination, sorting and filtering
 * @access  Admin only
 */
router.get('/shared-wishes', verifyFirebaseToken, async (req, res) => {
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
    const limit = parseInt(req.query.limit) || 10;
    const skip = (page - 1) * limit;
    
    // Sorting parameters
    const sortField = req.query.sort || 'createdAt';
    const sortOrder = req.query.order === 'asc' ? 1 : -1;
    const sort = { [sortField]: sortOrder };
    
    // Filtering parameters
    const filter = {};
    
    // Filter by search query (on shortCode, title, recipientName, senderName)
    if (req.query.q) {
      const searchQuery = req.query.q;
      filter.$or = [
        { shortCode: { $regex: searchQuery, $options: 'i' } },
        { title: { $regex: searchQuery, $options: 'i' } },
        { recipientName: { $regex: searchQuery, $options: 'i' } },
        { senderName: { $regex: searchQuery, $options: 'i' } }
      ];
    }
    
    // Time-based filtering
    if (req.query.timeFilter) {
      const now = new Date();
      let startDate;
      
      switch (req.query.timeFilter) {
        case 'today':
          startDate = new Date(now.setHours(0, 0, 0, 0));
          break;
        case 'yesterday':
          startDate = new Date(now.setDate(now.getDate() - 1));
          startDate.setHours(0, 0, 0, 0);
          break;
        case 'last7days':
          startDate = new Date(now.setDate(now.getDate() - 7));
          break;
        case 'last30days':
          startDate = new Date(now.setDate(now.getDate() - 30));
          break;
        case 'thisMonth':
          startDate = new Date(now.getFullYear(), now.getMonth(), 1);
          break;
        case 'lastMonth':
          startDate = new Date(now.getFullYear(), now.getMonth() - 1, 1);
          const endDate = new Date(now.getFullYear(), now.getMonth(), 0);
          filter.createdAt = { $gte: startDate, $lte: endDate };
          break;
        default:
          // No filter
      }
      
      if (startDate && !filter.createdAt) {
        filter.createdAt = { $gte: startDate };
      }
    }
    
    // Execute query with pagination and filters
    const sharedWishes = await SharedWish.find(filter)
      .sort(sort)
      .skip(skip)
      .limit(limit)
      .populate('template', 'title')
      .populate({
        path: 'viewerEngagement.userId',
        select: 'email displayName uid',
        model: 'User'
      });
    
    // Get total count for pagination
    const totalItems = await SharedWish.countDocuments(filter);
    
    logger.info(`Admin shared wishes list retrieved by ${req.adminInfo.email}`, {
      page,
      limit,
      totalItems,
      filters: JSON.stringify(filter)
    });
    
    res.status(200).json({
      success: true,
      data: sharedWishes,
      totalItems,
      page,
      limit
    });
  } catch (error) {
    logger.error(`Error retrieving shared wishes: ${error.message}`, { error });
    res.status(500).json({
      success: false,
      message: 'Server error retrieving shared wishes',
      error: error.message
    });
  }
});

/**
 * @route   GET /api/admin/shared-wishes/analytics
 * @desc    Get analytics data for shared wishes
 * @access  Admin only
 */
router.get('/shared-wishes/analytics', verifyFirebaseToken, async (req, res) => {
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
    
    // Time-based filtering
    const filter = {};
    if (req.query.timeFilter) {
      const now = new Date();
      let startDate;
      
      switch (req.query.timeFilter) {
        case 'today':
          startDate = new Date(now.setHours(0, 0, 0, 0));
          break;
        case 'yesterday':
          startDate = new Date(now.setDate(now.getDate() - 1));
          startDate.setHours(0, 0, 0, 0);
          break;
        case 'last7days':
          startDate = new Date(now.setDate(now.getDate() - 7));
          break;
        case 'last30days':
          startDate = new Date(now.setDate(now.getDate() - 30));
          break;
        case 'thisMonth':
          startDate = new Date(now.getFullYear(), now.getMonth(), 1);
          break;
        case 'lastMonth':
          startDate = new Date(now.getFullYear(), now.getMonth() - 1, 1);
          const endDate = new Date(now.getFullYear(), now.getMonth(), 0);
          filter.createdAt = { $gte: startDate, $lte: endDate };
          break;
        default:
          // No filter
      }
      
      if (startDate && !filter.createdAt) {
        filter.createdAt = { $gte: startDate };
      }
    }
    
    // Get analytics data
    const totalShares = await SharedWish.countDocuments(filter);
    
    // Get total views
    const viewsAggregate = await SharedWish.aggregate([
      { $match: filter },
      { $group: { _id: null, totalViews: { $sum: '$views' }, totalUniqueViews: { $sum: '$uniqueViews' } } }
    ]);
    
    const totalViews = viewsAggregate.length > 0 ? viewsAggregate[0].totalViews : 0;
    const totalUniqueViews = viewsAggregate.length > 0 ? viewsAggregate[0].totalUniqueViews : 0;
    
    // Get top templates by shares
    const topTemplates = await SharedWish.aggregate([
      { $match: filter },
      { $group: { _id: '$template', count: { $sum: 1 }, views: { $sum: '$views' } } },
      { $sort: { count: -1 } },
      { $limit: 5 },
      { $lookup: { from: 'templates', localField: '_id', foreignField: '_id', as: 'templateDetails' } },
      { $unwind: { path: '$templateDetails', preserveNullAndEmptyArrays: true } },
      { $project: { 
        templateId: '$_id', 
        title: '$templateDetails.title', 
        shareCount: '$count',
        viewCount: '$views'
      } }
    ]);
    
    // Get sharing by platform
    const sharingByPlatform = await SharedWish.aggregate([
      { $match: filter },
      { $group: { _id: '$sharedVia', count: { $sum: 1 } } },
      { $sort: { count: -1 } },
      { $project: { platform: '$_id', count: 1, _id: 0 } }
    ]);
    
    // Get daily sharing trend
    const now = new Date();
    const lastWeek = new Date(now.setDate(now.getDate() - 7));
    
    const dailyTrend = await SharedWish.aggregate([
      { $match: { createdAt: { $gte: lastWeek } } },
      { $group: {
        _id: { $dateToString: { format: '%Y-%m-%d', date: '$createdAt' } },
        count: { $sum: 1 },
        views: { $sum: '$views' }
      } },
      { $sort: { _id: 1 } },
      { $project: { date: '$_id', count: 1, views: 1, _id: 0 } }
    ]);
    
    logger.info(`Admin fetched shared wish analytics`, { 
      admin: req.adminInfo.email,
      role: req.adminInfo.role,
      filters: JSON.stringify(filter)
    });
    
    res.status(200).json({
      success: true,
      analytics: {
        totalShares,
        totalViews,
        totalUniqueViews,
        topTemplates,
        sharingByPlatform,
        dailyTrend
      }
    });
  } catch (error) {
    logger.error(`Error retrieving shared wish analytics: ${error.message}`, { error });
    res.status(500).json({
      success: false,
      message: 'Server error retrieving shared wish analytics',
      error: error.message
    });
  }
});

/**
 * @route   GET /api/admin/shared-wishes/:id
 * @desc    Get a single shared wish by ID
 * @access  Admin only
 */
router.get('/shared-wishes/:id', verifyFirebaseToken, async (req, res) => {
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
    
    const { id } = req.params;
    
    // Validate ID parameter
    if (!id || id === 'undefined' || id === 'null') {
      logger.warn(`Invalid SharedWish ID provided: "${id}"`);
      return res.status(400).json({
        success: false,
        message: 'Invalid SharedWish ID provided',
        error: 'ID parameter is missing or invalid'
      });
    }

    // Validate that ID is a valid MongoDB ObjectId
    if (!mongoose.Types.ObjectId.isValid(id)) {
      logger.warn(`Invalid MongoDB ObjectId format: "${id}"`);
      return res.status(400).json({
        success: false,
        message: 'Invalid SharedWish ID format',
        error: 'ID is not a valid MongoDB ObjectId'
      });
    }
    
    const sharedWish = await SharedWish.findById(id)
      .populate('template', 'title')
      .populate({
        path: 'viewerEngagement.userId',
        select: 'email displayName uid',
        model: 'User'
      });
    
    if (!sharedWish) {
      return res.status(404).json({
        success: false,
        message: 'Shared wish not found'
      });
    }
    
    logger.info(`Admin viewed shared wish ${id}`, { 
      admin: req.adminInfo.email,
      role: req.adminInfo.role
    });
    
    res.status(200).json({
      success: true,
      sharedWish
    });
  } catch (error) {
    logger.error(`Error retrieving shared wish: ${error.message}`, { error });
    res.status(500).json({
      success: false,
      message: 'Server error retrieving shared wish',
      error: error.message
    });
  }
});

/**
 * @route   PUT /api/admin/shared-wishes/:id
 * @desc    Update a shared wish
 * @access  Admin only
 */
router.put('/shared-wishes/:id', verifyFirebaseToken, async (req, res) => {
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
    
    const { id } = req.params;
    
    // Validate ID parameter
    if (!id || id === 'undefined' || id === 'null') {
      logger.warn(`Invalid SharedWish ID provided: "${id}"`);
      return res.status(400).json({
        success: false,
        message: 'Invalid SharedWish ID provided',
        error: 'ID parameter is missing or invalid'
      });
    }

    // Validate that ID is a valid MongoDB ObjectId
    if (!mongoose.Types.ObjectId.isValid(id)) {
      logger.warn(`Invalid MongoDB ObjectId format: "${id}"`);
      return res.status(400).json({
        success: false,
        message: 'Invalid SharedWish ID format',
        error: 'ID is not a valid MongoDB ObjectId'
      });
    }
    
    // Find shared wish
    const sharedWish = await SharedWish.findById(id);
    
    if (!sharedWish) {
      return res.status(404).json({
        success: false,
        message: 'Shared wish not found'
      });
    }
    
    // Update shared wish with request body
    // Don't allow updating template reference
    const updateData = { ...req.body };
    delete updateData.template;
    
    // Update the shared wish
    const updatedSharedWish = await SharedWish.findByIdAndUpdate(
      id,
      updateData,
      { new: true, runValidators: true }
    ).populate('template', 'title');
    
    logger.info(`Admin updated shared wish ${id}`, { 
      admin: req.adminInfo.email,
      role: req.adminInfo.role
    });
    
    res.status(200).json({
      success: true,
      message: 'Shared wish updated successfully',
      sharedWish: updatedSharedWish
    });
  } catch (error) {
    logger.error(`Error updating shared wish: ${error.message}`, { error });
    res.status(500).json({
      success: false,
      message: 'Server error updating shared wish',
      error: error.message
    });
  }
});

/**
 * @route   DELETE /api/admin/shared-wishes/:id
 * @desc    Delete a shared wish
 * @access  Admin only
 */
router.delete('/shared-wishes/:id', verifyFirebaseToken, async (req, res) => {
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
    
    const { id } = req.params;
    
    // Validate ID parameter
    if (!id || id === 'undefined' || id === 'null') {
      logger.warn(`Invalid SharedWish ID provided: "${id}"`);
      return res.status(400).json({
        success: false,
        message: 'Invalid SharedWish ID provided',
        error: 'ID parameter is missing or invalid'
      });
    }

    // Validate that ID is a valid MongoDB ObjectId
    if (!mongoose.Types.ObjectId.isValid(id)) {
      logger.warn(`Invalid MongoDB ObjectId format: "${id}"`);
      return res.status(400).json({
        success: false,
        message: 'Invalid SharedWish ID format',
        error: 'ID is not a valid MongoDB ObjectId'
      });
    }
    
    // Find and delete shared wish
    const sharedWish = await SharedWish.findByIdAndDelete(id);
    
    if (!sharedWish) {
      return res.status(404).json({
        success: false,
        message: 'Shared wish not found'
      });
    }
    
    logger.info(`Admin deleted shared wish ${id}`, { 
      admin: req.adminInfo.email,
      role: req.adminInfo.role,
      shortCode: sharedWish.shortCode
    });
    
    res.status(200).json({
      success: true,
      message: 'Shared wish deleted successfully'
    });
  } catch (error) {
    logger.error(`Error deleting shared wish: ${error.message}`, { error });
    res.status(500).json({
      success: false,
      message: 'Server error deleting shared wish',
      error: error.message
    });
  }
});

/**
 * @route   GET /api/admin/category-icons
 * @desc    Get all category icons with pagination, sorting and filtering
 * @access  Admin only
 */
router.get('/category-icons', verifyFirebaseToken, async (req, res) => {
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
    const limit = parseInt(req.query.limit) || 10;
    const skip = (page - 1) * limit;
    
    // Sorting parameters
    const sortField = req.query.sort || 'category';
    const sortOrder = req.query.order === 'asc' ? 1 : -1;
    const sort = { [sortField]: sortOrder };
    
    // Filtering parameters
    const filter = {};
    
    // Filter by search query (on id, category)
    if (req.query.q) {
      const searchQuery = req.query.q;
      filter.$or = [
        { id: { $regex: searchQuery, $options: 'i' } },
        { category: { $regex: searchQuery, $options: 'i' } }
      ];
    }
    
    // Execute query with pagination and filters
    const categoryIcons = await CategoryIcon.find(filter)
      .sort(sort)
      .skip(skip)
      .limit(limit);
    
    // Get total count for pagination
    const totalItems = await CategoryIcon.countDocuments(filter);
    
    logger.info(`Admin category icons list retrieved by ${req.adminInfo.email}`, {
      page,
      limit,
      totalItems,
      filters: JSON.stringify(filter)
    });
    
    res.status(200).json({
      success: true,
      data: categoryIcons,
      totalItems,
      page,
      limit
    });
  } catch (error) {
    logger.error(`Error retrieving category icons: ${error.message}`, { error });
    res.status(500).json({
      success: false,
      message: 'Server error retrieving category icons',
      error: error.message
    });
  }
});

/**
 * @route   GET /api/admin/category-icons/:id
 * @desc    Get a single category icon by ID
 * @access  Admin only
 */
router.get('/category-icons/:id', verifyFirebaseToken, async (req, res) => {
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
    
    const { id } = req.params;
    
    // Validate ID parameter
    if (!id || id === 'undefined' || id === 'null') {
      logger.warn(`Invalid CategoryIcon ID provided: "${id}"`);
      return res.status(400).json({
        success: false,
        message: 'Invalid CategoryIcon ID provided',
        error: 'ID parameter is missing or invalid'
      });
    }

    // Try to find the category icon by either _id or id field
    // This handles both MongoDB ObjectId and custom string ID
    const categoryIcon = await CategoryIcon.findOne({
      $or: [
        { _id: id },
        { id: id }
      ]
    });
    
    if (!categoryIcon) {
      return res.status(404).json({
        success: false,
        message: 'Category icon not found'
      });
    }
    
    logger.info(`Admin viewed category icon ${id}`, { 
      admin: req.adminInfo.email,
      role: req.adminInfo.role
    });
    
    res.status(200).json({
      success: true,
      categoryIcon
    });
  } catch (error) {
    logger.error(`Error retrieving category icon: ${error.message}`, { error });
    res.status(500).json({
      success: false,
      message: 'Server error retrieving category icon',
      error: error.message
    });
  }
});

/**
 * @route   POST /api/admin/category-icons
 * @desc    Create a new category icon
 * @access  Admin only
 */
router.post('/category-icons', verifyFirebaseToken, async (req, res) => {
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
    
    // Validate required fields
    if (!req.body.category || !req.body.categoryIcon) {
      return res.status(400).json({
        success: false,
        message: 'Category and categoryIcon are required fields'
      });
    }
    
    // Check if a category icon with the same category already exists
    const existingIcon = await CategoryIcon.findOne({ category: req.body.category });
    if (existingIcon) {
      return res.status(400).json({
        success: false,
        message: 'A category icon with this category already exists'
      });
    }
    
    // Create a temporary MongoDB ObjectId to use as the base for the custom ID
    const tempId = new mongoose.Types.ObjectId();
    
    // Create new category icon with auto-generated id using "cat_" prefix
    const categoryIcon = new CategoryIcon({
      ...req.body,
      id: `cat_${tempId.toString()}` // Auto-generate id with "cat_" prefix
    });
    
    await categoryIcon.save();
    
    logger.info(`Admin created new category icon: ${categoryIcon.category}`, { 
      admin: req.adminInfo.email,
      role: req.adminInfo.role
    });
    
    res.status(201).json({
      success: true,
      message: 'Category icon created successfully',
      categoryIcon
    });
  } catch (error) {
    logger.error(`Error creating category icon: ${error.message}`, { error });
    res.status(500).json({
      success: false,
      message: 'Server error creating category icon',
      error: error.message
    });
  }
});

/**
 * @route   PUT /api/admin/category-icons/:id
 * @desc    Update a category icon
 * @access  Admin only
 */
router.put('/category-icons/:id', verifyFirebaseToken, async (req, res) => {
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
    
    const { id } = req.params;
    
    // Validate ID parameter
    if (!id || id === 'undefined' || id === 'null') {
      logger.warn(`Invalid CategoryIcon ID provided: "${id}"`);
      return res.status(400).json({
        success: false,
        message: 'Invalid CategoryIcon ID provided',
        error: 'ID parameter is missing or invalid'
      });
    }

    // Try to find the category icon by either _id or id field
    // This handles both MongoDB ObjectId and custom string ID
    const categoryIcon = await CategoryIcon.findOne({
      $or: [
        { _id: id },
        { id: id }
      ]
    });
    
    if (!categoryIcon) {
      return res.status(404).json({
        success: false,
        message: 'Category icon not found'
      });
    }
    
    // If updating id or category, check if they're already in use by another icon
    if (req.body.id || req.body.category) {
      const existingIcon = await CategoryIcon.findOne({
        _id: { $ne: categoryIcon._id },
        $or: [
          { id: req.body.id || categoryIcon.id },
          { category: req.body.category || categoryIcon.category }
        ]
      });
      
      if (existingIcon) {
        return res.status(400).json({
          success: false,
          message: 'Another category icon with the same id or category already exists'
        });
      }
    }
    
    // Update the category icon by _id
    Object.assign(categoryIcon, req.body);
    await categoryIcon.save();
    
    logger.info(`Admin updated category icon ${id}`, { 
      admin: req.adminInfo.email,
      role: req.adminInfo.role
    });
    
    res.status(200).json({
      success: true,
      message: 'Category icon updated successfully',
      categoryIcon
    });
  } catch (error) {
    logger.error(`Error updating category icon: ${error.message}`, { error });
    res.status(500).json({
      success: false,
      message: 'Server error updating category icon',
      error: error.message
    });
  }
});

/**
 * @route   DELETE /api/admin/category-icons/:id
 * @desc    Delete a category icon
 * @access  Admin only
 */
router.delete('/category-icons/:id', verifyFirebaseToken, async (req, res) => {
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
    
    const { id } = req.params;
    
    // Validate ID parameter
    if (!id || id === 'undefined' || id === 'null') {
      logger.warn(`Invalid CategoryIcon ID provided: "${id}"`);
      return res.status(400).json({
        success: false,
        message: 'Invalid CategoryIcon ID provided',
        error: 'ID parameter is missing or invalid'
      });
    }

    // Try to find the category icon by either _id or id field
    // This handles both MongoDB ObjectId and custom string ID
    const categoryIcon = await CategoryIcon.findOne({
      $or: [
        { _id: id },
        { id: id }
      ]
    });
    
    if (!categoryIcon) {
      return res.status(404).json({
        success: false,
        message: 'Category icon not found'
      });
    }
    
    // Check if category icon is being used by templates
    const templatesUsingIcon = await Template.countDocuments({ categoryIcon: categoryIcon._id });
    
    if (templatesUsingIcon > 0) {
      return res.status(400).json({
        success: false,
        message: `Cannot delete category icon: it is being used by ${templatesUsingIcon} templates`
      });
    }
    
    // Delete the category icon
    await CategoryIcon.deleteOne({ _id: categoryIcon._id });
    
    logger.info(`Admin deleted category icon ${id}`, { 
      admin: req.adminInfo.email,
      role: req.adminInfo.role
    });
    
    res.status(200).json({
      success: true,
      message: 'Category icon deleted successfully'
    });
  } catch (error) {
    logger.error(`Error deleting category icon: ${error.message}`, { error });
    res.status(500).json({
      success: false,
      message: 'Server error deleting category icon',
      error: error.message
    });
  }
});

/**
 * @route   PATCH /api/admin/category-icons/:id/toggle-status
 * @desc    Toggle category icon status
 * @access  Admin only
 */
router.patch('/category-icons/:id/toggle-status', verifyFirebaseToken, async (req, res) => {
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
    
    const { id } = req.params;
    
    // Validate ID parameter
    if (!id || id === 'undefined' || id === 'null') {
      logger.warn(`Invalid CategoryIcon ID provided: "${id}"`);
      return res.status(400).json({
        success: false,
        message: 'Invalid CategoryIcon ID provided',
        error: 'ID parameter is missing or invalid'
      });
    }

    // Try to find the category icon by either _id or id field
    // This handles both MongoDB ObjectId and custom string ID
    const categoryIcon = await CategoryIcon.findOne({
      $or: [
        { _id: id },
        { id: id }
      ]
    });
    
    if (!categoryIcon) {
      return res.status(404).json({
        success: false,
        message: 'Category icon not found'
      });
    }
    
    // Toggle status (add status field if it doesn't exist)
    const currentStatus = categoryIcon.status !== undefined ? categoryIcon.status : true;
    categoryIcon.status = !currentStatus;
    await categoryIcon.save();
    
    logger.info(`Admin toggled category icon status ${id} to ${categoryIcon.status}`, { 
      admin: req.adminInfo.email,
      role: req.adminInfo.role
    });
    
    res.status(200).json({
      success: true,
      message: `Category icon status toggled to ${categoryIcon.status ? 'active' : 'inactive'}`,
      categoryIcon
    });
  } catch (error) {
    logger.error(`Error toggling category icon status: ${error.message}`, { error });
    res.status(500).json({
      success: false,
      message: 'Server error toggling category icon status',
      error: error.message
    });
  }
});

/**
 * @route   GET /api/admin/festivals
 * @desc    Get all festivals with pagination and filtering
 * @access  Admin only
 */
router.get('/festivals', verifyFirebaseToken, async (req, res) => {
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
    
    // Parse query parameters
    const page = parseInt(req.query.page) || 1;
    const limit = parseInt(req.query.limit) || 10;
    const skip = (page - 1) * limit;
    const sort = req.query.sort || 'createdAt';
    const order = req.query.order === 'asc' ? 1 : -1;
    
    // Build sort object
    const sortObj = {};
    sortObj[sort] = order;
    
    // Build filter object
    const filter = {};
    
    // Text search
    if (req.query.q) {
      filter.$or = [
        { name: { $regex: req.query.q, $options: 'i' } },
        { slug: { $regex: req.query.q, $options: 'i' } },
        { category: { $regex: req.query.q, $options: 'i' } },
        { description: { $regex: req.query.q, $options: 'i' } }
      ];
    }
    
    // Status filter
    if (req.query.status) {
      filter.status = req.query.status;
    }
    
    // Active filter
    if (req.query.isActive !== undefined) {
      filter.isActive = req.query.isActive === 'true';
    }
    
    // Category filter
    if (req.query.category) {
      filter.category = req.query.category;
    }
    
    // Date range filter
    if (req.query.startDate || req.query.endDate) {
      filter.date = {};
      if (req.query.startDate) {
        filter.date.$gte = new Date(req.query.startDate);
      }
      if (req.query.endDate) {
        filter.date.$lte = new Date(req.query.endDate);
      }
    }
    
    // Count total items
    const totalItems = await Festival.countDocuments(filter);
    
    // Get festivals with pagination, sorting, and filtering
    const festivals = await Festival.find(filter)
      .populate('categoryIcon')
      .populate('templates')
      .sort(sortObj)
      .skip(skip)
      .limit(limit);
    
    logger.info(`Admin fetched festivals list (page ${page}, limit ${limit})`, { 
      admin: req.adminInfo.email,
      role: req.adminInfo.role
    });
    
    res.status(200).json({
      success: true,
      data: festivals,
      page,
      limit,
      totalItems,
      totalPages: Math.ceil(totalItems / limit)
    });
  } catch (error) {
    logger.error(`Error fetching festivals: ${error.message}`, { error });
    res.status(500).json({
      success: false,
      message: 'Server error fetching festivals',
      error: error.message
    });
  }
});

/**
 * @route   GET /api/admin/festivals/:id
 * @desc    Get a single festival by ID
 * @access  Admin only
 */
router.get('/festivals/:id', verifyFirebaseToken, async (req, res) => {
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
    
    const { id } = req.params;
    
    // Validate ID parameter
    if (!id || id === 'undefined' || id === 'null') {
      logger.warn(`Invalid festival ID provided: "${id}"`);
      return res.status(400).json({
        success: false,
        message: 'Invalid festival ID provided',
        error: 'ID parameter is missing or invalid'
      });
    }

    // Find festival by ID
    const festival = await Festival.findById(id)
      .populate('categoryIcon')
      .populate('templates');
    
    if (!festival) {
      return res.status(404).json({
        success: false,
        message: 'Festival not found'
      });
    }
    
    logger.info(`Admin viewed festival ${id}`, { 
      admin: req.adminInfo.email,
      role: req.adminInfo.role
    });
    
    res.status(200).json({
      success: true,
      festival
    });
  } catch (error) {
    logger.error(`Error retrieving festival: ${error.message}`, { error });
    res.status(500).json({
      success: false,
      message: 'Server error retrieving festival',
      error: error.message
    });
  }
});

/**
 * @route   POST /api/admin/festivals
 * @desc    Create a new festival
 * @access  Admin only
 */
router.post('/festivals', verifyFirebaseToken, async (req, res) => {
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
    
    // Validate required fields
    if (!req.body.name || !req.body.date || !req.body.category) {
      return res.status(400).json({
        success: false,
        message: 'Missing required fields',
        error: 'name, date, and category are required'
      });
    }
    
    // Check if a festival with the same slug already exists
    if (req.body.slug) {
      const existingFestival = await Festival.findOne({ slug: req.body.slug });
      if (existingFestival) {
        return res.status(400).json({
          success: false,
          message: 'A festival with this slug already exists'
        });
      }
    }
    
    // Create new festival
    const festival = new Festival(req.body);
    await festival.save();
    
    logger.info(`Admin created new festival: ${festival.name}`, { 
      admin: req.adminInfo.email,
      role: req.adminInfo.role
    });
    
    res.status(201).json({
      success: true,
      message: 'Festival created successfully',
      festival
    });
  } catch (error) {
    logger.error(`Error creating festival: ${error.message}`, { error });
    res.status(500).json({
      success: false,
      message: 'Server error creating festival',
      error: error.message
    });
  }
});

/**
 * @route   PUT /api/admin/festivals/:id
 * @desc    Update a festival
 * @access  Admin only
 */
router.put('/festivals/:id', verifyFirebaseToken, async (req, res) => {
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
    
    const { id } = req.params;
    
    // Validate ID parameter
    if (!id || id === 'undefined' || id === 'null') {
      logger.warn(`Invalid festival ID provided: "${id}"`);
      return res.status(400).json({
        success: false,
        message: 'Invalid festival ID provided',
        error: 'ID parameter is missing or invalid'
      });
    }

    // Check if festival exists
    const festival = await Festival.findById(id);
    
    if (!festival) {
      return res.status(404).json({
        success: false,
        message: 'Festival not found'
      });
    }
    
    // If updating slug, check if it's already in use by another festival
    if (req.body.slug && req.body.slug !== festival.slug) {
      const existingFestival = await Festival.findOne({ 
        slug: req.body.slug,
        _id: { $ne: id }
      });
      
      if (existingFestival) {
        return res.status(400).json({
          success: false,
          message: 'Another festival with this slug already exists'
        });
      }
    }
    
    // Update festival
    const updatedFestival = await Festival.findByIdAndUpdate(
      id,
      req.body,
      { new: true, runValidators: true }
    ).populate('categoryIcon').populate('templates');
    
    logger.info(`Admin updated festival ${id}`, { 
      admin: req.adminInfo.email,
      role: req.adminInfo.role
    });
    
    res.status(200).json({
      success: true,
      message: 'Festival updated successfully',
      festival: updatedFestival
    });
  } catch (error) {
    logger.error(`Error updating festival: ${error.message}`, { error });
    res.status(500).json({
      success: false,
      message: 'Server error updating festival',
      error: error.message
    });
  }
});

/**
 * @route   DELETE /api/admin/festivals/:id
 * @desc    Delete a festival
 * @access  Admin only
 */
router.delete('/festivals/:id', verifyFirebaseToken, async (req, res) => {
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
    
    const { id } = req.params;
    
    // Validate ID parameter
    if (!id || id === 'undefined' || id === 'null') {
      logger.warn(`Invalid festival ID provided: "${id}"`);
      return res.status(400).json({
        success: false,
        message: 'Invalid festival ID provided',
        error: 'ID parameter is missing or invalid'
      });
    }

    // Check if festival exists
    const festival = await Festival.findById(id);
    
    if (!festival) {
      return res.status(404).json({
        success: false,
        message: 'Festival not found'
      });
    }
    
    // Delete festival
    await Festival.findByIdAndDelete(id);
    
    logger.info(`Admin deleted festival ${id}`, { 
      admin: req.adminInfo.email,
      role: req.adminInfo.role
    });
    
    res.status(200).json({
      success: true,
      message: 'Festival deleted successfully'
    });
  } catch (error) {
    logger.error(`Error deleting festival: ${error.message}`, { error });
    res.status(500).json({
      success: false,
      message: 'Server error deleting festival',
      error: error.message
    });
  }
});

/**
 * @route   PATCH /api/admin/festivals/:id/toggle-status
 * @desc    Toggle festival status (isActive)
 * @access  Admin only
 */
router.patch('/festivals/:id/toggle-status', verifyFirebaseToken, async (req, res) => {
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
    
    const { id } = req.params;
    
    // Validate ID parameter
    if (!id || id === 'undefined' || id === 'null') {
      logger.warn(`Invalid festival ID provided: "${id}"`);
      return res.status(400).json({
        success: false,
        message: 'Invalid festival ID provided',
        error: 'ID parameter is missing or invalid'
      });
    }

    // Find festival by ID
    const festival = await Festival.findById(id);
    
    if (!festival) {
      return res.status(404).json({
        success: false,
        message: 'Festival not found'
      });
    }
    
    // Toggle isActive status
    festival.isActive = !festival.isActive;
    await festival.save();
    
    logger.info(`Admin toggled festival status ${id} to ${festival.isActive}`, { 
      admin: req.adminInfo.email,
      role: req.adminInfo.role
    });
    
    res.status(200).json({
      success: true,
      message: `Festival status toggled to ${festival.isActive ? 'active' : 'inactive'}`,
      festival
    });
  } catch (error) {
    logger.error(`Error toggling festival status: ${error.message}`, { error });
    res.status(500).json({
      success: false,
      message: 'Server error toggling festival status',
      error: error.message
    });
  }
});

/**
 * @route   GET /api/admin/about
 * @desc    Get all about entries with pagination and filtering
 * @access  Admin only
 */
router.get('/about', verifyFirebaseToken, async (req, res) => {
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
    
    // Parse query parameters
    const page = parseInt(req.query.page) || 1;
    const limit = parseInt(req.query.limit) || 10;
    const skip = (page - 1) * limit;
    const sort = req.query.sort || 'createdAt';
    const order = req.query.order === 'asc' ? 1 : -1;
    
    // Build sort object
    const sortObj = {};
    sortObj[sort] = order;
    
    // Build filter object
    const filter = {};
    
    // Text search
    if (req.query.q) {
      filter.title = { $regex: req.query.q, $options: 'i' };
    }
    
    // Active filter
    if (req.query.isActive !== undefined) {
      filter.isActive = req.query.isActive === 'true';
    }
    
    // Count total items
    const totalItems = await About.countDocuments(filter);
    
    // Get about entries with pagination, sorting, and filtering
    const abouts = await About.find(filter)
      .sort(sortObj)
      .skip(skip)
      .limit(limit);
    
    logger.info(`Admin fetched about entries list (page ${page}, limit ${limit})`, { 
      admin: req.adminInfo.email,
      role: req.adminInfo.role
    });
    
    res.status(200).json({
      success: true,
      data: abouts,
      page,
      limit,
      totalItems,
      totalPages: Math.ceil(totalItems / limit)
    });
  } catch (error) {
    logger.error(`Error fetching about entries: ${error.message}`, { error });
    res.status(500).json({
      success: false,
      message: 'Server error fetching about entries',
      error: error.message
    });
  }
});

/**
 * @route   GET /api/admin/about/:id
 * @desc    Get a single about entry by ID
 * @access  Admin only
 */
router.get('/about/:id', verifyFirebaseToken, async (req, res) => {
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
    
    const { id } = req.params;
    
    // Validate ID parameter
    if (!id || id === 'undefined' || id === 'null') {
      logger.warn(`Invalid about ID provided: "${id}"`);
      return res.status(400).json({
        success: false,
        message: 'Invalid about ID provided',
        error: 'ID parameter is missing or invalid'
      });
    }

    // Find about entry by ID
    const about = await About.findById(id);
    
    if (!about) {
      return res.status(404).json({
        success: false,
        message: 'About entry not found'
      });
    }
    
    logger.info(`Admin viewed about entry ${id}`, { 
      admin: req.adminInfo.email,
      role: req.adminInfo.role
    });
    
    res.status(200).json({
      success: true,
      about
    });
  } catch (error) {
    logger.error(`Error retrieving about entry: ${error.message}`, { error });
    res.status(500).json({
      success: false,
      message: 'Server error retrieving about entry',
      error: error.message
    });
  }
});

/**
 * @route   POST /api/admin/about
 * @desc    Create a new about entry
 * @access  Admin only
 */
router.post('/about', verifyFirebaseToken, async (req, res) => {
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
    
    // Validate required fields
    if (!req.body.title || !req.body.htmlCode) {
      return res.status(400).json({
        success: false,
        message: 'Missing required fields',
        error: 'title and htmlCode are required'
      });
    }
    
    // Create new about entry
    const about = new About({
      title: req.body.title,
      htmlCode: req.body.htmlCode,
      isActive: req.body.isActive !== undefined ? req.body.isActive : true
    });
    
    await about.save();
    
    logger.info(`Admin created new about entry: ${about.title}`, { 
      admin: req.adminInfo.email,
      role: req.adminInfo.role
    });
    
    res.status(201).json({
      success: true,
      message: 'About entry created successfully',
      about
    });
  } catch (error) {
    logger.error(`Error creating about entry: ${error.message}`, { error });
    res.status(500).json({
      success: false,
      message: 'Server error creating about entry',
      error: error.message
    });
  }
});

/**
 * @route   PUT /api/admin/about/:id
 * @desc    Update an about entry
 * @access  Admin only
 */
router.put('/about/:id', verifyFirebaseToken, async (req, res) => {
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
    
    const { id } = req.params;
    
    // Validate ID parameter
    if (!id || id === 'undefined' || id === 'null') {
      logger.warn(`Invalid about ID provided: "${id}"`);
      return res.status(400).json({
        success: false,
        message: 'Invalid about ID provided',
        error: 'ID parameter is missing or invalid'
      });
    }

    // Check if about entry exists
    const about = await About.findById(id);
    
    if (!about) {
      return res.status(404).json({
        success: false,
        message: 'About entry not found'
      });
    }
    
    // Update about entry
    const updatedAbout = await About.findByIdAndUpdate(
      id,
      {
        title: req.body.title || about.title,
        htmlCode: req.body.htmlCode || about.htmlCode,
        isActive: req.body.isActive !== undefined ? req.body.isActive : about.isActive
      },
      { new: true, runValidators: true }
    );
    
    logger.info(`Admin updated about entry ${id}`, { 
      admin: req.adminInfo.email,
      role: req.adminInfo.role
    });
    
    res.status(200).json({
      success: true,
      message: 'About entry updated successfully',
      about: updatedAbout
    });
  } catch (error) {
    logger.error(`Error updating about entry: ${error.message}`, { error });
    res.status(500).json({
      success: false,
      message: 'Server error updating about entry',
      error: error.message
    });
  }
});

/**
 * @route   DELETE /api/admin/about/:id
 * @desc    Delete an about entry
 * @access  Admin only
 */
router.delete('/about/:id', verifyFirebaseToken, async (req, res) => {
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
    
    const { id } = req.params;
    
    // Validate ID parameter
    if (!id || id === 'undefined' || id === 'null') {
      logger.warn(`Invalid about ID provided: "${id}"`);
      return res.status(400).json({
        success: false,
        message: 'Invalid about ID provided',
        error: 'ID parameter is missing or invalid'
      });
    }

    // Check if about entry exists
    const about = await About.findById(id);
    
    if (!about) {
      return res.status(404).json({
        success: false,
        message: 'About entry not found'
      });
    }
    
    // Don't allow deletion of the only active entry if it's active
    if (about.isActive) {
      const count = await About.countDocuments();
      if (count === 1) {
        return res.status(400).json({
          success: false,
          message: 'Cannot delete the only active about entry. Create another entry first.'
        });
      }
    }
    
    // Delete about entry
    await About.findByIdAndDelete(id);
    
    logger.info(`Admin deleted about entry ${id}`, { 
      admin: req.adminInfo.email,
      role: req.adminInfo.role
    });
    
    res.status(200).json({
      success: true,
      message: 'About entry deleted successfully'
    });
  } catch (error) {
    logger.error(`Error deleting about entry: ${error.message}`, { error });
    res.status(500).json({
      success: false,
      message: 'Server error deleting about entry',
      error: error.message
    });
  }
});

/**
 * @route   PATCH /api/admin/about/:id/toggle-status
 * @desc    Toggle about entry status (isActive)
 * @access  Admin only
 */
router.patch('/about/:id/toggle-status', verifyFirebaseToken, async (req, res) => {
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
    
    const { id } = req.params;
    
    // Validate ID parameter
    if (!id || id === 'undefined' || id === 'null') {
      logger.warn(`Invalid about ID provided: "${id}"`);
      return res.status(400).json({
        success: false,
        message: 'Invalid about ID provided',
        error: 'ID parameter is missing or invalid'
      });
    }

    // Find about entry by ID
    const about = await About.findById(id);
    
    if (!about) {
      return res.status(404).json({
        success: false,
        message: 'About entry not found'
      });
    }
    
    // Toggle isActive status
    // Note: The pre-save hook will handle making other entries inactive
    about.isActive = !about.isActive;
    await about.save();
    
    logger.info(`Admin toggled about entry status ${id} to ${about.isActive}`, { 
      admin: req.adminInfo.email,
      role: req.adminInfo.role
    });
    
    res.status(200).json({
      success: true,
      message: `About entry status toggled to ${about.isActive ? 'active' : 'inactive'}`,
      about
    });
  } catch (error) {
    logger.error(`Error toggling about entry status: ${error.message}`, { error });
    res.status(500).json({
      success: false,
      message: 'Server error toggling about entry status',
      error: error.message
    });
  }
});

/**
 * @route   GET /api/admin/contact
 * @desc    Get all contact entries with pagination and filtering
 * @access  Admin only
 */
router.get('/contact', verifyFirebaseToken, async (req, res) => {
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
    
    // Parse query parameters
    const page = parseInt(req.query.page) || 1;
    const limit = parseInt(req.query.limit) || 10;
    const skip = (page - 1) * limit;
    const sort = req.query.sort || 'createdAt';
    const order = req.query.order === 'asc' ? 1 : -1;
    
    // Build sort object
    const sortObj = {};
    sortObj[sort] = order;
    
    // Build filter object
    const filter = {};
    
    // Text search
    if (req.query.q) {
      filter.title = { $regex: req.query.q, $options: 'i' };
    }
    
    // Active filter
    if (req.query.isActive !== undefined) {
      filter.isActive = req.query.isActive === 'true';
    }
    
    // Count total items
    const totalItems = await Contact.countDocuments(filter);
    
    // Get contact entries with pagination, sorting, and filtering
    const contacts = await Contact.find(filter)
      .sort(sortObj)
      .skip(skip)
      .limit(limit);
    
    logger.info(`Admin fetched contact entries list (page ${page}, limit ${limit})`, { 
      admin: req.adminInfo.email,
      role: req.adminInfo.role
    });
    
    res.status(200).json({
      success: true,
      data: contacts,
      page,
      limit,
      totalItems,
      totalPages: Math.ceil(totalItems / limit)
    });
  } catch (error) {
    logger.error(`Error fetching contact entries: ${error.message}`, { error });
    res.status(500).json({
      success: false,
      message: 'Server error fetching contact entries',
      error: error.message
    });
  }
});

/**
 * @route   GET /api/admin/contact/:id
 * @desc    Get a single contact entry by ID
 * @access  Admin only
 */
router.get('/contact/:id', verifyFirebaseToken, async (req, res) => {
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
    
    const { id } = req.params;
    
    // Validate ID parameter
    if (!id || id === 'undefined' || id === 'null') {
      logger.warn(`Invalid contact ID provided: "${id}"`);
      return res.status(400).json({
        success: false,
        message: 'Invalid contact ID provided',
        error: 'ID parameter is missing or invalid'
      });
    }

    // Find contact entry by ID
    const contact = await Contact.findById(id);
    
    if (!contact) {
      return res.status(404).json({
        success: false,
        message: 'Contact entry not found'
      });
    }
    
    logger.info(`Admin viewed contact entry ${id}`, { 
      admin: req.adminInfo.email,
      role: req.adminInfo.role
    });
    
    res.status(200).json({
      success: true,
      contact
    });
  } catch (error) {
    logger.error(`Error retrieving contact entry: ${error.message}`, { error });
    res.status(500).json({
      success: false,
      message: 'Server error retrieving contact entry',
      error: error.message
    });
  }
});

/**
 * @route   POST /api/admin/contact
 * @desc    Create a new contact entry
 * @access  Admin only
 */
router.post('/contact', verifyFirebaseToken, async (req, res) => {
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
    
    // Validate required fields
    if (!req.body.title || !req.body.htmlCode) {
      return res.status(400).json({
        success: false,
        message: 'Missing required fields',
        error: 'title and htmlCode are required'
      });
    }
    
    // Create new contact entry
    const contact = new Contact({
      title: req.body.title,
      htmlCode: req.body.htmlCode,
      isActive: req.body.isActive !== undefined ? req.body.isActive : true
    });
    
    await contact.save();
    
    logger.info(`Admin created new contact entry: ${contact.title}`, { 
      admin: req.adminInfo.email,
      role: req.adminInfo.role
    });
    
    res.status(201).json({
      success: true,
      message: 'Contact entry created successfully',
      contact
    });
  } catch (error) {
    logger.error(`Error creating contact entry: ${error.message}`, { error });
    res.status(500).json({
      success: false,
      message: 'Server error creating contact entry',
      error: error.message
    });
  }
});

/**
 * @route   PUT /api/admin/contact/:id
 * @desc    Update a contact entry
 * @access  Admin only
 */
router.put('/contact/:id', verifyFirebaseToken, async (req, res) => {
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
    
    const { id } = req.params;
    
    // Validate ID parameter
    if (!id || id === 'undefined' || id === 'null') {
      logger.warn(`Invalid contact ID provided: "${id}"`);
      return res.status(400).json({
        success: false,
        message: 'Invalid contact ID provided',
        error: 'ID parameter is missing or invalid'
      });
    }

    // Check if contact entry exists
    const contact = await Contact.findById(id);
    
    if (!contact) {
      return res.status(404).json({
        success: false,
        message: 'Contact entry not found'
      });
    }
    
    // Update contact entry
    const updatedContact = await Contact.findByIdAndUpdate(
      id,
      {
        title: req.body.title || contact.title,
        htmlCode: req.body.htmlCode || contact.htmlCode,
        isActive: req.body.isActive !== undefined ? req.body.isActive : contact.isActive
      },
      { new: true, runValidators: true }
    );
    
    logger.info(`Admin updated contact entry ${id}`, { 
      admin: req.adminInfo.email,
      role: req.adminInfo.role
    });
    
    res.status(200).json({
      success: true,
      message: 'Contact entry updated successfully',
      contact: updatedContact
    });
  } catch (error) {
    logger.error(`Error updating contact entry: ${error.message}`, { error });
    res.status(500).json({
      success: false,
      message: 'Server error updating contact entry',
      error: error.message
    });
  }
});

/**
 * @route   DELETE /api/admin/contact/:id
 * @desc    Delete a contact entry
 * @access  Admin only
 */
router.delete('/contact/:id', verifyFirebaseToken, async (req, res) => {
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
    
    const { id } = req.params;
    
    // Validate ID parameter
    if (!id || id === 'undefined' || id === 'null') {
      logger.warn(`Invalid contact ID provided: "${id}"`);
      return res.status(400).json({
        success: false,
        message: 'Invalid contact ID provided',
        error: 'ID parameter is missing or invalid'
      });
    }

    // Check if contact entry exists
    const contact = await Contact.findById(id);
    
    if (!contact) {
      return res.status(404).json({
        success: false,
        message: 'Contact entry not found'
      });
    }
    
    // Don't allow deletion of the only active entry if it's active
    if (contact.isActive) {
      const count = await Contact.countDocuments();
      if (count === 1) {
        return res.status(400).json({
          success: false,
          message: 'Cannot delete the only active contact entry. Create another entry first.'
        });
      }
    }
    
    // Delete contact entry
    await Contact.findByIdAndDelete(id);
    
    logger.info(`Admin deleted contact entry ${id}`, { 
      admin: req.adminInfo.email,
      role: req.adminInfo.role
    });
    
    res.status(200).json({
      success: true,
      message: 'Contact entry deleted successfully'
    });
  } catch (error) {
    logger.error(`Error deleting contact entry: ${error.message}`, { error });
    res.status(500).json({
      success: false,
      message: 'Server error deleting contact entry',
      error: error.message
    });
  }
});

/**
 * @route   PATCH /api/admin/contact/:id/toggle-status
 * @desc    Toggle contact entry status (isActive)
 * @access  Admin only
 */
router.patch('/contact/:id/toggle-status', verifyFirebaseToken, async (req, res) => {
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
    
    const { id } = req.params;
    
    // Validate ID parameter
    if (!id || id === 'undefined' || id === 'null') {
      logger.warn(`Invalid contact ID provided: "${id}"`);
      return res.status(400).json({
        success: false,
        message: 'Invalid contact ID provided',
        error: 'ID parameter is missing or invalid'
      });
    }

    // Find contact entry by ID
    const contact = await Contact.findById(id);
    
    if (!contact) {
      return res.status(404).json({
        success: false,
        message: 'Contact entry not found'
      });
    }
    
    // Toggle isActive status
    // Note: The pre-save hook will handle making other entries inactive
    contact.isActive = !contact.isActive;
    await contact.save();
    
    logger.info(`Admin toggled contact entry status ${id} to ${contact.isActive}`, { 
      admin: req.adminInfo.email,
      role: req.adminInfo.role
    });
    
    res.status(200).json({
      success: true,
      message: `Contact entry status toggled to ${contact.isActive ? 'active' : 'inactive'}`,
      contact
    });
  } catch (error) {
    logger.error(`Error toggling contact entry status: ${error.message}`, { error });
    res.status(500).json({
      success: false,
      message: 'Server error toggling contact entry status',
      error: error.message
    });
  }
});

/**
 * @route   GET /api/admin/sponsored-ads
 * @desc    Get all sponsored ads with pagination, sorting and filtering
 * @access  Admin only
 */
router.get('/sponsored-ads', async (req, res) => {
  try {
    // Check if we're in development mode
    const isDevelopment = process.env.NODE_ENV === 'development' || process.env.SKIP_AUTH === 'true';
    
    // Skip auth verification in development mode
    if (!isDevelopment) {
      // Verify Firebase token
      if (!req.headers.authorization || !req.headers.authorization.startsWith('Bearer ')) {
        return res.status(401).json({
          success: false,
          message: 'No Firebase token provided',
          error: 'AUTH_TOKEN_MISSING'
        });
      }
      
      // Check admin role
      const { getAdminRole } = require('../config/adminConfig');
      if (!req.user || !req.user.email || !getAdminRole(req.user.email)) {
        return res.status(403).json({
          success: false,
          message: 'User is not authorized for admin access',
          error: 'AUTH_ADMIN_REQUIRED'
        });
      }
    } else {
      // In development mode, set mock admin info
      req.adminInfo = {
        email: 'dev@example.com',
        role: 'superAdmin'
      };
    }
    
    // Pagination parameters
    const page = parseInt(req.query.page) || 1;
    const limit = parseInt(req.query.limit) || 10;
    const skip = (page - 1) * limit;
    
    // Sorting parameters
    const sortField = req.query.sort || 'createdAt';
    const sortOrder = req.query.order === 'asc' ? 1 : -1;
    const sort = { [sortField]: sortOrder };
    
    // Filtering parameters
    const filter = {};
    
    // Filter by status
    if (req.query.status === 'true' || req.query.status === true) {
      filter.status = true;
    } else if (req.query.status === 'false' || req.query.status === false) {
      filter.status = false;
    }
    
    // Filter by location
    if (req.query.location) {
      filter.location = req.query.location;
    }
    
    // Filter by search query (title, description)
    if (req.query.q) {
      const searchQuery = req.query.q;
      filter.$or = [
        { title: { $regex: searchQuery, $options: 'i' } },
        { description: { $regex: searchQuery, $options: 'i' } }
      ];
    }
    
    // Execute query with pagination and filters
    const sponsoredAds = await SponsoredAd.find(filter)
      .sort(sort)
      .skip(skip)
      .limit(limit)
      .populate('uid', 'email displayName');
    
    // Get total count for pagination
    const totalAds = await SponsoredAd.countDocuments(filter);
    const totalPages = Math.ceil(totalAds / limit);
    
    if (req.adminInfo) {
      logger.info(`Admin sponsored ads list retrieved by ${req.adminInfo.email}`, {
        page,
        limit,
        totalAds,
        filters: JSON.stringify(filter)
      });
    }
    
    res.status(200).json({
      success: true,
      sponsoredAds,
      pagination: {
        total: totalAds,
        page,
        limit,
        totalPages
      }
    });
  } catch (error) {
    logger.error(`Error retrieving sponsored ads: ${error.message}`, { error });
    res.status(500).json({
      success: false,
      message: 'Server error retrieving sponsored ads',
      error: error.message
    });
  }
});

/**
 * @route   GET /api/admin/sponsored-ads/:id
 * @desc    Get a single sponsored ad by ID
 * @access  Admin only
 */
router.get('/sponsored-ads/:id', async (req, res) => {
  try {
    // Check if we're in development mode
    const isDevelopment = process.env.NODE_ENV === 'development' || process.env.SKIP_AUTH === 'true';
    
    // Skip auth verification in development mode
    if (!isDevelopment) {
      // Verify Firebase token
      if (!req.headers.authorization || !req.headers.authorization.startsWith('Bearer ')) {
        return res.status(401).json({
          success: false,
          message: 'No Firebase token provided',
          error: 'AUTH_TOKEN_MISSING'
        });
      }
      
      // Check admin role
      const { getAdminRole } = require('../config/adminConfig');
      if (!req.user || !req.user.email || !getAdminRole(req.user.email)) {
        return res.status(403).json({
          success: false,
          message: 'User is not authorized for admin access',
          error: 'AUTH_ADMIN_REQUIRED'
        });
      }
    } else {
      // In development mode, set mock admin info
      req.adminInfo = {
        email: 'dev@example.com',
        role: 'superAdmin'
      };
    }
    
    const { id } = req.params;
    
    // Log the ID for debugging
    console.log(`Received GET request for sponsored ad ID: ${id}`);
    
    if (!mongoose.Types.ObjectId.isValid(id)) {
      return res.status(400).json({
        success: false,
        message: 'Invalid sponsored ad ID'
      });
    }
    
    const sponsoredAd = await SponsoredAd.findById(id)
      .populate('uid', 'email displayName');
    
    if (!sponsoredAd) {
      return res.status(404).json({
        success: false,
        message: 'Sponsored ad not found'
      });
    }
    
    if (req.adminInfo) {
      logger.info(`Admin sponsored ad detail retrieved by ${req.adminInfo.email}`, {
        id,
        title: sponsoredAd.title
      });
    }
    
    res.status(200).json({
      success: true,
      sponsoredAd
    });
  } catch (error) {
    logger.error(`Error retrieving sponsored ad: ${error.message}`, { error });
    res.status(500).json({
      success: false,
      message: 'Server error retrieving sponsored ad',
      error: error.message
    });
  }
});

/**
 * @route   POST /api/admin/sponsored-ads
 * @desc    Create a new sponsored ad
 * @access  Admin only
 */
router.post('/sponsored-ads', async (req, res) => {
  try {
    // Check if we're in development mode
    const isDevelopment = process.env.NODE_ENV === 'development' || process.env.SKIP_AUTH === 'true';
    
    // Skip auth verification in development mode
    if (!isDevelopment) {
      // Verify Firebase token
      if (!req.headers.authorization || !req.headers.authorization.startsWith('Bearer ')) {
        return res.status(401).json({
          success: false,
          message: 'No Firebase token provided',
          error: 'AUTH_TOKEN_MISSING'
        });
      }
      
      // Check admin role
      const { getAdminRole } = require('../config/adminConfig');
      if (!req.user || !req.user.email || !getAdminRole(req.user.email)) {
        return res.status(403).json({
          success: false,
          message: 'User is not authorized for admin access',
          error: 'AUTH_ADMIN_REQUIRED'
        });
      }
    } else {
      // In development mode, set mock admin info
      req.adminInfo = {
        email: 'dev@example.com',
        role: 'superAdmin'
      };
    }
    
    const newSponsoredAd = new SponsoredAd(req.body);
    
    await newSponsoredAd.save();
    
    if (req.adminInfo) {
      logger.info(`Admin created new sponsored ad: ${newSponsoredAd.title}`, {
        admin: req.adminInfo.email,
        id: newSponsoredAd._id
      });
    }
    
    res.status(201).json({
      success: true,
      message: 'Sponsored ad created successfully',
      sponsoredAd: newSponsoredAd
    });
  } catch (error) {
    logger.error(`Error creating sponsored ad: ${error.message}`, { error });
    res.status(500).json({
      success: false,
      message: 'Server error creating sponsored ad',
      error: error.message
    });
  }
});

/**
 * @route   PUT /api/admin/sponsored-ads/:id
 * @desc    Update a sponsored ad
 * @access  Admin only
 */
router.put('/sponsored-ads/:id', async (req, res) => {
  try {
    // Check if we're in development mode
    const isDevelopment = process.env.NODE_ENV === 'development' || process.env.SKIP_AUTH === 'true';
    
    // Skip auth verification in development mode
    if (!isDevelopment) {
      // Verify Firebase token
      if (!req.headers.authorization || !req.headers.authorization.startsWith('Bearer ')) {
        return res.status(401).json({
          success: false,
          message: 'No Firebase token provided',
          error: 'AUTH_TOKEN_MISSING'
        });
      }
      
      // Check admin role
      const { getAdminRole } = require('../config/adminConfig');
      if (!req.user || !req.user.email || !getAdminRole(req.user.email)) {
        return res.status(403).json({
          success: false,
          message: 'User is not authorized for admin access',
          error: 'AUTH_ADMIN_REQUIRED'
        });
      }
    } else {
      // In development mode, set mock admin info
      req.adminInfo = {
        email: 'dev@example.com',
        role: 'superAdmin'
      };
    }
    
    const { id } = req.params;
    
    // Log the ID for debugging
    console.log(`Received PUT request for sponsored ad ID: ${id}`);
    
    if (!mongoose.Types.ObjectId.isValid(id)) {
      return res.status(400).json({
        success: false,
        message: 'Invalid sponsored ad ID'
      });
    }
    
    const updatedSponsoredAd = await SponsoredAd.findByIdAndUpdate(
      id,
      req.body,
      { new: true, runValidators: true }
    );
    
    if (!updatedSponsoredAd) {
      return res.status(404).json({
        success: false,
        message: 'Sponsored ad not found'
      });
    }
    
    if (req.adminInfo) {
      logger.info(`Admin updated sponsored ad: ${updatedSponsoredAd.title}`, {
        admin: req.adminInfo.email,
        id
      });
    }
    
    res.status(200).json({
      success: true,
      message: 'Sponsored ad updated successfully',
      sponsoredAd: updatedSponsoredAd
    });
  } catch (error) {
    logger.error(`Error updating sponsored ad: ${error.message}`, { error });
    res.status(500).json({
      success: false,
      message: 'Server error updating sponsored ad',
      error: error.message
    });
  }
});

/**
 * @route   DELETE /api/admin/sponsored-ads/:id
 * @desc    Delete a sponsored ad
 * @access  Admin only
 */
router.delete('/sponsored-ads/:id', async (req, res) => {
  try {
    // Check if we're in development mode
    const isDevelopment = process.env.NODE_ENV === 'development' || process.env.SKIP_AUTH === 'true';
    
    // Skip auth verification in development mode
    if (!isDevelopment) {
      // Verify Firebase token
      if (!req.headers.authorization || !req.headers.authorization.startsWith('Bearer ')) {
        return res.status(401).json({
          success: false,
          message: 'No Firebase token provided',
          error: 'AUTH_TOKEN_MISSING'
        });
      }
      
      // Check admin role
      const { getAdminRole } = require('../config/adminConfig');
      if (!req.user || !req.user.email || !getAdminRole(req.user.email)) {
        return res.status(403).json({
          success: false,
          message: 'User is not authorized for admin access',
          error: 'AUTH_ADMIN_REQUIRED'
        });
      }
    } else {
      // In development mode, set mock admin info
      req.adminInfo = {
        email: 'dev@example.com',
        role: 'superAdmin'
      };
    }
    
    const { id } = req.params;
    
    // Log the ID for debugging
    console.log(`Received DELETE request for sponsored ad ID: ${id}`);
    
    if (!mongoose.Types.ObjectId.isValid(id)) {
      return res.status(400).json({
        success: false,
        message: 'Invalid sponsored ad ID'
      });
    }
    
    const deletedSponsoredAd = await SponsoredAd.findByIdAndDelete(id);
    
    if (!deletedSponsoredAd) {
      return res.status(404).json({
        success: false,
        message: 'Sponsored ad not found'
      });
    }
    
    if (req.adminInfo) {
      logger.info(`Admin deleted sponsored ad: ${deletedSponsoredAd.title}`, {
        admin: req.adminInfo.email,
        id
      });
    }
    
    res.status(200).json({
      success: true,
      message: 'Sponsored ad deleted successfully'
    });
  } catch (error) {
    logger.error(`Error deleting sponsored ad: ${error.message}`, { error });
    res.status(500).json({
      success: false,
      message: 'Server error deleting sponsored ad',
      error: error.message
    });
  }
});

/**
 * @route   PATCH /api/admin/sponsored-ads/:id/toggle-status
 * @desc    Toggle the status of a sponsored ad
 * @access  Admin only
 */
router.patch('/sponsored-ads/:id/toggle-status', async (req, res) => {
  try {
    // Check if we're in development mode
    const isDevelopment = process.env.NODE_ENV === 'development' || process.env.SKIP_AUTH === 'true';
    
    // Skip auth verification in development mode
    if (!isDevelopment) {
      // Verify Firebase token
      if (!req.headers.authorization || !req.headers.authorization.startsWith('Bearer ')) {
        return res.status(401).json({
          success: false,
          message: 'No Firebase token provided',
          error: 'AUTH_TOKEN_MISSING'
        });
      }
      
      // Check admin role
      const { getAdminRole } = require('../config/adminConfig');
      if (!req.user || !req.user.email || !getAdminRole(req.user.email)) {
        return res.status(403).json({
          success: false,
          message: 'User is not authorized for admin access',
          error: 'AUTH_ADMIN_REQUIRED'
        });
      }
    } else {
      // In development mode, set mock admin info
      req.adminInfo = {
        email: 'dev@example.com',
        role: 'superAdmin'
      };
    }
    
    const { id } = req.params;
    
    // Log the ID for debugging
    console.log(`Received toggle request for ID: ${id}`);
    
    if (!mongoose.Types.ObjectId.isValid(id)) {
      return res.status(400).json({
        success: false,
        message: 'Invalid sponsored ad ID'
      });
    }
    
    // First find the sponsored ad
    const sponsoredAd = await SponsoredAd.findById(id);
    
    if (!sponsoredAd) {
      return res.status(404).json({
        success: false,
        message: 'Sponsored ad not found'
      });
    }
    
    // Toggle the status
    sponsoredAd.status = !sponsoredAd.status;
    
    // Use findByIdAndUpdate instead of save to avoid validation errors
    const updatedAd = await SponsoredAd.findByIdAndUpdate(
      id,
      { status: sponsoredAd.status },
      { new: true, runValidators: false }
    );
    
    if (!updatedAd) {
      return res.status(500).json({
        success: false,
        message: 'Failed to update sponsored ad status'
      });
    }
    
    if (req.adminInfo) {
      logger.info(`Admin toggled sponsored ad status: ${sponsoredAd.title}`, {
        admin: req.adminInfo.email,
        id,
        newStatus: updatedAd.status
      });
    }
    
    res.status(200).json({
      success: true,
      message: `Sponsored ad ${updatedAd.status ? 'activated' : 'deactivated'} successfully`,
      status: updatedAd.status
    });
  } catch (error) {
    logger.error(`Error toggling sponsored ad status: ${error.message}`, { error });
    res.status(500).json({
      success: false,
      message: 'Server error toggling sponsored ad status',
      error: error.message
    });
  }
});

/**
 * @route   GET /api/admin/push-notifications
 * @desc    Get all push notifications with pagination, sorting and filtering
 * @access  Admin only
 */
router.get('/push-notifications', verifyFirebaseToken, verifyAdmin, async (req, res) => {
  try {
    console.log('🔍 GET /api/admin/push-notifications - Request received');
    console.log('MongoDB connection state:', mongoose.connection.readyState);
    // 0 = disconnected, 1 = connected, 2 = connecting, 3 = disconnecting
    
    // Pagination parameters
    const page = parseInt(req.query.page) || 1;
    const limit = parseInt(req.query.limit) || 20;
    const skip = (page - 1) * limit;
    
    // Sorting parameters
    const sortField = req.query.sort || 'createdAt';
    const sortOrder = req.query.order === 'asc' ? 1 : -1;
    const sort = { [sortField]: sortOrder };
    
    // Filtering parameters
    const filter = {};
    
    // Filter by status
    if (req.query.status) {
      filter.status = req.query.status;
    }
    
    // Filter by type
    if (req.query.type) {
      filter.type = req.query.type;
    }
    
    // Filter by topic
    if (req.query.topic) {
      filter.topic = req.query.topic;
    }
    
    // Filter by search query (on title, body)
    if (req.query.q) {
      const searchQuery = req.query.q;
      filter.$or = [
        { title: { $regex: searchQuery, $options: 'i' } },
        { body: { $regex: searchQuery, $options: 'i' } }
      ];
    }
    
    console.log('MongoDB query:', { filter, sort, skip, limit });
    
    // Check if PushNotification model exists
    console.log('PushNotification model exists:', !!PushNotification);
    
    // Check PushNotification collection
    try {
      const collections = await mongoose.connection.db.listCollections().toArray();
      const collectionNames = collections.map(c => c.name);
      console.log('Available collections:', collectionNames);
      console.log('PushNotification collection exists:', collectionNames.includes('pushnotifications'));
    } catch (collError) {
      console.error('Error checking collections:', collError);
    }
    
    // Execute query with pagination and filters
    const notifications = await PushNotification.find(filter)
      .sort(sort)
      .skip(skip)
      .limit(limit);
    
    console.log(`Found ${notifications.length} notifications`);
    
    // Get total count for pagination
    const totalNotifications = await PushNotification.countDocuments(filter);
    const totalPages = Math.ceil(totalNotifications / limit);
    
    console.log(`Total notifications: ${totalNotifications}, Total pages: ${totalPages}`);
    
    logger.info(`Admin push notifications list retrieved by ${req.adminInfo.email}`, {
      page,
      limit,
      totalNotifications,
      filters: JSON.stringify(filter)
    });
    
    res.status(200).json({
      success: true,
      notifications,
      pagination: {
        total: totalNotifications,
        page,
        limit,
        totalPages
      }
    });
  } catch (error) {
    console.error(`❌ Error retrieving push notifications:`, error);
    logger.error(`Error retrieving push notifications: ${error.message}`, { error });
    res.status(500).json({
      success: false,
      message: 'Server error retrieving push notifications',
      error: error.message
    });
  }
});

/**
 * @route   GET /api/admin/push-notifications/stats
 * @desc    Get push notification statistics
 * @access  Admin only
 */
router.get('/push-notifications/stats', verifyFirebaseToken, verifyAdmin, async (req, res) => {
  try {
    console.log('🔍 GET /api/admin/push-notifications/stats - Request received');
    console.log('MongoDB connection state:', mongoose.connection.readyState);
    
    // Get counts of notifications by status
    const total = await PushNotification.countDocuments();
    console.log(`Total notifications count: ${total}`);
    
    const sent = await PushNotification.countDocuments({ status: 'SENT' });
    console.log(`Sent notifications count: ${sent}`);
    
    const pending = await PushNotification.countDocuments({ 
      status: { $in: ['DRAFT', 'SCHEDULED'] } 
    });
    console.log(`Pending notifications count: ${pending}`);
    
    const failed = await PushNotification.countDocuments({ status: 'FAILED' });
    console.log(`Failed notifications count: ${failed}`);
    
    // Calculate success rate
    let successRate = 0;
    if (sent + failed > 0) {
      successRate = Math.round((sent / (sent + failed)) * 100);
    }
    console.log(`Success rate: ${successRate}%`);
    
    // Get recent notifications for trends
    const recentNotifications = await PushNotification.find()
      .sort({ createdAt: -1 })
      .limit(5)
      .select('title type status stats.total stats.success stats.failure');
    
    console.log(`Recent notifications count: ${recentNotifications.length}`);
    
    logger.info(`Admin retrieved push notification stats`, { 
      admin: req.adminInfo.email
    });
    
    res.status(200).json({
      success: true,
      stats: {
        total,
        sent,
        pending,
        failed,
        successRate
      },
      recentNotifications
    });
  } catch (error) {
    console.error(`❌ Error getting push notification stats:`, error);
    logger.error(`Error getting push notification stats: ${error.message}`, { error });
    res.status(500).json({
      success: false,
      message: 'Error getting push notification stats',
      error: error.message
    });
  }
});

/**
 * @route   GET /api/admin/push-notifications/topics
 * @desc    Get all topics with subscriber counts
 * @access  Admin only
 */
router.get('/push-notifications/topics', verifyFirebaseToken, verifyAdmin, async (req, res) => {
  try {
    console.log('🔍 GET /api/admin/push-notifications/topics - Request received');
    console.log('MongoDB connection state:', mongoose.connection.readyState);
    
    // Check if User model exists
    console.log('User model exists:', !!User);
    
    // Check User collection
    try {
      const collections = await mongoose.connection.db.listCollections().toArray();
      const collectionNames = collections.map(c => c.name);
      console.log('Available collections:', collectionNames);
      console.log('User collection exists:', collectionNames.includes('users'));
    } catch (collError) {
      console.error('Error checking collections:', collError);
    }
    
    // Aggregate to get unique topics and counts
    const topics = await User.aggregate([
      // Unwind the fcmTokens array
      { $unwind: "$fcmTokens" },
      // Unwind the subscribedTopics array within each fcmToken
      { $unwind: "$fcmTokens.subscribedTopics" },
      // Group by topic and count
      { 
        $group: { 
          _id: "$fcmTokens.subscribedTopics", 
          subscriberCount: { $sum: 1 } 
        } 
      },
      // Format the output
      { 
        $project: { 
          _id: 0, 
          name: "$_id", 
          subscriberCount: 1 
        } 
      },
      // Sort by subscriber count descending
      { $sort: { subscriberCount: -1 } }
    ]);
    
    console.log(`Found ${topics.length} topics`);
    console.log('Topics data:', topics);
    
    logger.info(`Admin retrieved topics with counts`, { 
      admin: req.adminInfo.email,
      topicCount: topics.length
    });
    
    res.status(200).json({
      success: true,
      topics
    });
  } catch (error) {
    console.error(`❌ Error getting topics with counts:`, error);
    logger.error(`Error getting topics with counts: ${error.message}`, { error });
    res.status(500).json({
      success: false,
      message: 'Error getting topics with counts',
      error: error.message
    });
  }
});

/**
 * @route   GET /api/admin/push-notifications/:id
 * @desc    Get a single push notification by ID
 * @access  Admin only
 */
router.get('/push-notifications/:id', verifyFirebaseToken, verifyAdmin, async (req, res) => {
  try {
    const notification = await PushNotification.findById(req.params.id);
    
    if (!notification) {
      return res.status(404).json({
        success: false,
        message: 'Push notification not found'
      });
    }
    
    res.status(200).json({
      success: true,
      notification
    });
  } catch (error) {
    logger.error(`Error retrieving push notification: ${error.message}`, { error });
    res.status(500).json({
      success: false,
      message: 'Server error retrieving push notification',
      error: error.message
    });
  }
});

/**
 * @route   POST /api/admin/push-notifications
 * @desc    Create a new push notification
 * @access  Admin only
 */
router.post('/push-notifications', verifyFirebaseToken, verifyAdmin, async (req, res) => {
  try {
    const { title, body, imageUrl, data, type, topic, targetUserIds, scheduledFor } = req.body;
    
    // Validate required fields
    if (!title || !body || !type) {
      return res.status(400).json({
        success: false,
        message: 'Title, body, and type are required fields'
      });
    }
    
    // Validate type-specific fields
    if (type === 'TOPIC' && !topic) {
      return res.status(400).json({
        success: false,
        message: 'Topic is required for TOPIC type notifications'
      });
    }
    
    if (type === 'PERSONALIZED' && (!targetUserIds || !targetUserIds.length)) {
      return res.status(400).json({
        success: false,
        message: 'Target user IDs are required for PERSONALIZED type notifications'
      });
    }
    
    // Create new notification
    const notification = new PushNotification({
      title,
      body,
      imageUrl,
      data: data || {},
      type,
      topic: type === 'TOPIC' ? topic : null,
      targetUserIds: type === 'PERSONALIZED' ? targetUserIds : [],
      status: scheduledFor ? 'SCHEDULED' : 'DRAFT',
      scheduledFor,
      createdBy: req.adminInfo.email
    });
    
    await notification.save();
    
    logger.info(`Push notification created by ${req.adminInfo.email}`, {
      notificationId: notification._id,
      type,
      status: notification.status
    });
    
    res.status(201).json({
      success: true,
      notification
    });
  } catch (error) {
    logger.error(`Error creating push notification: ${error.message}`, { error });
    res.status(500).json({
      success: false,
      message: 'Server error creating push notification',
      error: error.message
    });
  }
});

/**
 * @route   PUT /api/admin/push-notifications/:id
 * @desc    Update a push notification
 * @access  Admin only
 */
router.put('/push-notifications/:id', verifyFirebaseToken, verifyAdmin, async (req, res) => {
  try {
    const { title, body, imageUrl, data, type, topic, targetUserIds, scheduledFor, status } = req.body;
    
    // Find notification
    const notification = await PushNotification.findById(req.params.id);
    
    if (!notification) {
      return res.status(404).json({
        success: false,
        message: 'Push notification not found'
      });
    }
    
    // Only allow updating if not already sent
    if (notification.status === 'SENT') {
      return res.status(400).json({
        success: false,
        message: 'Cannot update a notification that has already been sent'
      });
    }
    
    // Update fields
    if (title) notification.title = title;
    if (body) notification.body = body;
    if (imageUrl !== undefined) notification.imageUrl = imageUrl;
    if (data) notification.data = data;
    
    // Update type-specific fields
    if (type) {
      notification.type = type;
      
      if (type === 'TOPIC') {
        if (!topic) {
          return res.status(400).json({
            success: false,
            message: 'Topic is required for TOPIC type notifications'
          });
        }
        notification.topic = topic;
        notification.targetUserIds = [];
      } else if (type === 'PERSONALIZED') {
        if (!targetUserIds || !targetUserIds.length) {
          return res.status(400).json({
            success: false,
            message: 'Target user IDs are required for PERSONALIZED type notifications'
          });
        }
        notification.targetUserIds = targetUserIds;
        notification.topic = null;
      } else {
        // BULK type
        notification.topic = null;
        notification.targetUserIds = [];
      }
    }
    
    // Update scheduling
    if (scheduledFor !== undefined) {
      notification.scheduledFor = scheduledFor;
      if (scheduledFor && notification.status === 'DRAFT') {
        notification.status = 'SCHEDULED';
      }
    }
    
    // Update status if provided
    if (status) {
      // Only allow certain status transitions
      const validTransitions = {
        'DRAFT': ['SCHEDULED', 'SENDING'],
        'SCHEDULED': ['DRAFT', 'SENDING'],
        'SENDING': ['SENT', 'FAILED'],
        'FAILED': ['DRAFT', 'SCHEDULED', 'SENDING']
      };
      
      if (!validTransitions[notification.status] || !validTransitions[notification.status].includes(status)) {
        return res.status(400).json({
          success: false,
          message: `Invalid status transition from ${notification.status} to ${status}`
        });
      }
      
      notification.status = status;
    }
    
    await notification.save();
    
    logger.info(`Push notification updated by ${req.adminInfo.email}`, {
      notificationId: notification._id,
      status: notification.status
    });
    
    res.status(200).json({
      success: true,
      notification
    });
  } catch (error) {
    logger.error(`Error updating push notification: ${error.message}`, { error });
    res.status(500).json({
      success: false,
      message: 'Server error updating push notification',
      error: error.message
    });
  }
});

/**
 * @route   DELETE /api/admin/push-notifications/:id
 * @desc    Delete a push notification
 * @access  Admin only
 */
router.delete('/push-notifications/:id', verifyFirebaseToken, verifyAdmin, async (req, res) => {
  try {
    const notification = await PushNotification.findById(req.params.id);
    
    if (!notification) {
      return res.status(404).json({
        success: false,
        message: 'Push notification not found'
      });
    }
    
    // Only allow deleting if not already sent
    if (notification.status === 'SENT') {
      return res.status(400).json({
        success: false,
        message: 'Cannot delete a notification that has already been sent'
      });
    }
    
    await notification.deleteOne();
    
    logger.info(`Push notification deleted by ${req.adminInfo.email}`, {
      notificationId: req.params.id
    });
    
    res.status(200).json({
      success: true,
      message: 'Push notification deleted successfully'
    });
  } catch (error) {
    logger.error(`Error deleting push notification: ${error.message}`, { error });
    res.status(500).json({
      success: false,
      message: 'Server error deleting push notification',
      error: error.message
    });
  }
});

/**
 * @route   POST /api/admin/push-notifications/:id/send
 * @desc    Send a push notification immediately
 * @access  Admin only
 */
router.post('/push-notifications/:id/send', verifyFirebaseToken, verifyAdmin, async (req, res) => {
  try {
    const notification = await PushNotification.findById(req.params.id);
    
    if (!notification) {
      return res.status(404).json({
        success: false,
        message: 'Push notification not found'
      });
    }
    
    // Only allow sending if in DRAFT or SCHEDULED status
    if (!['DRAFT', 'SCHEDULED'].includes(notification.status)) {
      return res.status(400).json({
        success: false,
        message: `Cannot send notification in ${notification.status} status`
      });
    }
    
    // Update status to SENDING
    notification.status = 'SENDING';
    await notification.save();
    
    // Process notification in background
    processNotification(notification._id, req.adminInfo.email)
      .then(() => {
        logger.info(`Push notification processing completed for ${notification._id}`);
      })
      .catch(error => {
        logger.error(`Error processing notification ${notification._id}: ${error.message}`, { error });
      });
    
    res.status(200).json({
      success: true,
      message: 'Push notification sending started',
      notification
    });
  } catch (error) {
    logger.error(`Error sending push notification: ${error.message}`, { error });
    res.status(500).json({
      success: false,
      message: 'Server error sending push notification',
      error: error.message
    });
  }
});

// Helper function to process notifications
async function processNotification(notificationId, adminEmail) {
  const pushNotificationService = require('../services/pushNotificationService');
  
  try {
    // Get notification
    const notification = await PushNotification.findById(notificationId);
    if (!notification) {
      logger.error(`Notification not found for processing: ${notificationId}`);
      return;
    }
    
    let tokens = [];
    let result;
    
    // Get tokens based on notification type
    switch (notification.type) {
      case 'BULK':
        tokens = await pushNotificationService.getAllTokens();
        break;
      case 'TOPIC':
        // For topics, we can either send directly to topic or get tokens subscribed to topic
        result = await pushNotificationService.sendToTopic(notification.topic, {
          title: notification.title,
          body: notification.body,
          imageUrl: notification.imageUrl
        }, notification.data);
        
        // Update notification status
        notification.status = result.success ? 'SENT' : 'FAILED';
        notification.sentAt = new Date();
        await notification.save();
        
        logger.info(`Topic notification sent: ${notification._id}`, { 
          result,
          topic: notification.topic
        });
        
        return;
      case 'PERSONALIZED':
        tokens = await pushNotificationService.getTokensByUsers(notification.targetUserIds);
        break;
    }
    
    // If no tokens found
    if (!tokens.length) {
      notification.status = 'FAILED';
      notification.stats = {
        total: 0,
        success: 0,
        failure: 0
      };
      notification.sentAt = new Date();
      await notification.save();
      
      logger.warn(`No tokens found for notification: ${notification._id}`);
      return;
    }
    
    // Send to tokens
    result = await pushNotificationService.sendToTokens(tokens, {
      title: notification.title,
      body: notification.body,
      imageUrl: notification.imageUrl
    }, notification.data);
    
    // Update notification status and stats
    notification.status = result.success ? 'SENT' : 'FAILED';
    notification.stats = {
      total: tokens.length,
      success: result.successCount || 0,
      failure: result.failureCount || 0
    };
    notification.sentAt = new Date();
    await notification.save();
    
    logger.info(`Push notification processed: ${notification._id}`, { 
      type: notification.type,
      tokens: tokens.length,
      success: result.successCount,
      failure: result.failureCount
    });
  } catch (error) {
    logger.error(`Error processing notification ${notificationId}: ${error.message}`, { error });
    
    // Update notification status to FAILED
    try {
      await PushNotification.findByIdAndUpdate(notificationId, {
        status: 'FAILED',
        sentAt: new Date()
      });
    } catch (updateError) {
      logger.error(`Error updating notification status: ${updateError.message}`);
    }
  }
}

/**
 * @route   POST /api/admin/push-notifications/personalized
 * @desc    Create and send personalized notifications with placeholders
 * @access  Admin only
 */
router.post('/push-notifications/personalized', verifyFirebaseToken, verifyAdmin, async (req, res) => {
  try {
    const { title, body, imageUrl, deepLink, targetUserIds, personalizationKeys, scheduledFor, notificationType = 'GENERAL', data = {} } = req.body;
    
    // Validate required fields
    if (!title || !body || !targetUserIds || !Array.isArray(targetUserIds) || targetUserIds.length === 0) {
      return res.status(400).json({
        success: false,
        message: 'Missing required fields',
        error: 'title, body, and targetUserIds are required'
      });
    }
    
    // Validate personalization keys if provided
    if (personalizationKeys && (!Array.isArray(personalizationKeys) || personalizationKeys.some(k => typeof k !== 'string'))) {
      return res.status(400).json({
        success: false,
        message: 'Invalid personalization keys',
        error: 'personalizationKeys must be an array of strings'
      });
    }
    
    // Extract personalization keys from title and body if not provided
    const extractedKeys = [];
    const placeholderRegex = /\{\{([^}]+)\}\}/g;
    let match;
    
    // Extract from title
    while ((match = placeholderRegex.exec(title)) !== null) {
      extractedKeys.push(match[1]);
    }
    
    // Extract from body
    placeholderRegex.lastIndex = 0; // Reset regex index
    while ((match = placeholderRegex.exec(body)) !== null) {
      extractedKeys.push(match[1]);
    }
    
    // Use provided keys or extracted keys
    const finalPersonalizationKeys = personalizationKeys || [...new Set(extractedKeys)];
    
    // Create notification
    const notification = new PushNotification({
      title,
      body,
      imageUrl,
      deepLink,
      type: 'PERSONALIZED',
      notificationType,
      personalizationKeys: finalPersonalizationKeys,
      targetUserIds,
      status: scheduledFor ? 'SCHEDULED' : 'DRAFT',
      scheduledFor: scheduledFor ? new Date(scheduledFor) : null,
      createdBy: req.user.email,
      data
    });
    
    await notification.save();
    
    logger.info(`Admin created personalized notification: ${notification._id}`, {
      admin: req.user.email,
      notificationId: notification._id
    });
    
    // Send immediately if no schedule is set
    if (!scheduledFor) {
      const pushNotificationService = require('../services/pushNotificationService');
      
      // Process in background to avoid timeout
      processNotification(notification._id, req.user.email);
      
      res.status(201).json({
        success: true,
        message: 'Personalized notification created and sending started',
        notification
      });
    } else {
      res.status(201).json({
        success: true,
        message: 'Personalized notification scheduled',
        notification
      });
    }
  } catch (error) {
    logger.error(`Error creating personalized notification: ${error.message}`, { error });
    res.status(500).json({
      success: false,
      message: 'Server error creating personalized notification',
      error: error.message
    });
  }
});

/**
 * @route   POST /api/admin/push-notifications/preview-personalized
 * @desc    Preview a personalized notification with sample data
 * @access  Admin only
 */
router.post('/push-notifications/preview-personalized', verifyFirebaseToken, verifyAdmin, async (req, res) => {
  try {
    const { title, body, personalizationKeys } = req.body;
    
    // Validate required fields
    if (!title || !body) {
      return res.status(400).json({
        success: false,
        message: 'Missing required fields',
        error: 'title and body are required'
      });
    }
    
    const pushNotificationService = require('../services/pushNotificationService');
    
    // Create sample user data for preview
    const sampleUserData = {
      _id: 'sample-user-id',
      displayName: 'Sample User',
      email: 'sample@example.com',
      preferredLanguage: 'English',
      lastOnline: new Date().toISOString(),
      categories: [
        { category: 'Birthday', visitCount: 5 },
        { category: 'Wedding', visitCount: 3 }
      ],
      favorites: ['template-1', 'template-2'],
      topicSubscriptions: ['birthday', 'wedding']
    };
    
    // Add custom sample data for provided personalization keys
    if (personalizationKeys && Array.isArray(personalizationKeys)) {
      personalizationKeys.forEach(key => {
        if (!sampleUserData[key]) {
          sampleUserData[key] = `Sample ${key}`;
        }
      });
    }
    
    // Process placeholders
    const processedTitle = pushNotificationService.processPlaceholders(title, sampleUserData);
    const processedBody = pushNotificationService.processPlaceholders(body, sampleUserData);
    
    res.status(200).json({
      success: true,
      preview: {
        title: processedTitle,
        body: processedBody,
        originalTitle: title,
        originalBody: body,
        sampleUserData
      }
    });
  } catch (error) {
    logger.error(`Error previewing personalized notification: ${error.message}`, { error });
    res.status(500).json({
      success: false,
      message: 'Server error previewing personalized notification',
      error: error.message
    });
  }
});

/**
 * @route   POST /api/admin/push-notifications/inactivity
 * @desc    Manually trigger inactivity notifications
 * @access  Admin only
 */
router.post('/push-notifications/inactivity', verifyFirebaseToken, verifyAdmin, async (req, res) => {
  try {
    const { inactivityThresholdDays = 3 } = req.body;
    
    // Validate inactivityThresholdDays
    if (isNaN(inactivityThresholdDays) || inactivityThresholdDays < 1) {
      return res.status(400).json({
        success: false,
        message: 'Invalid inactivityThresholdDays parameter',
        error: 'inactivityThresholdDays must be a positive number'
      });
    }
    
    const inactivityNotificationsJob = require('../jobs/inactivityNotifications');
    
    // Process in background to avoid timeout
    const jobPromise = inactivityNotificationsJob.runInactivityNotificationsJob({
      inactivityThresholdDays,
      forceRun: true,
      triggeredBy: req.user.email
    });
    
    // Respond immediately
    res.status(200).json({
      success: true,
      message: 'Inactivity notifications job started',
      inactivityThresholdDays
    });
    
    // Log result when job completes
    jobPromise.then(result => {
      logger.info(`Inactivity notifications job completed: ${result.userCount || 0} users processed`, {
        admin: req.user.email,
        result
      });
    }).catch(error => {
      logger.error(`Error in inactivity notifications job: ${error.message}`, { error });
    });
    
  } catch (error) {
    logger.error(`Error triggering inactivity notifications: ${error.message}`, { error });
    res.status(500).json({
      success: false,
      message: 'Server error triggering inactivity notifications',
      error: error.message
    });
  }
});

/**
 * @route   GET /api/admin/push-notifications/user-segments
 * @desc    Get user segments for targeting notifications
 * @access  Admin only
 */
router.get('/push-notifications/user-segments', verifyFirebaseToken, verifyAdmin, async (req, res) => {
  try {
    const User = require('../models/User');
    
    // Get counts by activity level
    const now = new Date();
    const oneDayAgo = new Date(now);
    oneDayAgo.setDate(oneDayAgo.getDate() - 1);
    
    const threeDaysAgo = new Date(now);
    threeDaysAgo.setDate(threeDaysAgo.getDate() - 3);
    
    const sevenDaysAgo = new Date(now);
    sevenDaysAgo.setDate(sevenDaysAgo.getDate() - 7);
    
    const thirtyDaysAgo = new Date(now);
    thirtyDaysAgo.setDate(thirtyDaysAgo.getDate() - 30);
    
    // Get active users in different time periods
    const activeToday = await User.countDocuments({ lastOnline: { $gte: oneDayAgo } });
    const activeThreeDays = await User.countDocuments({ lastOnline: { $gte: threeDaysAgo } });
    const activeSevenDays = await User.countDocuments({ lastOnline: { $gte: sevenDaysAgo } });
    const activeThirtyDays = await User.countDocuments({ lastOnline: { $gte: thirtyDaysAgo } });
    const totalUsers = await User.countDocuments();
    
    // Get inactive users eligible for notifications
    const inactiveThreeDays = await User.countDocuments({ 
      lastOnline: { $lt: threeDaysAgo },
      isBlocked: false,
      'pushPreferences.allowPersonalPush': true,
      muteNotificationsUntil: { $lt: new Date() }
    });
    
    // Get users by topic subscriptions
    const topicCounts = await User.aggregate([
      { $unwind: '$topicSubscriptions' },
      { $group: { _id: '$topicSubscriptions', count: { $sum: 1 } } },
      { $sort: { count: -1 } },
      { $limit: 10 }
    ]);
    
    // Get users by preferred language
    const languageCounts = await User.aggregate([
      { $group: { _id: '$preferredLanguage', count: { $sum: 1 } } },
      { $sort: { count: -1 } }
    ]);
    
    // Get users by top categories
    const categoryCounts = await User.aggregate([
      { $unwind: '$categories' },
      { $group: { _id: '$categories.category', count: { $sum: 1 } } },
      { $sort: { count: -1 } },
      { $limit: 10 }
    ]);
    
    res.status(200).json({
      success: true,
      segments: {
        activity: {
          activeToday,
          activeThreeDays,
          activeSevenDays,
          activeThirtyDays,
          inactiveThreeDays,
          totalUsers
        },
        topics: topicCounts.map(topic => ({
          name: topic._id,
          count: topic.count
        })),
        languages: languageCounts.map(lang => ({
          name: lang._id || 'Not Set',
          count: lang.count
        })),
        categories: categoryCounts.map(cat => ({
          name: cat._id,
          count: cat.count
        }))
      }
    });
  } catch (error) {
    logger.error(`Error getting user segments: ${error.message}`, { error });
    res.status(500).json({
      success: false,
      message: 'Server error getting user segments',
      error: error.message
    });
  }
});

/**
 * @route   GET /api/admin/push-notifications/inactivity-config
 * @desc    Get inactivity notification configuration
 * @access  Admin only
 */
router.get('/push-notifications/inactivity-config', verifyFirebaseToken, verifyAdmin, async (req, res) => {
  try {
    const inactivityNotificationsJob = require('../jobs/inactivityNotifications');
    const config = await inactivityNotificationsJob.getInactivityConfig();
    
    res.status(200).json({
      success: true,
      config
    });
  } catch (error) {
    logger.error(`Error getting inactivity notification config: ${error.message}`, { error });
    res.status(500).json({
      success: false,
      message: 'Server error getting inactivity notification config',
      error: error.message
    });
  }
});

/**
 * @route   PUT /api/admin/push-notifications/inactivity-config
 * @desc    Update inactivity notification configuration
 * @access  Admin only
 */
router.put('/push-notifications/inactivity-config', verifyFirebaseToken, verifyAdmin, async (req, res) => {
  try {
    const { 
      inactivityThresholdDays, 
      isAutomaticSendEnabled, 
      automaticSendHour,
      notificationTitle,
      notificationBody
    } = req.body;
    
    // Validate input
    if (inactivityThresholdDays !== undefined && (isNaN(inactivityThresholdDays) || inactivityThresholdDays < 1)) {
      return res.status(400).json({
        success: false,
        message: 'Invalid inactivityThresholdDays parameter',
        error: 'inactivityThresholdDays must be a positive number'
      });
    }
    
    if (automaticSendHour !== undefined && (isNaN(automaticSendHour) || automaticSendHour < 0 || automaticSendHour > 23)) {
      return res.status(400).json({
        success: false,
        message: 'Invalid automaticSendHour parameter',
        error: 'automaticSendHour must be between 0 and 23'
      });
    }
    
    // Build update object with only provided fields
    const updateData = {};
    if (inactivityThresholdDays !== undefined) updateData.inactivityThresholdDays = inactivityThresholdDays;
    if (isAutomaticSendEnabled !== undefined) updateData.isAutomaticSendEnabled = isAutomaticSendEnabled;
    if (automaticSendHour !== undefined) updateData.automaticSendHour = automaticSendHour;
    if (notificationTitle !== undefined) updateData.notificationTitle = notificationTitle;
    if (notificationBody !== undefined) updateData.notificationBody = notificationBody;
    
    const inactivityNotificationsJob = require('../jobs/inactivityNotifications');
    const updatedConfig = await inactivityNotificationsJob.updateInactivityConfig(
      updateData, 
      req.user.email
    );
    
    // If automatic send hour was updated, restart the job with new schedule
    if (automaticSendHour !== undefined) {
      const scheduler = require('../jobs/scheduler');
      await scheduler.startInactivityNotificationsJob();
      logger.info(`Inactivity notifications job rescheduled by ${req.user.email}`);
    }
    
    res.status(200).json({
      success: true,
      message: 'Inactivity notification configuration updated',
      config: updatedConfig
    });
  } catch (error) {
    logger.error(`Error updating inactivity notification config: ${error.message}`, { error });
    res.status(500).json({
      success: false,
      message: 'Server error updating inactivity notification config',
      error: error.message
    });
  }
});

/**
 * @route   GET /api/admin/diagnostics/mongodb
 * @desc    Check MongoDB connection status and collections
 * @access  Admin only
 */
router.get('/diagnostics/mongodb', async (req, res) => {
  console.log('🔍 GET /api/admin/diagnostics/mongodb - Request received');
  
  // Set a timeout for MongoDB operations
  const OPERATION_TIMEOUT = 3000; // 3 seconds timeout
  
  // Function to create a timeout promise
  const createTimeout = (ms) => {
    return new Promise((_, reject) => {
      setTimeout(() => reject(new Error(`Operation timed out after ${ms}ms`)), ms);
    });
  };
  
  // Check MongoDB connection state
  const connectionState = mongoose.connection.readyState;
  const connectionStateText = {
    0: 'disconnected',
    1: 'connected',
    2: 'connecting',
    3: 'disconnecting'
  }[connectionState] || 'unknown';
  
  console.log(`MongoDB connection state: ${connectionState} (${connectionStateText})`);
  
  // Basic response with connection state
  const response = {
    success: true,
    mongodb: {
      connectionState,
      connectionStateText,
      dbName: mongoose.connection.name || 'Unknown',
      dbHost: mongoose.connection.host || 'Unknown',
      collections: [],
      collectionStatus: {},
      documentCounts: {}
    },
    models: {
      pushNotification: {
        exists: !!PushNotification,
        schema: {}
      },
      user: {
        exists: !!User
      }
    }
  };
  
  // If not connected, return early with connection state only
  if (connectionState !== 1) {
    response.warning = 'MongoDB is not connected';
    return res.status(200).json(response);
  }
  
  try {
    // Get collection information with timeout
    try {
      const collections = await Promise.race([
        mongoose.connection.db.listCollections().toArray(),
        createTimeout(OPERATION_TIMEOUT)
      ]);
      
      response.mongodb.collections = collections.map(c => c.name);
      console.log('Collections retrieved successfully:', response.mongodb.collections);
      
      // Check for specific collections
      const requiredCollections = [
        'pushnotifications',
        'users',
        'templates',
        'festivals',
        'admobs',
        'sponsored_ads'
      ];
      
      // Set collection status
      requiredCollections.forEach(collection => {
        response.mongodb.collectionStatus[collection] = response.mongodb.collections.includes(collection);
      });
      
      // Count documents in important collections (with individual timeouts)
      await Promise.all(requiredCollections.map(async (collection) => {
        if (response.mongodb.collectionStatus[collection]) {
          try {
            const count = await Promise.race([
              mongoose.connection.db.collection(collection).countDocuments(),
              createTimeout(OPERATION_TIMEOUT)
            ]);
            response.mongodb.documentCounts[collection] = count;
          } catch (countError) {
            console.error(`Error counting documents in ${collection}:`, countError);
            response.mongodb.documentCounts[collection] = 'Timeout counting documents';
          }
        } else {
          response.mongodb.documentCounts[collection] = 'Collection not found';
        }
      }));
    } catch (collError) {
      console.error('Error or timeout retrieving collections:', collError);
      response.warning = 'Timed out retrieving collections';
    }
    
    // Get push notification model schema (with timeout)
    if (PushNotification) {
      try {
        const schema = PushNotification.schema.paths;
        const schemaInfo = {};
        
        Object.keys(schema).forEach(path => {
          schemaInfo[path] = {
            type: schema[path].instance,
            required: schema[path].isRequired || false
          };
        });
        
        response.models.pushNotification.schema = schemaInfo;
      } catch (schemaError) {
        console.error('Error getting PushNotification schema:', schemaError);
        response.models.pushNotification.schemaError = 'Error retrieving schema';
      }
    }
    
    logger.info(`Admin checked MongoDB diagnostics`, { 
      admin: req.adminInfo?.email || 'unknown'
    });
    
    return res.status(200).json(response);
  } catch (error) {
    console.error('❌ Error checking MongoDB diagnostics:', error);
    logger.error(`Error checking MongoDB diagnostics: ${error.message}`, { error });
    
    return res.status(500).json({
      success: false,
      message: 'Error checking MongoDB diagnostics',
      error: error.message,
      errorType: error.name || 'unknown',
      mongodb: {
        connectionState: mongoose.connection.readyState,
        connectionStateText: {
          0: 'disconnected',
          1: 'connected',
          2: 'connecting',
          3: 'disconnecting'
        }[mongoose.connection.readyState] || 'unknown'
      }
    });
  }
});

// Add a simple health check endpoint
router.get('/health', (req, res) => {
  try {
    // Check MongoDB connection state
    const connectionState = mongoose.connection.readyState;
    const connectionStateText = {
      0: 'disconnected',
      1: 'connected',
      2: 'connecting',
      3: 'disconnecting'
    }[connectionState] || 'unknown';
    
    res.status(200).json({
      success: true,
      message: 'API is healthy',
      mongodb: {
        connected: connectionState === 1,
        connectionState,
        connectionStateText
      },
      timestamp: new Date().toISOString()
    });
  } catch (error) {
    console.error('Error in health check endpoint:', error);
    res.status(500).json({
      success: false,
      message: 'Error checking API health',
      error: error.message,
      timestamp: new Date().toISOString()
    });
  }
});

/**
 * @route   PUT /api/admin/sponsored-ads/:id/force-update
 * @desc    Update a sponsored ad bypassing date validation
 * @access  Admin only
 */
router.put('/sponsored-ads/:id/force-update', async (req, res) => {
  try {
    // Check if we're in development mode
    const isDevelopment = process.env.NODE_ENV === 'development' || process.env.SKIP_AUTH === 'true';
    
    // Skip auth verification in development mode
    if (!isDevelopment) {
      // Verify Firebase token
      if (!req.headers.authorization || !req.headers.authorization.startsWith('Bearer ')) {
        return res.status(401).json({
          success: false,
          message: 'No Firebase token provided',
          error: 'AUTH_TOKEN_MISSING'
        });
      }
      
      // Check admin role
      const { getAdminRole } = require('../config/adminConfig');
      if (!req.user || !req.user.email || !getAdminRole(req.user.email)) {
        return res.status(403).json({
          success: false,
          message: 'User is not authorized for admin access',
          error: 'AUTH_ADMIN_REQUIRED'
        });
      }
    } else {
      // In development mode, set mock admin info
      req.adminInfo = {
        email: 'dev@example.com',
        role: 'superAdmin'
      };
    }
    
    const { id } = req.params;
    
    // Log the ID for debugging
    console.log(`Received PUT request for sponsored ad ID (force update): ${id}`);
    
    if (!mongoose.Types.ObjectId.isValid(id)) {
      return res.status(400).json({
        success: false,
        message: 'Invalid sponsored ad ID'
      });
    }
    
    // Get the existing ad first
    const existingAd = await SponsoredAd.findById(id);
    
    if (!existingAd) {
      return res.status(404).json({
        success: false,
        message: 'Sponsored ad not found'
      });
    }
    
    // Extract dates from the request body
    const { start_date, end_date } = req.body;
    
    // Ensure end date is after start date
    const startDate = start_date ? new Date(start_date) : existingAd.start_date;
    let endDate = end_date ? new Date(end_date) : existingAd.end_date;
    
    // If end date is not after start date, set it to 2 days after start date
    if (endDate <= startDate) {
      endDate = new Date(startDate);
      endDate.setDate(startDate.getDate() + 2);
    }
    
    // Update the request body with fixed dates
    const updatedData = {
      ...req.body,
      start_date: startDate,
      end_date: endDate
    };
    
    // Use findByIdAndUpdate with runValidators: false to bypass validation
    const updatedSponsoredAd = await SponsoredAd.findByIdAndUpdate(
      id,
      updatedData,
      { new: true, runValidators: false }
    );
    
    if (!updatedSponsoredAd) {
      return res.status(404).json({
        success: false,
        message: 'Sponsored ad not found'
      });
    }
    
    if (req.adminInfo) {
      logger.info(`Admin force updated sponsored ad: ${updatedSponsoredAd.title}`, {
        admin: req.adminInfo.email,
        id
      });
    }
    
    res.status(200).json({
      success: true,
      message: 'Sponsored ad updated successfully',
      sponsoredAd: updatedSponsoredAd
    });
  } catch (error) {
    logger.error(`Error force updating sponsored ad: ${error.message}`, { error });
    res.status(500).json({
      success: false,
      message: 'Server error updating sponsored ad',
      error: error.message
    });
  }
});

// Language routes
/**
 * @route   GET /api/admin/languages
 * @desc    Get all languages
 * @access  Admin only
 */
router.get('/languages', verifyFirebaseToken, async (req, res) => {
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
    
    // Get all languages from the main language router
    const Language = require('../models/Language');
    
    // Pagination parameters
    const page = parseInt(req.query.page) || 1;
    const limit = parseInt(req.query.limit) || 10;
    const skip = (page - 1) * limit;
    
    // Sorting parameters
    const sortField = req.query.sort || 'displayOrder';
    const sortOrder = req.query.order === 'asc' ? 1 : -1;
    const sort = { [sortField]: sortOrder };
    
    // Filtering parameters
    const filter = {};
    
    // Filter by search query
    if (req.query.search) {
      filter.$or = [
        { code: { $regex: req.query.search, $options: 'i' } },
        { name: { $regex: req.query.search, $options: 'i' } },
        { nativeName: { $regex: req.query.search, $options: 'i' } }
      ];
    }
    
    // Get languages with pagination
    const languages = await Language.find(filter)
      .sort(sort)
      .skip(skip)
      .limit(limit);
    
    // Get total count for pagination
    const total = await Language.countDocuments(filter);
    
    res.status(200).json({
      success: true,
      data: languages,
      total,
      page,
      pages: Math.ceil(total / limit),
      limit
    });
  } catch (error) {
    logger.error(`Error fetching languages: ${error.message}`, { error });
    res.status(500).json({
      success: false,
      message: 'Server error fetching languages',
      error: error.message
    });
  }
});

/**
 * @route   GET /api/admin/languages/:code
 * @desc    Get a language by code
 * @access  Admin only
 */
router.get('/languages/:code', verifyFirebaseToken, async (req, res) => {
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
    
    const Language = require('../models/Language');
    const language = await Language.findOne({ code: req.params.code });
    
    if (!language) {
      return res.status(404).json({
        success: false,
        message: 'Language not found'
      });
    }
    
    res.status(200).json({
      success: true,
      data: language
    });
  } catch (error) {
    logger.error(`Error fetching language: ${error.message}`, { error });
    res.status(500).json({
      success: false,
      message: 'Server error fetching language',
      error: error.message
    });
  }
});

/**
 * @route   POST /api/admin/languages
 * @desc    Create a new language
 * @access  Admin only
 */
router.post('/languages', verifyFirebaseToken, async (req, res) => {
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
    
    const Language = require('../models/Language');
    
    // Validate required fields
    if (!req.body.code || !req.body.name || !req.body.nativeName) {
      return res.status(400).json({
        success: false,
        message: 'Missing required fields',
        error: 'code, name, and nativeName are required'
      });
    }
    
    // Check if language already exists
    const existingLanguage = await Language.findOne({ code: req.body.code });
    if (existingLanguage) {
      return res.status(400).json({
        success: false,
        message: `Language with code ${req.body.code} already exists`
      });
    }
    
    // Create new language
    const language = new Language({
      code: req.body.code,
      name: req.body.name,
      nativeName: req.body.nativeName,
      isRTL: req.body.isRTL || false,
      isActive: req.body.isActive !== undefined ? req.body.isActive : true,
      displayOrder: req.body.displayOrder || 0
    });
    
    await language.save();
    
    res.status(201).json({
      success: true,
      message: 'Language created successfully',
      data: language
    });
  } catch (error) {
    logger.error(`Error creating language: ${error.message}`, { error });
    res.status(500).json({
      success: false,
      message: 'Server error creating language',
      error: error.message
    });
  }
});

/**
 * @route   PUT /api/admin/languages/:code
 * @desc    Update a language
 * @access  Admin only
 */
router.put('/languages/:code', verifyFirebaseToken, async (req, res) => {
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
    
    const Language = require('../models/Language');
    
    // Find language
    const language = await Language.findOne({ code: req.params.code });
    
    if (!language) {
      return res.status(404).json({
        success: false,
        message: 'Language not found'
      });
    }
    
    // Update language fields
    if (req.body.name) language.name = req.body.name;
    if (req.body.nativeName) language.nativeName = req.body.nativeName;
    if (req.body.isRTL !== undefined) language.isRTL = req.body.isRTL;
    if (req.body.isActive !== undefined) language.isActive = req.body.isActive;
    if (req.body.displayOrder !== undefined) language.displayOrder = req.body.displayOrder;
    
    await language.save();
    
    res.status(200).json({
      success: true,
      message: 'Language updated successfully',
      data: language
    });
  } catch (error) {
    logger.error(`Error updating language: ${error.message}`, { error });
    res.status(500).json({
      success: false,
      message: 'Server error updating language',
      error: error.message
    });
  }
});

/**
 * @route   DELETE /api/admin/languages/:code
 * @desc    Delete a language
 * @access  Admin only
 */
router.delete('/languages/:code', verifyFirebaseToken, async (req, res) => {
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
    
    const Language = require('../models/Language');
    
    // Find language
    const language = await Language.findOne({ code: req.params.code });
    
    if (!language) {
      return res.status(404).json({
        success: false,
        message: 'Language not found'
      });
    }
    
    // Check if language is in use by templates
    const Template = require('../models/Template');
    const templatesUsingLanguage = await Template.countDocuments({ language: language._id });
    
    if (templatesUsingLanguage > 0) {
      return res.status(400).json({
        success: false,
        message: `Cannot delete language that is used by ${templatesUsingLanguage} templates`
      });
    }
    
    // Delete language
    await Language.deleteOne({ code: req.params.code });
    
    res.status(200).json({
      success: true,
      message: 'Language deleted successfully'
    });
  } catch (error) {
    logger.error(`Error deleting language: ${error.message}`, { error });
    res.status(500).json({
      success: false,
      message: 'Server error deleting language',
      error: error.message
    });
  }
});

// Region routes
/**
 * @route   GET /api/admin/regions
 * @desc    Get all regions
 * @access  Admin only
 */
router.get('/regions', verifyFirebaseToken, async (req, res) => {
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
    
    // Get all regions from the main region router
    const Region = require('../models/Region');
    
    // Pagination parameters
    const page = parseInt(req.query.page) || 1;
    const limit = parseInt(req.query.limit) || 10;
    const skip = (page - 1) * limit;
    
    // Sorting parameters
    const sortField = req.query.sort || 'displayOrder';
    const sortOrder = req.query.order === 'asc' ? 1 : -1;
    const sort = { [sortField]: sortOrder };
    
    // Filtering parameters
    const filter = {};
    
    // Filter by search query
    if (req.query.search) {
      filter.$or = [
        { code: { $regex: req.query.search, $options: 'i' } },
        { name: { $regex: req.query.search, $options: 'i' } },
        { continent: { $regex: req.query.search, $options: 'i' } }
      ];
    }
    
    // Get regions with pagination
    const regions = await Region.find(filter)
      .sort(sort)
      .skip(skip)
      .limit(limit);
    
    // Get total count for pagination
    const total = await Region.countDocuments(filter);
    
    res.status(200).json({
      success: true,
      data: regions,
      total,
      page,
      pages: Math.ceil(total / limit),
      limit
    });
  } catch (error) {
    logger.error(`Error fetching regions: ${error.message}`, { error });
    res.status(500).json({
      success: false,
      message: 'Server error fetching regions',
      error: error.message
    });
  }
});

/**
 * @route   GET /api/admin/regions/continent/:continent
 * @desc    Get regions by continent
 * @access  Admin only
 */
router.get('/regions/continent/:continent', verifyFirebaseToken, async (req, res) => {
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
    
    const Region = require('../models/Region');
    const regions = await Region.find({ 
      continent: req.params.continent,
      isActive: true
    }).sort({ displayOrder: 1 });
    
    res.status(200).json({
      success: true,
      data: regions
    });
  } catch (error) {
    logger.error(`Error fetching regions by continent: ${error.message}`, { error });
    res.status(500).json({
      success: false,
      message: 'Server error fetching regions by continent',
      error: error.message
    });
  }
});

/**
 * @route   GET /api/admin/regions/:code
 * @desc    Get a region by code
 * @access  Admin only
 */
router.get('/regions/:code', verifyFirebaseToken, async (req, res) => {
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
    
    const Region = require('../models/Region');
    const region = await Region.findOne({ code: req.params.code });
    
    if (!region) {
      return res.status(404).json({
        success: false,
        message: 'Region not found'
      });
    }
    
    res.status(200).json({
      success: true,
      data: region
    });
  } catch (error) {
    logger.error(`Error fetching region: ${error.message}`, { error });
    res.status(500).json({
      success: false,
      message: 'Server error fetching region',
      error: error.message
    });
  }
});

/**
 * @route   POST /api/admin/regions
 * @desc    Create a new region
 * @access  Admin only
 */
router.post('/regions', verifyFirebaseToken, async (req, res) => {
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
    
    const Region = require('../models/Region');
    
    // Validate required fields
    if (!req.body.code || !req.body.name || !req.body.continent) {
      return res.status(400).json({
        success: false,
        message: 'Missing required fields',
        error: 'code, name, and continent are required'
      });
    }
    
    // Check if region already exists
    const existingRegion = await Region.findOne({ code: req.body.code });
    if (existingRegion) {
      return res.status(400).json({
        success: false,
        message: `Region with code ${req.body.code} already exists`
      });
    }
    
    // Create new region
    const region = new Region({
      code: req.body.code,
      name: req.body.name,
      continent: req.body.continent,
      flagIcon: req.body.flagIcon || '',
      isActive: req.body.isActive !== undefined ? req.body.isActive : true,
      displayOrder: req.body.displayOrder || 0,
      localization: req.body.localization || {}
    });
    
    await region.save();
    
    res.status(201).json({
      success: true,
      message: 'Region created successfully',
      data: region
    });
  } catch (error) {
    logger.error(`Error creating region: ${error.message}`, { error });
    res.status(500).json({
      success: false,
      message: 'Server error creating region',
      error: error.message
    });
  }
});

/**
 * @route   PUT /api/admin/regions/:code
 * @desc    Update a region
 * @access  Admin only
 */
router.put('/regions/:code', verifyFirebaseToken, async (req, res) => {
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
    
    const Region = require('../models/Region');
    
    // Find region
    const region = await Region.findOne({ code: req.params.code });
    
    if (!region) {
      return res.status(404).json({
        success: false,
        message: 'Region not found'
      });
    }
    
    // Update region fields
    if (req.body.name) region.name = req.body.name;
    if (req.body.continent) region.continent = req.body.continent;
    if (req.body.flagIcon !== undefined) region.flagIcon = req.body.flagIcon;
    if (req.body.isActive !== undefined) region.isActive = req.body.isActive;
    if (req.body.displayOrder !== undefined) region.displayOrder = req.body.displayOrder;
    if (req.body.localization) region.localization = req.body.localization;
    
    await region.save();
    
    res.status(200).json({
      success: true,
      message: 'Region updated successfully',
      data: region
    });
  } catch (error) {
    logger.error(`Error updating region: ${error.message}`, { error });
    res.status(500).json({
      success: false,
      message: 'Server error updating region',
      error: error.message
    });
  }
});

/**
 * @route   DELETE /api/admin/regions/:code
 * @desc    Delete a region
 * @access  Admin only
 */
router.delete('/regions/:code', verifyFirebaseToken, async (req, res) => {
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
    
    const Region = require('../models/Region');
    
    // Find region
    const region = await Region.findOne({ code: req.params.code });
    
    if (!region) {
      return res.status(404).json({
        success: false,
        message: 'Region not found'
      });
    }
    
    // Check if region is in use by templates
    const Template = require('../models/Template');
    const templatesUsingRegion = await Template.countDocuments({ region: region._id });
    
    if (templatesUsingRegion > 0) {
      return res.status(400).json({
        success: false,
        message: `Cannot delete region that is used by ${templatesUsingRegion} templates`
      });
    }
    
    // Delete region
    await Region.deleteOne({ code: req.params.code });
    
    res.status(200).json({
      success: true,
      message: 'Region deleted successfully'
    });
  } catch (error) {
    logger.error(`Error deleting region: ${error.message}`, { error });
    res.status(500).json({
      success: false,
      message: 'Server error deleting region',
      error: error.message
    });
  }
});

/**
 * @route POST /api/admin/category-icons/:id/duplicate
 * @desc Duplicate a category icon
 * @access Admin only
 */
router.post('/category-icons/:id/duplicate', verifyFirebaseToken, async (req, res) => {
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
    
    // Call the controller function
    await categoryIconController.duplicateCategoryIcon(req, res);
    
  } catch (error) {
    logger.error(`Error duplicating category icon: ${error.message}`, { error });
    res.status(500).json({
      success: false,
      message: 'Server error duplicating category icon',
      error: error.message
    });
  }
});

/**
 * @route   POST /api/admin/sponsored-ads/:id/duplicate
 * @desc    Create a duplicate copy of a sponsored ad
 * @access  Admin only
 */
router.post('/sponsored-ads/:id/duplicate', verifyFirebaseToken, async (req, res) => {
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
    
    // Call the controller function
    await sponsoredAdController.duplicateSponsoredAd(req, res);
    
  } catch (error) {
    logger.error(`Error duplicating sponsored ad: ${error.message}`, { error });
    res.status(500).json({
      success: false,
      message: 'Server error duplicating sponsored ad',
      error: error.message
    });
  }
});

module.exports = router; 