Write-Host "Starting EventWish backend server in PRODUCTION mode with clean environment..." -ForegroundColor Red

# Check if service account file exists
$serviceAccountFile = ".\firebase-service-account.json"
if (-not (Test-Path $serviceAccountFile)) {
    Write-Host "Error: Firebase service account file not found at $serviceAccountFile" -ForegroundColor Red
    Write-Host "Please create this file with your Firebase service account credentials." -ForegroundColor Red
    Write-Host "You can download it from the Firebase Console > Project Settings > Service accounts." -ForegroundColor Red
    exit 1
}

# Read service account file
$serviceAccountJson = Get-Content $serviceAccountFile -Raw

# Parse JSON to extract project ID
try {
    $serviceAccount = $serviceAccountJson | ConvertFrom-Json
    $projectId = $serviceAccount.project_id
    
    if (-not $projectId) {
        Write-Host "Error: Could not find project_id in service account file." -ForegroundColor Red
        exit 1
    }
    
    Write-Host "Found project ID in service account: $projectId" -ForegroundColor Green
} catch {
    Write-Host "Error parsing service account JSON: $_" -ForegroundColor Red
    exit 1
}

# Create a new clean process with only the required environment variables
$env:NODE_ENV = "production"
$env:FIREBASE_PROJECT_ID = $projectId
$env:FIREBASE_SERVICE_ACCOUNT = $serviceAccountJson
$env:JWT_SECRET = "production-jwt-secret-for-neweventwish"
$env:API_KEY = "production-api-key-for-neweventwish"
$env:INTERNAL_API_KEY = "production-internal-api-key-for-neweventwish"
$env:DISABLE_GEOIP = "true"

# Explicitly remove SKIP_AUTH if it exists
if ($env:SKIP_AUTH) {
    Remove-Item env:SKIP_AUTH -ErrorAction SilentlyContinue
    Write-Host "Removed SKIP_AUTH environment variable" -ForegroundColor Yellow
}

Write-Host "Environment variables set:" -ForegroundColor Cyan
Write-Host "- NODE_ENV: $($env:NODE_ENV)"
Write-Host "- FIREBASE_PROJECT_ID: $($env:FIREBASE_PROJECT_ID)"
Write-Host "- FIREBASE_SERVICE_ACCOUNT: [Set]"
Write-Host "- JWT_SECRET: $($env:JWT_SECRET.Substring(0, 5))..."
Write-Host "- API_KEY: $($env:API_KEY.Substring(0, 5))..."
Write-Host "- INTERNAL_API_KEY: $($env:INTERNAL_API_KEY.Substring(0, 5))..."
Write-Host "- DISABLE_GEOIP: $($env:DISABLE_GEOIP)"
Write-Host "- SKIP_AUTH: [Not Set]"

Write-Host "`nStarting server in PRODUCTION mode..." -ForegroundColor Red
Write-Host "Press Ctrl+C to stop the server." -ForegroundColor Yellow

# Start the server with increased memory allocation
node --max-old-space-size=4096 server.js 