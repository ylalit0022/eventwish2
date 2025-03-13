/**
 * Script to create a default preview image
 */
const fs = require('fs');
const path = require('path');

// Create a simple SVG image with text
const createDefaultPreviewSVG = () => {
    const width = 1200;
    const height = 630;
    
    const svg = `
    <svg width="${width}" height="${height}" xmlns="http://www.w3.org/2000/svg">
        <rect width="100%" height="100%" fill="#FFC107"/>
        <text x="50%" y="50%" font-family="Arial" font-size="60" text-anchor="middle" fill="#333">
            EventWish Greeting
        </text>
        <text x="50%" y="60%" font-family="Arial" font-size="40" text-anchor="middle" fill="#333">
            Open to view your special wish
        </text>
    </svg>
    `;
    
    return svg;
};

// Main function
const createDefaultPreview = () => {
    const imagesDir = path.join(__dirname, '../images');
    const defaultPreviewPath = path.join(imagesDir, 'default-preview.svg');
    
    // Create images directory if it doesn't exist
    if (!fs.existsSync(imagesDir)) {
        console.log(`Creating images directory: ${imagesDir}`);
        fs.mkdirSync(imagesDir, { recursive: true });
    }
    
    // Create default preview image
    const svg = createDefaultPreviewSVG();
    fs.writeFileSync(defaultPreviewPath, svg);
    console.log(`Created default preview image: ${defaultPreviewPath}`);
    
    // Also create a PNG version
    const defaultPreviewPngPath = path.join(imagesDir, 'default-preview.png');
    console.log(`To create a PNG version, you can convert the SVG using an image processing library.`);
    console.log(`For now, we've created an SVG version at: ${defaultPreviewPath}`);
    
    // Create a simple HTML file that uses the SVG
    const htmlPath = path.join(imagesDir, 'default-preview.html');
    const html = `
    <!DOCTYPE html>
    <html>
    <head>
        <title>Default Preview</title>
        <style>
            body { margin: 0; padding: 0; display: flex; justify-content: center; align-items: center; height: 100vh; }
            img { max-width: 100%; height: auto; }
        </style>
    </head>
    <body>
        <img src="default-preview.svg" alt="Default Preview">
    </body>
    </html>
    `;
    fs.writeFileSync(htmlPath, html);
    console.log(`Created HTML preview file: ${htmlPath}`);
};

// Run the function
createDefaultPreview(); 