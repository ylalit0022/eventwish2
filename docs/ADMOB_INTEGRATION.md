# AdMob Integration Technical Documentation

## Project Structure

```
app/
├── src/
│   └── main/
│       ├── java/com/ds/eventwish/
│       │   ├── ads/
│       │   │   ├── AdDemoActivity.java       # Activity for testing ad units
│       │   │   ├── AdMobManager.java         # Singleton for managing AdMob SDK
│       │   │   └── AdMobRepository.java      # Repository for fetching ad units
│       │   ├── data/
│       │   │   ├── model/
│       │   │   │   └── ads/
│       │   │   │       ├── AdUnit.java       # Server response model
│       │   │   │       └── SimpleAdUnit.java # Local model for ad units
│       │   │   └── remote/
│       │   │       ├── ApiClient.java        # Retrofit client setup
│       │   │       └── ApiService.java       # API endpoints interface
│       │   └── ui/
│       │       └── ads/
│       │           └── TestAdActivity.java    # Activity for ad testing
│       └── res/
│           ├── drawable/
│           │   └── ic_ads.xml                # Vector icon for ad activities
│           ├── layout/
│           │   └── activity_ad_demo.xml      # Layout for AdDemoActivity
│           ├── menu/
│           │   └── menu_main.xml             # Menu with ad testing shortcut
│           └── values/
│               └── strings.xml               # String resources for ad-related text
```

## Server Communication

### API Endpoint
- Base URL: `https://eventwish2.onrender.com/api`
- Endpoint: `/admob/units`
- Method: GET
- Query Parameter: `adType` (app_open, banner, interstitial, rewarded)

### Required Headers
```
x-api-key: ew_dev_c1ce47afeff9fa8b7b1aa165562cb915
x-app-signature: app_sig_1
x-device-id: [device-specific hash]
Content-Type: application/json
```

### Response Format
```json
{
  "success": boolean,
  "message": string,
  "data": [
    {
      "adName": string,      // Unique identifier for the ad unit
      "adType": string,      // Type of ad (banner, interstitial, etc.)
      "adUnitCode": string,  // AdMob unit ID
      "status": boolean      // Whether the ad unit is enabled
    }
  ]
}
```

## Key Components

### AdMobManager
- Singleton class for managing AdMob SDK initialization
- Handles initialization state tracking
- Provides retry mechanism for initialization failures
- Must be initialized before any ad operations

```java
// Proper initialization sequence
AdMobManager.init(context);
AdMobManager instance = AdMobManager.getInstance();
```

### AdMobRepository
- Handles fetching ad units from server
- Manages API communication through Retrofit
- Provides callbacks for success/failure scenarios
- Implements error handling for server hibernation (503)

```java
// Example usage
adMobRepository.fetchAdUnit("interstitial", new AdUnitCallback() {
    @Override
    public void onSuccess(JsonObject response) {
        // Handle successful response
    }
    
    @Override
    public void onError(String error) {
        // Handle error
    }
});
```

## Error Handling

### Server Errors
1. Server Hibernation (503)
   - Occurs when server is waking up
   - Implement retry mechanism with exponential backoff
   - Log detailed error information

2. Network Errors
   - SSL/TLS errors
   - Connection timeouts
   - No internet connection

### Client Errors
1. SDK Initialization
   - Track initialization state
   - Provide clear error messages
   - Implement retry mechanism

2. Ad Unit Loading
   - Handle missing ad units
   - Validate ad unit IDs
   - Check enabled status

## Best Practices

1. Server Communication
   - Always include required headers
   - Validate response format
   - Handle all error cases
   - Implement proper logging

2. Ad Unit Management
   - Check ad unit status before use
   - Validate ad unit codes
   - Handle disabled ad units gracefully
   - Log ad unit loading attempts

3. Testing
   - Use test ad unit IDs in debug builds
   - Implement UI for testing different ad types
   - Log all API responses
   - Monitor ad loading performance

## Required Permissions
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

## Manifest Configuration
```xml
<!-- AdMob App ID -->
<meta-data
    android:name="com.google.android.gms.ads.APPLICATION_ID"
    android:value="ca-app-pub-3940256099942544~3347511713"/>

<!-- Ad Demo Activity -->
<activity
    android:name=".ads.AdDemoActivity"
    android:exported="true"
    android:label="AdMob Demo"
    android:icon="@drawable/ic_ads"
    android:theme="@style/Theme.MaterialComponents.Light.DarkActionBar">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
</activity>
```

## Lessons Learned

1. Server Response Handling
   - Server uses `adName` instead of `id`
   - `adUnitCode` contains the actual AdMob unit ID
   - `status` field determines if ad unit is enabled
   - Response is wrapped in a data array

2. Error Cases
   - Server hibernation requires retry logic
   - SSL errors need proper error messages
   - Network timeouts should trigger retries
   - Invalid ad unit IDs must be handled

3. Testing Requirements
   - Test all ad types individually
   - Verify server responses
   - Check error handling
   - Monitor loading performance
   - Test with both test and production ad units

4. Future Improvements
   - Implement caching for offline use
   - Add analytics for ad performance
   - Improve error reporting
   - Enhance testing UI
   - Add support for new ad formats 