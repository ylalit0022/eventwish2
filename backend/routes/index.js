const express = require('express');
const router = express.Router();
const mongoose = require('mongoose');

// Health check endpoint
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
    
    // Calculate response time
    const start = process.hrtime();
    const end = process.hrtime(start);
    const responseTime = Math.round((end[0] * 1000) + (end[1] / 1000000)) + 'ms';
    
    res.status(200).json({
      success: true,
      status: connectionState === 1 ? 'up' : 'down',
      timestamp: new Date().toISOString(),
      responseTime,
      mongodb: {
        connected: connectionState === 1,
        connectionState,
        connectionStateText
      }
    });
  } catch (error) {
    console.error('Error in health check endpoint:', error);
    res.status(500).json({
      success: false,
      status: 'error',
      message: 'Error checking API health',
      error: error.message,
      timestamp: new Date().toISOString()
    });
  }
});

module.exports = router; 