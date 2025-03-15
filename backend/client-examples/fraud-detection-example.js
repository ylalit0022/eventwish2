/**
 * EventWish AdMob Fraud Detection Client Example
 * 
 * This example demonstrates how to integrate with the fraud detection system
 * to help prevent click fraud in your AdMob implementation.
 */

// Configuration
const API_BASE_URL = 'https://eventwish2.onrender.com';
const API_KEY = '8da9c210aa3635693bf68f85c5a3bc070e97cf43fdf9893ecf8b8fb08d285c16'; // From .env.temp
const APP_SIGNATURE = 'app_sig_1'; // From .env.temp

/**
 * Generate a device fingerprint
 * @returns {Promise<string>} Device fingerprint
 */
async function generateDeviceFingerprint() {
  try {
    // Collect device information
    const deviceInfo = {
      userAgent: navigator.userAgent,
      language: navigator.language,
      platform: navigator.platform,
      screenWidth: window.screen.width,
      screenHeight: window.screen.height,
      screenDepth: window.screen.colorDepth,
      timezone: Intl.DateTimeFormat().resolvedOptions().timeZone,
      timezoneOffset: new Date().getTimezoneOffset(),
      localStorage: !!window.localStorage,
      sessionStorage: !!window.sessionStorage,
      cookiesEnabled: navigator.cookieEnabled,
      doNotTrack: navigator.doNotTrack,
      hardwareConcurrency: navigator.hardwareConcurrency || 'unknown',
      deviceMemory: navigator.deviceMemory || 'unknown',
      plugins: Array.from(navigator.plugins || []).map(p => p.name),
      canvas: getCanvasFingerprint(),
      webgl: getWebGLFingerprint(),
      fonts: await getFontFingerprint(),
      audio: await getAudioFingerprint()
    };
    
    // Create fingerprint string
    const fingerprintString = JSON.stringify(deviceInfo);
    
    // Hash the fingerprint (simple hash for example purposes)
    const fingerprint = await hashString(fingerprintString);
    
    return fingerprint;
  } catch (error) {
    console.error('Error generating device fingerprint:', error);
    return 'unknown';
  }
}

/**
 * Get canvas fingerprint
 * @returns {string} Canvas fingerprint
 */
function getCanvasFingerprint() {
  try {
    const canvas = document.createElement('canvas');
    const ctx = canvas.getContext('2d');
    
    // Draw something unique
    canvas.width = 200;
    canvas.height = 50;
    ctx.textBaseline = 'top';
    ctx.font = '14px Arial';
    ctx.fillStyle = '#f60';
    ctx.fillRect(10, 10, 100, 30);
    ctx.fillStyle = '#069';
    ctx.fillText('EventWish Fingerprint', 2, 15);
    ctx.fillStyle = 'rgba(102, 204, 0, 0.7)';
    ctx.fillText('EventWish Fingerprint', 4, 17);
    
    return canvas.toDataURL();
  } catch (error) {
    return 'canvas-not-supported';
  }
}

/**
 * Get WebGL fingerprint
 * @returns {string} WebGL fingerprint
 */
function getWebGLFingerprint() {
  try {
    const canvas = document.createElement('canvas');
    const gl = canvas.getContext('webgl') || canvas.getContext('experimental-webgl');
    
    if (!gl) {
      return 'webgl-not-supported';
    }
    
    // Get WebGL info
    const debugInfo = gl.getExtension('WEBGL_debug_renderer_info');
    const vendor = gl.getParameter(debugInfo ? debugInfo.UNMASKED_VENDOR_WEBGL : gl.VENDOR);
    const renderer = gl.getParameter(debugInfo ? debugInfo.UNMASKED_RENDERER_WEBGL : gl.RENDERER);
    
    return `${vendor}~${renderer}`;
  } catch (error) {
    return 'webgl-error';
  }
}

/**
 * Get font fingerprint
 * @returns {Promise<string>} Font fingerprint
 */
async function getFontFingerprint() {
  try {
    // Common fonts to test
    const fonts = [
      'Arial', 'Arial Black', 'Arial Narrow', 'Calibri', 'Cambria', 
      'Comic Sans MS', 'Courier New', 'Georgia', 'Impact', 
      'Lucida Console', 'Tahoma', 'Times New Roman', 'Trebuchet MS', 'Verdana'
    ];
    
    // Create a test element
    const testElement = document.createElement('span');
    testElement.style.position = 'absolute';
    testElement.style.left = '-9999px';
    testElement.style.fontSize = '72px';
    testElement.innerHTML = 'mmmmmmmmmmlli';
    document.body.appendChild(testElement);
    
    // Check which fonts are available
    const availableFonts = [];
    
    // Get width with default font
    const defaultWidth = testElement.offsetWidth;
    const defaultHeight = testElement.offsetHeight;
    
    for (const font of fonts) {
      testElement.style.fontFamily = `'${font}', monospace`;
      
      // If width changed, font is available
      if (testElement.offsetWidth !== defaultWidth || testElement.offsetHeight !== defaultHeight) {
        availableFonts.push(font);
      }
    }
    
    // Clean up
    document.body.removeChild(testElement);
    
    return availableFonts.join(',');
  } catch (error) {
    return 'font-detection-error';
  }
}

/**
 * Get audio fingerprint
 * @returns {Promise<string>} Audio fingerprint
 */
async function getAudioFingerprint() {
  try {
    // Check if AudioContext is available
    if (!window.AudioContext && !window.webkitAudioContext) {
      return 'audio-not-supported';
    }
    
    const AudioContext = window.AudioContext || window.webkitAudioContext;
    const audioContext = new AudioContext();
    
    // Create oscillator
    const oscillator = audioContext.createOscillator();
    const analyser = audioContext.createAnalyser();
    const gain = audioContext.createGain();
    
    // Connect nodes
    oscillator.connect(analyser);
    analyser.connect(gain);
    gain.connect(audioContext.destination);
    
    // Set parameters
    oscillator.type = 'triangle';
    oscillator.frequency.value = 440;
    gain.gain.value = 0; // Mute the sound
    
    // Start oscillator
    oscillator.start(0);
    
    // Get frequency data
    analyser.fftSize = 2048;
    const bufferLength = analyser.frequencyBinCount;
    const dataArray = new Uint8Array(bufferLength);
    analyser.getByteFrequencyData(dataArray);
    
    // Stop oscillator
    oscillator.stop(0);
    await audioContext.close();
    
    // Convert to string
    return Array.from(dataArray).slice(0, 10).join(',');
  } catch (error) {
    return 'audio-error';
  }
}

/**
 * Hash a string using SHA-256
 * @param {string} str - String to hash
 * @returns {Promise<string>} Hashed string
 */
async function hashString(str) {
  try {
    // Use SubtleCrypto if available
    if (window.crypto && window.crypto.subtle) {
      const encoder = new TextEncoder();
      const data = encoder.encode(str);
      const hashBuffer = await window.crypto.subtle.digest('SHA-256', data);
      const hashArray = Array.from(new Uint8Array(hashBuffer));
      return hashArray.map(b => b.toString(16).padStart(2, '0')).join('');
    }
    
    // Fallback to simple hash
    let hash = 0;
    for (let i = 0; i < str.length; i++) {
      const char = str.charCodeAt(i);
      hash = ((hash << 5) - hash) + char;
      hash = hash & hash; // Convert to 32bit integer
    }
    return hash.toString(16);
  } catch (error) {
    console.error('Error hashing string:', error);
    
    // Fallback to even simpler hash
    let hash = 0;
    for (let i = 0; i < str.length; i++) {
      hash += str.charCodeAt(i);
    }
    return hash.toString(16);
  }
}

/**
 * Check if a click is potentially fraudulent
 * @param {Object} clickData - Click data
 * @returns {Promise<Object>} Fraud check result
 */
async function checkFraud(clickData) {
  try {
    // Generate device fingerprint
    const deviceFingerprint = await generateDeviceFingerprint();
    
    // Prepare request data
    const requestData = {
      ...clickData,
      context: {
        ...clickData.context,
        deviceFingerprint,
        screen: {
          width: window.screen.width,
          height: window.screen.height,
          colorDepth: window.screen.colorDepth,
          pixelRatio: window.devicePixelRatio || 1
        }
      }
    };
    
    // Send request to fraud detection API
    const response = await fetch(`${API_BASE_URL}/api/fraud/check`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-API-Key': API_KEY,
        'X-App-Signature': APP_SIGNATURE
      },
      body: JSON.stringify(requestData)
    });
    
    // Parse response
    const result = await response.json();
    
    return result;
  } catch (error) {
    console.error('Error checking fraud:', error);
    
    // In case of error, allow the click
    return {
      success: true,
      allowed: true,
      error: error.message
    };
  }
}

/**
 * Handle ad click with fraud detection
 * @param {Object} adData - Ad data
 * @param {Function} onValidClick - Callback for valid clicks
 * @returns {Promise<void>}
 */
async function handleAdClick(adData, onValidClick) {
  try {
    // Show loading indicator
    showLoadingIndicator();
    
    // Prepare click data
    const clickData = {
      userId: getUserId(),
      deviceId: getDeviceId(),
      adId: adData.adId,
      context: {
        adType: adData.adType,
        placement: adData.placement,
        referrer: document.referrer,
        url: window.location.href
      }
    };
    
    // Check for fraud
    const fraudResult = await checkFraud(clickData);
    
    // Hide loading indicator
    hideLoadingIndicator();
    
    // Log result
    console.log('Fraud check result:', fraudResult);
    
    // Handle result
    if (fraudResult.success && fraudResult.allowed) {
      // Valid click, proceed with action
      onValidClick();
    } else {
      // Potentially fraudulent click, show warning
      showFraudWarning(fraudResult.reasons);
    }
  } catch (error) {
    console.error('Error handling ad click:', error);
    
    // Hide loading indicator
    hideLoadingIndicator();
    
    // In case of error, allow the click
    onValidClick();
  }
}

// Helper functions
function getUserId() {
  // Get user ID from local storage or generate a new one
  let userId = localStorage.getItem('userId');
  
  if (!userId) {
    userId = 'user_' + Math.random().toString(36).substring(2, 15);
    localStorage.setItem('userId', userId);
  }
  
  return userId;
}

function getDeviceId() {
  // Get device ID from local storage or generate a new one
  let deviceId = localStorage.getItem('deviceId');
  
  if (!deviceId) {
    deviceId = 'device_' + Math.random().toString(36).substring(2, 15);
    localStorage.setItem('deviceId', deviceId);
  }
  
  return deviceId;
}

function showLoadingIndicator() {
  // Implementation depends on your UI
  console.log('Loading...');
}

function hideLoadingIndicator() {
  // Implementation depends on your UI
  console.log('Loading complete');
}

function showFraudWarning(reasons) {
  // Implementation depends on your UI
  console.warn('Potential fraud detected:', reasons);
  alert('This action was blocked for security reasons. Please try again later.');
}

// Example usage
document.addEventListener('DOMContentLoaded', () => {
  // Set up click handlers for ads
  const adElements = document.querySelectorAll('.ad-container');
  
  adElements.forEach(adElement => {
    const adData = {
      adId: adElement.dataset.adId,
      adType: adElement.dataset.adType,
      placement: adElement.dataset.placement
    };
    
    adElement.addEventListener('click', async (event) => {
      // Prevent default action
      event.preventDefault();
      
      // Get click target URL
      const targetUrl = adElement.dataset.targetUrl || adElement.href;
      
      // Handle click with fraud detection
      await handleAdClick(adData, () => {
        // Valid click, navigate to target URL
        window.location.href = targetUrl;
      });
    });
  });
}); 