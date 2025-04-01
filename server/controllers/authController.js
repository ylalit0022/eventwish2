const jwt = require('jsonwebtoken');
const User = require('../models/User');
const logger = require('../config/logger');
const firebase = require('../config/firebase');

/**
 * Generate JWT tokens for authentication
 * @param {Object} user - User object from database
 * @returns {Object} - Object containing token, refresh token, and expiry
 */
const generateTokens = (user) => {
    try {
        // Generate access token
        const token = jwt.sign(
            { userId: user._id, role: user.role },
            process.env.JWT_SECRET,
            { expiresIn: process.env.JWT_EXPIRY || '1h' }
        );
        
        // Generate refresh token with longer expiry
        const refreshToken = jwt.sign(
            { userId: user._id },
            process.env.JWT_REFRESH_SECRET || process.env.JWT_SECRET,
            { expiresIn: process.env.JWT_REFRESH_EXPIRY || '7d' }
        );
        
        // Get expiry time in seconds
        const tokenExpiry = jwt.decode(token).exp;
        const expiresIn = tokenExpiry - Math.floor(Date.now() / 1000);
        
        return { token, refreshToken, expiresIn };
    } catch (error) {
        logger.error(`Error generating tokens: ${error.message}`);
        throw error;
    }
};

/**
 * Register a new user with Firebase phone authentication
 * @param {Object} req - Express request object
 * @param {Object} res - Express response object
 */
const register = async (req, res) => {
    try {
        const { phoneNumber, firebaseUid, idToken } = req.body;
        
        // Validate input
        if (!phoneNumber || !firebaseUid || !idToken) {
            return res.status(400).json({ message: 'Phone number, Firebase UID, and ID token are required' });
        }
        
        // Verify Firebase ID token
        try {
            const decodedToken = await firebase.verifyIdToken(idToken);
            
            // Verify that token matches provided UID and phone
            if (decodedToken.uid !== firebaseUid) {
                logger.warn(`Token UID (${decodedToken.uid}) does not match provided UID (${firebaseUid})`);
                return res.status(401).json({ message: 'Invalid token: UID mismatch' });
            }
            
            if (decodedToken.phone_number !== phoneNumber) {
                logger.warn(`Token phone (${decodedToken.phone_number}) does not match provided phone (${phoneNumber})`);
                return res.status(401).json({ message: 'Invalid token: Phone number mismatch' });
            }
            
            // Check if user already exists
            const existingUser = await User.findOne({ 
                $or: [{ phoneNumber }, { firebaseUid }]
            });
            
            if (existingUser) {
                // If user exists, update login time and return tokens
                existingUser.lastLogin = new Date();
                await existingUser.save();
                
                const tokens = generateTokens(existingUser);
                
                // Update refresh token in database
                existingUser.refreshToken = tokens.refreshToken;
                await existingUser.save();
                
                return res.status(200).json({
                    message: 'User already exists, login successful',
                    user: existingUser,
                    ...tokens
                });
            }
            
            // Generate random password for user
            const password = User.generateRandomPassword();
            
            // Create new user
            const newUser = new User({
                phoneNumber,
                firebaseUid,
                password,
                displayName: decodedToken.name || '',
                email: decodedToken.email || '',
                photoUrl: decodedToken.picture || '',
                lastLogin: new Date()
            });
            
            // Save user
            await newUser.save();
            
            // Generate tokens
            const tokens = generateTokens(newUser);
            
            // Update refresh token in database
            newUser.refreshToken = tokens.refreshToken;
            await newUser.save();
            
            return res.status(201).json({
                message: 'User registered successfully',
                user: newUser,
                ...tokens
            });
        } catch (error) {
            logger.error(`Firebase token verification error: ${error.message}`);
            return res.status(401).json({ message: 'Invalid Firebase token' });
        }
    } catch (error) {
        logger.error(`Registration error: ${error.message}`);
        return res.status(500).json({ message: 'Server error' });
    }
};

/**
 * Login a user with phone number and password
 * @param {Object} req - Express request object
 * @param {Object} res - Express response object
 */
const login = async (req, res) => {
    try {
        const { phoneNumber, password } = req.body;
        
        // Validate input
        if (!phoneNumber || !password) {
            return res.status(400).json({ message: 'Phone number and password are required' });
        }
        
        // Find user by phone number
        const user = await User.findOne({ phoneNumber });
        if (!user) {
            return res.status(401).json({ message: 'Invalid credentials' });
        }
        
        // Check if user is active
        if (!user.active) {
            return res.status(401).json({ message: 'Account is inactive' });
        }
        
        // Verify password
        const isPasswordValid = await user.comparePassword(password);
        if (!isPasswordValid) {
            return res.status(401).json({ message: 'Invalid credentials' });
        }
        
        // Update last login time
        user.lastLogin = new Date();
        await user.save();
        
        // Generate tokens
        const tokens = generateTokens(user);
        
        // Update refresh token in database
        user.refreshToken = tokens.refreshToken;
        await user.save();
        
        return res.status(200).json({
            message: 'Login successful',
            user: user,
            ...tokens
        });
    } catch (error) {
        logger.error(`Login error: ${error.message}`);
        return res.status(500).json({ message: 'Server error' });
    }
};

/**
 * Refresh JWT token using refresh token
 * @param {Object} req - Express request object
 * @param {Object} res - Express response object
 */
const refreshToken = async (req, res) => {
    try {
        const { refreshToken } = req.body;
        
        // Validate input
        if (!refreshToken) {
            return res.status(400).json({ message: 'Refresh token is required' });
        }
        
        // Verify refresh token
        try {
            const decoded = jwt.verify(
                refreshToken,
                process.env.JWT_REFRESH_SECRET || process.env.JWT_SECRET
            );
            
            // Find user
            const user = await User.findById(decoded.userId);
            if (!user) {
                return res.status(401).json({ message: 'User not found' });
            }
            
            // Check if refresh token matches stored token
            if (user.refreshToken !== refreshToken) {
                return res.status(401).json({ message: 'Invalid refresh token' });
            }
            
            // Check if user is active
            if (!user.active) {
                return res.status(401).json({ message: 'Account is inactive' });
            }
            
            // Generate new tokens
            const tokens = generateTokens(user);
            
            // Update refresh token in database
            user.refreshToken = tokens.refreshToken;
            await user.save();
            
            return res.status(200).json({
                message: 'Token refreshed successfully',
                ...tokens
            });
        } catch (error) {
            if (error.name === 'TokenExpiredError') {
                return res.status(401).json({ message: 'Refresh token expired' });
            }
            
            logger.error(`Refresh token verification error: ${error.message}`);
            return res.status(401).json({ message: 'Invalid refresh token' });
        }
    } catch (error) {
        logger.error(`Refresh token error: ${error.message}`);
        return res.status(500).json({ message: 'Server error' });
    }
};

/**
 * Logout a user
 * @param {Object} req - Express request object
 * @param {Object} res - Express response object
 */
const logout = async (req, res) => {
    try {
        // Get user from token
        const userId = req.user.userId;
        
        // Find user
        const user = await User.findById(userId);
        if (!user) {
            return res.status(404).json({ message: 'User not found' });
        }
        
        // Clear refresh token
        user.refreshToken = null;
        await user.save();
        
        return res.status(200).json({ message: 'Logout successful' });
    } catch (error) {
        logger.error(`Logout error: ${error.message}`);
        return res.status(500).json({ message: 'Server error' });
    }
};

/**
 * Change user password
 * @param {Object} req - Express request object
 * @param {Object} res - Express response object
 */
const changePassword = async (req, res) => {
    try {
        const { currentPassword, newPassword } = req.body;
        const userId = req.user.userId;
        
        // Validate input
        if (!currentPassword || !newPassword) {
            return res.status(400).json({ message: 'Current and new password are required' });
        }
        
        // Find user
        const user = await User.findById(userId);
        if (!user) {
            return res.status(404).json({ message: 'User not found' });
        }
        
        // Verify current password
        const isPasswordValid = await user.comparePassword(currentPassword);
        if (!isPasswordValid) {
            return res.status(401).json({ message: 'Current password is incorrect' });
        }
        
        // Update password
        user.password = newPassword;
        await user.save();
        
        return res.status(200).json({ message: 'Password changed successfully' });
    } catch (error) {
        logger.error(`Change password error: ${error.message}`);
        return res.status(500).json({ message: 'Server error' });
    }
};

/**
 * Generate a random 6-digit verification code
 * @returns {string} 6-digit code
 */
const generateVerificationCode = () => {
    return Math.floor(100000 + Math.random() * 900000).toString();
};

/**
 * Send password reset code
 * @param {Object} req - Express request object
 * @param {Object} res - Express response object
 */
const sendPasswordResetCode = async (req, res) => {
    try {
        const { phoneNumber } = req.body;

        if (!phoneNumber) {
            return res.status(400).json({ message: 'Phone number is required' });
        }

        // Find user with the given phone number
        const user = await User.findOne({ phoneNumber });
        if (!user) {
            return res.status(404).json({ message: 'User not found with this phone number' });
        }

        // Generate verification code
        const verificationCode = generateVerificationCode();
        
        // Store verification code and expiry time
        user.resetPasswordCode = verificationCode;
        user.resetPasswordExpires = Date.now() + 15 * 60 * 1000; // 15 minutes
        await user.save();

        // In a production environment, send the code via SMS using a service like Twilio
        // This is a simplified version that just logs the code
        console.log(`Password reset code: ${verificationCode} sent to ${phoneNumber}`);
        
        // TODO: Implement actual SMS sending here
        // Example with Twilio:
        // await twilioClient.messages.create({
        //     body: `Your EventWish password reset code is: ${verificationCode}`,
        //     from: process.env.TWILIO_PHONE_NUMBER,
        //     to: phoneNumber
        // });

        return res.status(200).json({ 
            message: 'Password reset code sent successfully',
            // Only in development - don't include this in production!
            code: process.env.NODE_ENV === 'development' ? verificationCode : undefined
        });
    } catch (error) {
        logger.error('Error sending password reset code:', error);
        return res.status(500).json({ message: 'Error sending password reset code', error: error.message });
    }
};

/**
 * Reset password with verification code
 * @param {Object} req - Express request object
 * @param {Object} res - Express response object
 */
const resetPassword = async (req, res) => {
    try {
        const { verificationCode, newPassword, phoneNumber } = req.body;

        if (!verificationCode || !newPassword) {
            return res.status(400).json({ message: 'Verification code and new password are required' });
        }

        if (newPassword.length < 6) {
            return res.status(400).json({ message: 'Password must be at least 6 characters long' });
        }

        // Find user with the given verification code
        const query = phoneNumber 
            ? { phoneNumber, resetPasswordCode: verificationCode }
            : { resetPasswordCode: verificationCode };
            
        const user = await User.findOne(query);
        
        if (!user) {
            return res.status(400).json({ message: 'Invalid verification code' });
        }

        // Check if code is expired
        if (user.resetPasswordExpires < Date.now()) {
            return res.status(400).json({ message: 'Verification code has expired' });
        }

        // Update password
        user.password = newPassword;
        
        // Clear reset code and expiry
        user.resetPasswordCode = undefined;
        user.resetPasswordExpires = undefined;
        
        await user.save();

        return res.status(200).json({ message: 'Password reset successful' });
    } catch (error) {
        logger.error('Error resetting password:', error);
        return res.status(500).json({ message: 'Error resetting password', error: error.message });
    }
};

module.exports = {
    register,
    login,
    refreshToken,
    logout,
    changePassword,
    sendPasswordResetCode,
    resetPassword
}; 