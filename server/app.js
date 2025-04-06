const express = require('express');
const mongoose = require('mongoose');
const cors = require('cors');
const helmet = require('helmet');
const morgan = require('morgan');
require('dotenv').config();

// Import config modules
const logger = require('./config/logger');
const firebase = require('./config/firebase');

// Initialize Firebase Admin SDK
firebase.initializeFirebaseAdmin();

// Import routes
const authRoutes = require('./routes/authRoutes');
const templateRoutes = require('./routes/templates');
const wishRoutes = require('./routes/wishes');

// Initialize express app
const app = express();

// Middleware
app.use(express.json());
app.use(express.urlencoded({ extended: true }));
app.use(cors());
app.use(helmet());
app.use(morgan('dev'));

// Connect to MongoDB
mongoose.connect(process.env.MONGODB_URI)
    .then(() => {
        logger.info('Connected to MongoDB');
    })
    .catch((err) => {
        logger.error(`MongoDB connection error: ${err}`);
        process.exit(1);
    });

// Health check route
app.get('/health', (req, res) => {
    res.status(200).json({ status: 'ok', timestamp: new Date() });
});

// Routes
app.use('/api/auth', authRoutes);
app.use('/api/templates', templateRoutes);
app.use('/api/wishes', wishRoutes);

// Error handling
app.use((req, res, next) => {
    const error = new Error('Not Found');
    error.status = 404;
    next(error);
});

app.use((error, req, res, next) => {
    const status = error.status || 500;
    const message = error.message || 'Something went wrong';
    
    logger.error(`Error ${status}: ${message}`);
    
    res.status(status).json({
        error: {
            message,
            status
        }
    });
});

// Export app
module.exports = app; 