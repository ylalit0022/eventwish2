const express = require('express');
const mongoose = require('mongoose');
const cors = require('cors');
const logger = require('./utils/logger');
const errorHandler = require('./middleware/errorMiddleware');
const seedDatabase = require('./scripts/seedDatabase');

// Initialize Express app
const app = express();

// Import routes
const coinsRoutes = require('./routes/coinsRoutes');
const authRoute = require('./routes/auth');
const admobRoute = require('./routes/admob');
const userRoutes = require('./routes/userRoutes');

// Middleware
app.use(express.json());
app.use(express.urlencoded({ extended: true }));
app.use(cors());

// Use routes
app.use('/api/coins', coinsRoutes);
app.use('/api/auth', authRoute);
app.use('/api/admob', admobRoute);
app.use('/api/users', userRoutes);

// MongoDB connection
mongoose.connection.once('open', () => {
    logger.info('MongoDB database connection established successfully');
    
    // Run seed script in development
    if (process.env.NODE_ENV !== 'production') {
        seedDatabase().then(() => {
            logger.info('Database seeding completed');
        }).catch(err => {
            logger.error('Database seeding failed:', err);
        });
    }
});

// Error handling middleware
app.use(errorHandler);

module.exports = app;
