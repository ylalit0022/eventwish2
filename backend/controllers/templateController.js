const Template = require('../models/Template');

// Get all templates with pagination
exports.getTemplates = async (req, res) => {
    try {
        const page = parseInt(req.query.page) || 1;
        const limit = parseInt(req.query.limit) || 20;
        const skip = (page - 1) * limit;

        const templates = await Template.find({ status: true })
            .populate('categoryIconObj')
            .sort({ createdAt: -1 })
            .skip(skip)
            .limit(limit);

        const totalTemplates = await Template.countDocuments({ status: true });
        const totalPages = Math.ceil(totalTemplates / limit);

        // Get categories count
        const categories = await Template.aggregate([
            { $match: { status: true } },
            { $group: { _id: '$category', count: { $sum: 1 } } }
        ]);

        const categoriesObj = categories.reduce((acc, curr) => {
            acc[curr._id] = curr.count;
            return acc;
        }, {});

        res.json({
            data: templates,
            page,
            totalPages,
            totalItems: totalTemplates,
            hasMore: page < totalPages,
            categories: categoriesObj,
            totalTemplates
        });
    } catch (error) {
        res.status(500).json({ message: error.message });
    }
};

// Get templates by category
exports.getTemplatesByCategory = async (req, res) => {
    try {
        const { category } = req.params;
        const page = parseInt(req.query.page) || 1;
        const limit = parseInt(req.query.limit) || 20;
        const skip = (page - 1) * limit;

        const templates = await Template.find({ 
            category, 
            status: true 
        })
            .populate('categoryIconObj')
            .sort({ createdAt: -1 })
            .skip(skip)
            .limit(limit);

        const totalTemplates = await Template.countDocuments({ 
            category, 
            status: true 
        });
        const totalPages = Math.ceil(totalTemplates / limit);

        res.json({
            data: templates,
            page,
            totalPages,
            totalItems: totalTemplates,
            hasMore: page < totalPages
        });
    } catch (error) {
        res.status(500).json({ message: error.message });
    }
};

// Get template by ID
exports.getTemplateById = async (req, res) => {
    try {
        const template = await Template.findById(req.params.id)
            .populate('categoryIconObj');
            
        if (!template) {
            return res.status(404).json({ message: 'Template not found' });
        }
        res.json(template);
    } catch (error) {
        res.status(500).json({ message: error.message });
    }
};