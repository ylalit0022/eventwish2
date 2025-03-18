#!/usr/bin/env node

/**
 * API Tester for Coins Endpoints
 * 
 * This script allows testing the coins-related API endpoints
 * Usage: node api-tester.js <command> [options]
 * 
 * Commands:
 *   plan             - Get plan configuration
 *   get <deviceId>   - Get coins for device
 *   add <deviceId>   - Add coins for device
 *   unlock <deviceId> - Unlock feature using coins
 *   validate         - Validate unlock signature
 *   report <deviceId> - Report unlock to server
 *   time             - Get server time
 */

const axios = require('axios');
const crypto = require('crypto');
const chalk = require('chalk');

// Configuration
const API_URL = process.env.API_URL || 'https://eventwish2.onrender.com/api';
const API_KEY = process.env.API_KEY || ''; // Add your API key here
const defaultDeviceId = '00000000-0000-0000-0000-000000000000';

// Axios instance with common configuration
const api = axios.create({
  baseURL: API_URL,
  headers: {
    'Content-Type': 'application/json',
    'x-api-key': API_KEY
  },
  timeout: 10000
});

// Log request/response for debugging
api.interceptors.request.use(request => {
  console.log(chalk.blue('➤ Request:'), chalk.blue.bold(request.method.toUpperCase()), request.url);
  if (request.data) {
    console.log(chalk.blue('➤ Request data:'), request.data);
  }
  return request;
});

api.interceptors.response.use(
  response => {
    console.log(chalk.green('✓ Response:'), response.status, response.statusText);
    return response;
  },
  error => {
    if (error.response) {
      console.log(chalk.red('✗ Response:'), error.response.status, error.response.statusText);
      console.log(chalk.red('✗ Error data:'), error.response.data);
    } else {
      console.log(chalk.red('✗ Error:'), error.message);
    }
    return Promise.reject(error);
  }
);

// Helper to print response data
function printResponse(response) {
  console.log(chalk.yellow('Response Data:'));
  console.log(JSON.stringify(response.data, null, 2));
}

// Helper to generate signature
function generateSignature(deviceId, timestamp, duration) {
  const data = `${deviceId}:${timestamp}:${duration}`;
  const secret = process.env.JWT_SECRET || 'eventwish-coins-secret-key';
  return crypto.createHmac('sha256', secret).update(data).digest('hex');
}

// API functions
async function getPlanConfiguration() {
  try {
    const response = await api.get('/coins/plan');
    printResponse(response);
    return response.data;
  } catch (error) {
    console.error(chalk.red('Failed to get plan configuration'));
  }
}

async function getCoins(deviceId) {
  try {
    const response = await api.get(`/coins/${deviceId}`);
    printResponse(response);
    return response.data;
  } catch (error) {
    console.error(chalk.red(`Failed to get coins for device: ${deviceId}`));
  }
}

async function addCoins(deviceId, amount = 10) {
  try {
    const adUnitId = 'ca-app-pub-3940256099942544/5224354917'; // Test ad unit ID
    const response = await api.post(`/coins/${deviceId}`, {
      amount,
      adUnitId,
      adName: 'Test Rewarded Ad',
      deviceInfo: {
        os: 'nodejs',
        version: process.version,
        platform: process.platform
      }
    });
    printResponse(response);
    return response.data;
  } catch (error) {
    console.error(chalk.red(`Failed to add coins for device: ${deviceId}`));
  }
}

async function unlockFeature(deviceId) {
  try {
    const response = await api.post(`/coins/${deviceId}/unlock`, {});
    printResponse(response);
    return response.data;
  } catch (error) {
    console.error(chalk.red(`Failed to unlock feature for device: ${deviceId}`));
  }
}

async function validateUnlock(deviceId, timestamp, duration, signature) {
  try {
    const response = await api.post('/coins/validate', {
      deviceId,
      timestamp,
      duration,
      signature
    });
    printResponse(response);
    return response.data;
  } catch (error) {
    console.error(chalk.red('Failed to validate unlock'));
  }
}

async function reportUnlock(deviceId, timestamp, duration) {
  try {
    const response = await api.post('/coins/report', {
      deviceId,
      timestamp,
      duration
    });
    printResponse(response);
    return response.data;
  } catch (error) {
    console.error(chalk.red(`Failed to report unlock for device: ${deviceId}`));
  }
}

async function getServerTime() {
  try {
    const clientTime = Date.now();
    const response = await api.get('/test/time', {
      headers: {
        'x-request-time': clientTime
      }
    });
    
    printResponse(response);
    
    // Calculate time difference
    const serverTime = response.data.timestamp;
    const roundTripTime = Date.now() - clientTime;
    const timeDiff = serverTime - (clientTime + Math.floor(roundTripTime / 2));
    
    console.log(chalk.yellow('Time Analysis:'));
    console.log(`Client time:     ${new Date(clientTime).toISOString()}`);
    console.log(`Server time:     ${response.data.date}`);
    console.log(`Round-trip time: ${roundTripTime}ms`);
    console.log(`Time difference: ${timeDiff}ms (${timeDiff > 0 ? 'server ahead' : 'server behind'})`);
    
    return response.data;
  } catch (error) {
    console.error(chalk.red('Failed to get server time'));
  }
}

// Main function to parse command line arguments and execute appropriate function
async function main() {
  const args = process.argv.slice(2);
  const command = args[0];
  
  if (!command) {
    printHelp();
    return;
  }
  
  console.log(chalk.green(`API URL: ${API_URL}`));
  console.log(chalk.green(`API Key: ${API_KEY ? '****' + API_KEY.slice(-4) : 'Not set'}`));
  
  switch (command) {
    case 'plan':
      await getPlanConfiguration();
      break;
      
    case 'get':
      const getDeviceId = args[1] || defaultDeviceId;
      await getCoins(getDeviceId);
      break;
      
    case 'add':
      const addDeviceId = args[1] || defaultDeviceId;
      const amount = parseInt(args[2]) || 10;
      await addCoins(addDeviceId, amount);
      break;
      
    case 'unlock':
      const unlockDeviceId = args[1] || defaultDeviceId;
      await unlockFeature(unlockDeviceId);
      break;
      
    case 'validate':
      const validateDeviceId = args[1] || defaultDeviceId;
      const timestamp = args[2] || Date.now();
      const duration = args[3] || 30;
      let signature = args[4];
      
      if (!signature) {
        signature = generateSignature(validateDeviceId, timestamp, duration);
        console.log(chalk.blue(`Generated signature: ${signature}`));
      }
      
      await validateUnlock(validateDeviceId, timestamp, duration, signature);
      break;
      
    case 'report':
      const reportDeviceId = args[1] || defaultDeviceId;
      const reportTimestamp = args[2] || Date.now();
      const reportDuration = args[3] || 30;
      await reportUnlock(reportDeviceId, reportTimestamp, reportDuration);
      break;
      
    case 'time':
      await getServerTime();
      break;
      
    case 'help':
    default:
      printHelp();
      break;
  }
}

// Print help information
function printHelp() {
  console.log(`
${chalk.bold('API Tester for Coins Endpoints')}

Usage: node api-tester.js <command> [options]

${chalk.bold('Commands:')}
  ${chalk.green('plan')}             - Get plan configuration
  ${chalk.green('get')} <deviceId>   - Get coins for device
  ${chalk.green('add')} <deviceId> [amount] - Add coins for device (default: 10)
  ${chalk.green('unlock')} <deviceId> - Unlock feature using coins
  ${chalk.green('validate')} <deviceId> [timestamp] [duration] [signature] - Validate unlock signature
  ${chalk.green('report')} <deviceId> [timestamp] [duration] - Report unlock to server
  ${chalk.green('time')}             - Get server time
  ${chalk.green('help')}             - Show this help information

${chalk.bold('Environment Variables:')}
  API_URL - API base URL (default: https://eventwish2.onrender.com/api)
  API_KEY - API key for authentication
  
${chalk.bold('Examples:')}
  node api-tester.js plan
  node api-tester.js get device-123
  node api-tester.js add device-123 50
  node api-tester.js unlock device-123
  node api-tester.js validate device-123
  node api-tester.js report device-123
  node api-tester.js time
  `);
}

// Execute main function
main().catch(error => {
  console.error(chalk.red('Unhandled error:'), error);
  process.exit(1);
}); 