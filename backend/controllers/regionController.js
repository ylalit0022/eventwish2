const Region = require('../models/Region');

// Get all regions
exports.getAllRegions = async (req, res) => {
    try {
        const regions = await Region.find({ isActive: true })
            .sort({ displayOrder: 1, name: 1 });
        
        res.json({
            success: true,
            data: regions
        });
    } catch (error) {
        res.status(500).json({
            success: false,
            message: error.message
        });
    }
};

// Get regions by continent
exports.getRegionsByContinent = async (req, res) => {
    try {
        const { continent } = req.params;
        
        const regions = await Region.find({ 
            continent, 
            isActive: true 
        }).sort({ displayOrder: 1, name: 1 });
        
        res.json({
            success: true,
            data: regions
        });
    } catch (error) {
        res.status(500).json({
            success: false,
            message: error.message
        });
    }
};

// Get region by code
exports.getRegionByCode = async (req, res) => {
    try {
        const region = await Region.findOne({ 
            code: req.params.code.toUpperCase() 
        });
        
        if (!region) {
            return res.status(404).json({
                success: false,
                message: 'Region not found'
            });
        }
        
        res.json({
            success: true,
            data: region
        });
    } catch (error) {
        res.status(500).json({
            success: false,
            message: error.message
        });
    }
};

// Admin: Create a new region
exports.createRegion = async (req, res) => {
    try {
        const { code, name, continent, flagIcon, isActive, displayOrder, localization } = req.body;
        
        // Check if region already exists
        const existingRegion = await Region.findOne({ code: code.toUpperCase() });
        if (existingRegion) {
            return res.status(400).json({
                success: false,
                message: 'Region with this code already exists'
            });
        }
        
        const region = new Region({
            code: code.toUpperCase(),
            name,
            continent,
            flagIcon: flagIcon || '',
            isActive: isActive !== undefined ? isActive : true,
            displayOrder: displayOrder || 0,
            localization: localization || {}
        });
        
        await region.save();
        
        res.status(201).json({
            success: true,
            data: region,
            message: 'Region created successfully'
        });
    } catch (error) {
        res.status(500).json({
            success: false,
            message: error.message
        });
    }
};

// Admin: Update a region
exports.updateRegion = async (req, res) => {
    try {
        const { name, continent, flagIcon, isActive, displayOrder, localization } = req.body;
        
        const region = await Region.findOne({ code: req.params.code.toUpperCase() });
        
        if (!region) {
            return res.status(404).json({
                success: false,
                message: 'Region not found'
            });
        }
        
        if (name) region.name = name;
        if (continent) region.continent = continent;
        if (flagIcon !== undefined) region.flagIcon = flagIcon;
        if (isActive !== undefined) region.isActive = isActive;
        if (displayOrder !== undefined) region.displayOrder = displayOrder;
        if (localization) {
            // Merge existing localization with new values
            region.localization = { ...region.localization.toObject(), ...localization };
        }
        
        await region.save();
        
        res.json({
            success: true,
            data: region,
            message: 'Region updated successfully'
        });
    } catch (error) {
        res.status(500).json({
            success: false,
            message: error.message
        });
    }
};

// Admin: Delete a region
exports.deleteRegion = async (req, res) => {
    try {
        const region = await Region.findOne({ code: req.params.code.toUpperCase() });
        
        if (!region) {
            return res.status(404).json({
                success: false,
                message: 'Region not found'
            });
        }
        
        await region.deleteOne();
        
        res.json({
            success: true,
            message: 'Region deleted successfully'
        });
    } catch (error) {
        res.status(500).json({
            success: false,
            message: error.message
        });
    }
}; 