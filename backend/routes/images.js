const express = require('express');
const router = express.Router();
const { fetchImage } = require('../controllers/imageController');
const path = require('path');
const fs = require('fs');
const http = require('http');
const https = require('https');
const crypto = require('crypto');
const { URL } = require('url');

// Route to fetch an image from a URL
router.get('/fetch', fetchImage);

// Serve static images from the images directory
router.use('/', express.static(path.join(__dirname, '../images')));

// Helper function to download an image
const downloadImage = (url) => {
    return new Promise((resolve, reject) => {
        const parsedUrl = new URL(url);
        const protocol = parsedUrl.protocol === 'https:' ? https : http;
        
        protocol.get(url, (response) => {
            if (response.statusCode !== 200) {
                reject(new Error(`Failed to fetch image: ${response.statusCode}`));
                return;
            }
            
            const chunks = [];
            response.on('data', (chunk) => chunks.push(chunk));
            response.on('end', () => resolve({
                buffer: Buffer.concat(chunks),
                contentType: response.headers['content-type'] || 'image/jpeg'
            }));
        }).on('error', reject);
    });
};

// Serve images for social media sharing
router.get('/social/:encodedUrl', async (req, res) => {
    try {
        const { encodedUrl } = req.params;
        const imageUrl = Buffer.from(encodedUrl, 'base64').toString('utf-8');
        
        console.log(`Serving image for social sharing: ${imageUrl}`);
        
        // Create a hash of the URL to use as the filename
        const hash = crypto.createHash('md5').update(imageUrl).digest('hex');
        const cacheDir = path.join(__dirname, '../cache');
        const cachePath = path.join(cacheDir, `${hash}.jpg`);
        
        // Create cache directory if it doesn't exist
        if (!fs.existsSync(cacheDir)) {
            fs.mkdirSync(cacheDir, { recursive: true });
        }
        
        // Check if the image is already cached
        if (fs.existsSync(cachePath)) {
            console.log(`Serving cached image: ${cachePath}`);
            return res.sendFile(cachePath);
        }
        
        // Fetch the image
        if (imageUrl.startsWith('http')) {
            // Fetch from URL
            try {
                const { buffer, contentType } = await downloadImage(imageUrl);
                
                // Save the image to cache
                fs.writeFileSync(cachePath, buffer);
                
                // Set cache headers
                res.setHeader('Cache-Control', 'public, max-age=86400'); // 24 hours
                res.setHeader('Content-Type', contentType);
                
                // Send the image
                return res.sendFile(cachePath);
            } catch (error) {
                console.error(`Error downloading image from ${imageUrl}:`, error);
                throw error;
            }
        } else {
            // Load from local file
            const localPath = path.join(__dirname, '..', imageUrl.startsWith('/') ? imageUrl.substring(1) : imageUrl);
            if (!fs.existsSync(localPath)) {
                throw new Error(`Local image not found: ${localPath}`);
            }
            
            // Copy the file to cache
            fs.copyFileSync(localPath, cachePath);
            
            // Set cache headers
            res.setHeader('Cache-Control', 'public, max-age=86400'); // 24 hours
            res.setHeader('Content-Type', 'image/jpeg');
            
            // Send the image
            return res.sendFile(cachePath);
        }
    } catch (error) {
        console.error('Error serving image:', error);
        res.status(500).send('Error serving image');
    }
});

module.exports = router; 