const mongoose = require('mongoose');
const bcrypt = require('bcrypt');
const Schema = mongoose.Schema;

/**
 * User schema for storing user authentication data
 */
const UserSchema = new Schema({
    phoneNumber: {
        type: String,
        required: true,
        unique: true,
        trim: true
    },
    firebaseUid: {
        type: String,
        required: true,
        unique: true
    },
    password: {
        type: String,
        required: true
    },
    displayName: {
        type: String,
        trim: true
    },
    email: {
        type: String,
        trim: true,
        sparse: true
    },
    photoUrl: {
        type: String
    },
    refreshToken: {
        type: String
    },
    lastLogin: {
        type: Date,
        default: Date.now
    },
    active: {
        type: Boolean,
        default: true
    },
    role: {
        type: String,
        enum: ['user', 'admin'],
        default: 'user'
    },
    createdAt: {
        type: Date,
        default: Date.now
    },
    updatedAt: {
        type: Date,
        default: Date.now
    },
    resetPasswordCode: { 
        type: String, 
        default: null 
    },
    resetPasswordExpires: { 
        type: Date, 
        default: null 
    }
}, { 
    timestamps: { createdAt: 'createdAt', updatedAt: 'updatedAt' } 
});

// Hash user password before saving
UserSchema.pre('save', async function(next) {
    // Only hash the password if it has been modified (or is new)
    if (!this.isModified('password')) return next();
    
    try {
        // Generate a salt
        const salt = await bcrypt.genSalt(10);
        
        // Hash the password along with the new salt
        this.password = await bcrypt.hash(this.password, salt);
        next();
    } catch (error) {
        next(error);
    }
});

// Method to compare password
UserSchema.methods.comparePassword = async function(candidatePassword) {
    try {
        return await bcrypt.compare(candidatePassword, this.password);
    } catch (error) {
        throw error;
    }
};

// Method to generate random password
UserSchema.statics.generateRandomPassword = function() {
    // Generate a random password with 8 characters
    const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()-_=+';
    let password = '';
    for (let i = 0; i < 12; i++) {
        password += chars.charAt(Math.floor(Math.random() * chars.length));
    }
    return password;
};

// Helper method to sanitize user object for response
UserSchema.methods.toJSON = function() {
    const user = this.toObject();
    delete user.password;
    delete user.refreshToken;
    return user;
};

module.exports = mongoose.model('User', UserSchema); 