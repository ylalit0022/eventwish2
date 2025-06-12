Write-Host "Starting EventWish backend server in development mode..." -ForegroundColor Green

# Set environment variables
$env:NODE_ENV = "development"
$env:FIREBASE_PROJECT_ID = "eventwish-app"
$env:SKIP_AUTH = "true"

# Generate temporary secrets for development
$env:JWT_SECRET = "dev-jwt-secret-for-testing-only"
$env:API_KEY = "dev-api-key-for-testing-only"
$env:INTERNAL_API_KEY = "dev-internal-api-key-for-testing-only"

Write-Host "Environment variables set:" -ForegroundColor Cyan
Write-Host "- NODE_ENV: $($env:NODE_ENV)"
Write-Host "- FIREBASE_PROJECT_ID: $($env:FIREBASE_PROJECT_ID)"
Write-Host "- SKIP_AUTH: $($env:SKIP_AUTH)"
Write-Host "- JWT_SECRET: $($env:JWT_SECRET.Substring(0, 10))..."
Write-Host "- API_KEY: $($env:API_KEY.Substring(0, 10))..."
Write-Host "- INTERNAL_API_KEY: $($env:INTERNAL_API_KEY.Substring(0, 10))..."

Write-Host "Starting server..." -ForegroundColor Green
node server.js 