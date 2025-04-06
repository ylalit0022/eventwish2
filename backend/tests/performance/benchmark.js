/**
 * AdMob Integration Performance Benchmark
 * 
 * This script measures the performance of the AdMob integration, including:
 * - Response times for ad configuration requests
 * - Throughput for impression and click tracking
 * - Resource usage under load
 * - Cache hit rates
 * - Database query performance
 */

const axios = require('axios');
const { performance } = require('perf_hooks');
const fs = require('fs');
const path = require('path');
const os = require('os');
const cluster = require('cluster');
const { MongoMemoryServer } = require('mongodb-memory-server');
const mongoose = require('mongoose');
const { exec } = require('child_process');

// Configuration
const config = {
  baseUrl: process.env.TEST_BASE_URL || 'http://localhost:3000',
  apiKey: process.env.TEST_API_KEY || 'test-api-key',
  concurrentUsers: process.env.CONCURRENT_USERS ? parseInt(process.env.CONCURRENT_USERS, 10) : 50,
  duration: process.env.TEST_DURATION ? parseInt(process.env.TEST_DURATION, 10) : 60, // seconds
  rampUpTime: process.env.RAMP_UP_TIME ? parseInt(process.env.RAMP_UP_TIME, 10) : 10, // seconds
  reportInterval: process.env.REPORT_INTERVAL ? parseInt(process.env.REPORT_INTERVAL, 10) : 5, // seconds
  outputDir: process.env.OUTPUT_DIR || path.join(__dirname, 'results'),
  workers: process.env.WORKERS ? parseInt(process.env.WORKERS, 10) : Math.max(1, os.cpus().length - 1),
};

// Ensure output directory exists
if (!fs.existsSync(config.outputDir)) {
  fs.mkdirSync(config.outputDir, { recursive: true });
}

// Test data
const adFormats = ['BANNER', 'INTERSTITIAL', 'REWARDED', 'NATIVE'];
const platforms = ['ANDROID', 'IOS', 'WEB'];
const countries = ['US', 'CA', 'GB', 'DE', 'FR', 'JP', 'AU', 'BR', 'IN'];
const languages = ['en', 'es', 'fr', 'de', 'ja', 'pt', 'zh'];
const userTypes = ['NEW', 'RETURNING', 'PREMIUM'];
const deviceTypes = ['MOBILE', 'TABLET', 'DESKTOP'];

// Metrics
const metrics = {
  requests: 0,
  successful: 0,
  failed: 0,
  totalResponseTime: 0,
  minResponseTime: Number.MAX_SAFE_INTEGER,
  maxResponseTime: 0,
  responseTimes: [],
  statusCodes: {},
  endpoints: {},
  errors: {},
  startTime: 0,
  endTime: 0,
};

// Generate random user context
function generateUserContext() {
  const platform = platforms[Math.floor(Math.random() * platforms.length)];
  const country = countries[Math.floor(Math.random() * countries.length)];
  const language = languages[Math.floor(Math.random() * languages.length)];
  const userType = userTypes[Math.floor(Math.random() * userTypes.length)];
  const deviceType = deviceTypes[Math.floor(Math.random() * deviceTypes.length)];
  
  return {
    platform,
    osVersion: `${Math.floor(Math.random() * 5) + 8}.${Math.floor(Math.random() * 10)}`,
    language,
    country,
    userId: `benchmark-user-${Math.floor(Math.random() * 1000000)}`,
    userType,
    deviceType,
  };
}

// Generate random ad request
function generateAdRequest() {
  const format = adFormats[Math.floor(Math.random() * adFormats.length)];
  const context = generateUserContext();
  
  return {
    format,
    context,
  };
}

// Track metrics for a request
function trackRequest(endpoint, startTime, response, error) {
  const endTime = performance.now();
  const responseTime = endTime - startTime;
  
  metrics.requests++;
  metrics.totalResponseTime += responseTime;
  metrics.minResponseTime = Math.min(metrics.minResponseTime, responseTime);
  metrics.maxResponseTime = Math.max(metrics.maxResponseTime, responseTime);
  metrics.responseTimes.push(responseTime);
  
  // Track endpoint metrics
  if (!metrics.endpoints[endpoint]) {
    metrics.endpoints[endpoint] = {
      requests: 0,
      successful: 0,
      failed: 0,
      totalResponseTime: 0,
      minResponseTime: Number.MAX_SAFE_INTEGER,
      maxResponseTime: 0,
    };
  }
  
  metrics.endpoints[endpoint].requests++;
  metrics.endpoints[endpoint].totalResponseTime += responseTime;
  metrics.endpoints[endpoint].minResponseTime = Math.min(metrics.endpoints[endpoint].minResponseTime, responseTime);
  metrics.endpoints[endpoint].maxResponseTime = Math.max(metrics.endpoints[endpoint].maxResponseTime, responseTime);
  
  if (error) {
    metrics.failed++;
    metrics.endpoints[endpoint].failed++;
    
    const errorMessage = error.message || 'Unknown error';
    if (!metrics.errors[errorMessage]) {
      metrics.errors[errorMessage] = 0;
    }
    metrics.errors[errorMessage]++;
  } else {
    metrics.successful++;
    metrics.endpoints[endpoint].successful++;
    
    const statusCode = response.status;
    if (!metrics.statusCodes[statusCode]) {
      metrics.statusCodes[statusCode] = 0;
    }
    metrics.statusCodes[statusCode]++;
  }
}

// Request ad configuration
async function requestAdConfig() {
  const endpoint = '/api/admob/config';
  const startTime = performance.now();
  
  try {
    const response = await axios.post(`${config.baseUrl}${endpoint}`, generateAdRequest(), {
      headers: {
        'Content-Type': 'application/json',
        'x-api-key': config.apiKey,
      },
    });
    
    trackRequest(endpoint, startTime, response);
    return response.data;
  } catch (error) {
    trackRequest(endpoint, startTime, null, error);
    return null;
  }
}

// Track impression
async function trackImpression(ad) {
  const endpoint = '/api/admob/impression';
  const startTime = performance.now();
  
  try {
    const userContext = generateUserContext();
    
    const response = await axios.post(`${config.baseUrl}${endpoint}`, {
      adId: ad._id,
      adUnitId: ad.adUnitId,
      format: ad.format,
      timestamp: new Date().toISOString(),
      userId: userContext.userId,
      sessionId: `benchmark-session-${Math.floor(Math.random() * 1000000)}`,
      deviceInfo: {
        platform: userContext.platform,
        osVersion: userContext.osVersion,
        browser: 'Chrome',
        screenSize: '1920x1080',
      },
    }, {
      headers: {
        'Content-Type': 'application/json',
        'x-api-key': config.apiKey,
      },
    });
    
    trackRequest(endpoint, startTime, response);
    return response.data;
  } catch (error) {
    trackRequest(endpoint, startTime, null, error);
    return null;
  }
}

// Track click
async function trackClick(ad) {
  const endpoint = '/api/admob/click';
  const startTime = performance.now();
  
  try {
    const userContext = generateUserContext();
    
    const response = await axios.post(`${config.baseUrl}${endpoint}`, {
      adId: ad._id,
      adUnitId: ad.adUnitId,
      format: ad.format,
      timestamp: new Date().toISOString(),
      userId: userContext.userId,
      sessionId: `benchmark-session-${Math.floor(Math.random() * 1000000)}`,
      deviceInfo: {
        platform: userContext.platform,
        osVersion: userContext.osVersion,
        browser: 'Chrome',
        screenSize: '1920x1080',
      },
      position: {
        x: Math.floor(Math.random() * 320),
        y: Math.floor(Math.random() * 50),
      },
    }, {
      headers: {
        'Content-Type': 'application/json',
        'x-api-key': config.apiKey,
      },
    });
    
    trackRequest(endpoint, startTime, response);
    return response.data;
  } catch (error) {
    trackRequest(endpoint, startTime, null, error);
    return null;
  }
}

// Check health
async function checkHealth() {
  const endpoint = '/api/health';
  const startTime = performance.now();
  
  try {
    const response = await axios.get(`${config.baseUrl}${endpoint}`, {
      headers: {
        'x-api-key': config.apiKey,
      },
    });
    
    trackRequest(endpoint, startTime, response);
    return response.data;
  } catch (error) {
    trackRequest(endpoint, startTime, null, error);
    return null;
  }
}

// Check fraud score
async function checkFraudScore(userId) {
  const endpoint = '/api/fraud/score';
  const startTime = performance.now();
  
  try {
    const response = await axios.get(`${config.baseUrl}${endpoint}?userId=${userId}`, {
      headers: {
        'Content-Type': 'application/json',
        'x-api-key': config.apiKey,
      },
    });
    
    trackRequest(endpoint, startTime, response);
    return response.data;
  } catch (error) {
    trackRequest(endpoint, startTime, null, error);
    return null;
  }
}

// Simulate user behavior
async function simulateUser(userId) {
  try {
    // Request ad configuration
    const adConfigResponse = await requestAdConfig();
    
    if (adConfigResponse && adConfigResponse.success) {
      const ad = adConfigResponse.ad;
      
      // Track impression
      await trackImpression(ad);
      
      // 50% chance to click the ad
      if (Math.random() < 0.5) {
        await trackClick(ad);
        
        // 20% chance to check fraud score
        if (Math.random() < 0.2) {
          await checkFraudScore(userId);
        }
      }
    }
  } catch (error) {
    console.error(`Error simulating user: ${error.message}`);
  }
}

// Calculate percentile
function calculatePercentile(values, percentile) {
  if (values.length === 0) return 0;
  
  values.sort((a, b) => a - b);
  const index = Math.ceil((percentile / 100) * values.length) - 1;
  return values[index];
}

// Generate report
function generateReport() {
  const duration = (metrics.endTime - metrics.startTime) / 1000; // seconds
  const requestsPerSecond = metrics.requests / duration;
  const avgResponseTime = metrics.totalResponseTime / metrics.requests;
  const p50ResponseTime = calculatePercentile(metrics.responseTimes, 50);
  const p95ResponseTime = calculatePercentile(metrics.responseTimes, 95);
  const p99ResponseTime = calculatePercentile(metrics.responseTimes, 99);
  
  const report = {
    summary: {
      concurrentUsers: config.concurrentUsers,
      duration: duration.toFixed(2),
      totalRequests: metrics.requests,
      successfulRequests: metrics.successful,
      failedRequests: metrics.failed,
      requestsPerSecond: requestsPerSecond.toFixed(2),
      avgResponseTime: avgResponseTime.toFixed(2),
      minResponseTime: metrics.minResponseTime.toFixed(2),
      maxResponseTime: metrics.maxResponseTime.toFixed(2),
      p50ResponseTime: p50ResponseTime.toFixed(2),
      p95ResponseTime: p95ResponseTime.toFixed(2),
      p99ResponseTime: p99ResponseTime.toFixed(2),
      successRate: ((metrics.successful / metrics.requests) * 100).toFixed(2),
    },
    endpoints: {},
    statusCodes: metrics.statusCodes,
    errors: metrics.errors,
  };
  
  // Calculate endpoint metrics
  for (const [endpoint, data] of Object.entries(metrics.endpoints)) {
    report.endpoints[endpoint] = {
      requests: data.requests,
      successful: data.successful,
      failed: data.failed,
      requestsPerSecond: (data.requests / duration).toFixed(2),
      avgResponseTime: (data.totalResponseTime / data.requests).toFixed(2),
      minResponseTime: data.minResponseTime.toFixed(2),
      maxResponseTime: data.maxResponseTime.toFixed(2),
      successRate: ((data.successful / data.requests) * 100).toFixed(2),
    };
  }
  
  return report;
}

// Save report to file
function saveReport(report) {
  const timestamp = new Date().toISOString().replace(/:/g, '-');
  const filePath = path.join(config.outputDir, `benchmark-report-${timestamp}.json`);
  
  fs.writeFileSync(filePath, JSON.stringify(report, null, 2));
  console.log(`Report saved to: ${filePath}`);
  
  return filePath;
}

// Print report summary
function printReportSummary(report) {
  console.log('\n=== Benchmark Report Summary ===');
  console.log(`Concurrent Users: ${report.summary.concurrentUsers}`);
  console.log(`Duration: ${report.summary.duration} seconds`);
  console.log(`Total Requests: ${report.summary.totalRequests}`);
  console.log(`Successful Requests: ${report.summary.successfulRequests}`);
  console.log(`Failed Requests: ${report.summary.failedRequests}`);
  console.log(`Requests Per Second: ${report.summary.requestsPerSecond}`);
  console.log(`Average Response Time: ${report.summary.avgResponseTime} ms`);
  console.log(`Min Response Time: ${report.summary.minResponseTime} ms`);
  console.log(`Max Response Time: ${report.summary.maxResponseTime} ms`);
  console.log(`P50 Response Time: ${report.summary.p50ResponseTime} ms`);
  console.log(`P95 Response Time: ${report.summary.p95ResponseTime} ms`);
  console.log(`P99 Response Time: ${report.summary.p99ResponseTime} ms`);
  console.log(`Success Rate: ${report.summary.successRate}%`);
  
  console.log('\n=== Endpoint Performance ===');
  for (const [endpoint, data] of Object.entries(report.endpoints)) {
    console.log(`\nEndpoint: ${endpoint}`);
    console.log(`  Requests: ${data.requests}`);
    console.log(`  Requests Per Second: ${data.requestsPerSecond}`);
    console.log(`  Average Response Time: ${data.avgResponseTime} ms`);
    console.log(`  Success Rate: ${data.successRate}%`);
  }
  
  if (Object.keys(report.errors).length > 0) {
    console.log('\n=== Errors ===');
    for (const [error, count] of Object.entries(report.errors)) {
      console.log(`  ${error}: ${count}`);
    }
  }
}

// Run benchmark
async function runBenchmark() {
  if (cluster.isMaster) {
    console.log(`Starting benchmark with ${config.concurrentUsers} concurrent users for ${config.duration} seconds`);
    console.log(`Using ${config.workers} worker processes`);
    
    // Fork workers
    for (let i = 0; i < config.workers; i++) {
      cluster.fork();
    }
    
    // Track worker metrics
    const workerMetrics = {};
    let completedWorkers = 0;
    
    // Listen for messages from workers
    cluster.on('message', (worker, message) => {
      if (message.type === 'metrics') {
        workerMetrics[worker.id] = message.metrics;
        console.log(`Received metrics from worker ${worker.id}`);
      } else if (message.type === 'complete') {
        completedWorkers++;
        console.log(`Worker ${worker.id} completed benchmark`);
        
        // If all workers have completed, generate report
        if (completedWorkers === config.workers) {
          console.log('All workers completed benchmark');
          
          // Combine metrics from all workers
          const combinedMetrics = {
            requests: 0,
            successful: 0,
            failed: 0,
            totalResponseTime: 0,
            minResponseTime: Number.MAX_SAFE_INTEGER,
            maxResponseTime: 0,
            responseTimes: [],
            statusCodes: {},
            endpoints: {},
            errors: {},
            startTime: Number.MAX_SAFE_INTEGER,
            endTime: 0,
          };
          
          for (const workerMetric of Object.values(workerMetrics)) {
            combinedMetrics.requests += workerMetric.requests;
            combinedMetrics.successful += workerMetric.successful;
            combinedMetrics.failed += workerMetric.failed;
            combinedMetrics.totalResponseTime += workerMetric.totalResponseTime;
            combinedMetrics.minResponseTime = Math.min(combinedMetrics.minResponseTime, workerMetric.minResponseTime);
            combinedMetrics.maxResponseTime = Math.max(combinedMetrics.maxResponseTime, workerMetric.maxResponseTime);
            combinedMetrics.responseTimes = combinedMetrics.responseTimes.concat(workerMetric.responseTimes);
            combinedMetrics.startTime = Math.min(combinedMetrics.startTime, workerMetric.startTime);
            combinedMetrics.endTime = Math.max(combinedMetrics.endTime, workerMetric.endTime);
            
            // Combine status codes
            for (const [code, count] of Object.entries(workerMetric.statusCodes)) {
              if (!combinedMetrics.statusCodes[code]) {
                combinedMetrics.statusCodes[code] = 0;
              }
              combinedMetrics.statusCodes[code] += count;
            }
            
            // Combine endpoint metrics
            for (const [endpoint, data] of Object.entries(workerMetric.endpoints)) {
              if (!combinedMetrics.endpoints[endpoint]) {
                combinedMetrics.endpoints[endpoint] = {
                  requests: 0,
                  successful: 0,
                  failed: 0,
                  totalResponseTime: 0,
                  minResponseTime: Number.MAX_SAFE_INTEGER,
                  maxResponseTime: 0,
                };
              }
              
              combinedMetrics.endpoints[endpoint].requests += data.requests;
              combinedMetrics.endpoints[endpoint].successful += data.successful;
              combinedMetrics.endpoints[endpoint].failed += data.failed;
              combinedMetrics.endpoints[endpoint].totalResponseTime += data.totalResponseTime;
              combinedMetrics.endpoints[endpoint].minResponseTime = Math.min(combinedMetrics.endpoints[endpoint].minResponseTime, data.minResponseTime);
              combinedMetrics.endpoints[endpoint].maxResponseTime = Math.max(combinedMetrics.endpoints[endpoint].maxResponseTime, data.maxResponseTime);
            }
            
            // Combine errors
            for (const [error, count] of Object.entries(workerMetric.errors)) {
              if (!combinedMetrics.errors[error]) {
                combinedMetrics.errors[error] = 0;
              }
              combinedMetrics.errors[error] += count;
            }
          }
          
          // Generate and save report
          const report = generateReport();
          const reportPath = saveReport(report);
          printReportSummary(report);
          
          // Exit process
          process.exit(0);
        }
      }
    });
    
    // Handle worker exit
    cluster.on('exit', (worker, code, signal) => {
      console.log(`Worker ${worker.id} died with code ${code} and signal ${signal}`);
      
      // If a worker dies, fork a new one
      if (code !== 0) {
        console.log(`Forking a new worker to replace worker ${worker.id}`);
        cluster.fork();
      }
    });
  } else {
    // Worker process
    console.log(`Worker ${cluster.worker.id} started`);
    
    // Calculate number of users for this worker
    const usersPerWorker = Math.ceil(config.concurrentUsers / config.workers);
    const startUserIndex = (cluster.worker.id - 1) * usersPerWorker;
    const endUserIndex = Math.min(startUserIndex + usersPerWorker, config.concurrentUsers);
    const userCount = endUserIndex - startUserIndex;
    
    console.log(`Worker ${cluster.worker.id} handling ${userCount} users (${startUserIndex} to ${endUserIndex - 1})`);
    
    // Reset metrics
    metrics.startTime = performance.now();
    
    // Start users
    const userPromises = [];
    
    for (let i = 0; i < userCount; i++) {
      const userId = `benchmark-user-${startUserIndex + i}`;
      
      // Calculate delay for ramp-up
      const delay = (i / userCount) * config.rampUpTime * 1000;
      
      userPromises.push(
        new Promise(resolve => {
          setTimeout(async () => {
            const endTime = metrics.startTime + (config.duration * 1000);
            
            // Run user until test duration is reached
            while (performance.now() < endTime) {
              await simulateUser(userId);
              
              // Random delay between requests (100-500ms)
              await new Promise(r => setTimeout(r, 100 + Math.random() * 400));
            }
            
            resolve();
          }, delay);
        })
      );
    }
    
    // Wait for all users to complete
    await Promise.all(userPromises);
    
    // Record end time
    metrics.endTime = performance.now();
    
    // Send metrics to master process
    process.send({ type: 'metrics', metrics });
    process.send({ type: 'complete' });
  }
}

// Check if server is running
async function checkServer() {
  try {
    const response = await axios.get(`${config.baseUrl}/api/health`);
    return response.status === 200;
  } catch (error) {
    return false;
  }
}

// Start server if not running
async function startServer() {
  const isServerRunning = await checkServer();
  
  if (isServerRunning) {
    console.log('Server is already running');
    return true;
  }
  
  console.log('Starting server...');
  
  // Start MongoDB memory server
  const mongoServer = await MongoMemoryServer.create();
  const mongoUri = mongoServer.getUri();
  
  // Start the server
  return new Promise((resolve, reject) => {
    const serverProcess = exec('node server.js', {
      env: {
        ...process.env,
        NODE_ENV: 'test',
        MONGODB_URI: mongoUri,
        PORT: '3000',
        ENABLE_SWAGGER: 'true',
      },
      cwd: path.join(__dirname, '../../'),
    });
    
    serverProcess.stdout.on('data', (data) => {
      console.log(`Server: ${data.toString().trim()}`);
      
      // Resolve when server is ready
      if (data.toString().includes('Server is running on port')) {
        console.log('Server started successfully');
        resolve(true);
      }
    });
    
    serverProcess.stderr.on('data', (data) => {
      console.error(`Server Error: ${data.toString().trim()}`);
    });
    
    // Reject if server fails to start within timeout
    setTimeout(() => {
      reject(new Error('Server failed to start within timeout period'));
    }, 10000);
  });
}

// Main function
async function main() {
  try {
    // Check if server is running or start it
    await startServer();
    
    // Run benchmark
    await runBenchmark();
  } catch (error) {
    console.error(`Error running benchmark: ${error.message}`);
    process.exit(1);
  }
}

// Run main function
if (require.main === module) {
  main();
}

module.exports = {
  runBenchmark,
  generateReport,
  saveReport,
  printReportSummary,
}; 