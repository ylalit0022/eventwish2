# AdMob Integration

This document provides an overview of the server-side AdMob integration for the EventWish application.

## Overview

The AdMob integration is designed to handle most of the ad-related logic on the server-side, minimizing the client-side processing. This approach provides several benefits:

- **Better control**: Centralized management of ad configurations
- **Improved security**: Secure ad-related endpoints with authentication
- **Enhanced performance**: Caching and optimization for better performance
- **Offline support**: Client-side caching for offline usage
- **Analytics**: Server-side tracking of impressions and clicks

## Components

### Server-Side Components

1. **AdMob Configuration Service** (`adMobService.js`)
   - Manages ad configurations
   - Provides caching for frequently accessed configurations
   - Handles ad selection based on user context

2. **Ad Serving Controller** (`adMobController.js`)
   - Provides endpoints for ad configuration, impression tracking, and click tracking
   - Handles validation and error handling

3. **Authentication Middleware** (`authMiddleware.js`)
   - Secures ad-related endpoints with token-based authentication
   - Validates API keys and app signatures

4. **Analytics Service** (`analyticsService.js`)
   - Tracks ad impressions, clicks, and revenue
   - Provides analytics data for ad performance
   - Calculates metrics like CTR (Click-Through Rate)

5. **Monitoring Service** (`monitoringService.js`)
   - Monitors system performance and ad operations
   - Tracks request metrics, response times, and cache hit rates
   - Provides real-time alerts for critical issues

6. **Routes**
   - Admin routes (`adMobRoutes.js`): For managing ad configurations
   - Client routes (`adMobClientRoutes.js`): For serving ads to clients
   - Analytics routes (`analyticsRoutes.js`): For accessing analytics data
   - Monitoring routes (`monitoringRoutes.js`): For accessing monitoring data

### Client-Side Components

1. **AdMob Client Library** (`admob-client.js`)
   - Lightweight client library for integrating with the server-side AdMob service
   - Handles ad requests, impression tracking, and click tracking
   - Provides offline support with local storage

2. **Example Implementation** (`admob-example.js`, `admob-example.css`, `admob-example.html`)
   - Example implementation of the AdMob client library
   - Demonstrates how to load and display ads

## API Endpoints

### Client Endpoints

- `GET /api/admob/config`: Get ad configuration based on client context
- `POST /api/admob/impression/:adId`: Track ad impression
- `POST /api/admob/click/:adId`: Track ad click
- `GET /api/admob/types`: Get ad types

### Admin Endpoints

- `GET /api/admob-ads`: Get all ads with filters and pagination
- `POST /api/admob-ads`: Create new ad
- `PUT /api/admob-ads/:id`: Update ad
- `DELETE /api/admob-ads/:id`: Delete ad
- `GET /api/admob-ads/types`: Get all ad types
- `PATCH /api/admob-ads/:id/status`: Toggle ad status

### Analytics Endpoints

- `GET /api/analytics/ad/:adId`: Get analytics data for a specific ad
- `GET /api/analytics/summary`: Get summary analytics for all ads
- `POST /api/analytics/revenue/:adId`: Track ad revenue
- `POST /api/analytics/cache/invalidate/:adId`: Invalidate analytics cache for a specific ad
- `POST /api/analytics/cache/invalidate-all`: Invalidate all analytics caches

### Monitoring Endpoints

- `GET /api/monitoring/metrics`: Get current metrics
- `GET /api/monitoring/ad-metrics`: Get ad-specific metrics
- `POST /api/monitoring/reset`: Reset metrics

## Authentication

All admin endpoints require token-based authentication. The token should be provided in the `x-auth-token` header.

All client endpoints require app signature verification. The app signature should be provided in the `x-app-signature` header.

## Caching

The AdMob service uses a multi-level caching approach:

1. **Server-Side Caching**: The server caches ad configurations in memory using `node-cache`.
2. **Client-Side Caching**: The client library caches ad configurations in memory and localStorage.

## Analytics

The AdMob integration includes comprehensive analytics:

1. **Impression Tracking**: Track ad impressions with context information
2. **Click Tracking**: Track ad clicks with context information
3. **Revenue Tracking**: Track ad revenue with currency information
4. **Performance Metrics**: Calculate metrics like CTR (Click-Through Rate)
5. **Breakdown by Type**: View analytics broken down by ad type
6. **Breakdown by Device**: View analytics broken down by device type
7. **Breakdown by Platform**: View analytics broken down by platform

## Monitoring

The AdMob integration includes real-time monitoring:

1. **Request Metrics**: Track request counts, success rates, and error rates
2. **Response Times**: Track response times by endpoint
3. **Cache Metrics**: Track cache hit rates
4. **System Metrics**: Track memory usage, CPU usage, and disk usage
5. **Alerts**: Generate alerts when metrics exceed thresholds
6. **Ad-Specific Metrics**: Track ad-specific metrics like impressions, clicks, and revenue

## Error Handling

The AdMob integration includes comprehensive error handling:

1. **Server-Side Error Handling**: All server-side errors are logged using Winston and returned to the client with appropriate status codes.
2. **Client-Side Error Handling**: The client library includes error handling and retry logic for failed requests.

## Logging

The AdMob integration uses Winston for logging. Logs are stored in the `logs` directory.

## Environment Variables

The following environment variables are required for the AdMob integration:

- `JWT_SECRET`: Secret key for JWT token generation and verification
- `API_KEY`: API key for secure API access
- `VALID_APP_SIGNATURES`: Comma-separated list of valid app signatures
- `LOG_LEVEL`: Logging level (error, warn, info, http, verbose, debug, silly)

## Installation

1. Install dependencies:
   ```
   npm install
   ```

2. Set environment variables in `.env` file.

3. Start the server:
   ```
   npm start
   ```

## Client Integration

To integrate the AdMob client library into your application:

1. Include the AdMob client library:
   ```html
   <script src="/js/admob-client.js"></script>
   ```

2. Initialize the AdMob client:
   ```javascript
   const adMobClient = new AdMobClient({
     appSignature: 'YOUR_APP_SIGNATURE',
     baseUrl: '/api/admob',
     retryAttempts: 3,
     retryDelay: 1000,
     cacheExpiration: 300000,
     offlineSupport: true
   });
   ```

3. Load and display ads:
   ```javascript
   // Get ad configuration
   const adConfig = await adMobClient.getAdConfig('Banner', context);
   
   // Track impression
   adMobClient.trackImpression(adConfig.id, context);
   
   // Track click
   adMobClient.trackClick(adConfig.id, context);
   ```

See the example implementation for more details.

## Testing

### Testing Strategy

The AdMob integration includes comprehensive testing to ensure reliability and performance:

1. **Unit Tests**: Test individual components in isolation
   - AdMob model validation and methods
   - Service layer business logic
   - Controller request/response handling
   - Middleware authentication and validation

2. **Integration Tests**: Test interactions between components
   - Authentication middleware with routes
   - Analytics service with database operations
   - Caching service with monitoring
   - End-to-end request flows

3. **Performance Tests**: Test system under load
   - Cache hit/miss rates
   - Response times under concurrent requests
   - Rate limiting effectiveness

### Running Tests

To run the tests, you can use the following commands:

```bash
# Run all tests
npm test

# Run tests in watch mode during development
npm run test:watch

# Run tests with coverage report
npm run test:coverage

# Run the test script (includes coverage and opens report)
./run-tests.sh
```

### Test Coverage

The test suite aims for at least 80% coverage across:
- Branches
- Functions
- Lines
- Statements

The coverage report is generated in the `coverage` directory after running `npm run test:coverage` or `./run-tests.sh`.

### Testing Tools

- **Jest**: Test runner and assertion library
- **MongoDB Memory Server**: In-memory MongoDB for database tests
- **Supertest**: HTTP assertions for API testing
- **Mock implementations**: For isolating components during testing

### Continuous Integration

The tests are designed to run in CI environments. Set the following environment variables:

```
NODE_ENV=test
JWT_SECRET=test-secret
MONGODB_URI=mongodb://localhost:27017/eventwish-test
```

## Troubleshooting

### Common Issues

#### Authentication Issues

- **Invalid API Key**: Ensure the API key is correctly set in the client request headers
- **Token Expiration**: JWT tokens expire after the configured time period; request a new token
- **Rate Limiting**: Too many requests from the same IP will be blocked; implement exponential backoff
- **App Signature Mismatch**: Verify that the app signature in the request matches the one configured on the server
- **Missing Headers**: Ensure all required authentication headers are included in the request

#### Caching Issues

- **Stale Data**: If you're seeing outdated ad configurations, try clearing the client cache
- **Cache Misses**: High cache miss rates may indicate configuration issues; check cache TTL settings
- **Cache Invalidation Failures**: If updates aren't reflected, check that cache invalidation is working properly
- **Memory Cache Overflow**: If the server is running out of memory, adjust the cache size limits
- **Redis Connection Issues**: Verify Redis connection settings if using Redis for caching

#### Integration Issues

- **CORS Errors**: Ensure the client domain is added to the allowed origins in the server configuration
- **Network Errors**: Implement retry logic with exponential backoff for transient network issues
- **Offline Support**: Use the provided offline fallback mechanisms when network is unavailable
- **Content Security Policy**: If ads aren't displaying, check for CSP restrictions in your client application
- **Ad Blockers**: Be aware that client-side ad blockers may prevent ad loading; consider implementing detection

#### A/B Testing Issues

- **Test Not Running**: Verify that the test status is set to 'active' and the current date is within the test period
- **Variant Selection Issues**: Check that variant weights sum to 100% and targeting rules are correctly configured
- **No Test Results**: Ensure that impression and click tracking is properly implemented for accurate results
- **Statistical Significance**: Small sample sizes may not provide statistically significant results; extend test duration
- **Overlapping Tests**: Multiple active tests for the same ad type can cause conflicts; prioritize tests appropriately

#### Targeting Issues

- **Segment Not Matching**: Verify that targeting criteria are correctly configured and user context contains expected values
- **Missing User Context**: Ensure all relevant user context data is being sent from the client
- **Targeting Rule Syntax**: Check for syntax errors in targeting rule conditions
- **Empty Segments**: If segments contain no users, review targeting criteria for overly restrictive conditions
- **Performance Issues**: Complex targeting rules may impact performance; optimize or simplify rules if necessary

### Debugging

- Enable debug logging by setting `DEBUG=true` in the environment variables
- Check server logs for detailed error information
- Use the monitoring endpoints to check system health and metrics
- Enable verbose client-side logging with `localStorage.setItem('ADMOB_DEBUG', 'true')`
- Use the `/api/admob/debug` endpoint to test ad configurations without affecting production metrics

### Monitoring and Alerts

- Set up alerts for high error rates using the monitoring service
- Monitor cache hit rates to ensure optimal performance
- Track API response times to identify performance bottlenecks
- Set up alerts for authentication failures to detect potential security issues
- Monitor impression and click rates for unexpected changes that might indicate problems

### Getting Help

If you encounter issues not covered in this documentation:

1. Check the server logs for detailed error messages
2. Review the test cases for expected behavior
3. Check the GitHub repository for known issues and solutions
4. Run the diagnostic script: `node tools/admob-diagnostic.js`
5. Contact the EventWish development team for support

## Future Improvements

The following enhancements are planned for future releases:

- Configure load balancing for ad-related endpoints
- Implement health checks for ad services
- Add auto-scaling policies
- Enhance documentation with troubleshooting section
- Create video tutorials for complex integrations
- Implement interactive API explorer with Swagger UI
- Add end-to-end browser testing with Puppeteer or Cypress
- Implement performance benchmarking
- Enhance test coverage to 90%+

## A/B Testing

The AdMob integration includes A/B testing capabilities to optimize ad performance. This allows you to test different ad configurations and measure their effectiveness.

### Features

- **Test Creation**: Create A/B tests with multiple variants
- **Targeting Rules**: Target specific users based on device, platform, country, etc.
- **Traffic Allocation**: Control the percentage of users included in the test
- **Variant Weighting**: Assign different weights to variants
- **Results Analysis**: Calculate statistical significance of results
- **Metrics Tracking**: Track impressions, clicks, and custom metrics

### Components

#### ABTest Model (`models/ABTest.js`)

The ABTest model defines the structure of an A/B test, including:

- Test metadata (name, description, status)
- Variants with different ad configurations
- Targeting rules for user segmentation
- Traffic allocation percentage
- Metrics to track
- Results data

#### ABTest Service (`services/abTestService.js`)

The ABTest service provides methods for:

- Creating, updating, and deleting tests
- Getting tests by ID or filters
- Evaluating targeting rules
- Selecting variants for users
- Getting optimal ad configurations
- Tracking test events
- Calculating test results

#### ABTest Controller (`controllers/abTestController.js`)

The ABTest controller handles HTTP requests for:

- Creating and managing tests (admin routes)
- Getting optimal ad configurations (client routes)
- Tracking test events (client routes)
- Getting test results (admin routes)

### API Endpoints

#### Admin Routes

- `POST /api/abtest`: Create a new A/B test
- `PUT /api/abtest/:id`: Update an existing A/B test
- `DELETE /api/abtest/:id`: Delete an A/B test
- `POST /api/abtest/:id/start`: Start an A/B test
- `POST /api/abtest/:id/stop`: Stop an A/B test
- `GET /api/abtest`: Get all A/B tests (with optional filters)
- `GET /api/abtest/:id`: Get an A/B test by ID
- `GET /api/abtest/:id/results`: Get test results

#### Client Routes

- `GET /api/abtest/ad/:adType`: Get optimal ad configuration for a user
- `POST /api/abtest/track/:testId/:variantId/:eventType`: Track a test event

### Creating an A/B Test

To create an A/B test, send a POST request to `/api/abtest` with the following data:

```json
{
  "name": "Banner Ad Test",
  "description": "Testing different banner ad configurations",
  "adType": "banner",
  "status": "draft",
  "variants": [
    {
      "name": "Control",
      "description": "Current banner ad",
      "weight": 50,
      "adConfig": {
        "adId": "existing-ad-id",
        "parameters": {
          "backgroundColor": "#FFFFFF",
          "textColor": "#000000"
        }
      }
    },
    {
      "name": "Variant B",
      "description": "New banner ad with blue background",
      "weight": 50,
      "adConfig": {
        "adId": "existing-ad-id",
        "parameters": {
          "backgroundColor": "#0000FF",
          "textColor": "#FFFFFF"
        }
      }
    }
  ],
  "targetingRules": [
    {
      "type": "platform",
      "operator": "equals",
      "value": "android"
    },
    {
      "type": "country",
      "operator": "equals",
      "value": "US"
    }
  ],
  "metrics": ["impressions", "clicks"],
  "trafficAllocation": 100
}
```

### Getting Optimal Ad Configuration

To get the optimal ad configuration for a user, send a GET request to `/api/abtest/ad/:adType` with the following headers or query parameters:

- `x-user-id` or `userId`: User ID
- `x-device-id` or `deviceId`: Device ID
- `x-device-type` or `deviceType`: Device type (e.g., "mobile", "tablet")
- `x-platform` or `platform`: Platform (e.g., "android", "ios")
- `x-country` or `country`: Country code
- `x-language` or `language`: Language code
- `x-app-version` or `appVersion`: App version

The response will include the optimal ad configuration for the user, along with test metadata if the user is included in an A/B test:

```json
{
  "success": true,
  "data": {
    "adId": "ad-id",
    "adName": "Ad Name",
    "adType": "banner",
    "adUnitCode": "ad-unit-code",
    "parameters": {
      "backgroundColor": "#0000FF",
      "textColor": "#FFFFFF"
    },
    "isTest": true,
    "testId": "test-id",
    "testName": "Banner Ad Test",
    "variantId": "variant-id",
    "variantName": "Variant B"
  }
}
```

### Tracking Test Events

To track a test event, send a POST request to `/api/abtest/track/:testId/:variantId/:eventType` with the following headers or query parameters:

- `x-user-id` or `userId`: User ID
- `x-device-id` or `deviceId`: Device ID

The response will indicate whether the event was tracked successfully:

```json
{
  "success": true,
  "message": "Event tracked successfully"
}
```

### Viewing Test Results

To view test results, send a GET request to `/api/abtest/:id/results`. The response will include metrics for each variant, along with statistical significance calculations:

```json
{
  "success": true,
  "data": {
    "testId": "test-id",
    "testName": "Banner Ad Test",
    "variants": [
      {
        "variantId": "variant-1-id",
        "variantName": "Control",
        "metrics": {
          "impressions": 1000,
          "clicks": 20,
          "ctr": 2
        }
      },
      {
        "variantId": "variant-2-id",
        "variantName": "Variant B",
        "metrics": {
          "impressions": 1000,
          "clicks": 30,
          "ctr": 3,
          "improvement": 50,
          "significant": true,
          "confidence": 95.4
        }
      }
    ]
  }
}
```

### Client Integration

To integrate A/B testing into your client application:

1. **Get Optimal Ad Configuration**: When requesting an ad, use the `/api/abtest/ad/:adType` endpoint instead of the regular ad endpoint.

2. **Track Events**: When an ad is displayed or clicked, track the event using the `/api/abtest/track/:testId/:variantId/:eventType` endpoint if the ad is part of a test (check the `isTest` flag in the ad configuration).

3. **Handle Test Parameters**: Apply any test-specific parameters from the ad configuration to customize the ad display.

## User Targeting and Segmentation

The AdMob integration includes advanced user targeting and segmentation capabilities to deliver the most relevant ads to users based on their demographics, behavior, and context.

### Features

- **User Segmentation**: Create and manage user segments based on various criteria
- **Demographic Targeting**: Target users based on country, language, device type, etc.
- **Behavioral Targeting**: Target users based on their past behavior and interactions
- **Contextual Targeting**: Target users based on current context (time of day, day of week, etc.)
- **Custom Targeting**: Create custom targeting rules for specific use cases
- **Segment Performance**: Track ad performance by segment to optimize targeting

### Components

#### UserSegment Model (`models/UserSegment.js`)

The UserSegment model defines the structure of a user segment, including:

- Segment metadata (name, description, type)
- Targeting criteria for the segment
- Active status
- Estimated size of the segment
- Tags for categorization

#### Targeting Service (`services/targetingService.js`)

The Targeting service provides methods for:

- Evaluating targeting criteria against user context
- Getting segments that a user belongs to
- Checking if a user belongs to a specific segment
- Getting the best ad for a user based on targeting
- Enriching user context with additional data

#### Segment Controller (`controllers/segmentController.js`)

The Segment controller handles HTTP requests for:

- Creating and managing segments (admin routes)
- Testing if a user context matches a segment (admin routes)

### API Endpoints

#### Admin Routes

- `POST /api/segments`: Create a new user segment
- `PUT /api/segments/:id`: Update an existing user segment
- `DELETE /api/segments/:id`: Delete a user segment
- `GET /api/segments`: Get all user segments (with optional filters)
- `GET /api/segments/:id`: Get a user segment by ID
- `POST /api/segments/:id/test`: Test if a user context matches a segment

### Creating a User Segment

To create a user segment, send a POST request to `/api/segments` with the following data:

```json
{
  "name": "US Android Users",
  "description": "Users from the United States using Android devices",
  "type": "demographic",
  "criteria": {
    "country": "US",
    "platform": "android"
  },
  "tags": ["country", "platform"]
}
```

### Targeting Criteria

Targeting criteria can be specified in two ways:

1. **Simple Equality**: `"field": "value"` - The field must equal the value
2. **Advanced Operators**: `"field": { "operator": "equals", "value": "value" }` - More complex conditions

Available operators:

- `equals`: Field equals value
- `notEquals`: Field does not equal value
- `contains`: Field contains value
- `notContains`: Field does not contain value
- `startsWith`: Field starts with value
- `endsWith`: Field ends with value
- `regex`: Field matches regex pattern
- `greaterThan`: Field is greater than value
- `lessThan`: Field is less than value
- `greaterThanOrEqual`: Field is greater than or equal to value
- `lessThanOrEqual`: Field is less than or equal to value
- `in`: Field is in array of values
- `notIn`: Field is not in array of values

Example of advanced targeting criteria:

```json
{
  "criteria": {
    "country": { "operator": "in", "value": ["US", "CA", "UK"] },
    "appVersion": { "operator": "greaterThanOrEqual", "value": "2.0.0" },
    "deviceType": { "operator": "equals", "value": "mobile" }
  }
}
```

### Testing a Segment

To test if a user context matches a segment, send a POST request to `/api/segments/:id/test` with the user context in the request body:

```json
{
  "country": "US",
  "platform": "android",
  "deviceType": "mobile",
  "appVersion": "2.1.0"
}
```

The response will indicate whether the user context matches the segment:

```json
{
  "success": true,
  "data": {
    "segmentId": "segment-id",
    "segmentName": "US Android Users",
    "matches": true,
    "userContext": {
      "country": "US",
      "platform": "android",
      "deviceType": "mobile",
      "appVersion": "2.1.0"
    }
  }
}
```

### Ad Targeting

When creating or updating an ad, you can specify targeting criteria and target segments:

```json
{
  "adName": "Premium Android Banner",
  "adUnitCode": "ca-app-pub-1234567890123456/1234567890",
  "adType": "Banner",
  "targetingCriteria": {
    "country": { "operator": "in", "value": ["US", "CA"] },
    "platform": "android",
    "appVersion": { "operator": "greaterThanOrEqual", "value": "2.0.0" }
  },
  "targetSegments": ["segment-id-1", "segment-id-2"],
  "targetingPriority": 5,
  "parameters": {
    "backgroundColor": "#FFFFFF",
    "textColor": "#000000"
  }
}
```

### Client Integration

When requesting an ad, include as much user context as possible to enable accurate targeting:

```javascript
// Get user context
const userContext = {
  userId: 'user-123',
  deviceId: 'device-456',
  deviceType: 'mobile',
  platform: 'android',
  country: 'US',
  language: 'en',
  appVersion: '2.1.0',
  screenSize: '1080x1920',
  connectionType: 'wifi',
  timeZone: 'America/New_York',
  recentActivity: ['view_ad', 'click_ad', 'purchase']
};

// Build query string from user context
const queryParams = Object.entries(userContext)
  .map(([key, value]) => `${key}=${encodeURIComponent(value)}`)
  .join('&');

// Request ad configuration
const response = await fetch(`/api/admob/config?adType=Banner&${queryParams}`, {
  headers: {
    'x-api-key': 'your-api-key',
    'x-app-signature': 'your-app-signature',
    ...Object.entries(userContext).reduce((acc, [key, value]) => {
      acc[`x-${key}`] = value;
      return acc;
    }, {})
  }
});

// Parse response
const data = await response.json();

// Use the ad configuration
if (data.success) {
  const adConfig = data.data;
  // Display the ad using the configuration
}
```

The server will use the provided user context to select the most relevant ad based on targeting criteria and segment membership.

## Fraud Detection

The AdMob integration includes a comprehensive fraud detection system to prevent click fraud and ensure the integrity of ad interactions. The system uses multiple detection methods to identify potentially fraudulent clicks.

### Features

- **Click Frequency Analysis**: Detects abnormal click patterns from users, devices, and IP addresses.
- **Click Pattern Detection**: Identifies suspicious click patterns that may indicate automated clicking.
- **Device and IP Reputation**: Tracks reputation scores for devices and IPs based on past behavior.
- **Click-Through Rate Analysis**: Detects abnormally high CTRs that may indicate click fraud.
- **Fraud Scoring**: Calculates a fraud score (0-100) based on multiple factors.
- **Configurable Thresholds**: Allows adjustment of detection thresholds to balance security and user experience.

### Components

- **Fraud Detection Service** (`services/fraudDetectionService.js`): Methods for analyzing click frequency, tracking device reputation, calculating fraud scores, and processing click events.
- **Fraud Controller** (`controllers/fraudController.js`): Handles HTTP requests related to fraud detection, such as checking for fraudulent clicks and retrieving statistics.

### API Endpoints

- `POST /api/fraud/check`: Check if a click is potentially fraudulent.
- `GET /api/fraud/statistics`: Get fraud detection statistics (admin only).
- `GET /api/fraud/thresholds`: Get fraud detection thresholds (admin only).
- `PUT /api/fraud/thresholds`: Update fraud detection thresholds (admin only).

#### Integration with Ad Click Tracking

The fraud detection system is integrated with the ad click tracking system. When a click is tracked, it is automatically analyzed for potential fraud. If a click is determined to be fraudulent, it is not counted as a valid click and is not forwarded to the ad network.

#### Fraud Detection Thresholds

The following thresholds can be configured to adjust the sensitivity of the fraud detection system:

- `MAX_CLICKS_PER_USER_HOUR`: Maximum number of clicks allowed per user per hour (default: 10).
- `MAX_CLICKS_PER_IP_HOUR`: Maximum number of clicks allowed per IP address per hour (default: 20).
- `MAX_CLICKS_PER_DEVICE_HOUR`: Maximum number of clicks allowed per device per hour (default: 15).
- `MIN_CLICK_INTERVAL`: Minimum time between clicks in milliseconds (default: 500).
- `MAX_CLICKS_PER_AD_USER_DAY`: Maximum number of clicks allowed per ad per user per day (default: 5).
- `SUSPICIOUS_CTR_THRESHOLD`: CTR threshold for suspicious activity (default: 20%).

These thresholds can be adjusted via the admin API to balance security and user experience.

#### Statistics

The fraud detection system tracks statistics on total clicks, fraudulent clicks, and fraud rate. These statistics can be accessed via the admin API.

### IP and Device Fingerprinting

The AdMob integration includes advanced IP and device fingerprinting capabilities to enhance fraud detection and improve ad targeting. Fingerprinting provides a more reliable way to identify devices and detect suspicious behavior.

#### Features

- **Device Fingerprinting**: Creates a unique identifier for devices based on multiple factors.
- **IP Intelligence**: Analyzes IP addresses to detect proxies, VPNs, and datacenters.
- **Browser Fingerprinting**: Identifies browsers based on their unique characteristics.
- **Cross-Device Tracking**: Links multiple devices to the same user for better targeting.
- **Fraud Prevention**: Enhances fraud detection by identifying devices with suspicious behavior.

#### Components

- **Fingerprint Service** (`services/fingerprintService.js`): Methods for generating device and IP fingerprints, enriching context data, and analyzing fingerprint information.

#### Device Fingerprint Factors

Device fingerprints are generated based on multiple factors, including:

- User agent string
- Screen resolution and color depth
- Available fonts
- Browser plugins
- Canvas fingerprinting
- WebGL fingerprinting
- Audio fingerprinting
- Hardware concurrency
- Device memory
- Timezone information
- Language settings

#### IP Intelligence

IP intelligence provides information about IP addresses, including:

- Geographic location (country, region, city)
- ISP and organization
- Connection type (residential, mobile, datacenter)
- Proxy detection
- VPN detection
- Tor exit node detection

#### Integration with Fraud Detection

The fingerprinting system is integrated with the fraud detection system. When a click is analyzed for potential fraud, the device and IP fingerprints are used to enhance detection accuracy. Devices and IPs with suspicious fingerprints are assigned higher fraud scores.

#### Client-Side Integration

The client-side library includes methods for generating device fingerprints and sending them with ad requests. The fingerprinting is done in a privacy-respecting way, focusing on fraud prevention rather than user tracking.

#### Privacy Considerations

The fingerprinting system is designed with privacy in mind:

- No personally identifiable information (PII) is collected.
- Fingerprints are hashed to prevent reverse engineering.
- Data is stored securely and used only for fraud prevention and ad targeting.
- Users can opt out of fingerprinting via the privacy settings.
- Compliance with GDPR, CCPA, and other privacy regulations.

### Suspicious Activity Monitoring

The AdMob integration includes a comprehensive suspicious activity monitoring system to detect, track, and alert on potential security threats and fraudulent behavior related to ad interactions.

#### Features

- **Real-time Monitoring**: Tracks suspicious activities as they occur.
- **Activity Classification**: Categorizes activities by type and severity.
- **Reputation Scoring**: Maintains reputation scores for users, devices, and IPs.
- **Pattern Analysis**: Detects abnormal patterns in user behavior.
- **Alerting System**: Triggers alerts for high-severity activities.
- **Dashboard Visualization**: Provides a visual dashboard for monitoring activities.

#### Components

- **Suspicious Activity Service** (`services/suspiciousActivityService.js`): Methods for tracking activities, updating reputation scores, and analyzing traffic patterns.
- **Suspicious Activity Controller** (`controllers/suspiciousActivityController.js`): Handles HTTP requests related to suspicious activity monitoring.

#### Activity Types

The system tracks various types of suspicious activities, including:

- Click fraud
- Impression fraud
- Abnormal traffic patterns
- Proxy usage
- VPN usage
- Datacenter usage
- Suspicious devices
- Suspicious IPs
- Suspicious users
- Suspicious patterns

#### Severity Levels

Activities are classified by severity level:

- **Low**: Minor anomalies that may not indicate fraud.
- **Medium**: Suspicious behavior that warrants monitoring.
- **High**: Likely fraudulent activity requiring attention.
- **Critical**: Confirmed fraud or serious security threats.

#### API Endpoints

- `GET /api/suspicious-activity/dashboard`: Get dashboard data for suspicious activities.
- `GET /api/suspicious-activity/:entityType/:entityId`: Get activities for a specific entity.
- `GET /api/suspicious-activity/:entityType/:entityId/reputation`: Get reputation score for a specific entity.
- `GET /api/suspicious-activity/:entityType/:entityId/analyze`: Analyze traffic patterns for a specific entity.
- `GET /api/suspicious-activity/:entityType/:entityId/check`: Check if an entity is suspicious.

#### Integration with Fraud Detection

The suspicious activity monitoring system is integrated with the fraud detection system. When fraudulent activity is detected, it is automatically tracked in the suspicious activity system, and reputation scores are updated accordingly.

#### Dashboard

The system includes a comprehensive dashboard for monitoring suspicious activities, including:

- Activity statistics by type and severity
- Recent activities list
- Top suspicious users and IPs
- Activity charts and visualizations
- Entity reputation scores

#### Alerting

The system can trigger alerts for high-severity activities, which can be configured to send notifications via various channels (email, SMS, etc.). This helps administrators respond quickly to potential security threats.

## Load Balancing and Scaling

The AdMob integration includes load balancing and auto-scaling capabilities to ensure high availability and optimal performance under varying loads.

### Features

- **Worker Process Management**: Manages multiple worker processes to utilize all available CPU cores.
- **Auto-Scaling**: Dynamically adjusts the number of worker processes based on CPU and memory usage.
- **Load Distribution**: Distributes incoming requests across worker processes for optimal performance.
- **Graceful Shutdown**: Handles process termination gracefully to prevent request interruption.
- **Resource Monitoring**: Monitors CPU and memory usage to inform scaling decisions.

### Components

- **Load Balancer Configuration** (`config/loadBalancer.js`): Configuration for load balancing and auto-scaling.
- **Cluster Management**: Uses Node.js cluster module to manage worker processes.

### Configuration

The load balancer can be configured using environment variables:

- `LOAD_BALANCER_ENABLED`: Enable or disable load balancing (default: false).
- `WORKER_COUNT`: Number of worker processes to spawn (default: number of CPU cores).
- `AUTO_SCALING_ENABLED`: Enable or disable auto-scaling (default: false).
- `MIN_WORKERS`: Minimum number of worker processes (default: 2).
- `MAX_WORKERS`: Maximum number of worker processes (default: 2 * number of CPU cores).
- `CPU_THRESHOLD`: CPU usage threshold for scaling up (default: 70%).
- `MEMORY_THRESHOLD`: Memory usage threshold for scaling up (default: 80%).
- `SCALE_UP_COOLDOWN`: Cooldown period after scaling up (default: 60000ms).
- `SCALE_DOWN_COOLDOWN`: Cooldown period after scaling down (default: 300000ms).

### Endpoint-Specific Configuration

Each endpoint can have specific load balancing configuration:

- **Priority**: Determines the priority of the endpoint (highest, high, medium, low).
- **Rate Limiting**: Configures rate limiting for the endpoint.
- **Timeout**: Sets the timeout for the endpoint.

### Running with Load Balancing

To run the server with load balancing enabled:

```bash
npm run cluster
```

Or set the environment variables manually:

```bash
LOAD_BALANCER_ENABLED=true node server.js
```

## Health Checks

The AdMob integration includes comprehensive health checks to monitor the health of the application and its dependencies.

### Features

- **System Health Monitoring**: Monitors CPU, memory, and disk usage.
- **Database Health Checks**: Verifies database connectivity and performance.
- **Redis Health Checks**: Checks Redis connectivity and performance.
- **API Endpoint Health Checks**: Verifies that API endpoints are responding correctly.
- **AdMob Service Health Checks**: Monitors the health of AdMob-related services.
- **Kubernetes Integration**: Provides liveness and readiness probes for Kubernetes.

### Components

- **Health Check Service** (`services/healthCheckService.js`): Methods for checking the health of various components.
- **Health Check Routes** (`routes/healthRoutes.js`): API endpoints for health checks.

### API Endpoints

- `GET /api/health`: Get overall health status of the application.
- `GET /api/health/database`: Get database health status.
- `GET /api/health/redis`: Get Redis health status.
- `GET /api/health/api`: Get API endpoints health status.
- `GET /api/health/system`: Get system health status (disk, memory, CPU).
- `GET /api/health/admob`: Get AdMob services health status.
- `GET /api/health/liveness`: Simple liveness probe for Kubernetes.
- `GET /api/health/readiness`: Readiness probe for Kubernetes.

### Health Status

Health checks return one of the following statuses:

- **Up**: The component is healthy and functioning normally.
- **Degraded**: The component is functioning but with reduced performance or reliability.
- **Down**: The component is not functioning and requires attention.

### Monitoring Integration

Health check results are integrated with the monitoring service to track health metrics over time and trigger alerts when components become unhealthy.

## API Documentation

The AdMob integration provides interactive API documentation using Swagger UI.

### Features

- **Interactive API Explorer**: Test API endpoints directly from the documentation
- **Request/Response Examples**: View example requests and responses for each endpoint
- **Authentication Documentation**: Learn how to authenticate with the API
- **Schema Documentation**: View detailed schema information for all models

### Access

The API documentation is available at `/api-docs` when the server is running. By default, it is enabled in development mode and can be enabled in production by setting the `ENABLE_SWAGGER` environment variable to `true`.

```
http://localhost:3000/api-docs
```

## Testing and Performance

The AdMob integration includes comprehensive end-to-end testing and performance benchmarking capabilities.

### End-to-End Testing

End-to-end tests use Puppeteer to simulate real user interactions with the AdMob integration in a browser environment.

#### Features

- **Browser-Based Testing**: Tests run in a headless Chrome browser
- **In-Memory Database**: Tests use an in-memory MongoDB instance for isolation
- **Screenshot Capture**: Tests capture screenshots at key points for visual verification
- **Client Simulation**: Tests simulate client-side interactions with the AdMob integration
- **Fraud Detection Testing**: Tests verify the effectiveness of fraud detection mechanisms

#### Running End-to-End Tests

```bash
# Run all end-to-end tests
npm run test:e2e

# Run with visible browser (non-headless)
HEADLESS=false npm run test:e2e

# Run with slower execution for debugging
SLOW_MO=100 npm run test:e2e
```

#### Test Files

- `tests/e2e/setup.js`: Setup and teardown functions for end-to-end tests
- `tests/e2e/admob-client.test.js`: Tests for client-side AdMob integration
- `tests/e2e/fraud-detection.test.js`: Tests for fraud detection system

### Performance Benchmarking

Performance benchmarking measures the performance of the AdMob integration under various load conditions.

#### Features

- **Multi-Process Benchmarking**: Uses Node.js cluster module for parallel processing
- **Configurable Load Scenarios**: Supports different load scenarios (small, medium, large)
- **Comprehensive Metrics**: Measures response times, throughput, and resource usage
- **Detailed Reports**: Generates detailed performance reports with percentiles
- **Endpoint-Specific Metrics**: Provides metrics for each API endpoint

#### Running Performance Benchmarks

```bash
# Run default benchmark (50 concurrent users, 60 seconds)
npm run benchmark

# Run small benchmark (10 concurrent users, 30 seconds)
npm run benchmark:small

# Run medium benchmark (50 concurrent users, 60 seconds)
npm run benchmark:medium

# Run large benchmark (100 concurrent users, 120 seconds)
npm run benchmark:large

# Run custom benchmark
CONCURRENT_USERS=25 TEST_DURATION=45 npm run benchmark
```

#### Benchmark Configuration

The benchmark can be configured using environment variables:

- `CONCURRENT_USERS`: Number of concurrent users (default: 50)
- `TEST_DURATION`: Test duration in seconds (default: 60)
- `RAMP_UP_TIME`: Ramp-up time in seconds (default: 10)
- `WORKERS`: Number of worker processes (default: CPU cores - 1)
- `OUTPUT_DIR`: Directory for benchmark reports (default: tests/performance/results)

#### Benchmark Reports

Benchmark reports are saved as JSON files in the output directory and include:

- **Summary Metrics**: Requests per second, response times, success rate
- **Endpoint Metrics**: Performance metrics for each API endpoint
- **Status Codes**: Distribution of HTTP status codes
- **Errors**: List of errors encountered during the benchmark

## Future Improvements

The following enhancements are planned for future releases:

- Add end-to-end browser testing with Puppeteer or Cypress
- Implement performance benchmarking
- Enhance test coverage to 90%+
- Create video tutorials for complex integrations 

## Setup before all tests
beforeAll(async () => {
  await setup();
  
  // Create test client HTML file
  createTestClient();
  
  // Create test ad configuration in database
  const adMob = new AdMob(testAdConfig);
  await adMob.save();
}, 60000); 

// Test client HTML file path
const testClientPath = path.join(__dirname, 'fraud-test-client.html');

// Access the test client in tests
await page.goto(`file://${testClientPath}`); 

// Example of API call in the test client
const response = await fetch(`${API_BASE_URL}/api/admob/config`, {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'x-api-key': API_KEY
  },
  body: JSON.stringify({
    format: 'BANNER',
    context: userContext
  })
}); 

// Simulate rapid clicks
await page.click('#rapidClicks');

// Wait for rapid clicks simulation to complete
await page.waitForFunction(
  () => {
    const logContent = document.getElementById('log').textContent;
    return logContent.includes('Rapid clicks simulation completed');
  },
  { timeout: 15000 }
);

// Check if any clicks were flagged as suspicious
const logContent = await page.$eval('#log', el => el.textContent);
expect(logContent).toContain('flagged as suspicious');

// Check database for suspicious activities
const suspiciousActivities = await SuspiciousActivity.find({});
expect(suspiciousActivities.length).toBeGreaterThan(0); 

// Check fraud score
async function checkFraudScore() {
  try {
    log('Checking fraud score...');
    
    const response = await fetch(`${API_BASE_URL}/api/fraud/score?userId=${userId}`, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
        'x-api-key': API_KEY
      }
    });
    
    // Process and display the score
    const data = await response.json();
    scoreValueEl.textContent = data.score || 'N/A';
  } catch (error) {
    log(`Error checking fraud score: ${error.message}`);
  }
} 