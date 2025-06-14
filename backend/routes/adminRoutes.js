const express = require('express');
const router = express.Router();
const User = require('../models/User');
const { verifyFirebaseToken, verifyAdmin } = require('../middleware/authMiddleware');
const logger = require('../config/logger');

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
    
    const user = await User.findOne({ uid })
      .select('-engagementLog -__v'); // Exclude large arrays and version
    
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

module.exports = router; 