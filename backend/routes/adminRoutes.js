const express = require('express');
const router = express.Router();
const User = require('../models/User');
const Template = require('../models/Template');
const SharedWish = require('../models/SharedWish');
const { AdMob, adTypes } = require('../models/AdMob');
const { verifyFirebaseToken, verifyAdmin } = require('../middleware/authMiddleware');
const logger = require('../config/logger');
const multer = require('multer');
const csv = require('csv-parser');
const { Parser } = require('json2csv');
const fs = require('fs');
const path = require('path');
const { Readable } = require('stream');
const mongoose = require('mongoose');

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
    
    const template = await Template.findById(id);
    
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
    
    // Find and update template
    const template = await Template.findByIdAndUpdate(
      id,
      req.body,
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
  } catch (error) {
    logger.error(`Error updating template: ${error.message}`, { error });
    res.status(500).json({
      success: false,
      message: 'Server error updating template',
      error: error.message
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
    
    // Filter by search query (on title, category, festivalTag)
    if (req.query.q) {
      const searchQuery = req.query.q;
      filter.$or = [
        { title: { $regex: searchQuery, $options: 'i' } },
        { category: { $regex: searchQuery, $options: 'i' } },
        { festivalTag: { $regex: searchQuery, $options: 'i' } }
      ];
    }
    
    // Filter by premium status
    if (req.query.isPremium === 'true') {
      filter.isPremium = true;
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

module.exports = router; 