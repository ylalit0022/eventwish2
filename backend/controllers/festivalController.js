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