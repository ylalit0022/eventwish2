require('dotenv').config();
const express = require('express');
const cors = require('cors');
const connectDB = require('./config/db');
const CategoryIcon = require('./models/CategoryIcon');

// Connect to MongoDB
connectDB();

const app = express();

// Middleware
app.use(cors());
app.use(express.json());

// Routes for Category Icons

// Get all category icons
app.get('/api/categoryIcons', async (req, res) => {
    try {
        const categoryIcons = await CategoryIcon.find();
        res.json(categoryIcons);
    } catch (error) {
        console.error('Error fetching category icons:', error);
        res.status(500).json({ message: 'Error fetching category icons' });
    }
});

// Get category icon by category
app.get('/api/categoryIcons/:category', async (req, res) => {
    try {
        const categoryIcon = await CategoryIcon.findOne({ category: req.params.category });
        if (!categoryIcon) {
            return res.status(404).json({ message: 'Category icon not found' });
        }
        res.json(categoryIcon);
    } catch (error) {
        console.error('Error fetching category icon:', error);
        res.status(500).json({ message: 'Error fetching category icon' });
    }
});

// Create new category icon
app.post('/api/categoryIcons', async (req, res) => {
    try {
        const { category, categoryIcon } = req.body;
        const newCategoryIcon = new CategoryIcon({
            category,
            categoryIcon
        });
        const savedCategoryIcon = await newCategoryIcon.save();
        res.status(201).json(savedCategoryIcon);
    } catch (error) {
        console.error('Error creating category icon:', error);
        res.status(500).json({ message: 'Error creating category icon' });
    }
});

// Update category icon
app.put('/api/categoryIcons/:category', async (req, res) => {
    try {
        const updatedCategoryIcon = await CategoryIcon.findOneAndUpdate(
            { category: req.params.category },
            { categoryIcon: req.body.categoryIcon },
            { new: true }
        );
        if (!updatedCategoryIcon) {
            return res.status(404).json({ message: 'Category icon not found' });
        }
        res.json(updatedCategoryIcon);
    } catch (error) {
        console.error('Error updating category icon:', error);
        res.status(500).json({ message: 'Error updating category icon' });
    }
});

// Delete category icon
app.delete('/api/categoryIcons/:category', async (req, res) => {
    try {
        const deletedCategoryIcon = await CategoryIcon.findOneAndDelete({ category: req.params.category });
        if (!deletedCategoryIcon) {
            return res.status(404).json({ message: 'Category icon not found' });
        }
        res.json({ message: 'Category icon deleted successfully' });
    } catch (error) {
        console.error('Error deleting category icon:', error);
        res.status(500).json({ message: 'Error deleting category icon' });
    }
});

// Debug logging middleware
app.use((req, res, next) => {
    console.log(`${req.method} ${req.url}`);
    next();
});

// Error handling middleware
app.use((err, req, res, next) => {
    console.error(err.stack);
    res.status(500).json({ message: 'Something went wrong!' });
});

const PORT = process.env.CATEGORY_ICON_PORT || 3001;

app.listen(PORT, () => {
    console.log(`Category Icon Server is running on port ${PORT}`);
});