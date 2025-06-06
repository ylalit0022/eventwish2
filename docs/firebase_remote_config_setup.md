# Firebase Remote Config for App Update Checking

## Overview

This document outlines how to use Firebase Remote Config to check for app updates during development and track update prompt impressions and conversion rates using analytics.

## Implementation Status

✅ **COMPLETE** - All components have been successfully implemented and tested.

## Table of Contents

1. [Setup Instructions](#setup-instructions)
2. [Usage](#usage)
3. [Remote Config Parameters](#remote-config-parameters)
4. [Testing](#testing)
5. [Analytics Dashboard Setup](#analytics-dashboard-setup)
6. [Troubleshooting](#troubleshooting)

Firebase Remote Config allows us to check for app updates during development and in production, with detailed analytics tracking of update prompts and user actions.

## Setup Instructions

### 1. Firebase Console Setup

1. Go to the [Firebase Console](https://console.firebase.google.com/)
2. Select the EventWish project
3. Navigate to Remote Config in the left menu
4. Add the following parameters:

| Parameter Name | Data Type | Default Value | Description |
|---------------|-----------|---------------|-------------|
| `latest_version_code` | Number | Current version code | The latest app version code |
| `latest_version_name` | String | Current version name | The latest app version name (e.g., "2.7.0") |
| `update_message` | String | "A new version of the app is available." | Message to show in update dialog |
| `force_update` | Boolean | false | Whether the update is mandatory |
| `update_url` | String | Play Store URL | URL to download the update |

5. Click "Publish changes" to make the configuration available

### 2. Testing Update Flows

To test different update scenarios:

#### No Update Available
- Set `latest_version_code` equal to or less than your app's current version code

#### Optional Update Available
- Set `latest_version_code` higher than your app's current version code
- Set `force_update` to `false`

#### Mandatory Update Available
- Set `latest_version_code` higher than your app's current version code
- Set `force_update` to `true`

### 3. Analytics Dashboard Setup

For basic analytics setup:

1. Go to the Firebase Console
2. Navigate to Analytics
3. Create a new custom dashboard named "App Updates"
4. Add the following reports:

#### Update Check Report
- Dimension: Event name
- Filter: Event name equals `update_check`
- Metrics: Event count

#### Update Available Report
- Dimension: Event name
- Filter: Event name equals `update_available_impression`
- Metrics: Event count

#### Update Prompt Action Report
- Dimension: action
- Filter: Event name equals `update_prompt_action`
- Metrics: Event count

#### Update Funnel Report
- Step 1: Event name equals `update_check`
- Step 2: Event name equals `update_available_impression`
- Step 3: Event name equals `update_prompt_action` AND action equals "accepted"

For comprehensive analytics dashboards, see the detailed instructions in [Update Analytics Dashboards](update_analytics_dashboards.md).

## Usage in Code

### Checking for Updates

```java
// In MainActivity or other appropriate location
AppUpdateViewModel appUpdateViewModel = AppUpdateViewModel.getInstance(this);
appUpdateViewModel.init(this);

if (BuildConfig.DEBUG) {
    // For debug builds, use Remote Config
    appUpdateViewModel.checkForUpdatesWithRemoteConfig();
} else {
    // For production builds, use Play Store
    appUpdateViewModel.checkForUpdates(false);
}
```

### Silent Update Checking

```java
// Check for updates without showing UI
AppUpdateViewModel appUpdateViewModel = AppUpdateViewModel.getInstance(this);
appUpdateViewModel.checkForUpdatesSilentlyWithRemoteConfig();
```

## Tracked Analytics Events

| Event Name | Description | Key Parameters |
|------------|-------------|----------------|
| `update_check` | Fired when app checks for updates | `current_version_code`, `latest_version_code`, `is_update_available` |
| `update_available_impression` | Fired when an update is available | `current_version_name`, `latest_version_name`, `is_force_update` |
| `update_dialog_shown` | Fired when update dialog is displayed | `version_name`, `is_force_update` |
| `update_prompt_action` | Fired when user takes action on update prompt | `action` ("accepted", "declined", "deferred"), `version_name` |

## Key Metrics to Monitor

1. **Update Check Rate**: % of sessions where update check occurs
2. **Update Available Rate**: % of checks where update is available
3. **Update Prompt Impression Rate**: % of users who see update dialog
4. **Update Acceptance Rate**: % of users who accept the update
5. **Update Deferral Rate**: % of users who defer the update
6. **Update Completion Rate**: % of users who complete the update process
7. **Time-to-Update**: Average time between update availability and acceptance
8. **Version Distribution**: % of users on each app version

## Troubleshooting

### Common Issues

1. **Remote Config not fetching**: Check network connectivity and Firebase initialization
2. **Update dialog not showing**: Verify that the activity is not null or finishing
3. **Analytics events not appearing**: Check that Firebase Analytics is properly initialized

### Debug Logging

Enable verbose logging to see detailed information about update checks:

```java
// Enable debug logging
FirebaseRemoteConfig.getInstance().setConfigSettingsAsync(
    new FirebaseRemoteConfigSettings.Builder()
        .setMinimumFetchIntervalInSeconds(0)
        .build()
);
```

## Best Practices

1. **Fetch on App Start**: Always fetch Remote Config values on app start
2. **Respect User Choice**: Don't force updates unless absolutely necessary
3. **Clear Error Messages**: Provide clear update messages to users
4. **Graceful Fallbacks**: Handle errors gracefully if Remote Config fails
5. **Track Everything**: Use analytics to understand user update behavior