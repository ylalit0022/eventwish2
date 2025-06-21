const express = require('express');
const path = require('path');
const app = express();

// Set proper MIME types
express.static.mime.define({'application/javascript': ['js']});

// Serve static files from the build directory
app.use(express.static(path.join(__dirname, 'build')));

// Specific route for bundle.js to ensure proper MIME type
app.get('/bundle*.js', (req, res) => {
  const bundlePath = path.join(__dirname, 'build', req.path);
  res.set('Content-Type', 'application/javascript');
  res.sendFile(bundlePath);
});

// All other routes should serve the index.html
app.get('*', (req, res) => {
  res.sendFile(path.join(__dirname, 'build', 'index.html'));
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Admin panel server running on port ${PORT}`);
}); 