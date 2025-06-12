@echo off
echo Starting EventWish backend server in development mode...

rem Set environment variables
set NODE_ENV=development
set FIREBASE_PROJECT_ID=eventwish-app
set SKIP_AUTH=true

rem Generate temporary secrets for development
set JWT_SECRET=dev-jwt-secret-for-testing-only
set API_KEY=dev-api-key-for-testing-only
set INTERNAL_API_KEY=dev-internal-api-key-for-testing-only

echo Environment variables set:
echo - NODE_ENV: %NODE_ENV%
echo - FIREBASE_PROJECT_ID: %FIREBASE_PROJECT_ID%
echo - SKIP_AUTH: %SKIP_AUTH%
echo - JWT_SECRET: %JWT_SECRET:~0,10%...
echo - API_KEY: %API_KEY:~0,10%...
echo - INTERNAL_API_KEY: %INTERNAL_API_KEY:~0,10%...

echo Starting server...
node server.js 