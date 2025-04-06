/**
 * End-to-End Tests for Fraud Detection System
 * 
 * This file contains tests for the fraud detection system, including:
 * - Click fraud detection
 * - Suspicious activity monitoring
 * - IP and device fingerprinting
 */

const { setup, teardown, createPage, takeScreenshot, config } = require('./setup');
const path = require('path');
const fs = require('fs');
const mongoose = require('mongoose');
const AdMob = require('../../models/AdMob');
const AdClick = require('../../models/AdClick');
const SuspiciousActivity = require('../../models/SuspiciousActivity');
const FraudScore = require('../../models/FraudScore');

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
const testClientPath = path.join(__dirname, 'fraud-test-client.html');

// Create test client HTML file
function createTestClient() {
  const html = `
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Fraud Detection Test</title>
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
    .fraud-controls {
      margin-top: 20px;
      padding: 10px;
      background-color: #ffeeee;
      border-radius: 5px;
      border: 1px solid #ffcccc;
    }
    .fraud-controls h3 {
      color: #cc0000;
      margin-top: 0;
    }
    .fraud-score {
      font-size: 24px;
      font-weight: bold;
      text-align: center;
      margin: 20px 0;
      padding: 10px;
      background-color: #f5f5f5;
      border-radius: 5px;
    }
  </style>
</head>
<body>
  <h1>Fraud Detection Test</h1>
  
  <div class="controls">
    <button id="loadAd">Load Ad</button>
    <button id="refreshAd">Refresh Ad</button>
    <button id="clearLog">Clear Log</button>
  </div>
  
  <div class="fraud-controls">
    <h3>Fraud Simulation Controls</h3>
    <button id="rapidClicks">Simulate Rapid Clicks (10)</button>
    <button id="patternClicks">Simulate Pattern Clicks</button>
    <button id="multipleDevices">Simulate Multiple Devices</button>
    <button id="checkFraudScore">Check Fraud Score</button>
  </div>
  
  <div class="fraud-score" id="fraudScore">
    Fraud Score: <span id="scoreValue">N/A</span>
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
    const scoreValueEl = document.getElementById('scoreValue');
    
    // State
    let currentAd = null;
    let sessionId = 'test-session-' + Date.now();
    let userId = 'test-user-' + Date.now();
    
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
          userId: userId,
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
          <div class="test-ad" style="width: 320px; height: 50px; background-color: #4CAF50; color: white; display: flex; align-items: center; justify-content: center; cursor: pointer;" id="testAd">
            <strong>TEST AD</strong> - \${currentAd.adUnitId}
          </div>
        \`;
        
        // Add click handler to the ad
        document.getElementById('testAd').addEventListener('click', () => {
          log('Ad clicked');
          trackClick(currentAd);
        });
        
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
            userId: userId,
            sessionId: sessionId,
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
    async function trackClick(ad, position = { x: 160, y: 25 }) {
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
            userId: userId,
            sessionId: sessionId,
            deviceInfo: {
              platform: 'WEB',
              osVersion: '10',
              browser: 'Chrome',
              screenSize: \`\${window.innerWidth}x\${window.innerHeight}\`
            },
            position: position
          })
        });
        
        if (!response.ok) {
          throw new Error(\`HTTP error! status: \${response.status}\`);
        }
        
        const data = await response.json();
        log(\`Click tracked: \${JSON.stringify(data)}\`);
        
        // Check if click was flagged as suspicious
        if (data.suspicious) {
          log(\`⚠️ Click flagged as suspicious: \${data.reason}\`);
        }
        
      } catch (error) {
        log(\`Error tracking click: \${error.message}\`);
        console.error(error);
      }
    }
    
    // Simulate rapid clicks
    async function simulateRapidClicks(count = 10) {
      if (!currentAd) {
        log('No ad to click. Load an ad first.');
        return;
      }
      
      log(\`Simulating \${count} rapid clicks...\`);
      
      for (let i = 0; i < count; i++) {
        // Random position within ad
        const position = {
          x: 160 + Math.floor(Math.random() * 20 - 10),
          y: 25 + Math.floor(Math.random() * 10 - 5)
        };
        
        await trackClick(currentAd, position);
        
        // Small delay between clicks (50-150ms)
        await new Promise(resolve => setTimeout(resolve, 50 + Math.random() * 100));
      }
      
      log('Rapid clicks simulation completed');
      
      // Check fraud score after rapid clicks
      setTimeout(checkFraudScore, 1000);
    }
    
    // Simulate pattern clicks
    async function simulatePatternClicks() {
      if (!currentAd) {
        log('No ad to click. Load an ad first.');
        return;
      }
      
      log('Simulating pattern clicks...');
      
      // Define a pattern (e.g., a grid)
      const pattern = [
        { x: 140, y: 15 },
        { x: 160, y: 15 },
        { x: 180, y: 15 },
        { x: 140, y: 25 },
        { x: 160, y: 25 },
        { x: 180, y: 25 },
        { x: 140, y: 35 },
        { x: 160, y: 35 },
        { x: 180, y: 35 }
      ];
      
      for (const position of pattern) {
        await trackClick(currentAd, position);
        
        // Small delay between clicks
        await new Promise(resolve => setTimeout(resolve, 100 + Math.random() * 100));
      }
      
      log('Pattern clicks simulation completed');
      
      // Check fraud score after pattern clicks
      setTimeout(checkFraudScore, 1000);
    }
    
    // Simulate multiple devices
    async function simulateMultipleDevices() {
      if (!currentAd) {
        log('No ad to click. Load an ad first.');
        return;
      }
      
      log('Simulating clicks from multiple devices...');
      
      // Define different device profiles
      const devices = [
        {
          userId: 'test-user-device1-' + Date.now(),
          sessionId: 'test-session-device1-' + Date.now(),
          deviceInfo: {
            platform: 'ANDROID',
            osVersion: '11',
            browser: 'Chrome Mobile',
            screenSize: '360x640'
          }
        },
        {
          userId: 'test-user-device2-' + Date.now(),
          sessionId: 'test-session-device2-' + Date.now(),
          deviceInfo: {
            platform: 'IOS',
            osVersion: '14.5',
            browser: 'Safari',
            screenSize: '375x667'
          }
        },
        {
          userId: 'test-user-device3-' + Date.now(),
          sessionId: 'test-session-device3-' + Date.now(),
          deviceInfo: {
            platform: 'WEB',
            osVersion: '10',
            browser: 'Firefox',
            screenSize: '1920x1080'
          }
        }
      ];
      
      for (const device of devices) {
        try {
          log(\`Simulating click from \${device.deviceInfo.platform} device...\`);
          
          const response = await fetch(\`\${API_BASE_URL}/api/admob/click\`, {
            method: 'POST',
            headers: {
              'Content-Type': 'application/json',
              'x-api-key': API_KEY
            },
            body: JSON.stringify({
              adId: currentAd._id,
              adUnitId: currentAd.adUnitId,
              format: currentAd.format,
              timestamp: new Date().toISOString(),
              userId: device.userId,
              sessionId: device.sessionId,
              deviceInfo: device.deviceInfo,
              position: {
                x: 160 + Math.floor(Math.random() * 20 - 10),
                y: 25 + Math.floor(Math.random() * 10 - 5)
              }
            })
          });
          
          if (!response.ok) {
            throw new Error(\`HTTP error! status: \${response.status}\`);
          }
          
          const data = await response.json();
          log(\`Click from \${device.deviceInfo.platform} tracked: \${JSON.stringify(data)}\`);
          
          // Check if click was flagged as suspicious
          if (data.suspicious) {
            log(\`⚠️ Click from \${device.deviceInfo.platform} flagged as suspicious: \${data.reason}\`);
          }
          
          // Small delay between devices
          await new Promise(resolve => setTimeout(resolve, 500));
          
        } catch (error) {
          log(\`Error tracking click from \${device.deviceInfo.platform}: \${error.message}\`);
          console.error(error);
        }
      }
      
      log('Multiple devices simulation completed');
      
      // Check fraud score after multiple device clicks
      setTimeout(checkFraudScore, 1000);
    }
    
    // Check fraud score
    async function checkFraudScore() {
      try {
        log('Checking fraud score...');
        
        const response = await fetch(\`\${API_BASE_URL}/api/fraud/score?userId=\${userId}\`, {
          method: 'GET',
          headers: {
            'Content-Type': 'application/json',
            'x-api-key': API_KEY
          }
        });
        
        if (!response.ok) {
          throw new Error(\`HTTP error! status: \${response.status}\`);
        }
        
        const data = await response.json();
        log(\`Fraud score: \${JSON.stringify(data)}\`);
        
        // Update UI
        scoreValueEl.textContent = data.score || 'N/A';
        
        // Add color based on score
        if (data.score >= 75) {
          scoreValueEl.style.color = '#cc0000';
        } else if (data.score >= 50) {
          scoreValueEl.style.color = '#ff6600';
        } else if (data.score >= 25) {
          scoreValueEl.style.color = '#ffcc00';
        } else {
          scoreValueEl.style.color = '#009900';
        }
        
      } catch (error) {
        log(\`Error checking fraud score: \${error.message}\`);
        console.error(error);
        scoreValueEl.textContent = 'Error';
        scoreValueEl.style.color = '#cc0000';
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
    
    document.getElementById('rapidClicks').addEventListener('click', () => {
      simulateRapidClicks(10);
    });
    
    document.getElementById('patternClicks').addEventListener('click', () => {
      simulatePatternClicks();
    });
    
    document.getElementById('multipleDevices').addEventListener('click', () => {
      simulateMultipleDevices();
    });
    
    document.getElementById('checkFraudScore').addEventListener('click', () => {
      checkFraudScore();
    });
    
    // Initialize
    log('Fraud Detection Test initialized');
    loadAd(); // Auto-load ad on page load
  </script>
</body>
</html>
  `;
  
  fs.writeFileSync(testClientPath, html);
  return testClientPath;
}

// Test suite
describe('Fraud Detection System', () => {
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
    await AdClick.deleteMany({});
    await SuspiciousActivity.deleteMany({});
    await FraudScore.deleteMany({});
    
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
  test('should load the fraud detection test page', async () => {
    await page.goto(`file://${testClientPath}`);
    
    const title = await page.title();
    expect(title).toBe('Fraud Detection Test');
    
    const heading = await page.$eval('h1', el => el.textContent);
    expect(heading).toBe('Fraud Detection Test');
    
    await takeScreenshot(page, 'fraud-detection-initial');
  });
  
  test('should load an ad and track impression', async () => {
    await page.goto(`file://${testClientPath}`);
    
    // Wait for the ad to load (auto-loaded on page load)
    await page.waitForFunction(
      () => document.getElementById('status').textContent !== 'Loading...',
      { timeout: 5000 }
    );
    
    // Check if ad loaded successfully
    const status = await page.$eval('#status', el => el.textContent);
    expect(status).toBe('Ad Loaded');
    
    // Check if ad container has content
    const adContainer = await page.$eval('#adContainer', el => el.innerHTML);
    expect(adContainer).toContain('TEST AD');
    
    // Wait for impression tracking log
    await page.waitForFunction(
      () => {
        const logContent = document.getElementById('log').textContent;
        return logContent.includes('Impression tracked');
      },
      { timeout: 5000 }
    );
    
    await takeScreenshot(page, 'fraud-detection-ad-loaded');
  });
  
  test('should detect rapid clicks as suspicious', async () => {
    await page.goto(`file://${testClientPath}`);
    
    // Wait for the ad to load
    await page.waitForFunction(
      () => document.getElementById('status').textContent !== 'Loading...',
      { timeout: 5000 }
    );
    
    // Click the rapid clicks button
    await page.click('#rapidClicks');
    
    // Wait for rapid clicks simulation to complete
    await page.waitForFunction(
      () => {
        const logContent = document.getElementById('log').textContent;
        return logContent.includes('Rapid clicks simulation completed');
      },
      { timeout: 15000 }
    );
    
    // Wait for fraud score check
    await page.waitForFunction(
      () => {
        const scoreValue = document.getElementById('scoreValue').textContent;
        return scoreValue !== 'N/A';
      },
      { timeout: 5000 }
    );
    
    // Check if any clicks were flagged as suspicious
    const logContent = await page.$eval('#log', el => el.textContent);
    expect(logContent).toContain('flagged as suspicious');
    
    // Check database for suspicious activities
    const suspiciousActivities = await SuspiciousActivity.find({});
    expect(suspiciousActivities.length).toBeGreaterThan(0);
    
    // Check database for fraud score
    const fraudScores = await FraudScore.find({});
    expect(fraudScores.length).toBeGreaterThan(0);
    
    await takeScreenshot(page, 'fraud-detection-rapid-clicks');
  });
  
  test('should detect pattern clicks as suspicious', async () => {
    await page.goto(`file://${testClientPath}`);
    
    // Wait for the ad to load
    await page.waitForFunction(
      () => document.getElementById('status').textContent !== 'Loading...',
      { timeout: 5000 }
    );
    
    // Click the pattern clicks button
    await page.click('#patternClicks');
    
    // Wait for pattern clicks simulation to complete
    await page.waitForFunction(
      () => {
        const logContent = document.getElementById('log').textContent;
        return logContent.includes('Pattern clicks simulation completed');
      },
      { timeout: 15000 }
    );
    
    // Wait for fraud score check
    await page.waitForFunction(
      () => {
        const scoreValue = document.getElementById('scoreValue').textContent;
        return scoreValue !== 'N/A';
      },
      { timeout: 5000 }
    );
    
    // Check database for suspicious activities related to patterns
    const suspiciousActivities = await SuspiciousActivity.find({
      type: { $regex: /pattern/i }
    });
    expect(suspiciousActivities.length).toBeGreaterThan(0);
    
    await takeScreenshot(page, 'fraud-detection-pattern-clicks');
  });
  
  test('should detect clicks from multiple devices as suspicious', async () => {
    await page.goto(`file://${testClientPath}`);
    
    // Wait for the ad to load
    await page.waitForFunction(
      () => document.getElementById('status').textContent !== 'Loading...',
      { timeout: 5000 }
    );
    
    // Click the multiple devices button
    await page.click('#multipleDevices');
    
    // Wait for multiple devices simulation to complete
    await page.waitForFunction(
      () => {
        const logContent = document.getElementById('log').textContent;
        return logContent.includes('Multiple devices simulation completed');
      },
      { timeout: 15000 }
    );
    
    // Wait for fraud score check
    await page.waitForFunction(
      () => {
        const scoreValue = document.getElementById('scoreValue').textContent;
        return scoreValue !== 'N/A';
      },
      { timeout: 5000 }
    );
    
    // Check database for suspicious activities related to multiple devices
    const suspiciousActivities = await SuspiciousActivity.find({
      type: { $regex: /device/i }
    });
    expect(suspiciousActivities.length).toBeGreaterThan(0);
    
    await takeScreenshot(page, 'fraud-detection-multiple-devices');
  });
  
  test('should update fraud score based on suspicious activities', async () => {
    await page.goto(`file://${testClientPath}`);
    
    // Wait for the ad to load
    await page.waitForFunction(
      () => document.getElementById('status').textContent !== 'Loading...',
      { timeout: 5000 }
    );
    
    // Get initial fraud score
    await page.click('#checkFraudScore');
    
    // Wait for fraud score check
    await page.waitForFunction(
      () => {
        const scoreValue = document.getElementById('scoreValue').textContent;
        return scoreValue !== 'N/A';
      },
      { timeout: 5000 }
    );
    
    // Get initial score
    const initialScore = await page.$eval('#scoreValue', el => el.textContent);
    
    // Simulate rapid clicks to increase fraud score
    await page.click('#rapidClicks');
    
    // Wait for rapid clicks simulation to complete
    await page.waitForFunction(
      () => {
        const logContent = document.getElementById('log').textContent;
        return logContent.includes('Rapid clicks simulation completed');
      },
      { timeout: 15000 }
    );
    
    // Wait for fraud score check
    await page.waitForFunction(
      () => {
        const scoreValue = document.getElementById('scoreValue').textContent;
        return scoreValue !== initialScore;
      },
      { timeout: 5000 }
    );
    
    // Get updated score
    const updatedScore = await page.$eval('#scoreValue', el => el.textContent);
    
    // Check if score increased
    expect(Number(updatedScore)).toBeGreaterThan(Number(initialScore));
    
    await takeScreenshot(page, 'fraud-detection-score-update');
  });
});
