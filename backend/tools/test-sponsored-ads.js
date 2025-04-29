#!/usr/bin/env node

/**
 * Test script for Sponsored Ads API Endpoints
 * 
 * This script tests the sponsored ads API endpoints
 * Usage: node test-sponsored-ads.js <command>
 * 
 * Commands:
 *   get             - Get active sponsored ads
 *   impression <id> - Record an impression for a specific ad
 *   click <id>      - Record a click for a specific ad
 *   stats <id>      - Get stats for a specific ad (requires API key)
 */

const axios = require('axios');
const chalk = require('chalk');

// Configuration
const API_URL = process.env.API_URL || 'http://localhost:3000/api';
const API_KEY = process.env.API_KEY || '';
const deviceId = '00000000-0000-0000-0000-000000000TEST';

// Axios instance with common configuration
const api = axios.create({
  baseURL: API_URL,
  headers: {
    'Content-Type': 'application/json',
    'x-device-id': deviceId
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

// API functions
async function getActiveAds() {
  try {
    const response = await api.get('/sponsored-ads?location=category_below');
    printResponse(response);
    return response.data;
  } catch (error) {
    console.error(chalk.red('Failed to get active sponsored ads'));
  }
}

async function recordImpression(adId) {
  try {
    const response = await api.post(`/sponsored-ads/viewed/${adId}`);
    printResponse(response);
    return response.data;
  } catch (error) {
    console.error(chalk.red(`Failed to record impression for ad: ${adId}`));
  }
}

async function recordClick(adId) {
  try {
    const response = await api.post(`/sponsored-ads/clicked/${adId}`);
    printResponse(response);
    return response.data;
  } catch (error) {
    console.error(chalk.red(`Failed to record click for ad: ${adId}`));
  }
}

async function getAdStats(adId) {
  try {
    const response = await api.get(`/sponsored-ads/stats/${adId}`, {
      headers: {
        'x-api-key': API_KEY
      }
    });
    printResponse(response);
    return response.data;
  } catch (error) {
    console.error(chalk.red(`Failed to get stats for ad: ${adId}`));
  }
}

// Print help information
function printHelp() {
  console.log(chalk.yellow(`
Usage: node test-sponsored-ads.js <command> [options]

Commands:
  get                    - Get active sponsored ads
  impression <id>        - Record an impression for a specific ad
  click <id>             - Record a click for a specific ad
  stats <id>             - Get stats for a specific ad (requires API key)
  help                   - Show this help message

Examples:
  node test-sponsored-ads.js get
  node test-sponsored-ads.js impression 60f1a5b3e5a4c7001234abcd
  node test-sponsored-ads.js click 60f1a5b3e5a4c7001234abcd
  node test-sponsored-ads.js stats 60f1a5b3e5a4c7001234abcd
  `));
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
  
  switch (command) {
    case 'get':
      await getActiveAds();
      break;
      
    case 'impression':
      const impressionId = args[1];
      if (!impressionId) {
        console.error(chalk.red('Please provide an ad ID'));
        return;
      }
      await recordImpression(impressionId);
      break;
      
    case 'click':
      const clickId = args[1];
      if (!clickId) {
        console.error(chalk.red('Please provide an ad ID'));
        return;
      }
      await recordClick(clickId);
      break;
      
    case 'stats':
      const statsId = args[1];
      if (!statsId) {
        console.error(chalk.red('Please provide an ad ID'));
        return;
      }
      if (!API_KEY) {
        console.error(chalk.red('API key is required for this operation. Set API_KEY environment variable.'));
        return;
      }
      await getAdStats(statsId);
      break;
      
    case 'help':
    default:
      printHelp();
      break;
  }
}

// Run the main function
main().catch(error => {
  console.error(chalk.red('Error:'), error.message);
  process.exit(1);
}); 