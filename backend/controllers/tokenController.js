const DeviceToken = require('../models/DeviceToken');

/**
 * Register a new device token
 * @param {object} req - Express request object
 * @param {object} res - Express response object
 */
const registerToken = async (req, res) => {
  try {
    const { token, platform } = req.body;
    
    if (!token) {
      return res.status(400).json({ error: 'Token is required' });
    }
    
    // Check if token already exists
    const existingToken = await DeviceToken.findOne({ token });
    
    if (existingToken) {
      // Update existing token
      existingToken.active = true;
      existingToken.lastUsed = new Date();
      if (platform) {
        existingToken.platform = platform;
      }
      
      await existingToken.save();
      
      return res.status(200).json({ message: 'Token updated successfully' });
    }
    
    // Create new token
    const deviceToken = new DeviceToken({
      token,
      platform: platform || 'android'
    });
    
    await deviceToken.save();
    
    res.status(201).json({ message: 'Token registered successfully' });
  } catch (error) {
    console.error('Error registering token:', error);
    res.status(500).json({ error: 'Internal server error' });
  }
};

/**
 * Get all active device tokens
 * @param {object} req - Express request object
 * @param {object} res - Express response object
 */
const getActiveTokens = async (req, res) => {
  try {
    const tokens = await DeviceToken.find({ active: true });
    res.status(200).json(tokens);
  } catch (error) {
    console.error('Error getting active tokens:', error);
    res.status(500).json({ error: 'Internal server error' });
  }
};

/**
 * Delete a device token
 * @param {object} req - Express request object
 * @param {object} res - Express response object
 */
const deleteToken = async (req, res) => {
  try {
    const { token } = req.body;
    
    if (!token) {
      return res.status(400).json({ error: 'Token is required' });
    }
    
    // Mark token as inactive instead of deleting
    await DeviceToken.findOneAndUpdate(
      { token },
      { active: false }
    );
    
    res.status(200).json({ message: 'Token deleted successfully' });
  } catch (error) {
    console.error('Error deleting token:', error);
    res.status(500).json({ error: 'Internal server error' });
  }
};

module.exports = {
  registerToken,
  getActiveTokens,
  deleteToken
}; 