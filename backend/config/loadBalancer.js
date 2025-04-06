/**
 * Load Balancer Configuration
 * 
 * This module provides configuration for load balancing ad-related endpoints.
 * It includes settings for health checks, auto-scaling, and traffic distribution.
 */

const os = require('os');
const cluster = require('cluster');
const numCPUs = os.cpus().length;

// Configuration for load balancing
const loadBalancerConfig = {
  // Number of worker processes to spawn
  workers: process.env.WORKER_COUNT || numCPUs,
  
  // Health check configuration
  healthCheck: {
    path: '/api/health',
    interval: 30000, // 30 seconds
    timeout: 5000, // 5 seconds
    unhealthyThreshold: 3,
    healthyThreshold: 2
  },
  
  // Auto-scaling configuration
  autoScaling: {
    enabled: process.env.AUTO_SCALING_ENABLED === 'true',
    minWorkers: parseInt(process.env.MIN_WORKERS || '2'),
    maxWorkers: parseInt(process.env.MAX_WORKERS || numCPUs * 2),
    cpuThreshold: parseInt(process.env.CPU_THRESHOLD || '70'), // Scale up when CPU usage > 70%
    memoryThreshold: parseInt(process.env.MEMORY_THRESHOLD || '80'), // Scale up when memory usage > 80%
    scaleUpCooldown: parseInt(process.env.SCALE_UP_COOLDOWN || '60000'), // 1 minute
    scaleDownCooldown: parseInt(process.env.SCALE_DOWN_COOLDOWN || '300000') // 5 minutes
  },
  
  // Endpoint-specific configurations
  endpoints: {
    // Ad-related endpoints
    '/api/admob': {
      priority: 'high',
      rateLimit: {
        windowMs: 60000, // 1 minute
        max: 100 // 100 requests per minute
      },
      timeout: 10000 // 10 seconds
    },
    '/api/client': {
      priority: 'highest',
      rateLimit: {
        windowMs: 60000, // 1 minute
        max: 200 // 200 requests per minute
      },
      timeout: 5000 // 5 seconds
    },
    '/api/analytics': {
      priority: 'medium',
      rateLimit: {
        windowMs: 60000, // 1 minute
        max: 150 // 150 requests per minute
      },
      timeout: 15000 // 15 seconds
    },
    '/api/fraud': {
      priority: 'high',
      rateLimit: {
        windowMs: 60000, // 1 minute
        max: 100 // 100 requests per minute
      },
      timeout: 8000 // 8 seconds
    },
    '/api/suspicious-activity': {
      priority: 'medium',
      rateLimit: {
        windowMs: 60000, // 1 minute
        max: 50 // 50 requests per minute
      },
      timeout: 12000 // 12 seconds
    }
  }
};

/**
 * Initialize the load balancer
 * @returns {Object} Load balancer configuration
 */
function initLoadBalancer() {
  if (cluster.isMaster) {
    console.log(`Master process ${process.pid} is running`);
    
    // Fork workers
    for (let i = 0; i < loadBalancerConfig.workers; i++) {
      cluster.fork();
    }
    
    // Handle worker exit
    cluster.on('exit', (worker, code, signal) => {
      console.log(`Worker ${worker.process.pid} died with code: ${code} and signal: ${signal}`);
      console.log('Starting a new worker');
      cluster.fork();
    });
    
    // Set up auto-scaling if enabled
    if (loadBalancerConfig.autoScaling.enabled) {
      setupAutoScaling();
    }
  } else {
    console.log(`Worker ${process.pid} started`);
  }
  
  return loadBalancerConfig;
}

/**
 * Set up auto-scaling based on CPU and memory usage
 */
function setupAutoScaling() {
  let lastScaleUp = 0;
  let lastScaleDown = 0;
  
  // Check system resources every minute
  setInterval(() => {
    const cpuUsage = getCpuUsage();
    const memoryUsage = getMemoryUsage();
    const now = Date.now();
    
    console.log(`Current CPU usage: ${cpuUsage}%, Memory usage: ${memoryUsage}%`);
    console.log(`Current worker count: ${Object.keys(cluster.workers).length}`);
    
    // Scale up if CPU or memory usage is high
    if ((cpuUsage > loadBalancerConfig.autoScaling.cpuThreshold || 
         memoryUsage > loadBalancerConfig.autoScaling.memoryThreshold) && 
        Object.keys(cluster.workers).length < loadBalancerConfig.autoScaling.maxWorkers &&
        now - lastScaleUp > loadBalancerConfig.autoScaling.scaleUpCooldown) {
      
      console.log('Scaling up: Adding a new worker');
      cluster.fork();
      lastScaleUp = now;
    }
    
    // Scale down if CPU and memory usage is low
    if (cpuUsage < loadBalancerConfig.autoScaling.cpuThreshold / 2 && 
        memoryUsage < loadBalancerConfig.autoScaling.memoryThreshold / 2 &&
        Object.keys(cluster.workers).length > loadBalancerConfig.autoScaling.minWorkers &&
        now - lastScaleDown > loadBalancerConfig.autoScaling.scaleDownCooldown) {
      
      console.log('Scaling down: Removing a worker');
      const workerIds = Object.keys(cluster.workers);
      const workerId = workerIds[workerIds.length - 1];
      cluster.workers[workerId].kill();
      lastScaleDown = now;
    }
  }, 60000); // Check every minute
}

/**
 * Get current CPU usage as a percentage
 * @returns {number} CPU usage percentage
 */
function getCpuUsage() {
  const cpus = os.cpus();
  let totalIdle = 0;
  let totalTick = 0;
  
  for (const cpu of cpus) {
    for (const type in cpu.times) {
      totalTick += cpu.times[type];
    }
    totalIdle += cpu.times.idle;
  }
  
  // Calculate CPU usage percentage
  return 100 - (totalIdle / totalTick * 100);
}

/**
 * Get current memory usage as a percentage
 * @returns {number} Memory usage percentage
 */
function getMemoryUsage() {
  const totalMemory = os.totalmem();
  const freeMemory = os.freemem();
  return ((totalMemory - freeMemory) / totalMemory * 100);
}

module.exports = {
  config: loadBalancerConfig,
  initLoadBalancer
}; 