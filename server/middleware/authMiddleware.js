const jwt = require('jsonwebtoken');
const logger = require('../config/logger');
const User = require('../models/User');
const firebase = require('../config/firebase');

/**
 * Middleware to verify JWT token from Authorization header
 * @param {Object} req - Express request object
 * @param {Object} res - Express response object
 * @param {Function} next - Express next function
 */
const verifyToken = async (req, res, next) => {
    try {
        // Get auth header
        const authHeader = req.headers.authorization;
        if (!authHeader) {
            return res.status(401).json({ message: 'Authorization header missing' });
        }

        // Check if header format is valid
        const parts = authHeader.split(' ');
        if (parts.length !== 2 || parts[0] !== 'Bearer') {
            return res.status(401).json({ message: 'Invalid authorization format' });
        }

        const token = parts[1];
        
        // Verify token
        try {
            const decoded = jwt.verify(token, process.env.JWT_SECRET);
            req.user = decoded;
            
            // Check if user exists and is active
            const user = await User.findById(decoded.userId);
            if (!user || !user.active) {
                return res.status(401).json({ message: 'User not found or inactive' });
            }
            
            next();
        } catch (error) {
            if (error.name === 'TokenExpiredError') {
                return res.status(401).json({ message: 'Token expired', expiredAt: error.expiredAt });
            }
            
            logger.error(`Token verification error: ${error.message}`);
            return res.status(401).json({ message: 'Invalid token' });
        }
    } catch (error) {
        logger.error(`Auth middleware error: ${error.message}`);
        return res.status(500).json({ message: 'Server error' });
    }
};

/**
 * Middleware to verify Firebase ID token
 * @param {Object} req - Express request object
 * @param {Object} res - Express response object
 * @param {Function} next - Express next function
 */
const verifyFirebaseToken = async (req, res, next) => {
    try {
        // Get ID token from request body
        const { idToken } = req.body;
        if (!idToken) {
            return res.status(401).json({ message: 'Firebase ID token missing' });
        }
        
        // Verify ID token
        try {
            const decodedToken = await firebase.verifyIdToken(idToken);
            req.firebaseUser = decodedToken;
            next();
        } catch (error) {
            logger.error(`Firebase token verification error: ${error.message}`);
            return res.status(401).json({ message: 'Invalid Firebase token' });
        }
    } catch (error) {
        logger.error(`Firebase auth middleware error: ${error.message}`);
        return res.status(500).json({ message: 'Server error' });
    }
};

/**
 * Middleware to check admin role
 * @param {Object} req - Express request object
 * @param {Object} res - Express response object
 * @param {Function} next - Express next function
 */
const isAdmin = async (req, res, next) => {
    try {
        const user = await User.findById(req.user.userId);
        if (!user || user.role !== 'admin') {
            return res.status(403).json({ message: 'Access denied: Admin role required' });
        }
        next();
    } catch (error) {
        logger.error(`Admin check middleware error: ${error.message}`);
        return res.status(500).json({ message: 'Server error' });
    }
};

module.exports = {
    verifyToken,
    verifyFirebaseToken,
    isAdmin
}; 