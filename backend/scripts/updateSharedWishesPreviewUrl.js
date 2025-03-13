require('dotenv').config({ path: require('path').resolve(__dirname, '../.env') });
const mongoose = require('mongoose');
const SharedWish = require('../models/SharedWish');
const Template = require('../models/Template');

const updateSharedWishesPreviewUrl = async () => {
    try {
        // Connect to MongoDB
        await mongoose.connect(process.env.MONGODB_URI, {
            useNewUrlParser: true,
            useUnifiedTopology: true
        });
        console.log('Connected to MongoDB');

        // Find all shared wishes
        const sharedWishes = await SharedWish.find().populate('template');
        console.log(`Found ${sharedWishes.length} shared wishes`);

        let updatedCount = 0;
        let missingPreviewUrlCount = 0;
        let absoluteUrlCount = 0;
        let relativeUrlCount = 0;

        // Update each shared wish
        for (const wish of sharedWishes) {
            let needsUpdate = false;
            
            // Check if previewUrl is missing
            if (!wish.previewUrl || wish.previewUrl === '') {
                missingPreviewUrlCount++;
                
                // Try to get previewUrl from template
                if (wish.template) {
                    let templatePreviewUrl = wish.template.previewUrl || wish.template.thumbnailUrl || '';
                    
                    if (templatePreviewUrl) {
                        // Make sure the URL is absolute
                        if (!templatePreviewUrl.startsWith('http')) {
                            templatePreviewUrl = `https://eventwish2.onrender.com${templatePreviewUrl.startsWith('/') ? '' : '/'}${templatePreviewUrl}`;
                        }
                        
                        wish.previewUrl = templatePreviewUrl;
                        console.log(`Updated missing previewUrl for wish ${wish.shortCode} with template previewUrl: ${templatePreviewUrl}`);
                        needsUpdate = true;
                    }
                }
            } 
            // Check if previewUrl is relative
            else if (!wish.previewUrl.startsWith('http')) {
                relativeUrlCount++;
                
                // Make it absolute
                wish.previewUrl = `https://eventwish2.onrender.com${wish.previewUrl.startsWith('/') ? '' : '/'}${wish.previewUrl}`;
                console.log(`Updated relative previewUrl for wish ${wish.shortCode} to absolute URL: ${wish.previewUrl}`);
                needsUpdate = true;
            } else {
                absoluteUrlCount++;
            }
            
            // Save the wish if it was updated
            if (needsUpdate) {
                await wish.save();
                updatedCount++;
            }
        }

        console.log(`
Update Summary:
- Total shared wishes: ${sharedWishes.length}
- Wishes with missing previewUrl: ${missingPreviewUrlCount}
- Wishes with relative previewUrl: ${relativeUrlCount}
- Wishes with absolute previewUrl: ${absoluteUrlCount}
- Total wishes updated: ${updatedCount}
        `);

        // Disconnect from MongoDB
        await mongoose.disconnect();
        console.log('Disconnected from MongoDB');
    } catch (error) {
        console.error('Error updating shared wishes:', error);
    }
};

// Run the update function
updateSharedWishesPreviewUrl(); 