const Template = require('../models/Template');
const logger = require('../utils/logger') || console;

// Get all templates with pagination
exports.getTemplates = async (req, res) => {
    try {
        const page = parseInt(req.query.page) || 1;
        const limit = parseInt(req.query.limit) || 20;
        const skip = (page - 1) * limit;

        logger.debug(`Getting templates - page: ${page}, limit: ${limit}`);
        
        const templates = await Template.find({ status: true })
            .populate({
                path: 'categoryIcon',
                select: '_id id category categoryIcon iconType resourceName'
            })
            .sort({ createdAt: -1 })
            .skip(skip)
            .limit(limit);
            
        logger.debug(`Found ${templates.length} templates`);

        // Force convert ALL template IDs to strings (critical for Android navigation)
        const safeTemplates = templates.map(template => {
            const safeTemplate = template.toJSON();
            // Double ensure ID is a string
            safeTemplate.id = template._id.toString();
            return safeTemplate;
        });

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
            data: safeTemplates,
            page,
            totalPages,
            totalItems: totalTemplates,
            hasMore: page < totalPages,
            categories: categoriesObj,
            totalTemplates
        });
    } catch (error) {
        logger.error(`Error getting templates: ${error.message}`);
        logger.error(error.stack);
        res.status(500).json({ 
            success: false,
            message: error.message 
        });
    }
};

// Get templates by category
exports.getTemplatesByCategory = async (req, res) => {
    try {
        const { category } = req.params;
        const page = parseInt(req.query.page) || 1;
        const limit = parseInt(req.query.limit) || 20;
        const skip = (page - 1) * limit;
        
        logger.debug(`Getting templates for category: ${category} - page: ${page}, limit: ${limit}`);

        const templates = await Template.find({ 
            category, 
            status: true 
        })
            .populate({
                path: 'categoryIcon',
                select: '_id id category categoryIcon iconType resourceName'
            })
            .sort({ createdAt: -1 })
            .skip(skip)
            .limit(limit);
            
        logger.debug(`Found ${templates.length} templates for category: ${category}`);
        
        // Force convert ALL template IDs to strings (critical for Android navigation)
        const safeTemplates = templates.map(template => {
            const safeTemplate = template.toJSON();
            // Double ensure ID is a string
            safeTemplate.id = template._id.toString();
            return safeTemplate;
        });

        const totalTemplates = await Template.countDocuments({ 
            category, 
            status: true 
        });
        const totalPages = Math.ceil(totalTemplates / limit);

        res.json({
            data: safeTemplates,
            page,
            totalPages,
            totalItems: totalTemplates,
            hasMore: page < totalPages
        });
    } catch (error) {
        logger.error(`Error getting templates by category '${req.params.category}': ${error.message}`);
        logger.error(error.stack);
        res.status(500).json({ 
            success: false,
            message: error.message 
        });
    }
};

// Get template by ID
exports.getTemplateById = async (req, res) => {
    try {
        logger.debug(`Getting template by ID: ${req.params.id}`);
        
        if (!req.params.id) {
            logger.warn('Missing template ID in request');
            return res.status(400).json({
                success: false,
                message: 'Template ID is required'
            });
        }
        
        // Make sure we have a valid ID string
        const templateId = req.params.id.toString();
        
        const template = await Template.findById(templateId)
            .populate({
                path: 'categoryIcon',
                select: '_id id category categoryIcon iconType resourceName'
            });
            
        if (!template) {
            logger.warn(`Template not found: ${templateId}`);
            return res.status(404).json({ 
                success: false,
                message: 'Template not found' 
            });
        }
        
        // Convert to JSON and ensure id is always a string
        const safeTemplate = template.toJSON();
        // Double ensure ID is a string
        safeTemplate.id = template._id.toString();
        
        logger.debug(`Found template: ${template._id} with categoryIcon: ${template.categoryIcon ? template.categoryIcon._id : 'none'}`);
        
        res.json(safeTemplate);
    } catch (error) {
        logger.error(`Error getting template by ID '${req.params.id}': ${error.message}`);
        logger.error(error.stack);
        res.status(500).json({ 
            success: false,
            message: error.message 
        });
    }
};