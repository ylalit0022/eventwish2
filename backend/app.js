// ... existing code ...

const errorHandler = require('./middleware/errorMiddleware');

// Add this before any route definitions
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// Add this after all route definitions
app.use(errorHandler);

// ... rest of existing code ...
