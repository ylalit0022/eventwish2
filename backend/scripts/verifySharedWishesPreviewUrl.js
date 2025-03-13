require('dotenv').config({ path: require('path').resolve(__dirname, '../.env') });
const mongoose = require('mongoose');
const SharedWish = require('../models/SharedWish');

async function verifySharedWishesPreviewUrl() {
    try {
        // Connect to MongoDB
        await mongoose.connect(process.env.MONGODB_URI, {
            useNewUrlParser: true,
            useUnifiedTopology: true
        });
        console.log('Connected to MongoDB successfully');

        // Get all shared wishes
        const sharedWishes = await SharedWish.find().populate('template');
        console.log(`Total shared wishes: ${sharedWishes.length}`);

        // Count shared wishes with and without previewUrl
        const withPreviewUrl = sharedWishes.filter(wish => wish.previewUrl && wish.previewUrl.trim() !== '');
        const withoutPreviewUrl = sharedWishes.filter(wish => !wish.previewUrl || wish.previewUrl.trim() === '');
        
        // Count shared wishes with absolute and relative previewUrl
        const withAbsoluteUrl = withPreviewUrl.filter(wish => wish.previewUrl.startsWith('http'));
        const withRelativeUrl = withPreviewUrl.filter(wish => !wish.previewUrl.startsWith('http'));

        console.log(`Shared wishes with previewUrl: ${withPreviewUrl.length}`);
        console.log(`- With absolute URL: ${withAbsoluteUrl.length}`);
        console.log(`- With relative URL: ${withRelativeUrl.length}`);
        console.log(`Shared wishes without previewUrl: ${withoutPreviewUrl.length}`);

        // Log shared wishes without previewUrl
        if (withoutPreviewUrl.length > 0) {
            console.log('Shared wishes without previewUrl:');
            withoutPreviewUrl.forEach(wish => {
                console.log(`- ${wish.shortCode}`);
            });
        }
        
        // Log shared wishes with relative previewUrl
        if (withRelativeUrl.length > 0) {
            console.log('Shared wishes with relative previewUrl:');
            withRelativeUrl.forEach(wish => {
                console.log(`- ${wish.shortCode}: ${wish.previewUrl}`);
            });
        }

        // Count shared wishes without template
        const withoutTemplate = sharedWishes.filter(wish => !wish.template);
        console.log(`Shared wishes without template: ${withoutTemplate.length}`);

        // Log shared wishes without template
        if (withoutTemplate.length > 0) {
            console.log('Shared wishes without template:');
            withoutTemplate.forEach(wish => {
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