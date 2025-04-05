// ... existing code ...

const errorHandler = require('./middleware/errorMiddleware');
const seedDatabase = require('./scripts/seedDatabase');

// Import routes
const coinsRoutes = require('./routes/coinsRoutes');
const authRoute = require('./routes/auth');
const admobRoute = require('./routes/admob');
const usersRoute = require('./routes/users');

// Add this before any route definitions
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// Use routes
app.use('/api/coins', coinsRoutes);
app.use('/api/auth', authRoute);
app.use('/api/admob', admobRoute);
app.use('/api/users', usersRoute);

// Add this after database connection is established
mongoose.connection.once('open', () => {
    logger.info('MongoDB database connection established successfully');
    
    // Run seed script
    if (process.env.NODE_ENV !== 'production') {
        seedDatabase().then(() => {
            logger.info('Database seeding completed');
        }).catch(err => {
            logger.error('Database seeding failed:', err);
        });
    }
});

// Add this after all route definitions
app.use(errorHandler);

// ... rest of existing code ...
