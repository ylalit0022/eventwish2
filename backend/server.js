require('./config/env-loader');
const express = require('express');
const mongoose = require('mongoose');
const cors = require('cors');
const helmet = require('helmet');
const compression = require('compression');
const rateLimit = require('express-rate-limit');
const morgan = require('morgan');
const path = require('path');
const fs = require('fs');
const connectDB = require('./config/db');
const { generateWishLandingPage, generateFallbackLandingPage } = require('./views/wishLanding');
const logger = require('./config/logger');
const monitoringController = require('./controllers/monitoringController');
const loadBalancer = require('./config/loadBalancer');
const swagger = require('./config/swagger');

// Log environment variables for debugging (excluding sensitive ones)
console.log('NODE_ENV:', process.env.NODE_ENV);
console.log('PORT:', process.env.PORT);
console.log('MONGODB_URI exists:', !!process.env.MONGODB_URI);
console.log('JWT_SECRET exists:', !!process.env.JWT_SECRET);
console.log('API_KEY exists:', !!process.env.API_KEY);
console.log('API_BASE_URL:', process.env.API_BASE_URL);
console.log('LOG_LEVEL:', process.env.LOG_LEVEL);
console.log('VALID_APP_SIGNATURES exists:', !!process.env.VALID_APP_SIGNATURES);

// Initialize load balancer if enabled
if (process.env.LOAD_BALANCER_ENABLED === 'true') {
  loadBalancer.initLoadBalancer();
}

// Debug: Check if the model file exists
const modelPath = path.join(__dirname, 'models', 'SharedWish.js');
console.log(`Checking if model file exists at: ${modelPath}`);
console.log(`File exists: ${fs.existsSync(modelPath)}`);

// Try to import the model
try {
    const SharedWish = require('./models/SharedWish');
    console.log('SharedWish model loaded successfully');
} catch (error) {
    console.error('Error loading SharedWish model:', error);
}

// Connect to MongoDB with more detailed error logging
console.log('Attempting to connect to MongoDB...');
mongoose.connect(process.env.MONGODB_URI)
  .then(() => {
    console.log('Connected to MongoDB successfully');
    logger.info('Connected to MongoDB');
  })
  .catch((err) => {
    console.error('MongoDB connection error details:');
    console.error('Error message:', err.message);
    console.error('Error code:', err.code);
    console.error('Error name:', err.name);
    logger.error(`MongoDB connection error: ${err.message}`, { error: err });
    process.exit(1);
  });

// Add global error handlers
process.on('uncaughtException', (err) => {
  console.error('Uncaught Exception:');
  console.error(err);
  logger.error('Uncaught Exception:', err);
  process.exit(1);
});

process.on('unhandledRejection', (reason, promise) => {
  console.error('Unhandled Rejection at:', promise, 'reason:', reason);
  logger.error('Unhandled Rejection:', { reason, promise });
  // Don't exit here to allow the application to continue running
});

const app = express();

// Middleware
app.use(cors());
app.use(helmet());
app.use(compression());
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// Setup request logging
app.use(morgan('combined', { stream: { write: message => logger.http(message.trim()) } }));

// Setup request monitoring
app.use(monitoringController.trackRequestMiddleware);

// Setup Swagger UI
if (process.env.NODE_ENV !== 'production' || process.env.ENABLE_SWAGGER === 'true') {
  swagger.setupSwagger(app);
}

// Rate limiting
const apiLimiter = rateLimit({
  windowMs: 15 * 60 * 1000, // 15 minutes
  max: 100, // limit each IP to 100 requests per windowMs
  message: 'Too many requests from this IP, please try again after 15 minutes',
  standardHeaders: true,
  legacyHeaders: false,
});
app.use('/api/', apiLimiter);

// Serve static files from the backendUi directory
app.use(express.static('backendUi'));

// Serve assetlinks.json file
app.get('/.well-known/assetlinks.json', (req, res) => {
    res.setHeader('Content-Type', 'application/json');
    res.send([{
        "relation": ["delegate_permission/common.handle_all_urls"],
        "target": {
            "namespace": "android_app",
            "package_name": "com.ds.eventwish",
            "sha256_cert_fingerprints": [
                "B2:2F:26:9A:82:99:97:6C:FB:D3:6D:1D:80:DE:B0:93:22:F9:30:D2:0B:69:05:28:2F:05:60:39:0B:F1:4D:5D"
            ]
        }
    }]);
});

// Deep linking route for wishes
app.get('/wish/:shortCode', async (req, res) => {
    try {
        const { shortCode } = req.params;
        
        // Try to load the SharedWish model
        let SharedWish;
        try {
            SharedWish = require('./models/SharedWish');
            console.log('Using SharedWish model from ./models/SharedWish');
        } catch (error) {
            console.error('Error loading SharedWish from ./models/SharedWish:', error);
            try {
                SharedWish = require('./SharedWish');
                console.log('Using SharedWish model from ./SharedWish');
            } catch (error) {
                console.error('Error loading SharedWish from ./SharedWish:', error);
                throw new Error('Could not load SharedWish model');
            }
        }
        
        const wish = await SharedWish.findOne({ shortCode }).populate('template');
        
        // Track analytics
        if (wish) {
            // Increment views
            wish.views += 1;
            
            // Track unique views by IP
            const clientIp = req.headers['x-forwarded-for'] || req.connection.remoteAddress;
            if (clientIp && !wish.viewerIps.includes(clientIp)) {
                wish.viewerIps.push(clientIp);
                wish.uniqueViews += 1;
            }
            
            // Track referrer if available
            if (req.headers.referer && !wish.referrer) {
                wish.referrer = req.headers.referer;
            }
            
            // Track device info if available
            if (req.headers['user-agent'] && !wish.deviceInfo) {
                wish.deviceInfo = req.headers['user-agent'];
            }
            
            await wish.save();
        }
        
        // Generate landing page HTML
        const html = generateWishLandingPage(wish, shortCode);
        
        // Send the response
        res.send(html);
    } catch (error) {
        console.error('Error generating landing page:', error);
        const html = generateFallbackLandingPage(req.params.shortCode);
        res.send(html);
    }
});

// Routes
app.use('/api/templates', require('./routes/templates'));
app.use('/api/wishes', require('./routes/wishes'));
app.use('/api/festivals', require('./routes/festivals'));
app.use('/api/categoryIcons', require('./routes/categoryIcons'));
app.use('/api/test/time', require('./routes/timeRoutes'));
app.use('/api/images', require('./routes/images'));
app.use('/api/share', require('./routes/share'));

// Health check routes
app.use('/api/health', require('./routes/healthRoutes'));

// AdMob routes
app.use('/api/admob-ads', require('./routes/adMobRoutes')); // Admin routes
app.use('/api/admob', require('./routes/adMobClientRoutes')); // Client routes

// Analytics routes
app.use('/api/analytics', require('./routes/analyticsRoutes')); // Analytics routes

// Monitoring routes
app.use('/api/monitoring', require('./routes/monitoringRoutes')); // Monitoring routes

// Fraud detection routes
app.use('/api/fraud', require('./routes/fraudRoutes'));

// Suspicious activity routes
app.use('/api/suspicious-activity', require('./routes/suspiciousActivityRoutes'));

// A/B testing routes
app.use('/api/ab-test', require('./routes/abTestRoutes'));

// User segmentation routes
app.use('/api/segments', require('./routes/segmentRoutes'));

// Debug logging middleware
if (process.env.NODE_ENV === 'development') {
  app.use((req, res, next) => {
    logger.debug(`${req.method} ${req.originalUrl}`);
    next();
  });
}

// Error handling middleware
app.use((err, req, res, next) => {
  logger.error(`Unhandled error: ${err.message}`, { error: err, path: req.path });
  res.status(500).json({
    success: false,
    message: 'Server error',
    error: process.env.NODE_ENV === 'development' ? err.message : 'An unexpected error occurred'
  });
});

// Start server
const PORT = process.env.PORT || 3000;
const server = app.listen(PORT, () => {
    logger.info(`Server is running on port ${PORT}`);
    logger.info(`API documentation available at http://localhost:${PORT}/api-docs`);
});

// Graceful shutdown
process.on('SIGTERM', () => {
  logger.info('SIGTERM signal received: closing HTTP server');
  server.close(() => {
    logger.info('HTTP server closed');
    mongoose.connection.close(false, () => {
      logger.info('MongoDB connection closed');
      process.exit(0);
    });
  });
});
