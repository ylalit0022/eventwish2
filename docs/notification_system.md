# EventWish Notification System Documentation

This document provides comprehensive information about the EventWish notification system, including JSON formats, configuration options, and implementation details.

## Table of Contents

1. [Overview](#overview)
2. [Firebase Remote Config Integration](#firebase-remote-config-integration)
3. [JSON Configuration Formats](#json-configuration-formats)
   - [Single Event Format](#single-event-format)
   - [Multiple Events Format](#multiple-events-format)
4. [Notification Scheduling](#notification-scheduling)
5. [Configuration Fields Reference](#configuration-fields-reference)
6. [Sample JSON Configurations](#sample-json-configurations)
7. [Testing and Debugging](#testing-and-debugging)

## Overview

The EventWish notification system allows for scheduled push notifications about upcoming festivals and events. Notifications are configured through Firebase Remote Config, which provides flexibility to update notification content without requiring app updates.

## Key Components

1. **EventNotificationManager**: Central class that manages notification scheduling and display
2. **EventNotificationConfig**: Data model for notification configuration
3. **EventNotificationReceiver**: BroadcastReceiver for handling scheduled notifications
4. **RemoteConfigWrapper**: Wrapper for Firebase Remote Config JSON structure

## Configuration Format

The notification system supports two configuration formats:

### Single Event Configuration

```json
{
  "event_push": {
    "id": "single_event",
    "title": "Event Title",
    "body": "Event description with {{countdown}} left!",
    "status": true,
    "startdate": "2023-06-01T00:00:00Z",
    "enddate": "2023-12-31T23:59:59Z",
    "deeplinksupport": "eventwish://open/festival_notification",
    "showTime": "08:00"
  }
}
```

### Multiple Events Configuration

```json
{
  "events": [
    {
      "id": "diwali_2023",
      "title": "Diwali Festival 2023",
      "body": "Diwali celebration is coming soon! Only {{countdown}} left!",
      "status": true,
      "startdate": "2023-10-01T00:00:00Z",
      "enddate": "2023-11-12T23:59:59Z",
      "deeplinksupport": "eventwish://open/festival_notification",
      "showTime": "18:30"
    },
    {
      "id": "christmas_2023",
      "title": "Christmas 2023",
      "body": "Christmas is almost here! Only {{countdown}} left!",
      "status": true,
      "startdate": "2023-11-25T00:00:00Z",
      "enddate": "2023-12-25T23:59:59Z",
      "deeplinksupport": "eventwish://open/festival_notification",
      "showTime": "09:00"
    }
  ]
}
```

## Configuration Fields

| Field | Type | Description | Required | Default |
|-------|------|-------------|----------|---------|
| id | String | Unique identifier for the notification event | Yes | - |
| title | String | Title of the notification | Yes | - |
| body | String | Body text of the notification (supports `{{countdown}}` placeholder) | Yes | - |
| status | Boolean | Whether the notification is enabled | Yes | - |
| startdate | String | Start date in ISO 8601 format | Yes | - |
| enddate | String | End date in ISO 8601 format | Yes | - |
| deeplinksupport | String | Deep link URL to open the FestivalNotificationFragment when notification is tapped | No | - |
| showTime | String | Time to show notification (HH:MM) | No | "08:00" |

## Notification Scheduling

Notifications are scheduled based on the following rules:

1. Notifications are only shown if the current date is between `startdate` and `enddate`
2. Notifications are shown at the time specified in `showTime` (default: 08:00)
3. Notifications include a countdown to the `enddate` (e.g., "3 days left")
4. Each notification has a unique ID based on the event ID to prevent duplicates
5. Notifications are rescheduled when the app starts and after device boot

## Deep Linking

When a user taps on a notification, the app opens to the FestivalNotificationFragment using the deep link specified in the `deeplinksupport` field. The recommended deep link format is:

```
eventwish://open/festival_notification
```

This deep link will navigate directly to the festival notification screen showing all upcoming festivals.

## Testing

The notification system includes a test activity (`EventNotificationTestActivity`) that allows for:

1. Viewing current notification configuration
2. Testing notification display
3. Setting custom notification configuration for testing
4. Viewing scheduled notifications

## Implementation Details

### EventNotificationManager

The `EventNotificationManager` class is responsible for:

1. Fetching notification configuration from Firebase Remote Config
2. Parsing and validating configuration
3. Scheduling notifications at the appropriate time
4. Handling notification display
5. Managing notification history

### EventNotificationConfig

The `EventNotificationConfig` class represents a notification configuration with fields for:

1. Event ID, title, and body
2. Start and end dates
3. Status (enabled/disabled)
4. Deep link support
5. Show time

### EventNotificationReceiver

The `EventNotificationReceiver` class is a BroadcastReceiver that:

1. Receives scheduled notification intents
2. Creates and displays notifications
3. Handles notification click actions
4. Reschedules notifications as needed

## Best Practices

1. Use unique IDs for each notification event
2. Set appropriate start and end dates
3. Include meaningful countdown text in the notification body
4. Use deep links to provide a seamless user experience
5. Test notifications thoroughly before deployment

## Troubleshooting

Common issues and solutions:

1. **Notifications not showing**: Check that the current date is between `startdate` and `enddate`
2. **Incorrect countdown**: Verify the `enddate` format is correct (ISO 8601)
3. **Deep links not working**: Ensure the app has proper deep link handling configured
4. **Multiple notifications**: Check for duplicate event IDs
5. **Notification permission**: Ensure the app has notification permission

## Conclusion

The EventWish notification system provides a flexible and powerful way to engage users with timely reminders about upcoming festivals and events. By leveraging Firebase Remote Config, notifications can be updated without requiring app updates.

## Firebase Remote Config Integration

The system uses two main parameters in Firebase Remote Config:

1. `event_push` - For backward compatibility with single event configuration
2. `events` - An array of event configurations for multiple events

## JSON Configuration Formats

### Single Event Format

For a single event notification:

```json
{
  "id": "unique_event_id",
  "title": "Event Title",
  "body": "Event description with {{countdown}} placeholder",
  "status": true,
  "startdate": "YYYY-MM-DDT00:00:00Z",
  "enddate": "YYYY-MM-DDT23:59:59Z",
  "deeplinksupport": "eventwish://open/event/path",
  "showTime": "HH:MM"
}
```

### Multiple Events Format

For multiple event notifications:

```json
[
  {
    "id": "event_id_1",
    "title": "First Event Title",
    "body": "Event description with {{countdown}} placeholder",
    "status": true,
    "startdate": "YYYY-MM-DDT00:00:00Z",
    "enddate": "YYYY-MM-DDT23:59:59Z",
    "deeplinksupport": "eventwish://open/event/path1",
    "showTime": "HH:MM"
  },
  {
    "id": "event_id_2",
    "title": "Second Event Title",
    "body": "Event description with {{countdown}} placeholder",
    "status": true,
    "startdate": "YYYY-MM-DDT00:00:00Z",
    "enddate": "YYYY-MM-DDT23:59:59Z",
    "deeplinksupport": "eventwish://open/event/path2",
    "showTime": "HH:MM"
  }
]
```

## Notification Scheduling

Notifications are shown on specific days before the event:
- 30 days before
- 14 days before
- 7 days before
- 3 days before
- 1 day before
- On the event day

Each notification is shown at the time specified in the `showTime` field (default: 08:00).

## Configuration Fields Reference

| Field | Type | Description | Required | Default |
|-------|------|-------------|----------|---------|
| id | String | Unique identifier for the event | Yes | - |
| title | String | Notification title | Yes | - |
| body | String | Notification body text | Yes | - |
| status | Boolean | Whether the notification is enabled | Yes | - |
| startdate | String | Start date in ISO 8601 format | Yes | - |
| enddate | String | End date in ISO 8601 format | Yes | - |
| deeplinksupport | String | Deep link URL | No | - |
| showTime | String | Time to show notification (HH:MM) | No | "08:00" |

### Notes:
- The `{{countdown}}` placeholder in the body text will be replaced with the actual countdown (e.g., "3 days", "1 day", "Today").
- Dates must be in ISO 8601 format with UTC timezone (e.g., "2023-12-25T00:00:00Z").
- Time must be in 24-hour format (e.g., "18:30" for 6:30 PM).

## Sample JSON Configurations

### Sample 1: Diwali Festival

```json
{
  "id": "diwali_2023",
  "title": "Diwali Festival 2023",
  "body": "Diwali celebration is coming soon! Only {{countdown}} left!",
  "status": true,
  "startdate": "2023-10-01T00:00:00Z",
  "enddate": "2023-11-12T23:59:59Z",
  "deeplinksupport": "eventwish://open/festival_notification",
  "showTime": "18:30"
}
```

### Sample 2: Multiple Festivals

```json
[
  {
    "id": "diwali_2023",
    "title": "Diwali Festival 2023",
    "body": "Diwali celebration is coming soon! Only {{countdown}} left!",
    "status": true,
    "startdate": "2023-10-01T00:00:00Z",
    "enddate": "2023-11-12T23:59:59Z",
    "deeplinksupport": "eventwish://open/festival_notification",
    "showTime": "18:30"
  },
  {
    "id": "christmas_2023",
    "title": "Christmas 2023",
    "body": "Christmas is almost here! Only {{countdown}} left!",
    "status": true,
    "startdate": "2023-11-25T00:00:00Z",
    "enddate": "2023-12-25T23:59:59Z",
    "deeplinksupport": "eventwish://open/festival_notification",
    "showTime": "09:00"
  },
  {
    "id": "newyear_2024",
    "title": "New Year 2024",
    "body": "Get ready for New Year celebrations! Only {{countdown}} left!",
    "status": true,
    "startdate": "2023-12-01T00:00:00Z",
    "enddate": "2024-01-01T23:59:59Z",
    "deeplinksupport": "eventwish://open/festival_notification",
    "showTime": "20:00"
  }
]
```

### Sample 3: Disabling an Event

To disable an event temporarily without removing it:

```json
{
  "id": "holi_2024",
  "title": "Holi Festival 2024",
  "body": "Holi celebration is coming soon! Only {{countdown}} left!",
  "status": false,
  "startdate": "2024-02-15T00:00:00Z",
  "enddate": "2024-03-25T23:59:59Z",
  "deeplinksupport": "eventwish://open/festival_notification",
  "showTime": "10:00"
}
```

### Sample 4: Year-Round Event

For an event that spans the entire year:

```json
{
  "id": "birthdays_2023",
  "title": "Birthday Reminders",
  "body": "Don't forget upcoming birthdays! Check the app for details.",
  "status": true,
  "startdate": "2023-01-01T00:00:00Z",
  "enddate": "2023-12-31T23:59:59Z",
  "deeplinksupport": "eventwish://open/festival_notification",
  "showTime": "08:00"
}
```

## Testing and Debugging

The app includes a test activity (`EventNotificationTestActivity`) that allows you to:

1. Check for notifications
2. Fetch the latest configuration
3. Show test notifications
4. Set custom configurations
5. Set notification times
6. Configure multiple events

To test notifications:

1. Open the EventNotificationTestActivity
2. Use the "Set Multiple Events" button to configure events
3. Use the "Check for Notification" button to trigger notification checks
4. Use the "Show Test Notification" button to immediately show a notification

## Firebase Remote Config Setup

In the Firebase Console:

1. Go to Remote Config
2. Add a parameter named `events` with the JSON array of events
3. Set the default value in the app's `remote_config_defaults.xml` file
4. Publish changes

## Troubleshooting

Common issues:

1. **No notifications showing**: Check that the event is active (current date is between startdate and enddate) and status is true.
2. **Wrong notification time**: Verify the showTime format is correct (HH:MM).
3. **Multiple notifications**: Each event with the same notification day will show a separate notification.
4. **JSON parsing errors**: Validate your JSON format using a JSON validator. 