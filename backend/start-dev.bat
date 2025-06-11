@echo off
echo Starting server in development mode with authentication disabled...
set NODE_ENV=development
set SKIP_AUTH=true
node server.js 