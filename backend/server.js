// Debug environment information
console.log('=== SERVER STARTUP DIAGNOSTICS ===');
console.log(`Node.js version: ${process.version}`);
console.log(`Platform: ${process.platform}`);
console.log(`Architecture: ${process.arch}`);
console.log(`Working directory: ${process.cwd()}`);
console.log(`Memory usage: ${JSON.stringify(process.memoryUsage())}`);
console.log('=== END DIAGNOSTICS ===');

// Load environment variables
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
// Initialize Firebase Admin SDK
require('./config/firebase');

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

// MongoDB Connection
try {
  console.log('Attempting to connect to MongoDB...');
  
  // Add a default MongoDB URI as a fallback
  const mongoURI = process.env.MONGODB_URI || 'mongodb://localhost:27017/eventwish';
  console.log(`MongoDB URI (masked): ${mongoURI.replace(/mongodb(\+srv)?:\/\/([^:]+):([^@]+)@/, 'mongodb$1://***:***@')}`);
  
  mongoose.connect(mongoURI, {
    useNewUrlParser: true,
    useUnifiedTopology: true,
  })
  .then(() => {
    console.log('✅ MongoDB Connected');
    logger.info('MongoDB Connected');
  })
  .catch((err) => {
    console.error('❌ MongoDB Connection Error:', err.message);
    logger.error('MongoDB Connection Error:', err);
    console.log('⚠️ WARNING: Continuing without MongoDB connection. Some API endpoints may not work.');
  });
} catch (error) {
  console.error('❌ Error in MongoDB connection setup:', error.message);
  logger.error('Error in MongoDB connection setup:', error);
  console.log('⚠️ WARNING: Continuing without MongoDB connection. Some API endpoints may not work.');
}

// Add global error handlers
process.on('uncaughtException', (err) => {
  console.error('Uncaught Exception:');
  console.error(err);
  logger.error('Uncaught Exception:', err);
  // Don't exit, try to keep the server running
  console.log('EMERGENCY OVERRIDE: Continuing despite uncaught exception');
});

process.on('unhandledRejection', (reason, promise) => {
  console.error('Unhandled Rejection at:', promise, 'reason:', reason);
  logger.error('Unhandled Rejection:', { reason, promise });
  // Don't exit here to allow the application to continue running
});

const app = express();

// Configure trust proxy more securely for use with Render
// Only trust the first proxy in the chain
app.set('trust proxy', 1);

// Middleware
app.use(cors());
app.use(helmet({
  contentSecurityPolicy: {
    directives: {
      defaultSrc: ["'self'"],
      scriptSrc: ["'self'", "'unsafe-inline'", "'unsafe-hashes'", "https://cdn.jsdelivr.net"],
      scriptSrcAttr: ["'unsafe-inline'"],
      connectSrc: ["'self'", "https://api.eventwish.com", "https://eventwish2.onrender.com"],
      styleSrc: ["'self'", "'unsafe-inline'", "https://cdn.jsdelivr.net"],
      imgSrc: ["'self'", "data:", "https://via.placeholder.com"],
      fontSrc: ["'self'", "https://cdn.jsdelivr.net"]
    }
  }
}));
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
  windowMs: 60 * 60 * 1000, // 60 minutes
  max: Infinity, // No limit on requests per IP (unlimited)
  message: 'Too many requests from this IP, please try again after 60 minutes',
  standardHeaders: true,
  legacyHeaders: false,
  // Add a custom key generator to handle proxies securely
  keyGenerator: (req) => {
    // Get the leftmost IP in the X-Forwarded-For header
    // This is the client's real IP when behind a trusted proxy
    const xForwardedFor = req.headers['x-forwarded-for'];
    const ip = xForwardedFor ? xForwardedFor.split(',')[0].trim() : req.ip;
    return ip;
  }
});
app.use('/api/', apiLimiter);

// Serve static files from the backendUi directory
app.use(express.static('backendUi'));

// Serve static files from the public directory
app.use(express.static('public'));

// Serve static files from the client-examples directory
app.use('/client-examples', express.static('client-examples'));

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

// Basic health check endpoint that doesn't require MongoDB
app.get('/health', (req, res) => {
  res.status(200).json({
    status: 'ok',
    timestamp: new Date().toISOString(),
    uptime: process.uptime(),
    memory: process.memoryUsage(),
    mongodb_connected: mongoose.connection.readyState === 1
  });
});

// Root endpoint
app.get('/', (req, res) => {
  res.status(200).json({
    message: 'EventWish API is running',
    version: '1.0.0',
    environment: process.env.NODE_ENV,
    documentation: '/api-docs'
  });
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
try {
  console.log('Loading routes...');
  
  try { app.use('/api/auth', require('./routes/auth')); console.log('✅ Loaded auth routes'); } 
  catch (e) { console.error('❌ Failed to load auth routes:', e.message); }
  
  try { app.use('/api/templates', require('./routes/templates')); console.log('✅ Loaded templates routes'); } 
  catch (e) { console.error('❌ Failed to load templates routes:', e.message); }
  
  try { app.use('/api/wishes', require('./routes/wishes')); console.log('✅ Loaded wishes routes'); } 
  catch (e) { console.error('❌ Failed to load wishes routes:', e.message); }
  
  try { app.use('/api/festivals', require('./routes/festivals')); console.log('✅ Loaded festivals routes'); } 
  catch (e) { console.error('❌ Failed to load festivals routes:', e.message); }
  
  try { app.use('/api/categoryIcons', require('./routes/categoryIcons')); console.log('✅ Loaded categoryIcons routes'); } 
  catch (e) { console.error('❌ Failed to load categoryIcons routes:', e.message); }
  
  try { app.use('/api/test/time', require('./routes/timeRoutes')); console.log('✅ Loaded timeRoutes routes'); } 
  catch (e) { console.error('❌ Failed to load timeRoutes routes:', e.message); }
  
  try { app.use('/api/images', require('./routes/images')); console.log('✅ Loaded images routes'); } 
  catch (e) { console.error('❌ Failed to load images routes:', e.message); }
  
  try { app.use('/api/share', require('./routes/share')); console.log('✅ Loaded share routes'); } 
  catch (e) { console.error('❌ Failed to load share routes:', e.message); }
  
  try { app.use('/api/users', require('./routes/users')); console.log('✅ Loaded users routes'); } 
  catch (e) { console.error('❌ Failed to load users routes:', e.message); }
  
  try { app.use('/api/sponsored-ads', require('./routes/sponsoredAds')); console.log('✅ Loaded sponsoredAds routes'); } 
  catch (e) { console.error('❌ Failed to load sponsoredAds routes:', e.message); }
  
  console.log('✅ All main routes loaded');
} catch (error) {
  console.error('❌ Error loading routes:', error);
}

// Time synchronization route for Android client
app.get('/api/server/time', (req, res) => {
    try {
        // Get current server timestamp in milliseconds
        const timestamp = Date.now();
        const date = new Date(timestamp);
        const formatted = date.toISOString();
        
        // Return server time in the expected format for Android client
        res.json({
            timestamp: timestamp,
            formatted: formatted,
            success: true
        });
    } catch (error) {
        console.error('Error in server time endpoint:', error);
        res.status(500).json({
            success: false,
            message: 'Error getting server time'
        });
    }
});

// Health check routes
app.use('/api/health', require('./routes/healthRoutes'));

// AdMob routes
app.use('/api/admob-ads', require('./routes/adMobRoutes')); // Admin routes
app.use('/api/admob', require('./routes/adMobClientRoutes')); // Client routes

// Coins and rewards routes
app.use('/api/coins', require('./routes/coinsRoutes')); // User coins routes

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

// Test routes
app.use('/api/test', require('./routes/testRoutes'));

// Import routes
const aboutRoutes = require('./routes/aboutRoutes');
const contactRoutes = require('./routes/contactRoutes');

// Use routes
app.use('/api/about', aboutRoutes);
app.use('/api/contact', contactRoutes);

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
const PORT = process.env.PORT || 3007;
let server; // Define server in global scope

try {
  server = app.listen(PORT, () => {
    console.log(`✅ Server running on port ${PORT}`);
    logger.info(`Server running on port ${PORT}`);
  });

  server.on('error', (error) => {
    console.error('❌ Server error:', error.message);
    logger.error('Server error:', error);
    
    // Check for specific errors
    if (error.code === 'EADDRINUSE') {
      console.error(`❌ Port ${PORT} is already in use`);
      logger.error(`Port ${PORT} is already in use`);
    }
  });
} catch (error) {
  console.error('❌ Failed to start server:', error.message);
  logger.error('Failed to start server:', error);
}

// Graceful shutdown
process.on('SIGTERM', () => {
  logger.info('SIGTERM signal received: closing HTTP server');
  if (server) {
    server.close(() => {
      logger.info('HTTP server closed');
      mongoose.connection.close(false, () => {
        logger.info('MongoDB connection closed');
        process.exit(0);
      });
    });
  } else {
    logger.info('HTTP server not initialized, exiting directly');
    process.exit(0);
  }
});
