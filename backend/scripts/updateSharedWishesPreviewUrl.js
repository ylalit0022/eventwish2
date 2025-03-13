const mongoose = require('mongoose');
const SharedWish = require('../models/SharedWish');
const Template = require('../models/Template');
require('dotenv').config({ path: require('path').resolve(__dirname, '../.env') });

async function updateSharedWishesPreviewUrl() {
    try {
        // Connect to MongoDB
        await mongoose.connect(process.env.MONGODB_URI, {
            useNewUrlParser: true,
            useUnifiedTopology: true
        });
        console.log('Connected to MongoDB successfully');

        // Get all shared wishes that don't have a previewUrl
        const sharedWishes = await SharedWish.find({
            $or: [
                { previewUrl: { $exists: false } },
                { previewUrl: null },
                { previewUrl: "" }
            ]
        }).populate('template');
        
        console.log(`Found ${sharedWishes.length} shared wishes without previewUrl`);

        // Update each shared wish with the previewUrl from its template
        let updatedCount = 0;
        let errorCount = 0;
        
        for (const wish of sharedWishes) {
            try {
                if (!wish.template) {
                    console.log(`Wish ${wish.shortCode} has no associated template`);
                    continue;
                }
                
                // Get the template's previewUrl
                const templateId = wish.template._id;
                const template = await Template.findById(templateId);
                
                if (!template) {
                    console.log(`Template ${templateId} not found for wish ${wish.shortCode}`);
                    continue;
                }
                
                if (!template.previewUrl) {
                    console.log(`Template ${templateId} has no previewUrl for wish ${wish.shortCode}`);
                    continue;
                }
                
                // Update the shared wish with the template's previewUrl
                wish.previewUrl = template.previewUrl;
                await wish.save();
                
                console.log(`Updated wish ${wish.shortCode} with previewUrl: ${wish.previewUrl}`);
                updatedCount++;
            } catch (error) {
                console.error(`Error updating wish ${wish.shortCode}:`, error);
                errorCount++;
            }
        }

        console.log(`Updated ${updatedCount} shared wishes with previewUrl`);
        console.log(`Encountered errors with ${errorCount} shared wishes`);
        
        // Get count of shared wishes that still don't have a previewUrl
        const remainingCount = await SharedWish.countDocuments({
            $or: [
                { previewUrl: { $exists: false } },
                { previewUrl: null },
                { previewUrl: "" }
            ]
        });
        
        console.log(`${remainingCount} shared wishes still don't have a previewUrl`);
    } catch (error) {
        console.error('Error updating shared wishes:', error);
    } finally {
        mongoose.connection.close();
        console.log('MongoDB connection closed');
    }
}

updateSharedWishesPreviewUrl(); 