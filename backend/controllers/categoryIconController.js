const CategoryIcon = require('../models/CategoryIcon');

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