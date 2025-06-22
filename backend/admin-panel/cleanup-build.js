#!/usr/bin/env node

const fs = require('fs');
const path = require('path');

console.log('🧹 Cleaning up admin panel build files...');

// Function to remove directory recursively
function removeDirectory(dirPath) {
    if (fs.existsSync(dirPath)) {
        console.log(`📁 Removing directory: ${dirPath}`);
        fs.rmSync(dirPath, { recursive: true, force: true });
        console.log(`✅ Removed: ${dirPath}`);
    } else {
        console.log(`⚠️  Directory not found: ${dirPath}`);
    }
}

// Function to remove file
function removeFile(filePath) {
    if (fs.existsSync(filePath)) {
        console.log(`📄 Removing file: ${filePath}`);
        fs.unlinkSync(filePath);
        console.log(`✅ Removed: ${filePath}`);
    } else {
        console.log(`⚠️  File not found: ${filePath}`);
    }
}

try {
    // Remove build directory
    removeDirectory('./build');
    
    // Remove dist directory if it exists
    removeDirectory('./dist');
    
    // Remove any webpack build files
    removeFile('./bundle.js');
    removeFile('./bundle.js.map');
    
    console.log('🎉 Cleanup completed successfully!');
    console.log('📝 Note: Admin panel is now configured for LOCAL USE ONLY');
    console.log('🚀 Use "npm run dev" to run the admin panel locally');
    
} catch (error) {
    console.error('❌ Error during cleanup:', error.message);
    process.exit(1);
} 