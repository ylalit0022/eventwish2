const mongoose = require('mongoose');
const Schema = mongoose.Schema;

// Category visit schema (subdocument)
const CategoryVisitSchema = new Schema({
    category: {
        type: String,
        required: true,
        trim: true
    },
    visitDate: {
        type: Date,
        default: Date.now
    }
});

// User schema
const UserSchema = new Schema({
    deviceId: {
        type: String,
        required: true,
        unique: true,
        trim: true,
        index: true // Add index for efficient queries
    },
    lastOnline: {
        type: Date,
        default: Date.now
    },
    created: {
        type: Date,
        default: Date.now
    },
    categories: [CategoryVisitSchema]
}, {
    timestamps: true // Automatically add createdAt and updatedAt fields
});

// Add a method to update lastOnline
UserSchema.methods.updateLastOnline = function() {
    this.lastOnline = Date.now();
    return this.save();
};

// Add a method to record a category visit
UserSchema.methods.visitCategory = function(categoryName) {
    // Check if we already have this category in the list
    const existingCategory = this.categories.find(c => 
        c.category.toLowerCase() === categoryName.toLowerCase()
    );
    
    if (existingCategory) {
        // Update the visit date for existing category
        existingCategory.visitDate = Date.now();
    } else {
        // Add new category visit
        this.categories.push({
            category: categoryName,
            visitDate: Date.now()
        });
    }
    
    // Also update lastOnline time
    this.lastOnline = Date.now();
    
    return this.save();
};

module.exports = mongoose.model('User', UserSchema); 