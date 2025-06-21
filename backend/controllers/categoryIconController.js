const CategoryIcon = require('../models/CategoryIcon');
const mongoose = require('mongoose');

// Get all category icons
exports.getAllCategoryIcons = async (req, res) => {
    try {
        const categoryIcons = await CategoryIcon.find();
        res.status(200).json({
            success: true,
            data: categoryIcons,
            message: 'Category icons retrieved successfully'
        });
    } catch (error) {
        res.status(500).json({
            success: false,
            error: error.message,
            message: 'Failed to retrieve category icons'
        });
    }
};

// Create a new category icon
exports.createCategoryIcon = async (req, res) => {
    try {
        const categoryIcon = new CategoryIcon({
            category: req.body.category,
            categoryIcon: req.body.categoryIcon
        });
        const newCategoryIcon = await categoryIcon.save();
        res.status(201).json({
            success: true,
            data: newCategoryIcon,
            message: 'Category icon created successfully'
        });
    } catch (error) {
        res.status(400).json({
            success: false,
            error: error.message,
            message: 'Failed to create category icon'
        });
    }
};

// Duplicate a category icon
exports.duplicateCategoryIcon = async (req, res) => {
    try {
        // Find the category icon to duplicate
        const originalIcon = await CategoryIcon.findById(req.params.id);
        
        if (!originalIcon) {
            return res.status(404).json({
                success: false,
                message: 'Category icon not found'
            });
        }
        
        // Convert to plain JavaScript object
        const iconData = originalIcon.toObject();
        
        // Remove fields that should not be duplicated
        delete iconData._id;
        delete iconData.createdAt;
        delete iconData.updatedAt;
        delete iconData.__v;
        
        // Generate a new unique ID for the duplicated icon
        // If the original ID has a prefix like 'cat_', preserve it
        let prefix = 'cat_';
        if (iconData.id && iconData.id.includes('_')) {
            const parts = iconData.id.split('_');
            prefix = parts[0] + '_';
        }
        
        // Generate a new MongoDB ObjectId and use it as part of the ID
        const newObjectId = new mongoose.Types.ObjectId();
        iconData.id = `${prefix}${newObjectId.toString()}`;
        
        // Modify fields as needed
        iconData.category = `${iconData.category} (Copy)`;
        iconData.status = false; // Set status to inactive by default
        
        // Create new category icon with duplicated data
        const newIcon = new CategoryIcon(iconData);
        await newIcon.save();
        
        res.status(201).json({
            success: true,
            message: 'Category icon duplicated successfully',
            categoryIcon: newIcon
        });
    } catch (error) {
        console.error('Error duplicating category icon:', error);
        res.status(500).json({
            success: false,
            message: 'Error duplicating category icon',
            error: error.message
        });
    }
};