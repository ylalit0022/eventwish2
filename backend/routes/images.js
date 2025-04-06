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

// Define the default image path
const DEFAULT_IMAGE_PATH = path.join(__dirname, '../images/default-preview.svg');
const DEFAULT_IMAGE_CONTENT_TYPE = 'image/svg+xml';

// Helper function to download an image
const downloadImage = (url) => {
    return new Promise((resolve, reject) => {
        try {
            // Decode the URL if it's encoded
            const decodedUrl = decodeURIComponent(url);
            console.log(`Downloading image from URL: ${decodedUrl}`);
            
            const parsedUrl = new URL(decodedUrl);
            const protocol = parsedUrl.protocol === 'https:' ? https : http;
            
            const request = protocol.get(decodedUrl, (response) => {
                // Handle redirects
                if (response.statusCode === 301 || response.statusCode === 302) {
                    const redirectUrl = response.headers.location;
                    console.log(`Following redirect to: ${redirectUrl}`);
                    return downloadImage(redirectUrl)
                        .then(resolve)
                        .catch(reject);
                }
                
                if (response.statusCode !== 200) {
                    reject(new Error(`Failed to fetch image: ${response.statusCode}`));
                    return;
                }
                
                const chunks = [];
                response.on('data', (chunk) => chunks.push(chunk));
                response.on('end', () => {
                    console.log(`Successfully downloaded image, content-type: ${response.headers['content-type']}`);
                    resolve({
                        buffer: Buffer.concat(chunks),
                        contentType: response.headers['content-type'] || 'image/jpeg'
                    });
                });
            });
            
            // Set a timeout to prevent hanging requests
            request.setTimeout(10000, () => {
                request.abort();
                reject(new Error('Request timeout after 10 seconds'));
            });
            
            request.on('error', (err) => {
                console.error(`Error downloading image: ${err.message}`);
                reject(err);
            });
        } catch (error) {
            console.error(`Error in downloadImage: ${error.message}`);
            reject(error);
        }
    });
};

// Helper function to send the default image
function sendDefaultImage(res) {
    try {
        if (fs.existsSync(DEFAULT_IMAGE_PATH)) {
            console.log(`Sending default image: ${DEFAULT_IMAGE_PATH}`);
            res.setHeader('Cache-Control', 'public, max-age=86400'); // 24 hours
            res.setHeader('Content-Type', DEFAULT_IMAGE_CONTENT_TYPE);
            return res.sendFile(DEFAULT_IMAGE_PATH);
        }
    } catch (error) {
        console.error('Error sending default image:', error);
    }
    
    // If all else fails, send a text response
    res.status(500).send('Image not available');
}

// Serve images for social media sharing
router.get('/social/:encodedUrl', async (req, res) => {
    try {
        const { encodedUrl } = req.params;
        
        // Decode the base64 URL
        let imageUrl;
        try {
            imageUrl = decodeURIComponent(Buffer.from(encodedUrl, 'base64').toString('utf-8'));
            console.log(`Decoded image URL: ${imageUrl}`);
        } catch (error) {
            console.error(`Error decoding URL: ${error.message}`);
            return sendDefaultImage(res);
        }
        
        console.log(`Serving image for social sharing: ${imageUrl}`);
        
        // For local development, if the URL is for eventwish2.onrender.com, use the local file
        if (imageUrl.includes('eventwish2.onrender.com')) {
            const localPath = imageUrl.replace('https://eventwish2.onrender.com', '');
            const fullLocalPath = path.join(__dirname, '..', localPath.startsWith('/') ? localPath.substring(1) : localPath);
            
            console.log(`Looking for local file: ${fullLocalPath}`);
            
            if (fs.existsSync(fullLocalPath)) {
                console.log(`Serving local file: ${fullLocalPath}`);
                
                // Determine content type based on file extension
                const ext = path.extname(fullLocalPath).toLowerCase();
                let contentType = 'image/jpeg';
                if (ext === '.png') contentType = 'image/png';
                if (ext === '.gif') contentType = 'image/gif';
                if (ext === '.webp') contentType = 'image/webp';
                if (ext === '.svg') contentType = 'image/svg+xml';
                
                res.setHeader('Cache-Control', 'public, max-age=86400'); // 24 hours
                res.setHeader('Content-Type', contentType);
                
                return res.sendFile(fullLocalPath);
            }
        }
        
        // If it's a remote URL, try to download it
        if (imageUrl.startsWith('http')) {
            try {
                const { buffer, contentType } = await downloadImage(imageUrl);
                
                // Set cache headers
                res.setHeader('Cache-Control', 'public, max-age=86400'); // 24 hours
                res.setHeader('Content-Type', contentType);
                
                // Send the image directly
                return res.send(buffer);
            } catch (error) {
                console.error(`Error downloading image from ${imageUrl}:`, error);
                return sendDefaultImage(res);
            }
        } else {
            // It's a local path
            const localPath = path.join(__dirname, '..', imageUrl.startsWith('/') ? imageUrl.substring(1) : imageUrl);
            console.log(`Looking for local image at: ${localPath}`);
            
            if (!fs.existsSync(localPath)) {
                console.error(`Local image not found: ${localPath}`);
                return sendDefaultImage(res);
            }
            
            // Determine content type based on file extension
            const ext = path.extname(localPath).toLowerCase();
            let contentType = 'image/jpeg';
            if (ext === '.png') contentType = 'image/png';
            if (ext === '.gif') contentType = 'image/gif';
            if (ext === '.webp') contentType = 'image/webp';
            if (ext === '.svg') contentType = 'image/svg+xml';
            
            res.setHeader('Cache-Control', 'public, max-age=86400'); // 24 hours
            res.setHeader('Content-Type', contentType);
            
            // Send the image
            return res.sendFile(localPath);
        }
    } catch (error) {
        console.error('Error serving image:', error);
        return sendDefaultImage(res);
    }
});

module.exports = router; 