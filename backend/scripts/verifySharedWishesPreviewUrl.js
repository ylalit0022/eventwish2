const mongoose = require('mongoose');
const SharedWish = require('../models/SharedWish');
require('dotenv').config({ path: require('path').resolve(__dirname, '../.env') });

async function verifySharedWishesPreviewUrl() {
    try {
        // Connect to MongoDB
        await mongoose.connect(process.env.MONGODB_URI, {
            useNewUrlParser: true,
            useUnifiedTopology: true
        });
        console.log('Connected to MongoDB successfully');

        // Get total count of shared wishes
        const totalCount = await SharedWish.countDocuments();
        console.log(`Total shared wishes: ${totalCount}`);
        
        // Get count of shared wishes with previewUrl
        const withPreviewUrlCount = await SharedWish.countDocuments({
            previewUrl: { $exists: true, $ne: null, $ne: "" }
        });
        console.log(`Shared wishes with previewUrl: ${withPreviewUrlCount}`);
        
        // Get count of shared wishes without previewUrl
        const withoutPreviewUrlCount = await SharedWish.countDocuments({
            $or: [
                { previewUrl: { $exists: false } },
                { previewUrl: null },
                { previewUrl: "" }
            ]
        });
        console.log(`Shared wishes without previewUrl: ${withoutPreviewUrlCount}`);
        
        // Get shared wishes without previewUrl
        if (withoutPreviewUrlCount > 0) {
            const wishesWithoutPreviewUrl = await SharedWish.find({
                $or: [
                    { previewUrl: { $exists: false } },
                    { previewUrl: null },
                    { previewUrl: "" }
                ]
            }).select('shortCode');
            
            console.log('Shared wishes without previewUrl:');
            wishesWithoutPreviewUrl.forEach(wish => {
                console.log(`- ${wish.shortCode}`);
            });
        }
        
        // Verify that all shared wishes have a template
        const withoutTemplateCount = await SharedWish.countDocuments({
            $or: [
                { template: { $exists: false } },
                { template: null }
            ]
        });
        console.log(`Shared wishes without template: ${withoutTemplateCount}`);
        
        if (withoutTemplateCount > 0) {
            const wishesWithoutTemplate = await SharedWish.find({
                $or: [
                    { template: { $exists: false } },
                    { template: null }
                ]
            }).select('shortCode');
            
            console.log('Shared wishes without template:');
            wishesWithoutTemplate.forEach(wish => {
                console.log(`- ${wish.shortCode}`);
            });
        }
    } catch (error) {
        console.error('Error verifying shared wishes:', error);
    } finally {
        mongoose.connection.close();
        console.log('MongoDB connection closed');
    }
}

verifySharedWishesPreviewUrl(); 