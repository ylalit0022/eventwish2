const jwt = require('jsonwebtoken');
const logger = require('../config/logger');

/**
 * Generate a JWT token
 * @param {object} req - Express request object
 * @param {object} res - Express response object
 */
const generateToken = async (req, res) => {
  try {
    const { userId, role = 'user' } = req.body;

    if (!userId) {
      return res.status(400).json({
        success: false,
        message: 'User ID is required'
      });
    }

    // Create token payload
    const payload = {
      user: {
        id: userId,
        role
      }
    };

    // Sign token
    const token = jwt.sign(
      payload,
      process.env.JWT_SECRET,
      { expiresIn: '24h' }
    );

    // Return token
    res.json({
      success: true,
      token
    });

  } catch (error) {
    logger.error('Error generating token:', error);
    res.status(500).json({
      success: false,
      message: 'Error generating token'
    });
  }
};

module.exports = {
  generateToken
};