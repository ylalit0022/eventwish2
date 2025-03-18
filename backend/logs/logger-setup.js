const winston = require('winston');
const path = require('path');
const fs = require('fs');

// Create logs directory if it doesn't exist
const logsDir = path.join(__dirname, '..');
if (!fs.existsSync(logsDir)) {
  fs.mkdirSync(logsDir);
}

// Define log file paths
const errorLogPath = path.join(logsDir, 'error.log');
const combinedLogPath = path.join(logsDir, 'combined.log');
const coinsLogPath = path.join(logsDir, 'coins.log');
const timeSyncLogPath = path.join(logsDir, 'time-sync.log');

// Define log format
const logFormat = winston.format.combine(
  winston.format.timestamp({ format: 'YYYY-MM-DD HH:mm:ss' }),
  winston.format.errors({ stack: true }),
  winston.format.splat(),
  winston.format.json()
);

// Define console format (more readable)
const consoleFormat = winston.format.combine(
  winston.format.colorize(),
  winston.format.timestamp({ format: 'YYYY-MM-DD HH:mm:ss' }),
  winston.format.printf(({ level, message, timestamp, ...metadata }) => {
    let metaStr = '';
    
    if (Object.keys(metadata).length > 0 && metadata.stack) {
      // If it's an error with stack trace
      metaStr = `\n${metadata.stack}`;
    } else if (Object.keys(metadata).length > 0) {
      // For other metadata
      metaStr = Object.keys(metadata).length ? `\n${JSON.stringify(metadata, null, 2)}` : '';
    }
    
    return `${timestamp} ${level}: ${message}${metaStr}`;
  })
);

// Custom filter for coins operations
const coinsFilter = winston.format((info) => {
  if (info.category === 'coins' || info.message.includes('coins') || 
      info.message.includes('unlock') || info.message.includes('reward')) {
    return info;
  }
  return false;
})();

// Custom filter for time sync operations
const timeSyncFilter = winston.format((info) => {
  if (info.category === 'timeSync' || info.message.includes('time sync') || 
      info.message.includes('time manipulation') || info.message.includes('timestamp') ||
      info.timeSync === true) {
    return info;
  }
  return false;
})();

// Create the logger
const logger = winston.createLogger({
  level: process.env.LOG_LEVEL || 'info',
  format: logFormat,
  defaultMeta: { service: 'eventwish-api' },
  transports: [
    // Write all errors to error.log
    new winston.transports.File({
      filename: errorLogPath,
      level: 'error',
      handleExceptions: true
    }),
    
    // Write all logs to combined.log
    new winston.transports.File({
      filename: combinedLogPath
    }),
    
    // Write coins-related logs to coins.log
    new winston.transports.File({
      filename: coinsLogPath,
      format: winston.format.combine(
        coinsFilter,
        logFormat
      )
    }),
    
    // Write time-sync-related logs to time-sync.log
    new winston.transports.File({
      filename: timeSyncLogPath,
      format: winston.format.combine(
        timeSyncFilter,
        logFormat
      )
    })
  ],
  // Don't exit on handled exceptions
  exitOnError: false
});

// Add console transport for non-production environments
if (process.env.NODE_ENV !== 'production') {
  logger.add(new winston.transports.Console({
    format: consoleFormat,
    handleExceptions: true
  }));
}

// Specialized logging for coins operations
const coinsLogger = {
  reward: (deviceId, amount, adUnitId, message, metadata = {}) => {
    logger.info(`REWARD: Device ${deviceId} earned ${amount} coins for ad ${adUnitId}. ${message || ''}`, {
      category: 'coins',
      operation: 'reward',
      deviceId,
      amount,
      adUnitId,
      ...metadata
    });
  },
  
  unlock: (deviceId, duration, message, metadata = {}) => {
    logger.info(`UNLOCK: Device ${deviceId} unlocked feature for ${duration} days. ${message || ''}`, {
      category: 'coins',
      operation: 'unlock',
      deviceId,
      duration,
      ...metadata
    });
  },
  
  validate: (deviceId, isValid, message, metadata = {}) => {
    logger.info(`VALIDATE: Device ${deviceId} unlock validation: ${isValid ? 'VALID' : 'INVALID'}. ${message || ''}`, {
      category: 'coins',
      operation: 'validate',
      deviceId,
      isValid,
      ...metadata
    });
  },
  
  timeSync: (deviceId, clientTime, serverTime, offset, metadata = {}) => {
    logger.info(`TIME SYNC: Device ${deviceId} time offset: ${offset}ms. Server: ${serverTime}, Client: ${clientTime}`, {
      category: 'timeSync',
      operation: 'sync',
      deviceId,
      clientTime,
      serverTime,
      offset,
      timeSync: true,
      ...metadata
    });
  },
  
  timeSuspicious: (deviceId, message, metadata = {}) => {
    logger.warn(`TIME SUSPICIOUS: Device ${deviceId}. ${message}`, {
      category: 'timeSync',
      operation: 'suspicious',
      deviceId,
      timeSync: true,
      ...metadata
    });
  },
  
  fraud: (deviceId, operation, message, metadata = {}) => {
    logger.warn(`FRAUD: Device ${deviceId} attempted fraud in ${operation}. ${message}`, {
      category: 'coins',
      operation: 'fraud',
      deviceId,
      fraudOperation: operation,
      ...metadata
    });
  }
};

module.exports = {
  logger,
  coinsLogger
}; 