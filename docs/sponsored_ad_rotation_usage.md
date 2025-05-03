# Sponsored Ads Rotation Implementation Guide

This guide demonstrates how to implement the sponsored ad rotation feature in your app to display different ads over time without requiring user interaction or app restarts.

## Overview

The ad rotation system provides:
- Smooth transitions between ads
- Fair distribution based on ad priority and impression counts
- Configurable rotation intervals
- Persistence across app restarts
- Memory-efficient implementation

## Server-Side Features

The following API endpoints are available for ad rotation:

1. `GET /api/sponsored-ads/rotation` - Fetches ads for rotation with support for excluding already seen ads
2. `GET /api/sponsored-ads/fair-distribution` - Returns ads with weighted distribution based on priority and impression counts

## Client-Side Implementation

### 1. Basic Implementation (in a Fragment or Activity)

```java
@Override
public void onViewCreated(View view, Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    
    // Find sponsored ad view in layout
    sponsoredAdView = view.findViewById(R.id.sponsored_ad_view);
    
    // Initialize with rotation enabled and a 15-minute rotation interval
    sponsoredAdView.initialize(
        "home_bottom",   // Ad location identifier
        getViewLifecycleOwner(),  // Lifecycle owner for LiveData observation
        this,  // ViewModelStoreOwner for ViewModel acquisition
        true,  // Enable rotation
        15     // Rotate every 15 minutes
    );
    
    // Don't forget to clean up when view is destroyed
    @Override
    public void onDestroyView() {
        if (sponsoredAdView != null) {
            sponsoredAdView.cleanup();
        }
        super.onDestroyView();
    }
}
```

### 2. Enabling/Disabling Rotation on Existing Ad Views

If you have already initialized the ad view without rotation, you can enable or disable it later:

```java
// Enable rotation
sponsoredAdView.enableRotation(true);

// Set rotation interval (in minutes)
sponsoredAdView.setRotationIntervalMinutes(20);

// Disable rotation
sponsoredAdView.enableRotation(false);
```

### 3. XML Layout Example

```xml
<com.ds.eventwish.ui.ads.SponsoredAdView
    android:id="@+id/sponsored_ad_view"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_marginTop="16dp"
    android:layout_marginBottom="16dp"
    app:layout_constraintTop_toBottomOf="@id/some_other_view"
    app:layout_constraintStart_toStartOf="parent"
    app:layout_constraintEnd_toEndOf="parent" />
```

## Customization Options

### Rotation Interval

You can set the rotation interval between 5 minutes and 24 hours. The default is 20 minutes.

```java
// Set to 30 minutes
sponsoredAdView.setRotationIntervalMinutes(30);

// Set to 1 hour
sponsoredAdView.setRotationIntervalMinutes(60);
```

### Transition Animations

The system automatically handles cross-fade animations between ads. The default transition duration is 300ms.

## Performance Considerations

1. **Memory Usage**: The implementation is optimized to prevent memory leaks by:
   - Properly cleaning up resources
   - Using weak references where appropriate
   - Removing event listeners when views are detached

2. **Network Usage**: The system minimizes network calls by:
   - Fetching multiple ads at once
   - Storing ads in the local database
   - Only refreshing when necessary
   - Using a cache-first approach

3. **Battery Impact**: The rotation mechanism is designed to be battery-efficient by:
   - Using Handler for scheduling instead of AlarmManager
   - Suspending rotation when app is in background
   - Optimizing image loading with proper caching

## Debugging

To see detailed logs about ad rotation, filter LogCat for these tags:
- `LocalRotationManager` - Rotation timing and state management
- `SponsoredAdView` - UI transitions and animations
- `SponsoredAdViewModel` - Ad selection and LiveData events
- `SponsoredAdRepository` - Caching and network operations

## Best Practices

1. Use appropriate rotation intervals based on your app's usage patterns
   - News/Content apps: 5-10 minutes
   - Utility apps: 15-30 minutes
   - Gaming apps: Between game sessions

2. Place sponsored ads in locations where they won't disrupt user experience

3. Test rotation behavior with different network conditions

4. Monitor impression and click metrics to optimize rotation intervals

## Real-World Implementation Example

Below is a real-world example from the `HomeFragment` class that demonstrates how to properly implement ad rotation with lifecycle management:

```java
// In HomeFragment.java
@Override
public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    
    // Setup UI and other components
    setupUI();
    // ... other initialization code ...
}

private void setupUI() {
    // ... other UI setup code ...
    
    // Initialize sponsored ad view at the bottom of home screen
    sponsoredAdView = binding.sponsoredAdView;
    if (sponsoredAdView != null) {
        // Initialize with location that matches server-side configuration
        sponsoredAdView.initialize("category_below", getViewLifecycleOwner(), requireActivity());
        
        // Enable ad rotation with 3-minute interval for better user experience
        sponsoredAdView.enableRotation(true);
        sponsoredAdView.setRotationIntervalMinutes(3);
        Log.d(TAG, "Initialized sponsored ad view with rotation enabled");
    }
}

@Override
public void onResume() {
    super.onResume();
    
    // ... other onResume code ...
    
    // Refresh sponsored ads when the fragment resumes
    if (sponsoredAdView != null) {
        sponsoredAdView.refreshAds();
        // Ensure rotation is running when the fragment is visible
        sponsoredAdView.enableRotation(true);
    }
}

@Override
public void onPause() {
    super.onPause();
    
    // Pause ad rotation when the fragment is not visible to save resources
    if (sponsoredAdView != null) {
        sponsoredAdView.enableRotation(false);
    }
}

@Override
public void onDestroyView() {
    // ... other cleanup code ...
    
    // Properly cleanup the sponsored ad view
    if (sponsoredAdView != null) {
        sponsoredAdView.cleanup();
        sponsoredAdView = null;
    }
    
    binding = null;
    super.onDestroyView();
}
```

This implementation shows best practices:
- Initialize in `setupUI()` or `onViewCreated()`
- Enable rotation with appropriate interval
- Resume rotation in `onResume()`
- Pause rotation in `onPause()` to save resources
- Clean up properly in `onDestroyView()`

## Advanced Usage

For more advanced scenarios, you can directly access the underlying rotation manager:

```java
// In a ViewModel or Repository
LocalRotationManager rotationManager = new LocalRotationManager(context, sponsoredAdRepository);
rotationManager.setRotationInterval(TimeUnit.MINUTES.toMillis(15));
rotationManager.startRotation("location_id", new LocalRotationManager.RotationCallback() {
    @Override
    public void onAdRotated(SponsoredAd ad) {
        // Handle custom rotation logic
    }
});
``` 