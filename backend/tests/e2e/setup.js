/**
 * End-to-End Testing Setup
 * 
 * This file contains setup and teardown functions for Puppeteer-based end-to-end tests.
 */

const puppeteer = require('puppeteer');
const { MongoMemoryServer } = require('mongodb-memory-server');
const mongoose = require('mongoose');
const { exec } = require('child_process');
const path = require('path');
const fs = require('fs');

// Configuration
const config = {
  baseUrl: process.env.TEST_BASE_URL || 'http://localhost:3000',
  screenshotDir: path.join(__dirname, 'screenshots'),
  headless: process.env.HEADLESS !== 'false',
  slowMo: process.env.SLOW_MO ? parseInt(process.env.SLOW_MO, 10) : 0,
  timeout: process.env.TEST_TIMEOUT ? parseInt(process.env.TEST_TIMEOUT, 10) : 30000,
};

// Ensure screenshot directory exists
if (!fs.existsSync(config.screenshotDir)) {
  fs.mkdirSync(config.screenshotDir, { recursive: true });
}

// Global variables
let browser;
let mongoServer;
let serverProcess;

/**
 * Setup function to be called before all tests
 */
async function setup() {
  console.log('Setting up end-to-end test environment...');
  
  // Start MongoDB memory server
  mongoServer = await MongoMemoryServer.create();
  const mongoUri = mongoServer.getUri();
  
  // Connect to the in-memory database
  await mongoose.connect(mongoUri);
  console.log('Connected to in-memory MongoDB instance');
  
  // Set environment variables for the test server
  process.env.MONGODB_URI = mongoUri;
  process.env.NODE_ENV = 'test';
  process.env.PORT = '3000';
  process.env.ENABLE_SWAGGER = 'true';
  
  // Start the server in test mode
  return new Promise((resolve, reject) => {
    console.log('Starting test server...');
    serverProcess = exec('node server.js', {
      env: { ...process.env, NODE_ENV: 'test', MONGODB_URI: mongoUri },
      cwd: path.join(__dirname, '../../'),
    });
    
    serverProcess.stdout.on('data', (data) => {
      console.log(`Server: ${data.toString().trim()}`);
      // Resolve when server is ready
      if (data.toString().includes('Server is running on port')) {
        console.log('Test server started successfully');
        resolve();
      }
    });
    
    serverProcess.stderr.on('data', (data) => {
      console.error(`Server Error: ${data.toString().trim()}`);
    });
    
    // Reject if server fails to start within timeout
    setTimeout(() => {
      reject(new Error('Server failed to start within timeout period'));
    }, 10000);
  })
    .then(async () => {
      // Launch browser
      console.log('Launching browser...');
      browser = await puppeteer.launch({
        headless: config.headless ? 'new' : false,
        slowMo: config.slowMo,
        args: [
          '--no-sandbox',
          '--disable-setuid-sandbox',
          '--disable-dev-shm-usage',
          '--disable-accelerated-2d-canvas',
          '--disable-gpu',
        ],
        defaultViewport: { width: 1280, height: 800 },
      });
      console.log('Browser launched successfully');
    });
}

/**
 * Teardown function to be called after all tests
 */
async function teardown() {
  console.log('Tearing down end-to-end test environment...');
  
  // Close browser
  if (browser) {
    await browser.close();
    console.log('Browser closed');
  }
  
  // Stop server
  if (serverProcess) {
    serverProcess.kill();
    console.log('Test server stopped');
  }
  
  // Disconnect from MongoDB
  if (mongoose.connection.readyState !== 0) {
    await mongoose.disconnect();
    console.log('Disconnected from MongoDB');
  }
  
  // Stop MongoDB memory server
  if (mongoServer) {
    await mongoServer.stop();
    console.log('MongoDB memory server stopped');
  }
}

/**
 * Create a new browser page with common settings
 */
async function createPage() {
  const page = await browser.newPage();
  
  // Set default timeout
  page.setDefaultTimeout(config.timeout);
  
  // Enable console logging from the page
  page.on('console', (msg) => {
    console.log(`Browser Console [${msg.type()}]: ${msg.text()}`);
  });
  
  // Log page errors
  page.on('pageerror', (error) => {
    console.error(`Browser Page Error: ${error.message}`);
  });
  
  return page;
}

/**
 * Take a screenshot and save it to the screenshots directory
 * @param {Page} page - Puppeteer page object
 * @param {string} name - Screenshot name
 */
async function takeScreenshot(page, name) {
  const screenshotPath = path.join(config.screenshotDir, `${name}-${Date.now()}.png`);
  await page.screenshot({ path: screenshotPath, fullPage: true });
  console.log(`Screenshot saved to: ${screenshotPath}`);
  return screenshotPath;
}

module.exports = {
  setup,
  teardown,
  createPage,
  takeScreenshot,
  config,
  getBrowser: () => browser,
};
