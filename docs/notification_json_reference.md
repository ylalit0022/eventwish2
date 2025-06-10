# Notification JSON Reference

This document provides a quick reference for the JSON structure used in the EventWish notification system.

## Event Notification Config Fields

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `id` | String | Yes | Unique identifier for the notification event |
| `title` | String | Yes | Title of the notification |
| `body` | String | Yes | Body text of the notification (supports `{{countdown}}` placeholder) |
| `status` | Boolean | Yes | Whether the notification is enabled |
| `startdate` | String | Yes | ISO 8601 date string (YYYY-MM-DDThh:mm:ssZ) when notification should start showing |
| `enddate` | String | Yes | ISO 8601 date string (YYYY-MM-DDThh:mm:ssZ) when notification should stop showing |
| `deeplinksupport` | String | No | Deep link URL to open when notification is tapped (use "eventwish://open/festival_notification" to open the festival notification screen) |
| `showTime` | String | No | Time of day to show notification in 24-hour format (HH:MM), defaults to "08:00" |

## JSON Structure Examples

### Single Event

```json
{
  "id": "diwali_2023",
  "title": "Diwali Festival 2023",
  "body": "Diwali celebration is coming soon! Only {{countdown}} left!",
  "status": true,
  "startdate": "2025-06-11T00:00:00Z",
  "enddate": "2025-06-13T23:59:59Z",
  "deeplinksupport": "eventwish://open/festival_notification",
  "showTime": "18:30"
}
```

### Multiple Events Array

```json
[
  {
    "id": "diwali_2023",
    "title": "Diwali Festival 2023",
    "body": "Diwali celebration is coming soon! Only {{countdown}} left!",
    "status": true,
    "startdate": "2025-06-11T00:00:00Z",
    "enddate": "2025-06-13T23:59:59Z",
    "deeplinksupport": "eventwish://open/festival_notification",
    "showTime": "19:41"
  },
  {
    "id": "christmas_2023",
    "title": "Christmas 2023",
    "body": "Christmas is almost here! Only {{countdown}} left!",
    "status": true,
    "startdate": "2025-06-10T00:00:00Z",
    "enddate": "2025-06-12T23:59:59Z",
    "deeplinksupport": "eventwish://open/festival_notification",
    "showTime": "19:45"
  }
]
```

### Firebase Remote Config Format

**Important Note**: The Firebase Remote Config format requires `event_push` to be a single object (not an array) and `events` to be an array for multiple events. Using an incorrect format will result in parsing errors.

```json
{
  "event_push": {
    "id": "single_event",
    "title": "Single Event Example",
    "body": "This is a single event example. Only {{countdown}} left!",
    "status": true,
    "startdate": "2025-06-10T00:00:00Z",
    "enddate": "2025-06-12T23:59:59Z",
    "deeplinksupport": "eventwish://open/festival_notification",
    "showTime": "08:00"
  },
  "events": [
    {
      "id": "diwali_2023",
      "title": "Diwali Festival 2023",
      "body": "Diwali celebration is coming soon! Only {{countdown}} left!",
      "status": true,
      "startdate": "2025-06-11T00:00:00Z",
      "enddate": "2025-06-13T23:59:59Z",
      "deeplinksupport": "eventwish://open/festival_notification",
      "showTime": "19:41"
    },
    {
      "id": "christmas_2023",
      "title": "Christmas 2023",
      "body": "Christmas is almost here! Only {{countdown}} left!",
      "status": true,
      "startdate": "2025-06-10T00:00:00Z",
      "enddate": "2025-06-12T23:59:59Z",
      "deeplinksupport": "eventwish://open/festival_notification",
      "showTime": "19:45"
    }
  ]
} 