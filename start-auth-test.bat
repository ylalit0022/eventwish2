@echo off
echo Starting EventWish Firebase Authentication Test Environment
echo ========================================================

:: Check if Node.js is installed
where node >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
  echo ERROR: Node.js is not installed or not in your PATH
  echo Please install Node.js from https://nodejs.org/
  pause
  exit /b 1
)

:: Navigate to backend-mock directory
cd backend-mock

:: Check if the directory exists
if not exist package.json (
  echo ERROR: Cannot find backend-mock/package.json
  echo Please make sure you are running this script from the project root
  pause
  exit /b 1
)

:: Install dependencies if not already installed
if not exist node_modules (
  echo Installing dependencies...
  call npm install
  if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Failed to install dependencies
    pause
    exit /b 1
  )
)

:: Start the server in the background
start "EventWish Mock Server" cmd /c "npm start && pause"

:: Wait for server to start
echo Waiting for server to start...
timeout /t 5 /nobreak > nul

:: Run the tests
echo Running authentication tests...
call npm test

:: Provide instructions to stop the server
echo.
echo ========================================================
echo Tests completed. The mock server is still running.
echo To stop the server, close the other command window or press Ctrl+C there.
echo.
pause 