#!/usr/bin/env node

/**
 * AdMob Diagnostic Tool
 * 
 * This script helps diagnose common issues with the AdMob integration.
 * It checks for configuration issues, connectivity, and provides helpful information.
 * 
 * Usage: node tools/admob-diagnostic.js [--verbose]
 */

const fs = require('fs');
const path = require('path');
const mongoose = require('mongoose');
const axios = require('axios');
const dotenv = require('dotenv');
const { promisify } = require('util');
const exec = promisify(require('child_process').exec);
const Redis = require('ioredis');

// Load environment variables
dotenv.config();

// Configuration
const config = {
  verbose: process.argv.includes('--verbose'),
  mongoUrl: process.env.MONGO_URI || 'mongodb://localhost:27017/eventwish',
  redisUrl: process.env.REDIS_URL || 'redis://localhost:6379',
  port: process.env.PORT || 3000,
  apiKey: process.env.API_KEY,
  jwtSecret: process.env.JWT_SECRET,
  appSignature: process.env.APP_SIGNATURE
};

// Console output helpers
const log = {
  info: (message) => console.log(`\x1b[36mINFO:\x1b[0m ${message}`),
  success: (message) => console.log(`\x1b[32mSUCCESS:\x1b[0m ${message}`),
  warning: (message) => console.log(`\x1b[33mWARNING:\x1b[0m ${message}`),
  error: (message) => console.log(`\x1b[31mERROR:\x1b[0m ${message}`),
  verbose: (message) => {
    if (config.verbose) {
      console.log(`\x1b[90mDEBUG:\x1b[0m ${message}`);
    }
  }
};

// Main diagnostic function
async function runDiagnostics() {
  log.info('Starting AdMob integration diagnostics...');
  
  // Check environment variables
  await checkEnvironmentVariables();
  
  // Check file structure
  await checkFileStructure();
  
  // Check database connection
  await checkDatabaseConnection();
  
  // Check Redis connection
  await checkRedisConnection();
  
  // Check API server
  await checkApiServer();
  
  // Check AdMob configurations
  await checkAdMobConfigurations();
  
  // Check A/B tests
  await checkABTests();
  
  // Check user segments
  await checkUserSegments();
  
  // Check analytics data
  await checkAnalyticsData();
  
  // Check monitoring metrics
  await checkMonitoringMetrics();
  
  // Print summary
  printSummary();
  
  log.info('Diagnostics completed.');
}

// Check environment variables
async function checkEnvironmentVariables() {
  log.info('Checking environment variables...');
  
  const requiredVars = [
    'MONGO_URI',
    'JWT_SECRET',
    'API_KEY',
    'APP_SIGNATURE'
  ];
  
  const missingVars = [];
  
  for (const varName of requiredVars) {
    if (!process.env[varName]) {
      missingVars.push(varName);
    }
  }
  
  if (missingVars.length > 0) {
    log.error(`Missing required environment variables: ${missingVars.join(', ')}`);
    log.info('Please add these variables to your .env file');
  } else {
    log.success('All required environment variables are set');
  }
  
  // Check for optional variables
  const optionalVars = [
    'REDIS_URL',
    'CACHE_TTL',
    'RATE_LIMIT_WINDOW',
    'RATE_LIMIT_MAX'
  ];
  
  const missingOptionalVars = [];
  
  for (const varName of optionalVars) {
    if (!process.env[varName]) {
      missingOptionalVars.push(varName);
    }
  }
  
  if (missingOptionalVars.length > 0) {
    log.warning(`Missing optional environment variables: ${missingOptionalVars.join(', ')}`);
    log.info('These variables are not required but recommended for optimal performance');
  }
}

// Check file structure
async function checkFileStructure() {
  log.info('Checking file structure...');
  
  const requiredFiles = [
    'models/AdMob.js',
    'models/ABTest.js',
    'models/UserSegment.js',
    'services/adMobService.js',
    'services/abTestService.js',
    'services/targetingService.js',
    'services/cacheService.js',
    'services/analyticsService.js',
    'services/monitoringService.js',
    'controllers/adMobController.js',
    'controllers/abTestController.js',
    'controllers/segmentController.js',
    'middleware/authMiddleware.js',
    'routes/adMobRoutes.js',
    'routes/abTestRoutes.js',
    'routes/segmentRoutes.js'
  ];
  
  const missingFiles = [];
  
  for (const file of requiredFiles) {
    const filePath = path.join(process.cwd(), file);
    if (!fs.existsSync(filePath)) {
      missingFiles.push(file);
    }
  }
  
  if (missingFiles.length > 0) {
    log.error(`Missing required files: ${missingFiles.join(', ')}`);
    log.info('Please check your installation and ensure all files are present');
  } else {
    log.success('All required files are present');
  }
}

// Check database connection
async function checkDatabaseConnection() {
  log.info('Checking database connection...');
  
  try {
    await mongoose.connect(config.mongoUrl, {
      useNewUrlParser: true,
      useUnifiedTopology: true
    });
    
    log.success('Successfully connected to MongoDB');
    
    // Check collections
    const collections = await mongoose.connection.db.listCollections().toArray();
    const collectionNames = collections.map(c => c.name);
    
    log.verbose(`Found collections: ${collectionNames.join(', ')}`);
    
    const requiredCollections = ['admobs', 'abtests', 'usersegments', 'analytics'];
    const missingCollections = [];
    
    for (const collection of requiredCollections) {
      if (!collectionNames.includes(collection)) {
        missingCollections.push(collection);
      }
    }
    
    if (missingCollections.length > 0) {
      log.warning(`Missing collections: ${missingCollections.join(', ')}`);
      log.info('These collections will be created automatically when data is added');
    } else {
      log.success('All required collections exist');
    }
    
    // Check indexes
    for (const collection of requiredCollections) {
      if (collectionNames.includes(collection)) {
        const indexes = await mongoose.connection.db.collection(collection).indexes();
        log.verbose(`Collection ${collection} has ${indexes.length} indexes`);
      }
    }
    
    await mongoose.disconnect();
  } catch (error) {
    log.error(`Failed to connect to MongoDB: ${error.message}`);
    log.info('Please check your MONGO_URI environment variable and ensure MongoDB is running');
  }
}

// Check Redis connection
async function checkRedisConnection() {
  log.info('Checking Redis connection...');
  
  if (!process.env.REDIS_URL) {
    log.warning('REDIS_URL environment variable is not set');
    log.info('Redis is optional but recommended for better caching performance');
    return;
  }
  
  try {
    const redis = new Redis(config.redisUrl);
    
    // Test connection
    await redis.ping();
    log.success('Successfully connected to Redis');
    
    // Check cache keys
    const keys = await redis.keys('admob:*');
    log.verbose(`Found ${keys.length} AdMob-related keys in Redis`);
    
    // Check memory usage
    const info = await redis.info('memory');
    const memoryMatch = info.match(/used_memory_human:(.+)/);
    if (memoryMatch) {
      log.verbose(`Redis memory usage: ${memoryMatch[1].trim()}`);
    }
    
    await redis.quit();
  } catch (error) {
    log.error(`Failed to connect to Redis: ${error.message}`);
    log.info('Please check your REDIS_URL environment variable and ensure Redis is running');
  }
}

// Check API server
async function checkApiServer() {
  log.info('Checking API server...');
  
  try {
    // Check if server is running
    const { stdout, stderr } = await exec('lsof -i :' + config.port);
    
    if (stdout) {
      log.success(`API server is running on port ${config.port}`);
    } else {
      log.warning(`API server does not appear to be running on port ${config.port}`);
      log.info('Start the server with: npm start');
      return;
    }
  } catch (error) {
    log.warning(`Could not determine if API server is running: ${error.message}`);
    log.info('This may be due to permission issues or the server not running');
    return;
  }
  
  // Try to connect to the API
  try {
    const response = await axios.get(`http://localhost:${config.port}/api/health`);
    
    if (response.status === 200) {
      log.success('API health endpoint is responding');
    } else {
      log.warning(`API health endpoint returned status ${response.status}`);
    }
  } catch (error) {
    log.error(`Failed to connect to API health endpoint: ${error.message}`);
    log.info('Please check that the server is running and the health endpoint is implemented');
  }
}

// Check AdMob configurations
async function checkAdMobConfigurations() {
  log.info('Checking AdMob configurations...');
  
  try {
    await mongoose.connect(config.mongoUrl, {
      useNewUrlParser: true,
      useUnifiedTopology: true
    });
    
    // Load AdMob model
    const AdMob = require('../models/AdMob');
    
    // Count configurations
    const count = await AdMob.countDocuments();
    log.verbose(`Found ${count} AdMob configurations`);
    
    if (count === 0) {
      log.warning('No AdMob configurations found in the database');
      log.info('Add configurations using the admin API or database import');
    } else {
      log.success(`Found ${count} AdMob configurations`);
      
      // Check configuration types
      const typeCounts = await AdMob.aggregate([
        { $group: { _id: '$adType', count: { $sum: 1 } } }
      ]);
      
      for (const type of typeCounts) {
        log.verbose(`AdType "${type._id}": ${type.count} configurations`);
      }
    }
    
    await mongoose.disconnect();
  } catch (error) {
    log.error(`Failed to check AdMob configurations: ${error.message}`);
  }
}

// Check A/B tests
async function checkABTests() {
  log.info('Checking A/B tests...');
  
  try {
    await mongoose.connect(config.mongoUrl, {
      useNewUrlParser: true,
      useUnifiedTopology: true
    });
    
    // Load ABTest model
    const ABTest = require('../models/ABTest');
    
    // Count tests
    const count = await ABTest.countDocuments();
    log.verbose(`Found ${count} A/B tests`);
    
    if (count === 0) {
      log.warning('No A/B tests found in the database');
      log.info('Create A/B tests using the admin API');
    } else {
      log.success(`Found ${count} A/B tests`);
      
      // Check test statuses
      const statusCounts = await ABTest.aggregate([
        { $group: { _id: '$status', count: { $sum: 1 } } }
      ]);
      
      for (const status of statusCounts) {
        log.verbose(`Status "${status._id}": ${status.count} tests`);
      }
      
      // Check active tests
      const activeTests = await ABTest.countDocuments({ 
        status: 'active',
        startDate: { $lte: new Date() },
        endDate: { $gte: new Date() }
      });
      
      log.verbose(`Currently active tests: ${activeTests}`);
    }
    
    await mongoose.disconnect();
  } catch (error) {
    log.error(`Failed to check A/B tests: ${error.message}`);
  }
}

// Check user segments
async function checkUserSegments() {
  log.info('Checking user segments...');
  
  try {
    await mongoose.connect(config.mongoUrl, {
      useNewUrlParser: true,
      useUnifiedTopology: true
    });
    
    // Load UserSegment model
    const UserSegment = require('../models/UserSegment');
    
    // Count segments
    const count = await UserSegment.countDocuments();
    log.verbose(`Found ${count} user segments`);
    
    if (count === 0) {
      log.warning('No user segments found in the database');
      log.info('Create user segments using the admin API');
    } else {
      log.success(`Found ${count} user segments`);
      
      // Check segment statuses
      const statusCounts = await UserSegment.aggregate([
        { $group: { _id: '$isActive', count: { $sum: 1 } } }
      ]);
      
      for (const status of statusCounts) {
        const statusText = status._id ? 'active' : 'inactive';
        log.verbose(`${statusText}: ${status.count} segments`);
      }
    }
    
    await mongoose.disconnect();
  } catch (error) {
    log.error(`Failed to check user segments: ${error.message}`);
  }
}

// Check analytics data
async function checkAnalyticsData() {
  log.info('Checking analytics data...');
  
  try {
    await mongoose.connect(config.mongoUrl, {
      useNewUrlParser: true,
      useUnifiedTopology: true
    });
    
    // Check if analytics collection exists
    const collections = await mongoose.connection.db.listCollections().toArray();
    const collectionNames = collections.map(c => c.name);
    
    if (!collectionNames.includes('analytics')) {
      log.warning('Analytics collection does not exist');
      log.info('This collection will be created when analytics data is recorded');
      await mongoose.disconnect();
      return;
    }
    
    // Count analytics records
    const analyticsCount = await mongoose.connection.db.collection('analytics').countDocuments();
    log.verbose(`Found ${analyticsCount} analytics records`);
    
    if (analyticsCount === 0) {
      log.warning('No analytics data found');
      log.info('Analytics data will be recorded when ads are viewed and clicked');
    } else {
      log.success(`Found ${analyticsCount} analytics records`);
      
      // Check event types
      const eventCounts = await mongoose.connection.db.collection('analytics').aggregate([
        { $group: { _id: '$eventType', count: { $sum: 1 } } }
      ]).toArray();
      
      for (const event of eventCounts) {
        log.verbose(`Event type "${event._id}": ${event.count} records`);
      }
      
      // Check recent activity
      const recentCount = await mongoose.connection.db.collection('analytics').countDocuments({
        timestamp: { $gte: new Date(Date.now() - 24 * 60 * 60 * 1000) }
      });
      
      log.verbose(`Analytics records in the last 24 hours: ${recentCount}`);
    }
    
    await mongoose.disconnect();
  } catch (error) {
    log.error(`Failed to check analytics data: ${error.message}`);
  }
}

// Check monitoring metrics
async function checkMonitoringMetrics() {
  log.info('Checking monitoring metrics...');
  
  try {
    await mongoose.connect(config.mongoUrl, {
      useNewUrlParser: true,
      useUnifiedTopology: true
    });
    
    // Check if metrics collection exists
    const collections = await mongoose.connection.db.listCollections().toArray();
    const collectionNames = collections.map(c => c.name);
    
    if (!collectionNames.includes('metrics')) {
      log.warning('Metrics collection does not exist');
      log.info('This collection will be created when metrics are recorded');
      await mongoose.disconnect();
      return;
    }
    
    // Count metrics records
    const metricsCount = await mongoose.connection.db.collection('metrics').countDocuments();
    log.verbose(`Found ${metricsCount} metrics records`);
    
    if (metricsCount === 0) {
      log.warning('No metrics data found');
      log.info('Metrics will be recorded as the system operates');
    } else {
      log.success(`Found ${metricsCount} metrics records`);
      
      // Check metric types
      const metricCounts = await mongoose.connection.db.collection('metrics').aggregate([
        { $group: { _id: '$metricType', count: { $sum: 1 } } }
      ]).toArray();
      
      for (const metric of metricCounts) {
        log.verbose(`Metric type "${metric._id}": ${metric.count} records`);
      }
      
      // Check recent metrics
      const recentCount = await mongoose.connection.db.collection('metrics').countDocuments({
        timestamp: { $gte: new Date(Date.now() - 24 * 60 * 60 * 1000) }
      });
      
      log.verbose(`Metrics recorded in the last 24 hours: ${recentCount}`);
    }
    
    await mongoose.disconnect();
  } catch (error) {
    log.error(`Failed to check monitoring metrics: ${error.message}`);
  }
}

// Print summary
function printSummary() {
  log.info('\n=== AdMob Integration Diagnostic Summary ===\n');
  
  log.info('1. Check the logs above for any warnings or errors');
  log.info('2. Address any missing environment variables or files');
  log.info('3. Ensure database and Redis connections are working');
  log.info('4. Verify that the API server is running');
  log.info('5. Add AdMob configurations, A/B tests, and user segments if needed');
  
  log.info('\nFor more detailed information, run with --verbose flag');
  log.info('For additional help, refer to the troubleshooting section in README_ADMOB.md');
}

// Run the diagnostics
runDiagnostics().catch(error => {
  log.error(`Diagnostic failed with error: ${error.message}`);
  process.exit(1);
}); 