/**
 * End-to-End Tests for AdMob Client Integration
 * 
 * This file contains tests for the client-side AdMob integration.
 */

const { setup, teardown, createPage, takeScreenshot, config } = require('./setup');
const path = require('path');
const fs = require('fs');
const mongoose = require('mongoose');
const AdMob = require('../../models/AdMob');
const AdImpression = require('../../models/AdImpression');
const AdClick = require('../../models/AdClick');

// Test data
const testAdConfig = {
  name: 'Test Banner Ad',
  adUnitId: 'ca-app-pub-3940256099942544/6300978111', // Test ad unit ID
  format: 'BANNER',
  status: 'ACTIVE',
  refreshRate: 60,
  position: 'BOTTOM',
  targeting: {
    platforms: ['ANDROID', 'IOS'],
    minOsVersion: '9.0',
    countries: ['US', 'CA'],
    languages: ['en'],
    userTypes: ['NEW', 'RETURNING']
  }
};

// Test client HTML file path
const testClientPath = path.join(__dirname, 'test-client.html');

// Create test client HTML file
function createTestClient() {
  const html = `
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>AdMob Client Test</title>
  <style>
    body {
      font-family: Arial, sans-serif;
      max-width: 800px;
      margin: 0 auto;
      padding: 20px;
    }
    .ad-container {
      border: 1px dashed #ccc;
      padding: 10px;
      margin: 20px 0;
      min-height: 100px;
      display: flex;
      align-items: center;
      justify-content: center;
      background-color: #f9f9f9;
    }
    .controls {
      margin: 20px 0;
      padding: 10px;
      background-color: #eee;
      border-radius: 5px;
    }
    button {
      padding: 8px 16px;
      margin-right: 10px;
      cursor: pointer;
    }
    .status {
      margin-top: 20px;
      padding: 10px;
      border: 1px solid #ddd;
      border-radius: 5px;
      background-color: #f5f5f5;
    }
    .log {
      margin-top: 20px;
      padding: 10px;
      border: 1px solid #ddd;
      border-radius: 5px;
      background-color: #f5f5f5;
      max-height: 200px;
      overflow-y: auto;
      font-family: monospace;
      font-size: 12px;
    }
  </style>
</head>
<body>
  <h1>AdMob Client Test</h1>
  
  <div class="controls">
    <button id="loadAd">Load Ad</button>
    <button id="refreshAd">Refresh Ad</button>
    <button id="clickAd">Simulate Click</button>
    <button id="clearLog">Clear Log</button>
  </div>
  
  <div class="status">
    <p>Status: <span id="status">Idle</span></p>
    <p>Ad ID: <span id="adId">None</span></p>
    <p>Format: <span id="adFormat">None</span></p>
  </div>
  
  <div class="ad-container" id="adContainer">
    <p>Ad will appear here</p>
  </div>
  
  <div class="log" id="log"></div>

  <script>
    // Configuration
    const API_BASE_URL = '${config.baseUrl}';
    const API_KEY = 'test-api-key';
    
    // DOM Elements
    const adContainer = document.getElementById('adContainer');
    const statusEl = document.getElementById('status');
    const adIdEl = document.getElementById('adId');
    const adFormatEl = document.getElementById('adFormat');
    const logEl = document.getElementById('log');
    
    // State
    let currentAd = null;
    
    // Logging function
    function log(message) {
      const entry = document.createElement('div');
      entry.textContent = \`[\${new Date().toISOString()}] \${message}\`;
      logEl.appendChild(entry);
      logEl.scrollTop = logEl.scrollHeight;
      console.log(message);
    }
    
    // Clear log
    document.getElementById('clearLog').addEventListener('click', () => {
      logEl.innerHTML = '';
      log('Log cleared');
    });
    
    // Load ad
    async function loadAd() {
      try {
        statusEl.textContent = 'Loading...';
        log('Requesting ad configuration...');
        
        const userContext = {
          platform: 'WEB',
          osVersion: '10',
          language: 'en',
          country: 'US',
          userId: 'test-user-' + Date.now(),
          userType: 'TEST',
          deviceType: 'DESKTOP'
        };
        
        const response = await fetch(\`\${API_BASE_URL}/api/admob/config\`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'x-api-key': API_KEY
          },
          body: JSON.stringify({
            format: 'BANNER',
            context: userContext
          })
        });
        
        if (!response.ok) {
          throw new Error(\`HTTP error! status: \${response.status}\`);
        }
        
        const data = await response.json();
        log(\`Received ad configuration: \${JSON.stringify(data)}\`);
        
        if (!data.success) {
          throw new Error(data.message || 'Failed to load ad configuration');
        }
        
        currentAd = data.ad;
        
        // Update UI
        statusEl.textContent = 'Ad Loaded';
        adIdEl.textContent = currentAd.adUnitId;
        adFormatEl.textContent = currentAd.format;
        
        // Render ad
        adContainer.innerHTML = \`
          <div class="test-ad" style="width: 320px; height: 50px; background-color: #4CAF50; color: white; display: flex; align-items: center; justify-content: center;">
            <strong>TEST AD</strong> - \${currentAd.adUnitId}
          </div>
        \`;
        
        // Track impression
        trackImpression(currentAd);
        
      } catch (error) {
        statusEl.textContent = 'Error';
        log(\`Error loading ad: \${error.message}\`);
        console.error(error);
      }
    }
    
    // Track impression
    async function trackImpression(ad) {
      try {
        log('Tracking impression...');
        
        const response = await fetch(\`\${API_BASE_URL}/api/admob/impression\`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'x-api-key': API_KEY
          },
          body: JSON.stringify({
            adId: ad._id,
            adUnitId: ad.adUnitId,
            format: ad.format,
            timestamp: new Date().toISOString(),
            userId: 'test-user-' + Date.now(),
            sessionId: 'test-session-' + Date.now(),
            deviceInfo: {
              platform: 'WEB',
              osVersion: '10',
              browser: 'Chrome',
              screenSize: \`\${window.innerWidth}x\${window.innerHeight}\`
            }
          })
        });
        
        if (!response.ok) {
          throw new Error(\`HTTP error! status: \${response.status}\`);
        }
        
        const data = await response.json();
        log(\`Impression tracked: \${JSON.stringify(data)}\`);
        
      } catch (error) {
        log(\`Error tracking impression: \${error.message}\`);
        console.error(error);
      }
    }
    
    // Track click
    async function trackClick(ad) {
      try {
        log('Tracking click...');
        
        const response = await fetch(\`\${API_BASE_URL}/api/admob/click\`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'x-api-key': API_KEY
          },
          body: JSON.stringify({
            adId: ad._id,
            adUnitId: ad.adUnitId,
            format: ad.format,
            timestamp: new Date().toISOString(),
            userId: 'test-user-' + Date.now(),
            sessionId: 'test-session-' + Date.now(),
            deviceInfo: {
              platform: 'WEB',
              osVersion: '10',
              browser: 'Chrome',
              screenSize: \`\${window.innerWidth}x\${window.innerHeight}\`
            },
            position: {
              x: 160,
              y: 25
            }
          })
        });
        
        if (!response.ok) {
          throw new Error(\`HTTP error! status: \${response.status}\`);
        }
        
        const data = await response.json();
        log(\`Click tracked: \${JSON.stringify(data)}\`);
        
      } catch (error) {
        log(\`Error tracking click: \${error.message}\`);
        console.error(error);
      }
    }
    
    // Event listeners
    document.getElementById('loadAd').addEventListener('click', loadAd);
    
    document.getElementById('refreshAd').addEventListener('click', () => {
      if (currentAd) {
        adContainer.innerHTML = '<p>Refreshing ad...</p>';
        setTimeout(loadAd, 500);
      } else {
        log('No ad to refresh. Load an ad first.');
      }
    });
    
    document.getElementById('clickAd').addEventListener('click', () => {
      if (currentAd) {
        log('Simulating ad click...');
        trackClick(currentAd);
      } else {
        log('No ad to click. Load an ad first.');
      }
    });
    
    // Initialize
    log('AdMob Client Test initialized');
  </script>
</body>
</html>
  `;
  
  fs.writeFileSync(testClientPath, html);
  return testClientPath;
}

// Test suite
describe('AdMob Client Integration', () => {
  let page;
  
  // Setup before all tests
  beforeAll(async () => {
    await setup();
    
    // Create test client HTML file
    createTestClient();
    
    // Create test ad configuration in database
    const adMob = new AdMob(testAdConfig);
    await adMob.save();
  }, 60000);
  
  // Teardown after all tests
  afterAll(async () => {
    // Clean up test data
    await AdMob.deleteMany({});
    await AdImpression.deleteMany({});
    await AdClick.deleteMany({});
    
    // Remove test client file
    if (fs.existsSync(testClientPath)) {
      fs.unlinkSync(testClientPath);
    }
    
    await teardown();
  }, 60000);
  
  // Setup before each test
  beforeEach(async () => {
    page = await createPage();
  });
  
  // Teardown after each test
  afterEach(async () => {
    if (page) {
      await page.close();
    }
  });
  
  // Tests
  test('should load the test client page', async () => {
    await page.goto(`file://${testClientPath}`);
    
    const title = await page.title();
    expect(title).toBe('AdMob Client Test');
    
    const heading = await page.$eval('h1', el => el.textContent);
    expect(heading).toBe('AdMob Client Test');
    
    await takeScreenshot(page, 'admob-client-initial');
  });
  
  test('should load an ad configuration', async () => {
    await page.goto(`file://${testClientPath}`);
    
    // Click the load ad button
    await page.click('#loadAd');
    
    // Wait for the ad to load
    await page.waitForFunction(
      () => document.getElementById('status').textContent !== 'Loading...',
      { timeout: 5000 }
    );
    
    // Check if ad loaded successfully
    const status = await page.$eval('#status', el => el.textContent);
    expect(status).toBe('Ad Loaded');
    
    // Check if ad ID is displayed
    const adId = await page.$eval('#adId', el => el.textContent);
    expect(adId).not.toBe('None');
    
    // Check if ad format is displayed
    const adFormat = await page.$eval('#adFormat', el => el.textContent);
    expect(adFormat).toBe('BANNER');
    
    // Check if ad container has content
    const adContainer = await page.$eval('#adContainer', el => el.innerHTML);
    expect(adContainer).toContain('TEST AD');
    
    await takeScreenshot(page, 'admob-client-ad-loaded');
  });
  
  test('should track an impression', async () => {
    await page.goto(`file://${testClientPath}`);
    
    // Click the load ad button
    await page.click('#loadAd');
    
    // Wait for the ad to load
    await page.waitForFunction(
      () => document.getElementById('status').textContent !== 'Loading...',
      { timeout: 5000 }
    );
    
    // Wait for impression tracking log
    await page.waitForFunction(
      () => {
        const logContent = document.getElementById('log').textContent;
        return logContent.includes('Impression tracked');
      },
      { timeout: 5000 }
    );
    
    // Check database for impression
    const impressions = await AdImpression.find({});
    expect(impressions.length).toBeGreaterThan(0);
    
    await takeScreenshot(page, 'admob-client-impression-tracked');
  });
  
  test('should track a click', async () => {
    await page.goto(`file://${testClientPath}`);
    
    // Click the load ad button
    await page.click('#loadAd');
    
    // Wait for the ad to load
    await page.waitForFunction(
      () => document.getElementById('status').textContent !== 'Loading...',
      { timeout: 5000 }
    );
    
    // Click the simulate click button
    await page.click('#clickAd');
    
    // Wait for click tracking log
    await page.waitForFunction(
      () => {
        const logContent = document.getElementById('log').textContent;
        return logContent.includes('Click tracked');
      },
      { timeout: 5000 }
    );
    
    // Check database for click
    const clicks = await AdClick.find({});
    expect(clicks.length).toBeGreaterThan(0);
    
    await takeScreenshot(page, 'admob-client-click-tracked');
  });
  
  test('should refresh an ad', async () => {
    await page.goto(`file://${testClientPath}`);
    
    // Click the load ad button
    await page.click('#loadAd');
    
    // Wait for the ad to load
    await page.waitForFunction(
      () => document.getElementById('status').textContent !== 'Loading...',
      { timeout: 5000 }
    );
    
    // Get current ad ID
    const initialAdId = await page.$eval('#adId', el => el.textContent);
    
    // Click the refresh ad button
    await page.click('#refreshAd');
    
    // Wait for the ad to refresh
    await page.waitForFunction(
      () => document.getElementById('adContainer').textContent.includes('TEST AD'),
      { timeout: 5000 }
    );
    
    // Check if ad refreshed
    const refreshedAdId = await page.$eval('#adId', el => el.textContent);
    expect(refreshedAdId).not.toBe('None');
    
    // Check database for multiple impressions
    const impressions = await AdImpression.find({});
    expect(impressions.length).toBeGreaterThan(1);
    
    await takeScreenshot(page, 'admob-client-ad-refreshed');
  });
});
