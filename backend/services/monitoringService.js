/**
 * Monitoring Service
 * 
 * Provides monitoring and metrics functionality for the application.
 */

const winston = require('winston');
const fs = require('fs');
const path = require('path');

// Ensure logs directory exists
const logsDir = path.join(__dirname, '../logs');
if (!fs.existsSync(logsDir)) {
    fs.mkdirSync(logsDir, { recursive: true });
}

// Configure logger
const logger = winston.createLogger({
    level: 'info',
    format: winston.format.combine(
        winston.format.timestamp(),
        winston.format.json()
    ),
    defaultMeta: { service: 'monitoring-service' },
    transports: [
        new winston.transports.File({ filename: path.join(logsDir, 'metrics.log') }),
        new winston.transports.Console({
            format: winston.format.combine(
                winston.format.colorize(),
                winston.format.simple()
            )
        })
    ]
});

// In-memory metrics storage
const metrics = {};

/**
 * Track a metric value
 * 
 * @param {string} name - The name of the metric to track
 * @param {number} value - The value of the metric
 * @param {Object} [tags] - Optional tags/dimensions for the metric
 */
function trackMetric(name, value, tags = {}) {
    if (!name) {
        logger.warn('Attempted to track metric with no name');
        return;
    }

    // Initialize metric if it doesn't exist
    if (!metrics[name]) {
        metrics[name] = {
            count: 0,
            sum: 0,
            min: Number.MAX_VALUE,
            max: Number.MIN_VALUE,
            values: []
        };
    }

    // Update metric values
    const metric = metrics[name];
    metric.count++;
    metric.sum += value;
    metric.min = Math.min(metric.min, value);
    metric.max = Math.max(metric.max, value);
    metric.values.push(value);
    
    // Limit stored values to prevent memory issues
    if (metric.values.length > 100) {
        metric.values.shift();
    }

    // Log metric
    logger.info('Metric tracked', {
        metric: name,
        value,
        tags,
        stats: {
            count: metric.count,
            avg: metric.sum / metric.count,
            min: metric.min,
            max: metric.max
        }
    });
}

/**
 * Get aggregated metrics data
 * 
 * @returns {Object} The metrics data
 */
function getMetrics() {
    const result = {};
    
    for (const [name, data] of Object.entries(metrics)) {
        result[name] = {
            count: data.count,
            sum: data.sum,
            avg: data.sum / data.count,
            min: data.min,
            max: data.max
        };
    }
    
    return result;
}

/**
 * Reset all metrics
 */
function resetMetrics() {
    Object.keys(metrics).forEach(key => {
        delete metrics[key];
    });
    
    logger.info('All metrics reset');
}

/**
 * Track an error event
 * 
 * @param {Error} error - The error object
 * @param {Object} [context] - Additional context information
 */
function trackError(error, context = {}) {
    logger.error('Error tracked', {
        error: {
            message: error.message,
            stack: error.stack,
            name: error.name
        },
        context
    });
}

/**
 * Track an API request with metadata
 * 
 * @param {string} endpoint - The API endpoint path
 * @param {Object} metadata - Request metadata (method, status, duration, etc.)
 */
function trackRequest(endpoint, metadata = {}) {
    if (!endpoint) {
        logger.warn('Attempted to track request with no endpoint');
        return;
    }

    // Ensure metadata has required fields
    const data = {
        timestamp: new Date().toISOString(),
        method: metadata.method || 'GET',
        statusCode: metadata.statusCode || 200,
        duration: metadata.duration || 0,
        userAgent: metadata.userAgent || 'Unknown',
        ip: metadata.ip || 'Unknown',
        ...metadata
    };

    // Log the request
    logger.info('API request tracked', {
        endpoint,
        ...data
    });

    // Track request count metric
    trackMetric('api_requests_total', 1, { endpoint, method: data.method, status: data.statusCode });
    
    // Track response time metric
    if (data.duration) {
        trackMetric('api_response_time_ms', data.duration, { endpoint });
    }

    // Track error rate for non-2xx status codes
    if (data.statusCode >= 400) {
        trackMetric('api_error_rate', 1, { endpoint, statusCode: data.statusCode });
    }
}

module.exports = {
    trackMetric,
    getMetrics,
    resetMetrics,
    trackError,
    trackRequest
};
