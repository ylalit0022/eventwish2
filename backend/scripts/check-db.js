/**
 * Database Connection and Models Check
 * 
 * This script tests the MongoDB connection and verifies that all required models exist.
 */

require('../config/env-loader');
const mongoose = require('mongoose');
const fs = require('fs');
const path = require('path');
const logger = require('../utils/logger');

// Connect to MongoDB
const connectDB = async () => {
  try {
    logger.info('Attempting to connect to MongoDB...');
    await mongoose.connect(process.env.MONGODB_URI, {
      useNewUrlParser: true,
      useUnifiedTopology: true,
      serverSelectionTimeoutMS: 30000, // Timeout after 30s
      socketTimeoutMS: 45000, // Close sockets after 45s of inactivity
      family: 4 // Use IPv4, skip trying IPv6
    });
    
    logger.info(`MongoDB Connected: ${mongoose.connection.host}`);
    return true;
  } catch (error) {
    logger.error(`MongoDB connection error: ${error.message}`);
    return false;
  }
};

// Check if model files exist
const checkModelFiles = () => {
  const modelsDir = path.join(__dirname, '..', 'models');
  const requiredModels = ['User.js', 'SharedWish.js', 'Template.js'];
  
  logger.info(`Checking model files in ${modelsDir}`);
  
  const missingModels = [];
  
  requiredModels.forEach(model => {
    const modelPath = path.join(modelsDir, model);
    const exists = fs.existsSync(modelPath);
    
    if (exists) {
      logger.info(`✓ Model file exists: ${model}`);
    } else {
      logger.error(`✗ Missing model file: ${model}`);
      missingModels.push(model);
    }
  });
  
  return {
    success: missingModels.length === 0,
    missingModels
  };
};

// Try to load models
const loadModels = () => {
  logger.info('Attempting to load models...');
  
  const requiredModels = ['User', 'SharedWish', 'Template'];
  const loadedModels = [];
  const failedModels = [];
  
  requiredModels.forEach(modelName => {
    try {
      const modelPath = `../models/${modelName}`;
      const Model = require(modelPath);
      loadedModels.push({
        name: modelName,
        schema: Object.keys(Model.schema.paths)
      });
      logger.info(`✓ Successfully loaded model: ${modelName}`);
    } catch (error) {
      logger.error(`✗ Failed to load model ${modelName}: ${error.message}`);
      failedModels.push({
        name: modelName,
        error: error.message
      });
    }
  });
  
  return {
    success: failedModels.length === 0,
    loadedModels,
    failedModels
  };
};

// Check collections in the database
const checkCollections = async () => {
  try {
    const collections = await mongoose.connection.db.listCollections().toArray();
    const collectionNames = collections.map(c => c.name);
    
    logger.info(`Found ${collectionNames.length} collections in database:`);
    collectionNames.forEach(name => {
      logger.info(`- ${name}`);
    });
    
    const requiredCollections = ['users', 'sharedwishes', 'templates'];
    const missingCollections = [];
    
    requiredCollections.forEach(collection => {
      if (!collectionNames.includes(collection)) {
        logger.warn(`Collection "${collection}" is missing from database`);
        missingCollections.push(collection);
      }
    });
    
    return {
      success: missingCollections.length === 0,
      collections: collectionNames,
      missingCollections
    };
  } catch (error) {
    logger.error(`Error checking collections: ${error.message}`);
    return {
      success: false,
      error: error.message
    };
  }
};

// Run all checks
const runChecks = async () => {
  logger.info('Starting database and models check...');
  
  // 1. Check model files
  const modelFilesCheck = checkModelFiles();
  
  // 2. Connect to database
  const dbConnected = await connectDB();
  
  if (!dbConnected) {
    logger.error('Database connection failed, aborting remaining checks');
    return {
      success: false,
      modelFilesCheck,
      dbConnected
    };
  }
  
  // 3. Load models
  const modelsLoaded = loadModels();
  
  // 4. Check collections
  const collectionsCheck = await checkCollections();
  
  // 5. Close connection
  await mongoose.connection.close();
  logger.info('MongoDB connection closed');
  
  // 6. Return results
  const success = modelFilesCheck.success && dbConnected && modelsLoaded.success && collectionsCheck.success;
  
  if (success) {
    logger.info('✅ All checks passed successfully!');
  } else {
    logger.error('❌ Some checks failed. See details above.');
  }
  
  return {
    success,
    modelFilesCheck,
    dbConnected,
    modelsLoaded,
    collectionsCheck
  };
};

// Add direct console logging since logger might not be working
const consoleLog = (message) => {
  console.log(`[CHECK-DB] ${message}`);
};

// Run the checks
runChecks()
  .then(results => {
    consoleLog('Check results:');
    consoleLog(JSON.stringify(results, null, 2));
    
    if (!results.success) {
      process.exit(1);
    }
  })
  .catch(error => {
    console.error(`[CHECK-DB] Error in check script: ${error.message}`);
    console.error(error.stack);
    process.exit(1);
  }); 