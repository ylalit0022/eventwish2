/**
 * Fingerprint Service
 * 
 * This service provides methods for generating device and IP fingerprints
 * to enhance fraud detection capabilities.
 */

const crypto = require('crypto');
const geoip = require('geoip-lite');
const UAParser = require('ua-parser-js');
const cacheService = require('./cacheService');

// Cache key prefixes
const FINGERPRINT_CACHE_PREFIX = 'fingerprint:';
const IP_INFO_CACHE_PREFIX = 'ip_info:';

/**
 * Generate a device fingerprint based on device information
 * @param {Object} deviceInfo - Device information
 * @param {string} deviceInfo.deviceId - Device ID
 * @param {string} deviceInfo.userAgent - User agent string
 * @param {string} deviceInfo.platform - Platform (e.g., "android", "ios")
 * @param {string} deviceInfo.deviceType - Device type (e.g., "mobile", "tablet")
 * @param {string} deviceInfo.screenSize - Screen size (e.g., "1080x1920")
 * @returns {Promise<string>} Device fingerprint
 */
async function generateDeviceFingerprint(deviceInfo) {
  try {
    // Extract device information
    const { deviceId, userAgent, platform, deviceType, screenSize } = deviceInfo;
    
    // Parse user agent
    const parser = new UAParser(userAgent);
    const browser = parser.getBrowser();
    const os = parser.getOS();
    const device = parser.getDevice();
    
    // Create fingerprint data
    const fingerprintData = {
      deviceId: deviceId || 'unknown',
      browser: browser.name || 'unknown',
      browserVersion: browser.version || 'unknown',
      os: os.name || 'unknown',
      osVersion: os.version || 'unknown',
      deviceVendor: device.vendor || 'unknown',
      deviceModel: device.model || 'unknown',
      deviceType: deviceType || device.type || 'unknown',
      platform: platform || 'unknown',
      screenSize: screenSize || 'unknown'
    };
    
    // Generate fingerprint hash
    const fingerprintString = JSON.stringify(fingerprintData);
    const fingerprint = crypto
      .createHash('sha256')
      .update(fingerprintString)
      .digest('hex');
    
    // Cache fingerprint data
    const cacheKey = `${FINGERPRINT_CACHE_PREFIX}device:${fingerprint}`;
    await cacheService.set(cacheKey, fingerprintString, 30 * 24 * 60 * 60); // 30 day TTL
    
    return fingerprint;
  } catch (error) {
    console.error('Error generating device fingerprint:', error);
    
    // Fallback to simple device ID hash if available
    if (deviceInfo.deviceId) {
      return crypto
        .createHash('sha256')
        .update(deviceInfo.deviceId)
        .digest('hex');
    }
    
    // Return a random fingerprint as last resort
    return crypto
      .randomBytes(32)
      .toString('hex');
  }
}

/**
 * Generate an IP fingerprint and get IP information
 * @param {string} ip - IP address
 * @returns {Promise<Object>} IP fingerprint and information
 */
async function generateIpFingerprint(ip) {
  try {
    // Check cache first
    const cacheKey = `${IP_INFO_CACHE_PREFIX}${ip}`;
    const cachedInfo = await cacheService.get(cacheKey);
    
    if (cachedInfo) {
      return JSON.parse(cachedInfo);
    }
    
    // Get IP information
    const geoData = geoip.lookup(ip) || {};
    
    // Create IP information
    const ipInfo = {
      ip,
      fingerprint: crypto
        .createHash('sha256')
        .update(ip)
        .digest('hex'),
      country: geoData.country || 'unknown',
      region: geoData.region || 'unknown',
      city: geoData.city || 'unknown',
      ll: geoData.ll || [0, 0], // Latitude and longitude
      timezone: geoData.timezone || 'unknown',
      proxy: isLikelyProxy(ip, geoData),
      vpn: isLikelyVPN(ip, geoData),
      datacenter: isLikelyDatacenter(ip, geoData)
    };
    
    // Cache IP information
    await cacheService.set(cacheKey, JSON.stringify(ipInfo), 7 * 24 * 60 * 60); // 7 day TTL
    
    return ipInfo;
  } catch (error) {
    console.error('Error generating IP fingerprint:', error);
    
    // Return basic IP fingerprint
    return {
      ip,
      fingerprint: crypto
        .createHash('sha256')
        .update(ip)
        .digest('hex'),
      country: 'unknown',
      region: 'unknown',
      city: 'unknown',
      ll: [0, 0],
      timezone: 'unknown',
      proxy: false,
      vpn: false,
      datacenter: false
    };
  }
}

/**
 * Check if an IP is likely a proxy
 * @param {string} ip - IP address
 * @param {Object} geoData - GeoIP data
 * @returns {boolean} Whether the IP is likely a proxy
 */
function isLikelyProxy(ip, geoData) {
  // This is a simplified check
  // In a production environment, you would use a more comprehensive database
  // or a third-party service to check for proxies
  
  // Check for common proxy ports in the IP
  if (ip.includes(':8080') || ip.includes(':3128') || ip.includes(':80')) {
    return true;
  }
  
  // Check for known proxy hosting providers
  if (geoData && geoData.org) {
    const org = geoData.org.toLowerCase();
    if (
      org.includes('proxy') ||
      org.includes('vpn') ||
      org.includes('hosting') ||
      org.includes('cloud')
    ) {
      return true;
    }
  }
  
  return false;
}

/**
 * Check if an IP is likely a VPN
 * @param {string} ip - IP address
 * @param {Object} geoData - GeoIP data
 * @returns {boolean} Whether the IP is likely a VPN
 */
function isLikelyVPN(ip, geoData) {
  // This is a simplified check
  // In a production environment, you would use a more comprehensive database
  // or a third-party service to check for VPNs
  
  // Check for known VPN providers
  if (geoData && geoData.org) {
    const org = geoData.org.toLowerCase();
    if (
      org.includes('vpn') ||
      org.includes('private') ||
      org.includes('tunnel') ||
      org.includes('nord') ||
      org.includes('express') ||
      org.includes('cyber')
    ) {
      return true;
    }
  }
  
  return false;
}

/**
 * Check if an IP is likely a datacenter
 * @param {string} ip - IP address
 * @param {Object} geoData - GeoIP data
 * @returns {boolean} Whether the IP is likely a datacenter
 */
function isLikelyDatacenter(ip, geoData) {
  // This is a simplified check
  // In a production environment, you would use a more comprehensive database
  // or a third-party service to check for datacenters
  
  // Check for known datacenter providers
  if (geoData && geoData.org) {
    const org = geoData.org.toLowerCase();
    if (
      org.includes('amazon') ||
      org.includes('aws') ||
      org.includes('google') ||
      org.includes('microsoft') ||
      org.includes('azure') ||
      org.includes('digitalocean') ||
      org.includes('linode') ||
      org.includes('vultr') ||
      org.includes('ovh') ||
      org.includes('hetzner') ||
      org.includes('cloud')
    ) {
      return true;
    }
  }
  
  return false;
}

/**
 * Enrich user context with fingerprinting information
 * @param {Object} context - User context
 * @returns {Promise<Object>} Enriched user context
 */
async function enrichContext(context) {
  try {
    // Generate device fingerprint
    const deviceInfo = {
      deviceId: context.deviceId,
      userAgent: context.userAgent,
      platform: context.platform,
      deviceType: context.deviceType,
      screenSize: context.screenSize
    };
    
    const deviceFingerprint = await generateDeviceFingerprint(deviceInfo);
    
    // Generate IP fingerprint
    const ipInfo = await generateIpFingerprint(context.ip);
    
    // Enrich context
    return {
      ...context,
      deviceFingerprint,
      ipFingerprint: ipInfo.fingerprint,
      ipInfo: {
        country: ipInfo.country,
        region: ipInfo.region,
        city: ipInfo.city,
        timezone: ipInfo.timezone,
        proxy: ipInfo.proxy,
        vpn: ipInfo.vpn,
        datacenter: ipInfo.datacenter
      }
    };
  } catch (error) {
    console.error('Error enriching context with fingerprinting:', error);
    return context;
  }
}

module.exports = {
  generateDeviceFingerprint,
  generateIpFingerprint,
  enrichContext
}; 