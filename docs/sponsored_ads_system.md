# Sponsored Ads System Documentation

## Overview

The EventWish app implements a custom sponsored ads system that displays promotional content in strategic locations throughout the app. The system includes server-side ad management, client-side rendering, caching, and tracking of impressions and clicks.

## System Architecture

The sponsored ads implementation follows the MVVM architecture pattern and consists of the following key components:

### Data Layer

1. **SponsoredAd Model**: Represents an ad with fields for image URL, redirect URL, title, description, location, status, and tracking data.

2. **SponsoredAdRepository**: Manages data access with both network and local caching:
   - Fetches ads from server
   - Stores ads in local database
   - Handles impression and click tracking
   - Implements caching with TTL (time-to-live)
   - Provides offline support

3. **SponsoredAdEntity & SponsoredAdDao**: Room database components for local ad storage.

### View Layer

1. **SponsoredAdView**: Custom UI component that displays ad content:
   - Extends FrameLayout
   - Handles loading, error, and success states
   - Implements fade animations for smooth transitions
   - Manages visibility tracking for impressions
   - Handles click events

### ViewModel Layer

1. **SponsoredAdViewModel**: Manages ad selection and tracking:
   - Selects best ad based on priority or randomization
   - Provides LiveData for UI updates
   - Coordinates impression and click recording
   - Handles error states

### Factory

1. **SponsoredAdManagerFactory**: Singleton factory that:
   - Manages instances of SponsoredAdView
   - Creates and maintains ViewModels
   - Handles registration/unregistration of ad views

## Ad Display Process

1. **Initialization**: A UI component (like HomeFragment) initializes a SponsoredAdView with a specific location.
2. **Ad Selection**:
   - The ViewModel requests ads for the specified location
   - The Repository checks local cache first, then network if needed
   - Ads are filtered by location, status, and date validity
   - The best ad is selected based on priority
3. **Rendering**:
   - The selected ad is provided to the SponsoredAdView
   - Loading state is shown during image download
   - Image is loaded with Glide using optimized settings
   - Success state is shown when all content is ready
   - Error state is shown when loading fails

## Impression Tracking System

The impression tracking system ensures accurate ad view counting with several important features:

### 1. Visibility Tracking

Impressions are only counted when the ad is actually visible to the user, using these criteria:
- The ad must be fully loaded (image and data)
- The view must be visible on screen (not off-screen or hidden)
- The view must have positive dimensions (width > 0, height > 0)
- The ad must be visible for at least 1 second (configurable via `IMPRESSION_MIN_VISIBLE_TIME_MS`)

```java
private void checkForValidImpression() {
    if (currentAd == null || !adFullyLoaded || impressionTracked || !isVisibleToUser) {
        return;
    }
    
    long now = System.currentTimeMillis();
    long visibleTime = now - visibleSince;
    
    // Only track impression if visible for minimum time
    if (visibleTime >= IMPRESSION_MIN_VISIBLE_TIME_MS) {
        trackImpression();
    }
}
```

### 2. Impression Throttling

To prevent duplicate counting and API overload, impressions are throttled:
- Each ad impression is tracked at most once per day per device
- Throttling state is persisted using SharedPreferences
- In-memory cache provides additional protection
- Daily throttling is implemented using day-based keys

```java
// Create daily impression key for improved throttling
long today = System.currentTimeMillis() / (1000 * 60 * 60 * 24); // Days since epoch
String impressionKey = "impression_" + adId + "_" + today;
boolean alreadyTrackedToday = prefs.getBoolean(impressionKey, false);

// Check throttling state
if (alreadyTrackedToday) {
    Log.d(TAG, "Impression already tracked today for ad: " + adId);
    return;
}

// Mark as tracked
prefs.edit().putBoolean(impressionKey, true).apply();
```

### 3. Batch Processing

Impressions are batched to reduce network calls:
- Impressions are added to a queue until batch size is reached (default: 10)
- Batch processing runs at regular intervals (default: 5 minutes)
- Stale events older than 24 hours are automatically cleaned up
- Synchronized collections ensure thread safety

```java
// Add to pending batch with thread safety
synchronized (pendingImpressions) {
    pendingImpressions.add(event);
    Log.d(TAG, "Added impression to batch queue. Queue size: " + pendingImpressions.size());
}

// Process in batches
private int processImpressionBatch() {
    List<TrackingEvent> batchToProcess;
    
    synchronized (pendingImpressions) {
        if (pendingImpressions.isEmpty()) {
            return 0;
        }
        
        // Take up to MAX_BATCH_SIZE events
        int batchSize = Math.min(pendingImpressions.size(), MAX_BATCH_SIZE);
        batchToProcess = new ArrayList<>(pendingImpressions.subList(0, batchSize));
        
        // Remove processed events
        for (int i = 0; i < batchSize; i++) {
            pendingImpressions.remove(0);
        }
    }
    
    // Process each event
    int successCount = 0;
    for (TrackingEvent event : batchToProcess) {
        if (recordImpressionToServer(event.adId, event.deviceId)) {
            successCount++;
        }
    }
    
    return successCount;
}
```

### 4. Offline Support

Impressions are tracked even when offline:
- Impressions are stored locally when no network is available
- The local database is updated immediately even if API calls fail
- Pending impressions are sent when connectivity is restored
- The app maintains retry logic with backoff

### 5. Rate Limiting Protection

The system protects against server rate limiting:
- Detects 429 HTTP responses from the server
- Implements backoff periods (default: 60 minutes)
- Uses exponential backoff for retries
- Respects Retry-After headers when available

```java
private void handleRateLimiting(Response<?> response) {
    isRateLimited = true;
    rateLimitExpiresAt = System.currentTimeMillis() + RATE_LIMIT_BACKOFF_MS;
    
    // Try to extract Retry-After header if available
    String retryAfter = response.headers().get("Retry-After");
    if (retryAfter != null && !retryAfter.isEmpty()) {
        try {
            int seconds = Integer.parseInt(retryAfter.trim());
            rateLimitExpiresAt = System.currentTimeMillis() + (seconds * 1000L);
        } catch (NumberFormatException e) {
            // Use default backoff
        }
    }
}
```

## Click Tracking System

The click tracking system implements similar protections and features:

### 1. Database-First Approach

Every click is immediately recorded in the local database:
- `updateLocalClickCount` increments click count in the database
- Ensures clicks are tracked even if network fails
- Updates happen on a background thread to prevent UI freezes

```java
private void updateLocalClickCount(String adId) {
    executors.diskIO().execute(() -> {
        try {
            SponsoredAdEntity adEntity = sponsoredAdDao.getById(adId);
            if (adEntity != null) {
                adEntity.setClickCount(adEntity.getClickCount() + 1);
                sponsoredAdDao.update(adEntity);
                Log.d(TAG, "Updated local click count for ad: " + adId + 
                      " to " + adEntity.getClickCount());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating local click count", e);
        }
    });
}
```

### 2. Immediate Processing

Clicks are high-priority events:
- Clicks are processed immediately if online
- Batch processing is used as fallback for offline clicks
- The redirect URL is opened immediately for user feedback

### 3. Retry Mechanism

Clicks implement a retry mechanism:
- Failed click tracking is retried up to 3 times
- Each retry has increased delay
- Permanent failures are logged but don't block UX

```java
// TrackingEvent with retry support
private static class TrackingEvent {
    final String adId;
    final String deviceId;
    final long timestamp;
    int retryCount = 0;
    
    boolean incrementRetry() {
        retryCount++;
        return retryCount <= MAX_RETRY_ATTEMPTS;
    }
}
```

### 4. Thread Safety

The click tracking system ensures thread safety:
- Synchronized collections prevent race conditions
- Atomic operations for incrementing counters
- Handler posting for UI-thread safety

## Performance Optimizations

Several optimizations improve tracking performance:

1. **Image Loading**: Glide is configured with optimized settings:
   - Memory and disk caching
   - WebP format for efficiency
   - Target size optimization
   - Quality settings for balance

2. **Memory Management**:
   - Resources are properly cleaned up in lifecycle methods
   - References are cleared to prevent memory leaks
   - Handler callbacks are removed when views detach

3. **Network Efficiency**:
   - Batch processing reduces API calls
   - In-memory caching reduces database access
   - TTL-based caching prevents unnecessary server load

4. **Error Recovery**:
   - Multiple retry attempts with backoff
   - Stale event cleanup to prevent memory buildup
   - Detailed logging for debugging

## Integration in UI

To integrate a sponsored ad in any UI component:

```java
// In layout XML
<com.ds.eventwish.ui.ads.SponsoredAdView
    android:id="@+id/sponsored_ad_view"
    android:layout_width="match_parent"
    android:layout_height="wrap_content" />

// In Fragment or Activity
SponsoredAdView sponsoredAdView = findViewById(R.id.sponsored_ad_view);
sponsoredAdView.initialize("home_bottom", getViewLifecycleOwner(), this);

// Remember to clean up
@Override
public void onDestroyView() {
    if (sponsoredAdView != null) {
        sponsoredAdView.cleanup();
    }
    super.onDestroyView();
}
```

## Server API Endpoints

The system interacts with the following API endpoints:

1. **GET /api/sponsored-ads**: Fetches available ads, filtered by location
2. **POST /api/sponsored-ads/viewed/:id**: Records an impression with device ID
3. **POST /api/sponsored-ads/clicked/:id**: Records a click with device ID
4. **GET /api/sponsored-ads/stats/:id**: (Admin only) Get performance statistics

## Troubleshooting

Common issues and solutions:

1. **Ads not showing**: 
   - Check if valid ads exist for the requested location
   - Verify ad status is active and dates are valid
   - Ensure image URLs are reachable

2. **Impression not tracking**:
   - Check if the ad is fully visible on screen
   - Verify throttling isn't blocking tracking (once per day)
   - Check network connectivity for API calls

3. **Click not tracking**:
   - Ensure device has unique ID
   - Verify redirect URL opens properly
   - Check for rate limiting blocks

4. **Performance issues**:
   - Monitor batch sizes and processing times
   - Check image loading performance
   - Verify memory usage with profiler

## Data Privacy

The tracking system implements several privacy-focused features:

1. Impressions and clicks are associated with device IDs, not user IDs
2. Device IDs are hashed for additional privacy
3. No personally identifiable information is collected
4. Ads are clearly labeled as "Sponsored" for transparency
5. Data is stored securely with proper encryption

## Analytics Integration

The system integrates with the app's analytics system:
- Impression and click events are tracked via AnalyticsUtils
- Events include ad ID, title, and location for analysis
- Performance metrics can be monitored via analytics dashboards

```java
// Analytics tracking
AnalyticsUtils.getInstance().trackAdImpression(
    ad.getId(),
    ad.getTitle(),
    ad.getLocation()
);
``` 