const Language = require('../models/Language');

// Get all languages
exports.getAllLanguages = async (req, res) => {
    try {
        const languages = await Language.find({ isActive: true })
            .sort({ displayOrder: 1, name: 1 });
        
        res.json({
            success: true,
            data: languages
        });
    } catch (error) {
        res.status(500).json({
            success: false,
            message: error.message
        });
    }
};

// Get language by code
exports.getLanguageByCode = async (req, res) => {
    try {
        const language = await Language.findOne({ 
            code: req.params.code.toLowerCase() 
        });
        
        if (!language) {
            return res.status(404).json({
                success: false,
                message: 'Language not found'
            });
        }
        
        res.json({
            success: true,
            data: language
        });
    } catch (error) {
        res.status(500).json({
            success: false,
            message: error.message
        });
    }
};

// Admin: Create a new language
exports.createLanguage = async (req, res) => {
    try {
        const { code, name, nativeName, isRTL, isActive, displayOrder } = req.body;
        
        // Check if language already exists
        const existingLanguage = await Language.findOne({ code: code.toLowerCase() });
        if (existingLanguage) {
            return res.status(400).json({
                success: false,
                message: 'Language with this code already exists'
            });
        }
        
        const language = new Language({
            code: code.toLowerCase(),
            name,
            nativeName,
            isRTL: isRTL || false,
            isActive: isActive !== undefined ? isActive : true,
            displayOrder: displayOrder || 0
        });
        
        await language.save();
        
        res.status(201).json({
            success: true,
            data: language,
            message: 'Language created successfully'
        });
    } catch (error) {
        res.status(500).json({
            success: false,
            message: error.message
        });
    }
};

// Admin: Update a language
exports.updateLanguage = async (req, res) => {
    try {
        const { name, nativeName, isRTL, isActive, displayOrder } = req.body;
        
        const language = await Language.findOne({ code: req.params.code.toLowerCase() });
        
        if (!language) {
            return res.status(404).json({
                success: false,
                message: 'Language not found'
            });
        }
        
        if (name) language.name = name;
        if (nativeName) language.nativeName = nativeName;
        if (isRTL !== undefined) language.isRTL = isRTL;
        if (isActive !== undefined) language.isActive = isActive;
        if (displayOrder !== undefined) language.displayOrder = displayOrder;
        
        await language.save();
        
        res.json({
            success: true,
            data: language,
            message: 'Language updated successfully'
        });
    } catch (error) {
        res.status(500).json({
            success: false,
            message: error.message
        });
    }
};

// Admin: Delete a language
exports.deleteLanguage = async (req, res) => {
    try {
        const language = await Language.findOne({ code: req.params.code.toLowerCase() });
        
        if (!language) {
            return res.status(404).json({
                success: false,
                message: 'Language not found'
            });
        }
        
        await language.deleteOne();
        
        res.json({
            success: true,
            message: 'Language deleted successfully'
        });
    } catch (error) {
        res.status(500).json({
            success: false,
            message: error.message
        });
    }
}; 