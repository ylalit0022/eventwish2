/**
 * Middleware to validate that route parameters are valid MongoDB ObjectIds
 */

const mongoose = require('mongoose');
const logger = require('../config/logger');

/**
 * Creates middleware to validate a specific param as MongoDB ObjectId
 * 
 * @param {string} paramName - The name of the parameter to validate
 * @returns {Function} Express middleware function
 */
function validateObjectId(paramName) {
  return (req, res, next) => {
    const paramValue = req.params[paramName];
    
    if (!paramValue) {
      return res.status(400).json({
        success: false,
        message: `Missing required parameter: ${paramName}`
      });
    }
    
    // Check if the ID is a valid MongoDB ObjectId
    if (!mongoose.Types.ObjectId.isValid(paramValue)) {
      logger.warn(`Invalid ObjectId format for ${paramName}: ${paramValue}`);
      return res.status(400).json({
        success: false,
        message: `Invalid ${paramName} format. Must be a valid Object ID.`
      });
    }
    
    next();
  };
}

module.exports = validateObjectId; 