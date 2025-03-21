/**
 * Generate an enhanced landing page for shared wishes
 * @param {Object} wish The shared wish object
 * @param {String} shortCode The wish short code
 * @returns {String} HTML for the landing page
 */
const generateWishLandingPage = (wish, shortCode) => {
    if (!wish) {
        return generateFallbackLandingPage(shortCode);
    }

    const title = wish.title || 'EventWish Greeting';
    const description = wish.description || `A special wish from ${wish.senderName} to ${wish.recipientName}`;
    
    // Get the preview URL, ensuring it's an absolute URL
    let previewUrl = wish.previewUrl || (wish.template && wish.template.thumbnailUrl) || '/images/default-preview.png';
    
    // Debug logging for preview URL
    console.log(`Original preview URL for wish ${shortCode}: ${previewUrl}`);
    console.log(`Preview URL type: ${typeof previewUrl}`);
    
    // Handle null or undefined previewUrl
    if (!previewUrl) {
        console.log(`No preview URL found for wish ${shortCode}, using default`);
        previewUrl = '/images/default-preview.png';
    }
    
    // Make sure the URL is absolute
    if (!previewUrl.startsWith('http')) {
        previewUrl = `https://eventwish2.onrender.com${previewUrl.startsWith('/') ? '' : '/'}${previewUrl}`;
    }
    
    // Ensure the URL is properly formatted
    try {
        // Test if the URL is valid by creating a URL object
        new URL(previewUrl);
    } catch (error) {
        console.error(`Invalid preview URL for wish ${shortCode}: ${previewUrl}`, error);
        previewUrl = 'https://eventwish2.onrender.com/images/default-preview.png';
    }
    
    // Create an optimized version of the image for social media sharing
    // Use encodeURIComponent to handle special characters in the URL
    const safeUrl = encodeURIComponent(previewUrl);
    const encodedUrl = Buffer.from(safeUrl).toString('base64');
    const optimizedImageUrl = `https://eventwish2.onrender.com/api/images/social/${encodedUrl}`;
    
    console.log(`Using preview URL for wish ${shortCode}: ${previewUrl}`);
    console.log(`Using optimized image URL for social sharing: ${optimizedImageUrl}`);
    
    const appUrl = 'https://play.google.com/store/apps/details?id=com.ds.eventwish';
    const deepLink = `eventwish://wish/${shortCode}`;

    return `
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>${title}</title>
            <meta name="description" content="${description}">
            
            <!-- Open Graph / Facebook -->
            <meta property="og:type" content="website">
            <meta property="og:url" content="https://eventwish2.onrender.com/wish/${shortCode}">
            <meta property="og:title" content="${title}">
            <meta property="og:description" content="${description}">
            <meta property="og:image" content="${optimizedImageUrl}">
            <meta property="og:image:width" content="1200">
            <meta property="og:image:height" content="630">
            <meta property="og:image:alt" content="${title}">
            <meta property="og:site_name" content="EventWish">
            
            <!-- WhatsApp specific -->
            <meta property="og:image:secure_url" content="${optimizedImageUrl}">
            <meta property="og:locale" content="en_US">
            
            <!-- Twitter -->
            <meta property="twitter:card" content="summary_large_image">
            <meta property="twitter:url" content="https://eventwish2.onrender.com/wish/${shortCode}">
            <meta property="twitter:title" content="${title}">
            <meta property="twitter:description" content="${description}">
            <meta property="twitter:image" content="${optimizedImageUrl}">
            <meta property="twitter:image:alt" content="${title}">
            
            <style>
                body {
                    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, 'Open Sans', 'Helvetica Neue', sans-serif;
                    margin: 0;
                    padding: 0;
                    background: #f5f5f5;
                    display: flex;
                    flex-direction: column;
                    min-height: 100vh;
                }
                .container {
                    max-width: 600px;
                    margin: 0 auto;
                    padding: 20px;
                    text-align: center;
                }
                .preview-image {
                    width: 100%;
                    max-width: 600px; /* Increased from 300px */
                    height: auto;
                    border-radius: 12px;
                    margin: 20px 0;
                    box-shadow: 0 4px 12px rgba(0,0,0,0.1);
                    object-fit: contain; /* Ensures the image maintains its aspect ratio */
                }
                .title {
                    font-size: 24px;
                    color: #333;
                    margin: 10px 0;
                }
                .description {
                    font-size: 16px;
                    color: #666;
                    margin: 10px 0 20px;
                }
                .button {
                    display: inline-block;
                    padding: 12px 24px;
                    background: #FFC107;
                    color: #000;
                    text-decoration: none;
                    border-radius: 24px;
                    font-weight: 600;
                    margin: 10px;
                    transition: all 0.3s ease;
                }
                .button:hover {
                    background: #FFB300;
                    transform: translateY(-2px);
                }
                .footer {
                    margin-top: auto;
                    padding: 20px;
                    text-align: center;
                    color: #666;
                    font-size: 14px;
                }
            </style>
        </head>
        <body>
            <div class="container">
                <img src="${previewUrl}" alt="${title}" class="preview-image" onerror="this.src='https://eventwish2.onrender.com/images/default-preview.png';">
                <h1 class="title">${title}</h1>
                <p class="description">${description}</p>
                <a href="${deepLink}" class="button">Open in App</a>
                <a href="${appUrl}" class="button">Get the App</a>
            </div>
            <div class="footer">
                Powered by EventWish
            </div>
            <script>
                // Try to open the app first
                window.location.href = '${deepLink}';
                
                // After a delay, if the app didn't open, redirect to Play Store
                setTimeout(() => {
                    window.location.href = '${appUrl}';
                }, 2000);
            </script>
        </body>
        </html>
    `;
};

/**
 * Generate a fallback landing page for shared wishes
 * @param {String} shortCode The wish short code
 * @returns {String} HTML for the fallback landing page
 */
const generateFallbackLandingPage = (shortCode) => {
    const title = 'EventWish Greeting';
    const description = 'Someone sent you a special wish!';
    const previewUrl = 'https://eventwish2.onrender.com/images/default-preview.png';
    
    // Create an optimized version of the image for social media sharing
    const safeUrl = encodeURIComponent(previewUrl);
    const encodedUrl = Buffer.from(safeUrl).toString('base64');
    const optimizedImageUrl = `https://eventwish2.onrender.com/api/images/social/${encodedUrl}`;
    
    const appUrl = 'https://play.google.com/store/apps/details?id=com.ds.eventwish';
    const deepLink = `eventwish://wish/${shortCode}`;

    return `
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>${title}</title>
            <meta name="description" content="${description}">
            
            <!-- Open Graph / Facebook -->
            <meta property="og:type" content="website">
            <meta property="og:url" content="https://eventwish2.onrender.com/wish/${shortCode}">
            <meta property="og:title" content="${title}">
            <meta property="og:description" content="${description}">
            <meta property="og:image" content="${optimizedImageUrl}">
            <meta property="og:image:width" content="1200">
            <meta property="og:image:height" content="630">
            <meta property="og:image:alt" content="${title}">
            <meta property="og:site_name" content="EventWish">
            
            <!-- WhatsApp specific -->
            <meta property="og:image:secure_url" content="${optimizedImageUrl}">
            <meta property="og:locale" content="en_US">
            
            <!-- Twitter -->
            <meta property="twitter:card" content="summary_large_image">
            <meta property="twitter:url" content="https://eventwish2.onrender.com/wish/${shortCode}">
            <meta property="twitter:title" content="${title}">
            <meta property="twitter:description" content="${description}">
            <meta property="twitter:image" content="${optimizedImageUrl}">
            <meta property="twitter:image:alt" content="${title}">
            
            <style>
                body {
                    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, 'Open Sans', 'Helvetica Neue', sans-serif;
                    margin: 0;
                    padding: 0;
                    background: #f5f5f5;
                    display: flex;
                    flex-direction: column;
                    min-height: 100vh;
                }
                .container {
                    max-width: 600px;
                    margin: 0 auto;
                    padding: 20px;
                    text-align: center;
                }
                .preview-image {
                    width: 100%;
                    max-width: 600px; /* Increased from 300px */
                    height: auto;
                    border-radius: 12px;
                    margin: 20px 0;
                    box-shadow: 0 4px 12px rgba(0,0,0,0.1);
                    object-fit: contain; /* Ensures the image maintains its aspect ratio */
                }
                .title {
                    font-size: 24px;
                    color: #333;
                    margin: 10px 0;
                }
                .description {
                    font-size: 16px;
                    color: #666;
                    margin: 10px 0 20px;
                }
                .button {
                    display: inline-block;
                    padding: 12px 24px;
                    background: #FFC107;
                    color: #000;
                    text-decoration: none;
                    border-radius: 24px;
                    font-weight: 600;
                    margin: 10px;
                    transition: all 0.3s ease;
                }
                .button:hover {
                    background: #FFB300;
                    transform: translateY(-2px);
                }
                .footer {
                    margin-top: auto;
                    padding: 20px;
                    text-align: center;
                    color: #666;
                    font-size: 14px;
                }
            </style>
        </head>
        <body>
            <div class="container">
                <img src="${previewUrl}" alt="${title}" class="preview-image" onerror="this.src='https://eventwish2.onrender.com/images/default-preview.png';">
                <h1 class="title">${title}</h1>
                <p class="description">${description}</p>
                <a href="${deepLink}" class="button">Open in App</a>
                <a href="${appUrl}" class="button">Get the App</a>
            </div>
            <div class="footer">
                Powered by EventWish
            </div>
            <script>
                // Try to open the app first
                window.location.href = '${deepLink}';
                
                // After a delay, if the app didn't open, redirect to Play Store
                setTimeout(() => {
                    window.location.href = '${appUrl}';
                }, 2000);
            </script>
        </body>
        </html>
    `;
};

module.exports = {
    generateWishLandingPage,
    generateFallbackLandingPage
}; 