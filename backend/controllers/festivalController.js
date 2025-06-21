const Festival = require('../models/Festival');
const { startOfDay, endOfDay, addDays } = require('date-fns');

exports.getUpcomingFestivals = async (req, res) => {
    try {
        const today = startOfDay(new Date());
        const threeDaysLater = endOfDay(addDays(today, 3));

        const festivals = await Festival.find({
            date: {
                $gte: today,
                $lte: threeDaysLater
            },
            isActive: true
        })
        .populate('templates')
        .populate('categoryIcon')
        .sort({ date: 1 });

        res.json(festivals);
    } catch (error) {
        res.status(500).json({ message: error.message });
    }
};

exports.getFestivalsByCategory = async (req, res) => {
    try {
        const { category } = req.params;
        const festivals = await Festival.find({ 
            category,
            isActive: true 
        })
        .populate('templates')
        .populate('categoryIcon')
        .sort({ date: 1 });

        res.json(festivals);
    } catch (error) {
        res.status(500).json({ message: error.message });
    }
};

// Duplicate a festival
exports.duplicateFestival = async (req, res) => {
    try {
        // Find the festival to duplicate
        const originalFestival = await Festival.findById(req.params.id);
        
        if (!originalFestival) {
            return res.status(404).json({
                success: false,
                message: 'Festival not found'
            });
        }
        
        // Convert to plain JavaScript object
        const festivalData = originalFestival.toObject();
        
        // Remove fields that should not be duplicated
        delete festivalData._id;
        delete festivalData.createdAt;
        delete festivalData.updatedAt;
        delete festivalData.__v;
        
        // Modify fields as needed
        festivalData.name = `${festivalData.name} (Copy)`;
        festivalData.isActive = false; // Set status to inactive by default
        
        // Create new festival with duplicated data
        const newFestival = new Festival(festivalData);
        await newFestival.save();
        
        res.status(201).json({
            success: true,
            message: 'Festival duplicated successfully',
            festival: newFestival
        });
    } catch (error) {
        console.error('Error duplicating festival:', error);
        res.status(500).json({
            success: false,
            message: 'Error duplicating festival',
            error: error.message
        });
    }
};