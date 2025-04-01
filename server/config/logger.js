const winston = require('winston');

// Define log format
const logFormat = winston.format.printf(({ level, message, timestamp }) => {
    return `${timestamp} ${level}: ${message}`;
});

// Create logger instance
const logger = winston.createLogger({
    level: process.env.LOG_LEVEL || 'info',
    format: winston.format.combine(
        winston.format.timestamp({ format: 'YYYY-MM-DD HH:mm:ss' }),
        winston.format.errors({ stack: true }),
        logFormat
    ),
    defaultMeta: { service: 'eventwish-api' },
    transports: [
        // Write logs to console
        new winston.transports.Console({
            format: winston.format.combine(
                winston.format.colorize(),
                winston.format.timestamp({ format: 'YYYY-MM-DD HH:mm:ss' }),
                logFormat
            )
        })
    ]
});

// Add file transport in production
if (process.env.NODE_ENV === 'production') {
    logger.add(
        new winston.transports.File({ 
            filename: 'logs/error.log', 
            level: 'error',
            maxsize: 5242880, // 5MB
            maxFiles: 5
        })
    );
    
    logger.add(
        new winston.transports.File({ 
            filename: 'logs/combined.log',
            maxsize: 5242880, // 5MB
            maxFiles: 5
        })
    );
}

module.exports = logger; 