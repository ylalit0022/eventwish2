/**
 * Test script for checking routes
 * 
 * This script attempts to require each route file individually to identify which one might be causing issues
 */

const fs = require('fs');
const path = require('path');

// Initialize environment
require('../config/env-loader');

// List of all route files
const routesDir = path.join(__dirname, '..', 'routes');
const routeFiles = fs.readdirSync(routesDir).filter(file => file.endsWith('.js'));

console.log('Testing each route file individually...');

// Try to require each route file
routeFiles.forEach(file => {
  try {
    console.log(`Testing route: ${file}`);
    require(`../routes/${file}`);
    console.log(`✓ Successfully loaded route: ${file}`);
  } catch (error) {
    console.error(`✗ Error loading route ${file}:`);
    console.error(`  ${error.message}`);
    
    // Check if this is a missing dependency
    if (error.code === 'MODULE_NOT_FOUND') {
      const match = error.message.match(/Cannot find module '([^']+)'/);
      if (match) {
        const missingModule = match[1];
        console.error(`  Missing module: ${missingModule}`);
        
        // Suggest fix
        if (missingModule.startsWith('.')) {
          console.error('  This is a local module. Check if the file exists and has the correct path.');
        } else {
          console.error(`  This is an npm module. Try installing it with: npm install ${missingModule}`);
        }
      }
    }
  }
});

console.log('Route testing complete.'); 