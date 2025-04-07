# AdMob Integration

This module provides a separate entry point for AdMob integration with EventWish. The implementation is designed to be non-intrusive and maintainable.

## Architecture

The AdMob integration follows a modular architecture:

- **Models**: Data models for ad units and responses
  - `AdUnit`: Represents ad units from the backend
  - `AdMobResponse`: Handles API responses for AdMob endpoints
  - `AdConstants`: Contains constants for AdMob integration

- **Repository**: 
  - `AdMobRepository`: Handles communication with the backend API for ad-related operations

- **Manager**:
  - `AdMobManager`: Main entry point for AdMob functionality

- **UI Components**:
  - `AdBannerView`: Custom view for displaying banner ads

## Usage

### Initialization

The AdMob integration is initialized in the `EventWishApplication` class:

```java
// Initialize AdMobManager
AdMobManager.init(this);
adMobManager = AdMobManager.getInstance();
```

### Adding a Banner Ad

To add a banner ad to a layout:

```xml
<com.ds.eventwish.ads.AdBannerView
    android:id="@+id/adBannerView"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    app:testMode="true"
    app:enabled="true"
    app:adType="BANNER" />
```

### Fragment/Activity Integration

In your Fragment or Activity:

```java
// Get AdMobManager instance
adMobManager = ((EventWishApplication) requireActivity().getApplication()).getAdMobManager();

// Check if AdMob is enabled
if (adMobManager != null && adMobManager.isInitialized() && adMobManager.isEnabled()) {
    // Initialize your AdBannerView
    adBannerView.setTestMode(adMobManager.isTestModeEnabled());
    adBannerView.setEnabled(true);
} else {
    // Hide ad view
    adBannerView.setVisibility(View.GONE);
}
```

## Backend API

The integration relies on backend API endpoints:

- `GET /api/admob/units`: Get available ad units
- `GET /api/admob/status`: Get ad status and cooldowns
- `POST /api/admob/reward`: Handle reward redemption
- `POST /api/admob/impression`: Record ad impression
- `POST /api/admob/click`: Record ad click

## Future Extensions

This integration is designed to be extended with:

1. Support for additional ad formats:
   - Interstitial ads
   - Rewarded ads
   - Native ads

2. Enhanced ad targeting features

3. Integration with analytics for performance tracking

## Development Notes

- Test mode is enabled by default in debug builds
- Ad IDs from Google are used for testing
- The integration is designed to work without the Google Mobile Ads SDK until explicitly needed 