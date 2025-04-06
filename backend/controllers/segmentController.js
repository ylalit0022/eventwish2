const { UserSegment } = require('../models/UserSegment');
const logger = require('../config/logger');
const mongoose = require('mongoose');
const targetingService = require('../services/targetingService');

/**
 * Create a new user segment
 * @param {Object} req - Express request object
 * @param {Object} res - Express response object
 */
const createSegment = async (req, res) => {
  try {
    const { name, description, type, criteria, tags } = req.body;
    
    // Validate required fields
    if (!name || !type) {
      return res.status(400).json({
        success: false,
        message: 'Name and type are required',
        error: 'MISSING_REQUIRED_FIELDS'
      });
    }
    
    // Check if segment with the same name already exists
    const existingSegment = await UserSegment.findOne({ name });
    if (existingSegment) {
      return res.status(400).json({
        success: false,
        message: 'Segment with this name already exists',
        error: 'DUPLICATE_SEGMENT_NAME'
      });
    }
    
    // Create new segment
    const segment = new UserSegment({
      name,
      description,
      type,
      criteria: criteria || {},
      tags: tags || [],
      createdBy: req.user ? req.user.id : null
    });
    
    // Save segment
    await segment.save();
    
    // Invalidate targeting cache
    targetingService.invalidateCache();
    
    // Log creation
    logger.info(`User segment created: ${segment.name}`, {
      segmentId: segment._id,
      userId: req.user ? req.user.id : null
    });
    
    // Return the created segment
    return res.status(201).json({
      success: true,
      data: segment
    });
  } catch (error) {
    logger.error(`Error creating user segment: ${error.message}`, { error });
    return res.status(500).json({
      success: false,
      message: 'Failed to create user segment',
      error: 'SERVER_ERROR'
    });
  }
};

/**
 * Update an existing user segment
 * @param {Object} req - Express request object
 * @param {Object} res - Express response object
 */
const updateSegment = async (req, res) => {
  try {
    const { id } = req.params;
    const { name, description, type, criteria, isActive, tags } = req.body;
    
    // Validate ID
    if (!mongoose.Types.ObjectId.isValid(id)) {
      return res.status(400).json({
        success: false,
        message: 'Invalid segment ID format',
        error: 'INVALID_ID_FORMAT'
      });
    }
    
    // Find segment
    const segment = await UserSegment.findById(id);
    if (!segment) {
      return res.status(404).json({
        success: false,
        message: 'User segment not found',
        error: 'SEGMENT_NOT_FOUND'
      });
    }
    
    // Check if name is being changed and if it already exists
    if (name && name !== segment.name) {
      const existingSegment = await UserSegment.findOne({ name });
      if (existingSegment) {
        return res.status(400).json({
          success: false,
          message: 'Segment with this name already exists',
          error: 'DUPLICATE_SEGMENT_NAME'
        });
      }
    }
    
    // Update segment
    if (name) segment.name = name;
    if (description !== undefined) segment.description = description;
    if (type) segment.type = type;
    if (criteria) segment.criteria = criteria;
    if (isActive !== undefined) segment.isActive = isActive;
    if (tags) segment.tags = tags;
    segment.updatedBy = req.user ? req.user.id : null;
    
    // Save segment
    await segment.save();
    
    // Invalidate targeting cache
    targetingService.invalidateCache();
    
    // Log update
    logger.info(`User segment updated: ${segment.name}`, {
      segmentId: segment._id,
      userId: req.user ? req.user.id : null
    });
    
    // Return the updated segment
    return res.status(200).json({
      success: true,
      data: segment
    });
  } catch (error) {
    logger.error(`Error updating user segment: ${error.message}`, { error });
    return res.status(500).json({
      success: false,
      message: 'Failed to update user segment',
      error: 'SERVER_ERROR'
    });
  }
};

/**
 * Delete a user segment
 * @param {Object} req - Express request object
 * @param {Object} res - Express response object
 */
const deleteSegment = async (req, res) => {
  try {
    const { id } = req.params;
    
    // Validate ID
    if (!mongoose.Types.ObjectId.isValid(id)) {
      return res.status(400).json({
        success: false,
        message: 'Invalid segment ID format',
        error: 'INVALID_ID_FORMAT'
      });
    }
    
    // Find segment
    const segment = await UserSegment.findById(id);
    if (!segment) {
      return res.status(404).json({
        success: false,
        message: 'User segment not found',
        error: 'SEGMENT_NOT_FOUND'
      });
    }
    
    // Check if segment is used in any ads
    // TODO: Implement check for segment usage in ads
    
    // Delete segment
    await UserSegment.deleteOne({ _id: id });
    
    // Invalidate targeting cache
    targetingService.invalidateCache();
    
    // Log deletion
    logger.info(`User segment deleted: ${segment.name}`, {
      segmentId: segment._id,
      userId: req.user ? req.user.id : null
    });
    
    // Return success
    return res.status(200).json({
      success: true,
      message: 'User segment deleted successfully'
    });
  } catch (error) {
    logger.error(`Error deleting user segment: ${error.message}`, { error });
    return res.status(500).json({
      success: false,
      message: 'Failed to delete user segment',
      error: 'SERVER_ERROR'
    });
  }
};

/**
 * Get a user segment by ID
 * @param {Object} req - Express request object
 * @param {Object} res - Express response object
 */
const getSegment = async (req, res) => {
  try {
    const { id } = req.params;
    
    // Validate ID
    if (!mongoose.Types.ObjectId.isValid(id)) {
      return res.status(400).json({
        success: false,
        message: 'Invalid segment ID format',
        error: 'INVALID_ID_FORMAT'
      });
    }
    
    // Find segment
    const segment = await UserSegment.findById(id);
    if (!segment) {
      return res.status(404).json({
        success: false,
        message: 'User segment not found',
        error: 'SEGMENT_NOT_FOUND'
      });
    }
    
    // Return the segment
    return res.status(200).json({
      success: true,
      data: segment
    });
  } catch (error) {
    logger.error(`Error getting user segment: ${error.message}`, { error });
    return res.status(500).json({
      success: false,
      message: 'Failed to get user segment',
      error: 'SERVER_ERROR'
    });
  }
};

/**
 * Get all user segments with optional filters
 * @param {Object} req - Express request object
 * @param {Object} res - Express response object
 */
const getSegments = async (req, res) => {
  try {
    const { type, isActive, tag } = req.query;
    
    // Build query
    const query = {};
    
    if (type) {
      query.type = type;
    }
    
    if (isActive !== undefined) {
      query.isActive = isActive === 'true';
    }
    
    if (tag) {
      query.tags = tag;
    }
    
    // Find segments
    const segments = await UserSegment.find(query).sort({ name: 1 });
    
    // Return the segments
    return res.status(200).json({
      success: true,
      count: segments.length,
      data: segments
    });
  } catch (error) {
    logger.error(`Error getting user segments: ${error.message}`, { error });
    return res.status(500).json({
      success: false,
      message: 'Failed to get user segments',
      error: 'SERVER_ERROR'
    });
  }
};

/**
 * Test if a user context matches a segment
 * @param {Object} req - Express request object
 * @param {Object} res - Express response object
 */
const testSegment = async (req, res) => {
  try {
    const { id } = req.params;
    const userContext = req.body;
    
    // Validate ID
    if (!mongoose.Types.ObjectId.isValid(id)) {
      return res.status(400).json({
        success: false,
        message: 'Invalid segment ID format',
        error: 'INVALID_ID_FORMAT'
      });
    }
    
    // Find segment
    const segment = await UserSegment.findById(id);
    if (!segment) {
      return res.status(404).json({
        success: false,
        message: 'User segment not found',
        error: 'SEGMENT_NOT_FOUND'
      });
    }
    
    // Test if user context matches segment criteria
    const matches = targetingService.evaluateCriteria(segment.criteria, userContext);
    
    // Return the result
    return res.status(200).json({
      success: true,
      data: {
        segmentId: segment._id,
        segmentName: segment.name,
        matches,
        userContext
      }
    });
  } catch (error) {
    logger.error(`Error testing user segment: ${error.message}`, { error });
    return res.status(500).json({
      success: false,
      message: 'Failed to test user segment',
      error: 'SERVER_ERROR'
    });
  }
};

module.exports = {
  createSegment,
  updateSegment,
  deleteSegment,
  getSegment,
  getSegments,
  testSegment
}; 