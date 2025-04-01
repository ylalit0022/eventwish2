const app = require('./app');
const logger = require('./config/logger');

// Get port from environment variables or use default
const PORT = process.env.PORT || 3000;

// Start server
app.listen(PORT, () => {
    logger.info(`Server running on port ${PORT}`);
    
    // Log environment mode
    logger.info(`Environment: ${process.env.NODE_ENV || 'development'}`);
    
    // Log server address
    const serverAddress = `http://localhost:${PORT}`;
    logger.info(`Server address: ${serverAddress}`);
    logger.info(`Health check: ${serverAddress}/health`);
});

// Handle unhandled promise rejections
process.on('unhandledRejection', (err) => {
    logger.error(`Unhandled Rejection: ${err.message}`, { stack: err.stack });
    // Close server & exit process
    process.exit(1);
});

// Handle uncaught exceptions
process.on('uncaughtException', (err) => {
    logger.error(`Uncaught Exception: ${err.message}`, { stack: err.stack });
    // Close server & exit process
    process.exit(1);
}); 