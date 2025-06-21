const Template = require('../models/Template');

// Get all templates with pagination
exports.getTemplates = async (req, res) => {
    try {
        const page = parseInt(req.query.page) || 1;
        const limit = parseInt(req.query.limit) || 20;
        const skip = (page - 1) * limit;

        const templates = await Template.find({ status: true })
            .populate('categoryIcon')
            .populate('language')
            .populate('region')
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
            .populate('categoryIcon')
            .populate('language')
            .populate('region')
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
            .populate('categoryIcon')
            .populate('language')
            .populate('region');
            
        if (!template) {
            return res.status(404).json({ message: 'Template not found' });
        }
        res.json(template);
    } catch (error) {
        res.status(500).json({ message: error.message });
    }
};

// Duplicate a template
exports.duplicateTemplate = async (req, res) => {
    try {
        // Find the template to duplicate
        const originalTemplate = await Template.findById(req.params.id);
        
        if (!originalTemplate) {
            return res.status(404).json({ 
                success: false,
                message: 'Template not found' 
            });
        }
        
        // Convert the template to a plain JavaScript object
        const templateData = originalTemplate.toObject();
        
        // Remove fields that should not be duplicated
        delete templateData._id;
        delete templateData.createdAt;
        delete templateData.updatedAt;
        delete templateData.__v;
        
        // Modify fields as needed
        templateData.title = `${templateData.title} (Copy)`;
        templateData.status = false; // Set status to inactive by default
        
        // Create a new template with the duplicated data
        const newTemplate = new Template(templateData);
        await newTemplate.save();
        
        res.status(201).json({
            success: true,
            message: 'Template duplicated successfully',
            template: newTemplate
        });
    } catch (error) {
        console.error('Error duplicating template:', error);
        res.status(500).json({ 
            success: false,
            message: 'Error duplicating template',
            error: error.message
        });
    }
};