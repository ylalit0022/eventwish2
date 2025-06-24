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
// Import job scheduler
const jobScheduler = require('./jobs/scheduler');

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

// Add the import for the root routes
const rootRoutes = require('./routes/index');

// Add the root routes before other routes
const app = express();
app.use('/api', rootRoutes);

// Configure trust proxy more securely for use with Render
// Only trust the first proxy in the chain
app.set('trust proxy', 1);

// Middleware
app.use(cors({
  origin: function(origin, callback) {
    // Allow requests with no origin (like mobile apps or curl requests)
    if (!origin) return callback(null, true);
    
    // List of allowed origins
    const allowedOrigins = [
      'http://localhost:3000',
      'http://localhost:3001',
      'http://localhost:3007',
      'http://127.0.0.1:3000',
      'http://127.0.0.1:3001',
      'http://127.0.0.1:3007'
    ];
    
    // Check if the origin is allowed
    if (allowedOrigins.indexOf(origin) !== -1 || !origin) {
      callback(null, true);
    } else {
      // Allow all origins in development mode
      if (process.env.NODE_ENV !== 'production') {
        callback(null, true);
      } else {
        callback(null, origin); // Reflect the request origin
      }
    }
  },
  credentials: true, // Allow credentials
  methods: ['GET', 'POST', 'PUT', 'DELETE', 'OPTIONS', 'PATCH'],
  allowedHeaders: ['Content-Type', 'Authorization', 'X-Dev-Email', 'X-Dev-Admin', 'X-Requested-With', 'Accept', 'Origin', 'Cache-Control']
}));

// Log all incoming requests for debugging
app.use((req, res, next) => {
  console.log(`Incoming request: ${req.method} ${req.originalUrl} from ${req.ip}`);
  console.log('Headers:', JSON.stringify(req.headers));
  next();
});

app.use(helmet({
  contentSecurityPolicy: {
    directives: {
      defaultSrc: ["'self'"],
      scriptSrc: [
        "'self'", 
        "'unsafe-inline'", 
        "'unsafe-eval'",
        "https://apis.google.com",
        "https://*.firebaseio.com",
        "https://*.firebaseapp.com",
        "https://www.gstatic.com/"
      ],
      connectSrc: [
        "'self'",
        "http://localhost:3000",
        "http://localhost:3001",
        "http://127.0.0.1:3000",
        "http://127.0.0.1:3001",
        "https://*.firebaseio.com",
        "https://*.firebaseapp.com",
        "https://www.googleapis.com",
        "https://securetoken.googleapis.com",
        "https://identitytoolkit.googleapis.com",
        "wss://*.firebaseio.com"
      ],
      frameSrc: [
        "'self'",
        "https://*.firebaseio.com",
        "https://*.firebaseapp.com"
      ],
      imgSrc: [
        "'self'",
        "data:",
        "https://*.googleusercontent.com",
        "https://www.gstatic.com/"
      ],
      styleSrc: [
        "'self'",
        "'unsafe-inline'",
        "https://fonts.googleapis.com"
      ],
      fontSrc: [
        "'self'",
        "https://fonts.gstatic.com"
      ],
      objectSrc: ["'none'"],
      upgradeInsecureRequests: []
    }
  },
  crossOriginEmbedderPolicy: false,
  crossOriginOpenerPolicy: false,
  crossOriginResourcePolicy: false
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

// Set proper MIME types for JavaScript files
express.static.mime.define({'application/javascript': ['js']});

// Admin panel removed - using locally only

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
  
  try { app.use('/api/languages', require('./routes/languages')); console.log('✅ Loaded languages routes'); } 
  catch (e) { console.error('❌ Failed to load languages routes:', e.message); }
  
  try { app.use('/api/regions', require('./routes/regions')); console.log('✅ Loaded regions routes'); } 
  catch (e) { console.error('❌ Failed to load regions routes:', e.message); }
  
  try { app.use('/api/test/time', require('./routes/timeRoutes')); console.log('✅ Loaded timeRoutes routes'); } 
  catch (e) { console.error('❌ Failed to load timeRoutes routes:', e.message); }
  
  try { app.use('/api/images', require('./routes/images')); console.log('✅ Loaded images routes'); } 
  catch (e) { console.error('❌ Failed to load images routes:', e.message); }
  
  try { app.use('/api/share', require('./routes/share')); console.log('✅ Loaded share routes'); } 
  catch (e) { console.error('❌ Failed to load share routes:', e.message); }
  
  try { app.use('/api/users', require('./routes/users')); console.log('✅ Loaded users routes (refactored)'); } 
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
const adminRoutes = require('./routes/adminRoutes');

// Use routes
app.use('/api/about', aboutRoutes);
app.use('/api/contact', contactRoutes);
app.use('/api/admin', adminRoutes); // Add admin routes

// Debug logging middleware
if (process.env.NODE_ENV === 'development') {
  app.use((req, res, next) => {
    logger.debug(`${req.method} ${req.originalUrl}`);
    next();
  });
}

// Enhanced Error handling middleware
const { errorHandler, notFoundHandler } = require('./middleware/errorMiddleware');
const { addRequestId } = require('./middleware/validationMiddleware');

// Add request ID to all requests
app.use(addRequestId);

// 404 handler for unmatched routes
app.use(notFoundHandler);

// Enhanced error handling middleware
app.use(errorHandler);

// Update the MongoDB connection error handling
mongoose.connect(process.env.MONGODB_URI, {
  useNewUrlParser: true,
  useUnifiedTopology: true,
  serverSelectionTimeoutMS: 5000 // 5 second timeout for MongoDB connection
})
.then(() => {
  console.log('✅ MongoDB Connected');
  
  // Initialize scheduled jobs after successful MongoDB connection
  require('./jobs/scheduler').initializeJobs();
  console.log('✅ Scheduled jobs initialized');
})
.catch(err => {
  console.error('❌ MongoDB Connection Error:', err.message);
  if (err.name === 'MongoServerSelectionError') {
    console.error('Details:', err.reason);
  }
  
  // Continue running the server even if MongoDB fails to connect
  console.warn('⚠️ Server running without MongoDB connection. Some features will be unavailable.');
});

// Start server
const PORT = process.env.PORT || 3001;
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
  
  // Stop all scheduled jobs
  try {
    jobScheduler.stopAllJobs();
    logger.info('Scheduled jobs stopped');
  } catch (error) {
    logger.error('Error stopping scheduled jobs:', error);
  }
  
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
