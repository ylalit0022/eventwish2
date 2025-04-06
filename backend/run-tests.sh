#!/bin/bash

# Set environment variables for testing
export NODE_ENV=test
export JWT_SECRET=test-secret
export MONGODB_URI=mongodb://localhost:27017/eventwish-test

# Install dependencies if needed
if [ ! -d "node_modules" ]; then
  echo "Installing dependencies..."
  npm install
fi

# Run tests with coverage
echo "Running tests with coverage..."
npm run test:coverage

# Check if tests passed
if [ $? -eq 0 ]; then
  echo "All tests passed!"
  
  # Open coverage report if on a system with a browser
  if [ "$(uname)" == "Darwin" ]; then
    # macOS
    open coverage/lcov-report/index.html
  elif [ "$(expr substr $(uname -s) 1 5)" == "Linux" ]; then
    # Linux
    if [ -n "$DISPLAY" ]; then
      xdg-open coverage/lcov-report/index.html
    fi
  elif [ "$(expr substr $(uname -s) 1 10)" == "MINGW32_NT" ] || [ "$(expr substr $(uname -s) 1 10)" == "MINGW64_NT" ]; then
    # Windows
    start coverage/lcov-report/index.html
  fi
  
  exit 0
else
  echo "Some tests failed. Please check the output above."
  exit 1
fi 