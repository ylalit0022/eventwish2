require('dotenv').config();
const express = require('express');
const cors = require('cors');
const path = require('path');
const connectDB = require('./config/db');

// Connect to MongoDB
connectDB();

const app = express();

// Middleware
app.use(cors());
app.use(express.json());

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

// Deep linking route
app.get('/wish/:shortCode', (req, res) => {
    const { shortCode } = req.params;
    // Serve an HTML page that can either redirect to the app or show a fallback
    res.send(`
        <!DOCTYPE html>
        <html>
        <head>
            <title>EventWish</title>
            <meta name="viewport" content="width=device-width, initial-scale=1">
            <style>
                body { font-family: Arial, sans-serif; text-align: center; padding: 20px; }
                .button { 
                    display: inline-block;
                    padding: 10px 20px;
                    margin: 10px;
                    background-color: #4CAF50;
                    color: white;
                    text-decoration: none;
                    border-radius: 5px;
                }
            </style>
        </head>
        <body>
            <h1>EventWish</h1>
            <p>View this wish in our app!</p>
            <a href="eventwish://wish/${shortCode}" class="button">Open in App</a>
            <a href="https://play.google.com/store/apps/details?id=com.ds.eventwish" class="button">Get the App</a>
            <script>
                // Attempt to open the app
                window.location.href = 'eventwish://wish/${shortCode}';
                
                // After a delay, if the app hasn't opened, show the buttons
                setTimeout(function() {
                    document.body.style.opacity = '1';
                }, 1000);
            </script>
        </body>
        </html>
    `);
});

// Routes
app.use('/api/templates', require('./routes/templates'));
app.use('/api/wishes', require('./routes/wishes'));
app.use('/api/festivals', require('./routes/festivals'));
app.use('/api/categoryIcons', require('./routes/categoryIcons'));

// Debug logging middleware
app.use((req, res, next) => {
    console.log(`${req.method} ${req.url}`);
    next();
});

// Error handling middleware
app.use((err, req, res, next) => {
    console.error(err.stack);
    res.status(500).json({ message: 'Something went wrong!' });
});

const PORT = process.env.PORT || 3000;

app.listen(PORT, () => {
    console.log(`Server is running on port ${PORT}`);
});
