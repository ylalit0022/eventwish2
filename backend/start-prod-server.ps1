Write-Host "Starting EventWish backend server in PRODUCTION mode..." -ForegroundColor Red

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

# Set environment variables
$env:NODE_ENV = "production"
$env:FIREBASE_PROJECT_ID = $projectId
$env:FIREBASE_SERVICE_ACCOUNT = $serviceAccountJson

# Check for required secrets
if (-not $env:JWT_SECRET -or -not $env:API_KEY -or -not $env:INTERNAL_API_KEY) {
    Write-Host "Error: Missing required secrets." -ForegroundColor Red
    Write-Host "Please set the following environment variables:" -ForegroundColor Red
    Write-Host "  - JWT_SECRET: A secure random string for JWT signing" -ForegroundColor Red
    Write-Host "  - API_KEY: A secure API key for external access" -ForegroundColor Red
    Write-Host "  - INTERNAL_API_KEY: A secure API key for internal services" -ForegroundColor Red
    
    # Generate example secrets
    $exampleJwtSecret = -join ((65..90) + (97..122) + (48..57) | Get-Random -Count 32 | ForEach-Object {[char]$_})
    $exampleApiKey = -join ((65..90) + (97..122) + (48..57) | Get-Random -Count 32 | ForEach-Object {[char]$_})
    $exampleInternalApiKey = -join ((65..90) + (97..122) + (48..57) | Get-Random -Count 32 | ForEach-Object {[char]$_})
    
    Write-Host "`nExample values (DO NOT USE THESE IN PRODUCTION):" -ForegroundColor Yellow
    Write-Host "`$env:JWT_SECRET = `"$exampleJwtSecret`"" -ForegroundColor Yellow
    Write-Host "`$env:API_KEY = `"$exampleApiKey`"" -ForegroundColor Yellow
    Write-Host "`$env:INTERNAL_API_KEY = `"$exampleInternalApiKey`"" -ForegroundColor Yellow
    
    exit 1
}

Write-Host "Environment variables set:" -ForegroundColor Cyan
Write-Host "- NODE_ENV: $($env:NODE_ENV)"
Write-Host "- FIREBASE_PROJECT_ID: $($env:FIREBASE_PROJECT_ID)"
Write-Host "- FIREBASE_SERVICE_ACCOUNT: [Set]"
Write-Host "- JWT_SECRET: $($env:JWT_SECRET.Substring(0, 5))..."
Write-Host "- API_KEY: $($env:API_KEY.Substring(0, 5))..."
Write-Host "- INTERNAL_API_KEY: $($env:INTERNAL_API_KEY.Substring(0, 5))..."

Write-Host "`nStarting server in PRODUCTION mode..." -ForegroundColor Red
Write-Host "Press Ctrl+C to stop the server." -ForegroundColor Yellow
node server.js 