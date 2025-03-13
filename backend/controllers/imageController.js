const https = require('https');
const http = require('http');
const url = require('url');
const fs = require('fs');
const path = require('path');

/**
 * Fetch an image from a URL and return it
 * @param {Object} req - Express request object
 * @param {Object} res - Express response object
 */
exports.fetchImage = async (req, res) => {
    try {
        const imageUrl = req.query.url;
        
        if (!imageUrl) {
            return res.status(400).json({ message: 'URL parameter is required' });
        }
        
        // Parse the URL
        const parsedUrl = url.parse(imageUrl);
        
        // Validate URL
        if (!parsedUrl.protocol || !parsedUrl.hostname) {
            return res.status(400).json({ message: 'Invalid URL' });
        }
        
        // Choose the appropriate protocol
        const protocol = parsedUrl.protocol === 'https:' ? https : http;
        
        // Make the request
        protocol.get(imageUrl, (response) => {
            // Check if the response is an image
            const contentType = response.headers['content-type'];
            if (!contentType || !contentType.startsWith('image/')) {
                return res.status(400).json({ message: 'URL does not point to an image' });
            }
            
            // Set the appropriate headers
            res.setHeader('Content-Type', contentType);
            res.setHeader('Cache-Control', 'public, max-age=86400'); // Cache for 1 day
            
            // Pipe the image data to the response
            response.pipe(res);
        }).on('error', (error) => {
            console.error('Error fetching image:', error);
            res.status(500).json({ message: 'Error fetching image', error: error.message });
        });
    } catch (error) {
        console.error('Error in fetchImage controller:', error);
        res.status(500).json({ message: 'Server error', error: error.message });
    }
}; 