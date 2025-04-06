/**
 * This script simulates authentication flow for testing purposes
 * It interacts with both Firebase and our mock server
 */
const fetch = require('node-fetch');
const fs = require('fs');
const path = require('path');
const API_URL = 'http://localhost:3000/api';

// Setup logging to both console and file
const LOG_FILE = path.join(__dirname, '../backend-mock/test-logs', `auth-test-${new Date().toISOString().replace(/:/g, '-')}.log`);

// Create a write stream for the log file
const logStream = fs.createWriteStream(LOG_FILE, { flags: 'a' });

// Custom logger function
function log(message) {
  const timestamp = new Date().toISOString();
  const logMessage = `[${timestamp}] ${message}`;
  
  // Log to console
  console.log(logMessage);
  
  // Log to file
  logStream.write(logMessage + '\n');
}

/**
 * Test Firebase Phone Authentication flow:
 * 1. Firebase auth (simulated) - phone verification and sign in with credential
 * 2. Server-side registration with Firebase token
 * 3. Server-side login with password
 * 4. Token refresh
 * 5. Password reset flow
 */
async function testAuthFlow() {
  log('🔐 TESTING AUTHENTICATION FLOW 🔐');
  log('==================================');
  
  // Simulating Firebase phone auth
  log('\n📱 STEP 1: Firebase Phone Authentication (Simulated)');
  const mockFirebaseUser = {
    uid: 'firebase-' + Math.random().toString(36).substring(2, 10),
    phoneNumber: '+1234567890',
    idToken: 'mock-firebase-id-token-' + Math.random().toString(36).substring(2, 10)
  };
  log('Firebase User: ' + JSON.stringify(mockFirebaseUser, null, 2));
  
  // 2. User registration with backend
  log('\n📝 STEP 2: Registering User with Backend');
  let registrationData = await registerUser(
    mockFirebaseUser.phoneNumber, 
    'Password123!',
    mockFirebaseUser.uid,
    mockFirebaseUser.idToken,
    'Test User'
  );
  
  if (!registrationData) {
    log('❌ Registration failed. Exiting test.');
    return;
  }
  
  log('✅ Registration successful!');
  log('Access Token: ' + registrationData.token.substring(0, 20) + '...');
  log('Refresh Token: ' + registrationData.refreshToken.substring(0, 20) + '...');
  log('Token expires in: ' + registrationData.expiresIn + ' seconds');
  
  // 3. User login with password
  log('\n🔑 STEP 3: User Login with Password');
  let loginData = await loginUser(mockFirebaseUser.phoneNumber, 'Password123!');
  
  if (!loginData) {
    log('❌ Login failed. Exiting test.');
    return;
  }
  
  log('✅ Login successful!');
  log('Access Token: ' + loginData.token.substring(0, 20) + '...');
  log('Refresh Token: ' + loginData.refreshToken.substring(0, 20) + '...');
  
  // 4. Get user data with token
  log('\n👤 STEP 4: Get User Profile');
  let userData = await getUserProfile(loginData.token);
  
  if (!userData) {
    log('❌ Getting user profile failed. Exiting test.');
    return;
  }
  
  log('✅ User profile: ' + JSON.stringify(userData, null, 2));
  
  // 5. Token refresh
  log('\n🔄 STEP 5: Token Refresh');
  let refreshData = await refreshToken(loginData.refreshToken);
  
  if (!refreshData) {
    log('❌ Token refresh failed. Exiting test.');
    return;
  }
  
  log('✅ Token refresh successful!');
  log('New Access Token: ' + refreshData.token.substring(0, 20) + '...');
  log('New Refresh Token: ' + refreshData.refreshToken.substring(0, 20) + '...');
  
  // 6. Password reset flow
  log('\n🔒 STEP 6: Password Reset Flow');
  log('6.1: Request password reset code');
  let resetCodeResult = await requestResetCode(mockFirebaseUser.phoneNumber);
  
  if (!resetCodeResult) {
    log('❌ Requesting reset code failed. Exiting test.');
    return;
  }
  
  log('6.2: Reset password with verification code');
  log('Using fixed code 123456 from mock server');
  let resetPasswordResult = await resetPassword(
    mockFirebaseUser.phoneNumber,
    '123456',
    'NewPassword456!'
  );
  
  if (!resetPasswordResult) {
    log('❌ Password reset failed. Exiting test.');
    return;
  }
  
  log('✅ Password reset successful!');
  
  // 7. Login with new password
  log('\n🔐 STEP 7: Login with New Password');
  let newLoginData = await loginUser(mockFirebaseUser.phoneNumber, 'NewPassword456!');
  
  if (!newLoginData) {
    log('❌ Login with new password failed. Exiting test.');
    return;
  }
  
  log('✅ Login with new password successful!');
  log('Access Token: ' + newLoginData.token.substring(0, 20) + '...');
  
  log('\n✅ ALL TESTS PASSED! The authentication flow is working correctly.');
}

// Helper functions to interact with the API

async function registerUser(phoneNumber, password, firebaseUid, idToken, displayName) {
  try {
    log(`Sending registration request for ${phoneNumber}...`);
    
    const response = await fetch(`${API_URL}/auth/register`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        phoneNumber,
        password,
        firebaseUid,
        idToken,
        displayName
      })
    });
    
    if (!response.ok) {
      const error = await response.json();
      log('❌ Registration error: ' + JSON.stringify(error, null, 2));
      return null;
    }
    
    return await response.json();
  } catch (error) {
    log('❌ Registration failed: ' + error.message);
    return null;
  }
}

async function loginUser(phoneNumber, password) {
  try {
    log(`Sending login request for ${phoneNumber}...`);
    
    const response = await fetch(`${API_URL}/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ phoneNumber, password })
    });
    
    if (!response.ok) {
      const error = await response.json();
      log('❌ Login error: ' + JSON.stringify(error, null, 2));
      return null;
    }
    
    return await response.json();
  } catch (error) {
    log('❌ Login failed: ' + error.message);
    return null;
  }
}

async function getUserProfile(token) {
  try {
    log('Fetching user profile...');
    
    const response = await fetch(`${API_URL}/auth/me`, {
      method: 'GET',
      headers: { 
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      }
    });
    
    if (!response.ok) {
      const error = await response.json();
      log('❌ Get user profile error: ' + JSON.stringify(error, null, 2));
      return null;
    }
    
    return await response.json();
  } catch (error) {
    log('❌ Get user profile failed: ' + error.message);
    return null;
  }
}

async function refreshToken(refreshToken) {
  try {
    log('Requesting token refresh...');
    
    const response = await fetch(`${API_URL}/auth/refresh`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken })
    });
    
    if (!response.ok) {
      const error = await response.json();
      log('❌ Token refresh error: ' + JSON.stringify(error, null, 2));
      return null;
    }
    
    return await response.json();
  } catch (error) {
    log('❌ Token refresh failed: ' + error.message);
    return null;
  }
}

async function requestResetCode(phoneNumber) {
  try {
    log(`Requesting password reset code for ${phoneNumber}...`);
    
    const response = await fetch(`${API_URL}/auth/password/reset/send-code`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ phoneNumber })
    });
    
    if (!response.ok) {
      const error = await response.json();
      log('❌ Request reset code error: ' + JSON.stringify(error, null, 2));
      return null;
    }
    
    return await response.json();
  } catch (error) {
    log('❌ Request reset code failed: ' + error.message);
    return null;
  }
}

async function resetPassword(phoneNumber, verificationCode, newPassword) {
  try {
    log(`Resetting password for ${phoneNumber} with verification code...`);
    
    const response = await fetch(`${API_URL}/auth/password/reset`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ phoneNumber, verificationCode, newPassword })
    });
    
    if (!response.ok) {
      const error = await response.json();
      log('❌ Reset password error: ' + JSON.stringify(error, null, 2));
      return null;
    }
    
    return await response.json();
  } catch (error) {
    log('❌ Reset password failed: ' + error.message);
    return null;
  }
}

// Run the test
testAuthFlow()
  .catch(error => {
    log(`❌ Unhandled error in test: ${error.message}`);
    log(error.stack);
  })
  .finally(() => {
    // Close the log file stream when done
    log('Test completed. Log saved to: ' + LOG_FILE);
    logStream.end();
  }); 